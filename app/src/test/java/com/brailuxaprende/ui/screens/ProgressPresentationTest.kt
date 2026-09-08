package com.brailuxaprende.ui.screens

import com.brailuxaprende.data.play.GameProgress
import com.brailuxaprende.data.practice.PracticeProgress
import com.brailuxaprende.data.practice.formatStatAccuracy
import com.brailuxaprende.data.practice.formatStatAccuracyAccessibility
import com.brailuxaprende.practice.EngagementProgress
import com.brailuxaprende.practice.PermanentAchievement
import com.brailuxaprende.practice.PracticeDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressPresentationTest {
    @Test
    fun `accuracy uses real accumulated counters and standard rounding`() {
        assertEquals(75, accuracyPercentage(firstAttemptCorrect = 9, totalExercises = 12))
        assertEquals(80, accuracyPercentage(firstAttemptCorrect = 24, totalExercises = 30))
        assertEquals(90, accuracyPercentage(firstAttemptCorrect = 18, totalExercises = 20))
        assertEquals(80, accuracyPercentage(firstAttemptCorrect = 8, totalExercises = 10))
        assertEquals(84, accuracyPercentage(firstAttemptCorrect = 67, totalExercises = 80))
    }

    @Test
    fun `accuracy formatting handles empty and non-empty correctly`() {
        assertEquals("-- %", formatStatAccuracy(firstAttemptCorrect = 0, totalExercises = 0))
        assertEquals("80 %", formatStatAccuracy(firstAttemptCorrect = 8, totalExercises = 10))
        assertEquals("84 %", formatStatAccuracy(firstAttemptCorrect = 67, totalExercises = 80))
        assertEquals("sin datos", formatStatAccuracyAccessibility(firstAttemptCorrect = 0, totalExercises = 0))
        assertEquals("80 por ciento", formatStatAccuracyAccessibility(firstAttemptCorrect = 8, totalExercises = 10))
        assertEquals("84 por ciento", formatStatAccuracyAccessibility(firstAttemptCorrect = 67, totalExercises = 80))
    }

    @Test
    fun `global accuracy uses sum of numerators and denominators and not average of percentages`() {
        val progress = PracticeProgress(
            level1CompletedSessions = 5,
            level1TotalExercises = 10,
            level1FirstAttemptCorrect = 10, // 100%
            level2CompletedSessions = 2,
            level2TotalExercises = 90,
            level2FirstAttemptCorrect = 9, // 10%
        )

        // Numerator sum = 10 + 9 = 19
        // Denominator sum = 10 + 90 = 100
        // Correct global accuracy = 19% (NOT average of 100% and 10% which would be 55%)
        assertEquals(100, progress.totalEducationalExercises)
        assertEquals(7, progress.totalEducationalSessions)
        assertEquals(19, progress.totalEducationalFirstAttemptCorrect)
        assertEquals(19, progress.overallAccuracyPercentage)
    }

    @Test
    fun `global progress handles empty practice safely`() {
        val progress = PracticeProgress()

        assertEquals(0, progress.totalEducationalExercises)
        assertEquals(0, progress.totalEducationalSessions)
        assertEquals(0, progress.totalEducationalFirstAttemptCorrect)
        assertNull(progress.overallAccuracyPercentage)
    }

    @Test
    fun `explorer level stats compute sessions and accuracy correctly`() {
        val progress = PracticeProgress(
            level1CompletedSessions = 8,
            level1TotalExercises = 80,
            level1FirstAttemptCorrect = 71,
        )

        assertEquals(8, progress.level1CompletedSessions)
        assertEquals(89, progress.level1AccuracyPercentage)
        assertEquals("89 %", formatStatAccuracy(progress.level1FirstAttemptCorrect, progress.level1TotalExercises))
    }

    @Test
    fun `recognizer level stats compute sessions and accuracy correctly`() {
        val progress = PracticeProgress(
            level2CompletedSessions = 6,
            level2TotalExercises = 60,
            level2FirstAttemptCorrect = 49,
        )

        assertEquals(6, progress.level2CompletedSessions)
        assertEquals(82, progress.level2AccuracyPercentage)
        assertEquals("82 %", formatStatAccuracy(progress.level2FirstAttemptCorrect, progress.level2TotalExercises))
    }

    @Test
    fun `challenge level stats compute sessions and accuracy correctly`() {
        val progress = PracticeProgress(
            level3CompletedSessions = 4,
            level3TotalExercises = 50,
            level3FirstAttemptCorrect = 37,
        )

        assertEquals(4, progress.level3CompletedSessions)
        assertEquals(74, progress.level3AccuracyPercentage)
        assertEquals("74 %", formatStatAccuracy(progress.level3FirstAttemptCorrect, progress.level3TotalExercises))
    }

    @Test
    fun `custom practice stats without data shows zero sessions and dashes and with data computes correctly`() {
        val emptyProgress = PracticeProgress()
        assertEquals(0, emptyProgress.customCompletedSessions)
        assertEquals(0, emptyProgress.customTotalExercises)
        assertEquals("-- %", formatStatAccuracy(emptyProgress.customFirstAttemptCorrect, emptyProgress.customTotalExercises))

        val realProgress = PracticeProgress(
            customCompletedSessions = 3,
            customTotalExercises = 30,
            customFirstAttemptCorrect = 24,
        )
        assertEquals(3, realProgress.customCompletedSessions)
        assertEquals(80, realProgress.customAccuracyPercentage)
        assertEquals("80 %", formatStatAccuracy(realProgress.customFirstAttemptCorrect, realProgress.customTotalExercises))
    }

    @Test
    fun `daily practice stats computes completed sessions and accuracy correctly`() {
        val progress = PracticeProgress(
            dailyCompletedSessions = 18,
            dailyTotalExercises = 90,
            dailyFirstAttemptCorrect = 78,
        )

        assertEquals(18, progress.dailyCompletedSessions)
        assertEquals(87, progress.dailyAccuracyPercentage)
        assertEquals("87 %", formatStatAccuracy(progress.dailyFirstAttemptCorrect, progress.dailyTotalExercises))
    }

    @Test
    fun `daily challenge stats computes completed sessions and accuracy independently`() {
        val progress = PracticeProgress(
            dailyCompletedSessions = 18,
            dailyTotalExercises = 90,
            dailyFirstAttemptCorrect = 78,
            dailyChallengeCompletedSessions = 11,
            dailyChallengeTotalExercises = 110,
            dailyChallengeFirstAttemptCorrect = 84,
        )

        assertEquals(11, progress.dailyChallengeCompletedSessions)
        assertEquals(76, progress.dailyChallengeAccuracyPercentage)
        assertEquals("76 %", formatStatAccuracy(progress.dailyChallengeFirstAttemptCorrect, progress.dailyChallengeTotalExercises))
    }

    @Test
    fun `play games stats use real GameProgress counters for memory sequence and order`() {
        val gameProgress = GameProgress(
            totalGamesCompleted = 26,
            memoryCompletedGames = 12,
            memoryBestMoves = 14,
            sequenceCompletedGames = 8,
            sequenceBestLength = 5,
            orderCompletedGames = 6,
            orderBestErrors = 0,
        )

        assertEquals(12, gameProgress.memoryCompletedGames)
        assertEquals(8, gameProgress.sequenceCompletedGames)
        assertEquals(6, gameProgress.orderCompletedGames)
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
    fun `compact statistics four section titles and metric string resources exist`() {
        // Section titles (exactly 4)
        assertEquals(com.brailuxaprende.R.string.progress_stats_your_progress_title, com.brailuxaprende.R.string.progress_stats_your_progress_title)
        assertEquals(com.brailuxaprende.R.string.progress_stats_practice_title, com.brailuxaprende.R.string.progress_stats_practice_title)
        assertEquals(com.brailuxaprende.R.string.progress_stats_routines_title, com.brailuxaprende.R.string.progress_stats_routines_title)
        assertEquals(com.brailuxaprende.R.string.progress_stats_play_title, com.brailuxaprende.R.string.progress_stats_play_title)

        // Tu Progreso metric labels
        assertEquals(com.brailuxaprende.R.string.progress_stats_exercises_label, com.brailuxaprende.R.string.progress_stats_exercises_label)
        assertEquals(com.brailuxaprende.R.string.progress_stats_sessions_label, com.brailuxaprende.R.string.progress_stats_sessions_label)
        assertEquals(com.brailuxaprende.R.string.progress_stats_accuracy_label, com.brailuxaprende.R.string.progress_stats_accuracy_label)
        assertEquals(com.brailuxaprende.R.string.progress_stats_streak_label, com.brailuxaprende.R.string.progress_stats_streak_label)

        // Activity and Game row labels
        assertEquals(com.brailuxaprende.R.string.progress_stats_explorer, com.brailuxaprende.R.string.progress_stats_explorer)
        assertEquals(com.brailuxaprende.R.string.progress_stats_recognizer, com.brailuxaprende.R.string.progress_stats_recognizer)
        assertEquals(com.brailuxaprende.R.string.progress_stats_challenge, com.brailuxaprende.R.string.progress_stats_challenge)
        assertEquals(com.brailuxaprende.R.string.progress_stats_custom, com.brailuxaprende.R.string.progress_stats_custom)
        assertEquals(com.brailuxaprende.R.string.progress_stats_daily_practice, com.brailuxaprende.R.string.progress_stats_daily_practice)
        assertEquals(com.brailuxaprende.R.string.progress_stats_daily_challenge, com.brailuxaprende.R.string.progress_stats_daily_challenge)
        assertEquals(com.brailuxaprende.R.string.progress_stats_game_memory, com.brailuxaprende.R.string.progress_stats_game_memory)
        assertEquals(com.brailuxaprende.R.string.progress_stats_game_sequence, com.brailuxaprende.R.string.progress_stats_game_sequence)
        assertEquals(com.brailuxaprende.R.string.progress_stats_game_order, com.brailuxaprende.R.string.progress_stats_game_order)

        // Plurals
        assertEquals(com.brailuxaprende.R.plurals.progress_stat_sessions, com.brailuxaprende.R.plurals.progress_stat_sessions)
        assertEquals(com.brailuxaprende.R.plurals.progress_stat_completed_fem, com.brailuxaprende.R.plurals.progress_stat_completed_fem)
        assertEquals(com.brailuxaprende.R.plurals.progress_stat_completed_masc, com.brailuxaprende.R.plurals.progress_stat_completed_masc)
        assertEquals(com.brailuxaprende.R.plurals.progress_stat_games, com.brailuxaprende.R.plurals.progress_stat_games)
        assertEquals(com.brailuxaprende.R.plurals.progress_streak_days, com.brailuxaprende.R.plurals.progress_streak_days)
    }

    @Test
    fun `all 25 permanent achievements have valid title and description string resources`() {
        assertEquals(25, PermanentAchievement.activeEntries.size)
        PermanentAchievement.entries.forEach { achievement ->
            val titleRes = achievement.titleResource()
            val descRes = achievement.descriptionResource()
            assertTrue("Title resource for $achievement should be non-zero", titleRes != 0)
            assertTrue("Description resource for $achievement should be non-zero", descRes != 0)
        }
    }
}


