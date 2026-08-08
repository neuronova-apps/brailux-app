package com.brailuxaprende.ui.navigation

import androidx.annotation.StringRes
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.brailuxaprende.R
import com.brailuxaprende.data.learn.LearningProgress
import com.brailuxaprende.data.practice.PracticeProgress
import com.brailuxaprende.data.seasonal.SeasonalEvent
import com.brailuxaprende.data.settings.AccessibilityPreferences
import com.brailuxaprende.data.settings.AppearancePreference
import com.brailuxaprende.data.settings.TextSizePreference
import com.brailuxaprende.practice.PracticeSessionSummary
import com.brailuxaprende.practice.PracticeMode
import com.brailuxaprende.practice.CustomPracticeConfiguration
import com.brailuxaprende.practice.EngagementProgress
import com.brailuxaprende.practice.EngagementReward
import com.brailuxaprende.practice.PracticeDate
import com.brailuxaprende.practice.SystemPracticeClock
import com.brailuxaprende.learning.LearningLesson
import com.brailuxaprende.learning.LearningPath
import com.brailuxaprende.ui.screens.AboutScreen
import com.brailuxaprende.ui.screens.BrailleLessonScreen
import com.brailuxaprende.ui.screens.BrailleChallengeScreen
import com.brailuxaprende.ui.screens.BrailleExplorerScreen
import com.brailuxaprende.ui.screens.BrailleRecognizerScreen
import com.brailuxaprende.ui.screens.CustomBraillePracticeScreen
import com.brailuxaprende.ui.screens.CustomPracticeConfigurationScreen
import com.brailuxaprende.ui.screens.DailyPracticeScreen
import com.brailuxaprende.ui.screens.HomeScreen
import com.brailuxaprende.ui.screens.LearnScreen
import com.brailuxaprende.ui.screens.LettersAtoJLessonScreen
import com.brailuxaprende.ui.screens.LettersKtoTLessonScreen
import com.brailuxaprende.ui.screens.LettersUtoZAndEnyeLessonScreen
import com.brailuxaprende.ui.screens.VowelsLessonScreen
import com.brailuxaprende.ui.screens.PlaceholderScreen
import com.brailuxaprende.ui.screens.PracticeScreen
import com.brailuxaprende.ui.screens.ProgressScreen
import com.brailuxaprende.ui.screens.SettingsScreen
import com.brailuxaprende.ui.screens.WelcomeScreen

object BrailuxRoutes {
    const val WELCOME = "bienvenida"
    const val HOME = "inicio"
    const val LEARN = "aprende"
    const val PRACTICE = "practica"
    const val PLAY = "juega"
    const val PROGRESS = "mi_progreso"
    const val SETTINGS = "configuracion"
    const val ABOUT = "acerca_de"
    const val SIX_DOTS_LESSON = "leccion_seis_puntos"
    const val VOWELS_LESSON = "leccion_vocales"
    const val LETTERS_A_TO_J_LESSON = "leccion_letras_a_j"
    const val LETTERS_K_TO_T_LESSON = "leccion_letras_k_t"
    const val LETTERS_U_TO_Z_AND_ENYE_LESSON = "leccion_letras_u_z_enye"
    const val BRAILLE_EXPLORER = "practica_explorador_braille"
    const val BRAILLE_RECOGNIZER = "practica_reconocedor_braille"
    const val BRAILLE_CHALLENGE = "practica_desafio_braille"
    const val CUSTOM_PRACTICE_CONFIGURATION = "configuracion_practica_personalizada"
    const val CUSTOM_PRACTICE = "practica_personalizada"
    const val DAILY_PRACTICE = "practica_diaria"
}

private data class BottomDestination(
    val route: String,
    @param:StringRes val label: Int,
    @param:DrawableRes val icon: Int,
)

