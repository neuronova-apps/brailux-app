package com.brailuxaprende.data.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Excepción específica para errores reportados por Google Play Billing.
 */
class BrailuxBillingException(
    val responseCode: Int,
    message: String,
) : Exception(message)

/**
 * Implementación de [BrailuxBillingRepository] para Google Play Billing 9.1.0.
 *
 * Principios y restricciones de seguridad:
 * - Gestiona una única instancia de [BillingClient] a través de [BrailuxBillingGateway].
 * - Solo consulta productos y compras INAPP pertenecientes al catálogo oficial [BrailuxBillingProductCatalog].
 * - NO concede ningún derecho o entitlement en esta Fase B: ni [onPurchasesUpdated], ni [queryPurchases],
 *   ni [syncBillingData], ni [restorePurchases] modifican ownedBackgroundIds ni BrailuxPremiumAccess.
 * - Las compras PENDING nunca equivalen a adquiridas.
 * - Los productos no recuperados (unfetched) se marcan como [BrailuxProductState.Unavailable] y no inventan precios.
 * - Los métodos [launchPurchaseFlow] y [acknowledgePurchase] devuelven explícitamente fallo al no estar implementados.
 */
class GooglePlayBillingRepository(
    context: Context? = null,
    gateway: BrailuxBillingGateway? = null,
) : BrailuxBillingRepository, PurchasesUpdatedListener {

    private val gateway: BrailuxBillingGateway = gateway
        ?: DefaultBillingClientGateway(
            requireNotNull(context) { "Context is required when gateway is not provided" },
            this,
        )

    private val _connectionState = MutableStateFlow<BillingConnectionState>(BillingConnectionState.Disconnected)
    override val connectionState: StateFlow<BillingConnectionState> = _connectionState.asStateFlow()

    private val _products = MutableStateFlow<Map<String, BrailuxBillingProductDetails>>(emptyMap())
    override val products: StateFlow<Map<String, BrailuxBillingProductDetails>> = _products.asStateFlow()

    private val _purchases = MutableStateFlow<Map<String, BrailuxPurchaseRecord>>(emptyMap())
    override val purchases: StateFlow<Map<String, BrailuxPurchaseRecord>> = _purchases.asStateFlow()

    private val _lastBillingError = MutableStateFlow<BrailuxBillingError?>(null)
    override val lastBillingError: StateFlow<BrailuxBillingError?> = _lastBillingError.asStateFlow()

    private val _lastBillingOperation = MutableStateFlow<BrailuxBillingOperationState>(BrailuxBillingOperationState.Idle)
    override val lastBillingOperation: StateFlow<BrailuxBillingOperationState> = _lastBillingOperation.asStateFlow()

    private val connectionMutex = Mutex()

    override suspend fun startConnection(): Result<Unit> = connectionMutex.withLock {
        if (gateway.isReady && _connectionState.value is BillingConnectionState.Connected) {
            return Result.success(Unit)
        }

        _connectionState.value = BillingConnectionState.Connecting

        return suspendCancellableCoroutine { continuation ->
            gateway.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    when (billingResult.responseCode) {
                        BillingClient.BillingResponseCode.OK -> {
                            _connectionState.value = BillingConnectionState.Connected
                            if (continuation.isActive) {
                                continuation.resume(Result.success(Unit))
                            }
                        }
                        BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> {
                            _connectionState.value = BillingConnectionState.Unavailable
                            if (continuation.isActive) {
                                continuation.resume(
                                    Result.failure(
                                        BrailuxBillingException(
                                            responseCode = billingResult.responseCode,
                                            message = billingResult.debugMessage.ifBlank { "Billing service unavailable" },
                                        )
                                    )
                                )
                            }
                        }
                        else -> {
                            val msg = billingResult.debugMessage.ifBlank { "Billing connection error (${billingResult.responseCode})" }
                            _connectionState.value = BillingConnectionState.Error(
                                message = msg,
                                responseCode = billingResult.responseCode,
                            )
                            if (continuation.isActive) {
                                continuation.resume(
                                    Result.failure(
                                        BrailuxBillingException(
                                            responseCode = billingResult.responseCode,
                                            message = msg,
                                        )
                                    )
                                )
                            }
                        }
                    }
                }

                override fun onBillingServiceDisconnected() {
                    // enableAutoServiceReconnection handles reconnection behind the scenes.
                    // Update connection state without entering an aggressive manual reconnect loop.
                    _connectionState.value = BillingConnectionState.Disconnected
                }
            })
        }
    }

    override fun endConnection() {
        try {
            gateway.endConnection()
        } catch (_: Throwable) {
            // Safe cleanup
        } finally {
            _connectionState.value = BillingConnectionState.Disconnected
            _lastBillingOperation.value = BrailuxBillingOperationState.Idle
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                _lastBillingError.value = null
                _lastBillingOperation.value = BrailuxBillingOperationState.Success
                if (purchases != null) {
                    val mappedRecords = purchases.flatMap { BrailuxBillingMapper.mapPurchase(it) }
                    if (mappedRecords.isNotEmpty()) {
                        val current = _purchases.value.toMutableMap()
                        mappedRecords.forEach { record ->
                            current[record.productId] = record
                        }
                        _purchases.value = current
                    }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                // El usuario canceló la compra o el diálogo de facturación:
                // - No modificar purchases existentes.
                // - No tratarlo como compra.
                // - No conceder ningún entitlement.
                // - No marcar como error fatal ni alterar connectionState.
                _lastBillingError.value = null
                _lastBillingOperation.value = BrailuxBillingOperationState.UserCanceled
            }
            else -> {
                // Error técnico de facturación distinto de OK:
                // - No borrar ni modificar purchases existentes.
                // - Exponer como error técnico observable sin contaminar connectionState.
                val errorMsg = billingResult.debugMessage.ifBlank {
                    "Billing error on purchases update (${billingResult.responseCode})"
                }
                val error = BrailuxBillingError(
                    responseCode = billingResult.responseCode,
                    message = errorMsg,
                )
                _lastBillingError.value = error
                _lastBillingOperation.value = BrailuxBillingOperationState.Error(
                    responseCode = billingResult.responseCode,
                    message = errorMsg,
                )
            }
        }
        // REGLA ABSOLUTA DE ENTITLEMENT:
        // No conceder derechos, no alterar ownedBackgroundIds, no acknowledge, no DataStore.
    }

    override suspend fun queryProductDetails(productIds: Set<String>): Result<List<BrailuxBillingProductDetails>> {
        val validIds = productIds.filter { it in BrailuxBillingProductCatalog.allProductIds }
        if (validIds.isEmpty()) {
            return Result.success(emptyList())
        }

        val (billingResult, result) = gateway.queryProductDetails(validIds)

        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            val msg = billingResult.debugMessage.ifBlank { "Error querying product details (${billingResult.responseCode})" }
            val error = BrailuxBillingError(billingResult.responseCode, msg)
            _lastBillingError.value = error
            _lastBillingOperation.value = BrailuxBillingOperationState.Error(billingResult.responseCode, msg)
            return Result.failure(BrailuxBillingException(billingResult.responseCode, msg))
        }

        val fetchedDetails = result?.productDetailsList?.mapNotNull { BrailuxBillingMapper.mapProductDetails(it) } ?: emptyList()
        val unfetchedDetails = result?.unfetchedProductList?.mapNotNull { BrailuxBillingMapper.mapUnfetchedProduct(it) } ?: emptyList()

        val allDetails = fetchedDetails + unfetchedDetails
        val current = _products.value.toMutableMap()
        allDetails.forEach { details ->
            current[details.productId] = details
        }
        _products.value = current

        return Result.success(allDetails)
    }

    override suspend fun queryPurchases(): Result<List<BrailuxPurchaseRecord>> {
        val (billingResult, purchasesList) = gateway.queryPurchases()

        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            val msg = billingResult.debugMessage.ifBlank { "Error querying purchases (${billingResult.responseCode})" }
            val error = BrailuxBillingError(billingResult.responseCode, msg)
            _lastBillingError.value = error
            _lastBillingOperation.value = BrailuxBillingOperationState.Error(billingResult.responseCode, msg)
            return Result.failure(BrailuxBillingException(billingResult.responseCode, msg))
        }

        val mappedRecords = purchasesList.flatMap { BrailuxBillingMapper.mapPurchase(it) }

        val current = _purchases.value.toMutableMap()
        mappedRecords.forEach { record ->
            current[record.productId] = record
        }
        _purchases.value = current

        // REGLA ABSOLUTA DE ENTITLEMENT:
        // NO modificar todavía BrailuxPremiumAccess.currentState ni ownedBackgroundIds.
        return Result.success(mappedRecords)
    }

    override suspend fun syncBillingData(): Result<Unit> {
        if (!gateway.isReady || _connectionState.value !is BillingConnectionState.Connected) {
            val connectResult = startConnection()
            if (connectResult.isFailure) {
                return Result.failure(
                    connectResult.exceptionOrNull() ?: IllegalStateException("Failed to connect to billing service")
                )
            }
        }

        val detailsResult = queryProductDetails(BrailuxBillingProductCatalog.allProductIds.toSet())
        if (detailsResult.isFailure) {
            return Result.failure(
                detailsResult.exceptionOrNull() ?: IllegalStateException("Failed to query product details")
            )
        }

        val purchasesResult = queryPurchases()
        if (purchasesResult.isFailure) {
            return Result.failure(
                purchasesResult.exceptionOrNull() ?: IllegalStateException("Failed to query purchases")
            )
        }

        // NO conceder derechos ni hacer acknowledge.
        return Result.success(Unit)
    }

    override suspend fun restorePurchases(): Result<List<BrailuxPurchaseRecord>> {
        // En esta fase solo consulta compras activas. No modifica DataStore, no desbloquea UI.
        return queryPurchases()
    }

    override suspend fun launchPurchaseFlow(activity: Activity, productId: String): Result<Unit> {
        // NO implementar todavía en Fase B
        return Result.failure(
            UnsupportedOperationException("Purchase flow is not supported in Phase B")
        )
    }

    override suspend fun acknowledgePurchase(purchaseToken: String): Result<Unit> {
        // NO implementar todavía en Fase B
        return Result.failure(
            UnsupportedOperationException("Purchase acknowledgment is not supported in Phase B")
        )
    }

    override fun isProductPurchased(productId: String): Boolean {
        return _purchases.value[productId]?.purchaseState == BrailuxPurchaseState.Purchased
    }
}
