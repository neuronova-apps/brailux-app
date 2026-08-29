package com.brailuxaprende.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
import com.brailuxaprende.BrailuxFeatures
import com.brailuxaprende.R
import com.brailuxaprende.data.seasonal.SeasonalEvent
import com.brailuxaprende.data.seasonal.SeasonalTheme
import com.brailuxaprende.data.seasonal.SeasonalThemeCatalog
import com.brailuxaprende.data.seasonal.SeasonalThemeResources
import com.brailuxaprende.practice.EngagementProgress
import com.brailuxaprende.practice.PracticeDate
import com.brailuxaprende.practice.SystemPracticeClock
import com.brailuxaprende.ui.components.BrailuxMenuCard
import com.brailuxaprende.ui.components.BrailuxSectionCard
import com.brailuxaprende.ui.components.SeasonalBanner
import com.brailuxaprende.ui.theme.BrailuxPreviewTheme

@Composable
fun HomeScreen(
    engagementProgress: EngagementProgress = EngagementProgress(),
    currentDate: PracticeDate = SystemPracticeClock.today(),
    hasIncompleteDailySession: Boolean = false,
    hasIncompleteDailyChallengeSession: Boolean = false,
    seasonalEvent: SeasonalEvent? = null,
    seasonalTheme: SeasonalTheme = SeasonalTheme.NONE,
    onStartDailyPractice: () -> Unit = {},
    onStartDailyChallenge: () -> Unit = {},
    onLearn: () -> Unit,
    onPractice: () -> Unit,
    onAssistant: () -> Unit = {},
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val screenTitle = stringResource(R.string.home_title)
    val seasonalResources = SeasonalThemeCatalog.resourcesFor(seasonalTheme)

    Surface(
        modifier = modifier
            .fillMaxSize()
            .semantics { paneTitle = screenTitle },
        color = if (seasonalResources != null) Color.Transparent else MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HomeHeader(
                onOpenSettings = onSettings,
                seasonalResources = seasonalResources,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp),
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
            Spacer(modifier = Modifier.height(22.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                DailyPracticeCard(
                    completedToday = engagementProgress.isDailyPracticeCompleted(currentDate),
                    hasIncompleteSession = hasIncompleteDailySession,
                    onClick = onStartDailyPractice,
                    seasonalResources = seasonalResources,
                )
                DailyChallengeCard(
                    completedToday = engagementProgress.isDailyChallengeCompleted(currentDate),
                    hasIncompleteSession = hasIncompleteDailyChallengeSession,
                    onClick = onStartDailyChallenge,
                    seasonalResources = seasonalResources,
                )
                BrailuxMenuCard(
                    title = stringResource(R.string.home_access_practice),
                    description = stringResource(R.string.home_access_practice_description),
                    iconResource = R.drawable.ic_practice,
                    onClick = onPractice,
                )
                if (BrailuxFeatures.ASSISTANT_ENABLED) {
                    BrailuxMenuCard(
                        title = stringResource(R.string.assistant_title),
                        description = stringResource(R.string.home_access_assistant_description),
                        iconResource = R.drawable.ic_assistant,
                        onClick = onAssistant,
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HomeHeader(
    onOpenSettings: () -> Unit,
    seasonalResources: SeasonalThemeResources?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Logo with optional seasonal decoration overlaid on top (centered horizontally)
        Box(contentAlignment = Alignment.TopCenter) {
            Surface(
                modifier = Modifier.size(60.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shadowElevation = 2.dp,
            ) {
                Image(
                    painter = painterResource(R.drawable.brailux_logo),
                    contentDescription = stringResource(R.string.brailux_logo_description),
                    modifier = Modifier.padding(5.dp),
                )
            }
            // Seasonal logo decoration: centered horizontally, offset upward to sit above logo
            if (seasonalResources != null) {
                Image(
                    painter = painterResource(seasonalResources.logoDecorationResource),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(38.dp)
                        .offset(y = (-18).dp),
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.home_brand_name),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = stringResource(R.string.home_brand_subtitle),
                modifier = Modifier.padding(top = 2.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Surface(
            modifier = Modifier.size(52.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        ) {
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxSize(),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_settings),
                    contentDescription = stringResource(R.string.home_open_settings),
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

@Composable
private fun DailyPracticeCard(
    completedToday: Boolean,
    hasIncompleteSession: Boolean = false,
    onClick: () -> Unit,
    seasonalResources: SeasonalThemeResources?,
    modifier: Modifier = Modifier,
) {
    val status = stringResource(
        if (completedToday) {
            R.string.home_daily_practice_completed
        } else {
            R.string.home_daily_practice_pending
        },
    )
    val buttonText = stringResource(
        when {
            completedToday -> R.string.home_daily_practice_view_summary
            hasIncompleteSession -> R.string.home_daily_practice_continue
            else -> R.string.home_daily_practice_start
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
        // Wrap card content in a Box to allow layering the seasonal decoration
        Box {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = if (seasonalResources != null) 54.dp else 0.dp),
                    ) {
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
                        text = buttonText,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            // Seasonal decoration: top-end corner, non-interactive, decorative only
            if (seasonalResources != null) {
                Image(
                    painter = painterResource(seasonalResources.dailyPracticeDecorationResource),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(68.dp)
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun DailyChallengeCard(
    completedToday: Boolean,
    hasIncompleteSession: Boolean = false,
    onClick: () -> Unit,
    seasonalResources: SeasonalThemeResources?,
    modifier: Modifier = Modifier,
) {
    val status = stringResource(
        if (completedToday) {
            R.string.home_daily_challenge_completed
        } else {
            R.string.home_daily_challenge_pending
        },
    )
    val buttonText = stringResource(
        when {
            completedToday -> R.string.home_daily_challenge_view_summary
            hasIncompleteSession -> R.string.home_daily_challenge_continue
            else -> R.string.home_daily_challenge_start
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
        // Wrap card content in a Box to allow layering the seasonal decoration
        Box {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = if (seasonalResources != null) 54.dp else 0.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.home_daily_challenge),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = stringResource(R.string.home_daily_challenge_details),
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
                        text = buttonText,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            // Seasonal decoration: top-end corner, non-interactive, decorative only
            if (seasonalResources != null) {
                Image(
                    painter = painterResource(seasonalResources.dailyChallengeDecorationResource),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(68.dp)
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 10.dp),
                )
            }
        }
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
        )
    }
}
