package com.brailuxaprende.ui.identity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InstitutionalIdentityTest {
    @Test
    fun `identity keeps future names and urls undefined`() {
        val identity = InstitutionalIdentity.current

        assertNull(identity.studioName)
        assertNull(identity.brailuxWebsiteUrl)
        assertNull(identity.studioWebsiteUrl)
        assertNull(identity.privacyPolicyUrl)
        assertEquals("Gabriel Berrospi", identity.leadDeveloper)
    }
}
