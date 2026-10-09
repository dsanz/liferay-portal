/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {UNOWNED} from './CodeOwners.mjs';
import {FIX_TYPES} from './assignFixTypes.mjs';
import {SCOPE_BUILD, SCOPE_RUNTIME} from './collectFindings.mjs';

export const SEVERITIES = ['critical', 'high', 'moderate', 'low', 'info'];

const OTHER_FINDING_TYPES = [
	'dead-lockfile',
	'lockfile-drift',
	'lockfile-out-of-sync',
	'members-drift',
	'unpinned-install',
];

/**
 * Renders a report as Markdown with one section per approver, so that each
 * section can be pasted into that approver's ticket.
 *
 * @param {import('./reportTypes.mjs').Report} report
 * @return {string}
 */
export default function renderMarkdown(report) {
	const lines = [
		'# JS vulnerability report',
		'',
		`Generated ${report.generatedAt} at commit \`${report.gitCommit}\`. ${report.summary.uniqueAdvisoryPackagePairs} unique (advisory, package) pairs in ${report.summary.projects} projects.`,
		'',
	];

	if (report.options.severities.length) {
		lines.push(
			`Only advisories of severity ${report.options.severities.join(', ')} are listed.`,
			''
		);
	}

	lines.push(
		`Command: \`${report.invocation.command}\`, run from \`${report.invocation.directory}\`.`,
		'',
		...renderSummary(report),
		'',
		...renderProjects(report),
		''
	);

	const projectsByPath = new Map(
		report.projects.map((project) => [project.path, project])
	);

	for (const approver of getApprovers(report)) {
		lines.push(...renderApprover({approver, projectsByPath, report}), '');
	}

	if (report.errors.length) {
		lines.push('## Errors', '');

		for (const error of report.errors) {
			lines.push(
				`- \`${error.project}\` (${error.stage}): ${escapeText(error.message)}`
			);
		}

		lines.push('');
	}

	lines.push(
		'Full chains, declaring packages and fix details are in the JSON report (`--format json`).',
		''
	);

	return lines.join('\n');
}

/**
 * Escapes a table cell. An array becomes one line per item.
 */
function escapeCell(value) {
	if (Array.isArray(value)) {
		return value.map(escapeCell).join('<br>');
	}

	return escapeText(String(value ?? '')).replace(/\|/g, '\\|');
}

function escapeText(text) {
	return String(text).replace(/\s*\n\s*/g, ' ');
}

function formatCounts(findings) {
	const parts = SEVERITIES.map((severity) => [
		severity,
		findings.filter((finding) => finding.advisory.severity === severity)
			.length,
	])
		.filter(([, count]) => count)
		.map(([severity, count]) => `${count} ${severity}`);

	const runtime = findings.filter(
		(finding) => finding.scope === SCOPE_RUNTIME
	).length;
	const build = findings.filter(
		(finding) => finding.scope === SCOPE_BUILD
	).length;

	return `${parts.join(', ') || 'no advisories'}; runtime ${runtime}, build ${build}`;
}

function formatFix(finding) {
	const fix = finding.chains
		.map((chain) => chain.fix)
		.filter(Boolean)
		.sort(
			(left, right) =>
				FIX_TYPES.indexOf(left.type) - FIX_TYPES.indexOf(right.type)
		)
		.at(-1);

	if (!fix) {
		return finding.fixType || '';
	}

	const chain = finding.chains.find((candidate) => candidate.fix === fix);

	if (fix.type === 're-resolve') {
		return `re-resolve to ${fix.target}`;
	}

	if (fix.type === 'bump-minor') {
		return `bump-minor ${chain.directDependency} ${fix.from} → ${fix.to}`;
	}

	if (fix.type === 'bump-major') {
		return `bump-major ${chain.directDependency} ${fix.from} → ${fix.to} (${fix.fromMajor} → ${fix.toMajor})`;
	}

	if (fix.type === 'resolution') {
		return `resolution "${fix.key}": "${fix.value}"${fix.outsideParentRange ? ' (outside parent range)' : ''}`;
	}

	if (fix.reason) {
		return `${fix.type} (${fix.reason})`;
	}

	return fix.type;
}

