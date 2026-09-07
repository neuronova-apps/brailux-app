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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.brailuxaprende.R
import com.brailuxaprende.data.billing.BrailuxBillingProductCatalog
import com.brailuxaprende.data.billing.BrailuxBillingUiState
import com.brailuxaprende.data.billing.BrailuxRestoreEvent
import com.brailuxaprende.data.billing.BrailuxThemePurchaseStatus
import com.brailuxaprende.data.settings.AccessibilityPreferences
import com.brailuxaprende.data.settings.AppearancePreference
import com.brailuxaprende.data.settings.BackgroundRotationAction
import com.brailuxaprende.data.settings.BackgroundRotationMode
import com.brailuxaprende.data.settings.BrailuxBackgroundCatalog
import com.brailuxaprende.data.settings.BrailuxBackgroundOption
import com.brailuxaprende.data.settings.BrailuxBackgroundRotationPolicy
import com.brailuxaprende.data.settings.TextSizePreference
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.brailuxaprende.ui.components.BrailuxScreenHeader
import com.brailuxaprende.ui.components.BrailuxSecondaryButton
import com.brailuxaprende.ui.components.BrailuxSectionCard
import com.brailuxaprende.ui.components.BrailuxThemedAccent
import com.brailuxaprende.ui.theme.BrailuxThemeCatalog
import kotlinx.coroutines.flow.SharedFlow

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
    ownedBackgroundIds: Set<String> = emptySet(),
    onBackgroundChange: (String) -> Unit,
    onAbout: () -> Unit,
    onBack: () -> Unit,
    billingUiState: BrailuxBillingUiState = BrailuxBillingUiState(),
    onBuyProduct: (productId: String, offerToken: String) -> Unit = { _, _ -> },
    onRestorePurchases: () -> Unit = {},
    restoreEvents: SharedFlow<BrailuxRestoreEvent>? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val premiumMessage = stringResource(R.string.settings_background_available_with_premium)

    restoreEvents?.let { events ->
        LaunchedEffect(events) {
            events.collect { event ->
                val message = when (event) {
                    BrailuxRestoreEvent.RestoreSuccess -> context.getString(R.string.settings_restore_success)
                    BrailuxRestoreEvent.RestoreEmpty -> context.getString(R.string.settings_restore_empty)
                    BrailuxRestoreEvent.RestoreError -> context.getString(R.string.settings_restore_error)
                }
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

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
                rotationMode = preferences.backgroundRotationMode,
                isPremiumUnlocked = isPremiumUnlocked,
                ownedBackgroundIds = ownedBackgroundIds,
                billingUiState = billingUiState,
                onBackgroundChange = onBackgroundChange,
                onLockedBackground = {
                    Toast.makeText(context, premiumMessage, Toast.LENGTH_SHORT).show()
                },
                onBuyProduct = onBuyProduct,
                onRestorePurchases = onRestorePurchases,
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
    rotationMode: BackgroundRotationMode,
    isPremiumUnlocked: Boolean,
    ownedBackgroundIds: Set<String> = emptySet(),
    billingUiState: BrailuxBillingUiState = BrailuxBillingUiState(),
    onBackgroundChange: (String) -> Unit,
    onLockedBackground: () -> Unit,
    onBuyProduct: (productId: String, offerToken: String) -> Unit,
    onRestorePurchases: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var previewBackground by remember { mutableStateOf<BrailuxBackgroundOption?>(null) }
    val canRotate = BrailuxBackgroundRotationPolicy.canRotate(
        isPremiumUnlocked = isPremiumUnlocked,
        ownedBackgroundIds = ownedBackgroundIds,
    )

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
        Column(modifier = Modifier.padding(top = 8.dp)) {
            BrailuxBackgroundCatalog.backgrounds.forEach { background ->
                val itemStatus = billingUiState.items[background.id]?.status ?: if (background.premium) {
                    if (isPremiumUnlocked || background.id in ownedBackgroundIds) {
                        BrailuxThemePurchaseStatus.Purchased
                    } else {
                        BrailuxThemePurchaseStatus.Unavailable
                    }
                } else {
                    BrailuxThemePurchaseStatus.Free
                }
                BackgroundOptionRow(
                    background = background,
                    selected = selectedBackgroundId == background.id,
                    itemStatus = itemStatus,
                    onSelect = {
                        if (!background.premium ||
                            itemStatus is BrailuxThemePurchaseStatus.Purchased ||
                            itemStatus is BrailuxThemePurchaseStatus.Free
                        ) {
                            onBackgroundChange(background.id)
                        } else {
                            onLockedBackground()
                        }
                    },
                    onBuy = { offerToken ->
                        val productId = BrailuxBillingProductCatalog.productIdFor(background.id)
                        if (productId != null) {
                            onBuyProduct(productId, offerToken)
                        }
                    },
                    onPreview = { previewBackground = background },
                )
                HorizontalDivider()
            }
        }

        val restoreEnabled = !billingUiState.isRestoring && !billingUiState.isPurchasing
        TextButton(
            onClick = onRestorePurchases,
            enabled = restoreEnabled,
            modifier = Modifier
                .align(Alignment.End)
                .padding(top = 8.dp),
        ) {
            Text(text = stringResource(R.string.settings_restore_purchases))
        }

        if (canRotate) {
            Text(
                text = stringResource(R.string.settings_background_rotation_title),
                modifier = Modifier
                    .padding(top = 18.dp)
                    .semantics { heading() },
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.settings_background_rotation_description),
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .selectableGroup(),
            ) {
                SelectionOptionRow(
                    label = stringResource(R.string.settings_background_rotation_fixed),
                    selected = rotationMode == BackgroundRotationMode.Fixed,
                    onSelect = {
                        onBackgroundChange(
                            BackgroundRotationAction.actionFor(BackgroundRotationMode.Fixed),
                        )
                    },
                )
                SelectionOptionRow(
                    label = stringResource(R.string.settings_background_rotation_on_open),
                    selected = rotationMode == BackgroundRotationMode.OnAppOpen,
                    onSelect = {
                        onBackgroundChange(
                            BackgroundRotationAction.actionFor(BackgroundRotationMode.OnAppOpen),
                        )
                    },
                )
                SelectionOptionRow(
                    label = stringResource(R.string.settings_background_rotation_periodic),
                    selected = rotationMode == BackgroundRotationMode.EverySixHours,
                    onSelect = {
                        onBackgroundChange(
                            BackgroundRotationAction.actionFor(
                                BackgroundRotationMode.EverySixHours,
                            ),
                        )
                    },
                )
            }
            Text(
                text = stringResource(R.string.settings_background_rotation_periodic_description),
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    previewBackground?.let { background ->
        val itemStatus = billingUiState.items[background.id]?.status ?: if (background.premium) {
            if (isPremiumUnlocked || background.id in ownedBackgroundIds) {
                BrailuxThemePurchaseStatus.Purchased
            } else {
                BrailuxThemePurchaseStatus.Unavailable
            }
        } else {
            BrailuxThemePurchaseStatus.Free
        }
        BackgroundPreviewDialog(
            background = background,
            itemStatus = itemStatus,
            onUse = {
                onBackgroundChange(background.id)
                previewBackground = null
            },
            onBuy = { offerToken ->
                val productId = BrailuxBillingProductCatalog.productIdFor(background.id)
                if (productId != null) {
                    onBuyProduct(productId, offerToken)
                }
                previewBackground = null
            },
            onDismiss = { previewBackground = null },
        )
    }
}

@Composable
private fun BackgroundOptionRow(
    background: BrailuxBackgroundOption,
    selected: Boolean,
    itemStatus: BrailuxThemePurchaseStatus,
    onSelect: () -> Unit,
    onBuy: (offerToken: String) -> Unit,
    onPreview: () -> Unit,
) {
    val canUse = !background.premium ||
        itemStatus is BrailuxThemePurchaseStatus.Purchased ||
        itemStatus is BrailuxThemePurchaseStatus.Free
    val canPreview = BrailuxBackgroundCatalog.canPreview(background)

    val selectionState = stringResource(
        if (selected) R.string.settings_state_selected else R.string.settings_state_not_selected,
    )
    val statusDescription = when (itemStatus) {
        is BrailuxThemePurchaseStatus.Free -> null
        is BrailuxThemePurchaseStatus.Purchased -> stringResource(R.string.settings_purchased)
        is BrailuxThemePurchaseStatus.Pending -> stringResource(R.string.settings_pending)
        is BrailuxThemePurchaseStatus.AvailableForPurchase -> stringResource(
            R.string.settings_background_price_accessibility,
            stringResource(background.nameResource),
            itemStatus.formattedPrice,
        )
        is BrailuxThemePurchaseStatus.Unavailable -> stringResource(R.string.settings_unavailable)
    }
    val state = listOfNotNull(selectionState, statusDescription).joinToString(separator = ". ")

    Column(modifier = Modifier.fillMaxWidth()) {
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
                    val premiumLabel = when (itemStatus) {
                        is BrailuxThemePurchaseStatus.Purchased -> stringResource(R.string.settings_purchased)
                        is BrailuxThemePurchaseStatus.Pending -> stringResource(R.string.settings_pending)
                        is BrailuxThemePurchaseStatus.AvailableForPurchase -> itemStatus.formattedPrice
                        is BrailuxThemePurchaseStatus.Unavailable -> stringResource(R.string.settings_unavailable)
                        is BrailuxThemePurchaseStatus.Free -> null
                    }
                    if (premiumLabel != null) {
                        Text(
                            text = "${stringResource(R.string.settings_background_premium)} · $premiumLabel",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            RadioButton(selected = selected, onClick = null, enabled = canUse)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 4.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (canPreview) {
                TextButton(
                    onClick = onPreview,
                    modifier = Modifier.testTag("background_preview_${background.id}"),
                ) {
                    Text(text = stringResource(R.string.settings_background_preview))
                }
            }
            if (itemStatus is BrailuxThemePurchaseStatus.AvailableForPurchase) {
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(
                    onClick = { onBuy(itemStatus.offerToken) },
                ) {
                    Text(
                        text = stringResource(
                            R.string.settings_buy_with_price,
                            itemStatus.formattedPrice,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun BackgroundPreviewDialog(
    background: BrailuxBackgroundOption,
    itemStatus: BrailuxThemePurchaseStatus,
    onUse: () -> Unit,
    onBuy: (offerToken: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val canUse = !background.premium ||
        itemStatus is BrailuxThemePurchaseStatus.Purchased ||
        itemStatus is BrailuxThemePurchaseStatus.Free
    val drawableResource = background.drawableResource
    val themeDef = BrailuxThemeCatalog.theme(background.id) ?: BrailuxThemeCatalog.defaultTheme
    val visual = themeDef.visual

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(background.nameResource))
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                if (drawableResource == null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        shape = MaterialTheme.shapes.medium,
                        color = visual.surface,
                        border = BorderStroke(
                            1.dp,
                            visual.borderColor,
                        ),
                    ) { }
                } else {
                    Image(
                        painter = painterResource(drawableResource),
                        contentDescription = stringResource(background.nameResource),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(MaterialTheme.shapes.medium),
                    )
                }

                // Sample demonstration card showing the complete theme visual styling
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = visual.cardColor,
                    border = BorderStroke(1.5.dp, visual.borderColor),
                    shadowElevation = 2.dp,
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(
                                modifier = Modifier.size(36.dp),
                                shape = MaterialTheme.shapes.small,
                                color = visual.chipColor,
                                contentColor = visual.iconTint,
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_learn),
                                    contentDescription = null,
                                    modifier = Modifier.padding(8.dp),
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(background.nameResource),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = visual.onSurface,
                                )
                                Text(
                                    text = stringResource(R.string.settings_theme_sample_card),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = visual.onSurface.copy(alpha = 0.75f),
                                )
                            }
                            Surface(
                                shape = MaterialTheme.shapes.extraSmall,
                                color = visual.chipColor,
                            ) {
                                Text(
                                    text = stringResource(R.string.settings_theme_sample_chip),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = visual.primary,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }

                        if (themeDef.accentStyle != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            BrailuxThemedAccent(
                                accentStyle = themeDef.accentStyle,
                                color = visual.primary,
                                accentAlpha = visual.accentAlpha,
                            )
                        }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            shape = MaterialTheme.shapes.small,
                            color = visual.buttonColor,
                            contentColor = visual.onButtonColor,
                        ) {
                            Text(
                                text = stringResource(R.string.settings_theme_sample_button),
                                modifier = Modifier.padding(vertical = 8.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }

                if (background.premium) {
                    when (itemStatus) {
                        is BrailuxThemePurchaseStatus.Purchased -> {
                            Text(
                                text = "${stringResource(R.string.settings_background_premium_badge)} · " +
                                    stringResource(R.string.settings_purchased),
                                modifier = Modifier.padding(top = 12.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = visual.primary,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        is BrailuxThemePurchaseStatus.AvailableForPurchase -> {
                            Text(
                                text = "${stringResource(R.string.settings_background_premium_badge)} · " +
                                    itemStatus.formattedPrice,
                                modifier = Modifier.padding(top = 12.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = visual.primary,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = stringResource(R.string.settings_background_locked_preview),
                                modifier = Modifier.padding(top = 4.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        is BrailuxThemePurchaseStatus.Pending -> {
                            Text(
                                text = "${stringResource(R.string.settings_background_premium_badge)} · " +
                                    stringResource(R.string.settings_pending),
                                modifier = Modifier.padding(top = 12.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        else -> {
                            Text(
                                text = "${stringResource(R.string.settings_background_premium_badge)} · " +
                                    stringResource(R.string.settings_unavailable),
                                modifier = Modifier.padding(top = 12.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            when (itemStatus) {
                is BrailuxThemePurchaseStatus.Purchased, is BrailuxThemePurchaseStatus.Free -> {
                    TextButton(onClick = onUse) {
                        Text(text = stringResource(R.string.settings_background_use))
                    }
                }
                is BrailuxThemePurchaseStatus.AvailableForPurchase -> {
                    TextButton(onClick = { onBuy(itemStatus.offerToken) }) {
                        Text(
                            text = stringResource(
                                R.string.settings_buy_with_price,
                                itemStatus.formattedPrice,
                            ),
                        )
                    }
                }
                is BrailuxThemePurchaseStatus.Pending -> {
                    TextButton(onClick = {}, enabled = false) {
                        Text(text = stringResource(R.string.settings_pending))
                    }
                }
                is BrailuxThemePurchaseStatus.Unavailable -> {
                    TextButton(onClick = {}, enabled = false) {
                        Text(text = stringResource(R.string.settings_unavailable))
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.settings_background_close))
            }
        },
    )
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
