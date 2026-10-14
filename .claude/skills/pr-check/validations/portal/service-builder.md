# Service Builder

Regenerates Service Builder output and commits whatever has drifted. It runs when an input such as `service.xml` or a hand written `*Impl.java` changes, when the branch changes generated output without its input, and when the generator itself changes, since that alters the output of every module.

## Match

`(^|/)service\.xml$|(^|/)service\.properties$|/META-INF/module-hbm\.xml$|/META-INF/portlet-model-hints\.xml$|/META-INF/sql/[^/]+\.sql$|/model/impl/[^/]+Impl\.java$|/service/impl/[^/]+Impl\.java$|(BaseImpl|CacheModel|LocalService|LocalServiceUtil|LocalServiceWrapper|ModelArgumentsResolver|ModelImpl|Persistence|PersistenceConstants|PersistenceImpl|ServiceBaseImpl|ServiceHttp)\.java$|/model/[^/]+Wrapper\.java$|/service/[^/]+Service\.java$|/service/[^/]+ServiceUtil\.java$|/service/[^/]+ServiceWrapper\.java$|/service/persistence/[^/]+Util\.java$|^sql/indexes\.sql$|^modules/util/portal-tools-service-builder/`

## Command

When the only Service Builder path the branch changed is a hand edited `*ModelImpl.java`, with no input beside it, regenerate nothing. Report **NOT VERIFIED** and flag the file, since the next regeneration overwrites a hand edit to generated output.

```bash
(cd "${REPO_ROOT}/portal-impl" && ant build-services)
```

## Autocommit

After the regen, check for drift with `git diff --quiet` over the regen output paths. When the check shows drift, stage all changes (`git add --all`) and create a commit titled `<TICKET> buildServices`.

When the commit fails, record the failure and continue to the next validation.

## Time Estimate

~1-2 min.