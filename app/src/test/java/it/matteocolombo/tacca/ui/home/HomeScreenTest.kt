package it.matteocolombo.tacca.ui.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matteocolombo.tacca.domain.BuiltInMethod
import it.matteocolombo.tacca.domain.Calibration
import it.matteocolombo.tacca.domain.Coffee
import it.matteocolombo.tacca.domain.GrindSetting
import it.matteocolombo.tacca.domain.Grinder
import it.matteocolombo.tacca.domain.GrinderSpec
import it.matteocolombo.tacca.domain.Method
import it.matteocolombo.tacca.ui.theme.TaccaTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {
    @get:Rule val compose = createComposeRule()

    private val v60 = Method(3, null, BuiltInMethod.V60, BuiltInMethod.V60.icon, BuiltInMethod.V60.tint, 2)

    private fun show(state: HomeUiState, onNew: () -> Unit = {}) = compose.setContent {
        TaccaTheme { HomeScreen(state, HomeActions.Noop.copy(onNew = onNew)) }
    }

    @Test fun emptyStateInvitesToStart() {
        var clicked = false
        show(HomeUiState(loading = false, needsSetup = true)) { clicked = true }
        compose.onNodeWithText("No calibrations yet").assertIsDisplayed()
        compose.onNodeWithText("New calibration").performClick()
        assertTrue(clicked)
    }

    @Test fun longCoffeeNameKeepsSettingVisible() {
        val longName = "Colombia Huila Finca El Paraíso — lavato, lotto speciale doppia fermentazione anaerobica con frutta tropicale"
        val cal = Calibration(
            1, Coffee(1, longName, 0), v60, Grinder(1, "1Zpresso JX-Pro con un nome molto lungo", GrinderSpec(true, 40), 0),
            GrindSetting(2, 12), "", 0,
        )
        show(HomeUiState(loading = false, hasCalibrations = true, chips = listOf(v60), groups = listOf(HomeGroup(cal.coffee, listOf(cal)))))
        compose.onNodeWithText(longName).assertIsDisplayed()
        compose.onNodeWithText("12", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("2", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test fun fabHasDescription() {
        show(HomeUiState(loading = false, hasCalibrations = true))
        compose.onNodeWithContentDescription("New calibration").assertIsDisplayed()
    }

    @Test fun longCoffeeFilterNameKeepsClearIconVisible() {
        val longName = "Colombia Huila Finca El Paraíso — lavato, lotto speciale doppia fermentazione anaerobica con frutta tropicale"
        val cal = Calibration(
            1, Coffee(2, "Brasile", 0), v60, Grinder(1, "JX-Pro", GrinderSpec(true, 40), 0),
            GrindSetting(2, 12), "", 0,
        )
        show(
            HomeUiState(
                loading = false,
                hasCalibrations = true,
                coffeeFilter = Coffee(1, longName, 0),
                chips = listOf(v60),
                groups = listOf(HomeGroup(cal.coffee, listOf(cal))),
            ),
        )
        compose.onNodeWithContentDescription("Remove coffee filter").assertIsDisplayed()
        val pillBounds = compose.onNodeWithContentDescription("Remove coffee filter", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val labelBounds = compose.onNodeWithText(longName, useUnmergedTree = true).getUnclippedBoundsInRoot()
        val pillWidth = pillBounds.right - pillBounds.left
        val labelWidth = labelBounds.right - labelBounds.left
        // The label is clamped well short of its natural (700dp+) width...
        assertTrue(labelWidth < 200.dp)
        // ...leaving room in the chip for the leading bean icon, both gaps, the padding and the
        // trailing X (16 + 6 + 6 + 14 + 32 = 74dp) instead of being pushed out entirely.
        assertTrue(pillWidth - labelWidth >= 70.dp)
    }
}
