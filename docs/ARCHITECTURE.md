# Architecture and data behavior

The UI observes a `FamilyViewModel`, backed by `FamilyRepository` and Firestore snapshot listeners. `domain/Schedule.kt` contains pure scheduling and event-reduction logic. The app talks directly to Firebase; Firestore security rules are the backend authorization boundary.

## Storage

All data belongs to `households/family`:

- `members/{authUid}`: manually provisioned membership; authorized household members can read the parent roster; an unprovisioned user can read only their own membership. Clients cannot grant or change membership.
- `children/{stableId}`: shared name, ten-colour `bearColor` key, `icon`, `archived` and optimistic `version`. Existing `johann`/`ferdinand` IDs and appearance remain stable. Archive preserves historical references; profiles cannot be deleted.
- `childRegistry/state`: schema version 2, monotonically increasing version, known IDs, active IDs (at most ten), and the changed profile ID. Profile lifecycle/appearance writes and registry updates are atomic, validated in both directions by rules. See [migration and rollout](DYNAMIC_CHILDREN.md).
- `Haushalt` is a fixed section appended to the child profiles in the app, with reserved task `childId` value `haushalt`. It has no profile document and is not editable as a child. Existing task documents keep their current format.
- `revisions/{uuid}`: immutable full task definitions, including task ID, child ID, title, notes, recurrence, times, start/end, effective date, author, and server creation time. Optional `responsibleParentId` selects one parent; missing/null means both parents.
- `events/{uuid}`: immutable completion or retraction, occurrence ID, actual date, actor, device action time, server receipt time, and optional target completion ID. New confirmations include a full task occurrence snapshot.
- `itemOperations/{uuid}`: append-only EDIT, DELETE and RESTORE records, scoped to SINGLE, FOLLOWING or ALL. Records include the original anchor, actual date/start, source definition, actor, server timestamp, optimistic version and optional replacement snapshot or restored deletion ID.
- `itemControls/{TASK_taskId|APPOINTMENT_rootId}`: latest mutation version and operation ID, updated atomically with each operation. Rules enforce the pair; clients cannot overwrite or delete operations.

No custom HTTP API, public registration, child account, Cloud Function, analytics SDK, or storage bucket is needed. The application includes no administrator credentials. `google-services.json` is client configuration, not an authorization mechanism; rules restrict data access.

## Appointments and responsibility

`appointmentSeries/{uuid}` stores a complete appointment definition, recipient (`FERDINAND`, `JOHANN`, `BOTH_CHILDREN`, `HOUSEHOLD`), parent responsibility, recurrence, optional inclusive end, exclusive split boundary, and optimistic version. `appointmentExceptions/{seriesId_originalDate}` stores a replacement occurrence or cancellation, its version, and the series version it was based on. No new profile documents or backfill are needed.

Occurrence identity uses the series ID and original local date, so moving an occurrence does not change its identity. Monthly/yearly recurrence is calculated from the original anchor, avoiding month-end drift. A separate inclusive `fromDate` preserves that anchor when metadata changes split a series at a clamped date; changing the date or interval starts a new rhythm. Queries include moved-in exceptions even when the original date is outside the visible window. Notification links resolve the latest occurrence by identity.

New appointments are created with an atomic ADDED activity record. All new edits and recoverable deletions use item operations instead of shortening or replacing stored series. Started appointments cannot be edited, but may be hidden and restored. Explicit single-occurrence changes and legacy exceptions survive following-series edits, including occurrences moved into the past. Legacy physical splits remain readable; recorded result-series links and explicit root IDs identify related series. Unlinked legacy series are treated independently.

Task and appointment edits, including responsibility and pause/resume, apply immediately to eligible upcoming occurrences. Started occurrences and confirmed future tasks retain their snapshots. Either parent can help complete a task regardless of assignment; one active acknowledgement completes it. Both parents retain full household visibility, and `Nur meine` is a display filter rather than an access restriction.

Only responsible parents receive reminders. Shared assignments mean both parents, including for legacy tasks. Task and appointment alarms are merged chronologically under the 300-alarm limit. Appointment reminders retain an actual alarm timestamp, occurrence date, and expiry, allowing reminders on the previous day. Delivered appointment notifications are withdrawn when changed, canceled, expired, or reassigned once the phone receives the update. The midnight receiver replenishes alarms even when the app process is already running.

## Schedules

Legacy task scheduling chooses the most recent effective revision for each household date. Managed scheduling overlays following edits, then explicit single-occurrence edits, confirmation snapshots, and active deletion masks. The original occurrence ID remains stable after moving its date or time. Task slot origins retain unchanged times first and pair remaining slots deterministically; newly added slots receive new namespaces. This also maps across older effective revisions without duplicating a confirmed slot. Appointments retain their physical source-series ID and original local date, with logical root IDs used for whole-series scopes.

