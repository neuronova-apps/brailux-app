package com.brailuxaprende.data.billing

import com.brailuxaprende.data.settings.BrailuxBackgroundCatalog

/**
 * Representa una entrada de producto en el catálogo de Google Play Billing de Brailux.
 *
 * Cada tema Premium corresponde a una compra única (One-Time purchase) no consumible.
 */
data class BrailuxBillingProductItem(
    val productId: String,
    val backgroundId: String,
    val productType: BrailuxProductType = BrailuxProductType.OneTime,
    val isConsumable: Boolean = false,
)

/**
 * Catálogo técnico de productos de Google Play Billing para temas Premium de Brailux.
 *
 * Mapeo oficial:
 * - brailux_theme_celeste_geometrico -> celeste_geometrico
 * - brailux_theme_crema_ondas -> crema_ondas
 * - brailux_theme_lavanda_niebla -> lavanda_niebla
 * - brailux_theme_salvia_textura -> salvia_textura
 *
 * NOTA IMPORTANTE:
 * Estos IDs técnicos son el contrato estable del código. No implican que los productos
 * ya existan en Play Console ni que Play Console los reconozca en esta fase.
 */
object BrailuxBillingProductCatalog {

    const val PRODUCT_ID_CELESTE_GEOMETRICO = "brailux_theme_celeste_geometrico"
    const val PRODUCT_ID_CREMA_ONDAS = "brailux_theme_crema_ondas"
    const val PRODUCT_ID_LAVANDA_NIEBLA = "brailux_theme_lavanda_niebla"
    const val PRODUCT_ID_SALVIA_TEXTURA = "brailux_theme_salvia_textura"

    val products: List<BrailuxBillingProductItem> = listOf(
        BrailuxBillingProductItem(
            productId = PRODUCT_ID_CELESTE_GEOMETRICO,
            backgroundId = BrailuxBackgroundCatalog.CELESTE_GEOMETRICO_ID,
            productType = BrailuxProductType.OneTime,
            isConsumable = false,
        ),
        BrailuxBillingProductItem(
            productId = PRODUCT_ID_CREMA_ONDAS,
            backgroundId = BrailuxBackgroundCatalog.CREMA_ONDAS_ID,
            productType = BrailuxProductType.OneTime,
            isConsumable = false,
        ),
        BrailuxBillingProductItem(
            productId = PRODUCT_ID_LAVANDA_NIEBLA,
            backgroundId = BrailuxBackgroundCatalog.LAVANDA_NIEBLA_ID,
            productType = BrailuxProductType.OneTime,
            isConsumable = false,
        ),
        BrailuxBillingProductItem(
            productId = PRODUCT_ID_SALVIA_TEXTURA,
            backgroundId = BrailuxBackgroundCatalog.SALVIA_TEXTURA_ID,
            productType = BrailuxProductType.OneTime,
            isConsumable = false,
        ),
    )

    val allProductIds: List<String> = products.map { it.productId }

    val allBackgroundIds: List<String> = products.map { it.backgroundId }

    fun findProductByProductId(productId: String?): BrailuxBillingProductItem? {
        if (productId.isNullOrBlank()) return null
        return products.firstOrNull { it.productId == productId }
    }

    fun findProductByBackgroundId(backgroundId: String?): BrailuxBillingProductItem? {
        if (backgroundId.isNullOrBlank()) return null
        return products.firstOrNull { it.backgroundId == backgroundId }
    }

    fun backgroundIdFor(productId: String?): String? =
        findProductByProductId(productId)?.backgroundId

    fun productIdFor(backgroundId: String?): String? =
        findProductByBackgroundId(backgroundId)?.productId

    fun isBillingTheme(backgroundId: String?): Boolean =
        findProductByBackgroundId(backgroundId) != null
}
