package com.example.fitto.feature_home.presentation.social

import org.junit.Assert.assertEquals
import org.junit.Test

class SocialReadReceiptResolverTest {

    @Test
    fun readByIds_readsKeysFromMap() {
        val value = mapOf(
            "user-1" to 123L,
            "user-2" to 456L,
            42 to "ignored"
        )

        assertEquals(
            listOf("user-1", "user-2"),
            SocialReadReceiptResolver.readByIds(value)
        )
    }

    @Test
    fun readByIds_readsValuesFromLegacyList() {
        val value = listOf("user-1", 42, "user-2")

        assertEquals(
            listOf("user-1", "user-2"),
            SocialReadReceiptResolver.readByIds(value)
        )
    }

    @Test
    fun readByMap_readsNumericMapValuesAsLongs() {
        val value = mapOf(
            "user-1" to 123,
            "user-2" to 456L,
            "user-3" to "invalid",
            42 to 999L
        )

        assertEquals(
            mapOf(
                "user-1" to 123L,
                "user-2" to 456L,
                "user-3" to 0L
            ),
            SocialReadReceiptResolver.readByMap(value)
        )
    }

    @Test
    fun readByMap_readsLegacyListWithZeroTimestamps() {
        val value = listOf("user-1", 42, "user-2")

        assertEquals(
            mapOf(
                "user-1" to 0L,
                "user-2" to 0L
            ),
            SocialReadReceiptResolver.readByMap(value)
        )
    }

    @Test
    fun createReadReceiptText_formatsSingleRecipientState() {
        assertEquals(
            "Gönderildi",
            SocialReadReceiptResolver.createReadReceiptText(
                readByOthersCount = 0,
                totalRecipientCount = 1
            )
        )

        assertEquals(
            "Görüldü",
            SocialReadReceiptResolver.createReadReceiptText(
                readByOthersCount = 1,
                totalRecipientCount = 1
            )
        )
    }

    @Test
    fun createReadReceiptText_formatsGroupState() {
        assertEquals(
            "Gönderildi",
            SocialReadReceiptResolver.createReadReceiptText(
                readByOthersCount = 0,
                totalRecipientCount = 3
            )
        )

        assertEquals(
            "2/3 görüldü",
            SocialReadReceiptResolver.createReadReceiptText(
                readByOthersCount = 2,
                totalRecipientCount = 3
            )
        )
    }
}
