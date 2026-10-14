# PQL Validation

Validates the PQL in `test.batch.run.property.query` properties, which would otherwise fail only when the batch runs after the merge.

## Match

`(^|/)test\.properties$`

## Command

```bash
(cd "${REPO_ROOT}/modules/test/jenkins-results-parser" && ../../../gradlew test --tests com.liferay.jenkins.results.parser.TestPropertiesPQLValidationTest)
```

`TestPropertiesPQLValidationTest` validates every `test.properties` file in the repository, not only the changed file.

## Time Estimate

~30 sec.