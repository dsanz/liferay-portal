/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import fs from 'fs';
import path from 'path';

import {matchesWorkspaceGlob} from './patterns.mjs';

export const PROJECT_CLASS_FRONTEND = 'frontend';
export const PROJECT_CLASS_MODULES = 'modules';
export const PROJECT_CLASS_TOOLING_ONLY = 'tooling-only';

/**
 * Packages the `workspaces/` roots carry only to lint and format their sources.
 */
const LINT_SET_NAMES = new Set([
	'@liferay/eslint-plugin',
	'@liferay/prettier-plugin',
	'@liferay/stylelint-plugin',
	'babel-eslint',
	'eslint',
	'prettier',
	'typescript',
]);

const LINT_SET_PREFIXES = ['@typescript-eslint/'];

/**
 * Folder names the Gradle task `setUpYarn` skips when it lists the members of a
 * workspace (see `SetUpYarnTask` in `gradle-plugins-workspace`).
 */
const SET_UP_YARN_EXCLUDES = new Set([
	'.gradle',
	'build',
	'build_gradle',
	'dist',
	'gradle',
	'node_modules',
	'node_modules_cache',
	'src',
]);

/**
 * Finds every yarn project (a folder with a tracked `yarn.lock` yarn installs
 * from), its packages and its class, plus the lockfiles yarn never installs
 * from and the workspaces whose committed member list is out of date.
 */
export default function discoverProjects({
	errors,
	portalDir,
	projectPaths,
	trackedFiles,
}) {
	const packageJSONFiles = trackedFiles
		.filter((file) => path.posix.basename(file) === 'package.json')
		.filter((file) => !file.split('/').includes('node_modules'));

	const packageJSONDirs = new Set(
		packageJSONFiles.map((file) => path.posix.dirname(file))
	);

	const lockfileDirs = trackedFiles
		.filter((file) => path.posix.basename(file) === 'yarn.lock')
		.map((file) => path.posix.dirname(file))
		.sort();

	const jsonCache = new Map();

	function readPackageJSON(dir) {
		if (jsonCache.has(dir)) {
			return jsonCache.get(dir);
		}

		const file = path.posix.join(dir, 'package.json');

		let json = null;

		try {
			json = JSON.parse(
				fs.readFileSync(path.join(portalDir, file), 'utf-8')
			);
		}
		catch (error) {
			errors.push({
				message: `Unable to read ${file}: ${error.message}`,
				project: dir,
				stage: 'discover',
			});
		}

		jsonCache.set(dir, json);

		return json;
	}

	const deadLockfiles = [];
	const membersDrifts = [];
	const projects = [];

	for (const lockfileDir of lockfileDirs) {
		const ancestorDir = findWorkspaceAncestor(
			lockfileDir,
			packageJSONDirs,
			readPackageJSON
		);

		if (ancestorDir) {
			deadLockfiles.push({
				ancestor: ancestorDir,
				file: path.posix.join(lockfileDir, 'yarn.lock'),
			});

			continue;
		}

		if (!packageJSONDirs.has(lockfileDir)) {
			continue;
		}

		const rootJSON = readPackageJSON(lockfileDir);

		if (!rootJSON) {
			continue;
		}

		const root = createPackage(lockfileDir, rootJSON);

		const memberDirs = expandWorkspaceGlobs(
			lockfileDir,
			getWorkspaceGlobs(rootJSON),
			packageJSONDirs
		);

		const members = memberDirs
			.map((dir) => {
				const json = readPackageJSON(dir);

				return json ? createPackage(dir, json) : null;
			})
			.filter(Boolean);

		const project = {
			class: getProjectClass(lockfileDir, rootJSON, members),
			dir: path.join(portalDir, lockfileDir),
			members,
			membersByName: new Map(
				members
					.filter((member) => member.name)
					.map((member) => [member.name, member])
			),
			packages: [root, ...members],
			path: lockfileDir,
			root,
		};

		if (lockfileDir.startsWith('workspaces/')) {
			const drift = getMembersDrift(
				lockfileDir,
				memberDirs,
				packageJSONDirs
			);

			if (drift) {
				membersDrifts.push({project: lockfileDir, ...drift});
			}
		}

		projects.push(project);
	}

	if (!projectPaths.length) {
		return {deadLockfiles, membersDrifts, projects};
	}

	const projectPathsSet = new Set(
		projectPaths.map((projectPath) =>
			projectPath.replace(/^\.?\/+|\/+$/g, '')
		)
	);

	return {
		deadLockfiles: deadLockfiles.filter(({ancestor}) =>
			projectPathsSet.has(ancestor)
		),
		membersDrifts: membersDrifts.filter(({project}) =>
			projectPathsSet.has(project)
		),
		projects: projects.filter((project) =>
			projectPathsSet.has(project.path)
		),
	};
}