/**
 * Formats the package names along a chain, from the direct dependency down to
 * the vulnerable package. Versions are left out: the direct dependency and
 * installed columns carry the ones that matter.
 */
function formatChain(chain) {
	return chain.installedChain.map((link) => link.name).join('>');
}

/**
 * Formats the direct dependency of a chain, with the version the lockfile
 * installs, followed by the `package.json` files that declare it.
 */
function formatDirectDependency(chain) {
	const version = chain.installedChain[0]?.version;

	const directDependency = version
		? `${chain.directDependency}@${version}`
		: chain.directDependency;

	const files = [
		...new Set(chain.declaredIn.map((declaration) => declaration.file)),
	];

	return files.length
		? `${directDependency} (${files.join(', ')})`
		: directDependency;
}

/**
 * Returns a finding as one approver sees it: only the chains that approver
 * approves, and the scope those chains give it. A finding shared by several
 * teams would otherwise show each team the direct dependencies, fixes and
 * scope of the others.
 */
function getApproverView(finding, approver) {
	const chains = finding.chains.filter((chain) =>
		chain.approvers.includes(approver)
	);

	return {
		...finding,
		chains,
		scope: chains.some((chain) => chain.scope === SCOPE_RUNTIME)
			? SCOPE_RUNTIME
			: SCOPE_BUILD,
	};
}

function getApprovers(report) {
	if (report.options.owner) {
		return [report.options.owner];
	}

	const approvers = new Set();

	for (const item of [...report.findings, ...report.resolutions]) {
		for (const approver of item.approvers) {
			approvers.add(approver);
		}
	}

	return [...approvers].sort((left, right) => {
		if (left === UNOWNED) {
			return 1;
		}

		if (right === UNOWNED) {
			return -1;
		}

		return left.localeCompare(right);
	});
}

/**
 * Returns the severities a report lists: those of `--severity`, or all of them.
 */
function getListedSeverities(report) {
	const {severities} = report.options;

	return severities.length
		? SEVERITIES.filter((severity) => severities.includes(severity))
		: SEVERITIES;
}

/**
 * Renders the totals of the report as a table whose rows are always the same
 * and in the same order, zeros included, so that the summaries of two runs can
 * be compared line by line to track progress. With `--severity`, only the
 * listed severities get rows, and the lockfile and Dockerfile findings, which
 * have no severity, are left out of the table.
 */
function renderSummary(report) {
	const {findings, projects, uniqueAdvisoryPackagePairs} = report.summary;

	const count = (counts, key) => counts[key] || 0;

	const filtered = !!report.options.severities.length;

	const rows = [
		['Vulnerability findings', count(findings.byType, 'advisory')],
		['Unique (advisory, package) pairs', uniqueAdvisoryPackagePairs],
		...getListedSeverities(report).map((severity) => [
			`Severity: ${severity}`,
			count(findings.bySeverity, severity),
		]),
		['Scope: runtime', count(findings.byScope, SCOPE_RUNTIME)],
		['Scope: build', count(findings.byScope, SCOPE_BUILD)],
		...FIX_TYPES.map((fixType) => [
			`Fix: ${fixType}`,
			count(findings.byFixType, fixType),
		]),
		...(filtered ? [] : OTHER_FINDING_TYPES).map((type) => [
			`Other: ${type}`,
			count(findings.byType, type),
		]),
		['Resolutions', report.resolutions.length],
		['Projects', projects],
		['Errors', report.errors.length],
	];

	return [
		'## Summary',
		'',
		'| Measure | Count |',
		'| --- | --- |',
		...rows.map(([label, value]) => `| ${label} | ${value} |`),
	];
}

/**
 * Renders one row per project, sorted by path, under a grand total row, so
 * that the state of every project can be seen at a glance and compared
 * between runs.
 */
