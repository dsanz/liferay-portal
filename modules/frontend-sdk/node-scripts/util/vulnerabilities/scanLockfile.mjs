/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {PACKAGE_MANAGER_NPM} from './discoverProjects.mjs';

const GHSA_REGEXP = /GHSA-[0-9a-z]{4}-[0-9a-z]{4}-[0-9a-z]{4}/;

const LINK_RANGE_REGEXP = /^(?:link:|portal:|workspace:)/;

const MAX_CHAINS_PER_ENTRY = 50;

/**
 * The dependency fields each package manager installs from. yarn 1 never
 * installs `peerDependencies`.
 */
const INSTALLED_FIELDS = {
	npm: [
		'dependencies',
		'devDependencies',
		'optionalDependencies',
		'peerDependencies',
	],
	yarn: ['dependencies', 'devDependencies', 'optionalDependencies'],
};

/**
 * Scans a project's lockfile as the dependency tree its `package.json` files
 * declare, which is what a frozen-lockfile install produces:
 *
 * 1. Every dependency declared by every package of the project (the root and
 *    each workspace member, every installed field) is looked up in the
 *    lockfile. A declaration with no entry means the lockfile is out of sync.
 * 1. Every entry reachable from those declarations is checked against the
 *    advisory database, and the chains that reach it are rebuilt from the
 *    lockfile. Unreachable entries are leftovers (drift).
 *
 * Returns the advisories in the shape `collectFindings` reads, plus `sync`
 * (`missing` declarations and `incomplete` entries) and `orphans`.
 *
 * @param {object} options
 * @param {import('./parseYarnLock.mjs').Lock} options.lock
 * @param {import('./discoverProjects.mjs').Project} options.project
 * @param {import('./Registry.mjs').default} options.registry
 * @return {Promise<{advisories: {advisory: object}[], orphans: string[], sync: {incomplete: object[], missing: object[]}}>} `orphans` as `name@version`; `sync` as in `LockfileOutOfSyncFinding`
 */
export default async function scanLockfile({lock, project, registry}) {
	const graph =
		project.packageManager === PACKAGE_MANAGER_NPM
			? createNpmGraph(lock)
			: createYarnGraph(lock);

	const fields = INSTALLED_FIELDS[project.packageManager];

	const declarers = new Map();
	const missing = [];

	for (const pkg of project.packages) {
		for (const field of fields) {
			for (const [name, range] of Object.entries(pkg.json[field] || {})) {
				if (LINK_RANGE_REGEXP.test(range)) {
					continue;
				}

				const entry = graph.getDeclaredEntry(pkg, name, range);

				if (entry) {
					if (!declarers.has(entry)) {
						declarers.set(entry, []);
					}

					declarers.get(entry).push(pkg);
				}
				else if (
					!project.membersByName.has(name) &&
					field !== 'peerDependencies'
				) {
					missing.push({field, file: pkg.file, name, range});
				}
			}
		}
	}

	missing.push(...graph.getRootMismatches(project.root, fields));

	const incomplete = [];
	const parents = new Map();
	const reachable = new Set(declarers.keys());

	const queue = [...reachable];

	while (queue.length) {
		const entry = queue.shift();

		for (const {child, name, range} of graph.getChildren(entry)) {
			if (!child) {
				if (graph.checksCompleteness) {
					incomplete.push({entry: entry.id, name, range});
				}

				continue;
			}

			if (!parents.has(child)) {
				parents.set(child, new Set());
			}

			parents.get(child).add(entry);

			if (!reachable.has(child)) {
				reachable.add(child);
				queue.push(child);
			}
		}
	}

	const advisories = new Map();

	await Promise.all(
		[...reachable].map(async (entry) => {
			if (!entry.version) {
				return;
			}

			const found = await registry.getAdvisories(
				entry.realName || entry.name,
				entry.version
			);

			if (!found.length) {
				return;
			}

			const paths = getChains({declarers, entry, parents, project}).map(
				(names) => names.join('>')
			);

			for (const advisory of found) {
				if (!advisories.has(advisory.url)) {
					advisories.set(advisory.url, {
						cves: [],
						findings: [],
						github_advisory_id:
							GHSA_REGEXP.exec(advisory.url)?.[0] ||
							String(advisory.id),
						module_name: entry.name,
						patched_versions: null,
						severity: advisory.severity,
						title: advisory.title,
						url: advisory.url,
						vulnerable_versions: advisory.vulnerable_versions,
					});
				}

				advisories
					.get(advisory.url)
					.findings.push({paths, version: entry.version});
			}
		})
	);

	return {
		advisories: [...advisories.values()].map((advisory) => ({advisory})),
		orphans: lock.entries
			.filter((entry) => entry.version && !reachable.has(entry))
			.map((entry) => entry.id)
			.sort(),
		sync: {incomplete, missing},
	};
}

function createNpmGraph(lock) {
	return {
		checksCompleteness: false,
		getChildren: (entry) =>
			Object.entries(entry.dependencies).map(([name, range]) => ({
				child: entry.children[name] || null,
				name,
				range,
			})),
		getDeclaredEntry: (pkg, name) => lock.getRootEntry(name),

		/**
		 * `npm ci` refuses to install when the root dependencies recorded in
		 * a version 2 or 3 lockfile differ from `package.json`.
		 */
		getRootMismatches(root, fields) {
			if (!lock.rootDeclarations) {
				return [];
			}

			const mismatches = [];

			for (const field of fields) {
				const declared = root.json[field] || {};
				const recorded = lock.rootDeclarations[field] || {};

				for (const name of new Set([
					...Object.keys(declared),
					...Object.keys(recorded),
				])) {
					if (declared[name] !== recorded[name]) {
						mismatches.push({
							field,
							file: root.file,
							lockRange: recorded[name] ?? null,
							name,
							range: declared[name] ?? null,
						});
					}
				}
			}

			return mismatches;
		},
	};
}

function createYarnGraph(lock) {
	return {
		checksCompleteness: true,
		getChildren: (entry) =>
			Object.entries(entry.dependencies).map(([name, range]) => ({
				child: lock.bySpec.get(`${name}@${range}`) || null,
				name,
				range,
			})),
		getDeclaredEntry: (pkg, name, range) =>
			lock.bySpec.get(`${name}@${range}`) || null,
		getRootMismatches: () => [],
	};
}

/**
 * Rebuilds the chains that reach an entry, walking up the reverse graph to
 * the packages that declare each step. A chain starts with the declaring
 * workspace member's name when a member declares it, which is how
 * `collectFindings` tells which package declares the direct dependency.
 */
function getChains({declarers, entry, parents, project}) {
	const chains = [];

	function walk(current, names, visited) {
		if (chains.length >= MAX_CHAINS_PER_ENTRY) {
			return;
		}

		for (const pkg of declarers.get(current) || []) {
			chains.push(
				pkg !== project.root && pkg.name ? [pkg.name, ...names] : names
			);
		}

		for (const parent of parents.get(current) || []) {
			if (!visited.has(parent)) {
				walk(
					parent,
					[parent.name, ...names],
					new Set([...visited, parent])
				);
			}
		}
	}

	walk(entry, [entry.name], new Set([entry]));

	return [...new Set(chains.map((names) => names.join('\0')))].map((key) =>
		key.split('\0')
	);
}
