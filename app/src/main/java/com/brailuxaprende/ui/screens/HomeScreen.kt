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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.brailuxaprende.R
import com.brailuxaprende.data.seasonal.SeasonalEvent
import com.brailuxaprende.practice.DailyMiniAchievement
import com.brailuxaprende.practice.DailyMiniAchievementStatus
import com.brailuxaprende.practice.EngagementProgress
import com.brailuxaprende.practice.PracticeDate
import com.brailuxaprende.practice.SystemPracticeClock
import com.brailuxaprende.practice.WeeklyPracticeTarget
import com.brailuxaprende.ui.components.BrailuxMenuCard
import com.brailuxaprende.ui.components.BrailuxPrimaryButton
import com.brailuxaprende.ui.components.BrailuxSecondaryButton
import com.brailuxaprende.ui.components.BrailuxSectionCard
import com.brailuxaprende.ui.components.SeasonalBanner
import com.brailuxaprende.ui.theme.BrailuxPreviewTheme

@Composable
fun HomeScreen(
    engagementProgress: EngagementProgress = EngagementProgress(),
    currentDate: PracticeDate = SystemPracticeClock.today(),
    seasonalEvent: SeasonalEvent? = null,
    onStartDailyPractice: () -> Unit = {},
    onLearn: () -> Unit,
    onPractice: () -> Unit,
    onSettings: () -> Unit,
    onAbout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val weeklyPracticeDays = engagementProgress
        .weeklyPracticeDays(currentDate)
        .coerceIn(0, WeeklyPracticeTarget)
    val currentStreak = engagementProgress.displayedStreak(currentDate).coerceAtLeast(0)
    val miniAchievement = engagementProgress.miniAchievement(currentDate)

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
            Text(
                text = stringResource(R.string.home_title),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.home_welcome_message),
                modifier = Modifier
                    .padding(top = 8.dp)
                    .widthIn(max = 560.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (seasonalEvent != null) {
                var showSeasonalBanner by rememberSaveable(seasonalEvent.id) {
                    mutableStateOf(true)
                }
                if (showSeasonalBanner) {
                    Spacer(modifier = Modifier.height(20.dp))
                    SeasonalBanner(
                        event = seasonalEvent,
                        onDismiss = { showSeasonalBanner = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 560.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            DailyPracticeCard(
                completedToday = engagementProgress.isDailyPracticeCompleted(currentDate),
                onClick = onStartDailyPractice,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            EngagementOverviewCard(
                weeklyPracticeDays = weeklyPracticeDays,
                currentStreak = currentStreak,
                totalXp = engagementProgress.totalXp.coerceAtLeast(0),
                miniAchievement = miniAchievement,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp),
            )
            Spacer(modifier = Modifier.height(18.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                BrailuxPrimaryButton(
                    text = stringResource(R.string.home_continue_learning),
                    iconResource = R.drawable.ic_learn,
                    onClick = onLearn,
                    modifier = Modifier.fillMaxWidth(),
                )
                BrailuxMenuCard(
                    title = stringResource(R.string.home_access_practice),
                    description = stringResource(R.string.home_access_practice_description),
                    iconResource = R.drawable.ic_practice,
                    onClick = onPractice,
                )
            }
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = stringResource(R.string.home_more_options),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp)
                    .semantics { heading() },
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                BrailuxSecondaryButton(
                    text = stringResource(R.string.home_access_settings),
                    iconResource = R.drawable.ic_settings,
                    onClick = onSettings,
                    modifier = Modifier.fillMaxWidth(),
                )
                BrailuxSecondaryButton(
                    text = stringResource(R.string.home_access_about),
                    iconResource = R.drawable.ic_info,
                    onClick = onAbout,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DailyPracticeCard(
    completedToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val status = stringResource(
        if (completedToday) {
            R.string.home_daily_practice_completed_today
        } else {
            R.string.home_daily_practice_pending
        },
    )

    Card(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 172.dp)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                stateDescription = status
            },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(52.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_practice),
                        contentDescription = null,
                        modifier = Modifier.padding(13.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.home_daily_practice),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(R.string.home_daily_practice_details),
                        modifier = Modifier.padding(top = 4.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = status,
                        modifier = Modifier.padding(top = 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Text(
                    text = stringResource(R.string.home_daily_practice_start),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun EngagementOverviewCard(
    weeklyPracticeDays: Int,
    currentStreak: Int,
    totalXp: Long,
    miniAchievement: DailyMiniAchievementStatus,
    modifier: Modifier = Modifier,
) {
    val weeklyText = stringResource(
        R.string.home_weekly_practice_value,
        weeklyPracticeDays,
        WeeklyPracticeTarget,
    )
    val weeklyAccessibility = stringResource(
        R.string.home_weekly_practice_accessibility,
        weeklyPracticeDays,
        WeeklyPracticeTarget,
    )
    val streakText = pluralStringResource(
        R.plurals.home_streak_days,
        currentStreak,
        currentStreak,
    )
    val xpText = stringResource(R.string.home_xp_value, totalXp)
    val miniTitle = stringResource(miniAchievement.type.titleResource())
    val miniProgressText = if (miniAchievement.completed) {
        stringResource(R.string.home_mini_achievement_completed)
    } else {
        stringResource(
            R.string.home_mini_achievement_progress,
            miniAchievement.progress,
            miniAchievement.target,
        )
    }
    val miniAccessibility = if (miniAchievement.completed) {
        stringResource(R.string.home_mini_achievement_completed_accessibility, miniTitle)
    } else {
        stringResource(
            R.string.home_mini_achievement_progress_accessibility,
            miniTitle,
            miniAchievement.progress,
            miniAchievement.target,
        )
    }

    BrailuxSectionCard(modifier = modifier) {
        Text(
            text = stringResource(R.string.home_consistency_title),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.home_weekly_practice_title),
            modifier = Modifier.padding(top = 16.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = weeklyText,
            modifier = Modifier.padding(top = 4.dp),
            style = MaterialTheme.typography.bodyLarge,
        )
        AccessibleProgressBar(
            progress = weeklyPracticeDays / WeeklyPracticeTarget.toFloat(),
            description = weeklyAccessibility,
            modifier = Modifier.padding(top = 10.dp),
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
        HomeMetric(
            label = stringResource(R.string.home_streak_title),
            value = streakText,
        )
        Spacer(modifier = Modifier.height(14.dp))
        HomeMetric(
            label = stringResource(R.string.home_xp_title),
            value = xpText,
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
        Text(
            text = stringResource(R.string.home_mini_achievement_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = miniTitle,
            modifier = Modifier.padding(top = 6.dp),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = miniProgressText,
            modifier = Modifier.padding(top = 4.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
        AccessibleProgressBar(
            progress = miniAchievement.progress / miniAchievement.target.toFloat(),
            description = miniAccessibility,
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}

@Composable
private fun HomeMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val accessibilityText = stringResource(R.string.home_metric_accessibility, label, value)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = accessibilityText },
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = value,
            modifier = Modifier.padding(top = 4.dp),
            style = MaterialTheme.typography.headlineSmall,
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

@StringRes
private fun DailyMiniAchievement.titleResource(): Int = when (this) {
    DailyMiniAchievement.CompleteFiveExercises -> R.string.mini_achievement_five_exercises
    DailyMiniAchievement.CompleteSession -> R.string.mini_achievement_session
    DailyMiniAchievement.ThreeFirstAttemptCorrect -> R.string.mini_achievement_three_correct
    DailyMiniAchievement.TwoModalities -> R.string.mini_achievement_two_modalities
}

@Preview(name = "Inicio", showBackground = true, widthDp = 390, heightDp = 1200)
@Composable
private fun HomeScreenPreview() {
    val currentDate = PracticeDate(year = 2026, month = 8, day = 8)
    BrailuxPreviewTheme {
        HomeScreen(
            engagementProgress = EngagementProgress(
                totalXp = 86,
                activityDates = setOf(currentDate, currentDate.plusDays(-1)),
                lastActivityDate = currentDate,
                currentStreak = 2,
                miniAchievementDate = currentDate,
                miniAchievementType = DailyMiniAchievement.CompleteFiveExercises,
                miniAchievementProgress = 3,
            ),
            currentDate = currentDate,
            onLearn = {},
            onPractice = {},
            onSettings = {},
            onAbout = {},
        )
    }
}
