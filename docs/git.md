# Git Strategy

This project uses trunk-based development. `main` is always the trunk. Changes are small, short-lived, and merged through pull requests.

## Principles

- `main` always builds and passes CI.
- Branches live for a short time, usually less than two days.
- Every change reaches `main` through a pull request.
- Each commit is one logical change that passes the checks.

## Branches

- Create branches from the latest `main`.
- Name branches as `<type>/<short-description>` using lowercase and hyphens.
- Delete the branch after it is merged.

## Commits

- Use Conventional Commits prefixes: `feat:`, `fix:`, `chore:`, `refactor:`, `docs:`, `test:`.
- Write the subject in imperative mood, for example `feat: add cooldown to siren detection`. Keep it under 72 characters.
- Before every commit, run `./gradlew ktlintCheck lint test assembleDebug` and make sure it passes.
- Do not commit `local.properties`, `build/`, or IDE files. `.gitignore` already covers them.

## Pull Requests

- Target `main`.
- Keep a PR small enough to review in one sitting. Split large work into several PRs.
- Use the PR description to explain what changed and why.
- Merge only after CI passes.
- Default merge method: squash merge, so `main` keeps one commit per PR.
- Check the PR against `docs/spec.md` if it touches detection or alerts.

## CI

- The workflow in `.github/workflows/android-ci.yml` runs on pushes to `main` and on PRs targeting `main`.
- If `main` turns red, fixing it is the top priority. Do not start new feature branches until it is green again.

## Unfinished Features

- Split unfinished work into small pieces that each keep `main` working.
- Merge logic that the app does not call yet, and verify it with unit tests.
- Connect the pieces to user-visible behavior in the last PR.
- Keep a branch open only when a change cannot be split. Open a PR early so the work is visible, and do not let the branch live longer than two days.

## Hotfixes

- Branch from `main`, use `fix/` as the prefix, and go through the same PR and CI process.

## Tags

- Tag each release for submission as `vMAJOR.MINOR.PATCH`, matching `versionName` in `app/build.gradle.kts`.

## Forbidden

- Force-pushing to `main`.
- Committing secrets, `local.properties`, or build outputs.
- Merging a PR with failing CI.
