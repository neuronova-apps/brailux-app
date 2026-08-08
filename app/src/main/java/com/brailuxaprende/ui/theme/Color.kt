package com.brailuxaprende.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val BrailuxBlue = Color(0xFF1976D2)
val BrailuxBlueDark = Color(0xFF0D2B45)
val BrailuxSky = Color(0xFF64B5F6)
val BrailuxBackground = Color(0xFFF6FAFF)
val BrailuxSurface = Color(0xFFFFFFFF)
val BrailuxText = Color(0xFF10233E)
val BrailuxTextSecondary = Color(0xFF46566C)
val BrailuxSuccess = Color(0xFF2E7D32)
val BrailuxSuccessContainer = Color(0xFFE7F5E8)
val BrailuxWarning = Color(0xFF9A4600)
val BrailuxWarningContainer = Color(0xFFFFEEDB)
val BrailuxError = Color(0xFFB3261E)
val BrailuxErrorContainer = Color(0xFFF9DEDC)
val BrailuxOutline = Color(0xFF6F7F93)
val BrailuxSurfaceVariant = Color(0xFFE7F1FC)

val BrailuxDarkBackground = Color(0xFF081421)
val BrailuxDarkSurface = Color(0xFF0F2236)
val BrailuxDarkSurfaceVariant = Color(0xFF1B344D)
val BrailuxDarkPrimary = Color(0xFF8CC8FF)
val BrailuxDarkPrimaryContainer = Color(0xFF164A70)
val BrailuxDarkText = Color(0xFFF3F8FF)
val BrailuxDarkTextSecondary = Color(0xFFC4D5E7)
val BrailuxDarkOutline = Color(0xFF8FA3B8)
val BrailuxDarkOutlineVariant = Color(0xFF40566D)

val HighContrastBackground = Color(0xFF000000)
val HighContrastForeground = Color(0xFFFFFFFF)
val HighContrastPrimary = Color(0xFFFFE000)
val HighContrastSecondary = Color(0xFF64DFFF)
val HighContrastError = Color(0xFFFF8A80)
val HighContrastSuccess = Color(0xFF7CFF7C)
val HighContrastWarning = Color(0xFFFFC266)
val HighContrastSurfaceVariant = Color(0xFF1A1A1A)
val HighContrastOutlineVariant = Color(0xFFBDBDBD)

@Immutable
data class BrailuxStatusColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
)

internal val RegularStatusColors = BrailuxStatusColors(
    success = BrailuxSuccess,
    onSuccess = Color.White,
    successContainer = BrailuxSuccessContainer,
    onSuccessContainer = Color(0xFF123915),
    warning = BrailuxWarning,
    onWarning = Color.White,
    warningContainer = BrailuxWarningContainer,
    onWarningContainer = Color(0xFF4A2100),
)

internal val DarkStatusColors = BrailuxStatusColors(
    success = Color(0xFF80D982),
    onSuccess = Color(0xFF00390A),
    successContainer = Color(0xFF0C4A1E),
    onSuccessContainer = Color(0xFFA1F3A3),
    warning = Color(0xFFFFB870),
    onWarning = Color(0xFF4B2800),
    warningContainer = Color(0xFF633B08),
    onWarningContainer = Color(0xFFFFDCC0),
)

internal val HighContrastStatusColors = BrailuxStatusColors(
    success = HighContrastSuccess,
    onSuccess = HighContrastBackground,
    successContainer = HighContrastBackground,
    onSuccessContainer = HighContrastSuccess,
    warning = HighContrastWarning,
    onWarning = HighContrastBackground,
    warningContainer = HighContrastBackground,
    onWarningContainer = HighContrastWarning,
)

internal val LocalBrailuxStatusColors = staticCompositionLocalOf { RegularStatusColors }
