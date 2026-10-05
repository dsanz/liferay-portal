/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {getRegistryTarget} from './Registry.mjs';
import {DECLARATION_FIELDS} from './collectFindings.mjs';
import {PROJECT_CLASS_MODULES} from './discoverProjects.mjs';
import {splitPackagePath, toSegmentRegExp} from './patterns.mjs';
import {parseVersion} from './versions.mjs';

export const RESOLUTION_FLAG_FORCED_VERSION_VULNERABLE =
	'forced-version-vulnerable';
export const RESOLUTION_FLAG_NON_SELECTIVE = 'non-selective';

export const RESOLUTION_STATUS_NO_SECURITY_EFFECT = 'no-security-effect';
export const RESOLUTION_STATUS_NOT_APPLIED = 'not-applied';
export const RESOLUTION_STATUS_OBSOLETE = 'obsolete';
export const RESOLUTION_STATUS_STILL_NEEDED = 'still-needed';
export const RESOLUTION_STATUS_UNKNOWN = 'unknown';

/**
 * Checks every resolution of a project's root `package.json`: which lockfile
 * edges it applies to, what yarn would pick for each edge without it, and
 * whether that would bring an advisory back.
 *
 * @param {object} options
 * @param {Object<string, string> | null} options.allowedReasons reasons recorded in `ALLOWED_ROOT_PACKAGE_JSON_RESOLUTIONS.mjs`, for `modules`
 * @param {object} options.attribution see `collectFindings`
 * @param {ReturnType<import('./assignFixTypes.mjs').createFixer>} options.fixer
 * @param {import('./parseYarnLock.mjs').Lock} options.lock
 * @param {import('./discoverProjects.mjs').Project} options.project
 * @param {import('./Registry.mjs').default} options.registry
 * @return {Promise<import('./reportTypes.mjs').Resolution[]>}
 */
export default async function checkResolutions({
	allowedReasons,
	attribution,
	fixer,
	lock,
	project,
	registry,
}) {
	const resolutions = Object.entries(project.root.json.resolutions || {});

	if (!resolutions.length) {
		return [];
	}

	const graph = createGraph(lock, project);

	const approvers =
		project.class === PROJECT_CLASS_MODULES
			? attribution.infraOwners
			: attribution.codeOwners.getOwners(project.path);

	return Promise.all(
		resolutions.map(([key, value]) =>
			checkResolution({
				allowedReasons,
				approvers,
				fixer,
				graph,
				key,
				lock,
				project,
				registry,
				value,
			}).catch((error) => ({
				approvers,
				edges: [],
				error: error.message,
				flags: [],
				key,
				project: project.path,
				reason: allowedReasons?.[key] ?? null,
				releasedBy: null,
				status: RESOLUTION_STATUS_UNKNOWN,
				value,
			}))
		)
	);
}

async function checkResolution({
	allowedReasons,
	approvers,
	fixer,
	graph,
	key,
	lock,
	project,
	registry,
	value,
}) {
	const resolution = {
		approvers,
		edges: [],
		flags: [],
		key,
		project: project.path,
		reason: allowedReasons?.[key] ?? null,
		releasedBy: null,
		status: null,
		value,
	};

	const segments = splitPackagePath(key);

	const parentSegments = segments.slice(0, -1);
	const target = segments.at(-1);

	if (
		!parentSegments.some((segment) => segment !== '*' && segment !== '**')
	) {
		resolution.flags.push(RESOLUTION_FLAG_NON_SELECTIVE);
	}

	const edges = findEdges({graph, lock, parentSegments, project, target});

	if (!edges.length) {
		resolution.status = RESOLUTION_STATUS_OBSOLETE;

		return resolution;
	}

	if (!(await isForcedVersionClean(registry, target, value))) {
		resolution.flags.push(RESOLUTION_FLAG_FORCED_VERSION_VULNERABLE);
	}

	resolution.edges = await Promise.all(
		edges.map(async (edge) => {
			const unforced = await registry.resolve(target, edge.parentRange);

			const advisories = unforced
				? await registry.getAdvisories(unforced.name, unforced.version)
				: [];

			return {
				_entry: edge.entry,
				advisories: advisories.map(getAdvisoryId),
				clean: unforced ? !advisories.length : null,
				lockVersion: edge.lockVersion,
				parent: edge.parent,
				parentRange: edge.parentRange,
				unforced: unforced?.version ?? null,
			};
		})
	);

	const dirtyEdges = resolution.edges.filter((edge) => edge.clean === false);

	if (resolution.edges.some((edge) => edge.lockVersion !== value)) {
		resolution.status = RESOLUTION_STATUS_NOT_APPLIED;
	}
	else if (dirtyEdges.length) {
		resolution.status = RESOLUTION_STATUS_STILL_NEEDED;
	}
	else {
		resolution.status = RESOLUTION_STATUS_NO_SECURITY_EFFECT;
	}

	if (dirtyEdges.length) {
		resolution.releasedBy = await getReleasedBy({
			edge: dirtyEdges[0],
			fixer,
			graph,
			target,
		});
	}

	for (const edge of resolution.edges) {
		delete edge._entry;
	}

	return resolution;
}

