package com.brailuxaprende.data.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BrailuxBackgroundRotationPolicyTest {
    @Test
    fun rotationRequiresMoreThanOneUnlockedPremiumBackground() {
        assertFalse(BrailuxBackgroundRotationPolicy.canRotate(isPremiumUnlocked = false))
        assertTrue(BrailuxBackgroundRotationPolicy.canRotate(isPremiumUnlocked = true))
    }

    @Test
    fun fixedModeNeverRotates() {
        assertFalse(
            BrailuxBackgroundRotationPolicy.shouldRotate(
                mode = BackgroundRotationMode.Fixed,
                lastRotationAtMillis = 0L,
                nowMillis = Long.MAX_VALUE,
            ),
        )
    }

    @Test
    fun onAppOpenModeRotatesOnForegroundEvaluation() {
        assertTrue(
            BrailuxBackgroundRotationPolicy.shouldRotate(
                mode = BackgroundRotationMode.OnAppOpen,
                lastRotationAtMillis = 0L,
                nowMillis = 1L,
            ),
        )
    }

    @Test
    fun periodicModeWaitsSixHours() {
        val start = 1_000L

        assertFalse(
            BrailuxBackgroundRotationPolicy.shouldRotate(
                mode = BackgroundRotationMode.EverySixHours,
                lastRotationAtMillis = start,
                nowMillis = start +
                    BrailuxBackgroundRotationPolicy.PERIODIC_INTERVAL_MILLIS - 1L,
            ),
        )
        assertTrue(
            BrailuxBackgroundRotationPolicy.shouldRotate(
                mode = BackgroundRotationMode.EverySixHours,
                lastRotationAtMillis = start,
                nowMillis = start +
                    BrailuxBackgroundRotationPolicy.PERIODIC_INTERVAL_MILLIS,
            ),
        )
    }

    @Test
    fun nextPremiumBackgroundCyclesWithoutRepeatingCurrent() {
        val candidates = BrailuxBackgroundRotationPolicy.eligiblePremiumBackgrounds(
            isPremiumUnlocked = true,
        )
        val first = candidates.first()
        val second = candidates[1]
        val last = candidates.last()

        assertEquals(
            second.id,
            BrailuxBackgroundRotationPolicy.nextPremiumBackgroundId(
                currentId = first.id,
                isPremiumUnlocked = true,
            ),
        )
        assertEquals(
            first.id,
            BrailuxBackgroundRotationPolicy.nextPremiumBackgroundId(
                currentId = last.id,
                isPremiumUnlocked = true,
            ),
        )
    }

    @Test
    fun noPremiumAccessReturnsNoNextBackground() {
        assertNull(
            BrailuxBackgroundRotationPolicy.nextPremiumBackgroundId(
                currentId = BrailuxBackgroundCatalog.DEFAULT_ID,
                isPremiumUnlocked = false,
            ),
        )
    }

    @Test
    fun rotationActionsRoundTrip() {
        BackgroundRotationMode.entries.forEach { mode ->
            assertEquals(
                mode,
                BackgroundRotationAction.modeFromAction(
                    BackgroundRotationAction.actionFor(mode),
                ),
            )
        }
    }
}
