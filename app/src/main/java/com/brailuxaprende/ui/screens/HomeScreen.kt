package com.brailuxaprende.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.brailuxaprende.R
import com.brailuxaprende.data.seasonal.SeasonalEvent
import com.brailuxaprende.ui.components.BrailuxMenuCard
import com.brailuxaprende.ui.components.BrailuxPrimaryButton
import com.brailuxaprende.ui.components.BrailuxSecondaryButton
import com.brailuxaprende.ui.components.SeasonalBanner
import com.brailuxaprende.ui.theme.BrailuxPreviewTheme

@Composable
fun HomeScreen(
    seasonalEvent: SeasonalEvent? = null,
    onLearn: () -> Unit,
    onPractice: () -> Unit,
    onPlay: () -> Unit,
    onProgress: () -> Unit,
    onSettings: () -> Unit,
    onAbout: () -> Unit,
    onStartLesson: () -> Unit,
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
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.home_title),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.home_welcome_message),
                modifier = Modifier
                    .padding(top = 8.dp)
                    .widthIn(max = 560.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (seasonalEvent != null) {
                var showSeasonalBanner by rememberSaveable(seasonalEvent.id) {
                    mutableStateOf(true)
                }
                if (showSeasonalBanner) {
                    Spacer(modifier = Modifier.height(20.dp))
                    SeasonalBanner(
                        event = seasonalEvent,
                        onDismiss = { showSeasonalBanner = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 560.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                BrailuxMenuCard(
                    title = stringResource(R.string.home_access_learn),
                    description = stringResource(R.string.home_access_learn_description),
                    iconResource = R.drawable.ic_learn,
                    onClick = onLearn,
                )
                BrailuxMenuCard(
                    title = stringResource(R.string.home_access_practice),
                    description = stringResource(R.string.home_access_practice_description),
                    iconResource = R.drawable.ic_practice,
                    onClick = onPractice,
                )
                BrailuxMenuCard(
                    title = stringResource(R.string.home_access_play),
                    description = stringResource(R.string.home_access_play_description),
                    iconResource = R.drawable.ic_play,
                    onClick = onPlay,
                )
                BrailuxMenuCard(
                    title = stringResource(R.string.home_access_progress),
                    description = stringResource(R.string.home_access_progress_description),
                    iconResource = R.drawable.ic_progress,
                    onClick = onProgress,
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            BrailuxPrimaryButton(
                text = stringResource(R.string.home_continue_lesson),
                iconResource = R.drawable.ic_learn,
                onClick = onStartLesson,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp),
            )
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = stringResource(R.string.home_more_options),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp)
                    .semantics { heading() },
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                BrailuxSecondaryButton(
                    text = stringResource(R.string.home_access_settings),
                    iconResource = R.drawable.ic_settings,
                    onClick = onSettings,
                    modifier = Modifier.fillMaxWidth(),
                )
                BrailuxSecondaryButton(
                    text = stringResource(R.string.home_access_about),
                    iconResource = R.drawable.ic_info,
                    onClick = onAbout,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Preview(name = "Inicio", showBackground = true, widthDp = 390, heightDp = 1000)
@Composable
private fun HomeScreenPreview() {
    BrailuxPreviewTheme {
        HomeScreen(
            onLearn = {},
            onPractice = {},
            onPlay = {},
            onProgress = {},
            onSettings = {},
            onAbout = {},
            onStartLesson = {},
        )
    }
}
