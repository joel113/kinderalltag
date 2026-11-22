package de.kinderalltag.ui

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.*
import org.junit.Test
import org.w3c.dom.Element

class LauncherIconsTest {
    @Test
    fun everyThemeTransitionKeepsALauncherAvailableAndEndsWithOnlyTheSelectedIcon() {
        AppThemeId.entries.forEach { previous ->
            AppThemeId.entries.forEach { selected ->
                val enabled = mutableSetOf(previous)
                val changes = launcherChanges(selected, enabled)
                if (previous == selected) assertTrue(changes.isEmpty())
                changes.forEach { change ->
                    if (change.enabled) enabled.add(change.theme) else enabled.remove(change.theme)
                    assertTrue(
                        "A launcher must remain available during $previous → $selected",
                        enabled.isNotEmpty(),
                    )
                }
                assertEquals(setOf(selected), enabled)
            }
        }
    }

    @Test
    fun reconciliationRepairsMissingOrDuplicateLaunchers() {
        AppThemeId.entries.forEach { selected ->
            listOf(emptySet(), AppThemeId.entries.toSet()).forEach { initial ->
                val enabled = initial.toMutableSet()
                launcherChanges(selected, initial).forEach { change ->
                    if (change.enabled) enabled.add(change.theme) else enabled.remove(change.theme)
                    assertTrue(enabled.isNotEmpty())
                }
                assertEquals(setOf(selected), enabled)
            }
        }
    }

    @Test
    fun manifestDeclaresEveryAliasWithOnlyGardenEnabledAndPreservesCalendarImports() {
        val manifest = xml(File("src/main/AndroidManifest.xml"))
        val aliases = elements(manifest, "activity-alias")
        assertEquals(AppThemeId.entries.size, aliases.size)
        assertEquals(
            AppThemeId.entries.size,
            AppThemeId.entries.map { it.launcherAlias }.toSet().size,
        )
        AppThemeId.entries.forEach { theme ->
            val alias = aliases.single { it.android("name") == ".${theme.launcherAlias}" }
            assertEquals(".MainActivity", alias.android("targetActivity"))
            assertEquals("true", alias.android("exported"))
            assertEquals((theme == AppThemeId.GARDEN).toString(), alias.android("enabled"))
            assertEquals("@mipmap/ic_launcher_${theme.storedValue}", alias.android("icon"))
            assertEquals(
                listOf("android.intent.action.MAIN"),
                elements(alias, "action").map { it.android("name") },
            )
            assertEquals(
                listOf("android.intent.category.LAUNCHER"),
                elements(alias, "category").map { it.android("name") },
            )
        }
        val main = elements(manifest, "activity").single { it.android("name") == ".MainActivity" }
        val actions = elements(main, "action").map { it.android("name") }
        assertFalse(actions.contains("android.intent.action.MAIN"))
        assertTrue(
            actions.containsAll(
                listOf(
                    "android.intent.action.SEND",
                    "android.intent.action.SEND_MULTIPLE",
                    "android.intent.action.VIEW",
                )
            )
        )
        assertFalse(main.hasAttributeNS(ANDROID, "enabled"))
    }

    @Test
    fun everyLauncherHasAdaptiveAndMonochromeAssetsWithSafeForegroundInsets() {
        val resourceRoot = File("src/main/res")
        val colors = elements(xml(File(resourceRoot, "values/launcher_colors.xml")), "color")
        AppThemeId.entries.forEach { theme ->
            val name = theme.storedValue
            assertEquals(
                1,
                colors.count { it.getAttribute("name") == "launcher_${name}_background" },
            )
            listOf("mipmap-anydpi-v26", "mipmap-anydpi-v33").forEach { folder ->
                val icon = xml(File(resourceRoot, "$folder/ic_launcher_$name.xml"))
                assertEquals("adaptive-icon", icon.tagName)
                assertEquals(
                    "@color/launcher_${name}_background",
                    elements(icon, "background").single().android("drawable"),
                )
                assertEquals(
                    "@drawable/ic_launcher_${name}_foreground",
                    elements(icon, "foreground").single().android("drawable"),
                )
                if (folder.endsWith("v33")) {
                    assertEquals(
                        "@drawable/ic_launcher_${name}_foreground",
                        elements(icon, "monochrome").single().android("drawable"),
                    )
                }
            }
            val foreground = xml(File(resourceRoot, "drawable/ic_launcher_${name}_foreground.xml"))
            assertEquals("108", foreground.android("viewportWidth"))
            val inset = elements(foreground, "group").first()
            assertEquals("22", inset.android("translateX"))
            assertEquals("22", inset.android("translateY"))
            assertTrue(elements(foreground, "path").isNotEmpty())
        }
    }

    private fun xml(file: File): Element =
        DocumentBuilderFactory.newInstance()
            .apply {
                isNamespaceAware = true
                setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            }
            .newDocumentBuilder()
            .parse(file)
            .documentElement

    private fun elements(parent: Element, name: String): List<Element> {
        val nodes = parent.getElementsByTagName(name)
        return (0 until nodes.length).map { nodes.item(it) as Element }
    }

    private fun Element.android(name: String) = getAttributeNS(ANDROID, name)

    companion object {
        private const val ANDROID = "http://schemas.android.com/apk/res/android"
    }
}
