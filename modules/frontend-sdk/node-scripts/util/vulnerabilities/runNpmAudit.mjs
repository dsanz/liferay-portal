/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import fs from 'fs';
import os from 'os';
import path from 'path';

import runCommand, {sleep} from './runCommand.mjs';

const RETRY_DELAYS = [5000, 15000, 30000, 60000];

/**
 * Runs `npm audit --package-lock-only --json` against a project's lockfile and
 * returns its advisories in the shape `yarn audit` prints them, so the rest of
 * the report treats both package managers alike.
 *
 * The lockfile and `package.json` are audited from a copy in a temporary
 * folder: run in place, npm walks up to the enclosing yarn workspace root and
 * fails because that root has no npm lockfile.
 *
 * npm audit prints no dependency chains, so they are rebuilt from the
 * lockfile, and it reports every vulnerable install of a package against all
 * the package's advisories, so each install is checked against the advisory
 * database to keep only the advisories that affect its version.
 */
export default async function runNpmAudit({lock, projectDir, registry}) {
	const json = await auditCopy(projectDir);

	const advisories = new Map();

	for (const vulnerability of Object.values(json.vulnerabilities || {})) {
		for (const location of vulnerability.nodes || []) {
			const entry = lock.tree.get(location);

			if (!entry?.version) {
				continue;
			}

			const affecting = new Set(
				(await registry.getAdvisories(entry.name, entry.version)).map(
					(advisory) => advisory.url
				)
			);

			const paths = lock
				.getChains(location)
				.map((names) => names.join('>'));

			for (const via of vulnerability.via || []) {
				if (typeof via !== 'object' || !affecting.has(via.url)) {
					continue;
				}

				let advisory = advisories.get(via.url);

				if (!advisory) {
					advisory = {
						cves: [],
						findings: [],
						github_advisory_id:
							/GHSA-[0-9a-z]{4}-[0-9a-z]{4}-[0-9a-z]{4}/.exec(
								via.url
							)?.[0] || String(via.source),
						module_name: via.name,
						patched_versions: null,
						severity: via.severity,
						title: via.title,
						url: via.url,
						vulnerable_versions: via.range,
					};

					advisories.set(via.url, advisory);
				}

				advisory.findings.push({paths, version: entry.version});
			}
		}
	}

	return {
		advisories: [...advisories.values()].map((advisory) => ({advisory})),
		summary: json.metadata,
	};
}

async function auditCopy(projectDir) {
	const tempDir = fs.mkdtempSync(path.join(os.tmpdir(), 'npm-audit-'));

	try {
		for (const file of ['package.json', 'package-lock.json']) {
			fs.copyFileSync(
				path.join(projectDir, file),
				path.join(tempDir, file)
			);
		}

		let lastError = '';

		for (let attempt = 0; attempt <= RETRY_DELAYS.length; attempt++) {
			if (attempt) {
				await sleep(RETRY_DELAYS[attempt - 1]);
			}

			const {stderr, stdout} = await runCommand(
				'npm',
				['audit', '--package-lock-only', '--json'],
				{cwd: tempDir}
			);

			let json = null;

			try {
				json = JSON.parse(stdout);
			}
			catch {
				lastError =
					stderr.trim().split('\n')[0] ||
					'npm audit printed no JSON report';

				continue;
			}

			if (json.auditReportVersion) {
				return json;
			}

			lastError =
				json.error?.summary || 'npm audit printed no audit report';
		}

		throw new Error(lastError);
	}
	finally {
		fs.rmSync(tempDir, {force: true, recursive: true});
	}
}
