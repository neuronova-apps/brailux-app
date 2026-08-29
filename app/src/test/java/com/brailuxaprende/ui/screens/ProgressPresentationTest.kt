package com.brailuxaprende.ui.screens

import com.brailuxaprende.practice.EngagementProgress
import com.brailuxaprende.practice.PracticeDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressPresentationTest {
    @Test
    fun `accuracy uses real accumulated counters`() {
        assertEquals(75, accuracyPercentage(firstAttemptCorrect = 9, totalExercises = 12))
        assertEquals(80, accuracyPercentage(firstAttemptCorrect = 24, totalExercises = 30))
        assertEquals(90, accuracyPercentage(firstAttemptCorrect = 18, totalExercises = 20))
    }

    @Test
    fun `accuracy is safe for empty or inconsistent counters`() {
        assertEquals(0, accuracyPercentage(firstAttemptCorrect = 0, totalExercises = 0))
        assertEquals(100, accuracyPercentage(firstAttemptCorrect = 30, totalExercises = 20))
    }

    @Test
    fun `practice today includes every valid session and not only daily practice`() {
        val date = PracticeDate(2026, 8, 8)

        assertTrue(
            isPracticeCompletedToday(
                progress = EngagementProgress(activityDates = setOf(date)),
                date = date,
            ),
        )
        assertFalse(
            isPracticeCompletedToday(
                progress = EngagementProgress(),
                date = date,
            ),
        )
    }

    @Test
    fun `progress tabs are defined in expected order with Summary as default`() {
        val expectedTabs = listOf(
            ProgressTab.Summary,
            ProgressTab.Statistics,
            ProgressTab.Achievements,
        )
        assertEquals(expectedTabs, ProgressTab.entries)
        assertEquals(ProgressTab.Summary, ProgressTab.entries.first())
    }

    @Test
    fun `progress tabs map to expected string resources`() {
        assertEquals(com.brailuxaprende.R.string.progress_tab_summary, ProgressTab.Summary.labelResource)
        assertEquals(com.brailuxaprende.R.string.progress_tab_statistics, ProgressTab.Statistics.labelResource)
        assertEquals(com.brailuxaprende.R.string.progress_tab_achievements, ProgressTab.Achievements.labelResource)
    }

    @Test
    fun `practice level and metric string resources exist and match expectations`() {
        assertEquals(com.brailuxaprende.R.string.progress_level_1_title, com.brailuxaprende.R.string.progress_level_1_title)
        assertEquals(com.brailuxaprende.R.string.progress_level_2_title, com.brailuxaprende.R.string.progress_level_2_title)
        assertEquals(com.brailuxaprende.R.string.progress_level_3_title, com.brailuxaprende.R.string.progress_level_3_title)
        assertEquals(com.brailuxaprende.R.string.progress_level_4_title, com.brailuxaprende.R.string.progress_level_4_title)
        assertEquals(com.brailuxaprende.R.string.progress_daily_challenge_title, com.brailuxaprende.R.string.progress_daily_challenge_title)
        assertEquals(com.brailuxaprende.R.string.progress_errors_label, com.brailuxaprende.R.string.progress_errors_label)
        assertEquals(com.brailuxaprende.R.string.progress_hints_used_label, com.brailuxaprende.R.string.progress_hints_used_label)
    }
}
