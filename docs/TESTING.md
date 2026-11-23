# Validation

## App themes

Local validation: all **246 Android JVM tests** pass, together with Spotless, Android Lint and debug assembly. The preview composables compile as part of the app build.

`AppThemeTest` checks stable saved IDs and Garden fallback, readable text/button contrast for all palettes, Garden palette compatibility, preserved child color choices and complete theme-specific illustration mappings. `ThemePreviews.kt` provides parameterized Garden/Play/Space/Animals/Vehicles/Nature/Farm/Magic previews for Home, display settings, task details, the monthly tracker and artwork/bear galleries. Home also has a 320dp/large-font preview. They use sample data and do not require Firebase.

Device acceptance: switch **Einstellungen → Darstellung → Design** between Garten, Spielwelt, Weltraum, Tierwelt, Fahrzeugwelt, Naturwelt, Bauernhof and Magische Welt; confirm immediate changes, persistence after restart and independent choices on two phones. Check Home, tasks, appointments, shopping, gifts, history/statistics, trash, calendars, detail/edit dialogs, empty states, footers and login/setup screens. Menus and actions must retain their behavior. Switching must preserve the page, filters and editor values. Confirm both child colors stay selected and that completed/missing/error/delete colors and the sun/moon symbols retain their meaning. Verify legibility at narrow widths and large fonts. Confirm the launcher icon matches each theme, including Android themed icons when enabled, while the worm notification icon stays unchanged. Switch repeatedly, return to the launcher, and open the app using the new icon; confirm there is one app entry and it opens the same data. Check restart and an APK upgrade with a non-Garden theme selected. Test at least one older Android version and Android 13+, including Samsung One UI if available. Existing pinned shortcuts and launcher refresh delays require device checks. Verify calendar sharing/import still reaches MainActivity.

`LauncherIconsTest` checks all theme transitions retain a launcher entry, repairs missing/duplicate selections, verifies manifest aliases/defaults and calendar import filters, and checks adaptive/monochrome asset references.

The SDK in this workspace has no emulator system image, so compiled previews and palette tests do not establish that physical rendering/device acceptance passed. No Firestore changes are required for themes.

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

## Completed-task reminder cancellation

Task reminders are removed from saved alarm records and AlarmManager as soon as an active completion is observed, including pending offline writes and partial/error states in unrelated collections. Already-visible reminder notifications are removed as well; the separate other-parent activity notices retain their own behavior. A queued alarm is revalidated against the current occurrence, completion/correction history, schedule time, responsibility and expiry before delivery. A queued alarm for an old schedule cannot consume a newer future replacement with the same ID.

Cold-started task alarms use `goAsync`, wait up to six seconds for synchronized state, then validate the latest fully loaded state. Loaded cached data supports offline reminders; incomplete or unauthorized state suppresses delivery. Another phone's unsynchronized completion cannot be inferred while offline, so cross-phone cancellation requires that completion to reach this phone. The broadcast result is finished on every exit path. This does not add push infrastructure or new Firebase rules.

Phone acceptance: schedule a reminder, complete the execution before its time, and verify no reminder appears, including after process restart/reboot and offline completion. Repeat while an unrelated collection has a sync error. Complete on the other phone and synchronize; verify both pending and already-visible reminders disappear. Exercise corrections, concurrent confirmations, reassignment, deletion/restoration, moves with stable IDs, and a cold-started receiver. Only the completed execution is suppressed; other dates/times still remind normally. Unit tests cover planning and delivery eligibility; AlarmManager/broadcast timing and notification-tray behavior require device checks.

Validation: 210 Android JVM tests pass, along with Spotless, Android Lint and debug assembly.

## Task repeat intervals

Local validation: **221 Android JVM tests** and **39 demo Firestore rules tests** pass, along with Spotless, Android Lint and debug assembly. Tests cover N-day/N-week rhythm, DST, month-end/leap-day clamping without drift, start/effective/end bounds, sparse next-execution lookup, preview/reminder/overdue/tracker agreement, snapshot round trips and legacy defaults, and immutable started/early-completed executions during interval edits. Rules tests cover valid/invalid interval values and full eight-slot interval confirmations, corrections and future edits within the expression budget.

