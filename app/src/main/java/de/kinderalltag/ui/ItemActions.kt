package de.kinderalltag.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.kinderalltag.data.*
import de.kinderalltag.domain.*
import java.time.*
import java.util.UUID

internal data class ItemActionTarget(
    val deleting: Boolean,
    val definition: TaskRevision? = null,
    val task: Occurrence? = null,
    val appointment: AppointmentOccurrence? = null,
    val expectedVersion: Long = 0,
)

internal data class TaskEditSelection(
    val occurrence: Occurrence,
    val scope: OperationScope,
    val version: Long,
)

internal fun taskOperation(
    occurrence: Occurrence,
    scope: OperationScope,
    action: OperationAction,
    draft: TaskRevision? = null,
) =
    ItemOperation(
        UUID.randomUUID().toString(),
        ItemType.TASK,
        occurrence.revision.taskId,
        action,
        scope,
        occurrence.id,
        occurrence.originalDate,
        occurrence.originalTime,
        occurrence.sourceRevisionId,
        draft?.title ?: occurrence.revision.title,
        actualDate = occurrence.date,
        actualStartMillis = occurrence.instant.toEpochMilli(),
        replacementStartMillis =
            if (scope == OperationScope.SINGLE && draft != null)
                draft.start
                    .atTime(draft.times.single())
                    .atZone(FAMILY_ZONE)
                    .toInstant()
                    .toEpochMilli()
            else null,
        childId = draft?.childId ?: occurrence.revision.childId,
        task = draft,
    )

internal fun appointmentOperation(
    state: FamilyState,
    occurrence: AppointmentOccurrence,
    scope: OperationScope,
    action: OperationAction,
    draft: AppointmentSeries? = null,
) =
    ItemOperation(
        UUID.randomUUID().toString(),
        ItemType.APPOINTMENT,
        state.rootId(occurrence),
        action,
        scope,
        occurrence.id,
        occurrence.originalDate,
        occurrence.originTime,
        occurrence.sourceSeriesId,
        draft?.details?.title ?: occurrence.details.title,
        actualDate = occurrence.details.date,
        actualStartMillis = occurrence.details.startInstant.toEpochMilli(),
        replacementStartMillis =
            draft?.details?.copy(date = draft.activeFrom)?.startInstant?.toEpochMilli(),
        recipient = draft?.details?.recipient ?: occurrence.details.recipient,
        appointment = draft,
    )

internal fun taskActionOccurrences(
    state: FamilyState,
    task: TaskRevision,
    date: LocalDate,
): List<Occurrence> {
    val live = state.taskOccurrences(date).filter { it.revision.taskId == task.taskId }
    if (task.active) return live
    if (task.frequency == Frequency.WEEKDAYS && date.dayOfWeek !in task.weekdays) return live
    if (
        task.frequency in setOf(Frequency.DAILY, Frequency.WEEKS, Frequency.MONTHS) &&
            !task.copy(active = true).isDue(date)
    )
        return live
    val source = state.revisions.firstOrNull { it.taskId == task.taskId } ?: return emptyList()
    val resumed =
        task.times.mapIndexed { index, time ->
            val origin = task.slotOrigins.getOrNull(index) ?: "${source.id}|${time.toSecondOfDay()}"
            val originalTime = LocalTime.ofSecondOfDay(origin.substringAfterLast('|').toLong())
            Occurrence(
                task.copy(active = true),
                date,
                time,
                "${task.taskId}_${origin.substringBeforeLast('|')}_${date}_${originalTime.toSecondOfDay()}",
                date,
                originalTime,
                source.id,
            )
        }
    return (live + resumed).distinctBy { it.id }.sortedBy { it.time }
}

