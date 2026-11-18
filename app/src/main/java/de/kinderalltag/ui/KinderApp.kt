@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package de.kinderalltag.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.kinderalltag.KinderApplication
import de.kinderalltag.data.*
import de.kinderalltag.domain.*
import de.kinderalltag.notifications.ReminderScheduler
import de.kinderalltag.notifications.ActivityNotifications
import java.time.*
import java.util.UUID

private enum class AppPage(val title: String) {
    HOME("Heute"), TASKS("Aufgaben"), APPOINTMENTS("Termine"), HISTORY("Verlauf"),
    STATISTICS("Statistik"), TRASH("Papierkorb"), SETTINGS("Einstellungen"),
}

private val green = GardenColors.Green

private val taskEncouragements = listOf(
    "Verwalte Aufgaben, die erledigt werden müssen, und schaffe Platz im Kopf für die schönen Dinge des Lebens: das erste Lächeln, das erste Stehen, die ersten Schritte …",
    "Behalte die kleinen Aufgaben im Blick und den Kopf frei für große Momente: das erste Wort, ein stolzes Grinsen und eine feste Umarmung.",
    "Hier finden eure Aufgaben ihren Platz. So bleibt mehr Raum für Bilderbuchabenteuer, Pfützensprünge und gemeinsames Lachen.",
    "Manches muss erledigt werden. Halte es hier fest und genieße die Momente dazwischen: Kuscheln am Morgen, Quatsch am Tisch und ein Gute-Nacht-Küsschen.",
    "Teilt euch die Aufgaben und sammelt gemeinsam Erinnerungen: an kleine Entdeckungen, mutige Schritte und all die schönen ersten Male.",
)

private val appointmentEncouragements = listOf(
    "Behalte eure Termine im Blick und den Kopf frei für die schönen Dinge des Lebens: kleine Entdeckungen, große Umarmungen und gemeinsame Abenteuer.",
    "Hier finden eure Termine ihren Platz. So bleibt mehr Raum für spontane Spielplatzbesuche, Pfützensprünge und ein Eis auf dem Heimweg.",
    "Vom Kinderarzt bis zum Familienausflug: Plant, was euch wichtig ist, und freut euch auf die kleinen Glücksmomente dazwischen.",
    "Ein guter Überblick macht den Alltag leichter. So könnt ihr beim Vorlesen, Kuscheln und gemeinsamen Lachen ganz bei euren Kindern sein.",
    "Haltet eure Verabredungen hier fest und sammelt unterwegs Erinnerungen: an neue Freunde, mutige erste Male und schöne Stunden zusammen.",
)



@Preview(name = "Heute", showBackground = true, widthDp = 390, heightDp = 820)
@Composable
private fun TagesgartenPreview() {
    KinderTheme {
        val date = LocalDate.of(2026, 11, 16)
        val children = listOf(Child("ferdinand", "Ferdinand"), Child("johann", "Johann"))
        val routines = listOf(
            TaskRevision("preview-1", "task-1", "ferdinand", "Rucksack packen", start = date, effective = date),
        )
        val occurrence = Schedule.occurrences(routines, date).first()
        val state = FamilyState(children = children, revisions = routines,
            appointments = listOf(AppointmentSeries("preview-appointment", AppointmentDetails("Kinderarzt",
                AppointmentRecipient.JOHANN, date, LocalTime.of(9, 0), LocalTime.of(9, 30)))),
            events = listOf(CareEvent("preview-event", occurrence.id, date, EventKind.COMPLETE, "preview", "Mama",
                date.atTime(8, 5).atZone(FAMILY_ZONE).toInstant().toEpochMilli())))
        GardenBackground(Modifier.fillMaxSize()) {
            Checklist(state, date, date, null, now = date.atTime(10, 0).atZone(FAMILY_ZONE).toInstant(), onSelectDate = {}) {}
        }
    }
}

@Composable
fun KinderTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = lightColorScheme(
        primary = GardenColors.Blue, onPrimary = Color.White,
        secondary = GardenColors.Coral, onSecondary = Color.White,
        tertiary = GardenColors.Green, onTertiary = Color.White,
        background = GardenColors.Cream, onBackground = GardenColors.Ink,
        surface = GardenColors.Cream, onSurface = GardenColors.Ink,
        surfaceContainer = Color(0xFFFFF0CF), surfaceVariant = GardenColors.Mint,
        onSurfaceVariant = Color(0xFF505B50),
        primaryContainer = GardenColors.Sky, onPrimaryContainer = Color(0xFF184966),
        secondaryContainer = GardenColors.Blush, onSecondaryContainer = GardenColors.Burgundy,
        tertiaryContainer = GardenColors.Mint, onTertiaryContainer = GardenColors.Green,
        outline = Color(0xFF7F8375), outlineVariant = Color(0xFFD6D4BD),
    ), shapes = Shapes(
        small = RoundedCornerShape(14.dp), medium = RoundedCornerShape(20.dp),
        large = RoundedCornerShape(26.dp), extraLarge = RoundedCornerShape(32.dp),
    ), content = content)
}

