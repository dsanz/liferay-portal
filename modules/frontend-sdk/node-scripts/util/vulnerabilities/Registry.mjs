/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import runCommand, {sleep} from './runCommand.mjs';
import {compareVersions} from './versions.mjs';

/**
 * An advisory as the bulk advisory endpoint returns it.
 *
 * @typedef {object} RegistryAdvisory
 * @property {number} id
 * @property {string} url The GitHub advisory page, ending with the GHSA ID.
 * @property {string} title
 * @property {import('./reportTypes.mjs').Severity} severity
 * @property {string} vulnerable_versions
 */

/**
 * @typedef {object} ResolvedVersion
 * @property {string} name Registry package name.
 * @property {string} version Highest version matching the range.
 * @property {Object<string, string>} dependencies Its `dependencies` and
 * `optionalDependencies`.
 */

const BULK_BATCH_SIZE = 100;

const BULK_RETRY_DELAYS = [1000, 2000, 4000];

const NON_REGISTRY_RANGE_REGEXP =
	/^(?:file:|link:|portal:|workspace:|git[+:]|github:|https?:|[^@\s]+\/[^@\s]+$)/;

const OSV_VULNS_URL = 'https://api.osv.dev/v1/vulns/';

export class UnresolvableRangeError extends Error {}

/**
 * Answers the two registry questions the report needs, caching every answer
 * for the run:
 *
 * - resolve: which version (and dependencies) a fresh yarn resolve picks for a
 *   range, delegated to `npm view` so the full semver grammar is honored
 * - advisories: which advisories affect an exact version, asked to the bulk
 *   advisory endpoint the npm CLI uses for `npm audit`
 * - CVE IDs: which CVE IDs an advisory has, asked to OSV.dev, because the bulk
 *   advisory endpoint only returns GitHub advisory IDs
 *
 * Answers are only kept in memory for the run: nothing is written to disk.
 */
export default class Registry {

	/**
	 * @param {object} options
	 * @param {(task: () => Promise<any>) => Promise<any>} options.limit limiter shared by every registry request
	 * @param {string} options.registryURL npm registry URL
	 */
	constructor({limit, registryURL}) {
		this._advisoriesCache = new Map();
		this._cvesCache = new Map();
		this._flushTimeout = null;
		this._limit = limit;
		this._matchingVersionsCache = new Map();
		this._pendingAdvisories = new Map();
		this._registryURL = registryURL.endsWith('/')
			? registryURL
			: `${registryURL}/`;
		this._resolveCache = new Map();
		this._versionsCache = new Map();
	}

	/**
	 * Returns the advisories affecting an exact version of a package.
	 *
	 * @param {string} name registry package name
	 * @param {string} version exact version
	 * @return {Promise<RegistryAdvisory[]>}
	 */
	getAdvisories(name, version) {
		const key = `${name}@${version}`;

		if (!this._advisoriesCache.has(key)) {
			const promise = new Promise((resolve, reject) => {
				let versions = this._pendingAdvisories.get(name);

				if (!versions) {
					versions = new Map();

					this._pendingAdvisories.set(name, versions);
				}

				versions.set(version, {reject, resolve});
			});

			this._advisoriesCache.set(key, promise);

			this._scheduleFlush();
		}

		return this._advisoriesCache.get(key);
	}

	/**
	 * Returns the CVE IDs of a GitHub advisory, empty when it has none.
	 *
	 * @param {string} ghsa GitHub advisory ID
	 * @return {Promise<string[]>}
	 */
	getCves(ghsa) {
		if (!this._cvesCache.has(ghsa)) {
			this._cvesCache.set(
				ghsa,
				this._limit(() => this._fetchCves(ghsa))
			);
		}

		return this._cvesCache.get(ghsa);
	}

	/**
	 * @param {string} name registry package name
	 * @param {string} version exact version
	 * @return {Promise<boolean>} whether no advisory affects the version
	 */
	async isClean(name, version) {
		const advisories = await this.getAdvisories(name, version);

		return !advisories.length;
	}

	/**
	 * Returns `{dependencies, name, version}` for the highest version matching
	 * a range, `null` when nothing matches. `npm:` aliases are followed, and
	 * `dependencies` merges `dependencies` and `optionalDependencies`.
	 *
	 * @param {string} name
	 * @param {string} range
	 * @return {Promise<ResolvedVersion | null>}
	 */
	resolve(name, range) {
		const target = getRegistryTarget(name, range);

		const key = `${target.name}@${target.range}`;

		if (!this._resolveCache.has(key)) {
			this._resolveCache.set(
				key,
				this._view(target.name, target.range, [
					'version',
					'dependencies',
					'optionalDependencies',
				]).then((json) => {
					if (json === null) {
						return null;
					}

					// When no matching version declares any of the requested
					// dependency fields, npm prints bare version strings.

					const candidates = (
						Array.isArray(json) ? json : [json]
					).map((candidate) =>
						typeof candidate === 'string'
							? {version: candidate}
							: candidate
					);

					const best = candidates
						.filter((candidate) => candidate?.version)
						.sort((left, right) =>
							compareVersions(left.version, right.version)
						)
						.at(-1);

					if (!best) {
						return null;
					}

					return {
						dependencies: {
							...(best.dependencies || {}),
							...(best.optionalDependencies || {}),
						},
						name: target.name,
						version: best.version,
					};
				})
			);
		}

		return this._resolveCache.get(key);
	}

