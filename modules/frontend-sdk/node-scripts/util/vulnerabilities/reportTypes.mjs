/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

/**
 * The shape and meaning of the JSON report `report:vulnerabilities` writes
 * with `--format json`. This file is the contract with the consumers of the
 * report, the `fix-js-vulnerabilities` skill among them: bump
 * `SCHEMA_VERSION` whenever a field is added, removed or changes meaning.
 *
 * Paths are relative to the repository root and use `/` separators.
 */

export const SCHEMA_VERSION = 1;

/**
 * @typedef {object} Report
 * @property {number} schemaVersion Version of this shape, see `SCHEMA_VERSION`.
 * @property {string} generatedAt ISO date and time of the run, in UTC.
 * @property {string} gitCommit Abbreviated commit the run scanned.
 * @property {{node: string, npm: string, registry: string, yarn: string}} environment
 * Versions of the tools used and the npm registry the advisories came from.
 * @property {{owner: string | null, projects: string[]}} options The
 * `--owner` and `--project` filters of the run, empty when not given.
 * @property {Summary} summary Counts over the findings left after filtering.
 * @property {ProjectReport[]} projects One entry per scanned project.
 * @property {Finding[]} findings Every finding, sorted by project, type and
 * package.
 * @property {Resolution[]} resolutions Every entry of the `resolutions` field
 * of the yarn projects' root `package.json`.
 * @property {ReportError[]} errors Stages that failed. A project with errors
 * has incomplete findings, never zero findings.
 */

/**
 * @typedef {object} Summary
 * @property {number} projects Number of projects in the report.
 * @property {number} uniqueAdvisoryPackagePairs Distinct (GHSA ID, package)
 * pairs among the advisory findings, the repository-wide measure of what is
 * left to fix.
 * @property {object} findings Advisory finding counts.
 * @property {Object<string, number>} findings.byApprover Per approver handle.
 * A finding with several approvers counts once for each.
 * @property {Object<FixType, number>} findings.byFixType Per fix type.
 * @property {Object<Scope, number>} findings.byScope Per scope.
 * @property {Object<Severity, number>} findings.bySeverity Per severity.
 * @property {Object<FindingType, number>} findings.byType Every finding, not
 * only advisories, per type.
 */

/**
 * @typedef {object} ProjectReport
 * @property {string} path Project folder, the one holding the lockfile.
 * @property {ProjectClass} class How the project is built and owned.
 * @property {PackageManager} packageManager Which lockfile the project has,
 * which decides the commands that change it.
 * @property {number} members Number of workspace members.
 * @property {string[]} approvers CODEOWNERS handles owning the project folder.
 * @property {boolean} audited Whether the lockfile was scanned. When `false`,
 * the project's findings are missing, not absent.
 * @property {boolean} [inSync] Whether every declaration of the project's
 * `package.json` files has a locked entry. Missing when the scan failed.
 * @property {string[]} findings IDs of the project's findings.
 */

/**
 * @typedef {'frontend' | 'modules' | 'tooling-only'} ProjectClass `modules` is
 * built by node-scripts under `modules`, where libraries of
 * `frontend-js-dependencies-web` are approved by frontend infrastructure.
 * `frontend` is a workspace or standalone project with JS sources.
 * `tooling-only` is a workspace whose root `package.json` only carries the
 * lint and format toolchain.
 */

/**
 * @typedef {'npm' | 'yarn'} PackageManager
 */

/**
 * @typedef {AdvisoryFinding | DeadLockfileFinding | LockfileDriftFinding | LockfileOutOfSyncFinding | MembersDriftFinding | UnpinnedInstallFinding} Finding
 */

/**
 * @typedef {'advisory' | 'dead-lockfile' | 'lockfile-drift' | 'lockfile-out-of-sync' | 'members-drift' | 'unpinned-install'} FindingType
 */

/**
 * Fields every finding has.
 *
 * @typedef {object} BaseFinding
 * @property {string} id Stable ID: the same finding gets the same ID in every
 * run, so runs can be compared.
 * @property {FindingType} type What kind of finding it is.
 * @property {string} project Path of the project the finding belongs to.
 * @property {string[]} approvers CODEOWNERS handles that approve the fix, or
 * `unowned`. Frontend infrastructure appears as the owner of
 * `frontend-js-dependencies-web`.
 */

/**
 * One vulnerable installed version of a package in one project, for one
 * advisory.
 *
 * @typedef {BaseFinding & {
 *   type: 'advisory',
 *   package: string,
 *   installedVersion: string,
 *   advisory: Advisory,
 *   chains: Chain[],
 *   scope: Scope,
 *   tags: Tag[],
 *   fixType: FixType,
 * }} AdvisoryFinding `package` is the name the package is installed under,
 * which is the alias for `npm:` aliases. `scope` is `runtime` when any chain
 * is. `fixType` is the most expensive fix type among the chains, in the order
 * of `FixType`.
 */

