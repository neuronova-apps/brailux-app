package com.brailuxaprende.data.practice

import com.brailuxaprende.practice.CustomPracticeConfiguration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CustomPracticePreferencesState(
    private val repository: CustomPracticePreferencesRepository,
    private val scope: CoroutineScope,
) {
    val configuration: StateFlow<CustomPracticeConfiguration> = repository.configuration.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = CustomPracticeConfiguration(),
    )

    fun save(configuration: CustomPracticeConfiguration) {
        scope.launch { repository.save(configuration) }
    }
}
