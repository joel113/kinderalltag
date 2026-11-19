package de.kinderalltag.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.kinderalltag.data.FamilyState
import de.kinderalltag.domain.*
import java.util.UUID

internal data class ListEditTarget(val kind: FamilyListKind, val item: FamilyListItem? = null)

internal class FamilyListUiState {
    var editor by mutableStateOf<ListEditTarget?>(null)
    var voiceImportOpen by mutableStateOf(false)
    var selected by mutableStateOf<FamilyListItem?>(null)
    val thoughtIndices = mutableStateMapOf<FamilyListKind, Int>()

    fun nextThought(kind: FamilyListKind) {
        thoughtIndices[kind] = (thoughtIndices[kind] ?: -1) + 1
    }
}

internal fun listTitle(kind: FamilyListKind) =
    if (kind == FamilyListKind.SHOPPING) "Einkaufsliste" else "Geschenkideen"

internal fun labelGroupTitle(group: LabelGroup) =
    when (group) {
        LabelGroup.SHOPPING -> "Einkaufslabels"
        LabelGroup.OCCASION -> "Geschenkanlässe"
        LabelGroup.GIVER -> "Schenkende"
    }

private val shoppingThoughts =
    listOf(
        "Milch, Brot und ein bisschen Vorfreude: Gemeinsam bleibt der Kopf frei für eure Familienzeit.",
        "Die kleinen Besorgungen stehen hier. Platz im Kopf bleibt für ein gemeinsames Abenteuer.",
        "Ein gut gefüllter Einkaufskorb und Zeit für die Menschen, die euch am Herzen liegen.",
    )
private val giftThoughts =
    listOf(
        "Die schönsten Geschenkideen beginnen mit einem Gedanken an einen lieben Menschen.",
        "Ein kleines Geschenk, ein großes Strahlen: Hier dürfen eure Ideen wachsen.",
        "Manche Ideen brauchen nur einen Platz, bis der richtige Moment zum Schenken kommt.",
    )

@Composable
internal fun FamilyListPage(
    kind: FamilyListKind,
    state: FamilyState,
    vm: FamilyViewModel,
    showIntroduction: Boolean,
) {
    var labels by rememberSaveable(kind) { mutableStateOf(arrayListOf<String>()) }
    var child by rememberSaveable(kind) { mutableStateOf<String?>(null) }
    var showBought by rememberSaveable(kind) { mutableStateOf(false) }
    val thoughts = if (kind == FamilyListKind.SHOPPING) shoppingThoughts else giftThoughts
    val thought = thoughts[(vm.listUi.thoughtIndices[kind] ?: 0) % thoughts.size]
    val busy by vm.busy.collectAsStateWithLifecycle()
    fun visible(bought: Boolean) =
        listItems(state.listItems, state.listActivity, kind, bought).filter {
            (kind == FamilyListKind.SHOPPING || it.recipient?.matches(child) == true) &&
                matchesListLabels(it, labels.toSet(), state.listLabels)
        }
    val open = visible(false)
    val bought = visible(true)
    LazyColumn(
        contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (showIntroduction)
            item {
                GardenBanner(
                    if (kind == FamilyListKind.SHOPPING) "Eure Einkaufsliste"
                    else "Eure Geschenkideen",
                    thought,
                )
            }
        if (kind == FamilyListKind.GIFTS)
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(child == null, { child = null }, label = { Text("Alle") })
                    state.children.forEach { kid ->
                        FilterChip(child == kid.id, { child = kid.id }, label = { Text(kid.name) })
                    }
                }
            }
        val applicable =
            state.listLabels.filter { label ->
                (if (kind == FamilyListKind.SHOPPING) label.group == LabelGroup.SHOPPING
                else label.group != LabelGroup.SHOPPING) &&
                    (label.active ||
                        state.listItems.any {
                            label.id in it.labelIds && !it.deleted && it.kind == kind
                        })
            }
        if (applicable.isNotEmpty())
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Nach Labels filtern", style = MaterialTheme.typography.labelLarge)
                    LabelGroup.entries.forEach { group ->
                        val groupLabels = applicable.filter { it.group == group }
                        if (groupLabels.isNotEmpty()) {
                            Text(
                                labelGroupTitle(group),
                                style = MaterialTheme.typography.labelMedium,
                            )
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                groupLabels
                                    .sortedBy { it.name }
                                    .forEach { label ->
                                        LabelChip(label, label.id in labels) {
                                            labels =
                                                ArrayList(
                                                    if (label.id in labels) labels - label.id
                                                    else labels + label.id
                                                )
                                        }
                                    }
                            }
                        }
                    }
                    if (labels.isNotEmpty())
                        TextButton(onClick = { labels = arrayListOf() }) {
                            Text("Alle Labels anzeigen")
                        }
                }
            }
        item { Text("${open.size} offen", fontWeight = FontWeight.SemiBold) }
        if (open.isEmpty())
            item {
                Text(
                    if (kind == FamilyListKind.SHOPPING)
                        "Hier ist alles besorgt. Neue Einkäufe könnt ihr mit + Einkauf ergänzen."
                    else "Hier ist Platz für eure nächsten Geschenkideen."
                )
            }
        items(open, key = { it.id }) { item ->
            FamilyListCard(
                item,
                state,
                busy,
                onOpen = { vm.listUi.selected = item },
                onBought = { vm.buy(item, true) },
            )
        }
        item {
            TextButton(onClick = { showBought = !showBought }) {
                Text("Gekauft (${bought.size}) ${if (showBought) "ausblenden" else "anzeigen"}")
            }
        }
        if (showBought)
            items(bought, key = { "bought:${it.id}" }) { item ->
                FamilyListCard(
                    item,
                    state,
                    busy,
                    onOpen = { vm.listUi.selected = item },
                    onBought = { vm.buy(item, false) },
                )
            }
        item {
            EndOfPageFooter(
                if (kind == FamilyListKind.SHOPPING) GardenMotif.SPROUT else GardenMotif.HEART
            )
        }
    }
}

