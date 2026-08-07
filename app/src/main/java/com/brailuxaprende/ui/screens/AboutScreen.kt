package com.brailuxaprende.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.unit.dp
import com.brailuxaprende.R
import com.brailuxaprende.ui.components.BrailuxScreenHeader
import com.brailuxaprende.ui.components.BrailuxSectionCard

@Composable
fun AboutScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                AboutItem(R.string.about_educational_notice)
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                AboutItem(R.string.about_authorship_placeholder)
                AboutItem(R.string.about_privacy_placeholder, hasBottomPadding = false)
            }
            Text(
                text = stringResource(R.string.content_coming_later),
                modifier = Modifier
                    .padding(top = 18.dp)
                    .widthIn(max = 560.dp),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(20.dp))
        }
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
