package it.matteocolombo.tacca.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import it.matteocolombo.tacca.domain.AccentOption
import it.matteocolombo.tacca.domain.Tint
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeContrastTest {
    private fun contrast(a: Color, b: Color): Double {
        val la = a.luminance() + 0.05
        val lb = b.luminance() + 0.05
        return maxOf(la, lb).toDouble() / minOf(la, lb).toDouble()
    }

    private fun check(name: String, fg: Color, bg: Color, min: Double) {
        val ratio = contrast(fg, bg)
        assertTrue("$name contrast $ratio < $min", ratio >= min)
    }

    @Test fun allPairsMeetWcagAa() {
        for (dark in listOf(false, true)) for (accent in AccentOption.entries) {
            val c = taccaColors(dark, accent)
            val tag = "dark=$dark accent=$accent"
            check("$tag text/background", c.text, c.background, 4.5)
            check("$tag text/surface", c.text, c.surface, 4.5)
            check("$tag muted/background", c.textMuted, c.background, 4.5)
            check("$tag muted/surface", c.textMuted, c.surface, 4.5)
            check("$tag onAccent/accent", c.onAccent, c.accent, 4.5)
            check("$tag accentText/surface", c.accentText, c.surface, 4.5)
            check("$tag text/highlight", c.text, c.highlight, 4.5)
            check("$tag danger/surface", c.danger, c.surface, 4.5)
            for (t in Tint.entries) {
                check("$tag text/$t", c.text, c.tint(t), 4.5)
                check("$tag textOnTint/$t", c.textOnTint, c.tint(t), 4.5)
            }
        }
    }
}
