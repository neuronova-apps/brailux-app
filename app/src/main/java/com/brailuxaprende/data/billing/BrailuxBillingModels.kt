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
 * Detalles de una oferta individual one-time (compra única) desacoplada del SDK de Google Play Billing.
 *
 * Utiliza exclusivamente campos soportados por Google Play Billing 9.1.0:
 * - [offerToken]: Token requerido para procesar el flujo de compra.
 * - [purchaseOptionId]: Identificador de opción de compra si existe.
 * - [offerId]: Identificador de oferta si existe.
 * - [formattedPrice]: Precio formateado con símbolo de divisa.
 * - [priceAmountMicros]: Precio numérico en micro-unidades (ej: 1990000 para $1.99).
 * - [priceCurrencyCode]: Código de divisa ISO 4217 (ej: "USD").
 */
data class BrailuxOneTimeOfferDetails(
    val offerToken: String,
    val purchaseOptionId: String?,
    val offerId: String?,
    val formattedPrice: String?,
    val priceAmountMicros: Long?,
    val priceCurrencyCode: String?,
)

/**
 * Información de producto desacoplada de las clases internas de Google Play Billing.
 *
 * Compatibilidad de precios:
 * Los campos [formattedPrice], [priceAmountMicros] y [priceCurrencyCode] son una representación
 * resumida y compatible (generalmente derivada de la oferta inicial) para visualización básica
 * y compatibilidad hacia atrás. NO son la única fuente ni determinan la selección de oferta
 * para el futuro flujo de compra.
 * La fuente completa y autoritativa de ofertas disponibles se encuentra en [oneTimeOffers].
 */
data class BrailuxBillingProductDetails(
    val productId: String,
    val productType: BrailuxProductType = BrailuxProductType.OneTime,
    val name: String,
    val description: String = "",
    val formattedPrice: String? = null,
    val priceAmountMicros: Long? = null,
    val priceCurrencyCode: String? = null,
    val oneTimeOffers: List<BrailuxOneTimeOfferDetails> = emptyList(),
    val isConsumable: Boolean = false,
    val state: BrailuxProductState = BrailuxProductState.Available,
    val unfetchedStatusCode: Int? = null,
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

/**
 * Error técnico reportado por la capa de Google Play Billing.
 * Permite monitorear fallos técnicos de operaciones sin alterar [BillingConnectionState]
 * cuando el error no corresponde a una falla de conexión.
 */
data class BrailuxBillingError(
    val responseCode: Int,
    val message: String,
)

/**
 * Solicitud explícita de compra para Google Play Billing.
 *
 * Exige tanto el [productId] del producto en el catálogo oficial como el [offerToken]
 * específico de la oferta seleccionada, garantizando que ninguna oferta sea
 * elegida de forma implícita o automática.
 */
data class BrailuxPurchaseRequest(
    val productId: String,
    val offerToken: String,
)

/**
 * Estado observable de la última operación técnica procesada por Google Play Billing.
 */
sealed interface BrailuxBillingOperationState {
    data object Idle : BrailuxBillingOperationState
    data object Success : BrailuxBillingOperationState
    data object LaunchingPurchase : BrailuxBillingOperationState
    data object PurchaseFlowLaunched : BrailuxBillingOperationState
    data object UserCanceled : BrailuxBillingOperationState
    data class Error(
        val responseCode: Int,
        val message: String,
    ) : BrailuxBillingOperationState
}


