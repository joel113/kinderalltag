@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package de.kinderalltag.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.kinderalltag.data.*
import de.kinderalltag.domain.*
import java.time.*
import java.util.UUID

internal data class AppointmentEditTarget(val original: AppointmentOccurrence?, val following: Boolean = false,
    val imported: CalendarImport? = null, val importKey: String? = null,
    val operationVersion: Long = 0)
internal class AppointmentUiState {
    var selected by mutableStateOf<AppointmentOccurrence?>(null)
    var selectedOperationVersion: Long = 0
    var deletion by mutableStateOf<ItemActionTarget?>(null)
    var editor by mutableStateOf<AppointmentEditTarget?>(null)
    fun open(occurrence: AppointmentOccurrence, state: FamilyState) {
        selectedOperationVersion = state.version(ItemType.APPOINTMENT, state.rootId(occurrence))
        selected = occurrence
    }
}

internal fun visibleAppointments(state: FamilyState, from: LocalDate, to: LocalDate, section: String?, onlyMine: Boolean) =
    state.appointmentOccurrences(from, to).filter {
        it.details.recipient.matches(section) && (!onlyMine || isResponsible(it.details.responsibleParentId, state.uid))
    }

@Composable
internal fun AppointmentCard(occurrence: AppointmentOccurrence, state: FamilyState, onClick: () -> Unit) {
    val formats = rememberDateTimeFormats()
    val details = occurrence.details
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GardenColors.Sky, contentColor = GardenColors.Ink)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                state.sections.filter { details.recipient.matches(it.id) }.forEach { ChildIcon(it, Modifier.size(28.dp)) }
                Text(appointmentRecipientName(details.recipient, state.sections), style = MaterialTheme.typography.labelLarge)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(details.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Icon(Icons.Default.ChevronRight, null, tint = GardenColors.Blue)
            }
            Text(if (details.allDay) "Ganztägig" else formats.timeRange(details.startTime, details.endTime))
            if (details.location.isNotBlank()) Text(details.location)
            Text("Zuständig: ${responsibilityName(details.responsibleParentId, state.parents)}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
internal fun AppointmentAgenda(state: FamilyState, today: LocalDate, now: Instant, page: Long,
    section: String?, onlyMine: Boolean, encouragement: String, showIntroduction: Boolean, showCalendar: Boolean,
    calendar: CalendarOverviewState, onCalendarChange: (CalendarOverviewState) -> Unit, onPageChange: (Long) -> Unit, onOpen: (AppointmentOccurrence) -> Unit) {
    val formats = rememberDateTimeFormats()
    val calendarFiltered = showCalendar && calendar.filterActive
    val from = if (calendarFiltered) calendar.period.from else today.plusDays(page * 90)
    val to = if (calendarFiltered) calendar.period.to else from.plusDays(89)
    val calendarDates = calendar.visibleDates
    val calendarPreviews = if (showCalendar) appointmentCalendarPreviews(state.appointments, state.appointmentExceptions, calendarDates, section, state.uid, onlyMine, state.appointmentActivity, state.itemOperations) else emptyMap()
    val appointments = visibleAppointments(state, from, to, section, onlyMine)
        .filter { calendarFiltered || it.details.endInstant > now }
    LazyColumn(contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 100.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (showIntroduction) item { GardenBanner("Eure Termine", encouragement) }
        if (showCalendar) item {
            CalendarOverviewCard("Termine", calendar, today, calendarPreviews, "Kommende Termine anzeigen", onCalendarChange)
        }
        if (appointments.isEmpty()) item { Text("Keine Termine in diesem Zeitraum.") }
        appointments.groupBy { it.details.date }.forEach { (date, occurrences) ->
            item { Text(formats.fullDate(date), fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 12.dp)) }
            items(occurrences, key = { it.id }) { AppointmentCard(it, state) { onOpen(it) } }
        }
        if (!calendarFiltered) item { PeriodNavigationCard(today, page, onPageChange) }
        item { EndOfPageFooter(GardenMotif.SUN) }
    }
}

