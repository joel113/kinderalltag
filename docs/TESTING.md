# Validation

## Automated

```sh
./gradlew spotlessCheck :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
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
5. Edit a schedule and pause another task. Upcoming times later today change immediately; started and confirmed instances stay unchanged. Resume and verify the next eligible time, without replaying earlier times.
6. Browse earlier dates, filter by child, acknowledge an earlier unconfirmed occurrence, and check the actual acknowledgement date/time is visible.
7. Test Monday-only, one-off, end-date boundaries, and several daily times. Unit tests cover the Berlin DST transitions independently of the phone timezone.
8. Enable reminders on one phone only. Verify permission denial is handled, a reminder opens the right detail, and a known completion suppresses it. Repeat with reboot and an edited schedule. Test offline stale reminders and verify the cached-status message.
9. Sign in with an unprovisioned account: only the waiting-for-access screen appears. Confirm direct database reads/writes are denied by emulator tests.
10. Install a signed release APK on both phones. Increase the version code, rebuild with the same key, and update over the installed release. Verify sign-in, local notification preference, and shared data remain.

## Build status in the implementation environment

Validated locally for recoverable deletion, immediate schedule edits and calendar-editor copies:

- **145 JVM JUnit tests pass**, compiling the actual domain/UI utility/test sources with Android Studio's bundled Kotlin 2.4 compiler and cached JUnit 4.13.2. The project pins Kotlin 2.2.21, so run its Gradle tasks before release.
- All application Kotlin sources, including Compose screens, compile against cached Android/Firebase/Compose dependencies using the local Compose compiler plugin.
- The Firestore test file passes JavaScript syntax checking. The added emulator cases cover immutable scoped deletion/restoration, paired version controls, stale writes, schema/actor validation, schedule snapshots and protected-start edit validation.

The standard Gradle test/lint/APK command cannot start in this sandbox because the Gradle distribution lock is not writable; the isolated-cache attempt is also blocked by socket permissions. Emulator tests are **unrun** because the required Firebase npm packages are unavailable offline (`ENOTCACHED`). These local compiler checks produce no installable APK and do not replace Android Lint, rules emulator tests or two-phone checks. Run the commands above in a connected development environment before release.

## Appointments and responsibility acceptance

- Publish the updated rules and install the update on both phones. Existing tasks must show `Beide Eltern`. Both member names must appear in assignment pickers; unrelated accounts must still be denied access.
- Assign tasks to each parent and both parents. Verify `Nur meine` includes shared items, either parent can acknowledge any task, and responsibility changes affect upcoming instances immediately without rewriting started or confirmed history.
- Create appointments for each child, both children, and Haushalt. Check that both-child appointments appear under each child filter but only once under Alle. Verify the Termine agenda, 90-day paging, Today placement, and timestamped Verlauf.
- Check single appointments, positive day/week/month/year intervals, optional inclusive end dates, monthly January 31 recurrence, and annual February 29 recurrence.
- Move/cancel one occurrence, including moving outside the current agenda window. Confirm later occurrences keep their original schedule. Change/delete this and following occurrences; confirm single exceptions and earlier dates remain. A following edit must not erase an occurrence already moved into history.
- Edit the same occurrence on both phones: the stale editor must show a reload message without overwriting the first edit. Cancel a series on the other phone and verify stale edits are rejected.
- Verify past/started appointments cannot be edited, appointment edits fail visibly offline, and cached viewing still works. Reconnect and verify both phones converge.
- Enable reminders on both phones. Confirm assignment filtering, timed advance reminders across midnight, all-day reminders at 09:00 the previous day, reboot restoration, and withdrawal after cancellation/reassignment once synchronized. Tapping an old reminder must resolve current details or report cancellation.

The new domain tests also cover recurrence anchors, moved-in exceptions, history-protection boundaries, stable notification lookup, reminder ordering/limits, parent eligibility, and legacy assignment defaults. Firestore tests cover the parent roster, valid/invalid assignments, appointment schemas, immutable definitions, optimistic versions, and exception targeting.

## Sync, locale pickers, and sharing acceptance

- With an older ruleset on a test Firebase project, confirm denied parent/appointment access identifies the affected collections and requests rule publication. Publish the current rules and tap Erneut versuchen; existing records must remain visible and all listeners must recover without restarting. Check offline and malformed-data errors have distinct messages. Repeat retry while an acknowledgement is pending and confirm it is not lost.
- On German and US locales, test native date/time dialogs in both editors, task time add/edit/remove (1–8 unique slots), optional end-date clearing, and the history date picker. Change the phone's 12/24-hour setting and verify displays and picker agree. Check stored Firestore dates/times remain ISO and the family timezone remains Berlin.
- On Heute, verify the date and both counters precede Aufgaben, then Termine. Check empty sections, child/responsibility filters, and automatic appointment counts before/after end time (including all-day appointments). Verify the home screen starts with the summary card, without a Heute heading, and the date is plain text. Check green task and blue appointment bars at zero, partial, and full progress, including empty sections (full progress). Verify each bar has a small sun on its left and crescent moon on its right. Use the rounded green arrows to navigate several days backward and forward, then use Zurück zu heute; tapping the Heute navigation item must return to today. Verify both yellow add buttons remain side by side on narrow/large-font screens. Check the Termine introduction box and the combined activity feed in Verlauf.
- Share a timed and an all-day appointment to a calendar/email/file app. Confirm Unicode, commas, semicolons, and multiline notes survive and the all-day end date is exclusive. The share contains one occurrence and no parent UID.
- Share/open an `.ics` attachment from another app and import through the in-app file picker. Verify there is no database write until the review is saved, and signing in or restarting the process retains pending items. Cancel a review, reopen it via Prüfen, and skip an item. Test multiple files/events, exact duplicates, an existing similar appointment, unreadable URIs, unsupported recurrence, and the file/event limits.
- Test UTC/floating/foreign-zone and recurring imports, a recurrence starting in the past, month-end/leap-day warnings, missing end times, unsupported reminders, and malformed files. Required review acknowledgements must block Save until accepted. Reimporting from another calendar creates reviewed copies rather than updating the external calendar.

Date/time format, sync diagnostic, and iCalendar exchange tests are pure JVM tests alongside the domain suite. Actual native dialogs, external-app URI grants, process restoration, and cross-phone synchronization require device verification; source/resource checks do not establish those passed.

Current automated verification and its limits are recorded under Build status above. Cross-app handoff and native picker interactions remain device acceptance checks.

- Confirm a task with **Erledigt von** set to yourself, then another task on behalf of the other parent. Check both phones and Verlauf: credit goes to the selected parent, with “eingetragen von” showing the actual author for delegated entries. Retract and reconfirm; verify legacy confirmations still display, offline confirmations sync, counters update, and a task assigned to one parent can be credited to the other. Verify sun and moon are both yellow.

- Open Aufgaben and verify the heading **Euer Alltagshelfer** and its family-life message. Leave and reopen Aufgaben repeatedly: messages rotate through the collection without consecutive repeats. Changing filters, editing tasks, or rotating the phone must keep the current message.

- Reopen Termine repeatedly and check that the introduction message rotates without consecutive repeats. Filters, appointment editing and phone rotation retain the current message. Verify “Kalenderdatei importieren” and “Kommende Termine” are absent from the agenda, date groups remain, and receiving calendar files through Android sharing/opening still works.

- In Termine, use the green arrows in the date-range card to browse consecutive 90-day periods. Check boundary appointments (day 90/91), empty ranges, child/parent filters, and “Ab heute anzeigen”. The previous arrow is disabled in the first period; appointment actions appear in Verlauf by action time. Check the card on narrow screens with large text.

- Verify the navigation card appears at the bottom of Aufgaben and Termine, before the end-of-page message, including empty lists. On Aufgaben, check 0, 90, 91 and 181 tasks: at most 90 cards per page, correct count ranges, no gaps/duplicates, disabled boundary arrows, and scrolling to the top after paging. Tasks starting beyond 90 days remain visible; ended tasks are excluded. Filters reset to the first page, and synchronized deletions/pauses clamp stale pages. Paused tasks can be shown and resumed, also within the 90-card page limit. Termine retains its independent 90-day navigation.

- Verify Verlauf is a combined activity list with newest actions first and day overview boxes, without task/appointment selectors or calendar cards. Create/edit/pause/resume a task, confirm it on behalf of either parent, retract and reconfirm. Create, edit and cancel single and recurring appointments, including changes to following appointments. Each successful action appears on both phones by its actual action timestamp, with the scheduled/effective day separate. Repeated edits retain all activity rows.
- Browse 90-day history periods with the bottom bar; verify exact boundaries, DST, empty periods, newer-arrow disabling in the current period and “Zum aktuellen Verlauf”. Check child, both-child, household and Nur meine filters. Offline task confirmations should show pending sync and retain the recorded action date when synchronized later. Failed appointment transactions must leave neither a calendar mutation nor an activity event. After publishing rules, retry sync and verify all eight subscribed collections load.
- Open legacy data: existing appointment creations and the last retained single edit can appear, labeled “Aus vorhandenem Datenstand”; repeated older edits and un-timestamped series cutoffs are not fabricated. Ensure matching activity snapshots suppress legacy duplicates and a split does not show the replacement series as a second creation.

- Scroll to the end of Heute, Aufgaben, Termine, Verlauf and Einstellungen. Each page ends with a horizontally centered garden illustration and “Du bist am Ende angekommen.”, followed by “Ein kleiner Moment zum Durchatmen.” Check empty states, paged lists, narrow screens and large text: the footer stays after the content/navigation, wraps cleanly and remains clear of add buttons and bottom navigation. The message describes the end of the page even when tasks remain open; decorative illustrations are not announced separately by TalkBack.

- In Verlauf, each day's overview shows one colored rectangle per filtered event, in the same newest-first order as the rows below it. Task completions are green, corrections coral, task edits and appointment changes blue, new appointments yellow, cancellations burgundy. Every row has the matching small rectangle and a readable action label, so color is supplementary. Check many events wrapping on narrow screens, large text, filters, legacy entries, and TalkBack (no individual decorative markers announced).

## Statistics acceptance checks

- Open **… → Statistik** in the top bar. Verify the garden banner, two overview counts, color legend, parent/child/weekday charts, bottom period bar and worm footer. Check the three primary navigation labels and the secondary-page menu, settings gear/back behavior, task/appointment add buttons and notification routes on a narrow phone and with large text. Statistics has no add button or shared child/parent filters.
- Check 7/30/90-day switches, adjacent older/newer periods, disabled newer arrow for the current period, reset control and scrolling back to the top after changing periods. Duration changes reset to the latest period; leaving/returning and rotating retain the chosen duration/page. Include midnight and DST boundaries in Europe/Berlin.
- Compare counts with Verlauf for the same dates. Each stacked bar includes numeric totals and accessible event-kind descriptions. Zero-event periods still show both parents, all subject categories and all seven weekdays, without invalid bar sizes. Verify unknown and historical parent references, both-child appointments counted once, and pending/reconstructed-data notices. Every breakdown must reconcile with the overall event and completed-task counts.
- Confirm on behalf of another parent, confirm the same occurrence on both phones, then retract one or all confirmations. Raw events retain all actions; completed tasks count once while valid. Credit/date move to the earliest remaining confirmation after a correction. Confirm that a duplicate inside a period does not recount an earlier valid confirmation outside it, and that a retraction outside the selected period removes the earlier net completion. Test pending offline events followed by synchronization.

## Settings, live notifications and Impressum

- Check the grouped settings cards on narrow screens and large text, profile editing, account sync/retry, app version, sign-out restrictions and the heart footer. Open Impressum; verify all supplied contact details, selection/copy, email and website actions, scrolling and dismissal. No email or browser opens before tapping its action.
- On two connected phones with the app running, enable **Neue Aufgaben** and **Neue Termine** separately. Add a task/appointment from the other parent: each enabled category should notify once and open the correct page when tapped. Own additions, task edits/pauses/resumes and appointment edits/cancellations/splits must remain silent. Existing scheduled reminders remain functional.
- Check first install/sign-in, first cached then complete server sync, process restart, account changes, retry and reconnection: no initial history replay or duplicate alerts. While the same process reconnects, newly received additions may notify after confirmation. Disable a category, receive new data, then re-enable: no backlog. Force-stop the app and verify no claim of closed-app push delivery.
- Test Android 13+ permission grant/denial for each switch, rotation during the permission dialog, permission revocation/system-channel blocking and the Android settings link. Preferences persist per account/device. Disabling reminders must not remove activity alerts; disabling one live category must not remove the other. Sign-out/access loss clears family activity notifications.

- In Einstellungen → Darstellung, verify **Alltagsgedanken anzeigen** is off by default. Both Aufgaben and Termine introductions must be absent. Enable it and verify both boxes and their rotating messages appear; disable it and check both disappear without changing their lists. Relaunch/rotate and verify the device preference persists. Other banners are unaffected.

## In-app month calendars

- Check Aufgaben and Termine month cards: Monday-first weekday labels, four to six complete weeks, today outline, selected-day highlight, adjacent-month days and indicators. Browse across year/leap-month boundaries and tap days in the adjacent month. Check narrow screens, large fonts and spoken full dates/counts with TalkBack.
- Filter by child/responsibility and verify markers match each day's list. Task markers count each definition once despite multiple times; weekday rules, end dates, effective edits and pauses must agree. Selected historical task dates show no edit/pause controls. Current/future editing uses the current editable definition, preserving started/confirmed instances while applying upcoming edits immediately.
- For appointments verify recurring, all-day, shared/both-child, canceled and moved-in events on their actual dates. A selected past day includes elapsed appointments; editing restrictions remain. Clearing the selected day restores upcoming 90-day periods and their bottom bar. Aufgaben clearing restores its 90-task paging and paused-task option. Switch tabs/rotate/relaunch and check calendar/date persistence and the independent introduction preference.

- Switch Tag/Woche/Monat independently on Aufgaben and Termine: the list must reflect the focused day, Monday–Sunday week, or calendar month. Tapping a date enters Tag; switching back to Woche/Monat preserves the focus date. Arrows step by the selected unit, including year boundaries and leap-month ends. Reset restores the full list and current month. Check multiple task revisions in one week/month still produce one card per task, including once-only/weekday tasks and effective pauses.
- Verify compact heights: month rows are shorter with no unused trailing weeks, week shows one row, day shows only the date/count and agenda below. At large font sizes text must grow without clipping. Confirm view/focus/filter restoration after rotation and switching tabs, independent calendars, correct accessible arrow labels, and no regression to 90-task or 90-day paging when the calendar filter is reset.

- Verify calendar title previews for Aufgaben and Termine: Monat shows the first entry plus a remaining count, Woche up to two, Tag up to three with localized times (or “Ganztägig”). More entries remain in the list below. Check long titles, overflow counts, empty dates, multiple task times, effective title changes, moved/canceled appointments and existing child/parent filters. Tapping a grid preview opens its day; previews remain compact at large font sizes, and TalkBack exposes full preview titles and counts.

- Verify the bottom navigation contains only Heute/Aufgaben/Termine. From each primary page, open **… → Verlauf** and **… → Statistik**. Check menu icons, selection/dismissal, page title, back arrow and Android Back, which return to the previous primary page. Switch between secondary pages and settings without losing that return destination or their periods. Check rotation, notification/deep-link routes, narrow screens and large text. The settings gear remains accessible, and secondary pages have no add buttons.

## Deletion, restoration and immediate-edit acceptance

1. Publish rules first and update both phones. Verify all eight subscribed collections synchronize, an unrelated account remains denied, and no manual collection creation is needed.
2. Create a daily 08:00/18:00 task. Delete only one time, then following times from an 18:00 anchor; the earlier same-day time remains. Repeat from past and completed instances, and with a one-off task. Check day/week/month previews, home progress, lists, reminders and notification links on both phones.
3. Delete a whole task or appointment series, including known legacy splits. Verify unrelated/unlinked legacy series remain. Original completion credit and audit history must survive; deletion/restoration appear with actor and action time in Verlauf and Statistik.
4. Delete one occurrence and its whole series separately. Restore either in Papierkorb: the other mask still applies. Make an edit before deleting, restore, and check the edited schedule returns. Either parent can restore; neither can physically erase immutable records.
5. Move a future task/appointment to another date twice, then delete/restore it. Check its original ID, no ghost on either previous date, no duplicate, and correct future alarm. Following edits must retain explicit single changes, legacy cancellations, month-end rhythm and items moved into the past. Previously exported files remain unchanged.
6. Edit/pause/resume at midday: earlier tasks and started appointments stay unchanged, later times change immediately. Test weekday/end boundaries, newly added/removed time slots, and moving a time before now. Confirm a future task first: later edits must preserve its complete snapshot, even across legacy future-effective versions.
7. On both phones open the same editor, save on one, then save/delete on the other: reject the stale mutation without partial audit/control writes. Retry after reopening. Test offline structural changes fail visibly while offline care confirmations still queue and synchronize.
8. With one phone offline, confirm an occurrence the other parent then moves or removes. Reconnect and retain the recorded confirmation snapshot and credit. Confirm the same original ID on different actual dates, retract visible confirmations and verify both dates are corrected without rules rejection. Repeat with a deletion mask and restore.
9. Reconcile reminders after deletion, completion, reassignment and restoration; cancel stale delivered notifications and do not replay past alarms. Verify a paused task resumes with the same slot identities and previous single deletions remain effective.

## Calendar visibility and action alignment

- Disable **Einstellungen → Darstellung → Kalenderansicht anzeigen** after selecting a historical day/week/month in each calendar. Both calendars disappear, Aufgaben returns to its upcoming task list, and Termine returns to the current 90-day agenda with paging. Enable again: calendars start at the current month. Relaunch/rotate and check the switch persists on this phone, independently of Alltagsgedanken.
- Task and appointment overview cards open details when tapped and show a decorative chevron. Task cards have no Bearbeiten/Pausieren/Löschen buttons; paused and historical labels remain visible. In task details verify Bearbeiten/Fortsetzen, Pausieren and red Löschen, including the existing pause confirmation and deletion scopes.
- Verify both detail dialogs share a heading with a close button, grouped metadata/notes, scrollable content and a fixed action area. Actions wrap on narrow screens and large text. Löschen has red text, icon and outline; ordinary actions are blue. Appointment details retain scope selection, sharing and editing; past items retain deletion only. Home task details retain parent credit selection, offline/late confirmation, all acknowledgement history and two-step correction.
- While details are open, change/delete the item on the other phone; stale controls must disable or saving must reject the old version. Check paused task resumption, calendar-selected historical tasks, offline controls, dismissal and busy state. Check small landscape screens, long titles/notes and TalkBack.

## Save to calendar acceptance

- On phones with Google Calendar and Samsung Calendar, open a timed Termin and tap **Im Kalender speichern**. Verify the event editor opens, title/current date/start/end/location/notes are prefilled, and the calendar/account can be chosen. Save and cancel separately: neither action changes Kinderalltag records or shows a false save confirmation. No calendar permission is requested. Check with both handlers installed/defaults set, and with no handler (clear message; Teilen remains available).
- Copy a moved/edited instance of a recurring appointment, a past occurrence, and a shared/both-child appointment. Verify only the selected current occurrence is copied; no recurrence, original-series ID or member UID is transferred. Test blank/long notes and Unicode, landscape/large text, fixed-footer wrapping, stale details, offline viewing and busy controls.
- Copy all-day appointments on the Berlin spring/autumn DST transitions; the selected date must remain a single all-day date. Copy timed events across offsets, including while the phone uses a non-Berlin timezone. Check native editor display and saved event agree with the intended instant/date. Reminder defaults are reviewed in the calendar editor.
- Change/delete the original after saving a copy: the independent external event remains. Repeated exports may duplicate; no ongoing synchronization is promised. Existing .ics sharing/import must still work.

CalendarEventCopyTest covers current moved details, Berlin timed instants, all-day UTC/exclusive boundaries across DST, valid resolved DST-gap duration, and recipient/notes without database identifiers. External calendar app behavior requires these physical-device checks.

## Continuous integration and formatting

`.github/workflows/android.yml` uses JDK 21 and Android SDK 36. It applies pinned Spotless/ktfmt formatting to all app/test Kotlin and root/app Gradle Kotlin scripts, then checks formatting, Android lint, JVM tests and debug assembly. `--continue` lets independent Gradle checks report failures in the same run. A final diff check rejects formatting that has not been committed; `android-reports` includes the patch, lint HTML/XML, test HTML and JUnit XML. The `kinderalltag-debug` artifact is the setup-screen APK, without Firebase configuration. Reports are retained for 14 days and APKs for 7 days. CI does not commit, push, publish rules or deploy an app.

A separate Firestore job installs the pinned direct npm dependencies and runs the existing tests against `demo-kinderalltag` only. It needs no service account or production Firebase credentials. Dependency downloads and a network-enabled GitHub runner are required.

Before committing, run `./scripts/install-hooks.sh` once. The tracked `.githooks/pre-commit` runs `spotlessApply`, `spotlessCheck` and `:app:lintDebug` for Android/build changes. Documentation/backend-only changes skip Gradle. Unstaged Android/build edits block the hook before formatting, so partially staged content is preserved. Formatting changes are left for review and explicit staging; the hook never stages files. If another hook configuration exists, the installer refuses to replace it. Run the hook manually with `.githooks/pre-commit` after staging changes.

Hook behavior can be verified without Android dependencies:

```sh
python3 -m unittest discover -s scripts/tests -v
sh -n .githooks/pre-commit scripts/install-hooks.sh
```

Local validation for this automation: all **9 hook tests passed**, shell syntax passed, and both workflow jobs parsed successfully as YAML. Actual Spotless/lint/test/APK execution could not start because the sandbox blocks the Gradle distribution lock; dependency downloads also fail DNS resolution. Installing the hook in this checkout was attempted but `.git/config` is not writable in the sandbox. Run `./scripts/install-hooks.sh` in your normal terminal. The existing sources have **not** yet been reformatted by ktfmt; run `./gradlew spotlessApply` before the first CI pass (or review the patch that the first workflow run produces). No GitHub-hosted run has been triggered or observed here.
