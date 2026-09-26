package it.matteocolombo.tacca.ui.detail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matteocolombo.tacca.domain.BuiltInMethod
import it.matteocolombo.tacca.domain.Calibration
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
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h891dp")
class DetailScreenTest {
    @get:Rule val compose = createComposeRule()

    private fun cal(setting: GrindSetting, stepsPerTurn: Int? = 40) = Calibration(
        id = 1,
        coffee = Coffee(1, "Etiopia Yirgacheffe Kochere — lavato", 0),
        method = Method(3, null, BuiltInMethod.V60, BuiltInMethod.V60.icon, BuiltInMethod.V60.tint, 2),
        grinder = Grinder(1, "1Zpresso JX-Pro", GrinderSpec(true, stepsPerTurn), 0),
        setting = setting, notes = "", updatedAt = 0,
    )

    private fun show(c: Calibration) = compose.setContent {
        TaccaTheme { DetailScreen(DetailUiState(loading = false, calibration = c), {}, {}, {}) }
    }

    @Test fun showsThreeStepTiles() {
        show(cal(GrindSetting(2, 12)))
        compose.onNodeWithText("Close to zero").assertIsDisplayed()
        compose.onNodeWithText("Turn 2 full turns").assertIsDisplayed()
        compose.onNodeWithText("Add 12 steps").assertIsDisplayed()
    }

    @Test fun zeroTurnsHidesTurnsTile() {
        show(cal(GrindSetting(0, 5)))
        assertEquals(0, compose.onAllNodesWithText("Turn 0 full turns").fetchSemanticsNodes().size)
        compose.onNodeWithText("Add 5 steps").assertIsDisplayed()
    }

    @Test fun unknownStepsPerTurnShowsSetting() {
        show(cal(GrindSetting(1, 17), stepsPerTurn = null))
        compose.onNodeWithContentDescription("Dial: 1 turn + 17 steps").assertIsDisplayed()
        compose.onNodeWithText("Add 17 steps").assertIsDisplayed()
    }
}