Publish the updated rules and install this build on both phones. Create tasks every 2/3 days, 2/3 weeks, and 2/3 months. Check their start weekday/day anchor, shorter months, end dates, multiple times, responsibility, calendar and home previews, advance completion and correction, reminder cancellation, overdue replacement and tracker cells. Edit only one execution, then this and following executions; confirm that historical snapshots retain their old interval. Pause and resume a task whose next execution lies more than a week away. Existing tasks without an interval must still repeat as before. UI/picker rendering, alarm delivery and two-phone synchronization require device checks.

## Two-phone acceptance

### Retaining unfinished past tasks

Toggle **Einstellungen → Startseite → Vergangene Aufgaben anzeigen** to show or hide retained tasks. While enabled, set **Vergangene Aufgaben in Tagen** to 1–365 (default 7). Turning the switch off preserves the period; both values persist per phone independently of the future-preview switch. A previously saved zero-day period becomes an off switch with a 7-day period ready for re-enabling. Invalid input retains the last valid saved value.

Unfinished one-off executions inside the lookback remain in the regular Aufgaben list, labelled **Überfällig** with their original date/time. For recurring tasks, only the latest due execution from a previous day may remain; when a newer execution becomes due, the older one disappears even if the newer one was completed. A future preview alone does not replace an overdue execution. Today uses the current Berlin time; browsing another date evaluates that day's full schedule. The daily progress totals still count only the displayed day's executions.

Check inclusive lookback boundaries, disabled retention, completion/correction, weekly gaps, multiple daily times, the exact next due time, child/parent filters, deletion/restoration, Berlin DST and navigation between dates. No schema migration or rules deployment is required.

### Upcoming task preview on the home screen

Under **Einstellungen → Startseite**, enable **Kommende Aufgaben anzeigen**, choose 1–50 tasks (default 5), and enter **Vorschau in Tagen** (1–365, default 7). Both limits are saved on this device. Invalid or empty day input is marked and retains the last valid saved value. The option defaults to off and retains both limits when toggled off. Reopen the app to verify persistence.

Future tasks appear directly in the regular **Aufgaben** list under the matching child/household, after that day's tasks in chronological order. Their cards show a full date/time and **Geplant**; the separate **Nächste Aufgaben** section is removed. Termine follows the combined task list, and Einkäufe remains last. Future items do not affect the displayed day's progress totals.

Verify one next open execution per task after the displayed date, up to and including the final preview day. Test one-offs at midnight on the first day, 23:59 on the final day, and just beyond the configured range; recurring tasks with multiple daily times; advance completions; Berlin midnight/DST; moved/deleted/restored tasks; and expired/paused schedules. Child/parent filters apply before the count limit. Change the home date and verify the window moves with it. Tap a future task to confirm/correct it for either parent; its replacement must remain within the window. No Firestore schema or rules update is needed.

### Confirm executions from Aufgaben

Open a task under **Aufgaben → Ausführungen**, choose a date, and tap its time to open the existing completion dialog. Check one-off and recurring tasks, several times per day, and dates in the future. Select either parent under **Erledigt von** and confirm. Only the chosen execution should become completed; history records the actual acknowledgement time. Correct the confirmation and verify that an unconfirmed future execution becomes upcoming again.

Check dates without an execution, paused/deleted/moved instances, offline confirmations, calendar-filtered task lists, and synchronization to the other phone. Changed or deleted task definitions must not open stale executable actions. This UI change needs no new Firestore rules or migration.

Validation for this change: 186 Android JVM tests and 37 demo Firestore rules tests pass, along with Spotless, Android Lint and debug assembly. Phone rendering and the two-phone flow remain manual checks.

### Rejected-write regression checks

Firestore's 1,000-expression evaluation limit previously rejected valid task confirmations, task/appointment edits, and six-label shopping/gift writes. The rules now use bounded time-list validation, exact schema checks without duplicate field lists, and atomic-item or immutable-history pairing for audit snapshots. Current label snapshots reuse validated stored labels; historical labels validate presentation differences. Membership restrictions and immutable audit history remain enforced.

