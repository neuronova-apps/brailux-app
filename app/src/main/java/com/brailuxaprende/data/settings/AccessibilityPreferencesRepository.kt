package com.brailuxaprende.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
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
internal const val AppearanceKeyName = "appearance"
internal const val SeasonalThemesEnabledKeyName = "seasonal_themes_enabled"
internal const val SelectedBackgroundIdKeyName = "selected_background_id"
internal const val BackgroundRotationModeKeyName = "background_rotation_mode"
internal const val BackgroundLastRotationAtKeyName = "background_last_rotation_at"

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

    suspend fun setAppearance(appearance: AppearancePreference) {
        dataStore.edit { preferences -> preferences[AppearanceKey] = appearance.storedValue }
    }

    suspend fun setSeasonalThemesEnabled(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[SeasonalThemesEnabledKey] = enabled }
    }

    suspend fun setSelectedBackgroundId(backgroundId: String) {
        selectBackgroundAndFix(backgroundId)
    }

    suspend fun selectBackgroundAndFix(backgroundId: String) {
        dataStore.edit { preferences ->
            preferences[SelectedBackgroundIdKey] =
                BrailuxBackgroundCatalog.normalizedId(backgroundId)
            preferences[BackgroundRotationModeKey] = BackgroundRotationMode.Fixed.storedValue
            preferences.remove(BackgroundLastRotationAtKey)
        }
    }

    suspend fun setBackgroundRotationMode(
        mode: BackgroundRotationMode,
        nowMillis: Long = System.currentTimeMillis(),
    ) {
        dataStore.edit { preferences ->
            preferences[BackgroundRotationModeKey] = mode.storedValue
            if (mode == BackgroundRotationMode.EverySixHours) {
                preferences[BackgroundLastRotationAtKey] = nowMillis
            } else {
                preferences.remove(BackgroundLastRotationAtKey)
            }
        }
    }

    suspend fun rotatePremiumBackgroundOnForeground(
        isPremiumUnlocked: Boolean = false,
        nowMillis: Long = System.currentTimeMillis(),
        ownedBackgroundIds: Set<String> = emptySet(),
    ) {
        if (!BrailuxBackgroundRotationPolicy.canRotate(isPremiumUnlocked, ownedBackgroundIds)) return

        dataStore.edit { preferences ->
            val mode = BackgroundRotationMode.fromStoredValue(
                preferences[BackgroundRotationModeKey],
            )
            val lastRotationAt = preferences[BackgroundLastRotationAtKey] ?: 0L
            if (!BrailuxBackgroundRotationPolicy.shouldRotate(
                    mode = mode,
                    lastRotationAtMillis = lastRotationAt,
                    nowMillis = nowMillis,
                )
            ) {
                return@edit
            }

            val currentId = BrailuxBackgroundCatalog.normalizedId(
                preferences[SelectedBackgroundIdKey],
            )
            val nextId = BrailuxBackgroundRotationPolicy.nextPremiumBackgroundId(
                currentId = currentId,
                isPremiumUnlocked = isPremiumUnlocked,
                ownedBackgroundIds = ownedBackgroundIds,
            ) ?: return@edit

            preferences[SelectedBackgroundIdKey] = nextId
            preferences[BackgroundLastRotationAtKey] = nowMillis
        }
    }

    private companion object {
        val SoundEnabledKey = booleanPreferencesKey(SoundEnabledKeyName)
        val VibrationEnabledKey = booleanPreferencesKey(VibrationEnabledKeyName)
        val HighContrastEnabledKey = booleanPreferencesKey(HighContrastEnabledKeyName)
        val TextSizeKey = stringPreferencesKey(TextSizeKeyName)
        val AppearanceKey = stringPreferencesKey(AppearanceKeyName)
        val SeasonalThemesEnabledKey = booleanPreferencesKey(SeasonalThemesEnabledKeyName)
        val SelectedBackgroundIdKey = stringPreferencesKey(SelectedBackgroundIdKeyName)
        val BackgroundRotationModeKey = stringPreferencesKey(BackgroundRotationModeKeyName)
        val BackgroundLastRotationAtKey = longPreferencesKey(BackgroundLastRotationAtKeyName)
    }
}

private fun Preferences.toAccessibilityPreferences(): AccessibilityPreferences =
    AccessibilityPreferences(
        soundEnabled = valueNamed(SoundEnabledKeyName) as? Boolean ?: true,
        vibrationEnabled = valueNamed(VibrationEnabledKeyName) as? Boolean ?: true,
        highContrastEnabled = valueNamed(HighContrastEnabledKeyName) as? Boolean ?: false,
        textSize = TextSizePreference.fromStoredValue(valueNamed(TextSizeKeyName) as? String),
        appearance = AppearancePreference.fromStoredValue(valueNamed(AppearanceKeyName) as? String),
        seasonalThemesEnabled = valueNamed(SeasonalThemesEnabledKeyName) as? Boolean ?: true,
        selectedBackgroundId = BrailuxBackgroundCatalog.normalizedId(
            valueNamed(SelectedBackgroundIdKeyName) as? String,
        ),
        backgroundRotationMode = BackgroundRotationMode.fromStoredValue(
            valueNamed(BackgroundRotationModeKeyName) as? String,
        ),
    )

private fun Preferences.valueNamed(name: String): Any? =
    asMap().entries.firstOrNull { (key, _) -> key.name == name }?.value
