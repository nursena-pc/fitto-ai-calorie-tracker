package com.example.fitto.feature_home.presentation.social

internal enum class SocialProfileCopyQueryScope {
    COLLECTION,
    COLLECTION_GROUP
}

internal enum class SocialProfileCopyLookupOperator {
    EQUAL_TO,
    ARRAY_CONTAINS
}

internal data class SocialProfileCopyPropagationTarget(
    val scope: SocialProfileCopyQueryScope,
    val sourceName: String,
    val lookupField: String,
    val lookupValue: String,
    val updates: Map<String, Any>,
    val useSetMerge: Boolean = true,
    val lookupOperator: SocialProfileCopyLookupOperator = SocialProfileCopyLookupOperator.EQUAL_TO
)

internal object SocialProfileCopyPropagationResolver {

    private const val FIRESTORE_BATCH_WRITE_LIMIT = 450
    private const val POSTS_COLLECTION = "posts"
    private const val COMMENTS_SUBCOLLECTION = "comments"
    private const val MESSAGE_THREADS_COLLECTION = "messageThreads"
    private const val MESSAGES_SUBCOLLECTION = "messages"

    fun publicUsernameTargets(
        uid: String,
        userName: String,
        now: Long
    ): List<SocialProfileCopyPropagationTarget> {
        return listOf(
            SocialProfileCopyPropagationTarget(
                scope = SocialProfileCopyQueryScope.COLLECTION,
                sourceName = POSTS_COLLECTION,
                lookupField = "userId",
                lookupValue = uid,
                updates = SocialProfileCopyUpdateResolver.publicUsernameUpdate(
                    userName = userName,
                    now = now
                )
            ),
            SocialProfileCopyPropagationTarget(
                scope = SocialProfileCopyQueryScope.COLLECTION_GROUP,
                sourceName = COMMENTS_SUBCOLLECTION,
                lookupField = "userId",
                lookupValue = uid,
                updates = SocialProfileCopyUpdateResolver.publicUsernameUpdate(
                    userName = userName,
                    now = now
                )
            ),
            SocialProfileCopyPropagationTarget(
                scope = SocialProfileCopyQueryScope.COLLECTION,
                sourceName = MESSAGE_THREADS_COLLECTION,
                lookupField = "participantIds",
                lookupValue = uid,
                updates = SocialProfileCopyUpdateResolver.participantUsernameUpdate(
                    uid = uid,
                    userName = userName,
                    now = now
                ),
                useSetMerge = false,
                lookupOperator = SocialProfileCopyLookupOperator.ARRAY_CONTAINS
            ),
            SocialProfileCopyPropagationTarget(
                scope = SocialProfileCopyQueryScope.COLLECTION_GROUP,
                sourceName = MESSAGES_SUBCOLLECTION,
                lookupField = "senderId",
                lookupValue = uid,
                updates = SocialProfileCopyUpdateResolver.senderUsernameUpdate(
                    userName = userName,
                    now = now
                )
            )
        )
    }

    fun profilePhotoTargets(
        uid: String,
        photoUrl: String?,
        now: Long
    ): List<SocialProfileCopyPropagationTarget> {
        return listOf(
            SocialProfileCopyPropagationTarget(
                scope = SocialProfileCopyQueryScope.COLLECTION,
                sourceName = POSTS_COLLECTION,
                lookupField = "userId",
                lookupValue = uid,
                updates = SocialProfileCopyUpdateResolver.profilePhotoUpdate(
                    fieldNames = listOf("userPhotoUrl", "photoUrl"),
                    photoUrl = photoUrl,
                    now = now
                )
            ),
            SocialProfileCopyPropagationTarget(
                scope = SocialProfileCopyQueryScope.COLLECTION_GROUP,
                sourceName = COMMENTS_SUBCOLLECTION,
                lookupField = "userId",
                lookupValue = uid,
                updates = SocialProfileCopyUpdateResolver.profilePhotoUpdate(
                    fieldNames = listOf("userPhotoUrl", "photoUrl"),
                    photoUrl = photoUrl,
                    now = now
                )
            ),
            SocialProfileCopyPropagationTarget(
                scope = SocialProfileCopyQueryScope.COLLECTION,
                sourceName = MESSAGE_THREADS_COLLECTION,
                lookupField = "participantIds",
                lookupValue = uid,
                updates = SocialProfileCopyUpdateResolver.participantPhotoUpdate(
                    uid = uid,
                    photoUrl = photoUrl,
                    now = now
                ),
                useSetMerge = false,
                lookupOperator = SocialProfileCopyLookupOperator.ARRAY_CONTAINS
            ),
            SocialProfileCopyPropagationTarget(
                scope = SocialProfileCopyQueryScope.COLLECTION_GROUP,
                sourceName = MESSAGES_SUBCOLLECTION,
                lookupField = "senderId",
                lookupValue = uid,
                updates = SocialProfileCopyUpdateResolver.profilePhotoUpdate(
                    fieldNames = listOf("senderPhotoUrl", "photoUrl"),
                    photoUrl = photoUrl,
                    now = now
                )
            )
        )
    }

    fun <T> documentBatches(
        documents: List<T>,
        batchSize: Int = FIRESTORE_BATCH_WRITE_LIMIT
    ): List<List<T>> {
        if (batchSize <= 0) return emptyList()
        return documents.chunked(batchSize)
    }
}
