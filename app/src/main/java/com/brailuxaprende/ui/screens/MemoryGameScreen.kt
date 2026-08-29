package com.brailuxaprende.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
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
import com.brailuxaprende.data.learn.LearningProgress
import com.brailuxaprende.play.MemoryCard
import com.brailuxaprende.play.MemoryCardType
import com.brailuxaprende.play.MemoryGameState
import com.brailuxaprende.play.PlayRepertoireProvider
import com.brailuxaprende.ui.components.BrailleCellView
import com.brailuxaprende.ui.components.BrailuxPrimaryButton
import com.brailuxaprende.ui.components.BrailuxScreenHeader
import com.brailuxaprende.ui.components.BrailuxSecondaryButton
import com.brailuxaprende.ui.components.BrailuxSectionCard
import kotlinx.coroutines.delay

@Composable
fun MemoryGameScreen(
    learningProgress: LearningProgress,
    onRecordGameCompletion: (sessionId: String, moves: Int) -> Unit,
    onBackToPlay: () -> Unit,
    hasSeasonalBackground: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val repertoire = remember(learningProgress) {
        PlayRepertoireProvider.getAvailableRepertoire(learningProgress)
    }

    var gameSeed by rememberSaveable { mutableIntStateOf(0) }
    var gameState by remember(gameSeed, repertoire) {
        mutableStateOf(MemoryGameState.create(repertoire))
    }

    var recordedSessionId by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(gameState.isProcessingMismatch) {
        if (gameState.isProcessingMismatch) {
            delay(1200L)
            gameState = gameState.dismissMismatch()
        }
    }

    LaunchedEffect(gameState.isCompleted) {
        if (gameState.isCompleted && recordedSessionId != gameState.sessionId) {
            recordedSessionId = gameState.sessionId
            onRecordGameCompletion(gameState.sessionId, gameState.moves)
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = if (hasSeasonalBackground) Color.Transparent else MaterialTheme.colorScheme.background,
    ) {
        if (gameState.isCompleted) {
            MemoryGameResultContent(
                totalPairs = gameState.totalPairs,
                moves = gameState.moves,
                onPlayAgain = {
                    gameSeed++
                },
                onBackToPlay = onBackToPlay,
                hasSeasonalBackground = hasSeasonalBackground,
            )
        } else {
            MemoryGameBoardContent(
                gameState = gameState,
                onCardClick = { cardId ->
                    if (gameState.isProcessingMismatch) {
                        gameState = gameState.dismissMismatch().onCardClick(cardId)
                    } else {
                        gameState = gameState.onCardClick(cardId)
                    }
                },
                onBack = onBackToPlay,
                hasSeasonalBackground = hasSeasonalBackground,
            )
        }
    }
}

@Composable
private fun MemoryGameBoardContent(
    gameState: MemoryGameState,
    onCardClick: (Int) -> Unit,
    onBack: () -> Unit,
    hasSeasonalBackground: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BrailuxScreenHeader(
            title = stringResource(R.string.play_game_memory_title),
            subtitle = stringResource(
                R.string.memory_pairs_counter,
                gameState.matchedPairs,
                gameState.totalPairs,
            ) + "  ·  " + stringResource(R.string.memory_moves_counter, gameState.moves),
            onBack = onBack,
            hasSeasonalBackground = hasSeasonalBackground,
        )

        Spacer(modifier = Modifier.height(16.dp))

        val columns = if (gameState.cards.size <= 10) 3 else 3

        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .widthIn(max = 480.dp),
        ) {
            items(gameState.cards, key = { it.id }) { card ->
                MemoryCardView(
                    card = card,
                    onClick = { onCardClick(card.id) },
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun MemoryCardView(
    card: MemoryCard,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isOpen = card.isRevealed || card.isMatched

    val talkBackDesc = when {
        card.isMatched -> stringResource(R.string.memory_card_matched, card.character.printedCharacter)
        card.isRevealed && card.type == MemoryCardType.Letter ->
            stringResource(R.string.memory_card_letter_revealed, card.character.printedCharacter)
        card.isRevealed && card.type == MemoryCardType.Braille ->
            stringResource(
                R.string.memory_card_braille_revealed,
                card.character.printedCharacter,
                card.character.accessibleDescription,
            )
        else -> stringResource(R.string.memory_card_closed)
    }

    val cardModifier = modifier
        .aspectRatio(0.85f)
        .heightIn(min = 64.dp)
        .semantics {
            contentDescription = talkBackDesc
        }
        .clickable(
            enabled = !card.isMatched && !card.isRevealed,
            role = Role.Button,
            onClick = onClick,
        )

    val surfaceColor = when {
        card.isMatched -> MaterialTheme.colorScheme.primaryContainer
        card.isRevealed -> MaterialTheme.colorScheme.surface
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val borderColor = when {
        card.isMatched -> MaterialTheme.colorScheme.primary
        card.isRevealed -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }

    Surface(
        modifier = cardModifier,
        shape = MaterialTheme.shapes.medium,
        color = surfaceColor,
        border = BorderStroke(if (isOpen) 2.dp else 1.5.dp, borderColor),
        shadowElevation = if (isOpen) 2.dp else 1.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (isOpen) {
                when (card.type) {
                    MemoryCardType.Letter -> {
                        Text(
                            text = card.character.printedCharacter.toString(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (card.isMatched) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                        )
                    }
                    MemoryCardType.Braille -> {
                        BrailleCellView(
                            cell = card.character.cell,
                            showPointNumbers = false,
                            isCompact = true,
                            contentDescription = null,
                        )
                    }
                }
            } else {
                Text(
                    text = "?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MemoryGameResultContent(
    totalPairs: Int,
    moves: Int,
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
            title = stringResource(R.string.play_game_memory_title),
            subtitle = stringResource(R.string.memory_completed_title),
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
                text = stringResource(R.string.memory_completed_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.memory_result_pairs, totalPairs, totalPairs),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.memory_result_moves, moves),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

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
