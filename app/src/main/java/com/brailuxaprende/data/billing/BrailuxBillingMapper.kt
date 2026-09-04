package com.brailuxaprende.data.billing

import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.UnfetchedProduct

/**
 * Mapeador desacoplado y seguro entre los objetos del SDK de Google Play Billing y los
 * modelos de dominio de Brailux.
 *
 * Principios:
 * - Valida estrictamente que los productIds correspondan a [BrailuxBillingProductCatalog].
 * - Ignora de forma segura cualquier producto desconocido.
 * - Mapea [Purchase.PurchaseState.PURCHASED] a [BrailuxPurchaseState.Purchased].
 * - Mapea [Purchase.PurchaseState.PENDING] a [BrailuxPurchaseState.Pending].
 * - Una compra PENDING jamás se considera adquirida.
 * - Maneja compras con múltiples productIds de forma individual y segura.
 * - Mapea [UnfetchedProduct] a [BrailuxProductState.Unavailable] sin inventar precios ni usar precios hardcodeados.
 */
object BrailuxBillingMapper {

    /**
     * Mapea un [ProductDetails] del SDK a un [BrailuxBillingProductDetails].
     * Si el producto no pertenece al catálogo oficial, retorna null para no contaminar el dominio.
     */
    fun mapProductDetails(productDetails: ProductDetails): BrailuxBillingProductDetails? {
        val productId = productDetails.productId
        if (productId !in BrailuxBillingProductCatalog.allProductIds) {
            return null
        }

        val offer = productDetails.oneTimePurchaseOfferDetails
        return mapProductDetailsData(
            productId = productId,
            name = productDetails.name,
            description = productDetails.description,
            formattedPrice = offer?.formattedPrice,
            priceAmountMicros = offer?.priceAmountMicros,
            priceCurrencyCode = offer?.priceCurrencyCode,
        )
    }

    /**
     * Mapea datos puros de un producto consultado exitosamente a [BrailuxBillingProductDetails].
     */
    fun mapProductDetailsData(
        productId: String,
        name: String,
        description: String,
        formattedPrice: String?,
        priceAmountMicros: Long?,
        priceCurrencyCode: String?,
    ): BrailuxBillingProductDetails? {
        if (productId !in BrailuxBillingProductCatalog.allProductIds) {
            return null
        }

        return BrailuxBillingProductDetails(
            productId = productId,
            productType = BrailuxProductType.OneTime,
            name = name,
            description = description,
            formattedPrice = formattedPrice,
            priceAmountMicros = priceAmountMicros,
            priceCurrencyCode = priceCurrencyCode,
            isConsumable = false,
            state = BrailuxProductState.Available,
            unfetchedStatusCode = null,
        )
    }

    /**
     * Mapea un [UnfetchedProduct] del SDK a un [BrailuxBillingProductDetails] en estado Unavailable.
     * No inventa precio ni utiliza precios hardcodeados.
     */
    fun mapUnfetchedProduct(unfetchedProduct: UnfetchedProduct): BrailuxBillingProductDetails? {
        val productId = unfetchedProduct.productId
        if (productId !in BrailuxBillingProductCatalog.allProductIds) {
            return null
        }

        return mapUnfetchedProductData(
            productId = productId,
            statusCode = unfetchedProduct.statusCode,
        )
    }

    /**
     * Mapea datos puros de un producto no recuperado a [BrailuxBillingProductDetails] en estado Unavailable.
     */
    fun mapUnfetchedProductData(
        productId: String,
        statusCode: Int,
    ): BrailuxBillingProductDetails? {
        if (productId !in BrailuxBillingProductCatalog.allProductIds) {
            return null
        }

        return BrailuxBillingProductDetails(
            productId = productId,
            productType = BrailuxProductType.OneTime,
            name = "",
            description = "",
            formattedPrice = null,
            priceAmountMicros = null,
            priceCurrencyCode = null,
            isConsumable = false,
            state = BrailuxProductState.Unavailable,
            unfetchedStatusCode = statusCode,
        )
    }

    /**
     * Mapea un [Purchase] del SDK a una lista de [BrailuxPurchaseRecord].
     * Una Purchase puede contener múltiples product IDs; cada uno es evaluado contra el catálogo.
     * Los productos desconocidos se descartan de forma segura.
     */
    fun mapPurchase(purchase: Purchase): List<BrailuxPurchaseRecord> {
        return mapPurchaseData(
            productIds = purchase.products,
            purchaseToken = purchase.purchaseToken,
            purchaseTimeMillis = purchase.purchaseTime,
            purchaseStateCode = purchase.purchaseState,
            isAcknowledged = purchase.isAcknowledged,
        )
    }

    /**
     * Mapea datos puros de compra a registros de dominio [BrailuxPurchaseRecord].
     */
    fun mapPurchaseData(
        productIds: List<String>,
        purchaseToken: String,
        purchaseTimeMillis: Long,
        purchaseStateCode: Int,
        isAcknowledged: Boolean,
    ): List<BrailuxPurchaseRecord> {
        val validProductIds = productIds.filter { it in BrailuxBillingProductCatalog.allProductIds }
        if (validProductIds.isEmpty()) {
            return emptyList()
        }

        val domainPurchaseState = when (purchaseStateCode) {
            Purchase.PurchaseState.PURCHASED -> BrailuxPurchaseState.Purchased
            Purchase.PurchaseState.PENDING -> BrailuxPurchaseState.Pending
            else -> BrailuxPurchaseState.NotOwned
        }

        return validProductIds.map { productId ->
            BrailuxPurchaseRecord(
                productId = productId,
                purchaseToken = purchaseToken,
                purchaseTimeMillis = purchaseTimeMillis,
                purchaseState = domainPurchaseState,
                isAcknowledged = isAcknowledged,
            )
        }
    }
}
