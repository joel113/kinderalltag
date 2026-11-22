package de.kinderalltag.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

internal enum class AppThemeId(val storedValue: String, val label: String) {
    GARDEN("garden", "Garten"),
    PLAY("play", "Spielwelt"),
    SPACE("space", "Weltraum"),
    ANIMALS("animals", "Tierwelt"),
    VEHICLES("vehicles", "Fahrzeugwelt"),
    NATURE("nature", "Naturwelt"),
    FARM("farm", "Bauernhof"),
    MAGIC("magic", "Magische Welt");

    companion object {
        fun fromStored(value: String?): AppThemeId =
            entries.firstOrNull { it.storedValue == value } ?: GARDEN
    }
}

internal object StatusColors {
    val success = Color(0xFF32664D)
    val error = Color(0xFFB3261E)
    val correction = Color(0xFFBF4658)
    val change = Color(0xFF166FA6)
    val cancelled = Color(0xFF713D49)
    val yellow = Color(0xFFE8B643)
    val upcoming = Color(0xFFADB3B0)
    val successSurface = Color(0xFFE4F0DB)
    val errorSurface = Color(0xFFFCE6E4)
}

internal data class AppPalette(
    val background: Color,
    val ink: Color,
    val primary: Color,
    val secondary: Color,
    val accent: Color,
    val outline: Color,
    val deepAccent: Color,
    val primaryContainer: Color,
    val secondaryContainer: Color,
    val accentContainer: Color,
    val householdSurface: Color,
    val trackerPaper: Color,
    val trackerContainer: Color,
    val trackerOutline: Color,
    val artBlue: Color,
    val artCoral: Color,
    val artGreen: Color,
) {
    fun materialColors(): ColorScheme =
        lightColorScheme(
            primary = primary,
            onPrimary = Color.White,
            secondary = secondary,
            onSecondary = Color.White,
            tertiary = accent,
            onTertiary = Color.White,
            background = background,
            onBackground = ink,
            surface = background,
            onSurface = ink,
            surfaceContainer = householdSurface,
            surfaceVariant = accentContainer,
            onSurfaceVariant = if (this == AppThemes.garden.colors) Color(0xFF505B50) else ink,
            primaryContainer = primaryContainer,
            onPrimaryContainer =
                if (this == AppThemes.garden.colors) Color(0xFF184966) else primary,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = deepAccent,
            tertiaryContainer = accentContainer,
            onTertiaryContainer = accent,
            outline = if (this == AppThemes.garden.colors) Color(0xFF7F8375) else trackerOutline,
            outlineVariant = if (this == AppThemes.garden.colors) Color(0xFFD6D4BD) else outline,
        )
}

internal data class AppThemeDefinition(
    val id: AppThemeId,
    val colors: AppPalette,
    val settingsTitle: String,
)

internal object AppThemes {
    val garden =
        AppThemeDefinition(
            AppThemeId.GARDEN,
            AppPalette(
                Color(0xFFFFF8E8),
                Color(0xFF333C38),
                Color(0xFF166FA6),
                Color(0xFFBF4658),
                Color(0xFF32664D),
                Color(0xFF79A78B),
                Color(0xFF713D49),
                Color(0xFFE1F0FA),
                Color(0xFFFCE6E4),
                Color(0xFFE4F0DB),
                Color(0xFFFFF0CF),
                Color(0xFFFFFDF6),
                Color(0xFFE3E6D8),
                Color(0xFFADB69E),
                Color(0xFF166FA6),
                Color(0xFFBF4658),
                Color(0xFF79A78B),
            ),
            "Euer Familiengarten",
        )
    val play =
        AppThemeDefinition(
            AppThemeId.PLAY,
            AppPalette(
                Color(0xFFFFF9EF),
                Color(0xFF35434B),
                Color(0xFF326B7A),
                Color(0xFF985045),
                Color(0xFF4B6B45),
                Color(0xFFB9C8B0),
                Color(0xFF78433E),
                Color(0xFFE4EFF1),
                Color(0xFFFBE7DE),
                Color(0xFFEDF1DE),
                Color(0xFFFFEFD8),
                Color(0xFFFFFCF5),
                Color(0xFFE9EBDD),
                Color(0xFF849080),
                Color(0xFF8BAFB7),
                Color(0xFFD99580),
                Color(0xFFA4B889),
            ),
            "Eure Spielwelt",
        )
    val space =
        AppThemeDefinition(
            AppThemeId.SPACE,
            AppPalette(
                Color(0xFFF4F1FF),
                Color(0xFF34344D),
                Color(0xFF55549B),
                Color(0xFF965044),
                Color(0xFF416A78),
                Color(0xFFBAB7D8),
                Color(0xFF64416D),
                Color(0xFFE8E4FA),
                Color(0xFFF7E6EF),
                Color(0xFFE1EFF4),
                Color(0xFFEBE7FA),
                Color(0xFFFCFBFF),
                Color(0xFFE8E5F4),
                Color(0xFF85819F),
                Color(0xFF829BC9),
                Color(0xFFD69AAB),
                Color(0xFFA1B7CB),
            ),
            "Eure Raumstation",
        )

