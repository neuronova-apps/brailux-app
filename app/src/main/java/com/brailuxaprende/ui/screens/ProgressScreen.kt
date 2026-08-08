package com.brailuxaprende.ui.screens

import androidx.annotation.StringRes
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
import com.brailuxaprende.practice.EngagementProgress
import com.brailuxaprende.practice.MonthlyExerciseTarget
import com.brailuxaprende.practice.PermanentAchievement
import com.brailuxaprende.practice.PracticeDate
import com.brailuxaprende.practice.SystemPracticeClock
import com.brailuxaprende.ui.components.BrailuxPrimaryButton
import com.brailuxaprende.ui.components.BrailuxScreenHeader
import com.brailuxaprende.ui.components.BrailuxSectionCard

@Composable
fun ProgressScreen(
    progress: PracticeProgress,
    engagementProgress: EngagementProgress = EngagementProgress(),
    currentDate: PracticeDate = SystemPracticeClock.today(),
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
            Spacer(modifier = Modifier.height(16.dp))
            MonthlyGoalCard(
                engagementProgress = engagementProgress,
                currentDate = currentDate,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            PermanentAchievementsCard(
                unlockedAchievements = engagementProgress.unlockedAchievements,
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

                AccessibleProgressBar(
                    progress = accuracy / 100f,
                    description = accuracyDescription,
                )
            }
        }
    }
}

@Composable
private fun MonthlyGoalCard(
    engagementProgress: EngagementProgress,
    currentDate: PracticeDate,
    modifier: Modifier = Modifier,
) {
    val completedExercises = engagementProgress.monthlyExercises(currentDate).coerceAtLeast(0)
    val completed = engagementProgress.isMonthlyGoalCompleted(currentDate)
    val monthName = stringArrayResource(R.array.progress_month_names)[currentDate.month - 1]
    val status = stringResource(
        if (completed) {
            R.string.progress_monthly_goal_completed
        } else {
            R.string.progress_monthly_goal_in_progress
        },
    )
    val progressText = stringResource(
        R.string.progress_monthly_goal_value,
        completedExercises,
        MonthlyExerciseTarget,
    )
    val accessibilityText = stringResource(
        R.string.progress_monthly_goal_accessibility,
        completedExercises,
        MonthlyExerciseTarget,
        status,
    )

    BrailuxSectionCard(modifier = modifier) {
        Text(
            text = stringResource(R.string.progress_monthly_goal_title, monthName),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = progressText,
            modifier = Modifier.padding(top = 10.dp),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = status,
            modifier = Modifier.padding(top = 4.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
        AccessibleProgressBar(
            progress = completedExercises / MonthlyExerciseTarget.toFloat(),
            description = accessibilityText,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

@Composable
private fun PermanentAchievementsCard(
    unlockedAchievements: Set<PermanentAchievement>,
    modifier: Modifier = Modifier,
) {
    val unlockedInDisplayOrder = PermanentAchievement.entries.filter { it in unlockedAchievements }

    BrailuxSectionCard(modifier = modifier) {
        Text(
            text = stringResource(R.string.progress_achievements_title),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.progress_achievements_description),
            modifier = Modifier.padding(top = 6.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (unlockedInDisplayOrder.isEmpty()) {
            Text(
                text = stringResource(R.string.progress_no_achievements),
                modifier = Modifier.padding(top = 14.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
        } else {
            Column(
                modifier = Modifier.padding(top = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                unlockedInDisplayOrder.forEach { achievement ->
                    AchievementItem(achievement = achievement)
                }
            }
        }
    }
}

@Composable
private fun AchievementItem(
    achievement: PermanentAchievement,
    modifier: Modifier = Modifier,
) {
    val title = stringResource(achievement.titleResource())
    val description = stringResource(achievement.descriptionResource())
    val status = stringResource(R.string.progress_achievement_unlocked)
    val accessibilityText = stringResource(
        R.string.progress_achievement_accessibility,
        title,
        description,
        status,
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = accessibilityText },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = description,
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = status,
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun ProgressValue(label: String, value: String) {
    val accessibilityText = stringResource(R.string.progress_value_accessibility, label, value)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = accessibilityText },
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
private fun AccessibleProgressBar(
    progress: Float,
    description: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        color = MaterialTheme.colorScheme.surface,
    ) {
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .padding(2.dp)
                .clearAndSetSemantics { contentDescription = description },
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

@StringRes
private fun PermanentAchievement.titleResource(): Int = when (this) {
    PermanentAchievement.FirstStep -> R.string.achievement_first_step_title
    PermanentAchievement.Consistency -> R.string.achievement_consistency_title
    PermanentAchievement.WeekInMotion -> R.string.achievement_week_in_motion_title
    PermanentAchievement.Explorer -> R.string.achievement_explorer_title
    PermanentAchievement.Recognizer -> R.string.achievement_recognizer_title
    PermanentAchievement.Challenger -> R.string.achievement_challenger_title
    PermanentAchievement.HundredExercises -> R.string.achievement_hundred_exercises_title
}

@StringRes
private fun PermanentAchievement.descriptionResource(): Int = when (this) {
    PermanentAchievement.FirstStep -> R.string.achievement_first_step_description
    PermanentAchievement.Consistency -> R.string.achievement_consistency_description
    PermanentAchievement.WeekInMotion -> R.string.achievement_week_in_motion_description
    PermanentAchievement.Explorer -> R.string.achievement_explorer_description
    PermanentAchievement.Recognizer -> R.string.achievement_recognizer_description
    PermanentAchievement.Challenger -> R.string.achievement_challenger_description
    PermanentAchievement.HundredExercises -> R.string.achievement_hundred_exercises_description
}
