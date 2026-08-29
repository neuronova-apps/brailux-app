package com.brailuxaprende.data.play

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class GameProgressRepositoryTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var dataStoreFile: File
    private lateinit var dataStoreScope: CoroutineScope
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: GameProgressRepository

    @Before
    fun setUp() {
        dataStoreFile = File(temporaryFolder.root, "games.preferences_pb")
        createRepository()
    }

    @After
    fun tearDown() {
        runBlocking {
            dataStoreScope.coroutineContext[Job]?.cancelAndJoin()
        }
    }

    @Test
    fun `initial game progress is empty with 0 completed games`() = runBlocking {
        val progress = repository.progress.first()
        assertEquals(0, progress.totalGamesCompleted)
        assertEquals(0, progress.memoryCompletedGames)
        assertNull(progress.memoryBestMoves)
        assertEquals(0, progress.sequenceCompletedGames)
        assertEquals(0, progress.sequenceBestLength)
        assertEquals(0, progress.orderCompletedGames)
        assertEquals(0, progress.orderTotalErrors)
        assertNull(progress.orderBestErrors)
    }

    @Test
    fun `recording completed Memory game updates memory stats and global total`() = runBlocking {
        val session1 = UUID.randomUUID().toString()
        val record1 = repository.recordMemoryGame(sessionId = session1, moves = 12)

        assertEquals(1, record1.totalGamesCompleted)
        assertEquals(1, record1.memoryCompletedGames)
        assertEquals(12, record1.memoryBestMoves)

        // Second game with fewer moves (better result)
        val session2 = UUID.randomUUID().toString()
        val record2 = repository.recordMemoryGame(sessionId = session2, moves = 8)

        assertEquals(2, record2.totalGamesCompleted)
        assertEquals(2, record2.memoryCompletedGames)
        assertEquals(8, record2.memoryBestMoves)

        // Third game with more moves (best remains 8)
        val session3 = UUID.randomUUID().toString()
        val record3 = repository.recordMemoryGame(sessionId = session3, moves = 15)

        assertEquals(3, record3.totalGamesCompleted)
        assertEquals(3, record3.memoryCompletedGames)
        assertEquals(8, record3.memoryBestMoves)
    }

    @Test
    fun `recording completed Sequence game updates sequence stats and global total`() = runBlocking {
        val session1 = UUID.randomUUID().toString()
        val record1 = repository.recordSequenceGame(
            sessionId = session1,
            correctSequences = 4,
            bestLength = 4,
            errors = 1,
        )

        assertEquals(1, record1.totalGamesCompleted)
        assertEquals(1, record1.sequenceCompletedGames)
        assertEquals(4, record1.sequenceBestLength)

        // Second game with higher length (5)
        val session2 = UUID.randomUUID().toString()
        val record2 = repository.recordSequenceGame(
            sessionId = session2,
            correctSequences = 5,
            bestLength = 5,
            errors = 0,
        )

        assertEquals(2, record2.totalGamesCompleted)
        assertEquals(2, record2.sequenceCompletedGames)
        assertEquals(5, record2.sequenceBestLength)
    }

    @Test
    fun `recording completed Order game updates order stats and global total`() = runBlocking {
        val session1 = UUID.randomUUID().toString()
        val record1 = repository.recordOrderGame(sessionId = session1, errors = 3)

        assertEquals(1, record1.totalGamesCompleted)
        assertEquals(1, record1.orderCompletedGames)
        assertEquals(3, record1.orderTotalErrors)
        assertEquals(3, record1.orderBestErrors)

        // Second game with fewer errors (1)
        val session2 = UUID.randomUUID().toString()
        val record2 = repository.recordOrderGame(sessionId = session2, errors = 1)

        assertEquals(2, record2.totalGamesCompleted)
        assertEquals(2, record2.orderCompletedGames)
        assertEquals(4, record2.orderTotalErrors)
        assertEquals(1, record2.orderBestErrors)
    }

    @Test
    fun `same session ID recorded twice is idempotent and does not increment stats`() = runBlocking {
        val session = UUID.randomUUID().toString()
        repository.recordMemoryGame(sessionId = session, moves = 10)
        val afterFirst = repository.progress.first()

        repository.recordMemoryGame(sessionId = session, moves = 10)
        val afterSecond = repository.progress.first()

        assertEquals(afterFirst, afterSecond)
        assertEquals(1, afterSecond.totalGamesCompleted)
        assertEquals(1, afterSecond.memoryCompletedGames)
    }

    @Test
    fun `games stats are completely independent across different game modes`() = runBlocking {
        val sessionMemory = UUID.randomUUID().toString()
        repository.recordMemoryGame(sessionId = sessionMemory, moves = 7)

        val sessionSeq = UUID.randomUUID().toString()
        repository.recordSequenceGame(sessionId = sessionSeq, correctSequences = 5, bestLength = 5, errors = 0)

        val sessionOrder = UUID.randomUUID().toString()
        repository.recordOrderGame(sessionId = sessionOrder, errors = 2)

        val progress = repository.progress.first()
        assertEquals(3, progress.totalGamesCompleted)
        assertEquals(1, progress.memoryCompletedGames)
        assertEquals(7, progress.memoryBestMoves)
        assertEquals(1, progress.sequenceCompletedGames)
        assertEquals(5, progress.sequenceBestLength)
        assertEquals(1, progress.orderCompletedGames)
        assertEquals(2, progress.orderTotalErrors)
        assertEquals(2, progress.orderBestErrors)
    }

    private fun createRepository() {
        dataStoreScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        dataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { dataStoreFile },
        )
        repository = GameProgressRepository(dataStore)
    }
}