@Composable
fun KinderApp(vm: FamilyViewModel, initialDate: String?, initialOccurrence: String?,
    signingIn: Boolean, loginError: String?, onSignIn: () -> Unit, onSignOut: () -> Unit, initialPage: String? = null) = KinderTheme {
    val context = LocalContext.current
    val displayPreferences = remember(context) { context.getSharedPreferences("display", android.content.Context.MODE_PRIVATE) }
    var showIntroductions by remember { mutableStateOf(displayPreferences.getBoolean("show_introductions", false)) }
    var showCalendars by remember { mutableStateOf(displayPreferences.getBoolean("show_calendars", true)) }
    val inbox = remember { (context.applicationContext as KinderApplication).calendarInbox }
    val incoming by inbox.state.collectAsStateWithLifecycle()
    val state by vm.state.collectAsStateWithLifecycle()
    val today by vm.today.collectAsStateWithLifecycle()
    val now by vm.clock.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val taskSaveError by vm.taskError.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val snack = remember { SnackbarHostState() }
    LaunchedEffect(message) { message?.let { snack.showSnackbar(it); vm.message.value = null } }
    if (state.uid == null || !state.authorized) {
        GardenBackground(Modifier.fillMaxSize()) {
            Column(Modifier.safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Spacer(Modifier.height(12.dp))
                Text("Kinderalltag", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                GardenBanner("Zusammen wächst der Alltag.", "Für Ferdinand und Johann – und für euch beide.")
                Text("Kleine Aufgaben. Gemeinsam im Blick.", style = MaterialTheme.typography.titleLarge)
                if (incoming.current != null) Text("Ein Kalenderimport wartet. Bitte zuerst anmelden und den Familienzugang freischalten.")
                incoming.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (state.uid == null) {
                    Button(onClick = onSignIn, enabled = !signingIn) { Text(if (signingIn) "Anmelden …" else "Mit Google anmelden") }
                    loginError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                } else {
                    Text("Hallo ${state.name}. Dein Familienzugang muss noch freigeschaltet werden. Diese Nutzer-ID wird dafür benötigt:")
                    androidx.compose.foundation.text.selection.SelectionContainer { Text(state.uid.orEmpty()) }
                    Text("Die Einrichtung ist in docs/SETUP.md beschrieben. Nach der Freischaltung öffnet sich die Checkliste automatisch.")
                    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    TextButton(onClick = vm::retrySync) { Text("Erneut versuchen") }
                    TextButton(onClick = onSignOut) { Text("Abmelden") }
                }
            }
        }
        return@KinderTheme
    }
    var tab by rememberSaveable { mutableStateOf(if (initialOccurrence?.startsWith("appointment_") == true) AppPage.APPOINTMENTS else if (initialDate != null) AppPage.HISTORY else when (initialPage) { "tasks" -> AppPage.TASKS; "appointments" -> AppPage.APPOINTMENTS; else -> AppPage.HOME }) }
    val primaryPages = listOf(AppPage.HOME, AppPage.TASKS, AppPage.APPOINTMENTS)
    var lastPrimaryPage by rememberSaveable { mutableStateOf(if (tab in primaryPages) tab else AppPage.HOME) }
    var moreMenuExpanded by remember { mutableStateOf(false) }
    var statisticsDays by rememberSaveable { mutableIntStateOf(90) }
    var statisticsPage by rememberSaveable { mutableLongStateOf(0L) }
    var historyPage by rememberSaveable { mutableLongStateOf(0L) }
    var selectedChild by rememberSaveable { mutableStateOf<String?>(null) }
    var onlyMine by rememberSaveable { mutableStateOf(false) }
    var homeDayOffset by rememberSaveable { mutableLongStateOf(0L) }
    var taskMessageIndex by rememberSaveable { mutableIntStateOf(-1) }
    var appointmentMessageIndex by rememberSaveable { mutableIntStateOf(0) }
    var openedImportKey by rememberSaveable { mutableStateOf<String?>(null) }
    var taskCalendar by rememberSaveable(stateSaver = calendarOverviewSaver) { mutableStateOf(CalendarOverviewState(today)) }
    var appointmentCalendar by rememberSaveable(stateSaver = calendarOverviewSaver) { mutableStateOf(CalendarOverviewState(today)) }
    var taskPage by rememberSaveable { mutableLongStateOf(0L) }
    var agendaPage by rememberSaveable { mutableLongStateOf(0L) }
    val appointmentUi = remember { AppointmentUiState() }
    BackHandler(enabled = tab !in primaryPages) { tab = lastPrimaryPage }
    var editing by remember { mutableStateOf<TaskRevision?>(null) }
    var taskEditSelection by remember { mutableStateOf<TaskEditSelection?>(null) }
    var itemAction by remember { mutableStateOf<ItemActionTarget?>(null) }
    var editorOpen by remember { mutableStateOf(false) }
    LaunchedEffect(editorOpen) { if (editorOpen) vm.taskError.value = null }
    var selected by remember { mutableStateOf<Occurrence?>(null) }
    var openedInitial by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.revisions, state.appointments, state.appointmentExceptions, state.loaded, initialOccurrence) {
        if (!openedInitial && initialOccurrence != null) {
            val date = runCatching { LocalDate.parse(initialDate) }.getOrDefault(today)
            if (initialOccurrence.startsWith("appointment_")) {
                state.appointmentOccurrences(date, date).firstOrNull { it.id == initialOccurrence }?.let {
                    appointmentUi.open(it, state); openedInitial = true
                }
                if (!openedInitial && state.loaded) {
                    snack.showSnackbar("Dieser Termin wurde geändert oder abgesagt."); openedInitial = true
                }
            } else state.taskOccurrences(date).firstOrNull { it.id == initialOccurrence }?.let {
                selected = it; openedInitial = true
            }
        }
    }
    LaunchedEffect(incoming.key, editorOpen, appointmentUi.editor, appointmentUi.selected, selected) {
        val key = incoming.key
        val item = incoming.current
        if (key != null && item != null && key != openedImportKey && !editorOpen && appointmentUi.editor == null &&
            appointmentUi.selected == null && selected == null) {
            appointmentUi.editor = AppointmentEditTarget(null, imported = item, importKey = key)
            openedImportKey = key
        }
    }
    val homeButtonsWidth = (LocalConfiguration.current.screenWidthDp.dp - 32.dp).coerceAtMost(360.dp)
    val nav = primaryPages
    val icons = listOf(Icons.Default.WbSunny, Icons.Default.LocalFlorist, Icons.Default.Event)
    val navColors = listOf(GardenColors.Green, GardenColors.Blue, GardenColors.Burgundy)
    GardenBackground(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snack) },
            topBar = { TopAppBar(title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GardenDrawing(GardenMotif.SPROUT, Modifier.size(34.dp))
                    Text(if (tab in primaryPages) "Kinderalltag" else tab.title,
                        fontWeight = FontWeight.ExtraBold, color = green, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }, navigationIcon = {
                if (tab !in primaryPages) IconButton(onClick = { moreMenuExpanded = false; tab = lastPrimaryPage }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück")
                }
            }, actions = {
                IconButton(onClick = { moreMenuExpanded = false; tab = AppPage.SETTINGS }) { Icon(Icons.Default.Settings, "Einstellungen") }
                Box {
                    IconButton(onClick = { moreMenuExpanded = true }) { Icon(Icons.Default.MoreHoriz, "Weitere Seiten") }
                    DropdownMenu(expanded = moreMenuExpanded, onDismissRequest = { moreMenuExpanded = false }) {
                        DropdownMenuItem(text = { Text("Verlauf") }, leadingIcon = {
                            Icon(Icons.Default.History, null, tint = GardenColors.Coral)
                        }, onClick = { moreMenuExpanded = false; tab = AppPage.HISTORY })
                        DropdownMenuItem(text = { Text("Statistik") }, leadingIcon = {
                            Icon(Icons.Default.BarChart, null, tint = GardenColors.Green)
                        }, onClick = { moreMenuExpanded = false; tab = AppPage.STATISTICS })
                        DropdownMenuItem(text = { Text("Papierkorb") }, leadingIcon = {
                            Icon(Icons.Default.DeleteOutline, null, tint = GardenColors.Coral)
                        }, onClick = { moreMenuExpanded = false; tab = AppPage.TRASH })
                    }
                }
            }) },
            bottomBar = {
                NavigationBar(containerColor = GardenColors.Cream, tonalElevation = 0.dp) { nav.forEachIndexed { index, destination ->
                    NavigationBarItem(selected = tab == destination, onClick = {
                        if (destination == AppPage.TASKS && tab != AppPage.TASKS) taskMessageIndex = (taskMessageIndex + 1) % taskEncouragements.size
                        if (destination == AppPage.APPOINTMENTS && tab != AppPage.APPOINTMENTS) appointmentMessageIndex = (appointmentMessageIndex + 1) % appointmentEncouragements.size
                        moreMenuExpanded = false
                        lastPrimaryPage = destination
                        tab = destination
                        if (destination == AppPage.HOME) homeDayOffset = 0L
                    },
                        icon = { Icon(icons[index], null) }, label = { Text(destination.title) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = navColors[index], selectedTextColor = navColors[index],
                            indicatorColor = navColors[index].copy(alpha = 0.12f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant))
                } }
            },
            floatingActionButton = {
                when (tab) {
                    AppPage.HOME -> Row(Modifier.width(homeButtonsWidth), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        YellowAddButton("Aufgabe", Modifier.weight(1f)) { editing = null; taskEditSelection = null; editorOpen = true }
                        YellowAddButton("Termin", Modifier.weight(1f)) { appointmentUi.editor = AppointmentEditTarget(null) }
                    }
                    AppPage.TASKS -> YellowAddButton("Aufgabe") { editing = null; taskEditSelection = null; editorOpen = true }
                    AppPage.APPOINTMENTS -> YellowAddButton("Termin") { appointmentUi.editor = AppointmentEditTarget(null) }
                    else -> Unit
                }
            },
        ) { padding ->
            Column(Modifier.padding(padding).fillMaxSize()) {
                if (state.fromCache || state.pending || state.error != null) {
                    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer).padding(12.dp)) {
                        Text(state.error ?: if (state.pending) "Änderungen werden synchronisiert …" else "Gespeicherter Stand · Verbindung wird hergestellt",
                            style = MaterialTheme.typography.bodySmall)
                        if (state.error != null) TextButton(onClick = vm::retrySync, enabled = !busy) { Text("Erneut versuchen") }
                    }
                }
                incoming.error?.let { error ->
                    Column(Modifier.padding(horizontal = 20.dp)) {
                        Text(error.lineSequence().take(3).joinToString("\n"), color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = inbox::dismissError) { Text("Hinweis schließen") }
                    }
                }
                if (incoming.current != null) Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${incoming.remaining} ${if (incoming.remaining == 1) "Termin" else "Termine"} zum Übernehmen", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = {
                        incoming.current?.let { appointmentUi.editor = AppointmentEditTarget(null, imported = it, importKey = incoming.key) }
                        openedImportKey = incoming.key
                    }) { Text("Prüfen") }
                    TextButton(onClick = { incoming.key?.let { inbox.consume(it) } }) { Text("Überspringen") }
                }
                if (tab != AppPage.SETTINGS && tab != AppPage.STATISTICS && tab != AppPage.TRASH) Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(onlyMine, { onlyMine = !onlyMine; taskPage = 0 }, label = { Text("Nur meine") })
                    VerticalDivider(Modifier.height(24.dp))
                    FilterChip(selectedChild == null, { selectedChild = null; taskPage = 0 }, label = { Text("Alle") })
                    state.sections.forEach { child -> GardenChildChip(child, selectedChild == child.id) { selectedChild = child.id; taskPage = 0 } }
                }
                when (tab) {
                    AppPage.HOME -> {
                        val date = today.plusDays(homeDayOffset)
                        Checklist(state, date, today, selectedChild, onlyMine,
                            now = now, onSelectDate = { chosen -> homeDayOffset = chosen.toEpochDay() - today.toEpochDay() },
                            onOpenAppointment = { appointmentUi.open(it, state) }, onOpen = { selected = it })
                    }
                    AppPage.HISTORY -> HistoryFeed(state, today, historyPage, selectedChild, onlyMine,
                        onPageChange = { historyPage = it })
                    AppPage.TASKS -> TaskList(state, today, busy, selectedChild, onlyMine, taskPage, onPageChange = { taskPage = it },
                        encouragement = taskEncouragements[taskMessageIndex.coerceAtLeast(0)], showIntroduction = showIntroductions, showCalendar = showCalendars,
                        calendar = taskCalendar, onCalendarChange = { taskCalendar = it; taskPage = 0 }, onEdit = { itemAction = ItemActionTarget(false, definition = it, expectedVersion = state.version(ItemType.TASK, it.taskId)) },
                        onDelete = { task, date ->
                            val occurrence = date?.let { day -> state.taskOccurrences(day).firstOrNull { it.revision.taskId == task.taskId } }
                            itemAction = ItemActionTarget(true, definition = if (occurrence == null) task else null, task = occurrence,
                                expectedVersion = state.version(ItemType.TASK, task.taskId))
                        }, onArchive = vm::archive)
                    AppPage.APPOINTMENTS -> AppointmentAgenda(state, today, now, agendaPage, selectedChild, onlyMine,
                        encouragement = appointmentEncouragements[appointmentMessageIndex], showIntroduction = showIntroductions, showCalendar = showCalendars,
                        calendar = appointmentCalendar, onCalendarChange = { appointmentCalendar = it; agendaPage = 0 },
                        onPageChange = { agendaPage = it }, onOpen = { appointmentUi.open(it, state) })
                    AppPage.STATISTICS -> StatisticsScreen(state, today, statisticsDays, statisticsPage,
                        onDaysChange = { statisticsDays = it; statisticsPage = 0 }, onPageChange = { statisticsPage = it })
                    AppPage.TRASH -> TrashScreen(state, vm)
                    AppPage.SETTINGS -> Settings(state, busy, onSignOut, vm,
                        showIntroductions = showIntroductions, onShowIntroductionsChange = {
                            showIntroductions = it
                            displayPreferences.edit().putBoolean("show_introductions", it).apply()
                        }, showCalendars = showCalendars, onShowCalendarsChange = {
                            showCalendars = it
                            displayPreferences.edit().putBoolean("show_calendars", it).apply()
                            taskCalendar = taskCalendar.reset(today)
                            appointmentCalendar = appointmentCalendar.reset(today)
                            taskPage = 0
                            agendaPage = 0
                        })
                }
            }
        }
    }
    if (editorOpen) TaskEditor(editing, state.sections, state.parents, today, busy, state.fromCache,
        if (taskEditSelection == null) taskSaveError else vm.operationError.collectAsStateWithLifecycle().value,
        selection = taskEditSelection,
        syncError = state.error, onRetrySync = vm::retrySync,
        onDismiss = { if (!busy) editorOpen = false },
        onSave = { draft ->
            val selection = taskEditSelection
            if (selection == null) vm.save(draft) { editorOpen = false }
            else vm.saveTaskEdit(selection, draft) { editorOpen = false; taskEditSelection = null }
        })
    itemAction?.let { target -> ItemActionDialog(target, state, vm, onDismiss = { itemAction = null }, onEditTask = { selection ->
        itemAction = null; taskEditSelection = selection
        editing = if (selection.scope == OperationScope.FOLLOWING) state.taskDefinitions(Instant.now(), true)
            .firstOrNull { it.taskId == selection.occurrence.revision.taskId } ?: selection.occurrence.revision else selection.occurrence.revision
        editorOpen = true
    }) }
    AppointmentDialogs(appointmentUi, state, vm, onImported = { key, done -> inbox.consume(key, done) })
    selected?.let { occurrence -> OccurrenceDialog(occurrence, state, today,
        onDismiss = { selected = null }, onComplete = { parentId -> vm.complete(occurrence, parentId) }, onUndo = { vm.undo(occurrence) },
        onEdit = { selected = null; itemAction = ItemActionTarget(false, task = occurrence, expectedVersion = state.version(ItemType.TASK, occurrence.revision.taskId)) },
        onDelete = { selected = null; itemAction = ItemActionTarget(true, task = occurrence, expectedVersion = state.version(ItemType.TASK, occurrence.revision.taskId)) }) }
}

