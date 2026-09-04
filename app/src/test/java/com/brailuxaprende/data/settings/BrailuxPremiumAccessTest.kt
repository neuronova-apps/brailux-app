package com.brailuxaprende.data.settings

import com.brailuxaprende.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrailuxPremiumAccessTest {
    @Test
    fun debugBuildEnablesPremiumTesting() {
        val state = BrailuxPremiumAccess.resolveState(isDebug = true)
        assertTrue(state.isPremiumUnlocked)
    }

    @Test
    fun releaseBuildKeepsPremiumLocked() {
        val state = BrailuxPremiumAccess.resolveState(isDebug = false)
        assertFalse(state.isPremiumUnlocked)
    }

    @Test
    fun currentStateMatchesEnvironmentBuildConfig() {
        assertEquals(BuildConfig.DEBUG, BrailuxPremiumAccess.currentState.isPremiumUnlocked)
    }
}