New tasks can begin today. Single or following edits, pauses and resumptions affect upcoming times, including later today; times already started and active confirmations remain unchanged. Moving a time before the edit clock does not backfill today. Late offline confirmations can pin a removed or moved occurrence back to its recorded snapshot. Daily/weekday tasks allow an optional inclusive end date. A weekly schedule is a weekday selection; up to eight unique times are supported per day. Future-effective revisions are shown on the Aufgaben screen.

The timezone is fixed to `Europe/Berlin`. Java time moves nonexistent spring times forward by the DST gap and chooses the earlier offset for repeated autumn times. Each configured slot creates one occurrence. Unconfirmed tasks do not imply that care was omitted; they simply have no active acknowledgement.

## Offline and concurrency

Firestore persists its Android cache and queues acknowledgement writes. The UI marks pending changes and cached snapshots. The displayed action time comes from the device clock and is retained when syncing later. The server receipt time is also retained and shown in detail when synchronization is delayed by more than a minute. A late acknowledgement targets the original date while recording its current action time.

Completion status is derived from completion events that have no matching retraction. Simultaneous completions retain both parents. A correction retracts all completions currently visible for that occurrence. If another completion arrives concurrently, it remains active and can be corrected separately. Repeated retries do not replace history; the SDK retries the same queued event document.

Structural mutations require a current server snapshot and online transactions. Editors capture the logical item version; the transaction compares it with itemControls before appending the mutation and advancing the control. Stale editors must reopen. Child-name changes also require an online transaction. Offline care confirmations remain queued independently of structural changes. The UI blocks sign-out while writes are pending. Server write rejection removes the optimistic event and displays an error.

## Reminder limits

Local preferences contain upcoming reminder metadata. Reconciliation cancels obsolete alarms and delivered notifications for completed occurrences. A seven-day horizon is replenished when data is observed and on process startup; a midnight alarm starts the process for the next day. Reboot and clock changes restore stored alarms. Schedule at most the earliest 300 alarms to stay below common per-app alarm limits; this is intended for a small household task list. Tapping opens the occurrence detail; there is no notification action that blindly acknowledges care.

## Capacity and privacy

This small-family v1 listens to the complete revisions and event collections to make history available offline. Initial reads grow with history; monitor Firebase usage. Pagination and an explicit history download policy would be appropriate before expanding to many households or years of high-volume tasks. There is no automatic deletion or paid backup dependency. For an occasional manual export, an owner can use Firebase's supported console/administrative tooling; an in-app export is outside v1.

Android backup is disabled to avoid copying the app cache through device backup. Firebase encrypts transport/storage through its service, but this app does not implement end-to-end encryption. Sign-out changes authorization; it is not a remote wipe of the local Firestore cache. Use phone screen locks and device data erasure when retiring a phone.

## Synchronization and presentation

Listener failures are grouped by collection/code and logged without dumping stored documents. Permission failures explain rule publication; transport failures retain the cached-data message. Retry removes terminated listeners, rechecks membership, and registers all eleven data listeners (including itemOperations, listItems, listActivity and listLabels) again while retaining the same user's cached state and pending-write indication. Generation tokens prevent callbacks from an old subscription from contaminating a restarted session. Erroring/invalid collections cannot mark the state fully synchronized.

Date/time editor state uses LocalDate/LocalTime rather than parsing formatted text. Native Android pickers follow the device locale and clock preference. Presentation formatting is separate from ISO serialization and continues to display family-zone times even when the phone travels.

## Calendar exchange

`CalendarExchange` implements the bounded iCalendar subset supported by the domain, including UTF-8 line folding, escaped text, UTC/floating/IANA time zones, and basic RRULE intervals. Unsupported recurrence is rejected or explicitly warned about rather than silently dropped. Recurring imports can retain an old anchor with a future active boundary. Calendar parsing performs no network requests or database writes.

`CalendarInbox` copies content URIs into private app storage before the temporary read grant expires, serializes changes under a mutex, and persists queue progress through sign-in/process restarts. Limits apply to the complete pending batch. Each item requires editor confirmation and saves through the existing member-authorized transaction. Export files live in a dedicated cache subdirectory exposed only through a non-exported FileProvider and temporary read grants. Exported UID values are derived anonymous event identifiers; no member IDs or authorization data are included.

## Activity history

`activityHistory` merges immutable task acknowledgements/retractions, task revisions, appointment activity snapshots and item operations. Deleted entries retain their original activity and completion credit, with separate deletion/restoration audit entries. It filters half-open instant ranges derived from inclusive 90-day Europe/Berlin calendar periods, then sorts newest first with stable IDs. Task acknowledgements use the recorded client action time (server time is a fallback); revisions and online appointment mutations use server timestamps. Scheduled/effective dates remain separate from action dates. History's “Nur meine” matches the authenticated author or credited parent, while child filters use the subject snapshot.

