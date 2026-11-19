package de.kinderalltag.domain

import org.junit.Assert.*
import org.junit.Test

class ShoppingVoiceImportTest {
    @Test
    fun singleUtteranceBecomesSeparateShoppingTitles() {
        assertEquals(listOf("Milch", "Brot", "Eier"), splitShoppingSpeech("Milch, Brot und Eier."))
        assertEquals(
            listOf("Milch", "Brot", "Eier"),
            splitShoppingSpeech("Milch nächster Artikel Brot neuer Eintrag Eier"),
        )
    }

    @Test
    fun decimalQuantitiesAndWordsContainingUndArePreserved() {
        assertEquals(
            listOf("1,5 Liter Milch", "500 g Mehl", "Hundekekse"),
            splitShoppingSpeech("1,5 Liter Milch; 500 g Mehl und Hundekekse"),
        )
        assertEquals(listOf("2 Äpfel", "3 Birnen"), splitShoppingSpeech("2 Äpfel,3 Birnen"))
    }

    @Test
    fun explicitSpokenSeparatorsAndEmptyPartsAreHandled() {
        assertEquals(
            listOf("Äpfel", "Öl", "Brot"),
            splitShoppingSpeech("Äpfel Komma Öl neue Zeile Brot; ;\n"),
        )
        assertTrue(splitShoppingSpeech(" , ; und ").isEmpty())
        assertEquals(listOf("Salz"), splitShoppingSpeech("Salz"))
    }

    @Test
    fun reviewKeepsCompoundProductsAndSkipsOnlyKnownOpenOrRepeatedTitles() {
        val review = "Essig und Öl\n Milch \nMILCH\nBrot\n\n2 × Brot"
        assertEquals(
            listOf("Essig und Öl", "Milch", "2 × Brot"),
            shoppingImportTitles(review, listOf("Brot")),
        )
        assertEquals(listOf("Milch"), shoppingImportTitles("Milch", emptyList()))
    }

    @Test
    fun duplicateMatchingIgnoresWhitespaceCaseButPreservesReviewedDisplayText() {
        assertEquals(
            listOf("2 × Eier"),
            shoppingImportTitles("1,5  Liter Milch\n2 × Eier", listOf("1,5 Liter MILCH")),
        )
    }

    @Test
    fun limitsRejectRatherThanTruncateImportOrLongTitles() {
        validateShoppingImport((1..50).map { "Artikel $it" })
        assertThrows(IllegalArgumentException::class.java) {
            validateShoppingImport((1..51).map { "Artikel $it" })
        }
        assertThrows(IllegalArgumentException::class.java) { validateShoppingImport(emptyList()) }
        assertThrows(IllegalArgumentException::class.java) {
            validateShoppingImport(listOf("x".repeat(121)))
        }
    }
}
