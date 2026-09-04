package com.brailuxaprende.data.settings

enum class BackgroundRotationMode(
    internal val storedValue: String,
) {
    Fixed(storedValue = "fixed"),
    OnAppOpen(storedValue = "on_app_open"),
    EverySixHours(storedValue = "every_six_hours"),
    ;

    companion object {
        fun fromStoredValue(value: String?): BackgroundRotationMode =
            entries.firstOrNull { it.storedValue == value } ?: Fixed
    }
}

object BackgroundRotationAction {
    private const val Prefix = "__background_rotation__:"

    fun actionFor(mode: BackgroundRotationMode): String = Prefix + mode.storedValue

    fun modeFromAction(action: String): BackgroundRotationMode? {
        if (!action.startsWith(Prefix)) return null
        return BackgroundRotationMode.entries.firstOrNull {
            it.storedValue == action.removePrefix(Prefix)
        }
    }
}

object BrailuxBackgroundRotationPolicy {
    const val PERIODIC_INTERVAL_MILLIS: Long = 6L * 60L * 60L * 1_000L

    fun eligiblePremiumBackgrounds(
        isPremiumUnlocked: Boolean,
    ): List<BrailuxBackgroundOption> {
        if (!isPremiumUnlocked) return emptyList()
        return BrailuxBackgroundCatalog.backgrounds.filter { background ->
            background.premium && background.available
        }
    }

    fun canRotate(isPremiumUnlocked: Boolean): Boolean =
        eligiblePremiumBackgrounds(isPremiumUnlocked).size > 1

    fun shouldRotate(
        mode: BackgroundRotationMode,
        lastRotationAtMillis: Long,
        nowMillis: Long,
    ): Boolean = when (mode) {
        BackgroundRotationMode.Fixed -> false
        BackgroundRotationMode.OnAppOpen -> true
        BackgroundRotationMode.EverySixHours ->
            lastRotationAtMillis > 0L &&
                nowMillis - lastRotationAtMillis >= PERIODIC_INTERVAL_MILLIS
    }

    fun nextPremiumBackgroundId(
        currentId: String?,
        isPremiumUnlocked: Boolean,
    ): String? {
        val candidates = eligiblePremiumBackgrounds(isPremiumUnlocked)
        if (candidates.size < 2) return null

        val currentIndex = candidates.indexOfFirst { it.id == currentId }
        return if (currentIndex < 0) {
            candidates.first().id
        } else {
            candidates[(currentIndex + 1) % candidates.size].id
        }
    }
}
