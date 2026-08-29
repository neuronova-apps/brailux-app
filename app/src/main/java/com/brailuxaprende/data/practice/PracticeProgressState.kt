package com.brailuxaprende.data.practice

import com.brailuxaprende.data.learn.LearningProgress
import com.brailuxaprende.practice.CustomPracticeConfiguration
import com.brailuxaprende.practice.EngagementReward
import com.brailuxaprende.practice.PracticeClock
import com.brailuxaprende.practice.PracticeMode
import com.brailuxaprende.practice.PracticeSessionSummary
import com.brailuxaprende.practice.SystemPracticeClock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PracticeProgressState(
    private val repository: PracticeProgressRepository,
    private val scope: CoroutineScope,
    private val clock: PracticeClock = SystemPracticeClock,
) {
    val progress: StateFlow<PracticeProgress> = repository.progress.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = PracticeProgress(),
    )

    fun recordLevel1Session(
        summary: PracticeSessionSummary,
        learningProgress: LearningProgress = LearningProgress(),
        onRecorded: (EngagementReward?) -> Unit = {},
    ) {
        val practiceDate = clock.today().isoValue
        scope.launch {
            try {
                val record = repository.recordLevel1Session(
                    exercisesCompleted = summary.exercisesCompleted,
                    firstAttemptCorrect = summary.firstAttemptCorrect,
                    errors = summary.errors,
                    practiceDate = practiceDate,
                    mode = summary.mode ?: PracticeMode.SignToCharacter,
                    longestFirstAttemptCorrectStreak = summary.longestFirstAttemptCorrectStreak,
                    exerciseResults = summary.exerciseResults,
                    sessionId = summary.sessionId,
                    learningProgress = learningProgress,
                )
                onRecorded(record.engagementUpdate.reward)
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                onRecorded(null)
            }
        }
    }

    fun recordLevel2Session(
        summary: PracticeSessionSummary,
        learningProgress: LearningProgress = LearningProgress(),
        onRecorded: (EngagementReward?) -> Unit = {},
    ) {
        val practiceDate = clock.today().isoValue
        scope.launch {
            try {
                val record = repository.recordLevel2Session(
                    exercisesCompleted = summary.exercisesCompleted,
                    firstAttemptCorrect = summary.firstAttemptCorrect,
                    errors = summary.errors,
                    hintsUsed = summary.hintsUsed,
                    practiceDate = practiceDate,
                    mode = summary.mode ?: PracticeMode.SignToCharacter,
                    longestFirstAttemptCorrectStreak = summary.longestFirstAttemptCorrectStreak,
                    exerciseResults = summary.exerciseResults,
                    sessionId = summary.sessionId,
                    learningProgress = learningProgress,
                )
                onRecorded(record.engagementUpdate.reward)
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                onRecorded(null)
            }
        }
    }

    fun recordLevel3Session(
        summary: PracticeSessionSummary,
        learningProgress: LearningProgress = LearningProgress(),
        onRecorded: (EngagementReward?) -> Unit = {},
    ) {
        val practiceDate = clock.today().isoValue
        scope.launch {
            try {
                val record = repository.recordLevel3Session(
                    exercisesCompleted = summary.exercisesCompleted,
                    firstAttemptCorrect = summary.firstAttemptCorrect,
                    errors = summary.errors,
                    practiceDate = practiceDate,
                    mode = summary.mode ?: PracticeMode.SignToCharacter,
                    longestFirstAttemptCorrectStreak = summary.longestFirstAttemptCorrectStreak,
                    exerciseResults = summary.exerciseResults,
                    sessionId = summary.sessionId,
                    learningProgress = learningProgress,
                )
                onRecorded(record.engagementUpdate.reward)
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                onRecorded(null)
            }
        }
    }

    fun recordCustomSession(
        summary: PracticeSessionSummary,
        customConfiguration: CustomPracticeConfiguration? = null,
        learningProgress: LearningProgress = LearningProgress(),
        onRecorded: (EngagementReward?) -> Unit = {},
    ) {
        val practiceDate = clock.today().isoValue
        scope.launch {
            try {
                val record = repository.recordCustomSession(
                    exercisesCompleted = summary.exercisesCompleted,
                    firstAttemptCorrect = summary.firstAttemptCorrect,
                    errors = summary.errors,
                    hintsUsed = summary.hintsUsed,
                    practiceDate = practiceDate,
                    mode = summary.mode ?: PracticeMode.SignToCharacter,
                    longestFirstAttemptCorrectStreak = summary.longestFirstAttemptCorrectStreak,
                    exerciseResults = summary.exerciseResults,
                    customConfiguration = customConfiguration,
                    sessionId = summary.sessionId,
                    learningProgress = learningProgress,
                )
                onRecorded(record.engagementUpdate.reward)
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                onRecorded(null)
            }
        }
    }

    fun recordDailySession(
        summary: PracticeSessionSummary,
        learningProgress: LearningProgress = LearningProgress(),
        onRecorded: (EngagementReward?) -> Unit = {},
    ) {
        val practiceDate = clock.today().isoValue
        scope.launch {
            try {
                val record = repository.recordDailySession(
                    exercisesCompleted = summary.exercisesCompleted,
                    firstAttemptCorrect = summary.firstAttemptCorrect,
                    errors = summary.errors,
                    practiceDate = practiceDate,
                    mode = summary.mode ?: PracticeMode.Mixed,
                    longestFirstAttemptCorrectStreak = summary.longestFirstAttemptCorrectStreak,
                    exerciseResults = summary.exerciseResults,
                    sessionId = summary.sessionId,
                    learningProgress = learningProgress,
                )
                onRecorded(record.engagementUpdate.reward)
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                onRecorded(null)
            }
        }
    }

    fun recordDailyChallengeSession(
        summary: PracticeSessionSummary,
        learningProgress: LearningProgress = LearningProgress(),
        onRecorded: (EngagementReward?) -> Unit = {},
    ) {
        val practiceDate = clock.today().isoValue
        scope.launch {
            try {
                val record = repository.recordDailyChallengeSession(
                    exercisesCompleted = summary.exercisesCompleted,
                    firstAttemptCorrect = summary.firstAttemptCorrect,
                    errors = summary.errors,
                    practiceDate = practiceDate,
                    mode = summary.mode ?: PracticeMode.Mixed,
                    longestFirstAttemptCorrectStreak = summary.longestFirstAttemptCorrectStreak,
                    exerciseResults = summary.exerciseResults,
                    sessionId = summary.sessionId,
                    learningProgress = learningProgress,
                )
                onRecorded(record.engagementUpdate.reward)
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                onRecorded(null)
            }
        }
    }
}
