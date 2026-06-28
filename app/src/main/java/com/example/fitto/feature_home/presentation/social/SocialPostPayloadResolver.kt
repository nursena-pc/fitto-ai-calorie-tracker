package com.example.fitto.feature_home.presentation.social

internal data class SocialPostAuthor(
    val userName: String,
    val photoUrl: String?
)

internal object SocialPostPayloadResolver {

    fun resolveAuthor(
        profileUserName: String?,
        profilePhotoUrl: String?,
        email: String?,
        authPhotoUrl: String?
    ): SocialPostAuthor {
        return SocialPostAuthor(
            userName = profileUserName?.ifBlank { null }
                ?: email?.substringBefore("@")
                ?: "fitto_user",
            photoUrl = profilePhotoUrl ?: authPhotoUrl
        )
    }

    fun createPostPayload(
        userId: String,
        author: SocialPostAuthor,
        content: String,
        imageUrl: String?,
        mealInfo: String?,
        calories: Int?,
        allowComments: Boolean,
        visibility: String,
        tags: List<String>,
        isDraft: Boolean,
        createdAt: Long
    ): Map<String, Any?> {
        return mapOf(
            "userId" to userId,
            "userName" to author.userName,
            "userPhotoUrl" to author.photoUrl,
            "photoUrl" to author.photoUrl,
            "content" to content.trim(),
            "imageUrl" to imageUrl,
            "mealInfo" to mealInfo?.trim(),
            "calories" to calories,
            "visibility" to visibility,
            "tags" to tags,
            "isDraft" to isDraft,
            "likesCount" to 0,
            "commentsCount" to 0,
            "likedBy" to emptyList<String>(),
            "savedBy" to emptyList<String>(),
            "allowComments" to allowComments,
            "createdAt" to createdAt
        )
    }

    fun createRecipePostPayload(
        userId: String,
        author: SocialPostAuthor,
        content: String,
        recipeId: String,
        recipeTitle: String,
        recipeImageResId: Int?,
        recipeDuration: String,
        recipeServing: String,
        recipeCalories: Int?,
        recipeCategory: String,
        tags: List<String>,
        recipeSourceTag: String?,
        recipeDescription: String,
        recipeIngredients: List<String>,
        recipeSteps: List<String>,
        createdAt: Long
    ): Map<String, Any?> {
        return mapOf(
            "userId" to userId,
            "userName" to author.userName,
            "userPhotoUrl" to author.photoUrl,
            "photoUrl" to author.photoUrl,
            "content" to content.trim(),
            "imageUrl" to null,
            "mealInfo" to recipeTitle.trim(),
            "calories" to recipeCalories,
            "visibility" to "community",
            "tags" to tags.distinct().filter { it.isNotBlank() },
            "isDraft" to false,
            "likesCount" to 0,
            "commentsCount" to 0,
            "likedBy" to emptyList<String>(),
            "savedBy" to emptyList<String>(),
            "allowComments" to true,
            "createdAt" to createdAt,
            "postType" to "recipe",
            "recipeId" to recipeId,
            "recipeTitle" to recipeTitle,
            "recipeImageResId" to recipeImageResId,
            "recipeDuration" to recipeDuration,
            "recipeServing" to recipeServing,
            "recipeCalories" to recipeCalories,
            "recipeCategory" to recipeCategory,
            "recipeSourceTag" to recipeSourceTag,
            "recipeDescription" to recipeDescription.trim(),
            "recipeIngredients" to recipeIngredients.map { it.trim() }.filter { it.isNotBlank() },
            "recipeSteps" to recipeSteps.map { it.trim() }.filter { it.isNotBlank() }
        )
    }

    fun createCommentPayload(
        userId: String,
        author: SocialPostAuthor,
        postId: String,
        postOwnerId: String,
        text: String,
        parentCommentId: String?,
        createdAt: Long
    ): Map<String, Any?> {
        return mapOf(
            "userId" to userId,
            "userName" to author.userName,
            "userPhotoUrl" to author.photoUrl,
            "photoUrl" to author.photoUrl,
            "postId" to postId,
            "postOwnerId" to postOwnerId,
            "text" to text.trim(),
            "parentCommentId" to parentCommentId,
            "likesCount" to 0,
            "replyCount" to 0,
            "likedBy" to emptyList<String>(),
            "createdAt" to createdAt
        )
    }
}
