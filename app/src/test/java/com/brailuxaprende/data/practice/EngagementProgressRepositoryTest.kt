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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
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
    fun dailyCompletionRemainsFullyIdempotentAfterRestoration() = runBlocking {
        val date = dateForMini(DailyMiniAchievement.ThreeFirstAttemptCorrect)
        val dailySession = session(
            kind = PracticeSessionKind.Daily,
            exercises = 5,
            firstAttemptCorrect = 0,
        )
        val first = repository.recordSession(dailySession, date)

        val repeated = reopenRepository().recordSession(dailySession, date)

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
