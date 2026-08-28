package com.brailuxaprende.data.settings

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.brailuxaprende.R

data class BrailuxBackgroundOption(
    val id: String,
    @param:StringRes val nameResource: Int,
    @param:DrawableRes val drawableResource: Int?,
    val premium: Boolean,
    val available: Boolean,
)

object BrailuxBackgroundCatalog {
    const val DEFAULT_ID = "default"
    const val CELESTE_GEOMETRICO_ID = "celeste_geometrico"
    const val CREMA_ONDAS_ID = "crema_ondas"
    const val LAVANDA_NIEBLA_ID = "lavanda_niebla"
    const val SALVIA_TEXTURA_ID = "salvia_textura"

    val backgrounds: List<BrailuxBackgroundOption> = listOf(
        BrailuxBackgroundOption(
            id = DEFAULT_ID,
            nameResource = R.string.background_default,
            drawableResource = null,
            premium = false,
            available = true,
        ),
        BrailuxBackgroundOption(
            id = CELESTE_GEOMETRICO_ID,
            nameResource = R.string.background_celeste_geometrico,
            drawableResource = R.drawable.bg_brailux_celeste_geometrico,
            premium = true,
            available = true,
        ),
        BrailuxBackgroundOption(
            id = CREMA_ONDAS_ID,
            nameResource = R.string.background_crema_ondas,
            drawableResource = R.drawable.bg_brailux_crema_ondas,
            premium = true,
            available = true,
        ),
        BrailuxBackgroundOption(
            id = LAVANDA_NIEBLA_ID,
            nameResource = R.string.background_lavanda_niebla,
            drawableResource = R.drawable.bg_brailux_lavanda_niebla,
            premium = true,
            available = true,
        ),
        BrailuxBackgroundOption(
            id = SALVIA_TEXTURA_ID,
            nameResource = R.string.background_salvia_textura,
            drawableResource = R.drawable.bg_brailux_salvia_textura,
            premium = true,
            available = true,
        ),
    )

    fun option(id: String?): BrailuxBackgroundOption? =
        backgrounds.firstOrNull { background -> background.id == id }

    fun normalizedId(id: String?): String = option(id)?.id ?: DEFAULT_ID

    fun canSelect(id: String?, isPremiumUnlocked: Boolean): Boolean {
        val background = option(id) ?: return false
        return background.available && (!background.premium || isPremiumUnlocked)
    }

    fun selectionAfterRequest(
        currentId: String?,
        requestedId: String?,
        isPremiumUnlocked: Boolean,
    ): String = if (canSelect(requestedId, isPremiumUnlocked)) {
        requireNotNull(option(requestedId)).id
    } else {
        normalizedId(currentId)
    }

    @DrawableRes
    fun activeDrawableResource(
        selectedId: String?,
        isPremiumUnlocked: Boolean,
        highContrastEnabled: Boolean,
    ): Int? {
        if (highContrastEnabled || !canSelect(selectedId, isPremiumUnlocked)) return null
        return option(selectedId)?.drawableResource
    }
}
