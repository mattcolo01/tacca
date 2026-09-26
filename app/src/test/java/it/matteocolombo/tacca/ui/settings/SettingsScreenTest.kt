package it.matteocolombo.tacca.ui.settings

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import org.junit.Assert.assertFalse
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

    @Test fun messageEffectShowsSnackbarBeforeConsumingIt() {
        val snackbar = SnackbarHostState()
        var message by mutableStateOf<SettingsMessage?>(SettingsMessage.Exported)
        var consumed = false
        compose.setContent {
            SettingsMessageEffect(
                message = message,
                snackbar = snackbar,
                textFor = { "Data exported" },
                onConsumed = { consumed = true },
            )
        }

        compose.waitUntil(5_000) { snackbar.currentSnackbarData?.visuals?.message == "Data exported" }
        // showSnackbar() suspends until the snackbar is dismissed, so at this point it has not
        // returned yet: onConsumed must not have run (see review round 1 — consuming first would
        // race the LaunchedEffect key change and could cut the snackbar off).
        assertFalse(consumed)

        snackbar.currentSnackbarData?.dismiss()
        compose.waitUntil(5_000) { consumed }
    }
}
