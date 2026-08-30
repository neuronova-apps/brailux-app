package com.brailuxaprende.data.learn

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.brailuxaprende.data.practice.EngagementSchemaVersion
import com.brailuxaprende.data.practice.EngagementSchemaVersionKey
import com.brailuxaprende.data.practice.toEngagementProgress
import com.brailuxaprende.data.practice.writeEngagement
import com.brailuxaprende.learning.LearningLesson
import com.brailuxaprende.practice.PermanentAchievement
import com.brailuxaprende.practice.PracticeDate
import com.brailuxaprende.practice.SystemPracticeClock
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
        .map(Preferences::toLearningProgress)

    suspend fun markCompleted(
        lesson: LearningLesson,
        date: PracticeDate = SystemPracticeClock.today(),
    ) {
        val key = completedLessonKeys[lesson] ?: return
        dataStore.edit { preferences ->
            preferences[key] = true
            val learning = preferences.toLearningProgress()
            if (learning.completedLessons.size >= 5) {
                val current = preferences.toEngagementProgress()
                val isMissingDate = PermanentAchievement.FullAlphabet !in current.achievementUnlockDates
                val isSchemaUnversioned = (preferences[EngagementSchemaVersionKey] ?: 0) < EngagementSchemaVersion
                if (isMissingDate || isSchemaUnversioned) {
                    val updatedAchievements = current.unlockedAchievements + PermanentAchievement.FullAlphabet
                    val updatedDates = current.achievementUnlockDates + (PermanentAchievement.FullAlphabet to date)
                    val updatedProgress = current.copy(
                        unlockedAchievements = updatedAchievements,
                        achievementUnlockDates = updatedDates,
                    )
                    preferences.writeEngagement(updatedProgress)
                }
            }
        }
    }

    companion object {
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

fun Preferences.toLearningProgress(): LearningProgress =
    LearningProgress(
        completedLessons = LearningProgressRepository.completedLessonKeys.mapNotNullTo(mutableSetOf()) {
            (lesson, key) -> lesson.takeIf { this[key] == true }
        },
    )

