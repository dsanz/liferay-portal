# Remove Unused Dependencies

Check every `dependencies` and `devDependencies` entry of the `package.json` files the team owns in the project: the project root and, for workspaces and `modules`, each member package.

## Find Uses

A dependency `N` is used when any of these appears in its package folder (skip `build`, `classes`, and `node_modules`):

- **Configuration:** `.babelrc`, `.eslintrc*`, `babel.config.*`, `client-extension.yaml`, `jest.config.*`, `node-scripts.config.js`, `postcss.config.*`, `tsconfig*.json` (`paths`, `types`), `vite.config.*`, and `webpack.config.*`.
- **Java, JSP, and FreeMarker:** in `modules`, server-side code can reference npm modules by name (`NPMResolver`, `resolveModuleName`, `module=` attributes on taglibs). Search the module's `src/main/java` and `src/main/resources` too.
- **JS and TS:** `from 'N'`, `from 'N/…'`, `import 'N'`, `import('N')`, `jest.mock('N')`, and `require('N')`.
- **Scripts:** a `scripts` entry calling one of `N`'s binaries. `npm view N bin` lists them.
- **Styles:** `@import`, `@use`, or `url()` pointing at `N` or `~N`.

Search with `git grep` limited to the package folder, and account for scoped names and subpath imports.

## Keep Without a Direct Use

Some entries look unused but are not:

- `@types/X` when `X` is used.
- In `modules`, every library the package exports (its `node-scripts.config.js` `exports`, or its entry under `imports` in `modules/node-scripts.config.js`). Other modules import it at runtime.
- Internal `@liferay/*` packages declared with `*`: they are workspace links that the build and the import map rely on.
- Peer dependencies of a library the package uses (`npm view <library> peerDependencies`).
- Toolchain entries the build itself calls (`@liferay/node-scripts`, `typescript`, and the lint and format plugins) when a script runs them.

When unsure, keep the entry and ask the user.

## Check the Field

An entry in the wrong field is fixed here too:

- Used by code that ships (under `src/main`, or a client extension's source) → `dependencies`.
- Used only by tests, configuration, or scripts → `devDependencies`.

In `modules`, a module's `dependencies` must be provided by a module through the global import map (see LPS-168443), and the node-scripts formatter fails otherwise. Ask before moving an entry into `dependencies` there.

## Apply

1. Remove the unused entries and fix the misplaced ones.

1. Reinstall: `yarn install` at the project root, or the npm procedure from `references/recipes.md`.

1. Build and test (`references/verification.md`).

1. Commit the pruning on its own, then run the report again: removed dependencies take their findings with them.