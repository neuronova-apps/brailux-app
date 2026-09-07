package com.brailuxaprende.data.billing

import com.brailuxaprende.data.settings.BrailuxBackgroundCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BrailuxBillingProductCatalogTest {

    @Test
    fun exactlyFourProductsDefinedInCatalog() {
        assertEquals(
            "El catalogo de Google Play Billing debe contener exactamente cuatro productos",
            4,
            BrailuxBillingProductCatalog.products.size,
        )
    }

    @Test
    fun allProductIdsAreUnique() {
        val productIds = BrailuxBillingProductCatalog.allProductIds
        assertEquals(
            "Cada productId debe ser unico",
            productIds.size,
            productIds.distinct().size,
        )
    }

    @Test
    fun allBackgroundIdsAreUnique() {
        val backgroundIds = BrailuxBillingProductCatalog.allBackgroundIds
        assertEquals(
            "Cada backgroundId debe ser unico",
            backgroundIds.size,
            backgroundIds.distinct().size,
        )
    }

    @Test
    fun noProductUsesDefaultBackground() {
        val usesDefault = BrailuxBillingProductCatalog.products.any {
            it.backgroundId == BrailuxBackgroundCatalog.DEFAULT_ID
        }
        assertFalse(
            "Ningun producto de Billing debe asociarse al fondo default",
            usesDefault,
        )
    }

    @Test
    fun eachProductIdResolvesToCorrectBackgroundId() {
        assertEquals(
            BrailuxBackgroundCatalog.CELESTE_GEOMETRICO_ID,
            BrailuxBillingProductCatalog.backgroundIdFor(BrailuxBillingProductCatalog.PRODUCT_ID_CELESTE_GEOMETRICO),
        )
        assertEquals(
            BrailuxBackgroundCatalog.CREMA_ONDAS_ID,
            BrailuxBillingProductCatalog.backgroundIdFor(BrailuxBillingProductCatalog.PRODUCT_ID_CREMA_ONDAS),
        )
        assertEquals(
            BrailuxBackgroundCatalog.LAVANDA_NIEBLA_ID,
            BrailuxBillingProductCatalog.backgroundIdFor(BrailuxBillingProductCatalog.PRODUCT_ID_LAVANDA_NIEBLA),
        )
        assertEquals(
            BrailuxBackgroundCatalog.SALVIA_TEXTURA_ID,
            BrailuxBillingProductCatalog.backgroundIdFor(BrailuxBillingProductCatalog.PRODUCT_ID_SALVIA_TEXTURA),
        )
    }

    @Test
    fun eachBackgroundIdResolvesToCorrectProductId() {
        assertEquals(
            BrailuxBillingProductCatalog.PRODUCT_ID_CELESTE_GEOMETRICO,
            BrailuxBillingProductCatalog.productIdFor(BrailuxBackgroundCatalog.CELESTE_GEOMETRICO_ID),
        )
        assertEquals(
            BrailuxBillingProductCatalog.PRODUCT_ID_CREMA_ONDAS,
            BrailuxBillingProductCatalog.productIdFor(BrailuxBackgroundCatalog.CREMA_ONDAS_ID),
        )
        assertEquals(
            BrailuxBillingProductCatalog.PRODUCT_ID_LAVANDA_NIEBLA,
            BrailuxBillingProductCatalog.productIdFor(BrailuxBackgroundCatalog.LAVANDA_NIEBLA_ID),
        )
        assertEquals(
            BrailuxBillingProductCatalog.PRODUCT_ID_SALVIA_TEXTURA,
            BrailuxBillingProductCatalog.productIdFor(BrailuxBackgroundCatalog.SALVIA_TEXTURA_ID),
        )
    }

    @Test
    fun allProductsAreNonConsumable() {
        assertTrue(
            "Todos los temas Premium deben ser no consumibles",
            BrailuxBillingProductCatalog.products.all { !it.isConsumable },
        )
    }

    @Test
    fun noProductIsSubscription() {
        assertTrue(
            "Todos los productos deben ser OneTime (compras unicas)",
            BrailuxBillingProductCatalog.products.all { it.productType == BrailuxProductType.OneTime },
        )
        assertFalse(
            "Ningun producto debe ser suscripcion",
            BrailuxBillingProductCatalog.products.any { it.productType == BrailuxProductType.Subscription },
        )
    }

    @Test
    fun noEmptyOrBlankIdsExist() {
        assertTrue(
            "Ningun productId ni backgroundId debe ser vacio o estar en blanco",
            BrailuxBillingProductCatalog.products.all {
                it.productId.isNotBlank() && it.backgroundId.isNotBlank()
            },
        )
    }

    @Test
    fun allBackgroundIdsMapToExistingCatalogBackgrounds() {
        val existingBackgroundIds = BrailuxBackgroundCatalog.backgrounds.map { it.id }.toSet()
        assertTrue(
            "Todos los backgroundId deben existir en BrailuxBackgroundCatalog",
            BrailuxBillingProductCatalog.allBackgroundIds.all { it in existingBackgroundIds },
        )
    }

    @Test
    fun backgroundIdsCorrespondExactlyToTheFourPremiumThemes() {
        val existingPremiumIds = BrailuxBackgroundCatalog.backgrounds
            .filter { it.premium }
            .map { it.id }
            .toSet()

        assertEquals(
            "Los backgroundIds deben coincidir exactamente con los 4 temas Premium",
            existingPremiumIds,
            BrailuxBillingProductCatalog.allBackgroundIds.toSet(),
        )
    }

    @Test
    fun defaultBackgroundRemainsFreeAndOutsideBilling() {
        val defaultOption = BrailuxBackgroundCatalog.option(BrailuxBackgroundCatalog.DEFAULT_ID)
        assertNotNull(defaultOption)
        assertFalse("El fondo default no debe ser premium", defaultOption?.premium == true)

        assertNull(
            "El fondo default no debe resolver a ningun productId",
            BrailuxBillingProductCatalog.productIdFor(BrailuxBackgroundCatalog.DEFAULT_ID),
        )
        assertFalse(
            "isBillingTheme debe ser false para default",
            BrailuxBillingProductCatalog.isBillingTheme(BrailuxBackgroundCatalog.DEFAULT_ID),
        )
    }

    @Test
    fun findProductMethodsHandleNullAndInvalidInputsSafely() {
        assertNull(BrailuxBillingProductCatalog.findProductByProductId(null))
        assertNull(BrailuxBillingProductCatalog.findProductByProductId(""))
        assertNull(BrailuxBillingProductCatalog.findProductByProductId("   "))
        assertNull(BrailuxBillingProductCatalog.findProductByProductId("non_existent_product"))

        assertNull(BrailuxBillingProductCatalog.findProductByBackgroundId(null))
        assertNull(BrailuxBillingProductCatalog.findProductByBackgroundId(""))
        assertNull(BrailuxBillingProductCatalog.findProductByBackgroundId("   "))
        assertNull(BrailuxBillingProductCatalog.findProductByBackgroundId("non_existent_background"))

        assertNull(BrailuxBillingProductCatalog.backgroundIdFor(null))
        assertNull(BrailuxBillingProductCatalog.backgroundIdFor("unknown_id"))

        assertNull(BrailuxBillingProductCatalog.productIdFor(null))
        assertNull(BrailuxBillingProductCatalog.productIdFor("unknown_id"))

        assertFalse(BrailuxBillingProductCatalog.isBillingTheme(null))
        assertFalse(BrailuxBillingProductCatalog.isBillingTheme("unknown_id"))
    }

    @Test
    fun isBillingThemeReturnsTrueForConfiguredThemes() {
        for (bgId in BrailuxBillingProductCatalog.allBackgroundIds) {
            assertTrue(
                "isBillingTheme debe ser true para $bgId",
                BrailuxBillingProductCatalog.isBillingTheme(bgId),
            )
        }
    }
}
