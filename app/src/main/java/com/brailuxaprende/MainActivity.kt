package com.brailuxaprende

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.lifecycleScope
import com.brailuxaprende.data.settings.AccessibilityPreferencesRepository
import com.brailuxaprende.data.settings.AccessibilitySettingsState
import com.brailuxaprende.data.settings.accessibilityPreferencesDataStore
import com.brailuxaprende.ui.navigation.BrailuxApp
import com.brailuxaprende.ui.theme.BrailuxAprendeTheme

class MainActivity : ComponentActivity() {
    private val settingsState by lazy {
        AccessibilitySettingsState(
            repository = AccessibilityPreferencesRepository(
                applicationContext.accessibilityPreferencesDataStore,
            ),
            scope = lifecycleScope,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val preferences by settingsState.preferences.collectAsState()

            BrailuxAprendeTheme(
                highContrast = preferences.highContrastEnabled,
                textSize = preferences.textSize,
            ) {
                BrailuxApp(
                    preferences = preferences,
                    onSoundEnabledChange = settingsState::setSoundEnabled,
                    onVibrationEnabledChange = settingsState::setVibrationEnabled,
                    onHighContrastEnabledChange = settingsState::setHighContrastEnabled,
                    onTextSizeChange = settingsState::setTextSize,
                )
            }
        }
    }
}
