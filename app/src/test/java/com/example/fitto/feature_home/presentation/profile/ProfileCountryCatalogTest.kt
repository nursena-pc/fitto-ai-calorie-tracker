package com.example.fitto.feature_home.presentation.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileCountryCatalogTest {

    private val catalog = ProfileCountryCatalog()

    @Test
    fun `turkey is recommended and sorted first`() {
        val countries = catalog.countries(ProfileLanguage.ENGLISH)

        assertEquals("TR", countries.first().code)
        assertTrue(countries.first().isRecommended)
    }

    @Test
    fun `country names follow requested display language`() {
        val englishGermany = catalog.countries(ProfileLanguage.ENGLISH)
            .first { it.code == "DE" }
        val turkishGermany = catalog.countries(ProfileLanguage.TURKISH)
            .first { it.code == "DE" }

        assertTrue(englishGermany.name.isNotBlank())
        assertTrue(turkishGermany.name.isNotBlank())
        assertFalse(turkishGermany.name == englishGermany.name)
    }

    @Test
    fun `filter matches country code ignoring case and whitespace`() {
        val countries = listOf(
            ProfileCountryUiModel("TR", "Turkey", "🇹🇷", true),
            ProfileCountryUiModel("DE", "Germany", "🇩🇪")
        )

        assertEquals(listOf(countries.first()), catalog.filter(countries, " tr "))
    }

    @Test
    fun `blank filter preserves original list`() {
        val countries = listOf(
            ProfileCountryUiModel("TR", "Turkey", "🇹🇷", true),
            ProfileCountryUiModel("DE", "Germany", "🇩🇪")
        )

        assertEquals(countries, catalog.filter(countries, "   "))
    }

    @Test
    fun `valid country code becomes flag emoji`() {
        assertEquals("🇹🇷", catalog.flagEmoji("tr"))
    }

    @Test
    fun `invalid country code uses world fallback`() {
        assertEquals("🌍", catalog.flagEmoji("T1"))
        assertEquals("🌍", catalog.flagEmoji("TUR"))
    }
}
