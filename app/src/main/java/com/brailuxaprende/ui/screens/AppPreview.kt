package com.brailuxaprende.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.brailuxaprende.data.settings.AccessibilityPreferences
import com.brailuxaprende.ui.navigation.BrailuxApp
import com.brailuxaprende.ui.theme.BrailuxPreviewTheme

@Preview(showBackground = true, name = "App Completa")
@Composable
fun BrailuxAppPreview() {
    BrailuxPreviewTheme {
        BrailuxApp(
            preferences = AccessibilityPreferences(),
            onSoundEnabledChange = {},
            onVibrationEnabledChange = {},
            onHighContrastEnabledChange = {},
            onTextSizeChange = {},
            onAppearanceChange = {},
            onSeasonalThemesEnabledChange = {},
        )
    }
}
