package com.brailuxaprende.data.practice

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.brailuxaprende.practice.CustomExerciseCount
import com.brailuxaprende.practice.CustomPracticeConfiguration
import com.brailuxaprende.practice.PracticeLevel
import com.brailuxaprende.practice.PracticeMode
import com.brailuxaprende.practice.PracticeSessionGenerator
import com.brailuxaprende.practice.PracticeSessionSnapshot
import com.brailuxaprende.practice.PracticeSessionState
import java.io.File
import kotlin.random.Random
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

class PracticeSessionRepositoryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var dataStoreFile: File
    private lateinit var dataStoreScope: CoroutineScope
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: PracticeSessionRepository

    @Before
    fun setUp() {
        dataStoreFile = File(temporaryFolder.root, "practice-sessions.preferences_pb")
        createRepository()
    }

    private fun createRepository() {
        dataStoreScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        dataStore = PreferenceDataStoreFactory.create(scope = dataStoreScope) { dataStoreFile }
        repository = PracticeSessionRepository(dataStore)
    }

    @After
    fun tearDown() {
        runBlocking {
            dataStoreScope.coroutineContext[Job]?.cancelAndJoin()
        }
    }

    @Test
    fun freshRepositoryEmitsLoadedEmptyState() = runBlocking {
        val stored = repository.sessions.first()

        assertTrue(stored.isLoaded)
        assertTrue(stored.snapshots.isEmpty())
    }

    @Test
    fun sessionsAreStoredIndependentlyByLevelAndCanBeCleared() = runBlocking {
        val daily = PracticeSessionSnapshot(
            PracticeSessionState(
                PracticeSessionGenerator.generateDaily(Random(10)),
                sessionId = "daily-repository",
            ),
        )
        val customConfiguration = CustomPracticeConfiguration(
            exerciseCount = CustomExerciseCount.Fifteen,
            mode = PracticeMode.Mixed,
            hintsEnabled = false,
            showPointNumbers = false,
        )
        val custom = PracticeSessionSnapshot(
            PracticeSessionState(
                PracticeSessionGenerator.generateCustom(customConfiguration, Random(11)),
                sessionId = "custom-repository",
            ),
        )

        repository.save(daily)
        repository.save(custom)
        val stored = repository.sessions.first()

        assertEquals(daily, stored.snapshots[PracticeLevel.Daily])
        assertEquals(custom, stored.snapshots[PracticeLevel.Custom])

        repository.clear(PracticeLevel.Daily)
        val afterClear = repository.sessions.first()
        assertFalse(PracticeLevel.Daily in afterClear.snapshots)
        assertEquals(custom, afterClear.snapshots[PracticeLevel.Custom])
    }

    @Test
    fun sessionRestoresAfterDataStoreIsReallyReopened() = runBlocking {
        val original = PracticeSessionSnapshot(
            PracticeSessionState(
                PracticeSessionGenerator.generateLevel2(PracticeMode.Mixed, Random(16)),
                sessionId = "restored-after-process",
            ),
        )
        repository.save(original)

        dataStoreScope.coroutineContext[Job]?.cancelAndJoin()
        createRepository()

        val restored = repository.sessions.first()
        assertEquals(original, restored.snapshots[PracticeLevel.BrailleRecognizer])
    }

    @Test
    fun corruptStoredSessionIsRejectedWithoutHidingValidLevels() = runBlocking {
        val valid = PracticeSessionSnapshot(
            PracticeSessionState(
                PracticeSessionGenerator.generateLevel3(random = Random(12)),
                sessionId = "valid-level-3",
            ),
        )
        repository.save(valid)
        dataStore.edit { preferences ->
            preferences[stringPreferencesKey(
                practiceSessionSnapshotKeyName(PracticeLevel.BrailleExplorer),
            )] =
                "corrupt"
        }

        val stored = repository.sessions.first()

        assertEquals(valid, stored.snapshots[PracticeLevel.BrailleChallenge])
        assertFalse(PracticeLevel.BrailleExplorer in stored.snapshots)
    }
}
