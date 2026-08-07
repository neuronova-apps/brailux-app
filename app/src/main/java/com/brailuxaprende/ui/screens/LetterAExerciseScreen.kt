package com.brailuxaprende.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.brailuxaprende.R
import com.brailuxaprende.braille.BrailleCell
import com.brailuxaprende.braille.BrailleRepository
import com.brailuxaprende.ui.components.BrailleCellView

private enum class ExerciseFeedback {
    Correct,
    Incorrect,
}

@Composable
fun LetterAExerciseScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedPoints by remember { mutableStateOf(emptySet<Int>()) }
    var feedback by remember { mutableStateOf<ExerciseFeedback?>(null) }
    val expectedCell = remember { requireNotNull(BrailleRepository.findVowel('A')).cell }
    val isAnswerLocked = feedback == ExerciseFeedback.Correct

    fun resetExercise() {
        selectedPoints = emptySet()
        feedback = null
    }

    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TextButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.Start),
            ) {
                Text(text = stringResource(R.string.action_back))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.exercise_title),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.exercise_instruction),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(28.dp))
            BrailleCellView(
                cell = BrailleCell.fromPoints(selectedPoints),
                interactive = !isAnswerLocked,
                onPointClick = { point ->
                    selectedPoints = if (point in selectedPoints) {
                        selectedPoints - point
                    } else {
                        selectedPoints + point
                    }
                    feedback = null
                },
                contentDescription = stringResource(R.string.exercise_cell_description),
            )
            Spacer(modifier = Modifier.height(28.dp))
            Button(
                onClick = {
                    val answer = BrailleCell.fromPoints(selectedPoints)
                    feedback = if (answer == expectedCell) {
                        ExerciseFeedback.Correct
                    } else {
                        ExerciseFeedback.Incorrect
                    }
                },
                enabled = !isAnswerLocked,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 360.dp),
            ) {
                Text(text = stringResource(R.string.exercise_check))
            }

            feedback?.let { currentFeedback ->
                Spacer(modifier = Modifier.height(20.dp))
                ExerciseFeedbackCard(feedback = currentFeedback)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = ::resetExercise,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 360.dp),
                ) {
                    Text(text = stringResource(R.string.exercise_try_again))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ExerciseFeedbackCard(feedback: ExerciseFeedback) {
    val isCorrect = feedback == ExerciseFeedback.Correct
    val message = stringResource(
        if (isCorrect) R.string.exercise_correct else R.string.exercise_incorrect,
    )
    val symbol = stringResource(
        if (isCorrect) R.string.feedback_correct_symbol else R.string.feedback_incorrect_symbol,
    )
    val containerColor = if (isCorrect) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.errorContainer
    }
    val contentColor = if (isCorrect) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onErrorContainer
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 520.dp)
            .clearAndSetSemantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = message
            },
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(2.dp, contentColor),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = contentColor,
                contentColor = containerColor,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = symbol,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
