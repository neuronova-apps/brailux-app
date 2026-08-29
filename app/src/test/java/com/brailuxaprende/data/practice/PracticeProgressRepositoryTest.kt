package com.brailuxaprende.data.practice

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.brailuxaprende.practice.DailyMiniAchievement
import com.brailuxaprende.practice.EngagementReward
import com.brailuxaprende.practice.PracticeSessionSummary
import com.brailuxaprende.practice.PracticeDate
import java.io.File
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PracticeProgressRepositoryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var dataStoreFile: File
    private lateinit var dataStoreScope: CoroutineScope
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: PracticeProgressRepository

    @Before
    fun setUp() {
        dataStoreFile = File(temporaryFolder.root, "practice.preferences_pb")
        createRepository()
    }

    @After
    fun tearDown() {
        runBlocking {
            dataStoreScope.coroutineContext[Job]?.cancelAndJoin()
        }
    }

    @Test
    fun progressUsesSafeDefaults() = runBlocking {
        val progress = repository.progress.first()

        assertEquals(0, progress.level1CompletedSessions)
        assertEquals(0, progress.level1TotalExercises)
        assertEquals(0, progress.level1FirstAttemptCorrect)
        assertEquals(0, progress.level1Errors)
        assertEquals(0, progress.level1AccuracyPercentage)
        assertNull(progress.level1LastPracticeDate)
        assertEquals(0, progress.level2CompletedSessions)
        assertEquals(0, progress.level2TotalExercises)
        assertEquals(0, progress.level2FirstAttemptCorrect)
        assertEquals(0, progress.level2Errors)
        assertEquals(0, progress.level2HintsUsed)
        assertEquals(0, progress.level2AccuracyPercentage)
        assertNull(progress.level2LastPracticeDate)
        assertEquals(0, progress.level3CompletedSessions)
        assertEquals(0, progress.level3TotalExercises)
        assertEquals(0, progress.level3FirstAttemptCorrect)
        assertEquals(0, progress.level3Errors)
        assertEquals(0, progress.level3AccuracyPercentage)
        assertNull(progress.level3LastPracticeDate)
        assertEquals(0, progress.customCompletedSessions)
        assertEquals(0, progress.customTotalExercises)
        assertEquals(0, progress.customFirstAttemptCorrect)
        assertEquals(0, progress.customErrors)
        assertEquals(0, progress.customHintsUsed)
        assertEquals(0, progress.customAccuracyPercentage)
        assertNull(progress.customLastPracticeDate)
        assertEquals(0, progress.dailyCompletedSessions)
        assertEquals(0, progress.dailyTotalExercises)
        assertEquals(0, progress.dailyFirstAttemptCorrect)
        assertEquals(0, progress.dailyErrors)
        assertEquals(0, progress.dailyAccuracyPercentage)
        assertNull(progress.dailyLastPracticeDate)
        assertEquals(0, progress.dailyChallengeCompletedSessions)
        assertEquals(0, progress.dailyChallengeTotalExercises)
        assertEquals(0, progress.dailyChallengeFirstAttemptCorrect)
        assertEquals(0, progress.dailyChallengeErrors)
        assertEquals(0, progress.dailyChallengeAccuracyPercentage)
        assertNull(progress.dailyChallengeLastPracticeDate)
    }

    @Test
    fun dailyChallengeProgressPersistsIndependently() = runBlocking {
        repository.recordDailyChallengeSession(
            exercisesCompleted = 10,
            firstAttemptCorrect = 9,
            errors = 1,
            practiceDate = "2026-08-28",
        )

        val progress = reopenRepository().progress.first()

        assertEquals(1, progress.dailyChallengeCompletedSessions)
        assertEquals(10, progress.dailyChallengeTotalExercises)
        assertEquals(9, progress.dailyChallengeFirstAttemptCorrect)
        assertEquals(1, progress.dailyChallengeErrors)
        assertEquals(90, progress.dailyChallengeAccuracyPercentage)
        assertEquals("2026-08-28", progress.dailyChallengeLastPracticeDate)
    }

    @Test
    fun level3ProgressPersistsIndependentlyFromPreviousLevels() = runBlocking {
        repository.recordLevel3Session(20, 16, 4, "2026-08-06")
        repository.recordLevel1Session(10, 8, 2, "2026-08-07")
        repository.recordLevel2Session(15, 12, 3, 2, "2026-08-08")

        val progress = reopenRepository().progress.first()

        assertEquals(1, progress.level1CompletedSessions)
        assertEquals(10, progress.level1TotalExercises)
        assertEquals(8, progress.level1FirstAttemptCorrect)
        assertEquals(2, progress.level1Errors)
        assertEquals(1, progress.level2CompletedSessions)
        assertEquals(15, progress.level2TotalExercises)
        assertEquals(1, progress.level3CompletedSessions)
        assertEquals(20, progress.level3TotalExercises)
        assertEquals(16, progress.level3FirstAttemptCorrect)
        assertEquals(4, progress.level3Errors)
        assertEquals("2026-08-06", progress.level3LastPracticeDate)
    }

    @Test
    fun level2ProgressPersistsIndependentlyFromLevel1() = runBlocking {
        repository.recordLevel1Session(10, 8, "2026-08-06")
        repository.recordLevel2Session(
            exercisesCompleted = 15,
            firstAttemptCorrect = 11,
            errors = 5,
            hintsUsed = 2,
            practiceDate = "2026-08-07",
        )

        val progress = reopenRepository().progress.first()

        assertEquals(1, progress.level1CompletedSessions)
        assertEquals(10, progress.level1TotalExercises)
        assertEquals(8, progress.level1FirstAttemptCorrect)
        assertEquals("2026-08-06", progress.level1LastPracticeDate)
        assertEquals(1, progress.level2CompletedSessions)
        assertEquals(15, progress.level2TotalExercises)
        assertEquals(11, progress.level2FirstAttemptCorrect)
        assertEquals(5, progress.level2Errors)
        assertEquals(2, progress.level2HintsUsed)
        assertEquals("2026-08-07", progress.level2LastPracticeDate)
    }

    @Test
    fun level1RecordingDoesNotResetExistingLevel2Progress() = runBlocking {
        repository.recordLevel2Session(15, 12, 3, 1, "2026-08-06")
        repository.recordLevel1Session(10, 9, "2026-08-07")

        val progress = repository.progress.first()

        assertEquals(1, progress.level2CompletedSessions)
        assertEquals(15, progress.level2TotalExercises)
        assertEquals(12, progress.level2FirstAttemptCorrect)
        assertEquals(3, progress.level2Errors)
        assertEquals(1, progress.level2HintsUsed)
        assertEquals("2026-08-06", progress.level2LastPracticeDate)
    }

    @Test
    fun accuracyPercentageUsesExistingData() {
        val progress = PracticeProgress(
            level1CompletedSessions = 3,
            level1TotalExercises = 30,
            level1FirstAttemptCorrect = 24,
            level1LastPracticeDate = "2026-08-07",
        )

        assertEquals(80, progress.level1AccuracyPercentage)
    }

    @Test
    fun accuracyPercentageIsSafeWithZeroExercises() {
        val progress = PracticeProgress(
            level1CompletedSessions = 1,
            level1TotalExercises = 0,
            level1FirstAttemptCorrect = 0,
            level1LastPracticeDate = "2026-08-07",
        )

        assertEquals(0, progress.level1AccuracyPercentage)
    }

    @Test
    fun oneCompletedSessionProvidesRealProgress() = runBlocking {
        repository.recordLevel1Session(10, 9, "2026-08-07")

        val progress = repository.progress.first()

        assertEquals(1, progress.level1CompletedSessions)
        assertEquals(10, progress.level1TotalExercises)
        assertEquals(9, progress.level1FirstAttemptCorrect)
        assertEquals(90, progress.level1AccuracyPercentage)
        assertEquals("2026-08-07", progress.level1LastPracticeDate)
    }

    @Test
    fun completedLevelSessionAtomicallyRegistersEngagementDayAndXp() = runBlocking {
        val result = repository.recordLevel1Session(
            exercisesCompleted = 10,
            firstAttemptCorrect = 7,
            practiceDate = "2026-08-09",
        )
        val engagement = EngagementProgressRepository(dataStore).progress.first()

        assertEquals(1, result.practiceProgress.level1CompletedSessions)
        assertEquals(30L, engagement.totalXp)
        assertEquals(setOf(PracticeDate(2026, 8, 9)), engagement.activityDates)
        assertEquals(result.engagementUpdate.progress, engagement)
    }

    @Test
    fun replayingStableIdsDoesNotDuplicateLevelProgressOrEngagement() = runBlocking {
        val level1 = repository.recordLevel1Session(
            exercisesCompleted = 10,
            firstAttemptCorrect = 8,
            practiceDate = "2026-08-09",
            sessionId = "level-1-stable-session",
        )
        val level2 = repository.recordLevel2Session(
            exercisesCompleted = 15,
            firstAttemptCorrect = 11,
            errors = 4,
            hintsUsed = 2,
            practiceDate = "2026-08-09",
            sessionId = "level-2-stable-session",
        )
        val level3 = repository.recordLevel3Session(
            exercisesCompleted = 20,
            firstAttemptCorrect = 15,
            errors = 5,
            practiceDate = "2026-08-09",
            sessionId = "level-3-stable-session",
        )
        val engagementBeforeReplay = EngagementProgressRepository(dataStore).progress.first()

        val reopened = reopenRepository()
        val replayedLevel1 = reopened.recordLevel1Session(
            exercisesCompleted = 10,
            firstAttemptCorrect = 8,
            practiceDate = "2026-08-09",
            sessionId = "level-1-stable-session",
        )
        val replayedLevel2 = reopened.recordLevel2Session(
            exercisesCompleted = 15,
            firstAttemptCorrect = 11,
            errors = 4,
            hintsUsed = 2,
            practiceDate = "2026-08-09",
            sessionId = "level-2-stable-session",
        )
        val replayedLevel3 = reopened.recordLevel3Session(
            exercisesCompleted = 20,
            firstAttemptCorrect = 15,
            errors = 5,
            practiceDate = "2026-08-09",
            sessionId = "level-3-stable-session",
        )

        val progress = reopened.progress.first()
        val engagementAfterReplay = EngagementProgressRepository(dataStore).progress.first()
        assertEquals(1, progress.level1CompletedSessions)
        assertEquals(10, progress.level1TotalExercises)
        assertEquals(1, progress.level2CompletedSessions)
        assertEquals(15, progress.level2TotalExercises)
        assertEquals(1, progress.level3CompletedSessions)
        assertEquals(20, progress.level3TotalExercises)
        assertEquals(engagementBeforeReplay, engagementAfterReplay)
        assertEquals(3, engagementAfterReplay.totalSessions)
        assertEquals(45L, engagementAfterReplay.totalExercises)
        assertEquals(level1.engagementUpdate.reward, replayedLevel1.engagementUpdate.reward)
        assertEquals(level2.engagementUpdate.reward, replayedLevel2.engagementUpdate.reward)
        assertEquals(level3.engagementUpdate.reward, replayedLevel3.engagementUpdate.reward)
    }

    @Test
    fun completedSessionsAccumulateAndPersist() = runBlocking {
        repository.recordLevel1Session(10, 8, "2026-08-06")
        repository.recordLevel1Session(10, 7, "2026-08-07")

        val reopenedRepository = reopenRepository()
        val progress = reopenedRepository.progress.first()

        assertEquals(2, progress.level1CompletedSessions)
        assertEquals(20, progress.level1TotalExercises)
        assertEquals(15, progress.level1FirstAttemptCorrect)
        assertEquals("2026-08-07", progress.level1LastPracticeDate)
    }

    @Test
    fun progressUpdatesAfterEachCompletedSession() = runBlocking {
        repository.recordLevel1Session(10, 8, "2026-08-05")

        val firstSession = repository.progress.first()
        assertEquals(1, firstSession.level1CompletedSessions)
        assertEquals(10, firstSession.level1TotalExercises)
        assertEquals(8, firstSession.level1FirstAttemptCorrect)

        repository.recordLevel1Session(10, 6, "2026-08-07")

        val secondSession = repository.progress.first()
        assertEquals(2, secondSession.level1CompletedSessions)
        assertEquals(20, secondSession.level1TotalExercises)
        assertEquals(14, secondSession.level1FirstAttemptCorrect)
        assertEquals("2026-08-07", secondSession.level1LastPracticeDate)
    }

    @Test
    fun practiceProgressStatePublishesACompletedSessionToItsObservedFlow() = runBlocking {
        val state = PracticeProgressState(
            repository = repository,
            scope = dataStoreScope,
            clock = { PracticeDate(2026, 8, 9) },
        )
        val updates = Channel<PracticeProgress>(capacity = Channel.UNLIMITED)
        val collector = launch(start = CoroutineStart.UNDISPATCHED) {
            state.progress.collect { progress -> updates.send(progress) }
        }
        val rewardAtCallback = CompletableDeferred<EngagementReward?>()

        try {
            assertEquals(0, withTimeout(5_000L) { updates.receive() }.level1CompletedSessions)

            state.recordLevel1Session(
                summary = PracticeSessionSummary(
                    exercisesCompleted = 10,
                    firstAttemptCorrect = 8,
                    errors = 2,
                    accuracyPercentage = 80,
                    practicedLetters = ('A'..'J').toList(),
                    longestFirstAttemptCorrectStreak = 3,
                ),
                onRecorded = { reward ->
                    rewardAtCallback.complete(reward)
                },
            )

            val reward = requireNotNull(withTimeout(5_000L) { rewardAtCallback.await() })
            assertEquals(40, reward.xpEarned)
            val progress = withTimeout(5_000L) {
                var observed: PracticeProgress
                do {
                    observed = updates.receive()
                } while (observed.level1CompletedSessions != 1)
                observed
            }

            assertEquals(1, progress.level1CompletedSessions)
            assertEquals(10, progress.level1TotalExercises)
            assertEquals(8, progress.level1FirstAttemptCorrect)
            assertEquals(2, progress.level1Errors)
            assertEquals(80, progress.level1AccuracyPercentage)
            assertEquals("2026-08-09", progress.level1LastPracticeDate)
            val engagement = EngagementProgressRepository(dataStore).progress.first()
            assertEquals(40L, engagement.totalXp)
            assertEquals(
                DailyMiniAchievement.ThreeFirstAttemptCorrect,
                engagement.miniAchievementType,
            )
            assertTrue(engagement.miniAchievementCompleted)
        } finally {
            collector.cancelAndJoin()
        }
    }

    @Test
    fun level4SessionRecordsCompletedSessionAndCounters() = runBlocking {
        val result = repository.recordCustomSession(
            exercisesCompleted = 15,
            firstAttemptCorrect = 12,
            errors = 3,
            hintsUsed = 2,
            practiceDate = "2026-08-10",
        )

        val progress = reopenRepository().progress.first()

        assertEquals(1, progress.customCompletedSessions)
        assertEquals(15, progress.customTotalExercises)
        assertEquals(12, progress.customFirstAttemptCorrect)
        assertEquals(3, progress.customErrors)
        assertEquals(2, progress.customHintsUsed)
        assertEquals(80, progress.customAccuracyPercentage)
        assertEquals("2026-08-10", progress.customLastPracticeDate)
        assertEquals(result.practiceProgress, progress)
    }

    @Test
    fun level4ProgressAccumulatesAcrossMultipleSessions() = runBlocking {
        repository.recordCustomSession(
            exercisesCompleted = 10,
            firstAttemptCorrect = 8,
            errors = 2,
            hintsUsed = 1,
            practiceDate = "2026-08-08",
        )
        repository.recordCustomSession(
            exercisesCompleted = 15,
            firstAttemptCorrect = 12,
            errors = 3,
            hintsUsed = 2,
            practiceDate = "2026-08-09",
        )

        val progress = reopenRepository().progress.first()

        assertEquals(2, progress.customCompletedSessions)
        assertEquals(25, progress.customTotalExercises)
        assertEquals(20, progress.customFirstAttemptCorrect)
        assertEquals(5, progress.customErrors)
        assertEquals(3, progress.customHintsUsed)
        assertEquals(80, progress.customAccuracyPercentage)
        assertEquals("2026-08-09", progress.customLastPracticeDate)
    }

    @Test
    fun level4AccuracyPercentageCalculatesCorrectly() {
        val progress = PracticeProgress(
            customCompletedSessions = 2,
            customTotalExercises = 20,
            customFirstAttemptCorrect = 15,
            customErrors = 5,
            customHintsUsed = 3,
            customLastPracticeDate = "2026-08-09",
        )

        assertEquals(75, progress.customAccuracyPercentage)

        val emptyProgress = PracticeProgress()
        assertEquals(0, emptyProgress.customAccuracyPercentage)
    }

    @Test
    fun level4SessionProtectsAgainstDuplicatesViaSessionId() = runBlocking {
        val firstRecord = repository.recordCustomSession(
            exercisesCompleted = 10,
            firstAttemptCorrect = 8,
            errors = 2,
            hintsUsed = 1,
            practiceDate = "2026-08-09",
            sessionId = "custom-stable-session",
        )

        val reopened = reopenRepository()
        val secondRecord = reopened.recordCustomSession(
            exercisesCompleted = 10,
            firstAttemptCorrect = 8,
            errors = 2,
            hintsUsed = 1,
            practiceDate = "2026-08-09",
            sessionId = "custom-stable-session",
        )

        val progress = reopened.progress.first()
        assertEquals(1, progress.customCompletedSessions)
        assertEquals(10, progress.customTotalExercises)
        assertEquals(8, progress.customFirstAttemptCorrect)
        assertEquals(2, progress.customErrors)
        assertEquals(1, progress.customHintsUsed)
        assertEquals(firstRecord.engagementUpdate.reward, secondRecord.engagementUpdate.reward)
    }

    @Test
    fun legacyDataWithoutNewKeysLoadsDefaultZeros() = runBlocking {
        dataStore.edit { preferences ->
            preferences[intPreferencesKey(Level1CompletedSessionsKeyName)] = 3
            preferences[intPreferencesKey(Level1TotalExercisesKeyName)] = 30
            preferences[intPreferencesKey(Level1FirstAttemptCorrectKeyName)] = 25
            preferences[stringPreferencesKey(Level1LastPracticeDateKeyName)] = "2026-08-01"
            preferences[intPreferencesKey(Level2CompletedSessionsKeyName)] = 2
            preferences[intPreferencesKey(Level2TotalExercisesKeyName)] = 30
            preferences[intPreferencesKey(Level2FirstAttemptCorrectKeyName)] = 20
            preferences[intPreferencesKey(Level2ErrorsKeyName)] = 10
            preferences[intPreferencesKey(Level2HintsUsedKeyName)] = 4
            preferences[stringPreferencesKey(Level2LastPracticeDateKeyName)] = "2026-08-02"
        }

        val progress = repository.progress.first()

        assertEquals(3, progress.level1CompletedSessions)
        assertEquals(30, progress.level1TotalExercises)
        assertEquals(25, progress.level1FirstAttemptCorrect)
        assertEquals(0, progress.level1Errors)
        assertEquals("2026-08-01", progress.level1LastPracticeDate)
        assertEquals(0, progress.customCompletedSessions)
        assertEquals(0, progress.customTotalExercises)
        assertEquals(0, progress.customFirstAttemptCorrect)
        assertEquals(0, progress.customErrors)
        assertEquals(0, progress.customHintsUsed)
        assertEquals(0, progress.customAccuracyPercentage)
        assertNull(progress.customLastPracticeDate)
    }

    @Test
    fun level1RecordsErrorsAndPersistsIndependently() = runBlocking {
        repository.recordLevel1Session(
            exercisesCompleted = 10,
            firstAttemptCorrect = 7,
            errors = 3,
            practiceDate = "2026-08-08",
        )

        val progress = reopenRepository().progress.first()

        assertEquals(1, progress.level1CompletedSessions)
        assertEquals(10, progress.level1TotalExercises)
        assertEquals(7, progress.level1FirstAttemptCorrect)
        assertEquals(3, progress.level1Errors)
        assertEquals(70, progress.level1AccuracyPercentage)
        assertEquals("2026-08-08", progress.level1LastPracticeDate)
    }

    @Test
    fun practiceProgressStatePublishesCustomSessionToObservedFlow() = runBlocking {
        val state = PracticeProgressState(
            repository = repository,
            scope = dataStoreScope,
            clock = { PracticeDate(2026, 8, 9) },
        )
        val updates = Channel<PracticeProgress>(capacity = Channel.UNLIMITED)
        val collector = launch(start = CoroutineStart.UNDISPATCHED) {
            state.progress.collect { progress -> updates.send(progress) }
        }
        val rewardAtCallback = CompletableDeferred<EngagementReward?>()

        try {
            assertEquals(0, withTimeout(5_000L) { updates.receive() }.customCompletedSessions)

            state.recordCustomSession(
                summary = PracticeSessionSummary(
                    exercisesCompleted = 15,
                    firstAttemptCorrect = 12,
                    errors = 3,
                    hintsUsed = 1,
                    accuracyPercentage = 80,
                    practicedLetters = ('A'..'O').toList(),
                    longestFirstAttemptCorrectStreak = 4,
                ),
                onRecorded = { reward ->
                    rewardAtCallback.complete(reward)
                },
            )

            val reward = requireNotNull(withTimeout(5_000L) { rewardAtCallback.await() })
            assertEquals(50, reward.xpEarned)
            val progress = withTimeout(5_000L) {
                var observed: PracticeProgress
                do {
                    observed = updates.receive()
                } while (observed.customCompletedSessions != 1)
                observed
            }

            assertEquals(1, progress.customCompletedSessions)
            assertEquals(15, progress.customTotalExercises)
            assertEquals(12, progress.customFirstAttemptCorrect)
            assertEquals(3, progress.customErrors)
            assertEquals(1, progress.customHintsUsed)
            assertEquals(80, progress.customAccuracyPercentage)
            assertEquals("2026-08-09", progress.customLastPracticeDate)
        } finally {
            collector.cancelAndJoin()
        }
    }

    @Test
    fun dailySessionRecordsCompletedSessionAndCounters() = runBlocking {
        val result = repository.recordDailySession(
            exercisesCompleted = 5,
            firstAttemptCorrect = 4,
            errors = 1,
            practiceDate = "2026-08-11",
        )

        val progress = reopenRepository().progress.first()

        assertEquals(1, progress.dailyCompletedSessions)
        assertEquals(5, progress.dailyTotalExercises)
        assertEquals(4, progress.dailyFirstAttemptCorrect)
        assertEquals(1, progress.dailyErrors)
        assertEquals(80, progress.dailyAccuracyPercentage)
        assertEquals("2026-08-11", progress.dailyLastPracticeDate)
        assertEquals(result.practiceProgress, progress)
        val expectedXp = if (result.engagementUpdate.reward.miniAchievementCompleted != null) 30 else 20
        assertEquals(expectedXp, result.engagementUpdate.reward.xpEarned)
    }

    @Test
    fun dailySessionProtectsAgainstDuplicatesViaDateAndSessionId() = runBlocking {
        val firstRecord = repository.recordDailySession(
            exercisesCompleted = 5,
            firstAttemptCorrect = 5,
            errors = 0,
            practiceDate = "2026-08-11",
            sessionId = "daily-2026-08-11-first",
        )

        val reopened = reopenRepository()
        val replayedRecord = reopened.recordDailySession(
            exercisesCompleted = 5,
            firstAttemptCorrect = 5,
            errors = 0,
            practiceDate = "2026-08-11",
            sessionId = "daily-2026-08-11-first",
        )
        val repeatedOnSameDay = reopened.recordDailySession(
            exercisesCompleted = 5,
            firstAttemptCorrect = 5,
            errors = 0,
            practiceDate = "2026-08-11",
            sessionId = "daily-2026-08-11-second",
        )

        val progress = reopened.progress.first()
        assertEquals(1, progress.dailyCompletedSessions)
        assertEquals(5, progress.dailyTotalExercises)
        assertEquals(5, progress.dailyFirstAttemptCorrect)
        assertEquals(0, progress.dailyErrors)
        assertTrue(firstRecord.engagementUpdate.reward.xpEarned in listOf(20, 30))
        assertEquals(firstRecord.engagementUpdate.reward, replayedRecord.engagementUpdate.reward)
        assertEquals(0, repeatedOnSameDay.engagementUpdate.reward.xpEarned)
    }

    @Test
    fun practiceProgressStatePublishesDailySessionToObservedFlow() = runBlocking {
        val state = PracticeProgressState(
            repository = repository,
            scope = dataStoreScope,
            clock = { PracticeDate(2026, 8, 11) },
        )
        val updates = Channel<PracticeProgress>(capacity = Channel.UNLIMITED)
        val collector = launch(start = CoroutineStart.UNDISPATCHED) {
            state.progress.collect { progress -> updates.send(progress) }
        }
        val rewardAtCallback = CompletableDeferred<EngagementReward?>()

        try {
            assertEquals(0, withTimeout(5_000L) { updates.receive() }.dailyCompletedSessions)

            state.recordDailySession(
                summary = PracticeSessionSummary(
                    exercisesCompleted = 5,
                    firstAttemptCorrect = 4,
                    errors = 1,
                    accuracyPercentage = 80,
                    practicedLetters = ('A'..'E').toList(),
                    longestFirstAttemptCorrectStreak = 3,
                ),
                onRecorded = { reward ->
                    rewardAtCallback.complete(reward)
                },
            )

            val reward = requireNotNull(withTimeout(5_000L) { rewardAtCallback.await() })
            val expectedXp = if (reward.miniAchievementCompleted != null) 30 else 20
            assertEquals(expectedXp, reward.xpEarned)
            val progress = withTimeout(5_000L) {
                var observed: PracticeProgress
                do {
                    observed = updates.receive()
                } while (observed.dailyCompletedSessions != 1)
                observed
            }

            assertEquals(1, progress.dailyCompletedSessions)
            assertEquals(5, progress.dailyTotalExercises)
            assertEquals(4, progress.dailyFirstAttemptCorrect)
            assertEquals(1, progress.dailyErrors)
            assertEquals(80, progress.dailyAccuracyPercentage)
            assertEquals("2026-08-11", progress.dailyLastPracticeDate)
        } finally {
            collector.cancelAndJoin()
        }
    }

    private fun createRepository() {
        dataStoreScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        dataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { dataStoreFile },
        )
        repository = PracticeProgressRepository(dataStore)
    }

    private suspend fun reopenRepository(): PracticeProgressRepository {
        dataStoreScope.coroutineContext[Job]?.cancelAndJoin()
        createRepository()
        return repository
    }
}
