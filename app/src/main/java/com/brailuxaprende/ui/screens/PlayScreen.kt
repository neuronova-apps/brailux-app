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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.brailuxaprende.R
import com.brailuxaprende.data.learn.LearningProgress
import com.brailuxaprende.play.PlayGame
import com.brailuxaprende.play.PlayGameStatus
import com.brailuxaprende.play.PlayRepertoireProvider
import com.brailuxaprende.ui.components.BrailuxFeedbackCard
import com.brailuxaprende.ui.components.BrailuxFeedbackType
import com.brailuxaprende.ui.components.BrailuxPrimaryButton
import com.brailuxaprende.ui.components.BrailuxScreenHeader
import com.brailuxaprende.ui.components.BrailuxSectionCard
import com.brailuxaprende.ui.theme.LocalBrailuxTheme

@Composable
fun PlayScreen(
    learningProgress: LearningProgress,
    onStartMemory: () -> Unit,
    onStartSequence: () -> Unit,
    onStartOrder: () -> Unit,
    onBack: () -> Unit,
    hasSeasonalBackground: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val isMemoryUnlocked = PlayRepertoireProvider.isGameUnlocked(PlayGame.Memory, learningProgress)
    val isSequenceUnlocked = PlayRepertoireProvider.isGameUnlocked(PlayGame.Sequence, learningProgress)
    val isOrderUnlocked = PlayRepertoireProvider.isGameUnlocked(PlayGame.Order, learningProgress)

    val anyGameUnlocked = isMemoryUnlocked || isSequenceUnlocked || isOrderUnlocked

    Surface(
        modifier = modifier.fillMaxSize(),
        color = if (hasSeasonalBackground) Color.Transparent else MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BrailuxScreenHeader(
                title = stringResource(R.string.play_title),
                subtitle = stringResource(R.string.play_subtitle),
                onBack = onBack,
                hasSeasonalBackground = hasSeasonalBackground,
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (!anyGameUnlocked) {
                BrailuxFeedbackCard(
                    message = stringResource(R.string.play_locked_intro_message),
                    type = BrailuxFeedbackType.Warning,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 560.dp),
                )
                Spacer(modifier = Modifier.height(20.dp))
            }

            PlayGameCard(
                game = PlayGame.Memory,
                status = if (isMemoryUnlocked) PlayGameStatus.Available else PlayGameStatus.Locked,
                onPlay = onStartMemory,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp),
            )

            Spacer(modifier = Modifier.height(16.dp))

            PlayGameCard(
                game = PlayGame.Sequence,
                status = if (isSequenceUnlocked) PlayGameStatus.Available else PlayGameStatus.Locked,
                onPlay = onStartSequence,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp),
            )

            Spacer(modifier = Modifier.height(16.dp))

            PlayGameCard(
                game = PlayGame.Order,
                status = if (isOrderUnlocked) PlayGameStatus.Available else PlayGameStatus.Locked,
                onPlay = onStartOrder,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp),
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun PlayGameCard(
    game: PlayGame,
    status: PlayGameStatus,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isUnlocked = status == PlayGameStatus.Available
    val theme = LocalBrailuxTheme.current

    BrailuxSectionCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(game.titleResource),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = theme.visual.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() },
            )
            Surface(
                shape = MaterialTheme.shapes.small,
                color = if (isUnlocked) {
                    theme.visual.chipColor
                } else {
                    theme.visual.surfaceVariant
                },
                border = BorderStroke(
                    1.dp,
                    if (isUnlocked) theme.visual.primary else theme.visual.borderColor,
                ),
            ) {
                Text(
                    text = stringResource(
                        if (isUnlocked) R.string.play_status_available else R.string.play_status_locked,
                    ),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isUnlocked) {
                        theme.visual.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(game.descriptionResource),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (!isUnlocked) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(game.requirementResource),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Medium,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        BrailuxPrimaryButton(
            text = stringResource(R.string.play_action_play),
            onClick = onPlay,
            enabled = isUnlocked,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
