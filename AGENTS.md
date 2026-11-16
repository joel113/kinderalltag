# Repository Guidelines

## Project Structure & Module Organization

Kinderalltag uses Kotlin, Jetpack Compose, and Firebase, with a German interface.

- `app/src/main/java/de/kinderalltag/`: `ui/` contains Compose screens and ViewModels, `domain/` contains pure scheduling logic, `data/` handles Firestore, and `notifications/` manages local reminders.
- `app/src/main/res/`: Android strings, themes, and drawable assets.
- `app/src/test/`: domain unit tests.
- `firebase/`: access rules, indexes, and emulator tests in `tests/`.
- `docs/`: setup, architecture decisions, and acceptance checks.

## Build, Test, and Development Commands

Use JDK 17 or 21 and Android SDK Platform 36. Run locally through Android Studio.

- `./gradlew :app:assembleDebug`: build a debug APK; without Firebase configuration, it displays a setup screen.
- `./gradlew :app:testDebugUnitTest :app:lintDebug`: run unit tests and Android Lint.
- `./gradlew :app:assembleRelease`: build a release APK; installation requires signing configuration.

For backend tests, use Node 20+ and JDK 21:

```sh
cd firebase
npm install
npx firebase emulators:exec --config ../firebase.json --project demo-kinderalltag --only firestore 'npm test'
```

## Coding Style & Naming Conventions

Use four-space indentation in Kotlin and two spaces in JSON/JavaScript. Follow Kotlin conventions: `PascalCase` types and composables, `camelCase` functions/properties, and `UPPER_SNAKE_CASE` constants. Keep scheduling logic independent of Android/Firebase. Use Android Studio's Kotlin formatter; no standalone formatter is configured. Keep user-facing text German.

## Testing Guidelines

Use JUnit 4 for domain tests, with descriptive behavior names in `*Test.kt`. Rules tests use Node's test runner and Firebase Rules Unit Testing in `*.test.mjs`. No coverage threshold is configured; cover changed scheduling, concurrency, offline, and authorization behavior. Follow `docs/TESTING.md` for two-phone acceptance checks. Run rules tests only against the demo emulator project.

## Commit & Pull Request Guidelines

No Git history is available in this checkout, so no existing message convention can be inferred. Use concise, imperative commit subjects. PRs should describe behavior changes, reference relevant issues, report validation and limitations, and include screenshots for UI changes.

## Security & Configuration

Follow `docs/SETUP.md`. Keep signing keys, passwords, and local Firebase configuration untracked. Preserve membership restrictions, immutable audit events, tomorrow-effective schedule edits, and the `Europe/Berlin` household timezone.

No Git history is available in this checkout. Use concise, imperative commit subjects, such as `Fix reminder cancellation after sync`. PRs should describe behavior changes, link relevant issues, report validation and unrun checks, and include screenshots for UI changes.

## Security & Configuration

Keep Firebase configuration, signing passwords, and keystores out of commits. Follow `docs/SETUP.md`; preserve immutable task revisions and acknowledgement history.
