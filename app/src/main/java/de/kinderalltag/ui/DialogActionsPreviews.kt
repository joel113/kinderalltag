package de.kinderalltag.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview(name = "Dialogaktionen", widthDp = 390, heightDp = 900)
@Preview(
    name = "Dialogaktionen · schmal, große Schrift",
    widthDp = 320,
    heightDp = 1200,
    fontScale = 1.5f,
)
@Composable
private fun DialogActionsPreview() {
    KinderTheme {
        Surface(color = AppTheme.colors.background) {
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(36.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                val edit = DetailActionSpec("Bearbeiten", Icons.Default.Edit) {}
                val delete = DetailActionSpec("Löschen", Icons.Default.DeleteOutline) {}
                Text("Aufgabe", style = MaterialTheme.typography.titleMedium)
                DetailActions(
                    secondary = listOf(edit, DetailActionSpec("Pausieren", Icons.Default.Pause) {}),
                    deleteAction = delete,
                )
                HorizontalDivider()
                Text("Ausführung", style = MaterialTheme.typography.titleMedium)
                DetailActions(
                    secondary = listOf(edit),
                    deleteAction = delete,
                    primary = {
                        DetailAction(
                            "Als erledigt bestätigen",
                            Icons.Default.Check,
                            modifier = Modifier.fillMaxWidth(),
                        ) {}
                    },
                )
                HorizontalDivider()
                Text("Termin", style = MaterialTheme.typography.titleMedium)
                DetailActions(
                    secondary = listOf(edit, DetailActionSpec("Teilen", Icons.Default.Share) {}),
                    deleteAction = delete,
                    primary = {
                        DetailAction(
                            "Im Kalender speichern",
                            Icons.Default.EventAvailable,
                            modifier = Modifier.fillMaxWidth(),
                        ) {}
                    },
                )
                HorizontalDivider()
                Text("Einkauf / Geschenkidee", style = MaterialTheme.typography.titleMedium)
                DetailActions(
                    secondary = listOf(edit),
                    deleteAction = delete,
                    primary = {
                        DetailAction(
                            "Als gekauft markieren",
                            Icons.Default.Check,
                            modifier = Modifier.fillMaxWidth(),
                        ) {}
                    },
                )
            }
        }
    }
}