@Composable
private fun Checklist(state: FamilyState, date: LocalDate, today: LocalDate, childId: String?,
    onlyMine: Boolean = false, showTasks: Boolean = true, showAppointments: Boolean = true,
    now: Instant = Instant.now(), onSelectDate: ((LocalDate) -> Unit)? = null, onOpenAppointment: (AppointmentOccurrence) -> Unit = {}, onOpen: (Occurrence) -> Unit) {
    val formats = rememberDateTimeFormats()
    val due = state.taskOccurrences(date).filter {
        showTasks && (childId == null || it.revision.childId == childId) && (!onlyMine || isResponsible(it.revision.responsibleParentId, state.uid))
    }
    val appointments = if (showAppointments) visibleAppointments(state, date, date, childId, onlyMine) else emptyList()
    val done = due.count { Schedule.activeCompletions(state.events, it.id).isNotEmpty() }
    val appointmentsFinished = appointments.count { it.details.endInstant <= now }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (onSelectDate == null) item {
            Text("Verlauf", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, GardenColors.Sage),
                colors = CardDefaults.cardColors(containerColor = GardenColors.Mint, contentColor = GardenColors.Ink)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (onSelectDate != null) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledIconButton(onClick = { onSelectDate(date.minusDays(1)) },
                                modifier = Modifier.size(48.dp), shape = RoundedCornerShape(16.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = GardenColors.Green.copy(alpha = 0.12f), contentColor = GardenColors.Green)) {
                                Icon(Icons.Default.ChevronLeft, "Vorheriger Tag", modifier = Modifier.size(28.dp))
                            }
                            Text(formats.fullDate(date), modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            FilledIconButton(onClick = { onSelectDate(date.plusDays(1)) },
                                modifier = Modifier.size(48.dp), shape = RoundedCornerShape(16.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = GardenColors.Green.copy(alpha = 0.12f), contentColor = GardenColors.Green)) {
                                Icon(Icons.Default.ChevronRight, "Nächster Tag", modifier = Modifier.size(28.dp))
                            }
                        }
                    } else {
                        Text(formats.fullDate(date), style = MaterialTheme.typography.bodyMedium)
                    }
                    if (showTasks) {
                        Column(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
                            verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("$done von ${due.size} Aufgaben erledigt", color = green,
                                style = MaterialTheme.typography.bodyMedium)
                            DayProgressBar(if (due.isEmpty()) 1f else done.toFloat() / due.size, green)
                        }
                    }
                    if (showAppointments) {
                        Column(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
                            verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("$appointmentsFinished von ${appointments.size} Termine absolviert", color = GardenColors.Blue,
                                style = MaterialTheme.typography.bodyMedium)
                            DayProgressBar(if (appointments.isEmpty()) 1f else appointmentsFinished.toFloat() / appointments.size,
                                GardenColors.Blue)
                        }
                    }
                    if (onSelectDate != null && date != today) {
                        TextButton(onClick = { onSelectDate(today) }, modifier = Modifier.align(Alignment.End)) {
                            Text("Zurück zu heute")
                        }
                    }
                }
            }
        }
        if (showTasks) {
            item { Text("Aufgaben", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            if (due.isEmpty()) item { EmptyCard("Für diesen Tag stehen keine Aufgaben an.") }
        }
        state.sections.filter { childId == null || childId == it.id }.forEach { child ->
            val tasks = due.filter { it.revision.childId == child.id }
            if (tasks.isNotEmpty()) {
                item {
                    Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ChildBadge(child)
                        Text(child.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                            color = childColor(child))
                    }
                }
                items(tasks, key = { it.id }) { occurrence ->
                    val completed = Schedule.activeCompletions(state.events, occurrence.id)
                    Card(onClick = { onOpen(occurrence) }, shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, if (completed.isEmpty()) childColor(child).copy(alpha = 0.25f) else GardenColors.Sage),
                        colors = CardDefaults.cardColors(
                            containerColor = if (completed.isEmpty()) childPaper(child) else GardenColors.Mint,
                            contentColor = GardenColors.Ink)) {
                        Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Surface(shape = RoundedCornerShape(14.dp), color = Color.White.copy(alpha = 0.7f)) {
                                Icon(if (completed.isEmpty()) Icons.Default.Schedule else Icons.Default.CheckCircle,
                                    if (completed.isEmpty()) "Nicht bestätigt" else "Bestätigt",
                                    tint = if (completed.isEmpty()) childColor(child) else green,
                                    modifier = Modifier.padding(10.dp).size(24.dp))
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(occurrence.revision.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(formats.time(occurrence.time), style = MaterialTheme.typography.bodySmall)
                                Text("Zuständig: ${responsibilityName(occurrence.revision.responsibleParentId, state.parents)}", style = MaterialTheme.typography.bodySmall)
                                Text(if (completed.isEmpty()) {
                                    if (date < today) "Nicht bestätigt" else "Noch offen"
                                } else completed.joinToString(" · ") { completionText(it, formats) }, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
        if (showAppointments) {
            item { Text("Termine", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            if (appointments.isEmpty()) item { EmptyCard("Für diesen Tag stehen keine Termine an.") }
            items(appointments, key = { it.id }) { AppointmentCard(it, state) { onOpenAppointment(it) } }
        }
        item { EndOfPageFooter(GardenMotif.WORM) }
    }
}

@Composable
private fun DayProgressBar(progress: Float, color: Color) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Default.WbSunny, contentDescription = null,
            tint = GardenColors.Mustard, modifier = Modifier.size(20.dp))
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.weight(1f).height(8.dp),
            color = color, trackColor = Color.White.copy(alpha = 0.7f))
        Icon(Icons.Default.DarkMode, contentDescription = null,
            tint = GardenColors.Mustard, modifier = Modifier.size(20.dp))
    }
}

private fun completionText(event: CareEvent, formats: DateTimeFormats): String =
    "${event.completedByName ?: event.authorName}${if (event.completedById != null && event.completedById != event.authorId) " · eingetragen von ${event.authorName}" else ""} · ${formats.timestamp(Instant.ofEpochMilli(event.clientMillis))}${if (event.pending) " · ausstehend" else ""}"

@Composable
private fun EmptyCard(text: String) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF0CF))) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GardenDrawing(GardenMotif.SPROUT, Modifier.size(48.dp))
            Text(text, Modifier.weight(1f))
        }
    }
}

