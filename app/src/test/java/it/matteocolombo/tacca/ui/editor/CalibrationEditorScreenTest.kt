package it.matteocolombo.tacca.ui.editor

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matteocolombo.tacca.domain.BuiltInMethod
import it.matteocolombo.tacca.domain.Coffee
import it.matteocolombo.tacca.domain.GrindSetting
import it.matteocolombo.tacca.domain.Grinder
import it.matteocolombo.tacca.domain.GrinderSpec
import it.matteocolombo.tacca.domain.Method
import it.matteocolombo.tacca.ui.theme.TaccaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CalibrationEditorScreenTest {
    @get:Rule val compose = createComposeRule()

    private val coffee = Coffee(1, "Brasile", 0)
    private val v60 = Method(3, null, BuiltInMethod.V60, BuiltInMethod.V60.icon, BuiltInMethod.V60.tint, 2)
    private val single = Grinder(1, "Encore", GrinderSpec(false, 40), 0)
    private val multi = Grinder(2, "JX", GrinderSpec(true, 40), 0)
    private val base = EditorUiState(
        loaded = true, coffees = listOf(coffee), methods = listOf(v60), grinders = listOf(single, multi),
        coffeeId = 1, methodId = 3, grinderId = 2, setting = GrindSetting(1, 30),
    )

    private fun show(state: EditorUiState) = compose.setContent { TaccaTheme { CalibrationEditorScreen(state, EditorActions.Noop) } }

    @Test fun showsReplaceWarningAndSummary() {
        show(base.copy(replaces = GrindSetting(2, 5)))
        compose.onNodeWithText("Replaces the current setting (2 turns + 5 steps)").assertIsDisplayed()
        compose.onNodeWithText("Save · 1 turn + 30 steps").assertIsDisplayed()
    }

    @Test fun singleTurnGrinderHidesTurnsStepper() {
        show(base.copy(grinderId = 1, setting = GrindSetting(0, 12)))
        compose.onNodeWithText("This grinder adjusts within a single turn: steps are enough.").performScrollTo().assertIsDisplayed()
        assertEquals(0, compose.onAllNodesWithContentDescription("More turns").fetchSemanticsNodes().size)
    }

    @Test fun saveDisabledWithoutCoffee() {
        show(base.copy(coffeeId = null))
        compose.onNodeWithText("Save · 1 turn + 30 steps").assertIsNotEnabled()
    }
}
