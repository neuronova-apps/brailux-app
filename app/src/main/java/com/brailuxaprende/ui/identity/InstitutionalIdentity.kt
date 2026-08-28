package com.brailuxaprende.ui.identity

data class InstitutionalIdentityConfig(
    val studioName: String = "NeuroNova Apps",
    val studioWebsiteUrl: String = "https://neuronova-apps.github.io/",
    val brailuxWebsiteUrl: String = "https://neuronova-apps.github.io/brailux-app/",
    val privacyPolicyUrl: String = "https://neuronova-apps.github.io/brailux-app/privacy/",
    val repositoryUrl: String = "https://github.com/neuronova-apps/brailux-app",
    val termsUrl: String = "https://neuronova-apps.github.io/terms/",
    val licensesUrl: String = "https://neuronova-apps.github.io/licenses/",
    val supportUrl: String = "https://neuronova-apps.github.io/support/",
    val reportIssueUrl: String = "https://neuronova-apps.github.io/support/#reportar-problema",
    val appsUrl: String = "https://neuronova-apps.github.io/apps/",
    val leadDeveloper: String = "Gabriel Berrospi",
)

object InstitutionalIdentity {
    val current = InstitutionalIdentityConfig()
}
