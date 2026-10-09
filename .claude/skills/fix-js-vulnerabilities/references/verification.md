# Verification

Verify every batch of fixes before moving on: the lockfile, the build, the tests, then the report.

## Lockfile

- **npm:** run `npm ci --ignore-scripts` in a temporary copy of `package.json` and `package-lock.json` (see `references/recipes.md`, "npm Projects").
- **yarn:** run `yarn install --frozen-lockfile` at the project root. It fails when `yarn.lock` and the `package.json` files disagree, which is what the Gradle workspace build and CI run.

Then read `git diff` of the lockfile: only the packages the batch meant to change, and their own dependencies, may move. Check it with the lockfile model, not by eye: every version the batch adds must be a targeted package or reachable from one. A large diff is normal when a bump drops a whole tree (for example `@liferay/amd-loader` 5.4.4 removed 234 versions).

Check that each target reached its fixed version by package name and version, not by its old `name@range` key: when a parent moves to a newer version, it can ask for a different range, and the old key disappears even though the package is at the fixed version.

To compare yarn's peer dependency warnings before and after a batch, run the same command on both sides, `yarn install --check-files --ignore-scripts`: a frozen install over an existing `node_modules` prints no warnings at all, so it cannot be compared with anything.

## Build and Test

### `modules`

For every module whose `package.json` changed, and for the modules that import a provider whose exported library changed:

```bash
cd <module-root> && <gradlew> deploy
cd <module-root> && yarn test
```

Run `yarn test` only when the module's `package.json` has a `test` script, and never in `modules/test/playwright`, whose `test` script runs the Playwright end-to-end suite against a running portal. When a deployed bundle looks stale, rebuild with `<gradlew> clean deploy`.

Two traps make a build prove nothing:

- Gradle's build cache does not key on `modules/yarn.lock`, so after a lockfile change `packageRunBuild` reports `FROM-CACHE` or `UP-TO-DATE` and reuses an old bundle. Verify with `<gradlew> clean packageRunBuild --no-build-cache` and check that the log lists the esbuild bundles.
- `yarn build` alone fails with `ENOENT … .sass-cache` in modules with Sass, because Gradle compiles the Sass first. That failure says nothing about the dependency change; build those modules through Gradle.

When `deploy` fails in Java compilation of an unrelated module (for example a missing method of `portal-kernel`), the local portal snapshot is older than the branch. Run `ant deploy install-portal-snapshot` in `portal-kernel`, or verify the JavaScript with `<gradlew> clean packageRunBuild -x compileJava --no-build-cache`. Never commit a batch whose build failed before finding out why.

Then run the global checks from `modules`, which include the `resolutions` check and the `package.json` policies:

```bash
cd modules && yarn checkFormat
```

For changes to the toolchain itself (chains that start at `@liferay/node-scripts`, or `modules/package.json`), deploy a few representative modules that use the changed tool, for example one React module and one Sass-heavy module.

For a `bump-major` with `runtime` scope, also run the Playwright tests of the affected UI against a deployed bundle (see the repository's `CLAUDE.md`, "Functional Tests").

### Clay Projects

`modules/apps/frontend-js/frontend-js-clay-web/clay/www` and `modules/apps/frontend-js/frontend-js-clay-web/clay/storybook` are standalone yarn projects. Run their own build scripts from the project root: `yarn build` for `www`, and `yarn build-storybook` for `storybook`.

### `workspaces`

Run the workspace build from the workspace root. It runs one `yarn install --frozen-lockfile` at the root and builds every client extension:

```bash
cd workspaces/<workspace> && ./gradlew build
```

To build a single client extension, run `./gradlew :client-extensions:<name>:build`. Run `yarn test` in a client extension folder when its `package.json` has a `test` script.

The Gradle task `setUpYarn` rewrites the workspace root `package.json` on every build, so check `git status` after building and leave only the batch's own changes.

For a bump of a build tool (Vite, its plugins), compare the client extension's output folder before and after: the same file names (the client extension's `client-extension.yaml` points to them) and sizes within a few percent.

Vite builds do not type-check. Run the client extension's own TypeScript, `node_modules/.bin/tsc -p tsconfig.app.json --noEmit` from the client extension folder, and compare the error count before and after the batch. The `tsc` hoisted at the workspace root belongs to the lint and format toolchain (TypeScript 4) and fails on a TypeScript 5 configuration, which proves nothing.

For an npm project consumed by a Dockerfile (such as `liferay-sample-etc-node`), also build the image when Docker is available (`docker build <project-path>`), and start it when the change touched a runtime dependency.

### Other npm Projects

Run the project's own scripts in place. For `modules/test/poshi/poshi-vscode`, that is `npm ci`, `npm run compile`, and `npm test`.

## Report

Run the report again with the same `--owner` and `--project` and compare finding `id`s with the previous run. Fixed findings are gone. A new `id` is a regression only when its version is one the batch locked for the first time; carried-over advisories and advisory database changes are not (see `SKILL.md`, "Verify").

A `lockfile-out-of-sync` finding after a batch means the batch edited a `package.json` or the lockfile without reinstalling. Reinstall before anything else (see `SKILL.md`, "Bring the Lockfile in Sync").

## Source Formatter

After committing, the repository's commit rule runs `ant format-source-current-branch`. It checks every file committed since `origin/master`, so on a branch that is behind, it reports other teams' issues and rewrites their files. Keep only its changes to the batch's own files, revert the rest (`git checkout -- <file>`), and mention the unrelated issues instead of fixing them.