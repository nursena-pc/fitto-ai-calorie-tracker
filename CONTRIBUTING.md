# Contributing to Fitto

Thank you for improving Fitto. Changes should preserve user behavior, protect personal data, and keep feature boundaries clear.

## Development requirements

- Android Studio with Android SDK 36
- JDK 17 for Gradle
- Android device or emulator with API 26 or newer
- A local `google-services.json` for Firebase-backed runtime flows
- Local API values in `local.properties`; never commit real credentials

Example `local.properties` entries:

```properties
USDA_API_KEY=DEMO_KEY
GEMINI_API_KEY=
LOGMEAL_API_USER_TOKEN=
```

## Branches

Create every change from an up-to-date `main` branch.

```bash
git switch main
git pull origin main
git switch -c <type>/<short-description>
```

Recommended prefixes:

- `feature/` for user-facing functionality
- `fix/` for defects
- `refactor/` for behavior-preserving architecture work
- `test/` for test-only changes
- `docs/` for documentation
- `ci/` for GitHub Actions and automation
- `chore/` for maintenance

Keep one coherent purpose per branch and pull request.

## Architecture rules

- Fragments and Activities bind views, forward user events, render state, and navigate.
- Repositories and data sources are supplied through feature dependencies and the application composition root.
- Business decisions belong in independently testable coordinators, actions, policies, resolvers, or state stores.
- Long-lived screen state must survive view recreation where appropriate.
- Asynchronous results must not overwrite newer screen state.
- Use the shared coroutine dispatchers instead of choosing production dispatchers inside UI code.
- Do not change persistence formats or Firebase document structures without documenting compatibility and rollback behavior.

## Required local verification

Run the checks relevant to the change, followed by the complete verification suite before requesting review:

```bash
./gradlew :app:compileDebugKotlin
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
./gradlew :app:assembleDebugAndroidTest
./gradlew :app:lintDebug
```

Run instrumentation tests for Room migrations, Android lifecycle behavior, navigation, permissions, alarms, or platform integrations:

```bash
./gradlew :app:connectedDebugAndroidTest
```

## Commit and pull request standards

- Use an imperative, specific commit subject such as `Extract meal save action`.
- Do not mix unrelated cleanup with feature work.
- Complete the pull request template.
- Include screenshots or recordings for visible UI changes.
- Document test commands and their results.
- Call out migration, security, privacy, API-level, and rollback implications.
- Never commit `local.properties`, `google-services.json`, API keys, keystores, user data, or production credentials.

## Review and merge

Pull requests should be squash-merged after required checks pass, review conversations are resolved, and the branch is up to date with `main`. Delete the source branch after merge.