function renderProjects(report) {
	const rows = report.projects
		.map((project) => project.path)
		.sort()
		.map((projectPath) =>
			getProjectCounts(
				projectPath,
				report.findings.filter(
					(finding) => finding.project === projectPath
				)
			)
		);

	const total = getProjectCounts('**Total**', report.findings);

	const filtered = !!report.options.severities.length;

	const columns = [
		'Project',
		'Findings',
		'Unique',
		...getListedSeverities(report).map(
			(severity) => severity[0].toUpperCase() + severity.slice(1)
		),
		'Runtime',
		...(filtered ? [] : ['Other']),
	];

	return [
		'## Projects',
		'',
		`| ${columns.join(' | ')} |`,
		`| ${columns.map(() => '---').join(' | ')} |`,
		...[total, ...rows].map(
			(row) => `| ${columns.map((column) => row[column]).join(' | ')} |`
		),
		'',
		filtered
			? '"Unique" counts distinct (advisory, package) pairs, and "Runtime" the findings that can reach shipped code. Lockfile and Dockerfile findings have no severity, so they are only listed in the sections below.'
			: '"Unique" counts distinct (advisory, package) pairs, "Runtime" the findings that can reach shipped code, and "Other" the lockfile and Dockerfile findings.',
	];
}

function getProjectCounts(label, findings) {
	const advisories = findings.filter(
		(finding) => finding.type === 'advisory'
	);

	const bySeverity = (severity) =>
		advisories.filter((finding) => finding.advisory.severity === severity)
			.length;

	return {
		Critical: bySeverity('critical'),
		Findings: advisories.length,
		High: bySeverity('high'),
		Info: bySeverity('info'),
		Low: bySeverity('low'),
		Moderate: bySeverity('moderate'),
		Other: findings.length - advisories.length,
		Project: label.startsWith('**') ? label : `\`${label}\``,
		Runtime: advisories.filter((finding) => finding.scope === SCOPE_RUNTIME)
			.length,
		Unique: new Set(
			advisories.map(
				(finding) => `${finding.advisory.ghsa}|${finding.package}`
			)
		).size,
	};
}

