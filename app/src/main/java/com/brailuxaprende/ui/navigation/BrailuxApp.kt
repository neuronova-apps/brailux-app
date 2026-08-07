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
import com.brailuxaprende.data.settings.AccessibilityPreferences
import com.brailuxaprende.data.settings.TextSizePreference
import com.brailuxaprende.ui.screens.AboutScreen
import com.brailuxaprende.ui.screens.BrailleLessonScreen
import com.brailuxaprende.ui.screens.HomeScreen
import com.brailuxaprende.ui.screens.LearnScreen
import com.brailuxaprende.ui.screens.LetterAExerciseScreen
import com.brailuxaprende.ui.screens.PlaceholderScreen
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
    const val LETTER_A_EXERCISE = "ejercicio_letra_a"
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
    BrailuxRoutes.LETTER_A_EXERCISE,
)

@Composable
fun BrailuxApp(
    preferences: AccessibilityPreferences,
    onSoundEnabledChange: (Boolean) -> Unit,
    onVibrationEnabledChange: (Boolean) -> Unit,
    onHighContrastEnabledChange: (Boolean) -> Unit,
    onTextSizeChange: (TextSizePreference) -> Unit,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (currentRoute != null && currentRoute !in routesWithoutBottomBar) {
                BrailuxBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { route -> navController.navigateToMainDestination(route) },
                )
            }
        },
    ) { innerPadding ->
        BrailuxNavHost(
            navController = navController,
            preferences = preferences,
            onSoundEnabledChange = onSoundEnabledChange,
            onVibrationEnabledChange = onVibrationEnabledChange,
            onHighContrastEnabledChange = onHighContrastEnabledChange,
            onTextSizeChange = onTextSizeChange,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
private fun BrailuxBottomBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
) {
    NavigationBar {
        bottomDestinations.forEach { destination ->
            val selected = currentRoute == destination.route
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
    onSoundEnabledChange: (Boolean) -> Unit,
    onVibrationEnabledChange: (Boolean) -> Unit,
    onHighContrastEnabledChange: (Boolean) -> Unit,
    onTextSizeChange: (TextSizePreference) -> Unit,
    modifier: Modifier = Modifier,
) {
    fun goBack() {
        if (!navController.popBackStack()) {
            navController.navigate(BrailuxRoutes.HOME) { launchSingleTop = true }
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
                onLearn = { navController.navigate(BrailuxRoutes.LEARN) },
                onPractice = { navController.navigate(BrailuxRoutes.PRACTICE) },
                onPlay = { navController.navigate(BrailuxRoutes.PLAY) },
                onProgress = { navController.navigate(BrailuxRoutes.PROGRESS) },
                onSettings = { navController.navigate(BrailuxRoutes.SETTINGS) },
                onAbout = { navController.navigate(BrailuxRoutes.ABOUT) },
                onStartLesson = { navController.navigate(BrailuxRoutes.SIX_DOTS_LESSON) },
            )
        }
        composable(BrailuxRoutes.LEARN) {
            LearnScreen(
                onStartLesson = { navController.navigate(BrailuxRoutes.SIX_DOTS_LESSON) },
                onBack = ::goBack,
            )
        }
        composable(BrailuxRoutes.PRACTICE) {
            PlaceholderScreen(
                title = stringResource(R.string.practice_title),
                description = stringResource(R.string.practice_description),
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
            PlaceholderScreen(
                title = stringResource(R.string.progress_title),
                description = stringResource(R.string.progress_description),
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
                onBack = ::goBack,
            )
        }
        composable(BrailuxRoutes.ABOUT) {
            AboutScreen(onBack = ::goBack)
        }
        composable(BrailuxRoutes.SIX_DOTS_LESSON) {
            BrailleLessonScreen(
                onPracticeLetterA = { navController.navigate(BrailuxRoutes.LETTER_A_EXERCISE) },
                onBack = ::goBack,
            )
        }
        composable(BrailuxRoutes.LETTER_A_EXERCISE) {
            LetterAExerciseScreen(onBack = ::goBack)
        }
    }
}

private fun NavHostController.navigateToMainDestination(route: String) {
    navigate(route) {
        popUpTo(BrailuxRoutes.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
