package com.brailuxaprende.data.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryProductDetailsResult
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Contrato técnico para la interacción con [BillingClient].
 * Permite aislar la capa de bajo nivel del SDK y facilita pruebas unitarias completas
 * sin requerir un entorno Google Play activo ni librerías invasivas de mocking.
 */
interface BrailuxBillingGateway {

    /**
     * Indica si el cliente técnico está listo y conectado.
     */
    val isReady: Boolean

    /**
     * Inicia la conexión técnica con Google Play.
     */
    fun startConnection(listener: BillingClientStateListener)

    /**
     * Finaliza la conexión técnica liberando los recursos de [BillingClient].
     */
    fun endConnection()

    /**
     * Consulta asíncrona de ProductDetails devolviendo [BillingResult] y [QueryProductDetailsResult].
     */
    suspend fun queryProductDetails(productIds: List<String>): Pair<BillingResult, QueryProductDetailsResult?>

    /**
     * Consulta asíncrona de compras existentes INAPP devolviendo [BillingResult] y la lista de [Purchase].
     */
    suspend fun queryPurchases(): Pair<BillingResult, List<Purchase>>

    /**
     * Lanza el flujo de compra de Google Play Billing para un producto y oferta específicos.
     */
    fun launchBillingFlow(
        activity: Activity,
        productDetails: ProductDetails,
        offerToken: String,
    ): BillingResult
}

/**
 * Implementación de producción de [BrailuxBillingGateway] utilizando Google Play Billing 9.1.0.
 *
 * Configuración:
 * - Una única instancia de [BillingClient] por gateway.
 * - Application Context para evitar fugas de memoria.
 * - [enablePendingPurchases] habilitado explícitamente para One-Time products mediante [PendingPurchasesParams].
 * - [enableAutoServiceReconnection] habilitado.
 * - Solo consulta productos y compras de tipo [BillingClient.ProductType.INAPP].
 */
class DefaultBillingClientGateway(
    context: Context,
    purchasesUpdatedListener: PurchasesUpdatedListener,
) : BrailuxBillingGateway {

    private val appContext: Context = context.applicationContext ?: context

    private val billingClient: BillingClient by lazy {
        BillingClient.newBuilder(appContext)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .enableAutoServiceReconnection()
            .build()
    }

    override val isReady: Boolean
        get() = billingClient.isReady

    override fun startConnection(listener: BillingClientStateListener) {
        billingClient.startConnection(listener)
    }

    override fun endConnection() {
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
    }

    override suspend fun queryProductDetails(productIds: List<String>): Pair<BillingResult, QueryProductDetailsResult?> {
        val productList = productIds.map { id ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(id)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        }

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        return suspendCancellableCoroutine { continuation ->
            billingClient.queryProductDetailsAsync(params) { billingResult, queryProductDetailsResult ->
                if (continuation.isActive) {
                    continuation.resume(billingResult to queryProductDetailsResult)
                }
            }
        }
    }

    override suspend fun queryPurchases(): Pair<BillingResult, List<Purchase>> {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        return suspendCancellableCoroutine { continuation ->
            billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
                if (continuation.isActive) {
                    continuation.resume(billingResult to (purchases ?: emptyList()))
                }
            }
        }
    }

    override fun launchBillingFlow(
        activity: Activity,
        productDetails: ProductDetails,
        offerToken: String,
    ): BillingResult {
        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
            .setOfferToken(offerToken)
            .build()

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()

        return billingClient.launchBillingFlow(activity, billingFlowParams)
    }
}

