package com.brailuxaprende.data.settings

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

    val currentState: BrailuxPremiumState = resolveState()
}

