package com.brailuxaprende.data.settings

import com.brailuxaprende.BuildConfig

data class BrailuxPremiumState(
    val isPremiumUnlocked: Boolean,
)

object BrailuxPremiumAccess {
    fun resolveState(isDebug: Boolean = BuildConfig.DEBUG): BrailuxPremiumState =
        BrailuxPremiumState(isPremiumUnlocked = isDebug)

    val currentState: BrailuxPremiumState
        get() = resolveState()
}

