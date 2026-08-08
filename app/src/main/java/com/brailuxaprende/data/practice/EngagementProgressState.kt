package com.brailuxaprende.data.practice

import com.brailuxaprende.practice.EngagementProgress
import com.brailuxaprende.practice.EngagementReward
import com.brailuxaprende.practice.EngagementSession
import com.brailuxaprende.practice.PracticeClock
import com.brailuxaprende.practice.PracticeMode
import com.brailuxaprende.practice.PracticeSessionKind
import com.brailuxaprende.practice.PracticeSessionSummary
import com.brailuxaprende.practice.SystemPracticeClock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EngagementProgressState(
    private val repository: EngagementProgressRepository,
    private val scope: CoroutineScope,
    private val clock: PracticeClock = SystemPracticeClock,
) {
    val progress: StateFlow<EngagementProgress> = repository.progress.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = EngagementProgress(),
    )

    fun recordSession(
        summary: PracticeSessionSummary,
        kind: PracticeSessionKind,
        onRecorded: (EngagementReward?) -> Unit = {},
    ) {
        scope.launch {
            try {
                val update = repository.recordSession(
                    session = summary.toEngagementSession(kind),
                    date = clock.today(),
                )
                onRecorded(update.reward)
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                onRecorded(null)
            }
        }
    }
}

internal fun PracticeSessionSummary.toEngagementSession(
    kind: PracticeSessionKind,
): EngagementSession = EngagementSession(
    id = sessionId,
    kind = kind,
    exercisesCompleted = exercisesCompleted,
    firstAttemptCorrect = firstAttemptCorrect,
    errors = errors,
    hintsUsed = hintsUsed,
    mode = mode ?: PracticeMode.SignToCharacter,
    longestFirstAttemptCorrectStreak = longestFirstAttemptCorrectStreak,
)
