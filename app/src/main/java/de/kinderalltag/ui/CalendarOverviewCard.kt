package de.kinderalltag.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kinderalltag.domain.CalendarPreviewItem
import de.kinderalltag.domain.CalendarOverviewState
import de.kinderalltag.domain.CalendarView
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

internal val calendarOverviewSaver = Saver<CalendarOverviewState, List<String>>(
    save = { listOf(it.focus.toString(), it.view.name, it.filterActive.toString()) },
    restore = { CalendarOverviewState(LocalDate.parse(it[0]), CalendarView.valueOf(it[1]), it[2].toBoolean()) },
)

@Composable
internal fun CalendarOverviewCard(itemLabel: String, calendar: CalendarOverviewState, today: LocalDate,
    previews: Map<LocalDate, List<CalendarPreviewItem>>, clearLabel: String, onChange: (CalendarOverviewState) -> Unit,
    accentColor: Color = GardenColors.Blue) {
    val formats = rememberDateTimeFormats()
    val heading = when (calendar.view) {
        CalendarView.DAY -> formats.fullDate(calendar.focus)
        CalendarView.WEEK -> "${formats.date(calendar.period.from)} – ${formats.date(calendar.period.to)}"
        CalendarView.MONTH -> calendar.focus.format(DateTimeFormatter.ofPattern("LLLL yyyy", formats.locale))
    }
    val previousLabel = when (calendar.view) { CalendarView.DAY -> "Vorheriger Tag"; CalendarView.WEEK -> "Vorherige Woche"; CalendarView.MONTH -> "Vorheriger Monat" }
    val nextLabel = when (calendar.view) { CalendarView.DAY -> "Nächster Tag"; CalendarView.WEEK -> "Nächste Woche"; CalendarView.MONTH -> "Nächster Monat" }
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, GardenColors.Sage),
        colors = CardDefaults.cardColors(containerColor = GardenColors.Cream, contentColor = GardenColors.Ink)) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                CalendarView.entries.forEachIndexed { index, view ->
                    SegmentedButton(selected = calendar.view == view, onClick = { onChange(calendar.changeView(view)) },
                        shape = SegmentedButtonDefaults.itemShape(index, CalendarView.entries.size)) {
                        Text(when (view) { CalendarView.DAY -> "Tag"; CalendarView.WEEK -> "Woche"; CalendarView.MONTH -> "Monat" })
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                FilledIconButton(onClick = { onChange(calendar.move(-1)) }, modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(14.dp), colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = GardenColors.Green.copy(alpha = 0.12f), contentColor = GardenColors.Green)) {
                    Icon(Icons.Default.ChevronLeft, previousLabel)
                }
                Text(heading, Modifier.weight(1f).padding(horizontal = 4.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                FilledIconButton(onClick = { onChange(calendar.move(1)) }, modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(14.dp), colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = GardenColors.Green.copy(alpha = 0.12f), contentColor = GardenColors.Green)) {
                    Icon(Icons.Default.ChevronRight, nextLabel)
                }
            }
            if (calendar.view == CalendarView.DAY) {
                val items = previews[calendar.focus].orEmpty()
                Text("$itemLabel: ${items.size}", style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp))
                items.take(3).forEach { item ->
                    val time = if (item.allDay) "Ganztägig" else item.times.joinToString(" / ") { formats.time(it) }
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.1f)).padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(4.dp, 16.dp).background(accentColor, RoundedCornerShape(2.dp)))
                        Text("$time · ${item.title}", style = MaterialTheme.typography.bodySmall,
                            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    }
                }
                if (items.size > 3) Text("+${items.size - 3} weitere – alle Einträge stehen unten in der Liste.",
                    style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 4.dp))
            } else {
                Row(Modifier.fillMaxWidth()) {
                    DayOfWeek.entries.forEach { day ->
                        Text(day.getDisplayName(TextStyle.SHORT, formats.locale).trimEnd('.'), Modifier.weight(1f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center, style = MaterialTheme.typography.labelSmall)
                    }
                }
                calendar.visibleDates.chunked(7).forEach { week ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        week.forEach { date ->
                            val selected = calendar.filterActive && date == calendar.focus
                            val items = previews[date].orEmpty()
                            val limit = if (calendar.view == CalendarView.MONTH) 1 else 2
                            val remaining = (items.size - limit).coerceAtLeast(0)
                            val count = items.size
                            val inMonth = calendar.view != CalendarView.MONTH || YearMonth.from(date) == YearMonth.from(calendar.focus)
                            val color = if (selected) Color.White else if (inMonth) GardenColors.Ink else MaterialTheme.colorScheme.onSurfaceVariant
                            val shape = RoundedCornerShape(10.dp)
                            Column(Modifier.weight(1f).heightIn(min = if (calendar.view == CalendarView.MONTH) 40.dp else 48.dp).clip(shape)
                                .background(if (selected) GardenColors.Green else Color.Transparent)
                                .border(if (date == today && !selected) 1.dp else 0.dp,
                                    if (date == today && !selected) GardenColors.Sage else Color.Transparent, shape)
                                .selectable(selected, role = Role.RadioButton, onClick = { onChange(calendar.selectDay(date)) })
                                .semantics { contentDescription = "${formats.fullDate(date)}, $itemLabel: $count${if (date == today) ", heute" else ""}. " +
                                    items.take(limit).joinToString("; ") { it.title } + if (remaining > 0) "; $remaining weitere" else "" }
                                .padding(vertical = 3.dp), horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(date.dayOfMonth.toString(), color = color, style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (date == today || selected) FontWeight.Bold else FontWeight.Normal)
                                items.take(limit).forEachIndexed { index, item ->
                                    Row(Modifier.fillMaxWidth().padding(horizontal = 1.dp).clip(RoundedCornerShape(3.dp))
                                        .background(if (selected) Color.White.copy(alpha = 0.18f) else accentColor.copy(alpha = 0.12f))
                                        .padding(horizontal = 2.dp, vertical = 1.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(item.title, Modifier.weight(1f), fontSize = 9.sp, lineHeight = 11.sp,
                                            color = if (selected) Color.White else accentColor,
                                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        if (index == limit - 1 && remaining > 0) Text("+$remaining", fontSize = 8.sp, lineHeight = 11.sp,
                                            color = if (selected) Color.White else accentColor, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (calendar.filterActive) TextButton(onClick = { onChange(calendar.reset(today)) }, modifier = Modifier.align(Alignment.End)) {
                Text(clearLabel)
            }
        }
    }
}
