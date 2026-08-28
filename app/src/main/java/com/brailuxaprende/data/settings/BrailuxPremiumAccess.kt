package com.brailuxaprende.data.settings

data class BrailuxPremiumState(
    val isPremiumUnlocked: Boolean,
)

object BrailuxPremiumAccess {
    val currentState = BrailuxPremiumState(isPremiumUnlocked = false)
}
