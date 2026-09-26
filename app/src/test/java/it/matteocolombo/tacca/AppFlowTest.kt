package it.matteocolombo.tacca

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h891dp")
class AppFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun createGrinderCoffeeAndCalibration() {
        // Grinder
        compose.onNodeWithText("Grinders").performClick()
        compose.onNodeWithText("Add grinder").performClick()
        compose.onNodeWithContentDescription("Name").performTextInput("JX-Pro")
        compose.onNodeWithText("Save").performClick()
        compose.waitUntilAtLeastOneExists(hasText("JX-Pro"), 5_000)

        // Calibration with an inline new coffee
        compose.onNodeWithText("Calibrations").performClick()
        compose.waitUntilAtLeastOneExists(hasText("New calibration"), 5_000)
        compose.onNodeWithText("New calibration").performClick()
        compose.onAllNodesWithText("New").onFirst().performClick()
        compose.onNodeWithContentDescription("Name").performTextInput("Brasile")
        compose.onNodeWithText("Save").performClick()
        compose.waitUntilAtLeastOneExists(hasText("Brasile"), 5_000)
        compose.onNodeWithText("V60").performClick()
        repeat(3) { compose.onNodeWithContentDescription("More steps").performScrollTo().performClick() }
        compose.onNodeWithText("Save · 3 steps").performClick()

        // Detail, then back to home
        compose.waitUntilAtLeastOneExists(hasContentDescription("Dial: 3 steps"), 5_000)
        compose.onNodeWithContentDescription("Dial: 3 steps").assertIsDisplayed()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.waitUntilAtLeastOneExists(hasText("Brasile"), 5_000)
        compose.onNodeWithText("JX-Pro").assertIsDisplayed()
    }

    @Test fun newGrinderCreatedFromEditorEndsUpSelected() {
        // One existing grinder: with a single grinder, the editor pre-selects it by default.
        compose.onNodeWithText("Grinders").performClick()
        compose.onNodeWithText("Add grinder").performClick()
        compose.onNodeWithContentDescription("Name").performTextInput("Grinder A")
        compose.onNodeWithText("Save").performClick()
        compose.waitUntilAtLeastOneExists(hasText("Grinder A"), 5_000)

        compose.onNodeWithText("Calibrations").performClick()
        compose.waitUntilAtLeastOneExists(hasText("New calibration"), 5_000)
        compose.onNodeWithText("New calibration").performClick()
        compose.onAllNodesWithText("New").onFirst().performClick()
        compose.onNodeWithContentDescription("Name").performTextInput("Etiopia")
        compose.onNodeWithText("Save").performClick()
        compose.waitUntilAtLeastOneExists(hasText("Etiopia"), 5_000)

        compose.onNodeWithText("Grinder A").assertIsSelected()

        // "+ New" grinder chip is the second "New" chip on the editor screen (after the coffee section's).
        compose.onAllNodesWithText("New")[1].performClick()
        compose.onNodeWithContentDescription("Name").performTextInput("Grinder B")
        compose.onNodeWithText("Save").performClick()

        // Back in the editor, the newly created grinder is the one selected, not the pre-existing one.
        compose.waitUntilAtLeastOneExists(hasText("Grinder B"), 5_000)
        compose.onNodeWithText("Grinder B").assertIsSelected()
    }

    @Test @Config(qualifiers = "it-w411dp-h891dp")
    fun italianLabels() {
        compose.onNodeWithText("Macine").assertIsDisplayed()
        compose.onNodeWithText("Metodi").assertIsDisplayed()
    }
}
