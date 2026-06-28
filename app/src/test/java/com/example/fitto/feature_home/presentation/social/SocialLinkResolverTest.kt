package com.example.fitto.feature_home.presentation.social

import org.junit.Assert.assertEquals
import org.junit.Test

class SocialLinkResolverTest {

    @Test
    fun extractLinks_returnsEmptyListForBlankText() {
        assertEquals(emptyList<String>(), SocialLinkResolver.extractLinks("   "))
    }

    @Test
    fun extractLinks_preservesHttpAndHttpsLinks() {
        val text = "Tarif: https://fitto.app/recipes/1 ve http://fitto.app/help"

        assertEquals(
            listOf(
                "https://fitto.app/recipes/1",
                "http://fitto.app/help"
            ),
            SocialLinkResolver.extractLinks(text)
        )
    }

    @Test
    fun extractLinks_addsHttpsForBareAndWwwLinks() {
        val text = "Site fitto.app ve www.fitto.app/profile"

        assertEquals(
            listOf(
                "https://fitto.app",
                "https://www.fitto.app/profile"
            ),
            SocialLinkResolver.extractLinks(text)
        )
    }

    @Test
    fun extractLinks_trimsTrailingPunctuation() {
        val text = "Bakınız https://fitto.app/recipes/1, sonra fitto.app/help."

        assertEquals(
            listOf(
                "https://fitto.app/recipes/1",
                "https://fitto.app/help"
            ),
            SocialLinkResolver.extractLinks(text)
        )
    }

    @Test
    fun extractLinks_returnsDistinctLinksInEncounterOrder() {
        val text = "fitto.app https://fitto.app fitto.app"

        assertEquals(
            listOf("https://fitto.app"),
            SocialLinkResolver.extractLinks(text)
        )
    }
}
