package de.kinderalltag.ui

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import de.kinderalltag.text.*
import java.util.Locale

internal object AppStrings {
    @Volatile private var activityInitialized = false

    fun activityCreated() {
        activityInitialized = true
    }

    fun context(context: Context): Context {
        val localized = ContextCompat.getContextForLanguage(context)
        if (android.os.Build.VERSION.SDK_INT >= 33 || !activityInitialized) return localized
        // AndroidX persists locale changes asynchronously on older Android versions.
        // Read the current delegate selection in-process, including a reset to System.
        val requested = AppCompatDelegate.getApplicationLocales()
        val configuration = Configuration(localized.resources.configuration)
        configuration.setLocales(
            if (requested.isEmpty) android.content.res.Resources.getSystem().configuration.locales
            else android.os.LocaleList.forLanguageTags(requested.toLanguageTags())
        )
        return localized.createConfigurationContext(configuration)
    }

    fun install(context: Context) {
        val app = context.applicationContext
        QuantityCatalog.renderer = { key, count, args ->
            context(app).resources.getQuantityString(key.resourceId, count, *args)
        }
        TextCatalog.renderer = { key, args ->
            val localized = context(app)
            if (args.isEmpty()) localized.getString(key.resourceId)
            else localized.getString(key.resourceId, *args)
        }
    }
}

@Composable
internal fun LanguagePicker(onChanging: () -> Unit = {}) {
    // Locale changes recreate the activity; AndroidX also synchronizes Android 13+ system settings.
    val current =
        AppCompatDelegate.getApplicationLocales()
            .toLanguageTags()
            .substringBefore(',')
            .substringBefore('-')
    Text(text(TextKey.LANGUAGE), style = MaterialTheme.typography.titleMedium)
    Column(Modifier.fillMaxWidth().selectableGroup()) {
        listOf("" to text(TextKey.SYSTEM_LANGUAGE), "de" to "Deutsch", "en" to "English").forEach {
            (tag, label) ->
            Row(
                Modifier.fillMaxWidth()
                    .selectable(
                        selected = current == tag,
                        role = Role.RadioButton,
                        onClick = {
                            onChanging()
                            AppCompatDelegate.setApplicationLocales(
                                LocaleListCompat.forLanguageTags(tag)
                            )
                        },
                    )
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(current == tag, onClick = null)
                Text(label)
            }
        }
    }
}

internal fun presentationLocale(configuration: Configuration): Locale =
    resolveAppLocale((0 until configuration.locales.size()).map { configuration.locales[it] })

internal fun uses24HourClock(context: Context): Boolean {
    val configuration = Configuration(context.resources.configuration)
    configuration.setLocales(android.content.res.Resources.getSystem().configuration.locales)
    return android.text.format.DateFormat.is24HourFormat(
        context.createConfigurationContext(configuration)
    )
}
