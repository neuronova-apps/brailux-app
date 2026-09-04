package com.brailuxaprende.data.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.brailuxaprende.data.settings.accessibilityPreferencesDataStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
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
 * - Google Play Billing es la única fuente autoritativa de propiedad.
 * - [BrailuxPremiumEntitlementRepository] reconcilia compras válidas y actualiza el cache auxiliar.
 * - Las compras PENDING nunca equivalen a adquiridas ni se hace acknowledge de ellas.
 * - Los productos no recuperados (unfetched) se marcan como [BrailuxProductState.Unavailable] y no inventan precios.
 * - Cache de ProductDetails estrictamente volátil en memoria; nunca se persiste en DataStore ni se serializa.
 * - Reconciliación y acknowledgement asíncronos en [onPurchasesUpdated] mediante un [CoroutineScope] administrado.
 */
class GooglePlayBillingRepository(
    context: Context? = null,
    gateway: BrailuxBillingGateway? = null,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    val entitlementRepository: BrailuxPremiumEntitlementRepository = if (context != null) {
        BrailuxPremiumEntitlementRepository(context.applicationContext.accessibilityPreferencesDataStore)
    } else {
        BrailuxPremiumEntitlementRepository(null)
    },
    operationScope: CoroutineScope = CoroutineScope(SupervisorJob() + mainDispatcher),
    coroutineScope: CoroutineScope? = null,
) : BrailuxBillingRepository, PurchasesUpdatedListener {

    private val repositoryScope: CoroutineScope = coroutineScope ?: operationScope

    internal var lastProcessingJob: Job? = null

    private val gateway: BrailuxBillingGateway = gateway
        ?: DefaultBillingClientGateway(
            requireNotNull(context) { "Context is required when gateway is not provided" },
            this,
        )

    private val cachedProductDetails = ConcurrentHashMap<String, ProductDetails>()

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
    private val reconciliationMutex = Mutex()

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
            cachedProductDetails.clear()
            gateway.endConnection()
        } catch (_: Throwable) {
            // Safe cleanup
        } finally {
            cachedProductDetails.clear()
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

                        val purchasedRecords = mappedRecords.filter {
                            it.purchaseState is BrailuxPurchaseState.Purchased
                        }
                        if (purchasedRecords.isNotEmpty()) {
                            lastProcessingJob = repositoryScope.launch {
                                reconciliationMutex.withLock {
                                    for (record in purchasedRecords) {
                                        val granted = entitlementRepository.grantEntitlementForPurchase(record)
                                        if (granted && !record.isAcknowledged) {
                                            acknowledgePurchase(record.purchaseToken)
                                        }
                                    }
                                }
                            }
                        }
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
                // - No borrar ni modificar purchases existentes ni entitlements conocidos.
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

        val fetchedProductDetailsList = result?.productDetailsList.orEmpty()
        val fetchedDetails = fetchedProductDetailsList.mapNotNull { BrailuxBillingMapper.mapProductDetails(it) }
        val unfetchedDetails = result?.unfetchedProductList?.mapNotNull { BrailuxBillingMapper.mapUnfetchedProduct(it) }.orEmpty()

        // Cache volátil de ProductDetails SDK en memoria (representa la última consulta válida de la sesión):
        // 1. Reemplazar entradas recuperadas válidas
        val fetchedProductIds = mutableSetOf<String>()
        fetchedProductDetailsList.forEach { details ->
            if (details.productId in BrailuxBillingProductCatalog.allProductIds) {
                cachedProductDetails[details.productId] = details
                fetchedProductIds.add(details.productId)
            }
        }
        // 2. Eliminar del cache cualquier productId solicitado que resulte Unfetched o que deje de estar disponible
        result?.unfetchedProductList?.forEach { unfetched ->
            cachedProductDetails.remove(unfetched.productId)
        }
        validIds.forEach { requestedId ->
            if (requestedId !in fetchedProductIds) {
                cachedProductDetails.remove(requestedId)
            }
        }

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

        // El snapshot técnico de compras activas/pendientes de Google Play reemplaza autoritativamente _purchases
        _purchases.value = mappedRecords.associateBy { it.productId }

        // Reconciliación autoritativa: reemplaza el conjunto activo con las compras actualmente válidas de Google Play
        reconciliationMutex.withLock {
            entitlementRepository.reconcileFromPurchases(mappedRecords)
        }

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

        val records = purchasesResult.getOrThrow()
        reconciliationMutex.withLock {
            for (record in records) {
                if (record.purchaseState is BrailuxPurchaseState.Purchased &&
                    !record.isAcknowledged &&
                    entitlementRepository.isPurchaseEligible(record)
                ) {
                    acknowledgePurchase(record.purchaseToken)
                }
            }
        }

        return Result.success(Unit)
    }

    override suspend fun restorePurchases(): Result<List<BrailuxPurchaseRecord>> {
        if (!gateway.isReady || _connectionState.value !is BillingConnectionState.Connected) {
            val connectResult = startConnection()
            if (connectResult.isFailure) {
                return Result.failure(
                    connectResult.exceptionOrNull() ?: IllegalStateException("Failed to connect to billing service")
                )
            }
        }

        val queryResult = queryPurchases()
        if (queryResult.isFailure) {
            return queryResult
        }

        val records = queryResult.getOrThrow()
        reconciliationMutex.withLock {
            for (record in records) {
                if (record.purchaseState is BrailuxPurchaseState.Purchased &&
                    !record.isAcknowledged &&
                    entitlementRepository.isPurchaseEligible(record)
                ) {
                    acknowledgePurchase(record.purchaseToken)
                }
            }
        }

        return Result.success(records)
    }

    override suspend fun launchPurchaseFlow(activity: Activity, request: BrailuxPurchaseRequest): Result<Unit> {
        val productId = request.productId

        // 1. Validar productId en catálogo oficial
        if (productId !in BrailuxBillingProductCatalog.allProductIds) {
            val msg = "Product $productId does not belong to catalog"
            val error = BrailuxBillingError(BillingClient.BillingResponseCode.DEVELOPER_ERROR, msg)
            _lastBillingError.value = error
            _lastBillingOperation.value = BrailuxBillingOperationState.Error(BillingClient.BillingResponseCode.DEVELOPER_ERROR, msg)
            return Result.failure(BrailuxBillingException(BillingClient.BillingResponseCode.DEVELOPER_ERROR, msg))
        }

        // 2. Validar offerToken no vacío ni en blanco
        if (request.offerToken.isBlank()) {
            val msg = "offerToken cannot be blank"
            val error = BrailuxBillingError(BillingClient.BillingResponseCode.DEVELOPER_ERROR, msg)
            _lastBillingError.value = error
            _lastBillingOperation.value = BrailuxBillingOperationState.Error(BillingClient.BillingResponseCode.DEVELOPER_ERROR, msg)
            return Result.failure(BrailuxBillingException(BillingClient.BillingResponseCode.DEVELOPER_ERROR, msg))
        }

        // 3. Validar estado de conexión técnica del cliente
        if (!gateway.isReady || _connectionState.value !is BillingConnectionState.Connected) {
            val msg = "Billing service is not connected or ready"
            val error = BrailuxBillingError(BillingClient.BillingResponseCode.SERVICE_DISCONNECTED, msg)
            _lastBillingError.value = error
            _lastBillingOperation.value = BrailuxBillingOperationState.Error(BillingClient.BillingResponseCode.SERVICE_DISCONNECTED, msg)
            return Result.failure(BrailuxBillingException(BillingClient.BillingResponseCode.SERVICE_DISCONNECTED, msg))
        }

        // 4. Validar Activity para lanzar flujo
        val isActivityInvalid = try {
            activity.isFinishing || activity.isDestroyed
        } catch (_: Throwable) {
            false
        }
        if (isActivityInvalid) {
            val msg = "Activity is invalid (finishing or destroyed)"
            val error = BrailuxBillingError(BillingClient.BillingResponseCode.DEVELOPER_ERROR, msg)
            _lastBillingError.value = error
            _lastBillingOperation.value = BrailuxBillingOperationState.Error(BillingClient.BillingResponseCode.DEVELOPER_ERROR, msg)
            return Result.failure(BrailuxBillingException(BillingClient.BillingResponseCode.DEVELOPER_ERROR, msg))
        }

        // 5. Validar ProductDetails real en cache volátil
        val productDetails = cachedProductDetails[productId]
        if (productDetails == null) {
            val msg = "ProductDetails not cached for product $productId"
            val error = BrailuxBillingError(BillingClient.BillingResponseCode.ITEM_UNAVAILABLE, msg)
            _lastBillingError.value = error
            _lastBillingOperation.value = BrailuxBillingOperationState.Error(BillingClient.BillingResponseCode.ITEM_UNAVAILABLE, msg)
            return Result.failure(BrailuxBillingException(BillingClient.BillingResponseCode.ITEM_UNAVAILABLE, msg))
        }

        // 6. Validar que exista la oferta real coincidente con offerToken
        val rawOffers = productDetails.oneTimePurchaseOfferDetailsList
            ?: productDetails.oneTimePurchaseOfferDetails?.let { listOf(it) }
            ?: emptyList()
        val matchingOffer = rawOffers.firstOrNull { it.offerToken == request.offerToken }
        if (matchingOffer == null) {
            val msg = "offerToken ${request.offerToken} does not match any offer for product $productId"
            val error = BrailuxBillingError(BillingClient.BillingResponseCode.DEVELOPER_ERROR, msg)
            _lastBillingError.value = error
            _lastBillingOperation.value = BrailuxBillingOperationState.Error(BillingClient.BillingResponseCode.DEVELOPER_ERROR, msg)
            return Result.failure(BrailuxBillingException(BillingClient.BillingResponseCode.DEVELOPER_ERROR, msg))
        }

        // 7. Transición al estado operativo LaunchingPurchase y ejecución en Main thread
        _lastBillingOperation.value = BrailuxBillingOperationState.LaunchingPurchase

        val billingResult = withContext(mainDispatcher) {
            gateway.launchBillingFlow(activity, productDetails, request.offerToken)
        }

        // 8. Manejo de códigos de respuesta devueltos por el intento de apertura
        return when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                _lastBillingError.value = null
                _lastBillingOperation.value = BrailuxBillingOperationState.PurchaseFlowLaunched
                Result.success(Unit)
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _lastBillingError.value = null
                _lastBillingOperation.value = BrailuxBillingOperationState.UserCanceled
                Result.failure(
                    BrailuxBillingException(
                        responseCode = billingResult.responseCode,
                        message = billingResult.debugMessage.ifBlank { "User canceled purchase flow" },
                    )
                )
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                val msg = billingResult.debugMessage.ifBlank { "Item is already owned" }
                val error = BrailuxBillingError(billingResult.responseCode, msg)
                _lastBillingError.value = error
                _lastBillingOperation.value = BrailuxBillingOperationState.Error(billingResult.responseCode, msg)
                Result.failure(BrailuxBillingException(billingResult.responseCode, msg))
            }
            BillingClient.BillingResponseCode.ITEM_UNAVAILABLE -> {
                val msg = billingResult.debugMessage.ifBlank { "Item unavailable" }
                val error = BrailuxBillingError(billingResult.responseCode, msg)
                _lastBillingError.value = error
                _lastBillingOperation.value = BrailuxBillingOperationState.Error(billingResult.responseCode, msg)
                Result.failure(BrailuxBillingException(billingResult.responseCode, msg))
            }
            BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> {
                val msg = billingResult.debugMessage.ifBlank { "Billing unavailable" }
                val error = BrailuxBillingError(billingResult.responseCode, msg)
                _lastBillingError.value = error
                _lastBillingOperation.value = BrailuxBillingOperationState.Error(billingResult.responseCode, msg)
                Result.failure(BrailuxBillingException(billingResult.responseCode, msg))
            }
            BillingClient.BillingResponseCode.DEVELOPER_ERROR -> {
                val msg = billingResult.debugMessage.ifBlank { "Developer error" }
                val error = BrailuxBillingError(billingResult.responseCode, msg)
                _lastBillingError.value = error
                _lastBillingOperation.value = BrailuxBillingOperationState.Error(billingResult.responseCode, msg)
                Result.failure(BrailuxBillingException(billingResult.responseCode, msg))
            }
            BillingClient.BillingResponseCode.SERVICE_DISCONNECTED -> {
                val msg = billingResult.debugMessage.ifBlank { "Service disconnected" }
                val error = BrailuxBillingError(billingResult.responseCode, msg)
                _lastBillingError.value = error
                _lastBillingOperation.value = BrailuxBillingOperationState.Error(billingResult.responseCode, msg)
                Result.failure(BrailuxBillingException(billingResult.responseCode, msg))
            }
            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE -> {
                val msg = billingResult.debugMessage.ifBlank { "Service unavailable" }
                val error = BrailuxBillingError(billingResult.responseCode, msg)
                _lastBillingError.value = error
                _lastBillingOperation.value = BrailuxBillingOperationState.Error(billingResult.responseCode, msg)
                Result.failure(BrailuxBillingException(billingResult.responseCode, msg))
            }
            else -> {
                val msg = billingResult.debugMessage.ifBlank { "Billing error (${billingResult.responseCode})" }
                val error = BrailuxBillingError(billingResult.responseCode, msg)
                _lastBillingError.value = error
                _lastBillingOperation.value = BrailuxBillingOperationState.Error(billingResult.responseCode, msg)
                Result.failure(BrailuxBillingException(billingResult.responseCode, msg))
            }
        }
    }

    override suspend fun acknowledgePurchase(purchaseToken: String): Result<Unit> {
        if (purchaseToken.isBlank()) {
            val msg = "purchaseToken cannot be blank"
            val error = BrailuxBillingError(BillingClient.BillingResponseCode.DEVELOPER_ERROR, msg)
            _lastBillingError.value = error
            _lastBillingOperation.value = BrailuxBillingOperationState.Error(BillingClient.BillingResponseCode.DEVELOPER_ERROR, msg)
            return Result.failure(BrailuxBillingException(BillingClient.BillingResponseCode.DEVELOPER_ERROR, msg))
        }

        if (!gateway.isReady || _connectionState.value !is BillingConnectionState.Connected) {
            val msg = "Billing service is not connected or ready"
            val error = BrailuxBillingError(BillingClient.BillingResponseCode.SERVICE_DISCONNECTED, msg)
            _lastBillingError.value = error
            _lastBillingOperation.value = BrailuxBillingOperationState.Error(BillingClient.BillingResponseCode.SERVICE_DISCONNECTED, msg)
            return Result.failure(BrailuxBillingException(BillingClient.BillingResponseCode.SERVICE_DISCONNECTED, msg))
        }

        val billingResult = gateway.acknowledgePurchase(purchaseToken)

        return if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            _lastBillingError.value = null
            _lastBillingOperation.value = BrailuxBillingOperationState.Success

            val current = _purchases.value.toMutableMap()
            var updated = false
            current.forEach { (productId, record) ->
                if (record.purchaseToken == purchaseToken && !record.isAcknowledged) {
                    current[productId] = record.copy(isAcknowledged = true)
                    updated = true
                }
            }
            if (updated) {
                _purchases.value = current
            }

            Result.success(Unit)
        } else {
            val msg = billingResult.debugMessage.ifBlank {
                "Error acknowledging purchase (${billingResult.responseCode})"
            }
            val error = BrailuxBillingError(billingResult.responseCode, msg)
            _lastBillingError.value = error
            _lastBillingOperation.value = BrailuxBillingOperationState.Error(billingResult.responseCode, msg)
            Result.failure(BrailuxBillingException(billingResult.responseCode, msg))
        }
    }

    override fun isProductPurchased(productId: String): Boolean {
        return _purchases.value[productId]?.purchaseState == BrailuxPurchaseState.Purchased
    }
}