All **36 emulator tests pass**, covering full Android confirmation snapshots, delegated parent credit, eight-slot correction batches, six-label edits/purchases with archived labels, malformed time arrays, and attempts to bypass item validation through audit records. Publish the complete `firebase/firestore.rules` before the two-phone confirmation check; no data migration is required.

Task-confirmation errors remain visible in the details dialog. Logcat tag `KinderalltagCare` records error codes and source/cache flags without task contents. When investigating access failures, verify the Firebase project, membership document and App Check configuration. Cached content alone does not prove server access. The temporary Settings access-check UI has been removed.

Final local validation passed: `./gradlew spotlessApply spotlessCheck :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` and the complete demo Firestore emulator suite. All 185 Android JVM tests and all 36 rules tests pass. The repository's configured Spotless formatter was applied to existing Kotlin sources and Gradle scripts. Physical two-phone behavior remains a manual acceptance check.

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

- **183 JVM JUnit tests pass**, compiling the actual domain/UI utility/test sources with Android Studio's bundled Kotlin 2.4 compiler and cached JUnit 4.13.2. The project pins Kotlin 2.2.21, so run its Gradle tasks before release.
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
- Browse 90-day history periods with the bottom bar; verify exact boundaries, DST, empty periods, newer-arrow disabling in the current period and “Zum aktuellen Verlauf”. Check child, both-child, household and Nur meine filters. Offline task confirmations should show pending sync and retain the recorded action date when synchronized later. Failed appointment transactions must leave neither a calendar mutation nor an activity event. After publishing rules, retry sync and verify all eleven subscribed collections load.
- Open legacy data: existing appointment creations and the last retained single edit can appear, labeled “Aus vorhandenem Datenstand”; repeated older edits and un-timestamped series cutoffs are not fabricated. Ensure matching activity snapshots suppress legacy duplicates and a split does not show the replacement series as a second creation.

- Scroll to the end of Heute, Aufgaben, Termine, Verlauf and Einstellungen. Each page ends with a horizontally centered garden illustration and “Du bist am Ende angekommen.”, followed by “Ein kleiner Moment zum Durchatmen.” Check empty states, paged lists, narrow screens and large text: the footer stays after the content/navigation, wraps cleanly and remains clear of add buttons and bottom navigation. The message describes the end of the page even when tasks remain open; decorative illustrations are not announced separately by TalkBack.

- In Verlauf, each day's overview shows one colored rectangle per filtered event, in the same newest-first order as the rows below it. Task completions are green, corrections coral, task edits and appointment changes blue, new appointments yellow, cancellations burgundy. Every row has the matching small rectangle and a readable action label, so color is supplementary. Check many events wrapping on narrow screens, large text, filters, legacy entries, and TalkBack (no individual decorative markers announced).

## Statistics acceptance checks

- Open **Baum → Statistik** in the top bar. Verify the garden banner, event/task/purchase overview counts, color legend, parent/child/weekday charts, bottom period bar and worm footer. Check the four primary navigation labels and the secondary-page menu, settings gear/back behavior, task/appointment add buttons and notification routes on a narrow phone and with large text. Statistics has no add button or shared child/parent filters.
- Check 7/30/90-day switches, adjacent older/newer periods, disabled newer arrow for the current period, reset control and scrolling back to the top after changing periods. Duration changes reset to the latest period; leaving/returning and rotating retain the chosen duration/page. Include midnight and DST boundaries in Europe/Berlin.
- Compare counts with Verlauf for the same dates. Each stacked bar includes numeric totals and accessible event-kind descriptions. Zero-event periods still show both parents, all subject categories and all seven weekdays, without invalid bar sizes. Verify unknown and historical parent references, both-child appointments counted once, and pending/reconstructed-data notices. Every breakdown must reconcile with the overall event and completed-task counts.
- Confirm on behalf of another parent, confirm the same occurrence on both phones, then retract one or all confirmations. Raw events retain all actions; completed tasks count once while valid. Credit/date move to the earliest remaining confirmation after a correction. Confirm that a duplicate inside a period does not recount an earlier valid confirmation outside it, and that a retraction outside the selected period removes the earlier net completion. Test pending offline events followed by synchronization.

