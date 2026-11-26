package de.kinderalltag.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.kinderalltag.data.FamilyState
import de.kinderalltag.domain.*
import de.kinderalltag.text.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle

@Composable
internal fun StatisticsScreen(
    state: FamilyState,
    today: LocalDate,
    days: Int,
    page: Long,
    onDaysChange: (Int) -> Unit,
    onPageChange: (Long) -> Unit,
) {
    val formats = rememberDateTimeFormats()
    val period = statisticsPeriod(today, days, page)
    val statistics =
        remember(state, period) {
            val entries =
                activityHistory(
                    state.revisions,
                    state.events,
                    state.appointments,
                    state.appointmentExceptions,
                    state.appointmentActivity,
                    state.parents,
                    period,
                    operations = state.itemOperations,
                    listActivity = state.listActivity,
                )
            eventStatistics(
                entries,
                state.events,
                state.parents,
                state.children,
                state.listActivity,
            )
        }
    val listState = rememberLazyListState()
    LaunchedEffect(days, page) { listState.scrollToItem(0) }
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(text(TextKey.CHILD_STATISTICS_HELP), style = MaterialTheme.typography.bodySmall)
        }
        item {
            AppBanner(
                text(TextKey.YOUR_FAMILY_DAY_IN_NUMBERS),
                text(TextKey.LITTLE_STEPS_THAT_CARRY_YOUR_FAMILY_DAY),
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(7, 30, 90).forEach { value ->
                        FilterChip(
                            selected = days == value,
                            onClick = { onDaysChange(value) },
                            label = { Text(text(TextKey.DAYS, value)) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Text(
                    "${formats.date(period.from)} – ${formats.date(period.to)}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        item {
            Card(
                colors =
                    CardDefaults.cardColors(
                        containerColor = AppTheme.colors.accentContainer,
                        contentColor = AppTheme.colors.ink,
                    )
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    StatisticsCounter(
                        text(TextKey.EVENTS),
                        statistics.total.events,
                        Modifier.weight(1f),
                    )
                    StatisticsCounter(
                        text(TextKey.COMPLETED_TASKS),
                        statistics.total.completedTasks,
                        Modifier.weight(1f),
                    )
                }
            }
        }
        item {
            Card(
                colors =
                    CardDefaults.cardColors(
                        containerColor = AppTheme.colors.primaryContainer,
                        contentColor = AppTheme.colors.ink,
                    )
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    StatisticsCounter(
                        text(TextKey.BOUGHT_ITEMS),
                        statistics.total.boughtShopping,
                        Modifier.weight(1f),
                    )
                    StatisticsCounter(
                        text(TextKey.BOUGHT_GIFTS),
                        statistics.total.boughtGifts,
                        Modifier.weight(1f),
                    )
                }
            }
        }
        if (statistics.total.events == 0) item { Text(text(TextKey.NO_EVENTS_IN_THIS_PERIOD)) }
        if (statistics.total.events > 0)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text(TextKey.EVENT_COLOURS),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        HistoryKind.entries
                            .filter { (statistics.total.byKind[it] ?: 0) > 0 }
                            .forEach { kind ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Box(
                                        Modifier.size(10.dp, 14.dp)
                                            .background(
                                                historyEventColor(kind),
                                                RoundedCornerShape(3.dp),
                                            )
                                    )
                                    Text(
                                        statisticsKindLabel(kind),
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                    }
                }
            }
        item { StatisticsBreakdown(text(TextKey.BY_PARENT), statistics.parents) }
        item { StatisticsBreakdown(text(TextKey.BY_AREA), statistics.subjects) }
        item {
            StatisticsBreakdown(
                text(TextKey.BY_WEEKDAY),
                statistics.weekdays.map { row ->
                    row.copy(
                        label =
                            DayOfWeek.valueOf(row.id).getDisplayName(TextStyle.FULL, formats.locale)
                    )
                },
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text(
                        TextKey
                            .COUNTS_USE_THE_DAY_THE_EVENT_WAS_RECORDED_NOT_ITS_SCHEDULED_DAY_CONFIRMA
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text(
                        TextKey
                            .COMPLETED_TASKS_EACH_OCCURRENCE_COUNTS_ONCE_USING_ITS_FIRST_STILL_VALID
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text(
                        TextKey
                            .PURCHASED_ENTRIES_COUNT_ONCE_USING_THEIR_FIRST_STILL_VALID_PURCHASE_CONF
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text(TextKey.OLDER_APPOINTMENT_EVENTS_MAY_BE_INCOMPLETE),
                    style = MaterialTheme.typography.bodySmall,
                )
                if (statistics.reconstructedEvents > 0)
                    Text(
                        text(
                            TextKey.RECONSTRUCTED_FROM_AVAILABLE_DATA,
                            statistics.reconstructedEvents,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                    )
                if (statistics.pendingEvents > 0)
                    Text(
                        text(TextKey.EVENTS_NOT_YET_SYNCHRONIZED, statistics.pendingEvents),
                        style = MaterialTheme.typography.bodySmall,
                    )
            }
        }
        item {
            PageNavigationCard(
                text(TextKey.DAYS, days),
                "${formats.date(period.from)} – ${formats.date(period.to)}",
                canPrevious = true,
                canNext = page > 0,
                onPrevious = { onPageChange(page + 1) },
                onNext = { onPageChange(page - 1) },
                previousDescription = text(TextKey.DAYS_BACK, days),
                nextDescription = text(TextKey.DAYS_FORWARD, days),
                resetText = if (page > 0) text(TextKey.CURRENT_PERIOD) else null,
                onReset = { onPageChange(0) },
            )
        }
        item { EndOfPageFooter(IllustrationRole.FRIEND) }
    }
}

@Composable
private fun StatisticsCounter(label: String, value: Int, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            value.toString(),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = AppTheme.colors.accent,
        )
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun StatisticsBreakdown(title: String, rows: List<StatisticsRow>) {
    val maximum = rows.maxOfOrNull { it.counts.events }?.coerceAtLeast(1) ?: 1
    Card(
        Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = AppTheme.colors.background,
                contentColor = AppTheme.colors.ink,
            ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (rows.isEmpty())
                Text(
                    text(TextKey.NO_PARENTS_AVAILABLE_YET),
                    style = MaterialTheme.typography.bodySmall,
                )
            rows.forEach { row ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        row.label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "${row.counts.events} ${if (row.counts.events == 1) text(TextKey.EVENT) else text(TextKey.EVENTS)} · " +
                            "${row.counts.completedTasks} ${if (row.counts.completedTasks == 1) text(TextKey.COMPLETED_TASK) else text(TextKey.COMPLETED_TASKS_319F86)}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        text(
                            TextKey.BOUGHT_SHOPPING_ITEMS_GIFT_IDEAS,
                            row.counts.boughtShopping,
                            row.counts.boughtGifts,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Box(
                        Modifier.fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(AppTheme.colors.accentContainer)
                            .clearAndSetSemantics {
                                contentDescription =
                                    row.counts.byKind.entries
                                        .joinToString("; ") { (kind, count) ->
                                            "${statisticsKindLabel(kind)}: $count"
                                        }
                                        .ifEmpty { text(TextKey.NO_EVENTS) }
                            }
                    ) {
                        if (row.counts.events > 0)
                            Row(
                                Modifier.fillMaxWidth(row.counts.events.toFloat() / maximum)
                                    .fillMaxHeight()
                            ) {
                                HistoryKind.entries.forEach { kind ->
                                    val count = row.counts.byKind[kind] ?: 0
                                    if (count > 0)
                                        Box(
                                            Modifier.weight(count.toFloat())
                                                .fillMaxHeight()
                                                .background(historyEventColor(kind))
                                        )
                                }
                            }
                    }
                }
            }
        }
    }
}

private fun statisticsKindLabel(kind: HistoryKind): String =
    when (kind) {
        HistoryKind.SHOPPING_ADDED -> text(TextKey.SHOPPING_ITEM_ADDED)
        HistoryKind.SHOPPING_CHANGED -> text(TextKey.SHOPPING_ITEM_CHANGED)
        HistoryKind.SHOPPING_BOUGHT -> text(TextKey.SHOPPING_ITEM_BOUGHT)
        HistoryKind.SHOPPING_REOPENED -> text(TextKey.SHOPPING_ITEM_REOPENED)
        HistoryKind.SHOPPING_DELETED -> text(TextKey.SHOPPING_ITEM_DELETED)
        HistoryKind.SHOPPING_RESTORED -> text(TextKey.SHOPPING_ITEM_RESTORED)
        HistoryKind.GIFT_ADDED -> text(TextKey.GIFT_IDEA_ADDED)
        HistoryKind.GIFT_CHANGED -> text(TextKey.GIFT_IDEA_CHANGED)
        HistoryKind.GIFT_BOUGHT -> text(TextKey.GIFT_IDEA_BOUGHT)
        HistoryKind.GIFT_REOPENED -> text(TextKey.GIFT_IDEA_REOPENED)
        HistoryKind.GIFT_DELETED -> text(TextKey.GIFT_IDEA_DELETED)
        HistoryKind.GIFT_RESTORED -> text(TextKey.GIFT_IDEA_RESTORED)
        HistoryKind.TASK_COMPLETED -> text(TextKey.CONFIRMATIONS)
        HistoryKind.TASK_CORRECTED -> text(TextKey.CORRECTIONS)
        HistoryKind.TASK_UPDATED -> text(TextKey.TASK_MANAGEMENT)
        HistoryKind.APPOINTMENT_ADDED -> text(TextKey.NEW_APPOINTMENTS)
        HistoryKind.APPOINTMENT_CHANGED -> text(TextKey.APPOINTMENT_CHANGES)
        HistoryKind.APPOINTMENT_CANCELLED -> text(TextKey.APPOINTMENT_CANCELLATIONS)
        HistoryKind.TASK_DELETED -> text(TextKey.DELETED_TASKS)
        HistoryKind.TASK_RESTORED -> text(TextKey.RESTORED_TASKS)
        HistoryKind.APPOINTMENT_DELETED -> text(TextKey.DELETED_APPOINTMENTS)
        HistoryKind.APPOINTMENT_RESTORED -> text(TextKey.RESTORED_APPOINTMENTS)
    }

@Preview(name = "Statistik", widthDp = 390, heightDp = 820)
@Preview(name = "Statistik · große Schrift", widthDp = 320, heightDp = 820, fontScale = 1.5f)
@Composable
private fun StatisticsPreview() {
    val today = LocalDate.of(2026, 11, 18)
    val revision =
        TaskRevision("r1", "task1", "johann", "Vitamin D", start = today, effective = today)
    val occurrence = Occurrence(revision, today, revision.times.single())
    val state =
        FamilyState(
            parents = listOf(Parent("a", "Papa"), Parent("b", "Mama")),
            children = listOf(Child("ferdinand", "Ferdinand"), Child("johann", "Johann")),
            revisions = listOf(revision),
            events =
                listOf(
                    CareEvent(
                        "e1",
                        occurrence.id,
                        today,
                        EventKind.COMPLETE,
                        "a",
                        "Papa",
                        today.atTime(10, 0).atZone(FAMILY_ZONE).toInstant().toEpochMilli(),
                        completedById = "b",
                        completedByName = "Mama",
                    )
                ),
        )
    KinderTheme {
        AppBackground(Modifier.fillMaxSize()) { StatisticsScreen(state, today, 90, 0, {}, {}) }
    }
}
