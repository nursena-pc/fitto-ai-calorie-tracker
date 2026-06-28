package com.example.fitto.feature_home.presentation.social

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SocialProfileCopyPropagationResolverTest {

    @Test
    fun publicUsernameTargets_createExpectedPropagationTargets() {
        val targets = SocialProfileCopyPropagationResolver.publicUsernameTargets(
            uid = "user-1",
            userName = "nursena",
            now = 123L
        )

        assertEquals(4, targets.size)

        assertTarget(
            target = targets[0],
            scope = SocialProfileCopyQueryScope.COLLECTION,
            sourceName = "posts",
            lookupField = "userId",
            lookupValue = "user-1",
            useSetMerge = true,
            lookupOperator = SocialProfileCopyLookupOperator.EQUAL_TO
        )
        assertEquals("nursena", targets[0].updates["userName"])

        assertTarget(
            target = targets[1],
            scope = SocialProfileCopyQueryScope.COLLECTION_GROUP,
            sourceName = "comments",
            lookupField = "userId",
            lookupValue = "user-1",
            useSetMerge = true,
            lookupOperator = SocialProfileCopyLookupOperator.EQUAL_TO
        )
        assertEquals("nursena", targets[1].updates["userName"])

        assertTarget(
            target = targets[2],
            scope = SocialProfileCopyQueryScope.COLLECTION,
            sourceName = "messageThreads",
            lookupField = "participantIds",
            lookupValue = "user-1",
            useSetMerge = false,
            lookupOperator = SocialProfileCopyLookupOperator.ARRAY_CONTAINS
        )
        assertEquals("nursena", targets[2].updates["participants.user-1.userName"])

        assertTarget(
            target = targets[3],
            scope = SocialProfileCopyQueryScope.COLLECTION_GROUP,
            sourceName = "messages",
            lookupField = "senderId",
            lookupValue = "user-1",
            useSetMerge = true,
            lookupOperator = SocialProfileCopyLookupOperator.EQUAL_TO
        )
        assertEquals("nursena", targets[3].updates["senderName"])
    }

    @Test
    fun profilePhotoTargets_createExpectedPropagationTargets() {
        val targets = SocialProfileCopyPropagationResolver.profilePhotoTargets(
            uid = "user-1",
            photoUrl = "https://example.com/avatar.png",
            now = 456L
        )

        assertEquals(4, targets.size)

        assertTarget(
            target = targets[0],
            scope = SocialProfileCopyQueryScope.COLLECTION,
            sourceName = "posts",
            lookupField = "userId",
            lookupValue = "user-1",
            useSetMerge = true,
            lookupOperator = SocialProfileCopyLookupOperator.EQUAL_TO
        )
        assertEquals("https://example.com/avatar.png", targets[0].updates["userPhotoUrl"])
        assertEquals("https://example.com/avatar.png", targets[0].updates["photoUrl"])

        assertTarget(
            target = targets[1],
            scope = SocialProfileCopyQueryScope.COLLECTION_GROUP,
            sourceName = "comments",
            lookupField = "userId",
            lookupValue = "user-1",
            useSetMerge = true,
            lookupOperator = SocialProfileCopyLookupOperator.EQUAL_TO
        )
        assertEquals("https://example.com/avatar.png", targets[1].updates["userPhotoUrl"])
        assertEquals("https://example.com/avatar.png", targets[1].updates["photoUrl"])

        assertTarget(
            target = targets[2],
            scope = SocialProfileCopyQueryScope.COLLECTION,
            sourceName = "messageThreads",
            lookupField = "participantIds",
            lookupValue = "user-1",
            useSetMerge = false,
            lookupOperator = SocialProfileCopyLookupOperator.ARRAY_CONTAINS
        )
        assertEquals("https://example.com/avatar.png", targets[2].updates["participants.user-1.photoUrl"])

        assertTarget(
            target = targets[3],
            scope = SocialProfileCopyQueryScope.COLLECTION_GROUP,
            sourceName = "messages",
            lookupField = "senderId",
            lookupValue = "user-1",
            useSetMerge = true,
            lookupOperator = SocialProfileCopyLookupOperator.EQUAL_TO
        )
        assertEquals("https://example.com/avatar.png", targets[3].updates["senderPhotoUrl"])
        assertEquals("https://example.com/avatar.png", targets[3].updates["photoUrl"])
    }

    @Test
    fun profilePhotoTargets_keepDeletePayloadKeysWhenPhotoIsBlank() {
        val targets = SocialProfileCopyPropagationResolver.profilePhotoTargets(
            uid = "user-1",
            photoUrl = " ",
            now = 789L
        )

        assertTrue(targets[0].updates.containsKey("userPhotoUrl"))
        assertTrue(targets[0].updates.containsKey("photoUrl"))
        assertTrue(targets[2].updates.containsKey("participants.user-1.photoUrl"))
        assertTrue(targets[3].updates.containsKey("senderPhotoUrl"))
        assertTrue(targets[3].updates.containsKey("photoUrl"))
    }

    @Test
    fun documentBatches_chunksAtFirestoreBatchLimit() {
        val documents = (1..901).toList()

        val result = SocialProfileCopyPropagationResolver.documentBatches(documents)

        assertEquals(3, result.size)
        assertEquals(450, result[0].size)
        assertEquals(450, result[1].size)
        assertEquals(listOf(901), result[2])
    }

    @Test
    fun documentBatches_returnsEmptyWhenInputIsEmptyOrBatchSizeInvalid() {
        assertTrue(SocialProfileCopyPropagationResolver.documentBatches(emptyList<Int>()).isEmpty())
        assertTrue(
            SocialProfileCopyPropagationResolver.documentBatches(
                documents = listOf(1, 2, 3),
                batchSize = 0
            ).isEmpty()
        )
    }

    private fun assertTarget(
        target: SocialProfileCopyPropagationTarget,
        scope: SocialProfileCopyQueryScope,
        sourceName: String,
        lookupField: String,
        lookupValue: String,
        useSetMerge: Boolean,
        lookupOperator: SocialProfileCopyLookupOperator
    ) {
        assertEquals(scope, target.scope)
        assertEquals(sourceName, target.sourceName)
        assertEquals(lookupField, target.lookupField)
        assertEquals(lookupValue, target.lookupValue)
        assertEquals(useSetMerge, target.useSetMerge)
        assertEquals(lookupOperator, target.lookupOperator)
        assertTrue(target.updates.containsKey("updatedAt"))
        assertFalse(target.updates.isEmpty())
    }
}
