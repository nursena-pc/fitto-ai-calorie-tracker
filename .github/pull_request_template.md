## Summary

<!-- Explain what this pull request changes and why. -->

## Changes

- Describe the primary change.

## Verification

- [ ] `./gradlew :app:compileDebugKotlin`
- [ ] `./gradlew :app:testDebugUnitTest`
- [ ] `./gradlew :app:assembleDebug`
- [ ] `./gradlew :app:assembleDebugAndroidTest`
- [ ] `./gradlew :app:lintDebug`
- [ ] Instrumentation tests were run when the change affects Android runtime, Room migrations, navigation, or lifecycle behavior

## Screenshots or recordings

<!-- Required for visible UI changes. Write "Not applicable" otherwise. -->

## Architecture and behavior

- [ ] Existing user-facing behavior is preserved, or intentional behavior changes are documented
- [ ] UI classes do not create repositories or data sources directly
- [ ] New state and business rules are independently testable where practical
- [ ] Asynchronous work is lifecycle-aware and stale results cannot overwrite newer state

## Data, security, and compatibility

- [ ] No secret, API key, credential, personal data, or private Firebase configuration is committed
- [ ] Room schema and migration implications were reviewed
- [ ] Backup, permissions, notification, and API-level behavior were reviewed where applicable
- [ ] The minimum supported Android version remains compatible

## Risk and rollback

**Risk level:** Low / Medium / High

**Primary risk:**

**Rollback plan:** Revert this pull request.

## Related issue

<!-- Example: Closes #123 -->
