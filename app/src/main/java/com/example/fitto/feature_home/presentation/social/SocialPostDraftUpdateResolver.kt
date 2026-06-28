package com.example.fitto.feature_home.presentation.social

internal object SocialPostDraftUpdateResolver {

    fun resolveVisibility(
        publish: Boolean,
        visibility: String
    ): String {
        return if (publish) {
            visibility.ifBlank { POST_VISIBILITY_COMMUNITY }
        } else {
            POST_VISIBILITY_PRIVATE
        }
    }

    fun createUpdatePayload(
        content: String,
        imageUrl: String?,
        mealInfo: String?,
        calories: Int?,
        allowComments: Boolean,
        visibility: String,
        tags: List<String>,
        publish: Boolean,
        updatedAt: Long
    ): Map<String, Any?> {
        val updates = mutableMapOf<String, Any?>(
            "content" to content.trim(),
            "imageUrl" to imageUrl,
            "mealInfo" to mealInfo?.trim(),
            "calories" to calories,
            "allowComments" to allowComments,
            "visibility" to resolveVisibility(publish, visibility),
            "tags" to tags,
            "isDraft" to !publish,
            "updatedAt" to updatedAt
        )

        if (publish) {
            updates["publishedAt"] = updatedAt
        }

        return updates
    }
}
