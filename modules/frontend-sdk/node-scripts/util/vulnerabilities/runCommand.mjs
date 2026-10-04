/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {Buffer} from 'buffer';
import {spawn} from 'child_process';

/**
 * Creates a function that runs the async tasks it receives with at most
 * `concurrency` of them in flight at once.
 */
export function createLimiter(concurrency) {
	const queue = [];

	let running = 0;

	function next() {
		if (running >= concurrency || !queue.length) {
			return;
		}

		const {reject, resolve, task} = queue.shift();

		running++;

		Promise.resolve()
			.then(task)
			.then(resolve, reject)
			.finally(() => {
				running--;

				next();
			});
	}

	return (task) =>
		new Promise((resolve, reject) => {
			queue.push({reject, resolve, task});

			next();
		});
}

/**
 * Runs a command without a shell and resolves with its exit code and output.
 * It never rejects because of a non-zero exit code: callers decide what an exit
 * code means.
 */
export default function runCommand(command, args, {cwd} = {}) {
	return new Promise((resolve) => {
		const child = spawn(command, args, {
			cwd,
			env: {...process.env, FORCE_COLOR: '0', NO_COLOR: '1'},
			stdio: ['ignore', 'pipe', 'pipe'],
		});

		const stderrChunks = [];
		const stdoutChunks = [];

		child.stderr.on('data', (chunk) => stderrChunks.push(chunk));
		child.stdout.on('data', (chunk) => stdoutChunks.push(chunk));

		child.on('error', (error) =>
			resolve({
				code: -1,
				stderr: error.message,
				stdout: '',
			})
		);

		child.on('close', (code) =>
			resolve({
				code,
				stderr: Buffer.concat(stderrChunks).toString('utf-8'),
				stdout: Buffer.concat(stdoutChunks).toString('utf-8'),
			})
		);
	});
}

export function sleep(milliseconds) {
	return new Promise((resolve) => setTimeout(resolve, milliseconds));
}
