package com.brailuxaprende.practice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EngagementEngineTest {
    @Test
    fun freshProgressDoesNotRegisterActivityByItself() {
        val date = PracticeDate(2026, 8, 8)
        val progress = EngagementProgress()

        assertFalse(progress.hasPracticed(date))
        assertFalse(progress.isDailyPracticeCompleted(date))
        assertEquals(0, progress.weeklyPracticeDays(date))
        assertEquals(0, progress.displayedStreak(date))
        assertEquals(0L, progress.totalXp)
        assertEquals(0, progress.totalSessions)
    }

    @Test
    fun everyCompletedSessionKindRegistersPracticeButOnlyDailyMarksDailySession() {
        val date = neutralMiniDate()

        PracticeSessionKind.entries.forEach { kind ->
            val update = record(
                current = EngagementProgress(),
                session = session(kind),
                date = date,
            )

            assertTrue(update.progress.hasPracticed(date))
            assertEquals(1, update.progress.activityDates.size)
            assertEquals(1, update.progress.currentStreak)
            assertEquals(1, update.progress.totalSessions)
            assertEquals(kind == PracticeSessionKind.Daily, update.progress.isDailyPracticeCompleted(date))
            assertTrue(update.reward.addedPracticeDay)
        }
    }

    @Test
    fun multipleSessionsOnTheSameDayDoNotDuplicateDayWeekOrStreak() {
        val date = neutralMiniDate()
        val first = record(EngagementProgress(), session(PracticeSessionKind.Level1), date)
        val second = record(first.progress, session(PracticeSessionKind.Level2), date)

        assertEquals(setOf(date), second.progress.activityDates)
        assertEquals(1, second.progress.currentStreak)
        assertEquals(1, second.progress.bestStreak)
        assertEquals(1, second.progress.weeklyPracticeDays(date))
        assertEquals(2, second.progress.totalSessions)
        assertEquals(25L, second.progress.totalExercises)
        assertEquals(75L, second.progress.totalXp)
        assertFalse(second.reward.addedPracticeDay)
    }

    @Test
    fun consecutiveDayIncrementsStreakAndGapRestartsWithoutLosingProgress() {
        val firstDate = PracticeDate(2026, 8, 3)
        val first = record(EngagementProgress(), session(PracticeSessionKind.Level1), firstDate)
        val consecutive = record(
            first.progress,
            session(PracticeSessionKind.Level1),
            firstDate.plusDays(1),
        )
        val xpBeforeGap = consecutive.progress.totalXp
        val achievementsBeforeGap = consecutive.progress.unlockedAchievements
        val afterGap = record(
            consecutive.progress,
            session(PracticeSessionKind.Level1),
            firstDate.plusDays(4),
        )

        assertEquals(2, consecutive.progress.currentStreak)
        assertEquals(2, consecutive.progress.bestStreak)
        assertEquals(1, afterGap.progress.currentStreak)
        assertEquals(2, afterGap.progress.bestStreak)
        assertTrue(afterGap.progress.totalXp > xpBeforeGap)
        assertTrue(afterGap.progress.unlockedAchievements.containsAll(achievementsBeforeGap))
        assertEquals(3, afterGap.progress.activityDates.size)
    }

    @Test
    fun displayedStreakBecomesZeroAfterMissedDaysWithoutDeletingStoredBest() {
        val date = PracticeDate(2026, 8, 3)
        val progress = record(
            record(EngagementProgress(), session(PracticeSessionKind.Level1), date).progress,
            session(PracticeSessionKind.Level1),
            date.plusDays(1),
        ).progress

        assertEquals(2, progress.displayedStreak(date.plusDays(1)))
        assertEquals(2, progress.displayedStreak(date.plusDays(2)))
        assertEquals(0, progress.displayedStreak(date.plusDays(3)))
        assertEquals(2, progress.bestStreak)
    }

    @Test
    fun fiveDistinctDaysInOneWeekCompleteWeeklyGoalAndUnlockAchievement() {
        val monday = PracticeDate(2026, 8, 3)
        var progress = EngagementProgress()
        var fifthUpdate: EngagementUpdate? = null

        repeat(WeeklyPracticeTarget) { offset ->
            fifthUpdate = record(
                progress,
                session(PracticeSessionKind.Level1),
                monday.plusDays(offset),
            )
            progress = checkNotNull(fifthUpdate).progress
        }

        assertEquals(WeeklyPracticeTarget, progress.weeklyPracticeDays(monday.plusDays(4)))
        assertTrue(PermanentAchievement.WeekInMotion in progress.unlockedAchievements)
        assertTrue(
            PermanentAchievement.WeekInMotion in
                checkNotNull(fifthUpdate).reward.newlyUnlockedAchievements,
        )
    }

    @Test
    fun fiveDaysSplitBetweenWeeksDoNotCompleteWeeklyAchievement() {
        val friday = PracticeDate(2026, 8, 7)
        val dates = listOf(
            friday,
            friday.plusDays(1),
            friday.plusDays(2),
            friday.plusDays(3),
            friday.plusDays(4),
        )
        var progress = EngagementProgress()

        dates.forEach { date ->
            progress = record(progress, session(PracticeSessionKind.Level1), date).progress
        }

        assertEquals(3, progress.weeklyPracticeDays(friday))
        assertEquals(2, progress.weeklyPracticeDays(friday.plusDays(4)))
        assertFalse(PermanentAchievement.WeekInMotion in progress.unlockedAchievements)
    }

    @Test
    fun xpFormulaMatchesEverySessionKindWithoutMiniAchievementBonus() {
        val date = neutralMiniDate()
        val cases = listOf(
            Triple(PracticeSessionKind.Daily, 5, 20),
            Triple(PracticeSessionKind.Level1, 10, 30),
            Triple(PracticeSessionKind.Level2, 15, 45),
            Triple(PracticeSessionKind.Level3, 20, 60),
            Triple(PracticeSessionKind.Custom, 10, 30),
        )

        cases.forEach { (kind, exerciseCount, expectedXp) ->
            val update = record(
                EngagementProgress(),
                session(kind, exercises = exerciseCount, firstAttemptCorrect = 0),
                date,
            )

            assertEquals("XP for $kind", expectedXp, update.reward.xpEarned)
            assertEquals("Total XP for $kind", expectedXp.toLong(), update.progress.totalXp)
        }
    }

    @Test
    fun errorsAndHintsNeverReduceXp() {
        val date = neutralMiniDate()
        val withoutHelpOrErrors = record(
            EngagementProgress(),
            session(PracticeSessionKind.Level2, errors = 0, hintsUsed = 0),
            date,
        )
        val withHelpAndErrors = record(
            EngagementProgress(),
            session(PracticeSessionKind.Level2, errors = 40, hintsUsed = 20),
            date,
        )

        assertEquals(withoutHelpOrErrors.reward.xpEarned, withHelpAndErrors.reward.xpEarned)
        assertEquals(withoutHelpOrErrors.progress.totalXp, withHelpAndErrors.progress.totalXp)
    }

    @Test
    fun repeatedDailyCompletionIsFullyIdempotentForTheSameDate() {
        val date = neutralMiniDate()
        val first = record(EngagementProgress(), session(PracticeSessionKind.Daily), date)
        val repeated = record(first.progress, session(PracticeSessionKind.Daily), date)
        val sameMiniTypeOnAnotherDate = date.plusDays(DailyMiniAchievement.entries.size)
        val anotherDay = record(
            repeated.progress,
            session(PracticeSessionKind.Daily),
            sameMiniTypeOnAnotherDate,
        )

        assertEquals(20, first.reward.xpEarned)
        assertEquals(0, repeated.reward.xpEarned)
        assertEquals(first.progress, repeated.progress)
        assertEquals(20, anotherDay.reward.xpEarned)
        assertEquals(setOf(date, sameMiniTypeOnAnotherDate), anotherDay.progress.dailyPracticeDates)
    }

    @Test
    fun eachMiniAchievementCanCompleteAndItsXpIsGrantedOnce() {
        val immediatelyCompletable = listOf(
            DailyMiniAchievement.CompleteFiveExercises to session(
                PracticeSessionKind.Level1,
                firstAttemptCorrect = 0,
            ),
            DailyMiniAchievement.CompleteSession to session(
                PracticeSessionKind.Level1,
                firstAttemptCorrect = 0,
            ),
            DailyMiniAchievement.ThreeFirstAttemptCorrect to session(
                PracticeSessionKind.Level1,
                firstAttemptCorrect = 3,
                longestFirstAttemptCorrectStreak = 3,
            ),
        )

        immediatelyCompletable.forEach { (mini, qualifyingSession) ->
            val date = dateForMini(mini)
            val first = record(EngagementProgress(), qualifyingSession, date)
            val repeated = record(first.progress, qualifyingSession, date)

            assertEquals(mini, first.reward.miniAchievementCompleted)
            assertTrue(first.progress.miniAchievement(date).completed)
            assertEquals(mini.target, first.progress.miniAchievement(date).progress)
            assertEquals(10, first.reward.xpEarned - repeated.reward.xpEarned)
            assertNull(repeated.reward.miniAchievementCompleted)
        }
    }

    @Test
    fun twoModalitiesMiniRequiresDistinctExerciseTypes() {
        val date = dateForMini(DailyMiniAchievement.TwoModalities)
        val first = record(
            EngagementProgress(),
            session(PracticeSessionKind.Level1, mode = PracticeMode.SignToCharacter),
            date,
        )
        val repeatedMode = record(
            first.progress,
            session(PracticeSessionKind.Level1, mode = PracticeMode.SignToCharacter),
            date,
        )
        val secondMode = record(
            repeatedMode.progress,
            session(PracticeSessionKind.Level1, mode = PracticeMode.CharacterToSign),
            date,
        )

        assertEquals(1, first.progress.miniAchievement(date).progress)
        assertEquals(1, repeatedMode.progress.miniAchievement(date).progress)
        assertFalse(repeatedMode.progress.miniAchievement(date).completed)
        assertEquals(DailyMiniAchievement.TwoModalities, secondMode.reward.miniAchievementCompleted)
        assertEquals(2, secondMode.progress.miniAchievement(date).progress)
        assertTrue(secondMode.progress.miniAchievement(date).completed)
    }

    @Test
    fun threeCorrectMiniUsesConsecutiveStreakInsteadOfTotalCorrectAnswers() {
        val date = dateForMini(DailyMiniAchievement.ThreeFirstAttemptCorrect)
        val nonConsecutive = record(
            EngagementProgress(),
            session(
                PracticeSessionKind.Level1,
                firstAttemptCorrect = 8,
                longestFirstAttemptCorrectStreak = 2,
            ),
            date,
        )
        val consecutive = record(
            nonConsecutive.progress,
            session(
                PracticeSessionKind.Level1,
                firstAttemptCorrect = 3,
                longestFirstAttemptCorrectStreak = 3,
            ),
            date,
        )

        assertEquals(2, nonConsecutive.progress.miniAchievement(date).progress)
        assertFalse(nonConsecutive.progress.miniAchievement(date).completed)
        assertEquals(
            DailyMiniAchievement.ThreeFirstAttemptCorrect,
            consecutive.reward.miniAchievementCompleted,
        )
        assertTrue(consecutive.progress.miniAchievement(date).completed)
    }

    @Test
    fun returningToAnAlreadyRewardedDateNeverGrantsMiniXpTwice() {
        val firstDate = dateForMini(DailyMiniAchievement.CompleteSession)
        val first = record(
            EngagementProgress(),
            session(PracticeSessionKind.Level1),
            firstDate,
        )
        val laterDate = record(
            first.progress,
            session(PracticeSessionKind.Level1),
            firstDate.plusDays(1),
        )
        val returned = record(
            laterDate.progress,
            session(PracticeSessionKind.Level1),
            firstDate,
        )

        assertEquals(40, first.reward.xpEarned)
        assertEquals(DailyMiniAchievement.CompleteSession, first.reward.miniAchievementCompleted)
        assertTrue(firstDate in returned.progress.miniRewardedDates)
        assertEquals(30, returned.reward.xpEarned)
        assertNull(returned.reward.miniAchievementCompleted)
    }

    @Test
    fun permanentSessionAchievementsUnlockAtThresholdAndOnlyReportOnce() {
        val date = neutralMiniDate()
        val cases = listOf(
            Triple(PracticeSessionKind.Level1, 5, PermanentAchievement.Explorer),
            Triple(PracticeSessionKind.Level2, 5, PermanentAchievement.Recognizer),
            Triple(PracticeSessionKind.Level3, 3, PermanentAchievement.Challenger),
        )

        cases.forEach { (kind, threshold, achievement) ->
            var progress = EngagementProgress()
            var thresholdUpdate: EngagementUpdate? = null
            repeat(threshold) {
                thresholdUpdate = record(progress, session(kind), date)
                progress = checkNotNull(thresholdUpdate).progress
            }

            assertTrue(achievement in progress.unlockedAchievements)
            assertTrue(achievement in checkNotNull(thresholdUpdate).reward.newlyUnlockedAchievements)

            val repeated = record(progress, session(kind), date)
            assertTrue(achievement in repeated.progress.unlockedAchievements)
            assertFalse(achievement in repeated.reward.newlyUnlockedAchievements)
        }
    }

    @Test
    fun distinctDayAndExerciseAchievementsUseAccumulatedHistory() {
        val firstDate = PracticeDate(2026, 8, 3)
        var progress = EngagementProgress()

        repeat(3) { offset ->
            progress = record(
                progress,
                session(PracticeSessionKind.Level3),
                firstDate.plusDays(offset),
            ).progress
        }
        repeat(2) {
            progress = record(progress, session(PracticeSessionKind.Level3), firstDate.plusDays(2)).progress
        }

        assertTrue(PermanentAchievement.FirstStep in progress.unlockedAchievements)
        assertTrue(PermanentAchievement.Consistency in progress.unlockedAchievements)
        assertTrue(PermanentAchievement.HundredExercises in progress.unlockedAchievements)
        assertEquals(3, progress.activityDates.size)
        assertEquals(100L, progress.totalExercises)
    }

    @Test
    fun monthlyGoalResetsNaturallyAndKeepsCompletedMonthHistory() {
        val august = PracticeDate(2026, 8, 8)
        var progress = EngagementProgress()

        repeat(5) {
            progress = record(progress, session(PracticeSessionKind.Level3), august).progress
        }

        assertEquals(100, progress.monthlyExercises(august))
        assertTrue(progress.isMonthlyGoalCompleted(august))
        assertTrue(august.monthKey in progress.completedMonthGoals)

        val september = PracticeDate(2026, 9, 1)
        progress = record(progress, session(PracticeSessionKind.Daily), september).progress

        assertEquals(5, progress.monthlyExercises(september))
        assertFalse(progress.isMonthlyGoalCompleted(september))
        assertTrue(progress.isMonthlyGoalCompleted(august))
        assertTrue(august.monthKey in progress.completedMonthGoals)
    }

    @Test
    fun monthlyLedgerKeepsBothMonthsWhenClockMovesForwardAndBack() {
        val august = PracticeDate(2026, 8, 8)
        val september = PracticeDate(2026, 9, 1)
        val firstAugust = record(
            EngagementProgress(),
            session(PracticeSessionKind.Level1),
            august,
        )
        val septemberUpdate = record(
            firstAugust.progress,
            session(PracticeSessionKind.Level2),
            september,
        )
        val returnedToAugust = record(
            septemberUpdate.progress,
            session(PracticeSessionKind.Custom),
            august,
        )

        assertEquals(20, returnedToAugust.progress.monthlyExercises(august))
        assertEquals(15, returnedToAugust.progress.monthlyExercises(september))
        assertEquals(
            mapOf(august.monthKey to 20, september.monthKey to 15),
            returnedToAugust.progress.monthlyExerciseCounts,
        )
        assertEquals(august.monthKey, returnedToAugust.progress.currentMonthKey)
        assertEquals(20, returnedToAugust.progress.currentMonthExercises)
    }

    @Test
    fun yearChangeKeepsConsecutiveStreakAndUsesANewMonthlyBucket() {
        val lastDay = PracticeDate(2026, 12, 31)
        val first = record(EngagementProgress(), session(PracticeSessionKind.Level1), lastDay)
        val nextYear = record(
            first.progress,
            session(PracticeSessionKind.Level1),
            PracticeDate(2027, 1, 1),
        )

        assertEquals(2, nextYear.progress.currentStreak)
        assertEquals(2, nextYear.progress.bestStreak)
        assertEquals("2027-01", nextYear.progress.currentMonthKey)
        assertEquals(10, nextYear.progress.currentMonthExercises)
        assertEquals(2, nextYear.progress.activityDates.size)
    }

    @Test
    fun unlockedAchievementsNeverDisappearAfterAGapOrCalendarChange() {
        val current = EngagementProgress(
            totalXp = 500,
            activityDates = setOf(PracticeDate(2025, 12, 31)),
            lastActivityDate = PracticeDate(2025, 12, 31),
            currentStreak = 4,
            bestStreak = 8,
            unlockedAchievements = setOf(
                PermanentAchievement.FirstStep,
                PermanentAchievement.Explorer,
            ),
        )

        val update = record(
            current,
            session(PracticeSessionKind.Custom),
            PracticeDate(2026, 2, 1),
        )

        assertTrue(update.progress.unlockedAchievements.containsAll(current.unlockedAchievements))
        assertTrue(update.progress.totalXp > current.totalXp)
        assertEquals(1, update.progress.currentStreak)
        assertEquals(8, update.progress.bestStreak)
    }

    private fun record(
        current: EngagementProgress,
        session: EngagementSession,
        date: PracticeDate,
    ): EngagementUpdate = EngagementEngine.recordSession(current, session, date)

    private fun session(
        kind: PracticeSessionKind,
        exercises: Int = defaultExercises(kind),
        firstAttemptCorrect: Int = 0,
        errors: Int = 0,
        hintsUsed: Int = 0,
        mode: PracticeMode = PracticeMode.SignToCharacter,
        longestFirstAttemptCorrectStreak: Int = 0,
    ): EngagementSession = EngagementSession(
        kind = kind,
        exercisesCompleted = exercises,
        firstAttemptCorrect = firstAttemptCorrect,
        errors = errors,
        hintsUsed = hintsUsed,
        mode = mode,
        longestFirstAttemptCorrectStreak = longestFirstAttemptCorrectStreak,
    )

    private fun neutralMiniDate(): PracticeDate =
        dateForMini(DailyMiniAchievement.ThreeFirstAttemptCorrect)

    private fun dateForMini(type: DailyMiniAchievement): PracticeDate {
        var date = PracticeDate(2026, 1, 1)
        repeat(DailyMiniAchievement.entries.size * 2) {
            if (DailyMiniAchievement.forDate(date) == type) return date
            date = date.plusDays(1)
        }
        error("No date found for $type")
    }

    private companion object {
        fun defaultExercises(kind: PracticeSessionKind): Int = when (kind) {
            PracticeSessionKind.Daily -> 5
            PracticeSessionKind.DailyChallenge -> 10
            PracticeSessionKind.Level1 -> 10
            PracticeSessionKind.Level2 -> 15
            PracticeSessionKind.Level3 -> 20
            PracticeSessionKind.Custom -> 10
        }
    }
}
