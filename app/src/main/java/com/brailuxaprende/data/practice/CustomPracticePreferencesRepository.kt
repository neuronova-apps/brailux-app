package com.brailuxaprende.data.practice

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.brailuxaprende.practice.CustomExerciseCount
import com.brailuxaprende.practice.CustomPracticeConfiguration
import com.brailuxaprende.practice.PracticeContentGroup
import com.brailuxaprende.practice.PracticeMode
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

internal const val CustomContentGroupsKeyName = "custom_practice_content_groups"
internal const val CustomExerciseCountKeyName = "custom_practice_exercise_count"
internal const val CustomModeKeyName = "custom_practice_mode"
internal const val CustomHintsEnabledKeyName = "custom_practice_hints_enabled"
internal const val CustomPointNumbersVisibleKeyName = "custom_practice_point_numbers_visible"

class CustomPracticePreferencesRepository(
    private val dataStore: DataStore<Preferences>,
) {
    val configuration: Flow<CustomPracticeConfiguration> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map(Preferences::toCustomPracticeConfiguration)

    suspend fun save(configuration: CustomPracticeConfiguration) {
        dataStore.edit { preferences ->
            preferences[ContentGroupsKey] = configuration.additionalContentGroups
                .joinToString(",") { it.name }
            preferences[ExerciseCountKey] = configuration.exerciseCount.value
            preferences[ModeKey] = configuration.mode.name
            preferences[HintsEnabledKey] = configuration.hintsEnabled
            preferences[PointNumbersVisibleKey] = configuration.showPointNumbers
        }
    }

    private companion object {
        val ContentGroupsKey = stringPreferencesKey(CustomContentGroupsKeyName)
        val ExerciseCountKey = intPreferencesKey(CustomExerciseCountKeyName)
        val ModeKey = stringPreferencesKey(CustomModeKeyName)
        val HintsEnabledKey = booleanPreferencesKey(CustomHintsEnabledKeyName)
        val PointNumbersVisibleKey = booleanPreferencesKey(CustomPointNumbersVisibleKeyName)
    }
}

private fun Preferences.toCustomPracticeConfiguration(): CustomPracticeConfiguration {
    val additionalGroups = (valueNamed(CustomContentGroupsKeyName) as? String)
        .orEmpty()
        .split(',')
        .mapNotNull { storedName ->
            PracticeContentGroup.entries.firstOrNull { it.name == storedName }
        }
        .filter { it != PracticeContentGroup.SpanishAlphabet && it.isAvailable }
        .toSet()
    val modeName = valueNamed(CustomModeKeyName) as? String
    return CustomPracticeConfiguration(
        additionalContentGroups = additionalGroups,
        exerciseCount = CustomExerciseCount.fromValue(
            valueNamed(CustomExerciseCountKeyName) as? Int ?: CustomExerciseCount.Ten.value,
        ),
        mode = PracticeMode.entries.firstOrNull { it.name == modeName }
            ?: PracticeMode.SignToCharacter,
        hintsEnabled = valueNamed(CustomHintsEnabledKeyName) as? Boolean ?: true,
        showPointNumbers = valueNamed(CustomPointNumbersVisibleKeyName) as? Boolean ?: true,
    )
}

private fun Preferences.valueNamed(name: String): Any? =
    asMap().entries.firstOrNull { (key, _) -> key.name == name }?.value
