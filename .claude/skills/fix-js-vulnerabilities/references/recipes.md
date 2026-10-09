# Fix Recipes

One recipe per fix type the report assigns. `D` is the chain's `directDependency`, `V` is the finding's `package`, and every other name refers to a field of the report's JSON (`chain.declaredIn`, `chain.fix`, `chain.parentRange`, `finding.installedVersion`), as defined in `modules/frontend-sdk/node-scripts/util/vulnerabilities/reportTypes.mjs`: `AdvisoryFinding`, `Chain`, the `Fix` variants, and `Resolution`.

Run yarn commands from the project root (the folder holding `yarn.lock`). Run npm commands in a temporary copy, as described in "npm Projects", never in place.

## npm Projects

npm walks up to the enclosing yarn workspace and fails, or touches the wrong project, when run inside `workspaces`. Change an npm lockfile in a temporary copy:

```bash
temp_dir=$(mktemp -d)

cp <project-path>/package.json <project-path>/package-lock.json "${temp_dir}"

(cd "${temp_dir}" && npm install --ignore-scripts --package-lock-only)

cp "${temp_dir}/package-lock.json" <project-path>/package-lock.json

rm -f -r "${temp_dir}"
```

Replace the `npm install` line with the command each recipe names. Copy `package.json` back too when the command edits it.

Keep the lockfile's `lockfileVersion`. When npm upgrades it (for example from 1 to 3), mention it in the commit body, because the whole file is rewritten.

## `prune-toolchain`

The project is a `tooling-only` workspace: its root `package.json` only carries the lint and format toolchain, and every finding comes from it.

1. Confirm with the user that nothing runs `eslint`, `prettier`, or `stylelint` in this workspace. Search the workspace for scripts, Gradle tasks, and CI configuration that call them before asking.

1. Delete the root `package.json` and `yarn.lock`, as `liferay-forums-workspace` and `liferay-seostudio-workspace` already do. Keep the lint configuration files (`.eslintrc.js`, `.prettierrc.js`, `.stylelintrc.js`, `copyright.js`, and the ignore files): they belong to the workspace template.

1. Run the workspace build (`references/verification.md`) and check that it still passes. The Gradle workspace plugin creates an empty `package.json` at build time when there is none. Do not commit it.

## Dead Lockfiles

A `dead-lockfile` is a `yarn.lock` or `package-lock.json` that nothing installs from. Delete it in its own commit. Before deleting a `package-lock.json`, check that no Dockerfile, script, or CI job in its folder runs `npm install` or `npm ci`.

## `re-resolve`

The range the parent declares for `V` (`chain.parentRange`) already allows a clean version (`chain.fix.target`). No `package.json` changes.

**npm:** in the temporary copy, run `npm update V --ignore-scripts --package-lock-only`.

**yarn:**

1. In `yarn.lock`, find the entry whose key list contains `V@<chain.parentRange>` and whose `version` is `finding.installedVersion`. Delete the whole entry: the key line and every indented line below it, up to the next blank line.

1. Run `yarn install`.

1. Check that the new `V` entry is at `chain.fix.target` or higher, and that the diff only touches `V` and the packages `V` itself depends on.

Deleting the entry is what makes yarn resolve the range again: `yarn install` alone keeps an entry that still satisfies the range, and `yarn upgrade` rewrites far more than one package.

In `modules/yarn.lock`, one entry is often reached by chains of several teams. Re-resolve it when at least one of the team's own chains reaches it, and record the other teams whose findings it clears. Leave the entries only other teams' chains reach (see `SKILL.md`, "Policy").

## `bump-minor`

The highest version of `D` within its current major (`chain.fix.to`) fixes the chain.

Before any bump, minor or major, compare the license of both versions (`npm view D@<from> license` and `npm view D@<to> license`). A bump that changes the license is a licensing decision, not a dependency fix: do not apply it, and escalate it to the user and frontend infrastructure. `ckeditor4` is the known case: 4.22.1 is the last open source release, and 4.23.0 and later are CKEditor 4 LTS, licensed commercially and requiring a license key.

1. In every `chain.declaredIn` file the team owns, change the range of `D` to `chain.fix.to`, keeping the range's style: an exact version stays exact (`1.2.3` becomes `1.2.9`), and a caret range stays a caret range (`^1.2.3` becomes `^1.2.9`).

1. **npm:** run `npm install --ignore-scripts --package-lock-only` in the temporary copy. **yarn:** run `yarn install`.

1. Run the report again. When `V` is still at the old version, yarn kept an existing entry that satisfies the new range: apply `re-resolve` to `V`.

In `modules`, the same library is often declared by many modules, and `yarn checkFormat` requires them all to declare the same version. For a library of `frontend-js-dependencies-web`, frontend infrastructure bumps the provider and every consumer together. For any other library, bump the team's declarations, and when the alignment check forces other teams' declarations to move too, put those in a commit of their own and name the teams so they can review it.

