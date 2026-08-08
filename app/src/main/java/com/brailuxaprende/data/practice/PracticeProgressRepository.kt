package com.brailuxaprende.data.practice

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

internal const val Level1CompletedSessionsKeyName = "practice_level_1_completed_sessions"
internal const val Level1TotalExercisesKeyName = "practice_level_1_total_exercises"
internal const val Level1FirstAttemptCorrectKeyName = "practice_level_1_first_attempt_correct"
internal const val Level1LastPracticeDateKeyName = "practice_level_1_last_practice_date"
internal const val Level2CompletedSessionsKeyName = "practice_level_2_completed_sessions"
internal const val Level2TotalExercisesKeyName = "practice_level_2_total_exercises"
internal const val Level2FirstAttemptCorrectKeyName = "practice_level_2_first_attempt_correct"
internal const val Level2ErrorsKeyName = "practice_level_2_errors"
internal const val Level2HintsUsedKeyName = "practice_level_2_hints_used"
internal const val Level2LastPracticeDateKeyName = "practice_level_2_last_practice_date"

data class PracticeProgress(
    val level1CompletedSessions: Int = 0,
    val level1TotalExercises: Int = 0,
    val level1FirstAttemptCorrect: Int = 0,
    val level1LastPracticeDate: String? = null,
    val level2CompletedSessions: Int = 0,
    val level2TotalExercises: Int = 0,
    val level2FirstAttemptCorrect: Int = 0,
    val level2Errors: Int = 0,
    val level2HintsUsed: Int = 0,
    val level2LastPracticeDate: String? = null,
) {
    val level1AccuracyPercentage: Int
        get() {
            if (level1TotalExercises <= 0) return 0

            val safeCorrectAnswers = level1FirstAttemptCorrect.coerceIn(0, level1TotalExercises)
            return (safeCorrectAnswers.toLong() * 100 / level1TotalExercises).toInt()
        }
}