## Settings, live notifications and Impressum

- Check the grouped settings cards on narrow screens and large text, profile editing, account sync/retry, app version, sign-out restrictions and the heart footer. Open Impressum; verify all supplied contact details, selection/copy, email and website actions, scrolling and dismissal. No email or browser opens before tapping its action.
- On two connected phones with the app running, enable **Neue Aufgaben** and **Neue Termine** separately. Add a task/appointment from the other parent: each enabled category should notify once and open the correct page when tapped. Own additions, task edits/pauses/resumes and appointment edits/cancellations/splits must remain silent. Existing scheduled reminders remain functional.
- Check first install/sign-in, first cached then complete server sync, process restart, account changes, retry and reconnection: no initial history replay or duplicate alerts. While the same process reconnects, newly received additions may notify after confirmation. Disable a category, receive new data, then re-enable: no backlog. Force-stop the app and verify no claim of closed-app push delivery.
- Test Android 13+ permission grant/denial for each switch, rotation during the permission dialog, permission revocation/system-channel blocking and the Android settings link. Preferences persist per account/device. Disabling reminders must not remove activity alerts; disabling one live category must not remove the other. Sign-out/access loss clears family activity notifications.

- In Einstellungen → Darstellung, verify **Alltagsgedanken anzeigen** is off by default. Aufgaben, Termine, Einkaufsliste and Geschenkideen introductions must be absent. Enable it and verify all four boxes and their rotating messages appear; disable it and check they disappear without changing their lists. Relaunch/rotate and verify the device preference persists. Other banners are unaffected.

## In-app month calendars

- Check Aufgaben and Termine month cards: Monday-first weekday labels, four to six complete weeks, today outline, selected-day highlight, adjacent-month days and indicators. Browse across year/leap-month boundaries and tap days in the adjacent month. Check narrow screens, large fonts and spoken full dates/counts with TalkBack.
- Filter by child/responsibility and verify markers match each day's list. Task markers count each definition once despite multiple times; weekday rules, end dates, effective edits and pauses must agree. Selected historical task dates show no edit/pause controls. Current/future editing uses the current editable definition, preserving started/confirmed instances while applying upcoming edits immediately.
- For appointments verify recurring, all-day, shared/both-child, canceled and moved-in events on their actual dates. A selected past day includes elapsed appointments; editing restrictions remain. Clearing the selected day restores upcoming 90-day periods and their bottom bar. Aufgaben clearing restores its 90-task paging and paused-task option. Switch tabs/rotate/relaunch and check calendar/date persistence and the independent introduction preference.

- Switch Tag/Woche/Monat independently on Aufgaben and Termine: the list must reflect the focused day, Monday–Sunday week, or calendar month. Tapping a date enters Tag; switching back to Woche/Monat preserves the focus date. Arrows step by the selected unit, including year boundaries and leap-month ends. Reset restores the full list and current month. Check multiple task revisions in one week/month still produce one card per task, including once-only/weekday tasks and effective pauses.
- Verify compact heights: month rows are shorter with no unused trailing weeks, week shows one row, day shows only the date/count and agenda below. At large font sizes text must grow without clipping. Confirm view/focus/filter restoration after rotation and switching tabs, independent calendars, correct accessible arrow labels, and no regression to 90-task or 90-day paging when the calendar filter is reset.

- Verify calendar title previews for Aufgaben and Termine: Monat shows the first entry plus a remaining count, Woche up to two, Tag up to three with localized times (or “Ganztägig”). More entries remain in the list below. Check long titles, overflow counts, empty dates, multiple task times, effective title changes, moved/canceled appointments and existing child/parent filters. Tapping a grid preview opens its day; previews remain compact at large font sizes, and TalkBack exposes full preview titles and counts.

