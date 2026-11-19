package de.kinderalltag.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun ImpressumDialog(onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    var openError by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Impressum") },
        text = {
            Column(
                Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "Herausgeber & Kontakt",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                SelectionContainer {
                    Text(
                        "Johannes Ehm\nNebelhornstraße 38\n80686 München\n\nmail@johannesehm.de\nhttps://www.johannesehm.de"
                    )
                }
                TextButton(
                    onClick = {
                        openError =
                            runCatching { uriHandler.openUri("mailto:mail@johannesehm.de") }
                                .isFailure
                    }
                ) {
                    Text("E-Mail schreiben")
                }
                TextButton(
                    onClick = {
                        openError =
                            runCatching { uriHandler.openUri("https://www.johannesehm.de") }
                                .isFailure
                    }
                ) {
                    Text("Website öffnen")
                }
                if (openError)
                    Text(
                        "Kein passendes Programm verfügbar. Du kannst die Angaben oben kopieren.",
                        style = MaterialTheme.typography.bodySmall,
                    )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Schließen") } },
    )
}
