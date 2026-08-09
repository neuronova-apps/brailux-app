package com.brailuxaprende.ui.identity

data class InstitutionalIdentityConfig(
    val studioName: String = "NeuroNova Apps",
    val brailuxWebsiteUrl: String = "https://neuronova-apps.github.io/brailux-apps/",
    val studioWebsiteUrl: String = "https://neuronova-apps.github.io/",
    val privacyPolicyUrl: String? = null,
    val leadDeveloper: String = "Gabriel Berrospi",
)

object InstitutionalIdentity {
    val current = InstitutionalIdentityConfig()
}
