package com.brailuxaprende.ui.identity

import org.junit.Assert.assertEquals
import org.junit.Test

class InstitutionalIdentityTest {
    @Test
    fun `identity exposes definitive NeuroNova and Brailux data`() {
        val identity = InstitutionalIdentity.current

        assertEquals("NeuroNova Apps", identity.studioName)
        assertEquals(
            "https://neuronova-apps.github.io/brailux-apps/",
            identity.brailuxWebsiteUrl,
        )
        assertEquals("https://neuronova-apps.github.io/", identity.studioWebsiteUrl)
        assertEquals(
            "https://neuronova-apps.github.io/brailux-apps/privacy/",
            identity.privacyPolicyUrl,
        )
        assertEquals("Gabriel Berrospi", identity.leadDeveloper)
    }
}
