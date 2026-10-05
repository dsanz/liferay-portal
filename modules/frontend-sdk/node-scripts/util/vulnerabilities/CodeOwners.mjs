/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import fs from 'fs';
import path from 'path';

export const UNOWNED = 'unowned';

/**
 * Reads `.github/CODEOWNERS` and answers who owns a repository path, following
 * GitHub's rule: the last pattern that matches wins.
 */
export default class CodeOwners {

	/**
	 * @param {string} portalDir absolute path of the repository root
	 */
	constructor(portalDir) {
		this._cache = new Map();
		this._rules = [];

		const file = path.join(portalDir, '.github', 'CODEOWNERS');

		if (!fs.existsSync(file)) {
			return;
		}

		for (const line of fs.readFileSync(file, 'utf-8').split('\n')) {
			const trimmedLine = line.trim();

			if (!trimmedLine || trimmedLine.startsWith('#')) {
				continue;
			}

			const [pattern, ...owners] = trimmedLine.split(/\s+/);

			this._rules.push({owners, regExp: toRegExp(pattern)});
		}
	}

	/**
	 * Returns the owners of a path relative to the repository root, or
	 * `[UNOWNED]` when no pattern matches.
	 *
	 * @param {string} relativePath
	 * @return {string[]} the owner handles
	 */
	getOwners(relativePath) {
		const normalizedPath = relativePath.replace(/^\/+|\/+$/g, '');

		if (this._cache.has(normalizedPath)) {
			return this._cache.get(normalizedPath);
		}

		let owners = [UNOWNED];

		for (const rule of this._rules) {
			if (rule.regExp.test(normalizedPath)) {
				owners = rule.owners.length ? rule.owners : [UNOWNED];
			}
		}

		this._cache.set(normalizedPath, owners);

		return owners;
	}
}

function toRegExp(pattern) {
	let body = pattern.replace(/\/+$/, '');

	const anchored = body.startsWith('/') || body.includes('/');

	body = body.replace(/^\/+/, '');

	let source = '';

	for (let i = 0; i < body.length; i++) {
		const character = body[i];

		if (character === '*' && body[i + 1] === '*') {
			source += '.*';

			i++;
		}
		else if (character === '*') {
			source += '[^/]*';
		}
		else if (character === '?') {
			source += '[^/]';
		}
		else {
			source += character.replace(/[.+^${}()|[\]\\]/g, '\\$&');
		}
	}

	return new RegExp(`^${anchored ? '' : '(?:.*/)?'}${source}(?:/.*)?$`);
}
