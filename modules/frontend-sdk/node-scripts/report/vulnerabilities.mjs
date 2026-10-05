/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

/**
 * Reports the known vulnerabilities of every yarn project in the repository,
 * who approves each fix, and which fix the remediation policy calls for.
 *
 * This script only imports Node built-ins and calls the `git`, `yarn` and
 * `npm` command-line tools, so it runs from a bare checkout:
 *
 *     node modules/frontend-sdk/node-scripts/report/vulnerabilities.mjs
 *
 * It is report-only: it never edits files and its exit code never depends on
 * the findings.
 */

import fs from 'fs';
import path from 'path';
import url from 'url';

import print from '../util/print.mjs';
import CodeOwners from '../util/vulnerabilities/CodeOwners.mjs';
import Registry from '../util/vulnerabilities/Registry.mjs';
import assignFixTypes, {
	createFixer,
} from '../util/vulnerabilities/assignFixTypes.mjs';
import checkResolutions from '../util/vulnerabilities/checkResolutions.mjs';
import collectFindings, {
	TAG_OUT_OF_SYNC_LOCKFILE,
	getFindingId,
	unique,
} from '../util/vulnerabilities/collectFindings.mjs';
import discoverProjects, {
	PACKAGE_MANAGER_NPM,
} from '../util/vulnerabilities/discoverProjects.mjs';
import parsePackageLock from '../util/vulnerabilities/parsePackageLock.mjs';
import parseYarnLock from '../util/vulnerabilities/parseYarnLock.mjs';
import renderMarkdown from '../util/vulnerabilities/renderMarkdown.mjs';
import {SCHEMA_VERSION} from '../util/vulnerabilities/reportTypes.mjs';
import runCommand, {
	createLimiter,
} from '../util/vulnerabilities/runCommand.mjs';
import scanDockerfiles from '../util/vulnerabilities/scanDockerfiles.mjs';
import scanLockfile from '../util/vulnerabilities/scanLockfile.mjs';

const COMMAND_NAME = 'report:vulnerabilities';

/**
 * The repository root, worked out here instead of imported from
 * `util/locations.mjs`: that module reads the portal's built Node folder when
 * it loads, so importing it would break runs from a checkout that was never
 * built.
 */
const PORTAL_DIR = path.resolve(
	path.dirname(url.fileURLToPath(import.meta.url)),
	'..',
	'..',
	'..',
	'..'
);

const EXIT_CODE_PARTIAL = 2;
const EXIT_CODE_USAGE = 1;

const FRONTEND_JS_DEPENDENCIES_WEB_DIR =
	'modules/apps/frontend-js/frontend-js-dependencies-web';

const OPTIONS = {
	'--concurrency': 'concurrency',
	'--format': 'format',
	'--output': 'output',
	'--owner': 'owner',
	'--project': 'projects',
};

class UsageError extends Error {}

export default async function main() {
	let options;

	try {
		options = parseArguments(
			process.argv.slice(2).filter((arg) => arg !== COMMAND_NAME)
		);
	}
	catch (error) {
		print(0, print.error('ERROR:'), error.message, '\n');

		process.exitCode = EXIT_CODE_USAGE;

		return;
	}

	const environment = await getEnvironment();

	if (environment.error) {
		print(0, print.error('ERROR:'), environment.error, '\n');

		process.exitCode = EXIT_CODE_USAGE;

		return;
	}

	let report;

	try {
		report = await createReport({environment, options});
	}
	catch (error) {
		if (!(error instanceof UsageError)) {
			throw error;
		}

		print(0, print.error('ERROR:'), error.message, '\n');

		process.exitCode = EXIT_CODE_USAGE;

		return;
	}

	const output =
		options.format === 'json'
			? JSON.stringify(report, null, '\t') + '\n'
			: renderMarkdown(report);

	if (options.output) {
		fs.writeFileSync(options.output, output, 'utf-8');

		print(0, print.info('INFO:'), `Wrote report file: ${options.output}\n`);
	}
	else {
		process.stdout.write(output);
	}

	if (
		report.errors.length ||
		report.findings.some(
			(finding) => finding.type === 'lockfile-out-of-sync'
		)
	) {
		process.exitCode = EXIT_CODE_PARTIAL;
	}
}

/**
 * Builds the whole report. Its shape is defined in `util/vulnerabilities/reportTypes.mjs`.
 *
 * @param {object} params
 * @param {{gitCommit: string, npm: string, registry: string, yarn: string}} params.environment
 * @param {{concurrency: number, format: string, output: string | null, owner: string | null, projects: string[]}} params.options
 * @return {Promise<import('../util/vulnerabilities/reportTypes.mjs').Report>}
 */
