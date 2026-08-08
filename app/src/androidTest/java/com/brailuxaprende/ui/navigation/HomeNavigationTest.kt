package com.brailuxaprende.ui.navigation

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasNoClickAction
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
class HomeNavigationTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun homeActionsNavigateAndFutureContentIsNotInteractive() {
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

        composeRule.onAllNodesWithText(context.getString(R.string.nav_learn)).assertCountEquals(1)
        composeRule.onAllNodesWithText(context.getString(R.string.nav_play)).assertCountEquals(1)
        composeRule.onAllNodesWithText(context.getString(R.string.nav_progress)).assertCountEquals(1)
        composeRule.onAllNodesWithText(context.getString(R.string.progress_title)).assertCountEquals(0)

        val comingSoon = context.getString(R.string.home_coming_soon)
        composeRule.onNodeWithText(context.getString(R.string.home_daily_practice))
            .performScrollTo()
            .assertIsDisplayed()
            .assert(hasNoClickAction())
        composeRule.onNodeWithText(context.getString(R.string.home_daily_challenge))
            .performScrollTo()
            .assertIsDisplayed()
            .assert(hasNoClickAction())
        composeRule.onAllNodesWithText(comingSoon).assertCountEquals(2)

        composeRule.onNodeWithText(context.getString(R.string.home_continue_learning))
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithText(context.getString(R.string.learn_title)).assertIsDisplayed()

        composeRule.onNodeWithText(context.getString(R.string.nav_home)).performClick()
        composeRule.onNodeWithText(context.getString(R.string.home_access_practice))
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithText(context.getString(R.string.practice_title)).assertIsDisplayed()
    }
}
