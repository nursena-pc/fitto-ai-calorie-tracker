package com.example.fitto.feature_home.presentation.social

internal object SocialThreadReadResolver {

    fun shouldMarkMessageAsRead(
        senderId: String,
        createdAt: Long,
        readBy: Any?,
        currentUserId: String,
        otherUserId: String?,
        isOtherUserCurrentlyBlocked: Boolean,
        hiddenRanges: List<MessageHiddenRange>
    ): Boolean {
        val isBlockedSenderMessage =
            !otherUserId.isNullOrBlank() &&
                    senderId == otherUserId &&
                    (
                            SocialMessageHiddenRangeResolver.isMessageInHiddenRanges(
                                senderId = senderId,
                                createdAt = createdAt,
                                hiddenRanges = hiddenRanges
                            )
                                    ||
                                    (
                                            isOtherUserCurrentlyBlocked &&
                                                    hiddenRanges.isEmpty()
                                            )
                            )

        return senderId != currentUserId &&
                !isBlockedSenderMessage &&
                !SocialReadReceiptResolver.readByIds(readBy).contains(currentUserId)
    }

    fun createThreadReadPayload(currentUserId: String): Map<String, Any> {
        return mapOf("unreadCounts.$currentUserId" to 0)
    }

    fun createMessageReadPayload(
        readBy: Any?,
        currentUserId: String,
        readAt: Long
    ): Map<String, Any> {
        val readByMap = SocialReadReceiptResolver.readByMap(readBy).toMutableMap()
        readByMap[currentUserId] = readAt

        return mapOf(
            "readBy" to readByMap,
            "updatedAt" to readAt
        )
    }
}
