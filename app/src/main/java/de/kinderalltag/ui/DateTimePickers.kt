package de.kinderalltag.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.text.format.DateFormat
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import de.kinderalltag.domain.FAMILY_ZONE
import de.kinderalltag.text.*
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@Composable
internal fun rememberDateTimeFormats(): DateTimeFormats {
    val context = LocalContext.current
    val locale = presentationLocale(LocalConfiguration.current)
    val use24Hour = uses24HourClock(context)
    return remember(locale, use24Hour) {
        DateTimeFormats(
            locale,
            SimpleDateFormat(
                DateFormat.getBestDateTimePattern(locale, if (use24Hour) "Hm" else "hm"),
                locale,
            ),
        )
    }
}

@Composable
internal fun DatePickerField(
    label: String,
    value: LocalDate?,
    onSelect: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
    optional: Boolean = false,
    enabled: Boolean = true,
    minDate: LocalDate? = null,
    maxDate: LocalDate? = null,
) {
    val formats = rememberDateTimeFormats()
    var open by rememberSaveable { mutableStateOf(false) }
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(
            onClick = { open = true },
            enabled = enabled,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp, 12.dp),
            shape = MaterialTheme.shapes.small,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(label, style = MaterialTheme.typography.labelMedium)
                Text(
                    value?.let(formats::date)
                        ?: if (optional) text(TextKey.NO_END_DATE_08A7C2)
                        else text(TextKey.SELECT_DATE),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Icon(Icons.Default.CalendarMonth, null)
        }
        if (optional && value != null)
            IconButton(onClick = { onSelect(null) }, enabled = enabled) {
                Icon(Icons.Default.Close, text(TextKey.REMOVE, label))
            }
    }
    if (open) {
        val context = LocalContext.current
        val latestSelect by rememberUpdatedState(onSelect)
        DisposableEffect(context) {
            var initial = value ?: LocalDate.now(FAMILY_ZONE)
            minDate?.let { if (initial < it) initial = it }
            maxDate?.let { if (initial > it) initial = it }
            val dialog =
                DatePickerDialog(
                    languageContext(context),
                    { _, year, month, day ->
                        latestSelect(LocalDate.of(year, month + 1, day))
                        open = false
                    },
                    initial.year,
                    initial.monthValue - 1,
                    initial.dayOfMonth,
                )
            // Native DatePicker interprets its limits in the phone's local calendar.
            minDate?.let {
                dialog.datePicker.minDate =
                    it.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
            maxDate?.let {
                dialog.datePicker.maxDate =
                    it.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
            dialog.setOnDismissListener { open = false }
            dialog.show()
            onDispose {
                dialog.setOnDismissListener(null)
                dialog.dismiss()
            }
        }
    }
}

@Composable
internal fun TimePickerField(
    label: String,
    value: LocalTime,
    onSelect: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onRemove: (() -> Unit)? = null,
) {
    val formats = rememberDateTimeFormats()
    var open by rememberSaveable { mutableStateOf(false) }
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(
            onClick = { open = true },
            enabled = enabled,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp, 12.dp),
            shape = MaterialTheme.shapes.small,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(label, style = MaterialTheme.typography.labelMedium)
                Text(
                    formats.time(value),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Icon(Icons.Default.Schedule, null)
        }
        onRemove?.let { remove ->
            IconButton(onClick = remove, enabled = enabled) {
                Icon(Icons.Default.Close, text(TextKey.REMOVE, label))
            }
        }
    }
    if (open)
        NativeTimeDialog(
            value,
            {
                onSelect(it)
                open = false
            },
            { open = false },
        )
}

@Composable
private fun NativeTimeDialog(
    initial: LocalTime,
    onSelect: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val latestSelect by rememberUpdatedState(onSelect)
    val latestDismiss by rememberUpdatedState(onDismiss)
    DisposableEffect(context) {
        val dialog =
            TimePickerDialog(
                context,
                { _, hour, minute -> latestSelect(LocalTime.of(hour, minute)) },
                initial.hour,
                initial.minute,
                uses24HourClock(context),
            )
        dialog.setOnDismissListener { latestDismiss() }
        dialog.show()
        onDispose {
            dialog.setOnDismissListener(null)
            dialog.dismiss()
        }
    }
}

@Composable
internal fun TimeSlotsPicker(
    times: List<LocalTime>,
    onChange: (List<LocalTime>) -> Unit,
    enabled: Boolean,
) {
    var adding by rememberSaveable { mutableStateOf(false) }
    var duplicate by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        times.forEachIndexed { index, time ->
            TimePickerField(
                if (times.size == 1) text(TextKey.TIME) else text(TextKey.TIME_A3636D, index + 1),
                time,
                onSelect = { selected ->
                    duplicate =
                        times.filterIndexed { other, _ -> other != index }.contains(selected)
                    if (!duplicate)
                        onChange(times.toMutableList().apply { set(index, selected) }.sorted())
                },
                enabled = enabled,
                onRemove =
                    if (times.size > 1)
                        ({
                            onChange(times.filterIndexed { other, _ -> other != index })
                            duplicate = false
                        })
                    else null,
            )
        }
        if (duplicate)
            Text(text(TextKey.THIS_TIME_ALREADY_EXISTS), color = MaterialTheme.colorScheme.error)
        TextButton(onClick = { adding = true }, enabled = enabled && times.size < 8) {
            Text(text(TextKey.ADD_TIME_8, times.size))
        }
    }
    if (adding)
        NativeTimeDialog(
            times.last().plusHours(1),
            { selected ->
                duplicate = selected in times
                if (!duplicate) onChange((times + selected).sorted())
                adding = false
            },
            { adding = false },
        )
}
