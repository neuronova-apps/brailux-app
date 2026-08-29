package com.brailuxaprende.practice

object EngagementEngine {
    fun recordSession(
        current: EngagementProgress,
        session: EngagementSession,
        date: PracticeDate,
    ): EngagementUpdate {
        if (
            (session.kind == PracticeSessionKind.Daily && current.isDailyPracticeCompleted(date)) ||
            (session.kind == PracticeSessionKind.DailyChallenge && current.isDailyChallengeCompleted(date))
        ) {
            return EngagementUpdate(
                progress = current,
                reward = EngagementReward(
                    xpEarned = 0,
                    addedPracticeDay = false,
                    weeklyPracticeDays = current.weeklyPracticeDays(date),
                    currentStreak = current.currentStreak,
                    miniAchievementCompleted = null,
                    newlyUnlockedAchievements = emptySet(),
                ),
            )
        }

        val addedPracticeDay = date !in current.activityDates
        val newActivityDates = current.activityDates + date
        val streak = updatedStreak(current, date)
        val dailyPracticeDates = if (session.kind == PracticeSessionKind.Daily) {
            current.dailyPracticeDates + date
        } else {
            current.dailyPracticeDates
        }
        val dailyChallengeDates = if (session.kind == PracticeSessionKind.DailyChallenge) {
            current.dailyChallengeDates + date
        } else {
            current.dailyChallengeDates
        }
        val monthly = updatedMonthlyProgress(current, session.exercisesCompleted, date)
        val mini = updatedMiniAchievement(current, session, date)
        val sessionBonus = session.kind.completionBonusXp
        val miniXp = if (mini.completedNow != null) DailyMiniAchievementXp else 0
        val xpEarned = session.exercisesCompleted * XpPerCompletedExercise + sessionBonus + miniXp

        val beforeAchievements = current.unlockedAchievements
        val baseProgress = current.copy(
            totalXp = current.totalXp + xpEarned,
            activityDates = newActivityDates,
            lastActivityDate = streak.lastActivityDate,
            currentStreak = streak.current,
            bestStreak = maxOf(current.bestStreak, streak.current),
            totalSessions = current.totalSessions + 1,
            totalExercises = current.totalExercises + session.exercisesCompleted,
            level1Sessions = current.level1Sessions + if (
                session.kind == PracticeSessionKind.Level1
            ) 1 else 0,
            level2Sessions = current.level2Sessions + if (
                session.kind == PracticeSessionKind.Level2
            ) 1 else 0,
            level3Sessions = current.level3Sessions + if (
                session.kind == PracticeSessionKind.Level3
            ) 1 else 0,
            customSessions = current.customSessions + if (
                session.kind == PracticeSessionKind.Custom
            ) 1 else 0,
            dailyPracticeDates = dailyPracticeDates,
            dailyChallengeDates = dailyChallengeDates,
            dailyChallengeSessions = current.dailyChallengeSessions + if (
                session.kind == PracticeSessionKind.DailyChallenge
            ) 1 else 0,
            currentMonthKey = monthly.key,
            currentMonthExercises = monthly.exercises,
            completedMonthGoals = monthly.completedGoals,
            monthlyExerciseCounts = monthly.exerciseCounts,
            miniAchievementDate = date,
            miniAchievementType = mini.status.type,
            miniAchievementProgress = mini.status.progress,
            miniAchievementCompleted = mini.status.completed,
            miniRewardedDates = if (mini.completedNow != null) {
                current.miniRewardedDates + date
            } else {
                current.miniRewardedDates
            },
            practicedModalitiesToday = mini.modalities,
        )
        val evaluatedAchievements = evaluateAchievements(baseProgress)
        val progress = baseProgress.copy(
            unlockedAchievements = beforeAchievements + evaluatedAchievements,
        )

        return EngagementUpdate(
            progress = progress,
            reward = EngagementReward(
                xpEarned = xpEarned,
                addedPracticeDay = addedPracticeDay,
                weeklyPracticeDays = progress.weeklyPracticeDays(date),
                currentStreak = progress.currentStreak,
                miniAchievementCompleted = mini.completedNow,
                newlyUnlockedAchievements = evaluatedAchievements - beforeAchievements,
            ),
        )
    }

