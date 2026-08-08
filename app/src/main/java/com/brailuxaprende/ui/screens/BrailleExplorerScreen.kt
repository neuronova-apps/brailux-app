package com.brailuxaprende.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.brailuxaprende.R
import com.brailuxaprende.braille.BrailleCharacter
import com.brailuxaprende.practice.BrailleRow
import com.brailuxaprende.practice.PracticeExercise
import com.brailuxaprende.practice.PracticeExerciseType
import com.brailuxaprende.practice.PracticeHint
import com.brailuxaprende.practice.PracticeLevel
import com.brailuxaprende.practice.PracticeMode
import com.brailuxaprende.practice.PracticeSession
import com.brailuxaprende.practice.PracticeSessionGenerator
import com.brailuxaprende.practice.PracticeSessionState
import com.brailuxaprende.practice.PracticeSessionSummary
import com.brailuxaprende.practice.PracticeValidationState
import com.brailuxaprende.ui.components.BrailleCellView
import com.brailuxaprende.ui.components.BrailuxFeedbackCard
import com.brailuxaprende.ui.components.BrailuxFeedbackType
import com.brailuxaprende.ui.components.BrailuxPrimaryButton
import com.brailuxaprende.ui.components.BrailuxScreenHeader
import com.brailuxaprende.ui.components.BrailuxSecondaryButton
import com.brailuxaprende.ui.components.BrailuxSectionCard
import com.brailuxaprende.ui.theme.BrailuxTheme

private enum class AnswerResult {
    Correct,
    Incorrect,
}

@Composable
fun BrailleExplorerScreen(
    mode: PracticeMode,
    onSessionCompleted: (PracticeSessionSummary, onRecorded: (Boolean) -> Unit) -> Unit,
    onBackToPractice: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BraillePracticeLevelScreen(
        level = PracticeLevel.BrailleExplorer,
        sessionFactory = { PracticeSessionGenerator.generate(mode) },
        onSessionCompleted = onSessionCompleted,
        onBackToPractice = onBackToPractice,
        modifier = modifier,
    )
}

@Composable
fun BrailleRecognizerScreen(
    mode: PracticeMode,
    onSessionCompleted: (PracticeSessionSummary, onRecorded: (Boolean) -> Unit) -> Unit,
    onBackToPractice: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BraillePracticeLevelScreen(
        level = PracticeLevel.BrailleRecognizer,
        sessionFactory = { PracticeSessionGenerator.generateLevel2(mode) },
        onSessionCompleted = onSessionCompleted,
        onBackToPractice = onBackToPractice,
        modifier = modifier,
    )
}

@Composable
fun BrailleChallengeScreen(
    mode: PracticeMode,
    onSessionCompleted: (PracticeSessionSummary, onRecorded: (Boolean) -> Unit) -> Unit,
    onBackToPractice: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BraillePracticeLevelScreen(
        level = PracticeLevel.BrailleChallenge,
        sessionFactory = { PracticeSessionGenerator.generateLevel3(mode) },
        onSessionCompleted = onSessionCompleted,
        onBackToPractice = onBackToPractice,
        modifier = modifier,
    )
}

@Composable
private fun BraillePracticeLevelScreen(
    level: PracticeLevel,
    sessionFactory: () -> PracticeSession,
    onSessionCompleted: (PracticeSessionSummary, onRecorded: (Boolean) -> Unit) -> Unit,
    onBackToPractice: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var sessionKey by rememberSaveable { mutableStateOf(0) }
    val session = remember(sessionKey) { sessionFactory() }
    var state by remember(session) { mutableStateOf(PracticeSessionState(session)) }
    var completionPending by remember(session) { mutableStateOf(false) }
    var completionFailed by remember(session) { mutableStateOf(false) }

    if (state.isCompleted) {
        BraillePracticeSummary(
            level = level,
            summary = state.summary(),
            onPracticeAgain = { sessionKey += 1 },
            onBackToPractice = onBackToPractice,
            modifier = modifier,
        )
    } else {
        BraillePracticeExercise(
            level = level,
            state = state,
            completionPending = completionPending,
            completionFailed = completionFailed,
            onStateChange = { state = it },
            onNext = {
                if (!completionPending) {
                    val nextState = state.nextExercise()
                    if (nextState.isCompleted) {
                        completionPending = true
                        completionFailed = false
                        onSessionCompleted(nextState.summary()) { recorded ->
                            completionPending = false
                            if (recorded) {
                                state = nextState
                            } else {
                                completionFailed = true
                            }
                        }
                    } else {
                        state = nextState
                    }
                }
            },
            onBack = onBackToPractice,
            modifier = modifier,
        )
    }
}

