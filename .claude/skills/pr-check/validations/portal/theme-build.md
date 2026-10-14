# Theme Build

Rebuilds every theme when a shared stylesheet changes, since a change to `clay-css` or to the `frontend-theme-styled` and `frontend-theme-unstyled` parent themes rebuilds no theme on its own. `frontend-css-common` is left out on purpose. Themes take it from its published artifact rather than from the branch, so a theme build cannot see a change to it.

## Match

`^modules/apps/frontend-js/frontend-js-clay-web/clay/clay-css/|^modules/apps/frontend-theme/frontend-theme-(styled|unstyled)/`

## Command

A shared CSS change fans out to every theme. Select each module under `modules/apps` whose `package.json` declares a `liferayTheme` block, and take its Gradle project path. Scan `modules/apps` rather than `modules/apps/frontend-theme`, or the scan misses the commerce themes, which take the same `styled` parent and are affected by the same change:

```bash
(cd "${REPO_ROOT}" && command grep --files-with-matches --include='package.json' --recursive '"liferayTheme"' modules/apps) \
	| command grep --invert-match --regexp='/node_modules/' --regexp='/gradleTest/' \
	| bash "${SKILL_DIR}/find_modules.sh" "${MERGE_BASE}" \
	| cut -d " " -f1 \
	| sed "s#^modules/##; s#/#:#g" \
	| sort --unique
```

Run `packageRunBuild` (not `deploy`) per theme:

```bash
("${REPO_ROOT}/gradlew" \
	--project-dir "${REPO_ROOT}/modules" \
	:<path>:packageRunBuild)
```

The build needs `node` and an `npm`/`yarn` install and builds both parent themes once; that work amortizes across the theme set.

FAIL when a theme's build reports `BUILD FAILED`, and name the theme and the error. Selecting no theme at all is a broken scan rather than a pass, since a shared CSS change reaches every theme the modules build contains; report that as a FAIL too. PASS when every selected theme reports `BUILD SUCCESSFUL`.

## Checklist

```
- [ ] (One subitem per theme:) packageRunBuild <theme path>
```

## Time Estimate

~30 sec - 2 min per theme; the first also pays node download, install, and parent theme builds.