class PracticeProgressRepository(
    private val dataStore: DataStore<Preferences>,
) {
    val progress: Flow<PracticeProgress> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            PracticeProgress(
                level1CompletedSessions = preferences[Level1CompletedSessionsKey] ?: 0,
                level1TotalExercises = preferences[Level1TotalExercisesKey] ?: 0,
                level1FirstAttemptCorrect = preferences[Level1FirstAttemptCorrectKey] ?: 0,
                level1LastPracticeDate = preferences[Level1LastPracticeDateKey],
                level2CompletedSessions = preferences[Level2CompletedSessionsKey] ?: 0,
                level2TotalExercises = preferences[Level2TotalExercisesKey] ?: 0,
                level2FirstAttemptCorrect = preferences[Level2FirstAttemptCorrectKey] ?: 0,
                level2Errors = preferences[Level2ErrorsKey] ?: 0,
                level2HintsUsed = preferences[Level2HintsUsedKey] ?: 0,
                level2LastPracticeDate = preferences[Level2LastPracticeDateKey],
            )
        }

    suspend fun recordLevel1Session(
        exercisesCompleted: Int,
        firstAttemptCorrect: Int,
        practiceDate: String,
    ): PracticeProgress {
        require(exercisesCompleted >= 0)
        require(firstAttemptCorrect in 0..exercisesCompleted)
        require(practiceDate.isNotBlank())

        var recordedProgress: PracticeProgress? = null
        dataStore.edit { preferences ->
            val updatedProgress = PracticeProgress(
                level1CompletedSessions =
                    (preferences[Level1CompletedSessionsKey] ?: 0) + 1,
                level1TotalExercises =
                    (preferences[Level1TotalExercisesKey] ?: 0) + exercisesCompleted,
                level1FirstAttemptCorrect =
                    (preferences[Level1FirstAttemptCorrectKey] ?: 0) + firstAttemptCorrect,
                level1LastPracticeDate = practiceDate,
                level2CompletedSessions = preferences[Level2CompletedSessionsKey] ?: 0,
                level2TotalExercises = preferences[Level2TotalExercisesKey] ?: 0,
                level2FirstAttemptCorrect = preferences[Level2FirstAttemptCorrectKey] ?: 0,
                level2Errors = preferences[Level2ErrorsKey] ?: 0,
                level2HintsUsed = preferences[Level2HintsUsedKey] ?: 0,
                level2LastPracticeDate = preferences[Level2LastPracticeDateKey],
            )
            preferences[Level1CompletedSessionsKey] = updatedProgress.level1CompletedSessions
            preferences[Level1TotalExercisesKey] = updatedProgress.level1TotalExercises
            preferences[Level1FirstAttemptCorrectKey] = updatedProgress.level1FirstAttemptCorrect
            preferences[Level1LastPracticeDateKey] = practiceDate
            recordedProgress = updatedProgress
        }
        return checkNotNull(recordedProgress)
    }

    suspend fun recordLevel2Session(
        exercisesCompleted: Int,
        firstAttemptCorrect: Int,
        errors: Int,
        hintsUsed: Int,
        practiceDate: String,
    ): PracticeProgress {
        require(exercisesCompleted >= 0)
        require(firstAttemptCorrect in 0..exercisesCompleted)
        require(errors >= 0)
        require(hintsUsed in 0..3)
        require(practiceDate.isNotBlank())

        var recordedProgress: PracticeProgress? = null
        dataStore.edit { preferences ->
            val updatedProgress = PracticeProgress(
                level1CompletedSessions = preferences[Level1CompletedSessionsKey] ?: 0,
                level1TotalExercises = preferences[Level1TotalExercisesKey] ?: 0,
                level1FirstAttemptCorrect = preferences[Level1FirstAttemptCorrectKey] ?: 0,
                level1LastPracticeDate = preferences[Level1LastPracticeDateKey],
                level2CompletedSessions = (preferences[Level2CompletedSessionsKey] ?: 0) + 1,
                level2TotalExercises =
                    (preferences[Level2TotalExercisesKey] ?: 0) + exercisesCompleted,
                level2FirstAttemptCorrect =
                    (preferences[Level2FirstAttemptCorrectKey] ?: 0) + firstAttemptCorrect,
                level2Errors = (preferences[Level2ErrorsKey] ?: 0) + errors,
                level2HintsUsed = (preferences[Level2HintsUsedKey] ?: 0) + hintsUsed,
                level2LastPracticeDate = practiceDate,
            )
            preferences[Level2CompletedSessionsKey] = updatedProgress.level2CompletedSessions
            preferences[Level2TotalExercisesKey] = updatedProgress.level2TotalExercises
            preferences[Level2FirstAttemptCorrectKey] = updatedProgress.level2FirstAttemptCorrect
            preferences[Level2ErrorsKey] = updatedProgress.level2Errors
            preferences[Level2HintsUsedKey] = updatedProgress.level2HintsUsed
            preferences[Level2LastPracticeDateKey] = practiceDate
            recordedProgress = updatedProgress
        }
        return checkNotNull(recordedProgress)
    }

    private companion object {
        val Level1CompletedSessionsKey = intPreferencesKey(Level1CompletedSessionsKeyName)
        val Level1TotalExercisesKey = intPreferencesKey(Level1TotalExercisesKeyName)
        val Level1FirstAttemptCorrectKey = intPreferencesKey(Level1FirstAttemptCorrectKeyName)
        val Level1LastPracticeDateKey = stringPreferencesKey(Level1LastPracticeDateKeyName)
        val Level2CompletedSessionsKey = intPreferencesKey(Level2CompletedSessionsKeyName)
        val Level2TotalExercisesKey = intPreferencesKey(Level2TotalExercisesKeyName)
        val Level2FirstAttemptCorrectKey = intPreferencesKey(Level2FirstAttemptCorrectKeyName)
        val Level2ErrorsKey = intPreferencesKey(Level2ErrorsKeyName)
        val Level2HintsUsedKey = intPreferencesKey(Level2HintsUsedKeyName)
        val Level2LastPracticeDateKey = stringPreferencesKey(Level2LastPracticeDateKeyName)
    }
}
