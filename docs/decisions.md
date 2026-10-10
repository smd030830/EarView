# Decisions

## 2026-10-10 — ktlint configuration via `.editorconfig`

Added a root `.editorconfig` so `./gradlew ktlintCheck` passes. The failure came
from two ktlint standard rules that conflict with Jetpack Compose conventions:

- `function-naming` flagged PascalCase `@Composable` functions
  (`MainNavigation`, `EarViewTheme`, `MainScreen`, `Greeting`, previews).
- `filename` flagged `NavigationKeys.kt`, whose single top-level declaration is
  `Main`, and wanted the file renamed to `Main.kt`.

Both are intentional, so the rules are relaxed in `.editorconfig`:

- `ktlint_function_naming_ignore_when_annotated_with = Composable`
- `ktlint_standard_filename = disabled`

The remaining differences were genuine formatting drift. `ktlint_code_style =
intellij_idea` and `indent_size = 2` were pinned to match the code style already
used in the project, and the drift was fixed with `./gradlew ktlintFormat`.
