package de.kinderalltag.ui

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log

// Keep these component names stable: launchers can retain references across app updates.
internal val AppThemeId.launcherAlias: String
    get() =
        when (this) {
            AppThemeId.GARDEN -> "LauncherGarden"
            AppThemeId.PLAY -> "LauncherPlay"
            AppThemeId.SPACE -> "LauncherSpace"
            AppThemeId.ANIMALS -> "LauncherAnimals"
            AppThemeId.VEHICLES -> "LauncherVehicles"
            AppThemeId.NATURE -> "LauncherNature"
            AppThemeId.FARM -> "LauncherFarm"
            AppThemeId.MAGIC -> "LauncherMagic"
        }

internal data class LauncherChange(val theme: AppThemeId, val enabled: Boolean)

internal fun launcherChanges(selected: AppThemeId, enabled: Set<AppThemeId>): List<LauncherChange> =
    buildList {
        // Older Android versions apply changes individually. Always enable the replacement first.
        if (selected !in enabled) add(LauncherChange(selected, true))
        enabled.filter { it != selected }.forEach { add(LauncherChange(it, false)) }
    }

internal object LauncherIcons {
    fun syncSavedTheme(context: Context) {
        val saved =
            context
                .getSharedPreferences("display", Context.MODE_PRIVATE)
                .getString("app_theme", null)
        sync(context, AppThemeId.fromStored(saved))
    }

    fun sync(context: Context, theme: AppThemeId): Boolean {
        return try {
            val manager = context.packageManager
            val enabled =
                AppThemeId.entries.filterTo(mutableSetOf()) { id ->
                    when (manager.getComponentEnabledSetting(component(context, id))) {
                        PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> id == AppThemeId.GARDEN
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
                        else -> false
                    }
                }
            val changes = launcherChanges(theme, enabled)
            if (changes.isNotEmpty()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    manager.setComponentEnabledSettings(
                        changes.map {
                            PackageManager.ComponentEnabledSetting(
                                component(context, it.theme),
                                state(it.enabled),
                                PackageManager.DONT_KILL_APP,
                            )
                        }
                    )
                } else {
                    changes.forEach {
                        manager.setComponentEnabledSetting(
                            component(context, it.theme),
                            state(it.enabled),
                            PackageManager.DONT_KILL_APP,
                        )
                    }
                }
            }
            true
        } catch (error: RuntimeException) {
            // Keep the selected in-app theme; retry icon reconciliation on the next process start.
            Log.w("LauncherIcons", "Launcher-Symbol konnte nicht aktualisiert werden", error)
            false
        }
    }

    private fun component(context: Context, theme: AppThemeId) =
        ComponentName(context.packageName, "${context.packageName}.${theme.launcherAlias}")

    private fun state(enabled: Boolean) =
        if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
}
