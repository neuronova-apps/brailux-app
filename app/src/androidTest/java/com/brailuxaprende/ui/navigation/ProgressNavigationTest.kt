package com.brailuxaprende.ui.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.brailuxaprende.R
import com.brailuxaprende.data.practice.PracticeProgress
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
                    practiceProgress = PracticeProgress(
                        level1CompletedSessions = 2,
                        level1TotalExercises = 12,
                        level1FirstAttemptCorrect = 9,
                        level1LastPracticeDate = "2026-08-07",
                    ),
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
        composeRule.onNodeWithText(context.getString(R.string.progress_level_1_title))
            .assertIsDisplayed()
        assertProgressValue(R.string.progress_sessions_label, "2")
        assertProgressValue(R.string.progress_exercises_label, "12")
        assertProgressValue(R.string.progress_first_attempt_label, "9")
        assertProgressValue(R.string.progress_accuracy_label, "75%")
        assertProgressValue(R.string.progress_last_practice_label, "7 ago. 2026")
    }

    private fun assertProgressValue(labelResId: Int, value: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        composeRule.onNodeWithContentDescription(
            "${context.getString(labelResId)}, $value",
        ).assertIsDisplayed()
    }
}