/**
 * @typedef {object} Advisory
 * @property {string} ghsa GitHub Advisory Database ID.
 * @property {string[]} cves Always empty: the advisory database endpoint the
 * scan uses returns no CVE IDs.
 * @property {Severity} severity Severity the advisory database assigns.
 * @property {string} title One-line description of the vulnerability.
 * @property {string} url Advisory page.
 * @property {string} vulnerable Semver range of the affected versions.
 * @property {string | null} patched Always `null`: fixed versions are worked
 * out per chain, see `Fix`.
 */

/**
 * @typedef {'critical' | 'high' | 'info' | 'low' | 'moderate'} Severity
 */

/**
 * @typedef {'build' | 'runtime'} Scope `runtime` when a declaring package lists
 * the direct dependency outside `devDependencies`, so it can reach what the
 * project ships. Chains that start at a `modules/frontend-sdk` package are
 * always `build`. Scope only orders the work: no finding is accepted.
 */

/**
 * @typedef {'lint-template' | 'out-of-sync-lockfile' | 'shared-library'} Tag
 * `lint-template`: the finding comes from the lint and format toolchain a
 * workspace root declares, which every workspace fixes the same way.
 * `out-of-sync-lockfile`: the project's lockfile does not match its
 * `package.json` files, so the finding may be stale; bring the lockfile in
 * sync first. `shared-library`: the direct dependency is provided by
 * `frontend-js-dependencies-web`, so frontend infrastructure approves it.
 */

/**
 * One way the vulnerable package is reached, from a package the project
 * declares down to the vulnerable one.
 *
 * @typedef {object} Chain
 * @property {string[]} path Package names along the chain, as installed. It
 * can start with workspace members, which are skipped to find the direct
 * dependency.
 * @property {string} directDependency The first package of the chain that is
 * not a workspace member: the one a `package.json` declares and a bump
 * changes.
 * @property {Declaration[]} declaredIn The `package.json` entries that declare
 * the direct dependency.
 * @property {InstalledLink[]} installedChain What the lockfile installs at
 * each step, from the direct dependency down.
 * @property {string | null} parent The package that depends on the vulnerable
 * one (`name@version`), or the declaring `package.json` when the vulnerable
 * package is the direct dependency.
 * @property {string | null} parentRange The range the parent declares for the
 * vulnerable package.
 * @property {Scope} scope The chain's own scope.
 * @property {Fix} fix The cheapest fix the policy allows for this chain.
 */

/**
 * @typedef {object} Declaration
 * @property {string} file Path of the declaring `package.json`.
 * @property {'dependencies' | 'devDependencies' | 'optionalDependencies' | 'peerDependencies'} field
 * The field that declares the dependency.
 * @property {string} range The declared range.
 */

/**
 * @typedef {object} InstalledLink
 * @property {string} name Package name at this step.
 * @property {string | null} range Range the previous step declares for it.
 * @property {string | null} version Locked version, `null` when the step
 * could not be followed in the lockfile.
 */

/**
 * @typedef {'prune-toolchain' | 're-resolve' | 'bump-minor' | 'bump-major' | 'resolution' | 'no-patch' | 'unknown'} FixType
 * Listed from the cheapest to the most expensive.
 */

/**
 * @typedef {PruneToolchainFix | ReResolveFix | BumpFix | ResolutionFix | NoPatchFix | UnknownFix} Fix
 */

/**
 * The project is `tooling-only`: drop its toolchain.
 *
 * @typedef {{type: 'prune-toolchain'}} PruneToolchainFix
 */

/**
 * The parent's range already allows a clean version: re-resolve the lockfile,
 * no `package.json` change.
 *
 * @typedef {object} ReResolveFix
 * @property {'re-resolve'} type
 * @property {string} parent Same as `Chain.parent`.
 * @property {string} parentRange Same as `Chain.parentRange`.
 * @property {string} target Version a fresh resolution of the range picks.
 */

/**
 * Bump the direct dependency: within its caret group (`bump-minor`), or to a
 * higher one (`bump-major`, the smallest jump that fixes the chain).
 *
 * @typedef {object} BumpFix
 * @property {'bump-major' | 'bump-minor'} type
 * @property {string} from Installed version of the direct dependency.
 * @property {string} to Version to bump to.
 * @property {string} [fromMajor] Caret group bumped from (`bump-major` only),
 * such as `7` or `0.4`.
 * @property {string} [toMajor] Caret group bumped to (`bump-major` only).
 */

/**
 * No version of the direct dependency fixes the chain: add a scoped
 * resolution as a temporary fix.
 *
 * @typedef {object} ResolutionFix
 * @property {'resolution'} type
 * @property {string} key Suggested key, `<directDependency>/**\/<package>`.
 * @property {string} value Lowest clean version, inside the parent's range
 * when possible.
 * @property {boolean} outsideParentRange Whether `value` is outside the range
 * the parent declares, which can break the parent.
 */

