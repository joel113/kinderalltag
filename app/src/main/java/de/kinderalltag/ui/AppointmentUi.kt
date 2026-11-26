@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package de.kinderalltag.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import de.kinderalltag.text.*
import java.time.*
import java.util.UUID

internal data class AppointmentEditTarget(
    val original: AppointmentOccurrence?,
    val following: Boolean = false,
    val imported: CalendarImport? = null,
    val importKey: String? = null,
    val operationVersion: Long = 0,
    val initialDate: LocalDate? = null,
)

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

internal fun visibleAppointments(
    state: FamilyState,
    from: LocalDate,
    to: LocalDate,
    section: String?,
    onlyMine: Boolean,
) =
    state.appointmentOccurrences(from, to).filter {
        it.details.recipient.matches(section) &&
            (section != null || it.details.recipient.visibleFor(state.children)) &&
            (!onlyMine || isResponsible(it.details.responsibleParentId, state.uid))
    }

@Composable
internal fun AppointmentCard(
    occurrence: AppointmentOccurrence,
    state: FamilyState,
    onClick: () -> Unit,
) {
    val formats = rememberDateTimeFormats()
    val details = occurrence.details
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = AppTheme.colors.primaryContainer,
                contentColor = AppTheme.colors.ink,
            ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                state.sections
                    .filter { details.recipient.matches(it.id) }
                    .forEach { ChildIcon(it, Modifier.size(28.dp)) }
                Text(
                    appointmentRecipientName(details.recipient, state.sections),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    details.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Icon(Icons.Default.ChevronRight, null, tint = AppTheme.colors.primary)
            }
            Text(
                if (details.allDay) text(TextKey.ALL_DAY)
                else formats.timeRange(details.startTime, details.endTime)
            )
            ItemMetadataIndicators(details.notes, details.location)
            Text(
                text(
                    TextKey.ASSIGNED_TO_C1E2EE,
                    responsibilityName(details.responsibleParentId, state.parents),
                ),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
internal fun AppointmentAgenda(
    state: FamilyState,
    today: LocalDate,
    now: Instant,
    page: Long,
    section: String?,
    onlyMine: Boolean,
    encouragement: String,
    showIntroduction: Boolean,
    showCalendar: Boolean,
    calendar: CalendarOverviewState,
    onCalendarChange: (CalendarOverviewState) -> Unit,
    onPageChange: (Long) -> Unit,
    onOpen: (AppointmentOccurrence) -> Unit,
) {
    val formats = rememberDateTimeFormats()
    val calendarFiltered = showCalendar && calendar.filterActive
    val from = if (calendarFiltered) calendar.period.from else today.plusDays(page * 90)
    val to = if (calendarFiltered) calendar.period.to else from.plusDays(89)
    val calendarDates = calendar.visibleDates
    val calendarPreviews =
        if (showCalendar)
            appointmentCalendarPreviews(
                state.appointments,
                state.appointmentExceptions,
                calendarDates,
                section,
                state.uid,
                onlyMine,
                state.appointmentActivity,
                state.itemOperations,
                children = state.children,
            )
        else emptyMap()
    val appointments =
        visibleAppointments(state, from, to, section, onlyMine).filter {
            calendarFiltered || it.details.endInstant > now
        }
    LazyColumn(
        contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (showIntroduction) item { AppBanner(text(TextKey.YOUR_APPOINTMENTS), encouragement) }
        if (showCalendar)
            item {
                CalendarOverviewCard(
                    text(TextKey.APPOINTMENTS),
                    calendar,
                    today,
                    calendarPreviews,
                    text(TextKey.SHOW_UPCOMING_APPOINTMENTS),
                    onCalendarChange,
                )
            }
        if (appointments.isEmpty()) item { Text(text(TextKey.NO_APPOINTMENTS_IN_THIS_PERIOD)) }
        appointments
            .groupBy { it.details.date }
            .forEach { (date, occurrences) ->
                item {
                    Text(
                        formats.fullDate(date),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
                items(occurrences, key = { it.id }) { AppointmentCard(it, state) { onOpen(it) } }
            }
        if (!calendarFiltered) item { PeriodNavigationCard(today, page, onPageChange) }
        item { EndOfPageFooter(IllustrationRole.JOY) }
    }
}

@Composable
internal fun AppointmentDialogs(
    ui: AppointmentUiState,
    state: FamilyState,
    vm: FamilyViewModel,
    onImported: (String, () -> Unit) -> Unit,
) {
    val context = LocalContext.current
    val formats = rememberDateTimeFormats()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val now by vm.clock.collectAsStateWithLifecycle()
    val saveError by vm.appointmentError.collectAsStateWithLifecycle()
    LaunchedEffect(ui.selected, ui.editor) { vm.appointmentError.value = null }
    ui.selected?.let { selected ->
        var following by remember(selected.id) { mutableStateOf(false) }
        val future = selected.details.startInstant > now
        val visible =
            state.appointmentOccurrences(selected.details.date, selected.details.date).any {
                it.id == selected.id && it.details == selected.details
            }
        val allowFollowing = selected.series.repeat != AppointmentRepeat.ONCE
        val canManage = visible && !busy && !state.fromCache && state.error == null
        val details = selected.details
        ItemDetailsDialog(
            details.title,
            text(TextKey.APPOINTMENT),
            { ui.selected = null },
            busy,
            actions = {
                DetailActions(
                    primary = {
                        DetailAction(
                            text(TextKey.SAVE_TO_CALENDAR),
                            Icons.Default.EventAvailable,
                            visible && !busy,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                saveAppointmentToCalendar(
                                    context,
                                    selected,
                                    appointmentRecipientName(details.recipient, state.sections),
                                )
                            },
                        )
                    },
                    secondary =
                        buildList {
                            if (future)
                                add(
                                    DetailActionSpec(
                                        text(TextKey.EDIT),
                                        Icons.Default.Edit,
                                        canManage,
                                        onClick = {
                                            ui.editor =
                                                AppointmentEditTarget(
                                                    selected,
                                                    following,
                                                    operationVersion = ui.selectedOperationVersion,
                                                )
                                            ui.selected = null
                                        },
                                    )
                                )
                            add(
                                DetailActionSpec(
                                    text(TextKey.SHARE),
                                    Icons.Default.Share,
                                    visible && !busy,
                                    onClick = {
                                        shareAppointment(
                                            context,
                                            selected,
                                            appointmentRecipientName(
                                                details.recipient,
                                                state.sections,
                                            ),
                                        )
                                    },
                                )
                            )
                        },
                    deleteAction =
                        DetailActionSpec(
                            text(TextKey.DELETE),
                            Icons.Default.DeleteOutline,
                            canManage,
                            onClick = {
                                ui.deletion =
                                    ItemActionTarget(
                                        true,
                                        appointment = selected,
                                        expectedVersion = ui.selectedOperationVersion,
                                    )
                                ui.selected = null
                            },
                        ),
                )
            },
        ) {
            DetailSection(text(TextKey.AT_A_GLANCE)) {
                DetailValue(
                    Icons.Default.Face,
                    text(TextKey.AREA),
                    appointmentRecipientName(details.recipient, state.sections),
                )
                DetailValue(Icons.Default.Event, text(TextKey.DATE), formats.fullDate(details.date))
                DetailValue(
                    Icons.Default.Schedule,
                    text(TextKey.TIME),
                    if (details.allDay) text(TextKey.ALL_DAY)
                    else formats.timeRange(details.startTime, details.endTime),
                )
                DetailValue(
                    Icons.Default.Person,
                    text(TextKey.ASSIGNED_TO),
                    responsibilityName(details.responsibleParentId, state.parents),
                )
                DetailValue(
                    Icons.Default.Repeat,
                    text(TextKey.RECURRENCE),
                    if (selected.series.repeat == AppointmentRepeat.ONCE) text(TextKey.ONE_OFF)
                    else
                        text(
                            TextKey.EVERY,
                            selected.series.interval,
                            repeatLabel(selected.series.repeat),
                        ),
                )
                DetailValue(
                    Icons.Default.Notifications,
                    text(TextKey.REMINDER),
                    reminderLabel(details.reminderMinutes, details.allDay, formats),
                )
                if (details.location.isNotBlank())
                    DetailValue(Icons.Default.Place, text(TextKey.LOCATION), details.location)
            }
            if (details.notes.isNotBlank())
                DetailSection(text(TextKey.NOTES), AppTheme.colors.background) {
                    Text(details.notes)
                }
            Text(
                text(
                    TextKey.SAVE_TO_CALENDAR_OPENS_YOUR_CALENDAR_APP_FOR_CONFIRMATION_ONLY_THIS_APPO
                ),
                style = MaterialTheme.typography.bodySmall,
            )
            if (future && allowFollowing)
                DetailSection(text(TextKey.APPLY_CHANGE_TO), AppTheme.colors.background) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            !following,
                            { following = false },
                            label = { Text(text(TextKey.ONLY_THIS_APPOINTMENT)) },
                        )
                        FilterChip(
                            following,
                            { following = true },
                            label = { Text(text(TextKey.THIS_AND_FOLLOWING_APPOINTMENTS)) },
                        )
                    }
                    Text(
                        text(TextKey.SELECT_THE_SCOPE_BEFORE_TAPPING_EDIT),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            if (!future)
                DetailNotice(
                    text(
                        TextKey
                            .THIS_APPOINTMENT_HAS_ALREADY_STARTED_EDITING_IS_NO_LONGER_POSSIBLE_DELET
                    )
                )
            if (!visible)
                DetailNotice(
                    text(
                        TextKey
                            .THIS_APPOINTMENT_HAS_CHANGED_BEEN_DELETED_OR_MOVED_PLEASE_CLOSE_AND_REOP
                    )
                )
            if (state.fromCache)
                DetailNotice(text(TextKey.SAVED_DATA_EDITING_OR_DELETING_REQUIRES_A_CONNECTION))
            saveError?.let { DetailNotice(it) }
        }
    }
    ui.deletion?.let { target ->
        ItemActionDialog(target, state, vm, onDismiss = { ui.deletion = null }, onEditTask = {})
    }
    ui.editor?.let { target ->
        var finishingImport by remember(target) { mutableStateOf(false) }
        AppointmentEditor(
            target,
            state,
            busy || finishingImport,
            saveError,
            onRetrySync = vm::retrySync,
            onDismiss = { if (!busy && !finishingImport) ui.editor = null },
        ) { draft ->
            vm.saveAppointment(
                target.original,
                draft,
                target.following,
                operationVersion = target.operationVersion,
            ) {
                val key = target.importKey
                if (key == null) ui.editor = null
                else {
                    finishingImport = true
                    onImported(key) {
                        finishingImport = false
                        ui.editor = null
                    }
                }
            }
        }
    }
}

internal fun repeatLabel(repeat: AppointmentRepeat): String =
    when (repeat) {
        AppointmentRepeat.ONCE -> text(TextKey.ONE_OFF)
        AppointmentRepeat.DAYS -> text(TextKey.DAYS_E94F59)
        AppointmentRepeat.WEEKS -> text(TextKey.WEEKS)
        AppointmentRepeat.MONTHS -> text(TextKey.MONTHS)
        AppointmentRepeat.YEARS -> text(TextKey.YEARS)
    }

private fun reminderLabel(minutes: Int?, allDay: Boolean, formats: DateTimeFormats): String =
    when {
        minutes == null -> text(TextKey.NONE)
        allDay && minutes == 0 -> text(TextKey.THE_SAME_DAY_AT, formats.time(LocalTime.of(9, 0)))
        allDay -> text(TextKey.THE_DAY_BEFORE_AT, formats.time(LocalTime.of(9, 0)))
        minutes == 0 -> text(TextKey.AT_START)
        minutes == 1440 -> text(TextKey.VALUE_1_DAY_BEFORE)
        else -> text(TextKey.MINUTES_BEFORE, minutes)
    }

@Composable
private fun AppointmentEditor(
    target: AppointmentEditTarget,
    state: FamilyState,
    busy: Boolean,
    saveError: String?,
    onRetrySync: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (AppointmentSeries) -> Unit,
) {
    val formats = rememberDateTimeFormats()
    val series = target.original?.series ?: target.imported?.series
    val seed =
        target.original?.details
            ?: target.imported?.series?.let { it.details.copy(date = it.activeFrom) }
    val singleException = target.original != null && !target.following
    val id = remember { UUID.randomUUID().toString() }
    var title by rememberSaveable { mutableStateOf(seed?.title.orEmpty()) }
    var recipient by rememberSaveable {
        mutableStateOf(
            seed?.recipient
                ?: state.children
                    .firstOrNull { !it.archived }
                    ?.let { AppointmentRecipient.children(setOf(it.id)) }
                ?: AppointmentRecipient.HOUSEHOLD
        )
    }
    var parent by rememberSaveable { mutableStateOf(seed?.responsibleParentId) }
    var date by rememberSaveable {
        mutableStateOf(seed?.date ?: target.initialDate ?: LocalDate.now(FAMILY_ZONE).plusDays(1))
    }
    val context = LocalContext.current
    val defaultDuration = remember { AppointmentDefaults.duration(context) }
    var timing by rememberSaveable {
        val initialStart = seed?.startTime ?: LocalTime.of(9, 0)
        mutableStateOf(
            AppointmentTimeDraft(
                initialStart,
                seed?.endTime ?: defaultAppointmentEnd(initialStart, defaultDuration),
            )
        )
    }
    val start = timing.start
    val end = timing.end
    var allDay by rememberSaveable { mutableStateOf(seed?.allDay ?: false) }
    var location by rememberSaveable { mutableStateOf(seed?.location.orEmpty()) }
    var notes by rememberSaveable { mutableStateOf(seed?.notes.orEmpty()) }
    var reminder by rememberSaveable {
        mutableStateOf(if (seed != null) seed.reminderMinutes else 30)
    }
    var repeat by rememberSaveable {
        mutableStateOf(
            if (singleException) AppointmentRepeat.ONCE
            else series?.repeat ?: AppointmentRepeat.ONCE
        )
    }
    var interval by rememberSaveable { mutableStateOf((series?.interval ?: 1).toString()) }
    var until by rememberSaveable { mutableStateOf(if (singleException) null else series?.until) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmDraft by remember { mutableStateOf<AppointmentSeries?>(null) }
    var importReviewed by rememberSaveable {
        mutableStateOf(target.imported?.warnings.isNullOrEmpty())
    }
    val duplicateImport =
        target.imported != null &&
            state.appointmentOccurrences(date, date).any {
                it.details.title == title.trim() &&
                    it.details.allDay == allDay &&
                    (allDay || (it.details.startTime == start && it.details.endTime == end))
            }
    var duplicateReviewed by remember(title, date, start, end, allDay) { mutableStateOf(false) }
    Dialog(
        onDismissRequest = onDismiss,
        properties =
            DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = !busy,
                dismissOnClickOutside = !busy,
            ),
    ) {
        AppBackground(Modifier.fillMaxSize()) {
            Column(Modifier.safeDrawingPadding().imePadding()) {
                TopAppBar(
                    title = {
                        Text(
                            if (target.imported != null) text(TextKey.IMPORT_APPOINTMENT)
                            else if (seed == null) text(TextKey.NEW_APPOINTMENT)
                            else text(TextKey.EDIT_APPOINTMENT)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss, enabled = !busy) {
                            Icon(Icons.Default.Close, text(TextKey.CLOSE))
                        }
                    },
                )
                LazyColumn(
                    contentPadding = PaddingValues(24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    if (target.imported != null) {
                        item {
                            Text(
                                text(
                                    TextKey
                                        .PLEASE_CHECK_THE_DATE_RECIPIENT_ASSIGNMENT_AND_RECURRENCE_ONLY_SAVE_ADDS
                                )
                            )
                        }
                        target.imported.warnings.forEach { warning ->
                            item { Text(warning, color = MaterialTheme.colorScheme.error) }
                        }
                        if (target.imported.warnings.isNotEmpty())
                            item {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        importReviewed,
                                        { importReviewed = it },
                                        enabled = !busy,
                                    )
                                    Text(
                                        text(TextKey.I_HAVE_CHECKED_THE_IMPORT_NOTES),
                                        Modifier.weight(1f),
                                    )
                                }
                            }
                        if (duplicateImport)
                            item {
                                Text(
                                    text(TextKey.A_SIMILAR_APPOINTMENT_ALREADY_EXISTS),
                                    color = MaterialTheme.colorScheme.error,
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        duplicateReviewed,
                                        { duplicateReviewed = it },
                                        enabled = !busy,
                                    )
                                    Text(
                                        text(TextKey.CREATE_ANOTHER_COPY_ANYWAY),
                                        Modifier.weight(1f),
                                    )
                                }
                            }
                    }
                    item {
                        DictationField(
                            title,
                            { title = it },
                            text(TextKey.TITLE),
                            Modifier.fillMaxWidth(),
                            enabled = !busy,
                        )
                    }
                    item { Text(text(TextKey.WHICH_AREA), fontWeight = FontWeight.Bold) }
                    item {
                        ChildRecipientPicker(state.children, recipient, enabled = !busy) {
                            recipient = it
                        }
                    }
                    item { AssignmentPicker(state.parents, parent) { parent = it } }
                    item {
                        DatePickerField(
                            text(TextKey.DATE),
                            date,
                            { it?.let { chosen -> date = chosen } },
                            enabled = !busy,
                            minDate =
                                if (allDay) LocalDate.now(FAMILY_ZONE).plusDays(1)
                                else LocalDate.now(FAMILY_ZONE),
                        )
                    }
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text(TextKey.ALL_DAY), Modifier.weight(1f))
                            Switch(
                                allDay,
                                {
                                    allDay = it
                                    if (reminder != null) reminder = if (it) 1440 else 30
                                },
                            )
                        }
                    }
                    if (!allDay) {
                        item {
                            TimePickerField(
                                text(TextKey.START),
                                start,
                                { timing = timing.selectStart(it, defaultDuration) },
                                enabled = !busy,
                            )
                        }
                        item {
                            TimePickerField(
                                text(TextKey.END_SAME_DAY),
                                end,
                                { timing = timing.selectEnd(it) },
                                enabled = !busy,
                            )
                            if (timing.durationClipped(defaultDuration))
                                Text(
                                    text(TextKey.APPOINTMENT_DURATION_CLIPPED),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                        }
                    }
                    item {
                        OutlinedTextField(
                            location,
                            { location = it },
                            label = { Text(text(TextKey.LOCATION_OPTIONAL)) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    item {
                        DictationField(
                            notes,
                            { notes = it },
                            text(TextKey.NOTES_OPTIONAL),
                            Modifier.fillMaxWidth(),
                            notes = true,
                            enabled = !busy,
                        )
                    }
                    if (!singleException) {
                        if (target.following)
                            item {
                                Text(
                                    text(
                                        TextKey
                                            .KEEPING_THE_DATE_AND_INTERVAL_PRESERVES_THE_ORIGINAL_RHYTHM
                                    )
                                )
                            }
                        item { Text(text(TextKey.RECURRENCE), fontWeight = FontWeight.Bold) }
                        item {
                            Row(
                                Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                AppointmentRepeat.entries.forEach { value ->
                                    FilterChip(
                                        value == repeat,
                                        { repeat = value },
                                        label = { Text(repeatLabel(value)) },
                                    )
                                }
                            }
                        }
                        if (repeat != AppointmentRepeat.ONCE) {
                            item {
                                OutlinedTextField(
                                    interval,
                                    { interval = it },
                                    label = {
                                        Text(
                                            text(TextKey.EVERY_HOW_MANY_11000, repeatLabel(repeat))
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                            item {
                                DatePickerField(
                                    text(TextKey.UNTIL_INCLUSIVE_OPTIONAL),
                                    until,
                                    { until = it },
                                    optional = true,
                                    enabled = !busy,
                                    minDate = date,
                                )
                            }
                        }
                    }
                    item { Text(text(TextKey.REMINDER), fontWeight = FontWeight.Bold) }
                    item {
                        Column {
                            val options =
                                if (allDay) listOf(null, 0, 1440)
                                else listOf(null, 0, 15, 30, 60, 1440)
                            options.forEach { value ->
                                FilterChip(
                                    value == reminder,
                                    { reminder = value },
                                    label = { Text(reminderLabel(value, allDay, formats)) },
                                )
                            }
                        }
                    }
                    error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
                    saveError?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
                    if (state.error != null)
                        item {
                            Text(state.error, color = MaterialTheme.colorScheme.error)
                            TextButton(onClick = onRetrySync, enabled = !busy) {
                                Text(text(TextKey.TRY_AGAIN))
                            }
                        }
                    else if (state.fromCache)
                        item { Text(text(TextKey.SAVING_REQUIRES_A_CONNECTION)) }
                    item {
                        Button(
                            enabled =
                                !busy &&
                                    !state.fromCache &&
                                    importReviewed &&
                                    (!duplicateImport || duplicateReviewed),
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                try {
                                    val details =
                                        AppointmentDetails(
                                            title.trim(),
                                            recipient,
                                            date,
                                            if (allDay) LocalTime.of(9, 0) else start,
                                            if (allDay) LocalTime.of(10, 0) else end,
                                            allDay,
                                            parent,
                                            location.trim(),
                                            notes.trim(),
                                            reminder,
                                        )
                                    requireLocalized(details.startInstant > Instant.now()) {
                                        message(TextKey.THE_APPOINTMENT_MUST_BE_IN_THE_FUTURE)
                                    }
                                    val count =
                                        if (repeat == AppointmentRepeat.ONCE) 1
                                        else interval.toInt()
                                    val preserveAnchor =
                                        (target.following || target.imported != null) &&
                                            series != null &&
                                            seed != null &&
                                            details.date == seed.date &&
                                            (target.imported != null ||
                                                series.contains(seed.date)) &&
                                            repeat != AppointmentRepeat.ONCE &&
                                            repeat == series.repeat &&
                                            count == series.interval
                                    val draft =
                                        AppointmentSeries(
                                            id,
                                            if (preserveAnchor)
                                                details.copy(
                                                    date = requireNotNull(series).details.date
                                                )
                                            else details,
                                            repeat,
                                            count,
                                            if (repeat == AppointmentRepeat.ONCE) null else until,
                                            fromDate = details.date,
                                        )
                                    draft.validate()
                                    error = null
                                    if (target.following) confirmDraft = draft else onSave(draft)
                                } catch (e: Exception) {
                                    error =
                                        if (e is NumberFormatException)
                                            text(
                                                TextKey.PLEASE_ENTER_THE_INTERVAL_AS_A_WHOLE_NUMBER
                                            )
                                        else e.message ?: text(TextKey.PLEASE_CHECK_YOUR_ENTRIES)
                                }
                            },
                        ) {
                            Text(if (busy) text(TextKey.SAVING) else text(TextKey.SAVE_APPOINTMENT))
                        }
                    }
                }
            }
        }
        confirmDraft?.let { draft ->
            AlertDialog(
                onDismissRequest = { confirmDraft = null },
                title = { Text(text(TextKey.REPLACE_FUTURE_APPOINTMENTS)) },
                text = {
                    Text(
                        text(
                            TextKey
                                .THIS_AND_ALL_FOLLOWING_APPOINTMENTS_WILL_BE_REPLACED_STARTED_APPOINTMENT
                        )
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            confirmDraft = null
                            onSave(draft)
                        }
                    ) {
                        Text(text(TextKey.REPLACE))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { confirmDraft = null }) { Text(text(TextKey.CANCEL)) }
                },
            )
        }
    }
}