@Composable
internal fun ItemActionDialog(
    target: ItemActionTarget,
    state: FamilyState,
    vm: FamilyViewModel,
    onDismiss: () -> Unit,
    onEditTask: (TaskEditSelection) -> Unit,
) {
    val formats = rememberDateTimeFormats()
    val now by vm.clock.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val error by vm.operationError.collectAsStateWithLifecycle()
    val task = target.task?.revision ?: target.definition
    val next =
        remember(target) {
            target.task
                ?: task?.let { definition ->
                    val today = now.atZone(FAMILY_ZONE).toLocalDate()
                    val first = maxOf(today, definition.start)
                    val upcoming =
                        if (definition.active) state.futureTaskOccurrences(definition.taskId, now)
                        else {
                            val schedule = definition.copy(active = true)
                            val date = schedule.nextDueDate(first)
                            listOfNotNull(date, date?.let { schedule.nextDueDate(it.plusDays(1)) })
                                .flatMap { taskActionOccurrences(state, definition, it) }
                        }
                    upcoming.firstOrNull {
                        it.instant > now &&
                            (target.deleting ||
                                Schedule.activeCompletions(state.events, it.id).isEmpty())
                    }
                }
        }
    var date by
        remember(target) {
            mutableStateOf(
                next?.date
                    ?: target.appointment?.details?.date
                    ?: task?.start?.takeIf { it >= now.atZone(FAMILY_ZONE).toLocalDate() }
                    ?: now.atZone(FAMILY_ZONE).toLocalDate()
            )
        }
    var selectedId by remember(target) { mutableStateOf(next?.id) }
    val instances = task?.let { taskActionOccurrences(state, it, date) }.orEmpty()
    val selected = instances.firstOrNull { it.id == selectedId } ?: instances.firstOrNull()
    val recurring =
        task?.let { definition ->
            state.revisions.any {
                it.taskId == definition.taskId && it.frequency != Frequency.ONCE
            } ||
                state.itemOperations.any {
                    it.itemId == definition.taskId &&
                        it.scope == OperationScope.FOLLOWING &&
                        it.task?.frequency?.let { value -> value != Frequency.ONCE } == true
                }
        } ?: (target.appointment?.series?.repeat != AppointmentRepeat.ONCE)
    var scope by
        remember(target) {
            mutableStateOf(
                if (recurring && target.definition != null && target.deleting) OperationScope.ALL
                else if (recurring && target.definition?.active == false) OperationScope.FOLLOWING
                else OperationScope.SINGLE
            )
        }
    var confirming by remember(target) { mutableStateOf(false) }
    LaunchedEffect(target) { vm.operationError.value = null }
    val editable =
        selected?.let {
            it.instant > now && Schedule.activeCompletions(state.events, it.id).isEmpty()
        } ?: false
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(if (target.deleting) "Löschen" else "Aufgabe bearbeiten") },
        text = {
            Column(
                Modifier.heightIn(max = 500.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    task?.title ?: target.appointment?.details?.title.orEmpty(),
                    fontWeight = FontWeight.Bold,
                )
                if (recurring)
                    OperationScope.entries
                        .filter { target.deleting || it != OperationScope.ALL }
                        .forEach { value ->
                            FilterChip(
                                scope == value,
                                {
                                    scope = value
                                    confirming = false
                                },
                                label = { Text(scopeLabel(value)) },
                            )
                        }
                if (task != null && scope != OperationScope.ALL) {
                    DatePickerField(
                        "Ausführung am",
                        date,
                        { chosen ->
                            chosen?.let {
                                date = it
                                selectedId = null
                                confirming = false
                            }
                        },
                        minDate =
                            if (target.deleting) null else now.atZone(FAMILY_ZONE).toLocalDate(),
                    )
                    instances.forEach { occurrence ->
                        FilterChip(
                            selected?.id == occurrence.id,
                            {
                                selectedId = occurrence.id
                                confirming = false
                            },
                            label = {
                                Text(
                                    formats.time(occurrence.time) +
                                        " · " +
                                        occurrence.revision.title +
                                        if (
                                            Schedule.activeCompletions(state.events, occurrence.id)
                                                .isNotEmpty()
                                        )
                                            " · bestätigt"
                                        else ""
                                )
                            },
                        )
                    }
                    if (instances.isEmpty())
                        Text("An diesem Tag gibt es keine sichtbare Ausführung.")
                }
                target.appointment?.let { appointment ->
                    Text(
                        "${formats.fullDate(appointment.details.date)} · ${if (appointment.details.allDay) "Ganztägig" else formats.time(appointment.details.startTime)}"
                    )
                    if (scope == OperationScope.FOLLOWING)
                        Text(
                            "Ab ursprünglichem Termin: ${formats.date(appointment.originalDate)} · ${formats.time(appointment.originTime)}"
                        )
                }
                if (!target.deleting && selected != null && !editable)
                    Text(
                        "Begonnene oder bestätigte Ausführungen bleiben unverändert. Bitte eine kommende Ausführung wählen."
                    )
                if (target.deleting)
                    Text(
                        if (confirming)
                            "${scopeLabel(scope)} löschen? Bestätigungen und Verlauf bleiben erhalten. Ihr könnt die Löschung im Papierkorb rückgängig machen."
                        else
                            "Auch vergangene oder bestätigte Einträge können aus der Übersicht entfernt werden. Der Verlauf bleibt erhalten."
                    )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(
                colors =
                    ButtonDefaults.textButtonColors(
                        contentColor =
                            if (target.deleting) StatusColors.error else AppTheme.colors.primary
                    ),
                enabled =
                    !busy &&
                        !state.fromCache &&
                        state.error == null &&
                        (if (!target.deleting) editable
                        else
                            scope == OperationScope.ALL ||
                                selected != null ||
                                target.appointment != null),
                onClick = {
                    if (!target.deleting) {
                        selected?.let {
                            onEditTask(TaskEditSelection(it, scope, target.expectedVersion))
                        }
                        return@TextButton
                    }
                    if (!confirming) {
                        confirming = true
                        return@TextButton
                    }
                    val operation =
                        if (target.appointment != null)
                            appointmentOperation(
                                state,
                                target.appointment,
                                scope,
                                OperationAction.DELETE,
                            )
                        else {
                            val anchor =
                                selected
                                    ?: task?.let { definition ->
                                        val source =
                                            state.revisions.first { it.taskId == definition.taskId }
                                        Occurrence(
                                            definition,
                                            date,
                                            definition.times.first(),
                                            sourceRevisionId = source.id,
                                        )
                                    }
                            anchor?.let {
                                taskOperation(
                                    it,
                                    if (recurring) scope else OperationScope.SINGLE,
                                    OperationAction.DELETE,
                                )
                            }
                        }
                    operation?.let { vm.applyOperation(it, target.expectedVersion, onDismiss) }
                },
            ) {
                Text(
                    if (busy) "Speichert …"
                    else if (!target.deleting) "Weiter"
                    else if (confirming) "Ja, löschen" else "Löschen …",
                    color =
                        if (busy || state.fromCache || state.error != null)
                            androidx.compose.ui.graphics.Color.Unspecified
                        else if (target.deleting) StatusColors.error else AppTheme.colors.primary,
                )
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Abbrechen") } },
    )
}

internal fun scopeLabel(scope: OperationScope): String =
    when (scope) {
        OperationScope.SINGLE -> "Nur diese Ausführung"
        OperationScope.FOLLOWING -> "Diese und alle folgenden"
        OperationScope.ALL -> "Gesamte Reihe"
    }

@Composable
internal fun TrashScreen(state: FamilyState, vm: FamilyViewModel) {
    val formats = rememberDateTimeFormats()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val error by vm.operationError.collectAsStateWithLifecycle()
    var restoring by remember { mutableStateOf<ItemOperation?>(null) }
    val listError by vm.listError.collectAsStateWithLifecycle()
    var restoringList by remember { mutableStateOf<FamilyListItem?>(null) }
    LaunchedEffect(Unit) { vm.operationError.value = null }
    val deleted = activeDeletions(state.itemOperations).sortedByDescending { it.createdMillis }
    androidx.compose.foundation.lazy.LazyColumn(
        contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 80.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                "Papierkorb",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }
        item {
            Text(
                "Gelöschte Ausführungen und Reihen. Bestätigungen, Verlauf und Statistiken bleiben erhalten."
            )
        }
        if (deleted.isEmpty() && state.listItems.none { it.deleted })
            item { Text("Hier ist noch nichts im Papierkorb.") }
        error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        items(deleted, key = { it.id }) { item ->
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AppTheme.colors.background),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(item.title, fontWeight = FontWeight.Bold)
                    Text(
                        (if (item.type == ItemType.TASK) "Aufgabe" else "Termin") +
                            " · " +
                            scopeLabel(item.scope)
                    )
                    if (item.scope != OperationScope.ALL)
                        Text("${formats.date(item.anchorDate)} · ${formats.time(item.anchorTime)}")
                    Text(
                        "${item.authorName} · ${formats.timestamp(Instant.ofEpochMilli(item.createdMillis))}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    OutlinedButton(
                        onClick = { restoring = item },
                        enabled = !busy && !state.fromCache && state.error == null,
                    ) {
                        Icon(Icons.Default.Restore, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Wiederherstellen")
                    }
                }
            }
        }
        items(state.listItems.filter { it.deleted }, key = { "list:${it.id}" }) { item ->
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AppTheme.colors.background),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(item.title, fontWeight = FontWeight.Bold)
                    Text(listTitle(item.kind))
                    OutlinedButton(
                        enabled =
                            !busy && !state.fromCache && !state.pending && state.error == null,
                        onClick = {
                            vm.listError.value = null
                            restoringList = item
                        },
                    ) {
                        Text("Wiederherstellen")
                    }
                }
            }
        }
        item { EndOfPageFooter(IllustrationRole.BRAND) }
    }
    restoringList?.let { item ->
        AlertDialog(
            onDismissRequest = { if (!busy) restoringList = null },
            title = { Text("Wiederherstellen?") },
            text = {
                Column {
                    Text("„${item.title}“ wird mit seinem aktuellen Kaufstatus wieder sichtbar.")
                    listError?.let { Text(it, color = StatusColors.error) }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !busy && !state.fromCache && !state.pending,
                    onClick = {
                        vm.changeListItem(item, ListAction.RESTORED) { restoringList = null }
                    },
                ) {
                    Text("Wiederherstellen")
                }
            },
            dismissButton = {
                TextButton(enabled = !busy, onClick = { restoringList = null }) {
                    Text("Abbrechen")
                }
            },
        )
    }
    restoring?.let { deletion ->
        AlertDialog(
            onDismissRequest = { if (!busy) restoring = null },
            title = { Text("Wiederherstellen?") },
            text = {
                Text(
                    "„${deletion.title}“ wird nach dem aktuellen Zeitplan wieder sichtbar. Neuere Änderungen und andere Löschungen bleiben bestehen."
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !busy,
                    onClick = {
                        vm.applyOperation(
                            deletion.copy(
                                id = UUID.randomUUID().toString(),
                                action = OperationAction.RESTORE,
                                restoresId = deletion.id,
                                task = null,
                                appointment = null,
                            ),
                            state.version(deletion.type, deletion.itemId),
                        ) {
                            restoring = null
                            if (
                                deleted.any {
                                    it.id != deletion.id && deletionsOverlap(it, deletion)
                                }
                            )
                                vm.message.value =
                                    "Wiederhergestellt. Eine weitere Löschung blendet Teile weiterhin aus."
                        }
                    },
                ) {
                    Text("Wiederherstellen")
                }
            },
            dismissButton = {
                TextButton(onClick = { restoring = null }, enabled = !busy) { Text("Abbrechen") }
            },
        )
    }
}
