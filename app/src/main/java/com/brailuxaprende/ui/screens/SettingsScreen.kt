package com.brailuxaprende.ui.screens

import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.brailuxaprende.R
import com.brailuxaprende.data.settings.AccessibilityPreferences
import com.brailuxaprende.data.settings.AppearancePreference
import com.brailuxaprende.data.settings.BrailuxBackgroundCatalog
import com.brailuxaprende.data.settings.BrailuxBackgroundOption
import com.brailuxaprende.data.settings.TextSizePreference
import com.brailuxaprende.ui.components.BrailuxScreenHeader
import com.brailuxaprende.ui.components.BrailuxSecondaryButton
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
    isPremiumUnlocked: Boolean,
    onBackgroundChange: (String) -> Unit,
    onAbout: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val premiumMessage = stringResource(R.string.settings_background_available_with_premium)

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
                        label = stringResource(R.string.settings_appearance_light),
                        selected = preferences.appearance == AppearancePreference.Light,
                        onSelect = { onAppearanceChange(AppearancePreference.Light) },
                    )
                    SelectionOptionRow(
                        label = stringResource(R.string.settings_appearance_dark),
                        selected = preferences.appearance == AppearancePreference.Dark,
                        onSelect = { onAppearanceChange(AppearancePreference.Dark) },
                    )
                    SelectionOptionRow(
                        label = stringResource(R.string.settings_appearance_system),
                        selected = preferences.appearance == AppearancePreference.System,
                        onSelect = { onAppearanceChange(AppearancePreference.System) },
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            WallpaperSection(
                selectedBackgroundId = preferences.selectedBackgroundId,
                isPremiumUnlocked = isPremiumUnlocked,
                onBackgroundChange = onBackgroundChange,
                onLockedBackground = {
                    Toast.makeText(context, premiumMessage, Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            BrailuxSectionCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp),
            ) {
                Text(
                    text = stringResource(R.string.settings_accessibility),
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleLarge,
                )
                SettingsToggle(
                    label = stringResource(R.string.settings_high_contrast),
                    checked = preferences.highContrastEnabled,
                    onCheckedChange = onHighContrastEnabledChange,
                )
                HorizontalDivider()
                Text(
                    text = stringResource(R.string.settings_text_size),
                    modifier = Modifier.padding(top = 14.dp),
                    style = MaterialTheme.typography.titleMedium,
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
                    text = stringResource(R.string.settings_interaction),
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleLarge,
                )
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
            }
            Spacer(modifier = Modifier.height(16.dp))
            BrailuxSectionCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp),
            ) {
                Text(
                    text = stringResource(R.string.settings_personalization),
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = stringResource(R.string.settings_seasonal_themes),
                    modifier = Modifier.padding(top = 12.dp),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.settings_seasonal_themes_description),
                    modifier = Modifier.padding(top = 4.dp),
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
            Spacer(modifier = Modifier.height(16.dp))
            BrailuxSectionCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp),
            ) {
                Text(
                    text = stringResource(R.string.settings_information),
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = stringResource(R.string.settings_about_description),
                    modifier = Modifier.padding(top = 6.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val aboutAccessibilityLabel = stringResource(
                    R.string.settings_open_about_accessibility,
                )
                BrailuxSecondaryButton(
                    text = stringResource(R.string.settings_about),
                    iconResource = R.drawable.ic_info,
                    onClick = onAbout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                        .semantics { contentDescription = aboutAccessibilityLabel },
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun WallpaperSection(
    selectedBackgroundId: String,
    isPremiumUnlocked: Boolean,
    onBackgroundChange: (String) -> Unit,
    onLockedBackground: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BrailuxSectionCard(modifier = modifier) {
        Text(
            text = stringResource(R.string.settings_wallpaper),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = stringResource(R.string.settings_wallpaper_description),
            modifier = Modifier.padding(top = 4.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(
            modifier = Modifier
                .padding(top = 8.dp)
                .selectableGroup(),
        ) {
            BrailuxBackgroundCatalog.backgrounds.forEach { background ->
                BackgroundOptionRow(
                    background = background,
                    selected = selectedBackgroundId == background.id,
                    isPremiumUnlocked = isPremiumUnlocked,
                    onSelect = {
                        if (BrailuxBackgroundCatalog.canSelect(
                                id = background.id,
                                isPremiumUnlocked = isPremiumUnlocked,
                            )
                        ) {
                            onBackgroundChange(background.id)
                        } else {
                            onLockedBackground()
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun BackgroundOptionRow(
    background: BrailuxBackgroundOption,
    selected: Boolean,
    isPremiumUnlocked: Boolean,
    onSelect: () -> Unit,
) {
    val selectionState = stringResource(
        if (selected) R.string.settings_state_selected else R.string.settings_state_not_selected,
    )
    val premiumState = when {
        !background.premium -> null
        isPremiumUnlocked -> stringResource(R.string.settings_background_premium)
        else -> stringResource(R.string.settings_background_locked)
    }
    val state = listOfNotNull(selectionState, premiumState).joinToString(separator = ". ")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .semantics(mergeDescendants = true) { stateDescription = state }
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onSelect,
            )
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BackgroundThumbnail(background = background)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            Text(
                text = stringResource(background.nameResource),
                style = MaterialTheme.typography.bodyLarge,
            )
            if (selected) {
                Text(
                    text = stringResource(R.string.settings_background_selected),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (background.premium) {
                Text(
                    text = if (isPremiumUnlocked) {
                        stringResource(R.string.settings_background_premium)
                    } else {
                        "${stringResource(R.string.settings_background_premium)} · " +
                            stringResource(R.string.settings_background_locked)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        RadioButton(selected = selected, onClick = null)
    }
}

@Composable
private fun BackgroundThumbnail(background: BrailuxBackgroundOption) {
    val thumbnailModifier = Modifier.size(width = 72.dp, height = 52.dp)
    val drawableResource = background.drawableResource
    if (drawableResource == null) {
        Surface(
            modifier = thumbnailModifier,
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.background,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) { }
        return
    }

    val context = LocalContext.current
    val density = LocalDensity.current
    val targetWidth = with(density) { 72.dp.roundToPx() }
    val targetHeight = with(density) { 52.dp.roundToPx() }
    val bitmap = remember(drawableResource, targetWidth, targetHeight) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeResource(context.resources, drawableResource, bounds)
        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(bounds, targetWidth, targetHeight)
        }
        BitmapFactory.decodeResource(context.resources, drawableResource, options)
    }

    if (bitmap != null) {
        Image(
            painter = BitmapPainter(bitmap.asImageBitmap()),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = thumbnailModifier.clip(MaterialTheme.shapes.small),
        )
    }
}

private fun calculateInSampleSize(
    options: BitmapFactory.Options,
    requestedWidth: Int,
    requestedHeight: Int,
): Int {
    var sampleSize = 1
    while (
        options.outWidth / (sampleSize * 2) >= requestedWidth &&
        options.outHeight / (sampleSize * 2) >= requestedHeight
    ) {
        sampleSize *= 2
    }
    return sampleSize
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
