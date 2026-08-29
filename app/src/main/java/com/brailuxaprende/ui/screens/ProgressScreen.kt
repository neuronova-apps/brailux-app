package com.brailuxaprende.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.brailuxaprende.R
import com.brailuxaprende.data.learn.LearningProgress
import com.brailuxaprende.data.practice.PracticeProgress
import com.brailuxaprende.learning.LearningLesson
import com.brailuxaprende.learning.LearningPath
import com.brailuxaprende.practice.DailyMiniAchievement
import com.brailuxaprende.practice.DailyMiniAchievementXp
import com.brailuxaprende.practice.EngagementProgress
import com.brailuxaprende.practice.MonthlyExerciseTarget
import com.brailuxaprende.practice.PermanentAchievement
import com.brailuxaprende.practice.PracticeDate
import com.brailuxaprende.practice.SystemPracticeClock
import com.brailuxaprende.practice.WeeklyPracticeTarget
import com.brailuxaprende.ui.components.BrailuxScreenHeader
import com.brailuxaprende.ui.components.BrailuxSectionCard

enum class ProgressTab(
    @param:StringRes val labelResource: Int,
) {
    Summary(R.string.progress_tab_summary),
    Statistics(R.string.progress_tab_statistics),
    Achievements(R.string.progress_tab_achievements),
}

