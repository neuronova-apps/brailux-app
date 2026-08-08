package com.brailuxaprende.data.practice

import com.brailuxaprende.practice.PracticeSessionSummary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class PracticeProgressState(
    private val repository: PracticeProgressRepository,
    private val scope: CoroutineScope,
) {
    fun recordLevel1Session(
        summary: PracticeSessionSummary,
        practicedAt: Date = Date(),
    ) {
        val practiceDate = SimpleDateFormat(DatePattern, Locale.ROOT).format(practicedAt)
        scope.launch {
            repository.recordLevel1Session(
                exercisesCompleted = summary.exercisesCompleted,
                firstAttemptCorrect = summary.firstAttemptCorrect,
                practiceDate = practiceDate,
            )
        }
    }

    private companion object {
        const val DatePattern = "yyyy-MM-dd"
    }
}
