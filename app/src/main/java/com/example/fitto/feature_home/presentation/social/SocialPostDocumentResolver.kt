package com.example.fitto.feature_home.presentation.social

internal object SocialPostDocumentResolver {

    fun resolve(
        id: String,
        currentUserId: String,
        userId: String?,
        userName: String?,
        userPhotoUrl: String?,
        fallbackPhotoUrl: String?,
        content: String?,
        imageUrl: String?,
        mealInfo: String?,
        calories: Long?,
        likesCount: Long?,
        commentsCount: Long?,
        createdAt: Long?,
        likedBy: Any?,
        savedBy: Any?,
        allowComments: Boolean?,
        visibility: String?,
        tags: Any?,
        isDraft: Boolean?,
        postType: String?,
        recipeId: String?,
        recipeTitle: String?,
        recipeImageResId: Long?,
        recipeDuration: String?,
        recipeServing: String?,
        recipeCalories: Long?,
        recipeCategory: String?,
        recipeSourceTag: String?,
        recipeDescription: String?,
        recipeIngredients: Any?,
        recipeSteps: Any?
    ): SocialPost? {
        val resolvedUserId = userId.orEmpty()
        if (resolvedUserId.isBlank()) return null

        val likedByIds = stringList(likedBy)
        val savedByIds = stringList(savedBy)

        return SocialPost(
            id = id,
            userId = resolvedUserId,
            userName = userName.orEmpty().ifBlank { "fitto_user" },
            userPhotoUrl = userPhotoUrl ?: fallbackPhotoUrl,
            content = content.orEmpty(),
            imageUrl = imageUrl,
            mealInfo = mealInfo,
            calories = calories?.toInt(),
            likesCount = likesCount?.toInt() ?: 0,
            commentsCount = commentsCount?.toInt() ?: 0,
            createdAt = createdAt ?: 0L,
            likedByMe = likedByIds.contains(currentUserId),
            savedByMe = savedByIds.contains(currentUserId),
            allowComments = allowComments ?: true,
            visibility = visibility.orEmpty().ifBlank { POST_VISIBILITY_COMMUNITY },
            tags = stringList(tags),
            isDraft = isDraft ?: false,
            postType = postType,
            recipeId = recipeId,
            recipeTitle = recipeTitle,
            recipeImageResId = recipeImageResId?.toInt(),
            recipeDuration = recipeDuration,
            recipeServing = recipeServing,
            recipeCalories = recipeCalories?.toInt(),
            recipeCategory = recipeCategory,
            recipeSourceTag = recipeSourceTag,
            recipeDescription = recipeDescription,
            recipeIngredients = stringList(recipeIngredients),
            recipeSteps = stringList(recipeSteps),
            isMine = resolvedUserId == currentUserId
        )
    }

    private fun stringList(value: Any?): List<String> {
        return (value as? List<*>)
            ?.mapNotNull { item -> item as? String }
            ?: emptyList()
    }
}
