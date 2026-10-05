/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import crypto from 'crypto';
import path from 'path';

import {PROJECT_CLASS_MODULES, isLintSetPackage} from './discoverProjects.mjs';

export const DECLARATION_FIELDS = [
	'dependencies',
	'devDependencies',
	'optionalDependencies',
	'peerDependencies',
];

export const SCOPE_BUILD = 'build';
export const SCOPE_RUNTIME = 'runtime';

export const TAG_LINT_TEMPLATE = 'lint-template';
export const TAG_OUT_OF_SYNC_LOCKFILE = 'out-of-sync-lockfile';
export const TAG_SHARED_LIBRARY = 'shared-library';

const FRONTEND_SDK_PATH = 'modules/frontend-sdk/';

/**
 * The root `package.json` of `modules/` holds node-scripts' global toolchain,
 * which frontend infra owns although CODEOWNERS has no entry for it.
 */
const GLOBAL_PACKAGE_JSON_FILE = 'modules/package.json';

const NODE_SCRIPTS_PACKAGE_NAME = '@liferay/node-scripts';

const RUNTIME_FIELDS = [
	'dependencies',
	'optionalDependencies',
	'peerDependencies',
];

/**
 * Turns the `auditAdvisory` payloads of one project into deduplicated
 * findings: one per (project, GHSA id, package, installed version), keeping
 * every chain, each attributed to its declaring packages, scope and approvers.
 */
export default function collectFindings({
	advisories,
	attribution,
	lock,
	project,
}) {
	const findings = new Map();

	for (const {advisory} of advisories) {
		const ghsa = advisory.github_advisory_id || getGHSA(advisory.url);

		for (const {paths, version} of advisory.findings || []) {
			const key = [
				project.path,
				ghsa,
				advisory.module_name,
				version,
			].join('|');

			let finding = findings.get(key);

			if (!finding) {
				finding = {
					advisory: {
						cves: advisory.cves || [],
						ghsa,
						patched: advisory.patched_versions,
						severity: advisory.severity,
						title: advisory.title,
						url: advisory.url,
						vulnerable: advisory.vulnerable_versions,
					},
					approvers: [],
					chains: [],
					fixType: null,
					id: getFindingId(key),
					installedVersion: version,
					package: advisory.module_name,
					project: project.path,
					scope: null,
					tags: [],
					type: 'advisory',
				};

				findings.set(key, finding);
			}

			for (const chainPath of paths || []) {
				if (
					finding.chains.some(
						(chain) => chain.path.join('>') === chainPath
					)
				) {
					continue;
				}

				finding.chains.push(
					attributeChain({
						attribution,
						lock,
						names: chainPath.split('>'),
						project,
					})
				);
			}
		}
	}

	for (const finding of findings.values()) {
		finding.approvers = unique(
			finding.chains.flatMap((chain) => chain.approvers)
		);
		finding.scope = finding.chains.some(
			(chain) => chain.scope === SCOPE_RUNTIME
		)
			? SCOPE_RUNTIME
			: SCOPE_BUILD;
		finding.tags = unique(finding.chains.flatMap((chain) => chain.tags));
	}

	return [...findings.values()];
}

export function getFindingId(key) {
	return crypto.createHash('sha1').update(key).digest('hex').slice(0, 12);
}

export function unique(values) {
	return [...new Set(values)].sort();
}

/**
 * Attributes one chain. Leading elements that are workspace members (internal
 * packages) are skipped: the first element that is not a member is the direct
 * dependency, and the last member skipped declares it.
 */
