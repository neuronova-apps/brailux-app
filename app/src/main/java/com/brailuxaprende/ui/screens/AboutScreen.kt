package com.brailuxaprende.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BrailuxScreenHeader(
                title = stringResource(R.string.about_title),
                onBack = onBack,
            )
            Spacer(modifier = Modifier.height(22.dp))
            BrailuxSectionCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp),
            ) {
                AboutItem(R.string.about_app_name)
                AboutItem(R.string.about_version)
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                AboutItem(R.string.about_purpose)
                AboutItem(R.string.about_offline)
                AboutItem(R.string.about_educational_notice, hasBottomPadding = false)
            }
            Spacer(modifier = Modifier.height(16.dp))
            InstitutionalCard(
                identity = identity,
                onOpenWebsite = openWebsite,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp),
            )
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun InstitutionalCard(
    identity: InstitutionalIdentityConfig,
    onOpenWebsite: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BrailuxSectionCard(modifier = modifier) {
        Text(
            text = identity.studioName,
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.about_studio_description),
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.about_studio_membership, identity.studioName),
            modifier = Modifier.padding(top = 12.dp),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = stringResource(R.string.about_developer_credit, identity.leadDeveloper),
            modifier = Modifier.padding(top = 18.dp),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
        InstitutionalAccess(
            title = stringResource(R.string.about_brailux_website),
            url = identity.brailuxWebsiteUrl,
            accessibilityLabel = stringResource(R.string.about_brailux_website_accessibility),
            onOpenWebsite = onOpenWebsite,
        )
        InstitutionalAccess(
            title = stringResource(R.string.about_studio_website, identity.studioName),
            url = identity.studioWebsiteUrl,
            accessibilityLabel = stringResource(
                R.string.about_studio_website_accessibility,
                identity.studioName,
            ),
            onOpenWebsite = onOpenWebsite,
        )
        InstitutionalAccess(
            title = stringResource(R.string.about_privacy_policy),
            url = identity.privacyPolicyUrl,
            accessibilityLabel = stringResource(R.string.about_privacy_policy),
            onOpenWebsite = onOpenWebsite,
            showDivider = false,
        )
    }
}

@Composable
private fun InstitutionalAccess(
    title: String,
    url: String?,
    accessibilityLabel: String,
    onOpenWebsite: (String) -> Unit,
    showDivider: Boolean = true,
) {
    if (url != null) {
        TextButton(
            onClick = { onOpenWebsite(url) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .semantics { contentDescription = accessibilityLabel },
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Start,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = stringResource(R.string.about_open_website),
                modifier = Modifier.padding(start = 12.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
        if (showDivider) HorizontalDivider()
        return
    }

    val status = stringResource(R.string.about_coming_soon)
    val accessibilityText = stringResource(
        R.string.about_future_accessibility,
        title,
        status,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .clearAndSetSemantics { contentDescription = accessibilityText },
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = status,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
    }
    if (showDivider) HorizontalDivider()
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

@Composable
private fun AboutItem(
    @StringRes textResource: Int,
    hasBottomPadding: Boolean = true,
) {
    Text(
        text = stringResource(textResource),
        modifier = Modifier.padding(bottom = if (hasBottomPadding) 14.dp else 0.dp),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
