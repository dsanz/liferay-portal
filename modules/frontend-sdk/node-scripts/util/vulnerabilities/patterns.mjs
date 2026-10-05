/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

/**
 * Tells whether a relative path matches a Yarn workspaces glob such as
 * `client-extensions/foo`. A `*` matches within one path segment and a `**`
 * segment matches any number of segments.
 *
 * @param {string} relativePath path relative to the workspace root
 * @param {string} glob one entry of a `workspaces` list
 * @return {boolean} whether the path matches the glob
 */
export function matchesWorkspaceGlob(relativePath, glob) {
	const globSegments = trimSlashes(glob).split('/');
	const pathSegments = trimSlashes(relativePath).split('/');

	return matchSegments(pathSegments, 0, globSegments, 0);
}

/**
 * Splits a resolution key into package name segments, keeping scoped package
 * names together since they contain a slash themselves.
 *
 * @param {string} key a resolution key, made of package names, `*` and `**`
 * @return {string[]} its package name segments
 */
export function splitPackagePath(key) {
	const parts = key.split('/');

	const segments = [];

	for (let i = 0; i < parts.length; i++) {
		if (parts[i].startsWith('@') && i + 1 < parts.length) {
			segments.push(`${parts[i]}/${parts[i + 1]}`);

			i++;
		}
		else {
			segments.push(parts[i]);
		}
	}

	return segments;
}

/**
 * Converts a glob segment (where `*` matches anything) to a regular expression.
 *
 * @param {string} segment a glob segment
 * @return {RegExp} an anchored regular expression for it
 */
export function toSegmentRegExp(segment) {
	const source = segment
		.split('*')
		.map((part) => part.replace(/[.+?^${}()|[\]\\]/g, '\\$&'))
		.join('.*');

	return new RegExp(`^${source}$`);
}

function matchSegments(pathSegments, pathIndex, globSegments, globIndex) {
	if (globIndex === globSegments.length) {
		return pathIndex === pathSegments.length;
	}

	if (globSegments[globIndex] === '**') {
		for (let i = pathIndex; i <= pathSegments.length; i++) {
			if (matchSegments(pathSegments, i, globSegments, globIndex + 1)) {
				return true;
			}
		}

		return false;
	}

	if (pathIndex === pathSegments.length) {
		return false;
	}

	if (
		!toSegmentRegExp(globSegments[globIndex]).test(pathSegments[pathIndex])
	) {
		return false;
	}

	return matchSegments(
		pathSegments,
		pathIndex + 1,
		globSegments,
		globIndex + 1
	);
}

function trimSlashes(value) {
	return value.replace(/^\.?\/+/, '').replace(/\/+$/, '');
}
