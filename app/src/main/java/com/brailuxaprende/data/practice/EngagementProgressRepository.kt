package com.brailuxaprende.data.practice

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.brailuxaprende.practice.DailyMiniAchievement
import com.brailuxaprende.practice.EngagementEngine
import com.brailuxaprende.practice.EngagementProgress
import com.brailuxaprende.practice.EngagementReward
import com.brailuxaprende.practice.EngagementSession
import com.brailuxaprende.practice.EngagementUpdate
import com.brailuxaprende.practice.PermanentAchievement
import com.brailuxaprende.practice.PracticeDate
import com.brailuxaprende.practice.PracticeExerciseType
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class EngagementProgressRepository(
    private val dataStore: DataStore<Preferences>,
) {
    val progress: Flow<EngagementProgress> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map(Preferences::toEngagementProgress)

    suspend fun recordSession(
        session: EngagementSession,
        date: PracticeDate,
    ): EngagementUpdate {
        var record: EngagementRecordResult? = null
        dataStore.edit { preferences ->
            record = preferences.recordEngagement(session, date)
        }
        return checkNotNull(record).update
    }
}

internal data class EngagementRecordResult(
    val update: EngagementUpdate,
    val isNewlyRecorded: Boolean,
)

internal fun MutablePreferences.recordEngagement(
    session: EngagementSession,
    date: PracticeDate,
): EngagementRecordResult {
    val current = toEngagementProgress()
    if (session.id in this[RecordedSessionIdsKey].orEmpty()) {
        val reward = this[recordedRewardKey(session.id)]
            ?.toEngagementReward()
            ?: EngagementReward(
                xpEarned = 0,
                addedPracticeDay = false,
                weeklyPracticeDays = current.weeklyPracticeDays(date),
                currentStreak = current.currentStreak,
                miniAchievementCompleted = null,
                newlyUnlockedAchievements = emptySet(),
            )
        return EngagementRecordResult(
            update = EngagementUpdate(progress = current, reward = reward),
            isNewlyRecorded = false,
        )
    }

    val update = EngagementEngine.recordSession(current, session, date)
    writeEngagement(update.progress)
    this[RecordedSessionIdsKey] = this[RecordedSessionIdsKey].orEmpty() + session.id
    this[recordedRewardKey(session.id)] = update.reward.toStoredValue()
    return EngagementRecordResult(update = update, isNewlyRecorded = true)
}

private fun EngagementReward.toStoredValue(): String = listOf(
    xpEarned.toString(),
    addedPracticeDay.toString(),
    weeklyPracticeDays.toString(),
    currentStreak.toString(),
    miniAchievementCompleted?.name.orEmpty(),
    newlyUnlockedAchievements.map { it.name }.sorted().joinToString(","),
).joinToString("|")

private fun String.toEngagementReward(): EngagementReward? {
    val parts = split('|', limit = StoredRewardPartCount)
    if (parts.size != StoredRewardPartCount) return null

    val xpEarned = parts[0].toIntOrNull()?.takeIf { it >= 0 } ?: return null
    val addedPracticeDay = when (parts[1]) {
        "true" -> true
        "false" -> false
        else -> return null
    }
    val weeklyPracticeDays = parts[2].toIntOrNull()?.takeIf { it >= 0 } ?: return null
    val currentStreak = parts[3].toIntOrNull()?.takeIf { it >= 0 } ?: return null
    val miniAchievement = parts[4].takeIf(String::isNotEmpty)?.let { storedName ->
        DailyMiniAchievement.entries.firstOrNull { it.name == storedName } ?: return null
    }
    val achievements = if (parts[5].isEmpty()) {
        emptySet()
    } else {
        parts[5].split(',').map { storedName ->
            PermanentAchievement.entries.firstOrNull { it.name == storedName } ?: return null
        }.toSet()
    }
    return EngagementReward(
        xpEarned = xpEarned,
        addedPracticeDay = addedPracticeDay,
        weeklyPracticeDays = weeklyPracticeDays,
        currentStreak = currentStreak,
        miniAchievementCompleted = miniAchievement,
        newlyUnlockedAchievements = achievements,
    )
}

