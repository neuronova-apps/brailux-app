package com.brailuxaprende.data.billing

/**
 * Modelos de dominio y estados para la integración con Google Play Billing.
 *
 * Principio de Seguridad y Arquitectura:
 * - DataStore o cualquier almacenamiento local NO constituye evidencia autoritativa de compra.
 * - Un derecho o entitlement real debe originarse a partir de una compra reconocida
 *   y validada por Google Play.
 * - No considerar Firebase App Check ni Play Integrity como sustitutos de la verificación
 *   de compras.
 * - Este modelo desacopla el ciclo técnico del BillingClient de la lógica de negocio:
 *   purchaseToken -> capa de verificación -> entitlement (ownedBackgroundIds).
 * - Ni ProductDetails ni Purchase deben almacenarse directamente en DataStore.
 */

/**
 * Estado de conexión con el servicio de Google Play Billing.
 */
sealed interface BillingConnectionState {
    data object Disconnected : BillingConnectionState
    data object Connecting : BillingConnectionState
    data object Connected : BillingConnectionState
    data object Unavailable : BillingConnectionState
    data class Error(
        val message: String? = null,
        val responseCode: Int? = null,
    ) : BillingConnectionState
}

/**
 * Tipo lógico de producto en Google Play Billing.
 * Para los temas Premium de Brailux, todos los productos son de tipo [OneTime].
 */
enum class BrailuxProductType {
    OneTime,
    Subscription,
}

/**
 * Estado de disponibilidad de un producto en Google Play Billing.
 */
sealed interface BrailuxProductState {
    data object Unknown : BrailuxProductState
    data object Loading : BrailuxProductState
    data object Available : BrailuxProductState
    data object Unavailable : BrailuxProductState
}

/**
 * Estado de compra de un producto según Google Play Billing.
 */
sealed interface BrailuxPurchaseState {
    data object NotOwned : BrailuxPurchaseState
    data object Pending : BrailuxPurchaseState
    data object Purchased : BrailuxPurchaseState
    data class Error(val message: String? = null) : BrailuxPurchaseState
}

/**
 * Información de producto desacoplada de las clases internas de Google Play Billing.
 */
data class BrailuxBillingProductDetails(
    val productId: String,
    val productType: BrailuxProductType = BrailuxProductType.OneTime,
    val name: String,
    val description: String = "",
    val formattedPrice: String? = null,
    val priceAmountMicros: Long? = null,
    val priceCurrencyCode: String? = null,
    val isConsumable: Boolean = false,
)

/**
 * Registro de compra validable de dominio, independiente de la clase [com.android.billingclient.api.Purchase].
 */
data class BrailuxPurchaseRecord(
    val productId: String,
    val purchaseToken: String,
    val purchaseTimeMillis: Long,
    val purchaseState: BrailuxPurchaseState,
    val isAcknowledged: Boolean,
)
