package com.brailuxaprende.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.brailuxaprende.R
import com.brailuxaprende.data.settings.TextSizePreference
import com.brailuxaprende.ui.theme.BrailuxAprendeTheme
import com.brailuxaprende.ui.theme.BrailuxTheme

@Composable
fun BrailuxPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    @DrawableRes iconResource: Int? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 56.dp),
        shape = MaterialTheme.shapes.medium,
        contentPadding = ButtonDefaults.ContentPadding,
    ) {
        if (iconResource != null) {
            Icon(
                painter = painterResource(iconResource),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
        }
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun BrailuxSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    @DrawableRes iconResource: Int? = null,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 56.dp),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
        contentPadding = ButtonDefaults.ContentPadding,
    ) {
        if (iconResource != null) {
            Icon(
                painter = painterResource(iconResource),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
        }
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun BrailuxBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hasSeasonalBackground: Boolean = false,
) {
    val backDescription = stringResource(R.string.action_back_description)
    val colors = if (hasSeasonalBackground) {
        ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
            contentColor = MaterialTheme.colorScheme.onSurface,
        )
    } else {
        ButtonDefaults.outlinedButtonColors()
    }
    val border = if (hasSeasonalBackground) {
        BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        )
    } else {
        ButtonDefaults.outlinedButtonBorder(enabled = true)
    }

    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 48.dp)
            .semantics { contentDescription = backDescription },
        shape = MaterialTheme.shapes.small,
        colors = colors,
        border = border,
        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_arrow_back),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.action_back),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
fun BrailuxScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    textAlign: TextAlign = TextAlign.Center,
    hasSeasonalBackground: Boolean = false,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp),
        horizontalAlignment = if (textAlign == TextAlign.Center) {
            Alignment.CenterHorizontally
        } else {
            Alignment.Start
        },
    ) {
        if (onBack != null) {
            BrailuxBackButton(
                onClick = onBack,
                hasSeasonalBackground = hasSeasonalBackground,
                modifier = Modifier.align(Alignment.Start),
            )
            Spacer(modifier = Modifier.heightIn(min = 20.dp))
        }
        if (hasSeasonalBackground) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalAlignment = if (textAlign == TextAlign.Center) {
                        Alignment.CenterHorizontally
                    } else {
                        Alignment.Start
                    },
                ) {
                    Text(
                        text = title,
                        modifier = Modifier.semantics { heading() },
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = textAlign,
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            modifier = Modifier.padding(top = 8.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = textAlign,
                        )
                    }
                }
            }
        } else {
            Text(
                text = title,
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = textAlign,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    modifier = Modifier.padding(top = 10.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = textAlign,
                )
            }
        }
    }
}

@Composable
fun BrailuxSectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            content = content,
        )
    }
}

@Composable
fun BrailuxMenuCard(
    title: String,
    description: String,
    @DrawableRes iconResource: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 112.dp)
            .scale(if (pressed) 0.98f else 1f)
            .semantics(mergeDescendants = true) {
                role = Role.Button
            },
        interactionSource = interactionSource,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (pressed) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
        border = BorderStroke(
            width = if (pressed) 3.dp else 1.dp,
            color = if (pressed) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (pressed) 0.dp else 3.dp),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
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
                    painter = painterResource(iconResource),
                    contentDescription = null,
                    modifier = Modifier.padding(13.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = description,
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

enum class BrailuxFeedbackType {
    Success,
    Warning,
    Error,
}

@Composable
fun BrailuxFeedbackCard(
    message: String,
    type: BrailuxFeedbackType,
    modifier: Modifier = Modifier,
    announceForAccessibility: Boolean = true,
) {
    val statusColors = BrailuxTheme.statusColors
    val containerColor = when (type) {
        BrailuxFeedbackType.Success -> statusColors.successContainer
        BrailuxFeedbackType.Warning -> statusColors.warningContainer
        BrailuxFeedbackType.Error -> MaterialTheme.colorScheme.errorContainer
    }
    val contentColor = when (type) {
        BrailuxFeedbackType.Success -> statusColors.onSuccessContainer
        BrailuxFeedbackType.Warning -> statusColors.onWarningContainer
        BrailuxFeedbackType.Error -> MaterialTheme.colorScheme.onErrorContainer
    }
    val symbol = when (type) {
        BrailuxFeedbackType.Success -> stringResource(R.string.feedback_correct_symbol)
        BrailuxFeedbackType.Warning -> stringResource(R.string.feedback_warning_symbol)
        BrailuxFeedbackType.Error -> stringResource(R.string.feedback_incorrect_symbol)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics {
                contentDescription = message
                if (announceForAccessibility) liveRegion = LiveRegionMode.Polite
            },
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(2.dp, contentColor),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = MaterialTheme.shapes.small,
                color = contentColor,
                contentColor = containerColor,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = symbol,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Preview(name = "Botones", showBackground = true)
@Composable
private fun BrailuxButtonsPreview() {
    BrailuxAprendeTheme {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BrailuxPrimaryButton(text = "Comenzar", onClick = {})
            BrailuxSecondaryButton(text = "Configuración", onClick = {})
        }
    }
}

@Preview(name = "Tarjeta de menú", showBackground = true)
@Composable
private fun BrailuxMenuCardPreview() {
    BrailuxAprendeTheme {
        BrailuxMenuCard(
            title = "Aprende",
            description = "Conoce los fundamentos del sistema Braille.",
            iconResource = R.drawable.ic_learn,
            onClick = {},
            modifier = Modifier.padding(20.dp),
        )
    }
}

@Preview(name = "Retroalimentación", showBackground = true)
@Composable
private fun BrailuxFeedbackPreview() {
    BrailuxAprendeTheme {
        BrailuxFeedbackCard(
            message = "¡Muy bien! La respuesta es correcta.",
            type = BrailuxFeedbackType.Success,
            modifier = Modifier.padding(20.dp),
        )
    }
}

@Preview(name = "Alto contraste", showBackground = true)
@Composable
private fun BrailuxHighContrastPreview() {
    BrailuxAprendeTheme(highContrast = true) {
        BrailuxFeedbackCard(
            message = "Revisa los puntos e inténtalo otra vez.",
            type = BrailuxFeedbackType.Error,
            modifier = Modifier.padding(20.dp),
        )
    }
}

@Preview(name = "Texto muy grande", showBackground = true, widthDp = 360)
@Composable
private fun BrailuxVeryLargeTextPreview() {
    BrailuxAprendeTheme(textSize = TextSizePreference.VeryLarge) {
        BrailuxMenuCard(
            title = "Mi progreso",
            description = "Consulta tu avance en las lecciones y prácticas.",
            iconResource = R.drawable.ic_progress,
            onClick = {},
            modifier = Modifier.padding(20.dp),
        )
    }
}
