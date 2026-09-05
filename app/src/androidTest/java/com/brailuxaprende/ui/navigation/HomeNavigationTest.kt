package com.brailuxaprende.ui.navigation

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
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
    fun homeActionsNavigateAndDailyPracticeIsAvailable() {
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
                    onLevel1SessionCompleted = { _, onRecorded -> onRecorded(null) },
                )
            }
        }

        composeRule.onNodeWithText(context.getString(R.string.welcome_start)).performClick()

        composeRule.onAllNodesWithText(context.getString(R.string.nav_learn)).assertCountEquals(1)
        composeRule.onAllNodesWithText(context.getString(R.string.nav_play)).assertCountEquals(1)
        composeRule.onAllNodesWithText(context.getString(R.string.nav_progress)).assertCountEquals(1)
        composeRule.onAllNodesWithText(context.getString(R.string.progress_title)).assertCountEquals(0)
        composeRule.onAllNodes(
            hasText(context.getString(R.string.home_title)) and isHeading(),
        ).assertCountEquals(0)
        composeRule.onAllNodesWithText(context.getString(R.string.home_welcome_message))
            .assertCountEquals(0)
        composeRule.onAllNodesWithText(context.getString(R.string.home_continue_learning))
            .assertCountEquals(0)

        composeRule.onNodeWithText(context.getString(R.string.home_daily_practice))
            .performScrollTo()
            .assertIsDisplayed()
            .assertHasClickAction()
        composeRule.onNodeWithText(context.getString(R.string.home_daily_challenge))
            .performScrollTo()
            .assertIsDisplayed()
            .assertHasClickAction()
        composeRule.onNodeWithText(context.getString(R.string.home_access_practice))
            .performScrollTo()
            .assertIsDisplayed()
            .assertHasClickAction()
        composeRule.onAllNodesWithText(context.getString(R.string.home_xp_title))
            .assertCountEquals(0)
        composeRule.onAllNodesWithText(context.getString(R.string.home_streak_title))
            .assertCountEquals(0)
        composeRule.onAllNodesWithText(context.getString(R.string.home_weekly_practice_title))
            .assertCountEquals(0)
        composeRule.onAllNodesWithText(context.getString(R.string.home_coming_soon))
            .assertCountEquals(0)
        composeRule.onAllNodesWithText(context.getString(R.string.home_more_options))
            .assertCountEquals(0)
        composeRule.onAllNodesWithText(context.getString(R.string.home_access_about))
            .assertCountEquals(0)

        composeRule.onNodeWithContentDescription(
            context.getString(R.string.home_open_settings),
        )
            .assertIsDisplayed()
            .assertHasClickAction()
            .performClick()
        composeRule.onNodeWithText(context.getString(R.string.settings_title))
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            context.getString(R.string.settings_open_about_accessibility),
        )
            .performScrollTo()
            .assertIsDisplayed()
            .assertHasClickAction()
            .performClick()
        composeRule.onNodeWithText(context.getString(R.string.about_title))
            .assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.action_back))
            .assertIsDisplayed()
            .performClick()
        composeRule.onNodeWithText(context.getString(R.string.settings_title))
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.action_back))
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        composeRule.onNodeWithText(context.getString(R.string.nav_learn)).performClick()
        composeRule.onNode(
            hasText(context.getString(R.string.learn_title)) and isHeading(),
        ).assertIsDisplayed()

        composeRule.onNodeWithText(context.getString(R.string.nav_home)).performClick()
        composeRule.onNodeWithText(context.getString(R.string.home_access_practice))
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithText(context.getString(R.string.practice_title)).assertIsDisplayed()
    }
}
