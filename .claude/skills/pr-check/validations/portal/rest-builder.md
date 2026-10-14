# REST Builder

Regenerates REST Builder output and commits whatever has drifted. It runs when a `rest-config.yaml` or `rest-openapi.yaml` changes, when the branch changes generated output without its input, and when the generator itself changes, since that alters the output of every module.

## Match

`/rest-config\.yaml$|/rest-openapi\.yaml$|/dto/v[0-9_]+/[^/]+\.java$|SerDes\.java$|OpenAPIResource[^/]*\.java$|Base[^/]*ResourceTestCase\.java$|^modules/util/portal-tools-rest-builder/`

## Command

Run REST Builder for every module, as described in the "Every Module" section of `.claude/rules/rest-builder.md`.

## Autocommit

After the regen, check for drift with `git diff --quiet` over the regen output paths. When the check shows drift, stage all changes (`git add --all`) and create a commit titled `<TICKET> buildREST`.

When the commit fails, record the failure and continue to the next validation.

## Time Estimate

~1 min.