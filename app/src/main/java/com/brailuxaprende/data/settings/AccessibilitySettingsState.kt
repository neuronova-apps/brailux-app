package com.brailuxaprende.data.settings

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AccessibilitySettingsState(
    private val repository: AccessibilityPreferencesRepository,
    private val scope: CoroutineScope,
) {
    val preferences: StateFlow<AccessibilityPreferences> = repository.preferences.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = AccessibilityPreferences(),
    )

    fun setSoundEnabled(enabled: Boolean) {
        scope.launch { repository.setSoundEnabled(enabled) }
    }

    fun setVibrationEnabled(enabled: Boolean) {
        scope.launch { repository.setVibrationEnabled(enabled) }
    }

    fun setHighContrastEnabled(enabled: Boolean) {
        scope.launch { repository.setHighContrastEnabled(enabled) }
    }

    fun setTextSize(textSize: TextSizePreference) {
        scope.launch { repository.setTextSize(textSize) }
    }

    fun setAppearance(appearance: AppearancePreference) {
        scope.launch { repository.setAppearance(appearance) }
    }

    fun setSeasonalThemesEnabled(enabled: Boolean) {
        scope.launch { repository.setSeasonalThemesEnabled(enabled) }
    }
}
