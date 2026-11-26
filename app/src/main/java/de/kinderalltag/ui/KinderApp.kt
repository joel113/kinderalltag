@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package de.kinderalltag.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.kinderalltag.KinderApplication
import de.kinderalltag.data.*
import de.kinderalltag.domain.*
import de.kinderalltag.notifications.ActivityNotifications
import de.kinderalltag.notifications.ReminderScheduler
import de.kinderalltag.text.*
import java.time.*
import java.util.UUID

private enum class AppPage(private val titleKey: TextKey) {
    HOME(TextKey.TODAY),
    TASKS(TextKey.TASKS),
    APPOINTMENTS(TextKey.APPOINTMENTS),
    SHOPPING(TextKey.SHOPPING_LIST),
    GIFTS(TextKey.GIFT_IDEAS),
    HISTORY(TextKey.HISTORY),
    STATISTICS(TextKey.STATISTICS),
    TRASH(TextKey.TRASH),
    SETTINGS(TextKey.SETTINGS);

    val title: String
        get() = text(titleKey)
}

private val green = StatusColors.success

private val taskEncouragements
    get() =
        listOf(
            text(TextKey.MANAGE_THE_TASKS_THAT_NEED_DOING_AND_MAKE_ROOM_IN_YOUR_MIND_FOR_LIFE_S_L),
            text(TextKey.KEEP_THE_LITTLE_TASKS_IN_SIGHT_AND_YOUR_MIND_FREE_FOR_BIG_MOMENTS_A_FIRS),
            text(TextKey.YOUR_TASKS_HAVE_A_HOME_HERE_LEAVE_MORE_ROOM_FOR_PICTURE_BOOK_ADVENTURES),
            text(TextKey.SOME_THINGS_NEED_DOING_KEEP_THEM_HERE_AND_ENJOY_THE_MOMENTS_IN_BETWEEN_M),
            text(TextKey.SHARE_THE_TASKS_AND_COLLECT_MEMORIES_TOGETHER_LITTLE_DISCOVERIES_BRAVE_S),
        )

private val appointmentEncouragements
    get() =
        listOf(
            text(TextKey.KEEP_YOUR_APPOINTMENTS_IN_SIGHT_AND_YOUR_MIND_FREE_FOR_LIFE_S_LOVELY_THI),
            text(TextKey.YOUR_APPOINTMENTS_HAVE_A_HOME_HERE_LEAVE_MORE_ROOM_FOR_SPONTANEOUS_PLAYG),
            text(TextKey.FROM_THE_PAEDIATRICIAN_TO_A_FAMILY_TRIP_PLAN_WHAT_MATTERS_TO_YOU_AND_ENJ),
            text(TextKey.A_CLEAR_OVERVIEW_MAKES_EVERYDAY_LIFE_EASIER_SO_YOU_CAN_BE_FULLY_THERE_FO),
            text(TextKey.KEEP_YOUR_PLANS_HERE_AND_COLLECT_MEMORIES_ALONG_THE_WAY_NEW_FRIENDS_BRAV),
        )

@Preview(name = "Heute", showBackground = true, widthDp = 390, heightDp = 820)
@Composable
private fun TagesgartenPreview() {
    KinderTheme {
        val date = LocalDate.of(2026, 11, 16)
        val children = listOf(Child("ferdinand", "Ferdinand"), Child("johann", "Johann"))
        val routines =
            listOf(
                TaskRevision(
                    "preview-1",
                    "task-1",
                    "ferdinand",
                    "Rucksack packen",
                    start = date,
                    effective = date,
                )
            )
        val occurrence = Schedule.occurrences(routines, date).first()
        val state =
            FamilyState(
                children = children,
                revisions = routines,
                appointments =
                    listOf(
                        AppointmentSeries(
                            "preview-appointment",
                            AppointmentDetails(
                                text(TextKey.PAEDIATRICIAN),
                                AppointmentRecipient.JOHANN,
                                date,
                                LocalTime.of(9, 0),
                                LocalTime.of(9, 30),
                            ),
                        )
                    ),
                events =
                    listOf(
                        CareEvent(
                            "preview-event",
                            occurrence.id,
                            date,
                            EventKind.COMPLETE,
                            "preview",
                            "Mama",
                            date.atTime(8, 5).atZone(FAMILY_ZONE).toInstant().toEpochMilli(),
                        )
                    ),
            )
        AppBackground(Modifier.fillMaxSize()) {
            Checklist(
                state,
                date,
                date,
                null,
                now = date.atTime(10, 0).atZone(FAMILY_ZONE).toInstant(),
                onSelectDate = {},
            ) {}
        }
    }
}

