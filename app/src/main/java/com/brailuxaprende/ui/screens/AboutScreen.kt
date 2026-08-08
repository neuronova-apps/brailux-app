package com.brailuxaprende.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
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
) {
    val studioName = identity.studioName
        ?: stringResource(R.string.about_studio_name_placeholder)

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
                studioName = studioName,
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
    studioName: String,
    modifier: Modifier = Modifier,
) {
    BrailuxSectionCard(modifier = modifier) {
        Text(
            text = stringResource(R.string.about_studio_title, studioName),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.about_studio_description, studioName),
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LabeledInstitutionalValue(
            label = stringResource(R.string.about_lead_developer),
            value = identity.leadDeveloper,
            modifier = Modifier.padding(top = 18.dp),
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
        FutureInstitutionalAccess(
            title = stringResource(R.string.about_brailux_website),
            url = identity.brailuxWebsiteUrl,
        )
        FutureInstitutionalAccess(
            title = stringResource(R.string.about_studio_website, studioName),
            url = identity.studioWebsiteUrl,
        )
        FutureInstitutionalAccess(
            title = stringResource(R.string.about_privacy_policy),
            url = identity.privacyPolicyUrl,
            showDivider = false,
        )
    }
}

@Composable
private fun LabeledInstitutionalValue(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val accessibilityText = stringResource(
        R.string.about_value_accessibility,
        label,
        value,
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = accessibilityText },
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            modifier = Modifier.padding(top = 4.dp),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun FutureInstitutionalAccess(
    title: String,
    url: String?,
    showDivider: Boolean = true,
) {
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
    if (url != null) {
        Text(
            text = stringResource(R.string.about_link_configured),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (showDivider) HorizontalDivider()
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
