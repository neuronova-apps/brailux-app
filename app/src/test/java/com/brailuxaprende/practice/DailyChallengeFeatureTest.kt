package com.brailuxaprende.practice

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.brailuxaprende.data.learn.LearningProgress
import com.brailuxaprende.data.practice.EngagementProgressRepository
import com.brailuxaprende.data.practice.PracticeProgress
import com.brailuxaprende.data.practice.PracticeProgressRepository
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DailyChallengeFeatureTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var dataStoreFile: File
    private lateinit var dataStoreScope: CoroutineScope
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var practiceRepo: PracticeProgressRepository
    private lateinit var engagementRepo: EngagementProgressRepository

    @Before
    fun setUp() {
        dataStoreFile = File(temporaryFolder.root, "test_daily_challenge.preferences_pb")
        dataStoreScope = CoroutineScope(Dispatchers.IO)
        dataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { dataStoreFile },
        )
        practiceRepo = PracticeProgressRepository(dataStore)
        engagementRepo = EngagementProgressRepository(dataStore)
    }

    @After
    fun tearDown() {
        runBlocking {
            dataStoreScope.coroutineContext[Job]?.cancelAndJoin()
        }
    }

    @Test
    fun requirement1_generatesExactlyTenExercises() {
        val date = PracticeDate(2026, 8, 28)
        val session = PracticeSessionGenerator.generateDailyChallenge(date = date)

        assertEquals(10, session.exercises.size)
        assertEquals(PracticeLevel.DailyChallenge, session.level)
        assertEquals(PracticeMode.Mixed, session.mode)
    }

    @Test
    fun requirement2_combinesFiveSignToLetterAndFiveLetterToSign() {
        val date = PracticeDate(2026, 8, 28)
        val session = PracticeSessionGenerator.generateDailyChallenge(date = date)
        val signToLetter = session.exercises.count { it.type == PracticeExerciseType.SignToCharacter }
        val letterToSign = session.exercises.count { it.type == PracticeExerciseType.CharacterToSign }

        assertEquals(5, signToLetter)
        assertEquals(5, letterToSign)
        assertEquals(10, signToLetter + letterToSign)
    }

    @Test
    fun requirement3_dailyChallengeSessionPreservesDateAndConstructsDateSessionId() {
        val today = PracticeDate(2026, 8, 28)
        val expectedId = "daily_challenge_2026-08-28"

        assertEquals(expectedId, dailyChallengeSessionId(today))
        assertTrue(isDailyChallengeSessionId(expectedId, today))
        assertEquals(today, parseDailyChallengeDate(expectedId))
    }

    @Test
    fun requirement4_resumesSnapshotFromSameDay() {
        val date = PracticeDate(2026, 8, 28)
        val session = PracticeSessionGenerator.generateDailyChallenge(date = date)
        val sessionId = dailyChallengeSessionId(date)
        var state = PracticeSessionState(session = session, sessionId = sessionId)
        state = state.selectAnswer(session.exercises[0].target.printedCharacter).checkAnswer().nextExercise()

        val snapshot = PracticeSessionSnapshot(state = state)
        assertEquals(1, snapshot.state.currentExerciseIndex)
        assertEquals(sessionId, snapshot.sessionId)
        assertEquals(PracticeLevel.DailyChallenge, snapshot.level)
        assertTrue(isDailyChallengeSessionId(snapshot.sessionId, date))
    }

    @Test
    fun requirement5_snapshotFromYesterdayIsNotReused() {
        val yesterday = PracticeDate(2026, 8, 27)
        val today = PracticeDate(2026, 8, 28)
        val yesterdaySession = PracticeSessionGenerator.generateDailyChallenge(date = yesterday)
        val yesterdaySnapshot = PracticeSessionSnapshot(
            state = PracticeSessionState(
                session = yesterdaySession,
                sessionId = dailyChallengeSessionId(yesterday),
            ),
        )

        val isCompatibleToday = yesterdaySnapshot.level == PracticeLevel.DailyChallenge &&
            yesterdaySnapshot.sessionId == dailyChallengeSessionId(today)

        assertFalse(isCompatibleToday)
    }

    @Test
    fun requirement6_completingMarksDateAsCompleted() = runBlocking {
        val date = PracticeDate(2026, 8, 28)
        val before = engagementRepo.progress.first()
        assertFalse(before.isDailyChallengeCompleted(date))

        practiceRepo.recordDailyChallengeSession(
            exercisesCompleted = 10,
            firstAttemptCorrect = 10,
            errors = 0,
            practiceDate = date.isoValue,
            sessionId = dailyChallengeSessionId(date),
        )

        val after = engagementRepo.progress.first()
        assertTrue(after.isDailyChallengeCompleted(date))
        assertTrue(date in after.dailyChallengeDates)
        assertEquals(1, after.dailyChallengeSessions)
    }

    @Test
    fun requirement7_and_8_xpAndStreakAwardedOnce() = runBlocking {
        val date = PracticeDate(2026, 8, 28)
        val result = practiceRepo.recordDailyChallengeSession(
            exercisesCompleted = 10,
            firstAttemptCorrect = 10,
            errors = 0,
            practiceDate = date.isoValue,
            sessionId = dailyChallengeSessionId(date),
        )

        val xp = result.engagementUpdate.reward.xpEarned
        // Base 10*2 = 20 XP + 20 bonus XP = 40 XP (plus possible mini achievement 10 XP if completed on same day)
        assertTrue(xp in listOf(40, 50))
        assertEquals(1, result.engagementUpdate.reward.currentStreak)

        val engagement = engagementRepo.progress.first()
        assertEquals(xp.toLong(), engagement.totalXp)
        assertEquals(1, engagement.currentStreak)
    }

    @Test
    fun requirement8_sharedStreakDoesNotDoubleIncrementOnSameDay() = runBlocking {
        val date = PracticeDate(2026, 8, 28)

        // Complete Daily Practice first
        val dailyPracticeResult = practiceRepo.recordDailySession(
            exercisesCompleted = 5,
            firstAttemptCorrect = 5,
            errors = 0,
            practiceDate = date.isoValue,
            sessionId = dailyPracticeSessionId(date),
        )
        assertEquals(1, dailyPracticeResult.engagementUpdate.reward.currentStreak)
        assertTrue(dailyPracticeResult.engagementUpdate.reward.addedPracticeDay)

        // Complete Daily Challenge on the same date
        val dailyChallengeResult = practiceRepo.recordDailyChallengeSession(
            exercisesCompleted = 10,
            firstAttemptCorrect = 10,
            errors = 0,
            practiceDate = date.isoValue,
            sessionId = dailyChallengeSessionId(date),
        )
        assertEquals(1, dailyChallengeResult.engagementUpdate.reward.currentStreak)
        assertFalse(dailyChallengeResult.engagementUpdate.reward.addedPracticeDay)

        val engagement = engagementRepo.progress.first()
        assertEquals(1, engagement.currentStreak)
        assertEquals(1, engagement.activityDates.size)
    }

    @Test
    fun requirement9_repeatingSessionDoesNotDuplicateReward() = runBlocking {
        val date = PracticeDate(2026, 8, 28)
        val sessionId = dailyChallengeSessionId(date)

        val first = practiceRepo.recordDailyChallengeSession(
            exercisesCompleted = 10,
            firstAttemptCorrect = 10,
            errors = 0,
            practiceDate = date.isoValue,
            sessionId = sessionId,
        )
        val replayed = practiceRepo.recordDailyChallengeSession(
            exercisesCompleted = 10,
            firstAttemptCorrect = 10,
            errors = 0,
            practiceDate = date.isoValue,
            sessionId = sessionId,
        )
        val second = practiceRepo.recordDailyChallengeSession(
            exercisesCompleted = 10,
            firstAttemptCorrect = 10,
            errors = 0,
            practiceDate = date.isoValue,
            sessionId = "daily_challenge_2026-08-28_repeat",
        )

        val firstXp = first.engagementUpdate.reward.xpEarned
        assertTrue(firstXp in listOf(40, 50))
        assertEquals(first.engagementUpdate.reward, replayed.engagementUpdate.reward)
        assertEquals(0, second.engagementUpdate.reward.xpEarned)

        val progress = practiceRepo.progress.first()
        assertEquals(1, progress.dailyChallengeCompletedSessions)
        assertEquals(10, progress.dailyChallengeTotalExercises)

        val engagement = engagementRepo.progress.first()
        assertEquals(firstXp.toLong(), engagement.totalXp)
        assertEquals(1, engagement.currentStreak)
    }

    @Test
    fun requirement10_nextDayReturnsToPending() = runBlocking {
        val today = PracticeDate(2026, 8, 28)
        val tomorrow = PracticeDate(2026, 8, 29)

        practiceRepo.recordDailyChallengeSession(
            exercisesCompleted = 10,
            firstAttemptCorrect = 10,
            errors = 0,
            practiceDate = today.isoValue,
            sessionId = dailyChallengeSessionId(today),
        )

        val engagement = engagementRepo.progress.first()
        assertTrue(engagement.isDailyChallengeCompleted(today))
        assertFalse(engagement.isDailyChallengeCompleted(tomorrow))
    }

    @Test
    fun requirement11_progressAndGoalsReceiveCorrectUpdate() = runBlocking {
        val monday = PracticeDate(2026, 8, 24)
        practiceRepo.recordDailyChallengeSession(
            exercisesCompleted = 10,
            firstAttemptCorrect = 8,
            errors = 2,
            practiceDate = monday.isoValue,
            sessionId = dailyChallengeSessionId(monday),
        )

        val progress = practiceRepo.progress.first()
        val engagement = engagementRepo.progress.first()

        assertEquals(1, progress.dailyChallengeCompletedSessions)
        assertEquals(10, progress.dailyChallengeTotalExercises)
        assertEquals(8, progress.dailyChallengeFirstAttemptCorrect)
        assertEquals(2, progress.dailyChallengeErrors)
        assertEquals(80, progress.dailyChallengeAccuracyPercentage)
        assertEquals(monday.isoValue, progress.dailyChallengeLastPracticeDate)

        assertEquals(1, engagement.weeklyPracticeDays(monday))
        assertEquals(10, engagement.monthlyExercises(monday))
    }

    @Test
    fun requirement12_dailyPracticeAndNormalPracticeAreNotAffected() = runBlocking {
        val date = PracticeDate(2026, 8, 28)

        val dailyRecord = practiceRepo.recordDailySession(
            exercisesCompleted = 5,
            firstAttemptCorrect = 5,
            errors = 0,
            practiceDate = date.isoValue,
            sessionId = dailyPracticeSessionId(date),
        )
        val challengeRecord = practiceRepo.recordDailyChallengeSession(
            exercisesCompleted = 10,
            firstAttemptCorrect = 9,
            errors = 1,
            practiceDate = date.isoValue,
            sessionId = dailyChallengeSessionId(date),
        )
        val level1Record = practiceRepo.recordLevel1Session(
            exercisesCompleted = 10,
            firstAttemptCorrect = 8,
            errors = 2,
            practiceDate = date.isoValue,
        )

        val progress = practiceRepo.progress.first()
        assertEquals(1, progress.dailyCompletedSessions)
        assertEquals(5, progress.dailyTotalExercises)
        assertEquals(1, progress.dailyChallengeCompletedSessions)
        assertEquals(10, progress.dailyChallengeTotalExercises)
        assertEquals(9, progress.dailyChallengeFirstAttemptCorrect)
        assertEquals(1, progress.dailyChallengeErrors)
        assertEquals(1, progress.level1CompletedSessions)
        assertEquals(10, progress.level1TotalExercises)

        val engagement = engagementRepo.progress.first()
        assertEquals(3, engagement.totalSessions)
        assertEquals(25L, engagement.totalExercises)
        val expectedTotalXp = dailyRecord.engagementUpdate.reward.xpEarned.toLong() +
            challengeRecord.engagementUpdate.reward.xpEarned.toLong() +
            level1Record.engagementUpdate.reward.xpEarned.toLong()
        assertEquals(expectedTotalXp, engagement.totalXp)
    }

    @Test
    fun requirement13_dailyChallengeExerciseCountProgressStep() {
        val date = PracticeDate(2026, 8, 28)
        val session = PracticeSessionGenerator.generateDailyChallenge(date = date)
        var state = PracticeSessionState(session = session, sessionId = dailyChallengeSessionId(date))

        assertEquals(1, state.exerciseNumber)
        assertEquals(10, state.session.exercises.size)

        for (i in 1..9) {
            val currentTarget = state.currentExercise.target.printedCharacter
            val answer = currentTarget
            state = state.selectAnswer(answer).checkAnswer().nextExercise()
            assertEquals(i + 1, state.exerciseNumber)
        }

        assertEquals(10, state.exerciseNumber)
    }

    @Test
    fun requirement14_dailyChallengeLevelConfigurationStrictness() {
        assertEquals(10, PracticeLevel.DailyChallenge.exerciseCount)
        assertEquals(4, PracticeLevel.DailyChallenge.optionCount)
        assertFalse(PracticeLevel.DailyChallenge.allowsPointNumberToggle)
        assertFalse(PracticeLevel.DailyChallenge.showPointNumbersByDefault)
        assertEquals(0, PracticeLevel.DailyChallenge.hintLimit)
    }

    @Test
    fun requirement15_resourceIdsAreConfiguredForDailyChallengeIdentity() {
        val challengeCompleted = com.brailuxaprende.R.string.daily_challenge_completed
        val challengeTitle = com.brailuxaprende.R.string.daily_challenge_header_title
        val challengeSubtitle = com.brailuxaprende.R.string.daily_challenge_header_subtitle
        val challengeBadge = com.brailuxaprende.R.string.daily_challenge_badge
        val challengeBackHome = com.brailuxaprende.R.string.daily_challenge_back_to_home
        val dailyTitle = com.brailuxaprende.R.string.daily_practice_header_title
        val dailyCompleted = com.brailuxaprende.R.string.daily_practice_completed

        assertNotEquals(challengeTitle, dailyTitle)
        assertNotEquals(challengeCompleted, dailyCompleted)
        assertTrue(challengeBadge != 0)
        assertTrue(challengeSubtitle != 0)
        assertTrue(challengeBackHome != 0)
    }
}
