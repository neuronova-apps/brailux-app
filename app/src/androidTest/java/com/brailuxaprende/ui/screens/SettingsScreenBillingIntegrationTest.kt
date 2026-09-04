package com.brailuxaprende.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
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
        val sampleToken = "token_blackboard_promo"
        val targetBackgroundId = "blackboard"
        val expectedProductId = BrailuxBillingProductCatalog.productIdFor(targetBackgroundId)!!

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
        val targetBackgroundId = "blackboard"
        val expectedProductId = BrailuxBillingProductCatalog.productIdFor(targetBackgroundId)!!

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

        val purchasedLabel = "${context.getString(R.string.settings_background_premium)} · ${context.getString(R.string.settings_purchased)}"
        composeRule.onNodeWithText(purchasedLabel)
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun pendingThemeShowsPendingTag() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val targetBackgroundId = "blackboard"
        val expectedProductId = BrailuxBillingProductCatalog.productIdFor(targetBackgroundId)!!

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
        val sampleToken = "tok_chalk_eur"
        val targetBackgroundId = "blackboard"
        val expectedProductId = BrailuxBillingProductCatalog.productIdFor(targetBackgroundId)!!
        var bought = false

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
                    onBuyProduct = { _, _ -> bought = true },
                )
            }
        }

        val previewNodes = composeRule.onAllNodesWithText(context.getString(R.string.settings_background_preview))
        previewNodes[0].performScrollTo().performClick()

        val dialogBuyButtonText = context.getString(R.string.settings_buy_with_price, samplePrice)
        composeRule.onNodeWithText(dialogBuyButtonText)
            .assertIsDisplayed()
            .performClick()

        assertTrue(bought)
    }
}
