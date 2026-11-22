package de.kinderalltag.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import de.kinderalltag.domain.*
import org.junit.Assert.*
import org.junit.Test

class AppThemeTest {
    @Test
    fun savedThemeIdsAreStableAndUnknownValuesFallBackToGarden() {
        assertEquals(
            listOf("garden", "play", "space", "animals", "vehicles", "nature", "farm", "magic"),
            AppThemeId.entries.map { it.storedValue },
        )
        AppThemeId.entries.forEach {
            assertEquals(it, AppThemeId.fromStored(it.storedValue))
            assertEquals(it, AppThemes.get(it).id)
        }
        listOf(null, "", "obsolete", "PLAY").forEach {
            assertEquals(AppThemeId.GARDEN, AppThemeId.fromStored(it))
        }
    }

    @Test
    fun themesHaveReadableTextAndButtonContrast() {
        AppThemeId.entries.forEach { id ->
            val c = AppThemes.get(id).colors
            listOf(
                    c.background,
                    c.primaryContainer,
                    c.secondaryContainer,
                    c.accentContainer,
                    c.householdSurface,
                    c.trackerPaper,
                )
                .forEach { surface ->
                    assertTrue("$id body text contrast", contrast(c.ink, surface) >= 4.5)
                }
            listOf(c.primary, c.secondary, c.accent).forEach { action ->
                assertTrue("$id white button text contrast", contrast(Color.White, action) >= 4.5)
            }
            val material = c.materialColors()
            assertEquals(c.background, material.background)
            assertEquals(StatusColors.error, material.error)
        }
    }

    @Test
    fun gardenPreservesTheExistingBasePaletteAndTrackerSurfaces() {
        val c = AppThemes.garden.colors
        assertEquals(Color(0xFFFFF8E8), c.background)
        assertEquals(Color(0xFF166FA6), c.primary)
        assertEquals(Color(0xFF32664D), c.accent)
        assertEquals(Color(0xFFE3E6D8), c.trackerContainer)
        assertEquals(Color(0xFFADB69E), c.trackerOutline)
    }

    @Test
    fun childColorChoicesAreIndependentOfTheLocalTheme() {
        BearColor.entries.forEach { chosen ->
            val child = Child("johann", "Johann", chosen)
            val expected = childColor(child, AppThemes.garden.colors)
            AppThemeId.entries.forEach {
                assertEquals(expected, childColor(child, AppThemes.get(it).colors))
            }
        }
        AppThemeId.entries.forEach {
            assertEquals(
                AppThemes.get(it).colors.accent,
                childColor(HOUSEHOLD_SECTION, AppThemes.get(it).colors),
            )
        }
    }

    @Test
    fun allIllustrationRolesHaveThemeSpecificArtIncludingTheMenuAndHousehold() {
        IllustrationRole.entries.forEach { role ->
            assertEquals(ThemeArtwork.GARDEN, artworkFor(AppThemeId.GARDEN, role))
            assertNotEquals(ThemeArtwork.GARDEN, artworkFor(AppThemeId.PLAY, role))
            assertNotEquals(ThemeArtwork.GARDEN, artworkFor(AppThemeId.SPACE, role))
            assertNotEquals(ThemeArtwork.GARDEN, artworkFor(AppThemeId.ANIMALS, role))
            assertNotEquals(ThemeArtwork.GARDEN, artworkFor(AppThemeId.VEHICLES, role))
            assertNotEquals(ThemeArtwork.GARDEN, artworkFor(AppThemeId.NATURE, role))
            assertNotEquals(ThemeArtwork.GARDEN, artworkFor(AppThemeId.FARM, role))
            assertNotEquals(ThemeArtwork.GARDEN, artworkFor(AppThemeId.MAGIC, role))
        }
        assertEquals(ThemeArtwork.UNICORN, artworkFor(AppThemeId.MAGIC, IllustrationRole.BRAND))
        assertEquals(ThemeArtwork.FAIRY, artworkFor(AppThemeId.MAGIC, IllustrationRole.JOY))
        assertEquals(ThemeArtwork.ELF, artworkFor(AppThemeId.MAGIC, IllustrationRole.FRIEND))
        assertEquals(ThemeArtwork.PRINCESS, artworkFor(AppThemeId.MAGIC, IllustrationRole.ACTIVITY))
        assertEquals(ThemeArtwork.CASTLE, artworkFor(AppThemeId.MAGIC, IllustrationRole.HOUSEHOLD))
        assertEquals(ThemeArtwork.TRACTOR, artworkFor(AppThemeId.FARM, IllustrationRole.BRAND))
        assertEquals(ThemeArtwork.COMBINE, artworkFor(AppThemeId.FARM, IllustrationRole.HELPER))
        assertEquals(ThemeArtwork.BALER, artworkFor(AppThemeId.FARM, IllustrationRole.ACTIVITY))
        assertEquals(ThemeArtwork.BARN, artworkFor(AppThemeId.FARM, IllustrationRole.HOUSEHOLD))
        assertEquals(ThemeArtwork.TREE, artworkFor(AppThemeId.NATURE, IllustrationRole.BRAND))
        assertEquals(ThemeArtwork.WATERFALL, artworkFor(AppThemeId.NATURE, IllustrationRole.HELPER))
        assertEquals(
            ThemeArtwork.NATURE_COTTAGE,
            artworkFor(AppThemeId.NATURE, IllustrationRole.HOUSEHOLD),
        )
        assertEquals(ThemeArtwork.TRACTOR, artworkFor(AppThemeId.VEHICLES, IllustrationRole.BRAND))
        assertEquals(
            ThemeArtwork.EXCAVATOR,
            artworkFor(AppThemeId.VEHICLES, IllustrationRole.HELPER),
        )
        assertEquals(
            ThemeArtwork.GARAGE,
            artworkFor(AppThemeId.VEHICLES, IllustrationRole.HOUSEHOLD),
        )
        assertEquals(ThemeArtwork.FOX, artworkFor(AppThemeId.ANIMALS, IllustrationRole.BRAND))
        assertEquals(
            ThemeArtwork.ANIMAL_HOUSE,
            artworkFor(AppThemeId.ANIMALS, IllustrationRole.HOUSEHOLD),
        )
        assertEquals(ThemeArtwork.BLOCKS, artworkFor(AppThemeId.PLAY, IllustrationRole.BRAND))
        assertEquals(ThemeArtwork.PLANET, artworkFor(AppThemeId.SPACE, IllustrationRole.BRAND))
        assertEquals(ThemeArtwork.STATION, artworkFor(AppThemeId.SPACE, IllustrationRole.HOUSEHOLD))
    }

    private fun contrast(a: Color, b: Color): Double {
        val first = a.luminance().toDouble()
        val second = b.luminance().toDouble()
        return (maxOf(first, second) + 0.05) / (minOf(first, second) + 0.05)
    }
}
