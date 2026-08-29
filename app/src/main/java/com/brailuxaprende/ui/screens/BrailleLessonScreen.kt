package com.brailuxaprende.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.brailuxaprende.R
import com.brailuxaprende.braille.BrailleCell
import com.brailuxaprende.ui.components.BrailleCellView
import com.brailuxaprende.ui.components.BrailuxFeedbackCard
import com.brailuxaprende.ui.components.BrailuxFeedbackType
import com.brailuxaprende.ui.components.BrailuxPrimaryButton
import com.brailuxaprende.ui.components.BrailuxScreenHeader
import com.brailuxaprende.ui.components.BrailuxSecondaryButton
import com.brailuxaprende.ui.components.BrailuxSectionCard

private const val SIX_DOTS_EXPLANATION_STAGE = 0
private const val SIX_DOTS_CHECK_STAGE = 1
private const val SIX_DOTS_COMPLETED_STAGE = 2

private data class SixDotsCheckItem(
    val questionRes: Int,
    val options: List<SixDotsOption>,
    val correctId: Int,
)

private data class SixDotsOption(
    val id: Int,
    val labelRes: Int,
    val pointArg: Int? = null,
)

@Composable
fun BrailleLessonScreen(
    onCompleted: () -> Unit,
    onNextLesson: () -> Unit,
    onBackToLearn: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var stage by rememberSaveable { mutableIntStateOf(SIX_DOTS_EXPLANATION_STAGE) }
    var questionIndex by rememberSaveable { mutableIntStateOf(0) }
    var selectedAnswer by rememberSaveable { mutableStateOf<Int?>(null) }

    val questions = rememberSaveable(
        saver = androidx.compose.runtime.saveable.Saver(
            save = { 0 },
            restore = { null },
        ),
    ) {
        listOf(
            SixDotsCheckItem(
                questionRes = R.string.lesson_question_upper_left,
                options = listOf(
                    SixDotsOption(id = 1, labelRes = R.string.lesson_point_answer, pointArg = 1),
                    SixDotsOption(id = 4, labelRes = R.string.lesson_point_answer, pointArg = 4),
                ),
                correctId = 1,
            ),
            SixDotsCheckItem(
                questionRes = R.string.lesson_question_lower_right,
                options = listOf(
                    SixDotsOption(id = 3, labelRes = R.string.lesson_point_answer, pointArg = 3),
                    SixDotsOption(id = 6, labelRes = R.string.lesson_point_answer, pointArg = 6),
                ),
                correctId = 6,
            ),
            SixDotsCheckItem(
                questionRes = R.string.lesson_question_left_column,
                options = listOf(
                    SixDotsOption(id = 1, labelRes = R.string.lesson_option_left_column),
                    SixDotsOption(id = 2, labelRes = R.string.lesson_option_right_column),
                ),
                correctId = 1,
            ),
            SixDotsCheckItem(
                questionRes = R.string.lesson_question_letter_a,
                options = listOf(
                    SixDotsOption(id = 1, labelRes = R.string.lesson_point_answer, pointArg = 1),
                    SixDotsOption(id = 2, labelRes = R.string.lesson_point_answer, pointArg = 2),
                ),
                correctId = 1,
            ),
        )
    }

    fun repeatLesson() {
        stage = SIX_DOTS_EXPLANATION_STAGE
        questionIndex = 0
        selectedAnswer = null
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
                title = stringResource(R.string.learning_lesson_1_title),
                subtitle = stringResource(R.string.learning_lesson_number, 1),
                onBack = onBack,
            )
            Spacer(modifier = Modifier.height(22.dp))

            when (stage) {
                SIX_DOTS_EXPLANATION_STAGE -> {
                    BrailuxSectionCard(
                        modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.lesson_explanation),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = stringResource(R.string.lesson_columns_explanation),
                            modifier = Modifier.padding(top = 14.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                        )
                        BrailleCellView(
                            cell = BrailleCell.fromPoints(emptySet()),
                            contentDescription = stringResource(R.string.lesson_cell_description),
                            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 20.dp),
                        )
                        Text(
                            text = stringResource(R.string.lesson_combinations_explanation),
                            modifier = Modifier.padding(top = 20.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        BrailleCellView(
                            cell = BrailleCell.fromPoints(setOf(1)),
                            contentDescription = stringResource(R.string.lesson_cell_example_a_description),
                            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 16.dp),
                        )
                        BrailuxPrimaryButton(
                            text = stringResource(R.string.lesson_start_check),
                            onClick = { stage = SIX_DOTS_CHECK_STAGE },
                            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                        )
                    }
                }

                SIX_DOTS_CHECK_STAGE -> {
                    val currentQuestion = questions[questionIndex]
                    val answerIsCorrect = selectedAnswer == currentQuestion.correctId
                    val isLastQuestion = questionIndex == questions.lastIndex

                    Text(
                        text = stringResource(
                            R.string.lesson_practice_progress,
                            questionIndex + 1,
                            questions.size,
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    BrailuxSectionCard(
                        modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.lesson_mini_check),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = stringResource(currentQuestion.questionRes),
                            modifier = Modifier.padding(top = 12.dp),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            currentQuestion.options.forEach { option ->
                                val text = if (option.pointArg != null) {
                                    stringResource(option.labelRes, option.pointArg)
                                } else {
                                    stringResource(option.labelRes)
                                }
                                BrailuxSecondaryButton(
                                    text = text,
                                    onClick = { selectedAnswer = option.id },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                    selectedAnswer?.let {
                        Spacer(modifier = Modifier.height(16.dp))
                        BrailuxFeedbackCard(
                            message = stringResource(
                                if (answerIsCorrect) R.string.lesson_check_correct
                                else R.string.lesson_check_try_again,
                            ),
                            type = if (answerIsCorrect) {
                                BrailuxFeedbackType.Success
                            } else {
                                BrailuxFeedbackType.Warning
                            },
                            modifier = Modifier.widthIn(max = 560.dp),
                        )
                        if (answerIsCorrect) {
                            Spacer(modifier = Modifier.height(14.dp))
                            BrailuxPrimaryButton(
                                text = stringResource(
                                    if (isLastQuestion) R.string.lesson_finish
                                    else R.string.lesson_next_question,
                                ),
                                onClick = {
                                    if (isLastQuestion) {
                                        onCompleted()
                                        stage = SIX_DOTS_COMPLETED_STAGE
                                    } else {
                                        questionIndex++
                                        selectedAnswer = null
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp),
                            )
                        }
                    }
                }

                else -> LessonCompletionActions(
                    nextLabel = stringResource(R.string.lesson_next_vowels),
                    onNext = onNextLesson,
                    onRepeat = ::repeatLesson,
                    onBackToLearn = onBackToLearn,
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
internal fun LessonCompletionActions(
    nextLabel: String?,
    onNext: (() -> Unit)?,
    onRepeat: () -> Unit,
    onBackToLearn: () -> Unit,
) {
    BrailuxSectionCard(modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp)) {
        Text(
            text = stringResource(R.string.lesson_completed),
            modifier = Modifier.align(Alignment.CenterHorizontally),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        if (nextLabel != null && onNext != null) {
            BrailuxPrimaryButton(
                text = nextLabel,
                onClick = onNext,
                modifier = Modifier.fillMaxWidth().padding(top = 22.dp),
            )
        }
        BrailuxSecondaryButton(
            text = stringResource(R.string.lesson_repeat),
            onClick = onRepeat,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        )
        BrailuxSecondaryButton(
            text = stringResource(R.string.lesson_back_to_learn),
            onClick = onBackToLearn,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        )
    }
}
