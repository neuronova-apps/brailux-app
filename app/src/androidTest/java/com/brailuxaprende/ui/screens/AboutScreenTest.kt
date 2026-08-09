package com.brailuxaprende.ui.screens

import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.brailuxaprende.R
import com.brailuxaprende.ui.identity.InstitutionalIdentity
import com.brailuxaprende.ui.theme.BrailuxAprendeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AboutScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun definitiveIdentityLinksAndPendingPrivacyArePresentedCorrectly() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val identity = InstitutionalIdentity.current
        val openedUrls = mutableListOf<String>()
        composeRule.setContent {
            BrailuxAprendeTheme {
                AboutScreen(
                    onBack = {},
                    onOpenWebsite = { openedUrls += it },
                )
            }
        }

        composeRule.onNodeWithText("NeuroNova Apps")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Matriz de aplicaciones educativas y accesibles.")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Brailux Aprende forma parte de NeuroNova Apps.")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Desarrollado por Gabriel Berrospi")
            .performScrollTo()
            .assertIsDisplayed()

        composeRule.onNodeWithContentDescription(
            context.getString(R.string.about_brailux_website_accessibility),
        )
            .performScrollTo()
            .assertIsDisplayed()
            .assertHasClickAction()
            .performClick()
        composeRule.onNodeWithContentDescription(
            context.getString(
                R.string.about_studio_website_accessibility,
                identity.studioName,
            ),
        )
            .performScrollTo()
            .assertIsDisplayed()
            .assertHasClickAction()
            .performClick()

        assertEquals(
            listOf(identity.brailuxWebsiteUrl, identity.studioWebsiteUrl),
            openedUrls,
        )

        val privacyLabel = context.getString(R.string.about_privacy_policy)
        composeRule.onNodeWithContentDescription(
            context.getString(
                R.string.about_future_accessibility,
                privacyLabel,
                context.getString(R.string.about_coming_soon),
            ),
        )
            .performScrollTo()
            .assertIsDisplayed()
            .assertHasNoClickAction()
    }
}
