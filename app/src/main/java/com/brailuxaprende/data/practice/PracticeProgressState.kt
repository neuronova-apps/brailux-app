package com.brailuxaprende.data.practice

import com.brailuxaprende.practice.PracticeSessionSummary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PracticeProgressState(
    private val repository: PracticeProgressRepository,
    private val scope: CoroutineScope,
) {
    val progress: StateFlow<PracticeProgress> = repository.progress.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = PracticeProgress(),
    )

    fun recordLevel1Session(
        summary: PracticeSessionSummary,
        practicedAt: Date = Date(),
        onRecorded: (Boolean) -> Unit = {},
    ) {
        val practiceDate = SimpleDateFormat(DatePattern, Locale.ROOT).format(practicedAt)
        scope.launch {
            try {
                val recordedProgress = repository.recordLevel1Session(
                    exercisesCompleted = summary.exercisesCompleted,
                    firstAttemptCorrect = summary.firstAttemptCorrect,
                    practiceDate = practiceDate,
                )
                progress.first { observedProgress -> observedProgress == recordedProgress }
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
        practicedAt: Date = Date(),
        onRecorded: (Boolean) -> Unit = {},
    ) {
        val practiceDate = SimpleDateFormat(DatePattern, Locale.ROOT).format(practicedAt)
        scope.launch {
            try {
                val recordedProgress = repository.recordLevel2Session(
                    exercisesCompleted = summary.exercisesCompleted,
                    firstAttemptCorrect = summary.firstAttemptCorrect,
                    errors = summary.errors,
                    hintsUsed = summary.hintsUsed,
                    practiceDate = practiceDate,
                )
                progress.first { observedProgress -> observedProgress == recordedProgress }
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
        practicedAt: Date = Date(),
        onRecorded: (Boolean) -> Unit = {},
    ) {
        val practiceDate = SimpleDateFormat(DatePattern, Locale.ROOT).format(practicedAt)
        scope.launch {
            try {
                val recordedProgress = repository.recordLevel3Session(
                    exercisesCompleted = summary.exercisesCompleted,
                    firstAttemptCorrect = summary.firstAttemptCorrect,
                    errors = summary.errors,
                    practiceDate = practiceDate,
                )
                progress.first { observedProgress -> observedProgress == recordedProgress }
                onRecorded(true)
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                onRecorded(false)
            }
        }
    }

    private companion object {
        const val DatePattern = "yyyy-MM-dd"
    }
}