@Composable
private fun GardenChildChip(child: Child, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected, onClick, label = { Text(child.name, fontWeight = FontWeight.SemiBold) },
        leadingIcon = { ChildIcon(child, Modifier.size(26.dp)) },
        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = childPaper(child),
            selectedLabelColor = childColor(child)), shape = RoundedCornerShape(16.dp))
}

@Composable
private fun TaskList(state: FamilyState, today: LocalDate, busy: Boolean, sectionId: String?, onlyMine: Boolean, page: Long, onPageChange: (Long) -> Unit, encouragement: String, showIntroduction: Boolean, showCalendar: Boolean,
    calendar: CalendarOverviewState, onCalendarChange: (CalendarOverviewState) -> Unit, onEdit: (TaskRevision) -> Unit, onDelete: (TaskRevision, LocalDate?) -> Unit, onArchive: (TaskRevision) -> Unit) {
    val formats = rememberDateTimeFormats()
    val selectedPeriod = calendar.period.takeIf { showCalendar && calendar.filterActive }
    val calendarDates = calendar.visibleDates
    val calendarPreviews = if (showCalendar) taskCalendarPreviews(state.revisions, calendarDates, sectionId, state.uid, onlyMine, state.events, state.itemOperations) else emptyMap()
    val editableVersions = state.taskDefinitions(Instant.now(), includePaused = true).associateBy { it.taskId }
    val scheduledTasks = if (selectedPeriod == null) state.taskDefinitions(Instant.now())
        else tasksInCalendarPeriod(state.revisions, selectedPeriod, sectionId, state.uid, onlyMine, state.events, state.itemOperations)
    val tasks = scheduledTasks.filter {
        (sectionId == null || it.childId == sectionId) && (!onlyMine || isResponsible(it.responsibleParentId, state.uid))
    }.sortedWith(compareBy<TaskRevision> { it.title.lowercase(java.util.Locale.ROOT) }.thenBy { it.taskId })
    var showPaused by rememberSaveable { mutableStateOf(false) }
    val paused = state.taskDefinitions(Instant.now(), includePaused = true).filter { task ->
        !task.active && (sectionId == null || task.childId == sectionId) &&
            (!onlyMine || isResponsible(task.responsibleParentId, state.uid))
    }.sortedWith(compareBy<TaskRevision> { it.title.lowercase(java.util.Locale.ROOT) }.thenBy { it.taskId })
    val taskPage = taskPage(tasks + if (showPaused && selectedPeriod == null) paused else emptyList(), page)
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    LaunchedEffect(taskPage.index) { listState.scrollToItem(0) }
    LaunchedEffect(taskPage.index, page) { if (taskPage.index != page) onPageChange(taskPage.index) }
    var archive by remember { mutableStateOf<TaskRevision?>(null) }
    var details by remember { mutableStateOf<TaskOverviewSelection?>(null) }
    LazyColumn(state = listState, contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 100.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (showIntroduction) item { GardenBanner("Euer Alltagshelfer", encouragement) }
        if (showCalendar) item {
            CalendarOverviewCard("Aufgaben", calendar, today, calendarPreviews, "Alle Aufgaben anzeigen", onCalendarChange, GardenColors.Green)
        }
        if (tasks.isEmpty()) item { EmptyCard(if (selectedPeriod == null) "Keine anstehenden Aufgaben. Neue Aufgaben könnt ihr mit + Aufgabe anlegen."
            else "Keine Aufgaben im ausgewählten Zeitraum.") }
        if (paused.isNotEmpty() && selectedPeriod == null) item {
            TextButton(onClick = { onPageChange(0); showPaused = !showPaused }) {
                Text(if (showPaused) "Pausierte Aufgaben ausblenden" else "Pausierte Aufgaben anzeigen (${paused.size})")
            }
        }
        items(taskPage.tasks, key = { it.taskId }) { task ->
            val editableTask = editableVersions[task.taskId] ?: task
            val historical = selectedPeriod != null && (selectedPeriod.to < today || task.end?.let { it < today } == true ||
                (task.frequency == Frequency.ONCE && task.start < today))
            val child = state.sections.firstOrNull { it.id == task.childId } ?: Child(task.childId, "Bereich")
            Card(onClick = { details = TaskOverviewSelection(task, editableTask, selectedPeriod?.from, historical,
                state.version(ItemType.TASK, task.taskId)) }, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(
                    containerColor = childPaper(child), contentColor = GardenColors.Ink)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ChildIcon(child, Modifier.size(28.dp))
                        Text(child.name, style = MaterialTheme.typography.labelLarge)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(task.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ChevronRight, null, tint = GardenColors.Blue)
                    }
                    Text(task.times.joinToString(" · ", transform = formats::time) + " · " + frequencyLabel(task.frequency))
                    Text("Zuständig: ${responsibilityName(task.responsibleParentId, state.parents)}", style = MaterialTheme.typography.bodySmall)
                    if (historical) Text("Vergangene Ausführung", style = MaterialTheme.typography.bodySmall)
                    else if (!editableTask.active) Text("Pausiert", color = GardenColors.Coral, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            PageNavigationCard("Aufgaben", "${taskPage.first}–${taskPage.last} von ${taskPage.total}",
                canPrevious = taskPage.index > 0, canNext = taskPage.hasNext,
                onPrevious = { onPageChange(taskPage.index - 1) }, onNext = { onPageChange(taskPage.index + 1) },
                previousDescription = "Vorherige 90 Aufgaben", nextDescription = "Nächste 90 Aufgaben",
                resetText = if (taskPage.index > 0) "Zu den ersten Aufgaben" else null, onReset = { onPageChange(0) })
        }
        item { EndOfPageFooter(GardenMotif.HEART) }
    }
    details?.let { selection ->
        TaskDefinitionDialog(selection, state, busy, onDismiss = { details = null },
            onEdit = { details = null; onEdit(selection.editable) },
            onPause = { details = null; archive = selection.editable },
            onDelete = { details = null; onDelete(selection.task, selection.date) })
    }
    archive?.let { task -> AlertDialog(onDismissRequest = { archive = null }, title = { Text("Kommende Ausführungen pausieren?") },
        text = { Text("„${task.title}“ bleibt im Verlauf. Ihr könnt die Aufgabe später fortsetzen.") },
        confirmButton = { TextButton(onClick = { onArchive(task); archive = null }) { Text("Pausieren") } },
        dismissButton = { TextButton(onClick = { archive = null }) { Text("Abbrechen") } }) }
}

private fun frequencyLabel(frequency: Frequency) = when (frequency) {
    Frequency.ONCE -> "Einmalig"
    Frequency.DAILY -> "Täglich"
    Frequency.WEEKDAYS -> "An Wochentagen"
}

@Composable
private fun OccurrenceDialog(occurrence: Occurrence, state: FamilyState, today: LocalDate,
    onDismiss: () -> Unit, onComplete: (String) -> Unit, onUndo: () -> Unit,
    onEdit: () -> Unit, onDelete: () -> Unit) {
    val formats = rememberDateTimeFormats()
    val completed = Schedule.activeCompletions(state.events, occurrence.id)
    val visible = state.taskOccurrences(occurrence.date).any {
        it.id == occurrence.id && it.revision.id == occurrence.revision.id && it.time == occurrence.time
    }
    val events = state.events.filter { it.occurrenceId == occurrence.id }.sortedBy { it.serverMillis ?: it.clientMillis }
    var confirmingUndo by remember { mutableStateOf(false) }
    var completedById by rememberSaveable(occurrence.id) { mutableStateOf(state.uid) }
    ItemDetailsDialog(occurrence.revision.title, "Aufgabe · Ausführung", onDismiss, actions = {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (occurrence.instant > Instant.now() && completed.isEmpty())
                DetailAction("Bearbeiten", Icons.Default.Edit, visible && !state.fromCache && state.error == null, onClick = onEdit)
            DetailAction("Löschen", Icons.Default.DeleteOutline, visible && !state.fromCache && state.error == null,
                destructive = true, onClick = onDelete)
        }
        if (completed.isEmpty()) Button(onClick = { completedById?.let(onComplete) }, modifier = Modifier.fillMaxWidth(),
            enabled = visible && state.error == null && state.parents.any { it.id == completedById }) {
            Icon(Icons.Default.Check, null); Spacer(Modifier.width(8.dp)); Text("Als erledigt bestätigen")
        } else OutlinedButton(enabled = visible && state.error == null, modifier = Modifier.fillMaxWidth(), onClick = {
            if (confirmingUndo) { onUndo(); confirmingUndo = false } else confirmingUndo = true
        }) { Text(if (confirmingUndo) "Ja, Bestätigungen zurücknehmen" else "Bestätigung korrigieren") }
    }) {
        DetailSection("Auf einen Blick", GardenColors.Mint) {
            DetailValue(Icons.Default.Face, "Bereich", state.sections.firstOrNull { it.id == occurrence.revision.childId }?.name ?: "Bereich")
            DetailValue(Icons.Default.Event, "Datum", formats.fullDate(occurrence.date))
            DetailValue(Icons.Default.Schedule, "Uhrzeit", formats.time(occurrence.time))
            DetailValue(Icons.Default.Person, "Zuständig", responsibilityName(occurrence.revision.responsibleParentId, state.parents))
            DetailValue(if (completed.isEmpty()) Icons.Default.RadioButtonUnchecked else Icons.Default.CheckCircle, "Status",
                if (completed.isEmpty()) "Noch nicht bestätigt" else "Erledigt")
        }
        if (occurrence.revision.notes.isNotBlank()) DetailSection("Notizen", GardenColors.Cream) { Text(occurrence.revision.notes) }
        if (!visible) DetailNotice("Diese Ausführung wurde geändert, gelöscht oder verschoben. Bitte schließen und die Übersicht neu öffnen.")
        if (state.fromCache) DetailNotice("Gespeicherter Stand: Bestätigungen vom anderen Telefon sind möglicherweise noch nicht sichtbar.")
        if (completed.isEmpty()) {
            if (occurrence.date < today) Text("Eine nachträgliche Bestätigung zeichnet den jetzigen Zeitpunkt auf.", style = MaterialTheme.typography.bodySmall)
            DetailSection("Erledigt von", GardenColors.Cream) {
                Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    state.parents.forEach { parent ->
                        Row(Modifier.fillMaxWidth().selectable(selected = completedById == parent.id,
                            onClick = { completedById = parent.id }, role = Role.RadioButton).padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = completedById == parent.id, onClick = null)
                            Spacer(Modifier.width(8.dp))
                            Text(parent.name + if (parent.id == state.uid) " (ich)" else "")
                        }
                    }
                }
            }
        }
        if (events.isNotEmpty()) DetailSection("Bestätigungen & Korrekturen", GardenColors.Cream) {
            events.forEachIndexed { index, event ->
                if (index > 0) HorizontalDivider()
                Text(if (event.kind == EventKind.COMPLETE) "Bestätigt" else "Bestätigung zurückgenommen", fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium)
                Text(completionText(event, formats), style = MaterialTheme.typography.bodySmall)
                if (event.serverMillis != null && event.serverMillis - event.clientMillis > 60_000)
                    Text("Synchronisiert: ${formats.timestamp(Instant.ofEpochMilli(event.serverMillis))}", style = MaterialTheme.typography.bodySmall)
            }
        }
        if (confirmingUndo) DetailNotice("Alle aktuellen Bestätigungen für diese Ausführung zurücknehmen? Die Korrektur bleibt im Verlauf sichtbar.")
    }
}