async function createReport({environment, options}) {
	const errors = [];

	const trackedFiles = await getTrackedFiles();

	const {npmLockConsumerDirs, unpinnedInstalls} = scanDockerfiles({
		errors,
		portalDir: PORTAL_DIR,
		trackedFiles,
	});

	const {deadLockfiles, membersDrifts, projects} = discoverProjects({
		errors,
		npmLockConsumerDirs,
		portalDir: PORTAL_DIR,
		projectPaths: options.projects,
		trackedFiles,
	});

	const unknownProjectPaths = options.projects.filter(
		(projectPath) =>
			!projects.some((project) => project.path === projectPath)
	);

	if (unknownProjectPaths.length) {
		throw new UsageError(
			`No yarn project found at ${unknownProjectPaths.map((projectPath) => `'${projectPath}'`).join(', ')}. A project is a folder with a tracked yarn.lock that yarn installs from.`
		);
	}

	const codeOwners = new CodeOwners(PORTAL_DIR);

	const attribution = {
		codeOwners,
		infraOwners: codeOwners.getOwners(FRONTEND_JS_DEPENDENCIES_WEB_DIR),
		sharedLibraries: getSharedLibraries(),
	};

	const limit = createLimiter(options.concurrency);

	const registry = new Registry({
		limit,
		registryURL: environment.registry,
	});

	const fixer = createFixer(registry);

	const allowedReasons = await getAllowedReasons();

	print(0, print.title(`Auditing ${projects.length} projects...\n`));

	const projectReports = await Promise.all(
		projects.map((project) =>
			processProject({
				allowedReasons,
				attribution,
				errors,
				fixer,
				project,
				registry,
			})
		)
	);

	const findings = projectReports.flatMap(({findings}) => findings);
	const resolutions = projectReports.flatMap(({resolutions}) => resolutions);

	findings.push(
		...projectReports.flatMap(({lockfileFindings}) => lockfileFindings),
		...deadLockfiles.map(({ancestor, file}) => ({
			approvers: codeOwners.getOwners(ancestor),
			file,
			id: getFindingId(`dead-lockfile|${file}`),
			project: ancestor,
			type: 'dead-lockfile',
		})),
		...membersDrifts.map(({added, project, removed}) => ({
			added,
			approvers: codeOwners.getOwners(project),
			id: getFindingId(`members-drift|${project}`),
			project,
			removed,
			type: 'members-drift',
		})),
		...getUnpinnedInstallFindings({
			codeOwners,
			options,
			projects,
			unpinnedInstalls,
		})
	);

	return filterByOwner(
		{
			environment: {
				node: process.versions.node,
				npm: environment.npm,
				registry: environment.registry,
				yarn: environment.yarn,
			},
			errors,
			findings: findings.sort(compareFindings),
			generatedAt: new Date().toISOString().replace(/\.\d+Z$/, 'Z'),
			gitCommit: environment.gitCommit,
			options: {
				owner: options.owner,
				projects: options.projects,
			},
			projects: projectReports.map(({project}) => project),
			resolutions,
			schemaVersion: SCHEMA_VERSION,
			summary: null,
		},
		options.owner
	);
}

function compareFindings(left, right) {
	return (
		left.project.localeCompare(right.project) ||
		left.type.localeCompare(right.type) ||
		(left.package || left.file || '').localeCompare(
			right.package || right.file || ''
		) ||
		(left.installedVersion || '').localeCompare(
			right.installedVersion || ''
		) ||
		left.id.localeCompare(right.id)
	);
}

function countBy(items, getKeys) {
	const counts = {};

	for (const item of items) {
		for (const key of [getKeys(item)].flat()) {
			counts[key] = (counts[key] || 0) + 1;
		}
	}

	return Object.fromEntries(
		Object.entries(counts).sort(([left], [right]) =>
			left.localeCompare(right)
		)
	);
}

/**
 * Keeps only what one approver owns, then computes the summary over what is
 * left.
 */