@Composable
fun ProgressScreen(
    progress: PracticeProgress,
    learningProgress: LearningProgress = LearningProgress(),
    engagementProgress: EngagementProgress = EngagementProgress(),
    currentDate: PracticeDate = SystemPracticeClock.today(),
    initialTab: ProgressTab = ProgressTab.Summary,
    hasSeasonalBackground: Boolean = false,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by rememberSaveable { mutableStateOf(initialTab) }

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
                title = stringResource(R.string.progress_title),
                subtitle = stringResource(R.string.progress_description),
                onBack = onBack,
                hasSeasonalBackground = hasSeasonalBackground,
            )
            Spacer(modifier = Modifier.height(20.dp))
            ProgressTabSelector(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
            )
            Spacer(modifier = Modifier.height(20.dp))
            when (selectedTab) {
                ProgressTab.Summary -> {
                    ProgressContentCard {
                        GeneralSummary(engagementProgress, currentDate)
                    }
                    ProgressSpacer()
                    ProgressContentCard {
                        ConsistencySection(engagementProgress, currentDate)
                    }
                    ProgressSpacer()
                    ProgressContentCard {
                        DailyMiniAchievementSection(engagementProgress, currentDate)
                    }
                }
                ProgressTab.Statistics -> {
                    PracticeProgressSection(progress)
                    ProgressSpacer()
                    ProgressContentCard {
                        LearningProgressSection(learningProgress)
                    }
                }
                ProgressTab.Achievements -> {
                    ProgressContentCard {
                        PermanentAchievementsSection(engagementProgress.unlockedAchievements)
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun ProgressTabSelector(
    selectedTab: ProgressTab,
    onTabSelected: (ProgressTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 560.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ProgressTab.entries.forEach { tab ->
            val isSelected = tab == selectedTab
            val label = stringResource(tab.labelResource)
            val tabDescription = stringResource(R.string.progress_tab_accessibility, label)
            Surface(
                onClick = { onTabSelected(tab) },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .semantics {
                        this.selected = isSelected
                        role = Role.Tab
                        contentDescription = tabDescription
                    },
                shape = MaterialTheme.shapes.medium,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                },
                contentColor = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    },
                ),
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun GeneralSummary(
    engagementProgress: EngagementProgress,
    currentDate: PracticeDate,
) {
    val streak = engagementProgress.displayedStreak(currentDate).coerceAtLeast(0)
    val weeklyDays = engagementProgress.weeklyPracticeDays(currentDate).coerceAtLeast(0)
    val monthlyExercises = engagementProgress.monthlyExercises(currentDate).coerceAtLeast(0)

    SectionTitle(R.string.progress_summary_title)
    ProgressValue(
        label = stringResource(R.string.progress_current_streak),
        value = pluralStringResource(R.plurals.progress_streak_days, streak, streak),
    )
    ProgressValue(
        label = stringResource(R.string.progress_total_xp),
        value = stringResource(R.string.progress_xp_value, engagementProgress.totalXp.coerceAtLeast(0)),
    )
    ProgressValue(
        label = stringResource(R.string.progress_weekly_goal),
        value = stringResource(
            R.string.progress_weekly_goal_value,
            weeklyDays,
            WeeklyPracticeTarget,
        ),
    )
    ProgressValue(
        label = stringResource(R.string.progress_monthly_goal),
        value = stringResource(
            R.string.progress_monthly_goal_value,
            monthlyExercises,
            MonthlyExerciseTarget,
        ),
    )
}

@Composable
private fun ConsistencySection(
    engagementProgress: EngagementProgress,
    currentDate: PracticeDate,
) {
    val weeklyDays = engagementProgress.weeklyPracticeDays(currentDate).coerceAtLeast(0)
    val todayStatus = stringResource(
        if (isPracticeCompletedToday(engagementProgress, currentDate)) {
            R.string.progress_daily_practice_completed
        } else {
            R.string.progress_daily_practice_pending
        },
    )

    SectionTitle(R.string.progress_consistency_title)
    ProgressValue(
        label = stringResource(R.string.progress_today_practice),
        value = todayStatus,
    )
    ProgressValue(
        label = stringResource(R.string.progress_days_this_week),
        value = stringResource(
            R.string.progress_weekly_goal_value,
            weeklyDays,
            WeeklyPracticeTarget,
        ),
    )
    if (engagementProgress.bestStreak > 0) {
        ProgressValue(
            label = stringResource(R.string.progress_best_streak),
            value = pluralStringResource(
                R.plurals.progress_streak_days,
                engagementProgress.bestStreak,
                engagementProgress.bestStreak,
            ),
        )
    }
    ProgressValue(
        label = stringResource(R.string.progress_last_practice_label),
        value = formattedPracticeDate(engagementProgress.lastActivityDate),
    )
}

internal fun isPracticeCompletedToday(
    progress: EngagementProgress,
    date: PracticeDate,
): Boolean = progress.hasPracticed(date)

@Composable
private fun DailyMiniAchievementSection(
    engagementProgress: EngagementProgress,
    currentDate: PracticeDate,
) {
    val achievement = engagementProgress.miniAchievement(currentDate)
    val title = stringResource(achievement.type.titleResource())
    val status = stringResource(
        if (achievement.completed) {
            R.string.progress_mini_achievement_completed
        } else {
            R.string.progress_mini_achievement_pending
        },
    )
    val progressText = stringResource(
        R.string.progress_mini_achievement_value,
        achievement.progress,
        achievement.target,
    )

    SectionTitle(R.string.progress_mini_achievement_title)
    ProgressValue(
        label = stringResource(R.string.progress_mini_achievement_objective),
        value = title,
    )
    ProgressValue(
        label = stringResource(R.string.progress_mini_achievement_progress),
        value = progressText,
    )
    ProgressValue(
        label = stringResource(R.string.progress_mini_achievement_status),
        value = status,
    )
    ProgressValue(
        label = stringResource(R.string.progress_mini_achievement_reward),
        value = stringResource(R.string.progress_xp_value, DailyMiniAchievementXp),
    )
    AccessibleProgressBar(
        progress = achievement.progress / achievement.target.toFloat(),
        description = stringResource(
            R.string.progress_mini_achievement_accessibility,
            title,
            achievement.progress,
            achievement.target,
            status,
        ),
    )
}

@Composable
private fun PracticeProgressSection(progress: PracticeProgress) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 560.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = stringResource(R.string.progress_practice_title),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        PracticeLevelCard(
            title = stringResource(R.string.progress_level_1_title),
            completedSessions = progress.level1CompletedSessions,
            totalExercises = progress.level1TotalExercises,
            firstAttemptCorrect = progress.level1FirstAttemptCorrect,
            errors = progress.level1Errors,
        )
        PracticeLevelCard(
            title = stringResource(R.string.progress_level_2_title),
            completedSessions = progress.level2CompletedSessions,
            totalExercises = progress.level2TotalExercises,
            firstAttemptCorrect = progress.level2FirstAttemptCorrect,
            errors = progress.level2Errors,
            hintsUsed = progress.level2HintsUsed,
        )
        PracticeLevelCard(
            title = stringResource(R.string.progress_level_3_title),
            completedSessions = progress.level3CompletedSessions,
            totalExercises = progress.level3TotalExercises,
            firstAttemptCorrect = progress.level3FirstAttemptCorrect,
            errors = progress.level3Errors,
        )
        PracticeLevelCard(
            title = stringResource(R.string.progress_level_4_title),
            completedSessions = progress.customCompletedSessions,
            totalExercises = progress.customTotalExercises,
            firstAttemptCorrect = progress.customFirstAttemptCorrect,
            errors = progress.customErrors,
            hintsUsed = progress.customHintsUsed,
        )
        PracticeLevelCard(
            title = stringResource(R.string.progress_daily_challenge_title),
            completedSessions = progress.dailyChallengeCompletedSessions,
            totalExercises = progress.dailyChallengeTotalExercises,
            firstAttemptCorrect = progress.dailyChallengeFirstAttemptCorrect,
            errors = progress.dailyChallengeErrors,
        )
    }
}

@Composable
private fun PracticeLevelCard(
    title: String,
    completedSessions: Int,
    totalExercises: Int,
    firstAttemptCorrect: Int,
    errors: Int? = null,
    hintsUsed: Int? = null,
) {
    val accuracy = accuracyPercentage(firstAttemptCorrect, totalExercises)
    val accuracyText = stringResource(R.string.progress_percentage_value, accuracy)
    val accuracyAccessibility = stringResource(
        R.string.progress_level_accuracy_accessibility,
        title,
        accuracy,
    )

    BrailuxSectionCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        ProgressValue(
            label = stringResource(R.string.progress_sessions_label),
            value = completedSessions.coerceAtLeast(0).toString(),
            modifier = Modifier.padding(top = 12.dp),
        )
        ProgressValue(
            label = stringResource(R.string.progress_exercises_label),
            value = totalExercises.coerceAtLeast(0).toString(),
        )
        ProgressValue(
            label = stringResource(R.string.progress_first_attempt_label),
            value = firstAttemptCorrect.coerceIn(0, totalExercises.coerceAtLeast(0)).toString(),
        )
        if (errors != null) {
            ProgressValue(
                label = stringResource(R.string.progress_errors_label),
                value = errors.coerceAtLeast(0).toString(),
            )
        }
        if (hintsUsed != null) {
            ProgressValue(
                label = stringResource(R.string.progress_hints_used_label),
                value = hintsUsed.coerceAtLeast(0).toString(),
            )
        }
        ProgressValue(
            label = stringResource(R.string.progress_accuracy_label),
            value = accuracyText,
            accessibilityText = accuracyAccessibility,
        )
        AccessibleProgressBar(
            progress = accuracy / 100f,
            description = accuracyAccessibility,
        )
    }
}

@Composable
private fun LearningProgressSection(progress: LearningProgress) {
    val totalLessons = LearningPath.lessons.size
    val completedCount = LearningPath.completedCount(progress.completedLessons)
    val percentage = LearningPath.progressPercentage(progress.completedLessons)
    val summaryAccessibility = stringResource(
        R.string.progress_learning_summary_accessibility,
        completedCount,
        totalLessons,
        percentage,
    )

    SectionTitle(R.string.progress_learning_title)
    ProgressValue(
        label = stringResource(R.string.progress_learning_completed_label),
        value = stringResource(
            R.string.progress_learning_completed_value,
            completedCount,
            totalLessons,
        ),
    )
    ProgressValue(
        label = stringResource(R.string.progress_learning_percentage_label),
        value = stringResource(R.string.progress_percentage_value, percentage),
    )
    AccessibleProgressBar(
        progress = percentage / 100f,
        description = summaryAccessibility,
    )
    Spacer(modifier = Modifier.height(12.dp))
    LearningPath.lessons.forEachIndexed { index, lesson ->
        val status = LearningPath.statusFor(lesson, progress.completedLessons)
        val statusText = stringResource(status.labelResource())
        LearningProgressItem(
            lesson = lesson,
            status = statusText,
            showDivider = index != LearningPath.lessons.lastIndex,
        )
    }
}

@Composable
private fun LearningProgressItem(
    lesson: LearningLesson,
    status: String,
    showDivider: Boolean,
) {
    val title = stringResource(lesson.titleResource())
    val accessibilityText = stringResource(
        R.string.progress_learning_accessibility,
        title,
        status,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .clearAndSetSemantics { contentDescription = accessibilityText },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = status,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
    }
    if (showDivider) HorizontalDivider()
}

@Composable
private fun PermanentAchievementsSection(
    unlockedAchievements: Set<PermanentAchievement>,
) {
    SectionTitle(R.string.progress_achievements_title)
    Text(
        text = stringResource(R.string.progress_achievements_description),
        modifier = Modifier.padding(top = 6.dp, bottom = 14.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PermanentAchievement.entries.forEach { achievement ->
            AchievementItem(
                achievement = achievement,
                unlocked = achievement in unlockedAchievements,
            )
        }
    }
}

@Composable
private fun AchievementItem(
    achievement: PermanentAchievement,
    unlocked: Boolean,
    modifier: Modifier = Modifier,
) {
    val title = stringResource(achievement.titleResource())
    val description = stringResource(achievement.descriptionResource())
    val status = stringResource(
        if (unlocked) {
            R.string.progress_achievement_unlocked
        } else {
            R.string.progress_achievement_pending
        },
    )
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
private fun ProgressValue(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accessibilityText: String? = null,
) {
    val resolvedAccessibilityText = accessibilityText ?: stringResource(
        R.string.progress_value_accessibility,
        label,
        value,
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .clearAndSetSemantics { contentDescription = resolvedAccessibilityText },
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = value,
            modifier = Modifier.padding(top = 2.dp),
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
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
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
private fun SectionTitle(@StringRes titleResource: Int) {
    Text(
        text = stringResource(titleResource),
        modifier = Modifier.semantics { heading() },
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun ProgressContentCard(content: @Composable () -> Unit) {
    BrailuxSectionCard(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 560.dp),
    ) {
        content()
    }
}

@Composable
private fun ProgressSpacer() {
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
private fun formattedPracticeDate(date: PracticeDate?): String {
    date ?: return stringResource(R.string.progress_no_last_practice)
    val months = stringArrayResource(R.array.progress_month_abbreviations)
    return stringResource(
        R.string.progress_date_format,
        date.day,
        months[date.month - 1],
        date.year,
    )
}

internal fun accuracyPercentage(firstAttemptCorrect: Int, totalExercises: Int): Int =
    com.brailuxaprende.data.practice.calculateAccuracyPercentage(firstAttemptCorrect, totalExercises)

@StringRes
private fun DailyMiniAchievement.titleResource(): Int = when (this) {
    DailyMiniAchievement.CompleteFiveExercises -> R.string.mini_achievement_five_exercises
    DailyMiniAchievement.CompleteSession -> R.string.mini_achievement_session
    DailyMiniAchievement.ThreeFirstAttemptCorrect -> R.string.mini_achievement_three_correct
    DailyMiniAchievement.TwoModalities -> R.string.mini_achievement_two_modalities
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
