/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

const NODE_MODULES_SEGMENT = 'node_modules/';

/**
 * Parses an npm lockfile (`lockfileVersion` 1, 2 or 3) into the same model as
 * `parseYarnLock` (`bySpec` and `entries`), plus what npm's nested
 * `node_modules` layout needs:
 *
 * - every entry carries `children`, the entry each dependency name resolves to
 *   from its install location, following Node's lookup through parent folders
 * - `getRootEntry(name)` returns what a root dependency resolves to
 * - `rootDeclarations` holds the root dependencies the lockfile was written
 *   for (`packages[""]`, lockfile version 2 and 3 only), which `npm ci`
 *   compares with `package.json`
 *
 * An entry's `name` is the name it is installed under, which differs from its
 * `realName` for `npm:` aliases.
 */
export default function parsePackageLock(content) {
	const json = JSON.parse(content);

	const tree = json.packages
		? readPackages(json.packages)
		: readDependencies(json.dependencies || {});

	for (const entry of tree.values()) {
		for (const name of Object.keys(entry.dependencies)) {
			const child = resolveLocation(tree, entry.location, name);

			if (child) {
				entry.children[name] = child;
			}
		}
	}

	const bySpec = new Map();

	for (const entry of tree.values()) {
		for (const [name, range] of Object.entries(entry.dependencies)) {
			const child = entry.children[name];

			if (child && !bySpec.has(`${name}@${range}`)) {
				bySpec.set(`${name}@${range}`, child);
			}
		}
	}

	return {
		bySpec,
		entries: [...tree.values()],
		getRootEntry: (name) => resolveLocation(tree, '', name),
		rootDeclarations: json.packages?.[''] || null,
	};
}

function createEntry({dependencies, location, name, realName, version}) {
	return {
		children: {},
		dependencies,
		id: `${name}@${version}`,
		location,
		name,
		realName,
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

		const name = getLocationName(location);

		tree.set(
			location,
			createEntry({
				dependencies: {
					...(data.dependencies || {}),
					...(data.optionalDependencies || {}),
					...(data.peerDependencies || {}),
				},
				location,
				name,
				realName: data.name || name,
				version: data.version,
			})
		);
	}

	return tree;
}

/**
 * Reads a `lockfileVersion` 1 nested `dependencies` tree, turning it into the
 * install locations a version 2 lockfile would list. Aliases are recorded as
 * a `npm:<name>@<version>` version.
 */
function readDependencies(dependencies, parentLocation = '', tree = new Map()) {
	for (const [name, data] of Object.entries(dependencies)) {
		const location = parentLocation
			? `${parentLocation}/${NODE_MODULES_SEGMENT}${name}`
			: `${NODE_MODULES_SEGMENT}${name}`;

		let realName = name;
		let version = data.version;

		if (version?.startsWith('npm:')) {
			const alias = version.slice(4);

			const index = alias.lastIndexOf('@');

			realName = alias.slice(0, index);
			version = alias.slice(index + 1);
		}

		tree.set(
			location,
			createEntry({
				dependencies: {...(data.requires || {})},
				location,
				name,
				realName,
				version,
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
