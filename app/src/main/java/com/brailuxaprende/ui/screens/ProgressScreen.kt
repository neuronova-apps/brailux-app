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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.brailuxaprende.R
import com.brailuxaprende.data.practice.PracticeProgress
import com.brailuxaprende.data.practice.parseStoredPracticeDate
import com.brailuxaprende.ui.components.BrailuxPrimaryButton
import com.brailuxaprende.ui.components.BrailuxScreenHeader
import com.brailuxaprende.ui.components.BrailuxSectionCard

@Composable
fun ProgressScreen(
    progress: PracticeProgress,
    onStartPractice: () -> Unit,
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
                onStartPractice = onStartPractice,
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
    onStartPractice: () -> Unit,
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
                BrailuxPrimaryButton(
                    text = stringResource(R.string.progress_start_practice),
                    onClick = onStartPractice,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                )
            } else {
                ProgressValue(
                    label = stringResource(R.string.progress_sessions_label),
                    value = progress.level1CompletedSessions.toString(),
                )
                ProgressValue(
                    label = stringResource(R.string.progress_exercises_label),
                    value = progress.level1TotalExercises.toString(),
                )
                ProgressValue(
                    label = stringResource(R.string.progress_first_attempt_label),
                    value = progress.level1FirstAttemptCorrect.toString(),
                )
                ProgressValue(
                    label = stringResource(R.string.progress_accuracy_label),
                    value = stringResource(R.string.progress_percentage_value, accuracy),
                )
                ProgressValue(
                    label = stringResource(R.string.progress_last_practice_label),
                    value = formattedPracticeDate(progress.level1LastPracticeDate),
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
}

@Composable
private fun ProgressValue(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics {
                contentDescription = "$label, $value"
            },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun formattedPracticeDate(storedDate: String?): String {
    val date = parseStoredPracticeDate(storedDate)
        ?: return stringResource(R.string.progress_no_last_practice)
    val months = stringArrayResource(R.array.progress_month_abbreviations)
    return stringResource(
        R.string.progress_date_format,
        date.day,
        months[date.month - 1],
        date.year,
    )
}
