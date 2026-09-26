package it.matteocolombo.tacca.ui.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matteocolombo.tacca.domain.AccentOption
import it.matteocolombo.tacca.domain.ThemeMode
import it.matteocolombo.tacca.ui.theme.TaccaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Robolectric's default window (320x470dp) is smaller than this scrollable settings screen, so a
 * normal phone size is set explicitly; otherwise content past the first fold is laid out but not
 * "displayed" and assertIsDisplayed() fails even though the real app renders it fine.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h891dp")
class SettingsScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun pickingThemeAndAccentCallsBack() {
        var theme: ThemeMode? = null
        var accent: AccentOption? = null
        compose.setContent {
            TaccaTheme {
                SettingsScreen(
                    state = SettingsUiState(),
                    languageLabel = "System default",
                    versionName = "1.0.0",
                    actions = SettingsActions.Noop.copy(onTheme = { theme = it }, onAccent = { accent = it }),
                )
            }
        }
        compose.onNodeWithText("Dark").performClick()
        compose.onNodeWithContentDescription("Plum").performClick()
        compose.onNodeWithText("Version 1.0.0").assertIsDisplayed()
        assertEquals(ThemeMode.DARK, theme)
        assertEquals(AccentOption.PLUM, accent)
    }
}