@Composable
internal fun LabelChip(label: FamilyLabel, selected: Boolean, onClick: () -> Unit) {
    val color = if (label.color == BearColor.HONEY) Color(0xFF865E12) else Color(label.color.argb)
    FilterChip(
        selected,
        onClick,
        label = { Text(label.name + if (!label.active) " (archiviert)" else "") },
        colors =
            FilterChipDefaults.filterChipColors(
                selectedContainerColor = color.copy(alpha = 0.12f),
                selectedLabelColor = color,
            ),
    )
}

@Composable
internal fun FamilyListCard(
    item: FamilyListItem,
    state: FamilyState,
    busy: Boolean,
    onOpen: () -> Unit,
    onBought: () -> Unit,
) {
    val bought = activePurchases(state.listActivity, item.id).isNotEmpty()
    Card(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (bought) GardenColors.Mint
                    else if (item.kind == FamilyListKind.SHOPPING) GardenColors.Sky
                    else GardenColors.Blush,
                contentColor = GardenColors.Ink,
            ),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Checkbox(
                bought,
                { onBought() },
                enabled = !busy && state.authorized,
                modifier =
                    Modifier.semanticsLabel(
                        "${item.title}: ${if (bought) "wieder öffnen" else "als gekauft markieren"}"
                    ),
            )
            Column(
                Modifier.weight(1f).padding(vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    item.title,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (bought) TextDecoration.LineThrough else null,
                )
                item.recipient?.let {
                    Text(
                        appointmentRecipientName(it, state.sections),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    state.listLabels
                        .filter { it.id in item.labelIds }
                        .forEach { label ->
                            val color =
                                if (label.color == BearColor.HONEY) Color(0xFF865E12)
                                else Color(label.color.argb)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = color.copy(alpha = 0.1f),
                            ) {
                                Text(
                                    label.name,
                                    Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = color,
                                )
                            }
                        }
                }
            }
            Icon(
                Icons.Default.ChevronRight,
                null,
                tint = GardenColors.Blue,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

private fun Modifier.semanticsLabel(label: String) = semantics { contentDescription = label }

internal fun LazyListScope.shoppingHome(state: FamilyState, vm: FamilyViewModel, busy: Boolean) {
    item {
        Row(
            Modifier.fillMaxWidth().padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Aktuelle Einkaufsliste",
                Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            FilledTonalButton(onClick = { vm.listUi.voiceImportOpen = true }) {
                Icon(Icons.Default.Add, null)
                Text("Einkauf")
            }
        }
    }
    val open = listItems(state.listItems, state.listActivity, FamilyListKind.SHOPPING, false)
    if (open.isEmpty()) item { Text("Alles besorgt – eure Einkaufsliste ist leer.") }
    items(open, key = { "shopping:${it.id}" }) { item ->
        FamilyListCard(item, state, busy, { vm.listUi.selected = item }, { vm.buy(item, true) })
    }
}

@Composable
internal fun FamilyListDialogs(state: FamilyState, vm: FamilyViewModel) {
    val ui = vm.listUi
    val saveError by vm.listError.collectAsStateWithLifecycle()
    LaunchedEffect(ui.editor, ui.selected) { vm.listError.value = null }
    val busy by vm.busy.collectAsStateWithLifecycle()
    ui.selected?.let { item ->
        val current = state.listItems.firstOrNull { it.id == item.id }
        val fresh = current != null && !current.deleted && current.version == item.version
        val canManage =
            fresh &&
                !busy &&
                state.loaded &&
                !state.fromCache &&
                !state.pending &&
                state.error == null
        var deleting by remember(item.id) { mutableStateOf(false) }
        ItemDetailsDialog(
            item.title,
            listTitle(item.kind),
            { ui.selected = null },
            busy,
            actions = {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailAction(
                        "Bearbeiten",
                        Icons.Default.Edit,
                        canManage,
                        onClick = {
                            ui.selected = null
                            ui.editor = ListEditTarget(item.kind, item)
                        },
                    )
                    DetailAction(
                        "Löschen",
                        Icons.Default.DeleteOutline,
                        canManage,
                        destructive = true,
                        onClick = { deleting = true },
                    )
                }
                OutlinedButton(
                    onClick = {
                        vm.buy(item, activePurchases(state.listActivity, item.id).isEmpty())
                    },
                    enabled = fresh && !busy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (activePurchases(state.listActivity, item.id).isEmpty())
                            "Als gekauft markieren"
                        else "Wieder öffnen"
                    )
                }
            },
        ) {
            DetailSection("Auf einen Blick") {
                item.recipient?.let {
                    DetailValue(
                        Icons.Default.Face,
                        "Für",
                        appointmentRecipientName(it, state.sections),
                    )
                }
                DetailValue(
                    Icons.Default.CheckCircle,
                    "Status",
                    if (activePurchases(state.listActivity, item.id).isEmpty()) "Offen"
                    else "Gekauft",
                )
                LabelGroup.entries.forEach { group ->
                    val names =
                        state.listLabels
                            .filter { it.id in item.labelIds && it.group == group }
                            .map { it.name + if (!it.active) " (archiviert)" else "" }
                    if (names.isNotEmpty())
                        DetailValue(
                            Icons.AutoMirrored.Filled.Label,
                            labelGroupTitle(group),
                            names.joinToString(" · "),
                        )
                }
            }
            if (item.notes.isNotBlank())
                DetailSection("Notizen", GardenColors.Cream) { Text(item.notes) }
            saveError?.let { DetailNotice(it) }
            if (!fresh)
                DetailNotice("Dieser Eintrag wurde geändert. Bitte schließen und neu öffnen.")
            if (state.fromCache)
                DetailNotice(
                    "Gespeicherter Stand. Kaufen und Wiederöffnen werden offline vorgemerkt; Bearbeiten und Löschen benötigen eine Verbindung."
                )
        }
        if (deleting)
            AlertDialog(
                onDismissRequest = { if (!busy) deleting = false },
                title = { Text("Eintrag löschen?") },
                text = {
                    Column {
                        Text(
                            "„${item.title}“ wird im Papierkorb aufbewahrt. Der Kaufstatus und der Verlauf bleiben erhalten."
                        )
                        saveError?.let { Text(it, color = GardenColors.Red) }
                    }
                },
                confirmButton = {
                    TextButton(
                        enabled = canManage,
                        onClick = {
                            vm.changeListItem(item, ListAction.DELETED) {
                                deleting = false
                                ui.selected = null
                            }
                        },
                    ) {
                        Text("Löschen", color = GardenColors.Red)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { deleting = false }, enabled = !busy) {
                        Text("Abbrechen")
                    }
                },
            )
    }
    if (ui.voiceImportOpen) ShoppingVoiceImportDialog(state, vm, busy)
    ui.editor?.let { target ->
        FamilyListEditor(target, state, busy, saveError, { ui.editor = null }) { draft ->
            vm.saveListItem(draft, target.item == null) { ui.editor = null }
        }
    }
}

