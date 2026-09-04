package com.brailuxaprende.data.billing

import android.app.Activity
import com.brailuxaprende.data.settings.BrailuxBackgroundCatalog
import com.brailuxaprende.data.settings.BrailuxBackgroundOption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Estado de adquisición o disponibilidad comercial de un fondo en la interfaz.
 */
sealed interface BrailuxThemePurchaseStatus {
    /**
     * Fondo gratuito/predeterminado disponible para todos los usuarios.
     */
    data object Free : BrailuxThemePurchaseStatus

    /**
     * Tema Premium ya adquirido y con derecho (entitlement) confirmado.
     */
    data object Purchased : BrailuxThemePurchaseStatus

    /**
     * Compra en proceso de validación o pago diferido por Google Play.
     */
    data object Pending : BrailuxThemePurchaseStatus

    /**
     * Tema Premium disponible para compra con exactamente una oferta válida.
     */
    data class AvailableForPurchase(
        val formattedPrice: String,
        val offerToken: String,
    ) : BrailuxThemePurchaseStatus

    /**
     * Tema Premium no disponible para compra (sin ofertas, múltiples ofertas no soportadas en V1, o error).
     */
    data object Unavailable : BrailuxThemePurchaseStatus
}

/**
 * Estado derivado de un fondo individual para la capa de presentación.
 */
data class BrailuxThemeBillingItemState(
    val backgroundId: String,
    val productId: String?,
    val status: BrailuxThemePurchaseStatus,
)

/**
 * Estado general reactivo de facturación expuesto a la interfaz de usuario.
 */
data class BrailuxBillingUiState(
    val items: Map<String, BrailuxThemeBillingItemState> = emptyMap(),
    val isConnecting: Boolean = false,
    val isPurchasing: Boolean = false,
    val isRestoring: Boolean = false,
) {
    fun itemFor(backgroundId: String): BrailuxThemeBillingItemState? = items[backgroundId]
}

/**
 * Evento de feedback de la acción de restaurar compras.
 */
sealed interface BrailuxRestoreEvent {
    data object RestoreSuccess : BrailuxRestoreEvent
    data object RestoreEmpty : BrailuxRestoreEvent
    data object RestoreError : BrailuxRestoreEvent
}

/**
 * Modelo de presentación puro para desacoplar las reglas visuales de SettingsScreen.
 */
data class BrailuxThemePresentation(
    val showPremiumBadge: Boolean,
    val statusLabel: String?,
    val isSelectable: Boolean,
    val isPurchasable: Boolean,
    val canPreview: Boolean,
)

/**
 * Lógica pura de resolución de presentación y estados comerciales de temas.
 */
object BrailuxBillingResolver {

    /**
     * Resuelve el estado de un producto/fondo siguiendo la prioridad determinista de FASE E:
     * 1. DEFAULT -> Free
     * 2. Si backgroundId está en ownedBackgroundIds -> Purchased
     * 3. Si existe Purchase en estado PENDING -> Pending
     * 4. Si ofrece exactamente UNA oferta válida con offerToken y formattedPrice -> AvailableForPurchase
     * 5. En cualquier otro caso -> Unavailable
     */
    fun resolveThemePurchaseStatus(
        backgroundId: String,
        ownedBackgroundIds: Set<String>,
        purchases: Map<String, BrailuxPurchaseRecord>,
        products: Map<String, BrailuxBillingProductDetails>,
    ): BrailuxThemePurchaseStatus {
        if (backgroundId == BrailuxBackgroundCatalog.DEFAULT_ID) {
            return BrailuxThemePurchaseStatus.Free
        }
        if (backgroundId in ownedBackgroundIds) {
            return BrailuxThemePurchaseStatus.Purchased
        }
        val productId = BrailuxBillingProductCatalog.productIdFor(backgroundId)
            ?: return BrailuxThemePurchaseStatus.Unavailable

        val purchase = purchases[productId]
        if (purchase?.purchaseState is BrailuxPurchaseState.Pending) {
            return BrailuxThemePurchaseStatus.Pending
        }

        val productDetails = products[productId] ?: return BrailuxThemePurchaseStatus.Unavailable
        val offers = productDetails.oneTimeOffers
        if (offers.size == 1) {
            val singleOffer = offers.single()
            val token = singleOffer.offerToken
            val price = singleOffer.formattedPrice
            if (token.isNotBlank() && !price.isNullOrBlank()) {
                return BrailuxThemePurchaseStatus.AvailableForPurchase(
                    formattedPrice = price,
                    offerToken = token,
                )
            }
        }
        return BrailuxThemePurchaseStatus.Unavailable
    }

    fun resolveThemeItemState(
        backgroundId: String,
        ownedBackgroundIds: Set<String>,
        purchases: Map<String, BrailuxPurchaseRecord>,
        products: Map<String, BrailuxBillingProductDetails>,
    ): BrailuxThemeBillingItemState {
        val productId = BrailuxBillingProductCatalog.productIdFor(backgroundId)
        val status = resolveThemePurchaseStatus(
            backgroundId = backgroundId,
            ownedBackgroundIds = ownedBackgroundIds,
            purchases = purchases,
            products = products,
        )
        return BrailuxThemeBillingItemState(
            backgroundId = backgroundId,
            productId = productId,
            status = status,
        )
    }

