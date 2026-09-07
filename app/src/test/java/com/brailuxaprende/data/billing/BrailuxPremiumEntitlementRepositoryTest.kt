package com.brailuxaprende.data.billing

import android.app.Activity
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsResult
import com.brailuxaprende.data.settings.BrailuxBackgroundCatalog
import com.brailuxaprende.data.settings.BrailuxBackgroundRotationPolicy
import com.brailuxaprende.data.settings.BrailuxPremiumAccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class BrailuxPremiumEntitlementRepositoryTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var dataStoreFile: File
    private lateinit var dataStoreScope: CoroutineScope
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var entitlementRepository: BrailuxPremiumEntitlementRepository

    @Before
    fun setUp() {
        BrailuxPremiumAccess.reset()
        dataStoreFile = File(temporaryFolder.root, "test_entitlements.preferences_pb")
        dataStoreScope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())
        dataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { dataStoreFile },
        )
        entitlementRepository = BrailuxPremiumEntitlementRepository(
            dataStore = dataStore,
            ioDispatcher = Dispatchers.Unconfined,
        )
    }

    @After
    fun tearDown() {
        BrailuxPremiumAccess.reset()
        dataStoreScope.cancel()
    }

    private fun createPurchaseRecord(
        productId: String,
        purchaseToken: String = "token_$productId",
        purchaseState: BrailuxPurchaseState = BrailuxPurchaseState.Purchased,
        isAcknowledged: Boolean = false,
        purchaseTimeMillis: Long = 1000L,
    ): BrailuxPurchaseRecord = BrailuxPurchaseRecord(
        productId = productId,
        purchaseToken = purchaseToken,
        purchaseTimeMillis = purchaseTimeMillis,
        purchaseState = purchaseState,
        isAcknowledged = isAcknowledged,
    )

    private class TestBillingGateway : BrailuxBillingGateway {
        override var isReady: Boolean = true
        var connectionListener: BillingClientStateListener? = null
        var queryPurchasesResult: Pair<BillingResult, List<Purchase>> =
            BillingResult.newBuilder().setResponseCode(BillingClient.BillingResponseCode.OK).build() to emptyList()
        var queryProductDetailsResult: Pair<BillingResult, QueryProductDetailsResult?> =
            BillingResult.newBuilder().setResponseCode(BillingClient.BillingResponseCode.OK).build() to null

        var acknowledgePurchaseResult: BillingResult =
            BillingResult.newBuilder().setResponseCode(BillingClient.BillingResponseCode.OK).build()
        var acknowledgePurchaseCount: Int = 0
        var lastAcknowledgedToken: String? = null

        override fun startConnection(listener: BillingClientStateListener) {
            connectionListener = listener
            val result = BillingResult.newBuilder()
                .setResponseCode(BillingClient.BillingResponseCode.OK)
                .build()
            listener.onBillingSetupFinished(result)
        }

        override fun endConnection() {
            isReady = false
        }

        override suspend fun queryProductDetails(productIds: List<String>): Pair<BillingResult, QueryProductDetailsResult?> {
            return queryProductDetailsResult
        }

        override suspend fun queryPurchases(): Pair<BillingResult, List<Purchase>> {
            return queryPurchasesResult
        }

        override fun launchBillingFlow(
            activity: Activity,
            productDetails: ProductDetails,
            offerToken: String,
        ): BillingResult {
            return BillingResult.newBuilder().setResponseCode(BillingClient.BillingResponseCode.OK).build()
        }

        override suspend fun acknowledgePurchase(purchaseToken: String): BillingResult {
            acknowledgePurchaseCount++
            lastAcknowledgedToken = purchaseToken
            return acknowledgePurchaseResult
        }
    }

    private class TestPurchase(
        private val products: List<String>,
        private val token: String = "tok_test",
        private val purchaseState: Int = Purchase.PurchaseState.PURCHASED,
        private val acknowledged: Boolean = false,
        private val purchaseTime: Long = 1000L,
    ) : Purchase("{\"productId\":\"test\"}", "signature") {
        override fun getProducts(): List<String> = products
        override fun getPurchaseToken(): String = token
        override fun getPurchaseTime(): Long = purchaseTime
        override fun getPurchaseState(): Int = purchaseState
        override fun isAcknowledged(): Boolean = acknowledged
    }

    // 1. PURCHASED reconocido concede background correcto.
    @Test
    fun test01_purchasedRecognizedGrantsCorrectBackground() = runBlocking {
        val purchase = createPurchaseRecord(
            productId = BrailuxBillingProductCatalog.PRODUCT_ID_CELESTE_GEOMETRICO,
            purchaseToken = "token_celeste",
        )

        val result = entitlementRepository.reconcileFromPurchases(listOf(purchase))

        assertEquals(setOf(BrailuxBackgroundCatalog.CELESTE_GEOMETRICO_ID), result)
        assertTrue(entitlementRepository.isBackgroundOwned(BrailuxBackgroundCatalog.CELESTE_GEOMETRICO_ID))
    }

    // 2. PENDING no concede entitlement.
    @Test
    fun test02_pendingDoesNotGrantEntitlement() = runBlocking {
        val pendingPurchase = createPurchaseRecord(
            productId = BrailuxBillingProductCatalog.PRODUCT_ID_CELESTE_GEOMETRICO,
            purchaseState = BrailuxPurchaseState.Pending,
        )

        assertFalse(entitlementRepository.isPurchaseEligible(pendingPurchase))
        val granted = entitlementRepository.grantEntitlementForPurchase(pendingPurchase)
        assertFalse(granted)

        val reconciled = entitlementRepository.reconcileFromPurchases(listOf(pendingPurchase))
        assertTrue(reconciled.isEmpty())
        assertFalse(entitlementRepository.isBackgroundOwned(BrailuxBackgroundCatalog.CELESTE_GEOMETRICO_ID))
    }

    // 3. Producto desconocido no concede entitlement.
    @Test
    fun test03_unknownProductDoesNotGrantEntitlement() = runBlocking {
        val unknownPurchase = createPurchaseRecord(
            productId = "com.unknown.theme",
            purchaseToken = "tok_unknown",
        )

        assertFalse(entitlementRepository.isPurchaseEligible(unknownPurchase))
        val result = entitlementRepository.reconcileFromPurchases(listOf(unknownPurchase))
        assertTrue(result.isEmpty())
    }

    // 4. Token vacío no concede entitlement.
    @Test
    fun test04_emptyTokenDoesNotGrantEntitlement() = runBlocking {
        val emptyTokenPurchase = createPurchaseRecord(
            productId = BrailuxBillingProductCatalog.PRODUCT_ID_CREMA_ONDAS,
            purchaseToken = "   ",
        )

        assertFalse(entitlementRepository.isPurchaseEligible(emptyTokenPurchase))
        val result = entitlementRepository.reconcileFromPurchases(listOf(emptyTokenPurchase))
        assertTrue(result.isEmpty())
    }

    // 5. 1 compra produce 1 ownedBackgroundId.
    @Test
    fun test05_singlePurchaseProducesOneOwnedBackgroundId() = runBlocking {
        val purchase = createPurchaseRecord(BrailuxBillingProductCatalog.PRODUCT_ID_LAVANDA_NIEBLA)

        val result = entitlementRepository.reconcileFromPurchases(listOf(purchase))

        assertEquals(1, result.size)
        assertEquals(setOf(BrailuxBackgroundCatalog.LAVANDA_NIEBLA_ID), result)
    }

    // 6. 4 compras producen 4 IDs.
    @Test
    fun test06_fourPurchasesProduceFourIds() = runBlocking {
        val purchases = listOf(
            createPurchaseRecord(BrailuxBillingProductCatalog.PRODUCT_ID_CELESTE_GEOMETRICO),
            createPurchaseRecord(BrailuxBillingProductCatalog.PRODUCT_ID_CREMA_ONDAS),
            createPurchaseRecord(BrailuxBillingProductCatalog.PRODUCT_ID_LAVANDA_NIEBLA),
            createPurchaseRecord(BrailuxBillingProductCatalog.PRODUCT_ID_SALVIA_TEXTURA),
        )

        val result = entitlementRepository.reconcileFromPurchases(purchases)

        assertEquals(4, result.size)
        assertTrue(result.contains(BrailuxBackgroundCatalog.CELESTE_GEOMETRICO_ID))
        assertTrue(result.contains(BrailuxBackgroundCatalog.CREMA_ONDAS_ID))
        assertTrue(result.contains(BrailuxBackgroundCatalog.LAVANDA_NIEBLA_ID))
        assertTrue(result.contains(BrailuxBackgroundCatalog.SALVIA_TEXTURA_ID))
    }

    // 7. Duplicados no duplican entitlement.
    @Test
    fun test07_duplicatesDoNotDuplicateEntitlement() = runBlocking {
        val purchases = listOf(
            createPurchaseRecord(BrailuxBillingProductCatalog.PRODUCT_ID_SALVIA_TEXTURA, "token_1"),
            createPurchaseRecord(BrailuxBillingProductCatalog.PRODUCT_ID_SALVIA_TEXTURA, "token_2"),
        )

        val result = entitlementRepository.reconcileFromPurchases(purchases)

        assertEquals(1, result.size)
        assertEquals(setOf(BrailuxBackgroundCatalog.SALVIA_TEXTURA_ID), result)
    }

    // 8. Producto desaparecido tras query exitosa retira entitlement.
    @Test
    fun test08_disappearedProductAfterSuccessfulQueryRevokesEntitlement() = runBlocking {
        // Inicialmente celeste y crema
        entitlementRepository.reconcileFromPurchases(
            listOf(
                createPurchaseRecord(BrailuxBillingProductCatalog.PRODUCT_ID_CELESTE_GEOMETRICO),
                createPurchaseRecord(BrailuxBillingProductCatalog.PRODUCT_ID_CREMA_ONDAS),
            ),
        )
        assertEquals(2, entitlementRepository.ownedBackgroundIds.value.size)

        // Query exitosa posterior informa únicamente celeste
        entitlementRepository.reconcileFromPurchases(
            listOf(createPurchaseRecord(BrailuxBillingProductCatalog.PRODUCT_ID_CELESTE_GEOMETRICO)),
        )

        assertEquals(setOf(BrailuxBackgroundCatalog.CELESTE_GEOMETRICO_ID), entitlementRepository.ownedBackgroundIds.value)
        assertFalse(entitlementRepository.isBackgroundOwned(BrailuxBackgroundCatalog.CREMA_ONDAS_ID))
    }

    // 9. Query fallida NO retira entitlement.
    @Test
    fun test09_failedQueryDoesNotRevokeEntitlement() = runBlocking {
        val gateway = TestBillingGateway()
        val repository = GooglePlayBillingRepository(
            gateway = gateway,
            mainDispatcher = Dispatchers.Unconfined,
            entitlementRepository = entitlementRepository,
            coroutineScope = this,
        )

        // Inicialmente comprar celeste
        entitlementRepository.reconcileFromPurchases(
            listOf(createPurchaseRecord(BrailuxBillingProductCatalog.PRODUCT_ID_CELESTE_GEOMETRICO)),
        )
        assertTrue(entitlementRepository.isBackgroundOwned(BrailuxBackgroundCatalog.CELESTE_GEOMETRICO_ID))

        // Simular que queryPurchases falla por error de red
        gateway.queryPurchasesResult = BillingResult.newBuilder()
            .setResponseCode(BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE)
            .build() to emptyList()

        val result = repository.queryPurchases()
        assertTrue(result.isFailure)

        // Entitlement previo NO debe ser retirado
        assertTrue(entitlementRepository.isBackgroundOwned(BrailuxBackgroundCatalog.CELESTE_GEOMETRICO_ID))
    }

    // 10. Reembolso simulado elimina derecho tras sync exitoso.
    @Test
    fun test10_simulatedRefundRevokesEntitlementAfterSuccessfulSync() = runBlocking {
        val gateway = TestBillingGateway()
        val repository = GooglePlayBillingRepository(
            gateway = gateway,
            mainDispatcher = Dispatchers.Unconfined,
            entitlementRepository = entitlementRepository,
            coroutineScope = this,
        )

        // Usuario tenía celeste y crema
        entitlementRepository.reconcileFromPurchases(
            listOf(
                createPurchaseRecord(BrailuxBillingProductCatalog.PRODUCT_ID_CELESTE_GEOMETRICO),
                createPurchaseRecord(BrailuxBillingProductCatalog.PRODUCT_ID_CREMA_ONDAS),
            ),
        )

        // Play Store ahora sólo reporta celeste (crema fue reembolsado)
        gateway.queryPurchasesResult = BillingResult.newBuilder()
            .setResponseCode(BillingClient.BillingResponseCode.OK)
            .build() to listOf(
                TestPurchase(
                    products = listOf(BrailuxBillingProductCatalog.PRODUCT_ID_CELESTE_GEOMETRICO),
                    token = "tok_celeste",
                ),
            )

        val syncResult = repository.syncBillingData()
        assertTrue(syncResult.isSuccess)

        // Crema debe haber sido retirado y celeste conservado
        assertTrue(entitlementRepository.isBackgroundOwned(BrailuxBackgroundCatalog.CELESTE_GEOMETRICO_ID))
        assertFalse(entitlementRepository.isBackgroundOwned(BrailuxBackgroundCatalog.CREMA_ONDAS_ID))
    }

    // 11. Compra PENDING no ejecuta acknowledge.
    @Test
    fun test11_pendingPurchaseDoesNotExecuteAcknowledge() = runBlocking {
        val gateway = TestBillingGateway()
        val repository = GooglePlayBillingRepository(
            gateway = gateway,
            mainDispatcher = Dispatchers.Unconfined,
            entitlementRepository = entitlementRepository,
            coroutineScope = this,
        )

        gateway.queryPurchasesResult = BillingResult.newBuilder()
            .setResponseCode(BillingClient.BillingResponseCode.OK)
            .build() to listOf(
                TestPurchase(
                    products = listOf(BrailuxBillingProductCatalog.PRODUCT_ID_SALVIA_TEXTURA),
                    token = "tok_pending",
                    purchaseState = Purchase.PurchaseState.PENDING,
                    acknowledged = false,
                ),
            )

        repository.restorePurchases()

        assertEquals(0, gateway.acknowledgePurchaseCount)
        assertFalse(entitlementRepository.isBackgroundOwned(BrailuxBackgroundCatalog.SALVIA_TEXTURA_ID))
    }

    // 12. PURCHASED no reconocido no ejecuta acknowledge.
    @Test
    fun test12_unrecognizedPurchasedDoesNotExecuteAcknowledge() = runBlocking {
        val gateway = TestBillingGateway()
        val repository = GooglePlayBillingRepository(
            gateway = gateway,
            mainDispatcher = Dispatchers.Unconfined,
            entitlementRepository = entitlementRepository,
            coroutineScope = this,
        )

        gateway.queryPurchasesResult = BillingResult.newBuilder()
            .setResponseCode(BillingClient.BillingResponseCode.OK)
            .build() to listOf(
                TestPurchase(
                    products = listOf("com.unknown.arbitrary"),
                    token = "tok_unknown",
                    purchaseState = Purchase.PurchaseState.PURCHASED,
                    acknowledged = false,
                ),
            )

        repository.restorePurchases()

        assertEquals(0, gateway.acknowledgePurchaseCount)
    }

    // 13. PURCHASED acknowledged no vuelve a acknowledge.
    @Test
    fun test13_alreadyAcknowledgedPurchasedDoesNotAcknowledgeAgain() = runBlocking {
        val gateway = TestBillingGateway()
        val repository = GooglePlayBillingRepository(
            gateway = gateway,
            mainDispatcher = Dispatchers.Unconfined,
            entitlementRepository = entitlementRepository,
            coroutineScope = this,
        )

        gateway.queryPurchasesResult = BillingResult.newBuilder()
            .setResponseCode(BillingClient.BillingResponseCode.OK)
            .build() to listOf(
                TestPurchase(
                    products = listOf(BrailuxBillingProductCatalog.PRODUCT_ID_LAVANDA_NIEBLA),
                    token = "tok_already_ack",
                    purchaseState = Purchase.PurchaseState.PURCHASED,
                    acknowledged = true,
                ),
            )

        repository.restorePurchases()

        assertEquals(0, gateway.acknowledgePurchaseCount)
        assertTrue(entitlementRepository.isBackgroundOwned(BrailuxBackgroundCatalog.LAVANDA_NIEBLA_ID))
    }

    // 14. PURCHASED no acknowledged ejecuta acknowledge.
    @Test
    fun test14_unacknowledgedPurchasedExecutesAcknowledge() = runBlocking {
        val gateway = TestBillingGateway()
        val repository = GooglePlayBillingRepository(
            gateway = gateway,
            mainDispatcher = Dispatchers.Unconfined,
            entitlementRepository = entitlementRepository,
            coroutineScope = this,
        )

        gateway.queryPurchasesResult = BillingResult.newBuilder()
            .setResponseCode(BillingClient.BillingResponseCode.OK)
            .build() to listOf(
                TestPurchase(
                    products = listOf(BrailuxBillingProductCatalog.PRODUCT_ID_LAVANDA_NIEBLA),
                    token = "tok_need_ack",
                    purchaseState = Purchase.PurchaseState.PURCHASED,
                    acknowledged = false,
                ),
            )

        repository.restorePurchases()

        assertEquals(1, gateway.acknowledgePurchaseCount)
        assertEquals("tok_need_ack", gateway.lastAcknowledgedToken)
    }

    // 15. Acknowledge correcto conserva entitlement.
    @Test
    fun test15_successfulAcknowledgePreservesEntitlement() = runBlocking {
        val gateway = TestBillingGateway()
        val repository = GooglePlayBillingRepository(
            gateway = gateway,
            mainDispatcher = Dispatchers.Unconfined,
            entitlementRepository = entitlementRepository,
            coroutineScope = this,
        )
        repository.startConnection()

        val purchase = createPurchaseRecord(
            productId = BrailuxBillingProductCatalog.PRODUCT_ID_CELESTE_GEOMETRICO,
            purchaseToken = "tok_ack_ok",
        )
        entitlementRepository.grantEntitlementForPurchase(purchase)

        val ackResult = repository.acknowledgePurchase("tok_ack_ok")
        assertTrue(ackResult.isSuccess)
        assertTrue(entitlementRepository.isBackgroundOwned(BrailuxBackgroundCatalog.CELESTE_GEOMETRICO_ID))
    }

    // 16. Acknowledge fallido conserva entitlement pero expone error.
    @Test
    fun test16_failedAcknowledgePreservesEntitlementAndExposesError() = runBlocking {
        val gateway = TestBillingGateway()
        gateway.acknowledgePurchaseResult = BillingResult.newBuilder()
            .setResponseCode(BillingClient.BillingResponseCode.ERROR)
            .setDebugMessage("Network timeout on ack")
            .build()

        val repository = GooglePlayBillingRepository(
            gateway = gateway,
            mainDispatcher = Dispatchers.Unconfined,
            entitlementRepository = entitlementRepository,
            coroutineScope = this,
        )
        repository.startConnection()

        val purchase = createPurchaseRecord(
            productId = BrailuxBillingProductCatalog.PRODUCT_ID_SALVIA_TEXTURA,
            purchaseToken = "tok_ack_fail",
        )
        entitlementRepository.grantEntitlementForPurchase(purchase)

        val ackResult = repository.acknowledgePurchase("tok_ack_fail")
        assertTrue(ackResult.isFailure)
        assertEquals(
            BillingClient.BillingResponseCode.ERROR,
            repository.lastBillingError.value?.responseCode,
        )

        // El derecho Premium NO debe ser borrado por fallo de confirmación
        assertTrue(entitlementRepository.isBackgroundOwned(BrailuxBackgroundCatalog.SALVIA_TEXTURA_ID))
    }

    // 17. restorePurchases reconcilia correctamente.
    @Test
    fun test17_restorePurchasesReconcilesCorrectly() = runBlocking {
        val gateway = TestBillingGateway()
        val repository = GooglePlayBillingRepository(
            gateway = gateway,
            mainDispatcher = Dispatchers.Unconfined,
            entitlementRepository = entitlementRepository,
            coroutineScope = this,
        )

        gateway.queryPurchasesResult = BillingResult.newBuilder()
            .setResponseCode(BillingClient.BillingResponseCode.OK)
            .build() to listOf(
                TestPurchase(
                    products = listOf(BrailuxBillingProductCatalog.PRODUCT_ID_CREMA_ONDAS),
                    token = "tok_restored_crema",
                    acknowledged = true,
                ),
            )

        val result = repository.restorePurchases()
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrThrow().size)
        assertTrue(entitlementRepository.isBackgroundOwned(BrailuxBackgroundCatalog.CREMA_ONDAS_ID))
    }

    // 18. syncBillingData reconcilia correctamente.
    @Test
    fun test18_syncBillingDataReconcilesCorrectly() = runBlocking {
        val gateway = TestBillingGateway()
        val repository = GooglePlayBillingRepository(
            gateway = gateway,
            mainDispatcher = Dispatchers.Unconfined,
            entitlementRepository = entitlementRepository,
            coroutineScope = this,
        )

        gateway.queryPurchasesResult = BillingResult.newBuilder()
            .setResponseCode(BillingClient.BillingResponseCode.OK)
            .build() to listOf(
                TestPurchase(
                    products = listOf(BrailuxBillingProductCatalog.PRODUCT_ID_SALVIA_TEXTURA),
                    token = "tok_synced_salvia",
                    acknowledged = true,
                ),
            )

        val result = repository.syncBillingData()
        assertTrue(result.isSuccess)
        assertTrue(entitlementRepository.isBackgroundOwned(BrailuxBackgroundCatalog.SALVIA_TEXTURA_ID))
    }

    // 19. Offline conserva último cache confirmado.
    @Test
    fun test19_offlinePreservesLastConfirmedCache() = runBlocking {
        // Simular guardado previo en cache auxiliar
        dataStore.edit { preferences ->
            preferences[OwnedBackgroundIdsCacheKey] = setOf(
                BrailuxBackgroundCatalog.CELESTE_GEOMETRICO_ID,
                BrailuxBackgroundCatalog.CREMA_ONDAS_ID,
            )
        }

        // Crear repositorio en frío sin conexión y cargar cache
        val offlineRepo = BrailuxPremiumEntitlementRepository(
            dataStore = dataStore,
            ioDispatcher = Dispatchers.Unconfined,
        )
        val loaded = offlineRepo.loadCachedEntitlements()

        assertEquals(2, loaded.size)
        assertTrue(loaded.contains(BrailuxBackgroundCatalog.CELESTE_GEOMETRICO_ID))
        assertTrue(loaded.contains(BrailuxBackgroundCatalog.CREMA_ONDAS_ID))
        assertTrue(BrailuxPremiumAccess.currentState.ownedBackgroundIds.contains(BrailuxBackgroundCatalog.CELESTE_GEOMETRICO_ID))
    }

    // 20. Reconexión reemplaza cache con Google Play.
    @Test
    fun test20_reconnectionReplacesCacheWithGooglePlay() = runBlocking {
        // Cache inicial con celeste y crema
        dataStore.edit { preferences ->
            preferences[OwnedBackgroundIdsCacheKey] = setOf(
                BrailuxBackgroundCatalog.CELESTE_GEOMETRICO_ID,
                BrailuxBackgroundCatalog.CREMA_ONDAS_ID,
            )
        }
        entitlementRepository.loadCachedEntitlements()
        assertEquals(2, entitlementRepository.ownedBackgroundIds.value.size)

        // Google Play se reconecta y devuelve únicamente salvia
        val purchases = listOf(createPurchaseRecord(BrailuxBillingProductCatalog.PRODUCT_ID_SALVIA_TEXTURA))
        entitlementRepository.reconcileFromPurchases(purchases)

        assertEquals(setOf(BrailuxBackgroundCatalog.SALVIA_TEXTURA_ID), entitlementRepository.ownedBackgroundIds.value)

        // Verificar que el cache en DataStore fue actualizado con el nuevo estado
        val cachedInStore = dataStore.data.first()[OwnedBackgroundIdsCacheKey]
        assertEquals(setOf(BrailuxBackgroundCatalog.SALVIA_TEXTURA_ID), cachedInStore)
    }

    // 21. Nueva cuenta sin compras produce Set vacío tras sync exitoso.
    @Test
    fun test21_newAccountWithNoPurchasesProducesEmptySetAfterSuccessfulSync() = runBlocking {
        // Preexistía celeste en cache de sesión anterior
        entitlementRepository.reconcileFromPurchases(
            listOf(createPurchaseRecord(BrailuxBillingProductCatalog.PRODUCT_ID_CELESTE_GEOMETRICO)),
        )
        assertEquals(1, entitlementRepository.ownedBackgroundIds.value.size)

        // Nueva cuenta sin compras
        val gateway = TestBillingGateway()
        gateway.queryPurchasesResult = BillingResult.newBuilder()
            .setResponseCode(BillingClient.BillingResponseCode.OK)
            .build() to emptyList()

        val repository = GooglePlayBillingRepository(
            gateway = gateway,
            mainDispatcher = Dispatchers.Unconfined,
            entitlementRepository = entitlementRepository,
            coroutineScope = this,
        )

        repository.syncBillingData()

        assertTrue(entitlementRepository.ownedBackgroundIds.value.isEmpty())
        assertTrue(BrailuxPremiumAccess.currentState.ownedBackgroundIds.isEmpty())
    }

    // 22. Múltiples compras son idempotentes.
    @Test
    fun test22_multiplePurchasesAreIdempotent() = runBlocking {
        val purchases = listOf(
            createPurchaseRecord(BrailuxBillingProductCatalog.PRODUCT_ID_CELESTE_GEOMETRICO),
            createPurchaseRecord(BrailuxBillingProductCatalog.PRODUCT_ID_CREMA_ONDAS),
        )

        val run1 = entitlementRepository.reconcileFromPurchases(purchases)
        val run2 = entitlementRepository.reconcileFromPurchases(purchases)
        val run3 = entitlementRepository.reconcileFromPurchases(purchases)

        assertEquals(run1, run2)
        assertEquals(run2, run3)
        assertEquals(2, entitlementRepository.ownedBackgroundIds.value.size)
    }

    // 23. ownedBackgroundIds alimenta correctamente PremiumAccess.
    @Test
    fun test23_ownedBackgroundIdsFeedsPremiumAccess() = runBlocking {
        val purchases = listOf(createPurchaseRecord(BrailuxBillingProductCatalog.PRODUCT_ID_LAVANDA_NIEBLA))
        entitlementRepository.reconcileFromPurchases(purchases)

        val state = BrailuxPremiumAccess.currentState
        assertTrue(state.ownedBackgroundIds.contains(BrailuxBackgroundCatalog.LAVANDA_NIEBLA_ID))
        assertTrue(state.isBackgroundUnlocked(BrailuxBackgroundCatalog.LAVANDA_NIEBLA_ID))
        assertFalse(state.isBackgroundUnlocked(BrailuxBackgroundCatalog.SALVIA_TEXTURA_ID))
    }

    // 24. 2+ owned IDs permiten política de rotación.
    @Test
    fun test24_twoOrMoreOwnedIdsEnableRotationPolicy() = runBlocking {
        val purchases = listOf(
            createPurchaseRecord(BrailuxBillingProductCatalog.PRODUCT_ID_CELESTE_GEOMETRICO),
            createPurchaseRecord(BrailuxBillingProductCatalog.PRODUCT_ID_SALVIA_TEXTURA),
        )
        entitlementRepository.reconcileFromPurchases(purchases)

        val owned = BrailuxPremiumAccess.currentState.ownedBackgroundIds
        assertTrue(BrailuxBackgroundRotationPolicy.canRotate(ownedBackgroundIds = owned))

        val next = BrailuxBackgroundRotationPolicy.nextPremiumBackgroundId(
            currentId = BrailuxBackgroundCatalog.CELESTE_GEOMETRICO_ID,
            ownedBackgroundIds = owned,
        )
        assertEquals(BrailuxBackgroundCatalog.SALVIA_TEXTURA_ID, next)
    }

    // 25. 1 ID no activa rotación múltiple.
    @Test
    fun test25_singleOwnedIdDoesNotEnableMultipleRotation() = runBlocking {
        val purchases = listOf(createPurchaseRecord(BrailuxBillingProductCatalog.PRODUCT_ID_CELESTE_GEOMETRICO))
        entitlementRepository.reconcileFromPurchases(purchases)

        val owned = BrailuxPremiumAccess.currentState.ownedBackgroundIds
        assertFalse(BrailuxBackgroundRotationPolicy.canRotate(ownedBackgroundIds = owned))

        val next = BrailuxBackgroundRotationPolicy.nextPremiumBackgroundId(
            currentId = BrailuxBackgroundCatalog.CELESTE_GEOMETRICO_ID,
            ownedBackgroundIds = owned,
        )
        assertNull(next)
    }

    // 26. Preview bloqueado continúa sin conceder derecho.
    @Test
    fun test26_lockedPreviewContinuesToNotGrantEntitlement() {
        assertTrue(BrailuxBackgroundCatalog.canPreview(BrailuxBackgroundCatalog.SALVIA_TEXTURA_ID))

        val canSelectWithoutOwnership = BrailuxBackgroundCatalog.canSelect(
            id = BrailuxBackgroundCatalog.SALVIA_TEXTURA_ID,
            isPremiumUnlocked = false,
            ownedBackgroundIds = emptySet(),
        )
        assertFalse(canSelectWithoutOwnership)
    }

    // 27. Compra de celeste no desbloquea crema.
    @Test
    fun test27_purchaseOfCelesteDoesNotUnlockCrema() = runBlocking {
        entitlementRepository.reconcileFromPurchases(
            listOf(createPurchaseRecord(BrailuxBillingProductCatalog.PRODUCT_ID_CELESTE_GEOMETRICO)),
        )
        val owned = BrailuxPremiumAccess.currentState.ownedBackgroundIds

        assertTrue(BrailuxBackgroundCatalog.canSelect(BrailuxBackgroundCatalog.CELESTE_GEOMETRICO_ID, ownedBackgroundIds = owned))
        assertFalse(BrailuxBackgroundCatalog.canSelect(BrailuxBackgroundCatalog.CREMA_ONDAS_ID, ownedBackgroundIds = owned))
    }

    // 28. Compra de crema no desbloquea lavanda.
    @Test
    fun test28_purchaseOfCremaDoesNotUnlockLavanda() = runBlocking {
        entitlementRepository.reconcileFromPurchases(
            listOf(createPurchaseRecord(BrailuxBillingProductCatalog.PRODUCT_ID_CREMA_ONDAS)),
        )
        val owned = BrailuxPremiumAccess.currentState.ownedBackgroundIds

        assertTrue(BrailuxBackgroundCatalog.canSelect(BrailuxBackgroundCatalog.CREMA_ONDAS_ID, ownedBackgroundIds = owned))
        assertFalse(BrailuxBackgroundCatalog.canSelect(BrailuxBackgroundCatalog.LAVANDA_NIEBLA_ID, ownedBackgroundIds = owned))
    }

    // 29. Compra de lavanda no desbloquea salvia.
    @Test
    fun test29_purchaseOfLavandaDoesNotUnlockSalvia() = runBlocking {
        entitlementRepository.reconcileFromPurchases(
            listOf(createPurchaseRecord(BrailuxBillingProductCatalog.PRODUCT_ID_LAVANDA_NIEBLA)),
        )
        val owned = BrailuxPremiumAccess.currentState.ownedBackgroundIds

        assertTrue(BrailuxBackgroundCatalog.canSelect(BrailuxBackgroundCatalog.LAVANDA_NIEBLA_ID, ownedBackgroundIds = owned))
        assertFalse(BrailuxBackgroundCatalog.canSelect(BrailuxBackgroundCatalog.SALVIA_TEXTURA_ID, ownedBackgroundIds = owned))
    }

    // 30. Default nunca entra en Billing entitlement.
    @Test
    fun test30_defaultNeverEntersBillingEntitlement() = runBlocking {
        val fakeDefaultPurchase = createPurchaseRecord(
            productId = "brailux_theme_default",
            purchaseToken = "tok_default",
        )

        assertFalse(entitlementRepository.isPurchaseEligible(fakeDefaultPurchase))
        val result = entitlementRepository.reconcileFromPurchases(listOf(fakeDefaultPurchase))

        assertFalse(result.contains(BrailuxBackgroundCatalog.DEFAULT_ID))
        assertFalse(entitlementRepository.isBackgroundOwned(BrailuxBackgroundCatalog.DEFAULT_ID))
        assertFalse(BrailuxPremiumAccess.currentState.ownedBackgroundIds.contains(BrailuxBackgroundCatalog.DEFAULT_ID))
    }
}
