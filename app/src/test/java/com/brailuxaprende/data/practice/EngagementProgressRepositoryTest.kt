package com.brailuxaprende.data.practice

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.brailuxaprende.practice.DailyMiniAchievement
import com.brailuxaprende.practice.EngagementProgress
import com.brailuxaprende.practice.EngagementSession
import com.brailuxaprende.practice.PermanentAchievement
import com.brailuxaprende.practice.PracticeDate
import com.brailuxaprende.practice.PracticeMode
import com.brailuxaprende.practice.PracticeSessionKind
import java.io.File
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class EngagementProgressRepositoryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var dataStoreFile: File
    private lateinit var dataStoreScope: CoroutineScope
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: EngagementProgressRepository

    @Before
    fun setUp() {
        dataStoreFile = File(temporaryFolder.root, "engagement.preferences_pb")
        createRepository()
    }

    @After
    fun tearDown() {
        runBlocking {
            dataStoreScope.coroutineContext[Job]?.cancelAndJoin()
        }
    }

    @Test
    fun readingFreshProgressDoesNotRegisterActivityOrWritePreferences() = runBlocking {
        val progress = repository.progress.first()
        val storedPreferences = dataStore.data.first()

        assertEquals(EngagementProgress(), progress)
        assertTrue(storedPreferences.asMap().isEmpty())
    }

    @Test
    fun completeProgressRoundTripsAfterRepositoryIsReopened() = runBlocking {
        val date = dateForMini(DailyMiniAchievement.TwoModalities)
        val update = repository.recordSession(
            session = session(
                kind = PracticeSessionKind.Daily,
                exercises = 5,
                firstAttemptCorrect = 5,
                mode = PracticeMode.Mixed,
            ),
            date = date,
        )

        val restored = reopenRepository().progress.first()

        assertEquals(update.progress, restored)
        assertTrue(restored.hasPracticed(date))
        assertTrue(restored.isDailyPracticeCompleted(date))
        assertTrue(restored.miniAchievement(date).completed)
        assertEquals(setOf(date), restored.miniRewardedDates)
        assertEquals(mapOf(date.monthKey to 5), restored.monthlyExerciseCounts)
        assertTrue(PermanentAchievement.FirstStep in restored.unlockedAchievements)
    }

    @Test
    fun streakWeekMonthAndAchievementsSurviveRestoration() = runBlocking {
        val monday = PracticeDate(2026, 8, 3)
        repeat(5) { offset ->
            repository.recordSession(
                session = session(PracticeSessionKind.Level3, exercises = 20),
                date = monday.plusDays(offset),
            )
        }

        val restored = reopenRepository().progress.first()

        assertEquals(5, restored.currentStreak)
        assertEquals(5, restored.bestStreak)
        assertEquals(5, restored.weeklyPracticeDays(monday.plusDays(4)))
        assertEquals(100, restored.monthlyExercises(monday))
        assertTrue(restored.isMonthlyGoalCompleted(monday))
        assertTrue(PermanentAchievement.Consistency in restored.unlockedAchievements)
        assertTrue(PermanentAchievement.WeekInMotion in restored.unlockedAchievements)
        assertTrue(PermanentAchievement.Challenger in restored.unlockedAchievements)
        assertTrue(PermanentAchievement.HundredExercises in restored.unlockedAchievements)
        assertEquals(mapOf(monday.monthKey to 100), restored.monthlyExerciseCounts)
    }

    @Test
    fun dailyCompletionRemainsFullyIdempotentForANewSessionIdAfterRestoration() = runBlocking {
        val date = dateForMini(DailyMiniAchievement.ThreeFirstAttemptCorrect)
        val dailySession = session(
            kind = PracticeSessionKind.Daily,
            exercises = 5,
            firstAttemptCorrect = 0,
        ).copy(id = "daily-session-original")
        val first = repository.recordSession(dailySession, date)

        val repeated = reopenRepository().recordSession(
            dailySession.copy(id = "daily-session-new-attempt"),
            date,
        )

        assertEquals(20, first.reward.xpEarned)
        assertEquals(0, repeated.reward.xpEarned)
        assertEquals(20L, repeated.progress.totalXp)
        assertEquals(1, repeated.progress.totalSessions)
        assertEquals(5L, repeated.progress.totalExercises)
        assertEquals(mapOf(date.monthKey to 5), repeated.progress.monthlyExerciseCounts)
        assertEquals(setOf(date), repeated.progress.dailyPracticeDates)
        assertEquals(1, repeated.progress.activityDates.size)
    }

    @Test
    fun sameSessionIdReplaysOriginalRewardWithoutMutatingProgress() = runBlocking {
        val date = dateForMini(DailyMiniAchievement.CompleteSession)
        val dailySession = session(
            kind = PracticeSessionKind.Daily,
            exercises = 5,
            firstAttemptCorrect = 5,
            mode = PracticeMode.Mixed,
        ).copy(id = "daily-session-replayed")
        val first = repository.recordSession(dailySession, date)

        val replayed = reopenRepository().recordSession(dailySession, date)
        val restored = repository.progress.first()

        assertEquals(first.reward, replayed.reward)
        assertEquals(first.progress, replayed.progress)
        assertEquals(first.progress, restored)
        assertEquals(1, restored.totalSessions)
        assertEquals(5L, restored.totalExercises)
        assertEquals(setOf(date), restored.dailyPracticeDates)
        assertEquals(DailyMiniAchievement.CompleteSession, replayed.reward.miniAchievementCompleted)
        assertTrue(PermanentAchievement.FirstStep in replayed.reward.newlyUnlockedAchievements)
    }

    @Test
    fun customSessionReplayAfterRestorationPreservesRewardWithoutDuplicatingProgress() = runBlocking {
        val date = dateForMini(DailyMiniAchievement.CompleteFiveExercises)
        val customSession = session(
            kind = PracticeSessionKind.Custom,
            exercises = 10,
            firstAttemptCorrect = 7,
            mode = PracticeMode.Mixed,
        ).copy(id = "custom-session-replayed")
        val first = repository.recordSession(customSession, date)

        val replayed = reopenRepository().recordSession(customSession, date)
        val restored = repository.progress.first()

        assertEquals(first.reward, replayed.reward)
        assertEquals(first.progress, replayed.progress)
        assertEquals(first.progress, restored)
        assertEquals(40L, restored.totalXp)
        assertEquals(1, restored.totalSessions)
        assertEquals(10L, restored.totalExercises)
        assertEquals(1, restored.customSessions)
        assertEquals(mapOf(date.monthKey to 10), restored.monthlyExerciseCounts)
        assertEquals(first.progress.miniAchievement(date), restored.miniAchievement(date))
        assertTrue(restored.miniAchievement(date).completed)
        assertEquals(setOf(date), restored.miniRewardedDates)
    }

    @Test
    fun concurrentRecordingsWithTheSameSessionIdCreditOnlyOnce() = runBlocking {
        val date = dateForMini(DailyMiniAchievement.CompleteSession)
        val customSession = session(
            kind = PracticeSessionKind.Custom,
            exercises = 10,
            firstAttemptCorrect = 6,
            mode = PracticeMode.Mixed,
        ).copy(id = "custom-session-concurrent")
        val start = CompletableDeferred<Unit>()

        val results = coroutineScope {
            val recordings = List(2) {
                async(Dispatchers.Default) {
                    start.await()
                    repository.recordSession(customSession, date)
                }
            }
            start.complete(Unit)
            recordings.awaitAll()
        }
        val stored = repository.progress.first()

        assertEquals(results[0].reward, results[1].reward)
        assertEquals(stored, results[0].progress)
        assertEquals(stored, results[1].progress)
        assertEquals(40L, stored.totalXp)
        assertEquals(1, stored.totalSessions)
        assertEquals(10L, stored.totalExercises)
        assertEquals(1, stored.customSessions)
        assertEquals(mapOf(date.monthKey to 10), stored.monthlyExerciseCounts)
        assertTrue(stored.miniAchievement(date).completed)
        assertEquals(setOf(date), stored.miniRewardedDates)
    }

    @Test
    fun legacyLevelProgressIsRestoredWithoutLosingXpDatesOrDerivableAchievements() = runBlocking {
        dataStore.edit { preferences ->
            preferences[intPreferencesKey(Level1CompletedSessionsKeyName)] = 5
            preferences[intPreferencesKey(Level1TotalExercisesKeyName)] = 50
            preferences[stringPreferencesKey(Level1LastPracticeDateKeyName)] = "2026-08-06"
            preferences[intPreferencesKey(Level2CompletedSessionsKeyName)] = 2
            preferences[intPreferencesKey(Level2TotalExercisesKeyName)] = 30
            preferences[stringPreferencesKey(Level2LastPracticeDateKeyName)] = "2026-08-07"
            preferences[intPreferencesKey(Level3CompletedSessionsKeyName)] = 1
            preferences[intPreferencesKey(Level3TotalExercisesKeyName)] = 20
            preferences[stringPreferencesKey(Level3LastPracticeDateKeyName)] = "2026-08-08"
        }

        val migrated = repository.progress.first()

        assertEquals(8, migrated.totalSessions)
        assertEquals(100L, migrated.totalExercises)
        assertEquals(300L, migrated.totalXp)
        assertEquals(3, migrated.activityDates.size)
        assertEquals(3, migrated.currentStreak)
        assertEquals(PracticeDate(2026, 8, 8), migrated.lastActivityDate)
        assertTrue(PermanentAchievement.FirstStep in migrated.unlockedAchievements)
        assertTrue(PermanentAchievement.Consistency in migrated.unlockedAchievements)
        assertTrue(PermanentAchievement.Explorer in migrated.unlockedAchievements)
        assertTrue(PermanentAchievement.HundredExercises in migrated.unlockedAchievements)
        assertFalse(PermanentAchievement.Recognizer in migrated.unlockedAchievements)
        assertFalse(PermanentAchievement.Challenger in migrated.unlockedAchievements)
    }

    private fun createRepository() {
        dataStoreScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        dataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { dataStoreFile },
        )
        repository = EngagementProgressRepository(dataStore)
    }

    private suspend fun reopenRepository(): EngagementProgressRepository {
        dataStoreScope.coroutineContext[Job]?.cancelAndJoin()
        createRepository()
        return repository
    }

    private fun session(
        kind: PracticeSessionKind,
        exercises: Int,
        firstAttemptCorrect: Int = 0,
        mode: PracticeMode = PracticeMode.SignToCharacter,
    ): EngagementSession = EngagementSession(
        kind = kind,
        exercisesCompleted = exercises,
        firstAttemptCorrect = firstAttemptCorrect,
        mode = mode,
    )

    private fun session(kind: PracticeSessionKind): EngagementSession = session(
        kind = kind,
        exercises = when (kind) {
            PracticeSessionKind.Daily -> 5
            PracticeSessionKind.Level1 -> 10
            PracticeSessionKind.Level2 -> 15
            PracticeSessionKind.Level3 -> 20
            PracticeSessionKind.Custom -> 10
        },
    )

    private fun dateForMini(type: DailyMiniAchievement): PracticeDate {
        var date = PracticeDate(2026, 1, 1)
        repeat(DailyMiniAchievement.entries.size * 2) {
            if (DailyMiniAchievement.forDate(date) == type) return date
            date = date.plusDays(1)
        }
        error("No date found for $type")
    }
}