function attributeChain({attribution, lock, names, project}) {
	let index = 0;
	let lastMember = null;

	while (
		index < names.length - 1 &&
		project.membersByName.has(names[index])
	) {
		lastMember = project.membersByName.get(names[index]);

		index++;
	}

	const directDependency = names[index];

	const declaringPackages = lastMember ? [lastMember] : project.packages;

	const declaredIn = declaringPackages.flatMap((pkg) =>
		DECLARATION_FIELDS.filter(
			(field) => pkg.json[field]?.[directDependency] !== undefined
		).map((field) => ({
			field,
			file: pkg.file,
			range: pkg.json[field][directDependency],
		}))
	);

	const installedChain = walkLockfile({
		declaredIn,
		lock,
		names: names.slice(index),
	});

	const vulnerableIsDirect = index === names.length - 1;

	let parent = null;
	let parentRange = null;

	if (vulnerableIsDirect) {
		parent = declaredIn[0]?.file || null;
		parentRange = declaredIn[0]?.range || null;
	}
	else {
		const parentLink = installedChain.at(-2);
		const vulnerableLink = installedChain.at(-1);

		if (parentLink?.version && vulnerableLink?.range) {
			parent = `${parentLink.name}@${parentLink.version}`;
			parentRange = vulnerableLink.range;
		}
	}

	const scope = getScope({declaredIn, firstName: names[0], project});

	const tags = [];

	let approvers;

	if (project.class === PROJECT_CLASS_MODULES) {
		const sharedLibrary = attribution.sharedLibraries.has(directDependency);

		if (sharedLibrary) {
			tags.push(TAG_SHARED_LIBRARY);
		}

		if (sharedLibrary || names[0] === NODE_SCRIPTS_PACKAGE_NAME) {
			approvers = attribution.infraOwners;
		}
		else if (declaredIn.length) {
			approvers = unique(
				declaredIn.flatMap((declaration) =>
					declaration.file === GLOBAL_PACKAGE_JSON_FILE
						? attribution.infraOwners
						: attribution.codeOwners.getOwners(
								path.posix.dirname(declaration.file)
							)
				)
			);
		}
		else {
			approvers = attribution.codeOwners.getOwners(project.path);
		}
	}
	else {
		approvers = attribution.codeOwners.getOwners(project.path);

		if (
			declaredIn.length &&
			declaredIn.every(
				(declaration) => declaration.file === project.root.file
			) &&
			isLintSetPackage(directDependency)
		) {
			tags.push(TAG_LINT_TEMPLATE);
		}
	}

	return {
		approvers,
		declaredIn,
		directDependency,
		fix: null,
		installedChain,
		parent,
		parentRange,
		path: names,
		scope,
		tags,
	};
}

function getGHSA(url) {
	return (
		/GHSA-[0-9a-z]{4}-[0-9a-z]{4}-[0-9a-z]{4}/.exec(url || '')?.[0] || null
	);
}

/**
 * Tells whether a chain ends up in what the project ships. Chains that start
 * at a frontend SDK package (node-scripts, the lint and format plugins) are
 * build tooling, whatever field their dependencies are declared in.
 */
function getScope({declaredIn, firstName, project}) {
	const firstMember = project.membersByName.get(firstName);

	if (firstMember?.dir.startsWith(FRONTEND_SDK_PATH)) {
		return SCOPE_BUILD;
	}

	if (
		!declaredIn.length ||
		declaredIn.some((declaration) =>
			RUNTIME_FIELDS.includes(declaration.field)
		)
	) {
		return SCOPE_RUNTIME;
	}

	return SCOPE_BUILD;
}

/**
 * Follows a chain through the lockfile, from the range the declaring package
 * asks for down to the vulnerable package, recording what is installed at
 * each step. npm lockfiles resolve each dependency from its install location
 * (`children`), yarn lockfiles by `name@range`.
 */
function walkLockfile({declaredIn, lock, names}) {
	let entry = lock.getRootEntry?.(names[0]) || null;
	let range = entry ? declaredIn[0]?.range ?? null : null;

	for (const declaration of entry ? [] : declaredIn) {
		entry = lock.bySpec.get(`${names[0]}@${declaration.range}`) || null;

		if (entry) {
			range = declaration.range;

			break;
		}
	}

	const installedChain = [
		{
			name: names[0],
			range: range ?? declaredIn[0]?.range ?? null,
			version: entry?.version ?? null,
		},
	];

	for (const name of names.slice(1)) {
		const nextRange = entry?.dependencies[name] ?? null;

		entry = nextRange
			? entry.children?.[name] ||
				lock.bySpec.get(`${name}@${nextRange}`) ||
				null
			: null;

		installedChain.push({
			name,
			range: nextRange,
			version: entry?.version ?? null,
		});
	}

	return installedChain;
}
