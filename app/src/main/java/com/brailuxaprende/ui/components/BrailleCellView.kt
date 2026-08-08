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

@Composable
fun BrailleCellView(
    cell: BrailleCell,
    interactive: Boolean = false,
    showPointNumbers: Boolean = true,
    onPointClick: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val cellSemantics = if (contentDescription != null) {
        Modifier.semantics {
            this.contentDescription = contentDescription
        }
    } else {
        Modifier
    }

    Surface(
        modifier = modifier.then(cellSemantics),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(1 to 4, 2 to 5, 3 to 6).forEach { (leftPoint, rightPoint) ->
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    BraillePoint(
                        point = leftPoint,
                        active = cell.isPointActive(leftPoint),
                        interactive = interactive,
                        showPointNumber = showPointNumbers,
                        onClick = { onPointClick(leftPoint) },
                    )
                    BraillePoint(
                        point = rightPoint,
                        active = cell.isPointActive(rightPoint),
                        interactive = interactive,
                        showPointNumber = showPointNumbers,
                        onClick = { onPointClick(rightPoint) },
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

    Box(
        modifier = Modifier
            .size(PointTouchTargetSize)
            .then(interactionModifier)
            .semantics {
                contentDescription = pointDescription
            },
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.size(PointVisualSize),
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
                width = if (active) 3.dp else 2.dp,
                color = if (active) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.outline
                },
            ),
            shadowElevation = if (active) 4.dp else 0.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = when {
                        showPointNumber -> point.toString()
                        active -> "●"
                        else -> ""
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
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
