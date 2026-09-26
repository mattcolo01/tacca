package it.matteocolombo.tacca.ui.methods

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matteocolombo.tacca.domain.Method
import it.matteocolombo.tacca.domain.MethodIcon
import it.matteocolombo.tacca.domain.Tint
import it.matteocolombo.tacca.ui.theme.TaccaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Robolectric's default window is smaller than the bottom sheet's content, so a normal phone
 * size is set explicitly; otherwise the icon/tint pickers are laid out but not "displayed" and
 * the click on them does not register.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h891dp")
class MethodSheetTest {
    @get:Rule val compose = createComposeRule()

    @Test fun picksIconAndTint() {
        var result: Triple<String, MethodIcon, Tint>? = null
        compose.setContent {
            TaccaTheme { MethodSheet("New method", method = null, existingNames = emptySet(), onSave = { n, i, t -> result = Triple(n, i, t) }, onDismiss = {}) }
        }
        compose.onNodeWithContentDescription("Name").performTextInput("Chemex")
        compose.onNodeWithContentDescription("Drop").performClick()
        compose.onNodeWithContentDescription("Lilac").performClick()
        compose.onNodeWithText("Save").performClick()
        assertEquals(Triple("Chemex", MethodIcon.DROP, Tint.LILAC), result)
    }

    @Test fun flagsDuplicateAgainstABuiltInMethodDisplayName() {
        // "v60" simulates what MethodsEntry merges into existingNames for the localized
        // built-in method names, since the view model itself has no Resources to localize with.
        val chemex = Method(1, "Chemex", null, MethodIcon.DROP, Tint.SKY, 5)
        compose.setContent {
            TaccaTheme {
                MethodSheet("Edit method", method = chemex, existingNames = setOf("v60"), onSave = { _, _, _ -> }, onDismiss = {})
            }
        }
        compose.onNodeWithContentDescription("Name").performTextClearance()
        compose.onNodeWithContentDescription("Name").performTextInput("V60")
        compose.onNodeWithText("A method with this name already exists").assertIsDisplayed()
    }
}
