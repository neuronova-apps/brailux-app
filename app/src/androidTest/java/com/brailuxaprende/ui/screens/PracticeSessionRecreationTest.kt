package com.brailuxaprende.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.brailuxaprende.MainActivity
import com.brailuxaprende.R
import com.brailuxaprende.data.practice.EngagementProgressRepository
import com.brailuxaprende.data.practice.PracticeProgressRepository
import com.brailuxaprende.data.practice.practiceSessionSnapshotKeyName
import com.brailuxaprende.data.settings.accessibilityPreferencesDataStore
import com.brailuxaprende.practice.PracticeLevel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement

@RunWith(AndroidJUnit4::class)
class PracticeSessionRecreationTest {
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain
        .outerRule(ClearPracticeSessionSnapshotsRule())
        .around(composeRule)

    @Test
    fun level1SessionAndSummarySurviveActivityRecreationWithoutDuplicateCredit() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val baselineSessions = level1CompletedSessions()
        val baselineXp = totalXp()

        composeRule.onNodeWithText(context.getString(R.string.welcome_start)).performClick()
        composeRule.onNodeWithText(context.getString(R.string.home_access_practice))
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithText(context.getString(R.string.practice_level_1_title))
            .performScrollTo()
            .performClick()

        waitForExercise(number = 1)
        answerCurrentExerciseAndAdvance()
        waitForExercise(number = 2)

        composeRule.activityRule.scenario.recreate()

        waitForExercise(number = 2)
        repeat(9) { answerCurrentExerciseAndAdvance() }

        val completedTitle = context.getString(R.string.practice_level_completed)
        composeRule.waitUntil(timeoutMillis = 15_000L) {
            composeRule.onAllNodesWithText(completedTitle).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(completedTitle).assertIsDisplayed()
        assertEquals(baselineSessions + 1, level1CompletedSessions())
        val accreditedXp = (totalXp() - baselineXp).toInt()
        composeRule.onNodeWithText(
            context.getString(R.string.practice_reward_xp, accreditedXp),
        ).performScrollTo().assertIsDisplayed()

        composeRule.activityRule.scenario.recreate()

        composeRule.waitUntil(timeoutMillis = 10_000L) {
            composeRule.onAllNodesWithText(completedTitle).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(completedTitle).assertIsDisplayed()
        assertEquals(baselineSessions + 1, level1CompletedSessions())
        assertEquals(baselineXp + accreditedXp, totalXp())
        composeRule.onNodeWithText(
            context.getString(R.string.practice_reward_xp, accreditedXp),
        ).performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithText(context.getString(R.string.practice_again))
            .performScrollTo()
            .performClick()
        waitForExercise(number = 1)
        assertEquals(baselineSessions + 1, level1CompletedSessions())
        assertEquals(baselineXp + accreditedXp, totalXp())
    }

    private fun answerCurrentExerciseAndAdvance() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val checkAnswer = context.getString(R.string.practice_check_answer)
        val nextExercise = context.getString(R.string.practice_next_exercise)

        for (letter in 'A'..'J') {
            val letterText = letter.toString()
            if (composeRule.onAllNodesWithText(letterText).fetchSemanticsNodes().isEmpty()) continue

            composeRule.onAllNodesWithText(letterText).onFirst()
                .performScrollTo()
                .performClick()
            composeRule.onNodeWithText(checkAnswer)
                .performScrollTo()
                .performClick()
            composeRule.waitForIdle()

            if (composeRule.onAllNodesWithText(nextExercise).fetchSemanticsNodes().isNotEmpty()) {
                composeRule.onNodeWithText(nextExercise)
                    .performScrollTo()
                    .performClick()
                composeRule.waitForIdle()
                return
            }
        }
        fail("No correct option was found in the current Level 1 exercise.")
    }

    private fun waitForExercise(number: Int) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val progress = context.getString(
            R.string.practice_exercise_count,
            number,
            PracticeLevel.BrailleExplorer.exerciseCount,
        )
        composeRule.waitUntil(timeoutMillis = 10_000L) {
            composeRule.onAllNodesWithText(progress).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(progress).performScrollTo().assertIsDisplayed()
    }

    private fun level1CompletedSessions(): Int {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return runBlocking {
            PracticeProgressRepository(context.accessibilityPreferencesDataStore)
                .progress
                .first()
                .level1CompletedSessions
        }
    }

    private fun totalXp(): Long {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return runBlocking {
            EngagementProgressRepository(context.accessibilityPreferencesDataStore)
                .progress
                .first()
                .totalXp
        }
    }
}

private class ClearPracticeSessionSnapshotsRule : TestRule {
    override fun apply(base: Statement, description: Description): Statement = object : Statement() {
        override fun evaluate() {
            clearSnapshots()
            try {
                base.evaluate()
            } finally {
                clearSnapshots()
            }
        }
    }

    private fun clearSnapshots() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        runBlocking {
            context.accessibilityPreferencesDataStore.edit { preferences ->
                PracticeLevel.entries.forEach { level ->
                    preferences.remove(
                        stringPreferencesKey(practiceSessionSnapshotKeyName(level)),
                    )
                }
            }
        }
    }
}
