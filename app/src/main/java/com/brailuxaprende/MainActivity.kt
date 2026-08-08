package com.brailuxaprende

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.lifecycle.lifecycleScope
import com.brailuxaprende.data.practice.PracticeProgress
import com.brailuxaprende.data.practice.PracticeProgressRepository
import com.brailuxaprende.data.practice.PracticeProgressState
import com.brailuxaprende.data.seasonal.AnnualDate
import com.brailuxaprende.data.seasonal.SeasonalThemeResolver
import com.brailuxaprende.data.settings.AccessibilityPreferencesRepository
import com.brailuxaprende.data.settings.AccessibilitySettingsState
import com.brailuxaprende.data.settings.accessibilityPreferencesDataStore
import com.brailuxaprende.ui.navigation.BrailuxApp
import com.brailuxaprende.ui.theme.BrailuxAprendeTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private val settingsState by lazy {
        AccessibilitySettingsState(
            repository = AccessibilityPreferencesRepository(
                applicationContext.accessibilityPreferencesDataStore,
            ),
            scope = lifecycleScope,
        )
    }
    private val practiceProgressState by lazy {
        PracticeProgressState(
            repository = PracticeProgressRepository(
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
            val practiceProgress by practiceProgressState.progress.collectAsState(
                initial = PracticeProgress(),
            )
            val currentDate = rememberCurrentAnnualDate()
            val seasonalEvent = SeasonalThemeResolver.activeEvent(
                date = currentDate,
                eventsEnabled = preferences.seasonalThemesEnabled,
            )

            BrailuxAprendeTheme(
                appearance = preferences.appearance,
                highContrast = preferences.highContrastEnabled,
                textSize = preferences.textSize,
                seasonalAccent = seasonalEvent?.accent,
            ) {
                BrailuxApp(
                    preferences = preferences,
                    practiceProgress = practiceProgress,
                    seasonalEvent = seasonalEvent,
                    onSoundEnabledChange = settingsState::setSoundEnabled,
                    onVibrationEnabledChange = settingsState::setVibrationEnabled,
                    onHighContrastEnabledChange = settingsState::setHighContrastEnabled,
                    onTextSizeChange = settingsState::setTextSize,
                    onAppearanceChange = settingsState::setAppearance,
                    onSeasonalThemesEnabledChange = settingsState::setSeasonalThemesEnabled,
                    onLevel1SessionCompleted = practiceProgressState::recordLevel1Session,
                )
            }
        }
    }
}

@Composable
private fun rememberCurrentAnnualDate(): AnnualDate {
    val date by produceState(initialValue = AnnualDate.today()) {
        while (true) {
            delay(60_000L)
            value = AnnualDate.today()
        }
    }
    return date
}
