package com.brailuxaprende.data.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrailuxPremiumAccessTest {
    @Test
    fun debugBuildDoesNotGrantAutomaticPremiumAccess() {
        val state = BrailuxPremiumAccess.resolveState(isDebug = true)
        assertFalse(state.isPremiumUnlocked)
        assertTrue(state.ownedBackgroundIds.isEmpty())
    }

    @Test
    fun releaseBuildDoesNotGrantAutomaticPremiumAccess() {
        val state = BrailuxPremiumAccess.resolveState(isDebug = false)
        assertFalse(state.isPremiumUnlocked)
        assertTrue(state.ownedBackgroundIds.isEmpty())
    }

    @Test
    fun currentStateRemainsSecurelyLockedByDefault() {
        val state = BrailuxPremiumAccess.currentState
        assertFalse(state.isPremiumUnlocked)
        assertTrue(state.ownedBackgroundIds.isEmpty())
        assertFalse(state.isBackgroundUnlocked(BrailuxBackgroundCatalog.CELESTE_GEOMETRICO_ID))
    }

    @Test
    fun explicitTestingGrantsAccessWhenProvided() {
        val globalPremium = BrailuxPremiumAccess.resolveState(isPremiumUnlocked = true)
        assertTrue(globalPremium.isPremiumUnlocked)
        assertTrue(globalPremium.isBackgroundUnlocked(BrailuxBackgroundCatalog.CELESTE_GEOMETRICO_ID))

        val singleOwned = BrailuxPremiumAccess.resolveState(
            ownedBackgroundIds = setOf(BrailuxBackgroundCatalog.SALVIA_TEXTURA_ID),
        )
        assertFalse(singleOwned.isPremiumUnlocked)
        assertTrue(singleOwned.isBackgroundUnlocked(BrailuxBackgroundCatalog.SALVIA_TEXTURA_ID))
        assertFalse(singleOwned.isBackgroundUnlocked(BrailuxBackgroundCatalog.CREMA_ONDAS_ID))
    }
}
