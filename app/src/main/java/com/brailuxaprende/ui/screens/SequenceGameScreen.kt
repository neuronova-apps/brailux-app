package com.brailuxaprende.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.brailuxaprende.R
import com.brailuxaprende.braille.BrailleCharacter
import com.brailuxaprende.data.learn.LearningProgress
import com.brailuxaprende.play.PlayRepertoireProvider
import com.brailuxaprende.play.SequenceGameState
import com.brailuxaprende.play.SequenceRound
import com.brailuxaprende.play.SequenceRoundPhase
import com.brailuxaprende.ui.components.BrailleCellView
import com.brailuxaprende.ui.components.BrailuxFeedbackCard
import com.brailuxaprende.ui.components.BrailuxFeedbackType
import com.brailuxaprende.ui.components.BrailuxPrimaryButton
import com.brailuxaprende.ui.components.BrailuxScreenHeader
import com.brailuxaprende.ui.components.BrailuxSecondaryButton
import com.brailuxaprende.ui.components.BrailuxSectionCard

@Composable
fun SequenceGameScreen(
    learningProgress: LearningProgress,
    onRecordGameCompletion: (sessionId: String, correctSequences: Int, bestLength: Int, errors: Int) -> Unit,
    onBackToPlay: () -> Unit,
    hasSeasonalBackground: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val repertoire = remember(learningProgress) {
        PlayRepertoireProvider.getAvailableRepertoire(learningProgress)
    }

    var gameSeed by rememberSaveable { mutableIntStateOf(0) }
    var gameState by remember(gameSeed, repertoire) {
        mutableStateOf(SequenceGameState.create(repertoire))
    }

    var recordedSessionId by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(gameState.isCompleted) {
        if (gameState.isCompleted && recordedSessionId != gameState.sessionId) {
            recordedSessionId = gameState.sessionId
            onRecordGameCompletion(
                gameState.sessionId,
                gameState.correctRoundsCount,
                gameState.bestLength,
                gameState.totalErrors,
            )
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = if (hasSeasonalBackground) Color.Transparent else MaterialTheme.colorScheme.background,
    ) {
        if (gameState.isCompleted) {
            SequenceGameResultContent(
                totalRounds = gameState.totalRounds,
                correctRounds = gameState.correctRoundsCount,
                totalErrors = gameState.totalErrors,
                bestLength = gameState.bestLength,
                onPlayAgain = {
                    gameSeed++
                },
                onBackToPlay = onBackToPlay,
                hasSeasonalBackground = hasSeasonalBackground,
            )
        } else {
            val currentRound = gameState.currentRound
            if (currentRound != null) {
                SequenceRoundContent(
                    round = currentRound,
                    totalRounds = gameState.totalRounds,
                    onStartRecall = {
                        gameState = gameState.startRecall()
                    },
                    onInputCharacter = { char ->
                        gameState = gameState.onInputCharacter(char)
                    },
                    onRemoveLast = {
                        gameState = gameState.onRemoveLastInput()
                    },
                    onNextRound = {
                        gameState = gameState.onNextRound()
                    },
                    onBack = onBackToPlay,
                    hasSeasonalBackground = hasSeasonalBackground,
                )
            }
        }
    }
}

@Composable
private fun SequenceRoundContent(
    round: SequenceRound,
    totalRounds: Int,
    onStartRecall: () -> Unit,
    onInputCharacter: (BrailleCharacter) -> Unit,
    onRemoveLast: () -> Unit,
    onNextRound: () -> Unit,
    onBack: () -> Unit,
    hasSeasonalBackground: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BrailuxScreenHeader(
            title = stringResource(R.string.play_game_sequence_title),
            subtitle = stringResource(
                R.string.sequence_round_counter,
                round.roundNumber,
                totalRounds,
            ),
            onBack = onBack,
            hasSeasonalBackground = hasSeasonalBackground,
        )

        Spacer(modifier = Modifier.height(20.dp))

        when (round.phase) {
            SequenceRoundPhase.Presentation -> {
                SequencePresentationCard(
                    targetSequence = round.targetSequence,
                    onStartRecall = onStartRecall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 560.dp),
                )
            }
            SequenceRoundPhase.Recall -> {
                SequenceRecallCard(
                    targetLength = round.length,
                    userSequence = round.userSequence,
                    options = round.options,
                    onInputCharacter = onInputCharacter,
                    onRemoveLast = onRemoveLast,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 560.dp),
                )
            }
            SequenceRoundPhase.Feedback -> {
                SequenceFeedbackCard(
                    isCorrect = round.isCorrect == true,
                    targetSequence = round.targetSequence,
                    userSequence = round.userSequence,
                    isLastRound = round.roundNumber == totalRounds,
                    onNextRound = onNextRound,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 560.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SequencePresentationCard(
    targetSequence: List<BrailleCharacter>,
    onStartRecall: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BrailuxSectionCard(modifier = modifier) {
        Text(
            text = stringResource(R.string.sequence_presentation_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { heading() },
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = stringResource(R.string.sequence_presentation_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(20.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            targetSequence.forEachIndexed { index, char ->
                val desc = stringResource(
                    R.string.sequence_presentation_item,
                    index + 1,
                    targetSequence.size,
                    char.accessibleDescription,
                )
                Surface(
                    modifier = Modifier
                        .padding(horizontal = 6.dp)
                        .semantics { contentDescription = desc },
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        BrailleCellView(
                            cell = char.cell,
                            showPointNumbers = false,
                            isCompact = true,
                            contentDescription = null,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        BrailuxPrimaryButton(
            text = stringResource(R.string.sequence_start_recall),
            onClick = onStartRecall,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SequenceRecallCard(
    targetLength: Int,
    userSequence: List<BrailleCharacter>,
    options: List<BrailleCharacter>,
    onInputCharacter: (BrailleCharacter) -> Unit,
    onRemoveLast: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BrailuxSectionCard(modifier = modifier) {
        Text(
            text = stringResource(R.string.sequence_recall_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { heading() },
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = stringResource(
                R.string.sequence_recall_position,
                userSequence.size.coerceAtMost(targetLength),
                targetLength,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Slots
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            for (i in 0 until targetLength) {
                val filledChar = userSequence.getOrNull(i)
                val slotDesc = if (filledChar != null) {
                    stringResource(R.string.sequence_slot_filled, i + 1, filledChar.printedCharacter)
                } else {
                    stringResource(R.string.sequence_slot_empty, i + 1)
                }

                Surface(
                    modifier = Modifier
                        .padding(horizontal = 6.dp)
                        .size(52.dp)
                        .semantics { contentDescription = slotDesc },
                    shape = MaterialTheme.shapes.medium,
                    color = if (filledChar != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(
                        if (i == userSequence.size) 2.dp else 1.dp,
                        if (i == userSequence.size) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    ),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (filledChar != null) {
                            Text(
                                text = filledChar.printedCharacter.toString(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        } else {
                            Text(
                                text = "${i + 1}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Option buttons
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            options.forEach { char ->
                OutlinedButton(
                    onClick = { onInputCharacter(char) },
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .heightIn(min = 52.dp)
                        .widthIn(min = 52.dp),
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                ) {
                    Text(
                        text = char.printedCharacter.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (userSequence.isNotEmpty()) {
            BrailuxSecondaryButton(
                text = stringResource(R.string.sequence_clear_input),
                onClick = onRemoveLast,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SequenceFeedbackCard(
    isCorrect: Boolean,
    targetSequence: List<BrailleCharacter>,
    userSequence: List<BrailleCharacter>,
    isLastRound: Boolean,
    onNextRound: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BrailuxSectionCard(modifier = modifier) {
        BrailuxFeedbackCard(
            type = if (isCorrect) BrailuxFeedbackType.Success else BrailuxFeedbackType.Error,
            message = if (isCorrect) {
                stringResource(R.string.sequence_round_correct)
            } else {
                stringResource(R.string.sequence_round_incorrect)
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Secuencia correcta: " + targetSequence.joinToString(" - ") { it.printedCharacter.toString() },
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(24.dp))

        BrailuxPrimaryButton(
            text = stringResource(if (isLastRound) R.string.sequence_view_results else R.string.sequence_next_round),
            onClick = onNextRound,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SequenceGameResultContent(
    totalRounds: Int,
    correctRounds: Int,
    totalErrors: Int,
    bestLength: Int,
    onPlayAgain: () -> Unit,
    onBackToPlay: () -> Unit,
    hasSeasonalBackground: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BrailuxScreenHeader(
            title = stringResource(R.string.play_game_sequence_title),
            subtitle = stringResource(R.string.sequence_completed_title),
            onBack = onBackToPlay,
            hasSeasonalBackground = hasSeasonalBackground,
        )

        Spacer(modifier = Modifier.height(24.dp))

        BrailuxSectionCard(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp),
        ) {
            Text(
                text = stringResource(R.string.sequence_completed_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.sequence_result_rounds, totalRounds, totalRounds),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = stringResource(R.string.sequence_result_correct, correctRounds, totalRounds),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = stringResource(R.string.sequence_result_errors, totalErrors),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = stringResource(R.string.sequence_result_best_length, bestLength),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(24.dp))

            BrailuxPrimaryButton(
                text = stringResource(R.string.play_again),
                onClick = onPlayAgain,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(12.dp))

            BrailuxSecondaryButton(
                text = stringResource(R.string.play_back_to_play),
                onClick = onBackToPlay,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
