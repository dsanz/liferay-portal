# Go Generate

Runs `go generate` and fails when that changes the committed CRD, which means it has drifted from the API types that produce it. Drift has three sources: a type or a `+kubebuilder:` marker changed without a regeneration, a `go.mod` bump changed the controller-gen version stamped into the CRD, or the source formatter release `go_build.sh` uses disagrees with the one the repository resolves, as in commit `f3d6fcaa`.

## Match

`^cloud/operator/|^cloud/helm/dxp-operator/crds/`

## Command

Regenerate, then assert the working tree is unchanged:

```bash
(cd "${REPO_ROOT}/cloud/operator" && ./go_build.sh generate)
(cd "${REPO_ROOT}" && git status --porcelain)
```

A nonempty `git status --porcelain` is drift and fails this validation; the Autocommit step captures it. Report the drifted paths in the failure note, and say which of the three sources above they point to — a changed `controller-gen.kubebuilder.io/version` line is a generator bump, a reflowed `description` is formatter skew, and anything else is a stale CRD.

Only the CRD is observable here. `go generate` also rewrites `zz_generated.deepcopy.go`, which `cloud/.gitignore` excludes, so drift in the deepcopy methods never reaches `git status`; the compiler catches that one instead.

The script needs `java`, a Go toolchain, and network access; it downloads the source formatter to `${HOME}/.liferay/source-formatter` on first use. When any of those is missing the script exits nonzero — record that as a failure to run rather than as drift, since the two call for different fixes.

Do not set `LIFERAY_GO_BUILD_SKIP_SOURCE_FORMATTER=true` here. The committed CRD is source-formatter output, so skipping that step reports the whole file as drift.

## Autocommit

When `git status --porcelain` is nonempty after the regen, stage all changes (`git add --all`) and create a commit titled `<TICKET> go generate`, matching the existing convention (`LCD-52889 go generate`).

When the commit fails, record the failure and continue to the next validation.

## Notes

Run with the drift validations, **before** Source Format, so the formatter sees the regenerated CRD and owns the final bytes. Running it after would let `ant format-source-current-branch` reformat a CRD this validation just committed.

This overlaps the "Check generated files are up to date" step in `ci-reusable-test-cloud-operator.yaml`, deliberately — it surfaces the same failure locally, before the push. The overlap is not total: its caller, `ci-test-cloud-operator.yaml`, triggers only on `cloud/operator/**` and on the two workflow files themselves, so a change confined to the CRD never reaches it. The job is also gated on the `cloudnative-team/liferay-portal` repository, so it does not run everywhere the skill does.

## Time Estimate

~1-2 min, plus a source formatter download the first time, when `${HOME}/.liferay` holds no cached release.