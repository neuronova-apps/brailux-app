package com.brailuxaprende.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.brailuxaprende.ui.theme.BRAILLE_GEOMETRIC_ACCENT
import com.brailuxaprende.ui.theme.BRAILLE_MIST_ACCENT
import com.brailuxaprende.ui.theme.BRAILLE_ORGANIC_ACCENT
import com.brailuxaprende.ui.theme.BRAILLE_TACTILE_WAVE_ACCENT
import com.brailuxaprende.ui.theme.LocalBrailuxTheme
import kotlin.math.sin

/**
 * Purely decorative themed accent line rendered programmatically using Compose Canvas.
 * Fully excluded from accessibility (TalkBack) and focus order.
 */
@Composable
fun BrailuxThemedAccent(
    modifier: Modifier = Modifier,
    accentStyle: String? = LocalBrailuxTheme.current.accentStyle,
    color: Color = LocalBrailuxTheme.current.visual.primary,
    accentAlpha: Float = LocalBrailuxTheme.current.visual.accentAlpha,
) {
    if (accentStyle == null) return

    val effectiveColor = color.copy(alpha = accentAlpha)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(14.dp)
            .clearAndSetSemantics { },
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val width = size.width
            val height = size.height
            val centerY = height / 2f

            when (accentStyle) {
                BRAILLE_GEOMETRIC_ACCENT -> {
                    // Celeste: Technical segmented line with discrete Braille 6-dot cluster motifs
                    val segmentLength = 32.dp.toPx()
                    val gapLength = 20.dp.toPx()
                    val strokeWidth = 2.dp.toPx()

                    var currentX = 0f
                    while (currentX < width) {
                        val endX = (currentX + segmentLength).coerceAtMost(width)
                        drawLine(
                            color = effectiveColor,
                            start = Offset(currentX, centerY),
                            end = Offset(endX, centerY),
                            strokeWidth = strokeWidth,
                            cap = StrokeCap.Round,
                        )

                        // Draw micro Braille motif in the gap if space permits
                        val motifCenterX = endX + gapLength / 2f
                        if (motifCenterX + 6.dp.toPx() < width) {
                            val dotRadius = 1.2.dp.toPx()
                            val dotSpacing = 3.dp.toPx()
                            // 2 columns x 3 rows micro dots
                            for (col in -1..0) {
                                for (row in -1..1) {
                                    drawCircle(
                                        color = effectiveColor,
                                        radius = dotRadius,
                                        center = Offset(
                                            x = motifCenterX + (col + 0.5f) * dotSpacing,
                                            y = centerY + row * dotSpacing,
                                        ),
                                    )
                                }
                            }
                        }

                        currentX += segmentLength + gapLength
                    }
                }

                BRAILLE_TACTILE_WAVE_ACCENT -> {
                    // Crema: Soft smooth tactile wave line with subtle embossed tactile dot nodes
                    val strokeWidth = 2.dp.toPx()
                    val wavePath = Path()
                    val wavelength = 56.dp.toPx()
                    val amplitude = 3.5.dp.toPx()

                    wavePath.moveTo(0f, centerY)
                    var x = 0f
                    val step = 4f
                    while (x <= width) {
                        val y = centerY + (amplitude * sin((x / wavelength) * (2f * Math.PI.toFloat()))).toFloat()
                        wavePath.lineTo(x, y)
                        x += step
                    }

                    drawPath(
                        path = wavePath,
                        color = effectiveColor,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    )

                    // Draw tactile node dots at wave crests
                    var nodeX = wavelength * 0.25f
                    while (nodeX < width) {
                        val nodeY = centerY - amplitude
                        drawCircle(
                            color = effectiveColor,
                            radius = 2.2.dp.toPx(),
                            center = Offset(nodeX, nodeY),
                        )
                        nodeX += wavelength
                    }
                }

                BRAILLE_MIST_ACCENT -> {
                    // Lavanda: Diffuse mist gradient halo with fading suspended dots
                    val strokeWidth = 3.dp.toPx()
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                effectiveColor.copy(alpha = 0.35f),
                                effectiveColor,
                                effectiveColor.copy(alpha = 0.35f),
                                Color.Transparent,
                            ),
                        ),
                        topLeft = Offset(0f, centerY - strokeWidth / 2f),
                        size = androidx.compose.ui.geometry.Size(width, strokeWidth),
                    )

                    // Suspended soft dots with low opacity
                    val dotStep = 44.dp.toPx()
                    var dotX = dotStep / 2f
                    while (dotX < width) {
                        drawCircle(
                            color = effectiveColor.copy(alpha = 0.65f),
                            radius = 2.5.dp.toPx(),
                            center = Offset(dotX, centerY),
                        )
                        drawCircle(
                            color = effectiveColor.copy(alpha = 0.25f),
                            radius = 5.dp.toPx(),
                            center = Offset(dotX, centerY),
                        )
                        dotX += dotStep
                    }
                }

                BRAILLE_ORGANIC_ACCENT -> {
                    // Salvia: Discrete organic curve with delicate tactile nodes
                    val strokeWidth = 2.dp.toPx()
                    val organicPath = Path()
                    val period = 80.dp.toPx()
                    val curveHeight = 3.dp.toPx()

                    organicPath.moveTo(0f, centerY)
                    var px = 0f
                    while (px <= width) {
                        val py = centerY + (curveHeight * sin((px / period) * Math.PI.toFloat())).toFloat()
                        organicPath.lineTo(px, py)
                        px += 6f
                    }

                    drawPath(
                        path = organicPath,
                        color = effectiveColor,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    )

                    // Subtle organic dot pairs
                    val groupStep = 60.dp.toPx()
                    var gx = groupStep / 2f
                    while (gx < width) {
                        val gy = centerY + (curveHeight * sin((gx / period) * Math.PI.toFloat())).toFloat()
                        drawCircle(
                            color = effectiveColor,
                            radius = 1.8.dp.toPx(),
                            center = Offset(gx - 2.5.dp.toPx(), gy - 1.5.dp.toPx()),
                        )
                        drawCircle(
                            color = effectiveColor,
                            radius = 1.8.dp.toPx(),
                            center = Offset(gx + 2.5.dp.toPx(), gy + 1.5.dp.toPx()),
                        )
                        gx += groupStep
                    }
                }
            }
        }
    }
}

/**
 * Purely ornamental Braille 6-dot decorative motif (2 columns x 3 rows).
 * Excluded from TalkBack and focus navigation.
 */
@Composable
fun BrailuxBrailleMotif(
    modifier: Modifier = Modifier,
    dotColor: Color = LocalBrailuxTheme.current.visual.primary,
    dotRadius: Dp = 2.dp,
    spacing: Dp = 4.dp,
    alpha: Float = 0.6f,
) {
    Box(
        modifier = modifier.clearAndSetSemantics { },
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val radiusPx = dotRadius.toPx()
            val spacingPx = spacing.toPx()
            val effectiveColor = dotColor.copy(alpha = alpha)

            val totalWidth = spacingPx + radiusPx * 2
            val totalHeight = spacingPx * 2 + radiusPx * 2
            val startX = (size.width - totalWidth) / 2f + radiusPx
            val startY = (size.height - totalHeight) / 2f + radiusPx

            for (col in 0..1) {
                for (row in 0..2) {
                    drawCircle(
                        color = effectiveColor,
                        radius = radiusPx,
                        center = Offset(
                            x = startX + col * spacingPx,
                            y = startY + row * spacingPx,
                        ),
                    )
                }
            }
        }
    }
}
