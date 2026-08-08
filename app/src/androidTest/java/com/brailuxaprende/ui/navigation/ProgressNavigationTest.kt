package com.brailuxaprende.ui.navigation

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.brailuxaprende.R
import com.brailuxaprende.data.settings.AccessibilityPreferences
import com.brailuxaprende.ui.theme.BrailuxAprendeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProgressNavigationTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun homeCardAndBottomBarOpenTheFunctionalProgressScreen() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        composeRule.setContent {
            BrailuxAprendeTheme {
                BrailuxApp(
                    preferences = AccessibilityPreferences(),
                    onSoundEnabledChange = {},
                    onVibrationEnabledChange = {},
                    onHighContrastEnabledChange = {},
                    onTextSizeChange = {},
                    onAppearanceChange = {},
                    onSeasonalThemesEnabledChange = {},
                    onLevel1SessionCompleted = { _, onRecorded -> onRecorded(true) },
                )
            }
        }

        composeRule.onNodeWithText(context.getString(R.string.welcome_start)).performClick()
        composeRule.onNodeWithText(context.getString(R.string.home_access_progress))
            .performScrollTo()
            .performClick()
        assertFunctionalProgressScreen()

        composeRule.onNodeWithText(context.getString(R.string.nav_home)).performClick()
        composeRule.onNodeWithText(context.getString(R.string.nav_progress)).performClick()
        assertFunctionalProgressScreen()
    }

    private fun assertFunctionalProgressScreen() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        composeRule.onNodeWithText(
            context.getString(R.string.progress_no_level_1_practice),
        ).assertIsDisplayed()
        composeRule.onAllNodesWithText(
            context.getString(R.string.content_coming_later),
        ).assertCountEquals(0)
    }
}
