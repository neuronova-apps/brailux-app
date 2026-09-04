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
        isPremiumUnlocked: Boolean = false,
        ownedBackgroundIds: Set<String> = emptySet(),
    ): List<BrailuxBackgroundOption> {
        return BrailuxBackgroundCatalog.backgrounds.filter { background ->
            background.premium && background.available &&
                (isPremiumUnlocked || background.id in ownedBackgroundIds)
        }
    }

    fun canRotate(
        isPremiumUnlocked: Boolean = false,
        ownedBackgroundIds: Set<String> = emptySet(),
    ): Boolean =
        eligiblePremiumBackgrounds(isPremiumUnlocked, ownedBackgroundIds).size > 1

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
        isPremiumUnlocked: Boolean = false,
        ownedBackgroundIds: Set<String> = emptySet(),
    ): String? {
        val candidates = eligiblePremiumBackgrounds(isPremiumUnlocked, ownedBackgroundIds)
        if (candidates.size < 2) return null

        val currentIndex = candidates.indexOfFirst { it.id == currentId }
        return if (currentIndex < 0) {
            candidates.first().id
        } else {
            candidates[(currentIndex + 1) % candidates.size].id
        }
    }
}

object BrailuxBackgroundRotationLifecyclePolicy {
    internal var skipNextBackgroundRotation: Boolean = false

    fun handleStop(isChangingConfigurations: Boolean) {
        if (isChangingConfigurations) {
            skipNextBackgroundRotation = true
        }
    }

    fun shouldSkipRotationOnStart(): Boolean {
        if (skipNextBackgroundRotation) {
            skipNextBackgroundRotation = false
            return true
        }
        return false
    }

    fun reset() {
        skipNextBackgroundRotation = false
    }
}

