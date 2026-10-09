# Major Bumps

A `bump-major` fix moves a direct dependency `D` from `chain.fix.from` to `chain.fix.to`, across the majors `chain.fix.fromMajor` to `chain.fix.toMajor`. The report picks the smallest major jump that fixes the chain. Plan the migration before changing anything.

## Gather the Breaking Changes

1. Find where `D` publishes its changes: `npm view D homepage repository.url`.

1. Read the changelog or release notes of every major after `fromMajor` up to and including `toMajor`: the repository's `CHANGELOG.md`, its GitHub releases, and any migration or upgrade guide. Fetch the pages. Never rely on memory for a library's breaking changes.

1. List each breaking change in one line: what changed, and what replaces it.

1. Check the new version's `engines` and `peerDependencies` (`npm view D@<to> engines peerDependencies`). A new peer requirement, React for example, can force further bumps.

## Map Them to the Code

For each breaking change, search the packages that declare `D` (`chain.declaredIn`) for the affected API: imports, calls, options, configuration keys, and CLI flags in `scripts`. Record the file and line of every hit. A breaking change with no hit needs no work, so say so.

When `D` is only a build or test tool (`scope` is `build`), the code to check is configuration and scripts, not shipped sources.

## Write the Plan

Present the plan to the user before editing anything:

- A risk rating (low, medium, or high) with a one-line reason.
- Codemods the library provides, if any.
- How the change will be verified: which builds, unit tests, and Playwright tests (`references/verification.md`).
- One row per breaking change that hits the code: the change, the affected files, and the planned edit.
- The bump, and the findings it clears (their `id`s).

Ask the user to approve the plan, adjust it, or defer the bump. When the user defers it, propose a `resolution` for the affected chains as a temporary fix, with the bump as its removal condition.

## Apply

1. Bump `D` as `bump-minor` describes in `references/recipes.md`.

1. Apply the planned code changes.

1. Verify, including the Playwright tests of the affected UI when `scope` is `runtime`.

1. Commit the bump and its migration together, on their own, so that they can be reverted alone. Name the majors and the advisories cleared in the commit body.

## Exported Libraries

When `D` is exported to other modules (see `references/recipes.md`, "Provided Libraries"), a major bump changes what every consumer gets at runtime, including client extensions that use the import map. Do not apply it. Write the plan and hand it to frontend infrastructure.