function filterByOwner(report, owner) {
	if (owner) {
		report.findings = report.findings.filter((finding) =>
			finding.approvers.includes(owner)
		);
		report.resolutions = report.resolutions.filter((resolution) =>
			resolution.approvers.includes(owner)
		);

		const projectPaths = new Set([
			...report.findings.map((finding) => finding.project),
			...report.resolutions.map((resolution) => resolution.project),
		]);

		report.projects = report.projects.filter((project) =>
			projectPaths.has(project.path)
		);
	}

	const findingIds = new Set(report.findings.map((finding) => finding.id));

	for (const project of report.projects) {
		project.findings = project.findings.filter((id) => findingIds.has(id));
	}

	const advisoryFindings = report.findings.filter(
		(finding) => finding.type === 'advisory'
	);

	report.summary = {
		findings: {
			byApprover: countBy(
				advisoryFindings,
				(finding) => finding.approvers
			),
			byFixType: countBy(advisoryFindings, (finding) => finding.fixType),
			byScope: countBy(advisoryFindings, (finding) => finding.scope),
			bySeverity: countBy(
				advisoryFindings,
				(finding) => finding.advisory.severity
			),
			byType: countBy(report.findings, (finding) => finding.type),
		},
		projects: report.projects.length,
		uniqueAdvisoryPackagePairs: new Set(
			advisoryFindings.map(
				(finding) => `${finding.advisory.ghsa}|${finding.package}`
			)
		).size,
	};

	return report;
}

async function getAllowedReasons() {
	try {
		const {default: allowedReasons} = await import(
			'../util/format/formatters/ALLOWED_ROOT_PACKAGE_JSON_RESOLUTIONS.mjs'
		);

		return allowedReasons;
	}
	catch {
		return {};
	}
}

async function getEnvironment() {
	const [git, npm, registry, yarn] = await Promise.all([
		runCommand('git', ['rev-parse', '--short=13', 'HEAD'], {
			cwd: PORTAL_DIR,
		}),
		runCommand('npm', ['--version']),
		runCommand('npm', ['config', 'get', 'registry']),
		runCommand('yarn', ['--version']),
	]);

	if (git.code !== 0) {
		return {error: `${PORTAL_DIR} is not a git repository`};
	}

	if (npm.code !== 0) {
		return {error: 'npm is not available on PATH'};
	}

	if (yarn.code !== 0) {
		return {error: 'yarn is not available on PATH'};
	}

	if (!yarn.stdout.trim().startsWith('1.')) {
		return {error: `yarn 1.x is required, found ${yarn.stdout.trim()}`};
	}

	return {
		gitCommit: git.stdout.trim(),
		npm: npm.stdout.trim(),
		registry: registry.stdout.trim() || 'https://registry.npmjs.org/',
		yarn: yarn.stdout.trim(),
	};
}

function getSharedLibraries() {
	try {
		const json = JSON.parse(
			fs.readFileSync(
				path.join(
					PORTAL_DIR,
					FRONTEND_JS_DEPENDENCIES_WEB_DIR,
					'package.json'
				),
				'utf-8'
			)
		);

		return new Set(Object.keys(json.dependencies || {}));
	}
	catch {
		return new Set();
	}
}

async function getTrackedFiles() {
	const {stdout} = await runCommand(
		'git',
		[
			'ls-files',
			'-z',
			'--',
			'*Dockerfile*',
			'*package-lock.json',
			'*package.json',
			'*yarn.lock',
		],
		{cwd: PORTAL_DIR}
	);

	return stdout.split('\0').filter(Boolean);
}

function getUnpinnedInstallFindings({
	codeOwners,
	options,
	projects,
	unpinnedInstalls,
}) {
	const projectPaths = projects
		.map((project) => project.path)
		.sort((left, right) => right.length - left.length);

	return unpinnedInstalls
		.map(({command, dir, file}) => ({
			approvers: codeOwners.getOwners(dir),
			command,
			file,
			id: getFindingId(`unpinned-install|${file}|${command}`),
			project:
				projectPaths.find((projectPath) =>
					dir.startsWith(`${projectPath}/`)
				) || dir,
			type: 'unpinned-install',
		}))
		.filter(
			(finding) =>
				!options.projects.length ||
				projectPaths.includes(finding.project)
		);
}

function parseArguments(args) {
	const options = {
		concurrency: 8,
		format: 'md',
		output: null,
		owner: null,
		projects: [],
	};

	for (let i = 0; i < args.length; i++) {
		const [name, inlineValue] = args[i].split(/=(.*)/s);

		const option = OPTIONS[name];

		if (!option) {
			throw new Error(`Unknown option '${args[i]}'`);
		}

		const value = inlineValue ?? args[++i];

		if (value === undefined || value === '') {
			throw new Error(`Option '${name}' needs a value`);
		}

		if (option === 'projects') {
			options.projects.push(toRepositoryPath(value));
		}
		else {
			options[option] = value;
		}
	}

	options.concurrency = Number(options.concurrency);

	if (!Number.isInteger(options.concurrency) || options.concurrency < 1) {
		throw new Error(`Option '--concurrency' needs a positive integer`);
	}

	if (!['json', 'md'].includes(options.format)) {
		throw new Error(`Option '--format' must be 'json' or 'md'`);
	}

	return options;
}

