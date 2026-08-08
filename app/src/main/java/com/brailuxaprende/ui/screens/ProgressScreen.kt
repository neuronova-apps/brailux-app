package com.brailuxaprende.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.brailuxaprende.R
import com.brailuxaprende.data.practice.PracticeProgress
import com.brailuxaprende.ui.components.BrailuxScreenHeader
import com.brailuxaprende.ui.components.BrailuxSectionCard

@Composable
fun ProgressScreen(
    progress: PracticeProgress,
    onBack: () -> Unit,
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
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BrailuxScreenHeader(
                title = stringResource(R.string.progress_title),
                subtitle = stringResource(R.string.progress_description),
                onBack = onBack,
            )
            Spacer(modifier = Modifier.height(20.dp))
            Level1ProgressCard(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp),
            )
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun Level1ProgressCard(
    progress: PracticeProgress,
    modifier: Modifier = Modifier,
) {
    val accuracy = progress.level1AccuracyPercentage
    val accuracyDescription = stringResource(R.string.progress_accumulated_accuracy, accuracy)

    BrailuxSectionCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(R.string.progress_level_1_title),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            if (progress.level1CompletedSessions == 0) {
                Text(
                    text = stringResource(R.string.progress_no_level_1_practice),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            ProgressValue(
                text = stringResource(
                    R.string.progress_sessions_completed,
                    progress.level1CompletedSessions,
                ),
            )
            ProgressValue(
                text = stringResource(
                    R.string.progress_exercises_completed,
                    progress.level1TotalExercises,
                ),
            )
            ProgressValue(
                text = stringResource(
                    R.string.progress_first_attempt_correct,
                    progress.level1FirstAttemptCorrect,
                ),
            )
            ProgressValue(text = stringResource(R.string.progress_accuracy, accuracy))
            ProgressValue(
                text = stringResource(
                    R.string.progress_last_practice,
                    progress.level1LastPracticeDate
                        ?: stringResource(R.string.progress_no_last_practice),
                ),
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                color = MaterialTheme.colorScheme.surface,
            ) {
                LinearProgressIndicator(
                    progress = { accuracy / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .padding(2.dp)
                        .clearAndSetSemantics {
                            contentDescription = accuracyDescription
                        },
                )
            }
        }
    }
}

@Composable
private fun ProgressValue(text: String) {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.bodyLarge,
    )
}
