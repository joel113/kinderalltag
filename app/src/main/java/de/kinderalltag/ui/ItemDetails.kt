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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.kinderalltag.data.*
import de.kinderalltag.domain.*
import de.kinderalltag.text.*
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
            color = AppTheme.colors.background,
            contentColor = AppTheme.colors.ink,
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
                            color = AppTheme.colors.primary,
                        )
                        Text(
                            title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    IconButton(onClick = onDismiss, enabled = !busy) {
                        Icon(Icons.Default.Close, text(TextKey.CLOSE))
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
                    Modifier.fillMaxWidth()
                        .heightIn(
                            max =
                                (androidx.compose.ui.platform.LocalConfiguration.current
                                        .screenHeightDp * 0.46f)
                                    .dp
                        )
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
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
    color: Color = AppTheme.colors.primaryContainer,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = color,
        contentColor = AppTheme.colors.ink,
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
            tint = AppTheme.colors.primary,
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
        color = AppTheme.colors.secondaryContainer,
    ) {
        Text(
            text,
            Modifier.padding(14.dp),
            style = MaterialTheme.typography.bodySmall,
            color = AppTheme.colors.ink,
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
    val color = if (destructive) StatusColors.error else AppTheme.colors.primary
    val foreground = if (enabled) color else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, foreground.copy(alpha = 0.45f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
    ) {
        Icon(icon, null, tint = foreground, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            color = foreground,
            modifier = Modifier.weight(1f, fill = false),
            textAlign = TextAlign.Center,
        )
    }
}

internal data class DetailActionSpec(
    val label: String,
    val icon: ImageVector,
    val enabled: Boolean = true,
    val onClick: () -> Unit,
)

@Composable
internal fun DetailActions(
    secondary: List<DetailActionSpec> = emptyList(),
    deleteAction: DetailActionSpec? = null,
    primary: @Composable ColumnScope.() -> Unit = {},
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        primary()
        if (secondary.isNotEmpty()) {
            val fontScale = LocalDensity.current.fontScale
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val columns = if (maxWidth < 280.dp || fontScale > 1.3f) 1 else 2
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    secondary.chunked(columns).forEach { row ->
                        Row(
                            Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            row.forEach { action ->
                                DetailAction(
                                    action.label,
                                    action.icon,
                                    action.enabled,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    onClick = action.onClick,
                                )
                            }
                        }
                    }
                }
            }
        }
        deleteAction?.let { action ->
            DetailAction(
                action.label,
                action.icon,
                action.enabled,
                destructive = true,
                modifier = Modifier.fillMaxWidth(),
                onClick = action.onClick,
            )
        }
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
        text(TextKey.TASK),
        onDismiss,
        busy,
        actions = {
            DetailActions(
                secondary =
                    buildList {
                        if (!selection.historical) {
                            add(
                                DetailActionSpec(
                                    if (selection.editable.active) text(TextKey.EDIT)
                                    else text(TextKey.RESUME),
                                    if (selection.editable.active) Icons.Default.Edit
                                    else Icons.Default.PlayArrow,
                                    canManage,
                                    onClick = onEdit,
                                )
                            )
                            if (selection.editable.active)
                                add(
                                    DetailActionSpec(
                                        text(TextKey.PAUSE),
                                        Icons.Default.Pause,
                                        canManage,
                                        onClick = onPause,
                                    )
                                )
                        }
                    },
                deleteAction =
                    DetailActionSpec(
                        text(TextKey.DELETE),
                        Icons.Default.DeleteOutline,
                        canManage,
                        onClick = onDelete,
                    ),
            )
        },
    ) {
        DetailSection(text(TextKey.AT_A_GLANCE), AppTheme.colors.accentContainer) {
            DetailValue(
                Icons.Default.Face,
                text(TextKey.AREA),
                state.sections.firstOrNull { it.id == task.childId }?.name ?: text(TextKey.AREA),
            )
            DetailValue(
                Icons.Default.Schedule,
                text(TextKey.TIMES),
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
            DetailValue(Icons.Default.Repeat, text(TextKey.RECURRENCE), recurrence)
            DetailValue(
                Icons.Default.DateRange,
                if (task.frequency == Frequency.ONCE) text(TextKey.DATE) else text(TextKey.PERIOD),
                if (task.frequency == Frequency.ONCE) formats.fullDate(task.start)
                else
                    text(TextKey.FROM, formats.date(task.start)) +
                        (task.end?.let { text(TextKey.UNTIL_1_S, formats.date(it)) }
                            ?: text(TextKey.NO_END_DATE)),
            )
            DetailValue(
                Icons.Default.Person,
                text(TextKey.ASSIGNED_TO),
                responsibilityName(task.responsibleParentId, state.parents),
            )
            if (upcoming != null)
                DetailValue(
                    Icons.Default.Event,
                    text(TextKey.NEXT_OCCURRENCE),
                    "${formats.fullDate(upcoming.date)} · ${formats.time(upcoming.time)}",
                )
        }
        DetailSection(text(TextKey.OCCURRENCES), AppTheme.colors.background) {
            Text(
                text(TextKey.CHOOSE_A_DAY_AND_TIME_TO_CONFIRM_AN_OCCURRENCE_INCLUDING_IN_ADVANCE),
                style = MaterialTheme.typography.bodySmall,
            )
            DatePickerField(
                text(TextKey.DAY),
                LocalDate.parse(executionDate),
                onSelect = { date -> date?.let { executionDate = it.toString() } },
                enabled = !busy,
            )
            if (executions.isEmpty())
                Text(
                    text(TextKey.NO_OCCURRENCE_IS_SCHEDULED_FOR_THIS_DAY),
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
                        "${formats.time(occurrence.time)} · ${if (completed) text(TextKey.COMPLETED_DETAILS) else text(TextKey.CONFIRM_OCCURRENCE)}",
                        modifier = Modifier.weight(1f),
                    )
                    Icon(Icons.Default.ChevronRight, null)
                }
            }
        }
        TaskCompletionOverview(task.taskId, state, now, selection.date)
        if (task.notes.isNotBlank())
            DetailSection(text(TextKey.NOTES), AppTheme.colors.background) { Text(task.notes) }
        if (!selection.editable.active)
            DetailNotice(
                text(TextKey.THIS_TASK_IS_PAUSED_RESUME_LETS_YOU_CHOOSE_THE_NEXT_OCCURRENCE)
            )
        else if (selection.historical)
            DetailNotice(
                text(
                    TextKey.PAST_OCCURRENCES_REMAIN_UNCHANGED_YOU_CAN_DELETE_THEM_AND_RESTORE_THEM_F
                )
            )
        else
            Text(
                text(
                    TextKey.CHANGES_APPLY_TO_UPCOMING_UNCONFIRMED_OCCURRENCES_STARTED_OCCURRENCES_AN
                ),
                style = MaterialTheme.typography.bodySmall,
            )
        if (!current)
            DetailNotice(text(TextKey.THIS_TASK_HAS_CHANGED_PLEASE_CLOSE_AND_REOPEN_THE_OVERVIEW))
        if (state.fromCache)
            DetailNotice(text(TextKey.SAVED_DATA_EDITING_OR_DELETING_REQUIRES_A_CONNECTION))
    }
}
