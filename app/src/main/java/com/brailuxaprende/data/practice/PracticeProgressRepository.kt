package com.brailuxaprende.data.practice

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.IOException
import com.brailuxaprende.practice.EngagementSession
import com.brailuxaprende.practice.EngagementUpdate
import com.brailuxaprende.practice.PracticeDate
import com.brailuxaprende.practice.PracticeMode
import com.brailuxaprende.practice.PracticeSessionKind
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

internal const val Level1CompletedSessionsKeyName = "practice_level_1_completed_sessions"
internal const val Level1TotalExercisesKeyName = "practice_level_1_total_exercises"
internal const val Level1FirstAttemptCorrectKeyName = "practice_level_1_first_attempt_correct"
internal const val Level1ErrorsKeyName = "practice_level_1_errors"
internal const val Level1LastPracticeDateKeyName = "practice_level_1_last_practice_date"
internal const val Level2CompletedSessionsKeyName = "practice_level_2_completed_sessions"
internal const val Level2TotalExercisesKeyName = "practice_level_2_total_exercises"
internal const val Level2FirstAttemptCorrectKeyName = "practice_level_2_first_attempt_correct"
internal const val Level2ErrorsKeyName = "practice_level_2_errors"
internal const val Level2HintsUsedKeyName = "practice_level_2_hints_used"
internal const val Level2LastPracticeDateKeyName = "practice_level_2_last_practice_date"
internal const val Level3CompletedSessionsKeyName = "practice_level_3_completed_sessions"
internal const val Level3TotalExercisesKeyName = "practice_level_3_total_exercises"
internal const val Level3FirstAttemptCorrectKeyName = "practice_level_3_first_attempt_correct"
internal const val Level3ErrorsKeyName = "practice_level_3_errors"
internal const val Level3LastPracticeDateKeyName = "practice_level_3_last_practice_date"
internal const val CustomCompletedSessionsKeyName = "practice_custom_completed_sessions"
internal const val CustomTotalExercisesKeyName = "practice_custom_total_exercises"
internal const val CustomFirstAttemptCorrectKeyName = "practice_custom_first_attempt_correct"
internal const val CustomErrorsKeyName = "practice_custom_errors"
internal const val CustomHintsUsedKeyName = "practice_custom_hints_used"
internal const val CustomLastPracticeDateKeyName = "practice_custom_last_practice_date"
internal const val DailyCompletedSessionsKeyName = "practice_daily_completed_sessions"
internal const val DailyTotalExercisesKeyName = "practice_daily_total_exercises"
internal const val DailyFirstAttemptCorrectKeyName = "practice_daily_first_attempt_correct"
internal const val DailyErrorsKeyName = "practice_daily_errors"
internal const val DailyLastPracticeDateKeyName = "practice_daily_last_practice_date"
internal const val DailyChallengeCompletedSessionsKeyName = "practice_daily_challenge_completed_sessions"
internal const val DailyChallengeTotalExercisesKeyName = "practice_daily_challenge_total_exercises"
internal const val DailyChallengeFirstAttemptCorrectKeyName = "practice_daily_challenge_first_attempt_correct"
internal const val DailyChallengeErrorsKeyName = "practice_daily_challenge_errors"
internal const val DailyChallengeLastPracticeDateKeyName = "practice_daily_challenge_last_practice_date"