async function processProject({
	allowedReasons,
	attribution,
	errors,
	fixer,
	project,
	registry,
}) {
	const projectReport = {
		approvers: attribution.codeOwners.getOwners(project.path),
		audited: false,
		class: project.class,
		findings: [],
		members: project.members.length,
		packageManager: project.packageManager,
		path: project.path,
	};

	const npm = project.packageManager === PACKAGE_MANAGER_NPM;

	const lockfile = path.posix.join(
		project.path,
		npm ? 'package-lock.json' : 'yarn.lock'
	);

	let lock;

	try {
		const content = fs.readFileSync(
			path.join(PORTAL_DIR, lockfile),
			'utf-8'
		);

		lock = npm ? parsePackageLock(content) : parseYarnLock(content);
	}
	catch (error) {
		errors.push({
			message: error.message,
			project: project.path,
			stage: 'discover',
		});

		return {
			findings: [],
			lockfileFindings: [],
			project: projectReport,
			resolutions: [],
		};
	}

	let findings = [];

	const lockfileFindings = [];

	try {
		const {advisories, orphans, sync} = await scanLockfile({
			lock,
			project,
			registry,
		});

		projectReport.audited = true;
		projectReport.inSync = !sync.incomplete.length && !sync.missing.length;

		findings = collectFindings({advisories, attribution, lock, project});

		if (!projectReport.inSync) {
			for (const finding of findings) {
				finding.tags = unique([
					...finding.tags,
					TAG_OUT_OF_SYNC_LOCKFILE,
				]);
			}

			lockfileFindings.push({
				approvers: projectReport.approvers,
				file: lockfile,
				id: getFindingId(`lockfile-out-of-sync|${lockfile}`),
				incomplete: sync.incomplete,
				missing: sync.missing,
				project: project.path,
				type: 'lockfile-out-of-sync',
			});
		}

		if (orphans.length) {
			lockfileFindings.push({
				approvers: projectReport.approvers,
				entries: orphans,
				file: lockfile,
				id: getFindingId(`lockfile-drift|${lockfile}`),
				project: project.path,
				type: 'lockfile-drift',
			});
		}
	}
	catch (error) {
		errors.push({
			message: error.message,
			project: project.path,
			stage: 'scan',
		});
	}

	try {
		await assignFixTypes({findings, project, registry});
	}
	catch (error) {
		errors.push({
			message: error.message,
			project: project.path,
			stage: 'fix',
		});
	}

	let resolutions = [];

	// npm ignores the "resolutions" field (its equivalent is "overrides"),
	// so only yarn projects are checked.

	try {
		resolutions = npm
			? []
			: await checkResolutions({
					allowedReasons:
						project.path === 'modules' ? allowedReasons : null,
					attribution,
					fixer,
					lock,
					project,
					registry,
				});
	}
	catch (error) {
		errors.push({
			message: error.message,
			project: project.path,
			stage: 'resolutions',
		});
	}

	projectReport.findings = findings.map((finding) => finding.id);

	print(
		1,
		`${projectReport.audited ? print.success('DONE') : print.error('FAILED')} ${project.path}: ${findings.length} findings, ${resolutions.length} resolutions`
	);

	return {
		findings: findings.map(toOutputFinding),
		lockfileFindings,
		project: projectReport,
		resolutions,
	};
}

/**
 * Turns a `--project` value, relative to the current directory, into a path
 * relative to the repository root, which is how projects are named.
 */
function toRepositoryPath(value) {
	const relativePath = path
		.relative(PORTAL_DIR, path.resolve(value))
		.split(path.sep)
		.join('/');

	if (!relativePath || relativePath.startsWith('..')) {
		throw new UsageError(`'${value}' is not a folder inside ${PORTAL_DIR}`);
	}

	return relativePath;
}

function toOutputFinding(finding) {
	return {
		advisory: finding.advisory,
		approvers: unique(finding.approvers),
		chains: finding.chains.map((chain) => ({
			declaredIn: chain.declaredIn,
			directDependency: chain.directDependency,
			fix: chain.fix,
			installedChain: chain.installedChain,
			parent: chain.parent,
			parentRange: chain.parentRange,
			path: chain.path,
			scope: chain.scope,
		})),
		fixType: finding.fixType,
		id: finding.id,
		installedVersion: finding.installedVersion,
		package: finding.package,
		project: finding.project,
		scope: finding.scope,
		tags: finding.tags,
		type: finding.type,
	};
}

if (import.meta.url === url.pathToFileURL(process.argv[1]).href) {
	main().catch((error) => {
		console.error(error);

		process.exitCode = EXIT_CODE_USAGE;
	});
}