`appointmentActivity` is append-only and written in the same transaction as new appointment creation. It also preserves activity from older physical appointment edits/splits; new edits, deletions and restores are recorded in itemOperations. Records retain title/details, authenticated actor, kind, original date, following scope, and a new-series reference when splitting. Rules restrict writes to members, enforce the actor/timestamp/schema, and require a corresponding mutation via `getAfter`. The new collection participates in synchronization health checks. All collections are currently fully cached; 90-day navigation filters the local activity feed, rather than limiting Firestore reads.

Legacy series expose their existing creation timestamp; legacy exceptions expose only their latest update timestamp and actor. These appear as reconstructed entries and are suppressed when a matching immutable activity exists. Unknown timestamps are never invented. Old overwritten edits and series cutoffs without timestamps cannot be recovered. Old clients may still mutate appointments without logging; both phones should be updated.

## Event statistics

`statisticsPeriod` computes inclusive 7/30/90-day family-calendar periods. `eventStatistics` aggregates period-filtered `HistoryEntry` values by event kind, credited/acting parent, mutually exclusive subject category, and local weekday. Unknown references and zero-valued known categories remain represented. Both-child appointments contribute once to their own category, so each dimension reconciles with the overview total. Statistics uses existing subscribed/cached data and does not add backend writes or reads.

Completed-task totals are separate from raw event counts. Across the complete `CareEvent` history, choose each occurrence's earliest un-retracted confirmation by recorded action timestamp and ID. Count it only if its corresponding history entry is in the selected period. Corrections outside that period still affect current validity; duplicate confirmations never inflate the net count. Named app destinations distinguish the new statistics tab from settings and notification routes; its remembered duration/page do not depend on shared screen filters.

## Runtime family activity notifications

`FamilyActivityNoticeTracker` keeps a process-local baseline of task IDs and ADDED appointment activity IDs. Only a fully loaded, authorized, non-cached, non-pending, error-free state establishes/advances it. It emits only previously unseen additions by another account, and retains seen IDs through transient incomplete snapshots. Account changes, access loss and process restarts establish new silent baselines. Task identity, rather than revision identity, suppresses edit/resume alerts; appointment activity kind suppresses split/edit alerts.

`ActivityNotifications` consumes all resulting notices even when their account/device preference or Android permission prevents delivery. Local Android notifications use a separate channel, private lock-screen visibility, stable per-event tags and explicit page intents. The application collector performs observation while alive; no service, polling, FCM, or additional backend write was added. Reminder cleanup is scoped to its own channel. Impressum content is supplied publisher information displayed locally in settings.

## Recoverable deletion

Either parent can delete past, completed or upcoming tasks and appointments: one original occurrence, that anchor and following occurrences, or the whole logical series. Deletion masks hide entries from home, lists, calendar previews, sharing and care reminders. They never physically remove definitions, confirmations or audit entries, so statistics retain historical completion credit. Verlauf and Statistik include deletion/restoration actions.

**Baum → Papierkorb** lists active deletions. Restoration appends a record referencing one deletion; it does not rewind edits or other deletion masks. Overlapping deletions remain independent. Restored reminders are reconciled against the current clock and do not replay past alarms. Previously shared calendar files are independent copies and cannot be withdrawn.

All task/appointment consumers use the managed schedule through FamilyState helpers or the pure domain resolvers. No data migration, purge, automatic collection initialization or historical backfill is required. Old clients do not understand operations: publish the current rules and update both phones together.

## Calendar editor handoff

The appointment details action **Im Kalender speichern** builds an Android CalendarContract ACTION_INSERT intent for one managed, currently visible occurrence. The pure CalendarEventCopy mapping supplies its actual title/location/recipient/notes, resolved Berlin timestamps, or UTC midnight/exclusive end for all-day dates. No recurrence, internal IDs, account identifiers or attendee addresses are included. An installed calendar handler owns review, destination calendar, saving and reminders. No calendar read/write permission, provider query, backend mutation or ongoing synchronization is added. There is no saved-status claim or retained external event ID; a missing handler/security failure displays a message with the existing sharing alternative.

## Undated household lists

`listItems` holds shopping/gift definitions with title, notes, optional gift recipient, up to six label IDs, optimistic version, deleted flag and the ID of the last structural activity. There are no due dates or responsible-parent fields. `listLabels` contains stable IDs, group (SHOPPING/OCCASION/GIVER), name, bear-palette color, active flag and version. Archiving preserves references; groups cannot change. Optional presets use deterministic IDs and create only missing documents in a transaction.

