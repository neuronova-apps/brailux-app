package com.brailuxaprende.data.learn

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.brailuxaprende.learning.LearningLesson
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

data class LearningProgress(
    val completedLessons: Set<LearningLesson> = emptySet(),
) {
    fun isCompleted(lesson: LearningLesson): Boolean = lesson in completedLessons
}

class LearningProgressRepository(
    private val dataStore: DataStore<Preferences>,
) {
    val progress: Flow<LearningProgress> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            LearningProgress(
                completedLessons = completedLessonKeys.mapNotNullTo(mutableSetOf()) {
                    (lesson, key) -> lesson.takeIf { preferences[key] == true }
                },
            )
        }

    suspend fun markCompleted(lesson: LearningLesson) {
        val key = completedLessonKeys[lesson] ?: return
        dataStore.edit { preferences -> preferences[key] = true }
    }

    private companion object {
        val completedLessonKeys = mapOf(
            LearningLesson.SixDots to booleanPreferencesKey("learning_lesson_1_completed"),
            LearningLesson.Vowels to booleanPreferencesKey("learning_lesson_2_completed"),
            LearningLesson.LettersAtoJ to booleanPreferencesKey("learning_lesson_3_completed"),
            LearningLesson.LettersKtoT to booleanPreferencesKey("learning_lesson_4_completed"),
            LearningLesson.LettersUtoZAndEnye to
                booleanPreferencesKey("learning_lesson_5_completed"),
        )
    }
}