- Verify the bottom navigation contains Heute/Aufgaben/Termine/Einkaufsliste. From each primary page, open **Baum → Verlauf** and **Baum → Statistik**. Check menu icons, selection/dismissal, page title, back arrow and Android Back, which return to the previous primary page. Switch between secondary pages and settings without losing that return destination or their periods. Check rotation, notification/deep-link routes, narrow screens and large text. The settings gear remains accessible, and Verlauf/Statistik/Papierkorb/Einstellungen have no add buttons; Geschenkideen retains its add button.

## Deletion, restoration and immediate-edit acceptance

1. Publish rules first and update both phones. Verify all eleven subscribed collections synchronize, an unrelated account remains denied, and no manual collection creation is needed.
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
- Verify both detail dialogs share a heading with a close button, grouped metadata/notes, scrollable content and a fixed action area. Detail actions use a shared footer: the main action comes first, secondary actions have equal-width/equal-height columns, and Löschen occupies a separate full-width last row. Narrow screens or large text switch to a single column; unusually tall footers scroll independently so every button remains reachable. Löschen has red text, icon and outline; ordinary actions are blue. Appointment details retain scope selection, sharing and editing; past items retain deletion only. Home task details retain parent credit selection, offline/late confirmation, all acknowledgement history and two-step correction.
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

Install workflow tools with `./scripts/install-dev-tools.sh` (Node 20+, npm and Go 1.25+), then enable hooks with `./scripts/install-hooks.sh`. The installer preserves existing hook configurations. The tracked `.githooks/pre-commit` dispatches app and workflow checks independently:

- `.githooks/pre-commit-android`: `spotlessApply`, `spotlessCheck` and `:app:lintDebug` for staged Android/build changes.
- `.githooks/pre-commit-workflows`: pinned Prettier YAML formatting and actionlint for staged workflow/tooling changes. It checks all tracked workflows, includes staged additions, and handles deletions. Untracked drafts remain untouched. actionlint's optional ShellCheck/Pyflakes integrations are disabled to keep the checks consistent across machines.

Both scopes are checked for unstaged edits before either formatter runs. Partial staging blocks only relevant checks; docs/backend-only commits skip both. Formatter changes remain unstaged for review and block the commit until staged. Lint errors and missing tools also block commits with actionable output. The hooks never install workflow tools or stage files automatically; Gradle may resolve its usual dependencies.

The workflow CI job installs the same pinned tools, then runs `npm run check:workflows`. Local checks:

```sh
python3 -m unittest discover -s scripts/tests -v
sh -n .githooks/pre-commit .githooks/pre-commit-android .githooks/pre-commit-workflows scripts/*.sh
npm run format:workflows
npm run check:workflows
./gradlew spotlessCheck :app:lintDebug
```

Local validation: all **19 hook tests pass**, shell syntax checks pass, the real Prettier/actionlint workflow check passes, and `./gradlew spotlessCheck :app:lintDebug` passes. The combined hook is enabled in this checkout through `core.hooksPath=.githooks`.

Hook tests use disposable Git repositories and stub tools to cover app/workflow routing, formatter and linter failures, missing tools, partial staging, preservation of the index, untracked workflows, deleted workflows and installer conflicts. These test hook orchestration; run the real workflow and Gradle checks above to validate their toolchains. GitHub-hosted execution is separate from local validation.

## Shopping, gifts, dictation and completion-alert acceptance

