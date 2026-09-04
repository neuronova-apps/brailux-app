package com.brailuxaprende.data.billing

import android.app.Activity
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.brailuxaprende.data.settings.BrailuxBackgroundCatalog
import com.brailuxaprende.data.settings.BrailuxPremiumAccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

class BrailuxBillingCoordinatorTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var dataStoreFile: File
    private lateinit var dataStoreScope: CoroutineScope
    private lateinit var coordinatorScope: CoroutineScope
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var entitlementRepository: BrailuxPremiumEntitlementRepository
    private lateinit var fakeBillingRepository: FakeBrailuxBillingRepository

    private val dummyActivity = object : Activity() {}

    @Before
    fun setUp() {
        BrailuxPremiumAccess.reset()
        dataStoreFile = File(temporaryFolder.root, "coordinator_test.preferences_pb")
        dataStoreScope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())
        coordinatorScope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())
        dataStore = PreferenceDataStoreFactory.create(scope = dataStoreScope) { dataStoreFile }
        entitlementRepository = BrailuxPremiumEntitlementRepository(
            dataStore = dataStore,
            ioDispatcher = Dispatchers.Unconfined,
        )
        fakeBillingRepository = FakeBrailuxBillingRepository()
    }

    @After
    fun tearDown() {
        coordinatorScope.cancel()
        dataStoreScope.cancel()
        BrailuxPremiumAccess.reset()
    }

    private class FakeBrailuxBillingRepository : BrailuxBillingRepository {
        override val connectionState = MutableStateFlow<BillingConnectionState>(BillingConnectionState.Connected)
        override val products = MutableStateFlow<Map<String, BrailuxBillingProductDetails>>(emptyMap())
        override val purchases = MutableStateFlow<Map<String, BrailuxPurchaseRecord>>(emptyMap())
        override val lastBillingError = MutableStateFlow<BrailuxBillingError?>(null)
        override val lastBillingOperation = MutableStateFlow<BrailuxBillingOperationState>(BrailuxBillingOperationState.Idle)

        var startConnectionCount = 0
        var endConnectionCount = 0
        var syncBillingDataCount = 0
        var launchPurchaseFlowCount = 0
        var restorePurchasesCount = 0
        var syncResult: Result<Unit> = Result.success(Unit)
        var launchResult: Result<Unit> = Result.success(Unit)
        var restoreResult: Result<List<BrailuxPurchaseRecord>> = Result.success(emptyList())

        override suspend fun startConnection(): Result<Unit> {
            startConnectionCount++
            return Result.success(Unit)
        }

        override fun endConnection() {
            endConnectionCount++
            connectionState.value = BillingConnectionState.Disconnected
        }

        override suspend fun queryProductDetails(productIds: Set<String>): Result<List<BrailuxBillingProductDetails>> {
            return Result.success(products.value.values.toList())
        }

        override suspend fun queryPurchases(): Result<List<BrailuxPurchaseRecord>> {
            return Result.success(purchases.value.values.toList())
        }

        override suspend fun syncBillingData(): Result<Unit> {
            syncBillingDataCount++
            return syncResult
        }

        override suspend fun launchPurchaseFlow(activity: Activity, request: BrailuxPurchaseRequest): Result<Unit> {
            launchPurchaseFlowCount++
            return launchResult
        }

        override suspend fun restorePurchases(): Result<List<BrailuxPurchaseRecord>> {
            restorePurchasesCount++
            return restoreResult
        }

        override suspend fun acknowledgePurchase(purchaseToken: String): Result<Unit> = Result.success(Unit)

        override fun isProductPurchased(productId: String): Boolean =
            purchases.value[productId]?.purchaseState is BrailuxPurchaseState.Purchased
    }

    private fun createProduct(
        productId: String,
        offers: List<BrailuxOneTimeOfferDetails>,
    ): BrailuxBillingProductDetails = BrailuxBillingProductDetails(
        productId = productId,
        name = "Tema",
        oneTimeOffers = offers,
    )

    private fun createOffer(
        offerToken: String = "token_abc",
        formattedPrice: String? = "$1.99",
    ): BrailuxOneTimeOfferDetails = BrailuxOneTimeOfferDetails(
        offerToken = offerToken,
        purchaseOptionId = null,
        offerId = null,
        formattedPrice = formattedPrice,
        priceAmountMicros = 1990000L,
        priceCurrencyCode = "USD",
    )

    // Condition 1: DEFAULT background is always Free
    @Test
    fun defaultBackgroundIsAlwaysFree() {
        val status = BrailuxBillingResolver.resolveThemePurchaseStatus(
            backgroundId = BrailuxBackgroundCatalog.DEFAULT_ID,
            ownedBackgroundIds = emptySet(),
            purchases = emptyMap(),
            products = emptyMap(),
        )
        assertEquals(BrailuxThemePurchaseStatus.Free, status)

        val itemState = BrailuxBillingResolver.resolveThemeItemState(
            backgroundId = BrailuxBackgroundCatalog.DEFAULT_ID,
            ownedBackgroundIds = emptySet(),
            purchases = emptyMap(),
            products = emptyMap(),
        )
        assertNull(itemState.productId)
        assertEquals(BrailuxThemePurchaseStatus.Free, itemState.status)
    }

    // Condition 2: Premium background in ownedBackgroundIds is Purchased
    @Test
    fun premiumBackgroundInOwnedIsPurchased() {
        val status = BrailuxBillingResolver.resolveThemePurchaseStatus(
            backgroundId = BrailuxBackgroundCatalog.CELESTE_GEOMETRICO_ID,
            ownedBackgroundIds = setOf(BrailuxBackgroundCatalog.CELESTE_GEOMETRICO_ID),
            purchases = emptyMap(),
            products = emptyMap(),
        )
        assertEquals(BrailuxThemePurchaseStatus.Purchased, status)
    }

    // Condition 3: Premium background with PENDING purchase is Pending
    @Test
    fun pendingPurchaseResolvesToPending() {
        val productId = BrailuxBillingProductCatalog.PRODUCT_ID_CREMA_ONDAS
        val pendingRecord = BrailuxPurchaseRecord(
            productId = productId,
            purchaseToken = "tok_pend",
            purchaseTimeMillis = 1000L,
            purchaseState = BrailuxPurchaseState.Pending,
            isAcknowledged = false,
        )
        val status = BrailuxBillingResolver.resolveThemePurchaseStatus(
            backgroundId = BrailuxBackgroundCatalog.CREMA_ONDAS_ID,
            ownedBackgroundIds = emptySet(),
            purchases = mapOf(productId to pendingRecord),
            products = mapOf(productId to createProduct(productId, listOf(createOffer()))),
        )
        assertEquals(BrailuxThemePurchaseStatus.Pending, status)
    }

    // Condition 4: Premium background available with 1 offer has formattedPrice and offerToken
    @Test
    fun singleValidOfferResolvesToAvailableForPurchaseWithRealPriceAndToken() {
        val productId = BrailuxBillingProductCatalog.PRODUCT_ID_LAVANDA_NIEBLA
        val offer = createOffer(offerToken = "offer_tok_123", formattedPrice = "S/ 3.50")
        val status = BrailuxBillingResolver.resolveThemePurchaseStatus(
            backgroundId = BrailuxBackgroundCatalog.LAVANDA_NIEBLA_ID,
            ownedBackgroundIds = emptySet(),
            purchases = emptyMap(),
            products = mapOf(productId to createProduct(productId, listOf(offer))),
        )
        assertTrue(status is BrailuxThemePurchaseStatus.AvailableForPurchase)
        val available = status as BrailuxThemePurchaseStatus.AvailableForPurchase
        assertEquals("S/ 3.50", available.formattedPrice)
        assertEquals("offer_tok_123", available.offerToken)
    }

    // Condition 5: Product with 0 offers resolves to Unavailable
    @Test
    fun zeroOffersResolvesToUnavailable() {
        val productId = BrailuxBillingProductCatalog.PRODUCT_ID_SALVIA_TEXTURA
        val status = BrailuxBillingResolver.resolveThemePurchaseStatus(
            backgroundId = BrailuxBackgroundCatalog.SALVIA_TEXTURA_ID,
            ownedBackgroundIds = emptySet(),
            purchases = emptyMap(),
            products = mapOf(productId to createProduct(productId, emptyList())),
        )
        assertEquals(BrailuxThemePurchaseStatus.Unavailable, status)
    }

    // Condition 6: Product with >1 offers resolves to Unavailable in V1
    @Test
    fun multipleOffersResolvesToUnavailableForV1() {
        val productId = BrailuxBillingProductCatalog.PRODUCT_ID_SALVIA_TEXTURA
        val offers = listOf(
            createOffer(offerToken = "tok1", formattedPrice = "$1.00"),
            createOffer(offerToken = "tok2", formattedPrice = "$2.00"),
        )
        val status = BrailuxBillingResolver.resolveThemePurchaseStatus(
            backgroundId = BrailuxBackgroundCatalog.SALVIA_TEXTURA_ID,
            ownedBackgroundIds = emptySet(),
            purchases = emptyMap(),
            products = mapOf(productId to createProduct(productId, offers)),
        )
        assertEquals(BrailuxThemePurchaseStatus.Unavailable, status)
    }

    // Condition 7: Offer with blank offerToken resolves to Unavailable
    @Test
    fun blankOfferTokenResolvesToUnavailable() {
        val productId = BrailuxBillingProductCatalog.PRODUCT_ID_CELESTE_GEOMETRICO
        val offer = createOffer(offerToken = "  ", formattedPrice = "$1.99")
        val status = BrailuxBillingResolver.resolveThemePurchaseStatus(
            backgroundId = BrailuxBackgroundCatalog.CELESTE_GEOMETRICO_ID,
            ownedBackgroundIds = emptySet(),
            purchases = emptyMap(),
            products = mapOf(productId to createProduct(productId, listOf(offer))),
        )
        assertEquals(BrailuxThemePurchaseStatus.Unavailable, status)
    }

    // Condition 8: Offer with blank formattedPrice resolves to Unavailable
    @Test
    fun blankFormattedPriceResolvesToUnavailable() {
        val productId = BrailuxBillingProductCatalog.PRODUCT_ID_CELESTE_GEOMETRICO
        val offer = createOffer(offerToken = "tok_valid", formattedPrice = "   ")
        val status = BrailuxBillingResolver.resolveThemePurchaseStatus(
            backgroundId = BrailuxBackgroundCatalog.CELESTE_GEOMETRICO_ID,
            ownedBackgroundIds = emptySet(),
            purchases = emptyMap(),
            products = mapOf(productId to createProduct(productId, listOf(offer))),
        )
        assertEquals(BrailuxThemePurchaseStatus.Unavailable, status)
    }

    // Condition 9: Offer with null formattedPrice resolves to Unavailable
    @Test
    fun nullFormattedPriceResolvesToUnavailable() {
        val productId = BrailuxBillingProductCatalog.PRODUCT_ID_CELESTE_GEOMETRICO
        val offer = createOffer(offerToken = "tok_valid", formattedPrice = null)
        val status = BrailuxBillingResolver.resolveThemePurchaseStatus(
            backgroundId = BrailuxBackgroundCatalog.CELESTE_GEOMETRICO_ID,
            ownedBackgroundIds = emptySet(),
            purchases = emptyMap(),
            products = mapOf(productId to createProduct(productId, listOf(offer))),
        )
        assertEquals(BrailuxThemePurchaseStatus.Unavailable, status)
    }

    // Condition 10: Single offer safe access without first() or arbitrary choice
    @Test
    fun singleOfferNeverUsesFirstOrArbitraryChoice() {
        val productId = BrailuxBillingProductCatalog.PRODUCT_ID_CREMA_ONDAS
        // 0 offers -> Unavailable
        assertEquals(
            BrailuxThemePurchaseStatus.Unavailable,
            BrailuxBillingResolver.resolveThemePurchaseStatus(
                BrailuxBackgroundCatalog.CREMA_ONDAS_ID,
                emptySet(),
                emptyMap(),
                mapOf(productId to createProduct(productId, emptyList())),
            ),
        )
        // 2 offers -> Unavailable (never picks first)
        val twoOffers = listOf(
            createOffer(offerToken = "first_token", formattedPrice = "$0.99"),
            createOffer(offerToken = "second_token", formattedPrice = "$1.99"),
        )
        assertEquals(
            BrailuxThemePurchaseStatus.Unavailable,
            BrailuxBillingResolver.resolveThemePurchaseStatus(
                BrailuxBackgroundCatalog.CREMA_ONDAS_ID,
                emptySet(),
                emptyMap(),
                mapOf(productId to createProduct(productId, twoOffers)),
            ),
        )
    }

    // Condition 11: Prices strictly come from Google Play formattedPrice
    @Test
    fun pricesInUiStateComeStrictlyFromFormattedPrice() {
        val productId = BrailuxBillingProductCatalog.PRODUCT_ID_LAVANDA_NIEBLA
        val googlePlayPrice = "3,99 €"
        val offer = createOffer(offerToken = "tok_eur", formattedPrice = googlePlayPrice)
        val itemState = BrailuxBillingResolver.resolveThemeItemState(
            backgroundId = BrailuxBackgroundCatalog.LAVANDA_NIEBLA_ID,
            ownedBackgroundIds = emptySet(),
            purchases = emptyMap(),
            products = mapOf(productId to createProduct(productId, listOf(offer))),
        )
        assertTrue(itemState.status is BrailuxThemePurchaseStatus.AvailableForPurchase)
        val available = itemState.status as BrailuxThemePurchaseStatus.AvailableForPurchase
        assertEquals(googlePlayPrice, available.formattedPrice)
    }

    // Condition 12: Initialize loads cached entitlements before remote sync
    @Test
    fun initializeLoadsCachedEntitlementsBeforeRemoteSync() = runBlocking {
        dataStore.edit { preferences ->
            preferences[OwnedBackgroundIdsCacheKey] = setOf(BrailuxBackgroundCatalog.CELESTE_GEOMETRICO_ID)
        }

        val coordinator = BrailuxBillingCoordinator(
            billingRepository = fakeBillingRepository,
            entitlementRepository = entitlementRepository,
            coroutineScope = coordinatorScope,
        )

        fakeBillingRepository.syncResult = Result.success(Unit)

        coordinator.initialize()

        // El cache local debe estar publicado inmediatamente
        val itemState = coordinator.uiState.value.itemFor(BrailuxBackgroundCatalog.CELESTE_GEOMETRICO_ID)
        assertNotNull(itemState)
        assertEquals(BrailuxThemePurchaseStatus.Purchased, itemState?.status)
        assertTrue(entitlementRepository.isBackgroundOwned(BrailuxBackgroundCatalog.CELESTE_GEOMETRICO_ID))
        coordinator.destroy()
    }

    // Condition 13: Local state published from cache reflects BrailuxPremiumAccess
    @Test
    fun initializePublishesLocalStateFromCache() = runBlocking {
        dataStore.edit { preferences ->
            preferences[OwnedBackgroundIdsCacheKey] = setOf(BrailuxBackgroundCatalog.SALVIA_TEXTURA_ID)
        }

        val coordinator = BrailuxBillingCoordinator(
            billingRepository = fakeBillingRepository,
            entitlementRepository = entitlementRepository,
            coroutineScope = coordinatorScope,
        )
        coordinator.initialize()

        assertTrue(BrailuxPremiumAccess.currentState.ownedBackgroundIds.contains(BrailuxBackgroundCatalog.SALVIA_TEXTURA_ID))
        assertEquals(
            BrailuxThemePurchaseStatus.Purchased,
            coordinator.uiState.value.itemFor(BrailuxBackgroundCatalog.SALVIA_TEXTURA_ID)?.status,
        )
        coordinator.destroy()
    }

    // Condition 14: Cache load failure does not crash and does not grant fake entitlements
    @Test
    fun cacheLoadFailureDoesNotCrashAndDoesNotGrantArtificialEntitlements() = runBlocking {
        val failingEntitlementRepo = BrailuxPremiumEntitlementRepository(dataStore = null)
        val coordinator = BrailuxBillingCoordinator(
            billingRepository = fakeBillingRepository,
            entitlementRepository = failingEntitlementRepo,
            coroutineScope = coordinatorScope,
        )
        coordinator.initialize()

        assertTrue(failingEntitlementRepo.ownedBackgroundIds.value.isEmpty())
        assertFalse(BrailuxPremiumAccess.currentState.isBackgroundUnlocked(BrailuxBackgroundCatalog.CREMA_ONDAS_ID))
        coordinator.destroy()
    }

    // Condition 15: Sync failure preserves existing confirmed cached entitlements
    @Test
    fun syncFailurePreservesExistingConfirmedCachedEntitlements() = runBlocking {
        dataStore.edit { preferences ->
            preferences[OwnedBackgroundIdsCacheKey] = setOf(BrailuxBackgroundCatalog.CREMA_ONDAS_ID)
        }

        fakeBillingRepository.syncResult = Result.failure(IOException("Network timeout"))

        val coordinator = BrailuxBillingCoordinator(
            billingRepository = fakeBillingRepository,
            entitlementRepository = entitlementRepository,
            coroutineScope = coordinatorScope,
        )
        coordinator.initialize()

        // El fondo adquirido sigue utilizable
        assertTrue(entitlementRepository.isBackgroundOwned(BrailuxBackgroundCatalog.CREMA_ONDAS_ID))
        assertEquals(
            BrailuxThemePurchaseStatus.Purchased,
            coordinator.uiState.value.itemFor(BrailuxBackgroundCatalog.CREMA_ONDAS_ID)?.status,
        )
        coordinator.destroy()
    }

    // Condition 16: Sync failure leaves unpurchased products as Unavailable without blocking app
    @Test
    fun syncFailureLeavesUnpurchasedProductsAsUnavailableWithoutBlocking() = runBlocking {
        fakeBillingRepository.syncResult = Result.failure(IOException("Billing service unavailable"))

        val coordinator = BrailuxBillingCoordinator(
            billingRepository = fakeBillingRepository,
            entitlementRepository = entitlementRepository,
            coroutineScope = coordinatorScope,
        )
        coordinator.initialize()

        val itemState = coordinator.uiState.value.itemFor(BrailuxBackgroundCatalog.LAVANDA_NIEBLA_ID)
        assertEquals(BrailuxThemePurchaseStatus.Unavailable, itemState?.status)
        coordinator.destroy()
    }

    // Condition 17: Initialize is idempotent (multiple calls do not duplicate sync or collectors)
    @Test
    fun initializeIsIdempotentAndDoesNotDuplicateCollectors() = runBlocking {
        val coordinator = BrailuxBillingCoordinator(
            billingRepository = fakeBillingRepository,
            entitlementRepository = entitlementRepository,
            coroutineScope = coordinatorScope,
        )
        coordinator.initialize()
        coordinator.initialize()
        coordinator.initialize()

        assertEquals(1, fakeBillingRepository.startConnectionCount)
        assertEquals(1, fakeBillingRepository.syncBillingDataCount)
        coordinator.destroy()
    }

    // Condition 18: UiState updates reactively when ownedBackgroundIds changes
    @Test
    fun uiStateUpdatesWhenOwnedBackgroundIdsChanges() = runBlocking {
        val coordinator = BrailuxBillingCoordinator(
            billingRepository = fakeBillingRepository,
            entitlementRepository = entitlementRepository,
            coroutineScope = coordinatorScope,
        )
        coordinator.initialize()

        assertEquals(
            BrailuxThemePurchaseStatus.Unavailable,
            coordinator.uiState.value.itemFor(BrailuxBackgroundCatalog.CELESTE_GEOMETRICO_ID)?.status,
        )

        // Simular compra confirmada mediante grantEntitlementForPurchase
        val record = BrailuxPurchaseRecord(
            productId = BrailuxBillingProductCatalog.PRODUCT_ID_CELESTE_GEOMETRICO,
            purchaseToken = "tok_celeste",
            purchaseTimeMillis = 1000L,
            purchaseState = BrailuxPurchaseState.Purchased,
            isAcknowledged = true,
        )
        entitlementRepository.grantEntitlementForPurchase(record)

        assertEquals(
            BrailuxThemePurchaseStatus.Purchased,
            coordinator.uiState.value.itemFor(BrailuxBackgroundCatalog.CELESTE_GEOMETRICO_ID)?.status,
        )
        coordinator.destroy()
    }

    // Condition 19: UiState updates reactively when purchases changes
    @Test
    fun uiStateUpdatesWhenPurchasesChanges() = runBlocking {
        val coordinator = BrailuxBillingCoordinator(
            billingRepository = fakeBillingRepository,
            entitlementRepository = entitlementRepository,
            coroutineScope = coordinatorScope,
        )
        coordinator.initialize()

        val productId = BrailuxBillingProductCatalog.PRODUCT_ID_LAVANDA_NIEBLA
        val pendingRecord = BrailuxPurchaseRecord(
            productId = productId,
            purchaseToken = "tok_pending_lavanda",
            purchaseTimeMillis = 2000L,
            purchaseState = BrailuxPurchaseState.Pending,
            isAcknowledged = false,
        )
        fakeBillingRepository.purchases.value = mapOf(productId to pendingRecord)

        assertEquals(
            BrailuxThemePurchaseStatus.Pending,
            coordinator.uiState.value.itemFor(BrailuxBackgroundCatalog.LAVANDA_NIEBLA_ID)?.status,
        )
        coordinator.destroy()
    }

    // Condition 20: UiState updates reactively when products changes
    @Test
    fun uiStateUpdatesWhenProductsChanges() = runBlocking {
        val coordinator = BrailuxBillingCoordinator(
            billingRepository = fakeBillingRepository,
            entitlementRepository = entitlementRepository,
            coroutineScope = coordinatorScope,
        )
        coordinator.initialize()

        val productId = BrailuxBillingProductCatalog.PRODUCT_ID_SALVIA_TEXTURA
        val offer = createOffer(offerToken = "offer_salvia", formattedPrice = "$2.49")
        fakeBillingRepository.products.value = mapOf(productId to createProduct(productId, listOf(offer)))

        val itemState = coordinator.uiState.value.itemFor(BrailuxBackgroundCatalog.SALVIA_TEXTURA_ID)
        assertTrue(itemState?.status is BrailuxThemePurchaseStatus.AvailableForPurchase)
        val available = itemState?.status as BrailuxThemePurchaseStatus.AvailableForPurchase
        assertEquals("$2.49", available.formattedPrice)
        assertEquals("offer_salvia", available.offerToken)
        coordinator.destroy()
    }

    // Condition 21: LaunchPurchase succeeds when item is AvailableForPurchase
    @Test
    fun launchPurchaseSucceedsWhenItemIsAvailableForPurchase() = runBlocking {
        val coordinator = BrailuxBillingCoordinator(
            billingRepository = fakeBillingRepository,
            entitlementRepository = entitlementRepository,
            coroutineScope = coordinatorScope,
        )
        coordinator.initialize()

        val productId = BrailuxBillingProductCatalog.PRODUCT_ID_CREMA_ONDAS
        val offer = createOffer(offerToken = "valid_token_crema", formattedPrice = "$1.99")
        fakeBillingRepository.products.value = mapOf(productId to createProduct(productId, listOf(offer)))

        val result = coordinator.launchPurchase(
            activity = dummyActivity,
            productId = productId,
            offerToken = "valid_token_crema",
        )
        assertTrue(result.isSuccess)
        assertEquals(1, fakeBillingRepository.launchPurchaseFlowCount)
        coordinator.destroy()
    }

    // Condition 22: LaunchPurchase rejects when item is already Purchased
    @Test
    fun launchPurchaseRejectsWhenItemIsAlreadyPurchased() = runBlocking {
        val coordinator = BrailuxBillingCoordinator(
            billingRepository = fakeBillingRepository,
            entitlementRepository = entitlementRepository,
            coroutineScope = coordinatorScope,
        )
        coordinator.initialize()

        val record = BrailuxPurchaseRecord(
            productId = BrailuxBillingProductCatalog.PRODUCT_ID_CREMA_ONDAS,
            purchaseToken = "tok_owned",
            purchaseTimeMillis = 1000L,
            purchaseState = BrailuxPurchaseState.Purchased,
            isAcknowledged = true,
        )
        entitlementRepository.grantEntitlementForPurchase(record)

        val result = coordinator.launchPurchase(
            activity = dummyActivity,
            productId = BrailuxBillingProductCatalog.PRODUCT_ID_CREMA_ONDAS,
            offerToken = "tok_owned",
        )
        assertTrue(result.isFailure)
        assertEquals(0, fakeBillingRepository.launchPurchaseFlowCount)
        coordinator.destroy()
    }

    // Condition 23: LaunchPurchase rejects when item is Pending
    @Test
    fun launchPurchaseRejectsWhenItemIsPending() = runBlocking {
        val coordinator = BrailuxBillingCoordinator(
            billingRepository = fakeBillingRepository,
            entitlementRepository = entitlementRepository,
            coroutineScope = coordinatorScope,
        )
        coordinator.initialize()

        val productId = BrailuxBillingProductCatalog.PRODUCT_ID_CREMA_ONDAS
        fakeBillingRepository.purchases.value = mapOf(
            productId to BrailuxPurchaseRecord(
                productId = productId,
                purchaseToken = "tok_pending",
                purchaseTimeMillis = 1000L,
                purchaseState = BrailuxPurchaseState.Pending,
                isAcknowledged = false,
            ),
        )

        val result = coordinator.launchPurchase(
            activity = dummyActivity,
            productId = productId,
            offerToken = "any_token",
        )
        assertTrue(result.isFailure)
        assertEquals(0, fakeBillingRepository.launchPurchaseFlowCount)
        coordinator.destroy()
    }

    // Condition 24: LaunchPurchase rejects when item is Unavailable
    @Test
    fun launchPurchaseRejectsWhenItemIsUnavailable() = runBlocking {
        val coordinator = BrailuxBillingCoordinator(
            billingRepository = fakeBillingRepository,
            entitlementRepository = entitlementRepository,
            coroutineScope = coordinatorScope,
        )
        coordinator.initialize()

        val result = coordinator.launchPurchase(
            activity = dummyActivity,
            productId = BrailuxBillingProductCatalog.PRODUCT_ID_CELESTE_GEOMETRICO,
            offerToken = "unknown_token",
        )
        assertTrue(result.isFailure)
        assertEquals(0, fakeBillingRepository.launchPurchaseFlowCount)
        coordinator.destroy()
    }

    // Condition 25: LaunchPurchase rejects mismatched offerToken
    @Test
    fun launchPurchaseRejectsMismatchedOfferToken() = runBlocking {
        val coordinator = BrailuxBillingCoordinator(
            billingRepository = fakeBillingRepository,
            entitlementRepository = entitlementRepository,
            coroutineScope = coordinatorScope,
        )
        coordinator.initialize()

        val productId = BrailuxBillingProductCatalog.PRODUCT_ID_LAVANDA_NIEBLA
        val offer = createOffer(offerToken = "real_token", formattedPrice = "$1.99")
        fakeBillingRepository.products.value = mapOf(productId to createProduct(productId, listOf(offer)))

        val result = coordinator.launchPurchase(
            activity = dummyActivity,
            productId = productId,
            offerToken = "tampered_token",
        )
        assertTrue(result.isFailure)
        assertEquals(0, fakeBillingRepository.launchPurchaseFlowCount)
        coordinator.destroy()
    }

    // Condition 26: LaunchPurchase rejects mismatched productId
    @Test
    fun launchPurchaseRejectsMismatchedProductId() = runBlocking {
        val coordinator = BrailuxBillingCoordinator(
            billingRepository = fakeBillingRepository,
            entitlementRepository = entitlementRepository,
            coroutineScope = coordinatorScope,
        )
        coordinator.initialize()

        val result = coordinator.launchPurchase(
            activity = dummyActivity,
            productId = "non_existent_product_id",
            offerToken = "some_token",
        )
        assertTrue(result.isFailure)
        assertEquals(0, fakeBillingRepository.launchPurchaseFlowCount)
        coordinator.destroy()
    }

    // Condition 27: LaunchPurchase prevents concurrent duplicate purchase
    @Test
    fun launchPurchasePreventsConcurrentDuplicatePurchase() = runBlocking {
        val coordinator = BrailuxBillingCoordinator(
            billingRepository = fakeBillingRepository,
            entitlementRepository = entitlementRepository,
            coroutineScope = coordinatorScope,
        )
        coordinator.initialize()

        val productId = BrailuxBillingProductCatalog.PRODUCT_ID_CREMA_ONDAS
        val offer = createOffer(offerToken = "token_one", formattedPrice = "$1.99")
        fakeBillingRepository.products.value = mapOf(productId to createProduct(productId, listOf(offer)))

        val firstResult = coordinator.launchPurchase(
            activity = dummyActivity,
            productId = productId,
            offerToken = "token_one",
        )
        assertTrue(firstResult.isSuccess)
        assertTrue(coordinator.uiState.value.isPurchasing)

        // Intento duplicado inmediato mientras isPurchasing es true
        val secondResult = coordinator.launchPurchase(
            activity = dummyActivity,
            productId = productId,
            offerToken = "token_one",
        )
        assertTrue(secondResult.isFailure)
        assertEquals(1, fakeBillingRepository.launchPurchaseFlowCount)
        coordinator.destroy()
    }

    // Condition 28: LaunchPurchaseFlow success (OK) does NOT grant entitlement
    @Test
    fun launchPurchaseFlowSuccessDoesNotGrantEntitlement() = runBlocking {
        val coordinator = BrailuxBillingCoordinator(
            billingRepository = fakeBillingRepository,
            entitlementRepository = entitlementRepository,
            coroutineScope = coordinatorScope,
        )
        coordinator.initialize()

        val productId = BrailuxBillingProductCatalog.PRODUCT_ID_SALVIA_TEXTURA
        val offer = createOffer(offerToken = "salvia_token", formattedPrice = "$1.99")
        fakeBillingRepository.products.value = mapOf(productId to createProduct(productId, listOf(offer)))

        val result = coordinator.launchPurchase(dummyActivity, productId, "salvia_token")
        assertTrue(result.isSuccess)

        // No debe haberse concedido entitlement
        assertFalse(entitlementRepository.isBackgroundOwned(BrailuxBackgroundCatalog.SALVIA_TEXTURA_ID))
        assertFalse(BrailuxPremiumAccess.currentState.isBackgroundUnlocked(BrailuxBackgroundCatalog.SALVIA_TEXTURA_ID))
        coordinator.destroy()
    }

    // Condition 29: User canceled purchase flow resets isPurchasing and preserves purchases
    @Test
    fun userCanceledResetsIsPurchasingAndPreservesPurchases() = runBlocking {
        val coordinator = BrailuxBillingCoordinator(
            billingRepository = fakeBillingRepository,
            entitlementRepository = entitlementRepository,
            coroutineScope = coordinatorScope,
        )
        coordinator.initialize()

        val productId = BrailuxBillingProductCatalog.PRODUCT_ID_CELESTE_GEOMETRICO
        val offer = createOffer(offerToken = "tok_celeste", formattedPrice = "$1.99")
        fakeBillingRepository.products.value = mapOf(productId to createProduct(productId, listOf(offer)))

        coordinator.launchPurchase(dummyActivity, productId, "tok_celeste")
        assertTrue(coordinator.uiState.value.isPurchasing)

        // Simular evento UserCanceled reportado por repository
        fakeBillingRepository.lastBillingOperation.value = BrailuxBillingOperationState.UserCanceled

        assertFalse(coordinator.uiState.value.isPurchasing)
        assertFalse(entitlementRepository.isBackgroundOwned(BrailuxBackgroundCatalog.CELESTE_GEOMETRICO_ID))
        coordinator.destroy()
    }

    // Condition 30: Billing error on purchases update resets isPurchasing and preserves purchases
    @Test
    fun billingErrorResetsIsPurchasingAndPreservesPurchases() = runBlocking {
        val coordinator = BrailuxBillingCoordinator(
            billingRepository = fakeBillingRepository,
            entitlementRepository = entitlementRepository,
            coroutineScope = coordinatorScope,
        )
        coordinator.initialize()

        val productId = BrailuxBillingProductCatalog.PRODUCT_ID_CELESTE_GEOMETRICO
        val offer = createOffer(offerToken = "tok_celeste", formattedPrice = "$1.99")
        fakeBillingRepository.products.value = mapOf(productId to createProduct(productId, listOf(offer)))

        coordinator.launchPurchase(dummyActivity, productId, "tok_celeste")
        assertTrue(coordinator.uiState.value.isPurchasing)

        // Simular error técnico
        fakeBillingRepository.lastBillingOperation.value = BrailuxBillingOperationState.Error(
            responseCode = 6,
            message = "Fatal error",
        )

        assertFalse(coordinator.uiState.value.isPurchasing)
        coordinator.destroy()
    }

    // Condition 31: Purchased record grants entitlement and updates UI state to Purchased
    @Test
    fun purchasedRecordGrantsEntitlementAndUpdatesUiStateToPurchased() = runBlocking {
        val coordinator = BrailuxBillingCoordinator(
            billingRepository = fakeBillingRepository,
            entitlementRepository = entitlementRepository,
            coroutineScope = coordinatorScope,
        )
        coordinator.initialize()

        val productId = BrailuxBillingProductCatalog.PRODUCT_ID_CREMA_ONDAS
        val record = BrailuxPurchaseRecord(
            productId = productId,
            purchaseToken = "tok_crema_purchased",
            purchaseTimeMillis = 5000L,
            purchaseState = BrailuxPurchaseState.Purchased,
            isAcknowledged = true,
        )

        entitlementRepository.grantEntitlementForPurchase(record)

        assertTrue(entitlementRepository.isBackgroundOwned(BrailuxBackgroundCatalog.CREMA_ONDAS_ID))
        assertEquals(
            BrailuxThemePurchaseStatus.Purchased,
            coordinator.uiState.value.itemFor(BrailuxBackgroundCatalog.CREMA_ONDAS_ID)?.status,
        )
        coordinator.destroy()
    }

    // Condition 32: Pending record does not grant entitlement and updates UI state to Pending
    @Test
    fun pendingRecordDoesNotGrantEntitlementAndUpdatesUiStateToPending() = runBlocking {
        val coordinator = BrailuxBillingCoordinator(
            billingRepository = fakeBillingRepository,
            entitlementRepository = entitlementRepository,
            coroutineScope = coordinatorScope,
        )
        coordinator.initialize()

        val productId = BrailuxBillingProductCatalog.PRODUCT_ID_LAVANDA_NIEBLA
        val record = BrailuxPurchaseRecord(
            productId = productId,
            purchaseToken = "tok_lavanda_pend",
            purchaseTimeMillis = 6000L,
            purchaseState = BrailuxPurchaseState.Pending,
            isAcknowledged = false,
        )

        val granted = entitlementRepository.grantEntitlementForPurchase(record)
        assertFalse(granted)
        assertFalse(entitlementRepository.isBackgroundOwned(BrailuxBackgroundCatalog.LAVANDA_NIEBLA_ID))

        fakeBillingRepository.purchases.value = mapOf(productId to record)
        assertEquals(
            BrailuxThemePurchaseStatus.Pending,
            coordinator.uiState.value.itemFor(BrailuxBackgroundCatalog.LAVANDA_NIEBLA_ID)?.status,
        )
        coordinator.destroy()
    }

    // Condition 33: RestorePurchases emits RestoreSuccess when eligible purchases are restored
    @Test
    fun restorePurchasesEmitsRestoreSuccessWhenPurchasesExist() = runBlocking {
        val coordinator = BrailuxBillingCoordinator(
            billingRepository = fakeBillingRepository,
            entitlementRepository = entitlementRepository,
            coroutineScope = coordinatorScope,
        )
        coordinator.initialize()

        val record = BrailuxPurchaseRecord(
            productId = BrailuxBillingProductCatalog.PRODUCT_ID_SALVIA_TEXTURA,
            purchaseToken = "tok_salvia_restored",
            purchaseTimeMillis = 1000L,
            purchaseState = BrailuxPurchaseState.Purchased,
            isAcknowledged = true,
        )
        fakeBillingRepository.restoreResult = Result.success(listOf(record))

        var receivedEvent: BrailuxRestoreEvent? = null
        val job = coordinatorScope.launch {
            receivedEvent = coordinator.restoreEvents.first()
        }

        coordinator.restorePurchases()
        job.join()

        assertEquals(BrailuxRestoreEvent.RestoreSuccess, receivedEvent)
        coordinator.destroy()
    }

    // Condition 34: RestorePurchases emits RestoreEmpty when no eligible purchases exist
    @Test
    fun restorePurchasesEmitsRestoreEmptyWhenNoEligiblePurchases() = runBlocking {
        val coordinator = BrailuxBillingCoordinator(
            billingRepository = fakeBillingRepository,
            entitlementRepository = entitlementRepository,
            coroutineScope = coordinatorScope,
        )
        coordinator.initialize()

        fakeBillingRepository.restoreResult = Result.success(emptyList())

        var receivedEvent: BrailuxRestoreEvent? = null
        val job = coordinatorScope.launch {
            receivedEvent = coordinator.restoreEvents.first()
        }

        coordinator.restorePurchases()
        job.join()

        assertEquals(BrailuxRestoreEvent.RestoreEmpty, receivedEvent)
        coordinator.destroy()
    }

    // Condition 35: RestorePurchases emits RestoreError when repository fails
    @Test
    fun restorePurchasesEmitsRestoreErrorWhenRepositoryFails() = runBlocking {
        val coordinator = BrailuxBillingCoordinator(
            billingRepository = fakeBillingRepository,
            entitlementRepository = entitlementRepository,
            coroutineScope = coordinatorScope,
        )
        coordinator.initialize()

        fakeBillingRepository.restoreResult = Result.failure(IOException("Play Store disconnected"))

        var receivedEvent: BrailuxRestoreEvent? = null
        val job = coordinatorScope.launch {
            receivedEvent = coordinator.restoreEvents.first()
        }

        coordinator.restorePurchases()
        job.join()

        assertEquals(BrailuxRestoreEvent.RestoreError, receivedEvent)
        coordinator.destroy()
    }

    // Condition 36: Restore purchases does not directly grant entitlements
    @Test
    fun restorePurchasesDoesNotDirectlyGrantEntitlement() = runBlocking {
        val coordinator = BrailuxBillingCoordinator(
            billingRepository = fakeBillingRepository,
            entitlementRepository = entitlementRepository,
            coroutineScope = coordinatorScope,
        )
        coordinator.initialize()

        // Simular un producto en restore que NO es elegible (ej. consumible o token vacío)
        val ineligibleRecord = BrailuxPurchaseRecord(
            productId = "unknown_product",
            purchaseToken = "",
            purchaseTimeMillis = 1000L,
            purchaseState = BrailuxPurchaseState.Purchased,
            isAcknowledged = true,
        )
        fakeBillingRepository.restoreResult = Result.success(listOf(ineligibleRecord))

        coordinator.restorePurchases()

        assertTrue(entitlementRepository.ownedBackgroundIds.value.isEmpty())
        coordinator.destroy()
    }

    // Condition 37: Coordinator destroy cancels observers and calls repository.endConnection without cancelling external scope
    @Test
    fun destroyCancelsCoordinatorObserversAndCallsEndConnectionWithoutCancellingExternalScope() = runBlocking {
        val externalScope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())
        val coordinator = BrailuxBillingCoordinator(
            billingRepository = fakeBillingRepository,
            entitlementRepository = entitlementRepository,
            coroutineScope = externalScope,
        )
        coordinator.initialize()

        coordinator.destroy()

        assertEquals(1, fakeBillingRepository.endConnectionCount)
        assertEquals(BillingConnectionState.Disconnected, fakeBillingRepository.connectionState.value)
        // El scope externo no debe haberse cancelado
        assertTrue(externalScope.coroutineContext[Job]?.isActive == true)
        externalScope.cancel()
    }

    // Condition 38: Authoritative source of truth: Google Play is authoritative, DataStore is only cache
    @Test
    fun googlePlayIsAuthoritativeSourceOfTruthDataStoreIsOnlyCache() = runBlocking {
        dataStore.edit { preferences ->
            preferences[OwnedBackgroundIdsCacheKey] = setOf(BrailuxBackgroundCatalog.CELESTE_GEOMETRICO_ID)
        }
        entitlementRepository.loadCachedEntitlements()
        assertTrue(entitlementRepository.isBackgroundOwned(BrailuxBackgroundCatalog.CELESTE_GEOMETRICO_ID))

        // Si la reconciliación autoritativa con Google Play devuelve lista vacía, el derecho se retira
        entitlementRepository.reconcileFromPurchases(emptyList())
        assertFalse(entitlementRepository.isBackgroundOwned(BrailuxBackgroundCatalog.CELESTE_GEOMETRICO_ID))
    }

    // Condition 39: Debug flags never unlock premium backgrounds artificially
    @Test
    fun debugFlagsNeverUnlockPremiumBackgrounds() {
        val state = BrailuxPremiumAccess.resolveState(
            isDebug = true,
            isPremiumUnlocked = false,
            ownedBackgroundIds = emptySet(),
        )
        assertFalse(state.isPremiumUnlocked)
        assertFalse(state.isBackgroundUnlocked(BrailuxBackgroundCatalog.CELESTE_GEOMETRICO_ID))
        assertFalse(state.isBackgroundUnlocked(BrailuxBackgroundCatalog.CREMA_ONDAS_ID))
        assertFalse(state.isBackgroundUnlocked(BrailuxBackgroundCatalog.LAVANDA_NIEBLA_ID))
        assertFalse(state.isBackgroundUnlocked(BrailuxBackgroundCatalog.SALVIA_TEXTURA_ID))
    }

    // Condition 40: Presentation resolver for DEFAULT background
    @Test
    fun presentationResolvesCorrectlyForDefault() {
        val defaultBg = BrailuxBackgroundCatalog.backgrounds.first { it.id == BrailuxBackgroundCatalog.DEFAULT_ID }
        val presentation = BrailuxBillingPresentation.resolvePresentation(
            background = defaultBg,
            itemStatus = BrailuxThemePurchaseStatus.Free,
        )
        assertFalse(presentation.showPremiumBadge)
        assertNull(presentation.statusLabel)
        assertTrue(presentation.isSelectable)
        assertFalse(presentation.isPurchasable)
        assertTrue(presentation.canPreview)
    }

    // Condition 41: Presentation resolver for Purchased premium background
    @Test
    fun presentationResolvesCorrectlyForPurchased() {
        val premiumBg = BrailuxBackgroundCatalog.backgrounds.first { it.id == BrailuxBackgroundCatalog.CELESTE_GEOMETRICO_ID }
        val presentation = BrailuxBillingPresentation.resolvePresentation(
            background = premiumBg,
            itemStatus = BrailuxThemePurchaseStatus.Purchased,
        )
        assertTrue(presentation.showPremiumBadge)
        assertEquals("Comprado", presentation.statusLabel)
        assertTrue(presentation.isSelectable)
        assertFalse(presentation.isPurchasable)
        assertTrue(presentation.canPreview)
    }

    // Condition 42: Presentation resolver for Pending premium background
    @Test
    fun presentationResolvesCorrectlyForPending() {
        val premiumBg = BrailuxBackgroundCatalog.backgrounds.first { it.id == BrailuxBackgroundCatalog.CREMA_ONDAS_ID }
        val presentation = BrailuxBillingPresentation.resolvePresentation(
            background = premiumBg,
            itemStatus = BrailuxThemePurchaseStatus.Pending,
        )
        assertTrue(presentation.showPremiumBadge)
        assertEquals("Pendiente", presentation.statusLabel)
        assertFalse(presentation.isSelectable)
        assertFalse(presentation.isPurchasable)
        assertTrue(presentation.canPreview)
    }

    // Condition 43: Presentation resolver for AvailableForPurchase premium background
    @Test
    fun presentationResolvesCorrectlyForAvailable() {
        val premiumBg = BrailuxBackgroundCatalog.backgrounds.first { it.id == BrailuxBackgroundCatalog.LAVANDA_NIEBLA_ID }
        val presentation = BrailuxBillingPresentation.resolvePresentation(
            background = premiumBg,
            itemStatus = BrailuxThemePurchaseStatus.AvailableForPurchase(
                formattedPrice = "S/ 3.50",
                offerToken = "offer_tok",
            ),
        )
        assertTrue(presentation.showPremiumBadge)
        assertEquals("S/ 3.50", presentation.statusLabel)
        assertFalse(presentation.isSelectable)
        assertTrue(presentation.isPurchasable)
        assertTrue(presentation.canPreview)
    }

    // Condition 44: Presentation resolver for Unavailable premium background
    @Test
    fun presentationResolvesCorrectlyForUnavailable() {
        val premiumBg = BrailuxBackgroundCatalog.backgrounds.first { it.id == BrailuxBackgroundCatalog.SALVIA_TEXTURA_ID }
        val presentation = BrailuxBillingPresentation.resolvePresentation(
            background = premiumBg,
            itemStatus = BrailuxThemePurchaseStatus.Unavailable,
        )
        assertTrue(presentation.showPremiumBadge)
        assertEquals("No disponible", presentation.statusLabel)
        assertFalse(presentation.isSelectable)
        assertFalse(presentation.isPurchasable)
        assertTrue(presentation.canPreview)
    }
}