/**
 * No clean release of the vulnerable package exists. A person decides between
 * replacing or removing it.
 *
 * @typedef {object} NoPatchFix
 * @property {'no-patch'} type
 * @property {'no-clean-release' | 'no-patched-range'} reason
 */

/**
 * The fix could not be worked out, usually because a range does not point to
 * the npm registry. Escalate it with the reason.
 *
 * @typedef {object} UnknownFix
 * @property {'unknown'} type
 * @property {string} reason
 */

/**
 * A lockfile nothing installs from: a nested lockfile inside a yarn workspace,
 * or an npm lockfile no Dockerfile installs from. Delete it.
 *
 * @typedef {BaseFinding & {type: 'dead-lockfile', file: string}} DeadLockfileFinding
 */

/**
 * Locked entries nothing reachable depends on. The next install removes them.
 *
 * @typedef {BaseFinding & {
 *   type: 'lockfile-drift',
 *   file: string,
 *   entries: string[],
 * }} LockfileDriftFinding `entries` lists them as `name@version`.
 */

/**
 * The lockfile does not match the project's `package.json` files, so a
 * frozen-lockfile install fails. Reinstall and commit before anything else.
 *
 * @typedef {BaseFinding & {
 *   type: 'lockfile-out-of-sync',
 *   file: string,
 *   missing: {field: string, file: string, name: string, range: string | null, lockRange?: string | null}[],
 *   incomplete: {entry: string, name: string, range: string}[],
 * }} LockfileOutOfSyncFinding `missing` lists declarations with no locked
 * entry, or (npm) root declarations that differ from the lockfile's
 * `lockRange`. `incomplete` lists locked entries whose own dependency has no
 * entry (yarn only).
 */

/**
 * The committed `workspaces` list of a workspace root differs from the one
 * the Gradle task `setUpYarn` will write on the next build.
 *
 * @typedef {BaseFinding & {
 *   type: 'members-drift',
 *   added: string[],
 *   removed: string[],
 * }} MembersDriftFinding Paths are relative to the workspace root.
 */

/**
 * A Dockerfile `RUN` command that installs npm packages no lockfile controls,
 * so the image gets whatever versions exist when it is built.
 *
 * @typedef {BaseFinding & {
 *   type: 'unpinned-install',
 *   file: string,
 *   command: string,
 * }} UnpinnedInstallFinding
 */

/**
 * One entry of a yarn project's root `resolutions`, and what removing it would
 * do.
 *
 * @typedef {object} Resolution
 * @property {string} project Path of the project.
 * @property {string} key The resolution key.
 * @property {string} value The forced version or range.
 * @property {ResolutionStatus} status What removing the resolution would do.
 * @property {('forced-version-vulnerable' | 'non-selective')[]} flags
 * `forced-version-vulnerable`: the forced version itself has an advisory.
 * `non-selective`: the key applies to the whole dependency tree.
 * @property {ResolutionEdge[]} edges The lockfile edges the key applies to.
 * @property {{directDependency: string | null, version: string | null} | null} releasedBy
 * For `still-needed`, the direct dependency and the version of it that would
 * make the resolution unnecessary. `version` is `null` when no release does
 * yet.
 * @property {string | null} reason The reason recorded in
 * `ALLOWED_ROOT_PACKAGE_JSON_RESOLUTIONS.mjs` (`modules` only).
 * @property {string[]} approvers CODEOWNERS handles that approve changing it.
 * @property {string} [error] Why the check failed, when `status` is `unknown`.
 */

/**
 * @typedef {'no-security-effect' | 'not-applied' | 'obsolete' | 'still-needed' | 'unknown'} ResolutionStatus
 * `obsolete`: the key matches no lockfile edge. `not-applied`: the lockfile
 * does not resolve the edges to the forced value. `still-needed`: removing it
 * brings an advisory back. `no-security-effect`: removing it brings no
 * advisory back, though it may still be needed for another reason.
 * `unknown`: the check failed.
 */

/**
 * @typedef {object} ResolutionEdge
 * @property {string} parent The lockfile entry (`name@version`) or
 * `package.json` that depends on the resolved package.
 * @property {string} parentRange The range it declares.
 * @property {string | null} lockVersion The version the lockfile resolves it
 * to.
 * @property {string | null} unforced The version a fresh resolution would
 * pick without the resolution.
 * @property {boolean | null} clean Whether `unforced` has no advisory.
 * @property {string[]} advisories GHSA IDs affecting `unforced`.
 */

/**
 * @typedef {object} ReportError
 * @property {string} project Path of the project, or of the file, that failed.
 * @property {'discover' | 'dockerfiles' | 'fix' | 'resolutions' | 'scan'} stage
 * The stage that failed.
 * @property {string} message What went wrong.
 */