internal fun Preferences.toEngagementProgress(): EngagementProgress {
    if ((this[EngagementSchemaVersionKey] ?: 0) < EngagementSchemaVersion) {
        return migratedLegacyProgress()
    }

    return EngagementProgress(
        totalXp = this[TotalXpKey] ?: 0,
        activityDates = parseDates(this[ActivityDatesKey]),
        lastActivityDate = PracticeDate.parse(this[LastActivityDateKey]),
        currentStreak = this[CurrentStreakKey] ?: 0,
        bestStreak = this[BestStreakKey] ?: 0,
        totalSessions = this[TotalSessionsKey] ?: 0,
        totalExercises = this[TotalExercisesKey] ?: 0,
        level1Sessions = this[EngagementLevel1SessionsKey] ?: 0,
        level2Sessions = this[EngagementLevel2SessionsKey] ?: 0,
        level3Sessions = this[EngagementLevel3SessionsKey] ?: 0,
        customSessions = this[EngagementCustomSessionsKey] ?: 0,
        dailyPracticeDates = parseDates(this[DailyPracticeDatesKey]),
        dailyChallengeDates = parseDates(this[DailyChallengeDatesKey]),
        dailyChallengeSessions = this[EngagementDailyChallengeSessionsKey] ?: 0,
        currentMonthKey = this[CurrentMonthKey]?.takeIf(MonthKeyPattern::matches),
        currentMonthExercises = this[CurrentMonthExercisesKey] ?: 0,
        completedMonthGoals = parseNames(this[CompletedMonthGoalsKey])
            .filter(MonthKeyPattern::matches)
            .toSet(),
        monthlyExerciseCounts = parseMonthlyCounts(this[MonthlyExerciseCountsKey]),
        unlockedAchievements = parseEnumNames<PermanentAchievement>(this[AchievementsKey]),
        miniAchievementDate = PracticeDate.parse(this[MiniDateKey]),
        miniAchievementType = parseEnumName<DailyMiniAchievement>(this[MiniTypeKey]),
        miniAchievementProgress = this[MiniProgressKey] ?: 0,
        miniAchievementCompleted = this[MiniCompletedKey] ?: false,
        miniRewardedDates = parseDates(this[MiniRewardedDatesKey]),
        practicedModalitiesToday = parseEnumNames<PracticeExerciseType>(this[DailyModalitiesKey]),
    )
}

private fun Preferences.migratedLegacyProgress(): EngagementProgress {
    val level1Sessions = intValueNamed(Level1CompletedSessionsKeyName)
    val level2Sessions = intValueNamed(Level2CompletedSessionsKeyName)
    val level3Sessions = intValueNamed(Level3CompletedSessionsKeyName)
    val level1Exercises = intValueNamed(Level1TotalExercisesKeyName)
    val level2Exercises = intValueNamed(Level2TotalExercisesKeyName)
    val level3Exercises = intValueNamed(Level3TotalExercisesKeyName)
    val totalSessions = level1Sessions + level2Sessions + level3Sessions
    val totalExercises = level1Exercises.toLong() +
        level2Exercises.toLong() +
        level3Exercises.toLong()
    val knownDates = setOfNotNull(
        PracticeDate.parse(stringValueNamed(Level1LastPracticeDateKeyName)),
        PracticeDate.parse(stringValueNamed(Level2LastPracticeDateKeyName)),
        PracticeDate.parse(stringValueNamed(Level3LastPracticeDateKeyName)),
    )
    val sortedDates = knownDates.sorted()
    val streakRuns = consecutiveRuns(sortedDates)
    val streak = streakRuns.lastOrNull() ?: 0
    val achievements = buildSet {
        if (totalSessions >= 1) add(PermanentAchievement.FirstStep)
        if (knownDates.size >= 3) add(PermanentAchievement.Consistency)
        if (knownDates.groupingBy { it.weekStart }.eachCount().values.any { it >= 5 }) {
            add(PermanentAchievement.WeekInMotion)
        }
        if (level1Sessions >= 5) add(PermanentAchievement.Explorer)
        if (level2Sessions >= 5) add(PermanentAchievement.Recognizer)
        if (level3Sessions >= 3) add(PermanentAchievement.Challenger)
        if (totalExercises >= 100) add(PermanentAchievement.HundredExercises)
    }
    val xp = totalExercises * 2L +
        level1Sessions * 10L +
        level2Sessions * 15L +
        level3Sessions * 20L

    return EngagementProgress(
        totalXp = xp,
        activityDates = knownDates,
        lastActivityDate = knownDates.maxOrNull(),
        currentStreak = streak,
        bestStreak = streakRuns.maxOrNull() ?: 0,
        totalSessions = totalSessions,
        totalExercises = totalExercises,
        level1Sessions = level1Sessions,
        level2Sessions = level2Sessions,
        level3Sessions = level3Sessions,
        unlockedAchievements = achievements,
    )
}