    private fun updatedStreak(
        current: EngagementProgress,
        date: PracticeDate,
    ): StreakUpdate {
        val lastDate = current.lastActivityDate
        return when {
            lastDate == null -> StreakUpdate(date, current = 1)
            date == lastDate -> StreakUpdate(lastDate, current.currentStreak.coerceAtLeast(1))
            date == lastDate.plusDays(1) -> StreakUpdate(date, current.currentStreak + 1)
            date > lastDate -> StreakUpdate(date, current = 1)
            else -> StreakUpdate(lastDate, current.currentStreak)
        }
    }

    private fun updatedMonthlyProgress(
        current: EngagementProgress,
        completedExercises: Int,
        date: PracticeDate,
    ): MonthlyUpdate {
        val existingCounts = if (
            current.monthlyExerciseCounts.isEmpty() && current.currentMonthKey != null
        ) {
            mapOf(current.currentMonthKey to current.currentMonthExercises)
        } else {
            current.monthlyExerciseCounts
        }
        val exercises = existingCounts.getOrDefault(date.monthKey, 0) + completedExercises
        val exerciseCounts = existingCounts + (date.monthKey to exercises)
        val completedGoals = if (exercises >= MonthlyExerciseTarget) {
            current.completedMonthGoals + date.monthKey
        } else {
            current.completedMonthGoals
        }
        return MonthlyUpdate(
            key = date.monthKey,
            exercises = exercises,
            completedGoals = completedGoals,
            exerciseCounts = exerciseCounts,
        )
    }

    private fun updatedMiniAchievement(
        current: EngagementProgress,
        session: EngagementSession,
        date: PracticeDate,
    ): MiniUpdate {
        val currentStatus = current.miniAchievement(date)
        val previousModalities = if (current.miniAchievementDate == date) {
            current.practicedModalitiesToday
        } else {
            emptySet()
        }
        val modalities = previousModalities + session.exerciseTypes
        val rawProgress = when (currentStatus.type) {
            DailyMiniAchievement.CompleteFiveExercises ->
                currentStatus.progress + session.exercisesCompleted
            DailyMiniAchievement.CompleteSession -> currentStatus.progress + 1
            DailyMiniAchievement.ThreeFirstAttemptCorrect ->
                maxOf(currentStatus.progress, session.longestFirstAttemptCorrectStreak)
            DailyMiniAchievement.TwoModalities -> modalities.size
        }
        val progress = rawProgress.coerceAtMost(currentStatus.target)
        val completed = currentStatus.completed || progress >= currentStatus.target
        return MiniUpdate(
            status = DailyMiniAchievementStatus(currentStatus.type, progress, completed),
            modalities = modalities,
            completedNow = currentStatus.type.takeIf {
                completed && !currentStatus.completed && date !in current.miniRewardedDates
            },
        )
    }

    private fun evaluateAchievements(
        progress: EngagementProgress,
    ): Set<PermanentAchievement> = buildSet {
        if (progress.totalSessions >= 1) add(PermanentAchievement.FirstStep)
        if (progress.activityDates.size >= 3) add(PermanentAchievement.Consistency)
        if (progress.activityDates.groupingBy { it.weekStart }.eachCount().values.any { it >= 5 }) {
            add(PermanentAchievement.WeekInMotion)
        }
        if (progress.level1Sessions >= 5) add(PermanentAchievement.Explorer)
        if (progress.level2Sessions >= 5) add(PermanentAchievement.Recognizer)
        if (progress.level3Sessions >= 3) add(PermanentAchievement.Challenger)
        if (progress.totalExercises >= 100) add(PermanentAchievement.HundredExercises)
    }

    private data class StreakUpdate(
        val lastActivityDate: PracticeDate,
        val current: Int,
    )

    private data class MonthlyUpdate(
        val key: String,
        val exercises: Int,
        val completedGoals: Set<String>,
        val exerciseCounts: Map<String, Int>,
    )

    private data class MiniUpdate(
        val status: DailyMiniAchievementStatus,
        val modalities: Set<PracticeExerciseType>,
        val completedNow: DailyMiniAchievement?,
    )
}
