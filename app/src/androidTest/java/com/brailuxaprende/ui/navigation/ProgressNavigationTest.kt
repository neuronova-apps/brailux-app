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
import com.brailuxaprende.data.learn.LearningProgress
import com.brailuxaprende.data.practice.PracticeProgress
import com.brailuxaprende.data.settings.AccessibilityPreferences
import com.brailuxaprende.learning.LearningLesson
import com.brailuxaprende.practice.DailyMiniAchievement
import com.brailuxaprende.practice.EngagementProgress
import com.brailuxaprende.practice.PermanentAchievement
import com.brailuxaprende.practice.PracticeDate
import com.brailuxaprende.ui.theme.BrailuxAprendeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProgressNavigationTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun bottomBarOpensTheFunctionalProgressScreen() {
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
                        level2CompletedSessions = 3,
                        level2TotalExercises = 30,
                        level2FirstAttemptCorrect = 24,
                        level3CompletedSessions = 1,
                        level3TotalExercises = 20,
                        level3FirstAttemptCorrect = 18,
                    ),
                    learningProgress = LearningProgress(
                        completedLessons = setOf(
                            LearningLesson.SixDots,
                            LearningLesson.Vowels,
                        ),
                    ),
                    engagementProgress = EngagementProgress(
                        totalXp = 320,
                        activityDates = setOf(
                            PracticeDate(2026, 8, 5),
                            PracticeDate(2026, 8, 6),
                            PracticeDate(2026, 8, 7),
                        ),
                        lastActivityDate = PracticeDate(2026, 8, 7),
                        currentStreak = 3,
                        bestStreak = 4,
                        currentMonthKey = "2026-08",
                        currentMonthExercises = 42,
                        miniAchievementDate = PracticeDate(2026, 8, 8),
                        miniAchievementType = DailyMiniAchievement.CompleteFiveExercises,
                        miniAchievementProgress = 3,
                        unlockedAchievements = setOf(PermanentAchievement.FirstStep),
                    ),
                    currentDate = PracticeDate(2026, 8, 8),
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
        composeRule.onNodeWithText(context.getString(R.string.nav_progress)).performClick()
        assertFunctionalProgressScreen()
    }

    private fun assertFunctionalProgressScreen() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        composeRule.onNodeWithText(context.getString(R.string.progress_level_1_title))
            .performScrollTo()
            .assertIsDisplayed()
        assertProgressValue(R.string.progress_sessions_label, "2")
        assertProgressValue(R.string.progress_exercises_label, "12")
        assertProgressValue(R.string.progress_first_attempt_label, "9")
        assertProgressValue(R.string.progress_accuracy_label, "75%")
        composeRule.onNodeWithContentDescription("XP acumulado, 320 XP")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Racha actual, 3 días")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Meta semanal, 3 de 5 días")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Meta mensual, 42 de 100 ejercicios")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.progress_mini_achievement_title))
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.progress_level_2_title))
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.progress_level_3_title))
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.progress_learning_title))
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Los seis puntos Braille. Estado: Completada.")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            "${context.getString(R.string.achievement_first_step_title)}. " +
                "${context.getString(R.string.achievement_first_step_description)}. Estado: Obtenido.",
        ).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            "${context.getString(R.string.achievement_consistency_title)}. " +
                "${context.getString(R.string.achievement_consistency_description)}. Estado: Pendiente.",
        ).performScrollTo().assertIsDisplayed()
    }

    private fun assertProgressValue(labelResId: Int, value: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        composeRule.onNodeWithContentDescription(
            "${context.getString(labelResId)}, $value",
        ).performScrollTo().assertIsDisplayed()
    }
}
