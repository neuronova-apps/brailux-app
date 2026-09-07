package com.neuronovaapps.brailux

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
import com.neuronovaapps.brailux.ai.BrailuxAiService
import com.neuronovaapps.brailux.data.learn.LearningProgressRepository
import com.neuronovaapps.brailux.data.learn.LearningProgressState
import com.neuronovaapps.brailux.data.play.GameProgressRepository
import com.neuronovaapps.brailux.data.play.GameProgressState
import com.neuronovaapps.brailux.data.practice.PracticeProgressRepository
import com.neuronovaapps.brailux.data.practice.PracticeProgressState
import com.neuronovaapps.brailux.data.practice.PracticeSessionRepository
import com.neuronovaapps.brailux.data.practice.CustomPracticePreferencesRepository
import com.neuronovaapps.brailux.data.practice.CustomPracticePreferencesState
import com.neuronovaapps.brailux.data.practice.EngagementProgressRepository
import com.neuronovaapps.brailux.data.practice.EngagementProgressState
import com.neuronovaapps.brailux.data.seasonal.AnnualDate
import com.neuronovaapps.brailux.data.seasonal.SeasonalDebugOverride
import com.neuronovaapps.brailux.data.seasonal.SeasonalTheme
import com.neuronovaapps.brailux.data.seasonal.SeasonalThemeDetector
import com.neuronovaapps.brailux.data.seasonal.SeasonalThemeResolver
import com.neuronovaapps.brailux.data.settings.AccessibilityPreferencesRepository
import com.neuronovaapps.brailux.data.settings.AccessibilitySettingsState
import com.neuronovaapps.brailux.data.settings.BrailuxBackgroundCatalog
import com.neuronovaapps.brailux.data.settings.BrailuxBackgroundRotationLifecyclePolicy
import com.neuronovaapps.brailux.data.settings.BrailuxPremiumAccess
import com.neuronovaapps.brailux.data.settings.accessibilityPreferencesDataStore
import com.neuronovaapps.brailux.ui.navigation.BrailuxApp
import com.neuronovaapps.brailux.ui.screens.AssistantUiState
import com.neuronovaapps.brailux.ui.screens.AssistantViewModel
import com.neuronovaapps.brailux.ui.screens.AssistantViewModelFactory
import com.neuronovaapps.brailux.ui.screens.PracticeSessionViewModel
import com.neuronovaapps.brailux.ui.screens.PracticeSessionViewModelFactory
import com.neuronovaapps.brailux.ui.theme.BrailuxAprendeTheme
import com.neuronovaapps.brailux.ui.theme.BrailuxThemeCatalog
import com.neuronovaapps.brailux.data.billing.BrailuxBillingCoordinator
import com.neuronovaapps.brailux.data.billing.BrailuxPremiumEntitlementRepository
import com.neuronovaapps.brailux.data.billing.GooglePlayBillingRepository
import com.neuronovaapps.brailux.practice.PracticeDate
import com.neuronovaapps.brailux.practice.PracticeSessionKind
import com.neuronovaapps.brailux.practice.SystemPracticeClock
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    private val premiumEntitlementRepository by lazy {
        BrailuxPremiumEntitlementRepository(
            dataStore = applicationContext.accessibilityPreferencesDataStore,
        )
    }
    private val playBillingRepository by lazy {
        GooglePlayBillingRepository(
            context = applicationContext,
            entitlementRepository = premiumEntitlementRepository,
            coroutineScope = lifecycleScope,
        )
    }
    private val billingCoordinator by lazy {
        BrailuxBillingCoordinator(
            billingRepository = playBillingRepository,
            entitlementRepository = premiumEntitlementRepository,
            coroutineScope = lifecycleScope,
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
    private val gameProgressState by lazy {
        GameProgressState(
            repository = GameProgressRepository(
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
        lifecycleScope.launch {
            billingCoordinator.initialize()
        }
        setContent {
            val preferences by settingsState.preferences.collectAsState()
            val practiceProgress by practiceProgressState.progress.collectAsState()
            val learningProgress by learningProgressState.progress.collectAsState()
            val gameProgress by gameProgressState.progress.collectAsState()
            val customPracticeConfiguration by
                customPracticePreferencesState.configuration.collectAsState()
            val engagementProgress by engagementProgressState.progress.collectAsState()
            val practiceSessions by practiceSessionViewModel.sessions.collectAsState()
            val assistantUiState = if (BrailuxFeatures.ASSISTANT_ENABLED) {
                assistantViewModel.uiState.collectAsState().value
            } else {
                AssistantUiState()
            }
            val currentPracticeDate = rememberCurrentPracticeDate()
            val realDate = AnnualDate(
                month = currentPracticeDate.month,
                day = currentPracticeDate.day,
            )
            val currentDate = SeasonalDebugOverride.effectiveDate(realDate)
            val seasonalEvent = SeasonalThemeResolver.activeEvent(
                date = currentDate,
                eventsEnabled = preferences.seasonalThemesEnabled,
            )
            // Unified seasonal theme for background + decorations.
            // Uses the same seasonalThemesEnabled preference as the banner system.
            val seasonalTheme = if (preferences.seasonalThemesEnabled) {
                SeasonalThemeDetector.resolve(currentDate)
            } else {
                SeasonalTheme.NONE
            }
            val premiumState by BrailuxPremiumAccess.state.collectAsState()
            val billingUiState by billingCoordinator.uiState.collectAsState()
            val seasonalThemeActive = seasonalTheme != SeasonalTheme.NONE && preferences.seasonalThemesEnabled
            val themeDefinition = BrailuxThemeCatalog.resolveTheme(
                selectedId = preferences.selectedBackgroundId,
                isPremiumUnlocked = premiumState.isPremiumUnlocked,
                ownedBackgroundIds = premiumState.ownedBackgroundIds,
                highContrastEnabled = preferences.highContrastEnabled,
                seasonalThemeActive = seasonalThemeActive,
            )
            val customBackgroundVisible = themeDefinition.backgroundRes != null

            BrailuxAprendeTheme(
                appearance = preferences.appearance,
                highContrast = preferences.highContrastEnabled,
                textSize = preferences.textSize,
                seasonalAccent = seasonalEvent?.accent,
                customBackgroundVisible = customBackgroundVisible,
                themeDefinition = themeDefinition,
            ) {
                BrailuxApp(
                    preferences = preferences,
                    seasonalTheme = seasonalTheme,
                    assistantState = assistantUiState,
                    onAssistantInputChange = if (BrailuxFeatures.ASSISTANT_ENABLED) {
                        assistantViewModel::updateInput
                    } else {
                        {}
                    },
                    onAssistantSend = if (BrailuxFeatures.ASSISTANT_ENABLED) {
                        assistantViewModel::send
                    } else {
                        {}
                    },
                    learningProgress = learningProgress,
                    practiceProgress = practiceProgress,
                    engagementProgress = engagementProgress,
                    gameProgress = gameProgress,
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
                    ownedBackgroundIds = premiumState.ownedBackgroundIds,
                    billingUiState = billingUiState,
                    onBuyProduct = { productId, offerToken ->
                        lifecycleScope.launch {
                            billingCoordinator.launchPurchase(this@MainActivity, productId, offerToken)
                        }
                    },
                    onRestorePurchases = {
                        lifecycleScope.launch {
                            billingCoordinator.restorePurchases()
                        }
                    },
                    restoreEvents = billingCoordinator.restoreEvents,
                    onBackgroundChange = { backgroundId ->
                        settingsState.requestBackgroundSelection(
                            backgroundId = backgroundId,
                            isPremiumUnlocked = premiumState.isPremiumUnlocked,
                            ownedBackgroundIds = premiumState.ownedBackgroundIds,
                        )
                    },
                    onLearningLessonCompleted = learningProgressState::markCompleted,
                    onRecordMemoryGame = gameProgressState::recordMemoryGame,
                    onRecordSequenceGame = gameProgressState::recordSequenceGame,
                    onRecordOrderGame = gameProgressState::recordOrderGame,
                    onLevel1SessionCompleted = { summary, onRecorded ->
                        practiceProgressState.recordLevel1Session(
                            summary = summary,
                            learningProgress = learningProgress,
                            onRecorded = onRecorded,
                        )
                    },
                    onLevel2SessionCompleted = { summary, onRecorded ->
                        practiceProgressState.recordLevel2Session(
                            summary = summary,
                            learningProgress = learningProgress,
                            onRecorded = onRecorded,
                        )
                    },
                    onLevel3SessionCompleted = { summary, onRecorded ->
                        practiceProgressState.recordLevel3Session(
                            summary = summary,
                            learningProgress = learningProgress,
                            onRecorded = onRecorded,
                        )
                    },
                    onCustomPracticeConfigurationUsed = customPracticePreferencesState::save,
                    onCustomSessionCompleted = { summary, onRecorded ->
                        practiceProgressState.recordCustomSession(
                            summary = summary,
                            customConfiguration = customPracticeConfiguration,
                            learningProgress = learningProgress,
                            onRecorded = onRecorded,
                        )
                    },
                    onDailySessionCompleted = { summary, onRecorded ->
                        practiceProgressState.recordDailySession(
                            summary = summary,
                            learningProgress = learningProgress,
                            onRecorded = onRecorded,
                        )
                    },
                    onDailyChallengeSessionCompleted = { summary, onRecorded ->
                        practiceProgressState.recordDailyChallengeSession(
                            summary = summary,
                            learningProgress = learningProgress,
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

    override fun onStart() {
        super.onStart()
        if (BrailuxBackgroundRotationLifecyclePolicy.shouldSkipRotationOnStart()) {
            return
        }
        val premiumState = BrailuxPremiumAccess.currentState
        settingsState.onAppForegrounded(
            isPremiumUnlocked = premiumState.isPremiumUnlocked,
            ownedBackgroundIds = premiumState.ownedBackgroundIds,
        )
    }

    override fun onStop() {
        BrailuxBackgroundRotationLifecyclePolicy.handleStop(isChangingConfigurations)
        super.onStop()
    }

    override fun onDestroy() {
        billingCoordinator.destroy()
        super.onDestroy()
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
