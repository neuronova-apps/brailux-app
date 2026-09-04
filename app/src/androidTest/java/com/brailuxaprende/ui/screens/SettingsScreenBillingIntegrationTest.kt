package com.brailuxaprende.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasAnySibling
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.brailuxaprende.R
import com.brailuxaprende.data.billing.BrailuxBillingProductCatalog
import com.brailuxaprende.data.billing.BrailuxBillingUiState
import com.brailuxaprende.data.billing.BrailuxThemeBillingItemState
import com.brailuxaprende.data.billing.BrailuxThemePurchaseStatus
import com.brailuxaprende.data.settings.AccessibilityPreferences
import com.brailuxaprende.data.settings.BrailuxBackgroundCatalog
import com.brailuxaprende.ui.theme.BrailuxAprendeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenBillingIntegrationTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun availableForPurchaseThemeShowsFormattedPriceAndDispatchesBuy() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        var boughtProductId: String? = null
        var boughtOfferToken: String? = null

        val samplePrice = "USD 0.99"
        val sampleToken = "token_celeste_promo"
        val targetBackgroundId = BrailuxBackgroundCatalog.CELESTE_GEOMETRICO_ID
        val expectedProductId = requireNotNull(
            BrailuxBillingProductCatalog.productIdFor(targetBackgroundId),
        )

        val billingState = BrailuxBillingUiState(
            items = mapOf(
                targetBackgroundId to BrailuxThemeBillingItemState(
                    backgroundId = targetBackgroundId,
                    productId = expectedProductId,
                    status = BrailuxThemePurchaseStatus.AvailableForPurchase(
                        formattedPrice = samplePrice,
                        offerToken = sampleToken,
                    ),
                ),
            ),
        )

        composeRule.setContent {
            BrailuxAprendeTheme {
                SettingsScreen(
                    preferences = AccessibilityPreferences(),
                    onSoundEnabledChange = {},
                    onVibrationEnabledChange = {},
                    onHighContrastEnabledChange = {},
                    onTextSizeChange = {},
                    onAppearanceChange = {},
                    onSeasonalThemesEnabledChange = {},
                    isPremiumUnlocked = false,
                    ownedBackgroundIds = emptySet(),
                    onBackgroundChange = {},
                    onAbout = {},
                    onBack = {},
                    billingUiState = billingState,
                    onBuyProduct = { pid, token ->
                        boughtProductId = pid
                        boughtOfferToken = token
                    },
                )
            }
        }

        val targetOption = requireNotNull(BrailuxBackgroundCatalog.option(targetBackgroundId))
        val targetBackgroundName = context.getString(targetOption.nameResource)
        composeRule.onNodeWithText(targetBackgroundName)
            .performScrollTo()
            .assertIsDisplayed()

        val buyButtonText = context.getString(R.string.settings_buy_with_price, samplePrice)
        composeRule.onNodeWithText(buyButtonText)
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        assertEquals(expectedProductId, boughtProductId)
        assertEquals(sampleToken, boughtOfferToken)
    }

    @Test
    fun purchasedThemeShowsPurchasedTagAndNoBuyButton() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val targetBackgroundId = BrailuxBackgroundCatalog.CREMA_ONDAS_ID
        val expectedProductId = requireNotNull(
            BrailuxBillingProductCatalog.productIdFor(targetBackgroundId),
        )

        val billingState = BrailuxBillingUiState(
            items = mapOf(
                targetBackgroundId to BrailuxThemeBillingItemState(
                    backgroundId = targetBackgroundId,
                    productId = expectedProductId,
                    status = BrailuxThemePurchaseStatus.Purchased,
                ),
            ),
        )

        composeRule.setContent {
            BrailuxAprendeTheme {
                SettingsScreen(
                    preferences = AccessibilityPreferences(),
                    onSoundEnabledChange = {},
                    onVibrationEnabledChange = {},
                    onHighContrastEnabledChange = {},
                    onTextSizeChange = {},
                    onAppearanceChange = {},
                    onSeasonalThemesEnabledChange = {},
                    isPremiumUnlocked = false,
                    ownedBackgroundIds = setOf(targetBackgroundId),
                    onBackgroundChange = {},
                    onAbout = {},
                    onBack = {},
                    billingUiState = billingState,
                )
            }
        }

        val targetOption = requireNotNull(BrailuxBackgroundCatalog.option(targetBackgroundId))
        val targetBackgroundName = context.getString(targetOption.nameResource)
        composeRule.onNodeWithText(targetBackgroundName)
            .performScrollTo()
            .assertIsDisplayed()

        val purchasedLabel = "${context.getString(R.string.settings_background_premium)} · ${context.getString(R.string.settings_purchased)}"
        composeRule.onNodeWithText(purchasedLabel)
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun pendingThemeShowsPendingTag() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val targetBackgroundId = BrailuxBackgroundCatalog.LAVANDA_NIEBLA_ID
        val expectedProductId = requireNotNull(
            BrailuxBillingProductCatalog.productIdFor(targetBackgroundId),
        )

        val billingState = BrailuxBillingUiState(
            items = mapOf(
                targetBackgroundId to BrailuxThemeBillingItemState(
                    backgroundId = targetBackgroundId,
                    productId = expectedProductId,
                    status = BrailuxThemePurchaseStatus.Pending,
                ),
            ),
        )

        composeRule.setContent {
            BrailuxAprendeTheme {
                SettingsScreen(
                    preferences = AccessibilityPreferences(),
                    onSoundEnabledChange = {},
                    onVibrationEnabledChange = {},
                    onHighContrastEnabledChange = {},
                    onTextSizeChange = {},
                    onAppearanceChange = {},
                    onSeasonalThemesEnabledChange = {},
                    isPremiumUnlocked = false,
                    ownedBackgroundIds = emptySet(),
                    onBackgroundChange = {},
                    onAbout = {},
                    onBack = {},
                    billingUiState = billingState,
                )
            }
        }

        val targetOption = requireNotNull(BrailuxBackgroundCatalog.option(targetBackgroundId))
        val targetBackgroundName = context.getString(targetOption.nameResource)
        composeRule.onNodeWithText(targetBackgroundName)
            .performScrollTo()
            .assertIsDisplayed()

        val pendingLabel = "${context.getString(R.string.settings_background_premium)} · ${context.getString(R.string.settings_pending)}"
        composeRule.onNodeWithText(pendingLabel)
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun restorePurchasesButtonInvokesCallback() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        var restoreInvoked = false

        composeRule.setContent {
            BrailuxAprendeTheme {
                SettingsScreen(
                    preferences = AccessibilityPreferences(),
                    onSoundEnabledChange = {},
                    onVibrationEnabledChange = {},
                    onHighContrastEnabledChange = {},
                    onTextSizeChange = {},
                    onAppearanceChange = {},
                    onSeasonalThemesEnabledChange = {},
                    isPremiumUnlocked = false,
                    ownedBackgroundIds = emptySet(),
                    onBackgroundChange = {},
                    onAbout = {},
                    onBack = {},
                    onRestorePurchases = { restoreInvoked = true },
                )
            }
        }

        composeRule.onNodeWithText(context.getString(R.string.settings_restore_purchases))
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        assertTrue(restoreInvoked)
    }

    @Test
    fun previewDialogForAvailableThemeShowsBuyAction() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val samplePrice = "EUR 1.19"
        val sampleToken = "tok_salvia_eur"
        val targetBackgroundId = BrailuxBackgroundCatalog.SALVIA_TEXTURA_ID
        val expectedProductId = requireNotNull(
            BrailuxBillingProductCatalog.productIdFor(targetBackgroundId),
        )
        var boughtProductId: String? = null
        var boughtOfferToken: String? = null

        val billingState = BrailuxBillingUiState(
            items = mapOf(
                targetBackgroundId to BrailuxThemeBillingItemState(
                    backgroundId = targetBackgroundId,
                    productId = expectedProductId,
                    status = BrailuxThemePurchaseStatus.AvailableForPurchase(
                        formattedPrice = samplePrice,
                        offerToken = sampleToken,
                    ),
                ),
            ),
        )

        composeRule.setContent {
            BrailuxAprendeTheme {
                SettingsScreen(
                    preferences = AccessibilityPreferences(),
                    onSoundEnabledChange = {},
                    onVibrationEnabledChange = {},
                    onHighContrastEnabledChange = {},
                    onTextSizeChange = {},
                    onAppearanceChange = {},
                    onSeasonalThemesEnabledChange = {},
                    isPremiumUnlocked = false,
                    ownedBackgroundIds = emptySet(),
                    onBackgroundChange = {},
                    onAbout = {},
                    onBack = {},
                    billingUiState = billingState,
                    onBuyProduct = { pid, token ->
                        boughtProductId = pid
                        boughtOfferToken = token
                    },
                )
            }
        }

        val targetOption = requireNotNull(BrailuxBackgroundCatalog.option(targetBackgroundId))
        val targetBackgroundName = context.getString(targetOption.nameResource)
        val previewButtonText = context.getString(R.string.settings_background_preview)

        // Localize the specific real background by its name first
        composeRule.onNodeWithText(targetBackgroundName)
            .performScrollTo()
            .assertIsDisplayed()

        // Deterministically click the preview button of this specific background row
        composeRule.onNode(
            hasText(previewButtonText) and
                hasAnyAncestor(hasAnySibling(hasText(targetBackgroundName))),
        )
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        // Confirm dialog opened and contains the buy action for this specific theme
        val dialogBuyButtonText = context.getString(R.string.settings_buy_with_price, samplePrice)
        composeRule.onNodeWithText(dialogBuyButtonText)
            .assertIsDisplayed()
            .performClick()

        assertEquals(expectedProductId, boughtProductId)
        assertEquals(sampleToken, boughtOfferToken)
    }
}
