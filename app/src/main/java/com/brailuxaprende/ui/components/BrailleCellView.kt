package com.brailuxaprende.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.brailuxaprende.R
import com.brailuxaprende.braille.BrailleCell
import com.brailuxaprende.braille.BrailleRepository
import com.brailuxaprende.ui.theme.BrailuxAprendeTheme

private val PointTouchTargetSize = 56.dp
private val PointVisualSize = 42.dp
private val CompactPointTouchTargetSize = 36.dp
private val CompactPointVisualSize = 36.dp

@Composable
fun BrailleCellView(
    cell: BrailleCell,
    interactive: Boolean = false,
    showPointNumbers: Boolean = true,
    onPointClick: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    isCompact: Boolean = false,
) {
    val cellSemantics = if (contentDescription != null) {
        Modifier.semantics {
            this.contentDescription = contentDescription
        }
    } else {
        Modifier
    }

    val horizontalPadding = if (isCompact) 12.dp else 18.dp
    val verticalPadding = if (isCompact) 8.dp else 14.dp
    val rowSpacing = if (isCompact) 7.dp else 8.dp
    val colSpacing = if (isCompact) 12.dp else 18.dp
    val shape = if (isCompact) MaterialTheme.shapes.medium else MaterialTheme.shapes.large
    val borderWidth = if (isCompact) 1.5.dp else 2.dp

    Surface(
        modifier = modifier.then(cellSemantics),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(borderWidth, MaterialTheme.colorScheme.outline),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = horizontalPadding, vertical = verticalPadding),
            verticalArrangement = Arrangement.spacedBy(rowSpacing),
        ) {
            listOf(1 to 4, 2 to 5, 3 to 6).forEach { (leftPoint, rightPoint) ->
                Row(horizontalArrangement = Arrangement.spacedBy(colSpacing)) {
                    BraillePoint(
                        point = leftPoint,
                        active = cell.isPointActive(leftPoint),
                        interactive = interactive,
                        showPointNumber = showPointNumbers,
                        onClick = { onPointClick(leftPoint) },
                        isCompact = isCompact,
                    )
                    BraillePoint(
                        point = rightPoint,
                        active = cell.isPointActive(rightPoint),
                        interactive = interactive,
                        showPointNumber = showPointNumbers,
                        onClick = { onPointClick(rightPoint) },
                        isCompact = isCompact,
                    )
                }
            }
        }
    }
}

@Composable
private fun BraillePoint(
    point: Int,
    active: Boolean,
    interactive: Boolean,
    showPointNumber: Boolean,
    onClick: () -> Unit,
    isCompact: Boolean = false,
) {
    val state = stringResource(
        if (active) R.string.braille_point_active else R.string.braille_point_inactive,
    )
    val pointDescription = stringResource(R.string.braille_point_description, point, state)
    val clickLabel = stringResource(R.string.braille_point_click_label, point)
    val interactionModifier = if (interactive) {
        Modifier.clickable(
            role = Role.Button,
            onClickLabel = clickLabel,
            onClick = onClick,
        )
    } else {
        Modifier
    }

    val touchTargetSize = if (isCompact) CompactPointTouchTargetSize else PointTouchTargetSize
    val visualSize = if (isCompact) CompactPointVisualSize else PointVisualSize
    val borderWidth = if (isCompact) {
        if (active) 2.dp else 1.5.dp
    } else {
        if (active) 3.dp else 2.dp
    }
    val textStyle = if (isCompact) {
        MaterialTheme.typography.labelMedium
    } else {
        MaterialTheme.typography.bodyLarge
    }

    Box(
        modifier = Modifier
            .size(touchTargetSize)
            .then(interactionModifier)
            .semantics {
                contentDescription = pointDescription
            },
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.size(visualSize),
            shape = CircleShape,
            color = if (active) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surface
            },
            contentColor = if (active) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            border = BorderStroke(
                width = borderWidth,
                color = if (active) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.outline
                },
            ),
            shadowElevation = if (active) (if (isCompact) 2.dp else 4.dp) else 0.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = when {
                        showPointNumber -> point.toString()
                        active -> "●"
                        else -> ""
                    },
                    style = textStyle,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Preview(name = "Celda vacía", showBackground = true)
@Composable
private fun EmptyBrailleCellPreview() {
    BrailuxAprendeTheme {
        BrailleCellView(cell = BrailleCell.fromPoints(emptySet()))
    }
}

@Preview(name = "Letra A", showBackground = true)
@Composable
private fun LetterABrailleCellPreview() {
    BrailuxAprendeTheme {
        BrailleCellView(cell = requireNotNull(BrailleRepository.findVowel('A')).cell)
    }
}

@Preview(name = "Letra E", showBackground = true)
@Composable
private fun LetterEBrailleCellPreview() {
    BrailuxAprendeTheme {
        BrailleCellView(cell = requireNotNull(BrailleRepository.findVowel('E')).cell)
    }
}

@Preview(name = "Celda interactiva", showBackground = true)
@Composable
private fun InteractiveBrailleCellPreview() {
    BrailuxAprendeTheme {
        BrailleCellView(
            cell = BrailleCell.fromPoints(setOf(1, 2, 4, 6)),
            interactive = true,
            contentDescription = "Celda Braille interactiva",
        )
    }
}

@Preview(name = "Celda compacta", showBackground = true)
@Composable
private fun CompactBrailleCellPreview() {
    BrailuxAprendeTheme {
        BrailleCellView(
            cell = requireNotNull(BrailleRepository.findVowel('A')).cell,
            isCompact = true,
        )
    }
}
