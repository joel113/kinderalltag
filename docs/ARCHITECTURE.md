# Architecture and data behavior

The UI observes a `FamilyViewModel`, backed by `FamilyRepository` and Firestore snapshot listeners. `domain/Schedule.kt` contains pure scheduling and event-reduction logic. The app talks directly to Firebase; Firestore security rules are the backend authorization boundary.

## Storage

All data belongs to `households/family`:

- `members/{authUid}`: manually provisioned membership; clients may read only their own membership and cannot change it.
- `children/{ferdinand|johann}`: editable `name` and optional `bearColor` palette key; console-provisioned IDs remain stable. Missing or unknown color values use each child's default. The bear is drawn in Compose from the `bear.jpg` reference, with only the fur color changing; household tasks keep their house icon.
- `Haushalt` is a fixed section appended to the child profiles in the app, with reserved task `childId` value `haushalt`. It has no profile document and is not editable as a child. Existing task documents keep their current format.
- `revisions/{uuid}`: immutable full task definitions, including task ID, child ID, title, notes, recurrence, times, start/end, effective date, author, and server creation time.
- `events/{uuid}`: immutable completion or retraction, occurrence ID, occurrence date, author ID/name, device action time, server receipt time, and optional target completion ID.

No custom HTTP API, public registration, child account, Cloud Function, analytics SDK, or storage bucket is needed. The application includes no administrator credentials. `google-services.json` is client configuration, not an authorization mechanism; rules restrict data access.

## Schedules

Each local household date chooses the most recent effective revision for each task, breaking concurrent edits by server creation time and then revision ID. The occurrence ID includes task, revision, local date, and scheduled minute, so different children, dates, and time slots remain independent.

New tasks can begin today. Edits, pauses, and resumptions apply tomorrow; they cannot rewrite earlier occurrences. A one-off task edited after its original date needs a new future date. Daily/weekday tasks allow an optional inclusive end date. A weekly schedule is a weekday selection; up to eight unique times are supported per day. Future-effective revisions are shown on the Aufgaben screen.

The timezone is fixed to `Europe/Berlin`. Java time moves nonexistent spring times forward by the DST gap and chooses the earlier offset for repeated autumn times. Each configured slot creates one occurrence. Unconfirmed tasks do not imply that care was omitted; they simply have no active acknowledgement.

## Offline and concurrency

Firestore persists its Android cache and queues acknowledgement writes. The UI marks pending changes and cached snapshots. The displayed action time comes from the device clock and is retained when syncing later. The server receipt time is also retained and shown in detail when synchronization is delayed by more than a minute. A late acknowledgement targets the original date while recording its current action time.

Completion status is derived from completion events that have no matching retraction. Simultaneous completions retain both parents. A correction retracts all completions currently visible for that occurrence. If another completion arrives concurrently, it remains active and can be corrected separately. Repeated retries do not replace history; the SDK retries the same queued event document.

Task and child-name edits use online transactions and never queue offline. Two simultaneous task edits converge deterministically; the latest revision wins for its effective date, while both revisions remain stored. The UI blocks sign-out while writes are pending. Server write rejection removes the optimistic event and displays an error.

## Reminder limits

Local preferences contain upcoming reminder metadata. Reconciliation cancels obsolete alarms and delivered notifications for completed occurrences. A seven-day horizon is replenished when data is observed and on process startup; a midnight alarm starts the process for the next day. Reboot and clock changes restore stored alarms. Schedule at most the earliest 300 alarms to stay below common per-app alarm limits; this is intended for a small household task list. Tapping opens the occurrence detail; there is no notification action that blindly acknowledges care.

## Capacity and privacy

This small-family v1 listens to the complete revisions and event collections to make history available offline. Initial reads grow with history; monitor Firebase usage. Pagination and an explicit history download policy would be appropriate before expanding to many households or years of high-volume tasks. There is no automatic deletion or paid backup dependency. For an occasional manual export, an owner can use Firebase's supported console/administrative tooling; an in-app export is outside v1.

Android backup is disabled to avoid copying the app cache through device backup. Firebase encrypts transport/storage through its service, but this app does not implement end-to-end encryption. Sign-out changes authorization; it is not a remote wipe of the local Firestore cache. Use phone screen locks and device data erasure when retiring a phone.
