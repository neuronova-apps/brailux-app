package com.brailuxaprende.data.practice

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.brailuxaprende.practice.CustomExerciseCount
import com.brailuxaprende.practice.CustomPracticeConfiguration
import com.brailuxaprende.practice.PracticeMode
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CustomPracticePreferencesRepositoryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var dataStoreFile: File
    private lateinit var dataStoreScope: CoroutineScope
    private lateinit var repository: CustomPracticePreferencesRepository

    @Before
    fun setUp() {
        dataStoreFile = File(temporaryFolder.root, "custom-practice.preferences_pb")
        createRepository()
    }

    @After
    fun tearDown() {
        dataStoreScope.cancel()
    }

    @Test
    fun defaultsMatchCustomPracticeRequirements() = runBlocking {
        assertEquals(CustomPracticeConfiguration(), repository.configuration.first())
    }

    @Test
    fun restoresLastUsedConfiguration() = runBlocking {
        repository.save(
            CustomPracticeConfiguration(
                exerciseCount = CustomExerciseCount.Twenty,
                mode = PracticeMode.Mixed,
                hintsEnabled = false,
                showPointNumbers = false,
            ),
        )

        val restored = reopenRepository().configuration.first()

        assertEquals(CustomExerciseCount.Twenty, restored.exerciseCount)
        assertEquals(PracticeMode.Mixed, restored.mode)
        assertFalse(restored.hintsEnabled)
        assertFalse(restored.showPointNumbers)
        assertEquals(emptySet<Any>(), restored.additionalContentGroups)
    }

    private fun createRepository() {
        dataStoreScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { dataStoreFile },
        )
        repository = CustomPracticePreferencesRepository(dataStore)
    }

    private suspend fun reopenRepository(): CustomPracticePreferencesRepository {
        dataStoreScope.coroutineContext[Job]?.cancelAndJoin()
        createRepository()
        return repository
    }
}
