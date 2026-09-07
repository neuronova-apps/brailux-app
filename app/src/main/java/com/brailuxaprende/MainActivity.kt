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
import com.brailuxaprende.data.play.GameProgressRepository
import com.brailuxaprende.data.play.GameProgressState
import com.brailuxaprende.data.practice.PracticeProgressRepository
import com.brailuxaprende.data.practice.PracticeProgressState
import com.brailuxaprende.data.practice.PracticeSessionRepository
import com.brailuxaprende.data.practice.CustomPracticePreferencesRepository
import com.brailuxaprende.data.practice.CustomPracticePreferencesState
import com.brailuxaprende.data.practice.EngagementProgressRepository
import com.brailuxaprende.data.practice.EngagementProgressState
import com.brailuxaprende.data.seasonal.AnnualDate
import com.brailuxaprende.data.seasonal.SeasonalDebugOverride
import com.brailuxaprende.data.seasonal.SeasonalTheme
import com.brailuxaprende.data.seasonal.SeasonalThemeDetector
import com.brailuxaprende.data.seasonal.SeasonalThemeResolver
import com.brailuxaprende.data.settings.AccessibilityPreferencesRepository
import com.brailuxaprende.data.settings.AccessibilitySettingsState
import com.brailuxaprende.data.settings.BrailuxBackgroundCatalog
import com.brailuxaprende.data.settings.BrailuxBackgroundRotationLifecyclePolicy
import com.brailuxaprende.data.settings.BrailuxPremiumAccess
import com.brailuxaprende.data.settings.accessibilityPreferencesDataStore
import com.brailuxaprende.ui.navigation.BrailuxApp
import com.brailuxaprende.ui.screens.AssistantUiState
import com.brailuxaprende.ui.screens.AssistantViewModel
import com.brailuxaprende.ui.screens.AssistantViewModelFactory
import com.brailuxaprende.ui.screens.PracticeSessionViewModel
import com.brailuxaprende.ui.screens.PracticeSessionViewModelFactory
import com.brailuxaprende.ui.theme.BrailuxAprendeTheme
import com.brailuxaprende.ui.theme.BrailuxThemeCatalog
import com.brailuxaprende.data.billing.BrailuxBillingCoordinator
import com.brailuxaprende.data.billing.BrailuxPremiumEntitlementRepository
import com.brailuxaprende.data.billing.GooglePlayBillingRepository
import com.brailuxaprende.practice.PracticeDate
import com.brailuxaprende.practice.PracticeSessionKind
import com.brailuxaprende.practice.SystemPracticeClock
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
