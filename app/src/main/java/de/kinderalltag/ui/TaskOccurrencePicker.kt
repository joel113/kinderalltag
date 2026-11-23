package de.kinderalltag.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.kinderalltag.data.*
import de.kinderalltag.domain.*
import de.kinderalltag.text.*
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

internal const val TASK_OCCURRENCE_PAGE_SIZE = 4

// The cursor includes time, so paging never skips remaining slots on the same day.
internal fun taskActionChoices(
    state: FamilyState,
    task: TaskRevision,
    from: Instant,
    limit: Int = TASK_OCCURRENCE_PAGE_SIZE + 1,
): List<Occurrence> {
    if (task.active) {
        val choices = mutableListOf<Occurrence>()
        var cursor = from
        while (choices.size < limit) {
            val next =
                state.futureTaskOccurrences(task.taskId, cursor.minusNanos(1)).firstOrNull()
                    ?: break
            choices += taskActionOccurrences(state, task, next.date).filter { it.instant >= cursor }
            cursor = next.date.plusDays(1).atStartOfDay(FAMILY_ZONE).toInstant()
        }
        return choices.distinctBy { it.id }.sortedBy { it.instant }.take(limit)
    }
    val schedule = task.copy(active = true)
    val choices = mutableListOf<Occurrence>()
    var day = schedule.nextDueDate(from.atZone(FAMILY_ZONE).toLocalDate())
    while (day != null && choices.size < limit) {
        choices += taskActionOccurrences(state, task, day).filter { it.instant >= from }
        if (schedule.frequency == Frequency.ONCE) break
        day = schedule.nextDueDate(day.plusDays(1))
    }
    return choices.distinctBy { it.id }.sortedBy { it.instant }.take(limit)
}

@Composable
internal fun TaskOccurrenceChoices(
    occurrences: List<Occurrence>,
    selectedId: String?,
    state: FamilyState,
    now: Instant,
    deleting: Boolean,
    enabled: Boolean,
    onSelect: (Occurrence) -> Unit,
) {
    val formats = rememberDateTimeFormats()
    Column(
        Modifier.fillMaxWidth().selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        occurrences.forEach { occurrence ->
            val selected = occurrence.id == selectedId
            val blocked =
                if (deleting) null
                else taskEditBlockReason(occurrence, state.revisions, state.events, now)
            val selectable = enabled && blocked == null
            Surface(
                modifier =
                    Modifier.fillMaxWidth()
                        .selectable(
                            selected = selected,
                            enabled = selectable,
                            role = Role.RadioButton,
                            onClick = { onSelect(occurrence) },
                        ),
                shape = MaterialTheme.shapes.small,
                color =
                    if (selected) AppTheme.colors.primaryContainer else AppTheme.colors.background,
                border =
                    BorderStroke(
                        if (selected) 2.dp else 1.dp,
                        if (selected) AppTheme.colors.primary
                        else MaterialTheme.colorScheme.outlineVariant,
                    ),
            ) {
                Row(
                    Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    RadioButton(selected, onClick = null, enabled = selectable)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            formats.fullDate(occurrence.date),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            formats.time(occurrence.time),
                            style = MaterialTheme.typography.titleMedium,
                            color = AppTheme.colors.primary,
                        )
                        if (blocked != null)
                            Text(blocked, style = MaterialTheme.typography.bodySmall)
                        else if (
                            Schedule.activeCompletions(state.events, occurrence.id).isNotEmpty()
                        )
                            Text(
                                text(TextKey.ALREADY_CONFIRMED),
                                style = MaterialTheme.typography.bodySmall,
                            )
                    }
                }
            }
        }
    }
}

@Preview(name = "Ausführung auswählen", widthDp = 390, heightDp = 540)
@Preview(
    name = "Ausführung auswählen · große Schrift",
    widthDp = 320,
    heightDp = 900,
    fontScale = 1.5f,
)
@Composable
private fun TaskOccurrenceChoicesPreview() {
    val day = LocalDate.of(2026, 11, 23)
    val task =
        TaskRevision(
            "source",
            "task",
            "johann",
            "Vitamin D",
            start = day,
            effective = day,
            times = listOf(LocalTime.of(8, 0), LocalTime.of(18, 0)),
        )
    val state = FamilyState(revisions = listOf(task))
    val now = day.atTime(12, 0).atZone(FAMILY_ZONE).toInstant()
    val choices = taskActionChoices(state, task, now, 4)
    KinderTheme {
        Surface(color = AppTheme.colors.background) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Vitamin D", style = MaterialTheme.typography.titleLarge)
                Text(text(TextKey.WHICH_OCCURRENCE), style = MaterialTheme.typography.titleMedium)
                TaskOccurrenceChoices(
                    choices,
                    choices.first().id,
                    state,
                    now,
                    deleting = false,
                    enabled = true,
                    onSelect = {},
                )
            }
        }
    }
}
