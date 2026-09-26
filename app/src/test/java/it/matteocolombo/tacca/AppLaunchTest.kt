package it.matteocolombo.tacca

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Temporary smoke test; replaced by AppFlowTest in Task 17.
@RunWith(AndroidJUnit4::class)
class AppLaunchTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun launchesAndShowsAppName() {
        compose.onNodeWithText("Tacca").assertIsDisplayed()
    }
}