@Composable
internal fun AppointmentDialogs(ui: AppointmentUiState, state: FamilyState, vm: FamilyViewModel, onImported: (String, () -> Unit) -> Unit) {
    val context = LocalContext.current
    val formats = rememberDateTimeFormats()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val now by vm.clock.collectAsStateWithLifecycle()
    val saveError by vm.appointmentError.collectAsStateWithLifecycle()
    LaunchedEffect(ui.selected, ui.editor) { vm.appointmentError.value = null }
    ui.selected?.let { selected ->
        var following by remember(selected.id) { mutableStateOf(false) }
        val future = selected.details.startInstant > now
        val visible = state.appointmentOccurrences(selected.details.date, selected.details.date).any { it.id == selected.id && it.details == selected.details }
        val allowFollowing = selected.series.repeat != AppointmentRepeat.ONCE
        val canManage = visible && !busy && !state.fromCache && state.error == null
        val details = selected.details
        ItemDetailsDialog(details.title, "Termin", { ui.selected = null }, busy, actions = {
            DetailAction("Im Kalender speichern", Icons.Default.EventAvailable, visible && !busy, modifier = Modifier.fillMaxWidth(), onClick = {
                saveAppointmentToCalendar(context, selected, appointmentRecipientName(details.recipient, state.sections))
            })
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (future) DetailAction("Bearbeiten", Icons.Default.Edit, canManage, onClick = {
                    ui.editor = AppointmentEditTarget(selected, following, operationVersion = ui.selectedOperationVersion)
                    ui.selected = null
                })
                DetailAction("Teilen", Icons.Default.Share, visible && !busy, onClick = {
                    shareAppointment(context, selected, appointmentRecipientName(details.recipient, state.sections))
                })
                DetailAction("Löschen", Icons.Default.DeleteOutline, canManage, destructive = true, onClick = {
                    ui.deletion = ItemActionTarget(true, appointment = selected, expectedVersion = ui.selectedOperationVersion)
                    ui.selected = null
                })
            }
        }) {
            DetailSection("Auf einen Blick") {
                DetailValue(Icons.Default.Face, "Bereich", appointmentRecipientName(details.recipient, state.sections))
                DetailValue(Icons.Default.Event, "Datum", formats.fullDate(details.date))
                DetailValue(Icons.Default.Schedule, "Uhrzeit", if (details.allDay) "Ganztägig" else formats.timeRange(details.startTime, details.endTime))
                DetailValue(Icons.Default.Person, "Zuständig", responsibilityName(details.responsibleParentId, state.parents))
                DetailValue(Icons.Default.Repeat, "Wiederholung", if (selected.series.repeat == AppointmentRepeat.ONCE) "Einmalig"
                    else "Alle ${selected.series.interval} ${repeatLabel(selected.series.repeat)}")
                DetailValue(Icons.Default.Notifications, "Erinnerung", reminderLabel(details.reminderMinutes, details.allDay, formats))
                if (details.location.isNotBlank()) DetailValue(Icons.Default.Place, "Ort", details.location)
            }
            if (details.notes.isNotBlank()) DetailSection("Notizen", GardenColors.Cream) { Text(details.notes) }
            Text("Im Kalender speichern öffnet deine Kalender-App zur Bestätigung. Übernommen wird nur dieser Termin als unabhängige Kopie; spätere Änderungen werden nicht automatisch abgeglichen.",
                style = MaterialTheme.typography.bodySmall)
            if (future && allowFollowing) DetailSection("Änderung gilt für", GardenColors.Cream) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(!following, { following = false }, label = { Text("Nur diesen Termin") })
                    FilterChip(following, { following = true }, label = { Text("Diesen und folgende") })
                }
                Text("Wähle den Umfang, bevor du auf Bearbeiten tippst.", style = MaterialTheme.typography.bodySmall)
            }
            if (!future) DetailNotice("Dieser Termin hat bereits begonnen. Bearbeiten ist nicht mehr möglich; Löschen bleibt verfügbar.")
            if (!visible) DetailNotice("Dieser Termin wurde geändert, gelöscht oder verschoben. Bitte schließen und die Übersicht neu öffnen.")
            if (state.fromCache) DetailNotice("Gespeicherter Stand. Zum Bearbeiten oder Löschen ist eine Verbindung erforderlich.")
            saveError?.let { DetailNotice(it) }
        }
    }
    ui.deletion?.let { target -> ItemActionDialog(target, state, vm, onDismiss = { ui.deletion = null }, onEditTask = {}) }
    ui.editor?.let { target ->
        var finishingImport by remember(target) { mutableStateOf(false) }
        AppointmentEditor(target, state, busy || finishingImport, saveError, onRetrySync = vm::retrySync,
            onDismiss = { if (!busy && !finishingImport) ui.editor = null }) { draft ->
            vm.saveAppointment(target.original, draft, target.following, operationVersion = target.operationVersion) {
                val key = target.importKey
                if (key == null) ui.editor = null
                else {
                    finishingImport = true
                    onImported(key) { finishingImport = false; ui.editor = null }
                }
            }
        }
    }
}

