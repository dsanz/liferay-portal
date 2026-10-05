/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

/**
 * An entry of a lockfile, yarn or npm.
 *
 * @typedef {object} LockEntry
 * @property {string} id `name@version`.
 * @property {string} name Name the package is installed under, the alias for
 * `npm:` aliases.
 * @property {string} realName Registry package name.
 * @property {string | null} version Locked version.
 * @property {Object<string, string>} dependencies Name to range, merging
 * `dependencies` and `optionalDependencies` (and, for npm,
 * `peerDependencies`).
 * @property {string[]} specs The `name@range` keys of a yarn entry. Empty for
 * npm.
 * @property {Object<string, LockEntry>} [children] npm only: the entry each
 * dependency resolves to from this entry's install location.
 * @property {string} [location] npm only: the install location.
 */

/**
 * A parsed lockfile. `getRootEntry` and `rootDeclarations` only exist for npm.
 *
 * @typedef {object} Lock
 * @property {Map<string, LockEntry>} bySpec Entry for each `name@range`.
 * @property {LockEntry[]} entries Every entry, once.
 * @property {(name: string) => LockEntry | null} [getRootEntry]
 * @property {object | null} [rootDeclarations]
 */

const DEPENDENCY_FIELDS = new Set(['dependencies', 'optionalDependencies']);

/**
 * Parses a Yarn v1 lockfile.
 *
 * Returns `{bySpec, entries}`, where `bySpec` maps every `name@range` key of the
 * lockfile to its entry and `entries` lists each entry once. An entry is
 * `{dependencies, id, name, realName, specs, version}`, where `dependencies` merges the
 * `dependencies` and `optionalDependencies` sections (name to range).
 *
 * @param {string} content the lockfile text
 * @return {Lock}
 */
export default function parseYarnLock(content) {
	const bySpec = new Map();
	const entries = [];

	let currentEntry = null;
	let currentField = null;

	for (const line of content.split('\n')) {
		if (!line.trim() || line.startsWith('#')) {
			continue;
		}

		if (!line.startsWith(' ')) {
			const specs = line.replace(/:\s*$/, '').split(/,\s*/).map(unquote);

			currentEntry = {
				dependencies: {},
				id: null,
				name: getSpecName(specs[0]),
				realName: getAliasedName(specs[0]),
				specs,
				version: null,
			};
			currentField = null;

			entries.push(currentEntry);

			for (const spec of specs) {
				bySpec.set(spec, currentEntry);
			}

			continue;
		}

		if (!currentEntry) {
			continue;
		}

		if (line.startsWith('    ')) {
			if (DEPENDENCY_FIELDS.has(currentField)) {
				const [name, range] = splitKeyValue(line.trim());

				currentEntry.dependencies[name] = range;
			}

			continue;
		}

		const trimmedLine = line.trim();

		if (trimmedLine.endsWith(':')) {
			currentField = trimmedLine.slice(0, -1);

			continue;
		}

		currentField = null;

		const [key, value] = splitKeyValue(trimmedLine);

		if (key === 'version') {
			currentEntry.version = value;
			currentEntry.id = `${currentEntry.name}@${value}`;
		}
	}

	return {bySpec, entries};
}

/**
 * Returns the package name of a `name@range` spec. Scoped names start with an
 * `@`, so the separator is the first `@` after the first character.
 */

/**
 * Returns the registry package an `npm:` alias spec points to
 * (`react-dom-16@npm:react-dom@16.12.0` points to `react-dom`), or the spec's
 * own name when it is not an alias.
 *
 * @param {string} spec a `name@range` key
 * @return {string}
 */
export function getAliasedName(spec) {
	const range = spec.slice(getSpecName(spec).length + 1);

	if (!range.startsWith('npm:')) {
		return getSpecName(spec);
	}

	return getSpecName(range.slice(4));
}

/**
 * @param {string} spec a `name@range` key
 * @return {string} the package name
 */
export function getSpecName(spec) {
	const index = spec.indexOf('@', 1);

	return index === -1 ? spec : spec.slice(0, index);
}

function splitKeyValue(text) {
	let key;
	let rest;

	if (text.startsWith('"')) {
		const end = text.indexOf('"', 1);

		key = text.slice(1, end);
		rest = text.slice(end + 1);
	}
	else {
		const index = text.search(/\s/);

		key = index === -1 ? text : text.slice(0, index);
		rest = index === -1 ? '' : text.slice(index);
	}

	return [key, unquote(rest.trim())];
}

function unquote(text) {
	const trimmedText = text.trim();

	if (trimmedText.startsWith('"') && trimmedText.endsWith('"')) {
		return trimmedText.slice(1, -1);
	}

	return trimmedText;
}
