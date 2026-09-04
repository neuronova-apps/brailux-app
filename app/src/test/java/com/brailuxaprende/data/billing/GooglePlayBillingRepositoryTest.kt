package com.brailuxaprende.data.billing

import android.app.Activity
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsResult
import com.android.billingclient.api.UnfetchedProduct
import com.brailuxaprende.data.settings.BrailuxPremiumAccess
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GooglePlayBillingRepositoryTest {

    private class FakeBrailuxBillingGateway : BrailuxBillingGateway {
        var isReadyValue: Boolean = false
        override val isReady: Boolean
            get() = isReadyValue

        var connectionListener: BillingClientStateListener? = null
        var startConnectionCount: Int = 0
        var endConnectionCount: Int = 0

        var queryProductDetailsResult: Pair<BillingResult, QueryProductDetailsResult?> =
            BillingResult.newBuilder().setResponseCode(BillingClient.BillingResponseCode.OK).build() to null
        var queriedProductIds: List<String> = emptyList()

        var queryPurchasesResult: Pair<BillingResult, List<Purchase>> =
            BillingResult.newBuilder().setResponseCode(BillingClient.BillingResponseCode.OK).build() to emptyList()
        var queryPurchasesCount: Int = 0

        override fun startConnection(listener: BillingClientStateListener) {
            startConnectionCount++
            connectionListener = listener
        }

        override fun endConnection() {
            endConnectionCount++
            isReadyValue = false
        }

        override suspend fun queryProductDetails(productIds: List<String>): Pair<BillingResult, QueryProductDetailsResult?> {
            queriedProductIds = productIds
            return queryProductDetailsResult
        }

        override suspend fun queryPurchases(): Pair<BillingResult, List<Purchase>> {
            queryPurchasesCount++
            return queryPurchasesResult
        }

        fun completeConnection(responseCode: Int, debugMessage: String = "") {
            isReadyValue = (responseCode == BillingClient.BillingResponseCode.OK)
            val result = BillingResult.newBuilder()
                .setResponseCode(responseCode)
                .setDebugMessage(debugMessage)
                .build()
            connectionListener?.onBillingSetupFinished(result)
        }

        fun triggerDisconnect() {
            isReadyValue = false
            connectionListener?.onBillingServiceDisconnected()
        }
    }

    // 1. PENDING nunca equivale a Purchased
    @Test
    fun pendingNeverEqualsPurchased() {
        val pendingRecords = BrailuxBillingMapper.mapPurchaseData(
            productIds = listOf(BrailuxBillingProductCatalog.PRODUCT_ID_CELESTE_GEOMETRICO),
            purchaseToken = "token_pending_1",
            purchaseTimeMillis = 1000L,
            purchaseStateCode = Purchase.PurchaseState.PENDING,
            isAcknowledged = false,
        )

        assertEquals(1, pendingRecords.size)
        val record = pendingRecords.first()
        assertEquals(BrailuxPurchaseState.Pending, record.purchaseState)
        assertFalse("Pending nunca debe ser Purchased", record.purchaseState == BrailuxPurchaseState.Purchased)
    }

    // 2. PURCHASED se mapea correctamente
    @Test
    fun purchasedMapsCorrectly() {
        val records = BrailuxBillingMapper.mapPurchaseData(
            productIds = listOf(BrailuxBillingProductCatalog.PRODUCT_ID_CREMA_ONDAS),
            purchaseToken = "token_purchased_2",
            purchaseTimeMillis = 2000L,
            purchaseStateCode = Purchase.PurchaseState.PURCHASED,
            isAcknowledged = true,
        )

        assertEquals(1, records.size)
        val record = records.first()
        assertEquals(BrailuxBillingProductCatalog.PRODUCT_ID_CREMA_ONDAS, record.productId)
        assertEquals("token_purchased_2", record.purchaseToken)
        assertEquals(2000L, record.purchaseTimeMillis)
        assertEquals(BrailuxPurchaseState.Purchased, record.purchaseState)
        assertTrue(record.isAcknowledged)
    }

    // 3. Compra de producto desconocido no genera derecho
    @Test
    fun unknownProductPurchaseDoesNotGenerateEntitlementOrRecord() = runBlocking {
        val gateway = FakeBrailuxBillingGateway()
        val repository = GooglePlayBillingRepository(gateway = gateway)

        val unknownRecords = BrailuxBillingMapper.mapPurchaseData(
            productIds = listOf("unknown_theme_gold_metallic"),
            purchaseToken = "token_unknown",
            purchaseTimeMillis = 3000L,
            purchaseStateCode = Purchase.PurchaseState.PURCHASED,
            isAcknowledged = true,
        )

        assertTrue("Productos desconocidos deben ser ignorados por el mapper", unknownRecords.isEmpty())
        assertFalse(repository.isProductPurchased("unknown_theme_gold_metallic"))
        assertTrue("ownedBackgroundIds debe permanecer vacío", BrailuxPremiumAccess.currentState.ownedBackgroundIds.isEmpty())
    }

    // 4. Múltiples productIds se filtran correctamente
    @Test
    fun multipleProductIdsFilteredCorrectly() {
        val records = BrailuxBillingMapper.mapPurchaseData(
            productIds = listOf(
                BrailuxBillingProductCatalog.PRODUCT_ID_CELESTE_GEOMETRICO,
                "unknown_external_product",
                BrailuxBillingProductCatalog.PRODUCT_ID_LAVANDA_NIEBLA,
                "another_random_id",
            ),
            purchaseToken = "token_multi",
            purchaseTimeMillis = 4000L,
            purchaseStateCode = Purchase.PurchaseState.PURCHASED,
            isAcknowledged = false,
        )

        assertEquals(2, records.size)
        val productIds = records.map { it.productId }.toSet()
        assertTrue(BrailuxBillingProductCatalog.PRODUCT_ID_CELESTE_GEOMETRICO in productIds)
        assertTrue(BrailuxBillingProductCatalog.PRODUCT_ID_LAVANDA_NIEBLA in productIds)
        assertFalse("unknown_external_product" in productIds)
        assertFalse("another_random_id" in productIds)
    }

    // 5. Catálogo completo genera QueryProductDetails correcto
    @Test
    fun fullCatalogGeneratesCorrectProductDetailsQuery() = runBlocking {
        val gateway = FakeBrailuxBillingGateway()
        gateway.isReadyValue = true
        val repository = GooglePlayBillingRepository(gateway = gateway)

        val connectJob = launch(Dispatchers.Unconfined) {
            repository.startConnection()
        }
        gateway.completeConnection(BillingClient.BillingResponseCode.OK)
        connectJob.join()

        repository.queryProductDetails(BrailuxBillingProductCatalog.allProductIds.toSet())

        assertEquals(4, gateway.queriedProductIds.size)
        assertEquals(BrailuxBillingProductCatalog.allProductIds.toSet(), gateway.queriedProductIds.toSet())
    }

    // 6. Los cuatro productos usan ProductType.INAPP
    @Test
    fun allFourProductsUseProductTypeInapp() {
        assertEquals(4, BrailuxBillingProductCatalog.products.size)
        assertTrue(
            "Todos los productos deben ser OneTime (INAPP no consumible)",
            BrailuxBillingProductCatalog.products.all { it.productType == BrailuxProductType.OneTime && !it.isConsumable },
        )
    }

    // 7. ProductDetails se mapea correctamente
    @Test
    fun productDetailsMapsCorrectly() {
        val details = BrailuxBillingMapper.mapProductDetailsData(
            productId = BrailuxBillingProductCatalog.PRODUCT_ID_CELESTE_GEOMETRICO,
            name = "Celeste Geométrico",
            description = "Fondo decorativo accesible",
            formattedPrice = "$1.99",
            priceAmountMicros = 1990000L,
            priceCurrencyCode = "USD",
        )

        assertNotNull(details)
        assertEquals(BrailuxBillingProductCatalog.PRODUCT_ID_CELESTE_GEOMETRICO, details?.productId)
        assertEquals("Celeste Geométrico", details?.name)
        assertEquals("Fondo decorativo accesible", details?.description)
        assertEquals("$1.99", details?.formattedPrice)
        assertEquals(1990000L, details?.priceAmountMicros)
        assertEquals("USD", details?.priceCurrencyCode)
        assertEquals(BrailuxProductType.OneTime, details?.productType)
        assertEquals(BrailuxProductState.Available, details?.state)
        assertNull(details?.unfetchedStatusCode)
    }

    // 8. Precio localizado no se inventa
    @Test
    fun localizedPriceNotInventedWhenMissing() {
        val details = BrailuxBillingMapper.mapProductDetailsData(
            productId = BrailuxBillingProductCatalog.PRODUCT_ID_SALVIA_TEXTURA,
            name = "Salvia Textura",
            description = "Fondo salvia",
            formattedPrice = null,
            priceAmountMicros = null,
            priceCurrencyCode = null,
        )

        assertNotNull(details)
        assertNull("formattedPrice debe ser null si no lo devolvió Play Billing", details?.formattedPrice)
        assertNull("priceAmountMicros debe ser null si no lo devolvió Play Billing", details?.priceAmountMicros)
        assertNull("priceCurrencyCode debe ser null si no lo devolvió Play Billing", details?.priceCurrencyCode)
    }

    // 9. Producto unfetched queda Unavailable
    @Test
    fun unfetchedProductMarkedAsUnavailableWithStatusCode() {
        val unfetched = BrailuxBillingMapper.mapUnfetchedProductData(
            productId = BrailuxBillingProductCatalog.PRODUCT_ID_LAVANDA_NIEBLA,
            statusCode = UnfetchedProduct.StatusCode.PRODUCT_NOT_FOUND,
        )

        assertNotNull(unfetched)
        assertEquals(BrailuxBillingProductCatalog.PRODUCT_ID_LAVANDA_NIEBLA, unfetched?.productId)
        assertEquals(BrailuxProductState.Unavailable, unfetched?.state)
        assertEquals(UnfetchedProduct.StatusCode.PRODUCT_NOT_FOUND, unfetched?.unfetchedStatusCode)
        assertNull("No debe haber precio inventado para producto unfetched", unfetched?.formattedPrice)
        assertNull("No debe haber precio en micros para producto unfetched", unfetched?.priceAmountMicros)
        assertNull("No debe haber código de moneda para producto unfetched", unfetched?.priceCurrencyCode)
    }

    // 10. Error BillingResult queda representado
    @Test
    fun errorBillingResultRepresentedInConnectionState() = runBlocking {
        val gateway = FakeBrailuxBillingGateway()
        val repository = GooglePlayBillingRepository(gateway = gateway)

        val connectJob = launch(Dispatchers.Unconfined) {
            repository.startConnection()
        }
        gateway.completeConnection(BillingClient.BillingResponseCode.DEVELOPER_ERROR, "Config error")
        connectJob.join()

        val state = repository.connectionState.value
        assertTrue("Estado debe ser BillingConnectionState.Error", state is BillingConnectionState.Error)
        val errorState = state as BillingConnectionState.Error
        assertEquals(BillingClient.BillingResponseCode.DEVELOPER_ERROR, errorState.responseCode)
        assertEquals("Config error", errorState.message)
    }

    // 11. Conexión inicial Disconnected
    @Test
    fun initialConnectionStateIsDisconnected() {
        val gateway = FakeBrailuxBillingGateway()
        val repository = GooglePlayBillingRepository(gateway = gateway)

        assertEquals(BillingConnectionState.Disconnected, repository.connectionState.value)
    }

    // 12. Estado Connecting
    @Test
    fun connectionStateTransitionsToConnectingDuringStart() = runBlocking {
        val gateway = FakeBrailuxBillingGateway()
        val repository = GooglePlayBillingRepository(gateway = gateway)

        val connectJob = launch(start = CoroutineStart.UNDISPATCHED) {
            repository.startConnection()
        }

        assertEquals(BillingConnectionState.Connecting, repository.connectionState.value)
        gateway.completeConnection(BillingClient.BillingResponseCode.OK)
        connectJob.join()
        assertEquals(BillingConnectionState.Connected, repository.connectionState.value)
    }

    // 13. Setup OK → Connected
    @Test
    fun setupOkTransitionsToConnected() = runBlocking {
        val gateway = FakeBrailuxBillingGateway()
        val repository = GooglePlayBillingRepository(gateway = gateway)

        val connectJob = launch(Dispatchers.Unconfined) {
            repository.startConnection()
        }
        gateway.completeConnection(BillingClient.BillingResponseCode.OK)
        connectJob.join()

        assertEquals(BillingConnectionState.Connected, repository.connectionState.value)
    }

    // 14. Fallo setup → Error/Unavailable según código
    @Test
    fun setupFailureTransitionsToUnavailableOrError() = runBlocking {
        val gateway = FakeBrailuxBillingGateway()
        val repository = GooglePlayBillingRepository(gateway = gateway)

        val unavailableJob = launch(Dispatchers.Unconfined) {
            repository.startConnection()
        }
        gateway.completeConnection(BillingClient.BillingResponseCode.BILLING_UNAVAILABLE, "Billing unavailable")
        unavailableJob.join()

        assertEquals(BillingConnectionState.Unavailable, repository.connectionState.value)

        val errorGateway = FakeBrailuxBillingGateway()
        val errorRepository = GooglePlayBillingRepository(gateway = errorGateway)

        val errorJob = launch(Dispatchers.Unconfined) {
            errorRepository.startConnection()
        }
        errorGateway.completeConnection(BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE, "Service unavailable")
        errorJob.join()

        assertTrue(errorRepository.connectionState.value is BillingConnectionState.Error)
    }

    // 15. Desconexión no entra en bucle manual
    @Test
    fun disconnectionDoesNotEnterManualReconnectLoop() = runBlocking {
        val gateway = FakeBrailuxBillingGateway()
        val repository = GooglePlayBillingRepository(gateway = gateway)

        val connectJob = launch(Dispatchers.Unconfined) {
            repository.startConnection()
        }
        gateway.completeConnection(BillingClient.BillingResponseCode.OK)
        connectJob.join()
        assertEquals(BillingConnectionState.Connected, repository.connectionState.value)
        assertEquals(1, gateway.startConnectionCount)

        // Simular desconexión
        gateway.triggerDisconnect()

        assertEquals(BillingConnectionState.Disconnected, repository.connectionState.value)
        // Verificar que no se lanzó un startConnection manual adicional
        assertEquals(1, gateway.startConnectionCount)
    }

    // 16. queryPurchases filtra únicamente productos Brailux
    @Test
    fun queryPurchasesFiltersOnlyBrailuxProducts() = runBlocking {
        val gateway = FakeBrailuxBillingGateway()
        val repository = GooglePlayBillingRepository(gateway = gateway)

        val mixedRawPurchases = BrailuxBillingMapper.mapPurchaseData(
            productIds = listOf(
                BrailuxBillingProductCatalog.PRODUCT_ID_CELESTE_GEOMETRICO,
                "external_game_pass",
            ),
            purchaseToken = "token_mix",
            purchaseTimeMillis = 5000L,
            purchaseStateCode = Purchase.PurchaseState.PURCHASED,
            isAcknowledged = true,
        )

        assertEquals(1, mixedRawPurchases.size)
        assertEquals(BrailuxBillingProductCatalog.PRODUCT_ID_CELESTE_GEOMETRICO, mixedRawPurchases.first().productId)
        assertFalse(mixedRawPurchases.any { it.productId == "external_game_pass" })
    }

    // 17. PURCHASED no modifica ownedBackgroundIds
    @Test
    fun purchasedDoesNotModifyOwnedBackgroundIds() = runBlocking {
        val initialOwned = BrailuxPremiumAccess.currentState.ownedBackgroundIds
        val gateway = FakeBrailuxBillingGateway()
        val repository = GooglePlayBillingRepository(gateway = gateway)

        val purchasedRecord = BrailuxBillingMapper.mapPurchaseData(
            productIds = listOf(BrailuxBillingProductCatalog.PRODUCT_ID_CREMA_ONDAS),
            purchaseToken = "token_purchased",
            purchaseTimeMillis = 6000L,
            purchaseStateCode = Purchase.PurchaseState.PURCHASED,
            isAcknowledged = true,
        )

        assertEquals(1, purchasedRecord.size)
        assertEquals(BrailuxPurchaseState.Purchased, purchasedRecord.first().purchaseState)
        assertEquals(
            "ownedBackgroundIds debe permanecer intacto tras mapear PURCHASED",
            initialOwned,
            BrailuxPremiumAccess.currentState.ownedBackgroundIds,
        )
    }

    // 18. PENDING no modifica ownedBackgroundIds
    @Test
    fun pendingDoesNotModifyOwnedBackgroundIds() = runBlocking {
        val initialOwned = BrailuxPremiumAccess.currentState.ownedBackgroundIds
        val pendingRecord = BrailuxBillingMapper.mapPurchaseData(
            productIds = listOf(BrailuxBillingProductCatalog.PRODUCT_ID_SALVIA_TEXTURA),
            purchaseToken = "token_pending",
            purchaseTimeMillis = 7000L,
            purchaseStateCode = Purchase.PurchaseState.PENDING,
            isAcknowledged = false,
        )

        assertEquals(1, pendingRecord.size)
        assertEquals(BrailuxPurchaseState.Pending, pendingRecord.first().purchaseState)
        assertEquals(
            "ownedBackgroundIds debe permanecer intacto tras mapear PENDING",
            initialOwned,
            BrailuxPremiumAccess.currentState.ownedBackgroundIds,
        )
    }

    // 19. syncBillingData no concede entitlement
    @Test
    fun syncBillingDataDoesNotGrantEntitlement() = runBlocking {
        val initialOwned = BrailuxPremiumAccess.currentState.ownedBackgroundIds
        val initialPremium = BrailuxPremiumAccess.currentState.isPremiumUnlocked

        val gateway = FakeBrailuxBillingGateway()
        gateway.isReadyValue = true
        val repository = GooglePlayBillingRepository(gateway = gateway)

        val connectJob = launch(Dispatchers.Unconfined) {
            repository.startConnection()
        }
        gateway.completeConnection(BillingClient.BillingResponseCode.OK)
        connectJob.join()

        val syncResult = repository.syncBillingData()
        assertTrue(syncResult.isSuccess)

        assertEquals(initialOwned, BrailuxPremiumAccess.currentState.ownedBackgroundIds)
        assertEquals(initialPremium, BrailuxPremiumAccess.currentState.isPremiumUnlocked)
    }

    // 20. restorePurchases no concede entitlement
    @Test
    fun restorePurchasesDoesNotGrantEntitlement() = runBlocking {
        val initialOwned = BrailuxPremiumAccess.currentState.ownedBackgroundIds
        val gateway = FakeBrailuxBillingGateway()
        val repository = GooglePlayBillingRepository(gateway = gateway)

        val restoreResult = repository.restorePurchases()
        assertTrue(restoreResult.isSuccess)

        assertEquals(initialOwned, BrailuxPremiumAccess.currentState.ownedBackgroundIds)
    }

    // 21. launchPurchaseFlow retorna fallo explícito no soportado
    @Test
    fun launchPurchaseFlowReturnsUnsupportedFailure() = runBlocking {
        val gateway = FakeBrailuxBillingGateway()
        val repository = GooglePlayBillingRepository(gateway = gateway)

        val dummyActivity = object : Activity() {}
        val result = repository.launchPurchaseFlow(dummyActivity, BrailuxBillingProductCatalog.PRODUCT_ID_CELESTE_GEOMETRICO)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is UnsupportedOperationException)
    }

    // 22. acknowledgePurchase retorna fallo explícito no soportado
    @Test
    fun acknowledgePurchaseReturnsUnsupportedFailure() = runBlocking {
        val gateway = FakeBrailuxBillingGateway()
        val repository = GooglePlayBillingRepository(gateway = gateway)

        val result = repository.acknowledgePurchase("dummy_token")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is UnsupportedOperationException)
    }

    // 23. endConnection libera recursos y resetea estado
    @Test
    fun endConnectionResetsStateToDisconnected() = runBlocking {
        val gateway = FakeBrailuxBillingGateway()
        val repository = GooglePlayBillingRepository(gateway = gateway)

        val connectJob = launch(Dispatchers.Unconfined) {
            repository.startConnection()
        }
        gateway.completeConnection(BillingClient.BillingResponseCode.OK)
        connectJob.join()
        assertEquals(BillingConnectionState.Connected, repository.connectionState.value)

        repository.endConnection()
        assertEquals(1, gateway.endConnectionCount)
        assertEquals(BillingConnectionState.Disconnected, repository.connectionState.value)
    }
}
