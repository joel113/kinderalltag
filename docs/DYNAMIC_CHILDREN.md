# Dynamic child profiles

## Using profiles

In **Settings → Your children** / **Einstellungen → Eure Kinder**, add or edit a name,
choose one of ten built-in symbols and ten colours, or archive/restore a profile.
Profiles are shared between both parents. Names may be duplicated; identity comes
from the stable document ID. Household is a separate built-in area.

There can be at most **10 active children**. Archived profiles do not use a slot.
Archiving preserves all definitions, confirmations and history. Default upcoming
views, calendars and reminders omit records belonging exclusively to archived
children. Selecting an archived child explicitly still shows their records; history
and statistics retain them. Shared appointments stay visible for active participants.
Restoring a profile resumes its existing schedules; it does not change dates or
create replacement tasks.

New tasks can select multiple children. Saving creates independent task definitions,
atomically, one per child. Completion, postponement and later edits affect only that
child's copy. Appointments and gift ideas instead retain an explicit set of child IDs.
“All children” captures today's selection, so adding another profile does not change
older assignments. Shopping has no child assignment. Household is available for tasks
and appointments only; parent responsibility works as before.

Child statistics count a shared appointment/gift action for each participant. Overall
and parent/weekday totals still count the event once; child rows are not additive.

## Existing Johann/Ferdinand data

The first authorized online startup creates `households/family/childRegistry/state`
in a Firestore transaction. It registers whichever original `johann` and `ferdinand`
profiles exist, without changing their IDs, names, colours or documents. Concurrent
startups retry safely; subsequent starts leave the registry unchanged. An empty new
household starts without children; create them in Settings.

Missing profile fields decode as bear, active, version zero. Existing colours keep
their stored values; absent colours retain blue for Ferdinand and rose for Johann.
Editing a profile adds `icon`, `archived` and a monotonically increasing `version`.
The registry retains known IDs, active IDs and a version; every profile write must be
paired with its registry update. Rules enforce the active limit even across phones.
Conflicting profile edits require reopening the editor instead of overwriting changes.

Tasks remain single-child records. No task, revision, occurrence, completion,
operation, appointment or list-history document is rewritten by initialization.
Legacy appointment/gift recipients remain readable: `FERDINAND`, `JOHANN`,
`BOTH_CHILDREN`, `HOUSEHOLD`. In particular, legacy `BOTH_CHILDREN` always means
`ferdinand` and `johann`, even after more children are added. Those same selections
keep their original wire representation so immutable audit snapshot equality survives.
Other selections use `{kind: "CHILDREN", childIds: [...]}` with sorted, unique IDs.
Archived IDs remain known so historical snapshots and corrections remain valid.

## Rollout and recovery

1. Save a recoverable backup of the household with your existing administrator backup
   process. Record child IDs/appearance, task and event counts and several completed
   occurrence IDs; include all subcollections, not just the household document.
2. Run the Android and demo-emulator checks from [TESTING.md](TESTING.md). No test
   command needs production credentials.
3. Publish the updated `firebase/firestore.rules` before opening the new APK.
4. Update **both phones before using dynamic profiles**. Sign in online and let
   initialization/synchronization finish. Settings offers retry if preparation fails.
   Do not manually recreate existing children or change their document IDs.
5. Compare the recorded IDs/counts and completed occurrences, then create a temporary
   profile and verify its name/icon/colour and archive state synchronize between phones.

Old APKs cannot understand arbitrary child selections; after migration the rules also
reject profile writes that would remove the new fields. Use the updated version on
both devices. To recover from a client defect, keep the compatible rules and install
a corrected client. Returning to a pre-dynamic APK requires restoring a compatible
backup; deleting children, the registry or history is not a rollback procedure.

Initialization is additive and needs neither Admin credentials in the APK nor manual
collection creation. Membership provisioning remains unchanged. This feature does
not deploy production rules or modify live production data from development tools.
