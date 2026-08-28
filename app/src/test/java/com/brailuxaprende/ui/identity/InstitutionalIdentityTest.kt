package com.brailuxaprende.ui.identity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InstitutionalIdentityTest {
    @Test
    fun `identity exposes definitive NeuroNova and Brailux data`() {
        val identity = InstitutionalIdentity.current

        assertEquals("NeuroNova Apps", identity.studioName)
        assertEquals("https://neuronova-apps.github.io/", identity.studioWebsiteUrl)
        assertEquals(
            "https://neuronova-apps.github.io/brailux-app/",
            identity.brailuxWebsiteUrl,
        )
        assertEquals(
            "https://neuronova-apps.github.io/brailux-app/privacy/",
            identity.privacyPolicyUrl,
        )
        assertEquals("https://github.com/neuronova-apps/brailux-app", identity.repositoryUrl)
        assertEquals("https://neuronova-apps.github.io/terms/", identity.termsUrl)
        assertEquals("https://neuronova-apps.github.io/licenses/", identity.licensesUrl)
        assertEquals("https://neuronova-apps.github.io/support/", identity.supportUrl)
        assertEquals(
            "https://neuronova-apps.github.io/support/#reportar-problema",
            identity.reportIssueUrl,
        )
        assertEquals("https://neuronova-apps.github.io/apps/", identity.appsUrl)
        assertEquals("Gabriel Berrospi", identity.leadDeveloper)

        listOf(
            identity.studioWebsiteUrl,
            identity.brailuxWebsiteUrl,
            identity.privacyPolicyUrl,
            identity.repositoryUrl,
            identity.termsUrl,
            identity.licensesUrl,
            identity.supportUrl,
            identity.reportIssueUrl,
            identity.appsUrl,
        ).forEach { url ->
            assertTrue(url.startsWith("https://"))
        }
    }
}
