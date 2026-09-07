package com.brailuxaprende.data.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BrailuxPremiumState(
    val isPremiumUnlocked: Boolean = false,
    val ownedBackgroundIds: Set<String> = emptySet(),
) {
    fun isBackgroundUnlocked(backgroundId: String): Boolean =
        isPremiumUnlocked || ownedBackgroundIds.contains(backgroundId)
}

object BrailuxPremiumAccess {
    fun resolveState(
        isDebug: Boolean = false,
        isPremiumUnlocked: Boolean = false,
        ownedBackgroundIds: Set<String> = emptySet(),
    ): BrailuxPremiumState = BrailuxPremiumState(
        isPremiumUnlocked = isPremiumUnlocked,
        ownedBackgroundIds = ownedBackgroundIds,
    )

    private val _state = MutableStateFlow(resolveState())
    val state: StateFlow<BrailuxPremiumState> = _state.asStateFlow()

    val currentState: BrailuxPremiumState
        get() = _state.value

    fun updateOwnedBackgroundIds(ownedBackgroundIds: Set<String>) {
        _state.value = _state.value.copy(ownedBackgroundIds = ownedBackgroundIds)
    }

    fun updateState(newState: BrailuxPremiumState) {
        _state.value = newState
    }

    fun reset() {
        _state.value = resolveState()
    }
}

