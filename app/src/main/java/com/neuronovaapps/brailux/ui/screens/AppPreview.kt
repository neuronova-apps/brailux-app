package com.neuronovaapps.brailux.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.neuronovaapps.brailux.data.settings.AccessibilityPreferences
import com.neuronovaapps.brailux.ui.navigation.BrailuxApp
import com.neuronovaapps.brailux.ui.theme.BrailuxPreviewTheme

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
            onLevel1SessionCompleted = { _, onRecorded -> onRecorded(null) },
        )
    }
}
