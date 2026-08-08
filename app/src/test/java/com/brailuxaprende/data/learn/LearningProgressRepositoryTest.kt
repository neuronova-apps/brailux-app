package com.brailuxaprende.data.learn

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.brailuxaprende.learning.LearningLesson
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class LearningProgressRepositoryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var dataStoreFile: File
    private lateinit var dataStoreScope: CoroutineScope
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: LearningProgressRepository

    @Before
    fun setUp() {
        dataStoreFile = File(temporaryFolder.root, "learning.preferences_pb")
        createRepository()
    }

    @After
    fun tearDown() {
        runBlocking {
            dataStoreScope.coroutineContext[Job]?.cancelAndJoin()
        }
    }

    @Test
    fun `completed guided lessons persist locally`() = runBlocking {
        repository.markCompleted(LearningLesson.SixDots)
        repository.markCompleted(LearningLesson.Vowels)
        repository.markCompleted(LearningLesson.LettersAtoJ)
        repository.markCompleted(LearningLesson.LettersKtoT)
        repository.markCompleted(LearningLesson.LettersUtoZAndEnye)

        val progress = reopenRepository().progress.first()

        assertEquals(
            setOf(
                LearningLesson.SixDots,
                LearningLesson.Vowels,
                LearningLesson.LettersAtoJ,
                LearningLesson.LettersKtoT,
                LearningLesson.LettersUtoZAndEnye,
            ),
            progress.completedLessons,
        )
    }

    @Test
    fun `returning and repeating final lessons does not duplicate progress`() = runBlocking {
        repository.markCompleted(LearningLesson.LettersKtoT)
        repository.markCompleted(LearningLesson.LettersUtoZAndEnye)

        val progressAfterReturning = reopenRepository().progress.first()

        repository.markCompleted(LearningLesson.LettersKtoT)
        repository.markCompleted(LearningLesson.LettersUtoZAndEnye)

        val progress = repository.progress.first()

        assertEquals(progressAfterReturning, progress)
        assertEquals(2, progress.completedLessons.size)
        assertTrue(progress.isCompleted(LearningLesson.LettersKtoT))
        assertTrue(progress.isCompleted(LearningLesson.LettersUtoZAndEnye))
    }

    private fun createRepository() {
        dataStoreScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        dataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { dataStoreFile },
        )
        repository = LearningProgressRepository(dataStore)
    }

    private suspend fun reopenRepository(): LearningProgressRepository {
        dataStoreScope.coroutineContext[Job]?.cancelAndJoin()
        createRepository()
        return repository
    }
}
