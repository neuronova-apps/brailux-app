package com.brailuxaprende.data.settings

data class AccessibilityPreferences(
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val highContrastEnabled: Boolean = false,
    val textSize: TextSizePreference = TextSizePreference.Normal,
)

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
