@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package de.kinderalltag.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.kinderalltag.data.FamilyState
import de.kinderalltag.domain.*
import de.kinderalltag.notifications.ReminderScheduler
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

private val green = GardenColors.Green
private val dayFormat = DateTimeFormatter.ofPattern("EEEE, d. MMMM", Locale.GERMAN)
private val stampFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy 'um' HH:mm", Locale.GERMAN)

@Preview(name = "Heute", showBackground = true, widthDp = 390, heightDp = 820)
@Composable
private fun TagesgartenPreview() {
    KinderTheme {
        val date = LocalDate.of(2026, 11, 16)
        val children = listOf(Child("ferdinand", "Ferdinand"), Child("johann", "Johann"))
        val routines = listOf(
            TaskRevision("preview-1", "task-1", "ferdinand", "Rucksack packen", start = date, effective = date),
            TaskRevision("preview-2", "task-2", "johann", "Zähne putzen", start = date, effective = date),
        )
        val occurrence = Schedule.occurrences(routines, date).first()
        val state = FamilyState(children = children, revisions = routines,
            events = listOf(CareEvent("preview-event", occurrence.id, date, EventKind.COMPLETE, "preview", "Mama",
                date.atTime(8, 5).atZone(FAMILY_ZONE).toInstant().toEpochMilli())))
        GardenBackground(Modifier.fillMaxSize()) { Checklist(state, date, date, null) {} }
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
    signingIn: Boolean, loginError: String?, onSignIn: () -> Unit, onSignOut: () -> Unit) = KinderTheme {
    val state by vm.state.collectAsStateWithLifecycle()
    val today by vm.today.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
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
                if (state.uid == null) {
                    Button(onClick = onSignIn, enabled = !signingIn) { Text(if (signingIn) "Anmelden …" else "Mit Google anmelden") }
                    loginError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                } else {
                    Text("Hallo ${state.name}. Dein Familienzugang muss noch freigeschaltet werden. Diese Nutzer-ID wird dafür benötigt:")
                    androidx.compose.foundation.text.selection.SelectionContainer { Text(state.uid.orEmpty()) }
                    Text("Die Einrichtung ist in docs/SETUP.md beschrieben. Nach der Freischaltung öffnet sich die Checkliste automatisch.")
                    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    TextButton(onClick = onSignOut) { Text("Abmelden") }
                }
            }
        }
        return@KinderTheme
    }
    var tab by rememberSaveable { mutableIntStateOf(if (initialDate != null) 2 else 0) }
    var historyDate by rememberSaveable { mutableStateOf(initialDate ?: today.toString()) }
    var selectedChild by rememberSaveable { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<TaskRevision?>(null) }
    var editorOpen by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Occurrence?>(null) }
    var openedInitial by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.revisions, initialOccurrence) {
        if (!openedInitial && initialOccurrence != null) {
            val date = runCatching { LocalDate.parse(initialDate) }.getOrDefault(today)
            Schedule.occurrences(state.revisions, date).firstOrNull { it.id == initialOccurrence }?.let {
                selected = it; openedInitial = true
            }
        }
    }
    val nav = listOf("Heute", "Aufgaben", "Verlauf", "Einstellungen")
    val icons = listOf(Icons.Default.WbSunny, Icons.Default.LocalFlorist, Icons.Default.History, Icons.Default.Settings)
    val navColors = listOf(GardenColors.Green, GardenColors.Blue, GardenColors.Burgundy, GardenColors.Coral)
    GardenBackground(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snack) },
            topBar = { TopAppBar(title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GardenDrawing(GardenMotif.SPROUT, Modifier.size(34.dp))
                    Text("Kinderalltag", fontWeight = FontWeight.ExtraBold, color = green)
                }
            }) },
            bottomBar = {
                NavigationBar(containerColor = GardenColors.Cream, tonalElevation = 0.dp) { nav.forEachIndexed { index, label ->
                    NavigationBarItem(selected = tab == index, onClick = { tab = index },
                        icon = { Icon(icons[index], null) }, label = { Text(label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = navColors[index], selectedTextColor = navColors[index],
                            indicatorColor = navColors[index].copy(alpha = 0.12f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant))
                } }
            },
            floatingActionButton = {
                if (tab <= 1) ExtendedFloatingActionButton(
                    onClick = { editing = null; editorOpen = true }, icon = { Icon(Icons.Default.Add, null) },
                    text = { Text("Aufgabe", fontWeight = FontWeight.Bold) },
                    containerColor = GardenColors.Mustard, contentColor = GardenColors.Ink,
                    shape = RoundedCornerShape(22.dp))
            },
        ) { padding ->
            Column(Modifier.padding(padding).fillMaxSize()) {
                if (state.fromCache || state.pending || state.error != null) {
                    Text(state.error ?: if (state.pending) "Änderungen werden synchronisiert …" else "Gespeicherter Stand · Verbindung wird hergestellt",
                        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer).padding(12.dp),
                        style = MaterialTheme.typography.bodySmall)
                }
                when (tab) {
                    0, 2 -> {
                        val date = if (tab == 0) today else runCatching { LocalDate.parse(historyDate) }.getOrDefault(today)
                        if (tab == 2) Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            IconButton(onClick = { historyDate = date.minusDays(1).toString() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Vorheriger Tag")
                            }
                            TextButton(onClick = { historyDate = today.toString() }) { Text("Zu heute") }
                            IconButton(onClick = { historyDate = date.plusDays(1).toString() }, enabled = date < today) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, "Nächster Tag")
                            }
                        }
                        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selectedChild == null, { selectedChild = null }, label = { Text("Alle") })
                            state.sections.forEach { child -> GardenChildChip(child, selectedChild == child.id) { selectedChild = child.id } }
                        }
                        Checklist(state, date, today, selectedChild, onOpen = { selected = it })
                    }
                    1 -> TaskList(state, today, busy, onEdit = { editing = it; editorOpen = true }, onArchive = vm::archive)
                    3 -> Settings(state, busy, onSignOut, vm)
                }
            }
        }
    }
    if (editorOpen) TaskEditor(editing, state.sections, today, busy, state.fromCache,
        onDismiss = { if (!busy) editorOpen = false },
        onSave = { vm.save(it) { editorOpen = false } })
    selected?.let { occurrence -> OccurrenceDialog(occurrence, state, today,
        onDismiss = { selected = null }, onComplete = { vm.complete(occurrence) }, onUndo = { vm.undo(occurrence) }) }
}

