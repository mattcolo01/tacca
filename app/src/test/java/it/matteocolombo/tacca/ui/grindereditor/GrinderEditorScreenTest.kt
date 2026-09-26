package it.matteocolombo.tacca.ui.grindereditor

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matteocolombo.tacca.ui.theme.TaccaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Robolectric's default window (320x470dp) is smaller than this scrollable editor screen, so a
 * normal phone size is set explicitly; otherwise content past the first fold is laid out but not
 * "displayed" and assertIsDisplayed() fails even though the real app renders it fine.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h891dp")
class GrinderEditorScreenTest {
    @get:Rule val compose = createComposeRule()

    private fun show(state: GrinderEditorUiState) = compose.setContent {
        TaccaTheme { GrinderEditorScreen(state, {}, {}, {}, {}, {}, {}, {}, {}) }
    }

    @Test fun singleTurnShowsTotalStepsAndPreview() {
        show(GrinderEditorUiState(loaded = true, name = "Encore", multiTurn = false, stepsPerTurn = 40))
        compose.onNodeWithText("Total steps").assertIsDisplayed()
        compose.onNodeWithText("12 steps").assertIsDisplayed()
    }

    @Test fun blankNameDisablesSave() {
        show(GrinderEditorUiState(loaded = true, name = " "))
        compose.onNodeWithText("Save").assertIsNotEnabled()
    }

    @Test fun showsOutOfRangeWarningAndDelete() {
        show(GrinderEditorUiState(loaded = true, isNew = false, name = "JX", outOfRangeCount = 2))
        compose.onNodeWithText("2 calibrations are outside the new range").assertIsDisplayed()
        compose.onNodeWithText("Delete grinder").assertIsDisplayed()
    }
}