internal fun repeatLabel(repeat: AppointmentRepeat): String = when (repeat) {
    AppointmentRepeat.ONCE -> "Einmalig"
    AppointmentRepeat.DAYS -> "Tage"
    AppointmentRepeat.WEEKS -> "Wochen"
    AppointmentRepeat.MONTHS -> "Monate"
    AppointmentRepeat.YEARS -> "Jahre"
}
private fun reminderLabel(minutes: Int?, allDay: Boolean, formats: DateTimeFormats): String = when {
    minutes == null -> "Keine"
    allDay && minutes == 0 -> "Am selben Tag um ${formats.time(LocalTime.of(9, 0))}"
    allDay -> "Am Vortag um ${formats.time(LocalTime.of(9, 0))}"
    minutes == 0 -> "Bei Beginn"
    minutes == 1440 -> "1 Tag vorher"
    else -> "$minutes Minuten vorher"
}

@Composable
private fun AppointmentEditor(target: AppointmentEditTarget, state: FamilyState, busy: Boolean, saveError: String?,
    onRetrySync: () -> Unit, onDismiss: () -> Unit, onSave: (AppointmentSeries) -> Unit) {
    val formats = rememberDateTimeFormats()
    val series = target.original?.series ?: target.imported?.series
    val seed = target.original?.details ?: target.imported?.series?.let { it.details.copy(date = it.activeFrom) }
    val singleException = target.original != null && !target.following
    val id = remember { UUID.randomUUID().toString() }
    var title by remember { mutableStateOf(seed?.title.orEmpty()) }
    var recipient by remember { mutableStateOf(seed?.recipient ?: AppointmentRecipient.FERDINAND) }
    var parent by remember { mutableStateOf(seed?.responsibleParentId) }
    var date by remember { mutableStateOf(seed?.date ?: LocalDate.now(FAMILY_ZONE).plusDays(1)) }
    var start by remember { mutableStateOf(seed?.startTime ?: LocalTime.of(9, 0)) }
    var end by remember { mutableStateOf(seed?.endTime ?: LocalTime.of(10, 0)) }
    var allDay by remember { mutableStateOf(seed?.allDay ?: false) }
    var location by remember { mutableStateOf(seed?.location.orEmpty()) }
    var notes by remember { mutableStateOf(seed?.notes.orEmpty()) }
    var reminder by remember { mutableStateOf(if (seed != null) seed.reminderMinutes else 30) }
    var repeat by remember { mutableStateOf(if (singleException) AppointmentRepeat.ONCE else series?.repeat ?: AppointmentRepeat.ONCE) }
    var interval by remember { mutableStateOf((series?.interval ?: 1).toString()) }
    var until by remember { mutableStateOf(if (singleException) null else series?.until) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmDraft by remember { mutableStateOf<AppointmentSeries?>(null) }
    var importReviewed by remember { mutableStateOf(target.imported?.warnings.isNullOrEmpty()) }
    val duplicateImport = target.imported != null && state.appointmentOccurrences(date, date).any {
        it.details.title == title.trim() && it.details.allDay == allDay &&
            (allDay || (it.details.startTime == start && it.details.endTime == end))
    }
    var duplicateReviewed by remember(title, date, start, end, allDay) { mutableStateOf(false) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false,
        dismissOnBackPress = !busy, dismissOnClickOutside = !busy)) {
        GardenBackground(Modifier.fillMaxSize()) {
            Column(Modifier.safeDrawingPadding().imePadding()) {
                TopAppBar(title = { Text(if (target.imported != null) "Termin übernehmen" else if (seed == null) "Neuer Termin" else "Termin bearbeiten") },
                    navigationIcon = { IconButton(onClick = onDismiss, enabled = !busy) { Icon(Icons.Default.Close, "Schließen") } })
                LazyColumn(contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    if (target.imported != null) {
                        item { Text("Bitte Datum, Zuordnung, Zuständigkeit und Wiederholung prüfen. Erst Speichern übernimmt den Termin in euren Haushalt.") }
                        target.imported.warnings.forEach { warning -> item { Text(warning, color = MaterialTheme.colorScheme.error) } }
                        if (target.imported.warnings.isNotEmpty()) item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(importReviewed, { importReviewed = it }, enabled = !busy)
                                Text("Ich habe die Importhinweise geprüft.", Modifier.weight(1f))
                            }
                        }
                        if (duplicateImport) item {
                            Text("Ein ähnlicher Termin ist bereits vorhanden.", color = MaterialTheme.colorScheme.error)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(duplicateReviewed, { duplicateReviewed = it }, enabled = !busy)
                                Text("Trotzdem eine weitere Kopie anlegen.", Modifier.weight(1f))
                            }
                        }
                    }
                    item { OutlinedTextField(title, { title = it }, label = { Text("Titel") }, modifier = Modifier.fillMaxWidth()) }
                    item { Text("Für welchen Bereich?", fontWeight = FontWeight.Bold) }
                    item { Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppointmentRecipient.entries.forEach { value -> FilterChip(value == recipient, { recipient = value },
                            label = { Text(appointmentRecipientName(value, state.sections)) }) }
                    } }
                    item { AssignmentPicker(state.parents, parent) { parent = it } }
                    item { DatePickerField("Datum", date, { it?.let { chosen -> date = chosen } }, enabled = !busy,
                        minDate = if (allDay) LocalDate.now(FAMILY_ZONE).plusDays(1) else LocalDate.now(FAMILY_ZONE)) }
                    item { Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Ganztägig", Modifier.weight(1f))
                        Switch(allDay, { allDay = it; if (reminder != null) reminder = if (it) 1440 else 30 })
                    } }
                    if (!allDay) {
                        item { TimePickerField("Beginn", start, { start = it }, enabled = !busy) }
                        item { TimePickerField("Ende (gleicher Tag)", end, { end = it }, enabled = !busy) }
                    }
                    item { OutlinedTextField(location, { location = it }, label = { Text("Ort (optional)") }, modifier = Modifier.fillMaxWidth()) }
                    item { OutlinedTextField(notes, { notes = it }, label = { Text("Notizen (optional)") }, modifier = Modifier.fillMaxWidth(), minLines = 2) }
                    if (!singleException) {
                        if (target.following) item { Text("Bei unverändertem Datum und Intervall bleibt der ursprüngliche Rhythmus erhalten.") }
                        item { Text("Wiederholung", fontWeight = FontWeight.Bold) }
                        item { Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AppointmentRepeat.entries.forEach { value -> FilterChip(value == repeat, { repeat = value }, label = { Text(repeatLabel(value)) }) }
                        } }
                        if (repeat != AppointmentRepeat.ONCE) {
                            item { OutlinedTextField(interval, { interval = it }, label = { Text("Alle wie viele ${repeatLabel(repeat)}? (1–1000)") }, modifier = Modifier.fillMaxWidth()) }
                            item { DatePickerField("Bis einschließlich (optional)", until, { until = it }, optional = true, enabled = !busy, minDate = date) }
                        }
                    }
                    item { Text("Erinnerung", fontWeight = FontWeight.Bold) }
                    item { Column {
                        val options = if (allDay) listOf(null, 0, 1440) else listOf(null, 0, 15, 30, 60, 1440)
                        options.forEach { value -> FilterChip(value == reminder, { reminder = value }, label = { Text(reminderLabel(value, allDay, formats)) }) }
                    } }
                    error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
                    saveError?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
                    if (state.error != null) item {
                        Text(state.error, color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = onRetrySync, enabled = !busy) { Text("Erneut versuchen") }
                    } else if (state.fromCache) item { Text("Zum Speichern ist eine Verbindung erforderlich.") }
                    item { Button(enabled = !busy && !state.fromCache && importReviewed && (!duplicateImport || duplicateReviewed), modifier = Modifier.fillMaxWidth(), onClick = {
                        try {
                            val details = AppointmentDetails(title.trim(), recipient, date,
                                if (allDay) LocalTime.of(9, 0) else start,
                                if (allDay) LocalTime.of(10, 0) else end,
                                allDay, parent, location.trim(), notes.trim(), reminder)
                            require(details.startInstant > Instant.now()) { "Der Termin muss in der Zukunft liegen." }
                            val count = if (repeat == AppointmentRepeat.ONCE) 1 else interval.toInt()
                            val preserveAnchor = (target.following || target.imported != null) && series != null && seed != null &&
                                details.date == seed.date && (target.imported != null || series.contains(seed.date)) &&
                                repeat != AppointmentRepeat.ONCE && repeat == series.repeat && count == series.interval
                            val draft = AppointmentSeries(id, if (preserveAnchor) details.copy(date = requireNotNull(series).details.date) else details,
                                repeat, count, if (repeat == AppointmentRepeat.ONCE) null else until,
                                fromDate = details.date)
                            draft.validate()
                            error = null
                            if (target.following) confirmDraft = draft else onSave(draft)
                        } catch (e: Exception) { error = if (e is NumberFormatException)
                            "Bitte das Intervall als ganze Zahl eingeben." else e.message ?: "Bitte Angaben prüfen." }
                    }) { Text(if (busy) "Speichert …" else "Termin speichern") } }
                }
            }
        }
        confirmDraft?.let { draft -> AlertDialog(onDismissRequest = { confirmDraft = null }, title = { Text("Zukünftige Termine ersetzen?") },
            text = { Text("Dieser und alle folgenden Termine werden ersetzt. Begonnene Termine und einzelne Ausnahmen bleiben erhalten. Löschungen werden nicht rückgängig gemacht.") },
            confirmButton = { TextButton(onClick = { confirmDraft = null; onSave(draft) }) { Text("Ersetzen") } },
            dismissButton = { TextButton(onClick = { confirmDraft = null }) { Text("Abbrechen") } }) }
    }
}
