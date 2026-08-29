package com.brailuxaprende.practice

import java.util.UUID

const val WeeklyPracticeTarget = 5
const val MonthlyExerciseTarget = 100
const val XpPerCompletedExercise = 2
const val DailyMiniAchievementXp = 10

enum class PracticeSessionKind(
    val completionBonusXp: Int,
) {
    Daily(completionBonusXp = 10),
    DailyChallenge(completionBonusXp = 20),
    Level1(completionBonusXp = 10),
    Level2(completionBonusXp = 15),
    Level3(completionBonusXp = 20),
    Custom(completionBonusXp = 10),
}

enum class AchievementFamily {
    LearningPath,
    Consistency,
    BrailleTrajectory,
    Precision,
    MixedMastery,
}

enum class PermanentAchievement(
    val family: AchievementFamily?,
    val threshold: Int,
    val isLegacy: Boolean = false,
) {
    // Familia A: Camino de aprendizaje
    FirstStep(family = AchievementFamily.LearningPath, threshold = 1),
    Explorer(family = AchievementFamily.LearningPath, threshold = 5),
    Recognizer(family = AchievementFamily.LearningPath, threshold = 5),
    Challenger(family = AchievementFamily.LearningPath, threshold = 3),
    FullAlphabet(family = AchievementFamily.LearningPath, threshold = 5),

    // Familia B: Constancia
    Consistency(family = AchievementFamily.Consistency, threshold = 3),
    WeekInMotion(family = AchievementFamily.Consistency, threshold = 5),
    ConstantWeek(family = AchievementFamily.Consistency, threshold = 7),
    TwoWeeks(family = AchievementFamily.Consistency, threshold = 14),
    ConsistencyMonth(family = AchievementFamily.Consistency, threshold = 30),
    SuperiorConsistency(family = AchievementFamily.Consistency, threshold = 60),

    // Familia C: Trayectoria Braille
    Bronze(family = AchievementFamily.BrailleTrajectory, threshold = 25),
    Silver(family = AchievementFamily.BrailleTrajectory, threshold = 75),
    Gold(family = AchievementFamily.BrailleTrajectory, threshold = 125),
    Platinum(family = AchievementFamily.BrailleTrajectory, threshold = 300),
    Diamond(family = AchievementFamily.BrailleTrajectory, threshold = 600),
    BrailleSupremacy(family = AchievementFamily.BrailleTrajectory, threshold = 1200),

    // Familia D: Precisión
    BrailleFocus(family = AchievementFamily.Precision, threshold = 5),
    BrailleRhythm(family = AchievementFamily.Precision, threshold = 10),
    BraillePrecision(family = AchievementFamily.Precision, threshold = 15),
    SustainedReading(family = AchievementFamily.Precision, threshold = 30),
    ConstantMastery(family = AchievementFamily.Precision, threshold = 50),
    SuperiorPrecision(family = AchievementFamily.Precision, threshold = 75),

    // Familia E: Dominio mixto
    DoubleMeaning(family = AchievementFamily.MixedMastery, threshold = 5),
    BidirectionalReading(family = AchievementFamily.MixedMastery, threshold = 15),

    // Logro legacy (mantenido internamente para compatibilidad de esquemas previos)
    HundredExercises(family = null, threshold = 100, isLegacy = true),
    ;

    companion object {
        val activeEntries: List<PermanentAchievement>
            get() = entries.filterNot { it.isLegacy }
    }
}

data class PracticeExerciseResult(
    val firstAttemptCorrect: Boolean,
    val hintUsed: Boolean = false,
)

enum class DailyMiniAchievement(
    val target: Int,
) {
    CompleteFiveExercises(target = 5),
    CompleteSession(target = 1),
    ThreeFirstAttemptCorrect(target = 3),
    TwoModalities(target = 2),
    ;

    companion object {
        fun forDate(date: PracticeDate): DailyMiniAchievement =
            entries[Math.floorMod(date.epochDay, entries.size.toLong()).toInt()]
    }
}

data class DailyMiniAchievementStatus(
    val type: DailyMiniAchievement,
    val progress: Int,
    val completed: Boolean,
) {
    val target: Int
        get() = type.target
}