private fun MutablePreferences.writeEngagement(progress: EngagementProgress) {
    this[EngagementSchemaVersionKey] = EngagementSchemaVersion
    this[TotalXpKey] = progress.totalXp
    this[ActivityDatesKey] = progress.activityDates.toStoredDates()
    progress.lastActivityDate?.let { this[LastActivityDateKey] = it.isoValue }
    this[CurrentStreakKey] = progress.currentStreak
    this[BestStreakKey] = progress.bestStreak
    this[TotalSessionsKey] = progress.totalSessions
    this[TotalExercisesKey] = progress.totalExercises
    this[EngagementLevel1SessionsKey] = progress.level1Sessions
    this[EngagementLevel2SessionsKey] = progress.level2Sessions
    this[EngagementLevel3SessionsKey] = progress.level3Sessions
    this[EngagementCustomSessionsKey] = progress.customSessions
    this[DailyPracticeDatesKey] = progress.dailyPracticeDates.toStoredDates()
    this[DailyChallengeDatesKey] = progress.dailyChallengeDates.toStoredDates()
    this[EngagementDailyChallengeSessionsKey] = progress.dailyChallengeSessions
    progress.currentMonthKey?.let { this[CurrentMonthKey] = it }
    this[CurrentMonthExercisesKey] = progress.currentMonthExercises
    this[CompletedMonthGoalsKey] = progress.completedMonthGoals.sorted().joinToString(",")
    this[MonthlyExerciseCountsKey] = progress.monthlyExerciseCounts
        .toSortedMap()
        .entries
        .joinToString(";") { (month, exercises) -> "$month=$exercises" }
    this[AchievementsKey] = progress.unlockedAchievements
        .map { it.name }
        .sorted()
        .joinToString(",")
    progress.miniAchievementDate?.let { this[MiniDateKey] = it.isoValue }
    progress.miniAchievementType?.let { this[MiniTypeKey] = it.name }
    this[MiniProgressKey] = progress.miniAchievementProgress
    this[MiniCompletedKey] = progress.miniAchievementCompleted
    this[MiniRewardedDatesKey] = progress.miniRewardedDates.toStoredDates()
    this[DailyModalitiesKey] = progress.practicedModalitiesToday
        .map { it.name }
        .sorted()
        .joinToString(",")
}

private fun Set<PracticeDate>.toStoredDates(): String =
    sorted().joinToString(",") { it.isoValue }

private fun parseDates(value: String?): Set<PracticeDate> =
    parseNames(value).mapNotNull(PracticeDate::parse).toSet()

private fun parseNames(value: String?): Set<String> = value
    .orEmpty()
    .split(',')
    .filter(String::isNotBlank)
    .toSet()

private fun parseMonthlyCounts(value: String?): Map<String, Int> = value
    .orEmpty()
    .split(';')
    .mapNotNull { entry ->
        val parts = entry.split('=', limit = 2)
        val month = parts.getOrNull(0)?.takeIf(MonthKeyPattern::matches) ?: return@mapNotNull null
        val exercises = parts.getOrNull(1)?.toIntOrNull()?.takeIf { it >= 0 }
            ?: return@mapNotNull null
        month to exercises
    }
    .toMap()

