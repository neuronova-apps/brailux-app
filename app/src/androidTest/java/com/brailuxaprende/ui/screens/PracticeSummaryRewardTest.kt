package com.brailuxaprende.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.brailuxaprende.R
import com.brailuxaprende.practice.EngagementReward
import com.brailuxaprende.practice.PracticeLevel
import com.brailuxaprende.practice.PracticeSessionSummary
import com.brailuxaprende.ui.theme.BrailuxAprendeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PracticeSummaryRewardTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun dailySummaryShowsTheAccreditedXpValue() {
        assertAccreditedXp(PracticeLevel.Daily, xpEarned = 21)
    }

    @Test
    fun level1SummaryShowsTheAccreditedXpValue() {
        assertAccreditedXp(PracticeLevel.BrailleExplorer, xpEarned = 32)
    }

    @Test
    fun level2SummaryShowsTheAccreditedXpValue() {
        assertAccreditedXp(PracticeLevel.BrailleRecognizer, xpEarned = 43)
    }

    @Test
    fun level3SummaryShowsTheAccreditedXpValue() {
        assertAccreditedXp(PracticeLevel.BrailleChallenge, xpEarned = 54)
    }

    @Test
    fun customSummaryShowsTheAccreditedXpValue() {
        assertAccreditedXp(PracticeLevel.Custom, xpEarned = 0)
    }

    private fun assertAccreditedXp(level: PracticeLevel, xpEarned: Int) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        composeRule.setContent {
            BrailuxAprendeTheme {
                BraillePracticeSummary(
                    level = level,
                    summary = PracticeSessionSummary(
                        exercisesCompleted = level.exerciseCount,
                        firstAttemptCorrect = level.exerciseCount,
                        errors = 0,
                        accuracyPercentage = 100,
                        practicedLetters = listOf('A'),
                        sessionId = "summary-${level.name}",
                    ),
                    onPracticeAgain = {},
                    engagementReward = EngagementReward(
                        xpEarned = xpEarned,
                        addedPracticeDay = true,
                        weeklyPracticeDays = 1,
                        currentStreak = 1,
                        miniAchievementCompleted = null,
                        newlyUnlockedAchievements = emptySet(),
                    ),
                    onBackToPractice = {},
                )
            }
        }

        composeRule.onNodeWithText(
            context.getString(R.string.practice_reward_xp, xpEarned),
        ).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            context.getString(R.string.practice_reward_xp_accessibility, xpEarned),
        ).performScrollTo().assertIsDisplayed()
    }
}
