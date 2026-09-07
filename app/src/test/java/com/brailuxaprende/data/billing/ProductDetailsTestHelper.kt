package com.brailuxaprende.data.billing

import com.android.billingclient.api.ProductDetails
import sun.misc.Unsafe

/**
 * Helper de pruebas para instanciar [ProductDetails] exclusivamente dentro del conjunto
 * de pruebas unitarias (src/test).
 *
 * Asigna campos directamente en memoria sin invocar el constructor interno de Android SDK
 * que depende de org.json.JSONObject (evitando RuntimeException: Method not mocked).
 *
 * Mapeo de campos en ProductDetails (Play Billing 9.1.0):
 * - zzc: productId
 * - zzd: productType ("inapp")
 * - zze: title
 * - zzf: name
 * - zzg: description
 * - zzk: oneTimePurchaseOfferDetailsList
 *
 * El código de producción NO referencia ni depende de esta utilidad.
 */
object ProductDetailsTestHelper {

    private val unsafe: Unsafe by lazy {
        val field = Unsafe::class.java.getDeclaredField("theUnsafe")
        field.isAccessible = true
        field.get(null) as Unsafe
    }

    fun createOneTimeProductDetails(
        productId: String,
        name: String = "Test Product",
        description: String = "Test Description",
        offers: List<TestOfferDetails> = emptyList(),
    ): ProductDetails {
        val productDetails = unsafe.allocateInstance(ProductDetails::class.java) as ProductDetails

        val zzaField = ProductDetails::class.java.getDeclaredField("zza").apply { isAccessible = true }
        zzaField.set(productDetails, "com.brailuxaprende")

        val zzcField = ProductDetails::class.java.getDeclaredField("zzc").apply { isAccessible = true }
        zzcField.set(productDetails, productId)

        val zzdField = ProductDetails::class.java.getDeclaredField("zzd").apply { isAccessible = true }
        zzdField.set(productDetails, "inapp")

        val zzeField = ProductDetails::class.java.getDeclaredField("zze").apply { isAccessible = true }
        zzeField.set(productDetails, name)

        val zzfField = ProductDetails::class.java.getDeclaredField("zzf").apply { isAccessible = true }
        zzfField.set(productDetails, name)

        val zzgField = ProductDetails::class.java.getDeclaredField("zzg").apply { isAccessible = true }
        zzgField.set(productDetails, description)

        val oneTimeOffersList = offers.map { offer ->
            createOneTimeOfferDetails(offer)
        }

        val zzkField = ProductDetails::class.java.getDeclaredField("zzk").apply { isAccessible = true }
        zzkField.set(productDetails, oneTimeOffersList)

        return productDetails
    }

    fun createOneTimeOfferDetails(offer: TestOfferDetails): ProductDetails.OneTimePurchaseOfferDetails {
        val offerClass = ProductDetails.OneTimePurchaseOfferDetails::class.java
        val offerInstance = unsafe.allocateInstance(offerClass) as ProductDetails.OneTimePurchaseOfferDetails

        val zzaField = offerClass.getDeclaredField("zza").apply { isAccessible = true }
        zzaField.set(offerInstance, offer.formattedPrice ?: "$1.99")

        val zzbField = offerClass.getDeclaredField("zzb").apply { isAccessible = true }
        zzbField.set(offerInstance, offer.priceAmountMicros ?: 1990000L)

        val zzcField = offerClass.getDeclaredField("zzc").apply { isAccessible = true }
        zzcField.set(offerInstance, offer.priceCurrencyCode ?: "USD")

        val zzdField = offerClass.getDeclaredField("zzd").apply { isAccessible = true }
        zzdField.set(offerInstance, offer.offerToken)

        val zzeField = offerClass.getDeclaredField("zze").apply { isAccessible = true }
        zzeField.set(offerInstance, offer.offerId)

        val zzfField = offerClass.getDeclaredField("zzf").apply { isAccessible = true }
        zzfField.set(offerInstance, offer.purchaseOptionId)

        return offerInstance
    }
}

data class TestOfferDetails(
    val offerToken: String,
    val formattedPrice: String? = "$1.99",
    val priceAmountMicros: Long? = 1990000L,
    val priceCurrencyCode: String? = "USD",
    val purchaseOptionId: String? = null,
    val offerId: String? = null,
)
