package de.kinderalltag.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ChildTest {
    @Test fun existingProfilesHaveDistinctDefaultColors() {
        assertEquals(BearColor.BLUE, Child("ferdinand", "Ferdinand").bearColor)
        assertEquals(BearColor.ROSE, Child("johann", "Johann").bearColor)
    }

    @Test fun savedColorsRoundTripAndUnknownColorsFallBack() {
        BearColor.entries.forEach { color ->
            assertEquals(color, BearColor.fromStored(color.name, "johann"))
        }
        assertEquals(BearColor.BLUE, BearColor.fromStored(null, "ferdinand"))
        assertEquals(BearColor.ROSE, BearColor.fromStored("FUTURE_COLOR", "johann"))
    }
}