	/**
	 * Returns the versions matching a range, oldest first. `npm:` aliases are
	 * followed.
	 *
	 * @param {string} name
	 * @param {string} range
	 * @return {Promise<string[]>}
	 */
	getMatchingVersions(name, range) {
		const target = getRegistryTarget(name, range);

		const key = `${target.name}@${target.range}`;

		if (!this._matchingVersionsCache.has(key)) {
			this._matchingVersionsCache.set(
				key,
				this._view(target.name, target.range, ['version']).then(
					(json) => {
						if (json === null) {
							return [];
						}

						const versions = Array.isArray(json) ? json : [json];

						return versions.sort(compareVersions);
					}
				)
			);
		}

		return this._matchingVersionsCache.get(key);
	}

	/**
	 * Returns every published version of a package, oldest first.
	 *
	 * @param {string} name registry package name
	 * @return {Promise<string[]>}
	 */
	getVersions(name) {
		if (!this._versionsCache.has(name)) {
			this._versionsCache.set(
				name,
				this._view(name, null, ['versions']).then((json) => {
					if (json === null) {
						return [];
					}

					const versions = Array.isArray(json) ? json : [json];

					return versions.sort(compareVersions);
				})
			);
		}

		return this._versionsCache.get(name);
	}

	async _fetchAdvisories(batch) {
		let lastError;

		for (let attempt = 0; attempt <= BULK_RETRY_DELAYS.length; attempt++) {
			if (attempt) {
				await sleep(BULK_RETRY_DELAYS[attempt - 1]);
			}

			try {
				const response = await fetch(
					`${this._registryURL}-/npm/v1/security/advisories/bulk`,
					{
						body: JSON.stringify(batch),
						headers: {'content-type': 'application/json'},
						method: 'POST',
					}
				);

				if (response.ok) {
					return await response.json();
				}

				lastError = new Error(
					`Advisory endpoint answered ${response.status} ${response.statusText}`
				);
			}
			catch (error) {
				lastError = error;
			}
		}

		throw lastError;
	}

	async _fetchCves(ghsa) {
		let lastError;

		for (let attempt = 0; attempt <= BULK_RETRY_DELAYS.length; attempt++) {
			if (attempt) {
				await sleep(BULK_RETRY_DELAYS[attempt - 1]);
			}

			try {
				const response = await fetch(
					`${OSV_VULNS_URL}${encodeURIComponent(ghsa)}`
				);

				if (response.status === 404) {
					return [];
				}

				if (response.ok) {
					const json = await response.json();

					return (json.aliases || [])
						.filter((alias) => alias.startsWith('CVE-'))
						.sort();
				}

				lastError = new Error(
					`OSV.dev answered ${response.status} ${response.statusText}`
				);
			}
			catch (error) {
				lastError = error;
			}
		}

		throw lastError;
	}

	async _flush() {
		this._flushTimeout = null;

		const tasks = [];

		while (this._pendingAdvisories.size) {
			const batch = {};
			const waiters = {};

			for (const [name, versions] of this._pendingAdvisories) {
				if (Object.keys(batch).length >= BULK_BATCH_SIZE) {
					break;
				}

				const [version, waiter] = versions.entries().next().value;

				versions.delete(version);

				if (!versions.size) {
					this._pendingAdvisories.delete(name);
				}

				batch[name] = [version];
				waiters[name] = waiter;
			}

			tasks.push(
				this._limit(() => this._fetchAdvisories(batch)).then(
					(json) => {
						for (const [name, waiter] of Object.entries(waiters)) {
							waiter.resolve(json[name] || []);
						}
					},
					(error) => {
						for (const waiter of Object.values(waiters)) {
							waiter.reject(error);
						}
					}
				)
			);
		}

		await Promise.all(tasks);
	}

	_scheduleFlush() {
		if (this._flushTimeout) {
			return;
		}

		this._flushTimeout = setTimeout(() => this._flush(), 20);
	}

	async _view(name, range, fields) {
		const spec = range ? `${name}@${range}` : name;

		const {code, stderr, stdout} = await this._limit(() =>
			runCommand('npm', ['view', spec, ...fields, '--json'])
		);

		let json = null;

		if (stdout.trim()) {
			try {
				json = JSON.parse(stdout);
			}
			catch {
				throw new Error(`Unable to parse 'npm view ${spec}' output`);
			}
		}

		if (code === 0) {
			return json;
		}

		if (json?.error?.code === 'E404') {
			return null;
		}

		throw new Error(
			json?.error?.summary ||
				stderr.trim().split('\n')[0] ||
				`'npm view ${spec}' exited with code ${code}`
		);
	}
}

/**
 * Turns a lockfile `name` and `range` into the registry package and range they
 * stand for, following `npm:` aliases. Ranges that do not point to the
 * registry (files, links, git, URLs) cannot be resolved.
 *
 * @param {string} name
 * @param {string} range
 * @return {{name: string, range: string}}
 * @throws {UnresolvableRangeError} when the range does not point to the registry
 */
export function getRegistryTarget(name, range) {
	if (range.startsWith('npm:')) {
		const alias = range.slice(4);

		const index = alias.indexOf('@', 1);

		if (index === -1) {
			return {name: alias, range: 'latest'};
		}

		return {name: alias.slice(0, index), range: alias.slice(index + 1)};
	}

	if (NON_REGISTRY_RANGE_REGEXP.test(range)) {
		throw new UnresolvableRangeError(
			`${name}@${range} does not point to the npm registry`
		);
	}

	return {name, range: range || 'latest'};
}
