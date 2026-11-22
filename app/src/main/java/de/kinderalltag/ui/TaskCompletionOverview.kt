package de.kinderalltag.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.kinderalltag.data.FamilyState
import de.kinderalltag.domain.*
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private fun trackerStatusLabel(status: TaskCompletionStatus) =
    when (status) {
        TaskCompletionStatus.COMPLETED -> "Bestätigt"
        TaskCompletionStatus.MISSING -> "Nicht bestätigt"
        TaskCompletionStatus.UPCOMING -> "Bevorstehend"
    }

private fun trackerStatusColor(status: TaskCompletionStatus) =
    when (status) {
        TaskCompletionStatus.COMPLETED -> StatusColors.success
        TaskCompletionStatus.MISSING -> StatusColors.error
        TaskCompletionStatus.UPCOMING -> StatusColors.upcoming
    }

@Composable
internal fun TaskCompletionOverview(
    taskId: String,
    state: FamilyState,
    now: Instant,
    anchor: LocalDate? = null,
) {
    val trackerSage = AppTheme.colors.trackerContainer
    val trackerOutline = AppTheme.colors.trackerOutline
    val recurring =
        state.revisions.any { it.taskId == taskId && it.frequency != Frequency.ONCE } ||
            state.itemOperations.any {
                it.itemId == taskId &&
                    it.task?.frequency?.let { frequency -> frequency != Frequency.ONCE } == true
            }
    if (!recurring) return
    val formats = rememberDateTimeFormats()
    val currentMonth = YearMonth.from(now.atZone(FAMILY_ZONE))
    val initialMonth =
        YearMonth.from(anchor ?: now.atZone(FAMILY_ZONE).toLocalDate()).coerceAtMost(currentMonth)
    var monthValue by
        rememberSaveable(taskId, initialMonth) { mutableStateOf(initialMonth.toString()) }
    val month = YearMonth.parse(monthValue)
    var selectedDay by rememberSaveable(taskId, monthValue) { mutableStateOf<Int?>(null) }
    val tracker =
        remember(taskId, state.revisions, state.events, state.itemOperations, month, now) {
            taskCompletionMonth(
                taskId,
                state.revisions,
                state.events,
                state.itemOperations,
                month,
                now,
            )
        }
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = AppTheme.colors.trackerPaper,
        contentColor = AppTheme.colors.ink,
        border = BorderStroke(1.dp, trackerOutline.copy(alpha = 0.5f)),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AppIllustration(IllustrationRole.JOY, Modifier.size(46.dp))
                Surface(
                    Modifier.weight(1f),
                    color = trackerSage,
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            "Monats-Tracker",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Kleine Schritte, Tag für Tag.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { monthValue = month.minusMonths(1).toString() }) {
                    Icon(Icons.Default.ChevronLeft, "Vorheriger Monat", tint = StatusColors.success)
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Monat",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppTheme.colors.accent,
                    )
                    Text(
                        month.format(DateTimeFormatter.ofPattern("LLLL yyyy", formats.locale)),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
                IconButton(
                    enabled = month < currentMonth,
                    onClick = { monthValue = month.plusMonths(1).toString() },
                ) {
                    Icon(
                        Icons.Default.ChevronRight,
                        "Nächster Monat",
                        tint = if (month < currentMonth) AppTheme.colors.accent else trackerOutline,
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    java.time.DayOfWeek.entries.forEach { day ->
                        Text(
                            day.getDisplayName(java.time.format.TextStyle.SHORT, formats.locale),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            maxLines = 1,
                            color = AppTheme.colors.accent,
                        )
                    }
                }
                tracker.weeks.forEach { week ->
                    Row(
                        Modifier.height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        week.forEach { day ->
                            if (day == null) Spacer(Modifier.weight(1f).fillMaxHeight())
                            else {
                                val description =
                                    formats.fullDate(day.date) +
                                        if (day.cells.isEmpty()) ": Keine Ausführung"
                                        else
                                            ": " +
                                                day.cells.joinToString("; ") {
                                                    "${formats.time(it.occurrence.time)} ${trackerStatusLabel(it.status)}"
                                                }
                                TrackerDay(
                                    day,
                                    day.date.dayOfMonth == selectedDay,
                                    day.date == now.atZone(FAMILY_ZONE).toLocalDate(),
                                    description,
                                    Modifier.weight(1f).fillMaxHeight(),
                                ) {
                                    selectedDay = day.date.dayOfMonth
                                }
                            }
                        }
                    }
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                TaskCompletionStatus.entries.forEach { status ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Surface(
                            Modifier.size(8.dp),
                            shape = CircleShape,
                            color = trackerStatusColor(status),
                        ) {}
                        Text(
                            trackerStatusLabel(status),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
            selectedDay?.let { number ->
                val day = tracker.days[number - 1]
                Surface(shape = RoundedCornerShape(14.dp), color = AppTheme.colors.background) {
                    Column(
                        Modifier.fillMaxWidth().padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            formats.fullDate(day.date),
                            style = MaterialTheme.typography.labelLarge,
                        )
                        if (day.cells.isEmpty())
                            Text(
                                "Keine Ausführung geplant.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        day.cells.forEach { cell ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Surface(
                                    Modifier.size(8.dp),
                                    shape = CircleShape,
                                    color = trackerStatusColor(cell.status),
                                ) {}
                                Text(
                                    "${formats.time(cell.occurrence.time)} · ${trackerStatusLabel(cell.status)}",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }
            Surface(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = trackerSage,
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            "Euer Ergebnis",
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        AppIllustration(IllustrationRole.AFFECTION, Modifier.size(26.dp))
                    }
                    Surface(
                        Modifier.fillMaxWidth(),
                        color = AppTheme.colors.trackerPaper,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, trackerOutline),
                    ) {
                        Row(
                            Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                "${tracker.confirmed} / ${tracker.eligible}",
                                Modifier.weight(1f),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                tracker.percentage?.let { "$it %" } ?: "–",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = StatusColors.success,
                            )
                        }
                    }
                    if (tracker.eligible > 0)
                        LinearProgressIndicator(
                            progress = { tracker.confirmed.toFloat() / tracker.eligible },
                            modifier = Modifier.fillMaxWidth(),
                            color = StatusColors.success,
                            trackColor = Color.White.copy(alpha = 0.7f),
                        )
                    Text(
                        if (tracker.eligible == 0)
                            "Noch keine fälligen oder bestätigten Ausführungen."
                        else
                            "Bestätigte Ausführungen. ${tracker.upcoming} bevorstehende zählen noch nicht mit.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Text(
                "Ein Ringabschnitt pro Uhrzeit. Tippe auf einen Tag für die Details. Leere Tage waren nicht geplant; Rot bedeutet fehlende Bestätigung.",
                style = MaterialTheme.typography.bodySmall,
            )
            if (state.fromCache)
                Text(
                    "Gespeicherter Stand – Bestätigungen können noch fehlen.",
                    style = MaterialTheme.typography.bodySmall,
                )
            if (month != currentMonth)
                TextButton(
                    onClick = { monthValue = currentMonth.toString() },
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text("Zum aktuellen Monat")
                }
        }
    }
}

@Composable
private fun TrackerDay(
    day: TaskCompletionDay,
    selected: Boolean,
    today: Boolean,
    description: String,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val trackerOutline = AppTheme.colors.trackerOutline
    val confirmed =
        day.cells.isNotEmpty() && day.cells.all { it.status == TaskCompletionStatus.COMPLETED }
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 68.dp).semantics { contentDescription = description },
        shape = RoundedCornerShape(12.dp),
        border =
            BorderStroke(
                if (selected || today) 2.dp else 1.dp,
                if (selected) AppTheme.colors.primary
                else if (today) AppTheme.colors.accent else trackerOutline.copy(alpha = 0.45f),
            ),
        color =
            when {
                confirmed -> StatusColors.successSurface.copy(alpha = 0.6f)
                day.cells.any { it.status == TaskCompletionStatus.MISSING } ->
                    StatusColors.errorSurface.copy(alpha = 0.5f)
                else -> AppTheme.colors.trackerPaper
            },
    ) {
        Column(
            Modifier.padding(horizontal = 3.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                "${day.date.dayOfMonth}.",
                Modifier.align(Alignment.Start).clearAndSetSemantics {},
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (day.cells.isEmpty()) trackerOutline else AppTheme.colors.ink,
            )
            Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize().clearAndSetSemantics {}) {
                    val stroke = 2.dp.toPx()
                    val inset = stroke / 2
                    val origin = androidx.compose.ui.geometry.Offset(inset, inset)
                    val ringSize =
                        androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
                    drawOval(
                        trackerOutline.copy(alpha = 0.35f),
                        origin,
                        ringSize,
                        style = Stroke(stroke),
                    )
                    if (day.cells.isNotEmpty()) {
                        val angle = 360f / day.cells.size
                        val gap = if (day.cells.size == 1) 0f else 12f
                        day.cells.forEachIndexed { index, cell ->
                            drawArc(
                                trackerStatusColor(cell.status),
                                -90f + index * angle + gap / 2,
                                angle - gap,
                                false,
                                origin,
                                ringSize,
                                style = Stroke(stroke, cap = StrokeCap.Round),
                            )
                        }
                    }
                }
                if (confirmed)
                    Icon(
                        Icons.Default.Check,
                        null,
                        Modifier.size(18.dp),
                        tint = StatusColors.success,
                    )
                else if (day.cells.isEmpty())
                    Text(
                        "–",
                        Modifier.clearAndSetSemantics {},
                        color = trackerOutline,
                        style = MaterialTheme.typography.labelSmall,
                    )
            }
        }
    }
}
