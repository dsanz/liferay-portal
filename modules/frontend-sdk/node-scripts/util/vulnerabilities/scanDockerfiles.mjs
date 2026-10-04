/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import fs from 'fs';
import path from 'path';

const EXACT_VERSION_REGEXP =
	/^(?:@[^@/]+\/)?[^@/]+@\d+\.\d+\.\d+(?:-[0-9A-Za-z.-]+)?$/;

const NPM_INSTALL_COMMANDS = new Set(['add', 'i', 'install']);

/**
 * Finds `RUN` instructions in tracked Dockerfiles that install npm packages no
 * lockfile controls. Returns `{command, dir, file}` for each of them.
 */
export default function scanDockerfiles({errors, portalDir, trackedFiles}) {
	const dockerfiles = trackedFiles.filter((file) =>
		path.posix.basename(file).startsWith('Dockerfile')
	);

	const trackedFilesSet = new Set(trackedFiles);

	const unpinnedInstalls = [];

	for (const file of dockerfiles) {
		let content;

		try {
			content = fs.readFileSync(path.join(portalDir, file), 'utf-8');
		}
		catch (error) {
			errors.push({
				message: `Unable to read ${file}: ${error.message}`,
				project: path.posix.dirname(file),
				stage: 'dockerfiles',
			});

			continue;
		}

		const dir = path.posix.dirname(file);

		const copied = {packageLock: false, yarnLock: false};

		for (const instruction of getInstructions(content)) {
			const [keyword, ...rest] = instruction.split(/\s+/);

			const upperKeyword = keyword.toUpperCase();

			if (upperKeyword === 'COPY' || upperKeyword === 'ADD') {
				updateCopied(copied, rest, dir, trackedFilesSet);
			}
			else if (upperKeyword === 'RUN') {
				const body = instruction.slice(keyword.length).trim();

				for (const command of body.split(/&&|\|\||;/)) {
					if (isUnpinned(command.trim(), copied)) {
						unpinnedInstalls.push({
							command: command.trim(),
							dir,
							file,
						});
					}
				}
			}
		}
	}

	return unpinnedInstalls;
}

/**
 * Joins continued lines and drops comments, returning one string per
 * instruction.
 */
function getInstructions(content) {
	const instructions = [];

	let current = '';

	for (const line of content.split('\n')) {
		const trimmedLine = line.trim();

		if (!current && (!trimmedLine || trimmedLine.startsWith('#'))) {
			continue;
		}

		if (trimmedLine.endsWith('\\')) {
			current += trimmedLine.slice(0, -1) + ' ';

			continue;
		}

		current += trimmedLine;

		instructions.push(current.trim());

		current = '';
	}

	if (current.trim()) {
		instructions.push(current.trim());
	}

	return instructions;
}

function getPackageArguments(tokens) {
	return tokens.filter((token) => !token.startsWith('-'));
}

function hasUnpinnedPackage(packages) {
	return packages.some((pkg) => !EXACT_VERSION_REGEXP.test(pkg));
}

function isUnpinned(command, copied) {
	const tokens = command.split(/\s+/).filter(Boolean);

	if (tokens[0] === 'npm' && NPM_INSTALL_COMMANDS.has(tokens[1])) {
		const flags = tokens.slice(2);

		const packages = getPackageArguments(flags);

		if (flags.includes('-g') || flags.includes('--global')) {
			return hasUnpinnedPackage(packages);
		}

		if (packages.length) {
			return hasUnpinnedPackage(packages) || !copied.packageLock;
		}

		return !flags.includes('--package-lock-only') && !copied.packageLock;
	}

	if (tokens[0] === 'yarn') {
		if (tokens[1] === 'global' && tokens[2] === 'add') {
			return hasUnpinnedPackage(getPackageArguments(tokens.slice(3)));
		}

		if (tokens[1] === 'add') {
			return (
				hasUnpinnedPackage(getPackageArguments(tokens.slice(2))) ||
				!copied.yarnLock
			);
		}

		if (
			tokens.length === 1 ||
			tokens[1] === 'install' ||
			tokens[1].startsWith('-')
		) {
			return !tokens.includes('--frozen-lockfile') || !copied.yarnLock;
		}

		return false;
	}

	if (tokens[0] === 'npx') {
		const [pkg] = getPackageArguments(tokens.slice(1));

		return !!pkg && !EXACT_VERSION_REGEXP.test(pkg);
	}

	return false;
}

/**
 * Records whether a `COPY` or `ADD` brings a lockfile into the image. A source
 * of `.` or a glob copies whatever the Dockerfile's folder holds.
 */
function updateCopied(copied, tokens, dir, trackedFilesSet) {
	const sources = tokens
		.filter((token) => !token.startsWith('--'))
		.slice(0, -1);

	if (tokens.some((token) => token.startsWith('--from'))) {
		return;
	}

	for (const source of sources) {
		const baseName = path.posix.basename(source);

		if (baseName === '.' || source.includes('*')) {
			copied.packageLock ||= trackedFilesSet.has(
				path.posix.join(dir, 'package-lock.json')
			);
			copied.yarnLock ||= trackedFilesSet.has(
				path.posix.join(dir, 'yarn.lock')
			);
		}
		else if (baseName === 'package-lock.json') {
			copied.packageLock = true;
		}
		else if (baseName === 'yarn.lock') {
			copied.yarnLock = true;
		}
	}
}