    val animals =
        AppThemeDefinition(
            AppThemeId.ANIMALS,
            AppPalette(
                background = Color(0xFFF1F6E9),
                ink = Color(0xFF374535),
                primary = Color(0xFF426952),
                secondary = Color(0xFF885A36),
                accent = Color(0xFF356E7E),
                outline = Color(0xFFBCCCB1),
                deepAccent = Color(0xFF6E4E35),
                primaryContainer = Color(0xFFE1ECDD),
                secondaryContainer = Color(0xFFF6E9D6),
                accentContainer = Color(0xFFE0EFF0),
                householdSurface = Color(0xFFF3ECD9),
                trackerPaper = Color(0xFFFBFCF4),
                trackerContainer = Color(0xFFE6EDD9),
                trackerOutline = Color(0xFF8A967C),
                artBlue = Color(0xFF9FC2CC),
                artCoral = Color(0xFFF0A65A),
                artGreen = Color(0xFFB0C67F),
            ),
            "Eure Tierwelt",
        )

    val vehicles =
        AppThemeDefinition(
            AppThemeId.VEHICLES,
            AppPalette(
                background = Color(0xFFF6F7EC),
                ink = Color(0xFF34434A),
                primary = Color(0xFF35687F),
                secondary = Color(0xFF925129),
                accent = Color(0xFF466A42),
                outline = Color(0xFFC4CCB7),
                deepAccent = Color(0xFF704728),
                primaryContainer = Color(0xFFE1EFF4),
                secondaryContainer = Color(0xFFF8E7D3),
                accentContainer = Color(0xFFE9EFDD),
                householdSurface = Color(0xFFF5EDD9),
                trackerPaper = Color(0xFFFCFDF7),
                trackerContainer = Color(0xFFE8ECDD),
                trackerOutline = Color(0xFF8F947F),
                artBlue = Color(0xFF86B3C7),
                artCoral = Color(0xFFE0A25E),
                artGreen = Color(0xFF92B76D),
            ),
            "Eure Fahrzeugwelt",
        )

    val nature =
        AppThemeDefinition(
            AppThemeId.NATURE,
            AppPalette(
                background = Color(0xFFF0F7F3),
                ink = Color(0xFF30453E),
                primary = Color(0xFF2F6953),
                secondary = Color(0xFF995268),
                accent = Color(0xFF2D6C83),
                outline = Color(0xFFB4CEC0),
                deepAccent = Color(0xFF754457),
                primaryContainer = Color(0xFFDDEDE2),
                secondaryContainer = Color(0xFFF8E4EA),
                accentContainer = Color(0xFFDDEFF5),
                householdSurface = Color(0xFFF1EEDB),
                trackerPaper = Color(0xFFFAFDFA),
                trackerContainer = Color(0xFFE0EDE5),
                trackerOutline = Color(0xFF81998C),
                artBlue = Color(0xFF76BBD2),
                artCoral = Color(0xFFE7A2B5),
                artGreen = Color(0xFF88B98A),
            ),
            "Eure Naturwelt",
        )

