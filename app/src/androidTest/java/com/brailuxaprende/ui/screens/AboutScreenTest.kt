package com.brailuxaprende.ui.screens

import androidx.annotation.StringRes
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.brailuxaprende.BuildConfig
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
    fun institutionalInformationAndLinksUseTheDefinitiveValues() {
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

        composeRule.onNodeWithText(context.getString(R.string.about_app_name))
            .assertIsDisplayed()
        composeRule.onNodeWithText(
            context.getString(R.string.about_version, BuildConfig.VERSION_NAME),
        ).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.about_section_brailux))
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText(
            context.getString(R.string.about_app_info_build, BuildConfig.VERSION_CODE),
        )
            .performScrollTo()
            .assertIsDisplayed()

        fun externalLabel(@StringRes title: Int): String = context.getString(
            R.string.about_external_link_accessibility,
            context.getString(title),
        )

        listOf(
            externalLabel(R.string.about_privacy_policy),
            externalLabel(R.string.about_terms),
            externalLabel(R.string.about_licenses),
            externalLabel(R.string.about_contact_support),
            externalLabel(R.string.about_report_issue),
            externalLabel(R.string.about_more_apps),
            externalLabel(R.string.about_official_website),
            externalLabel(R.string.about_brailux_website),
            context.getString(R.string.about_repository_accessibility),
        ).forEach { accessibilityLabel ->
            composeRule.onNodeWithContentDescription(accessibilityLabel)
                .performScrollTo()
                .assertIsDisplayed()
                .assertHasClickAction()
                .performClick()
        }

        assertEquals(
            listOf(
                identity.privacyPolicyUrl,
                identity.termsUrl,
                identity.licensesUrl,
                identity.supportUrl,
                identity.reportIssueUrl,
                identity.appsUrl,
                identity.studioWebsiteUrl,
                identity.brailuxWebsiteUrl,
                identity.repositoryUrl,
            ),
            openedUrls,
        )
    }
}
