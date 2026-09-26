package it.matteocolombo.tacca.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextMatchTest {
    @Test fun normalizeStripsDiacriticsAndCase() {
        assertEquals("finca el paraiso", TextMatch.normalize("  Finca El Paraíso "))
        assertEquals("cafe", TextMatch.normalize("CAFÉ"))
    }

    @Test fun matchesAnyField() {
        assertTrue(TextMatch.matches("paraiso", "Colombia Huila Finca El Paraíso", "V60"))
        assertTrue(TextMatch.matches("v6", "Brasile", "V60"))
        assertFalse(TextMatch.matches("moka", "Brasile", "V60"))
    }

    @Test fun blankQueryMatchesEverything() {
        assertTrue(TextMatch.matches("   ", "anything"))
    }
}
