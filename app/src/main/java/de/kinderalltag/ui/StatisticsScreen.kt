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
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle

@Composable
internal fun StatisticsScreen(state: FamilyState, today: LocalDate, days: Int, page: Long,
    onDaysChange: (Int) -> Unit, onPageChange: (Long) -> Unit) {
    val formats = rememberDateTimeFormats()
    val period = statisticsPeriod(today, days, page)
    val statistics = remember(state, period) {
        val entries = activityHistory(state.revisions, state.events, state.appointments, state.appointmentExceptions,
            state.appointmentActivity, state.parents, period, operations = state.itemOperations)
        eventStatistics(entries, state.events, state.parents, state.children)
    }
    val listState = rememberLazyListState()
    LaunchedEffect(days, page) { listState.scrollToItem(0) }
    LazyColumn(state = listState, contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { GardenBanner("Euer Alltag in Zahlen", "Kleine Schritte, die euren Alltag tragen.") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(7, 30, 90).forEach { value ->
                        FilterChip(selected = days == value, onClick = { onDaysChange(value) },
                            label = { Text("$value Tage") }, modifier = Modifier.weight(1f))
                    }
                }
                Text("${formats.date(period.from)} – ${formats.date(period.to)}", style = MaterialTheme.typography.bodyMedium)
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = GardenColors.Mint, contentColor = GardenColors.Ink)) {
                Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatisticsCounter("Ereignisse", statistics.total.events, Modifier.weight(1f))
                    StatisticsCounter("Erledigte Aufgaben", statistics.total.completedTasks, Modifier.weight(1f))
                }
            }
        }
        if (statistics.total.events == 0) item { Text("Keine Ereignisse in diesem Zeitraum.") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Farben der Ereignisse", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HistoryKind.entries.forEach { kind ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(Modifier.size(10.dp, 14.dp).background(historyEventColor(kind), RoundedCornerShape(3.dp)))
                            Text(statisticsKindLabel(kind), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
        item { StatisticsBreakdown("Nach Elternteil", statistics.parents) }
        item { StatisticsBreakdown("Nach Kind & Haushalt", statistics.subjects) }
        item {
            StatisticsBreakdown("Nach Wochentag", statistics.weekdays.map { row ->
                row.copy(label = DayOfWeek.valueOf(row.id).getDisplayName(TextStyle.FULL, formats.locale))
            })
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Gezählt wird der Tag der Eintragung, nicht der geplante Tag. Bestätigungen zählen zum ausgewählten Elternteil, andere Ereignisse zur Person, die sie eingetragen hat.",
                    style = MaterialTheme.typography.bodySmall)
                Text("Erledigte Aufgaben: Jede Ausführung zählt einmal, anhand der ersten noch gültigen Bestätigung. Zurückgenommene Bestätigungen zählen nicht; spätere Korrekturen können frühere Statistiken verändern.",
                    style = MaterialTheme.typography.bodySmall)
                Text("Ältere Terminereignisse können unvollständig sein.", style = MaterialTheme.typography.bodySmall)
                if (statistics.reconstructedEvents > 0) Text("Aus vorhandenem Datenstand rekonstruiert: ${statistics.reconstructedEvents}.",
                    style = MaterialTheme.typography.bodySmall)
                if (statistics.pendingEvents > 0) Text("Noch nicht synchronisierte Ereignisse: ${statistics.pendingEvents}.",
                    style = MaterialTheme.typography.bodySmall)
            }
        }
        item {
            PageNavigationCard("$days Tage", "${formats.date(period.from)} – ${formats.date(period.to)}",
                canPrevious = true, canNext = page > 0,
                onPrevious = { onPageChange(page + 1) }, onNext = { onPageChange(page - 1) },
                previousDescription = "$days Tage zurück", nextDescription = "$days Tage vor",
                resetText = if (page > 0) "Zum aktuellen Zeitraum" else null, onReset = { onPageChange(0) })
        }
        item { EndOfPageFooter(GardenMotif.WORM) }
    }
}

@Composable
private fun StatisticsCounter(label: String, value: Int, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(value.toString(), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = GardenColors.Green)
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun StatisticsBreakdown(title: String, rows: List<StatisticsRow>) {
    val maximum = rows.maxOfOrNull { it.counts.events }?.coerceAtLeast(1) ?: 1
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = GardenColors.Cream, contentColor = GardenColors.Ink)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (rows.isEmpty()) Text("Noch keine Elternteile verfügbar.", style = MaterialTheme.typography.bodySmall)
            rows.forEach { row ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(row.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text("${row.counts.events} ${if (row.counts.events == 1) "Ereignis" else "Ereignisse"} · " +
                        "${row.counts.completedTasks} ${if (row.counts.completedTasks == 1) "erledigte Aufgabe" else "erledigte Aufgaben"}",
                        style = MaterialTheme.typography.bodySmall)
                    Box(Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp))
                        .background(GardenColors.Mint).clearAndSetSemantics {
                            contentDescription = row.counts.byKind.entries.joinToString("; ") { (kind, count) ->
                                "${statisticsKindLabel(kind)}: $count"
                            }.ifEmpty { "Keine Ereignisse" }
                        }) {
                        if (row.counts.events > 0) Row(Modifier.fillMaxWidth(row.counts.events.toFloat() / maximum).fillMaxHeight()) {
                            HistoryKind.entries.forEach { kind ->
                                val count = row.counts.byKind[kind] ?: 0
                                if (count > 0) Box(Modifier.weight(count.toFloat()).fillMaxHeight().background(historyEventColor(kind)))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun statisticsKindLabel(kind: HistoryKind): String = when (kind) {
    HistoryKind.TASK_COMPLETED -> "Bestätigungen"
    HistoryKind.TASK_CORRECTED -> "Korrekturen"
    HistoryKind.TASK_UPDATED -> "Aufgabenverwaltung"
    HistoryKind.APPOINTMENT_ADDED -> "Neue Termine"
    HistoryKind.APPOINTMENT_CHANGED -> "Terminänderungen"
    HistoryKind.APPOINTMENT_CANCELLED -> "Terminabsagen"
    HistoryKind.TASK_DELETED -> "Gelöschte Aufgaben"
    HistoryKind.TASK_RESTORED -> "Wiederhergestellte Aufgaben"
    HistoryKind.APPOINTMENT_DELETED -> "Gelöschte Termine"
    HistoryKind.APPOINTMENT_RESTORED -> "Wiederhergestellte Termine"
}

@Preview(name = "Statistik", widthDp = 390, heightDp = 820)
@Preview(name = "Statistik · große Schrift", widthDp = 320, heightDp = 820, fontScale = 1.5f)
@Composable
private fun StatisticsPreview() {
    val today = LocalDate.of(2026, 11, 18)
    val revision = TaskRevision("r1", "task1", "johann", "Vitamin D", start = today, effective = today)
    val occurrence = Occurrence(revision, today, revision.times.single())
    val state = FamilyState(parents = listOf(Parent("a", "Papa"), Parent("b", "Mama")),
        children = listOf(Child("ferdinand", "Ferdinand"), Child("johann", "Johann")), revisions = listOf(revision),
        events = listOf(CareEvent("e1", occurrence.id, today, EventKind.COMPLETE, "a", "Papa",
            today.atTime(10, 0).atZone(FAMILY_ZONE).toInstant().toEpochMilli(), completedById = "b", completedByName = "Mama")))
    KinderTheme {
        GardenBackground(Modifier.fillMaxSize()) { StatisticsScreen(state, today, 90, 0, {}, {}) }
    }
}
