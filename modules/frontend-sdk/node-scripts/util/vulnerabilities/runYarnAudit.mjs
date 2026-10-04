/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import runCommand, {sleep} from './runCommand.mjs';

/**
 * The audit endpoint answers concurrent audits with HTTP 429, so audits run
 * with a concurrency of their own, lower than the registry lookups'.
 */
export const AUDIT_CONCURRENCY = 2;

const RETRY_DELAYS = [5000, 15000, 30000, 60000];

/**
 * Runs `yarn audit --json` in a project root and returns its `auditAdvisory`
 * payloads and its `auditSummary` payload.
 *
 * A run only counts when its output contains the `auditSummary` line: the
 * audit endpoint sometimes answers with an empty output or a rate limit error,
 * so the run is retried with backoff before giving up.
 */
export default async function runYarnAudit(projectDir) {
	let lastError = '';

	for (let attempt = 0; attempt <= RETRY_DELAYS.length; attempt++) {
		if (attempt) {
			await sleep(RETRY_DELAYS[attempt - 1]);
		}

		const {stderr, stdout} = await runCommand(
			'yarn',
			['audit', '--json', '--non-interactive'],
			{cwd: projectDir}
		);

		const advisories = [];
		const errors = [];

		let summary = null;

		for (const line of `${stdout}\n${stderr}`.split('\n')) {
			if (!line.trim()) {
				continue;
			}

			let json;

			try {
				json = JSON.parse(line);
			}
			catch {
				continue;
			}

			if (json.type === 'auditAdvisory') {
				advisories.push(json.data);
			}
			else if (json.type === 'auditSummary') {
				summary = json.data;
			}
			else if (json.type === 'error') {
				errors.push(String(json.data).split('\n')[0]);
			}
		}

		if (summary) {
			return {advisories, summary};
		}

		lastError =
			errors.join(' ') ||
			stderr.trim().split('\n')[0] ||
			'yarn audit printed no auditSummary line';
	}

	throw new Error(lastError);
}
