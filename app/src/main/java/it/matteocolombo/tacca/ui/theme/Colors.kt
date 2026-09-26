package it.matteocolombo.tacca.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import it.matteocolombo.tacca.domain.AccentOption
import it.matteocolombo.tacca.domain.Tint

/** Palette from the mockup (light) and its espresso-toned dark counterpart (spec section 6). */
@Immutable
data class TaccaColors(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val outline: Color,
    val dashed: Color,
    val tickInactive: Color,
    val text: Color,
    val textMuted: Color,
    val textOnTint: Color,
    val highlight: Color,
    /** Fills (FAB, primary button, active ticks). */
    val accent: Color,
    /** Accent used for text and numbers on surfaces. */
    val accentText: Color,
    val onAccent: Color,
    val danger: Color,
    private val tints: Map<Tint, Color>,
) {
    fun tint(t: Tint): Color = tints.getValue(t)
}

private val lightTints = mapOf(
    Tint.PEACH to Color(0xFFF7DCCB),
    Tint.SAND to Color(0xFFF2E4BC),
    Tint.SKY to Color(0xFFD7E3EE),
    Tint.SAGE to Color(0xFFDCE6D1),
    Tint.LILAC to Color(0xFFE6DDEE),
)

private val darkTints = mapOf(
    Tint.PEACH to Color(0xFF4A3226),
    Tint.SAND to Color(0xFF463B22),
    Tint.SKY to Color(0xFF2A3844),
    Tint.SAGE to Color(0xFF2F3A2A),
    Tint.LILAC to Color(0xFF3A3044),
)

fun taccaColors(dark: Boolean, accent: AccentOption): TaccaColors {
    val accentColor = Color(accent.argb)
    return if (!dark) {
        TaccaColors(
            isDark = false,
            background = Color(0xFFF6F0E8),
            surface = Color(0xFFFFFCF8),
            outline = Color(0xFFEADFD3),
            dashed = Color(0xFFD9C9B8),
            tickInactive = Color(0xFFE3D6C8),
            text = Color(0xFF2A1C13),
            textMuted = Color(0xFF6F5B4C),
            textOnTint = Color(0xFF5E4A3C),
            highlight = Color(0xFFF4DCCD),
            accent = accentColor,
            accentText = accentColor,
            onAccent = Color.White,
            danger = Color(0xFFB3261E),
            tints = lightTints,
        )
    } else {
        TaccaColors(
            isDark = true,
            background = Color(0xFF1C1410),
            surface = Color(0xFF261C16),
            outline = Color(0xFF3A2C23),
            dashed = Color(0xFF4A392D),
            tickInactive = Color(0xFF4A392D),
            text = Color(0xFFF3E9DF),
            textMuted = Color(0xFFB9A594),
            textOnTint = Color(0xFFD9C8B8),
            highlight = Color(0xFF4A2E22),
            accent = accentColor,
            accentText = lerp(accentColor, Color.White, 0.35f),
            onAccent = Color.White,
            danger = Color(0xFFFFB4AB),
            tints = darkTints,
        )
    }
}
