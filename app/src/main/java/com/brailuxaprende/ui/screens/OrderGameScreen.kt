package com.brailuxaprende.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.brailuxaprende.R
import com.brailuxaprende.braille.BrailleCharacter
import com.brailuxaprende.data.learn.LearningProgress
import com.brailuxaprende.play.OrderFeedback
import com.brailuxaprende.play.OrderGameState
import com.brailuxaprende.play.OrderItem
import com.brailuxaprende.play.OrderRound
import com.brailuxaprende.play.PlayRepertoireProvider
import com.brailuxaprende.ui.components.BrailleCellView
import com.brailuxaprende.ui.components.BrailuxFeedbackCard
import com.brailuxaprende.ui.components.BrailuxFeedbackType
import com.brailuxaprende.ui.components.BrailuxPrimaryButton
import com.brailuxaprende.ui.components.BrailuxScreenHeader
import com.brailuxaprende.ui.components.BrailuxSecondaryButton
import com.brailuxaprende.ui.components.BrailuxSectionCard

@Composable
fun OrderGameScreen(
    learningProgress: LearningProgress,
    onRecordGameCompletion: (sessionId: String, errors: Int) -> Unit,
    onBackToPlay: () -> Unit,
    hasSeasonalBackground: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val repertoire = remember(learningProgress) {
        PlayRepertoireProvider.getAvailableRepertoire(learningProgress)
    }

    var gameSeed by rememberSaveable { mutableIntStateOf(0) }
    var gameState by remember(gameSeed, repertoire) {
        mutableStateOf(OrderGameState.create(repertoire))
    }

    var recordedSessionId by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(gameState.isCompleted) {
        if (gameState.isCompleted && recordedSessionId != gameState.sessionId) {
            recordedSessionId = gameState.sessionId
            onRecordGameCompletion(
                gameState.sessionId,
                gameState.totalErrors,
            )
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = if (hasSeasonalBackground) Color.Transparent else MaterialTheme.colorScheme.background,
    ) {
        if (gameState.isCompleted) {
            OrderGameResultContent(
                totalRounds = gameState.totalRounds,
                totalErrors = gameState.totalErrors,
                onPlayAgain = {
                    gameSeed++
                },
                onBackToPlay = onBackToPlay,
                hasSeasonalBackground = hasSeasonalBackground,
            )
        } else {
            val currentRound = gameState.currentRound
            if (currentRound != null) {
                OrderRoundContent(
                    round = currentRound,
                    totalRounds = gameState.totalRounds,
                    onSelectCharacter = { char ->
                        gameState = gameState.onSelectCharacter(char)
                    },
                    onBack = onBackToPlay,
                    hasSeasonalBackground = hasSeasonalBackground,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OrderRoundContent(
    round: OrderRound,
    totalRounds: Int,
    onSelectCharacter: (BrailleCharacter) -> Unit,
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
            title = stringResource(R.string.play_game_order_title),
            subtitle = stringResource(
                R.string.order_round_counter,
                round.roundNumber,
                totalRounds,
            ),
            onBack = onBack,
            hasSeasonalBackground = hasSeasonalBackground,
        )

        Spacer(modifier = Modifier.height(20.dp))

        BrailuxSectionCard(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp),
        ) {
            Text(
                text = stringResource(R.string.order_instruction),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() },
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (round.lastFeedbackMessage == OrderFeedback.Incorrect) {
                BrailuxFeedbackCard(
                    type = BrailuxFeedbackType.Error,
                    message = stringResource(R.string.order_error_feedback),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(16.dp))
            } else if (round.lastFeedbackMessage == OrderFeedback.Correct) {
                BrailuxFeedbackCard(
                    type = BrailuxFeedbackType.Success,
                    message = stringResource(R.string.order_correct_feedback),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                round.items.forEach { item ->
                    OrderSignButton(
                        item = item,
                        onClick = { onSelectCharacter(item.character) },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun OrderSignButton(
    item: OrderItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val talkBackDesc = if (item.isSolved) {
        stringResource(
            R.string.order_sign_solved,
            item.character.printedCharacter,
            item.character.accessibleDescription,
        )
    } else {
        stringResource(
            R.string.order_sign_pending,
            item.character.printedCharacter,
            item.character.accessibleDescription,
        )
    }

    Surface(
        modifier = modifier
            .padding(horizontal = 6.dp)
            .semantics { contentDescription = talkBackDesc }
            .clickable(
                enabled = !item.isSolved,
                role = Role.Button,
                onClick = onClick,
            ),
        shape = MaterialTheme.shapes.medium,
        color = if (item.isSolved) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            if (item.isSolved) 2.dp else 1.5.dp,
            if (item.isSolved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        ),
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (item.isSolved && item.solvedOrder != null) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${item.solvedOrder}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                } else {
                    Spacer(modifier = Modifier.height(26.dp + 4.dp))
                }

                BrailleCellView(
                    cell = item.character.cell,
                    showPointNumbers = false,
                    isCompact = true,
                    contentDescription = null,
                )

                if (item.isSolved) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.character.printedCharacter.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                } else {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun OrderGameResultContent(
    totalRounds: Int,
    totalErrors: Int,
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
            title = stringResource(R.string.play_game_order_title),
            subtitle = stringResource(R.string.order_completed_title),
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
                text = stringResource(R.string.order_completed_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.order_result_rounds, totalRounds, totalRounds),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = stringResource(R.string.order_result_errors, totalErrors),
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
