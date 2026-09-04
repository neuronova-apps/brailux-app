package com.brailuxaprende.data.billing

import android.app.Activity
import kotlinx.coroutines.flow.StateFlow

/**
 * Contrato de repositorio para operaciones con Google Play Billing.
 *
 * Principio de Diseño y Seguridad:
 * - Define las operaciones y flujos de estado necesarios para gestionar la conexión,
 *   la consulta del catálogo, el inicio de compras, la confirmación de transacciones y
 *   la reconciliación de derechos (entitlements).
 * - Desacopla la lógica de la aplicación de la implementación concreta de
 *   [com.android.billingclient.api.BillingClient].
 * - En esta Fase A solo se define el contrato; la implementación real de BillingClient
 *   se desarrollará en fases posteriores.
 * - DataStore y cualquier almacenamiento local NO constituyen evidencia autoritativa
 *   de compra: el desbloqueo de temas Premium debe originarse de una compra reconocida
 *   y validada por Google Play.
 */
interface BrailuxBillingRepository {

    /**
     * Flujo reactivo del estado de conexión con el servicio de Google Play Billing.
     */
    val connectionState: StateFlow<BillingConnectionState>

    /**
     * Flujo reactivo de productos consultados desde Google Play,
     * indexados por su productId técnico.
     */
    val products: StateFlow<Map<String, BrailuxBillingProductDetails>>

    /**
     * Flujo reactivo de compras activas conocidas, indexadas por productId técnico.
     */
    val purchases: StateFlow<Map<String, BrailuxPurchaseRecord>>

    /**
     * Flujo reactivo del último error técnico observable reportado por Google Play Billing.
     * Permite observar fallos técnicos (por ejemplo en PurchasesUpdatedListener)
     * sin alterar [connectionState] si el error no corresponde a la conexión.
     */
    val lastBillingError: StateFlow<BrailuxBillingError?>

    /**
     * Flujo reactivo del estado de la última operación técnica procesada por BillingClient.
     */
    val lastBillingOperation: StateFlow<BrailuxBillingOperationState>

    /**
     * Inicia la conexión asíncrona con el servicio de Google Play Billing.
     */
    suspend fun startConnection(): Result<Unit>

    /**
     * Cierra la conexión activa con Google Play Billing liberando recursos.
     */
    fun endConnection()

    /**
     * Consulta detalles actualizados de los productos especificados en Google Play.
     */
    suspend fun queryProductDetails(productIds: Set<String>): Result<List<BrailuxBillingProductDetails>>

    /**
     * Consulta y actualiza el estado de las compras existentes del usuario.
     */
    suspend fun queryPurchases(): Result<List<BrailuxPurchaseRecord>>

    /**
     * Sincroniza tanto los productos disponibles como las compras activas.
     */
    suspend fun syncBillingData(): Result<Unit>

    /**
     * Inicia el flujo de compra de Google Play para el producto y oferta especificados en [request].
     */
    suspend fun launchPurchaseFlow(activity: Activity, request: BrailuxPurchaseRequest): Result<Unit>

    /**
     * Sobrecarga de conveniencia que construye [BrailuxPurchaseRequest] con selección explícita de oferta.
     */
    suspend fun launchPurchaseFlow(
        activity: Activity,
        productId: String,
        offerToken: String,
    ): Result<Unit> = launchPurchaseFlow(activity, BrailuxPurchaseRequest(productId, offerToken))

    /**
     * Restaura y reconcilia compras existentes con el estado local.
     */
    suspend fun restorePurchases(): Result<List<BrailuxPurchaseRecord>>

    /**
     * Confirma (acknowledge) una compra ante Google Play mediante su [purchaseToken].
     */
    suspend fun acknowledgePurchase(purchaseToken: String): Result<Unit>

    /**
     * Verifica de manera sincrónica si un producto específico figura como adquirido en el estado actual.
     */
    fun isProductPurchased(productId: String): Boolean
}