data class PracticeProgress(
    val level1CompletedSessions: Int = 0,
    val level1TotalExercises: Int = 0,
    val level1FirstAttemptCorrect: Int = 0,
    val level1Errors: Int = 0,
    val level1LastPracticeDate: String? = null,
    val level2CompletedSessions: Int = 0,
    val level2TotalExercises: Int = 0,
    val level2FirstAttemptCorrect: Int = 0,
    val level2Errors: Int = 0,
    val level2HintsUsed: Int = 0,
    val level2LastPracticeDate: String? = null,
    val level3CompletedSessions: Int = 0,
    val level3TotalExercises: Int = 0,
    val level3FirstAttemptCorrect: Int = 0,
    val level3Errors: Int = 0,
    val level3LastPracticeDate: String? = null,
    val customCompletedSessions: Int = 0,
    val customTotalExercises: Int = 0,
    val customFirstAttemptCorrect: Int = 0,
    val customErrors: Int = 0,
    val customHintsUsed: Int = 0,
    val customLastPracticeDate: String? = null,
    val dailyCompletedSessions: Int = 0,
    val dailyTotalExercises: Int = 0,
    val dailyFirstAttemptCorrect: Int = 0,
    val dailyErrors: Int = 0,
    val dailyLastPracticeDate: String? = null,
    val dailyChallengeCompletedSessions: Int = 0,
    val dailyChallengeTotalExercises: Int = 0,
    val dailyChallengeFirstAttemptCorrect: Int = 0,
    val dailyChallengeErrors: Int = 0,
    val dailyChallengeLastPracticeDate: String? = null,
) {
    val level1AccuracyPercentage: Int
        get() = calculateAccuracyPercentage(level1FirstAttemptCorrect, level1TotalExercises)

    val level2AccuracyPercentage: Int
        get() = calculateAccuracyPercentage(level2FirstAttemptCorrect, level2TotalExercises)

    val level3AccuracyPercentage: Int
        get() = calculateAccuracyPercentage(level3FirstAttemptCorrect, level3TotalExercises)

    val customAccuracyPercentage: Int
        get() = calculateAccuracyPercentage(customFirstAttemptCorrect, customTotalExercises)

    val dailyAccuracyPercentage: Int
        get() = calculateAccuracyPercentage(dailyFirstAttemptCorrect, dailyTotalExercises)

    val dailyChallengeAccuracyPercentage: Int
        get() = calculateAccuracyPercentage(dailyChallengeFirstAttemptCorrect, dailyChallengeTotalExercises)
}

internal fun calculateAccuracyPercentage(firstAttemptCorrect: Int, totalExercises: Int): Int {
    if (totalExercises <= 0) return 0

    val safeCorrectAnswers = firstAttemptCorrect.coerceIn(0, totalExercises)
    return (safeCorrectAnswers.toLong() * 100 / totalExercises).toInt()
}

data class PracticeProgressRecord(
    val practiceProgress: PracticeProgress,
    val engagementUpdate: EngagementUpdate,
)

