# Kinderalltag

A shared Android checklist for Ferdinand and Johann, with a German interface. Both parents can manage routines, acknowledge each occurrence, and see who did what and when.

Native Kotlin / Jetpack Compose, Android 8+, Firebase Google sign-in and Cloud Firestore. There is no application server to run. Firebase configuration and the two household accounts must be provisioned before shared use.

## Get started

Open this directory in Android Studio, select JDK 17 or 21 for Gradle, and install Android SDK Platform 36. Then follow [the setup guide](docs/SETUP.md).

```sh
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Without `app/google-services.json`, the app builds into a setup screen. With configuration it supports:

- Shared today view, grouped by child, with completion details.
- One-off, daily, selected weekday and multiple-time schedules.
- History by date and child, including late acknowledgements and corrections.
- Offline acknowledgements with visible pending synchronization.
- Schedule changes effective tomorrow, retaining previous versions.
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
