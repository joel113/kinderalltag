package de.kinderalltag.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.kinderalltag.data.FamilyState
import de.kinderalltag.domain.*
import java.time.Instant
import java.time.LocalDate

@Composable
internal fun HistoryFeed(
    state: FamilyState,
    today: LocalDate,
    page: Long,
    sectionId: String?,
    onlyMine: Boolean,
    onPageChange: (Long) -> Unit,
) {
    val formats = rememberDateTimeFormats()
    val period = historyPeriod(today, page)
    val entries =
        remember(state, period, sectionId, onlyMine) {
            activityHistory(
                state.revisions,
                state.events,
                state.appointments,
                state.appointmentExceptions,
                state.appointmentActivity,
                state.parents,
                period,
                sectionId,
                onlyMine,
                state.uid,
                state.itemOperations,
                state.listActivity,
            )
        }
    val listState = rememberLazyListState()
    LaunchedEffect(page) { listState.scrollToItem(0) }
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                "Verlauf",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "${formats.date(period.from)} – ${formats.date(period.to)}",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (entries.isEmpty()) item { Text("Keine Ereignisse in diesem Zeitraum.") }
        entries
            .groupBy { Instant.ofEpochMilli(it.millis).atZone(FAMILY_ZONE).toLocalDate() }
            .forEach { (date, dayEntries) ->
                item {
                    Surface(
                        Modifier.fillMaxWidth().padding(top = 12.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = AppTheme.colors.background,
                        border = BorderStroke(1.dp, AppTheme.colors.outline.copy(alpha = 0.45f)),
                    ) {
                        Column(
                            Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                formats.fullDate(date),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                "${dayEntries.size} ${if (dayEntries.size == 1) "Ereignis" else "Ereignisse"} · neueste zuerst",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            FlowRow(
                                Modifier.fillMaxWidth().clearAndSetSemantics {
                                    contentDescription =
                                        "Tagesübersicht: ${dayEntries.size} Ereignisse"
                                },
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                dayEntries.forEach { entry ->
                                    HistoryEventMarker(entry.kind, Modifier.size(14.dp, 22.dp))
                                }
                            }
                        }
                    }
                }
                items(dayEntries, key = { it.id }) { entry -> HistoryEventRow(entry, state) }
            }

        item {
            PageNavigationCard(
                "90 Tage",
                "${formats.date(period.from)} – ${formats.date(period.to)}",
                canPrevious = true,
                canNext = page > 0,
                onPrevious = { onPageChange(page + 1) },
                onNext = { onPageChange(page - 1) },
                previousDescription = "90 Tage zurück",
                nextDescription = "90 Tage vor",
                resetText = if (page > 0) "Zum aktuellen Verlauf" else null,
                onReset = { onPageChange(0) },
            )
        }
        item { EndOfPageFooter(IllustrationRole.BRAND) }
    }
}

internal fun historyEventColor(kind: HistoryKind): Color =
    when (kind) {
        HistoryKind.SHOPPING_ADDED,
        HistoryKind.GIFT_ADDED -> StatusColors.yellow
        HistoryKind.SHOPPING_CHANGED,
        HistoryKind.GIFT_CHANGED -> StatusColors.change
        HistoryKind.SHOPPING_BOUGHT,
        HistoryKind.GIFT_BOUGHT -> StatusColors.success
        HistoryKind.SHOPPING_REOPENED,
        HistoryKind.GIFT_REOPENED -> StatusColors.correction
        HistoryKind.SHOPPING_DELETED,
        HistoryKind.GIFT_DELETED -> StatusColors.correction
        HistoryKind.SHOPPING_RESTORED,
        HistoryKind.GIFT_RESTORED -> StatusColors.success
        HistoryKind.TASK_COMPLETED -> StatusColors.success
        HistoryKind.TASK_CORRECTED -> StatusColors.correction
        HistoryKind.TASK_UPDATED -> StatusColors.change
        HistoryKind.APPOINTMENT_ADDED -> StatusColors.yellow
        HistoryKind.APPOINTMENT_CHANGED -> StatusColors.change
        HistoryKind.APPOINTMENT_CANCELLED -> StatusColors.cancelled
        HistoryKind.TASK_DELETED,
        HistoryKind.APPOINTMENT_DELETED -> StatusColors.correction
        HistoryKind.TASK_RESTORED,
        HistoryKind.APPOINTMENT_RESTORED -> StatusColors.success
    }

@Composable
private fun HistoryEventMarker(kind: HistoryKind, modifier: Modifier) {
    Box(
        modifier
            .background(historyEventColor(kind), RoundedCornerShape(3.dp))
            .clearAndSetSemantics {}
    )
}

@Composable
private fun HistoryEventRow(entry: HistoryEntry, state: FamilyState) {
    val formats = rememberDateTimeFormats()
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = historyEventColor(entry.kind).copy(alpha = 0.06f),
        contentColor = AppTheme.colors.ink,
    ) {
        Row(
            Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            HistoryEventMarker(entry.kind, Modifier.padding(top = 3.dp).size(12.dp, 18.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    formats.time(
                        Instant.ofEpochMilli(entry.millis).atZone(FAMILY_ZONE).toLocalTime()
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "${entry.action} · ${entry.title}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                val subject =
                    entry.childId?.let { id -> state.sections.firstOrNull { it.id == id }?.name }
                        ?: entry.recipient?.let { appointmentRecipientName(it, state.sections) }
                if (subject != null) Text(subject, style = MaterialTheme.typography.bodySmall)
                Text(
                    if (entry.creditedId != null && entry.creditedId != entry.authorId)
                        "${entry.creditedName ?: "Elternteil"} · eingetragen von ${entry.authorName}"
                    else entry.creditedName ?: entry.authorName,
                    style = MaterialTheme.typography.bodySmall,
                )
                if (entry.labels.isNotEmpty())
                    Text(
                        entry.labels.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                    )
                entry.operationScope?.let {
                    Text(scopeLabel(it), style = MaterialTheme.typography.bodySmall)
                }
                entry.subjectDate?.let {
                    Text(
                        "Bezugstag: ${formats.date(it)}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (entry.pending)
                    Text("Synchronisierung ausstehend", style = MaterialTheme.typography.bodySmall)
                if (entry.legacy)
                    Text("Aus vorhandenem Datenstand", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
