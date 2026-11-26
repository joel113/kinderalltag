package de.kinderalltag.ui

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import de.kinderalltag.domain.*
import de.kinderalltag.text.*

internal object AppointmentDefaults {
    private const val DURATION = "default_appointment_minutes"

    fun duration(context: Context): Int =
        context
            .getSharedPreferences("display", Context.MODE_PRIVATE)
            .getInt(DURATION, DEFAULT_APPOINTMENT_MINUTES)
            .takeIf { it in 1..MAX_DEFAULT_APPOINTMENT_MINUTES } ?: DEFAULT_APPOINTMENT_MINUTES

    fun setDuration(context: Context, minutes: Int) {
        require(minutes in 1..MAX_DEFAULT_APPOINTMENT_MINUTES)
        context
            .getSharedPreferences("display", Context.MODE_PRIVATE)
            .edit()
            .putInt(DURATION, minutes)
            .apply()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AppointmentDurationSettings() {
    val context = LocalContext.current
    var input by remember { mutableStateOf(AppointmentDefaults.duration(context).toString()) }
    fun update(value: String) {
        if (value.length <= 3 && value.all { it in '0'..'9' }) {
            input = value
            value
                .toIntOrNull()
                ?.takeIf { it in 1..MAX_DEFAULT_APPOINTMENT_MINUTES }
                ?.let { AppointmentDefaults.setDuration(context, it) }
        }
    }
    OutlinedTextField(
        input,
        ::update,
        label = { Text(text(TextKey.DEFAULT_APPOINTMENT_DURATION)) },
        supportingText = { Text(text(TextKey.APPOINTMENT_DURATION_RANGE)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        isError = input.toIntOrNull()?.let { it !in 1..MAX_DEFAULT_APPOINTMENT_MINUTES } ?: true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(15, 30, 45, 60, 90, 120).forEach { minutes ->
            FilterChip(
                input.toIntOrNull() == minutes,
                { update(minutes.toString()) },
                label = { Text(text(TextKey.APPOINTMENT_DURATION_MINUTES, minutes)) },
            )
        }
    }
    Text(text(TextKey.APPOINTMENT_DURATION_HELP), style = MaterialTheme.typography.bodySmall)
}