`listActivity` is immutable and retains item and label snapshots, actual actor, device action time, server receipt time and optional target confirmation. Creation queues an atomic item/ADDED batch, allowing offline use. BOUGHT records and targeted REOPENED records also queue offline; a concurrent unseen purchase survives reopening. Bought state is reduced from un-retracted purchases and never overwrites item details. Online edits/deletions/restores compare the item version and atomically write the next definition and matching audit record. Label changes require connectivity and versions. Rules enforce membership, schema, audit pairing, immutable activity, valid label groups and matching purchase-retraction targets; soft deletion never blocks late offline purchase audit.

Home always shows current open shopping after appointments, independently of the selected day/child/parent filters. Both new list pages have inline purchase controls, shared details/editors, archived bought sections and configurable label filters (OR within a group, AND across groups). Gift recipients are Ferdinand/Johann/both; shopping has none. Neither page has parent assignment or calendar controls. Deleted items share Papierkorb and restore their current purchase state. Introductions reuse the device's Alltagsgedanken setting and rotate on page entry.

Verlauf includes adding/editing/buying/reopening/deleting/restoring list items by actual action time. Stored title/label snapshots survive renaming and archiving. Statistics groups shopping separately, gifts by their child recipient and all list actions by their actual author. Purchase totals use each item's earliest un-retracted confirmation, counting concurrent confirmations once; corrections outside a period can change its net totals. Deleted items retain their audit and purchase credit.

## Dictation and completion notices

The four editors use Android's external ACTION_RECOGNIZE_SPEECH activity for titles and notes, defaulting to German. Recognized text fills an empty field or appends with a space/newline; ordinary validation and explicit Save remain. Cancellation/empty results preserve text, and missing/blocked providers leave keyboard input available. No audio is recorded or stored by Kinderalltag and no microphone permission is added. Provider processing may require a network connection. Editor contexts survive configuration changes in the ViewModel and field values use saveable state; account changes clear those contexts.

FamilyActivityNoticeTracker also tracks open/completed task transitions from fully ready server snapshots. The first valid confirmation's authenticated author determines whether a completion notice is for the other parent, independently of credited parent. Own actions, duplicate active confirmations, initial loads/restarts and corrections are silent; a later open-to-done transition can notify again. The separate TASK_COMPLETED preference defaults off and is scoped to account/device. Notification taps carry the occurrence and its actual date; delivered notices are withdrawn on correction or deletion. This remains process-lifetime delivery, without FCM, a foreground service or closed-app push.

## Reviewed shopping voice lists

The tree in the top-bar title anchors the secondary-page menu; gift ideas are a secondary destination, leaving four primary tabs. Shopping's separate voice-import dialog reuses the external dictation launcher, splits only explicit German list separators, preserves decimal commas, and presents editable one-item-per-line review text. Review text is not re-split on “und”, allowing manual compound-product corrections. A pure normalizer skips within-import duplicates and currently known open titles. Validation bounds new imports to 50 items and normal title/label limits. The ViewModel rechecks known open titles before enqueueing; existing individual item/audit batches avoid Firestore's aggregate rule-access limit and remain offline-compatible. This is not an all-or-nothing batch or cross-phone uniqueness constraint. No save occurs from recognition itself.

## Task overview chronology and per-series completion cells

Pure taskOverviewTimings and chronologicalTasks derive actual managed execution dates/times for each definition. Upcoming views use future execution candidates; calendar-filtered views use the selected period. Definitions are sorted before task paging. The first actual day/times drive one-off card date presentation, including moved slots. Paused definitions remain in their optional appended group.

The reusable task-completion detail component resolves actual occurrences over natural calendar months, then independently reduces confirmations per occurrence: active confirmation first, otherwise due/missing versus upcoming using the Berlin instant. It uses the same managed scheduling, pinning and deletion masks as home/reminders, avoiding invented gaps or rewriting audit history. Both task-definition and home-occurrence details use it. Color is complemented by counts, explicit legend, numbered day cards, segmented completion rings and accessible/tappable per-time status details. No listeners, writes or new stored aggregates are introduced.

The monthly completion aggregator exposes padded Monday–Sunday weeks, with null cells outside the selected month, and includes every real calendar date (including leap days) while retaining empty days separately from missing confirmations. It counts executions rather than days: confirmed/(confirmed + due-unconfirmed), with no percentage when the denominator is zero. Early confirmations count as confirmed; upcoming executions do not depress the result. The UI uses a seven-column Monday–Sunday grid of numbered cards, a sunshine/sage header and a result panel, following the paper-tracker layout supplied as a visual reference. It renders native Compose graphics and does not load the reference bitmap.