data class EngagementProgress(
    val totalXp: Long = 0,
    val activityDates: Set<PracticeDate> = emptySet(),
    val lastActivityDate: PracticeDate? = null,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val totalSessions: Int = 0,
    val totalExercises: Long = 0,
    val level1Sessions: Int = 0,
    val level2Sessions: Int = 0,
    val level3Sessions: Int = 0,
    val customSessions: Int = 0,
    val dailyPracticeDates: Set<PracticeDate> = emptySet(),
    val dailyChallengeDates: Set<PracticeDate> = emptySet(),
    val dailyChallengeSessions: Int = 0,
    val recognizerMixedSessions: Int = 0,
    val challengeMixedSessions: Int = 0,
    val currentPrecisionStreak: Int = 0,
    val bestPrecisionStreak: Int = 0,
    val currentMonthKey: String? = null,
    val currentMonthExercises: Int = 0,
    val completedMonthGoals: Set<String> = emptySet(),
    val monthlyExerciseCounts: Map<String, Int> = emptyMap(),
    val unlockedAchievements: Set<PermanentAchievement> = emptySet(),
    val achievementUnlockDates: Map<PermanentAchievement, PracticeDate> = emptyMap(),
    val miniAchievementDate: PracticeDate? = null,
    val miniAchievementType: DailyMiniAchievement? = null,
    val miniAchievementProgress: Int = 0,
    val miniAchievementCompleted: Boolean = false,
    val miniRewardedDates: Set<PracticeDate> = emptySet(),
    val practicedModalitiesToday: Set<PracticeExerciseType> = emptySet(),
) {
    val advancedMixedSessions: Int
        get() = recognizerMixedSessions + challengeMixedSessions

    fun hasPracticed(date: PracticeDate): Boolean = date in activityDates

    fun isDailyPracticeCompleted(date: PracticeDate): Boolean = date in dailyPracticeDates

    fun isDailyChallengeCompleted(date: PracticeDate): Boolean = date in dailyChallengeDates

    fun weeklyPracticeDays(date: PracticeDate): Int = activityDates.count {
        it.weekStart == date.weekStart
    }

    fun displayedStreak(date: PracticeDate): Int = when (lastActivityDate) {
        date, date.plusDays(-1) -> currentStreak
        else -> 0
    }

    fun monthlyExercises(date: PracticeDate): Int =
        monthlyExerciseCounts[date.monthKey]
            ?: if (currentMonthKey == date.monthKey) currentMonthExercises else 0

    fun isMonthlyGoalCompleted(date: PracticeDate): Boolean =
        monthlyExercises(date) >= MonthlyExerciseTarget || date.monthKey in completedMonthGoals

    fun miniAchievement(date: PracticeDate): DailyMiniAchievementStatus {
        val typeForDate = DailyMiniAchievement.forDate(date)
        return if (date in miniRewardedDates) {
            DailyMiniAchievementStatus(typeForDate, typeForDate.target, completed = true)
        } else if (miniAchievementDate == date && miniAchievementType == typeForDate) {
            DailyMiniAchievementStatus(
                type = typeForDate,
                progress = miniAchievementProgress.coerceIn(0, typeForDate.target),
                completed = miniAchievementCompleted,
            )
        } else {
            DailyMiniAchievementStatus(typeForDate, progress = 0, completed = false)
        }
    }
}

data class EngagementSession(
    val kind: PracticeSessionKind,
    val exercisesCompleted: Int,
    val firstAttemptCorrect: Int,
    val errors: Int = 0,
    val hintsUsed: Int = 0,
    val mode: PracticeMode = PracticeMode.SignToCharacter,
    val longestFirstAttemptCorrectStreak: Int = 0,
    val exerciseResults: List<PracticeExerciseResult> = emptyList(),
    val isPrecisionEligible: Boolean = false,
    val id: String = UUID.randomUUID().toString(),
) {
    init {
        require(id.isNotBlank())
        require(exercisesCompleted > 0)
        require(firstAttemptCorrect in 0..exercisesCompleted)
        require(errors >= 0)
        require(hintsUsed >= 0)
        require(longestFirstAttemptCorrectStreak in 0..exercisesCompleted)
        require(longestFirstAttemptCorrectStreak <= firstAttemptCorrect)
        when (kind) {
            PracticeSessionKind.Daily -> require(exercisesCompleted == 5)
            PracticeSessionKind.DailyChallenge -> require(exercisesCompleted == 10)
            PracticeSessionKind.Level1 -> require(exercisesCompleted == 10)
            PracticeSessionKind.Level2 -> require(exercisesCompleted == 15)
            PracticeSessionKind.Level3 -> require(exercisesCompleted == 20)
            PracticeSessionKind.Custom -> require(exercisesCompleted in setOf(10, 15, 20))
        }
    }

    val exerciseTypes: Set<PracticeExerciseType>
        get() = when (mode) {
            PracticeMode.SignToCharacter -> setOf(PracticeExerciseType.SignToCharacter)
            PracticeMode.CharacterToSign -> setOf(PracticeExerciseType.CharacterToSign)
            PracticeMode.Mixed -> PracticeExerciseType.entries.toSet()
        }
}

data class EngagementReward(
    val xpEarned: Int,
    val addedPracticeDay: Boolean,
    val weeklyPracticeDays: Int,
    val currentStreak: Int,
    val miniAchievementCompleted: DailyMiniAchievement?,
    val newlyUnlockedAchievements: Set<PermanentAchievement>,
)

data class EngagementUpdate(
    val progress: EngagementProgress,
    val reward: EngagementReward,
)
