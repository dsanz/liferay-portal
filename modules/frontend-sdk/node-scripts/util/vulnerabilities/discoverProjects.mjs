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
 * @typedef {object} Package
 * @property {string} dir Folder holding the `package.json`.
 * @property {string} file Path of the `package.json`.
 * @property {object} json Its parsed content.
 * @property {string | null} name Its `name` field.
 */

/**
 * @typedef {object} Project
 * @property {string} path Folder holding the lockfile, relative to the
 * repository root.
 * @property {string} dir The same folder, absolute.
 * @property {import('./reportTypes.mjs').ProjectClass} class
 * @property {import('./reportTypes.mjs').PackageManager} packageManager
 * @property {Package} root The `package.json` next to the lockfile.
 * @property {Package[]} members The workspace members (none for npm).
 * @property {Map<string, Package>} membersByName Members by package name.
 * @property {Package[]} packages The root followed by the members.
 */

export const PACKAGE_MANAGER_NPM = 'npm';
export const PACKAGE_MANAGER_YARN = 'yarn';

const TEST_FIXTURE_REGEXP =
	/(?:^|\/)src\/(?:gradleTest|test|testIntegration)\//;

/**
 * Finds every project, its packages and its class, plus the lockfiles nothing
 * installs from and the workspaces whose committed member list is out of date.
 *
 * A yarn project is a folder with a tracked `yarn.lock` yarn installs from. An
 * npm project is a folder with a tracked, non-empty `package-lock.json` that is
 * either outside any yarn workspace, or inside one but consumed by a
 * Dockerfile that installs from it (`npmLockConsumerDirs`). Lockfiles under
 * test fixtures are ignored.
 *
 * @param {object} options
 * @param {object[]} options.errors where failures are appended, as `ReportError`s
 * @param {Set<string>} options.npmLockConsumerDirs folders whose Dockerfile installs from their `package-lock.json`
 * @param {string} options.portalDir absolute path of the repository root
 * @param {string[]} options.projectPaths the `--project` filter, empty for every project
 * @param {string[]} options.trackedFiles tracked lockfiles, `package.json` files and Dockerfiles
 * @return {{deadLockfiles: {ancestor: string, file: string}[], membersDrifts: {added: string[], project: string, removed: string[]}[], projects: Project[]}}
 */
export default function discoverProjects({
	errors,
	npmLockConsumerDirs,
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

	const lockfileDirs = getLockfileDirs(trackedFiles, 'yarn.lock');
	const npmLockfileDirs = getLockfileDirs(trackedFiles, 'package-lock.json');

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
			packageManager: PACKAGE_MANAGER_YARN,
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

	const yarnProjectPaths = new Set(projects.map((project) => project.path));

	for (const lockfileDir of npmLockfileDirs) {
		const file = path.posix.join(lockfileDir, 'package-lock.json');

		if (
			yarnProjectPaths.has(lockfileDir) ||
			!packageJSONDirs.has(lockfileDir) ||
			!fs.statSync(path.join(portalDir, file)).size
		) {
			continue;
		}

		const ancestorDir = findWorkspaceAncestor(
			lockfileDir,
			packageJSONDirs,
			readPackageJSON
		);

		if (ancestorDir && !npmLockConsumerDirs.has(lockfileDir)) {
			deadLockfiles.push({ancestor: ancestorDir, file});

			continue;
		}

		const rootJSON = readPackageJSON(lockfileDir);

		if (!rootJSON) {
			continue;
		}

		const root = createPackage(lockfileDir, rootJSON);

		projects.push({
			class: isModulesPath(lockfileDir)
				? PROJECT_CLASS_MODULES
				: PROJECT_CLASS_FRONTEND,
			dir: path.join(portalDir, lockfileDir),
			members: [],
			membersByName: new Map(),
			packageManager: PACKAGE_MANAGER_NPM,
			packages: [root],
			path: lockfileDir,
			root,
		});
	}

	projects.sort((left, right) => left.path.localeCompare(right.path));

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

/**
 * @param {object} json a parsed `package.json`
 * @return {string[]} its `workspaces` globs, in either form
 */
export function getWorkspaceGlobs(json) {
	const workspaces = json?.workspaces;

	if (Array.isArray(workspaces)) {
		return workspaces;
	}

	return workspaces?.packages || [];
}

/**
 * @param {string} name
 * @return {boolean} whether the package belongs to the lint and format toolchain
 */
export function isLintSetPackage(name) {
	return (
		LINT_SET_NAMES.has(name) ||
		LINT_SET_PREFIXES.some((prefix) => name.startsWith(prefix))
	);
}

/**
 * @param {string} file path relative to the repository root
 * @return {boolean} whether it is under a test fixture folder
 */
export function isTestFixture(file) {
	return TEST_FIXTURE_REGEXP.test(file);
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
function getLockfileDirs(trackedFiles, baseName) {
	return trackedFiles
		.filter((file) => path.posix.basename(file) === baseName)
		.filter((file) => !file.split('/').includes('node_modules'))
		.filter((file) => !isTestFixture(file))
		.map((file) => path.posix.dirname(file))
		.sort();
}

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
	if (isModulesPath(projectDir)) {
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

function isModulesPath(dir) {
	return dir === 'modules' || dir.startsWith('modules/');
}
