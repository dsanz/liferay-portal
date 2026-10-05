/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {UNOWNED} from './CodeOwners.mjs';
import {FIX_TYPES} from './assignFixTypes.mjs';
import {SCOPE_BUILD, SCOPE_RUNTIME} from './collectFindings.mjs';

export const SEVERITIES = ['critical', 'high', 'moderate', 'low', 'info'];

/**
 * Renders a report as Markdown with one section per approver, so that each
 * section can be pasted into that approver's ticket.
 */
export default function renderMarkdown(report) {
	const lines = [
		'# JS vulnerability report',
		'',
		`Generated ${report.generatedAt} at commit \`${report.gitCommit}\`. ${report.summary.uniqueAdvisoryPackagePairs} unique (advisory, package) pairs in ${report.summary.projects} projects.`,
		'',
	];

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

function escapeCell(text) {
	return escapeText(String(text ?? '')).replace(/\|/g, '\\|');
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

function renderApprover({approver, projectsByPath, report}) {
	const findings = report.findings.filter((finding) =>
		finding.approvers.includes(approver)
	);
	const resolutions = report.resolutions.filter((resolution) =>
		resolution.approvers.includes(approver)
	);

	const advisoryFindings = findings.filter(
		(finding) => finding.type === 'advisory'
	);

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
				'| Package | Installed | Advisory | Severity | Scope | Fix | Direct dependency | Id |',
				'| --- | --- | --- | --- | --- | --- | --- | --- |'
			);

			for (const finding of projectAdvisories) {
				const directDependencies = [
					...new Set(
						finding.chains.map((chain) => chain.directDependency)
					),
				];

				lines.push(
					`| ${[
						finding.package,
						finding.installedVersion,
						finding.advisory.ghsa,
						finding.advisory.severity,
						finding.scope,
						formatFix(finding),
						directDependencies.join(', '),
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