@Composable
fun KinderApp(
    vm: FamilyViewModel,
    initialDate: String?,
    initialOccurrence: String?,
    signingIn: Boolean,
    loginError: String?,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    initialPage: String? = null,
) {
    val context = LocalContext.current
    val displayPreferences =
        remember(context) {
            context.getSharedPreferences("display", android.content.Context.MODE_PRIVATE)
        }
    var showIntroductions by remember {
        mutableStateOf(displayPreferences.getBoolean("show_introductions", false))
    }
    var showCalendars by remember {
        mutableStateOf(displayPreferences.getBoolean("show_calendars", true))
    }
    var showUpcomingTasks by remember {
        mutableStateOf(displayPreferences.getBoolean("show_upcoming_tasks", false))
    }
    var upcomingTaskCount by remember {
        mutableStateOf(displayPreferences.getInt("upcoming_task_count", 5).coerceIn(1, 50))
    }
    var upcomingTaskDays by remember {
        mutableStateOf(displayPreferences.getInt("upcoming_task_days", 7).coerceIn(1, 365))
    }
    var pastTaskDays by remember {
        mutableStateOf(displayPreferences.getInt("past_task_days", 7).takeIf { it in 1..365 } ?: 7)
    }
    var showPastTasks by remember {
        mutableStateOf(
            displayPreferences.getBoolean(
                "show_past_tasks",
                displayPreferences.getInt("past_task_days", 7) > 0,
            )
        )
    }
    val inbox = remember { (context.applicationContext as KinderApplication).calendarInbox }
    val incoming by inbox.state.collectAsStateWithLifecycle()
    val state by vm.state.collectAsStateWithLifecycle()
    val today by vm.today.collectAsStateWithLifecycle()
    val now by vm.clock.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val taskSaveError by vm.taskError.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val snack = remember { SnackbarHostState() }
    val localeTags =
        androidx.compose.ui.platform.LocalConfiguration.current.locales.toLanguageTags()
    LaunchedEffect(localeTags) { vm.refreshPresentation() }
    LaunchedEffect(message) {
        message?.let {
            snack.showSnackbar(it)
            vm.message.value = null
        }
    }
    if (state.uid == null || !state.authorized) {
        AppBackground(Modifier.fillMaxSize()) {
            Column(
                Modifier.safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Kinderalltag",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                )
                AppBanner(
                    text(TextKey.EVERYDAY_LIFE_GROWS_TOGETHER),
                    text(TextKey.FOR_FERDINAND_AND_JOHANN_AND_FOR_BOTH_OF_YOU),
                )
                Text(
                    text(TextKey.LITTLE_TASKS_IN_SIGHT_TOGETHER),
                    style = MaterialTheme.typography.titleLarge,
                )
                if (incoming.current != null)
                    Text(
                        text(
                            TextKey
                                .A_CALENDAR_IMPORT_IS_WAITING_PLEASE_SIGN_IN_AND_ENABLE_FAMILY_ACCESS_FIR
                        )
                    )
                incoming.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (state.uid == null) {
                    Button(onClick = onSignIn, enabled = !signingIn) {
                        Text(
                            if (signingIn) text(TextKey.SIGNING_IN)
                            else text(TextKey.SIGN_IN_WITH_GOOGLE)
                        )
                    }
                    loginError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                } else {
                    Text(
                        text(
                            TextKey
                                .HELLO_YOUR_FAMILY_ACCESS_NEEDS_TO_BE_ENABLED_THIS_USER_ID_IS_REQUIRED,
                            state.name,
                        )
                    )
                    androidx.compose.foundation.text.selection.SelectionContainer {
                        Text(state.uid.orEmpty())
                    }
                    Text(
                        text(
                            TextKey
                                .SETUP_IS_DESCRIBED_IN_DOCS_SETUP_MD_THE_CHECKLIST_OPENS_AUTOMATICALLY_ON
                        )
                    )
                    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    TextButton(onClick = vm::retrySync) { Text(text(TextKey.TRY_AGAIN)) }
                    TextButton(onClick = onSignOut) { Text(text(TextKey.SIGN_OUT)) }
                }
            }
        }
        return
    }
    var tab by rememberSaveable {
        mutableStateOf(
            if (initialOccurrence?.startsWith("appointment_") == true) AppPage.APPOINTMENTS
            else if (initialOccurrence != null) AppPage.HOME
            else if (initialDate != null) AppPage.HISTORY
            else
                when (initialPage) {
                    "tasks" -> AppPage.TASKS
                    "appointments" -> AppPage.APPOINTMENTS
                    "shopping" -> AppPage.SHOPPING
                    "gifts" -> AppPage.GIFTS
                    else -> AppPage.HOME
                }
        )
    }
    val primaryPages = listOf(AppPage.HOME, AppPage.TASKS, AppPage.APPOINTMENTS, AppPage.SHOPPING)
    var lastPrimaryPage by rememberSaveable {
        mutableStateOf(if (tab in primaryPages) tab else AppPage.HOME)
    }
    LaunchedEffect(lastPrimaryPage) {
        if (lastPrimaryPage !in primaryPages) lastPrimaryPage = AppPage.HOME
    }
    var moreMenuExpanded by remember { mutableStateOf(false) }
    var statisticsDays by rememberSaveable { mutableIntStateOf(90) }
    var statisticsPage by rememberSaveable { mutableLongStateOf(0L) }
    var historyPage by rememberSaveable { mutableLongStateOf(0L) }
    var selectedChild by rememberSaveable { mutableStateOf<String?>(null) }
    var onlyMine by rememberSaveable { mutableStateOf(false) }
    var homeDayOffset by rememberSaveable { mutableLongStateOf(0L) }
    var taskCreationDate by rememberSaveable { mutableStateOf<LocalDate?>(null) }
    var taskMessageIndex by rememberSaveable { mutableIntStateOf(-1) }
    var appointmentMessageIndex by rememberSaveable { mutableIntStateOf(0) }
    var openedImportKey by rememberSaveable { mutableStateOf<String?>(null) }
    var taskCalendar by
        rememberSaveable(stateSaver = calendarOverviewSaver) {
            mutableStateOf(CalendarOverviewState(today))
        }
    var appointmentCalendar by
        rememberSaveable(stateSaver = calendarOverviewSaver) {
            mutableStateOf(CalendarOverviewState(today))
        }
    var taskPage by rememberSaveable { mutableLongStateOf(0L) }
    var agendaPage by rememberSaveable { mutableLongStateOf(0L) }
    val appointmentUi = vm.appointmentUi
    BackHandler(enabled = tab !in primaryPages) { tab = lastPrimaryPage }
    var editing by vm.taskEditorRevision
    var taskEditSelection by vm.taskEditorSelection
    var itemAction by remember { mutableStateOf<ItemActionTarget?>(null) }
    var editorOpen by vm.taskEditorOpen
    LaunchedEffect(editorOpen) { if (editorOpen) vm.taskError.value = null }
    var selected by remember { mutableStateOf<Occurrence?>(null) }
    var openedInitial by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(
        state.revisions,
        state.appointments,
        state.appointmentExceptions,
        state.loaded,
        initialOccurrence,
    ) {
        if (!openedInitial && initialOccurrence != null) {
            val date = runCatching { LocalDate.parse(initialDate) }.getOrDefault(today)
            if (initialOccurrence.startsWith("appointment_")) {
                state
                    .appointmentOccurrences(date, date)
                    .firstOrNull { it.id == initialOccurrence }
                    ?.let {
                        appointmentUi.open(it, state)
                        openedInitial = true
                    }
                if (!openedInitial && state.loaded) {
                    snack.showSnackbar(text(TextKey.THIS_APPOINTMENT_HAS_CHANGED_OR_BEEN_CANCELLED))
                    openedInitial = true
                }
            } else
                state
                    .taskOccurrences(date)
                    .firstOrNull { it.id == initialOccurrence }
                    ?.let {
                        selected = it
                        openedInitial = true
                    }
        }
    }
    LaunchedEffect(
        incoming.key,
        editorOpen,
        appointmentUi.editor,
        appointmentUi.selected,
        selected,
    ) {
        val key = incoming.key
        val item = incoming.current
        if (
            key != null &&
                item != null &&
                key != openedImportKey &&
                !editorOpen &&
                appointmentUi.editor == null &&
                appointmentUi.selected == null &&
                selected == null
        ) {
            appointmentUi.editor = AppointmentEditTarget(null, imported = item, importKey = key)
            openedImportKey = key
        }
    }
    val homeButtonsWidth =
        (LocalConfiguration.current.screenWidthDp.dp - 32.dp).coerceAtMost(360.dp)
    val nav = primaryPages
    val icons =
        listOf(
            Icons.Default.WbSunny,
            Icons.Default.LocalFlorist,
            Icons.Default.Event,
            Icons.Default.ShoppingBasket,
        )
    val navColors =
        listOf(
            AppTheme.colors.accent,
            AppTheme.colors.primary,
            AppTheme.colors.deepAccent,
            AppTheme.colors.accent,
        )
    AppBackground(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snack) },
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Box {
                                IconButton(
                                    onClick = { moreMenuExpanded = !moreMenuExpanded },
                                    modifier =
                                        Modifier.semantics {
                                            contentDescription = text(TextKey.OPEN_MORE_PAGES)
                                        },
                                ) {
                                    AppIllustration(IllustrationRole.BRAND, Modifier.size(34.dp))
                                }
                                DropdownMenu(
                                    expanded = moreMenuExpanded,
                                    onDismissRequest = { moreMenuExpanded = false },
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(text(TextKey.GIFT_IDEAS)) },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.CardGiftcard,
                                                null,
                                                tint = AppTheme.colors.secondary,
                                            )
                                        },
                                        onClick = {
                                            moreMenuExpanded = false
                                            if (tab != AppPage.GIFTS)
                                                vm.listUi.nextThought(FamilyListKind.GIFTS)
                                            tab = AppPage.GIFTS
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(text(TextKey.HISTORY)) },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.History,
                                                null,
                                                tint = AppTheme.colors.secondary,
                                            )
                                        },
                                        onClick = {
                                            moreMenuExpanded = false
                                            tab = AppPage.HISTORY
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(text(TextKey.STATISTICS)) },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.BarChart,
                                                null,
                                                tint = AppTheme.colors.accent,
                                            )
                                        },
                                        onClick = {
                                            moreMenuExpanded = false
                                            tab = AppPage.STATISTICS
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(text(TextKey.TRASH)) },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.DeleteOutline,
                                                null,
                                                tint = AppTheme.colors.secondary,
                                            )
                                        },
                                        onClick = {
                                            moreMenuExpanded = false
                                            tab = AppPage.TRASH
                                        },
                                    )
                                }
                            }
                            Text(
                                if (tab in primaryPages) "Kinderalltag" else tab.title,
                                fontWeight = FontWeight.ExtraBold,
                                color = AppTheme.colors.accent,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    },
                    navigationIcon = {
                        if (tab !in primaryPages)
                            IconButton(
                                onClick = {
                                    moreMenuExpanded = false
                                    tab = lastPrimaryPage
                                }
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, text(TextKey.BACK))
                            }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                moreMenuExpanded = false
                                tab = AppPage.SETTINGS
                            }
                        ) {
                            Icon(Icons.Default.Settings, text(TextKey.SETTINGS))
                        }
                    },
                )
            },
            bottomBar = {
                NavigationBar(containerColor = AppTheme.colors.background, tonalElevation = 0.dp) {
                    nav.forEachIndexed { index, destination ->
                        NavigationBarItem(
                            selected = tab == destination,
                            onClick = {
                                if (destination == AppPage.SHOPPING && tab != destination)
                                    vm.listUi.nextThought(FamilyListKind.SHOPPING)
                                if (destination == AppPage.TASKS && tab != AppPage.TASKS)
                                    taskMessageIndex =
                                        (taskMessageIndex + 1) % taskEncouragements.size
                                if (
                                    destination == AppPage.APPOINTMENTS &&
                                        tab != AppPage.APPOINTMENTS
                                )
                                    appointmentMessageIndex =
                                        (appointmentMessageIndex + 1) %
                                            appointmentEncouragements.size
                                moreMenuExpanded = false
                                lastPrimaryPage = destination
                                tab = destination
                                if (destination == AppPage.HOME) homeDayOffset = 0L
                            },
                            icon = { Icon(icons[index], null) },
                            label = {
                                Text(
                                    destination.title,
                                    maxLines = 2,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                )
                            },
                            colors =
                                NavigationBarItemDefaults.colors(
                                    selectedIconColor = navColors[index],
                                    selectedTextColor = navColors[index],
                                    indicatorColor = navColors[index].copy(alpha = 0.12f),
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                        )
                    }
                }
            },
            floatingActionButton = {
                when (tab) {
                    AppPage.HOME ->
                        Row(
                            Modifier.width(homeButtonsWidth),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            YellowAddButton(text(TextKey.TASK), Modifier.weight(1f)) {
                                editing = null
                                taskEditSelection = null
                                taskCreationDate = today.plusDays(homeDayOffset)
                                editorOpen = true
                            }
                            YellowAddButton(text(TextKey.APPOINTMENT), Modifier.weight(1f)) {
                                appointmentUi.editor =
                                    AppointmentEditTarget(
                                        null,
                                        initialDate = today.plusDays(homeDayOffset),
                                    )
                            }
                        }
                    AppPage.TASKS ->
                        YellowAddButton(text(TextKey.TASK)) {
                            editing = null
                            taskEditSelection = null
                            taskCreationDate =
                                taskCalendar.focus.takeIf {
                                    showCalendars && taskCalendar.filterActive
                                } ?: today
                            editorOpen = true
                        }
                    AppPage.APPOINTMENTS ->
                        YellowAddButton(text(TextKey.APPOINTMENT)) {
                            appointmentUi.editor =
                                AppointmentEditTarget(
                                    null,
                                    initialDate =
                                        appointmentCalendar.focus.takeIf {
                                            showCalendars && appointmentCalendar.filterActive
                                        },
                                )
                        }
                    AppPage.SHOPPING ->
                        YellowAddButton(text(TextKey.SHOPPING_ITEM)) {
                            vm.listUi.voiceImportOpen = true
                        }
                    AppPage.GIFTS ->
                        YellowAddButton(text(TextKey.GIFT_IDEA)) {
                            vm.listUi.editor = ListEditTarget(FamilyListKind.GIFTS)
                        }
                    else -> Unit
                }
            },
        ) { padding ->
            Column(Modifier.padding(padding).fillMaxSize()) {
                if (state.fromCache || state.pending || state.error != null) {
                    Column(
                        Modifier.fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(12.dp)
                    ) {
                        Text(
                            state.error
                                ?: if (state.pending) text(TextKey.SYNCHRONIZING_CHANGES)
                                else text(TextKey.SAVED_DATA_CONNECTING),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        if (state.error != null)
                            TextButton(onClick = vm::retrySync, enabled = !busy) {
                                Text(text(TextKey.TRY_AGAIN))
                            }
                    }
                }
                incoming.error?.let { error ->
                    Column(Modifier.padding(horizontal = 20.dp)) {
                        Text(
                            error.lineSequence().take(3).joinToString("\n"),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        TextButton(onClick = inbox::dismissError) {
                            Text(text(TextKey.DISMISS_NOTICE))
                        }
                    }
                }
                if (incoming.current != null)
                    Row(
                        Modifier.padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text(
                                TextKey.TO_ADD,
                                incoming.remaining,
                                if (incoming.remaining == 1) text(TextKey.APPOINTMENT)
                                else text(TextKey.APPOINTMENTS),
                            ),
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        TextButton(
                            onClick = {
                                incoming.current?.let {
                                    appointmentUi.editor =
                                        AppointmentEditTarget(
                                            null,
                                            imported = it,
                                            importKey = incoming.key,
                                        )
                                }
                                openedImportKey = incoming.key
                            }
                        ) {
                            Text(text(TextKey.REVIEW))
                        }
                        TextButton(onClick = { incoming.key?.let { inbox.consume(it) } }) {
                            Text(text(TextKey.SKIP))
                        }
                    }
                if (
                    tab in
                        listOf(AppPage.HOME, AppPage.TASKS, AppPage.APPOINTMENTS, AppPage.HISTORY)
                )
                    Row(
                        Modifier.horizontalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FilterChip(
                            onlyMine,
                            {
                                onlyMine = !onlyMine
                                taskPage = 0
                            },
                            label = { Text(text(TextKey.ONLY_MINE)) },
                        )
                        VerticalDivider(Modifier.height(24.dp))
                        FilterChip(
                            selectedChild == null,
                            {
                                selectedChild = null
                                taskPage = 0
                            },
                            label = { Text(text(TextKey.ALL)) },
                        )
                        state.sections.forEach { child ->
                            AppChildChip(child, selectedChild == child.id) {
                                selectedChild = child.id
                                taskPage = 0
                            }
                        }
                    }
                when (tab) {
                    AppPage.HOME -> {
                        val date = today.plusDays(homeDayOffset)
                        Checklist(
                            state,
                            date,
                            today,
                            selectedChild,
                            onlyMine,
                            vm = vm,
                            busy = busy,
                            now = now,
                            upcomingTaskLimit = if (showUpcomingTasks) upcomingTaskCount else 0,
                            upcomingTaskDays = upcomingTaskDays,
                            pastTaskDays = if (showPastTasks) pastTaskDays else 0,
                            onSelectDate = { chosen ->
                                homeDayOffset = chosen.toEpochDay() - today.toEpochDay()
                            },
                            onOpenAppointment = { appointmentUi.open(it, state) },
                            onOpen = { selected = it },
                        )
                    }
                    AppPage.HISTORY ->
                        HistoryFeed(
                            state,
                            today,
                            historyPage,
                            selectedChild,
                            onlyMine,
                            onPageChange = { historyPage = it },
                        )
                    AppPage.TASKS ->
                        TaskList(
                            state,
                            today,
                            now,
                            busy,
                            selectedChild,
                            onlyMine,
                            taskPage,
                            onPageChange = { taskPage = it },
                            encouragement = taskEncouragements[taskMessageIndex.coerceAtLeast(0)],
                            showIntroduction = showIntroductions,
                            showCalendar = showCalendars,
                            calendar = taskCalendar,
                            onOpen = { selected = it },
                            onCalendarChange = {
                                taskCalendar = it
                                taskPage = 0
                            },
                            onEdit = {
                                itemAction =
                                    ItemActionTarget(
                                        false,
                                        definition = it,
                                        expectedVersion = state.version(ItemType.TASK, it.taskId),
                                    )
                            },
                            onDelete = { task, date ->
                                val occurrence =
                                    date?.let { day ->
                                        state.taskOccurrences(day).firstOrNull {
                                            it.revision.taskId == task.taskId
                                        }
                                    }
                                itemAction =
                                    ItemActionTarget(
                                        true,
                                        definition = if (occurrence == null) task else null,
                                        task = occurrence,
                                        expectedVersion = state.version(ItemType.TASK, task.taskId),
                                    )
                            },
                            onArchive = vm::archive,
                        )
                    AppPage.APPOINTMENTS ->
                        AppointmentAgenda(
                            state,
                            today,
                            now,
                            agendaPage,
                            selectedChild,
                            onlyMine,
                            encouragement = appointmentEncouragements[appointmentMessageIndex],
                            showIntroduction = showIntroductions,
                            showCalendar = showCalendars,
                            calendar = appointmentCalendar,
                            onCalendarChange = {
                                appointmentCalendar = it
                                agendaPage = 0
                            },
                            onPageChange = { agendaPage = it },
                            onOpen = { appointmentUi.open(it, state) },
                        )
                    AppPage.SHOPPING ->
                        FamilyListPage(FamilyListKind.SHOPPING, state, vm, showIntroductions)
                    AppPage.GIFTS ->
                        FamilyListPage(FamilyListKind.GIFTS, state, vm, showIntroductions)
                    AppPage.STATISTICS ->
                        StatisticsScreen(
                            state,
                            today,
                            statisticsDays,
                            statisticsPage,
                            onDaysChange = {
                                statisticsDays = it
                                statisticsPage = 0
                            },
                            onPageChange = { statisticsPage = it },
                        )
                    AppPage.TRASH -> TrashScreen(state, vm)
                    AppPage.SETTINGS ->
                        Settings(
                            state,
                            busy,
                            onSignOut,
                            vm,
                            showIntroductions = showIntroductions,
                            onShowIntroductionsChange = {
                                showIntroductions = it
                                displayPreferences
                                    .edit()
                                    .putBoolean("show_introductions", it)
                                    .apply()
                            },
                            showCalendars = showCalendars,
                            showUpcomingTasks = showUpcomingTasks,
                            onShowUpcomingTasksChange = {
                                showUpcomingTasks = it
                                displayPreferences
                                    .edit()
                                    .putBoolean("show_upcoming_tasks", it)
                                    .apply()
                            },
                            upcomingTaskCount = upcomingTaskCount,
                            onUpcomingTaskCountChange = {
                                upcomingTaskCount = it.coerceIn(1, 50)
                                displayPreferences
                                    .edit()
                                    .putInt("upcoming_task_count", upcomingTaskCount)
                                    .apply()
                            },
                            upcomingTaskDays = upcomingTaskDays,
                            onUpcomingTaskDaysChange = {
                                upcomingTaskDays = it.coerceIn(1, 365)
                                displayPreferences
                                    .edit()
                                    .putInt("upcoming_task_days", upcomingTaskDays)
                                    .apply()
                            },
                            showPastTasks = showPastTasks,
                            onShowPastTasksChange = {
                                showPastTasks = it
                                displayPreferences.edit().putBoolean("show_past_tasks", it).apply()
                            },
                            pastTaskDays = pastTaskDays,
                            onPastTaskDaysChange = {
                                pastTaskDays = it.coerceIn(1, 365)
                                displayPreferences
                                    .edit()
                                    .putInt("past_task_days", pastTaskDays)
                                    .apply()
                            },
                            onShowCalendarsChange = {
                                showCalendars = it
                                displayPreferences.edit().putBoolean("show_calendars", it).apply()
                                taskCalendar = taskCalendar.reset(today)
                                appointmentCalendar = appointmentCalendar.reset(today)
                                taskPage = 0
                                agendaPage = 0
                            },
                        )
                }
            }
        }
    }
    if (editorOpen)
        TaskEditor(
            editing,
            state.sections,
            state.parents,
            today,
            busy,
            state.fromCache,
            if (taskEditSelection == null) taskSaveError
            else vm.operationError.collectAsStateWithLifecycle().value,
            selection = taskEditSelection,
            initialDate = taskCreationDate,
            syncError = state.error,
            onRetrySync = vm::retrySync,
            onDismiss = { if (!busy) editorOpen = false },
            onSave = { draft, childIds ->
                val selection = taskEditSelection
                if (selection == null)
                    vm.saveTasks(
                        tasksForChildren(draft, childIds) { UUID.randomUUID().toString() }
                    ) {
                        editorOpen = false
                    }
                else
                    vm.saveTaskEdit(selection, draft) {
                        editorOpen = false
                        taskEditSelection = null
                    }
            },
        )
    itemAction?.let { target ->
        ItemActionDialog(
            target,
            state,
            vm,
            onDismiss = { itemAction = null },
            onEditTask = { selection ->
                itemAction = null
                taskEditSelection = selection
                editing =
                    if (selection.scope == OperationScope.FOLLOWING)
                        state.taskDefinitions(Instant.now(), true).firstOrNull {
                            it.taskId == selection.occurrence.revision.taskId
                        } ?: selection.occurrence.revision
                    else selection.occurrence.revision
                editorOpen = true
            },
        )
    }
    FamilyListDialogs(state, vm)
    AppointmentDialogs(
        appointmentUi,
        state,
        vm,
        onImported = { key, done -> inbox.consume(key, done) },
    )
    selected?.let { occurrence ->
        LaunchedEffect(occurrence.id) { vm.careError.value = null }
        OccurrenceDialog(
            occurrence,
            state,
            today,
            now,
            vm.careError.collectAsStateWithLifecycle().value,
            onDismiss = { selected = null },
            onComplete = { parentId -> vm.complete(occurrence, parentId) },
            onUndo = { vm.undo(occurrence) },
            onEdit = {
                selected = null
                itemAction =
                    ItemActionTarget(
                        false,
                        task = occurrence,
                        expectedVersion = state.version(ItemType.TASK, occurrence.revision.taskId),
                    )
            },
            onDelete = {
                selected = null
                itemAction =
                    ItemActionTarget(
                        true,
                        task = occurrence,
                        expectedVersion = state.version(ItemType.TASK, occurrence.revision.taskId),
                    )
            },
        )
    }
}