private val bottomDestinations = listOf(
    BottomDestination(BrailuxRoutes.HOME, R.string.nav_home, R.drawable.ic_home),
    BottomDestination(BrailuxRoutes.LEARN, R.string.nav_learn, R.drawable.ic_learn),
    BottomDestination(BrailuxRoutes.PLAY, R.string.nav_play, R.drawable.ic_play),
    BottomDestination(BrailuxRoutes.PROGRESS, R.string.nav_progress, R.drawable.ic_progress),
)

private val routesWithoutBottomBar = setOf(
    BrailuxRoutes.WELCOME,
    BrailuxRoutes.SIX_DOTS_LESSON,
    BrailuxRoutes.VOWELS_LESSON,
    BrailuxRoutes.LETTERS_A_TO_J_LESSON,
    BrailuxRoutes.LETTERS_K_TO_T_LESSON,
    BrailuxRoutes.LETTERS_U_TO_Z_AND_ENYE_LESSON,
    BrailuxRoutes.BRAILLE_EXPLORER,
    BrailuxRoutes.BRAILLE_RECOGNIZER,
    BrailuxRoutes.BRAILLE_CHALLENGE,
    BrailuxRoutes.CUSTOM_PRACTICE_CONFIGURATION,
    BrailuxRoutes.CUSTOM_PRACTICE,
    BrailuxRoutes.DAILY_PRACTICE,
)

