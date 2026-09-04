package com.brailuxaprende.data.billing

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.brailuxaprende.data.settings.BrailuxBackgroundCatalog
import com.brailuxaprende.data.settings.BrailuxPremiumAccess
import com.brailuxaprende.data.settings.BrailuxPremiumState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException

internal const val OwnedBackgroundIdsCacheKeyName = "owned_background_ids_cache"
internal val OwnedBackgroundIdsCacheKey = stringSetPreferencesKey(OwnedBackgroundIdsCacheKeyName)

/**
 * Repositorio de reconciliación de derechos Premium (entitlements) de Brailux.
 *
 * Principio de Fuente de Verdad y Arquitectura Client-Only:
 * - Google Play Billing es la única fuente autoritativa de propiedad.
 * - El almacenamiento en DataStore ([OwnedBackgroundIdsCacheKey]) es ESTRICTAMENTE un cache
 *   auxiliar del último estado conocido confirmado para soportar arranques sin conexión (offline).
 *   NO es comprobante definitivo de compra.
 * - Esta Fase D opera en modalidad client-only: las validaciones de [productId], [purchaseState]
 *   y [purchaseToken] corresponden a reglas de validación y eligibilidad local en el dispositivo.
 *   Hardening posterior mediante backend, Google Play Developer API o Voided Purchases API
 *   queda reservado para fases de backend/seguridad avanzadas.
 * - Reutiliza exclusivamente la instancia existente de DataStore provista (no crea otro archivo
 *   DataStore ni otra instancia de [preferencesDataStore]).
 *
 * Concurrencia:
 * - Emplea un [Mutex] interno para serializar de forma determinista la reconciliación,
 *   la actualización en memoria y la persistencia en el cache auxiliar.
 */
