package com.brailuxaprende.practice

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.brailuxaprende.data.learn.LearningProgress
import com.brailuxaprende.data.practice.EngagementProgressRepository
import com.brailuxaprende.data.practice.PracticeProgress
import com.brailuxaprende.data.practice.PracticeProgressRepository
import com.brailuxaprende.learning.LearningLesson
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DailyPracticeFeatureTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var dataStoreFile: File
    private lateinit var dataStoreScope: CoroutineScope
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var practiceRepo: PracticeProgressRepository
    private lateinit var engagementRepo: EngagementProgressRepository

    @Before
    fun setUp() {
        dataStoreFile = File(temporaryFolder.root, "test_daily_practice.preferences_pb")
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
    fun requirement1_generatesExactlyFiveExercises() {
        val date = PracticeDate(2026, 8, 28)
        val session = PracticeSessionGenerator.generateDaily(date = date)

        assertEquals(5, session.exercises.size)
        assertEquals(PracticeLevel.Daily, session.level)
        assertEquals(PracticeMode.Mixed, session.mode)
    }

    @Test
    fun requirement2_combinesSignToLetterAndLetterToSign() {
        val date = PracticeDate(2026, 8, 28)
        val session = PracticeSessionGenerator.generateDaily(date = date)
        val signToLetter = session.exercises.count { it.type == PracticeExerciseType.SignToCharacter }
        val letterToSign = session.exercises.count { it.type == PracticeExerciseType.CharacterToSign }

        assertTrue(signToLetter > 0)
        assertTrue(letterToSign > 0)
        assertEquals(5, signToLetter + letterToSign)
        assertTrue(kotlin.math.abs(signToLetter - letterToSign) <= 1)
    }

    @Test
    fun requirement3_dailySessionPreservesDateAndConstructsDateSessionId() {
        val today = PracticeDate(2026, 8, 28)
        val expectedId = "daily_2026-08-28"

        assertEquals(expectedId, dailyPracticeSessionId(today))
        assertTrue(isDailyPracticeSessionId(expectedId, today))
        assertEquals(today, parseDailyPracticeDate(expectedId))
    }

    @Test
    fun requirement4_resumesSnapshotFromSameDay() {
        val date = PracticeDate(2026, 8, 28)
        val session = PracticeSessionGenerator.generateDaily(date = date)
        val sessionId = dailyPracticeSessionId(date)
        var state = PracticeSessionState(session = session, sessionId = sessionId)
        state = state.selectAnswer(session.exercises[0].target.printedCharacter).checkAnswer().nextExercise()

        val snapshot = PracticeSessionSnapshot(state = state)
        assertEquals(1, snapshot.state.currentExerciseIndex)
        assertEquals(sessionId, snapshot.sessionId)
        assertEquals(PracticeLevel.Daily, snapshot.level)
        assertTrue(isDailyPracticeSessionId(snapshot.sessionId, date))
    }

    @Test
    fun requirement5_snapshotFromYesterdayIsNotReused() {
        val yesterday = PracticeDate(2026, 8, 27)
        val today = PracticeDate(2026, 8, 28)
        val yesterdaySession = PracticeSessionGenerator.generateDaily(date = yesterday)
        val yesterdaySnapshot = PracticeSessionSnapshot(
            state = PracticeSessionState(session = yesterdaySession, sessionId = dailyPracticeSessionId(yesterday)),
        )

        val isCompatibleToday = yesterdaySnapshot.level == PracticeLevel.Daily &&
            yesterdaySnapshot.sessionId == dailyPracticeSessionId(today)

        assertFalse(isCompatibleToday)
    }

    @Test
    fun requirement6_completingMarksDateAsCompleted() = runBlocking {
        val date = PracticeDate(2026, 8, 28)
        val before = engagementRepo.progress.first()
        assertFalse(before.isDailyPracticeCompleted(date))

        practiceRepo.recordDailySession(
            exercisesCompleted = 5,
            firstAttemptCorrect = 5,
            errors = 0,
            practiceDate = date.isoValue,
            sessionId = dailyPracticeSessionId(date),
        )

        val after = engagementRepo.progress.first()
        assertTrue(after.isDailyPracticeCompleted(date))
        assertTrue(date in after.dailyPracticeDates)
    }

    @Test
    fun requirement7_and_8_xpAndStreakAwardedOnce() = runBlocking {
        val date = PracticeDate(2026, 8, 28)
        val result = practiceRepo.recordDailySession(
            exercisesCompleted = 5,
            firstAttemptCorrect = 5,
            errors = 0,
            practiceDate = date.isoValue,
            sessionId = dailyPracticeSessionId(date),
        )

        val xp = result.engagementUpdate.reward.xpEarned
        assertTrue(xp in listOf(20, 30))
        assertEquals(1, result.engagementUpdate.reward.currentStreak)

        val engagement = engagementRepo.progress.first()
        assertEquals(xp.toLong(), engagement.totalXp)
        assertEquals(1, engagement.currentStreak)
    }

    @Test
    fun requirement9_repeatingSessionDoesNotDuplicateReward() = runBlocking {
        val date = PracticeDate(2026, 8, 28)
        val sessionId = dailyPracticeSessionId(date)

        val first = practiceRepo.recordDailySession(
            exercisesCompleted = 5,
            firstAttemptCorrect = 5,
            errors = 0,
            practiceDate = date.isoValue,
            sessionId = sessionId,
        )
        val replayed = practiceRepo.recordDailySession(
            exercisesCompleted = 5,
            firstAttemptCorrect = 5,
            errors = 0,
            practiceDate = date.isoValue,
            sessionId = sessionId,
        )
        val second = practiceRepo.recordDailySession(
            exercisesCompleted = 5,
            firstAttemptCorrect = 5,
            errors = 0,
            practiceDate = date.isoValue,
            sessionId = "daily_2026-08-28_repeat",
        )

        val firstXp = first.engagementUpdate.reward.xpEarned
        assertTrue(firstXp in listOf(20, 30))
        assertEquals(first.engagementUpdate.reward, replayed.engagementUpdate.reward)
        assertEquals(0, second.engagementUpdate.reward.xpEarned)

        val progress = practiceRepo.progress.first()
        assertEquals(1, progress.dailyCompletedSessions)
        assertEquals(5, progress.dailyTotalExercises)

        val engagement = engagementRepo.progress.first()
        assertEquals(firstXp.toLong(), engagement.totalXp)
        assertEquals(1, engagement.currentStreak)
    }

    @Test
    fun requirement10_nextDayReturnsToPending() = runBlocking {
        val today = PracticeDate(2026, 8, 28)
        val tomorrow = PracticeDate(2026, 8, 29)

        practiceRepo.recordDailySession(
            exercisesCompleted = 5,
            firstAttemptCorrect = 5,
            errors = 0,
            practiceDate = today.isoValue,
            sessionId = dailyPracticeSessionId(today),
        )

        val engagement = engagementRepo.progress.first()
        assertTrue(engagement.isDailyPracticeCompleted(today))
        assertFalse(engagement.isDailyPracticeCompleted(tomorrow))
    }

    @Test
    fun requirement11_progressAndGoalsReceiveCorrectUpdate() = runBlocking {
        val monday = PracticeDate(2026, 8, 24)
        practiceRepo.recordDailySession(
            exercisesCompleted = 5,
            firstAttemptCorrect = 4,
            errors = 1,
            practiceDate = monday.isoValue,
            sessionId = dailyPracticeSessionId(monday),
        )

        val progress = practiceRepo.progress.first()
        val engagement = engagementRepo.progress.first()

        assertEquals(1, progress.dailyCompletedSessions)
        assertEquals(5, progress.dailyTotalExercises)
        assertEquals(4, progress.dailyFirstAttemptCorrect)
        assertEquals(1, progress.dailyErrors)
        assertEquals(80, progress.dailyAccuracyPercentage)
        assertEquals(monday.isoValue, progress.dailyLastPracticeDate)

        assertEquals(1, engagement.weeklyPracticeDays(monday))
        assertEquals(5, engagement.monthlyExercises(monday))
    }

    @Test
    fun requirement12_normalPracticeIsNotAffected() = runBlocking {
        val date = PracticeDate(2026, 8, 28)

        val level1Record = practiceRepo.recordLevel1Session(
            exercisesCompleted = 10,
            firstAttemptCorrect = 8,
            errors = 2,
            practiceDate = date.isoValue,
        )
        val dailyRecord = practiceRepo.recordDailySession(
            exercisesCompleted = 5,
            firstAttemptCorrect = 5,
            errors = 0,
            practiceDate = date.isoValue,
            sessionId = dailyPracticeSessionId(date),
        )

        val progress = practiceRepo.progress.first()
        assertEquals(1, progress.level1CompletedSessions)
        assertEquals(10, progress.level1TotalExercises)
        assertEquals(1, progress.dailyCompletedSessions)
        assertEquals(5, progress.dailyTotalExercises)

        val engagement = engagementRepo.progress.first()
        assertEquals(2, engagement.totalSessions)
        assertEquals(15L, engagement.totalExercises)
        val expectedTotalXp = level1Record.engagementUpdate.reward.xpEarned.toLong() +
            dailyRecord.engagementUpdate.reward.xpEarned.toLong()
        assertEquals(expectedTotalXp, engagement.totalXp)
    }

    @Test
    fun requirement13_dailyPracticeExerciseCountProgressStep() {
        val date = PracticeDate(2026, 8, 28)
        val session = PracticeSessionGenerator.generateDaily(date = date)
        var state = PracticeSessionState(session = session, sessionId = dailyPracticeSessionId(date))

        assertEquals(1, state.exerciseNumber)
        assertEquals(5, state.session.exercises.size)

        for (i in 1..4) {
            val currentTarget = state.currentExercise.target.printedCharacter
            val answer = if (state.currentExercise.type == PracticeExerciseType.SignToCharacter) {
                currentTarget
            } else {
                currentTarget
            }
            state = state.selectAnswer(answer).checkAnswer().nextExercise()
            assertEquals(i + 1, state.exerciseNumber)
        }

        assertEquals(5, state.exerciseNumber)
    }

    @Test
    fun requirement14_normalLevelsRetainTheirIndependentConfiguration() {
        assertEquals(10, PracticeLevel.BrailleExplorer.exerciseCount)
        assertEquals(4, PracticeLevel.BrailleExplorer.optionCount)
        assertTrue(PracticeLevel.BrailleExplorer.allowsPointNumberToggle)
        assertEquals(null, PracticeLevel.BrailleExplorer.hintLimit)

        assertEquals(15, PracticeLevel.BrailleRecognizer.exerciseCount)
        assertEquals(6, PracticeLevel.BrailleRecognizer.optionCount)
        assertTrue(PracticeLevel.BrailleRecognizer.allowsPointNumberToggle)
        assertEquals(3, PracticeLevel.BrailleRecognizer.hintLimit)

        assertEquals(20, PracticeLevel.BrailleChallenge.exerciseCount)
        assertEquals(6, PracticeLevel.BrailleChallenge.optionCount)
        assertFalse(PracticeLevel.BrailleChallenge.allowsPointNumberToggle)
        assertEquals(0, PracticeLevel.BrailleChallenge.hintLimit)

        assertEquals(10, PracticeLevel.Custom.exerciseCount)
        assertEquals(6, PracticeLevel.Custom.optionCount)
        assertTrue(PracticeLevel.Custom.allowsPointNumberToggle)
        assertEquals(null, PracticeLevel.Custom.hintLimit)

        assertEquals(5, PracticeLevel.Daily.exerciseCount)
        assertEquals(4, PracticeLevel.Daily.optionCount)
        assertFalse(PracticeLevel.Daily.allowsPointNumberToggle)
        assertEquals(0, PracticeLevel.Daily.hintLimit)
    }

    @Test
    fun requirement15_resourceIdsAreConfiguredForDailyIdentity() {
        val dailyCompleted = com.brailuxaprende.R.string.daily_practice_completed
        val dailyTitle = com.brailuxaprende.R.string.daily_practice_header_title
        val dailySubtitle = com.brailuxaprende.R.string.daily_practice_header_subtitle
        val dailyBadge = com.brailuxaprende.R.string.daily_practice_badge
        val dailyBackHome = com.brailuxaprende.R.string.daily_practice_back_to_home
        val level1Title = com.brailuxaprende.R.string.practice_level_1_title
        val level1Completed = com.brailuxaprende.R.string.practice_level_completed

        assertNotEquals(dailyTitle, level1Title)
        assertNotEquals(dailyCompleted, level1Completed)
        assertTrue(dailyBadge != 0)
        assertTrue(dailySubtitle != 0)
        assertTrue(dailyBackHome != 0)
    }
}