/**
 * Builds the reverse dependency graph of a lockfile and the set of entries
 * the project's packages depend on directly.
 */
function createGraph(lock, project) {
	const parents = new Map(lock.entries.map((entry) => [entry, new Set()]));

	for (const entry of lock.entries) {
		for (const [name, range] of Object.entries(entry.dependencies)) {
			const child = lock.bySpec.get(`${name}@${range}`);

			if (child) {
				parents.get(child).add(entry);
			}
		}
	}

	const declaredRanges = new Map();
	const topEntries = new Set();

	for (const pkg of project.packages) {
		for (const field of DECLARATION_FIELDS) {
			for (const [name, range] of Object.entries(pkg.json[field] || {})) {
				const entry = lock.bySpec.get(`${name}@${range}`);

				if (entry) {
					declaredRanges.set(entry, range);
					topEntries.add(entry);
				}
			}
		}
	}

	return {declaredRanges, parents, topEntries};
}

/**
 * Collects the edges parent -> target a resolution key applies to, following
 * Yarn's selective resolution paths: named segments are consecutive packages
 * starting at a direct dependency, `*` matches one package and `**` any
 * number of them.
 */
function findEdges({graph, lock, parentSegments, project, target}) {
	const memo = new Map();
	const visiting = new Set();

	function canEnd(entry, index) {
		const key = `${entry.id}|${index}`;

		if (memo.has(key)) {
			return memo.get(key);
		}

		if (visiting.has(key)) {
			return false;
		}

		visiting.add(key);

		const segment = parentSegments[index];

		let result = false;

		if (segment === '**') {
			result =
				index === 0 ||
				canEnd(entry, index - 1) ||
				[...graph.parents.get(entry)].some(
					(parent) =>
						canEnd(parent, index) || canEnd(parent, index - 1)
				);
		}
		else if (matchesSegment(entry.name, segment)) {
			result =
				index === 0
					? graph.topEntries.has(entry)
					: [...graph.parents.get(entry)].some((parent) =>
							canEnd(parent, index - 1)
						);
		}

		visiting.delete(key);
		memo.set(key, result);

		return result;
	}

	const edges = [];

	for (const entry of lock.entries) {
		const range = entry.dependencies[target];

		if (range === undefined) {
			continue;
		}

		if (
			parentSegments.length &&
			!canEnd(entry, parentSegments.length - 1)
		) {
			continue;
		}

		edges.push({
			entry,
			lockVersion: lock.bySpec.get(`${target}@${range}`)?.version ?? null,
			parent: entry.id,
			parentRange: range,
		});
	}

	if (parentSegments.every((segment) => segment === '**')) {
		for (const pkg of project.packages) {
			for (const field of DECLARATION_FIELDS) {
				const range = pkg.json[field]?.[target];

				if (range === undefined) {
					continue;
				}

				edges.push({
					entry: null,
					lockVersion:
						lock.bySpec.get(`${target}@${range}`)?.version ?? null,
					parent: pkg.file,
					parentRange: range,
				});
			}
		}
	}

	return edges;
}

function getAdvisoryId(advisory) {
	return (
		/GHSA-[0-9a-z]{4}-[0-9a-z]{4}-[0-9a-z]{4}/.exec(
			advisory.url || ''
		)?.[0] || String(advisory.id)
	);
}

/**
 * Works out which version of a direct dependency would make a resolution
 * unnecessary, walking up from the edge's parent to a direct dependency.
 */
async function getReleasedBy({edge, fixer, graph, target}) {
	if (!edge._entry) {
		const bump = await fixer.findBump({
			name: target,
			range: edge.parentRange,
			rest: [],
			version: edge.lockVersion,
		});

		return {directDependency: target, version: bump?.to ?? null};
	}

	const chain = findPathToTop(edge._entry, graph);

	if (!chain) {
		return {directDependency: null, version: null};
	}

	const [top, ...rest] = chain;

	const bump = await fixer.findBump({
		name: top.name,
		range: graph.declaredRanges.get(top),
		rest: [...rest.map((entry) => entry.name), target],
		version: top.version,
	});

	return {directDependency: top.name, version: bump?.to ?? null};
}

/**
 * Finds the shortest chain of lockfile entries from a direct dependency down
 * to an entry.
 */
function findPathToTop(entry, graph) {
	const previous = new Map([[entry, null]]);
	const queue = [entry];

	while (queue.length) {
		const current = queue.shift();

		if (graph.topEntries.has(current)) {
			const chain = [];

			for (let link = current; link; link = previous.get(link)) {
				chain.push(link);
			}

			return chain;
		}

		for (const parent of graph.parents.get(current)) {
			if (!previous.has(parent)) {
				previous.set(parent, current);
				queue.push(parent);
			}
		}
	}

	return null;
}

async function isForcedVersionClean(registry, target, value) {
	if (parseVersion(value)) {
		return registry.isClean(target, value);
	}

	const {name, range} = getRegistryTarget(target, value);

	const resolved = await registry.resolve(name, range);

	return resolved ? registry.isClean(resolved.name, resolved.version) : true;
}

function matchesSegment(name, segment) {
	return (
		segment === '*' ||
		segment === name ||
		toSegmentRegExp(segment).test(name)
	);
}
