# Instance Wrapper Build

Regenerates the instance wrappers declared in `instance_wrappers.xml` and commits whatever has drifted from them.

## Match

`instance_wrappers\.xml$`

## Command

```bash
(cd "${REPO_ROOT}/portal-impl" && ant build-iw)
```

## Autocommit

After the regen, check for real drift with `git diff --quiet` excluding `*-portlet-service.jar`, `packageinfo`, `service.properties`, `yarn.lock` (these always fluctuate). When the check shows drift, stage all changes (`git add --all`) and create a commit titled `<TICKET> buildIW`.

When the commit fails, record the failure and continue to the next validation.

## Time Estimate

~30 sec.