/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {getRegistryTarget} from './Registry.mjs';
import {PROJECT_CLASS_TOOLING_ONLY} from './discoverProjects.mjs';
import {
	compareCaretGroups,
	formatCaretGroup,
	getCaretGroup,
	isPrerelease,
} from './versions.mjs';

export const FIX_TYPE_BUMP_MAJOR = 'bump-major';
export const FIX_TYPE_BUMP_MINOR = 'bump-minor';
export const FIX_TYPE_NO_PATCH = 'no-patch';
export const FIX_TYPE_PRUNE_TOOLCHAIN = 'prune-toolchain';
export const FIX_TYPE_RE_RESOLVE = 're-resolve';
export const FIX_TYPE_RESOLUTION = 'resolution';
export const FIX_TYPE_UNKNOWN = 'unknown';

/**
 * Fix types from the cheapest to the most expensive. A finding takes the most
 * expensive fix type among its chains.
 */
export const FIX_TYPES = [
	FIX_TYPE_PRUNE_TOOLCHAIN,
	FIX_TYPE_RE_RESOLVE,
	FIX_TYPE_BUMP_MINOR,
	FIX_TYPE_BUMP_MAJOR,
	FIX_TYPE_RESOLUTION,
	FIX_TYPE_NO_PATCH,
	FIX_TYPE_UNKNOWN,
];

const NO_PATCHED_RANGE = '<0.0.0';

/**
 * Assigns a fix to every chain of every finding, and to every finding the most
 * expensive fix type among its chains.
 */
export default async function assignFixTypes({findings, project, registry}) {
	const fixer = createFixer(registry);

	await Promise.all(
		findings.flatMap((finding) =>
			finding.chains.map(async (chain) => {
				chain.fix = await fixer.getChainFix({chain, finding, project});
			})
		)
	);

	for (const finding of findings) {
		finding.fixType = finding.chains
			.map((chain) => chain.fix.type)
			.sort(
				(left, right) =>
					FIX_TYPES.indexOf(left) - FIX_TYPES.indexOf(right)
			)
			.at(-1);
	}
}

/**
 * Creates the functions that work out fixes, sharing their memoized answers
 * between projects: the same chain seen in many workspaces is worked out once.
 */
