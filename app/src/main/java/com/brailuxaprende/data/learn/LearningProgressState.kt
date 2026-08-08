package com.brailuxaprende.data.learn

import com.brailuxaprende.learning.LearningLesson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LearningProgressState(
    private val repository: LearningProgressRepository,
    private val scope: CoroutineScope,
) {
    val progress: StateFlow<LearningProgress> = repository.progress.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = LearningProgress(),
    )

    fun markCompleted(lesson: LearningLesson) {
        scope.launch { repository.markCompleted(lesson) }
    }
}
