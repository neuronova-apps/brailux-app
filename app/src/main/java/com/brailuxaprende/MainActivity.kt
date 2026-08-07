package com.brailuxaprende

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.brailuxaprende.ui.screens.BrailleLessonScreen
import com.brailuxaprende.ui.screens.HomeScreen
import com.brailuxaprende.ui.screens.LetterAExerciseScreen
import com.brailuxaprende.ui.theme.BrailuxAprendeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BrailuxAprendeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    BrailuxLearningFlow(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

private enum class BrailuxScreen {
    Home,
    Lesson,
    LetterAExercise,
}

@Composable
private fun BrailuxLearningFlow(modifier: Modifier = Modifier) {
    var currentScreen by remember { mutableStateOf(BrailuxScreen.Home) }

    when (currentScreen) {
        BrailuxScreen.Home -> HomeScreen(
            onStart = { currentScreen = BrailuxScreen.Lesson },
            modifier = modifier,
        )

        BrailuxScreen.Lesson -> BrailleLessonScreen(
            onPracticeLetterA = { currentScreen = BrailuxScreen.LetterAExercise },
            onBack = { currentScreen = BrailuxScreen.Home },
            modifier = modifier,
        )

        BrailuxScreen.LetterAExercise -> LetterAExerciseScreen(
            onBack = { currentScreen = BrailuxScreen.Lesson },
            modifier = modifier,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BrailuxLearningFlowPreview() {
    BrailuxAprendeTheme {
        BrailuxLearningFlow()
    }
}
