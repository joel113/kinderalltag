package de.kinderalltag.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import de.kinderalltag.domain.appendDictation

@Composable
internal fun rememberDictation(prompt: String, enabled: Boolean = true, onRecognized: (String) -> Unit): () -> Unit {
    val context = LocalContext.current
    val latestChange by rememberUpdatedState(onRecognized)
    val latestEnabled by rememberUpdatedState(enabled)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && latestEnabled) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
            if (spoken.isNotBlank()) latestChange(spoken)
        }
    }
    return {
        if (enabled) {
            try {
                launcher.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                    .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "de-DE")
                    .putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
                    .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1))
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(context, "Keine Spracherkennung verfügbar. Bitte die Tastatur verwenden.", Toast.LENGTH_LONG).show()
            } catch (_: SecurityException) {
                Toast.makeText(context, "Spracherkennung konnte nicht geöffnet werden. Bitte die Tastatur verwenden.", Toast.LENGTH_LONG).show()
            }
        }
    }
}

@Composable
internal fun DictationField(value: String, onValueChange: (String) -> Unit, label: String,
    modifier: Modifier = Modifier, notes: Boolean = false, enabled: Boolean = true) {
    val latestValue by rememberUpdatedState(value)
    val dictate = rememberDictation(label, enabled) { onValueChange(appendDictation(latestValue, it, notes)) }
    OutlinedTextField(value, onValueChange, modifier = modifier, label = { Text(label) }, enabled = enabled,
        singleLine = !notes, minLines = if (notes) 2 else 1, trailingIcon = {
            IconButton(enabled = enabled, onClick = dictate) { Icon(Icons.Default.Mic, "$label diktieren") }
        })
}