    val farm =
        AppThemeDefinition(
            AppThemeId.FARM,
            AppPalette(
                background = Color(0xFFFFF8E7),
                ink = Color(0xFF454431),
                primary = Color(0xFF4E6B36),
                secondary = Color(0xFF9A493B),
                accent = Color(0xFF846019),
                outline = Color(0xFFD2CAA5),
                deepAccent = Color(0xFF714536),
                primaryContainer = Color(0xFFE6EED8),
                secondaryContainer = Color(0xFFF9E3DB),
                accentContainer = Color(0xFFF5EAC7),
                householdSurface = Color(0xFFF7EDD4),
                trackerPaper = Color(0xFFFFFDF5),
                trackerContainer = Color(0xFFECEBD6),
                trackerOutline = Color(0xFF999475),
                artBlue = Color(0xFF91B9C9),
                artCoral = Color(0xFFE5A0A1),
                artGreen = Color(0xFF9FBA73),
            ),
            "Euer Bauernhof",
        )

    val magic =
        AppThemeDefinition(
            AppThemeId.MAGIC,
            AppPalette(
                background = Color(0xFFFFF4FA),
                ink = Color(0xFF483B53),
                primary = Color(0xFF76519A),
                secondary = Color(0xFFA14373),
                accent = Color(0xFF326D68),
                outline = Color(0xFFD4BFD9),
                deepAccent = Color(0xFF713E65),
                primaryContainer = Color(0xFFEDE2F7),
                secondaryContainer = Color(0xFFFBE1ED),
                accentContainer = Color(0xFFDFF1EA),
                householdSurface = Color(0xFFF4E6F3),
                trackerPaper = Color(0xFFFFFBFE),
                trackerContainer = Color(0xFFEEE3F0),
                trackerOutline = Color(0xFF9E86AA),
                artBlue = Color(0xFFB5A3DB),
                artCoral = Color(0xFFEDA9CA),
                artGreen = Color(0xFF96CDBB),
            ),
            "Eure magische Welt",
        )

    fun get(id: AppThemeId): AppThemeDefinition =
        when (id) {
            AppThemeId.GARDEN -> garden
            AppThemeId.PLAY -> play
            AppThemeId.SPACE -> space
            AppThemeId.ANIMALS -> animals
            AppThemeId.VEHICLES -> vehicles
            AppThemeId.NATURE -> nature
            AppThemeId.FARM -> farm
            AppThemeId.MAGIC -> magic
        }
}

private val LocalAppTheme = staticCompositionLocalOf { AppThemes.garden }

internal data class ThemeSelection(val id: AppThemeId, val select: (AppThemeId) -> Unit)

internal val LocalThemeSelection = staticCompositionLocalOf { ThemeSelection(AppThemeId.GARDEN) {} }

internal object AppTheme {
    val definition: AppThemeDefinition
        @Composable get() = LocalAppTheme.current

    val colors: AppPalette
        @Composable get() = definition.colors
}

@Composable
internal fun AppThemeHost(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val preferences =
        remember(context) { context.getSharedPreferences("display", Context.MODE_PRIVATE) }
    var selected by
        remember(preferences) {
            mutableStateOf(AppThemeId.fromStored(preferences.getString("app_theme", null)))
        }
    val selection =
        ThemeSelection(selected) {
            selected = it
            preferences.edit().putString("app_theme", it.storedValue).apply()
            if (!LauncherIcons.sync(context, it)) {
                Toast.makeText(
                        context,
                        "Das App-Symbol konnte noch nicht aktualisiert werden. Bitte öffne die App erneut.",
                        Toast.LENGTH_LONG,
                    )
                    .show()
            }
        }
    CompositionLocalProvider(LocalThemeSelection provides selection) {
        KinderTheme(selected, content)
    }
}

@Composable
internal fun KinderTheme(theme: AppThemeId = AppThemeId.GARDEN, content: @Composable () -> Unit) {
    val definition = AppThemes.get(theme)
    CompositionLocalProvider(LocalAppTheme provides definition) {
        MaterialTheme(
            colorScheme = definition.colors.materialColors(),
            shapes =
                Shapes(
                    small = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                    medium = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                    large = androidx.compose.foundation.shape.RoundedCornerShape(26.dp),
                    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(32.dp),
                ),
            content = content,
        )
    }
}