class PracticeProgressRepository(
    private val dataStore: DataStore<Preferences>,
) {
    val progress: Flow<PracticeProgress> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences -> preferences.toPracticeProgress() }

    suspend fun recordLevel1Session(
        exercisesCompleted: Int,
        firstAttemptCorrect: Int,
        errors: Int = 0,
        practiceDate: String,
        mode: PracticeMode = PracticeMode.SignToCharacter,
        longestFirstAttemptCorrectStreak: Int = 0,
        sessionId: String = UUID.randomUUID().toString(),
    ): PracticeProgressRecord {
        require(exercisesCompleted > 0)
        require(firstAttemptCorrect in 0..exercisesCompleted)
        require(errors >= 0)
        require(practiceDate.isNotBlank())
        require(sessionId.isNotBlank())

        var recordedProgress: PracticeProgress? = null
        var engagementUpdate: EngagementUpdate? = null
        dataStore.edit { preferences ->
            val currentProgress = preferences.toPracticeProgress()
            val engagementRecord = preferences.recordEngagement(
                session = EngagementSession(
                    id = sessionId,
                    kind = PracticeSessionKind.Level1,
                    exercisesCompleted = exercisesCompleted,
                    firstAttemptCorrect = firstAttemptCorrect,
                    errors = errors,
                    mode = mode,
                    longestFirstAttemptCorrectStreak = longestFirstAttemptCorrectStreak,
                ),
                date = requireNotNull(PracticeDate.parse(practiceDate)),
            )
            engagementUpdate = engagementRecord.update
            if (!engagementRecord.isNewlyRecorded) {
                recordedProgress = currentProgress
                return@edit
            }
            val updatedProgress = currentProgress.copy(
                level1CompletedSessions = currentProgress.level1CompletedSessions + 1,
                level1TotalExercises = currentProgress.level1TotalExercises + exercisesCompleted,
                level1FirstAttemptCorrect =
                    currentProgress.level1FirstAttemptCorrect + firstAttemptCorrect,
                level1Errors = currentProgress.level1Errors + errors,
                level1LastPracticeDate = practiceDate,
            )
            preferences[Level1CompletedSessionsKey] = updatedProgress.level1CompletedSessions
            preferences[Level1TotalExercisesKey] = updatedProgress.level1TotalExercises
            preferences[Level1FirstAttemptCorrectKey] = updatedProgress.level1FirstAttemptCorrect
            preferences[Level1ErrorsKey] = updatedProgress.level1Errors
            preferences[Level1LastPracticeDateKey] = practiceDate
            recordedProgress = updatedProgress
        }
        return PracticeProgressRecord(
            practiceProgress = checkNotNull(recordedProgress),
            engagementUpdate = checkNotNull(engagementUpdate),
        )
    }

    suspend fun recordLevel1Session(
        exercisesCompleted: Int,
        firstAttemptCorrect: Int,
        practiceDate: String,
        mode: PracticeMode = PracticeMode.SignToCharacter,
        longestFirstAttemptCorrectStreak: Int = 0,
        sessionId: String = UUID.randomUUID().toString(),
    ): PracticeProgressRecord = recordLevel1Session(
        exercisesCompleted = exercisesCompleted,
        firstAttemptCorrect = firstAttemptCorrect,
        errors = 0,
        practiceDate = practiceDate,
        mode = mode,
        longestFirstAttemptCorrectStreak = longestFirstAttemptCorrectStreak,
        sessionId = sessionId,
    )

    suspend fun recordLevel2Session(
        exercisesCompleted: Int,
        firstAttemptCorrect: Int,
        errors: Int,
        hintsUsed: Int,
        practiceDate: String,
        mode: PracticeMode = PracticeMode.SignToCharacter,
        longestFirstAttemptCorrectStreak: Int = 0,
        sessionId: String = UUID.randomUUID().toString(),
    ): PracticeProgressRecord {
        require(exercisesCompleted > 0)
        require(firstAttemptCorrect in 0..exercisesCompleted)
        require(errors >= 0)
        require(hintsUsed in 0..3)
        require(practiceDate.isNotBlank())
        require(sessionId.isNotBlank())

        var recordedProgress: PracticeProgress? = null
        var engagementUpdate: EngagementUpdate? = null
        dataStore.edit { preferences ->
            val currentProgress = preferences.toPracticeProgress()
            val engagementRecord = preferences.recordEngagement(
                session = EngagementSession(
                    id = sessionId,
                    kind = PracticeSessionKind.Level2,
                    exercisesCompleted = exercisesCompleted,
                    firstAttemptCorrect = firstAttemptCorrect,
                    errors = errors,
                    hintsUsed = hintsUsed,
                    mode = mode,
                    longestFirstAttemptCorrectStreak = longestFirstAttemptCorrectStreak,
                ),
                date = requireNotNull(PracticeDate.parse(practiceDate)),
            )
            engagementUpdate = engagementRecord.update
            if (!engagementRecord.isNewlyRecorded) {
                recordedProgress = currentProgress
                return@edit
            }
            val updatedProgress = currentProgress.copy(
                level2CompletedSessions = currentProgress.level2CompletedSessions + 1,
                level2TotalExercises = currentProgress.level2TotalExercises + exercisesCompleted,
                level2FirstAttemptCorrect =
                    currentProgress.level2FirstAttemptCorrect + firstAttemptCorrect,
                level2Errors = currentProgress.level2Errors + errors,
                level2HintsUsed = currentProgress.level2HintsUsed + hintsUsed,
                level2LastPracticeDate = practiceDate,
            )
            preferences[Level2CompletedSessionsKey] = updatedProgress.level2CompletedSessions
            preferences[Level2TotalExercisesKey] = updatedProgress.level2TotalExercises
            preferences[Level2FirstAttemptCorrectKey] = updatedProgress.level2FirstAttemptCorrect
            preferences[Level2ErrorsKey] = updatedProgress.level2Errors
            preferences[Level2HintsUsedKey] = updatedProgress.level2HintsUsed
            preferences[Level2LastPracticeDateKey] = practiceDate
            recordedProgress = updatedProgress
        }
        return PracticeProgressRecord(
            practiceProgress = checkNotNull(recordedProgress),
            engagementUpdate = checkNotNull(engagementUpdate),
        )
    }

    suspend fun recordLevel3Session(
        exercisesCompleted: Int,
        firstAttemptCorrect: Int,
        errors: Int,
        practiceDate: String,
        mode: PracticeMode = PracticeMode.SignToCharacter,
        longestFirstAttemptCorrectStreak: Int = 0,
        sessionId: String = UUID.randomUUID().toString(),
    ): PracticeProgressRecord {
        require(exercisesCompleted > 0)
        require(firstAttemptCorrect in 0..exercisesCompleted)
        require(errors >= 0)
        require(practiceDate.isNotBlank())
        require(sessionId.isNotBlank())

        var recordedProgress: PracticeProgress? = null
        var engagementUpdate: EngagementUpdate? = null
        dataStore.edit { preferences ->
            val currentProgress = preferences.toPracticeProgress()
            val engagementRecord = preferences.recordEngagement(
                session = EngagementSession(
                    id = sessionId,
                    kind = PracticeSessionKind.Level3,
                    exercisesCompleted = exercisesCompleted,
                    firstAttemptCorrect = firstAttemptCorrect,
                    errors = errors,
                    mode = mode,
                    longestFirstAttemptCorrectStreak = longestFirstAttemptCorrectStreak,
                ),
                date = requireNotNull(PracticeDate.parse(practiceDate)),
            )
            engagementUpdate = engagementRecord.update
            if (!engagementRecord.isNewlyRecorded) {
                recordedProgress = currentProgress
                return@edit
            }
            val updatedProgress = currentProgress.copy(
                level3CompletedSessions = currentProgress.level3CompletedSessions + 1,
                level3TotalExercises = currentProgress.level3TotalExercises + exercisesCompleted,
                level3FirstAttemptCorrect =
                    currentProgress.level3FirstAttemptCorrect + firstAttemptCorrect,
                level3Errors = currentProgress.level3Errors + errors,
                level3LastPracticeDate = practiceDate,
            )
            preferences[Level3CompletedSessionsKey] = updatedProgress.level3CompletedSessions
            preferences[Level3TotalExercisesKey] = updatedProgress.level3TotalExercises
            preferences[Level3FirstAttemptCorrectKey] = updatedProgress.level3FirstAttemptCorrect
            preferences[Level3ErrorsKey] = updatedProgress.level3Errors
            preferences[Level3LastPracticeDateKey] = practiceDate
            recordedProgress = updatedProgress
        }
        return PracticeProgressRecord(
            practiceProgress = checkNotNull(recordedProgress),
            engagementUpdate = checkNotNull(engagementUpdate),
        )
    }

    suspend fun recordCustomSession(
        exercisesCompleted: Int,
        firstAttemptCorrect: Int,
        errors: Int,
        hintsUsed: Int,
        practiceDate: String,
        mode: PracticeMode = PracticeMode.SignToCharacter,
        longestFirstAttemptCorrectStreak: Int = 0,
        sessionId: String = UUID.randomUUID().toString(),
    ): PracticeProgressRecord {
        require(exercisesCompleted > 0)
        require(firstAttemptCorrect in 0..exercisesCompleted)
        require(errors >= 0)
        require(hintsUsed >= 0)
        require(practiceDate.isNotBlank())
        require(sessionId.isNotBlank())

        var recordedProgress: PracticeProgress? = null
        var engagementUpdate: EngagementUpdate? = null
        dataStore.edit { preferences ->
            val currentProgress = preferences.toPracticeProgress()
            val engagementRecord = preferences.recordEngagement(
                session = EngagementSession(
                    id = sessionId,
                    kind = PracticeSessionKind.Custom,
                    exercisesCompleted = exercisesCompleted,
                    firstAttemptCorrect = firstAttemptCorrect,
                    errors = errors,
                    hintsUsed = hintsUsed,
                    mode = mode,
                    longestFirstAttemptCorrectStreak = longestFirstAttemptCorrectStreak,
                ),
                date = requireNotNull(PracticeDate.parse(practiceDate)),
            )
            engagementUpdate = engagementRecord.update
            if (!engagementRecord.isNewlyRecorded) {
                recordedProgress = currentProgress
                return@edit
            }
            val updatedProgress = currentProgress.copy(
                customCompletedSessions = currentProgress.customCompletedSessions + 1,
                customTotalExercises = currentProgress.customTotalExercises + exercisesCompleted,
                customFirstAttemptCorrect =
                    currentProgress.customFirstAttemptCorrect + firstAttemptCorrect,
                customErrors = currentProgress.customErrors + errors,
                customHintsUsed = currentProgress.customHintsUsed + hintsUsed,
                customLastPracticeDate = practiceDate,
            )
            preferences[CustomCompletedSessionsKey] = updatedProgress.customCompletedSessions
            preferences[CustomTotalExercisesKey] = updatedProgress.customTotalExercises
            preferences[CustomFirstAttemptCorrectKey] = updatedProgress.customFirstAttemptCorrect
            preferences[CustomErrorsKey] = updatedProgress.customErrors
            preferences[CustomHintsUsedKey] = updatedProgress.customHintsUsed
            preferences[CustomLastPracticeDateKey] = practiceDate
            recordedProgress = updatedProgress
        }
        return PracticeProgressRecord(
            practiceProgress = checkNotNull(recordedProgress),
            engagementUpdate = checkNotNull(engagementUpdate),
        )
    }

    suspend fun recordDailySession(
        exercisesCompleted: Int,
        firstAttemptCorrect: Int,
        errors: Int = 0,
        practiceDate: String,
        mode: PracticeMode = PracticeMode.Mixed,
        longestFirstAttemptCorrectStreak: Int = 0,
        sessionId: String = UUID.randomUUID().toString(),
    ): PracticeProgressRecord {
        require(exercisesCompleted > 0)
        require(firstAttemptCorrect in 0..exercisesCompleted)
        require(errors >= 0)
        require(practiceDate.isNotBlank())
        require(sessionId.isNotBlank())

        val parsedDate = requireNotNull(PracticeDate.parse(practiceDate))
        var recordedProgress: PracticeProgress? = null
        var engagementUpdate: EngagementUpdate? = null
        dataStore.edit { preferences ->
            val currentProgress = preferences.toPracticeProgress()
            val currentEngagement = preferences.toEngagementProgress()
            val wasAlreadyCompletedToday = currentEngagement.isDailyPracticeCompleted(parsedDate)

            val engagementRecord = preferences.recordEngagement(
                session = EngagementSession(
                    id = sessionId,
                    kind = PracticeSessionKind.Daily,
                    exercisesCompleted = exercisesCompleted,
                    firstAttemptCorrect = firstAttemptCorrect,
                    errors = errors,
                    mode = mode,
                    longestFirstAttemptCorrectStreak = longestFirstAttemptCorrectStreak,
                ),
                date = parsedDate,
            )
            engagementUpdate = engagementRecord.update
            if (!engagementRecord.isNewlyRecorded || wasAlreadyCompletedToday) {
                recordedProgress = currentProgress
                return@edit
            }
            val updatedProgress = currentProgress.copy(
                dailyCompletedSessions = currentProgress.dailyCompletedSessions + 1,
                dailyTotalExercises = currentProgress.dailyTotalExercises + exercisesCompleted,
                dailyFirstAttemptCorrect =
                    currentProgress.dailyFirstAttemptCorrect + firstAttemptCorrect,
                dailyErrors = currentProgress.dailyErrors + errors,
                dailyLastPracticeDate = practiceDate,
            )
            preferences[DailyCompletedSessionsKey] = updatedProgress.dailyCompletedSessions
            preferences[DailyTotalExercisesKey] = updatedProgress.dailyTotalExercises
            preferences[DailyFirstAttemptCorrectKey] = updatedProgress.dailyFirstAttemptCorrect
            preferences[DailyErrorsKey] = updatedProgress.dailyErrors
            preferences[DailyLastPracticeDateKey] = practiceDate
            recordedProgress = updatedProgress
        }
        return PracticeProgressRecord(
            practiceProgress = checkNotNull(recordedProgress),
            engagementUpdate = checkNotNull(engagementUpdate),
        )
    }

    suspend fun recordDailyChallengeSession(
        exercisesCompleted: Int,
        firstAttemptCorrect: Int,
        errors: Int = 0,
        practiceDate: String,
        mode: PracticeMode = PracticeMode.Mixed,
        longestFirstAttemptCorrectStreak: Int = 0,
        sessionId: String = UUID.randomUUID().toString(),
    ): PracticeProgressRecord {
        require(exercisesCompleted > 0)
        require(firstAttemptCorrect in 0..exercisesCompleted)
        require(errors >= 0)
        require(practiceDate.isNotBlank())
        require(sessionId.isNotBlank())

        val parsedDate = requireNotNull(PracticeDate.parse(practiceDate))
        var recordedProgress: PracticeProgress? = null
        var engagementUpdate: EngagementUpdate? = null
        dataStore.edit { preferences ->
            val currentProgress = preferences.toPracticeProgress()
            val currentEngagement = preferences.toEngagementProgress()
            val wasAlreadyCompletedToday = currentEngagement.isDailyChallengeCompleted(parsedDate)

            val engagementRecord = preferences.recordEngagement(
                session = EngagementSession(
                    id = sessionId,
                    kind = PracticeSessionKind.DailyChallenge,
                    exercisesCompleted = exercisesCompleted,
                    firstAttemptCorrect = firstAttemptCorrect,
                    errors = errors,
                    mode = mode,
                    longestFirstAttemptCorrectStreak = longestFirstAttemptCorrectStreak,
                ),
                date = parsedDate,
            )
            engagementUpdate = engagementRecord.update
            if (!engagementRecord.isNewlyRecorded || wasAlreadyCompletedToday) {
                recordedProgress = currentProgress
                return@edit
            }
            val updatedProgress = currentProgress.copy(
                dailyChallengeCompletedSessions = currentProgress.dailyChallengeCompletedSessions + 1,
                dailyChallengeTotalExercises = currentProgress.dailyChallengeTotalExercises + exercisesCompleted,
                dailyChallengeFirstAttemptCorrect =
                    currentProgress.dailyChallengeFirstAttemptCorrect + firstAttemptCorrect,
                dailyChallengeErrors = currentProgress.dailyChallengeErrors + errors,
                dailyChallengeLastPracticeDate = practiceDate,
            )
            preferences[DailyChallengeCompletedSessionsKey] = updatedProgress.dailyChallengeCompletedSessions
            preferences[DailyChallengeTotalExercisesKey] = updatedProgress.dailyChallengeTotalExercises
            preferences[DailyChallengeFirstAttemptCorrectKey] = updatedProgress.dailyChallengeFirstAttemptCorrect
            preferences[DailyChallengeErrorsKey] = updatedProgress.dailyChallengeErrors
            preferences[DailyChallengeLastPracticeDateKey] = practiceDate
            recordedProgress = updatedProgress
        }
        return PracticeProgressRecord(
            practiceProgress = checkNotNull(recordedProgress),
            engagementUpdate = checkNotNull(engagementUpdate),
        )
    }

    private fun Preferences.toPracticeProgress(): PracticeProgress = PracticeProgress(
        level1CompletedSessions = this[Level1CompletedSessionsKey] ?: 0,
        level1TotalExercises = this[Level1TotalExercisesKey] ?: 0,
        level1FirstAttemptCorrect = this[Level1FirstAttemptCorrectKey] ?: 0,
        level1Errors = this[Level1ErrorsKey] ?: 0,
        level1LastPracticeDate = this[Level1LastPracticeDateKey],
        level2CompletedSessions = this[Level2CompletedSessionsKey] ?: 0,
        level2TotalExercises = this[Level2TotalExercisesKey] ?: 0,
        level2FirstAttemptCorrect = this[Level2FirstAttemptCorrectKey] ?: 0,
        level2Errors = this[Level2ErrorsKey] ?: 0,
        level2HintsUsed = this[Level2HintsUsedKey] ?: 0,
        level2LastPracticeDate = this[Level2LastPracticeDateKey],
        level3CompletedSessions = this[Level3CompletedSessionsKey] ?: 0,
        level3TotalExercises = this[Level3TotalExercisesKey] ?: 0,
        level3FirstAttemptCorrect = this[Level3FirstAttemptCorrectKey] ?: 0,
        level3Errors = this[Level3ErrorsKey] ?: 0,
        level3LastPracticeDate = this[Level3LastPracticeDateKey],
        customCompletedSessions = this[CustomCompletedSessionsKey] ?: 0,
        customTotalExercises = this[CustomTotalExercisesKey] ?: 0,
        customFirstAttemptCorrect = this[CustomFirstAttemptCorrectKey] ?: 0,
        customErrors = this[CustomErrorsKey] ?: 0,
        customHintsUsed = this[CustomHintsUsedKey] ?: 0,
        customLastPracticeDate = this[CustomLastPracticeDateKey],
        dailyCompletedSessions = this[DailyCompletedSessionsKey] ?: 0,
        dailyTotalExercises = this[DailyTotalExercisesKey] ?: 0,
        dailyFirstAttemptCorrect = this[DailyFirstAttemptCorrectKey] ?: 0,
        dailyErrors = this[DailyErrorsKey] ?: 0,
        dailyLastPracticeDate = this[DailyLastPracticeDateKey],
        dailyChallengeCompletedSessions = this[DailyChallengeCompletedSessionsKey] ?: 0,
        dailyChallengeTotalExercises = this[DailyChallengeTotalExercisesKey] ?: 0,
        dailyChallengeFirstAttemptCorrect = this[DailyChallengeFirstAttemptCorrectKey] ?: 0,
        dailyChallengeErrors = this[DailyChallengeErrorsKey] ?: 0,
        dailyChallengeLastPracticeDate = this[DailyChallengeLastPracticeDateKey],
    )

    private companion object {
        val Level1CompletedSessionsKey = intPreferencesKey(Level1CompletedSessionsKeyName)
        val Level1TotalExercisesKey = intPreferencesKey(Level1TotalExercisesKeyName)
        val Level1FirstAttemptCorrectKey = intPreferencesKey(Level1FirstAttemptCorrectKeyName)
        val Level1ErrorsKey = intPreferencesKey(Level1ErrorsKeyName)
        val Level1LastPracticeDateKey = stringPreferencesKey(Level1LastPracticeDateKeyName)
        val Level2CompletedSessionsKey = intPreferencesKey(Level2CompletedSessionsKeyName)
        val Level2TotalExercisesKey = intPreferencesKey(Level2TotalExercisesKeyName)
        val Level2FirstAttemptCorrectKey = intPreferencesKey(Level2FirstAttemptCorrectKeyName)
        val Level2ErrorsKey = intPreferencesKey(Level2ErrorsKeyName)
        val Level2HintsUsedKey = intPreferencesKey(Level2HintsUsedKeyName)
        val Level2LastPracticeDateKey = stringPreferencesKey(Level2LastPracticeDateKeyName)
        val Level3CompletedSessionsKey = intPreferencesKey(Level3CompletedSessionsKeyName)
        val Level3TotalExercisesKey = intPreferencesKey(Level3TotalExercisesKeyName)
        val Level3FirstAttemptCorrectKey = intPreferencesKey(Level3FirstAttemptCorrectKeyName)
        val Level3ErrorsKey = intPreferencesKey(Level3ErrorsKeyName)
        val Level3LastPracticeDateKey = stringPreferencesKey(Level3LastPracticeDateKeyName)
        val CustomCompletedSessionsKey = intPreferencesKey(CustomCompletedSessionsKeyName)
        val CustomTotalExercisesKey = intPreferencesKey(CustomTotalExercisesKeyName)
        val CustomFirstAttemptCorrectKey = intPreferencesKey(CustomFirstAttemptCorrectKeyName)
        val CustomErrorsKey = intPreferencesKey(CustomErrorsKeyName)
        val CustomHintsUsedKey = intPreferencesKey(CustomHintsUsedKeyName)
        val CustomLastPracticeDateKey = stringPreferencesKey(CustomLastPracticeDateKeyName)
        val DailyCompletedSessionsKey = intPreferencesKey(DailyCompletedSessionsKeyName)
        val DailyTotalExercisesKey = intPreferencesKey(DailyTotalExercisesKeyName)
        val DailyFirstAttemptCorrectKey = intPreferencesKey(DailyFirstAttemptCorrectKeyName)
        val DailyErrorsKey = intPreferencesKey(DailyErrorsKeyName)
        val DailyLastPracticeDateKey = stringPreferencesKey(DailyLastPracticeDateKeyName)
        val DailyChallengeCompletedSessionsKey = intPreferencesKey(DailyChallengeCompletedSessionsKeyName)
        val DailyChallengeTotalExercisesKey = intPreferencesKey(DailyChallengeTotalExercisesKeyName)
        val DailyChallengeFirstAttemptCorrectKey = intPreferencesKey(DailyChallengeFirstAttemptCorrectKeyName)
        val DailyChallengeErrorsKey = intPreferencesKey(DailyChallengeErrorsKeyName)
        val DailyChallengeLastPracticeDateKey = stringPreferencesKey(DailyChallengeLastPracticeDateKeyName)
    }
}
