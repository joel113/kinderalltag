package de.kinderalltag.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.kinderalltag.data.FamilyState
import de.kinderalltag.domain.*
import java.util.UUID

@Composable
internal fun ListLabelSettings(state: FamilyState, vm: FamilyViewModel, busy: Boolean) {
    var open by remember { mutableStateOf(false) }
    SettingsSection("Labels", Icons.AutoMirrored.Filled.Label, GardenColors.Green) {
        Text("Gemeinsame Labels für eure Einkäufe, Geschenkanlässe und Schenkenden.")
        OutlinedButton(onClick = { open = true }) { Text("Labels verwalten") }
    }
    if (open) LabelManager(state, vm, busy) { open = false }
}

@Composable
private fun LabelManager(state: FamilyState, vm: FamilyViewModel, busy: Boolean, onDismiss: () -> Unit) {
    var group by remember { mutableStateOf(LabelGroup.SHOPPING) }
    var editing by remember { mutableStateOf<FamilyLabel?>(null) }
    var creating by remember { mutableStateOf(false) }
    val saveError by vm.listError.collectAsStateWithLifecycle()
    LaunchedEffect(group, editing) { vm.listError.value = null }
    val online = state.loaded && !state.fromCache && !state.pending && state.error == null && !busy
    ItemDetailsDialog("Labels verwalten", "Einstellungen", onDismiss, busy, actions = {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DetailAction("Neues Label", Icons.Default.Add, online, onClick = {
                editing = FamilyLabel(UUID.randomUUID().toString(), group, ""); creating = true
            })
            DetailAction("Vorschläge", Icons.Default.AutoAwesome, online, onClick = { vm.addListStarters(group) })
        }
    }) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LabelGroup.entries.forEach { value -> FilterChip(group == value, { group = value }, label = { Text(labelGroupTitle(value)) }) }
        }
        Text("Archivierte Labels bleiben an bestehenden Einträgen und im Verlauf erhalten. „Vorschläge“ ergänzt nur fehlende Startlabels.", style = MaterialTheme.typography.bodySmall)
        val labels = state.listLabels.filter { it.group == group }.sortedBy { it.name }
        if (labels.isEmpty()) Text("Noch keine Labels. Lege eigene an oder verwende die Vorschläge.")
        labels.forEach { label ->
            DetailSection(label.name + if (!label.active) " (archiviert)" else "", colorForLabel(label)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(enabled = online, onClick = { editing = label; creating = false }) { Text("Bearbeiten") }
                    TextButton(enabled = online, onClick = { vm.saveListLabel(label.copy(active = !label.active), false) {} }) { Text(if (label.active) "Archivieren" else "Aktivieren") }
                }
            }
        }
        saveError?.let { DetailNotice(it) }
        if (!online) DetailNotice("Zum Verwalten von Labels ist eine aktuelle Verbindung erforderlich.")
    }
    editing?.let { label -> key(label.id) {
        var name by remember { mutableStateOf(label.name) }
        var color by remember { mutableStateOf(label.color) }
        var error by remember { mutableStateOf<String?>(null) }
        val fresh = creating || state.listLabels.firstOrNull { it.id == label.id }?.version == label.version
        ItemDetailsDialog(if (creating) "Neues Label" else "Label bearbeiten", labelGroupTitle(label.group), { editing = null }, busy, actions = {
            Button(enabled = online && fresh, onClick = {
                try {
                    val updated = label.copy(name = name.trim(), color = color)
                    updated.validate()
                    require(state.listLabels.none { it.id != label.id && it.group == label.group && it.name.equals(updated.name, ignoreCase = true) }) { "Dieses Label gibt es bereits." }
                    vm.saveListLabel(updated, creating) { editing = null }
                } catch (e: IllegalArgumentException) { error = e.message }
            }, modifier = Modifier.fillMaxWidth()) { Text("Speichern") }
        }) {
            OutlinedTextField(name, { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Text("Farbe")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                BearColor.entries.forEach { choice ->
                    LabelChip(label.copy(name = when (choice) {
                        BearColor.BLUE -> "Blau"; BearColor.ROSE -> "Rosa"; BearColor.GREEN -> "Grün"
                        BearColor.HONEY -> "Gelb"; BearColor.BROWN -> "Braun"; BearColor.PURPLE -> "Lila"
                    }, color = choice), color == choice) { color = choice }
                }
            }
            saveError?.let { DetailNotice(it) }
            if (!fresh) DetailNotice("Dieses Label wurde geändert. Bitte schließen und neu öffnen.")
            error?.let { DetailNotice(it) }
        }
    } }
}
private fun colorForLabel(label: FamilyLabel) = androidx.compose.ui.graphics.Color(label.color.argb).copy(alpha = 0.10f)
