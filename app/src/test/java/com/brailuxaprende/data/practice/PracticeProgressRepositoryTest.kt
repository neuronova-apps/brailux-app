package com.brailuxaprende.data.practice

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.brailuxaprende.practice.PracticeSessionSummary
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
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
        assertEquals(0, progress.level1AccuracyPercentage)
        assertNull(progress.level1LastPracticeDate)
        assertEquals(0, progress.level2CompletedSessions)
        assertEquals(0, progress.level2TotalExercises)
        assertEquals(0, progress.level2FirstAttemptCorrect)
        assertEquals(0, progress.level2Errors)
        assertEquals(0, progress.level2HintsUsed)
        assertNull(progress.level2LastPracticeDate)
        assertEquals(0, progress.level3CompletedSessions)
        assertEquals(0, progress.level3TotalExercises)
        assertEquals(0, progress.level3FirstAttemptCorrect)
        assertEquals(0, progress.level3Errors)
        assertNull(progress.level3LastPracticeDate)
    }

    @Test
    fun level3ProgressPersistsIndependentlyFromPreviousLevels() = runBlocking {
        repository.recordLevel3Session(20, 16, 4, "2026-08-06")
        repository.recordLevel1Session(10, 8, "2026-08-07")
        repository.recordLevel2Session(15, 12, 3, 2, "2026-08-08")

        val progress = reopenRepository().progress.first()

        assertEquals(1, progress.level1CompletedSessions)
        assertEquals(10, progress.level1TotalExercises)
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
        val state = PracticeProgressState(repository, dataStoreScope)
        val updates = Channel<PracticeProgress>(capacity = Channel.UNLIMITED)
        val collector = launch(start = CoroutineStart.UNDISPATCHED) {
            state.progress.collect { progress -> updates.send(progress) }
        }
        val progressAtCallback = CompletableDeferred<Pair<Boolean, PracticeProgress>>()

        try {
            assertEquals(0, withTimeout(5_000L) { updates.receive() }.level1CompletedSessions)

            state.recordLevel1Session(
                summary = PracticeSessionSummary(
                    exercisesCompleted = 10,
                    firstAttemptCorrect = 8,
                    errors = 2,
                    accuracyPercentage = 80,
                    practicedLetters = ('A'..'J').toList(),
                ),
                practicedAt = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).parse("2026-08-07")!!,
                onRecorded = { recorded ->
                    progressAtCallback.complete(recorded to state.progress.value)
                },
            )

            val (recorded, progressObservedByState) = withTimeout(5_000L) {
                progressAtCallback.await()
            }
            assertTrue(recorded)
            assertEquals(1, progressObservedByState.level1CompletedSessions)
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
            assertEquals(80, progress.level1AccuracyPercentage)
            assertEquals("2026-08-07", progress.level1LastPracticeDate)
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
