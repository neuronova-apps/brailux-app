package com.brailuxaprende.ui.screens

import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.brailuxaprende.R
import com.brailuxaprende.ui.theme.BrailuxAprendeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AboutScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun institutionalIdentityIsVisibleAndFutureLinksAreNotInteractive() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        composeRule.setContent {
            BrailuxAprendeTheme {
                AboutScreen(onBack = {})
            }
        }

        composeRule.onNodeWithText("Parte de [Nombre de la matriz]")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Desarrollador principal, Gabriel Berrospi.")
            .performScrollTo()
            .assertIsDisplayed()
        listOf(
            context.getString(R.string.about_brailux_website),
            context.getString(
                R.string.about_studio_website,
                context.getString(R.string.about_studio_name_placeholder),
            ),
            context.getString(R.string.about_privacy_policy),
        ).forEach { label ->
            composeRule.onNodeWithContentDescription("$label. Próximamente.")
                .performScrollTo()
                .assertIsDisplayed()
                .assertHasNoClickAction()
        }
    }
}