@Composable
private fun BraillePracticeExercise(
    level: PracticeLevel,
    state: PracticeSessionState,
    completionPending: Boolean,
    completionFailed: Boolean,
    onStateChange: (PracticeSessionState) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val exercise = state.currentExercise
    val progressDescription = stringResource(
        R.string.practice_exercise_count,
        state.exerciseNumber,
        state.session.exercises.size,
    )

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
                title = stringResource(level.titleResource()),
                subtitle = stringResource(state.session.mode.titleResource()),
                onBack = onBack,
            )
            if (level == PracticeLevel.BrailleChallenge && state.currentExerciseIndex == 0) {
                Spacer(modifier = Modifier.height(18.dp))
                BrailuxSectionCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 560.dp),
                ) {
                    Text(
                        text = stringResource(R.string.practice_level_3_intro),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
            BrailuxSectionCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp),
            ) {
                Text(
                    text = progressDescription,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                LinearProgressIndicator(
                    progress = { state.exerciseNumber.toFloat() / state.session.exercises.size },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .semantics { contentDescription = progressDescription },
                )
                Text(
                    text = stringResource(
                        R.string.practice_first_attempt_count,
                        state.firstAttemptCorrect,
                    ),
                    modifier = Modifier.padding(top = 12.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = stringResource(R.string.practice_error_count, state.errors),
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (level.hintLimit != 0) {
                    state.hintsRemaining?.let { hintsRemaining ->
                        Text(
                            text = stringResource(R.string.practice_hints_available, hintsRemaining),
                            modifier = Modifier.padding(top = 4.dp),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
            if (level.allowsPointNumberToggle) {
                Spacer(modifier = Modifier.height(14.dp))
                BrailuxSectionCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 560.dp),
                ) {
                    Text(
                        text = stringResource(R.string.practice_orientation_reading),
                        modifier = Modifier.semantics { heading() },
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(R.string.practice_point_orientation),
                        modifier = Modifier.padding(top = 6.dp),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    PointNumberToggle(
                        checked = state.showPointNumbers,
                        onCheckedChange = { onStateChange(state.togglePointNumbers()) },
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            BrailuxSectionCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp),
            ) {
                ExercisePrompt(
                    exercise = exercise,
                    showPointNumbers = state.showPointNumbers,
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp)
                        .selectableGroup(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    exercise.options.forEach { option ->
                        val answerResult = when {
                            state.selectedCharacter != option.printedCharacter -> null
                            state.validation == PracticeValidationState.Correct -> AnswerResult.Correct
                            state.validation == PracticeValidationState.Incorrect -> AnswerResult.Incorrect
                            else -> null
                        }
                        PracticeAnswerOption(
                            option = option,
                            type = exercise.type,
                            selected = state.selectedCharacter == option.printedCharacter,
                            result = answerResult,
                            showPointNumbers = state.showPointNumbers,
                            enabled = state.validation != PracticeValidationState.Correct,
                            onSelect = {
                                onStateChange(state.selectAnswer(option.printedCharacter))
                            },
                        )
                    }
                }
            }

            if (state.visibleHints.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Column(
                    modifier = Modifier.widthIn(max = 560.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    state.visibleHints.forEachIndexed { index, hint ->
                        BrailuxFeedbackCard(
                            message = hintText(hint),
                            type = BrailuxFeedbackType.Warning,
                            announceForAccessibility = index == state.visibleHints.lastIndex,
                        )
                    }
                }
            }

            if (state.validation != PracticeValidationState.AwaitingAnswer) {
                Spacer(modifier = Modifier.height(14.dp))
                BrailuxFeedbackCard(
                    message = if (state.validation == PracticeValidationState.Correct) {
                        stringResource(
                            R.string.practice_answer_correct,
                            exercise.target.printedCharacter.toString(),
                        )
                    } else {
                        stringResource(R.string.practice_answer_incorrect)
                    },
                    type = if (state.validation == PracticeValidationState.Correct) {
                        BrailuxFeedbackType.Success
                    } else {
                        BrailuxFeedbackType.Error
                    },
                    modifier = Modifier.widthIn(max = 560.dp),
                )
            }

            if (completionFailed) {
                Spacer(modifier = Modifier.height(14.dp))
                BrailuxFeedbackCard(
                    message = stringResource(R.string.practice_progress_save_error),
                    type = BrailuxFeedbackType.Error,
                    announceForAccessibility = true,
                    modifier = Modifier.widthIn(max = 560.dp),
                )
            }

            if (level.hintLimit != 0) {
                Spacer(modifier = Modifier.height(14.dp))
                BrailuxSecondaryButton(
                    text = stringResource(R.string.practice_show_hint),
                    onClick = { onStateChange(state.showHint()) },
                    enabled = state.canShowHint,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 560.dp),
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            if (state.validation == PracticeValidationState.Correct) {
                BrailuxPrimaryButton(
                    text = stringResource(R.string.practice_next_exercise),
                    onClick = onNext,
                    enabled = !completionPending,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 560.dp),
                )
            } else {
                BrailuxPrimaryButton(
                    text = stringResource(R.string.practice_check_answer),
                    onClick = { onStateChange(state.checkAnswer()) },
                    enabled = state.selectedCharacter != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 560.dp),
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ColumnScope.ExercisePrompt(
    exercise: PracticeExercise,
    showPointNumbers: Boolean,
) {
    Text(
        text = stringResource(
            if (exercise.type == PracticeExerciseType.SignToCharacter) {
                R.string.practice_prompt_sign_to_character
            } else {
                R.string.practice_prompt_character_to_sign
            },
        ),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { heading() },
        style = MaterialTheme.typography.titleLarge,
        textAlign = TextAlign.Center,
    )
    if (exercise.type == PracticeExerciseType.SignToCharacter) {
        BrailleCellView(
            cell = exercise.target.cell,
            showPointNumbers = showPointNumbers,
            contentDescription = stringResource(
                R.string.practice_displayed_cell_description,
                activePointsText(exercise.target),
            ),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 16.dp),
        )
    } else {
        Text(
            text = exercise.target.printedCharacter.toString(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PracticeAnswerOption(
    option: BrailleCharacter,
    type: PracticeExerciseType,
    selected: Boolean,
    result: AnswerResult?,
    showPointNumbers: Boolean,
    enabled: Boolean,
    onSelect: () -> Unit,
) {
    val statusColors = BrailuxTheme.statusColors
    val stateText = when (result) {
        AnswerResult.Correct -> stringResource(R.string.practice_option_correct)
        AnswerResult.Incorrect -> stringResource(R.string.practice_option_incorrect)
        null -> stringResource(
            if (selected) R.string.settings_state_selected else R.string.settings_state_not_selected,
        )
    }
    val optionDescription = if (type == PracticeExerciseType.SignToCharacter) {
        stringResource(R.string.practice_letter_option_description, option.printedCharacter.toString())
    } else {
        stringResource(R.string.practice_cell_option_description, activePointsText(option))
    }
    val containerColor = when (result) {
        AnswerResult.Correct -> statusColors.successContainer
        AnswerResult.Incorrect -> MaterialTheme.colorScheme.errorContainer
        null -> if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        }
    }
    val contentColor = when (result) {
        AnswerResult.Correct -> statusColors.onSuccessContainer
        AnswerResult.Incorrect -> MaterialTheme.colorScheme.onErrorContainer
        null -> if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        }
    }
    val borderColor = when (result) {
        AnswerResult.Correct -> statusColors.onSuccessContainer
        AnswerResult.Incorrect -> MaterialTheme.colorScheme.onErrorContainer
        null -> if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onSelect,
            )
            .semantics(mergeDescendants = true) {
                contentDescription = optionDescription
                stateDescription = stateText
            },
        shape = MaterialTheme.shapes.medium,
        color = containerColor,
        contentColor = contentColor,
        border = BorderStroke(if (selected) 3.dp else 1.dp, borderColor),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (type == PracticeExerciseType.SignToCharacter) {
                Text(
                    text = option.printedCharacter.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
            } else {
                BrailleCellView(
                    cell = option.cell,
                    showPointNumbers = showPointNumbers,
                    modifier = Modifier.clearAndSetSemantics { },
                )
            }
            if (result != null) {
                Text(
                    text = stateText,
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun PointNumberToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = stringResource(
        if (checked) R.string.settings_state_enabled else R.string.settings_state_disabled,
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .semantics(mergeDescendants = true) { stateDescription = state },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.practice_show_point_numbers),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
        )
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun BraillePracticeSummary(
    level: PracticeLevel,
    summary: PracticeSessionSummary,
    onPracticeAgain: () -> Unit,
    onBackToPractice: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BrailuxScreenHeader(
                title = stringResource(level.completionTitleResource()),
                subtitle = stringResource(level.titleResource()),
            )
            Spacer(modifier = Modifier.height(22.dp))
            BrailuxSectionCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp),
            ) {
                SummaryLine(
                    stringResource(
                        if (level != PracticeLevel.BrailleExplorer) {
                            R.string.practice_summary_exercises_done
                        } else {
                            R.string.practice_summary_completed
                        },
                        summary.exercisesCompleted,
                    ),
                )
                SummaryLine(
                    stringResource(
                        R.string.practice_first_attempt_count,
                        summary.firstAttemptCorrect,
                    ),
                )
                SummaryLine(stringResource(R.string.practice_error_count, summary.errors))
                SummaryLine(
                    stringResource(
                        R.string.practice_summary_accuracy,
                        summary.accuracyPercentage,
                    ),
                )
                if (level == PracticeLevel.BrailleRecognizer) {
                    SummaryLine(
                        stringResource(R.string.practice_summary_hints_used, summary.hintsUsed),
                    )
                }
                SummaryLine(
                    stringResource(
                        R.string.practice_summary_letters,
                        summary.practicedLetters.joinToString(", "),
                    ),
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            BrailuxPrimaryButton(
                text = stringResource(R.string.practice_again),
                onClick = onPracticeAgain,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp),
            )
            Spacer(modifier = Modifier.height(10.dp))
            BrailuxSecondaryButton(
                text = stringResource(R.string.practice_back_to_practice),
                onClick = onBackToPractice,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp),
            )
        }
    }
}

@Composable
private fun SummaryLine(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        style = MaterialTheme.typography.bodyLarge,
    )
}

@Composable
private fun hintText(hint: PracticeHint): String = when (hint) {
    is PracticeHint.ActivePointCount -> stringResource(
        if (hint.count == 1) {
            R.string.practice_hint_point_count_one
        } else {
            R.string.practice_hint_point_count_many
        },
        hint.count,
    )
    is PracticeHint.ColumnDistribution -> stringResource(
        when {
            hint.leftCount > 0 && hint.rightCount > 0 -> R.string.practice_hint_both_columns
            hint.leftCount > 0 -> R.string.practice_hint_left_column
            hint.rightCount > 0 -> R.string.practice_hint_right_column
            else -> R.string.practice_hint_no_active_points
        },
    )
    is PracticeHint.RowState -> {
        val rowName = stringResource(
            when (hint.row) {
                BrailleRow.Top -> R.string.practice_hint_row_top
                BrailleRow.Middle -> R.string.practice_hint_row_middle
                BrailleRow.Bottom -> R.string.practice_hint_row_bottom
            },
        )
        stringResource(
            when (hint.activeCount) {
                0 -> R.string.practice_hint_row_empty
                1 -> R.string.practice_hint_row_one_point
                else -> R.string.practice_hint_row_two_points
            },
            rowName,
        )
    }
    is PracticeHint.PointState -> stringResource(
        if (hint.isActive) {
            R.string.practice_hint_point_active
        } else {
            R.string.practice_hint_point_inactive
        },
        hint.point,
    )
    is PracticeHint.CharacterCategory -> stringResource(
        if (hint.isVowel) {
            R.string.practice_hint_character_vowel
        } else {
            R.string.practice_hint_character_consonant
        },
    )
    is PracticeHint.AlphabetRange -> stringResource(
        R.string.practice_hint_alphabet_range,
        hint.first.toString(),
        hint.last.toString(),
    )
    is PracticeHint.AlphabetComparison -> stringResource(
        if (hint.targetComesAfter) {
            R.string.practice_hint_after_character
        } else {
            R.string.practice_hint_before_character
        },
        hint.reference.toString(),
    )
}

private fun activePointsText(character: BrailleCharacter): String =
    character.cell.activePoints().joinToString(", ")

@androidx.annotation.StringRes
private fun PracticeLevel.titleResource(): Int = when (this) {
    PracticeLevel.BrailleExplorer -> R.string.practice_level_1_title
    PracticeLevel.BrailleRecognizer -> R.string.practice_level_2_title
    PracticeLevel.BrailleChallenge -> R.string.practice_level_3_title
}

@androidx.annotation.StringRes
private fun PracticeLevel.completionTitleResource(): Int = when (this) {
    PracticeLevel.BrailleChallenge -> R.string.practice_challenge_completed
    PracticeLevel.BrailleExplorer,
    PracticeLevel.BrailleRecognizer -> R.string.practice_level_completed
}

@androidx.annotation.StringRes
private fun PracticeMode.titleResource(): Int = when (this) {
    PracticeMode.SignToCharacter -> R.string.practice_mode_sign_to_character
    PracticeMode.CharacterToSign -> R.string.practice_mode_character_to_sign
    PracticeMode.Mixed -> R.string.practice_mode_mixed
}
