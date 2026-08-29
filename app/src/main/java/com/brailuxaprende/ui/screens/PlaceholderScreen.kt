package com.brailuxaprende.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.brailuxaprende.R
import com.brailuxaprende.ui.components.BrailuxScreenHeader
import com.brailuxaprende.ui.components.BrailuxSectionCard

@Composable
fun PlaceholderScreen(
    title: String,
    description: String,
    onBack: () -> Unit,
    hasSeasonalBackground: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = if (hasSeasonalBackground) Color.Transparent else MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BrailuxScreenHeader(
                title = title,
                subtitle = description,
                onBack = onBack,
                hasSeasonalBackground = hasSeasonalBackground,
            )
            Spacer(modifier = Modifier.height(24.dp))
            BrailuxSectionCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp),
            ) {
                Text(
                    text = stringResource(R.string.content_coming_later),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