@Composable
private fun Checklist(state: FamilyState, date: LocalDate, today: LocalDate, childId: String?, onOpen: (Occurrence) -> Unit) {
    val due = Schedule.occurrences(state.revisions, date).filter { childId == null || it.revision.childId == childId }
    val done = due.count { Schedule.activeCompletions(state.events, it.id).isNotEmpty() }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(if (date == today) "Heute" else "Verlauf",
                    style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(date.format(dayFormat), style = MaterialTheme.typography.bodyMedium)
                if (due.isNotEmpty()) {
                    Text("$done von ${due.size} Aufgaben erledigt", color = green,
                        style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        if (state.children.isEmpty()) item { EmptyCard("Die Kinderprofile werden geladen. Falls sie fehlen: Einrichtung in docs/SETUP.md abschließen.") }
        else if (due.isEmpty()) item { EmptyCard("Für diesen Tag stehen keine Aufgaben an.") }
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
                                Text(occurrence.time.toString(), style = MaterialTheme.typography.bodySmall)
                                Text(if (completed.isEmpty()) {
                                    if (date < today) "Nicht bestätigt" else "Noch offen"
                                } else completed.joinToString(" · ") { completionText(it) }, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun completionText(event: CareEvent): String {
    val whenDone = Instant.ofEpochMilli(event.clientMillis).atZone(FAMILY_ZONE)
    return "${event.authorName} · ${whenDone.format(stampFormat)}${if (event.pending) " · ausstehend" else ""}"
}

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
private fun TaskList(state: FamilyState, today: LocalDate, busy: Boolean, onEdit: (TaskRevision) -> Unit, onArchive: (TaskRevision) -> Unit) {
    val tasks = Schedule.revisionsOn(state.revisions, today.plusDays(1)).sortedBy { it.title }
    var archive by remember { mutableStateOf<TaskRevision?>(null) }
    LazyColumn(contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 100.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { GardenBanner("Eure Alltagshelfer", "Kleine Routinen, die euren Tag begleiten.") }
        item { Text("Änderungen und Pausen gelten ab morgen. Der heutige Tag und der Verlauf bleiben erhalten.") }
        if (tasks.isEmpty()) item { EmptyCard("Legt eure erste Aufgabe mit + Aufgabe an.") }
        items(tasks, key = { it.taskId }) { task ->
            val child = state.sections.firstOrNull { it.id == task.childId } ?: Child(task.childId, "Bereich")
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(
                containerColor = childPaper(child), contentColor = GardenColors.Ink)) {
                Column(Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ChildBadge(child)
                        Text(task.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f))
                    }
                    Text("${state.sections.firstOrNull { it.id == task.childId }?.name ?: "Bereich"} · ${frequencyLabel(task.frequency)}")
                    Text(task.times.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
                    if (!task.active) Text(if (task.effective > today) "Ab morgen pausiert" else "Pausiert", color = MaterialTheme.colorScheme.secondary)
                    else if (task.effective > today) Text("Neue Fassung ab morgen", color = green)
                    Row {
                        TextButton(onClick = { onEdit(task) }, enabled = !busy && !state.fromCache) { Text(if (task.active) "Bearbeiten" else "Fortsetzen") }
                        if (task.active) TextButton(onClick = { archive = task }, enabled = !busy && !state.fromCache) { Text("Pausieren") }
                    }
                }
            }
        }
    }
    archive?.let { task -> AlertDialog(onDismissRequest = { archive = null }, title = { Text("Ab morgen pausieren?") },
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
    onDismiss: () -> Unit, onComplete: () -> Unit, onUndo: () -> Unit) {
    val completed = Schedule.activeCompletions(state.events, occurrence.id)
    val events = state.events.filter { it.occurrenceId == occurrence.id }.sortedBy { it.serverMillis ?: it.clientMillis }
    var confirmingUndo by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(occurrence.revision.title) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { Text("${state.sections.firstOrNull { it.id == occurrence.revision.childId }?.name.orEmpty()} · ${occurrence.date.format(dayFormat)} · ${occurrence.time}") }
                if (occurrence.revision.notes.isNotBlank()) item { Text(occurrence.revision.notes) }
                if (state.fromCache) item { Text("Gespeicherter Stand: Bestätigungen vom anderen Telefon sind möglicherweise noch nicht sichtbar.") }
                if (completed.isEmpty()) item { Text(if (occurrence.date < today) "Für diesen Tag nicht bestätigt. Eine nachträgliche Bestätigung zeichnet den jetzigen Zeitpunkt auf." else "Noch nicht bestätigt.") }
                if (events.isNotEmpty()) item { Text("Bestätigungen & Korrekturen", fontWeight = FontWeight.SemiBold) }
                items(events, key = { it.id }) { event ->
                    Text("${if (event.kind == EventKind.COMPLETE) "Bestätigt" else "Bestätigung zurückgenommen"}\n${completionText(event)}",
                        style = MaterialTheme.typography.bodySmall)
                    if (event.serverMillis != null && event.serverMillis - event.clientMillis > 60_000)
                        Text("Synchronisiert: ${Instant.ofEpochMilli(event.serverMillis).atZone(FAMILY_ZONE).format(stampFormat)}",
                            style = MaterialTheme.typography.bodySmall)
                }
                if (confirmingUndo) item { Text("Alle aktuellen Bestätigungen für diese Ausführung zurücknehmen? Die Korrektur bleibt im Verlauf sichtbar.") }
            }
        },
        confirmButton = {
            if (completed.isEmpty()) TextButton(onClick = onComplete, enabled = state.error == null) { Text("Als erledigt bestätigen") }
            else TextButton(onClick = {
                if (confirmingUndo) { onUndo(); confirmingUndo = false } else confirmingUndo = true
            }) { Text(if (confirmingUndo) "Ja, zurücknehmen" else "Korrigieren") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Schließen") } })
}

@Composable
private fun Settings(state: FamilyState, busy: Boolean, onSignOut: () -> Unit, vm: FamilyViewModel) {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(ReminderScheduler.enabled(context)) }
    var rename by remember { mutableStateOf<Child?>(null) }
    var name by remember { mutableStateOf("") }
    var bearColor by remember { mutableStateOf(BearColor.BLUE) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        enabled = granted; ReminderScheduler.setEnabled(context, granted)
        if (granted) ReminderScheduler.update(context, vm.state.value)
    }
    LazyColumn(contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item { GardenBanner("Euer Familiengarten", "Hier macht ihr Kinderalltag zu eurem Begleiter.") }
        item { Text("Angemeldet als ${state.name}") }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Erinnerungen auf diesem Telefon", Modifier.weight(1f))
                Switch(enabled, onCheckedChange = { value ->
                    if (value && Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                        permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else {
                        enabled = value; ReminderScheduler.setEnabled(context, value)
                        if (value) ReminderScheduler.update(context, state)
                    }
                })
            }
        }
        item { Text("Eine Erinnerung pro Uhrzeit. Android kann die Zustellung verzögern. Öffnet die Aufgabe, um den gemeinsamen Status zu prüfen.", style = MaterialTheme.typography.bodySmall) }
        item { Text("Kinder", style = MaterialTheme.typography.titleLarge) }
        items(state.children, key = { it.id }) { child ->
            OutlinedButton(onClick = { rename = child; name = child.name; bearColor = child.bearColor }, enabled = !busy && !state.fromCache,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = childColor(child)),
                modifier = Modifier.fillMaxWidth()) {
                ChildIcon(child, Modifier.padding(end = 8.dp).size(32.dp))
                Text("${child.name} · Name & Bärenfarbe")
            }
        }
        item { Text("Familienzeitzone: Europe/Berlin\nTageswechsel und Erinnerungen richten sich auch auf Reisen danach.", style = MaterialTheme.typography.bodySmall) }
        item { TextButton(onClick = onSignOut, enabled = !state.pending && !busy) { Text("Abmelden") } }
        if (state.pending) item { Text("Vor dem Abmelden bitte ausstehende Änderungen synchronisieren.") }
    }
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
private fun TaskEditor(existing: TaskRevision?, children: List<Child>, today: LocalDate,
    busy: Boolean, offline: Boolean, onDismiss: () -> Unit, onSave: (TaskRevision) -> Unit) {
    val effective = if (existing == null) today else today.plusDays(1)
    val id = remember { UUID.randomUUID().toString() }
    val taskId = remember { existing?.taskId ?: UUID.randomUUID().toString() }
    var title by rememberSaveable { mutableStateOf(existing?.title ?: "") }
    var notes by rememberSaveable { mutableStateOf(existing?.notes ?: "") }
    var childId by rememberSaveable { mutableStateOf(existing?.childId ?: children.firstOrNull()?.id.orEmpty()) }
    var frequency by remember { mutableStateOf(existing?.frequency ?: Frequency.DAILY) }
    var weekdays by remember { mutableStateOf(existing?.weekdays ?: emptySet()) }
    var times by rememberSaveable { mutableStateOf(existing?.times?.joinToString(", ") ?: "08:00") }
    var start by rememberSaveable { mutableStateOf((existing?.start?.let {
        if (existing.frequency == Frequency.ONCE && it < effective) effective else it
    } ?: effective).toString()) }
    var end by rememberSaveable { mutableStateOf(existing?.end?.toString().orEmpty()) }
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
                    if (existing != null) item { Text("Diese Fassung gilt ab morgen (${effective}). Heute bleibt unverändert.") }
                    item { OutlinedTextField(title, { title = it }, label = { Text("Aufgabe") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
                    item { Text("Für welchen Bereich?", style = MaterialTheme.typography.titleSmall) }
                    item { Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        children.forEach { child -> GardenChildChip(child, childId == child.id) { childId = child.id } }
                    } }
                    item { OutlinedTextField(notes, { notes = it }, label = { Text("Notizen (optional)") }, modifier = Modifier.fillMaxWidth(), minLines = 2) }
                    item { Text("Wiederholung", style = MaterialTheme.typography.titleSmall) }
                    items(Frequency.entries) { value ->
                        Row(Modifier.fillMaxWidth().clickable { frequency = value }, verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(frequency == value, { frequency = value }); Text(frequencyLabel(value))
                        }
                    }
                    if (frequency == Frequency.WEEKDAYS) item {
                        Column { DayOfWeek.values().toList().chunked(4).forEach { days ->
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { days.forEach { day ->
                                FilterChip(day in weekdays, { weekdays = if (day in weekdays) weekdays - day else weekdays + day },
                                    label = { Text(day.getDisplayName(java.time.format.TextStyle.SHORT, Locale.GERMAN)) })
                            } }
                        } }
                    }
                    item { OutlinedTextField(times, { times = it }, label = { Text("Uhrzeiten") }, supportingText = { Text("Zum Beispiel 08:00, 18:30 · bis zu 8 Uhrzeiten") }, modifier = Modifier.fillMaxWidth()) }
                    item { OutlinedTextField(start, { start = it }, label = { Text(if (frequency == Frequency.ONCE) "Datum (JJJJ-MM-TT)" else "Startdatum (JJJJ-MM-TT)") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
                    if (frequency != Frequency.ONCE) item { OutlinedTextField(end, { end = it }, label = { Text("Enddatum (optional, JJJJ-MM-TT)") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
                    error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
                    if (offline) item { Text("Zum Bearbeiten ist eine Verbindung erforderlich.") }
                    item {
                        Button(enabled = !busy && !offline && children.isNotEmpty(), modifier = Modifier.fillMaxWidth(), onClick = {
                            try {
                                val revision = TaskRevision(id, taskId, childId, title.trim(), notes.trim(), frequency,
                                    weekdays, times.split(',').map { LocalTime.parse(it.trim(), DateTimeFormatter.ofPattern("H:mm").withResolverStyle(java.time.format.ResolverStyle.STRICT)) }.sorted(),
                                    LocalDate.parse(start.trim()), if (frequency == Frequency.ONCE || end.isBlank()) null else LocalDate.parse(end.trim()), effective)
                                revision.validate()
                                require(revision.frequency != Frequency.ONCE || revision.start >= effective) { "Das Datum muss am oder nach dem Gültigkeitsbeginn liegen." }
                                require(existing != null || revision.start >= today) { "Neue Aufgaben beginnen frühestens heute." }
                                require(revision.end == null || revision.end >= effective) { "Das Enddatum liegt vor dem Gültigkeitsbeginn." }
                                error = null; onSave(revision)
                            } catch (e: Exception) {
                                error = if (e is java.time.format.DateTimeParseException) "Bitte Datum und Uhrzeiten im angegebenen Format eingeben." else e.message ?: "Bitte Angaben prüfen."
                            }
                        }) { Text(if (busy) "Wird gespeichert …" else "Aufgabe speichern") }
                    }
                }
            }
        }
    }
}
