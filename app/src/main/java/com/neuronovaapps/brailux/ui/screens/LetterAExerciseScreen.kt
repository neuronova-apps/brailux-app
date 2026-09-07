package com.neuronovaapps.brailux.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.neuronovaapps.brailux.R
import com.neuronovaapps.brailux.braille.BrailleCell
import com.neuronovaapps.brailux.braille.BrailleRepository
import com.neuronovaapps.brailux.ui.components.BrailleCellView
import com.neuronovaapps.brailux.ui.components.BrailuxFeedbackCard
import com.neuronovaapps.brailux.ui.components.BrailuxFeedbackType
import com.neuronovaapps.brailux.ui.components.BrailuxPrimaryButton
import com.neuronovaapps.brailux.ui.components.BrailuxSecondaryButton
import com.neuronovaapps.brailux.ui.components.BrailuxSectionCard

private enum class ExerciseFeedback {
    Correct,
    Incorrect,
}

private val BraillePointSelectionSaver = Saver<Set<Int>, IntArray>(
    save = { points -> points.sorted().toIntArray() },
    restore = { points -> points.toSet() },
)

@Composable
fun GuidedBrailleExercise(
    character: Char,
    continueLabel: String,
    onSolved: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedPoints by rememberSaveable(
        character,
        stateSaver = BraillePointSelectionSaver,
    ) { mutableStateOf(emptySet<Int>()) }
    var feedbackName by rememberSaveable(character) { mutableStateOf<String?>(null) }
    val feedback = feedbackName?.let(ExerciseFeedback::valueOf)
    val expectedCell = remember(character) {
        requireNotNull(BrailleRepository.findCharacter(character)).cell
    }
    val isAnswerLocked = feedback == ExerciseFeedback.Correct

    fun resetExercise() {
        selectedPoints = emptySet()
        feedbackName = null
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        BrailuxSectionCard(modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp)) {
            BrailleCellView(
                cell = BrailleCell.fromPoints(selectedPoints),
                interactive = !isAnswerLocked,
                onPointClick = { point ->
                    selectedPoints = if (point in selectedPoints) {
                        selectedPoints - point
                    } else {
                        selectedPoints + point
                    }
                    feedbackName = null
                },
                contentDescription = stringResource(
                    R.string.guided_exercise_cell_description,
                    character.toString(),
                ),
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            BrailuxPrimaryButton(
                text = stringResource(R.string.exercise_check),
                onClick = {
                    feedbackName = if (BrailleCell.fromPoints(selectedPoints) == expectedCell) {
                        ExerciseFeedback.Correct.name
                    } else {
                        ExerciseFeedback.Incorrect.name
                    }
                },
                enabled = !isAnswerLocked,
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            )
        }

        feedback?.let { currentFeedback ->
            Spacer(modifier = Modifier.height(16.dp))
            BrailuxFeedbackCard(
                message = if (currentFeedback == ExerciseFeedback.Correct) {
                    stringResource(R.string.guided_exercise_correct, character.toString())
                } else {
                    stringResource(R.string.exercise_incorrect)
                },
                type = if (currentFeedback == ExerciseFeedback.Correct) {
                    BrailuxFeedbackType.Success
                } else {
                    BrailuxFeedbackType.Warning
                },
                modifier = Modifier.widthIn(max = 560.dp),
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (currentFeedback == ExerciseFeedback.Correct) {
                BrailuxPrimaryButton(
                    text = continueLabel,
                    onClick = onSolved,
                    modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp),
                )
            } else {
                BrailuxSecondaryButton(
                    text = stringResource(R.string.exercise_try_again),
                    onClick = ::resetExercise,
                    modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp),
                )
            }
        }
    }
}
