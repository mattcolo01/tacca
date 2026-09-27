package it.matteocolombo.tacca.ui.components

import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matteocolombo.tacca.domain.GrindSetting
import it.matteocolombo.tacca.ui.navigation.TopLevelDestination
import it.matteocolombo.tacca.ui.theme.TaccaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ComponentsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun stepperIncrementsAndDisablesAtBound() {
        compose.setContent {
            TaccaTheme {
                var v by remember { mutableIntStateOf(0) }
                Stepper(
                    value = v.toString(), onDecrement = { v-- }, onIncrement = { v++ },
                    decrementLabel = "Fewer", incrementLabel = "More", canDecrement = v > 0,
                )
            }
        }
        compose.onNodeWithContentDescription("Fewer").assertIsNotEnabled()
        compose.onNodeWithContentDescription("More").performClick()
        compose.onNodeWithText("1").assertIsDisplayed()
    }

    @Test fun dialDescribesTheSetting() {
        compose.setContent { TaccaTheme { Dial(GrindSetting(2, 12), stepsPerTurn = 40) } }
        compose.onNodeWithContentDescription("Dial: 2 turns + 12 steps").assertIsDisplayed()
    }

    @Test fun bottomBarSelectsAndCallsBack() {
        var picked: TopLevelDestination? = null
        compose.setContent {
            TaccaTheme {
                var sel by remember { mutableStateOf(TopLevelDestination.CALIBRATIONS) }
                TaccaBottomBar(selected = sel, onSelect = { sel = it; picked = it })
            }
        }
        compose.onNodeWithText("Calibrations").assertIsSelected()
        compose.onNodeWithText("Grinders").performClick()
        compose.onNodeWithText("Grinders").assertIsSelected()
        assertEquals(TopLevelDestination.GRINDERS, picked)
    }

    @Test fun filterPillHasAtLeast48dpTouchTarget() {
        compose.setContent {
            TaccaTheme { FilterPill(label = "All", selected = false, onClick = {}) }
        }
        compose.onNodeWithText("All").assertHeightIsAtLeast(48.dp)
    }

    @Test fun filterPillDisplaysTextInLazyRow() {
        compose.setContent {
            TaccaTheme {
                LazyRow {
                    item { FilterPill(label = "Espresso", selected = false, onClick = {}) }
                }
            }
        }
        compose.onNodeWithText("Espresso").assertIsDisplayed()
    }

    @Test fun confirmDialogInvokesConfirm() {
        var confirmed = false
        compose.setContent {
            TaccaTheme { ConfirmDialog("Delete “V60”?", "3 calibrations will be deleted too.", "Delete", { confirmed = true }, {}) }
        }
        compose.onNodeWithText("Delete").performClick()
        assertEquals(true, confirmed)
    }
}
