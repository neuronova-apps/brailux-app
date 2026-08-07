package com.brailuxaprende.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.brailuxaprende.R
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
    @param:StringRes val symbol: Int,
)

private val bottomDestinations = listOf(
    BottomDestination(BrailuxRoutes.HOME, R.string.nav_home, R.string.nav_symbol_home),
    BottomDestination(BrailuxRoutes.LEARN, R.string.nav_learn, R.string.nav_symbol_learn),
    BottomDestination(BrailuxRoutes.PLAY, R.string.nav_play, R.string.nav_symbol_play),
    BottomDestination(BrailuxRoutes.PROGRESS, R.string.nav_progress, R.string.nav_symbol_progress),
)

private val routesWithoutBottomBar = setOf(
    BrailuxRoutes.WELCOME,
    BrailuxRoutes.SIX_DOTS_LESSON,
    BrailuxRoutes.LETTER_A_EXERCISE,
)

@Composable
fun BrailuxApp() {
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
            NavigationBarItem(
                selected = currentRoute == destination.route,
                onClick = { onNavigate(destination.route) },
                icon = {
                    Text(
                        text = stringResource(destination.symbol),
                        modifier = Modifier.clearAndSetSemantics { },
                    )
                },
                label = { Text(stringResource(destination.label)) },
                alwaysShowLabel = true,
            )
        }
    }
}

@Composable
private fun BrailuxNavHost(
    navController: NavHostController,
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
            SettingsScreen(onBack = ::goBack)
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