- Publish rules and update both phones; all eleven collections must load without manual seeding. Verify four bottom labels, narrow/landscape layouts, large fonts, icons, add buttons and back behavior from secondary pages. No child/parent filters or calendars appear on shopping; gifts have child filters but no parent assignment/filter. Items have no due dates. Verify all four optional introductions rotate when reopened and stay unchanged while filtering.
- Add undated shopping and gifts for either/both children, including quantities in shopping titles. Shopping appears last on home above its footer, including with other home dates, child filters and Nur meine. Gifts stay on their own page. Check title/notes limits, editing, clear stale/offline errors and details; title and label edits must not alter old history rows.
- Configure all three label groups from each phone: create/rename/recolor/archive/reactivate and apply several labels. Presets are optional and simultaneous/repeated preset requests must not duplicate preset IDs or reactivate archived presets. Archived references remain on existing items and history; new selections exclude them. Verify OR within a group, AND across occasion/giver groups, all-label reset and the six-label bound.
- Offline, add items, buy/reopen them, close/reopen the app and reconnect. No spinner should wait indefinitely for server confirmation; pending data stays visible. Concurrent purchases retain both actors, count once and require targeting both known confirmations to reopen. An unseen concurrent purchase survives a correction. Edits/deletions/label changes require a connection and stale versions reject without partial audit writes.
- Buy then delete/restore items through Papierkorb. Purchase state and audit must remain; a late offline purchase on a deleted item cannot resurrect it. Verify the bought sections default collapsed, checkbox accessibility and open/purchased sorting. Compare Verlauf and all statistical dimensions, including both-child gifts counted once, shopping's own subject row, corrections across period boundaries and actor-based parent credit.
- Dictate titles/notes in all four editors, including nonempty fields, Unicode, long results, cancellation, empty results, unavailable/blocked recognition and network loss. Text must remain editable, append without overwriting, respect Save validation and never submit automatically. Rotate while the provider is open and verify editor context/fields survive; check navigation/account changes cannot apply a result to another editor. Verify the app requests no microphone permission.
- Enable Aufgabe erledigt on the receiving phone. Other-parent confirmations notify once, including when credited to the receiver; own entries credited to the other parent remain silent. Concurrent duplicate confirmations must not duplicate alerts. Correct the completion (withdraw notification), complete again, delete its occurrence and tap older notices. Verify moved-date links, permission/channel denial, preferences, cached/pending/reconnected snapshots, account changes, process restarts and no backlog after enabling. Closed-app push is not promised.

Current local validation: **183 JVM tests passed** and all application Kotlin/Compose sources compiled using the cached compiler/dependencies. The JavaScript rules test file passes syntax checking. Full Gradle/Spotless/lint/APK checks remain blocked by Gradle lock-file sandbox permissions; emulator dependencies are not available offline (`ENOTCACHED`), so added emulator cases have not run here. Physical speech-provider, notifications and two-phone checks remain pending.

## Tree navigation and multi-item voice shopping

- Verify four bottom destinations; the tree is a labeled button with a 48dp touch target opening Geschenkideen/Verlauf/Statistik/Papierkorb. No ellipsis button remains. Open from each primary/secondary page, dismiss/back, select gifts and add/edit there, then return to the previous primary page. Check settings, repeated gift-entry messages, rotation, narrow screens, large text and TalkBack.
- From Einkaufsliste use the yellow + Einkauf button to open Einkäufe hinzufügen. Recognize “Milch, Brot und Eier”, explicit “nächster Artikel” and quantities such as “1,5 Liter Milch”. Verify separate editable rows and no write before confirmation. Append a second utterance, cancel recognition/import, type/paste, delete rows and merge “Essig und Öl” back to one product. Missing/blocked providers preserve the review and keyboard entry.
- Check repeated titles/case/whitespace and already-open products are skipped visibly, while a previously bought product can be added again. Test 0/1/50/51 items, long titles and common labels (including six-label bound and concurrent archiving). Invalid input is rejected without truncation or partial enqueue. Verify on both phones, offline/reconnect, history/statistics and pending/error messages; each entry/audit pair is atomic, not the entire import. New items are independent IDs and concurrent unknown additions may duplicate.

- Verify both the yellow shopping add button and home's shopping add control open the same Einkäufe hinzufügen dialog. There must be no separate Mehrere Einkäufe button. Test one typed item, several typed lines, voice import, cancel and reopening with a clean draft; editing an existing shopping item and adding/editing gifts must retain their individual editors.

## Task dates, ordering and recurring completion boxes

