package com.brailuxaprende.ui.screens

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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.brailuxaprende.R
import com.brailuxaprende.data.settings.AccessibilityPreferences
import com.brailuxaprende.data.settings.AppearancePreference
import com.brailuxaprende.data.settings.TextSizePreference
import com.brailuxaprende.ui.components.BrailuxScreenHeader
import com.brailuxaprende.ui.components.BrailuxSectionCard

@Composable
fun SettingsScreen(
    preferences: AccessibilityPreferences,
    onSoundEnabledChange: (Boolean) -> Unit,
    onVibrationEnabledChange: (Boolean) -> Unit,
    onHighContrastEnabledChange: (Boolean) -> Unit,
    onTextSizeChange: (TextSizePreference) -> Unit,
    onAppearanceChange: (AppearancePreference) -> Unit,
    onSeasonalThemesEnabledChange: (Boolean) -> Unit,
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
                title = stringResource(R.string.settings_title),
                subtitle = stringResource(R.string.settings_description),
                onBack = onBack,
            )
            Spacer(modifier = Modifier.height(22.dp))
            BrailuxSectionCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp),
            ) {
                Text(
                    text = stringResource(R.string.settings_appearance),
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleLarge,
                )
                Column(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .selectableGroup(),
                ) {
                    SelectionOptionRow(
                        label = stringResource(R.string.settings_appearance_system),
                        selected = preferences.appearance == AppearancePreference.System,
                        onSelect = { onAppearanceChange(AppearancePreference.System) },
                    )
                    SelectionOptionRow(
                        label = stringResource(R.string.settings_appearance_light),
                        selected = preferences.appearance == AppearancePreference.Light,
                        onSelect = { onAppearanceChange(AppearancePreference.Light) },
                    )
                    SelectionOptionRow(
                        label = stringResource(R.string.settings_appearance_dark),
                        selected = preferences.appearance == AppearancePreference.Dark,
                        onSelect = { onAppearanceChange(AppearancePreference.Dark) },
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            BrailuxSectionCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp),
            ) {
                SettingsToggle(
                    label = stringResource(R.string.settings_sound),
                    checked = preferences.soundEnabled,
                    onCheckedChange = onSoundEnabledChange,
                )
                HorizontalDivider()
                SettingsToggle(
                    label = stringResource(R.string.settings_vibration),
                    checked = preferences.vibrationEnabled,
                    onCheckedChange = onVibrationEnabledChange,
                )
                HorizontalDivider()
                SettingsToggle(
                    label = stringResource(R.string.settings_high_contrast),
                    checked = preferences.highContrastEnabled,
                    onCheckedChange = onHighContrastEnabledChange,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            BrailuxSectionCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp),
            ) {
                Text(
                    text = stringResource(R.string.settings_text_size),
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleLarge,
                )
                Column(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .selectableGroup(),
                ) {
                    SelectionOptionRow(
                        label = stringResource(R.string.settings_text_normal),
                        selected = preferences.textSize == TextSizePreference.Normal,
                        onSelect = { onTextSizeChange(TextSizePreference.Normal) },
                    )
                    SelectionOptionRow(
                        label = stringResource(R.string.settings_text_large),
                        selected = preferences.textSize == TextSizePreference.Large,
                        onSelect = { onTextSizeChange(TextSizePreference.Large) },
                    )
                    SelectionOptionRow(
                        label = stringResource(R.string.settings_text_very_large),
                        selected = preferences.textSize == TextSizePreference.VeryLarge,
                        onSelect = { onTextSizeChange(TextSizePreference.VeryLarge) },
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            BrailuxSectionCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp),
            ) {
                Text(
                    text = stringResource(R.string.settings_seasonal_themes),
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = stringResource(R.string.settings_seasonal_themes_description),
                    modifier = Modifier.padding(top = 6.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SettingsToggle(
                    label = stringResource(
                        if (preferences.seasonalThemesEnabled) {
                            R.string.settings_seasonal_themes_enabled
                        } else {
                            R.string.settings_seasonal_themes_disabled
                        },
                    ),
                    checked = preferences.seasonalThemesEnabled,
                    onCheckedChange = onSeasonalThemesEnabledChange,
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SettingsToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val state = stringResource(
        if (checked) R.string.settings_state_enabled else R.string.settings_state_disabled,
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .semantics(mergeDescendants = true) { stateDescription = state }
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
        )
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun SelectionOptionRow(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val state = stringResource(
        if (selected) R.string.settings_state_selected else R.string.settings_state_not_selected,
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .semantics(mergeDescendants = true) { stateDescription = state }
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onSelect,
            )
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            text = label,
            modifier = Modifier.padding(start = 12.dp),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
