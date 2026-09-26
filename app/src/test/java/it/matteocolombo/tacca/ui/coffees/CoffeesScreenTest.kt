package it.matteocolombo.tacca.ui.coffees

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matteocolombo.tacca.ui.theme.TaccaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CoffeesScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun emptyStateOffersAdd() {
        compose.setContent { TaccaTheme { CoffeesScreen(CoffeesUiState(loading = false), {}, {}, {}, {}, {}, {}, {}) } }
        compose.onNodeWithText("No coffees yet. Add the first one.").assertIsDisplayed()
    }

    @Test fun sheetShowsDuplicateHint() {
        compose.setContent {
            TaccaTheme { CoffeeSheet("New coffee", "", setOf("brasile"), onSave = {}, onDismiss = {}) }
        }
        compose.onNodeWithContentDescription("Name").performTextInput("Brasile")
        compose.onNodeWithText("A coffee with this name already exists").assertIsDisplayed()
    }
}