@Composable
internal fun Checklist(
    state: FamilyState,
    date: LocalDate,
    today: LocalDate,
    childId: String?,
    onlyMine: Boolean = false,
    showTasks: Boolean = true,
    showAppointments: Boolean = true,
    vm: FamilyViewModel? = null,
    busy: Boolean = false,
    now: Instant = Instant.now(),
    upcomingTaskLimit: Int = 0,
    upcomingTaskDays: Int = 7,
    pastTaskDays: Int = 0,
    onSelectDate: ((LocalDate) -> Unit)? = null,
    onOpenAppointment: (AppointmentOccurrence) -> Unit = {},
    onOpen: (Occurrence) -> Unit,
) {
    val formats = rememberDateTimeFormats()
    val due =
        state.taskOccurrences(date).filter {
            showTasks &&
                (childId != null ||
                    state.children.none { c -> c.archived && c.id == it.revision.childId }) &&
                (childId == null || it.revision.childId == childId) &&
                (!onlyMine || isResponsible(it.revision.responsibleParentId, state.uid))
        }
    val dueTaskIds = due.map { it.revision.taskId }.toSet()
    val appointments =
        if (showAppointments) visibleAppointments(state, date, date, childId, onlyMine)
        else emptyList()
    val upcomingTasks =
        remember(
            state.children,
            state.revisions,
            state.events,
            state.itemOperations,
            date,
            upcomingTaskLimit,
            upcomingTaskDays,
            dueTaskIds,
            childId,
            onlyMine,
            state.uid,
            showTasks,
        ) {
            if (showTasks && upcomingTaskLimit > 0)
                upcomingTaskOccurrences(
                    state.revisions,
                    state.events,
                    state.itemOperations,
                    date,
                    upcomingTaskLimit,
                    childId,
                    state.uid,
                    onlyMine,
                    days = upcomingTaskDays,
                    excludedTaskIds = dueTaskIds,
                    children = state.children,
                )
            else emptyList()
        }
    val overdueTasks =
        remember(
            state.children,
            state.revisions,
            state.events,
            state.itemOperations,
            date,
            now.epochSecond / 60,
            pastTaskDays,
            childId,
            onlyMine,
            state.uid,
            showTasks,
        ) {
            if (showTasks && pastTaskDays > 0)
                overdueTaskOccurrences(
                    state.revisions,
                    state.events,
                    state.itemOperations,
                    date,
                    now,
                    pastTaskDays,
                    childId,
                    state.uid,
                    onlyMine,
                    children = state.children,
                )
            else emptyList()
        }
    val displayedTasks =
        (overdueTasks + due + upcomingTasks).sortedWith(
            compareBy<Occurrence> { it.instant }.thenBy { it.revision.title }.thenBy { it.id }
        )
    val done = due.count { Schedule.activeCompletions(state.events, it.id).isNotEmpty() }
    val appointmentsFinished = appointments.count { it.details.endInstant <= now }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (onSelectDate == null)
            item {
                Text(
                    text(TextKey.HISTORY),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        item {
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, AppTheme.colors.outline),
                colors =
                    CardDefaults.cardColors(
                        containerColor = AppTheme.colors.accentContainer,
                        contentColor = AppTheme.colors.ink,
                    ),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (onSelectDate != null) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FilledIconButton(
                                onClick = { onSelectDate(date.minusDays(1)) },
                                modifier = Modifier.size(48.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors =
                                    IconButtonDefaults.filledIconButtonColors(
                                        containerColor = AppTheme.colors.accent.copy(alpha = 0.12f),
                                        contentColor = AppTheme.colors.accent,
                                    ),
                            ) {
                                Icon(
                                    Icons.Default.ChevronLeft,
                                    text(TextKey.PREVIOUS_DAY),
                                    modifier = Modifier.size(28.dp),
                                )
                            }
                            Text(
                                formats.fullDate(date),
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                            FilledIconButton(
                                onClick = { onSelectDate(date.plusDays(1)) },
                                modifier = Modifier.size(48.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors =
                                    IconButtonDefaults.filledIconButtonColors(
                                        containerColor = AppTheme.colors.accent.copy(alpha = 0.12f),
                                        contentColor = AppTheme.colors.accent,
                                    ),
                            ) {
                                Icon(
                                    Icons.Default.ChevronRight,
                                    text(TextKey.NEXT_DAY),
                                    modifier = Modifier.size(28.dp),
                                )
                            }
                        }
                    } else {
                        Text(formats.fullDate(date), style = MaterialTheme.typography.bodyMedium)
                    }
                    if (showTasks) {
                        Column(
                            Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                quantity(QuantityKey.TASK_PROGRESS, due.size, done, due.size),
                                color = green,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            DayProgressBar(
                                if (due.isEmpty()) 1f else done.toFloat() / due.size,
                                green,
                            )
                        }
                    }
                    if (showAppointments) {
                        Column(
                            Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                quantity(
                                    QuantityKey.APPOINTMENT_PROGRESS,
                                    appointments.size,
                                    appointmentsFinished,
                                    appointments.size,
                                ),
                                color = AppTheme.colors.primary,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            DayProgressBar(
                                if (appointments.isEmpty()) 1f
                                else appointmentsFinished.toFloat() / appointments.size,
                                AppTheme.colors.primary,
                            )
                        }
                    }
                    if (onSelectDate != null && date != today) {
                        TextButton(
                            onClick = { onSelectDate(today) },
                            modifier = Modifier.align(Alignment.End),
                        ) {
                            Text(text(TextKey.BACK_TO_TODAY))
                        }
                    }
                }
            }
        }
        if (showTasks) {
            item {
                Text(
                    text(TextKey.TASKS),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            if (displayedTasks.isEmpty())
                item {
                    EmptyCard(
                        if (upcomingTaskLimit > 0 || pastTaskDays > 0)
                            text(TextKey.THERE_ARE_NO_TASKS_FOR_THIS_DAY_AND_THE_SELECTED_PERIOD)
                        else text(TextKey.THERE_ARE_NO_TASKS_FOR_THIS_DAY)
                    )
                }
        }
        state.sections
            .filter { (childId != null || !it.archived) && (childId == null || childId == it.id) }
            .forEach { child ->
                val tasks = displayedTasks.filter { it.revision.childId == child.id }
                if (tasks.isNotEmpty()) {
                    item {
                        Row(
                            Modifier.padding(top = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            ChildBadge(child)
                            Text(
                                child.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = childColor(child),
                            )
                        }
                    }
                    items(tasks, key = { it.id }) { occurrence ->
                        val completed = Schedule.activeCompletions(state.events, occurrence.id)
                        Card(
                            onClick = { onOpen(occurrence) },
                            shape = RoundedCornerShape(24.dp),
                            border =
                                BorderStroke(
                                    1.dp,
                                    if (completed.isEmpty()) childColor(child).copy(alpha = 0.25f)
                                    else AppTheme.colors.outline,
                                ),
                            colors =
                                CardDefaults.cardColors(
                                    containerColor =
                                        if (completed.isEmpty()) childPaper(child)
                                        else StatusColors.successSurface,
                                    contentColor = AppTheme.colors.ink,
                                ),
                        ) {
                            Row(
                                Modifier.padding(16.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color.White.copy(alpha = 0.7f),
                                ) {
                                    Icon(
                                        if (completed.isEmpty()) Icons.Default.Schedule
                                        else Icons.Default.CheckCircle,
                                        if (completed.isEmpty()) text(TextKey.NOT_CONFIRMED)
                                        else text(TextKey.CONFIRMED),
                                        tint =
                                            if (completed.isEmpty()) childColor(child) else green,
                                        modifier = Modifier.padding(10.dp).size(24.dp),
                                    )
                                }
                                Column(
                                    Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        occurrence.revision.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    ItemMetadataIndicators(occurrence.revision.notes)
                                    Text(
                                        if (occurrence.date == date) formats.time(occurrence.time)
                                        else
                                            "${formats.fullDate(occurrence.date)} · ${formats.time(occurrence.time)}",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                    Text(
                                        text(
                                            TextKey.ASSIGNED_TO_C1E2EE,
                                            responsibilityName(
                                                occurrence.revision.responsibleParentId,
                                                state.parents,
                                            ),
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                    Text(
                                        if (completed.isEmpty()) {
                                            if (occurrence.date > date) text(TextKey.PLANNED)
                                            else if (occurrence.date < date) text(TextKey.OVERDUE)
                                            else if (date < today) text(TextKey.NOT_CONFIRMED)
                                            else text(TextKey.STILL_OPEN)
                                        } else
                                            completed.joinToString(" · ") {
                                                completionText(it, formats)
                                            },
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        if (showAppointments) {
            item {
                Text(
                    text(TextKey.APPOINTMENTS),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            if (appointments.isEmpty())
                item { EmptyCard(text(TextKey.THERE_ARE_NO_APPOINTMENTS_FOR_THIS_DAY)) }
            items(appointments, key = { it.id }) {
                AppointmentCard(it, state) { onOpenAppointment(it) }
            }
        }
        if (vm != null) shoppingHome(state, vm, busy)
        item { EndOfPageFooter(IllustrationRole.FRIEND) }
    }
}

@Composable
private fun DayProgressBar(progress: Float, color: Color) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            Icons.Default.WbSunny,
            contentDescription = null,
            tint = StatusColors.yellow,
            modifier = Modifier.size(20.dp),
        )
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.weight(1f).height(8.dp),
            color = color,
            trackColor = Color.White.copy(alpha = 0.7f),
        )
        Icon(
            Icons.Default.DarkMode,
            contentDescription = null,
            tint = StatusColors.yellow,
            modifier = Modifier.size(20.dp),
        )
    }
}

private fun completionText(event: CareEvent, formats: DateTimeFormats): String =
    "${event.completedByName ?: event.authorName}${if (event.completedById != null && event.completedById != event.authorId) text(TextKey.RECORDED_BY, event.authorName) else ""} · ${formats.timestamp(Instant.ofEpochMilli(event.clientMillis))}${if (event.pending) text(TextKey.PENDING) else ""}"

@Composable
private fun EmptyCard(text: String) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.householdSurface),
    ) {
        Row(
            Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppIllustration(IllustrationRole.BRAND, Modifier.size(48.dp))
            Text(text, Modifier.weight(1f))
        }
    }
}

@Composable
private fun AppChildChip(child: Child, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected,
        onClick,
        label = {
            Text(
                child.name + if (child.archived) " (" + text(TextKey.CHILD_ARCHIVED) + ")" else "",
                fontWeight = FontWeight.SemiBold,
            )
        },
        leadingIcon = { ChildIcon(child, Modifier.size(26.dp)) },
        colors =
            FilterChipDefaults.filterChipColors(
                selectedContainerColor = childPaper(child),
                selectedLabelColor = childColor(child),
            ),
        shape = RoundedCornerShape(16.dp),
    )
}

@Composable
private fun TaskList(
    state: FamilyState,
    today: LocalDate,
    now: Instant,
    busy: Boolean,
    sectionId: String?,
    onlyMine: Boolean,
    page: Long,
    onPageChange: (Long) -> Unit,
    encouragement: String,
    showIntroduction: Boolean,
    showCalendar: Boolean,
    calendar: CalendarOverviewState,
    onCalendarChange: (CalendarOverviewState) -> Unit,
    onOpen: (Occurrence) -> Unit,
    onEdit: (TaskRevision) -> Unit,
    onDelete: (TaskRevision, LocalDate?) -> Unit,
    onArchive: (TaskRevision) -> Unit,
) {
    val formats = rememberDateTimeFormats()
    val selectedPeriod = calendar.period.takeIf { showCalendar && calendar.filterActive }
    val calendarDates = calendar.visibleDates
    val calendarPreviews =
        if (showCalendar)
            taskCalendarPreviews(
                state.revisions,
                calendarDates,
                sectionId,
                state.uid,
                onlyMine,
                state.events,
                state.itemOperations,
                children = state.children,
            )
        else emptyMap()
    val editableVersions =
        state.taskDefinitions(now, includePaused = true).associateBy { it.taskId }
    val scheduledTasks =
        if (selectedPeriod == null) state.taskDefinitions(now)
        else
            tasksInCalendarPeriod(
                state.revisions,
                selectedPeriod,
                sectionId,
                state.uid,
                onlyMine,
                state.events,
                state.itemOperations,
            )
    val filteredTasks =
        scheduledTasks.filter {
            (sectionId == null || it.childId == sectionId) &&
                (sectionId != null ||
                    state.children.none { c -> c.id == it.childId && c.archived }) &&
                (!onlyMine || isResponsible(it.responsibleParentId, state.uid))
        }
    val timings =
        taskOverviewTimings(
            filteredTasks.map { it.taskId }.toSet(),
            state.revisions,
            state.events,
            state.itemOperations,
            now,
            selectedPeriod,
        )
    val tasks = chronologicalTasks(filteredTasks, timings)
    var showPaused by rememberSaveable { mutableStateOf(false) }
    val paused =
        state
            .taskDefinitions(now, includePaused = true)
            .filter { task ->
                !task.active &&
                    (sectionId == null || task.childId == sectionId) &&
                    (sectionId != null ||
                        state.children.none { c -> c.archived && c.id == task.childId }) &&
                    (!onlyMine || isResponsible(task.responsibleParentId, state.uid))
            }
            .sortedWith(
                compareBy<TaskRevision> { it.title.lowercase(java.util.Locale.ROOT) }
                    .thenBy { it.taskId }
            )
    val taskPage =
        taskPage(tasks + if (showPaused && selectedPeriod == null) paused else emptyList(), page)
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    LaunchedEffect(taskPage.index) { listState.scrollToItem(0) }
    LaunchedEffect(taskPage.index, page) {
        if (taskPage.index != page) onPageChange(taskPage.index)
    }
    var archive by remember { mutableStateOf<TaskRevision?>(null) }
    var details by remember { mutableStateOf<TaskOverviewSelection?>(null) }
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (showIntroduction) item { AppBanner(text(TextKey.YOUR_EVERYDAY_HELPER), encouragement) }
        if (showCalendar)
            item {
                CalendarOverviewCard(
                    text(TextKey.TASKS),
                    calendar,
                    today,
                    calendarPreviews,
                    text(TextKey.SHOW_ALL_TASKS),
                    onCalendarChange,
                    AppTheme.colors.accent,
                )
            }
        if (tasks.isEmpty())
            item {
                EmptyCard(
                    if (selectedPeriod == null)
                        text(TextKey.NO_UPCOMING_TASKS_ADD_NEW_TASKS_WITH_TASK)
                    else text(TextKey.NO_TASKS_IN_THE_SELECTED_PERIOD)
                )
            }
        if (paused.isNotEmpty() && selectedPeriod == null)
            item {
                TextButton(
                    onClick = {
                        onPageChange(0)
                        showPaused = !showPaused
                    }
                ) {
                    Text(
                        if (showPaused) text(TextKey.HIDE_PAUSED_TASKS)
                        else text(TextKey.SHOW_PAUSED_TASKS, paused.size)
                    )
                }
            }
        items(taskPage.tasks, key = { it.taskId }) { task ->
            val editableTask = editableVersions[task.taskId] ?: task
            val historical =
                selectedPeriod != null &&
                    (selectedPeriod.to < today ||
                        task.end?.let { it < today } == true ||
                        (task.frequency == Frequency.ONCE && task.start < today))
            val child =
                state.sections.firstOrNull { it.id == task.childId }
                    ?: Child(task.childId, text(TextKey.AREA))
            Card(
                onClick = {
                    details =
                        TaskOverviewSelection(
                            task,
                            editableTask,
                            selectedPeriod?.from,
                            historical,
                            state.version(ItemType.TASK, task.taskId),
                        )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor = childPaper(child),
                        contentColor = AppTheme.colors.ink,
                    ),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        ChildIcon(child, Modifier.size(28.dp))
                        Text(child.name, style = MaterialTheme.typography.labelLarge)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            task.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(Icons.Default.ChevronRight, null, tint = AppTheme.colors.primary)
                    }
                    ItemMetadataIndicators(task.notes)
                    val timing = timings[task.taskId]
                    if (task.frequency == Frequency.ONCE)
                        Text(
                            formats.date(timing?.date ?: task.start),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    val times =
                        if (task.frequency == Frequency.ONCE) timing?.times ?: task.times
                        else task.times
                    Text(
                        times.sorted().joinToString(" · ", transform = formats::time) +
                            " · " +
                            frequencyLabel(task.frequency, task.interval)
                    )
                    Text(
                        text(
                            TextKey.ASSIGNED_TO_C1E2EE,
                            responsibilityName(task.responsibleParentId, state.parents),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (historical)
                        Text(
                            text(TextKey.PAST_OCCURRENCE),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    else if (!editableTask.active)
                        Text(
                            text(TextKey.PAUSED),
                            color = AppTheme.colors.secondary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                }
            }
        }
        item {
            PageNavigationCard(
                text(TextKey.TASKS),
                text(TextKey.OF, taskPage.first, taskPage.last, taskPage.total),
                canPrevious = taskPage.index > 0,
                canNext = taskPage.hasNext,
                onPrevious = { onPageChange(taskPage.index - 1) },
                onNext = { onPageChange(taskPage.index + 1) },
                previousDescription = text(TextKey.PREVIOUS_90_TASKS),
                nextDescription = text(TextKey.NEXT_90_TASKS),
                resetText = if (taskPage.index > 0) text(TextKey.FIRST_TASKS) else null,
                onReset = { onPageChange(0) },
            )
        }
        item { EndOfPageFooter(IllustrationRole.AFFECTION) }
    }
    details?.let { selection ->
        TaskDefinitionDialog(
            selection,
            state,
            busy,
            now,
            onDismiss = { details = null },
            onOpen = {
                details = null
                onOpen(it)
            },
            onEdit = {
                details = null
                onEdit(selection.editable)
            },
            onPause = {
                details = null
                archive = selection.editable
            },
            onDelete = {
                details = null
                onDelete(selection.task, selection.date)
            },
        )
    }
    archive?.let { task ->
        AlertDialog(
            onDismissRequest = { archive = null },
            title = { Text(text(TextKey.PAUSE_UPCOMING_OCCURRENCES)) },
            text = {
                Text(text(TextKey.STAYS_IN_THE_HISTORY_YOU_CAN_RESUME_THE_TASK_LATER, task.title))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onArchive(task)
                        archive = null
                    }
                ) {
                    Text(text(TextKey.PAUSE))
                }
            },
            dismissButton = {
                TextButton(onClick = { archive = null }) { Text(text(TextKey.CANCEL)) }
            },
        )
    }
}

internal fun frequencyLabel(frequency: Frequency, interval: Int = 1) =
    when (frequency) {
        Frequency.ONCE -> text(TextKey.ONE_OFF)
        Frequency.DAILY ->
            if (interval == 1) text(TextKey.DAILY) else text(TextKey.EVERY_DAYS, interval)
        Frequency.WEEKDAYS -> text(TextKey.ON_WEEKDAYS)
        Frequency.WEEKS ->
            if (interval == 1) text(TextKey.WEEKLY) else text(TextKey.EVERY_WEEKS, interval)
        Frequency.MONTHS ->
            if (interval == 1) text(TextKey.MONTHLY) else text(TextKey.EVERY_MONTHS, interval)
    }

@Composable
internal fun OccurrenceDialog(
    occurrence: Occurrence,
    state: FamilyState,
    today: LocalDate,
    now: Instant,
    saveError: String?,
    onDismiss: () -> Unit,
    onComplete: (String) -> Unit,
    onUndo: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val formats = rememberDateTimeFormats()
    val completed = Schedule.activeCompletions(state.events, occurrence.id)
    val visible =
        state.taskOccurrences(occurrence.date).any {
            it.id == occurrence.id &&
                it.revision.id == occurrence.revision.id &&
                it.time == occurrence.time
        }
    val events =
        state.events
            .filter { it.occurrenceId == occurrence.id }
            .sortedBy { it.serverMillis ?: it.clientMillis }
    var confirmingUndo by remember { mutableStateOf(false) }
    var completedById by rememberSaveable(occurrence.id) { mutableStateOf(state.uid) }
    ItemDetailsDialog(
        occurrence.revision.title,
        text(TextKey.TASK_OCCURRENCE),
        onDismiss,
        actions = {
            saveError?.let {
                Text(it, color = StatusColors.error, style = MaterialTheme.typography.bodySmall)
            }
            DetailActions(
                secondary =
                    buildList {
                        if (
                            (occurrence.instant > now ||
                                isOneOffTaskSource(
                                    state.revisions,
                                    occurrence.revision.taskId,
                                    occurrence.sourceRevisionId,
                                )) && completed.isEmpty()
                        )
                            add(
                                DetailActionSpec(
                                    text(TextKey.EDIT),
                                    Icons.Default.Edit,
                                    visible && !state.fromCache && state.error == null,
                                    onClick = onEdit,
                                )
                            )
                    },
                deleteAction =
                    DetailActionSpec(
                        text(TextKey.DELETE),
                        Icons.Default.DeleteOutline,
                        visible && !state.fromCache && state.error == null,
                        onClick = onDelete,
                    ),
                primary = {
                    if (completed.isEmpty())
                        Button(
                            onClick = { completedById?.let(onComplete) },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            shape = RoundedCornerShape(14.dp),
                            enabled =
                                visible &&
                                    state.error == null &&
                                    state.parents.any { it.id == completedById },
                        ) {
                            Icon(Icons.Default.Check, null)
                            Spacer(Modifier.width(8.dp))
                            Text(text(TextKey.MARK_AS_COMPLETED))
                        }
                    else
                        OutlinedButton(
                            enabled = visible && state.error == null,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            shape = RoundedCornerShape(14.dp),
                            onClick = {
                                if (confirmingUndo) {
                                    onUndo()
                                    confirmingUndo = false
                                } else confirmingUndo = true
                            },
                        ) {
                            Text(
                                if (confirmingUndo) text(TextKey.YES_RETRACT_CONFIRMATIONS)
                                else text(TextKey.CORRECT_CONFIRMATION)
                            )
                        }
                },
            )
        },
    ) {
        DetailSection(text(TextKey.AT_A_GLANCE), AppTheme.colors.accentContainer) {
            DetailValue(
                Icons.Default.Face,
                text(TextKey.AREA),
                state.sections.firstOrNull { it.id == occurrence.revision.childId }?.name
                    ?: text(TextKey.AREA),
            )
            DetailValue(Icons.Default.Event, text(TextKey.DATE), formats.fullDate(occurrence.date))
            DetailValue(Icons.Default.Schedule, text(TextKey.TIME), formats.time(occurrence.time))
            DetailValue(
                Icons.Default.Person,
                text(TextKey.ASSIGNED_TO),
                responsibilityName(occurrence.revision.responsibleParentId, state.parents),
            )
            DetailValue(
                if (completed.isEmpty()) Icons.Default.RadioButtonUnchecked
                else Icons.Default.CheckCircle,
                text(TextKey.STATUS),
                if (completed.isEmpty()) text(TextKey.NOT_CONFIRMED_YET)
                else text(TextKey.COMPLETED),
            )
        }
        TaskCompletionOverview(occurrence.revision.taskId, state, now, occurrence.date)
        if (occurrence.revision.notes.isNotBlank())
            DetailSection(text(TextKey.NOTES), AppTheme.colors.background) {
                Text(occurrence.revision.notes)
            }
        if (!visible)
            DetailNotice(
                text(
                    TextKey.THIS_OCCURRENCE_HAS_CHANGED_BEEN_DELETED_OR_MOVED_PLEASE_CLOSE_AND_REOPE
                )
            )
        if (state.fromCache)
            DetailNotice(
                text(TextKey.SAVED_DATA_CONFIRMATIONS_FROM_THE_OTHER_PHONE_MAY_NOT_BE_VISIBLE_YET)
            )
        if (completed.isEmpty()) {
            if (occurrence.instant > now)
                Text(
                    text(
                        TextKey
                            .AN_EARLY_CONFIRMATION_COUNTS_FOR_THIS_OCCURRENCE_THE_HISTORY_RECORDS_THE
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
            if (occurrence.date < today)
                Text(
                    text(TextKey.A_LATE_CONFIRMATION_RECORDS_THE_CURRENT_TIME),
                    style = MaterialTheme.typography.bodySmall,
                )
            DetailSection(text(TextKey.COMPLETED_BY), AppTheme.colors.background) {
                Column(
                    Modifier.selectableGroup(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    state.parents.forEach { parent ->
                        Row(
                            Modifier.fillMaxWidth()
                                .selectable(
                                    selected = completedById == parent.id,
                                    onClick = { completedById = parent.id },
                                    role = Role.RadioButton,
                                )
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = completedById == parent.id, onClick = null)
                            Spacer(Modifier.width(8.dp))
                            Text(parent.name + if (parent.id == state.uid) text(TextKey.ME) else "")
                        }
                    }
                }
            }
        }
        if (events.isNotEmpty())
            DetailSection(text(TextKey.CONFIRMATIONS_CORRECTIONS), AppTheme.colors.background) {
                events.forEachIndexed { index, event ->
                    if (index > 0) HorizontalDivider()
                    Text(
                        if (event.kind == EventKind.COMPLETE) text(TextKey.CONFIRMED)
                        else text(TextKey.CONFIRMATION_RETRACTED),
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(completionText(event, formats), style = MaterialTheme.typography.bodySmall)
                    if (
                        event.serverMillis != null &&
                            event.serverMillis - event.clientMillis > 60_000
                    )
                        Text(
                            text(
                                TextKey.SYNCHRONIZED,
                                formats.timestamp(Instant.ofEpochMilli(event.serverMillis)),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                        )
                }
            }
        if (confirmingUndo)
            DetailNotice(
                text(
                    TextKey.RETRACT_ALL_CURRENT_CONFIRMATIONS_FOR_THIS_OCCURRENCE_THE_CORRECTION_REM
                )
            )
    }
}

@Composable
private fun Settings(
    state: FamilyState,
    busy: Boolean,
    onSignOut: () -> Unit,
    vm: FamilyViewModel,
    showIntroductions: Boolean,
    onShowIntroductionsChange: (Boolean) -> Unit,
    showCalendars: Boolean,
    onShowCalendarsChange: (Boolean) -> Unit,
    showUpcomingTasks: Boolean,
    onShowUpcomingTasksChange: (Boolean) -> Unit,
    upcomingTaskCount: Int,
    onUpcomingTaskCountChange: (Int) -> Unit,
    upcomingTaskDays: Int,
    onUpcomingTaskDaysChange: (Int) -> Unit,
    pastTaskDays: Int,
    onPastTaskDaysChange: (Int) -> Unit,
    showPastTasks: Boolean,
    onShowPastTasksChange: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    var upcomingDaysText by
        remember(upcomingTaskDays) { mutableStateOf(upcomingTaskDays.toString()) }
    var pastDaysText by remember(pastTaskDays) { mutableStateOf(pastTaskDays.toString()) }
    var enabled by remember { mutableStateOf(ReminderScheduler.enabled(context)) }
    var newTasks by remember {
        mutableStateOf(ActivityNotifications.enabled(context, state.uid, FamilyNoticeKind.NEW_TASK))
    }
    var taskCompletions by remember {
        mutableStateOf(
            ActivityNotifications.enabled(context, state.uid, FamilyNoticeKind.TASK_COMPLETED)
        )
    }
    var newAppointments by remember {
        mutableStateOf(
            ActivityNotifications.enabled(context, state.uid, FamilyNoticeKind.NEW_APPOINTMENT)
        )
    }
    var pendingPermission by rememberSaveable { mutableStateOf<String?>(null) }
    var showImpressum by rememberSaveable { mutableStateOf(false) }
    fun setNotificationChoice(choice: String, value: Boolean) {
        when (choice) {
            "reminders" -> {
                enabled = value
                ReminderScheduler.setEnabled(context, value)
                if (value) ReminderScheduler.update(context, vm.state.value)
            }
            "tasks" -> {
                newTasks = value
                ActivityNotifications.setEnabled(
                    context,
                    state.uid,
                    FamilyNoticeKind.NEW_TASK,
                    value,
                )
            }
            "completions" -> {
                taskCompletions = value
                ActivityNotifications.setEnabled(
                    context,
                    state.uid,
                    FamilyNoticeKind.TASK_COMPLETED,
                    value,
                )
            }
            "appointments" -> {
                newAppointments = value
                ActivityNotifications.setEnabled(
                    context,
                    state.uid,
                    FamilyNoticeKind.NEW_APPOINTMENT,
                    value,
                )
            }
        }
    }
    val permission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            pendingPermission?.let { setNotificationChoice(it, granted) }
            pendingPermission = null
        }
    fun changeNotificationChoice(choice: String, value: Boolean) {
        if (
            value &&
                Build.VERSION.SDK_INT >= 33 &&
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED
        ) {
            pendingPermission = choice
            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else setNotificationChoice(choice, value)
    }
    val version =
        remember(context) {
            runCatching {
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName
                }
                .getOrNull() ?: "1.0.0"
        }
    LazyColumn(
        contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            AppBanner(
                AppTheme.definition.settingsTitle,
                text(TextKey.EVERYTHING_THAT_MAKES_YOUR_FAMILY_DAY_YOUR_OWN),
            )
        }
        item {
            SettingsSection(
                text(TextKey.YOUR_ACCOUNT),
                Icons.Default.AccountCircle,
                AppTheme.colors.accent,
            ) {
                Text(
                    state.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    state.error
                        ?: if (state.pending) text(TextKey.SYNCHRONIZING_CHANGES)
                        else if (state.fromCache) text(TextKey.SAVED_DATA)
                        else text(TextKey.EVERYTHING_IS_UP_TO_DATE),
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedButton(onClick = vm::retrySync, enabled = !busy) {
                    Icon(Icons.Default.Sync, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(text(TextKey.SYNCHRONIZE_NOW))
                }
            }
        }
        item {
            SettingsSection(
                text(TextKey.APPEARANCE),
                Icons.Default.Palette,
                AppTheme.colors.deepAccent,
            ) {
                LanguagePicker(vm::clearTransientMessages)
                HorizontalDivider()
                ThemePicker()
                HorizontalDivider()
                SettingsToggle(
                    text(TextKey.SHOW_DAILY_THOUGHTS),
                    text(
                        TextKey
                            .SHOWS_ROTATING_MESSAGES_IN_TASKS_APPOINTMENTS_SHOPPING_LIST_AND_GIFT_IDE
                    ),
                    showIntroductions,
                    onShowIntroductionsChange,
                )
                HorizontalDivider()
                SettingsToggle(
                    text(TextKey.SHOW_CALENDAR_VIEW),
                    text(TextKey.SHOWS_THE_CALENDAR_IN_TASKS_AND_APPOINTMENTS),
                    showCalendars,
                    onShowCalendarsChange,
                )
            }
        }
        item {
            SettingsSection(text(TextKey.HOME), Icons.Default.Home, AppTheme.colors.accent) {
                SettingsToggle(
                    text(TextKey.SHOW_PAST_TASKS),
                    text(TextKey.SHOWS_OPEN_TASKS_FROM_PAST_DAYS_ON_HOME),
                    showPastTasks,
                    onShowPastTasksChange,
                )
                if (showPastTasks) {
                    OutlinedTextField(
                        value = pastDaysText,
                        onValueChange = { value ->
                            if (value.length <= 3 && value.all { it in '0'..'9' }) {
                                pastDaysText = value
                                value
                                    .toIntOrNull()
                                    ?.takeIf { it in 1..365 }
                                    ?.let(onPastTaskDaysChange)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(text(TextKey.PAST_TASKS_IN_DAYS)) },
                        supportingText = { Text(text(TextKey.VALUE_1_TO_365_DAYS_DEFAULT_7)) },
                        isError = pastDaysText.toIntOrNull()?.let { it !in 1..365 } ?: true,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    Text(
                        text(
                            TextKey
                                .OPEN_ONE_OFF_TASKS_STAY_VISIBLE_FOR_THE_SELECTED_PERIOD_FOR_RECURRING_TA
                        ),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                HorizontalDivider()
                SettingsToggle(
                    text(TextKey.SHOW_UPCOMING_TASKS),
                    text(TextKey.SHOWS_FUTURE_OCCURRENCES_WITH_THE_TASKS_OF_THE_SELECTED_DAY),
                    showUpcomingTasks,
                    onShowUpcomingTasksChange,
                )
                if (showUpcomingTasks) {
                    HorizontalDivider()
                    OutlinedTextField(
                        value = upcomingDaysText,
                        onValueChange = { value ->
                            if (value.length <= 3 && value.all { it in '0'..'9' }) {
                                upcomingDaysText = value
                                value
                                    .toIntOrNull()
                                    ?.takeIf { it in 1..365 }
                                    ?.let(onUpcomingTaskDaysChange)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(text(TextKey.PREVIEW_IN_DAYS)) },
                        supportingText = {
                            Text(
                                text(TextKey.VALUE_1_TO_365_DAYS_AFTER_THE_DISPLAYED_DAY_DEFAULT_7)
                            )
                        },
                        isError = upcomingDaysText.toIntOrNull()?.let { it !in 1..365 } ?: true,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    Text(
                        text(TextKey.ONLY_THE_NEXT_OPEN_OCCURRENCE_OF_EACH_TASK_IS_SHOWN),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    HorizontalDivider()
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text(TextKey.NUMBER_OF_TASKS),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text(TextKey.VALUE_1_TO_50_ON_THIS_DEVICE),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        IconButton(
                            onClick = { onUpcomingTaskCountChange(upcomingTaskCount - 1) },
                            enabled = upcomingTaskCount > 1,
                        ) {
                            Icon(Icons.Default.Remove, text(TextKey.FEWER_UPCOMING_TASKS))
                        }
                        Text(
                            upcomingTaskCount.toString(),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        IconButton(
                            onClick = { onUpcomingTaskCountChange(upcomingTaskCount + 1) },
                            enabled = upcomingTaskCount < 50,
                        ) {
                            Icon(Icons.Default.Add, text(TextKey.MORE_UPCOMING_TASKS))
                        }
                    }
                }
            }
        }
        item { ListLabelSettings(state, vm, busy) }
        item {
            SettingsSection(
                text(TextKey.NOTIFICATIONS),
                Icons.Default.Notifications,
                AppTheme.colors.primary,
            ) {
                SettingsToggle(
                    text(TextKey.REMINDERS),
                    text(
                        TextKey
                            .REMINDERS_FOR_YOUR_DUE_TASKS_AND_APPOINTMENTS_INCLUDING_THOSE_ASSIGNED_T
                    ),
                    enabled,
                ) {
                    changeNotificationChoice("reminders", it)
                }
                HorizontalDivider()
                SettingsToggle(
                    text(TextKey.NEW_TASKS),
                    text(TextKey.WHEN_THE_OTHER_PARENT_ADDS_A_TASK),
                    newTasks,
                ) {
                    changeNotificationChoice("tasks", it)
                }
                SettingsToggle(
                    text(TextKey.TASK_COMPLETED),
                    text(
                        TextKey
                            .WHEN_THE_OTHER_PARENT_CONFIRMS_AN_OPEN_TASK_WHILE_KINDERALLTAG_IS_RUNNIN
                    ),
                    taskCompletions,
                ) {
                    changeNotificationChoice("completions", it)
                }
                SettingsToggle(
                    text(TextKey.NEW_APPOINTMENTS),
                    text(TextKey.WHEN_THE_OTHER_PARENT_ADDS_AN_APPOINTMENT),
                    newAppointments,
                ) {
                    changeNotificationChoice("appointments", it)
                }
                Text(
                    text(
                        TextKey
                            .UPDATES_ARRIVE_WHILE_THE_APP_IS_RUNNING_AND_CONNECTED_PUSH_NOTIFICATIONS
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
                TextButton(
                    onClick = {
                        context.startActivity(
                            android.content
                                .Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(
                                    android.provider.Settings.EXTRA_APP_PACKAGE,
                                    context.packageName,
                                )
                        )
                    }
                ) {
                    Text(text(TextKey.MANAGE_ANDROID_NOTIFICATIONS))
                }
                Text(
                    text(TextKey.THESE_SETTINGS_APPLY_TO_THIS_PHONE_ANDROID_MAY_DELAY_DELIVERY),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        item {
            SettingsSection(
                text(TextKey.YOUR_CHILDREN),
                Icons.Default.ChildCare,
                AppTheme.colors.secondary,
            ) {
                ChildrenSettings(state, busy, vm::saveChild, vm::retrySync)
            }
        }
        item {
            SettingsSection(
                text(TextKey.APPOINTMENTS),
                Icons.Default.Event,
                AppTheme.colors.primary,
            ) {
                AppointmentDurationSettings()
            }
        }
        item {
            SettingsSection(
                text(TextKey.YOUR_DAILY_RHYTHM),
                Icons.Default.WbSunny,
                AppTheme.colors.deepAccent,
            ) {
                Text(
                    text(TextKey.FAMILY_TIMEZONE_EUROPE_BERLIN),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text(
                        TextKey
                            .DAY_BOUNDARIES_AND_REMINDERS_FOLLOW_YOUR_FAMILY_TIMEZONE_EVEN_WHEN_TRAVE
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        item {
            SettingsSection(
                text(TextKey.ABOUT_KINDERALLTAG),
                Icons.Default.Favorite,
                AppTheme.colors.secondary,
            ) {
                Text(
                    text(TextKey.A_SHARED_VIEW_OF_YOUR_FAMILY_S_SMALL_AND_BIG_TASKS),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(text(TextKey.VERSION, version), style = MaterialTheme.typography.bodySmall)
                OutlinedButton(
                    onClick = { showImpressum = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.Info, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(text(TextKey.LEGAL_NOTICE))
                }
            }
        }
        item {
            OutlinedButton(
                onClick = onSignOut,
                enabled = !state.pending && !busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(text(TextKey.SIGN_OUT))
            }
        }
        if (state.pending)
            item {
                Text(
                    text(TextKey.PLEASE_SYNCHRONIZE_PENDING_CHANGES_BEFORE_SIGNING_OUT),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        item { EndOfPageFooter(IllustrationRole.AFFECTION) }
    }
    if (showImpressum) ImpressumDialog { showImpressum = false }
}

@Composable
private fun TaskEditor(
    existing: TaskRevision?,
    children: List<Child>,
    parents: List<Parent>,
    today: LocalDate,
    busy: Boolean,
    offline: Boolean,
    saveError: String?,
    selection: TaskEditSelection? = null,
    initialDate: LocalDate? = null,
    syncError: String?,
    onRetrySync: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (TaskRevision, Set<String>) -> Unit,
) {
    val formats = rememberDateTimeFormats()
    val effective = today
    val single = selection?.scope == OperationScope.SINGLE
    val id = rememberSaveable { UUID.randomUUID().toString() }
    val taskId = rememberSaveable { existing?.taskId ?: UUID.randomUUID().toString() }
    var title by rememberSaveable { mutableStateOf(existing?.title ?: "") }
    var notes by rememberSaveable { mutableStateOf(existing?.notes ?: "") }
    var childId by rememberSaveable {
        mutableStateOf(existing?.childId ?: children.firstOrNull { !it.archived }?.id.orEmpty())
    }
    var selectedChildren by remember { mutableStateOf(setOf(childId)) }
    var responsibleParent by rememberSaveable { mutableStateOf(existing?.responsibleParentId) }
    var frequency by rememberSaveable {
        mutableStateOf(if (single) Frequency.ONCE else existing?.frequency ?: Frequency.DAILY)
    }
    var weekdays by rememberSaveable { mutableStateOf(existing?.weekdays ?: emptySet()) }
    var intervalText by rememberSaveable { mutableStateOf((existing?.interval ?: 1).toString()) }
    var times by rememberSaveable {
        mutableStateOf(
            if (single) listOf(requireNotNull(selection).occurrence.time)
            else existing?.times ?: listOf(LocalTime.of(8, 0))
        )
    }
    var start by rememberSaveable {
        mutableStateOf(
            if (single) requireNotNull(selection).occurrence.date
            else
                existing?.start?.let {
                    if (existing.frequency == Frequency.ONCE && it < effective) effective else it
                } ?: initialDate ?: effective
        )
    }
    var end by rememberSaveable { mutableStateOf(existing?.end) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
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
                            if (existing == null) text(TextKey.NEW_TASK)
                            else text(TextKey.EDIT_TASK)
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
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            AppIllustration(IllustrationRole.HELPER, Modifier.size(56.dp))
                            Text(
                                text(TextKey.ROOM_FOR_A_LITTLE_EVERYDAY_HELPER),
                                style = MaterialTheme.typography.titleMedium,
                                color = AppTheme.colors.primary,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    if (existing != null)
                        item {
                            Text(
                                text(
                                    TextKey
                                        .APPLIES_TO_CONFIRMATIONS_AND_STARTED_RECURRING_OCCURRENCES_ARE_PRESERVED,
                                    if (single) text(TextKey.ONLY_THIS_OCCURRENCE_BEA118)
                                    else text(TextKey.THIS_AND_ALL_FOLLOWING_OCCURRENCES_8EB5E8),
                                )
                            )
                        }
                    item {
                        DictationField(
                            title,
                            { title = it },
                            text(TextKey.TASK),
                            Modifier.fillMaxWidth(),
                            enabled = !busy,
                        )
                    }
                    item {
                        Text(text(TextKey.WHICH_AREA), style = MaterialTheme.typography.titleSmall)
                    }
                    item {
                        Row(
                            Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (existing == null) {
                                val ids =
                                    children
                                        .filter { !it.archived && it.id != "haushalt" }
                                        .map { it.id }
                                        .toSet()
                                if (ids.isNotEmpty())
                                    FilterChip(
                                        selectedChildren == ids,
                                        {
                                            selectedChildren = ids
                                            childId = ids.first()
                                        },
                                        label = { Text(text(TextKey.ALL_CHILDREN)) },
                                    )
                            }
                            children
                                .filter { !it.archived || it.id == existing?.childId }
                                .forEach { child ->
                                    AppChildChip(
                                        child,
                                        if (existing == null) child.id in selectedChildren
                                        else childId == child.id,
                                    ) {
                                        if (existing != null) childId = child.id
                                        else {
                                            selectedChildren =
                                                if (child.id == "haushalt") setOf("haushalt")
                                                else if (child.id in selectedChildren)
                                                    selectedChildren - child.id
                                                else selectedChildren - "haushalt" + child.id
                                            childId = selectedChildren.firstOrNull().orEmpty()
                                        }
                                    }
                                }
                        }
                    }
                    item { AssignmentPicker(parents, responsibleParent) { responsibleParent = it } }
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
                    item {
                        Text(text(TextKey.RECURRENCE), style = MaterialTheme.typography.titleSmall)
                    }
                    if (!single)
                        items(Frequency.entries) { value ->
                            Row(
                                Modifier.fillMaxWidth().clickable { frequency = value },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(frequency == value, { frequency = value })
                                Text(
                                    when (value) {
                                        Frequency.DAILY -> text(TextKey.DAYS_E94F59)
                                        Frequency.WEEKS -> text(TextKey.WEEKS)
                                        Frequency.MONTHS -> text(TextKey.MONTHS)
                                        else -> frequencyLabel(value)
                                    }
                                )
                            }
                        }
                    if (
                        !single &&
                            frequency in setOf(Frequency.DAILY, Frequency.WEEKS, Frequency.MONTHS)
                    )
                        item {
                            OutlinedTextField(
                                intervalText,
                                { value ->
                                    if (value.length <= 4 && value.all { it in '0'..'9' })
                                        intervalText = value
                                },
                                label = { Text(text(TextKey.ALL)) },
                                supportingText = {
                                    Text(
                                        when (frequency) {
                                            Frequency.WEEKS -> text(TextKey.WEEKS_1_TO_1000)
                                            Frequency.MONTHS -> text(TextKey.MONTHS_1_TO_1000)
                                            else -> text(TextKey.DAYS_1_TO_1000)
                                        }
                                    )
                                },
                                keyboardOptions =
                                    KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                enabled = !busy,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Text(
                                text(TextKey.THE_START_DATE_DETERMINES_THE_RHYTHM),
                                style = MaterialTheme.typography.bodySmall,
                            )
                            if (frequency == Frequency.MONTHS)
                                Text(
                                    text(
                                        TextKey
                                            .IF_THE_DAY_DOES_NOT_EXIST_IN_A_MONTH_THE_LAST_DAY_OF_THE_MONTH_IS_USED
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                        }
                    if (frequency == Frequency.WEEKDAYS)
                        item {
                            Column {
                                DayOfWeek.values().toList().chunked(4).forEach { days ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        days.forEach { day ->
                                            FilterChip(
                                                day in weekdays,
                                                {
                                                    weekdays =
                                                        if (day in weekdays) weekdays - day
                                                        else weekdays + day
                                                },
                                                label = {
                                                    Text(
                                                        day.getDisplayName(
                                                            java.time.format.TextStyle.SHORT,
                                                            formats.locale,
                                                        )
                                                    )
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    item {
                        if (single)
                            TimePickerField(
                                text(TextKey.TIME),
                                times.single(),
                                { times = listOf(it) },
                                enabled = !busy,
                            )
                        else TimeSlotsPicker(times, { times = it }, enabled = !busy)
                    }
                    item {
                        DatePickerField(
                            if (frequency == Frequency.ONCE) text(TextKey.DATE)
                            else text(TextKey.START_DATE),
                            start,
                            { it?.let { chosen -> start = chosen } },
                            enabled = !busy,
                            minDate =
                                if (single) null
                                else if (existing == null) today
                                else if (frequency == Frequency.ONCE) effective else null,
                        )
                    }
                    if (frequency != Frequency.ONCE)
                        item {
                            DatePickerField(
                                text(TextKey.END_DATE_OPTIONAL),
                                end,
                                { end = it },
                                optional = true,
                                enabled = !busy,
                                minDate = maxOf(start, effective),
                            )
                        }
                    error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
                    saveError?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
                    if (syncError != null)
                        item {
                            Text(syncError, color = MaterialTheme.colorScheme.error)
                            TextButton(onClick = onRetrySync, enabled = !busy) {
                                Text(text(TextKey.TRY_AGAIN))
                            }
                        }
                    else if (offline) item { Text(text(TextKey.EDITING_REQUIRES_A_CONNECTION)) }
                    item {
                        Button(
                            enabled =
                                !busy && !offline && childId.isNotBlank() && children.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                try {
                                    val revision =
                                        TaskRevision(
                                            id,
                                            taskId,
                                            childId,
                                            title.trim(),
                                            notes.trim(),
                                            frequency,
                                            weekdays,
                                            times.sorted(),
                                            start,
                                            if (frequency == Frequency.ONCE) null else end,
                                            effective,
                                            responsibleParentId = responsibleParent,
                                            interval =
                                                if (
                                                    frequency in
                                                        setOf(
                                                            Frequency.DAILY,
                                                            Frequency.WEEKS,
                                                            Frequency.MONTHS,
                                                        )
                                                )
                                                    intervalText.toIntOrNull()
                                                        ?: throw IllegalArgumentException(
                                                            text(
                                                                TextKey
                                                                    .PLEASE_ENTER_AN_INTERVAL_FROM_1_TO_1000
                                                            )
                                                        )
                                                else 1,
                                        )
                                    revision.validate()
                                    require(
                                        single ||
                                            revision.frequency != Frequency.ONCE ||
                                            revision.start >= effective
                                    ) {
                                        text(
                                            TextKey
                                                .THE_DATE_MUST_BE_ON_OR_AFTER_THE_EFFECTIVE_START_DATE
                                        )
                                    }
                                    requireLocalized(existing != null || revision.start >= today) {
                                        message(TextKey.NEW_TASKS_START_NO_EARLIER_THAN_TODAY)
                                    }
                                    require(revision.end == null || revision.end >= effective) {
                                        text(
                                            TextKey.THE_END_DATE_IS_BEFORE_THE_EFFECTIVE_START_DATE
                                        )
                                    }
                                    error = null
                                    onSave(
                                        revision,
                                        if (existing == null) selectedChildren else setOf(childId),
                                    )
                                } catch (e: Exception) {
                                    error = e.message ?: text(TextKey.PLEASE_CHECK_YOUR_ENTRIES)
                                }
                            },
                        ) {
                            Text(if (busy) text(TextKey.SAVING_6B432E) else text(TextKey.SAVE_TASK))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun YellowAddButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        icon = { Icon(Icons.Default.Add, null) },
        text = { Text(label, fontWeight = FontWeight.Bold) },
        containerColor = StatusColors.yellow,
        contentColor = AppTheme.colors.ink,
        shape = RoundedCornerShape(22.dp),
    )
}
