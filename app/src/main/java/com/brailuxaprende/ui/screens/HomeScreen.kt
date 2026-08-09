package com.brailuxaprende.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.brailuxaprende.R
import com.brailuxaprende.data.seasonal.SeasonalEvent
import com.brailuxaprende.practice.EngagementProgress
import com.brailuxaprende.practice.PracticeDate
import com.brailuxaprende.practice.SystemPracticeClock
import com.brailuxaprende.ui.components.BrailuxMenuCard
import com.brailuxaprende.ui.components.BrailuxSecondaryButton
import com.brailuxaprende.ui.components.BrailuxSectionCard
import com.brailuxaprende.ui.components.SeasonalBanner
import com.brailuxaprende.ui.theme.BrailuxPreviewTheme

@Composable
fun HomeScreen(
    engagementProgress: EngagementProgress = EngagementProgress(),
    currentDate: PracticeDate = SystemPracticeClock.today(),
    seasonalEvent: SeasonalEvent? = null,
    onStartDailyPractice: () -> Unit = {},
    onLearn: () -> Unit,
    onPractice: () -> Unit,
    onSettings: () -> Unit,
    onAbout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val screenTitle = stringResource(R.string.home_title)
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
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_brailux_identity),
                contentDescription = null,
                modifier = Modifier.size(84.dp),
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
                DailyPracticeCard(
                    completedToday = engagementProgress.isDailyPracticeCompleted(currentDate),
                    onClick = onStartDailyPractice,
                )
                BrailuxMenuCard(
                    title = stringResource(R.string.home_access_practice),
                    description = stringResource(R.string.home_access_practice_description),
                    iconResource = R.drawable.ic_practice,
                    onClick = onPractice,
                )
                DailyChallengeCard()
            }
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

@Composable
private fun DailyPracticeCard(
    completedToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val status = stringResource(
        if (completedToday) {
            R.string.home_daily_practice_completed_today
        } else {
            R.string.home_daily_practice_pending
        },
    )

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 172.dp)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                stateDescription = status
            },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(52.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_practice),
                        contentDescription = null,
                        modifier = Modifier.padding(13.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.home_daily_practice),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(R.string.home_daily_practice_details),
                        modifier = Modifier.padding(top = 4.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = status,
                        modifier = Modifier.padding(top = 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Text(
                    text = stringResource(R.string.home_daily_practice_start),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun DailyChallengeCard(modifier: Modifier = Modifier) {
    BrailuxSectionCard(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.home_daily_challenge),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.home_daily_challenge_description),
            modifier = Modifier.padding(top = 6.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.home_coming_soon),
            modifier = Modifier.padding(top = 10.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Preview(name = "Inicio", showBackground = true, widthDp = 390, heightDp = 1200)
@Composable
private fun HomeScreenPreview() {
    val currentDate = PracticeDate(year = 2026, month = 8, day = 8)
    BrailuxPreviewTheme {
        HomeScreen(
            engagementProgress = EngagementProgress(dailyPracticeDates = setOf(currentDate)),
            currentDate = currentDate,
            onLearn = {},
            onPractice = {},
            onSettings = {},
            onAbout = {},
        )
    }
}