function renderApprover({approver, projectsByPath, report}) {
	const findings = report.findings.filter((finding) =>
		finding.approvers.includes(approver)
	);
	const resolutions = report.resolutions.filter((resolution) =>
		resolution.approvers.includes(approver)
	);

	const advisoryFindings = findings
		.filter((finding) => finding.type === 'advisory')
		.map((finding) => getApproverView(finding, approver));

	const projectPaths = [
		...new Set([
			...findings.map((finding) => finding.project),
			...resolutions.map((resolution) => resolution.project),
		]),
	].sort();

	const lines = [
		`## ${approver} — ${advisoryFindings.length} findings in ${projectPaths.length} projects (${formatCounts(advisoryFindings)})`,
		'',
	];

	for (const projectPath of projectPaths) {
		const project = projectsByPath.get(projectPath);

		const projectLabel = project
			? [project.class, project.packageManager].join(', ')
			: '';

		lines.push(
			`### ${projectPath}${projectLabel ? ` (${projectLabel})` : ''}`,
			''
		);

		const projectAdvisories = advisoryFindings
			.filter((finding) => finding.project === projectPath)
			.sort(compareFindings);

		if (projectAdvisories.length) {
			lines.push(
				`Findings: ${projectAdvisories.length} (${formatCounts(projectAdvisories)})`,
				'',
				'| Package | Installed | Advisory | CVE | Severity | Scope | Fix | Direct dependency (declared in) | Chain | Id |',
				'| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |'
			);

			for (const finding of projectAdvisories) {
				const chains = [...finding.chains].sort(compareChains);

				const directDependencies = [
					...new Set(chains.map(formatDirectDependency)),
				];
				const chainNames = [...new Set(chains.map(formatChain))];

				lines.push(
					`| ${[
						finding.package,
						finding.installedVersion,
						finding.advisory.ghsa,
						finding.advisory.cves.join(', '),
						finding.advisory.severity,
						finding.scope,
						formatFix(finding),
						directDependencies,
						chainNames,
						finding.id,
					]
						.map(escapeCell)
						.join(' | ')} |`
				);
			}

			lines.push('');
		}

		const projectResolutions = resolutions.filter(
			(resolution) => resolution.project === projectPath
		);

		if (projectResolutions.length) {
			lines.push(
				'| Resolution | Value | Status | Flags | Released by |',
				'| --- | --- | --- | --- | --- |'
			);

			for (const resolution of projectResolutions) {
				const releasedBy = resolution.releasedBy?.directDependency
					? `${resolution.releasedBy.directDependency}${resolution.releasedBy.version ? `@${resolution.releasedBy.version}` : ' (no release yet)'}`
					: '';

				lines.push(
					`| ${[
						resolution.key,
						resolution.value,
						resolution.status,
						resolution.flags.join(', '),
						releasedBy,
					]
						.map(escapeCell)
						.join(' | ')} |`
				);
			}

			lines.push('');
		}

		const others = findings.filter(
			(finding) =>
				finding.project === projectPath && finding.type !== 'advisory'
		);

		for (const finding of others) {
			if (finding.type === 'dead-lockfile') {
				lines.push(
					`- Dead lockfile \`${finding.file}\`: yarn never installs from it. Delete it.`
				);
			}
			else if (finding.type === 'unpinned-install') {
				lines.push(
					`- Unpinned install in \`${finding.file}\`: \`${finding.command}\``
				);
			}
			else if (finding.type === 'lockfile-drift') {
				lines.push(
					`- Lockfile drift in \`${finding.file}\`: ${finding.entries.length} entries nothing depends on (${finding.entries.slice(0, 5).join(', ')}${finding.entries.length > 5 ? ', …' : ''}). The next install removes them.`
				);
			}
			else if (finding.type === 'lockfile-out-of-sync') {
				const declarations = [
					...finding.missing.map(
						(item) =>
							`\`${item.name}@${item.range}\` (${item.field} of \`${item.file}\`)`
					),
					...finding.incomplete.map(
						(item) =>
							`\`${item.name}@${item.range}\` (dependency of \`${item.entry}\`)`
					),
				];

				lines.push(
					`- **Lockfile out of sync** \`${finding.file}\`: a frozen-lockfile install fails, and the findings above come from a lockfile that does not match \`package.json\`. Reinstall and commit before fixing anything else. Not in the lockfile: ${declarations.slice(0, 10).join(', ')}${declarations.length > 10 ? `, and ${declarations.length - 10} more` : ''}.`
				);
			}
			else if (finding.type === 'members-drift') {
				lines.push(
					`- Workspace members drift: Gradle will add ${finding.added.length ? finding.added.map((dir) => `\`${dir}\``).join(', ') : 'nothing'} and remove ${finding.removed.length ? finding.removed.map((dir) => `\`${dir}\``).join(', ') : 'nothing'}.`
				);
			}
		}

		if (others.length) {
			lines.push('');
		}
	}

	return lines;
}

/**
 * Groups chains by their first two packages, so that every chain through the
 * same dependency of a direct dependency (like `d3>d3-interpolate`) goes
 * together, and puts the shortest chains of each group first.
 */
function compareChains(left, right) {
	const group = (chain) =>
		chain.installedChain
			.slice(0, 2)
			.map((link) => link.name)
			.join('>');

	return (
		left.directDependency.localeCompare(right.directDependency) ||
		group(left).localeCompare(group(right)) ||
		left.installedChain.length - right.installedChain.length ||
		formatChain(left).localeCompare(formatChain(right))
	);
}

function compareFindings(left, right) {
	return (
		SEVERITIES.indexOf(left.advisory.severity) -
			SEVERITIES.indexOf(right.advisory.severity) ||
		(left.scope === right.scope
			? 0
			: left.scope === SCOPE_RUNTIME
				? -1
				: 1) ||
		left.package.localeCompare(right.package) ||
		left.installedVersion.localeCompare(right.installedVersion)
	);
}
