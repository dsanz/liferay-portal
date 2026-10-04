/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

/**
 * Minimal handling of exact semver versions. Range matching is never done here:
 * the registry primitives delegate it to `npm view`, which implements the full
 * semver grammar.
 */

const VERSION_REGEXP = /^v?(\d+)\.(\d+)\.(\d+)(?:-([0-9A-Za-z.-]+))?(?:\+.*)?$/;

/**
 * Returns the caret group of a version, that is, the part caret ranges keep
 * fixed: the major, or the minor for `0.y.z`, or the patch for `0.0.z`.
 */
export function getCaretGroup(version) {
	const parsed = parseVersion(version);

	if (!parsed) {
		return null;
	}

	if (parsed.major > 0) {
		return [parsed.major];
	}

	if (parsed.minor > 0) {
		return [0, parsed.minor];
	}

	return [0, 0, parsed.patch];
}

export function compareCaretGroups(left, right) {
	for (let i = 0; i < Math.max(left.length, right.length); i++) {
		const difference = (left[i] ?? -1) - (right[i] ?? -1);

		if (difference) {
			return difference;
		}
	}

	return 0;
}

export function compareVersions(left, right) {
	const parsedLeft = parseVersion(left);
	const parsedRight = parseVersion(right);

	if (!parsedLeft || !parsedRight) {
		return String(left).localeCompare(String(right));
	}

	for (const field of ['major', 'minor', 'patch']) {
		const difference = parsedLeft[field] - parsedRight[field];

		if (difference) {
			return difference;
		}
	}

	if (parsedLeft.prerelease === parsedRight.prerelease) {
		return 0;
	}

	if (!parsedLeft.prerelease) {
		return 1;
	}

	if (!parsedRight.prerelease) {
		return -1;
	}

	return parsedLeft.prerelease.localeCompare(parsedRight.prerelease, 'en', {
		numeric: true,
	});
}

export function formatCaretGroup(group) {
	return group.join('.');
}

export function isPrerelease(version) {
	return !!parseVersion(version)?.prerelease;
}

export function parseVersion(version) {
	const match = VERSION_REGEXP.exec(String(version));

	if (!match) {
		return null;
	}

	return {
		major: Number(match[1]),
		minor: Number(match[2]),
		patch: Number(match[3]),
		prerelease: match[4] || '',
	};
}
