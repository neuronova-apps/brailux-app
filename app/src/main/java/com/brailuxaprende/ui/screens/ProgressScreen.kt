package com.brailuxaprende.ui.screens

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
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
import com.brailuxaprende.data.play.GameProgress
import com.brailuxaprende.data.practice.PracticeProgress
import com.brailuxaprende.learning.LearningLesson
import com.brailuxaprende.learning.LearningPath
import com.brailuxaprende.practice.AchievementFamily
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
    gameProgress: GameProgress = GameProgress(),
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
                        GameProgressSection(gameProgress)
                    }
                    ProgressSpacer()
                    ProgressContentCard {
                        LearningProgressSection(learningProgress)
                    }
                }
                ProgressTab.Achievements -> {
                    PermanentAchievementsTabContent(
                        engagementProgress = engagementProgress,
                        learningProgress = learningProgress,
                    )
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
private fun GameProgressSection(progress: GameProgress) {
    SectionTitle(R.string.progress_play_title)
    ProgressValue(
        label = stringResource(R.string.progress_play_total_games, progress.totalGamesCompleted),
        value = progress.totalGamesCompleted.toString(),
        modifier = Modifier.padding(top = 4.dp),
    )
    ProgressValue(
        label = stringResource(R.string.play_game_memory_title),
        value = stringResource(R.string.progress_play_memory_games, progress.memoryCompletedGames),
    )
    if (progress.memoryBestMoves != null && progress.memoryBestMoves > 0) {
        ProgressValue(
            label = stringResource(R.string.progress_play_best_memory, progress.memoryBestMoves),
            value = "${progress.memoryBestMoves}",
        )
    }
    ProgressValue(
        label = stringResource(R.string.play_game_sequence_title),
        value = stringResource(R.string.progress_play_sequence_games, progress.sequenceCompletedGames),
    )
    if (progress.sequenceBestLength > 0) {
        ProgressValue(
            label = stringResource(R.string.progress_play_best_sequence, progress.sequenceBestLength),
            value = "${progress.sequenceBestLength}",
        )
    }
    ProgressValue(
        label = stringResource(R.string.play_game_order_title),
        value = stringResource(R.string.progress_play_order_games, progress.orderCompletedGames),
    )
    if (progress.orderBestErrors != null) {
        ProgressValue(
            label = stringResource(R.string.progress_play_best_order, progress.orderBestErrors),
            value = "${progress.orderBestErrors}",
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
private fun PermanentAchievementsTabContent(
    engagementProgress: EngagementProgress,
    learningProgress: LearningProgress,
) {
    val activeAchievements = PermanentAchievement.activeEntries
    val totalBadges = activeAchievements.size
    val unlockedCount = activeAchievements.count { it in engagementProgress.unlockedAchievements }
    val accessibilityHeader = stringResource(
        R.string.progress_achievements_header_summary_accessibility,
        unlockedCount,
        totalBadges,
    )
    val precisionAccessibility = stringResource(
        R.string.progress_precision_header_summary_accessibility,
        engagementProgress.currentPrecisionStreak,
        engagementProgress.bestPrecisionStreak,
    )

    ProgressContentCard {
        SectionTitle(R.string.progress_achievements_title)
        Text(
            text = stringResource(R.string.progress_achievements_description),
            modifier = Modifier.padding(top = 6.dp, bottom = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clearAndSetSemantics { contentDescription = accessibilityHeader },
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(
                        R.string.progress_achievements_header_summary,
                        unlockedCount,
                        totalBadges,
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "${(unlockedCount * 100 / totalBadges)}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        AccessibleProgressBar(
            progress = unlockedCount.toFloat() / totalBadges.toFloat(),
            description = accessibilityHeader,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clearAndSetSemantics { contentDescription = precisionAccessibility },
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = stringResource(R.string.achievements_family_precision),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(
                        R.string.progress_precision_header_summary,
                        engagementProgress.currentPrecisionStreak,
                        engagementProgress.bestPrecisionStreak,
                    ),
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    ProgressSpacer()

    AchievementFamily.entries.forEach { family ->
        val familyBadges = activeAchievements.filter { it.family == family }
        val familyUnlockedCount = familyBadges.count { it in engagementProgress.unlockedAchievements }
        ProgressContentCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(family.titleResource()),
                    modifier = Modifier
                        .weight(1f)
                        .semantics { heading() },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = if (familyUnlockedCount == familyBadges.size) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    contentColor = if (familyUnlockedCount == familyBadges.size) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                ) {
                    Text(
                        text = "$familyUnlockedCount / ${familyBadges.size}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                familyBadges.forEach { achievement ->
                    AchievementBadgeCard(
                        achievement = achievement,
                        engagementProgress = engagementProgress,
                        learningProgress = learningProgress,
                    )
                }
            }
        }
        ProgressSpacer()
    }
}

@Composable
private fun AchievementBadgeCard(
    achievement: PermanentAchievement,
    engagementProgress: EngagementProgress,
    learningProgress: LearningProgress,
    modifier: Modifier = Modifier,
) {
    val isUnlocked = achievement in engagementProgress.unlockedAchievements
    val unlockDate = engagementProgress.achievementUnlockDates[achievement]
    val (currentVal, targetVal, progressFormatted) = computeAchievementProgress(
        achievement = achievement,
        engagementProgress = engagementProgress,
        learningProgress = learningProgress,
    )
    val title = stringResource(achievement.titleResource())
    val description = stringResource(achievement.descriptionResource())
    val formattedDate = unlockDate?.let { formattedPracticeDate(it) }
    val statusText = when {
        isUnlocked && formattedDate != null -> stringResource(
            R.string.progress_achievement_unlocked_with_date,
            formattedDate,
        )
        isUnlocked -> stringResource(R.string.progress_achievement_unlocked)
        currentVal > 0 -> stringResource(R.string.progress_achievement_in_progress)
        else -> stringResource(R.string.progress_achievement_locked)
    }

    val accessibilityText = if (isUnlocked) {
        stringResource(
            R.string.progress_achievement_accessibility,
            title,
            description,
            statusText,
        )
    } else {
        stringResource(
            R.string.progress_achievement_progress_accessibility,
            title,
            description,
            statusText,
            progressFormatted,
        )
    }

    val cardBorder = if (isUnlocked) {
        BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    }

    val cardBackground = if (isUnlocked) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    val iconResource = achievement.iconResource()
    val isMedal = achievement.family == AchievementFamily.BrailleTrajectory

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = accessibilityText },
        shape = MaterialTheme.shapes.medium,
        color = cardBackground,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = cardBorder,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            if (iconResource != null) {
                Box(
                    modifier = if (isMedal) {
                        Modifier
                            .width(58.dp)
                            .aspectRatio(512f / 612f)
                    } else {
                        Modifier.size(64.dp)
                    },
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(iconResource),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                        alpha = if (isUnlocked) 1f else 0.38f,
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        text = title,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = when {
                            isUnlocked -> MaterialTheme.colorScheme.primaryContainer
                            currentVal > 0 -> MaterialTheme.colorScheme.secondaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        contentColor = when {
                            isUnlocked -> MaterialTheme.colorScheme.onPrimaryContainer
                            currentVal > 0 -> MaterialTheme.colorScheme.onSecondaryContainer
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    ) {
                        Text(
                            text = if (isUnlocked) {
                                stringResource(R.string.progress_achievement_unlocked)
                            } else if (currentVal > 0) {
                                stringResource(R.string.progress_achievement_in_progress)
                            } else {
                                stringResource(R.string.progress_achievement_locked)
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Text(
                    text = description,
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (isUnlocked) {
                    if (formattedDate != null) {
                        Text(
                            text = stringResource(
                                R.string.progress_achievement_unlocked_with_date,
                                formattedDate,
                            ),
                            modifier = Modifier.padding(top = 8.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                } else {
                    Text(
                        text = progressFormatted,
                        modifier = Modifier.padding(top = 8.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val progressFraction =
                        (currentVal.toFloat() / targetVal.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun computeAchievementProgress(
    achievement: PermanentAchievement,
    engagementProgress: EngagementProgress,
    learningProgress: LearningProgress,
): Triple<Int, Int, String> {
    return when (achievement) {
        PermanentAchievement.FirstStep -> {
            val current = engagementProgress.totalSessions
            Triple(current, 1, stringResource(R.string.badge_progress_sessions, current, 1))
        }
        PermanentAchievement.Explorer -> {
            val current = engagementProgress.level1Sessions
            Triple(current, 5, stringResource(R.string.badge_progress_sessions, current, 5))
        }
        PermanentAchievement.Recognizer -> {
            val current = engagementProgress.level2Sessions
            Triple(current, 5, stringResource(R.string.badge_progress_sessions, current, 5))
        }
        PermanentAchievement.Challenger -> {
            val current = engagementProgress.level3Sessions
            Triple(current, 3, stringResource(R.string.badge_progress_sessions, current, 3))
        }
        PermanentAchievement.FullAlphabet -> {
            val current = learningProgress.completedLessons.size
            Triple(current, 5, stringResource(R.string.badge_progress_lessons, current, 5))
        }
        PermanentAchievement.Consistency -> {
            val current = engagementProgress.activityDates.size
            Triple(current, 3, stringResource(R.string.badge_progress_days, current, 3))
        }
        PermanentAchievement.WeekInMotion -> {
            val current = engagementProgress.activityDates.groupingBy { it.weekStart }
                .eachCount().values.maxOrNull() ?: 0
            Triple(current, 5, stringResource(R.string.badge_progress_days, current, 5))
        }
        PermanentAchievement.ConstantWeek -> {
            val current = maxOf(engagementProgress.currentStreak, engagementProgress.bestStreak)
            Triple(
                current,
                7,
                stringResource(R.string.badge_progress_consecutive_days, current, 7),
            )
        }
        PermanentAchievement.TwoWeeks -> {
            val current = maxOf(engagementProgress.currentStreak, engagementProgress.bestStreak)
            Triple(
                current,
                14,
                stringResource(R.string.badge_progress_consecutive_days, current, 14),
            )
        }
        PermanentAchievement.ConsistencyMonth -> {
            val current = maxOf(engagementProgress.currentStreak, engagementProgress.bestStreak)
            Triple(
                current,
                30,
                stringResource(R.string.badge_progress_consecutive_days, current, 30),
            )
        }
        PermanentAchievement.SuperiorConsistency -> {
            val current = maxOf(engagementProgress.currentStreak, engagementProgress.bestStreak)
            Triple(
                current,
                60,
                stringResource(R.string.badge_progress_consecutive_days, current, 60),
            )
        }
        PermanentAchievement.Bronze -> {
            val current = engagementProgress.totalExercises.toInt()
            Triple(current, 25, stringResource(R.string.badge_progress_exercises, current, 25))
        }
        PermanentAchievement.Silver -> {
            val current = engagementProgress.totalExercises.toInt()
            Triple(current, 75, stringResource(R.string.badge_progress_exercises, current, 75))
        }
        PermanentAchievement.Gold -> {
            val current = engagementProgress.totalExercises.toInt()
            Triple(current, 125, stringResource(R.string.badge_progress_exercises, current, 125))
        }
        PermanentAchievement.Platinum -> {
            val current = engagementProgress.totalExercises.toInt()
            Triple(current, 300, stringResource(R.string.badge_progress_exercises, current, 300))
        }
        PermanentAchievement.Diamond -> {
            val current = engagementProgress.totalExercises.toInt()
            Triple(current, 600, stringResource(R.string.badge_progress_exercises, current, 600))
        }
        PermanentAchievement.BrailleSupremacy -> {
            val current = engagementProgress.totalExercises.toInt()
            Triple(current, 1200, stringResource(R.string.badge_progress_exercises, current, 1200))
        }
        PermanentAchievement.BrailleFocus -> {
            val current = engagementProgress.bestPrecisionStreak
            Triple(
                current,
                5,
                stringResource(R.string.badge_progress_precision_streak, current, 5),
            )
        }
        PermanentAchievement.BrailleRhythm -> {
            val current = engagementProgress.bestPrecisionStreak
            Triple(
                current,
                10,
                stringResource(R.string.badge_progress_precision_streak, current, 10),
            )
        }
        PermanentAchievement.BraillePrecision -> {
            val current = engagementProgress.bestPrecisionStreak
            Triple(
                current,
                15,
                stringResource(R.string.badge_progress_precision_streak, current, 15),
            )
        }
        PermanentAchievement.SustainedReading -> {
            val current = engagementProgress.bestPrecisionStreak
            Triple(
                current,
                30,
                stringResource(R.string.badge_progress_precision_streak, current, 30),
            )
        }
        PermanentAchievement.ConstantMastery -> {
            val current = engagementProgress.bestPrecisionStreak
            Triple(
                current,
                50,
                stringResource(R.string.badge_progress_precision_streak, current, 50),
            )
        }
        PermanentAchievement.SuperiorPrecision -> {
            val current = engagementProgress.bestPrecisionStreak
            Triple(
                current,
                75,
                stringResource(R.string.badge_progress_precision_streak, current, 75),
            )
        }
        PermanentAchievement.DoubleMeaning -> {
            val current = engagementProgress.recognizerMixedSessions
            Triple(current, 5, stringResource(R.string.badge_progress_sessions, current, 5))
        }
        PermanentAchievement.BidirectionalReading -> {
            val current = engagementProgress.advancedMixedSessions
            Triple(current, 15, stringResource(R.string.badge_progress_sessions, current, 15))
        }
        PermanentAchievement.HundredExercises -> {
            val current = engagementProgress.totalExercises.toInt()
            Triple(current, 100, stringResource(R.string.badge_progress_exercises, current, 100))
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
private fun AchievementFamily.titleResource(): Int = when (this) {
    AchievementFamily.LearningPath -> R.string.achievements_family_learning_path
    AchievementFamily.Consistency -> R.string.achievements_family_consistency
    AchievementFamily.BrailleTrajectory -> R.string.achievements_family_trajectory
    AchievementFamily.Precision -> R.string.achievements_family_precision
    AchievementFamily.MixedMastery -> R.string.achievements_family_mixed_mastery
}

@StringRes
internal fun PermanentAchievement.titleResource(): Int = when (this) {
    PermanentAchievement.FirstStep -> R.string.achievement_first_step_title
    PermanentAchievement.Explorer -> R.string.achievement_explorer_title
    PermanentAchievement.Recognizer -> R.string.achievement_recognizer_title
    PermanentAchievement.Challenger -> R.string.achievement_challenger_title
    PermanentAchievement.FullAlphabet -> R.string.achievement_full_alphabet_title
    PermanentAchievement.Consistency -> R.string.achievement_consistency_title
    PermanentAchievement.WeekInMotion -> R.string.achievement_week_in_motion_title
    PermanentAchievement.ConstantWeek -> R.string.achievement_constant_week_title
    PermanentAchievement.TwoWeeks -> R.string.achievement_two_weeks_title
    PermanentAchievement.ConsistencyMonth -> R.string.achievement_consistency_month_title
    PermanentAchievement.SuperiorConsistency -> R.string.achievement_superior_consistency_title
    PermanentAchievement.Bronze -> R.string.achievement_bronze_title
    PermanentAchievement.Silver -> R.string.achievement_silver_title
    PermanentAchievement.Gold -> R.string.achievement_gold_title
    PermanentAchievement.Platinum -> R.string.achievement_platinum_title
    PermanentAchievement.Diamond -> R.string.achievement_diamond_title
    PermanentAchievement.BrailleSupremacy -> R.string.achievement_braille_supremacy_title
    PermanentAchievement.BrailleFocus -> R.string.achievement_braille_focus_title
    PermanentAchievement.BrailleRhythm -> R.string.achievement_braille_rhythm_title
    PermanentAchievement.BraillePrecision -> R.string.achievement_braille_precision_title
    PermanentAchievement.SustainedReading -> R.string.achievement_sustained_reading_title
    PermanentAchievement.ConstantMastery -> R.string.achievement_constant_mastery_title
    PermanentAchievement.SuperiorPrecision -> R.string.achievement_superior_precision_title
    PermanentAchievement.DoubleMeaning -> R.string.achievement_double_meaning_title
    PermanentAchievement.BidirectionalReading -> R.string.achievement_bidirectional_reading_title
    PermanentAchievement.HundredExercises -> R.string.achievement_hundred_exercises_title
}

@StringRes
internal fun PermanentAchievement.descriptionResource(): Int = when (this) {
    PermanentAchievement.FirstStep -> R.string.achievement_first_step_description
    PermanentAchievement.Explorer -> R.string.achievement_explorer_description
    PermanentAchievement.Recognizer -> R.string.achievement_recognizer_description
    PermanentAchievement.Challenger -> R.string.achievement_challenger_description
    PermanentAchievement.FullAlphabet -> R.string.achievement_full_alphabet_description
    PermanentAchievement.Consistency -> R.string.achievement_consistency_description
    PermanentAchievement.WeekInMotion -> R.string.achievement_week_in_motion_description
    PermanentAchievement.ConstantWeek -> R.string.achievement_constant_week_description
    PermanentAchievement.TwoWeeks -> R.string.achievement_two_weeks_description
    PermanentAchievement.ConsistencyMonth -> R.string.achievement_consistency_month_description
    PermanentAchievement.SuperiorConsistency -> R.string.achievement_superior_consistency_description
    PermanentAchievement.Bronze -> R.string.achievement_bronze_description
    PermanentAchievement.Silver -> R.string.achievement_silver_description
    PermanentAchievement.Gold -> R.string.achievement_gold_description
    PermanentAchievement.Platinum -> R.string.achievement_platinum_description
    PermanentAchievement.Diamond -> R.string.achievement_diamond_description
    PermanentAchievement.BrailleSupremacy -> R.string.achievement_braille_supremacy_description
    PermanentAchievement.BrailleFocus -> R.string.achievement_braille_focus_description
    PermanentAchievement.BrailleRhythm -> R.string.achievement_braille_rhythm_description
    PermanentAchievement.BraillePrecision -> R.string.achievement_braille_precision_description
    PermanentAchievement.SustainedReading -> R.string.achievement_sustained_reading_description
    PermanentAchievement.ConstantMastery -> R.string.achievement_constant_mastery_description
    PermanentAchievement.SuperiorPrecision -> R.string.achievement_superior_precision_description
    PermanentAchievement.DoubleMeaning -> R.string.achievement_double_meaning_description
    PermanentAchievement.BidirectionalReading -> R.string.achievement_bidirectional_reading_description
    PermanentAchievement.HundredExercises -> R.string.achievement_hundred_exercises_description
}

@DrawableRes
internal fun PermanentAchievement.iconResource(): Int? = when (this) {
    PermanentAchievement.FirstStep -> R.drawable.achievement_primer_paso
    PermanentAchievement.Explorer -> R.drawable.achievement_explorador_braille
    PermanentAchievement.Recognizer -> R.drawable.achievement_reconocedor_braille
    PermanentAchievement.Challenger -> R.drawable.achievement_desafiante_braille
    PermanentAchievement.FullAlphabet -> R.drawable.achievement_alfabeto_completo
    PermanentAchievement.Consistency -> R.drawable.achievement_constancia
    PermanentAchievement.WeekInMotion -> R.drawable.achievement_semana_en_movimiento
    PermanentAchievement.ConstantWeek -> R.drawable.achievement_semana_constante
    PermanentAchievement.TwoWeeks -> R.drawable.achievement_dos_semanas
    PermanentAchievement.ConsistencyMonth -> R.drawable.achievement_mes_de_constancia
    PermanentAchievement.SuperiorConsistency -> R.drawable.achievement_constancia_superior
    PermanentAchievement.Bronze -> R.drawable.medal_bronze
    PermanentAchievement.Silver -> R.drawable.medal_silver
    PermanentAchievement.Gold -> R.drawable.medal_gold
    PermanentAchievement.Platinum -> R.drawable.medal_platinum
    PermanentAchievement.Diamond -> R.drawable.medal_diamond
    PermanentAchievement.BrailleSupremacy -> R.drawable.medal_supremacy
    PermanentAchievement.BrailleFocus -> R.drawable.achievement_enfoque_braille
    PermanentAchievement.BrailleRhythm -> R.drawable.achievement_ritmo_braille
    PermanentAchievement.BraillePrecision -> R.drawable.achievement_precision_braille
    PermanentAchievement.SustainedReading -> R.drawable.achievement_lectura_sostenida
    PermanentAchievement.ConstantMastery -> R.drawable.achievement_dominio_constante
    PermanentAchievement.SuperiorPrecision -> R.drawable.achievement_precision_superior
    PermanentAchievement.DoubleMeaning -> R.drawable.achievement_doble_sentido
    PermanentAchievement.BidirectionalReading -> R.drawable.achievement_lectura_bidireccional
    PermanentAchievement.HundredExercises -> null
}


