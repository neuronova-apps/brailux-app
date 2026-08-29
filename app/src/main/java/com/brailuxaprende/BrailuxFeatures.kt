package com.brailuxaprende

/**
 * Central feature flags configuration for Brailux.
 * Acts as the single source of truth for toggling optional or upcoming modules.
 */
object BrailuxFeatures {
    /**
     * Controls availability of the Brailux Assistant feature.
     * When false:
     * - Assistant is hidden from Home UI.
     * - Assistant route is not registered in NavHost.
     * - Firebase AI and App Check are not initialized.
     * - No network requests or background AI services are executed.
     */
    const val ASSISTANT_ENABLED: Boolean = false
}
