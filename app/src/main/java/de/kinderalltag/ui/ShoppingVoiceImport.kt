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
import de.kinderalltag.text.*

@Composable
internal fun ShoppingVoiceImportDialog(state: FamilyState, vm: FamilyViewModel, busy: Boolean) {
    val language =
        presentationLocale(androidx.compose.ui.platform.LocalConfiguration.current).language
    var review by rememberSaveable { mutableStateOf("") }
    var selectedLabels by rememberSaveable { mutableStateOf(arrayListOf<String>()) }
    var error by remember { mutableStateOf<String?>(null) }
    val saveError by vm.listError.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.listError.value = null }
    val open = listItems(state.listItems, state.listActivity, FamilyListKind.SHOPPING, false)
    val titles = shoppingImportTitles(review, open.map { it.title })
    val skipped = review.lineSequence().count { it.isNotBlank() } - titles.size
    val dictate =
        rememberDictation(
            text(TextKey.FOR_EXAMPLE_MILK_BREAD_AND_EGGS_OR_SAY_NEXT_ITEM_BETWEEN_ITEMS),
            enabled = !busy && vm.listUi.voiceImportOpen,
        ) { spoken ->
            val lines = splitShoppingSpeech(spoken, language).joinToString("\n")
            if (lines.isNotBlank()) {
                review = if (review.isBlank()) lines else review.trimEnd() + "\n" + lines
                error = null
            }
        }
    ItemDetailsDialog(
        text(TextKey.ADD_SHOPPING_ITEMS),
        text(TextKey.SHOPPING_LIST),
        { vm.listUi.voiceImportOpen = false },
        busy,
        actions = {
            Button(
                onClick = {
                    try {
                        validateShoppingImport(titles)
                        requireLocalized(selectedLabels.size <= 6) {
                            message(TextKey.PLEASE_SELECT_NO_MORE_THAN_6_SHARED_LABELS)
                        }
                        error = null
                        vm.importShopping(titles, selectedLabels.toSet()) {
                            vm.listUi.voiceImportOpen = false
                        }
                    } catch (e: IllegalArgumentException) {
                        error = e.message
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !busy && state.authorized && titles.isNotEmpty(),
            ) {
                Text(
                    "${titles.size} ${if (titles.size == 1) text(TextKey.ADD_SHOPPING_ITEM) else text(TextKey.ADD_SHOPPING_ITEMS)}"
                )
            }
        },
    ) {
        Text(text(TextKey.ENTER_ONE_ITEM_PER_LINE_OR_DICTATE_YOUR_LIST_FOR_EXAMPLE_MILK_AND_BREAD))
        OutlinedButton(onClick = dictate, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Mic, null)
            Spacer(Modifier.width(8.dp))
            Text(
                if (review.isBlank()) text(TextKey.DICTATE_LIST)
                else text(TextKey.DICTATE_MORE_SHOPPING_ITEMS)
            )
        }
        OutlinedTextField(
            review,
            {
                review = it
                error = null
            },
            label = { Text(text(TextKey.ONE_SHOPPING_ITEM_PER_LINE)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 5,
            enabled = !busy,
        )
        Text(
            text(
                TextKey.NEW_SHOPPING_ITEMS_UP_TO_PER_IMPORT_YOU_CAN_EDIT_MERGE_OR_REMOVE_LINES,
                titles.size,
                MAX_SHOPPING_IMPORT_ITEMS,
            ),
            style = MaterialTheme.typography.bodySmall,
        )
        if (skipped > 0)
            DetailNotice(text(TextKey.DUPLICATE_OR_ALREADY_OPEN_ITEMS_WILL_BE_SKIPPED, skipped))
        Text(
            text(TextKey.YOU_CAN_MERGE_PRODUCTS_WITH_AND_IN_THEIR_NAMES_INTO_ONE_LINE_HERE_QUANTI),
            style = MaterialTheme.typography.bodySmall,
        )
        val labels =
            state.listLabels
                .filter {
                    it.group == LabelGroup.SHOPPING && (it.active || it.id in selectedLabels)
                }
                .sortedBy { it.name }
        if (labels.isNotEmpty())
            DetailSection(text(TextKey.SHARED_LABELS_OPTIONAL), AppTheme.colors.background) {
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
                Text(
                    text(TextKey.THESE_LABELS_APPLY_TO_ALL_NEW_SHOPPING_ITEMS_UP_TO_6),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        Text(
            text(TextKey.NOTHING_IS_SAVED_UNTIL_YOU_TAP_ADD_OFFLINE_SHOPPING_ITEMS_ARE_QUEUED_IND),
            style = MaterialTheme.typography.bodySmall,
        )
        error?.let { DetailNotice(it) }
        saveError?.let { DetailNotice(it) }
    }
}
