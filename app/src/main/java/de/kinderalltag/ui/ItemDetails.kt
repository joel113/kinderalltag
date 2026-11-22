package de.kinderalltag.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.kinderalltag.data.*
import de.kinderalltag.domain.*
import java.time.Instant
import java.time.LocalDate
import java.time.format.TextStyle

// A shared, scrollable details layout keeps management actions out of the overview cards.
@Composable
internal fun ItemDetailsDialog(
    title: String,
    kind: String,
    onDismiss: () -> Unit,
    busy: Boolean = false,
    actions: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = { if (!busy) onDismiss() },
        properties =
            DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = !busy,
                dismissOnClickOutside = !busy,
            ),
    ) {
        Surface(
            Modifier.padding(horizontal = 20.dp, vertical = 24.dp)
                .widthIn(max = 560.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = GardenColors.Cream,
            contentColor = GardenColors.Ink,
        ) {
            Column(
                Modifier.heightIn(
                    max =
                        (androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp *
                                0.86f)
                            .dp
                )
            ) {
                Row(
                    Modifier.fillMaxWidth()
                        .padding(start = 20.dp, top = 12.dp, end = 8.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Column(
                        Modifier.weight(1f).padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            kind,
                            style = MaterialTheme.typography.labelLarge,
                            color = GardenColors.Blue,
                        )
                        Text(
                            title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    IconButton(onClick = onDismiss, enabled = !busy) {
                        Icon(Icons.Default.Close, "Schließen")
                    }
                }
                Column(
                    Modifier.weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    content = content,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Column(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    content = actions,
                )
            }
        }
    }
}

@Composable
internal fun DetailSection(
    title: String,
    color: Color = GardenColors.Sky,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = color,
        contentColor = GardenColors.Ink,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
internal fun DetailValue(icon: ImageVector, label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Icon(
            icon,
            null,
            tint = GardenColors.Blue,
            modifier = Modifier.padding(top = 2.dp).size(20.dp),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
internal fun DetailNotice(text: String) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = GardenColors.Blush,
    ) {
        Text(
            text,
            Modifier.padding(14.dp),
            style = MaterialTheme.typography.bodySmall,
            color = GardenColors.Ink,
        )
    }
}

@Composable
internal fun DetailAction(
    label: String,
    icon: ImageVector,
    enabled: Boolean = true,
    destructive: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val color = if (destructive) GardenColors.Red else GardenColors.Blue
    val foreground = if (enabled) color else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, foreground.copy(alpha = 0.45f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
    ) {
        Icon(icon, null, tint = foreground, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = foreground)
    }
}

internal data class TaskOverviewSelection(
    val task: TaskRevision,
    val editable: TaskRevision,
    val date: LocalDate?,
    val historical: Boolean,
    val version: Long,
)

@Composable
internal fun TaskDefinitionDialog(
    selection: TaskOverviewSelection,
    state: FamilyState,
    busy: Boolean,
    now: Instant,
    onDismiss: () -> Unit,
    onOpen: (Occurrence) -> Unit,
    onEdit: () -> Unit,
    onPause: () -> Unit,
    onDelete: () -> Unit,
) {
    val task = selection.task
    val formats = rememberDateTimeFormats()
    val current =
        state.version(ItemType.TASK, task.taskId) == selection.version &&
            activeDeletions(state.itemOperations).none {
                it.type == ItemType.TASK &&
                    it.itemId == task.taskId &&
                    it.scope == OperationScope.ALL
            }
    val canManage = current && !busy && !state.fromCache && state.error == null
    val upcoming =
        if (!selection.historical && task.active)
            state.futureTaskOccurrences(task.taskId, now).firstOrNull()
        else null
    var executionDate by
        rememberSaveable(task.taskId, selection.date) {
            mutableStateOf(
                (selection.date ?: upcoming?.date ?: now.atZone(FAMILY_ZONE).toLocalDate())
                    .toString()
            )
        }
    val executions =
        state.taskOccurrences(LocalDate.parse(executionDate)).filter {
            it.revision.taskId == task.taskId
        }
    ItemDetailsDialog(
        task.title,
        "Aufgabe",
        onDismiss,
        busy,
        actions = {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!selection.historical) {
                    DetailAction(
                        if (selection.editable.active) "Bearbeiten" else "Fortsetzen",
                        if (selection.editable.active) Icons.Default.Edit
                        else Icons.Default.PlayArrow,
                        canManage,
                        onClick = onEdit,
                    )
                    if (selection.editable.active)
                        DetailAction("Pausieren", Icons.Default.Pause, canManage, onClick = onPause)
                }
                DetailAction(
                    "Löschen",
                    Icons.Default.DeleteOutline,
                    canManage,
                    destructive = true,
                    onClick = onDelete,
                )
            }
        },
    ) {
        DetailSection("Auf einen Blick", GardenColors.Mint) {
            DetailValue(
                Icons.Default.Face,
                "Bereich",
                state.sections.firstOrNull { it.id == task.childId }?.name ?: "Bereich",
            )
            DetailValue(
                Icons.Default.Schedule,
                "Uhrzeiten",
                task.times.joinToString(" · ", transform = formats::time),
            )
            val recurrence =
                when (task.frequency) {
                    Frequency.WEEKDAYS ->
                        task.weekdays
                            .sortedBy { it.value }
                            .joinToString(" · ") {
                                it.getDisplayName(TextStyle.SHORT, formats.locale)
                            }
                    else -> frequencyLabel(task.frequency, task.interval)
                }
            DetailValue(Icons.Default.Repeat, "Wiederholung", recurrence)
            DetailValue(
                Icons.Default.DateRange,
                if (task.frequency == Frequency.ONCE) "Datum" else "Zeitraum",
                if (task.frequency == Frequency.ONCE) formats.fullDate(task.start)
                else
                    "Ab ${formats.date(task.start)}" +
                        (task.end?.let { " bis ${formats.date(it)}" } ?: " · ohne Enddatum"),
            )
            DetailValue(
                Icons.Default.Person,
                "Zuständig",
                responsibilityName(task.responsibleParentId, state.parents),
            )
            if (upcoming != null)
                DetailValue(
                    Icons.Default.Event,
                    "Nächste Ausführung",
                    "${formats.fullDate(upcoming.date)} · ${formats.time(upcoming.time)}",
                )
        }
        DetailSection("Ausführungen", GardenColors.Cream) {
            Text(
                "Wähle Tag und Uhrzeit, um eine Ausführung zu bestätigen – auch im Voraus.",
                style = MaterialTheme.typography.bodySmall,
            )
            DatePickerField(
                "Tag",
                LocalDate.parse(executionDate),
                onSelect = { date -> date?.let { executionDate = it.toString() } },
                enabled = !busy,
            )
            if (executions.isEmpty())
                Text(
                    "Für diesen Tag ist keine Ausführung geplant.",
                    style = MaterialTheme.typography.bodySmall,
                )
            executions.forEach { occurrence ->
                val completed = Schedule.activeCompletions(state.events, occurrence.id).isNotEmpty()
                OutlinedButton(
                    onClick = { onOpen(occurrence) },
                    enabled = current && !busy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(if (completed) Icons.Default.CheckCircle else Icons.Default.Schedule, null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${formats.time(occurrence.time)} · ${if (completed) "Erledigt · Details" else "Ausführung bestätigen"}",
                        modifier = Modifier.weight(1f),
                    )
                    Icon(Icons.Default.ChevronRight, null)
                }
            }
        }
        TaskCompletionOverview(task.taskId, state, now, selection.date)
        if (task.notes.isNotBlank())
            DetailSection("Notizen", GardenColors.Cream) { Text(task.notes) }
        if (!selection.editable.active)
            DetailNotice(
                "Diese Aufgabe ist pausiert. Mit Fortsetzen wählt ihr die nächste Ausführung."
            )
        else if (selection.historical)
            DetailNotice(
                "Vergangene Ausführungen bleiben unverändert. Ihr könnt sie löschen und im Papierkorb wiederherstellen."
            )
        else
            Text(
                "Änderungen gelten für kommende, unbestätigte Ausführungen. Begonnene Ausführungen und Bestätigungen bleiben erhalten.",
                style = MaterialTheme.typography.bodySmall,
            )
        if (!current)
            DetailNotice(
                "Diese Aufgabe wurde geändert. Bitte schließen und die Übersicht neu öffnen."
            )
        if (state.fromCache)
            DetailNotice(
                "Gespeicherter Stand. Zum Bearbeiten oder Löschen ist eine Verbindung erforderlich."
            )
    }
}
