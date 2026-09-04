package com.brailuxaprende.data.settings

data class AccessibilityPreferences(
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val highContrastEnabled: Boolean = false,
    val textSize: TextSizePreference = TextSizePreference.Normal,
    val appearance: AppearancePreference = AppearancePreference.Light,
    val seasonalThemesEnabled: Boolean = true,
    val selectedBackgroundId: String = BrailuxBackgroundCatalog.DEFAULT_ID,
    val backgroundRotationMode: BackgroundRotationMode = BackgroundRotationMode.Fixed,
)

enum class AppearancePreference(internal val storedValue: String) {
    System(storedValue = "system"),
    Light(storedValue = "light"),
    Dark(storedValue = "dark"),
    ;

    companion object {
        fun fromStoredValue(value: String?): AppearancePreference =
            entries.firstOrNull { it.storedValue == value } ?: Light
    }
}

enum class TextSizePreference(
    internal val storedValue: String,
    val scaleFactor: Float,
) {
    Normal(storedValue = "normal", scaleFactor = 1f),
    Large(storedValue = "large", scaleFactor = 1.15f),
    VeryLarge(storedValue = "very_large", scaleFactor = 1.3f),
    ;

    companion object {
        fun fromStoredValue(value: String?): TextSizePreference =
            entries.firstOrNull { it.storedValue == value } ?: Normal
    }
}