export function createFixer(registry) {
	const bumpCache = new Map();
	const chainFixCache = new Map();
	const cleanReleaseCache = new Map();

	/**
	 * Tells whether `name@version` fixes a chain whose remaining elements are
	 * `rest`, resolving each step to its highest match. A step that no longer
	 * depends on the next element breaks the chain, which counts as fixed.
	 */
	async function fixesChain(name, version, rest) {
		let current = await registry.resolve(name, version);

		if (!current) {
			return false;
		}

		for (const nextName of rest) {
			const range = current.dependencies[nextName];

			if (range === undefined) {
				return true;
			}

			current = await registry.resolve(nextName, range);

			if (!current) {
				return false;
			}
		}

		return registry.isClean(current.name, current.version);
	}

	/**
	 * Finds the smallest bump of a direct dependency that fixes a chain: the
	 * highest version of its current caret group first, then the highest
	 * version of each higher caret group in ascending order.
	 */
	function findBump({name, range, rest, version}) {
		const key = [name, range, version, ...rest].join('|');

		if (!bumpCache.has(key)) {
			bumpCache.set(
				key,
				(async () => {
					const target = getRegistryTarget(name, range || version);

					const sameGroup = await registry.resolve(
						target.name,
						`^${version}`
					);

					if (
						sameGroup &&
						sameGroup.version !== version &&
						(await fixesChain(target.name, sameGroup.version, rest))
					) {
						return {
							from: version,
							to: sameGroup.version,
							type: FIX_TYPE_BUMP_MINOR,
						};
					}

					const currentGroup = getCaretGroup(version);

					if (!currentGroup) {
						return null;
					}

					for (const [group, highest] of await getHigherGroups(
						target.name,
						currentGroup
					)) {
						if (await fixesChain(target.name, highest, rest)) {
							return {
								from: version,
								fromMajor: formatCaretGroup(currentGroup),
								to: highest,
								toMajor: formatCaretGroup(group),
								type: FIX_TYPE_BUMP_MAJOR,
							};
						}
					}

					return null;
				})()
			);
		}

		return bumpCache.get(key);
	}

	async function getChainFix({chain, finding, project}) {
		if (project.class === PROJECT_CLASS_TOOLING_ONLY) {
			return {type: FIX_TYPE_PRUNE_TOOLCHAIN};
		}

		if (finding.advisory.patched === NO_PATCHED_RANGE) {
			return {reason: 'no-patched-range', type: FIX_TYPE_NO_PATCH};
		}

		const key = [
			finding.package,
			finding.advisory.ghsa,
			chain.parentRange,
			...chain.installedChain.map(
				(link) => `${link.name}@${link.range}=${link.version}`
			),
		].join('|');

		if (!chainFixCache.has(key)) {
			chainFixCache.set(
				key,
				getUncachedChainFix({chain, finding}).catch((error) => ({
					reason: error.message,
					type: FIX_TYPE_UNKNOWN,
				}))
			);
		}

		return {...(await chainFixCache.get(key))};
	}

	async function getHigherGroups(name, currentGroup) {
		const groups = new Map();

		for (const version of await registry.getVersions(name)) {
			if (isPrerelease(version)) {
				continue;
			}

			const group = getCaretGroup(version);

			if (!group || compareCaretGroups(group, currentGroup) <= 0) {
				continue;
			}

			groups.set(formatCaretGroup(group), [group, version]);
		}

		return [...groups.values()].sort((left, right) =>
			compareCaretGroups(left[0], right[0])
		);
	}

	async function getUncachedChainFix({chain, finding}) {
		const {installedChain, parent, parentRange} = chain;

		const vulnerable = parentRange
			? getRegistryTarget(finding.package, parentRange)
			: {name: finding.package};

		if (!(await hasCleanRelease(vulnerable.name))) {
			return {reason: 'no-clean-release', type: FIX_TYPE_NO_PATCH};
		}

		if (parentRange) {
			const resolved = await registry.resolve(
				finding.package,
				parentRange
			);

			if (
				resolved &&
				(await registry.isClean(resolved.name, resolved.version))
			) {
				return {
					parent,
					parentRange,
					target: resolved.version,
					type: FIX_TYPE_RE_RESOLVE,
				};
			}
		}

		const [direct, ...links] = installedChain;

		if (!direct.version) {
			return {
				reason: `${direct.name} is not in yarn.lock`,
				type: FIX_TYPE_UNKNOWN,
			};
		}

		const bump = await findBump({
			name: direct.name,
			range: direct.range,
			rest: links.map((link) => link.name),
			version: direct.version,
		});

		if (bump) {
			return bump;
		}

		if (!links.length) {
			return {
				reason: `No newer release of ${direct.name} is clean`,
				type: FIX_TYPE_UNKNOWN,
			};
		}

		return getResolution({
			directDependency: direct.name,
			installedVersion: finding.installedVersion,
			parentRange,
			vulnerable,
		});
	}

	/**
	 * Suggests a scoped resolution: the lowest clean version of the
	 * vulnerable package, inside the parent's range when possible.
	 */
	async function getResolution({
		directDependency,
		installedVersion,
		parentRange,
		vulnerable,
	}) {
		const key = `${directDependency}/**/${vulnerable.name}`;

		if (parentRange) {
			const inRange = await registry.getMatchingVersions(
				vulnerable.name,
				vulnerable.range || parentRange
			);

			const value = await findLowestClean(
				vulnerable.name,
				inRange.filter((version) => !isPrerelease(version))
			);

			if (value) {
				return {
					key,
					outsideParentRange: false,
					type: FIX_TYPE_RESOLUTION,
					value,
				};
			}
		}

		const newer = (await registry.getVersions(vulnerable.name)).filter(
			(version) =>
				!isPrerelease(version) &&
				compareCaretGroups(
					getCaretGroup(version) || [],
					getCaretGroup(installedVersion) || []
				) >= 0
		);

		const value = await findLowestClean(vulnerable.name, newer);

		if (!value) {
			return {reason: 'no-clean-release', type: FIX_TYPE_NO_PATCH};
		}

		return {
			key,
			outsideParentRange: true,
			type: FIX_TYPE_RESOLUTION,
			value,
		};
	}

	/**
	 * Finds the lowest clean version in an ascending list with a binary
	 * search. Advisories affect a version prefix in practice (`<x.y.z`), so the
	 * clean versions form a suffix of the list.
	 */
	async function findLowestClean(name, versions) {
		if (
			!versions.length ||
			!(await registry.isClean(name, versions.at(-1)))
		) {
			return null;
		}

		let high = versions.length - 1;
		let low = 0;

		while (low < high) {
			const middle = Math.floor((low + high) / 2);

			if (await registry.isClean(name, versions.at(middle))) {
				high = middle;
			}
			else {
				low = middle + 1;
			}
		}

		return versions.at(low);
	}

	/**
	 * Tells whether any release of a package is clean, checking the highest
	 * version of each caret group from the newest down.
	 */
	function hasCleanRelease(name) {
		if (!cleanReleaseCache.has(name)) {
			cleanReleaseCache.set(
				name,
				(async () => {
					const highestByGroup = new Map();

					for (const version of await registry.getVersions(name)) {
						const group = getCaretGroup(version);

						if (group && !isPrerelease(version)) {
							highestByGroup.set(
								formatCaretGroup(group),
								version
							);
						}
					}

					const highest = [...highestByGroup.values()].reverse();

					for (const version of highest) {
						if (await registry.isClean(name, version)) {
							return true;
						}
					}

					return false;
				})()
			);
		}

		return cleanReleaseCache.get(name);
	}

	return {findBump, getChainFix};
}
