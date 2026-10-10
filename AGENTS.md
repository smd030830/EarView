## Project Overview

EarView is an on-device Android app that helps deaf and hard-of-hearing users stay aware of danger. It listens to ambient sound with YAMNet, recognizes a set of hazardous sounds, and alerts the user with vibration patterns and screen colors. The app runs in the background through a foreground service. All audio processing happens on the device, and no audio or inference results leave it.

Target classes (7): Siren, Fire alarm, Vehicle horn, Screaming, Baby cry, Glass shatter, Gunshot.


## Documentation

Read the relevant doc before starting a task. Keep this file short; details live in `docs/`.

- `docs/spec.md`: Source of truth for features, the 7 classes, thresholds, and alert patterns. Read it before changing detection or alert logic.
- `docs/architecture.md`: Feature-first clean architecture, data flow, module boundaries, permissions, and service structure.
- `docs/conventions.md`: Code rules beyond ktlint, and testing principles.
- `docs/git.md`: Trunk-based branch, commit, and PR rules.
- `docs/decisions.md`: Dated decision log.

If a doc conflicts with this file, `spec.md` wins for product behavior and this file wins for process rules.


## Tech Stack

- Language: Kotlin, Java 17 target
- UI: Jetpack Compose with Material3
- Build: Gradle Kotlin DSL with a version catalog (`gradle/libs.versions.toml`)
- Lint and format: ktlint
- Android: minSdk 24, compileSdk 36, targetSdk 36
- Dev tool versions are managed in `mise.toml`


## Architecture & Git

- Architecture: feature-first clean architecture. Details in `docs/architecture.md`.
- Git: trunk-based development. Details in `docs/git.md`.


## Rules

- Keep the package name `com.example.earview`.
- Do not add network calls or analytics. Do not store audio or inference results.
- Do not add libraries or plugins without asking first.
- Do not invent thresholds, colors, or alert patterns that are not in `docs/spec.md`.
- Keep `main` green. Merge changes through PRs.
- Record changed decisions in `docs/decisions.md`.


## Command

- Check: `./gradlew ktlintCheck lint test assembleDebug`
- Format: `./gradlew ktlintFormat`

Run the check before every commit.