class BrailuxPremiumEntitlementRepository(
    private val dataStore: DataStore<Preferences>? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val mutex = Mutex()

    private val _ownedBackgroundIds = MutableStateFlow<Set<String>>(emptySet())
    val ownedBackgroundIds: StateFlow<Set<String>> = _ownedBackgroundIds.asStateFlow()

    private val _premiumState = MutableStateFlow(BrailuxPremiumAccess.currentState)
    val premiumState: StateFlow<BrailuxPremiumState> = _premiumState.asStateFlow()

    /**
     * Evalúa si un registro de compra cumple las reglas de validación y eligibilidad local:
     * 1. [productId] pertenece al catálogo oficial de temas Premium de Brailux.
     * 2. Existe un mapeo oficial a un [backgroundId] válido (y distinto del fondo 'default').
     * 3. [purchaseState] es exactamente [BrailuxPurchaseState.Purchased].
     * 4. [purchaseToken] no está vacío ni en blanco.
     * 5. El producto en catálogo es de tipo [BrailuxProductType.OneTime] y no consumible.
     */
    fun isPurchaseEligible(purchase: BrailuxPurchaseRecord): Boolean {
        val productId = purchase.productId
        if (productId !in BrailuxBillingProductCatalog.allProductIds) {
            return false
        }

        val backgroundId = BrailuxBillingProductCatalog.backgroundIdFor(productId)
        if (backgroundId.isNullOrBlank() || backgroundId == BrailuxBackgroundCatalog.DEFAULT_ID) {
            return false
        }

        if (purchase.purchaseState !is BrailuxPurchaseState.Purchased) {
            return false
        }

        if (purchase.purchaseToken.isBlank()) {
            return false
        }

        val productItem = BrailuxBillingProductCatalog.findProductByProductId(productId) ?: return false
        if (productItem.productType != BrailuxProductType.OneTime || productItem.isConsumable) {
            return false
        }

        return true
    }

    /**
     * Mapea de forma segura un registro elegible a su backgroundId.
     * Retorna null si la compra no es elegible.
     */
    fun eligibleBackgroundId(purchase: BrailuxPurchaseRecord): String? {
        if (!isPurchaseEligible(purchase)) return null
        return BrailuxBillingProductCatalog.backgroundIdFor(purchase.productId)
    }

    /**
     * Reconciliación autoritativa ante una consulta exitosa de Google Play Billing ([purchases]).
     *
     * Reglas:
     * - Reemplaza autoritativamente el conjunto activo local con las compras actualmente válidas.
     * - Si una compra anteriormente presente deja de informarse (reembolso, revocación, cambio de cuenta),
     *   su entitlement se retira en esta arquitectura client-only.
     * - Una lista vacía de compras (ej: nueva cuenta o todos reembolsados) produce un conjunto vacío.
     * - Compras duplicadas en la lista son filtradas por el Set sin duplicar derechos.
     * - Actualiza [BrailuxPremiumAccess] y persiste el nuevo conjunto en el cache auxiliar DataStore.
     */
    suspend fun reconcileFromPurchases(purchases: Collection<BrailuxPurchaseRecord>): Set<String> = mutex.withLock {
        val resolvedIds = purchases
            .filter { isPurchaseEligible(it) }
            .mapNotNull { BrailuxBillingProductCatalog.backgroundIdFor(it.productId) }
            .filter { it != BrailuxBackgroundCatalog.DEFAULT_ID }
            .toSet()

        applyEntitlementsLocked(resolvedIds)
        persistCacheLocked(resolvedIds)
        return resolvedIds
    }

    /**
     * Concede el derecho correspondiente a una compra individual válida (típicamente recibida
     * mediante [PurchasesUpdatedListener]).
     *
     * Si la compra es [BrailuxPurchaseState.Pending], de producto desconocido o con token inválido,
     * la rechaza y no modifica los derechos.
     */
    suspend fun grantEntitlementForPurchase(purchase: BrailuxPurchaseRecord): Boolean = mutex.withLock {
        val backgroundId = eligibleBackgroundId(purchase) ?: return false

        val updated = _ownedBackgroundIds.value + backgroundId
        applyEntitlementsLocked(updated)
        persistCacheLocked(updated)
        return true
    }

    /**
     * Carga el último cache confirmado desde DataStore para permitir el arranque offline.
     *
     * Sanitiza los IDs leídos contra [BrailuxBillingProductCatalog.allBackgroundIds] para
     * prevenir corrupción o IDs arbitrarios (como 'default').
     * No inventa nuevas compras ni concede derechos no confirmados previamente.
     */
    suspend fun loadCachedEntitlements(): Set<String> = mutex.withLock {
        val cached = readCacheFromDataStore()
        val sanitized = cached.filter { id ->
            id in BrailuxBillingProductCatalog.allBackgroundIds && id != BrailuxBackgroundCatalog.DEFAULT_ID
        }.toSet()

        applyEntitlementsLocked(sanitized)
        return sanitized
    }

    /**
     * Limpia todos los derechos y vacía el cache auxiliar.
     */
    suspend fun clearEntitlements(): Unit = mutex.withLock {
        applyEntitlementsLocked(emptySet())
        persistCacheLocked(emptySet())
    }

    fun isBackgroundOwned(backgroundId: String): Boolean =
        _ownedBackgroundIds.value.contains(backgroundId)

    private fun applyEntitlementsLocked(backgroundIds: Set<String>) {
        _ownedBackgroundIds.value = backgroundIds
        val newState = BrailuxPremiumAccess.resolveState(
            isPremiumUnlocked = BrailuxPremiumAccess.currentState.isPremiumUnlocked,
            ownedBackgroundIds = backgroundIds,
        )
        _premiumState.value = newState
        BrailuxPremiumAccess.updateOwnedBackgroundIds(backgroundIds)
    }

    private suspend fun persistCacheLocked(backgroundIds: Set<String>) {
        val ds = dataStore ?: return
        try {
            withContext(ioDispatcher) {
                ds.edit { preferences ->
                    if (backgroundIds.isEmpty()) {
                        preferences.remove(OwnedBackgroundIdsCacheKey)
                    } else {
                        preferences[OwnedBackgroundIdsCacheKey] = backgroundIds
                    }
                }
            }
        } catch (_: IOException) {
            // Falla de I/O en persistencia no debe tumbar el estado en memoria
        }
    }

    private suspend fun readCacheFromDataStore(): Set<String> {
        val ds = dataStore ?: return emptySet()
        return try {
            withContext(ioDispatcher) {
                val prefs = ds.data.first()
                prefs[OwnedBackgroundIdsCacheKey] ?: emptySet()
            }
        } catch (_: IOException) {
            emptySet()
        }
    }
}
