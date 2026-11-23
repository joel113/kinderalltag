package de.kinderalltag.text

import de.kinderalltag.domain.*
import de.kinderalltag.ui.AppThemeId
import de.kinderalltag.ui.DateTimeFormats
import de.kinderalltag.ui.resolveAppLocale
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.*
import org.junit.Test
import org.w3c.dom.Element

class LocalizationTest {
    private fun root(path: String) =
        DocumentBuilderFactory.newInstance()
            .apply { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
            .newDocumentBuilder()
            .parse(File("src/main/$path"))
            .documentElement

    private fun decode(value: String): String =
        value
            .removeSurrounding("\"")
            .replace("\\'", "'")
            .replace("\\\"", "\"")
            .replace("\\n", "\n")
            .replace("\\\\", "\\")

    private fun strings(language: String): Map<String, String> {
        val elements = root("res/$language/messages.xml").getElementsByTagName("string")
        return (0 until elements.length).associate {
            val element = elements.item(it) as Element
            element.getAttribute("name") to decode(element.textContent)
        }
    }

    private fun <T> inEnglish(block: () -> T): T {
        val english = strings("values-en")
        return TextCatalog.withRenderer(
            { key, args ->
                val pattern = english.getValue(key.resourceName)
                if (args.isEmpty()) pattern else String.format(Locale.UK, pattern, *args)
            },
            block,
        )
    }

    @Test
    fun systemLocaleResolutionPreservesRegionsAndFallsBackToGerman() {
        assertEquals(Locale.GERMANY, resolveAppLocale(listOf(Locale.FRENCH)))
        assertEquals(Locale.US, resolveAppLocale(listOf(Locale.FRENCH, Locale.US)))
        assertEquals(Locale.UK, resolveAppLocale(listOf(Locale.ENGLISH)))
        assertEquals(Locale.GERMANY, resolveAppLocale(listOf(Locale.GERMAN, Locale.US)))
    }

    @Test
    fun nestedRenderScopesDoNotLeakBetweenPreviewsOrCallers() {
        val original = text(TextKey.TASK)
        TextCatalog.withRenderer({ _, _ -> "outer" }) {
            assertEquals("outer", text(TextKey.TASK))
            TextCatalog.withRenderer({ _, _ -> "inner" }) {
                assertEquals("inner", text(TextKey.TASK))
            }
            assertEquals("outer", text(TextKey.TASK))
        }
        assertEquals(original, text(TextKey.TASK))
    }

    @Test
    fun everyTypedMessageHasGermanAndEnglishResourcesAndMatchingArguments() {
        val de = strings("values")
        val en = strings("values-en")
        val names = TextKey.entries.map { it.resourceName }
        assertEquals(names.size, names.toSet().size)
        assertEquals(names.toSet(), de.keys)
        assertEquals(de.keys, en.keys)
        val arguments = Regex("%([0-9]+)\\\$([a-z])")
        TextKey.entries.forEach { key ->
            val german = de.getValue(key.resourceName)
            val english = en.getValue(key.resourceName)
            assertTrue(key.name, english.isNotBlank())
            assertEquals(key.name, key.german, german)
            assertEquals(
                key.name,
                arguments.findAll(german).map { it.value }.toSet(),
                arguments.findAll(english).map { it.value }.toSet(),
            )
            val count = arguments.findAll(german).maxOfOrNull { it.groupValues[1].toInt() } ?: 0
            if (count > 0) {
                val values = Array<Any>(count) { "value" }
                String.format(Locale.GERMAN, german, *values)
                String.format(Locale.UK, english, *values)
            }
        }
    }

    @Test
    fun validationKeepsATypedMessageThatCanBeRenderedInEitherLanguage() {
        val task =
            TaskRevision(
                "id",
                "task",
                "johann",
                "",
                start = LocalDate.of(2026, 11, 23),
                effective = LocalDate.of(2026, 11, 23),
            )
        val error = assertThrows(LocalizedArgumentException::class.java) { task.validate() }
        assertEquals(TextKey.TITLE_1_TO_120_CHARACTERS, error.displayMessage.key)
        assertTrue(error.message.contains("Titel"))
        inEnglish { assertEquals("Title: 1 to 120 characters.", error.message) }
        assertTrue(error.message.contains("Titel"))
    }

    @Test
    fun themeAndHouseholdLabelsUpdateWithoutChangingStoredIdsOrUserText() {
        val ids = AppThemeId.entries.map { it.storedValue }
        val title = "Johanns Lieblingsbuch"
        val item =
            FamilyListItem(
                "gift",
                FamilyListKind.GIFTS,
                title,
                notes = "Für Weihnachten",
                recipient = AppointmentRecipient.JOHANN,
            )
        assertEquals("Garten", AppThemeId.GARDEN.label)
        inEnglish {
            assertEquals("Garden", AppThemeId.GARDEN.label)
            assertEquals("Household", HOUSEHOLD_SECTION.name)
            assertEquals("haushalt", HOUSEHOLD_SECTION.id)
            assertEquals(ids, AppThemeId.entries.map { it.storedValue })
            assertEquals(title, item.title)
            assertEquals("Für Weihnachten", item.notes)
        }
        assertEquals("Garten", AppThemeId.GARDEN.label)
    }

    @Test
    fun starterLabelsTranslateOnlyNewNamesAndKeepStablePresetIds() {
        val german = starterLabels(LabelGroup.OCCASION)
        val english = inEnglish { starterLabels(LabelGroup.OCCASION) }
        assertEquals(german.map { it.id }, english.map { it.id })
        assertEquals(listOf("Christmas", "Easter", "Birthday"), english.map { it.name })
        assertEquals("Weihnachten", german.first().name)
    }

    @Test
    fun englishSpeechSeparatorsPreserveQuantitiesAndReviewedProductNames() {
        assertEquals(
            listOf("milk", "bread", "1.5 litres of juice", "eggs"),
            splitShoppingSpeech("milk and bread, 1.5 litres of juice next item eggs", "en"),
        )
        assertEquals(
            listOf("Milch", "Brot", "1,5 Liter Saft"),
            splitShoppingSpeech("Milch und Brot, 1,5 Liter Saft", "de"),
        )
        assertEquals(
            listOf("salt and pepper", "milk"),
            shoppingImportTitles("salt and pepper\nmilk", emptyList()),
        )
    }

    @Test
    fun datesAndClockFormatsUseTheirSuppliedLocaleAndBerlinTime() {
        val day = LocalDate.of(2026, 11, 23)
        val de = DateTimeFormats(Locale.GERMANY, SimpleDateFormat("HH:mm", Locale.GERMANY))
        val en = DateTimeFormats(Locale.US, SimpleDateFormat("h:mm a", Locale.US))
        assertTrue(de.fullDate(day).contains("Montag"))
        assertTrue(en.fullDate(day).contains("Monday"))
        assertEquals("18:30", de.time(LocalTime.of(18, 30)))
        assertEquals("6:30 PM", en.time(LocalTime.of(18, 30)))
    }

    @Test
    fun pluralResourcesHaveBothFormsAndMatchTheTypedArgumentContracts() {
        val forms = mutableMapOf<String, Map<String, Map<String, String>>>()
        listOf("values", "values-en").forEach { language ->
            val plurals = root("res/$language/plurals.xml").getElementsByTagName("plurals")
            forms[language] =
                (0 until plurals.length).associate { index ->
                    val plural = plurals.item(index) as Element
                    val items = plural.getElementsByTagName("item")
                    plural.getAttribute("name") to
                        (0 until items.length).associate {
                            val item = items.item(it) as Element
                            item.getAttribute("quantity") to decode(item.textContent)
                        }
                }
        }
        assertEquals(
            QuantityKey.entries.map { it.resourceName }.toSet(),
            forms.getValue("values").keys,
        )
        assertEquals(forms.getValue("values").keys, forms.getValue("values-en").keys)
        QuantityKey.entries.forEach { key ->
            assertEquals(
                setOf("one", "other"),
                forms.getValue("values-en").getValue(key.resourceName).keys,
            )
            assertEquals(
                key.one,
                forms.getValue("values").getValue(key.resourceName).getValue("one"),
            )
            assertEquals(
                key.other,
                forms.getValue("values").getValue(key.resourceName).getValue("other"),
            )
        }
        assertEquals("1 von 1 Aufgabe erledigt", quantity(QuantityKey.TASK_PROGRESS, 1, 1, 1))
        assertEquals("0 von 0 Aufgaben erledigt", quantity(QuantityKey.TASK_PROGRESS, 0, 0, 0))
    }

    @Test
    fun reminderPayloadsKeepRawTitlesAndTranslateSubjectsAtDelivery() {
        val title = "Vitamin D"
        assertEquals("Haushalt: $title", reminderDisplayTitle(title, "haushalt", null, "Haushalt"))
        inEnglish {
            assertEquals(
                "Household: $title",
                reminderDisplayTitle(title, "haushalt", null, "Haushalt"),
            )
            assertEquals(
                "Both children: $title",
                reminderDisplayTitle(
                    title,
                    null,
                    AppointmentRecipient.BOTH_CHILDREN,
                    "Beide Kinder",
                ),
            )
            assertEquals(
                "Ferdinand: $title",
                reminderDisplayTitle(title, "ferdinand", null, "Ferdinand"),
            )
        }
    }

    @Test
    fun bothLocalesAreDeclaredAndAppCompatStorageDoesNotExposeAService() {
        val config = root("res/xml/locales_config.xml")
        val locales = config.getElementsByTagName("locale")
        assertEquals(
            listOf("de", "en"),
            (0 until locales.length).map {
                (locales.item(it) as Element).getAttribute("android:name")
            },
        )
        val manifest = root("AndroidManifest.xml")
        val services = manifest.getElementsByTagName("service")
        val service =
            (0 until services.length)
                .map { services.item(it) as Element }
                .single {
                    it.getAttribute("android:name").endsWith("AppLocalesMetadataHolderService")
                }
        assertEquals("false", service.getAttribute("android:enabled"))
        assertEquals("false", service.getAttribute("android:exported"))
        assertEquals(
            "true",
            (service.getElementsByTagName("meta-data").item(0) as Element).getAttribute(
                "android:value"
            ),
        )
    }
}
