package com.brailuxaprende.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.brailuxaprende.R
import com.brailuxaprende.braille.BrailleCell
import com.brailuxaprende.braille.BrailleRepository
import com.brailuxaprende.ui.components.BrailleCellView
import com.brailuxaprende.ui.components.BrailuxFeedbackCard
import com.brailuxaprende.ui.components.BrailuxFeedbackType
import com.brailuxaprende.ui.components.BrailuxPrimaryButton
import com.brailuxaprende.ui.components.BrailuxScreenHeader
import com.brailuxaprende.ui.components.BrailuxSecondaryButton
import com.brailuxaprende.ui.components.BrailuxSectionCard

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

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BrailuxScreenHeader(
                title = stringResource(R.string.exercise_title),
                subtitle = stringResource(R.string.exercise_instruction),
                onBack = onBack,
            )
            Spacer(modifier = Modifier.height(22.dp))
            BrailuxSectionCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp),
            ) {
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
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                BrailuxPrimaryButton(
                    text = stringResource(R.string.exercise_check),
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
                        .padding(top = 24.dp),
                )
            }

            feedback?.let { currentFeedback ->
                Spacer(modifier = Modifier.height(18.dp))
                BrailuxFeedbackCard(
                    message = stringResource(
                        if (currentFeedback == ExerciseFeedback.Correct) {
                            R.string.exercise_correct
                        } else {
                            R.string.exercise_incorrect
                        },
                    ),
                    type = if (currentFeedback == ExerciseFeedback.Correct) {
                        BrailuxFeedbackType.Success
                    } else {
                        BrailuxFeedbackType.Error
                    },
                    modifier = Modifier.widthIn(max = 560.dp),
                )
                Spacer(modifier = Modifier.height(14.dp))
                BrailuxSecondaryButton(
                    text = stringResource(R.string.exercise_try_again),
                    onClick = ::resetExercise,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 560.dp),
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
