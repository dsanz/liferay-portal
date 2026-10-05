/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

const MAX_CHAINS_PER_NODE = 50;

const NODE_MODULES_SEGMENT = 'node_modules/';

const ROOT_DEPENDENCY_FIELDS = [
	'dependencies',
	'devDependencies',
	'optionalDependencies',
	'peerDependencies',
];

/**
 * Parses an npm lockfile (`lockfileVersion` 1, 2 or 3) into the same model as
 * `parseYarnLock` (`bySpec` and `entries`), plus what npm's nested
 * `node_modules` layout needs:
 *
 * - every entry carries `children`, the entry each dependency name resolves to
 *   from its location, following Node's lookup through parent folders
 * - `root.children` holds what the root `package.json` resolves to
 * - `getChains(location)` returns the dependency chains (package names from a
 *   direct dependency down) that reach the entry installed at a location
 */
export default function parsePackageLock(content, rootJSON) {
	const json = JSON.parse(content);

	const tree = json.packages
		? readPackages(json.packages)
		: readDependencies(json.dependencies || {});

	const root = {children: {}, dependencies: {}, location: ''};

	for (const field of ROOT_DEPENDENCY_FIELDS) {
		Object.assign(root.dependencies, rootJSON[field] || {});
	}

	const parents = new Map([...tree.values()].map((entry) => [entry, []]));

	for (const entry of [root, ...tree.values()]) {
		for (const name of Object.keys(entry.dependencies)) {
			const child = resolveLocation(tree, entry.location, name);

			if (child) {
				entry.children[name] = child;

				parents.get(child).push(entry);
			}
		}
	}

	const bySpec = new Map();

	for (const entry of [root, ...tree.values()]) {
		for (const [name, range] of Object.entries(entry.dependencies)) {
			const child = entry.children[name];

			if (child && !bySpec.has(`${name}@${range}`)) {
				bySpec.set(`${name}@${range}`, child);
			}
		}
	}

	function getChains(location) {
		const entry = tree.get(location);

		if (!entry) {
			return [];
		}

		const chains = [];

		function walk(current, names, visited) {
			if (chains.length >= MAX_CHAINS_PER_NODE) {
				return;
			}

			for (const parent of parents.get(current) || []) {
				if (parent === root) {
					chains.push(names);
				}
				else if (!visited.has(parent)) {
					walk(
						parent,
						[parent.name, ...names],
						new Set([...visited, parent])
					);
				}
			}
		}

		walk(entry, [entry.name], new Set([entry]));

		return chains;
	}

	return {
		bySpec,
		entries: [...tree.values()],
		getChains,
		root,
		tree,
	};
}

function createEntry(location, name, version, dependencies) {
	return {
		children: {},
		dependencies,
		id: `${name}@${version}`,
		location,
		name,
		specs: [],
		version,
	};
}

function getLocationName(location) {
	return location.slice(
		location.lastIndexOf(NODE_MODULES_SEGMENT) + NODE_MODULES_SEGMENT.length
	);
}

/**
 * Reads a `lockfileVersion` 2 or 3 `packages` map, keyed by install location.
 */
function readPackages(packages) {
	const tree = new Map();

	for (const [location, data] of Object.entries(packages)) {
		if (
			!location ||
			!location.includes(NODE_MODULES_SEGMENT) ||
			data.link
		) {
			continue;
		}

		tree.set(
			location,
			createEntry(
				location,
				data.name || getLocationName(location),
				data.version,
				{
					...(data.dependencies || {}),
					...(data.optionalDependencies || {}),
					...(data.peerDependencies || {}),
				}
			)
		);
	}

	return tree;
}

/**
 * Reads a `lockfileVersion` 1 nested `dependencies` tree, turning it into the
 * install locations a version 2 lockfile would list.
 */
function readDependencies(dependencies, parentLocation = '', tree = new Map()) {
	for (const [name, data] of Object.entries(dependencies)) {
		const location = parentLocation
			? `${parentLocation}/${NODE_MODULES_SEGMENT}${name}`
			: `${NODE_MODULES_SEGMENT}${name}`;

		tree.set(
			location,
			createEntry(location, name, data.version, {
				...(data.requires || {}),
			})
		);

		if (data.dependencies) {
			readDependencies(data.dependencies, location, tree);
		}
	}

	return tree;
}

/**
 * Finds the entry a package name resolves to from a location, the way Node
 * does: the location's own `node_modules` first, then each parent's.
 */
function resolveLocation(tree, fromLocation, name) {
	const candidates = [];

	let base = fromLocation;

	while (base) {
		candidates.push(`${base}/${NODE_MODULES_SEGMENT}${name}`);

		const index = base.lastIndexOf(`/${NODE_MODULES_SEGMENT}`);

		base = index === -1 ? '' : base.slice(0, index);
	}

	candidates.push(`${NODE_MODULES_SEGMENT}${name}`);

	const location = candidates.find((candidate) => tree.has(candidate));

	return location ? tree.get(location) : null;
}