    fun buildInitialItems(ownedBackgroundIds: Set<String> = emptySet()): Map<String, BrailuxThemeBillingItemState> {
        return BrailuxBackgroundCatalog.backgrounds.associate { background ->
            val backgroundId = background.id
            val itemState = resolveThemeItemState(
                backgroundId = backgroundId,
                ownedBackgroundIds = ownedBackgroundIds,
                purchases = emptyMap(),
                products = emptyMap(),
            )
            backgroundId to itemState
        }
    }
}

/**
 * Presentador puro para comprobación de reglas de interfaz sin dependencias de Android/Compose.
 */
object BrailuxBillingPresentation {
    fun resolvePresentation(
        background: BrailuxBackgroundOption,
        itemStatus: BrailuxThemePurchaseStatus,
    ): BrailuxThemePresentation {
        if (!background.premium || itemStatus is BrailuxThemePurchaseStatus.Free) {
            return BrailuxThemePresentation(
                showPremiumBadge = false,
                statusLabel = null,
                isSelectable = true,
                isPurchasable = false,
                canPreview = true,
            )
        }
        return when (itemStatus) {
            is BrailuxThemePurchaseStatus.Purchased -> BrailuxThemePresentation(
                showPremiumBadge = true,
                statusLabel = "Comprado",
                isSelectable = true,
                isPurchasable = false,
                canPreview = true,
            )
            is BrailuxThemePurchaseStatus.Pending -> BrailuxThemePresentation(
                showPremiumBadge = true,
                statusLabel = "Pendiente",
                isSelectable = false,
                isPurchasable = false,
                canPreview = true,
            )
            is BrailuxThemePurchaseStatus.AvailableForPurchase -> BrailuxThemePresentation(
                showPremiumBadge = true,
                statusLabel = itemStatus.formattedPrice,
                isSelectable = false,
                isPurchasable = true,
                canPreview = true,
            )
            is BrailuxThemePurchaseStatus.Unavailable -> BrailuxThemePresentation(
                showPremiumBadge = true,
                statusLabel = "No disponible",
                isSelectable = false,
                isPurchasable = false,
                canPreview = true,
            )
            is BrailuxThemePurchaseStatus.Free -> BrailuxThemePresentation(
                showPremiumBadge = false,
                statusLabel = null,
                isSelectable = true,
                isPurchasable = false,
                canPreview = true,
            )
        }
    }
}

/**
 * Coordinador de facturación y ciclo de vida para Google Play Billing.
 *
 * Principios y Restricciones de Arquitectura:
 * - Desacoplado completamente de clases y componentes Compose.
 * - Trabaja contra la abstracción [BrailuxBillingRepository] para garantizar testabilidad.
 * - Comparte la misma instancia de [BrailuxPremiumEntitlementRepository] creada en MainActivity.
 * - Idempotente: múltiples invocaciones a [initialize] no duplican recolectores ni tareas.
 * - Valida productId y offerToken antes de despachar el flujo de compra a BillingClient.
 * - Garantiza como máximo una compra en progreso concurrente para evitar pulsaciones dobles.
 * - La destrucción ([destroy]) cancela únicamente sus recolectores y cierra la conexión de facturación
 *   sin cancelar el lifecycleScope externo administrado por Android.
 */
