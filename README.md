# Kinderalltag

A shared Android checklist for Ferdinand and Johann, with a German interface. Both parents can manage routines, acknowledge each occurrence, and see who did what and when.

Native Kotlin / Jetpack Compose, Android 8+, Firebase Google sign-in and Cloud Firestore. There is no application server to run. Firebase configuration and the two household accounts must be provisioned before shared use.

## Get started

Open this directory in Android Studio, select JDK 17 or 21 for Gradle, and install Android SDK Platform 36. Then follow [the setup guide](docs/SETUP.md).

```sh
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Without `app/google-services.json`, the app builds into a setup screen. With configuration it supports:

- Shared today view, grouped by child, with completion details and the current shopping list.
- Undated shopping and gift lists, configurable labels and offline purchase checkboxes.
- Gift ideas for either child or both, with occasion and giver labels.
- German voice dictation for titles/notes and reviewed multi-item shopping imports.
- Optional live notifications when the other parent completes a task.
- One-off, daily, selected weekday and multiple-time schedules.
- History by date and child, including late acknowledgements and corrections.
- Offline acknowledgements with visible pending synchronization.
- Immediate edits to upcoming occurrences, retaining started and confirmed instances.
- Recoverable deletion of one occurrence, following occurrences, or a whole series via Papierkorb.
- One-off and recurring appointments, including single-occurrence changes and cancellations.
- Responsibility for one parent or both, with a “Nur meine” filter and matching reminders.
- Native date/time pickers with locale-aware formatting.
- Saving individual appointments through the phone’s calendar editor.
- Sharing and reviewed import of standard `.ics` calendar files.
- Optional reminders configured separately on each phone.
- Editable child names and Google account sign-in.

For release signing and manual installation, see [SETUP.md](docs/SETUP.md). For storage and concurrency decisions, see [ARCHITECTURE.md](docs/ARCHITECTURE.md). For acceptance checks, see [TESTING.md](docs/TESTING.md).

## Backend tests

Install Node 20+ and JDK 21, then:

```sh
cd firebase
npm install
npx firebase emulators:exec --config ../firebase.json --project demo-kinderalltag --only firestore 'npm test'
```

The emulator tests only use the `demo-kinderalltag` project; they must never target a real household database.

## Development checks and Git hooks

Formatting uses [Spotless](https://github.com/diffplug/spotless/tree/main/plugin-gradle) with ktfmt's four-space Kotlin style for app/test Kotlin and Gradle Kotlin scripts.

```sh
./gradlew spotlessApply          # Format sources and build scripts
./gradlew spotlessCheck          # Check formatting without changing files
./scripts/install-hooks.sh      # Enable the repository's pre-commit hook once per checkout
```

The hook formats and runs Android lint for app/build changes. It refuses unstaged Android changes, never auto-stages files, and stops for review if formatting changes anything. Stage the reviewed files and commit again. It requires the same JDK/SDK as a normal build; the first run needs dependency downloads. Existing hooks are preserved by the installer.

[Android checks](.github/workflows/android.yml) runs on pushes, pull requests and manual dispatch: formatting, lint, JVM tests, debug compilation, hook tests and demo Firestore rules tests. Reports, a formatting patch and the setup-screen debug APK are uploaded as artifacts. CI flags uncommitted formatting changes; run `spotlessApply` locally or review/apply the uploaded patch. No Firebase configuration or signing secrets are required. See [TESTING.md](docs/TESTING.md) for details and local validation limits.
