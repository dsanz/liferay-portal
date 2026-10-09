---

allowed-tools: [AskUserQuestion, Bash, Edit, Glob, Grep, Read, Skill, WebFetch, Write]
argument-hint: '<project-path> [<owner>]'
description: Resolve the known vulnerabilities of a JS project's dependencies (a yarn or npm project under modules or workspaces) following the frontend infrastructure remediation policy. Use when asked to fix, resolve, or remediate JS vulnerabilities, a vulnerability report, or vulnerable npm dependencies.
name: fix-js-vulnerabilities

---

# Fix JS Vulnerabilities

Resolve the vulnerabilities reported for one JS project, following the policy frontend infrastructure applies to every team. The `report:vulnerabilities` tool decides what is vulnerable, who approves each fix, and which fix the policy calls for. This skill applies those fixes, verifies them, and runs the tool again until nothing the team owns is left.

## Policy

Every team follows these rules. Do not trade them for convenience.

- No finding is accepted. A finding is either fixed, held by a documented resolution, or escalated, whatever its severity or scope. Scope (`runtime` or `build`) only orders the work.
- Fix through a direct dependency whenever possible, even when that means a major bump. A resolution is a temporary fix of last resort, and every resolution states when it can be removed.
- Prefer the cheapest fix that clears a finding, in this order: remove an unused dependency, re-resolve the lockfile, bump within the major, bump across majors, add a resolution.
- Only change what the team approves. A finding whose approvers do not include the team's owner is reported, not fixed.
- In a lockfile several owners share (`modules/yarn.lock`), the team may re-resolve any entry one of its own chains reaches, even when other teams' chains reach it too: re-resolving stays within every declared range. Entries only other teams' chains reach are left to them. The summary lists the other teams whose findings clear as a side effect.
- Libraries in `frontend-js-dependencies-web`, chains that start at `@liferay/node-scripts`, and declarations in `modules/package.json` are approved by frontend infrastructure. Any other library needs its owner's approval.
- How to split the work into pull requests is the team's choice. Recommend a split, never impose one.

## Input

Read `${ARGUMENTS}`:

- **Project Path** — required. A folder with a tracked `yarn.lock` or `package-lock.json` that something installs from, relative to the current directory or to the repository root: `modules`, `workspaces/liferay-sample-workspace`, `modules/apps/frontend-js/frontend-js-clay-web/clay/www`.
- **Owner** — optional. The team's CODEOWNERS handle, such as `@liferay-commerce`. When omitted, read `.github/CODEOWNERS` for the project folder: use the handle when exactly one owner matches, and ask the user otherwise. For `modules`, always ask, because every module has its own owner. When the project is `unowned` (most of `workspaces` today), ask the user which handle to act under, and pass `unowned` to the tool.

## Prerequisites

- `node` 18 or later, `git`, `yarn` 1.x, and `npm` are on `PATH`, and the npm registry is reachable.
- The current branch is not `master`. When it is, ask for the Jira ticket and run the `start-work` skill, or create a branch named after the ticket.
- The working tree is clean. Abort when it has uncommitted changes.

## Run the Report

Run it from the repository root:

```bash
report_file="$(mktemp -d)/report.json"

node \
	modules/frontend-sdk/node-scripts/report/vulnerabilities.mjs \
	--format json \
	--output "${report_file}" \
	--owner "<owner>" \
	--project "<project-path>"
```

A workspace takes about 10 seconds and `modules` about a minute.

When the user wants to work on the most severe findings first, add `--severity critical,high` (a comma separated list of `critical`, `high`, `moderate`, `low`, `info`). It only filters advisory findings, so lockfile and Dockerfile findings still show. Run the final report of the work without it: the work is only done when no finding of any severity is left. Exit code 2 means some stage failed: read `errors` and retry once before reporting the failure to the user.

Read the JSON. Every field, its type, and what it means are defined as JSDoc types in `modules/frontend-sdk/node-scripts/util/vulnerabilities/reportTypes.mjs`. Read that file before interpreting a report, and rely on its definitions rather than guessing from field names: they say, for example, which scope is shipped code, why `cves` is empty, and what each fix type and resolution status asks for.

Stop and tell the user when the report's `schemaVersion` differs from `SCHEMA_VERSION` in that file, because this skill was written for the shape it defines.

Summarize for the user, in a short table, the findings by `fixType` and by `advisory.severity`, the resolutions by `status`, and every non-advisory finding (`dead-lockfile`, `lockfile-drift`, `lockfile-out-of-sync`, `members-drift`, `unpinned-install`). Keep the report file: later steps compare finding `id`s against it.

