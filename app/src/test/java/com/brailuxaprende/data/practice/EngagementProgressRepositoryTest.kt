package com.brailuxaprende.data.practice

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.brailuxaprende.data.learn.LearningProgressRepository
import com.brailuxaprende.learning.LearningLesson
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
    fun dailyChallengeProgressRoundTripsAfterRepositoryIsReopened() = runBlocking {
        val date = PracticeDate(2026, 8, 28)
        val update = repository.recordSession(
            session = session(
                kind = PracticeSessionKind.DailyChallenge,
                exercises = 10,
                firstAttemptCorrect = 10,
                mode = PracticeMode.Mixed,
            ),
            date = date,
        )

        val restored = reopenRepository().progress.first()

        assertEquals(update.progress, restored)
        assertTrue(restored.hasPracticed(date))
        assertTrue(restored.isDailyChallengeCompleted(date))
        assertEquals(1, restored.dailyChallengeSessions)
        assertEquals(mapOf(date.monthKey to 10), restored.monthlyExerciseCounts)
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

    @Test
    fun schemaV2FieldsRoundTripAfterRepositoryIsReopened() = runBlocking {
        val date = PracticeDate(2026, 8, 29)
        val mixedSession = session(
            kind = PracticeSessionKind.Level2,
            exercises = 15,
            firstAttemptCorrect = 15,
            mode = PracticeMode.Mixed,
        ).copy(
            isPrecisionEligible = true,
            exerciseResults = List(15) {
                com.brailuxaprende.practice.PracticeExerciseResult(firstAttemptCorrect = true, hintUsed = false)
            },
        )

        val update = repository.recordSession(mixedSession, date)
        val restored = reopenRepository().progress.first()

        assertEquals(update.progress, restored)
        assertEquals(1, restored.recognizerMixedSessions)
        assertEquals(0, restored.challengeMixedSessions)
        assertEquals(1, restored.advancedMixedSessions)
        assertEquals(15, restored.currentPrecisionStreak)
        assertEquals(15, restored.bestPrecisionStreak)
        assertEquals(date, restored.achievementUnlockDates[PermanentAchievement.FirstStep])
        assertEquals(date, restored.achievementUnlockDates[PermanentAchievement.BrailleFocus])
        assertEquals(date, restored.achievementUnlockDates[PermanentAchievement.BrailleRhythm])
        assertEquals(date, restored.achievementUnlockDates[PermanentAchievement.BraillePrecision])
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
            PracticeSessionKind.DailyChallenge -> 10
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

    @Test
    fun `existing user with 5 of 5 lessons completed retroactively syncs and unlocks FullAlphabet`() = runBlocking {
        // Simular usuario existente con las 5 lecciones completadas en DataStore pero sin FullAlphabet en logros
        dataStore.edit { preferences ->
            LearningProgressRepository.completedLessonKeys.values.forEach { key ->
                preferences[key] = true
            }
        }

        // Caso D: Al sincronizar o leer el progreso, FullAlphabet debe estar desbloqueado y tener fecha
        val syncDate = PracticeDate(2026, 8, 30)
        val synced = repository.syncAchievements(date = syncDate)

        assertTrue(
            "Caso D: Usuario con 5/5 debe desbloquear FullAlphabet retroactivamente",
            PermanentAchievement.FullAlphabet in synced.unlockedAchievements,
        )
        assertEquals(syncDate, synced.achievementUnlockDates[PermanentAchievement.FullAlphabet])

        // Verificar que persiste tras reiniciar el repositorio
        val restored = reopenRepository().progress.first()
        assertTrue(
            "FullAlphabet debe persistir tras reapertura del repositorio",
            PermanentAchievement.FullAlphabet in restored.unlockedAchievements,
        )
        assertEquals(syncDate, restored.achievementUnlockDates[PermanentAchievement.FullAlphabet])
    }

    @Test
    fun `syncing FullAlphabet when already unlocked is idempotent and preserves original date`() = runBlocking {
        val originalDate = PracticeDate(2026, 8, 15)
        dataStore.edit { preferences ->
            LearningProgressRepository.completedLessonKeys.values.forEach { key ->
                preferences[key] = true
            }
        }
        val firstSync = repository.syncAchievements(date = originalDate)
        assertEquals(originalDate, firstSync.achievementUnlockDates[PermanentAchievement.FullAlphabet])

        // Caso E: Volver a sincronizar en una fecha posterior
        val laterDate = PracticeDate(2026, 8, 30)
        val secondSync = repository.syncAchievements(date = laterDate)

        // No debe duplicar el logro ni alterar la fecha original
        assertEquals(
            1,
            secondSync.unlockedAchievements.count { it == PermanentAchievement.FullAlphabet },
        )
        assertEquals(
            "Caso E: La fecha original debe preservarse intacta tras nueva sincronización",
            originalDate,
            secondSync.achievementUnlockDates[PermanentAchievement.FullAlphabet],
        )
    }

    @Test
    fun `completing 5th lesson with LearningProgressRepository automatically unlocks and persists FullAlphabet`() = runBlocking {
        val learnRepo = LearningProgressRepository(dataStore)
        val completionDate = PracticeDate(2026, 8, 30)

        // Completar lecciones 1 a 4
        learnRepo.markCompleted(LearningLesson.SixDots, date = completionDate)
        learnRepo.markCompleted(LearningLesson.Vowels, date = completionDate)
        learnRepo.markCompleted(LearningLesson.LettersAtoJ, date = completionDate)
        learnRepo.markCompleted(LearningLesson.LettersKtoT, date = completionDate)

        val progress4 = repository.progress.first()
        assertFalse(
            "Con 4 lecciones no debe estar desbloqueado FullAlphabet",
            PermanentAchievement.FullAlphabet in progress4.unlockedAchievements,
        )

        // Completar lección 5
        learnRepo.markCompleted(LearningLesson.LettersUtoZAndEnye, date = completionDate)

        val progress5 = repository.progress.first()
        assertTrue(
            "Al completar la 5ta lección debe desbloquearse FullAlphabet automáticamente",
            PermanentAchievement.FullAlphabet in progress5.unlockedAchievements,
        )
        assertEquals(completionDate, progress5.achievementUnlockDates[PermanentAchievement.FullAlphabet])
    }
}

