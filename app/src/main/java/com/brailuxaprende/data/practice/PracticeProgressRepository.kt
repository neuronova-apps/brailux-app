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

data class PracticeProgress(
    val level1CompletedSessions: Int = 0,
    val level1TotalExercises: Int = 0,
    val level1FirstAttemptCorrect: Int = 0,
    val level1LastPracticeDate: String? = null,
)

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
            )
        }

    suspend fun recordLevel1Session(
        exercisesCompleted: Int,
        firstAttemptCorrect: Int,
        practiceDate: String,
    ) {
        require(exercisesCompleted >= 0)
        require(firstAttemptCorrect in 0..exercisesCompleted)
        require(practiceDate.isNotBlank())

        dataStore.edit { preferences ->
            preferences[Level1CompletedSessionsKey] =
                (preferences[Level1CompletedSessionsKey] ?: 0) + 1
            preferences[Level1TotalExercisesKey] =
                (preferences[Level1TotalExercisesKey] ?: 0) + exercisesCompleted
            preferences[Level1FirstAttemptCorrectKey] =
                (preferences[Level1FirstAttemptCorrectKey] ?: 0) + firstAttemptCorrect
            preferences[Level1LastPracticeDateKey] = practiceDate
        }
    }

    private companion object {
        val Level1CompletedSessionsKey = intPreferencesKey(Level1CompletedSessionsKeyName)
        val Level1TotalExercisesKey = intPreferencesKey(Level1TotalExercisesKeyName)
        val Level1FirstAttemptCorrectKey = intPreferencesKey(Level1FirstAttemptCorrectKeyName)
        val Level1LastPracticeDateKey = stringPreferencesKey(Level1LastPracticeDateKeyName)
    }
}