A minor bump can still change behavior. Run the tests of every declaring module that has a `test` script, and when a test fails, run the same tests without the bump: a failure that also happens before the bump is pre-existing, one that only happens after it is a regression. A minor bump with a regression is treated as a `bump-major`: split it into its own pull request with a migration plan (`references/major-bumps.md`) and the affected owners' approval. `moment` 2.31.0 is the known case: since 2.30.0, strict parsing with single-letter `M` or `D` tokens rejects zero-padded input.

## `bump-major`

The same steps as `bump-minor`, license check included, using `chain.fix.to`. Before changing any version, follow `references/major-bumps.md`: the migration plan and the user's approval come first.

## `resolution`

No version of `D` fixes the chain. Add a scoped resolution as a temporary fix.

**npm:** npm ignores `resolutions`. Use a scoped override in the project's `package.json` instead, `"overrides": {"<D>": {"<V>": "<value>"}}`, and run `npm install --ignore-scripts --package-lock-only` in the temporary copy. The report does not check npm overrides yet, so record the reason and removal condition in the commit body.

**yarn:**

1. Add `"<chain.fix.key>": "<chain.fix.value>"` to the `resolutions` field of the project root `package.json`, keeping keys sorted. Never use a bare package name as a key: it applies to the whole dependency tree.

1. In `modules`, also add the key to `modules/frontend-sdk/node-scripts/util/format/formatters/ALLOWED_ROOT_PACKAGE_JSON_RESOLUTIONS.mjs`, with a reason that names the advisory, the chain, and the removal condition, for example: `Holds <V> at a patched version until <D> requires <V> >= <value> on its own (GHSA-xxxx-xxxx-xxxx).` The node-scripts formatter fails on resolutions missing from this file.

1. In `workspaces`, nothing checks resolutions. Put the same reason and removal condition in the commit body and the pull request description.

1. Run `yarn install`.

When `chain.fix.outsideParentRange` is `true`, the forced version is outside the range `D`'s chain declares. Warn the user, and test the code paths that use `D` before proposing the change.

## `no-patch`

No clean release of `V` exists (`reason` is `no-clean-release` or `no-patched-range`), so neither a bump nor a resolution helps. Never decide this alone.

1. Find out what `V` is used for in the chain, and whether `D` still needs it.

1. Look for options: a maintained replacement for `D` or `V`, a newer major of `D` that drops `V`, or removing the feature that needs it.

1. Present the options to the user with their cost and risk, and follow their decision.

In `modules`, a new runtime dependency must be provided by a module through the global import map (see LPS-168443). Adding a new library as a replacement needs frontend infrastructure's approval.

When no release of a package is clean, the report marks every advisory of that package `no-patch`, even an older one that a newer version would clear. Upgrading to the newest release is still worth doing in that case: it reduces the advisories to the unpatched ones.

## Provided Libraries

In `modules`, many libraries reach the browser through the module that exports them, not through the module that declares them. `modules/node-scripts.config.js` lists, under `imports`, every provider package and the libraries it exports: `@liferay/frontend-js-react-web` exports `classnames`, `formik`, `react`, `react-dom`, and others.

When `D` is exported by a provider other than the declaring package:

- A consumer's declaration only matters for its build and types. Align it with the provider's version after the provider is bumped, not before.
- A major bump of an exported library changes what every consumer, and every client extension using the import map, gets at runtime. It always needs frontend infrastructure's approval.
- The runtime copy is the provider's, so the fix belongs in the provider's `package.json`, approved by the provider's owner. Libraries of `@liferay/frontend-js-dependencies-web` are approved by frontend infrastructure.

## Existing Resolutions

For each entry in the report's `resolutions`:

| Status or Flag | Action |
| --- | --- |
| `forced-version-vulnerable` | The forced version itself has an advisory. Update the value to a clean version, or remove the resolution when its status is `no-security-effect`. |
| `no-security-effect` | Removing it brings back no advisory. Propose removing it, and remove it only when the build and the type check pass without it: it may still pin a version for another reason, such as type compatibility. |
| `non-selective` | The key applies to the whole tree. In `modules` the formatter rejects it. In `workspaces`, propose a scoped key when the resolution stays. |
| `not-applied` | The lockfile is out of date with `package.json`. Run `yarn install`, then run the report again. |
| `obsolete` | The resolution matches nothing. Delete it, and its `ALLOWED_ROOT_PACKAGE_JSON_RESOLUTIONS.mjs` entry in `modules`. |
| `still-needed` with `releasedBy.version` | Bumping `releasedBy.directDependency` to that version makes the resolution unnecessary. Treat it as a bump, then remove the resolution. |
| `still-needed` without `releasedBy.version` | Keep it. No release of the direct dependency removes the need yet. |