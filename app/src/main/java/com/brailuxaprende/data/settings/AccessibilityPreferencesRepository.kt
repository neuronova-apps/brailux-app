package com.brailuxaprende.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private const val DataStoreName = "accessibility_settings"
internal const val SoundEnabledKeyName = "sound_enabled"
internal const val VibrationEnabledKeyName = "vibration_enabled"
internal const val HighContrastEnabledKeyName = "high_contrast_enabled"
internal const val TextSizeKeyName = "text_size"

val Context.accessibilityPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = DataStoreName,
)

class AccessibilityPreferencesRepository(
    private val dataStore: DataStore<Preferences>,
) {
    val preferences: Flow<AccessibilityPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map(Preferences::toAccessibilityPreferences)

    suspend fun setSoundEnabled(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[SoundEnabledKey] = enabled }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[VibrationEnabledKey] = enabled }
    }

    suspend fun setHighContrastEnabled(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[HighContrastEnabledKey] = enabled }
    }

    suspend fun setTextSize(textSize: TextSizePreference) {
        dataStore.edit { preferences -> preferences[TextSizeKey] = textSize.storedValue }
    }

    private companion object {
        val SoundEnabledKey = booleanPreferencesKey(SoundEnabledKeyName)
        val VibrationEnabledKey = booleanPreferencesKey(VibrationEnabledKeyName)
        val HighContrastEnabledKey = booleanPreferencesKey(HighContrastEnabledKeyName)
        val TextSizeKey = stringPreferencesKey(TextSizeKeyName)
    }
}

private fun Preferences.toAccessibilityPreferences(): AccessibilityPreferences =
    AccessibilityPreferences(
        soundEnabled = valueNamed(SoundEnabledKeyName) as? Boolean ?: true,
        vibrationEnabled = valueNamed(VibrationEnabledKeyName) as? Boolean ?: true,
        highContrastEnabled = valueNamed(HighContrastEnabledKeyName) as? Boolean ?: false,
        textSize = TextSizePreference.fromStoredValue(valueNamed(TextSizeKeyName) as? String),
    )

private fun Preferences.valueNamed(name: String): Any? =
    asMap().entries.firstOrNull { (key, _) -> key.name == name }?.value