export function getWorkspaceGlobs(json) {
	const workspaces = json?.workspaces;

	if (Array.isArray(workspaces)) {
		return workspaces;
	}

	return workspaces?.packages || [];
}

export function isLintSetPackage(name) {
	return (
		LINT_SET_NAMES.has(name) ||
		LINT_SET_PREFIXES.some((prefix) => name.startsWith(prefix))
	);
}

function createPackage(dir, json) {
	return {
		dir,
		file: path.posix.join(dir, 'package.json'),
		json,
		name: json.name || null,
	};
}

function expandWorkspaceGlobs(projectDir, globs, packageJSONDirs) {
	if (!globs.length) {
		return [];
	}

	const prefix = `${projectDir}/`;

	return [...packageJSONDirs]
		.filter((dir) => dir.startsWith(prefix))
		.filter((dir) => {
			const relativeDir = dir.slice(prefix.length);

			return globs.some((glob) =>
				matchesWorkspaceGlob(relativeDir, glob)
			);
		})
		.sort();
}

function findWorkspaceAncestor(lockfileDir, packageJSONDirs, readPackageJSON) {
	let dir = lockfileDir;

	while (dir !== '.' && dir.includes('/')) {
		dir = path.posix.dirname(dir);

		if (!packageJSONDirs.has(dir)) {
			continue;
		}

		const relativeDir = lockfileDir.slice(dir.length + 1);

		const globs = getWorkspaceGlobs(readPackageJSON(dir));

		if (globs.some((glob) => matchesWorkspaceGlob(relativeDir, glob))) {
			return dir;
		}
	}

	return null;
}

/**
 * Compares the committed member list with the one the Gradle task `setUpYarn`
 * will write on the next build: every `package.json` below the root, skipping
 * the folders it excludes.
 */
function getMembersDrift(projectDir, memberDirs, packageJSONDirs) {
	const prefix = `${projectDir}/`;

	const expectedDirs = [...packageJSONDirs]
		.filter((dir) => dir.startsWith(prefix))
		.filter(
			(dir) =>
				!dir
					.slice(prefix.length)
					.split('/')
					.some((segment) => SET_UP_YARN_EXCLUDES.has(segment))
		)
		.sort();

	const committedDirs = new Set(memberDirs);
	const expectedDirsSet = new Set(expectedDirs);

	const added = expectedDirs.filter((dir) => !committedDirs.has(dir));
	const removed = memberDirs.filter((dir) => !expectedDirsSet.has(dir));

	if (!added.length && !removed.length) {
		return null;
	}

	return {
		added: added.map((dir) => dir.slice(prefix.length)),
		removed: removed.map((dir) => dir.slice(prefix.length)),
	};
}

function getProjectClass(projectDir, rootJSON, members) {
	if (projectDir === 'modules' || projectDir.startsWith('modules/')) {
		return PROJECT_CLASS_MODULES;
	}

	if (!projectDir.startsWith('workspaces/')) {
		return PROJECT_CLASS_FRONTEND;
	}

	if (members.length || Object.keys(rootJSON.dependencies || {}).length) {
		return PROJECT_CLASS_FRONTEND;
	}

	const devDependencies = Object.keys(rootJSON.devDependencies || {});

	if (devDependencies.every((name) => isLintSetPackage(name))) {
		return PROJECT_CLASS_TOOLING_ONLY;
	}

	return PROJECT_CLASS_FRONTEND;
}