@Composable
private fun Settings(state: FamilyState, busy: Boolean, onSignOut: () -> Unit, vm: FamilyViewModel,
    showIntroductions: Boolean, onShowIntroductionsChange: (Boolean) -> Unit,
    showCalendars: Boolean, onShowCalendarsChange: (Boolean) -> Unit) {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(ReminderScheduler.enabled(context)) }
    var newTasks by remember { mutableStateOf(ActivityNotifications.enabled(context, state.uid, FamilyNoticeKind.NEW_TASK)) }
    var newAppointments by remember { mutableStateOf(ActivityNotifications.enabled(context, state.uid, FamilyNoticeKind.NEW_APPOINTMENT)) }
    var pendingPermission by rememberSaveable { mutableStateOf<String?>(null) }
    var showImpressum by rememberSaveable { mutableStateOf(false) }
    var rename by remember { mutableStateOf<Child?>(null) }
    var name by remember { mutableStateOf("") }
    var bearColor by remember { mutableStateOf(BearColor.BLUE) }
    fun setNotificationChoice(choice: String, value: Boolean) {
        when (choice) {
            "reminders" -> {
                enabled = value
                ReminderScheduler.setEnabled(context, value)
                if (value) ReminderScheduler.update(context, vm.state.value)
            }
            "tasks" -> {
                newTasks = value
                ActivityNotifications.setEnabled(context, state.uid, FamilyNoticeKind.NEW_TASK, value)
            }
            "appointments" -> {
                newAppointments = value
                ActivityNotifications.setEnabled(context, state.uid, FamilyNoticeKind.NEW_APPOINTMENT, value)
            }
        }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        pendingPermission?.let { setNotificationChoice(it, granted) }
        pendingPermission = null
    }
    fun changeNotificationChoice(choice: String, value: Boolean) {
        if (value && Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            pendingPermission = choice
            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else setNotificationChoice(choice, value)
    }
    val version = remember(context) {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "1.0.0"
    }
    LazyColumn(contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { GardenBanner("Euer Familiengarten", "Alles, was euren Kinderalltag persönlich macht.") }
        item {
            SettingsSection("Dein Konto", Icons.Default.AccountCircle, GardenColors.Green) {
                Text(state.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(state.error ?: if (state.pending) "Änderungen werden synchronisiert …" else if (state.fromCache) "Gespeicherter Stand" else "Alles auf dem aktuellen Stand.",
                    style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = vm::retrySync, enabled = !busy) {
                    Icon(Icons.Default.Sync, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Jetzt synchronisieren")
                }
            }
        }
        item {
            SettingsSection("Darstellung", Icons.Default.Palette, GardenColors.Burgundy) {
                SettingsToggle("Alltagsgedanken anzeigen", "Zeigt die wechselnden Texte in „Euer Alltagshelfer“ und „Eure Termine“.",
                    showIntroductions, onShowIntroductionsChange)
                HorizontalDivider()
                SettingsToggle("Kalenderansicht anzeigen", "Zeigt den Kalender in Aufgaben und Termine.",
                    showCalendars, onShowCalendarsChange)
            }
        }
        item {
            SettingsSection("Benachrichtigungen", Icons.Default.Notifications, GardenColors.Blue) {
                SettingsToggle("Erinnerungen", "An deine fälligen Aufgaben und Termine – auch bei Zuständigkeit für beide Eltern.", enabled) {
                    changeNotificationChoice("reminders", it)
                }
                HorizontalDivider()
                SettingsToggle("Neue Aufgaben", "Wenn der andere Elternteil eine Aufgabe anlegt.", newTasks) {
                    changeNotificationChoice("tasks", it)
                }
                SettingsToggle("Neue Termine", "Wenn der andere Elternteil einen Termin anlegt.", newAppointments) {
                    changeNotificationChoice("appointments", it)
                }
                Text("Neuigkeiten werden empfangen, solange die App läuft und verbunden ist. Bei geschlossener App gibt es dafür noch keine Push-Mitteilungen.",
                    style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = {
                    context.startActivity(android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName))
                }) { Text("Android-Benachrichtigungen verwalten") }
                Text("Diese Einstellungen gelten für dieses Telefon. Android kann die Zustellung verzögern.", style = MaterialTheme.typography.bodySmall)
            }
        }
        item {
            SettingsSection("Eure Kinder", Icons.Default.ChildCare, GardenColors.Coral) {
                Text("Ein eigener Bär und eine Lieblingsfarbe für jedes Kind.", style = MaterialTheme.typography.bodySmall)
                state.children.forEach { child ->
                    OutlinedButton(onClick = { rename = child; name = child.name; bearColor = child.bearColor },
                        enabled = !busy && !state.fromCache, shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = childColor(child)),
                        modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(12.dp)) {
                        ChildIcon(child, Modifier.padding(end = 12.dp).size(42.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(child.name, fontWeight = FontWeight.Bold)
                            Text("Name & Bärenfarbe", style = MaterialTheme.typography.bodySmall)
                        }
                        Icon(Icons.Default.Edit, null, Modifier.size(20.dp))
                    }
                }
            }
        }
        item {
            SettingsSection("Euer Tagesrhythmus", Icons.Default.WbSunny, GardenColors.Burgundy) {
                Text("Familienzeitzone: Europe/Berlin", style = MaterialTheme.typography.titleSmall)
                Text("Tageswechsel und Erinnerungen richten sich auch auf Reisen nach eurer Familienzeit.", style = MaterialTheme.typography.bodySmall)
            }
        }
        item {
            SettingsSection("Über Kinderalltag", Icons.Default.Favorite, GardenColors.Coral) {
                Text("Ein gemeinsamer Blick auf die kleinen und großen Aufgaben eurer Familie.", style = MaterialTheme.typography.bodyMedium)
                Text("Version $version", style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = { showImpressum = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Info, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Impressum")
                }
            }
        }
        item {
            OutlinedButton(onClick = onSignOut, enabled = !state.pending && !busy, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.AutoMirrored.Filled.Logout, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Abmelden")
            }
        }
        if (state.pending) item { Text("Vor dem Abmelden bitte ausstehende Änderungen synchronisieren.", style = MaterialTheme.typography.bodySmall) }
        item { EndOfPageFooter(GardenMotif.HEART) }
    }
    if (showImpressum) ImpressumDialog { showImpressum = false }
    rename?.let { child -> AlertDialog(onDismissRequest = { if (!busy) rename = null }, title = { Text("Kinderprofil") },
        text = {
            Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true, enabled = !busy)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ChildIcon(child.copy(bearColor = bearColor), Modifier.size(72.dp))
                    Text("Bärenfarbe", style = MaterialTheme.typography.titleMedium)
                }
                Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    BearColor.entries.chunked(3).forEach { choices ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            choices.forEach { choice ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(Modifier.size(48.dp)
                                        .selectable(choice == bearColor, enabled = !busy, role = Role.RadioButton) { bearColor = choice }
                                        .semantics { contentDescription = bearColorLabel(choice) }
                                        .background(Color(choice.argb), CircleShape)
                                        .border(if (choice == bearColor) 3.dp else 0.dp,
                                            if (choice == bearColor) GardenColors.Ink else Color.Transparent, CircleShape),
                                        contentAlignment = Alignment.Center) {
                                        if (choice == bearColor) Icon(Icons.Default.Check, null,
                                            tint = if (choice == BearColor.HONEY) GardenColors.Ink else Color.White)
                                    }
                                    Text(bearColorLabel(choice), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { vm.saveChild(child.copy(name = name.trim(), bearColor = bearColor)) { rename = null } },
            enabled = !busy && !state.fromCache && name.isNotBlank() && name.length <= 60) { Text(if (busy) "Speichert …" else "Speichern") } },
        dismissButton = { TextButton(onClick = { rename = null }, enabled = !busy) { Text("Abbrechen") } }) }
}

private fun bearColorLabel(color: BearColor) = when (color) {
    BearColor.BLUE -> "Blau"
    BearColor.ROSE -> "Rosa"
    BearColor.GREEN -> "Grün"
    BearColor.HONEY -> "Honig"
    BearColor.BROWN -> "Braun"
    BearColor.PURPLE -> "Lila"
}

@Composable
private fun TaskEditor(existing: TaskRevision?, children: List<Child>, parents: List<Parent>, today: LocalDate,
    busy: Boolean, offline: Boolean, saveError: String?, selection: TaskEditSelection? = null, syncError: String?, onRetrySync: () -> Unit,
    onDismiss: () -> Unit, onSave: (TaskRevision) -> Unit) {
    val formats = rememberDateTimeFormats()
    val effective = today
    val single = selection?.scope == OperationScope.SINGLE
    val id = remember { UUID.randomUUID().toString() }
    val taskId = remember { existing?.taskId ?: UUID.randomUUID().toString() }
    var title by rememberSaveable { mutableStateOf(existing?.title ?: "") }
    var notes by rememberSaveable { mutableStateOf(existing?.notes ?: "") }
    var childId by rememberSaveable { mutableStateOf(existing?.childId ?: children.firstOrNull()?.id.orEmpty()) }
    var responsibleParent by remember { mutableStateOf(existing?.responsibleParentId) }
    var frequency by remember { mutableStateOf(if (single) Frequency.ONCE else existing?.frequency ?: Frequency.DAILY) }
    var weekdays by remember { mutableStateOf(existing?.weekdays ?: emptySet()) }
    var times by remember { mutableStateOf(if (single) listOf(requireNotNull(selection).occurrence.time) else existing?.times ?: listOf(LocalTime.of(8, 0))) }
    var start by remember { mutableStateOf(if (single) requireNotNull(selection).occurrence.date else existing?.start?.let {
        if (existing.frequency == Frequency.ONCE && it < effective) effective else it
    } ?: effective) }
    var end by remember { mutableStateOf(existing?.end) }
    var error by remember { mutableStateOf<String?>(null) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false,
        dismissOnBackPress = !busy, dismissOnClickOutside = !busy)) {
        GardenBackground(Modifier.fillMaxSize()) {
            Column(Modifier.safeDrawingPadding().imePadding()) {
                TopAppBar(title = { Text(if (existing == null) "Neue Aufgabe" else "Aufgabe bearbeiten") },
                    navigationIcon = { IconButton(onClick = onDismiss, enabled = !busy) { Icon(Icons.Default.Close, "Schließen") } })
                LazyColumn(contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            GardenDrawing(GardenMotif.WATERING_CAN, Modifier.size(56.dp))
                            Text("Platz für einen kleinen Alltagshelfer.", style = MaterialTheme.typography.titleMedium,
                                color = GardenColors.Blue, modifier = Modifier.weight(1f))
                        }
                    }
                    if (existing != null) item { Text("Änderung gilt für: ${if (single) "nur diese Ausführung" else "diese und alle folgenden"}. Begonnene oder bestätigte Ausführungen bleiben erhalten.") }
                    item { OutlinedTextField(title, { title = it }, label = { Text("Aufgabe") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
                    item { Text("Für welchen Bereich?", style = MaterialTheme.typography.titleSmall) }
                    item { Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        children.forEach { child -> GardenChildChip(child, childId == child.id) { childId = child.id } }
                    } }
                    item { AssignmentPicker(parents, responsibleParent) { responsibleParent = it } }
                    item { OutlinedTextField(notes, { notes = it }, label = { Text("Notizen (optional)") }, modifier = Modifier.fillMaxWidth(), minLines = 2) }
                    item { Text("Wiederholung", style = MaterialTheme.typography.titleSmall) }
                    if (!single) items(Frequency.entries) { value ->
                        Row(Modifier.fillMaxWidth().clickable { frequency = value }, verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(frequency == value, { frequency = value }); Text(frequencyLabel(value))
                        }
                    }
                    if (frequency == Frequency.WEEKDAYS) item {
                        Column { DayOfWeek.values().toList().chunked(4).forEach { days ->
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { days.forEach { day ->
                                FilterChip(day in weekdays, { weekdays = if (day in weekdays) weekdays - day else weekdays + day },
                                    label = { Text(day.getDisplayName(java.time.format.TextStyle.SHORT, formats.locale)) })
                            } }
                        } }
                    }
                    item {
                        if (single) TimePickerField("Uhrzeit", times.single(), { times = listOf(it) }, enabled = !busy)
                        else TimeSlotsPicker(times, { times = it }, enabled = !busy)
                    }
                    item { DatePickerField(if (frequency == Frequency.ONCE) "Datum" else "Startdatum", start,
                        { it?.let { chosen -> start = chosen } }, enabled = !busy,
                        minDate = if (existing == null) today else if (frequency == Frequency.ONCE) effective else null) }
                    if (frequency != Frequency.ONCE) item { DatePickerField("Enddatum (optional)", end, { end = it },
                        optional = true, enabled = !busy, minDate = maxOf(start, effective)) }
                    error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
                    saveError?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
                    if (syncError != null) item {
                        Text(syncError, color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = onRetrySync, enabled = !busy) { Text("Erneut versuchen") }
                    } else if (offline) item { Text("Zum Bearbeiten ist eine Verbindung erforderlich.") }
                    item {
                        Button(enabled = !busy && !offline && children.isNotEmpty(), modifier = Modifier.fillMaxWidth(), onClick = {
                            try {
                                val revision = TaskRevision(id, taskId, childId, title.trim(), notes.trim(), frequency,
                                    weekdays, times.sorted(), start, if (frequency == Frequency.ONCE) null else end, effective, responsibleParentId = responsibleParent)
                                revision.validate()
                                require(revision.frequency != Frequency.ONCE || revision.start >= effective) { "Das Datum muss am oder nach dem Gültigkeitsbeginn liegen." }
                                require(existing != null || revision.start >= today) { "Neue Aufgaben beginnen frühestens heute." }
                                require(revision.end == null || revision.end >= effective) { "Das Enddatum liegt vor dem Gültigkeitsbeginn." }
                                error = null; onSave(revision)
                            } catch (e: Exception) {
                                error = e.message ?: "Bitte Angaben prüfen."
                            }
                        }) { Text(if (busy) "Wird gespeichert …" else "Aufgabe speichern") }
                    }
                }
            }
        }
    }
}

@Composable
private fun YellowAddButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    ExtendedFloatingActionButton(onClick = onClick, modifier = modifier, icon = { Icon(Icons.Default.Add, null) },
        text = { Text(label, fontWeight = FontWeight.Bold) }, containerColor = GardenColors.Mustard,
        contentColor = GardenColors.Ink, shape = RoundedCornerShape(22.dp))
}
