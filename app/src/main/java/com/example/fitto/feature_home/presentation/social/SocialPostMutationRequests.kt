package com.example.fitto.feature_home.presentation.social

import android.net.Uri

internal data class SocialPostCreateRequest(
    val content: String,
    val imageUri: Uri?,
    val mealInfo: String?,
    val calories: Int?,
    val allowComments: Boolean,
    val visibility: String,
    val tags: List<String>,
    val isDraft: Boolean
)

internal data class SocialRecipePostCreateRequest(
    val content: String,
    val recipeId: String,
    val recipeTitle: String,
    val recipeImageResId: Int,
    val recipeDuration: String,
    val recipeServing: String,
    val recipeCalories: Int?,
    val recipeCategory: String,
    val tags: List<String>,
    val recipeSourceTag: String,
    val recipeDescription: String,
    val recipeIngredients: List<String>,
    val recipeSteps: List<String>
)

internal data class SocialCommentCreateRequest(
    val postId: String,
    val text: String,
    val parentCommentId: String?
)

internal data class SocialDraftPostUpdateRequest(
    val draftId: String,
    val content: String,
    val imageUri: Uri?,
    val existingImageUrl: String?,
    val mealInfo: String?,
    val calories: Int?,
    val allowComments: Boolean,
    val visibility: String,
    val tags: List<String>,
    val publish: Boolean
)
