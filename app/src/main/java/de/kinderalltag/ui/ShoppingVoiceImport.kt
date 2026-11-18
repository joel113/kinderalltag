package de.kinderalltag.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.kinderalltag.data.FamilyState
import de.kinderalltag.domain.*

@Composable
internal fun ShoppingVoiceImportDialog(state: FamilyState, vm: FamilyViewModel, busy: Boolean) {
    var review by rememberSaveable { mutableStateOf("") }
    var selectedLabels by rememberSaveable { mutableStateOf(arrayListOf<String>()) }
    var error by remember { mutableStateOf<String?>(null) }
    val saveError by vm.listError.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.listError.value = null }
    val open = listItems(state.listItems, state.listActivity, FamilyListKind.SHOPPING, false)
    val titles = shoppingImportTitles(review, open.map { it.title })
    val skipped = review.lineSequence().count { it.isNotBlank() } - titles.size
    val dictate = rememberDictation("Zum Beispiel: Milch, Brot und Eier. Oder sage zwischen Artikeln: nächster Artikel.",
        enabled = !busy && vm.listUi.voiceImportOpen) { spoken ->
        val lines = splitShoppingSpeech(spoken).joinToString("\n")
        if (lines.isNotBlank()) {
            review = if (review.isBlank()) lines else review.trimEnd() + "\n" + lines
            error = null
        }
    }
    ItemDetailsDialog("Einkäufe hinzufügen", "Einkaufsliste", { vm.listUi.voiceImportOpen = false }, busy, actions = {
        Button(onClick = {
            try {
                validateShoppingImport(titles)
                require(selectedLabels.size <= 6) { "Bitte höchstens 6 gemeinsame Labels auswählen." }
                error = null
                vm.importShopping(titles, selectedLabels.toSet()) { vm.listUi.voiceImportOpen = false }
            } catch (e: IllegalArgumentException) { error = e.message }
        }, modifier = Modifier.fillMaxWidth(), enabled = !busy && state.authorized && titles.isNotEmpty()) {
            Text("${titles.size} ${if (titles.size == 1) "Einkauf hinzufügen" else "Einkäufe hinzufügen"}")
        }
    }) {
        Text("Trage einen Einkauf pro Zeile ein oder sprich deine Liste, zum Beispiel „Milch und Brot und Eier“. Beim Sprechen trennen „und“, Kommas oder „nächster Artikel“ die Einkäufe. Danach prüfst du die Liste.")
        OutlinedButton(onClick = dictate, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Mic, null); Spacer(Modifier.width(8.dp))
            Text(if (review.isBlank()) "Liste sprechen" else "Weitere Einkäufe sprechen")
        }
        OutlinedTextField(review, { review = it; error = null }, label = { Text("Ein Einkauf pro Zeile") },
            modifier = Modifier.fillMaxWidth(), minLines = 5, enabled = !busy)
        Text("${titles.size} neue Einkäufe · höchstens $MAX_SHOPPING_IMPORT_ITEMS pro Import. Du kannst Zeilen ändern, zusammenführen oder entfernen.",
            style = MaterialTheme.typography.bodySmall)
        if (skipped > 0) DetailNotice("$skipped doppelte oder bereits offene Einträge werden übersprungen.")
        Text("Auch Produkte mit „und“ im Namen kannst du hier zu einer Zeile zusammenführen. Mengen wie „1,5 Liter Milch“ bleiben erhalten.",
            style = MaterialTheme.typography.bodySmall)
        val labels = state.listLabels.filter { it.group == LabelGroup.SHOPPING && (it.active || it.id in selectedLabels) }.sortedBy { it.name }
        if (labels.isNotEmpty()) DetailSection("Gemeinsame Labels (optional)", GardenColors.Cream) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                labels.forEach { label -> LabelChip(label, label.id in selectedLabels) {
                    selectedLabels = ArrayList(if (label.id in selectedLabels) selectedLabels - label.id else selectedLabels + label.id)
                } }
            }
            Text("Diese Labels gelten für alle neuen Einkäufe, höchstens 6.", style = MaterialTheme.typography.bodySmall)
        }
        Text("Gespeichert wird erst mit Hinzufügen. Offline werden die Einkäufe einzeln vorgemerkt und später synchronisiert.", style = MaterialTheme.typography.bodySmall)
        error?.let { DetailNotice(it) }
        saveError?.let { DetailNotice(it) }
    }
}
