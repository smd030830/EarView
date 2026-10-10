# Conventions

ktlint handles formatting. This file covers the rules ktlint cannot check.

## Kotlin

- Use `val` by default. Model types are `data class` or `sealed interface`.
- Avoid `!!`. Handle nullability explicitly.
- Keep classes and functions `internal` or `private` unless another feature needs them.
- Model closed sets of states and events with `sealed interface`.

## Naming

- Packages are lowercase, grouped by feature: `detection`, `alert`, `settings`.
- Suffixes: `*UseCase`, `*Repository`, `*ViewModel`, `*Service`.
- Sound classes are defined once, as an enum that maps YAMNet `display_name` values. Do not write class name strings anywhere else.

## Layers

- `domain` uses pure Kotlin only. It must not import `android.*` or `androidx.*`.
- `presentation` depends on `domain`. It does not depend on `data` directly.
- Do not pass `Context` into `domain` or `presentation` ViewModels.

## Coroutines

- Inject dispatchers. Do not hardcode `Dispatchers.IO` or `Dispatchers.Default` in `domain`.
- Expose state from ViewModels as `StateFlow`.

## Android and Compose

- Write new UI in Compose only. Do not add XML layouts.
- Compose screens receive state and callbacks as parameters.
- The foreground service owns its own lifecycle. It must call `startForeground` promptly after starting.

## Dependencies

- Declare versions only in `gradle/libs.versions.toml`. Do not hardcode versions in `build.gradle.kts`.

## Logging

- Do not log raw audio.
- Log detection results only in debug builds.

## Comments

- Explain why, not what.
- Add KDoc to public types in `domain`.

## Testing

- Unit test the `domain` logic: class filtering, thresholds, cooldown, and class-to-alert mapping.
- Use fixed inputs in unit tests. Do not depend on a microphone or real audio.
- Every bug fix includes a regression test.
- Device and audio behavior is checked manually. The checklist is in `docs/testing.md`, which is not written yet.
- Test names describe behavior, for example `cooldown_suppresses_repeated_siren_within_window`.

## Open Items

- Coroutine dispatcher injection: decide between a simple constructor parameter and a DI library. Do not add a DI library without asking first (see AGENTS.md).
- Detection result and alert event models: finalize when `docs/spec.md` is filled in.
