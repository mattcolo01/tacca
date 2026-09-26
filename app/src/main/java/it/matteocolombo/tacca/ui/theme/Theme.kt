package it.matteocolombo.tacca.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import it.matteocolombo.tacca.domain.AccentOption
import it.matteocolombo.tacca.domain.AppSettings
import it.matteocolombo.tacca.domain.ThemeMode

val LocalTaccaColors = staticCompositionLocalOf { taccaColors(dark = false, accent = AccentOption.TERRACOTTA) }

object TaccaTheme {
    val colors: TaccaColors
        @Composable @ReadOnlyComposable get() = LocalTaccaColors.current
}

@Composable
fun TaccaTheme(settings: AppSettings = AppSettings(), content: @Composable () -> Unit) {
    val dark = when (settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colors = remember(dark, settings.accent) { taccaColors(dark, settings.accent) }
    CompositionLocalProvider(LocalTaccaColors provides colors) {
        MaterialTheme(colorScheme = colors.toMaterial(), typography = MaterialTypography, content = content)
    }
}

private fun TaccaColors.toMaterial(): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = accent,
        onPrimary = onAccent,
        primaryContainer = highlight,
        onPrimaryContainer = text,
        secondary = accent,
        onSecondary = onAccent,
        background = background,
        onBackground = text,
        surface = surface,
        onSurface = text,
        surfaceVariant = background,
        onSurfaceVariant = textMuted,
        surfaceContainerLowest = surface,
        surfaceContainerLow = surface,
        surfaceContainer = surface,
        surfaceContainerHigh = surface,
        surfaceContainerHighest = background,
        inverseSurface = text,
        inverseOnSurface = surface,
        outline = outline,
        outlineVariant = outline,
        error = danger,
    )
}
