package com.brailuxaprende.data.play

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class GameProgress(
    val totalGamesCompleted: Int = 0,
    val memoryCompletedGames: Int = 0,
    val memoryBestMoves: Int? = null,
    val sequenceCompletedGames: Int = 0,
    val sequenceBestLength: Int = 0,
    val orderCompletedGames: Int = 0,
    val orderTotalErrors: Int = 0,
    val orderBestErrors: Int? = null,
)

class GameProgressRepository(
    private val dataStore: DataStore<Preferences>,
) {
    val progress: Flow<GameProgress> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences -> preferences.toGameProgress() }

    suspend fun recordMemoryGame(
        sessionId: String,
        moves: Int,
    ): GameProgress {
        require(sessionId.isNotBlank())
        require(moves > 0)

        var updatedProgress: GameProgress? = null
        dataStore.edit { preferences ->
            val creditedSessions = preferences[CreditedGameSessionsKey] ?: emptySet()
            if (sessionId in creditedSessions) {
                updatedProgress = preferences.toGameProgress()
                return@edit
            }

            val current = preferences.toGameProgress()
            val newTotalGames = current.totalGamesCompleted + 1
            val newMemoryGames = current.memoryCompletedGames + 1
            val newBestMoves = if (current.memoryBestMoves == null || current.memoryBestMoves == 0) {
                moves
            } else {
                minOf(current.memoryBestMoves, moves)
            }

            preferences[TotalGamesCompletedKey] = newTotalGames
            preferences[MemoryCompletedGamesKey] = newMemoryGames
            preferences[MemoryBestMovesKey] = newBestMoves
            preferences[CreditedGameSessionsKey] = creditedSessions + sessionId

            updatedProgress = current.copy(
                totalGamesCompleted = newTotalGames,
                memoryCompletedGames = newMemoryGames,
                memoryBestMoves = newBestMoves,
            )
        }
        return checkNotNull(updatedProgress)
    }

    suspend fun recordSequenceGame(
        sessionId: String,
        correctSequences: Int,
        bestLength: Int,
        errors: Int,
    ): GameProgress {
        require(sessionId.isNotBlank())
        require(correctSequences in 0..5)
        require(bestLength in 0..5)
        require(errors >= 0)

        var updatedProgress: GameProgress? = null
        dataStore.edit { preferences ->
            val creditedSessions = preferences[CreditedGameSessionsKey] ?: emptySet()
            if (sessionId in creditedSessions) {
                updatedProgress = preferences.toGameProgress()
                return@edit
            }

            val current = preferences.toGameProgress()
            val newTotalGames = current.totalGamesCompleted + 1
            val newSequenceGames = current.sequenceCompletedGames + 1
            val newBestLength = maxOf(current.sequenceBestLength, bestLength)

            preferences[TotalGamesCompletedKey] = newTotalGames
            preferences[SequenceCompletedGamesKey] = newSequenceGames
            preferences[SequenceBestLengthKey] = newBestLength
            preferences[CreditedGameSessionsKey] = creditedSessions + sessionId

            updatedProgress = current.copy(
                totalGamesCompleted = newTotalGames,
                sequenceCompletedGames = newSequenceGames,
                sequenceBestLength = newBestLength,
            )
        }
        return checkNotNull(updatedProgress)
    }

    suspend fun recordOrderGame(
        sessionId: String,
        errors: Int,
    ): GameProgress {
        require(sessionId.isNotBlank())
        require(errors >= 0)

        var updatedProgress: GameProgress? = null
        dataStore.edit { preferences ->
            val creditedSessions = preferences[CreditedGameSessionsKey] ?: emptySet()
            if (sessionId in creditedSessions) {
                updatedProgress = preferences.toGameProgress()
                return@edit
            }

            val current = preferences.toGameProgress()
            val newTotalGames = current.totalGamesCompleted + 1
            val newOrderGames = current.orderCompletedGames + 1
            val newTotalErrors = current.orderTotalErrors + errors
            val newBestErrors = if (current.orderBestErrors == null) {
                errors
            } else {
                minOf(current.orderBestErrors, errors)
            }

            preferences[TotalGamesCompletedKey] = newTotalGames
            preferences[OrderCompletedGamesKey] = newOrderGames
            preferences[OrderTotalErrorsKey] = newTotalErrors
            preferences[OrderBestErrorsKey] = newBestErrors
            preferences[CreditedGameSessionsKey] = creditedSessions + sessionId

            updatedProgress = current.copy(
                totalGamesCompleted = newTotalGames,
                orderCompletedGames = newOrderGames,
                orderTotalErrors = newTotalErrors,
                orderBestErrors = newBestErrors,
            )
        }
        return checkNotNull(updatedProgress)
    }

    private fun Preferences.toGameProgress(): GameProgress = GameProgress(
        totalGamesCompleted = this[TotalGamesCompletedKey] ?: 0,
        memoryCompletedGames = this[MemoryCompletedGamesKey] ?: 0,
        memoryBestMoves = this[MemoryBestMovesKey]?.takeIf { it > 0 },
        sequenceCompletedGames = this[SequenceCompletedGamesKey] ?: 0,
        sequenceBestLength = this[SequenceBestLengthKey] ?: 0,
        orderCompletedGames = this[OrderCompletedGamesKey] ?: 0,
        orderTotalErrors = this[OrderTotalErrorsKey] ?: 0,
        orderBestErrors = this[OrderBestErrorsKey],
    )

    private companion object {
        val TotalGamesCompletedKey = intPreferencesKey("play_total_games_completed")
        val MemoryCompletedGamesKey = intPreferencesKey("play_memory_completed_games")
        val MemoryBestMovesKey = intPreferencesKey("play_memory_best_moves")
        val SequenceCompletedGamesKey = intPreferencesKey("play_sequence_completed_games")
        val SequenceBestLengthKey = intPreferencesKey("play_sequence_best_length")
        val OrderCompletedGamesKey = intPreferencesKey("play_order_completed_games")
        val OrderTotalErrorsKey = intPreferencesKey("play_order_total_errors")
        val OrderBestErrorsKey = intPreferencesKey("play_order_best_errors")
        val CreditedGameSessionsKey = stringSetPreferencesKey("play_credited_game_sessions")
    }
}

class GameProgressState(
    private val repository: GameProgressRepository,
    private val scope: CoroutineScope,
) {
    val progress: StateFlow<GameProgress> = repository.progress.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = GameProgress(),
    )

    fun recordMemoryGame(sessionId: String, moves: Int) {
        scope.launch {
            repository.recordMemoryGame(sessionId, moves)
        }
    }

    fun recordSequenceGame(
        sessionId: String,
        correctSequences: Int,
        bestLength: Int,
        errors: Int,
    ) {
        scope.launch {
            repository.recordSequenceGame(sessionId, correctSequences, bestLength, errors)
        }
    }

    fun recordOrderGame(sessionId: String, errors: Int) {
        scope.launch {
            repository.recordOrderGame(sessionId, errors)
        }
    }
}
