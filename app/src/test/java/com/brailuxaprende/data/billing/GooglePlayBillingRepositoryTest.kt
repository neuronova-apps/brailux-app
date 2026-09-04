package com.brailuxaprende.data.billing

import android.app.Activity
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
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

        var launchBillingFlowResult: BillingResult =
            BillingResult.newBuilder().setResponseCode(BillingClient.BillingResponseCode.OK).build()
        var launchBillingFlowCount: Int = 0
        var lastLaunchedActivity: Activity? = null
        var lastLaunchedProductDetails: ProductDetails? = null
        var lastLaunchedOfferToken: String? = null

        override fun launchBillingFlow(
            activity: Activity,
            productDetails: ProductDetails,
            offerToken: String,
        ): BillingResult {
            launchBillingFlowCount++
            lastLaunchedActivity = activity
            lastLaunchedProductDetails = productDetails
            lastLaunchedOfferToken = offerToken
            return launchBillingFlowResult
        }

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

    private class FakePurchase(
        private val fakeProducts: List<String>,
        private val fakePurchaseToken: String = "token_test",
        private val fakePurchaseTime: Long = 1000L,
        private val fakePurchaseState: Int = Purchase.PurchaseState.PURCHASED,
        private val fakeAcknowledged: Boolean = true,
    ) : Purchase("{\"productId\":\"fake\"}", "sig") {
        override fun getProducts(): List<String> = fakeProducts
        override fun getPurchaseToken(): String = fakePurchaseToken
        override fun getPurchaseTime(): Long = fakePurchaseTime
        override fun getPurchaseState(): Int = fakePurchaseState
        override fun isAcknowledged(): Boolean = fakeAcknowledged
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

    // 21. launchPurchaseFlow retorna fallo explícito cuando el servicio está desconectado
    @Test
    fun launchPurchaseFlowFailsWhenServiceDisconnected() = runBlocking {
        val gateway = FakeBrailuxBillingGateway()
        gateway.isReadyValue = false
        val repository = GooglePlayBillingRepository(gateway = gateway, mainDispatcher = Dispatchers.Unconfined)

        val dummyActivity = object : Activity() {}
        val request = BrailuxPurchaseRequest(BrailuxBillingProductCatalog.PRODUCT_ID_CELESTE_GEOMETRICO, "tok_1")
        val result = repository.launchPurchaseFlow(dummyActivity, request)

        assertTrue(result.isFailure)
        val ex = result.exceptionOrNull()
        assertTrue(ex is BrailuxBillingException)
        assertEquals(BillingClient.BillingResponseCode.SERVICE_DISCONNECTED, (ex as BrailuxBillingException).responseCode)
    }

    // 22. acknowledgePurchase retorna fallo explícito no soportado
    @Test
    fun acknowledgePurchaseReturnsUnsupportedFailure() = runBlocking {
        val gateway = FakeBrailuxBillingGateway()
        val repository = GooglePlayBillingRepository(gateway = gateway, mainDispatcher = Dispatchers.Unconfined)

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

    // 24. Un ProductDetails con múltiples ofertas conserva todas
    @Test
    fun productDetailsWithMultipleOffersPreservesAllOffers() {
        val offer1 = BrailuxOneTimeOfferDetails(
            offerToken = "token_offer_base_1",
            purchaseOptionId = "option_standard_1",
            offerId = "offer_base_1",
            formattedPrice = "$1.99",
            priceAmountMicros = 1990000L,
            priceCurrencyCode = "USD",
        )
        val offer2 = BrailuxOneTimeOfferDetails(
            offerToken = "token_offer_promo_2",
            purchaseOptionId = "option_promo_2",
            offerId = "offer_discount_2",
            formattedPrice = "$0.99",
            priceAmountMicros = 990000L,
            priceCurrencyCode = "USD",
        )
        val details = BrailuxBillingMapper.mapProductDetailsData(
            productId = BrailuxBillingProductCatalog.PRODUCT_ID_CELESTE_GEOMETRICO,
            name = "Celeste Geométrico",
            description = "Fondo decorativo accesible",
            oneTimeOffers = listOf(offer1, offer2),
        )

        assertNotNull(details)
        assertEquals(2, details?.oneTimeOffers?.size)
        assertEquals(listOf(offer1, offer2), details?.oneTimeOffers)
    }

    // 25. Cada oferta conserva offerToken
    @Test
    fun eachOfferPreservesOfferToken() {
        val offer1 = BrailuxOneTimeOfferDetails(
            offerToken = "token_alpha_123",
            purchaseOptionId = "opt_1",
            offerId = "off_1",
            formattedPrice = "$2.99",
            priceAmountMicros = 2990000L,
            priceCurrencyCode = "USD",
        )
        val offer2 = BrailuxOneTimeOfferDetails(
            offerToken = "token_beta_456",
            purchaseOptionId = "opt_2",
            offerId = "off_2",
            formattedPrice = "$1.49",
            priceAmountMicros = 1490000L,
            priceCurrencyCode = "USD",
        )
        val details = BrailuxBillingMapper.mapProductDetailsData(
            productId = BrailuxBillingProductCatalog.PRODUCT_ID_CREMA_ONDAS,
            name = "Crema Ondas",
            description = "Fondo decorativo accesible",
            oneTimeOffers = listOf(offer1, offer2),
        )

        assertNotNull(details)
        assertEquals("token_alpha_123", details?.oneTimeOffers?.get(0)?.offerToken)
        assertEquals("token_beta_456", details?.oneTimeOffers?.get(1)?.offerToken)
    }

    // 26. purchaseOptionId se conserva cuando existe
    @Test
    fun purchaseOptionIdPreservedWhenPresent() {
        val offerWithOption = BrailuxOneTimeOfferDetails(
            offerToken = "tok_opt_present",
            purchaseOptionId = "purchase_option_launch",
            offerId = "off_launch",
            formattedPrice = "$1.99",
            priceAmountMicros = 1990000L,
            priceCurrencyCode = "USD",
        )
        val offerWithoutOption = BrailuxOneTimeOfferDetails(
            offerToken = "tok_opt_absent",
            purchaseOptionId = null,
            offerId = "off_standard",
            formattedPrice = "$1.99",
            priceAmountMicros = 1990000L,
            priceCurrencyCode = "USD",
        )
        val details = BrailuxBillingMapper.mapProductDetailsData(
            productId = BrailuxBillingProductCatalog.PRODUCT_ID_LAVANDA_NIEBLA,
            name = "Lavanda Niebla",
            description = "Fondo decorativo accesible",
            oneTimeOffers = listOf(offerWithOption, offerWithoutOption),
        )

        assertNotNull(details)
        assertEquals("purchase_option_launch", details?.oneTimeOffers?.get(0)?.purchaseOptionId)
        assertNull(details?.oneTimeOffers?.get(1)?.purchaseOptionId)
    }

    // 27. offerId se conserva cuando existe
    @Test
    fun offerIdPreservedWhenPresent() {
        val offerWithId = BrailuxOneTimeOfferDetails(
            offerToken = "tok_id_present",
            purchaseOptionId = "opt_present",
            offerId = "offer_black_friday_2026",
            formattedPrice = "$0.99",
            priceAmountMicros = 990000L,
            priceCurrencyCode = "USD",
        )
        val offerWithoutId = BrailuxOneTimeOfferDetails(
            offerToken = "tok_id_absent",
            purchaseOptionId = "opt_present_2",
            offerId = null,
            formattedPrice = "$1.99",
            priceAmountMicros = 1990000L,
            priceCurrencyCode = "USD",
        )
        val details = BrailuxBillingMapper.mapProductDetailsData(
            productId = BrailuxBillingProductCatalog.PRODUCT_ID_SALVIA_TEXTURA,
            name = "Salvia Textura",
            description = "Fondo decorativo accesible",
            oneTimeOffers = listOf(offerWithId, offerWithoutId),
        )

        assertNotNull(details)
        assertEquals("offer_black_friday_2026", details?.oneTimeOffers?.get(0)?.offerId)
        assertNull(details?.oneTimeOffers?.get(1)?.offerId)
    }

    // 28. Cada oferta conserva su precio y moneda
    @Test
    fun eachOfferPreservesPriceAndCurrency() {
        val offerA = BrailuxOneTimeOfferDetails(
            offerToken = "tok_eur",
            purchaseOptionId = "opt_eur",
            offerId = "off_eur",
            formattedPrice = "2,49 €",
            priceAmountMicros = 2490000L,
            priceCurrencyCode = "EUR",
        )
        val offerB = BrailuxOneTimeOfferDetails(
            offerToken = "tok_usd",
            purchaseOptionId = "opt_usd",
            offerId = "off_usd",
            formattedPrice = "$1.99",
            priceAmountMicros = 1990000L,
            priceCurrencyCode = "USD",
        )
        val details = BrailuxBillingMapper.mapProductDetailsData(
            productId = BrailuxBillingProductCatalog.PRODUCT_ID_CELESTE_GEOMETRICO,
            name = "Celeste Geométrico",
            description = "Fondo decorativo accesible",
            oneTimeOffers = listOf(offerA, offerB),
        )

        assertNotNull(details)
        val first = details?.oneTimeOffers?.get(0)
        assertEquals("2,49 €", first?.formattedPrice)
        assertEquals(2490000L, first?.priceAmountMicros)
        assertEquals("EUR", first?.priceCurrencyCode)

        val second = details?.oneTimeOffers?.get(1)
        assertEquals("$1.99", second?.formattedPrice)
        assertEquals(1990000L, second?.priceAmountMicros)
        assertEquals("USD", second?.priceCurrencyCode)
    }

    // 29. No se selecciona/destruye información de ofertas adicionales
    @Test
    fun additionalOffersInformationNotDestroyedOrOverwritten() {
        val standardOffer = BrailuxOneTimeOfferDetails(
            offerToken = "token_standard",
            purchaseOptionId = "opt_standard",
            offerId = "offer_standard",
            formattedPrice = "$1.99",
            priceAmountMicros = 1990000L,
            priceCurrencyCode = "USD",
        )
        val discountedOffer = BrailuxOneTimeOfferDetails(
            offerToken = "token_discounted",
            purchaseOptionId = "opt_discounted",
            offerId = "offer_discounted",
            formattedPrice = "$0.99",
            priceAmountMicros = 990000L,
            priceCurrencyCode = "USD",
        )
        val details = BrailuxBillingMapper.mapProductDetailsData(
            productId = BrailuxBillingProductCatalog.PRODUCT_ID_CREMA_ONDAS,
            name = "Crema Ondas",
            description = "Fondo crema",
            oneTimeOffers = listOf(standardOffer, discountedOffer),
        )

        assertNotNull(details)
        // Campos compatibles reflejan la primera oferta como resumen
        assertEquals("$1.99", details?.formattedPrice)
        assertEquals(1990000L, details?.priceAmountMicros)
        assertEquals("USD", details?.priceCurrencyCode)

        // La oferta adicional no fue eliminada ni sus campos sobrescritos
        assertEquals(2, details?.oneTimeOffers?.size)
        val secondOffer = details?.oneTimeOffers?.get(1)
        assertEquals("token_discounted", secondOffer?.offerToken)
        assertEquals("opt_discounted", secondOffer?.purchaseOptionId)
        assertEquals("offer_discounted", secondOffer?.offerId)
        assertEquals("$0.99", secondOffer?.formattedPrice)
        assertEquals(990000L, secondOffer?.priceAmountMicros)
        assertEquals("USD", secondOffer?.priceCurrencyCode)
    }

    // 30. Producto sin ofertas produce lista vacía sin precio inventado
    @Test
    fun productWithoutOffersProducesEmptyListWithoutInventedPrice() {
        val details = BrailuxBillingMapper.mapProductDetailsData(
            productId = BrailuxBillingProductCatalog.PRODUCT_ID_SALVIA_TEXTURA,
            name = "Salvia Textura",
            description = "Fondo salvia",
            formattedPrice = null,
            priceAmountMicros = null,
            priceCurrencyCode = null,
            oneTimeOffers = emptyList(),
        )

        assertNotNull(details)
        assertTrue("oneTimeOffers debe ser vacía cuando no hay ofertas", details!!.oneTimeOffers.isEmpty())
        assertNull("formattedPrice debe ser null", details.formattedPrice)
        assertNull("priceAmountMicros debe ser null", details.priceAmountMicros)
        assertNull("priceCurrencyCode debe ser null", details.priceCurrencyCode)
    }

    // 31. UnfetchedProduct tiene lista de ofertas vacía
    @Test
    fun unfetchedProductHasEmptyOffersList() {
        val unfetched = BrailuxBillingMapper.mapUnfetchedProductData(
            productId = BrailuxBillingProductCatalog.PRODUCT_ID_LAVANDA_NIEBLA,
            statusCode = UnfetchedProduct.StatusCode.PRODUCT_NOT_FOUND,
        )

        assertNotNull(unfetched)
        assertTrue("oneTimeOffers debe ser lista vacía en producto unfetched", unfetched!!.oneTimeOffers.isEmpty())
        assertEquals(BrailuxProductState.Unavailable, unfetched.state)
        assertEquals(UnfetchedProduct.StatusCode.PRODUCT_NOT_FOUND, unfetched.unfetchedStatusCode)
        assertNull(unfetched.formattedPrice)
        assertNull(unfetched.priceAmountMicros)
        assertNull(unfetched.priceCurrencyCode)
    }

    // 32. USER_CANCELED no modifica purchases
    @Test
    fun userCanceledDoesNotModifyPurchases() = runBlocking {
        val gateway = FakeBrailuxBillingGateway()
        val repository = GooglePlayBillingRepository(gateway = gateway)

        // Registrar una compra exitosa inicial
        val okResult = BillingResult.newBuilder().setResponseCode(BillingClient.BillingResponseCode.OK).build()
        val initialPurchases = listOf(
            FakePurchase(
                fakeProducts = listOf(BrailuxBillingProductCatalog.PRODUCT_ID_CREMA_ONDAS),
                fakePurchaseToken = "tok_existing",
                fakePurchaseTime = 1000L,
                fakePurchaseState = Purchase.PurchaseState.PURCHASED,
                fakeAcknowledged = true,
            )
        )
        repository.onPurchasesUpdated(okResult, initialPurchases)
        assertEquals(1, repository.purchases.value.size)
        assertTrue(repository.isProductPurchased(BrailuxBillingProductCatalog.PRODUCT_ID_CREMA_ONDAS))

        // Simular USER_CANCELED
        val cancelResult = BillingResult.newBuilder()
            .setResponseCode(BillingClient.BillingResponseCode.USER_CANCELED)
            .setDebugMessage("User cancelled flow")
            .build()
        repository.onPurchasesUpdated(cancelResult, null)

        // purchases debe permanecer exactamente intacto
        assertEquals(1, repository.purchases.value.size)
        assertTrue(repository.isProductPurchased(BrailuxBillingProductCatalog.PRODUCT_ID_CREMA_ONDAS))
        assertEquals("tok_existing", repository.purchases.value[BrailuxBillingProductCatalog.PRODUCT_ID_CREMA_ONDAS]?.purchaseToken)
        assertEquals(BrailuxBillingOperationState.UserCanceled, repository.lastBillingOperation.value)
        assertNull("USER_CANCELED no debe marcarse como error fatal", repository.lastBillingError.value)
    }

    // 33. USER_CANCELED no concede entitlement
    @Test
    fun userCanceledDoesNotGrantEntitlement() {
        val initialOwned = BrailuxPremiumAccess.currentState.ownedBackgroundIds
        val initialPremium = BrailuxPremiumAccess.currentState.isPremiumUnlocked

        val gateway = FakeBrailuxBillingGateway()
        val repository = GooglePlayBillingRepository(gateway = gateway)

        val cancelResult = BillingResult.newBuilder()
            .setResponseCode(BillingClient.BillingResponseCode.USER_CANCELED)
            .setDebugMessage("User aborted purchase")
            .build()
        repository.onPurchasesUpdated(cancelResult, null)

        assertEquals("ownedBackgroundIds no debe ser modificado por USER_CANCELED", initialOwned, BrailuxPremiumAccess.currentState.ownedBackgroundIds)
        assertEquals("isPremiumUnlocked no debe ser modificado por USER_CANCELED", initialPremium, BrailuxPremiumAccess.currentState.isPremiumUnlocked)
    }

    // 34. Error distinto de OK no elimina purchases existentes
    @Test
    fun nonOkErrorDoesNotDeleteExistingPurchases() = runBlocking {
        val gateway = FakeBrailuxBillingGateway()
        val repository = GooglePlayBillingRepository(gateway = gateway)

        // Sembrar compra existente
        val okResult = BillingResult.newBuilder().setResponseCode(BillingClient.BillingResponseCode.OK).build()
        val existing = listOf(
            FakePurchase(
                fakeProducts = listOf(BrailuxBillingProductCatalog.PRODUCT_ID_SALVIA_TEXTURA),
                fakePurchaseToken = "tok_salvia",
                fakePurchaseTime = 2000L,
                fakePurchaseState = Purchase.PurchaseState.PURCHASED,
                fakeAcknowledged = true,
            )
        )
        repository.onPurchasesUpdated(okResult, existing)
        assertEquals(1, repository.purchases.value.size)

        // Disparar error técnico (ej. ERROR o ITEM_UNAVAILABLE)
        val errorResult = BillingResult.newBuilder()
            .setResponseCode(BillingClient.BillingResponseCode.ERROR)
            .setDebugMessage("Fatal billing service error")
            .build()
        repository.onPurchasesUpdated(errorResult, null)

        // purchases no debe ser borrado
        assertEquals(1, repository.purchases.value.size)
        assertEquals(BrailuxPurchaseState.Purchased, repository.purchases.value[BrailuxBillingProductCatalog.PRODUCT_ID_SALVIA_TEXTURA]?.purchaseState)
    }

    // 35. Error técnico queda observable
    @Test
    fun technicalErrorRemainsObservableWithoutContaminatingConnectionState() {
        val gateway = FakeBrailuxBillingGateway()
        val repository = GooglePlayBillingRepository(gateway = gateway)

        val errorResult = BillingResult.newBuilder()
            .setResponseCode(BillingClient.BillingResponseCode.DEVELOPER_ERROR)
            .setDebugMessage("Misconfigured SKU parameters")
            .build()
        repository.onPurchasesUpdated(errorResult, null)

        // Error técnico observable
        val error = repository.lastBillingError.value
        assertNotNull("lastBillingError debe reportar el error", error)
        assertEquals(BillingClient.BillingResponseCode.DEVELOPER_ERROR, error?.responseCode)
        assertEquals("Misconfigured SKU parameters", error?.message)

        // Operación observable en estado Error
        val opState = repository.lastBillingOperation.value
        assertTrue("lastBillingOperation debe ser Error", opState is BrailuxBillingOperationState.Error)
        assertEquals(BillingClient.BillingResponseCode.DEVELOPER_ERROR, (opState as BrailuxBillingOperationState.Error).responseCode)

        // connectionState NO debe ser contaminado por un error de PurchasesUpdatedListener
        assertEquals(BillingConnectionState.Disconnected, repository.connectionState.value)

        // Comprobar que USER_CANCELED no se marca como error técnico
        val cancelResult = BillingResult.newBuilder()
            .setResponseCode(BillingClient.BillingResponseCode.USER_CANCELED)
            .build()
        repository.onPurchasesUpdated(cancelResult, null)
        assertNull("USER_CANCELED no debe registrarse en lastBillingError", repository.lastBillingError.value)
        assertEquals(BrailuxBillingOperationState.UserCanceled, repository.lastBillingOperation.value)
    }

    // 36. PURCHASED sigue sin modificar ownedBackgroundIds
    @Test
    fun purchasedStateStillDoesNotModifyOwnedBackgroundIds() {
        val initialOwned = BrailuxPremiumAccess.currentState.ownedBackgroundIds
        val gateway = FakeBrailuxBillingGateway()
        val repository = GooglePlayBillingRepository(gateway = gateway)

        val okResult = BillingResult.newBuilder().setResponseCode(BillingClient.BillingResponseCode.OK).build()
        val purchasedList = listOf(
            FakePurchase(
                fakeProducts = listOf(BrailuxBillingProductCatalog.PRODUCT_ID_CELESTE_GEOMETRICO),
                fakePurchaseToken = "tok_purchased_check",
                fakePurchaseTime = 3000L,
                fakePurchaseState = Purchase.PurchaseState.PURCHASED,
                fakeAcknowledged = true,
            )
        )
        repository.onPurchasesUpdated(okResult, purchasedList)

        assertEquals(1, repository.purchases.value.size)
        assertEquals(
            "Regla de Entitlement: PURCHASED en PurchasesUpdatedListener no debe alterar ownedBackgroundIds",
            initialOwned,
            BrailuxPremiumAccess.currentState.ownedBackgroundIds,
        )
    }

    // 37. PENDING sigue sin modificar ownedBackgroundIds
    @Test
    fun pendingStateStillDoesNotModifyOwnedBackgroundIds() {
        val initialOwned = BrailuxPremiumAccess.currentState.ownedBackgroundIds
        val gateway = FakeBrailuxBillingGateway()
        val repository = GooglePlayBillingRepository(gateway = gateway)

        val okResult = BillingResult.newBuilder().setResponseCode(BillingClient.BillingResponseCode.OK).build()
        val pendingList = listOf(
            FakePurchase(
                fakeProducts = listOf(BrailuxBillingProductCatalog.PRODUCT_ID_LAVANDA_NIEBLA),
                fakePurchaseToken = "tok_pending_check",
                fakePurchaseTime = 4000L,
                fakePurchaseState = Purchase.PurchaseState.PENDING,
                fakeAcknowledged = false,
            )
        )
        repository.onPurchasesUpdated(okResult, pendingList)

        assertEquals(1, repository.purchases.value.size)
        assertEquals(
            "Regla de Entitlement: PENDING en PurchasesUpdatedListener no debe alterar ownedBackgroundIds",
            initialOwned,
            BrailuxPremiumAccess.currentState.ownedBackgroundIds,
        )
    }

    private fun createConnectedRepository(
        gateway: FakeBrailuxBillingGateway = FakeBrailuxBillingGateway(),
    ): Pair<FakeBrailuxBillingGateway, GooglePlayBillingRepository> {
        val repository = GooglePlayBillingRepository(gateway = gateway, mainDispatcher = Dispatchers.Unconfined)
        runBlocking {
            val connectJob = launch(Dispatchers.Unconfined) {
                repository.startConnection()
            }
            gateway.completeConnection(BillingClient.BillingResponseCode.OK)
            connectJob.join()
        }
        return gateway to repository
    }

    private fun seedProductDetails(
        repository: GooglePlayBillingRepository,
        gateway: FakeBrailuxBillingGateway,
        productDetails: ProductDetails,
    ) = runBlocking {
        gateway.queryProductDetailsResult = BillingResult.newBuilder()
            .setResponseCode(BillingClient.BillingResponseCode.OK)
            .build() to QueryProductDetailsResult.create(listOf(productDetails), emptyList())
        repository.queryProductDetails(setOf(productDetails.productId))
    }

    // 38. request requiere productId válido
    @Test
    fun requestRequiresValidProductId() = runBlocking {
        val (gateway, repository) = createConnectedRepository()
        val dummyActivity = object : Activity() {}
        val request = BrailuxPurchaseRequest("invalid_product_id", "tok_valid")

        val result = repository.launchPurchaseFlow(dummyActivity, request)

        assertTrue(result.isFailure)
        val ex = result.exceptionOrNull()
        assertTrue(ex is BrailuxBillingException)
        assertEquals(BillingClient.BillingResponseCode.DEVELOPER_ERROR, (ex as BrailuxBillingException).responseCode)
        assertEquals(0, gateway.launchBillingFlowCount)
    }

    // 39. request requiere offerToken no vacío ni en blanco
    @Test
    fun requestRequiresNonBlankOfferToken() = runBlocking {
        val (gateway, repository) = createConnectedRepository()
        val dummyActivity = object : Activity() {}

        val emptyResult = repository.launchPurchaseFlow(
            dummyActivity,
            BrailuxPurchaseRequest(BrailuxBillingProductCatalog.PRODUCT_ID_CELESTE_GEOMETRICO, ""),
        )
        assertTrue(emptyResult.isFailure)
        assertEquals(BillingClient.BillingResponseCode.DEVELOPER_ERROR, (emptyResult.exceptionOrNull() as BrailuxBillingException).responseCode)

        val blankResult = repository.launchPurchaseFlow(
            dummyActivity,
            BrailuxPurchaseRequest(BrailuxBillingProductCatalog.PRODUCT_ID_CELESTE_GEOMETRICO, "   "),
        )
        assertTrue(blankResult.isFailure)
        assertEquals(BillingClient.BillingResponseCode.DEVELOPER_ERROR, (blankResult.exceptionOrNull() as BrailuxBillingException).responseCode)

        assertEquals(0, gateway.launchBillingFlowCount)
    }

    // 40. producto desconocido no lanza Billing
    @Test
    fun unknownProductDoesNotLaunchBilling() = runBlocking {
        val (gateway, repository) = createConnectedRepository()
        val dummyActivity = object : Activity() {}
        val request = BrailuxPurchaseRequest("unknown_custom_theme_diamond", "tok_test")

        val result = repository.launchPurchaseFlow(dummyActivity, request)

        assertTrue(result.isFailure)
        assertEquals(0, gateway.launchBillingFlowCount)
    }

    // 41. producto sin ProductDetails cacheado no lanza Billing
    @Test
    fun productWithoutCachedProductDetailsDoesNotLaunchBilling() = runBlocking {
        val (gateway, repository) = createConnectedRepository()
        val dummyActivity = object : Activity() {}
        val request = BrailuxPurchaseRequest(BrailuxBillingProductCatalog.PRODUCT_ID_CELESTE_GEOMETRICO, "tok_123")

        val result = repository.launchPurchaseFlow(dummyActivity, request)

        assertTrue(result.isFailure)
        val ex = result.exceptionOrNull()
        assertTrue(ex is BrailuxBillingException)
        assertEquals(BillingClient.BillingResponseCode.ITEM_UNAVAILABLE, (ex as BrailuxBillingException).responseCode)
        assertEquals(0, gateway.launchBillingFlowCount)
    }

    // 42. offerToken inexistente no lanza Billing
    @Test
    fun nonExistentOfferTokenDoesNotLaunchBilling() = runBlocking {
        val (gateway, repository) = createConnectedRepository()
        val dummyActivity = object : Activity() {}

        val details = ProductDetailsTestHelper.createOneTimeProductDetails(
            productId = BrailuxBillingProductCatalog.PRODUCT_ID_CELESTE_GEOMETRICO,
            offers = listOf(TestOfferDetails(offerToken = "offer_alpha")),
        )
        seedProductDetails(repository, gateway, details)

        val request = BrailuxPurchaseRequest(BrailuxBillingProductCatalog.PRODUCT_ID_CELESTE_GEOMETRICO, "offer_beta_unknown")
        val result = repository.launchPurchaseFlow(dummyActivity, request)

        assertTrue(result.isFailure)
        val ex = result.exceptionOrNull()
        assertTrue(ex is BrailuxBillingException)
        assertEquals(BillingClient.BillingResponseCode.DEVELOPER_ERROR, (ex as BrailuxBillingException).responseCode)
        assertEquals(0, gateway.launchBillingFlowCount)
    }

    // 43. offerToken correcto sí construye parámetros válidos y lanza Billing
    @Test
    fun validOfferTokenBuildsValidParametersAndLaunchesBilling() = runBlocking {
        val (gateway, repository) = createConnectedRepository()
        val dummyActivity = object : Activity() {}

        val details = ProductDetailsTestHelper.createOneTimeProductDetails(
            productId = BrailuxBillingProductCatalog.PRODUCT_ID_CELESTE_GEOMETRICO,
            offers = listOf(TestOfferDetails(offerToken = "offer_alpha_exact")),
        )
        seedProductDetails(repository, gateway, details)

        val request = BrailuxPurchaseRequest(BrailuxBillingProductCatalog.PRODUCT_ID_CELESTE_GEOMETRICO, "offer_alpha_exact")
        val result = repository.launchPurchaseFlow(dummyActivity, request)

        assertTrue(result.isSuccess)
        assertEquals(1, gateway.launchBillingFlowCount)
    }

    // 44. ProductDetails correcto se usa en BillingFlowParams
    @Test
    fun correctProductDetailsIsPassedToGateway() = runBlocking {
        val (gateway, repository) = createConnectedRepository()
        val dummyActivity = object : Activity() {}

        val details = ProductDetailsTestHelper.createOneTimeProductDetails(
            productId = BrailuxBillingProductCatalog.PRODUCT_ID_CREMA_ONDAS,
            offers = listOf(TestOfferDetails(offerToken = "offer_crema")),
        )
        seedProductDetails(repository, gateway, details)

        val request = BrailuxPurchaseRequest(BrailuxBillingProductCatalog.PRODUCT_ID_CREMA_ONDAS, "offer_crema")
        val result = repository.launchPurchaseFlow(dummyActivity, request)

        assertTrue(result.isSuccess)
        assertEquals(details, gateway.lastLaunchedProductDetails)
        assertEquals(BrailuxBillingProductCatalog.PRODUCT_ID_CREMA_ONDAS, gateway.lastLaunchedProductDetails?.productId)
    }

    // 45. offerToken seleccionado se conserva exactamente
    @Test
    fun selectedOfferTokenIsPreservedExactly() = runBlocking {
        val (gateway, repository) = createConnectedRepository()
        val dummyActivity = object : Activity() {}

        val details = ProductDetailsTestHelper.createOneTimeProductDetails(
            productId = BrailuxBillingProductCatalog.PRODUCT_ID_LAVANDA_NIEBLA,
            offers = listOf(TestOfferDetails(offerToken = "token_exact_preserved_987")),
        )
        seedProductDetails(repository, gateway, details)

        val request = BrailuxPurchaseRequest(BrailuxBillingProductCatalog.PRODUCT_ID_LAVANDA_NIEBLA, "token_exact_preserved_987")
        val result = repository.launchPurchaseFlow(dummyActivity, request)

        assertTrue(result.isSuccess)
        assertEquals("token_exact_preserved_987", gateway.lastLaunchedOfferToken)
    }

    // 46. no se selecciona automáticamente la primera oferta
    @Test
    fun firstOfferIsNotAutomaticallySelected() = runBlocking {
        val (gateway, repository) = createConnectedRepository()
        val dummyActivity = object : Activity() {}

        val details = ProductDetailsTestHelper.createOneTimeProductDetails(
            productId = BrailuxBillingProductCatalog.PRODUCT_ID_SALVIA_TEXTURA,
            offers = listOf(
                TestOfferDetails(offerToken = "offer_1_standard"),
                TestOfferDetails(offerToken = "offer_2_discount"),
            ),
        )
        seedProductDetails(repository, gateway, details)

        val request = BrailuxPurchaseRequest(BrailuxBillingProductCatalog.PRODUCT_ID_SALVIA_TEXTURA, "offer_2_discount")
        val result = repository.launchPurchaseFlow(dummyActivity, request)

        assertTrue(result.isSuccess)
        assertEquals("offer_2_discount", gateway.lastLaunchedOfferToken)
        assertFalse("No debe elegir automáticamente la primera oferta", gateway.lastLaunchedOfferToken == "offer_1_standard")
    }

    // 47. producto con 2 ofertas permite seleccionar explícitamente la segunda
    @Test
    fun productWithMultipleOffersAllowsExplicitSelectionOfSecondOffer() = runBlocking {
        val (gateway, repository) = createConnectedRepository()
        val dummyActivity = object : Activity() {}

        val details = ProductDetailsTestHelper.createOneTimeProductDetails(
            productId = BrailuxBillingProductCatalog.PRODUCT_ID_CELESTE_GEOMETRICO,
            offers = listOf(
                TestOfferDetails(offerToken = "tok_opt_a", purchaseOptionId = "opt_a"),
                TestOfferDetails(offerToken = "tok_opt_b", purchaseOptionId = "opt_b"),
            ),
        )
        seedProductDetails(repository, gateway, details)

        val result = repository.launchPurchaseFlow(
            dummyActivity,
            BrailuxPurchaseRequest(BrailuxBillingProductCatalog.PRODUCT_ID_CELESTE_GEOMETRICO, "tok_opt_b"),
        )

        assertTrue(result.isSuccess)
        assertEquals("tok_opt_b", gateway.lastLaunchedOfferToken)
    }

    // 48. launchBillingFlow OK no marca producto como Purchased
    @Test
    fun launchBillingFlowOkDoesNotMarkProductAsPurchased() = runBlocking {
        val (gateway, repository) = createConnectedRepository()
        val dummyActivity = object : Activity() {}

        val details = ProductDetailsTestHelper.createOneTimeProductDetails(
            productId = BrailuxBillingProductCatalog.PRODUCT_ID_CELESTE_GEOMETRICO,
            offers = listOf(TestOfferDetails(offerToken = "tok_ok")),
        )
        seedProductDetails(repository, gateway, details)

        val request = BrailuxPurchaseRequest(BrailuxBillingProductCatalog.PRODUCT_ID_CELESTE_GEOMETRICO, "tok_ok")
        val result = repository.launchPurchaseFlow(dummyActivity, request)

        assertTrue(result.isSuccess)
        assertFalse(
            "launchBillingFlow OK jamás debe marcar el producto como adquirido",
            repository.isProductPurchased(BrailuxBillingProductCatalog.PRODUCT_ID_CELESTE_GEOMETRICO),
        )
        assertNull(repository.purchases.value[BrailuxBillingProductCatalog.PRODUCT_ID_CELESTE_GEOMETRICO])
    }

    // 49. launch OK deja estado PurchaseFlowLaunched
    @Test
    fun launchOkLeavesStatePurchaseFlowLaunched() = runBlocking {
        val (gateway, repository) = createConnectedRepository()
        val dummyActivity = object : Activity() {}

        val details = ProductDetailsTestHelper.createOneTimeProductDetails(
            productId = BrailuxBillingProductCatalog.PRODUCT_ID_CREMA_ONDAS,
            offers = listOf(TestOfferDetails(offerToken = "tok_flow")),
        )
        seedProductDetails(repository, gateway, details)

        val request = BrailuxPurchaseRequest(BrailuxBillingProductCatalog.PRODUCT_ID_CREMA_ONDAS, "tok_flow")
        val result = repository.launchPurchaseFlow(dummyActivity, request)

        assertTrue(result.isSuccess)
        assertEquals(BrailuxBillingOperationState.PurchaseFlowLaunched, repository.lastBillingOperation.value)
        assertNull(repository.lastBillingError.value)
    }

    // 50. USER_CANCELED no concede entitlement
    @Test
    fun launchUserCanceledDoesNotGrantEntitlement() = runBlocking {
        val initialOwned = BrailuxPremiumAccess.currentState.ownedBackgroundIds
        val initialPremium = BrailuxPremiumAccess.currentState.isPremiumUnlocked

        val (gateway, repository) = createConnectedRepository()
        gateway.launchBillingFlowResult = BillingResult.newBuilder()
            .setResponseCode(BillingClient.BillingResponseCode.USER_CANCELED)
            .setDebugMessage("User cancelled")
            .build()

        val dummyActivity = object : Activity() {}
        val details = ProductDetailsTestHelper.createOneTimeProductDetails(
            productId = BrailuxBillingProductCatalog.PRODUCT_ID_LAVANDA_NIEBLA,
            offers = listOf(TestOfferDetails(offerToken = "tok_cancel")),
        )
        seedProductDetails(repository, gateway, details)

        val result = repository.launchPurchaseFlow(
            dummyActivity,
            BrailuxPurchaseRequest(BrailuxBillingProductCatalog.PRODUCT_ID_LAVANDA_NIEBLA, "tok_cancel"),
        )

        assertTrue(result.isFailure)
        assertEquals(BrailuxBillingOperationState.UserCanceled, repository.lastBillingOperation.value)
        assertEquals(initialOwned, BrailuxPremiumAccess.currentState.ownedBackgroundIds)
        assertEquals(initialPremium, BrailuxPremiumAccess.currentState.isPremiumUnlocked)
    }

    // 51. ITEM_ALREADY_OWNED no concede entitlement directo
    @Test
    fun itemAlreadyOwnedDoesNotGrantDirectEntitlement() = runBlocking {
        val initialOwned = BrailuxPremiumAccess.currentState.ownedBackgroundIds
        val (gateway, repository) = createConnectedRepository()
        gateway.launchBillingFlowResult = BillingResult.newBuilder()
            .setResponseCode(BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED)
            .setDebugMessage("Item already owned")
            .build()

        val dummyActivity = object : Activity() {}
        val details = ProductDetailsTestHelper.createOneTimeProductDetails(
            productId = BrailuxBillingProductCatalog.PRODUCT_ID_SALVIA_TEXTURA,
            offers = listOf(TestOfferDetails(offerToken = "tok_owned")),
        )
        seedProductDetails(repository, gateway, details)

        val result = repository.launchPurchaseFlow(
            dummyActivity,
            BrailuxPurchaseRequest(BrailuxBillingProductCatalog.PRODUCT_ID_SALVIA_TEXTURA, "tok_owned"),
        )

        assertTrue(result.isFailure)
        val ex = result.exceptionOrNull() as BrailuxBillingException
        assertEquals(BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED, ex.responseCode)
        assertEquals("ownedBackgroundIds debe permanecer intacto ante ITEM_ALREADY_OWNED", initialOwned, BrailuxPremiumAccess.currentState.ownedBackgroundIds)
    }

    // 52. ITEM_UNAVAILABLE no concede entitlement
    @Test
    fun itemUnavailableDoesNotGrantEntitlement() = runBlocking {
        val initialOwned = BrailuxPremiumAccess.currentState.ownedBackgroundIds
        val (gateway, repository) = createConnectedRepository()
        gateway.launchBillingFlowResult = BillingResult.newBuilder()
            .setResponseCode(BillingClient.BillingResponseCode.ITEM_UNAVAILABLE)
            .setDebugMessage("Item unavailable")
            .build()

        val dummyActivity = object : Activity() {}
        val details = ProductDetailsTestHelper.createOneTimeProductDetails(
            productId = BrailuxBillingProductCatalog.PRODUCT_ID_CELESTE_GEOMETRICO,
            offers = listOf(TestOfferDetails(offerToken = "tok_unavail")),
        )
        seedProductDetails(repository, gateway, details)

        val result = repository.launchPurchaseFlow(
            dummyActivity,
            BrailuxPurchaseRequest(BrailuxBillingProductCatalog.PRODUCT_ID_CELESTE_GEOMETRICO, "tok_unavail"),
        )

        assertTrue(result.isFailure)
        assertEquals(initialOwned, BrailuxPremiumAccess.currentState.ownedBackgroundIds)
    }

    // 53. BILLING_UNAVAILABLE queda representado
    @Test
    fun billingUnavailableIsProperlyRepresented() = runBlocking {
        val (gateway, repository) = createConnectedRepository()
        gateway.launchBillingFlowResult = BillingResult.newBuilder()
            .setResponseCode(BillingClient.BillingResponseCode.BILLING_UNAVAILABLE)
            .setDebugMessage("Billing unavailable")
            .build()

        val dummyActivity = object : Activity() {}
        val details = ProductDetailsTestHelper.createOneTimeProductDetails(
            productId = BrailuxBillingProductCatalog.PRODUCT_ID_CREMA_ONDAS,
            offers = listOf(TestOfferDetails(offerToken = "tok_b_unavail")),
        )
        seedProductDetails(repository, gateway, details)

        val result = repository.launchPurchaseFlow(
            dummyActivity,
            BrailuxPurchaseRequest(BrailuxBillingProductCatalog.PRODUCT_ID_CREMA_ONDAS, "tok_b_unavail"),
        )

        assertTrue(result.isFailure)
        val op = repository.lastBillingOperation.value
        assertTrue(op is BrailuxBillingOperationState.Error)
        assertEquals(BillingClient.BillingResponseCode.BILLING_UNAVAILABLE, (op as BrailuxBillingOperationState.Error).responseCode)
        assertEquals(BillingClient.BillingResponseCode.BILLING_UNAVAILABLE, repository.lastBillingError.value?.responseCode)
    }

    // 54. DEVELOPER_ERROR queda representado
    @Test
    fun developerErrorIsProperlyRepresented() = runBlocking {
        val (gateway, repository) = createConnectedRepository()
        gateway.launchBillingFlowResult = BillingResult.newBuilder()
            .setResponseCode(BillingClient.BillingResponseCode.DEVELOPER_ERROR)
            .setDebugMessage("Developer error")
            .build()

        val dummyActivity = object : Activity() {}
        val details = ProductDetailsTestHelper.createOneTimeProductDetails(
            productId = BrailuxBillingProductCatalog.PRODUCT_ID_LAVANDA_NIEBLA,
            offers = listOf(TestOfferDetails(offerToken = "tok_dev_err")),
        )
        seedProductDetails(repository, gateway, details)

        val result = repository.launchPurchaseFlow(
            dummyActivity,
            BrailuxPurchaseRequest(BrailuxBillingProductCatalog.PRODUCT_ID_LAVANDA_NIEBLA, "tok_dev_err"),
        )

        assertTrue(result.isFailure)
        val op = repository.lastBillingOperation.value
        assertTrue(op is BrailuxBillingOperationState.Error)
        assertEquals(BillingClient.BillingResponseCode.DEVELOPER_ERROR, (op as BrailuxBillingOperationState.Error).responseCode)
    }

    // 55. SERVICE_DISCONNECTED queda representado
    @Test
    fun serviceDisconnectedIsProperlyRepresented() = runBlocking {
        val (gateway, repository) = createConnectedRepository()
        gateway.launchBillingFlowResult = BillingResult.newBuilder()
            .setResponseCode(BillingClient.BillingResponseCode.SERVICE_DISCONNECTED)
            .setDebugMessage("Service disconnected")
            .build()

        val dummyActivity = object : Activity() {}
        val details = ProductDetailsTestHelper.createOneTimeProductDetails(
            productId = BrailuxBillingProductCatalog.PRODUCT_ID_SALVIA_TEXTURA,
            offers = listOf(TestOfferDetails(offerToken = "tok_disc")),
        )
        seedProductDetails(repository, gateway, details)

        val result = repository.launchPurchaseFlow(
            dummyActivity,
            BrailuxPurchaseRequest(BrailuxBillingProductCatalog.PRODUCT_ID_SALVIA_TEXTURA, "tok_disc"),
        )

        assertTrue(result.isFailure)
        val op = repository.lastBillingOperation.value
        assertTrue(op is BrailuxBillingOperationState.Error)
        assertEquals(BillingClient.BillingResponseCode.SERVICE_DISCONNECTED, (op as BrailuxBillingOperationState.Error).responseCode)
    }

    // 56. SERVICE_UNAVAILABLE queda representado
    @Test
    fun serviceUnavailableIsProperlyRepresented() = runBlocking {
        val (gateway, repository) = createConnectedRepository()
        gateway.launchBillingFlowResult = BillingResult.newBuilder()
            .setResponseCode(BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE)
            .setDebugMessage("Service unavailable")
            .build()

        val dummyActivity = object : Activity() {}
        val details = ProductDetailsTestHelper.createOneTimeProductDetails(
            productId = BrailuxBillingProductCatalog.PRODUCT_ID_CELESTE_GEOMETRICO,
            offers = listOf(TestOfferDetails(offerToken = "tok_srv_unavail")),
        )
        seedProductDetails(repository, gateway, details)

        val result = repository.launchPurchaseFlow(
            dummyActivity,
            BrailuxPurchaseRequest(BrailuxBillingProductCatalog.PRODUCT_ID_CELESTE_GEOMETRICO, "tok_srv_unavail"),
        )

        assertTrue(result.isFailure)
        val op = repository.lastBillingOperation.value
        assertTrue(op is BrailuxBillingOperationState.Error)
        assertEquals(BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE, (op as BrailuxBillingOperationState.Error).responseCode)
    }

    // 57. ERROR no borra purchases existentes
    @Test
    fun launchErrorDoesNotDeleteExistingPurchases() = runBlocking {
        val (gateway, repository) = createConnectedRepository()

        // Sembrar compra previa
        val okResult = BillingResult.newBuilder().setResponseCode(BillingClient.BillingResponseCode.OK).build()
        repository.onPurchasesUpdated(
            okResult,
            listOf(
                FakePurchase(
                    fakeProducts = listOf(BrailuxBillingProductCatalog.PRODUCT_ID_CREMA_ONDAS),
                    fakePurchaseToken = "tok_existing_preserved",
                )
            ),
        )
        assertEquals(1, repository.purchases.value.size)

        gateway.launchBillingFlowResult = BillingResult.newBuilder()
            .setResponseCode(BillingClient.BillingResponseCode.ERROR)
            .setDebugMessage("Launch error")
            .build()

        val dummyActivity = object : Activity() {}
        val details = ProductDetailsTestHelper.createOneTimeProductDetails(
            productId = BrailuxBillingProductCatalog.PRODUCT_ID_CELESTE_GEOMETRICO,
            offers = listOf(TestOfferDetails(offerToken = "tok_err")),
        )
        seedProductDetails(repository, gateway, details)

        val result = repository.launchPurchaseFlow(
            dummyActivity,
            BrailuxPurchaseRequest(BrailuxBillingProductCatalog.PRODUCT_ID_CELESTE_GEOMETRICO, "tok_err"),
        )

        assertTrue(result.isFailure)
        assertEquals("La compra previa no debe ser borrada por error de lanzamiento", 1, repository.purchases.value.size)
        assertTrue(repository.isProductPurchased(BrailuxBillingProductCatalog.PRODUCT_ID_CREMA_ONDAS))
    }

    // 58. PENDING sigue sin conceder entitlement
    @Test
    fun pendingStateContinuesToNotGrantEntitlement() {
        val initialOwned = BrailuxPremiumAccess.currentState.ownedBackgroundIds
        val initialPremium = BrailuxPremiumAccess.currentState.isPremiumUnlocked

        val gateway = FakeBrailuxBillingGateway()
        val repository = GooglePlayBillingRepository(gateway = gateway)

        val okResult = BillingResult.newBuilder().setResponseCode(BillingClient.BillingResponseCode.OK).build()
        repository.onPurchasesUpdated(
            okResult,
            listOf(
                FakePurchase(
                    fakeProducts = listOf(BrailuxBillingProductCatalog.PRODUCT_ID_SALVIA_TEXTURA),
                    fakePurchaseToken = "tok_pending_guard",
                    fakePurchaseState = Purchase.PurchaseState.PENDING,
                )
            ),
        )

        assertEquals(initialOwned, BrailuxPremiumAccess.currentState.ownedBackgroundIds)
        assertEquals(initialPremium, BrailuxPremiumAccess.currentState.isPremiumUnlocked)
    }

    // 59. PURCHASED recibido por listener sigue sin conceder entitlement
    @Test
    fun purchasedReceivedByListenerStillDoesNotGrantEntitlement() {
        val initialOwned = BrailuxPremiumAccess.currentState.ownedBackgroundIds
        val initialPremium = BrailuxPremiumAccess.currentState.isPremiumUnlocked

        val gateway = FakeBrailuxBillingGateway()
        val repository = GooglePlayBillingRepository(gateway = gateway)

        val okResult = BillingResult.newBuilder().setResponseCode(BillingClient.BillingResponseCode.OK).build()
        repository.onPurchasesUpdated(
            okResult,
            listOf(
                FakePurchase(
                    fakeProducts = listOf(BrailuxBillingProductCatalog.PRODUCT_ID_LAVANDA_NIEBLA),
                    fakePurchaseToken = "tok_purchased_no_entitlement",
                    fakePurchaseState = Purchase.PurchaseState.PURCHASED,
                )
            ),
        )

        assertEquals("ownedBackgroundIds debe permanecer intacto tras PURCHASED en Fase C", initialOwned, BrailuxPremiumAccess.currentState.ownedBackgroundIds)
        assertEquals(initialPremium, BrailuxPremiumAccess.currentState.isPremiumUnlocked)
    }

    // 60. ownedBackgroundIds permanece intacto después de launch OK
    @Test
    fun ownedBackgroundIdsRemainsIntactAfterLaunchOk() = runBlocking {
        val initialOwned = BrailuxPremiumAccess.currentState.ownedBackgroundIds
        val (gateway, repository) = createConnectedRepository()
        val dummyActivity = object : Activity() {}

        val details = ProductDetailsTestHelper.createOneTimeProductDetails(
            productId = BrailuxBillingProductCatalog.PRODUCT_ID_CELESTE_GEOMETRICO,
            offers = listOf(TestOfferDetails(offerToken = "tok_ok_check")),
        )
        seedProductDetails(repository, gateway, details)

        val result = repository.launchPurchaseFlow(
            dummyActivity,
            BrailuxPurchaseRequest(BrailuxBillingProductCatalog.PRODUCT_ID_CELESTE_GEOMETRICO, "tok_ok_check"),
        )

        assertTrue(result.isSuccess)
        assertEquals(
            "Regla de Oro: ownedBackgroundIds debe permanecer exactamente igual tras launch OK",
            initialOwned,
            BrailuxPremiumAccess.currentState.ownedBackgroundIds,
        )
    }

    // 61. ownedBackgroundIds permanece intacto después de error
    @Test
    fun ownedBackgroundIdsRemainsIntactAfterLaunchError() = runBlocking {
        val initialOwned = BrailuxPremiumAccess.currentState.ownedBackgroundIds
        val (gateway, repository) = createConnectedRepository()
        gateway.launchBillingFlowResult = BillingResult.newBuilder()
            .setResponseCode(BillingClient.BillingResponseCode.ERROR)
            .build()

        val dummyActivity = object : Activity() {}
        val details = ProductDetailsTestHelper.createOneTimeProductDetails(
            productId = BrailuxBillingProductCatalog.PRODUCT_ID_CREMA_ONDAS,
            offers = listOf(TestOfferDetails(offerToken = "tok_err_check")),
        )
        seedProductDetails(repository, gateway, details)

        val result = repository.launchPurchaseFlow(
            dummyActivity,
            BrailuxPurchaseRequest(BrailuxBillingProductCatalog.PRODUCT_ID_CREMA_ONDAS, "tok_err_check"),
        )

        assertTrue(result.isFailure)
        assertEquals(
            "Regla de Oro: ownedBackgroundIds debe permanecer exactamente igual tras error",
            initialOwned,
            BrailuxPremiumAccess.currentState.ownedBackgroundIds,
        )
    }

    // 62. acknowledgePurchase no se ejecuta automáticamente
    @Test
    fun acknowledgePurchaseDoesNotExecuteAutomatically() = runBlocking {
        val (gateway, repository) = createConnectedRepository()
        val dummyActivity = object : Activity() {}

        val details = ProductDetailsTestHelper.createOneTimeProductDetails(
            productId = BrailuxBillingProductCatalog.PRODUCT_ID_SALVIA_TEXTURA,
            offers = listOf(TestOfferDetails(offerToken = "tok_ack_check")),
        )
        seedProductDetails(repository, gateway, details)

        // 1. Launch OK no invoca acknowledge
        repository.launchPurchaseFlow(
            dummyActivity,
            BrailuxPurchaseRequest(BrailuxBillingProductCatalog.PRODUCT_ID_SALVIA_TEXTURA, "tok_ack_check"),
        )

        // 2. onPurchasesUpdated PURCHASED tampoco invoca acknowledge
        val okResult = BillingResult.newBuilder().setResponseCode(BillingClient.BillingResponseCode.OK).build()
        repository.onPurchasesUpdated(
            okResult,
            listOf(
                FakePurchase(
                    fakeProducts = listOf(BrailuxBillingProductCatalog.PRODUCT_ID_SALVIA_TEXTURA),
                    fakePurchaseToken = "tok_ack_token",
                    fakePurchaseState = Purchase.PurchaseState.PURCHASED,
                    fakeAcknowledged = false,
                )
            ),
        )

        // 3. Invocar acknowledge manualmente falla con UnsupportedOperationException en Fase C
        val ackResult = repository.acknowledgePurchase("tok_ack_token")
        assertTrue(ackResult.isFailure)
        assertTrue(ackResult.exceptionOrNull() is UnsupportedOperationException)
    }

    // 63. ProductDetails SDK no se persiste en DataStore
    @Test
    fun productDetailsSdkIsNotPersistedInDataStore() = runBlocking {
        val (gateway, repository) = createConnectedRepository()
        val dummyActivity = object : Activity() {}

        val details = ProductDetailsTestHelper.createOneTimeProductDetails(
            productId = BrailuxBillingProductCatalog.PRODUCT_ID_CELESTE_GEOMETRICO,
            offers = listOf(TestOfferDetails(offerToken = "tok_volatile")),
        )
        seedProductDetails(repository, gateway, details)

        // Comprobar que tras endConnection, el cache se limpia y no se guarda nada permanentemente
        repository.endConnection()

        // Reconectar con gateway nuevo sin ProductDetails
        val gateway2 = FakeBrailuxBillingGateway()
        val repository2 = GooglePlayBillingRepository(gateway = gateway2, mainDispatcher = Dispatchers.Unconfined)
        val connectJob = launch(Dispatchers.Unconfined) {
            repository2.startConnection()
        }
        gateway2.completeConnection(BillingClient.BillingResponseCode.OK)
        connectJob.join()

        val result = repository2.launchPurchaseFlow(
            dummyActivity,
            BrailuxPurchaseRequest(BrailuxBillingProductCatalog.PRODUCT_ID_CELESTE_GEOMETRICO, "tok_volatile"),
        )
        // Falla porque el nuevo repositorio no tiene cache heredado ni persistido
        assertTrue(result.isFailure)
        assertEquals(BillingClient.BillingResponseCode.ITEM_UNAVAILABLE, (result.exceptionOrNull() as BrailuxBillingException).responseCode)
    }

    // 64. Cache de ProductDetails es volátil, reemplaza válidos, elimina unfetched y se limpia en endConnection
    @Test
    fun productDetailsCacheReplacesRetrievedRemovesUnfetchedAndClearsOnDisconnect() = runBlocking {
        val (gateway, repository) = createConnectedRepository()
        val dummyActivity = object : Activity() {}

        val detailsCeleste = ProductDetailsTestHelper.createOneTimeProductDetails(
            productId = BrailuxBillingProductCatalog.PRODUCT_ID_CELESTE_GEOMETRICO,
            offers = listOf(TestOfferDetails(offerToken = "tok_celeste_1")),
        )
        val detailsCrema = ProductDetailsTestHelper.createOneTimeProductDetails(
            productId = BrailuxBillingProductCatalog.PRODUCT_ID_CREMA_ONDAS,
            offers = listOf(TestOfferDetails(offerToken = "tok_crema_1")),
        )

        // 1. Primera consulta: ambos productos disponibles
        gateway.queryProductDetailsResult = BillingResult.newBuilder().setResponseCode(BillingClient.BillingResponseCode.OK).build() to
            QueryProductDetailsResult.create(listOf(detailsCeleste, detailsCrema), emptyList())
        repository.queryProductDetails(setOf(BrailuxBillingProductCatalog.PRODUCT_ID_CELESTE_GEOMETRICO, BrailuxBillingProductCatalog.PRODUCT_ID_CREMA_ONDAS))

        // Ambos lanzan exitosamente
        assertTrue(repository.launchPurchaseFlow(dummyActivity, BrailuxPurchaseRequest(BrailuxBillingProductCatalog.PRODUCT_ID_CELESTE_GEOMETRICO, "tok_celeste_1")).isSuccess)
        assertTrue(repository.launchPurchaseFlow(dummyActivity, BrailuxPurchaseRequest(BrailuxBillingProductCatalog.PRODUCT_ID_CREMA_ONDAS, "tok_crema_1")).isSuccess)

        // 2. Segunda consulta: celeste deja de estar disponible y crema se actualiza
        val detailsCremaActualizado = ProductDetailsTestHelper.createOneTimeProductDetails(
            productId = BrailuxBillingProductCatalog.PRODUCT_ID_CREMA_ONDAS,
            offers = listOf(TestOfferDetails(offerToken = "tok_crema_v2")),
        )
        gateway.queryProductDetailsResult = BillingResult.newBuilder().setResponseCode(BillingClient.BillingResponseCode.OK).build() to
            QueryProductDetailsResult.create(listOf(detailsCremaActualizado), emptyList())

        // Consultamos celeste y crema: celeste no vino en productDetailsList
        repository.queryProductDetails(setOf(BrailuxBillingProductCatalog.PRODUCT_ID_CELESTE_GEOMETRICO, BrailuxBillingProductCatalog.PRODUCT_ID_CREMA_ONDAS))

        // Celeste debe haber sido eliminado del cache: no se puede comprar con token viejo
        val celesteLaunch = repository.launchPurchaseFlow(dummyActivity, BrailuxPurchaseRequest(BrailuxBillingProductCatalog.PRODUCT_ID_CELESTE_GEOMETRICO, "tok_celeste_1"))
        assertTrue("Celeste fue eliminado del cache por no estar disponible", celesteLaunch.isFailure)
        assertEquals(BillingClient.BillingResponseCode.ITEM_UNAVAILABLE, (celesteLaunch.exceptionOrNull() as BrailuxBillingException).responseCode)

        // Crema debe haberse actualizado a v2
        assertTrue(repository.launchPurchaseFlow(dummyActivity, BrailuxPurchaseRequest(BrailuxBillingProductCatalog.PRODUCT_ID_CREMA_ONDAS, "tok_crema_v2")).isSuccess)

        // 3. endConnection limpia completamente el cache
        repository.endConnection()
        val connectJob = launch(Dispatchers.Unconfined) { repository.startConnection() }
        gateway.completeConnection(BillingClient.BillingResponseCode.OK)
        connectJob.join()

        val cremaPostDisconnect = repository.launchPurchaseFlow(dummyActivity, BrailuxPurchaseRequest(BrailuxBillingProductCatalog.PRODUCT_ID_CREMA_ONDAS, "tok_crema_v2"))
        assertTrue("Cache debe estar vacío tras endConnection", cremaPostDisconnect.isFailure)
    }
}