- Create one-off tasks with reversed alphabetical/date order and different times on the same day. Verify localized dates and chronological ordering before 90-task paging; daily/weekday routines use their next actual execution, not their old start date. Check calendar-filtered historical day/week/month ordering, moved one-off slots, several daily times and first-slot deletion. Paused definitions remain after active ones.
- Open a recurring task from Aufgaben and Heute. Verify green confirmed, red due-unconfirmed and grey upcoming boxes for each individual time, including early confirmations, two-parent confirmations/retractions, late offline entries, week-day gaps, end dates, pause/resume, moved occurrences and delete/restore. Unscheduled dates must never become missing-confirmation boxes. Counts, accessible full date/time/status labels and color legend must agree.
- Browse adjacent calendar months, verify the current-month bound/reset, empty months, historical selection anchors, cache indication, selected-day details, midnight/DST boundaries, scrolling on narrow screens and large fonts. One-off-only tasks have no recurring overview. Confirm that no extra data writes are issued by viewing statistics.

Local checks for these changes: all **183 JVM tests pass**, including chronology/actual-time mapping and completion-status/deletion/pause cases; all app sources compile with the cached Kotlin/Compose toolchain. Full Gradle/lint/APK and physical rendering checks retain the environment limitations documented above.

## Paper-style monthly task tracker

- Compare the recurring-task tracker with the supplied reference layout: sunshine header, soft sage title/month controls, consecutively numbered date cards with circular markers, compact legend and an Euer Ergebnis panel. Verify fixed Monday–Sunday columns, weekday headings, blank leading/trailing cells, readable day targets, selecting full day/time details and month navigation without a date picker. Check large text and narrow/landscape layouts on a device.
- Verify 28/29/30/31-day months and year transitions. Unscheduled days remain neutral with a dash; upcoming executions are grey, due-unconfirmed executions red, completed executions green with a check when the entire day is confirmed. Multiple times retain separate ring segments, including mixed states. Existing pause/deletion/move/correction rules remain effective.
- Verify result percentages count individual executions and exclude upcoming times; an early confirmed execution counts, empty/future-only months show no percentage, and a completed daily month shows its actual month-length denominator. Three new domain tests cover leap-day/empty-day construction, mixed current-day denominators and absent percentages. All **183 JVM tests pass** and app sources compile with the local cached toolchain; actual phone rendering remains unverified here.

- Verify every tracker row is exactly Monday–Sunday: November 2026 starts with six empty cells and ends with Monday 30 followed by six empty cells. February 2021 has exactly four complete rows. Leap February 2024 has 29 dates, correctly positioned without adjacent-month date cards. Grid padding must not affect the result count or percentage. Check all seven columns remain aligned on narrow screens.

## One-off task postponement

Regression tests cover opening the edit dialog with an elapsed one-off task already selected, including a previously moved task; edit eligibility and confirmation corrections; repeated moves with stable IDs and no entries left on old dates; protected completion snapshots; elapsed recurring instances; and moving a single future recurring execution into the past without changing other instances or confirmation snapshots. The Firestore emulator suite has 43 tests, including overdue ONCE/SINGLE edits and past replacement dates for individual tasks, and rejection of overdue FOLLOWING/ALL edits, invalid sources, multiple replacement slots and unauthorized writers.

Phone acceptance: publish the updated rules, install the new APK on both phones, and create an unfinished one-off task whose planned time has passed. Open Aufgaben → task → Bearbeiten and press Weiter without touching the source date. Choose a past date and save; verify it disappears from the former day, appears on the selected past day on both phones, and keeps its history. Move it again to tomorrow, then back into the past. Complete it and confirm it cannot be moved; retract completion and check it can be moved again. Verify an elapsed recurring instance stays protected, future recurring edits still work, disabled Weiter looks disabled and explains why, and opening the editor during synchronization does not permit an offline save.

## Dialog button alignment

Compare task definitions (active, paused and historical), task executions (open, completed and confirming correction), appointments (future and past), shopping, gift ideas and label management. Check every secondary row has aligned edges and matching button heights, and deletion always occupies the final full-width row. Actions and their enabled/disabled states must retain their previous behavior. Check 320dp, 390dp, landscape and 150–200% font settings; long labels must wrap without clipping and every footer action must stay reachable. `DialogActionsPreviews.kt` provides standard and narrow/large-text previews of the shared layouts; physical-device visual acceptance remains pending.