class BrailuxBillingCoordinator(
    private val billingRepository: BrailuxBillingRepository,
    val entitlementRepository: BrailuxPremiumEntitlementRepository,
    coroutineScope: CoroutineScope? = null,
) {
    private val externalScope: CoroutineScope =
        coroutineScope ?: CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val coordinatorJob = SupervisorJob(externalScope.coroutineContext[Job])
    private val scope = CoroutineScope(externalScope.coroutineContext + coordinatorJob)

    private val initMutex = Mutex()
    private var isInitialized = false

    private val purchaseMutex = Mutex()
    private val _isPurchasing = MutableStateFlow(false)
    private val _isRestoring = MutableStateFlow(false)
    private val _isConnecting = MutableStateFlow(false)

    private val _uiState = MutableStateFlow(
        BrailuxBillingUiState(
            items = BrailuxBillingResolver.buildInitialItems(
                entitlementRepository.ownedBackgroundIds.value,
            ),
        ),
    )
    val uiState: StateFlow<BrailuxBillingUiState> = _uiState.asStateFlow()

    private val _restoreEvents = MutableSharedFlow<BrailuxRestoreEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val restoreEvents: SharedFlow<BrailuxRestoreEvent> = _restoreEvents.asSharedFlow()

    private val observerJobs = mutableListOf<Job>()

    suspend fun initialize(): Unit = initMutex.withLock {
        if (isInitialized) return@withLock
        isInitialized = true

        // 1. Cargar cache de entitlement para permitir arranque offline
        try {
            entitlementRepository.loadCachedEntitlements()
        } catch (_: Throwable) {
            // No bloquear la app ni conceder derechos artificiales
        }

        // 2. Publicar estado local
        rebuildUiState()

        // 3. Iniciar observadores reactivos de flujos
        startObservers()

        // 4. Iniciar y sincronizar Google Play de forma asíncrona en segundo plano
        scope.launch {
            _isConnecting.value = true
            rebuildUiState()
            try {
                billingRepository.startConnection()
                billingRepository.syncBillingData()
            } catch (_: Throwable) {
                // Si sync falla, no borrar derechos del cache ni bloquear la app
            } finally {
                _isConnecting.value = false
                rebuildUiState()
            }
        }
        Unit
    }

    private fun startObservers() {
        observerJobs += scope.launch {
            entitlementRepository.ownedBackgroundIds.collect {
                _isPurchasing.value = false
                rebuildUiState()
            }
        }

        observerJobs += scope.launch {
            billingRepository.products.collect {
                rebuildUiState()
            }
        }

        observerJobs += scope.launch {
            billingRepository.purchases.collect {
                _isPurchasing.value = false
                rebuildUiState()
            }
        }

        observerJobs += scope.launch {
            billingRepository.lastBillingOperation.collect { operation ->
                when (operation) {
                    is BrailuxBillingOperationState.UserCanceled,
                    is BrailuxBillingOperationState.Error,
                    is BrailuxBillingOperationState.Success -> {
                        _isPurchasing.value = false
                        rebuildUiState()
                    }
                    else -> { /* Mantener isPurchasing durante lanzamiento / flujo activo */ }
                }
            }
        }

        observerJobs += scope.launch {
            billingRepository.lastBillingError.collect { error ->
                if (error != null) {
                    _isPurchasing.value = false
                    rebuildUiState()
                }
            }
        }
    }

    fun rebuildUiState() {
        val owned = entitlementRepository.ownedBackgroundIds.value
        val currentPurchases = billingRepository.purchases.value
        val currentProducts = billingRepository.products.value

        val newItems = BrailuxBackgroundCatalog.backgrounds.associate { background ->
            val itemState = BrailuxBillingResolver.resolveThemeItemState(
                backgroundId = background.id,
                ownedBackgroundIds = owned,
                purchases = currentPurchases,
                products = currentProducts,
            )
            background.id to itemState
        }

        _uiState.value = BrailuxBillingUiState(
            items = newItems,
            isConnecting = _isConnecting.value,
            isPurchasing = _isPurchasing.value,
            isRestoring = _isRestoring.value,
        )
    }

    suspend fun launchPurchase(
        activity: Activity,
        productId: String,
        offerToken: String,
    ): Result<Unit> = purchaseMutex.withLock {
        if (_isPurchasing.value) {
            return Result.failure(IllegalStateException("A purchase is already in progress"))
        }

        val backgroundId = BrailuxBillingProductCatalog.backgroundIdFor(productId)
            ?: return Result.failure(IllegalArgumentException("Unknown product $productId"))

        val currentItem = _uiState.value.items[backgroundId]
            ?: return Result.failure(IllegalArgumentException("Item not found for product $productId"))

        val status = currentItem.status
        if (status !is BrailuxThemePurchaseStatus.AvailableForPurchase) {
            return Result.failure(IllegalStateException("Item $productId is not available for purchase"))
        }

        if (currentItem.productId != productId || status.offerToken != offerToken) {
            return Result.failure(IllegalArgumentException("Product or offerToken does not match current state"))
        }

        _isPurchasing.value = true
        rebuildUiState()

        val result = billingRepository.launchPurchaseFlow(activity, productId, offerToken)
        if (result.isFailure) {
            _isPurchasing.value = false
            rebuildUiState()
            return result
        }
        return result
    }

    suspend fun restorePurchases() {
        if (_isRestoring.value) return
        _isRestoring.value = true
        rebuildUiState()
        try {
            val result = billingRepository.restorePurchases()
            if (result.isSuccess) {
                val records = result.getOrThrow()
                val hasEligiblePurchased = records.any {
                    it.purchaseState is BrailuxPurchaseState.Purchased &&
                        entitlementRepository.isPurchaseEligible(it)
                }
                if (hasEligiblePurchased) {
                    _restoreEvents.emit(BrailuxRestoreEvent.RestoreSuccess)
                } else {
                    _restoreEvents.emit(BrailuxRestoreEvent.RestoreEmpty)
                }
            } else {
                _restoreEvents.emit(BrailuxRestoreEvent.RestoreError)
            }
        } catch (_: Throwable) {
            _restoreEvents.emit(BrailuxRestoreEvent.RestoreError)
        } finally {
            _isRestoring.value = false
            rebuildUiState()
        }
    }

    fun destroy() {
        observerJobs.forEach { it.cancel() }
        observerJobs.clear()
        coordinatorJob.cancelChildren()
        billingRepository.endConnection()
        isInitialized = false
        _isPurchasing.value = false
        _isRestoring.value = false
        _isConnecting.value = false
    }
}