private fun consecutiveRuns(dates: List<PracticeDate>): List<Int> {
    if (dates.isEmpty()) return emptyList()
    val runs = mutableListOf<Int>()
    var currentRun = 1
    for (index in 1..dates.lastIndex) {
        if (dates[index] == dates[index - 1].plusDays(1)) {
            currentRun += 1
        } else {
            runs += currentRun
            currentRun = 1
        }
    }
    runs += currentRun
    return runs
}

private fun Preferences.intValueNamed(name: String): Int =
    (asMap().entries.firstOrNull { (key, _) -> key.name == name }?.value as? Int) ?: 0

private fun Preferences.stringValueNamed(name: String): String? =
    asMap().entries.firstOrNull { (key, _) -> key.name == name }?.value as? String

private inline fun <reified T : Enum<T>> parseEnumName(value: String?): T? =
    enumValues<T>().firstOrNull { it.name == value }

private inline fun <reified T : Enum<T>> parseEnumNames(value: String?): Set<T> =
    parseNames(value).mapNotNull { name -> enumValues<T>().firstOrNull { it.name == name } }.toSet()

private const val EngagementSchemaVersion = 1
private val EngagementSchemaVersionKey = intPreferencesKey("engagement_schema_version")
private val TotalXpKey = longPreferencesKey("engagement_total_xp")
private val ActivityDatesKey = stringPreferencesKey("engagement_activity_dates")
private val LastActivityDateKey = stringPreferencesKey("engagement_last_activity_date")
private val CurrentStreakKey = intPreferencesKey("engagement_current_streak")
private val BestStreakKey = intPreferencesKey("engagement_best_streak")
private val TotalSessionsKey = intPreferencesKey("engagement_total_sessions")
private val TotalExercisesKey = longPreferencesKey("engagement_total_exercises")
private val EngagementLevel1SessionsKey = intPreferencesKey("engagement_level_1_sessions")
private val EngagementLevel2SessionsKey = intPreferencesKey("engagement_level_2_sessions")
private val EngagementLevel3SessionsKey = intPreferencesKey("engagement_level_3_sessions")
private val EngagementCustomSessionsKey = intPreferencesKey("engagement_custom_sessions")
private val DailyPracticeDatesKey = stringPreferencesKey("engagement_daily_practice_dates")
private val DailyChallengeDatesKey = stringPreferencesKey("engagement_daily_challenge_dates")
private val EngagementDailyChallengeSessionsKey = intPreferencesKey("engagement_daily_challenge_sessions")
private val CurrentMonthKey = stringPreferencesKey("engagement_current_month")
private val CurrentMonthExercisesKey = intPreferencesKey("engagement_current_month_exercises")
private val CompletedMonthGoalsKey = stringPreferencesKey("engagement_completed_month_goals")
private val MonthlyExerciseCountsKey = stringPreferencesKey("engagement_monthly_exercise_counts")
private val AchievementsKey = stringPreferencesKey("engagement_achievements")
private val MiniDateKey = stringPreferencesKey("engagement_mini_date")
private val MiniTypeKey = stringPreferencesKey("engagement_mini_type")
private val MiniProgressKey = intPreferencesKey("engagement_mini_progress")
private val MiniCompletedKey = booleanPreferencesKey("engagement_mini_completed")
private val MiniRewardedDatesKey = stringPreferencesKey("engagement_mini_rewarded_dates")
private val DailyModalitiesKey = stringPreferencesKey("engagement_daily_modalities")
private val RecordedSessionIdsKey = stringSetPreferencesKey("engagement_recorded_session_ids")
private val MonthKeyPattern = Regex("\\d{4}-(0[1-9]|1[0-2])")
private const val RecordedRewardKeyPrefix = "engagement_recorded_session_reward_"
private const val StoredRewardPartCount = 6

private fun recordedRewardKey(sessionId: String) =
    stringPreferencesKey(RecordedRewardKeyPrefix + sessionId)