@Composable
private fun FamilyListEditor(
    target: ListEditTarget,
    state: FamilyState,
    busy: Boolean,
    saveError: String?,
    onDismiss: () -> Unit,
    onSave: (FamilyListItem) -> Unit,
) {
    val existing = target.item
    val id = rememberSaveable { existing?.id ?: UUID.randomUUID().toString() }
    var title by rememberSaveable { mutableStateOf(existing?.title.orEmpty()) }
    var notes by rememberSaveable { mutableStateOf(existing?.notes.orEmpty()) }
    var recipient by rememberSaveable {
        mutableStateOf(existing?.recipient ?: AppointmentRecipient.BOTH_CHILDREN)
    }
    var selectedLabels by rememberSaveable {
        mutableStateOf(ArrayList(existing?.labelIds.orEmpty()))
    }
    var error by remember { mutableStateOf<String?>(null) }
    val stale =
        existing != null &&
            state.listItems
                .firstOrNull { it.id == existing.id }
                ?.let { it.version != existing.version || it.deleted } != false
    val canSave =
        !busy &&
            !stale &&
            (existing == null || (!state.fromCache && !state.pending && state.error == null))
    ItemDetailsDialog(
        if (existing == null)
            if (target.kind == FamilyListKind.SHOPPING) "Neuer Einkauf" else "Neue Geschenkidee"
        else "Eintrag bearbeiten",
        listTitle(target.kind),
        onDismiss,
        busy,
        actions = {
            Button(
                onClick = {
                    try {
                        val draft =
                            FamilyListItem(
                                id,
                                target.kind,
                                title.trim(),
                                notes.trim(),
                                if (target.kind == FamilyListKind.SHOPPING) null else recipient,
                                selectedLabels.toSet(),
                                existing?.version ?: 0,
                                lastActivityId = existing?.lastActivityId.orEmpty(),
                            )
                        draft.validate()
                        error = null
                        onSave(draft)
                    } catch (e: IllegalArgumentException) {
                        error = e.message
                    }
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (busy) "Speichert …" else "Speichern")
            }
        },
    ) {
        DictationField(title, { title = it }, "Titel", Modifier.fillMaxWidth(), enabled = !busy)
        DictationField(
            notes,
            { notes = it },
            "Notizen (optional)",
            Modifier.fillMaxWidth(),
            notes = true,
            enabled = !busy,
        )
        if (target.kind == FamilyListKind.GIFTS)
            DetailSection("Für wen?", GardenColors.Cream) {
                listOf(
                        AppointmentRecipient.FERDINAND,
                        AppointmentRecipient.JOHANN,
                        AppointmentRecipient.BOTH_CHILDREN,
                    )
                    .forEach { choice ->
                        FilterChip(
                            recipient == choice,
                            { recipient = choice },
                            label = { Text(appointmentRecipientName(choice, state.sections)) },
                        )
                    }
            }
        Text(
            "Bis zu 6 Labels insgesamt. Labels verwaltet ihr gemeinsam in den Einstellungen.",
            style = MaterialTheme.typography.bodySmall,
        )
        LabelGroup.entries
            .filter {
                if (target.kind == FamilyListKind.SHOPPING) it == LabelGroup.SHOPPING
                else it != LabelGroup.SHOPPING
            }
            .forEach { group ->
                DetailSection(labelGroupTitle(group), GardenColors.Cream) {
                    val labels =
                        state.listLabels
                            .filter { it.group == group && (it.active || it.id in selectedLabels) }
                            .sortedBy { it.name }
                    if (labels.isEmpty())
                        Text(
                            "Labels könnt ihr in den Einstellungen anlegen.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        labels.forEach { label ->
                            LabelChip(label, label.id in selectedLabels) {
                                selectedLabels =
                                    ArrayList(
                                        if (label.id in selectedLabels) selectedLabels - label.id
                                        else selectedLabels + label.id
                                    )
                            }
                        }
                    }
                }
            }
        saveError?.let { DetailNotice(it) }
        if (stale) DetailNotice("Dieser Eintrag wurde geändert. Bitte schließen und neu öffnen.")
        error?.let { DetailNotice(it) }
    }
}