The report reads each lockfile as the dependency tree its `package.json` files declare, members' `devDependencies` included, and checks every reachable version against the GitHub Advisory Database. Findings carry GHSA IDs only, not CVE IDs.

## Bring the Lockfile in Sync

When the report has a `lockfile-out-of-sync` finding for the project, stop before anything else: a frozen-lockfile install fails, and every finding of the project was read from a lockfile that does not match its `package.json` files (they carry the `out-of-sync-lockfile` tag).

1. Reinstall: `yarn install` at the project root, or the npm procedure from `references/recipes.md`, "npm Projects".

1. Check that the lockfile now has an entry for every declaration the finding listed under `missing` and `incomplete`.

1. Commit the lockfile on its own, then run the report again.

A `lockfile-drift` finding lists entries that nothing depends on. The next install removes them, so the reinstall above, or any later fix, clears it. It needs no commit of its own.

## Route What the Team Does Not Own

Before changing anything, list the findings the team must not fix and tell the user who owns them.

In `modules`, judge ownership per chain, not per finding: a finding's `approvers` merges the owners of all its chains, so a single lockfile entry reached from many modules lists many teams. A chain belongs to frontend infrastructure when it starts at a `modules/frontend-sdk` package, is declared in `modules/package.json`, or goes through a library of `frontend-js-dependencies-web`; otherwise it belongs to the CODEOWNERS owner of the module that declares its direct dependency. The team acts on the chains it owns.


- Findings tagged `shared-library` — frontend infrastructure owns `frontend-js-dependencies-web`.
- Findings whose `fixType` is `unknown` — escalate them to frontend infrastructure with the chain's `fix.reason`.
- In `modules`, findings whose direct dependency another module provides. Read `modules/node-scripts.config.js`: its `imports` map lists, for each provider package, the libraries it exports. When the direct dependency appears under a provider other than the declaring package, the runtime copy is the provider's, so the fix belongs to the provider's owner (see `references/recipes.md`, "Provided Libraries").
- `unpinned-install` findings — they are tracked as their own ticket in the remediation effort. Mention them, do not fix them.

## Remove Unused Dependencies

Prune the project's `package.json` files before fixing anything else, because removed dependencies take their findings with them. Follow `references/prune.md`. Commit the pruning on its own, then run the report again.

## Plan the Work

Group the remaining findings the team owns by fix type and direct dependency. A single bump often clears many findings at once, so plan by direct dependency, not by finding.

Recommend a pull request split, for example one pull request for every `prune-toolchain`, `re-resolve`, and `bump-minor` fix, and one pull request per `bump-major`. Ask the user to accept the split or describe their own, then follow it.

## Apply the Fixes

Work from the cheapest fix type to the most expensive, following `references/recipes.md` for each one and the project's `packageManager` (`npm` or `yarn`):

1. `prune-toolchain`

1. Dead lockfiles

1. `re-resolve`

1. `bump-minor`

1. `bump-major` (also follow `references/major-bumps.md`)

1. `resolution`

1. `no-patch`

After each fix type, verify (next section) and run the report again. Fixes interact: a bump can turn a later `resolution` into a `re-resolve`, or clear it. Always act on the latest report, never on a stale one.

Then handle the existing resolutions the report lists (see `references/recipes.md`, "Existing Resolutions").

## Verify

After every batch of fixes:

1. Review the lockfile diff. It must only touch the packages the batch meant to change. Revert and redo a batch whose lockfile diff moves unrelated packages.

1. Build and test the affected packages, following `references/verification.md`.

1. Run the report again and compare finding `id`s with the previous run: list what was fixed, what is still there, and anything new. A finding `id` includes the installed version, so tell the new ones apart before acting on them:

	- The same advisory on the same package at another version is carried over: a fix moved the version but not out of the advisory's range. It is still pending, not a regression.
	- A finding at a version the lockfile already held before the batch comes from the advisory database, which changes between runs (advisories get published, and the endpoint has been seen to omit and later return a package's advisories). Add it to the plan and move on.
	- Only a finding at a version the batch locked for the first time means the fix pulled in a vulnerable version. Fix it before moving on.

The work is done when the report has no advisory findings for the owner in the project, apart from escalated ones.

## Commit

Follow `.claude/rules/commit.md`. Commit each fix type on its own and each major bump on its own, so any of them can be reverted alone. Never mix a lockfile regeneration with unrelated source changes.

## Report Back

End with a short summary for the user:

- Findings escalated or routed, with their owner and the reason.
- Findings fixed, by fix type, with the before and after counts from the report.
- `no-patch` findings and the decision taken or still pending for each.
- Resolutions added, each with its key, value, reason, and removal condition.
- The pull requests or commits created.

Paste the owner's Markdown section of a final run (`--format md`) into the pull request description.