Backdating acceptance: on an unfinished one-off task and on a future individual recurring execution, choose Nur diese Ausführung, enter a past date/time and save. Verify the execution moves once, retains its ID and original identity, and appears on that historical day with no fabricated completion event. Other recurring executions and confirmed snapshots must remain unchanged. New task creation and appointment date restrictions remain unchanged. A rules deployment is required.

## IDE launch with themed icons

Use the shared **Kinderalltag (IDE)** run configuration. Select a non-Garden theme, stop the app, then run and debug from Android Studio repeatedly. Verify MainActivity opens while the selected theme and its launcher icon persist, and the launcher still has exactly one entry. Repeat after reinstalling with app data retained and after switching back to Garden. Check the same flow with the existing local configuration set to Specified Activity. This requires a connected phone or emulator; local validation checks the shared configuration XML and its manifest target.

## Recurring occurrence selection

The edit/delete scope dialog now offers one selectable date-and-time card per task execution. No separate mandatory source date and time chips remain. The next/opened execution is preselected; one-off tasks show a date/time summary. Test Weiter without changing the preselection, changing to another card, Nur diese / following scopes, four-entry pagination (especially eight daily slots), three-week/month intervals, paused tasks, moved executions and end dates. Verify Weitere disables at the last page, Zurück restores earlier pages, and optional date search displays executions at or after the chosen date. Completed/elapsed executions stay protected for editing and remain available for deletion.

TaskEditingTest covers same-day pagination without skipped slots, nearby choices ahead of distant moved dates, actual dates of moved executions, finite schedule boundaries and sparse/paused schedules. TaskOccurrencePicker.kt includes standard and narrow/large-text previews; phone visual acceptance remains pending.

Home preview regression: enable future tasks and verify a daily task due today shows only today's execution(s), including after completion; tomorrow's execution must not appear alongside it. Repeat with weekly/monthly recurrence and multiple daily times. A future one-off and a series starting tomorrow should still appear and fill the configured preview count. Browse another home date and verify suppression follows that displayed date and the active child/parent filters. UpcomingTasksTest covers exclusion before limiting the preview and completed/uncompleted displayed-day tasks.

## Notes and location indicators

Task cards on Home and Aufgaben show a note icon with Notizen when the displayed task has nonblank notes. Appointment cards on Home and Termine show the same note indicator and a location pin beside the actual place when available. Verify blank/whitespace-only values show no badge, edited/moved instances use their displayed notes/place, both appointment indicators can coexist, long locations wrap at narrow widths/large fonts, and tapping anywhere on the card still opens details. Check completed tasks and all themes; the theme Home previews include notes and location sample data. No database change is required.

## Shopping and gift ordering

ListOrderingTest covers alphabetical compatibility, saved order and new items, independent list kinds, moves across filtered/hidden items without duplicates/loss, buying/reopening/deletion/restoration, preserved audit snapshots, and bounded/unique/valid IDs. The 43-test Firestore suite verifies both parents can share independent order documents, strict actor/schema/version/ID validation (including delimiter injection, non-string IDs, duplicates and 1000/1001 boundaries), denied outsider/unauthenticated access, and competing writes allowing only one next version. App tests, formatting, lint and debug assembly pass; physical gesture acceptance remains pending.

Phone acceptance after publishing the rules: in both Einkaufsliste and Geschenkideen, drag the first item down and the last up, cancel a drag, drag past screen edges in a long list, and use tap-menu and TalkBack Nach oben/Nach unten. Verify card opening and purchase checkboxes still work. Repeat with long titles, labels, 150–200% text, child/label filters, collapsed/expanded Gekauft and all themes. Confirm filters retain hidden positions, shopping Home follows the shared order, new items append, and bought/reopened/deleted/restored items keep expected state and positions.

Check persistence after relaunch and on the other phone; attempt simultaneous reorders and observe a clear conflict without lost items. Modify/buy/delete an entry or change filters while dragging, and verify stale dragging cancels. Disconnect before/during a save: ordering must be disabled or roll back with an error, and offline purchase/creation must still work. Reordering must create no purchase/activity notification, Verlauf entry or statistics count.
