package com.brailuxaprende.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.brailuxaprende.R
import com.brailuxaprende.ui.components.BrailuxScreenHeader
import com.brailuxaprende.ui.components.BrailuxSectionCard

@Composable
fun PracticeScreen(
    onStartLevel1: () -> Unit,
    onStartLevel2: () -> Unit,
    onStartLevel3: () -> Unit,
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
                title = stringResource(R.string.practice_title),
                subtitle = stringResource(R.string.practice_description),
                onBack = onBack,
            )
            Spacer(modifier = Modifier.height(20.dp))
            BrailuxSectionCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp),
            ) {
                Text(
                    text = stringResource(R.string.practice_orientation_title),
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleLarge,
                )
                OrientationOption(
                    title = stringResource(R.string.practice_orientation_reading),
                    status = stringResource(R.string.practice_selected),
                    selected = true,
                    enabled = true,
                    modifier = Modifier.padding(top = 8.dp),
                )
                OrientationOption(
                    title = stringResource(R.string.practice_orientation_slate),
                    status = stringResource(R.string.practice_coming_soon),
                    selected = false,
                    enabled = false,
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AvailableLevelCard(onClick = onStartLevel1)
                AvailableLevelCard(
                    title = stringResource(R.string.practice_level_2_title),
                    description = stringResource(R.string.practice_level_2_description),
                    onClick = onStartLevel2,
                )
                AvailableLevelCard(
                    title = stringResource(R.string.practice_level_3_title),
                    description = stringResource(R.string.practice_level_3_description),
                    onClick = onStartLevel3,
                )
                UnavailableLevelCard(
                    title = stringResource(R.string.practice_level_4_title),
                    description = stringResource(R.string.practice_level_4_description),
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun OrientationOption(
    title: String,
    status: String,
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .semantics(mergeDescendants = true) {
                stateDescription = status
                if (!enabled) disabled()
            }
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            enabled = enabled,
        )
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = status,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AvailableLevelCard(
    title: String = stringResource(R.string.practice_level_1_title),
    description: String = stringResource(R.string.practice_level_1_description),
    onClick: () -> Unit,
) {
    val available = stringResource(R.string.practice_available)
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 112.dp)
            .semantics(mergeDescendants = true) { stateDescription = available },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
    ) {
        LevelCardContent(
            title = title,
            description = description,
            status = available,
        )
    }
}

@Composable
private fun UnavailableLevelCard(
    title: String,
    description: String,
) {
    val comingSoon = stringResource(R.string.practice_coming_soon)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 112.dp)
            .semantics(mergeDescendants = true) {
                disabled()
                stateDescription = comingSoon
            },
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        LevelCardContent(
            title = title,
            description = description,
            status = comingSoon,
        )
    }
}

@Composable
private fun LevelCardContent(
    title: String,
    description: String,
    status: String,
) {
    Column(modifier = Modifier.padding(18.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        Text(
            text = description,
            modifier = Modifier.padding(top = 4.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = status,
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
