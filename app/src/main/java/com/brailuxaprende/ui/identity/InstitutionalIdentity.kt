package com.brailuxaprende.ui.identity

data class InstitutionalIdentityConfig(
    val studioName: String? = null,
    val brailuxWebsiteUrl: String? = null,
    val studioWebsiteUrl: String? = null,
    val privacyPolicyUrl: String? = null,
    val leadDeveloper: String = "Gabriel Berrospi",
)

object InstitutionalIdentity {
    val current = InstitutionalIdentityConfig()
}
