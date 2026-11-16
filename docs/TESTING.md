# Validation

## Automated

```sh
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

`ScheduleTest` covers daily independence, weekday/date boundaries, one-off tasks, multiple slots and children, immutable historical schedules, deterministic concurrent revisions, concurrent acknowledgements, targeted corrections, DST gaps/overlaps, invalid input, delayed inexact alarms, and reminder horizon limits.

Firestore emulator rules tests cover both authorized parents, unrelated and unauthenticated access, membership protection, immutable task/event records, validation, author spoofing, and correction target validation:

```sh
cd firebase
npm install
npx firebase emulators:exec --config ../firebase.json --project demo-kinderalltag --only firestore 'npm test'
```

## Two-phone acceptance

Use a test Firebase project with two provisioned accounts and an unrelated third account.

1. Create daily Johann tasks at two times and a Ferdinand one-off. Confirm they appear on both phones, grouped independently.
2. Acknowledge one Johann slot. Check the other phone updates with the parent's name/time; the other slot and Ferdinand remain open.
3. Disable connectivity on one phone. Acknowledge a task, verify the pending marker, close/reopen the app, reconnect, and verify both phones converge.
4. Acknowledge the same slot on both phones while offline. Reconnect: retain both acknowledgements. Correct them with confirmation; history retains all actions. Acknowledge again to reopen a completed record correctly.
5. Edit a schedule and pause another task. Today and earlier days stay unchanged; tomorrow uses the new definitions. Resume a paused task and verify its next effective date.
6. Browse earlier dates, filter by child, acknowledge an earlier unconfirmed occurrence, and check the actual acknowledgement date/time is visible.
7. Test Monday-only, one-off, end-date boundaries, and several daily times. Unit tests cover the Berlin DST transitions independently of the phone timezone.
8. Enable reminders on one phone only. Verify permission denial is handled, a reminder opens the right detail, and a known completion suppresses it. Repeat with reboot and an edited schedule. Test offline stale reminders and verify the cached-status message.
9. Sign in with an unprovisioned account: only the waiting-for-access screen appears. Confirm direct database reads/writes are denied by emulator tests.
10. Install a signed release APK on both phones. Increase the version code, rebuild with the same key, and update over the installed release. Verify sign-in, local notification preference, and shared data remain.

## Build status in the implementation environment

Validated locally:

- All **12 domain JUnit tests passed**, compiling the actual domain and test sources with Android Studio's bundled Kotlin 2.4 compiler and the cached JUnit 4.13.2 runtime. The project itself pins Kotlin 2.2.21; its Gradle test task still needs a connected run.
- Kotlin syntax parsing passed for all application/test sources and Gradle Kotlin scripts.
- Android resources compiled and linked with the installed `aapt2` and Android 36.1 SDK. This verifies resources and manifest attributes, not the complete application.
- Reminder source compiled against the installed Android API using minimal application-type fixtures, checking Android API usage independently of Firebase/Compose dependencies.
- JSON/XML parsing, shell syntax, and JavaScript test-file syntax checks passed.

The full Gradle test/lint/APK command was attempted and failed while downloading Gradle 8.13 with `UnknownHostException: services.gradle.org`. Network DNS resolution for dependency downloads is unavailable in the supplied environment. Full Android compilation, lint, emulator rules tests, physical notification delivery, Google sign-in, and signed release installation remain unverified and require the connected setup described above. No installable APK has been produced.
