package com.brailuxaprende

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.lifecycle.lifecycleScope
import com.brailuxaprende.ai.BrailuxAiService
import com.brailuxaprende.data.learn.LearningProgressRepository
import com.brailuxaprende.data.learn.LearningProgressState
import com.brailuxaprende.data.practice.PracticeProgressRepository
import com.brailuxaprende.data.practice.PracticeProgressState
import com.brailuxaprende.data.practice.PracticeSessionRepository
import com.brailuxaprende.data.practice.CustomPracticePreferencesRepository
import com.brailuxaprende.data.practice.CustomPracticePreferencesState
import com.brailuxaprende.data.practice.EngagementProgressRepository
import com.brailuxaprende.data.practice.EngagementProgressState
import com.brailuxaprende.data.seasonal.AnnualDate
import com.brailuxaprende.data.seasonal.SeasonalThemeResolver
import com.brailuxaprende.data.settings.AccessibilityPreferencesRepository
import com.brailuxaprende.data.settings.AccessibilitySettingsState
import com.brailuxaprende.data.settings.BrailuxBackgroundCatalog
import com.brailuxaprende.data.settings.BrailuxPremiumAccess
import com.brailuxaprende.data.settings.accessibilityPreferencesDataStore
import com.brailuxaprende.ui.navigation.BrailuxApp
import com.brailuxaprende.ui.screens.AssistantViewModel
import com.brailuxaprende.ui.screens.AssistantViewModelFactory
import com.brailuxaprende.ui.screens.PracticeSessionViewModel
import com.brailuxaprende.ui.screens.PracticeSessionViewModelFactory
import com.brailuxaprende.ui.theme.BrailuxAprendeTheme
import com.brailuxaprende.practice.PracticeDate
import com.brailuxaprende.practice.PracticeSessionKind
import com.brailuxaprende.practice.SystemPracticeClock
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private val assistantViewModel by viewModels<AssistantViewModel> {
        AssistantViewModelFactory(BrailuxAiService())
    }
    private val practiceSessionViewModel by viewModels<PracticeSessionViewModel> {
        PracticeSessionViewModelFactory(
            owner = this,
            defaultArgs = null,
            repository = PracticeSessionRepository(
                applicationContext.accessibilityPreferencesDataStore,
            ),
        )
    }
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
    private val learningProgressState by lazy {
        LearningProgressState(
            repository = LearningProgressRepository(
                applicationContext.accessibilityPreferencesDataStore,
            ),
            scope = lifecycleScope,
        )
    }
    private val customPracticePreferencesState by lazy {
        CustomPracticePreferencesState(
            repository = CustomPracticePreferencesRepository(
                applicationContext.accessibilityPreferencesDataStore,
            ),
            scope = lifecycleScope,
        )
    }
    private val engagementProgressState by lazy {
        EngagementProgressState(
            repository = EngagementProgressRepository(
                applicationContext.accessibilityPreferencesDataStore,
            ),
            scope = lifecycleScope,
            clock = SystemPracticeClock,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val preferences by settingsState.preferences.collectAsState()
            val practiceProgress by practiceProgressState.progress.collectAsState()
            val learningProgress by learningProgressState.progress.collectAsState()
            val customPracticeConfiguration by
                customPracticePreferencesState.configuration.collectAsState()
            val engagementProgress by engagementProgressState.progress.collectAsState()
            val practiceSessions by practiceSessionViewModel.sessions.collectAsState()
            val assistantUiState by assistantViewModel.uiState.collectAsState()
            val currentPracticeDate = rememberCurrentPracticeDate()
            val currentDate = AnnualDate(
                month = currentPracticeDate.month,
                day = currentPracticeDate.day,
            )
            val seasonalEvent = SeasonalThemeResolver.activeEvent(
                date = currentDate,
                eventsEnabled = preferences.seasonalThemesEnabled,
            )
            val premiumState = BrailuxPremiumAccess.currentState
            val customBackgroundVisible = BrailuxBackgroundCatalog.activeDrawableResource(
                selectedId = preferences.selectedBackgroundId,
                isPremiumUnlocked = premiumState.isPremiumUnlocked,
                highContrastEnabled = preferences.highContrastEnabled,
            ) != null

            BrailuxAprendeTheme(
                appearance = preferences.appearance,
                highContrast = preferences.highContrastEnabled,
                textSize = preferences.textSize,
                seasonalAccent = seasonalEvent?.accent,
                customBackgroundVisible = customBackgroundVisible,
            ) {
                BrailuxApp(
                    preferences = preferences,
                    assistantState = assistantUiState,
                    onAssistantInputChange = assistantViewModel::updateInput,
                    onAssistantSend = assistantViewModel::send,
                    learningProgress = learningProgress,
                    practiceProgress = practiceProgress,
                    engagementProgress = engagementProgress,
                    practiceSessions = practiceSessions,
                    currentDate = currentPracticeDate,
                    customPracticeConfiguration = customPracticeConfiguration,
                    seasonalEvent = seasonalEvent,
                    onSoundEnabledChange = settingsState::setSoundEnabled,
                    onVibrationEnabledChange = settingsState::setVibrationEnabled,
                    onHighContrastEnabledChange = settingsState::setHighContrastEnabled,
                    onTextSizeChange = settingsState::setTextSize,
                    onAppearanceChange = settingsState::setAppearance,
                    onSeasonalThemesEnabledChange = settingsState::setSeasonalThemesEnabled,
                    isPremiumUnlocked = premiumState.isPremiumUnlocked,
                    onBackgroundChange = { backgroundId ->
                        settingsState.requestBackgroundSelection(
                            backgroundId = backgroundId,
                            isPremiumUnlocked = premiumState.isPremiumUnlocked,
                        )
                    },
                    onLearningLessonCompleted = learningProgressState::markCompleted,
                    onLevel1SessionCompleted = { summary, onRecorded ->
                        practiceProgressState.recordLevel1Session(
                            summary = summary,
                            onRecorded = onRecorded,
                        )
                    },
                    onLevel2SessionCompleted = { summary, onRecorded ->
                        practiceProgressState.recordLevel2Session(
                            summary = summary,
                            onRecorded = onRecorded,
                        )
                    },
                    onLevel3SessionCompleted = { summary, onRecorded ->
                        practiceProgressState.recordLevel3Session(
                            summary = summary,
                            onRecorded = onRecorded,
                        )
                    },
                    onCustomPracticeConfigurationUsed = customPracticePreferencesState::save,
                    onCustomSessionCompleted = { summary, onRecorded ->
                        engagementProgressState.recordSession(
                            summary = summary,
                            kind = PracticeSessionKind.Custom,
                            onRecorded = onRecorded,
                        )
                    },
                    onDailySessionCompleted = { summary, onRecorded ->
                        engagementProgressState.recordSession(
                            summary = summary,
                            kind = PracticeSessionKind.Daily,
                            onRecorded = onRecorded,
                        )
                    },
                    onPracticeSessionChanged = practiceSessionViewModel::save,
                    onPracticeSessionReadyForCredit =
                        practiceSessionViewModel::saveBeforeCredit,
                    onPracticeSessionCreditResolved =
                        practiceSessionViewModel::resolveCredit,
                    onPracticeSessionCleared = practiceSessionViewModel::clear,
                )
            }
        }
    }
}

@Composable
private fun rememberCurrentPracticeDate(): PracticeDate {
    val date by produceState(initialValue = SystemPracticeClock.today()) {
        while (true) {
            delay(60_000L)
            value = SystemPracticeClock.today()
        }
    }
    return date
}
