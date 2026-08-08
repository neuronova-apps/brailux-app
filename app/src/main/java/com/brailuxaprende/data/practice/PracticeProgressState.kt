package com.brailuxaprende.data.practice

import com.brailuxaprende.practice.PracticeSessionSummary
import com.brailuxaprende.practice.PracticeClock
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
        onRecorded: (Boolean) -> Unit = {},
    ) {
        val practiceDate = clock.today().isoValue
        scope.launch {
            try {
                repository.recordLevel1Session(
                    exercisesCompleted = summary.exercisesCompleted,
                    firstAttemptCorrect = summary.firstAttemptCorrect,
                    practiceDate = practiceDate,
                    mode = summary.mode ?: com.brailuxaprende.practice.PracticeMode.SignToCharacter,
                    longestFirstAttemptCorrectStreak = summary.longestFirstAttemptCorrectStreak,
                    sessionId = summary.sessionId,
                )
                onRecorded(true)
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                onRecorded(false)
            }
        }
    }

    fun recordLevel2Session(
        summary: PracticeSessionSummary,
        onRecorded: (Boolean) -> Unit = {},
    ) {
        val practiceDate = clock.today().isoValue
        scope.launch {
            try {
                repository.recordLevel2Session(
                    exercisesCompleted = summary.exercisesCompleted,
                    firstAttemptCorrect = summary.firstAttemptCorrect,
                    errors = summary.errors,
                    hintsUsed = summary.hintsUsed,
                    practiceDate = practiceDate,
                    mode = summary.mode ?: com.brailuxaprende.practice.PracticeMode.SignToCharacter,
                    longestFirstAttemptCorrectStreak = summary.longestFirstAttemptCorrectStreak,
                    sessionId = summary.sessionId,
                )
                onRecorded(true)
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                onRecorded(false)
            }
        }
    }

    fun recordLevel3Session(
        summary: PracticeSessionSummary,
        onRecorded: (Boolean) -> Unit = {},
    ) {
        val practiceDate = clock.today().isoValue
        scope.launch {
            try {
                repository.recordLevel3Session(
                    exercisesCompleted = summary.exercisesCompleted,
                    firstAttemptCorrect = summary.firstAttemptCorrect,
                    errors = summary.errors,
                    practiceDate = practiceDate,
                    mode = summary.mode ?: com.brailuxaprende.practice.PracticeMode.SignToCharacter,
                    longestFirstAttemptCorrectStreak = summary.longestFirstAttemptCorrectStreak,
                    sessionId = summary.sessionId,
                )
                onRecorded(true)
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                onRecorded(false)
            }
        }
    }
}