@Composable
fun BrailuxApp(
    preferences: AccessibilityPreferences,
    learningProgress: LearningProgress = LearningProgress(),
    practiceProgress: PracticeProgress = PracticeProgress(),
    engagementProgress: EngagementProgress = EngagementProgress(),
    currentDate: PracticeDate = SystemPracticeClock.today(),
    seasonalEvent: SeasonalEvent? = null,
    onSoundEnabledChange: (Boolean) -> Unit,
    onVibrationEnabledChange: (Boolean) -> Unit,
    onHighContrastEnabledChange: (Boolean) -> Unit,
    onTextSizeChange: (TextSizePreference) -> Unit,
    onAppearanceChange: (AppearancePreference) -> Unit,
    onSeasonalThemesEnabledChange: (Boolean) -> Unit,
    onLearningLessonCompleted: (LearningLesson) -> Unit = {},
    onLevel1SessionCompleted: (PracticeSessionSummary, onRecorded: (Boolean) -> Unit) -> Unit,
    onLevel2SessionCompleted: (PracticeSessionSummary, onRecorded: (Boolean) -> Unit) -> Unit =
        { _, onRecorded -> onRecorded(true) },
    onLevel3SessionCompleted: (PracticeSessionSummary, onRecorded: (Boolean) -> Unit) -> Unit =
        { _, onRecorded -> onRecorded(true) },
    customPracticeConfiguration: CustomPracticeConfiguration = CustomPracticeConfiguration(),
    onCustomPracticeConfigurationUsed: (CustomPracticeConfiguration) -> Unit = {},
    onCustomSessionCompleted: (
        PracticeSessionSummary,
        onRecorded: (Boolean) -> Unit,
    ) -> Unit = { _, onRecorded -> onRecorded(true) },
    onDailySessionCompleted: (
        PracticeSessionSummary,
        onRecorded: (EngagementReward?) -> Unit,
    ) -> Unit = { _, onRecorded -> onRecorded(null) },
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val currentRoute = currentDestination?.route
    val selectedMainRoute = selectedMainDestination(currentRoute)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (currentRoute != null && currentRoute !in routesWithoutBottomBar) {
                BrailuxBottomBar(
                    selectedRoute = selectedMainRoute,
                    onNavigate = { route -> navController.navigateToMainDestination(route) },
                )
            }
        },
    ) { innerPadding ->
        BrailuxNavHost(
            navController = navController,
            preferences = preferences,
            learningProgress = learningProgress,
            practiceProgress = practiceProgress,
            engagementProgress = engagementProgress,
            currentDate = currentDate,
            seasonalEvent = seasonalEvent,
            onSoundEnabledChange = onSoundEnabledChange,
            onVibrationEnabledChange = onVibrationEnabledChange,
            onHighContrastEnabledChange = onHighContrastEnabledChange,
            onTextSizeChange = onTextSizeChange,
            onAppearanceChange = onAppearanceChange,
            onSeasonalThemesEnabledChange = onSeasonalThemesEnabledChange,
            onLearningLessonCompleted = onLearningLessonCompleted,
            onLevel1SessionCompleted = onLevel1SessionCompleted,
            onLevel2SessionCompleted = onLevel2SessionCompleted,
            onLevel3SessionCompleted = onLevel3SessionCompleted,
            customPracticeConfiguration = customPracticeConfiguration,
            onCustomPracticeConfigurationUsed = onCustomPracticeConfigurationUsed,
            onCustomSessionCompleted = onCustomSessionCompleted,
            onDailySessionCompleted = onDailySessionCompleted,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
private fun BrailuxBottomBar(
    selectedRoute: String?,
    onNavigate: (String) -> Unit,
) {
    NavigationBar {
        bottomDestinations.forEach { destination ->
            val selected = selectedRoute == destination.route
            val state = stringResource(
                if (selected) R.string.nav_selected else R.string.nav_not_selected,
            )
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(destination.route) },
                modifier = Modifier.semantics { stateDescription = state },
                icon = {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clearAndSetSemantics { },
                        contentAlignment = Alignment.Center,
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            shape = CircleShape,
                            color = if (selected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            contentColor = if (selected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            border = if (selected) {
                                BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                            } else {
                                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            },
                        ) {
                            Icon(
                                painter = painterResource(destination.icon),
                                contentDescription = null,
                                modifier = Modifier.padding(8.dp),
                            )
                        }
                        if (selected) {
                            Text(
                                text = stringResource(R.string.nav_selected_mark),
                                modifier = Modifier.align(Alignment.TopEnd),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                },
                label = {
                    Text(
                        text = stringResource(destination.label),
                        style = if (selected) {
                            MaterialTheme.typography.labelLarge
                        } else {
                            MaterialTheme.typography.labelMedium
                        },
                    )
                },
                alwaysShowLabel = true,
            )
        }
    }
}

@Composable
private fun BrailuxNavHost(
    navController: NavHostController,
    preferences: AccessibilityPreferences,
    learningProgress: LearningProgress,
    practiceProgress: PracticeProgress,
    engagementProgress: EngagementProgress,
    currentDate: PracticeDate,
    seasonalEvent: SeasonalEvent?,
    onSoundEnabledChange: (Boolean) -> Unit,
    onVibrationEnabledChange: (Boolean) -> Unit,
    onHighContrastEnabledChange: (Boolean) -> Unit,
    onTextSizeChange: (TextSizePreference) -> Unit,
    onAppearanceChange: (AppearancePreference) -> Unit,
    onSeasonalThemesEnabledChange: (Boolean) -> Unit,
    onLearningLessonCompleted: (LearningLesson) -> Unit,
    onLevel1SessionCompleted: (PracticeSessionSummary, onRecorded: (Boolean) -> Unit) -> Unit,
    onLevel2SessionCompleted: (PracticeSessionSummary, onRecorded: (Boolean) -> Unit) -> Unit,
    onLevel3SessionCompleted: (PracticeSessionSummary, onRecorded: (Boolean) -> Unit) -> Unit,
    customPracticeConfiguration: CustomPracticeConfiguration,
    onCustomPracticeConfigurationUsed: (CustomPracticeConfiguration) -> Unit,
    onCustomSessionCompleted: (
        PracticeSessionSummary,
        onRecorded: (Boolean) -> Unit,
    ) -> Unit,
    onDailySessionCompleted: (
        PracticeSessionSummary,
        onRecorded: (EngagementReward?) -> Unit,
    ) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedPracticeModeName by rememberSaveable {
        mutableStateOf(PracticeMode.SignToCharacter.name)
    }
    val selectedPracticeMode = PracticeMode.valueOf(selectedPracticeModeName)
    var activeCustomConfiguration by rememberSaveable(
        stateSaver = androidx.compose.runtime.saveable.Saver<
            CustomPracticeConfiguration,
            List<String>,
        >(
            save = { configuration ->
                listOf(
                    configuration.exerciseCount.name,
                    configuration.mode.name,
                    configuration.hintsEnabled.toString(),
                    configuration.showPointNumbers.toString(),
                )
            },
            restore = { values ->
                CustomPracticeConfiguration(
                    exerciseCount = com.brailuxaprende.practice.CustomExerciseCount.valueOf(values[0]),
                    mode = PracticeMode.valueOf(values[1]),
                    hintsEnabled = values[2].toBoolean(),
                    showPointNumbers = values[3].toBoolean(),
                )
            },
        ),
    ) { mutableStateOf(customPracticeConfiguration) }

    fun goBack() {
        if (!navController.popBackStack()) {
            navController.navigate(BrailuxRoutes.HOME) { launchSingleTop = true }
        }
    }

    fun backToPractice() {
        if (!navController.popBackStack(BrailuxRoutes.PRACTICE, inclusive = false)) {
            navController.navigate(BrailuxRoutes.PRACTICE) { launchSingleTop = true }
        }
    }

    fun backToHome() {
        if (!navController.popBackStack(BrailuxRoutes.HOME, inclusive = false)) {
            navController.navigate(BrailuxRoutes.HOME) { launchSingleTop = true }
        }
    }

    fun backToLearn() {
        if (!navController.popBackStack(BrailuxRoutes.LEARN, inclusive = false)) {
            navController.navigate(BrailuxRoutes.LEARN) { launchSingleTop = true }
        }
    }

    fun openLearningLesson(lesson: LearningLesson) {
        navController.navigate(learningRouteFor(lesson)) { launchSingleTop = true }
    }

    fun openNextLearningLesson(current: LearningLesson) {
        val next = LearningPath.nextLesson(current) ?: return
        navController.navigate(learningRouteFor(next)) {
            popUpTo(BrailuxRoutes.LEARN)
            launchSingleTop = true
        }
    }

    fun openAlphabetPractice() {
        navController.navigate(alphabetPracticeRoute()) {
            popUpTo(BrailuxRoutes.LEARN)
            launchSingleTop = true
        }
    }

    NavHost(
        navController = navController,
        startDestination = BrailuxRoutes.WELCOME,
        modifier = modifier,
    ) {
        composable(BrailuxRoutes.WELCOME) {
            WelcomeScreen(
                onStart = {
                    navController.navigate(BrailuxRoutes.HOME) {
                        popUpTo(BrailuxRoutes.WELCOME) { inclusive = true }
                    }
                },
            )
        }
        composable(BrailuxRoutes.HOME) {
            HomeScreen(
                seasonalEvent = seasonalEvent,
                engagementProgress = engagementProgress,
                currentDate = currentDate,
                onStartDailyPractice = {
                    navController.navigate(BrailuxRoutes.DAILY_PRACTICE)
                },
                onLearn = { navController.navigate(BrailuxRoutes.LEARN) },
                onPractice = { navController.navigate(BrailuxRoutes.PRACTICE) },
                onSettings = { navController.navigate(BrailuxRoutes.SETTINGS) },
                onAbout = { navController.navigate(BrailuxRoutes.ABOUT) },
            )
        }
        composable(BrailuxRoutes.LEARN) {
            LearnScreen(
                progress = learningProgress,
                onOpenLesson = ::openLearningLesson,
                onBack = ::goBack,
            )
        }
        composable(BrailuxRoutes.PRACTICE) {
            PracticeScreen(
                onStartLevel1 = { mode ->
                    selectedPracticeModeName = mode.name
                    navController.navigate(BrailuxRoutes.BRAILLE_EXPLORER)
                },
                onStartLevel2 = { mode ->
                    selectedPracticeModeName = mode.name
                    navController.navigate(BrailuxRoutes.BRAILLE_RECOGNIZER)
                },
                onStartLevel3 = { mode ->
                    selectedPracticeModeName = mode.name
                    navController.navigate(BrailuxRoutes.BRAILLE_CHALLENGE)
                },
                onStartLevel4 = {
                    navController.navigate(BrailuxRoutes.CUSTOM_PRACTICE_CONFIGURATION)
                },
                onBack = ::goBack,
            )
        }
        composable(BrailuxRoutes.PLAY) {
            PlaceholderScreen(
                title = stringResource(R.string.play_title),
                description = stringResource(R.string.play_description),
                onBack = ::goBack,
            )
        }
        composable(BrailuxRoutes.PROGRESS) {
            ProgressScreen(
                progress = practiceProgress,
                engagementProgress = engagementProgress,
                currentDate = currentDate,
                onStartPractice = {
                    navController.navigate(BrailuxRoutes.PRACTICE) { launchSingleTop = true }
                },
                onBack = ::goBack,
            )
        }
        composable(BrailuxRoutes.SETTINGS) {
            SettingsScreen(
                preferences = preferences,
                onSoundEnabledChange = onSoundEnabledChange,
                onVibrationEnabledChange = onVibrationEnabledChange,
                onHighContrastEnabledChange = onHighContrastEnabledChange,
                onTextSizeChange = onTextSizeChange,
                onAppearanceChange = onAppearanceChange,
                onSeasonalThemesEnabledChange = onSeasonalThemesEnabledChange,
                onBack = ::goBack,
            )
        }
        composable(BrailuxRoutes.ABOUT) {
            AboutScreen(onBack = ::goBack)
        }
        composable(BrailuxRoutes.SIX_DOTS_LESSON) {
            BrailleLessonScreen(
                onCompleted = {
                    onLearningLessonCompleted(LearningLesson.SixDots)
                },
                onNextLesson = {
                    openNextLearningLesson(LearningLesson.SixDots)
                },
                onBackToLearn = ::backToLearn,
                onBack = ::goBack,
            )
        }
        composable(BrailuxRoutes.VOWELS_LESSON) {
            VowelsLessonScreen(
                onCompleted = {
                    onLearningLessonCompleted(LearningLesson.Vowels)
                },
                onNextLesson = {
                    openNextLearningLesson(LearningLesson.Vowels)
                },
                onBackToLearn = ::backToLearn,
                onBack = ::goBack,
            )
        }
        composable(BrailuxRoutes.LETTERS_A_TO_J_LESSON) {
            LettersAtoJLessonScreen(
                onCompleted = {
                    onLearningLessonCompleted(LearningLesson.LettersAtoJ)
                },
                onNextLesson = {
                    openNextLearningLesson(LearningLesson.LettersAtoJ)
                },
                onBackToLearn = ::backToLearn,
                onBack = ::goBack,
            )
        }
        composable(BrailuxRoutes.LETTERS_K_TO_T_LESSON) {
            LettersKtoTLessonScreen(
                onCompleted = {
                    onLearningLessonCompleted(LearningLesson.LettersKtoT)
                },
                onNextLesson = {
                    openNextLearningLesson(LearningLesson.LettersKtoT)
                },
                onBackToLearn = ::backToLearn,
                onBack = ::goBack,
            )
        }
        composable(BrailuxRoutes.LETTERS_U_TO_Z_AND_ENYE_LESSON) {
            LettersUtoZAndEnyeLessonScreen(
                onCompleted = {
                    onLearningLessonCompleted(LearningLesson.LettersUtoZAndEnye)
                },
                onPracticeAlphabet = ::openAlphabetPractice,
                onBackToLearn = ::backToLearn,
                onBack = ::goBack,
            )
        }
        composable(BrailuxRoutes.BRAILLE_EXPLORER) {
            BrailleExplorerScreen(
                mode = selectedPracticeMode,
                onSessionCompleted = onLevel1SessionCompleted,
                onBackToPractice = ::backToPractice,
            )
        }
        composable(BrailuxRoutes.BRAILLE_RECOGNIZER) {
            BrailleRecognizerScreen(
                mode = selectedPracticeMode,
                onSessionCompleted = onLevel2SessionCompleted,
                onBackToPractice = ::backToPractice,
            )
        }
        composable(BrailuxRoutes.BRAILLE_CHALLENGE) {
            BrailleChallengeScreen(
                mode = selectedPracticeMode,
                onSessionCompleted = onLevel3SessionCompleted,
                onBackToPractice = ::backToPractice,
            )
        }
        composable(BrailuxRoutes.CUSTOM_PRACTICE_CONFIGURATION) {
            CustomPracticeConfigurationScreen(
                initialConfiguration = customPracticeConfiguration,
                onStartPractice = { configuration ->
                    activeCustomConfiguration = configuration
                    onCustomPracticeConfigurationUsed(configuration)
                    navController.navigate(BrailuxRoutes.CUSTOM_PRACTICE)
                },
                onBack = ::goBack,
            )
        }
        composable(BrailuxRoutes.CUSTOM_PRACTICE) {
            CustomBraillePracticeScreen(
                configuration = activeCustomConfiguration,
                onSessionCompleted = onCustomSessionCompleted,
                onChangeConfiguration = {
                    navController.popBackStack(
                        BrailuxRoutes.CUSTOM_PRACTICE_CONFIGURATION,
                        inclusive = false,
                    )
                },
                onBackToPractice = ::backToPractice,
            )
        }
        composable(BrailuxRoutes.DAILY_PRACTICE) {
            DailyPracticeScreen(
                onSessionCompleted = onDailySessionCompleted,
                onBackToHome = ::backToHome,
            )
        }
    }
}

private fun NavHostController.navigateToMainDestination(route: String) {
    if (currentDestination?.route == route) return

    val preserveDestinationState = shouldPreserveMainDestinationState(route)
    navigate(route) {
        popUpTo(BrailuxRoutes.HOME) {
            saveState = preserveDestinationState
        }
        launchSingleTop = true
        restoreState = preserveDestinationState
    }
}

internal fun selectedMainDestination(currentRoute: String?): String? =
    currentRoute?.takeIf { route -> bottomDestinations.any { it.route == route } }

internal fun bottomDestinationRoutes(): List<String> = bottomDestinations.map { it.route }

internal fun shouldPreserveMainDestinationState(route: String): Boolean =
    route != BrailuxRoutes.HOME

internal fun learningRouteFor(lesson: LearningLesson): String = when (lesson) {
    LearningLesson.SixDots -> BrailuxRoutes.SIX_DOTS_LESSON
    LearningLesson.Vowels -> BrailuxRoutes.VOWELS_LESSON
    LearningLesson.LettersAtoJ -> BrailuxRoutes.LETTERS_A_TO_J_LESSON
    LearningLesson.LettersKtoT -> BrailuxRoutes.LETTERS_K_TO_T_LESSON
    LearningLesson.LettersUtoZAndEnye -> BrailuxRoutes.LETTERS_U_TO_Z_AND_ENYE_LESSON
}

internal fun nextLearningRoute(current: LearningLesson): String? =
    LearningPath.nextLesson(current)?.let(::learningRouteFor)

internal fun learningParentRoute(): String = BrailuxRoutes.LEARN

internal fun alphabetPracticeRoute(): String = BrailuxRoutes.PRACTICE
