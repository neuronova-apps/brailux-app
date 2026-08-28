package com.brailuxaprende.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.ArrayRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.brailuxaprende.BuildConfig
import com.brailuxaprende.R
import com.brailuxaprende.ui.components.BrailuxScreenHeader
import com.brailuxaprende.ui.components.BrailuxSectionCard
import com.brailuxaprende.ui.identity.InstitutionalIdentity
import com.brailuxaprende.ui.identity.InstitutionalIdentityConfig

@Composable
fun AboutScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    identity: InstitutionalIdentityConfig = InstitutionalIdentity.current,
    onOpenWebsite: ((String) -> Unit)? = null,
) {
    val context = LocalContext.current
    val openWebsite = onOpenWebsite ?: { url: String -> openHttpsWebsite(context, url) }
    val screenTitle = stringResource(R.string.about_title)

    Surface(
        modifier = modifier
            .fillMaxSize()
            .semantics { paneTitle = screenTitle },
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BrailuxScreenHeader(title = screenTitle, onBack = onBack)
            Spacer(modifier = Modifier.height(22.dp))
            AboutHero(identity = identity)
            Spacer(modifier = Modifier.height(22.dp))
            AboutTextCard(
                title = stringResource(R.string.about_section_brailux),
                body = stringResource(R.string.about_brailux_description),
            )
            AboutSectionSpacer()
            AboutTextCard(
                title = stringResource(R.string.about_section_purpose),
                body = stringResource(R.string.about_purpose_description),
            )
            AboutSectionSpacer()
            AboutListCard(
                title = stringResource(R.string.about_section_features),
                itemsResource = R.array.about_features,
            )
            AboutSectionSpacer()
            AboutListCard(
                title = stringResource(R.string.about_section_accessibility),
                itemsResource = R.array.about_accessibility_features,
            )
            AboutSectionSpacer()
            AboutLinkCard(
                title = stringResource(R.string.about_section_privacy),
                description = stringResource(R.string.about_privacy_description),
                links = listOf(
                    AboutLink(
                        title = stringResource(R.string.about_privacy_policy),
                        url = identity.privacyPolicyUrl,
                    ),
                ),
                onOpenWebsite = openWebsite,
            )
            AboutSectionSpacer()
            AboutLinkCard(
                title = stringResource(R.string.about_section_terms),
                links = listOf(
                    AboutLink(
                        title = stringResource(R.string.about_terms),
                        url = identity.termsUrl,
                    ),
                ),
                onOpenWebsite = openWebsite,
            )
            AboutSectionSpacer()
            AboutLinkCard(
                title = stringResource(R.string.about_section_licenses),
                links = listOf(
                    AboutLink(
                        title = stringResource(R.string.about_licenses),
                        url = identity.licensesUrl,
                    ),
                ),
                onOpenWebsite = openWebsite,
            )
            AboutSectionSpacer()
            AboutLinkCard(
                title = stringResource(R.string.about_section_support),
                links = listOf(
                    AboutLink(
                        title = stringResource(R.string.about_contact_support),
                        url = identity.supportUrl,
                    ),
                    AboutLink(
                        title = stringResource(R.string.about_report_issue),
                        url = identity.reportIssueUrl,
                    ),
                ),
                onOpenWebsite = openWebsite,
            )
            AboutSectionSpacer()
            AboutLinkCard(
                title = stringResource(R.string.about_section_neuronova),
                links = listOf(
                    AboutLink(
                        title = stringResource(R.string.about_more_apps),
                        url = identity.appsUrl,
                    ),
                    AboutLink(
                        title = stringResource(R.string.about_official_website),
                        url = identity.studioWebsiteUrl,
                    ),
                ),
                onOpenWebsite = openWebsite,
            )
            AboutSectionSpacer()
            AboutLinkCard(
                title = stringResource(R.string.about_section_online),
                links = listOf(
                    AboutLink(
                        title = stringResource(R.string.about_brailux_website),
                        url = identity.brailuxWebsiteUrl,
                    ),
                    AboutLink(
                        title = stringResource(R.string.about_repository),
                        url = identity.repositoryUrl,
                        accessibilityLabel = stringResource(
                            R.string.about_repository_accessibility,
                        ),
                    ),
                ),
                onOpenWebsite = openWebsite,
            )
            AboutSectionSpacer()
            AppInformationCard(identity = identity)
            AboutSectionSpacer()
            CreditsCard(identity = identity)
            Text(
                text = stringResource(R.string.about_copyright),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
                    .padding(horizontal = 12.dp, vertical = 24.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun AboutHero(identity: InstitutionalIdentityConfig) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 640.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(R.drawable.brailux_logo),
            contentDescription = stringResource(R.string.brailux_logo_description),
            modifier = Modifier.size(116.dp),
        )
        Text(
            text = stringResource(R.string.about_app_name),
            modifier = Modifier
                .padding(top = 14.dp)
                .semantics { heading() },
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.about_subtitle),
            modifier = Modifier.padding(top = 6.dp),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
            modifier = Modifier.padding(top = 10.dp),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = identity.studioName,
            modifier = Modifier.padding(top = 3.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun AboutTextCard(
    title: String,
    body: String,
) {
    AboutCard(title = title) {
        Text(
            text = body,
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AboutListCard(
    title: String,
    @ArrayRes itemsResource: Int,
) {
    val items = stringArrayResource(itemsResource)
    AboutCard(title = title) {
        Column(
            modifier = Modifier.padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items.forEach { item ->
                Text(
                    text = stringResource(R.string.about_list_item, item),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private data class AboutLink(
    val title: String,
    val url: String,
    val accessibilityLabel: String? = null,
)

@Composable
private fun AboutLinkCard(
    title: String,
    links: List<AboutLink>,
    onOpenWebsite: (String) -> Unit,
    description: String? = null,
) {
    AboutCard(title = title) {
        description?.let {
            Text(
                text = it,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        links.forEachIndexed { index, link ->
            if (index > 0) HorizontalDivider()
            AboutLinkButton(link = link, onOpenWebsite = onOpenWebsite)
        }
    }
}

@Composable
private fun AboutLinkButton(
    link: AboutLink,
    onOpenWebsite: (String) -> Unit,
) {
    val accessibilityLabel = link.accessibilityLabel
        ?: stringResource(R.string.about_external_link_accessibility, link.title)
    TextButton(
        onClick = { onOpenWebsite(link.url) },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .semantics { contentDescription = accessibilityLabel },
    ) {
        Text(
            text = link.title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Start,
        )
        Icon(
            painter = painterResource(R.drawable.ic_open_in_new),
            contentDescription = null,
            modifier = Modifier
                .padding(start = 12.dp)
                .size(22.dp),
        )
    }
}

@Composable
private fun AppInformationCard(identity: InstitutionalIdentityConfig) {
    AboutCard(title = stringResource(R.string.about_section_app_information)) {
        Column(
            modifier = Modifier.padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(R.string.about_app_info_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = stringResource(R.string.about_app_info_build, BuildConfig.VERSION_CODE),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = stringResource(R.string.about_app_info_developer, identity.studioName),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun CreditsCard(identity: InstitutionalIdentityConfig) {
    AboutCard(title = stringResource(R.string.about_section_credits)) {
        Column(
            modifier = Modifier.padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(R.string.about_developed_by, identity.studioName),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = stringResource(R.string.about_created_by, identity.leadDeveloper),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun AboutCard(
    title: String,
    content: @Composable () -> Unit,
) {
    BrailuxSectionCard(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 640.dp),
    ) {
        Text(
            text = title,
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        content()
    }
}

@Composable
private fun AboutSectionSpacer() {
    Spacer(modifier = Modifier.height(16.dp))
}

private fun openHttpsWebsite(context: Context, url: String) {
    val uri = Uri.parse(url)
    if (uri.scheme != "https") return
    val intent = Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE)
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // The access remains available when the device has an HTTPS browser.
    }
}
