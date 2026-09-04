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
}
