package com.example.fitto.feature_home.presentation.social

import com.example.fitto.feature_home.presentation.social.POST_VISIBILITY_COMMUNITY

data class SocialUser(
    val id: String = "",
    val userName: String = "",
    val displayName: String? = null,
    val personalName: String? = null,
    val email: String? = null,
    val photoUrl: String? = null,
    val bio: String? = null,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val isFollowedByMe: Boolean = false
)


data class SocialPost(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userPhotoUrl: String? = null,
    val content: String = "",
    val imageUrl: String? = null,
    val mealInfo: String? = null,
    val calories: Int? = null,
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val createdAt: Long = 0L,
    val likedByMe: Boolean = false,
    val savedByMe: Boolean = false,
    val allowComments: Boolean = true,
    val visibility: String = POST_VISIBILITY_COMMUNITY,
    val tags: List<String> = emptyList(),
    val isDraft: Boolean = false,
    val postType: String? = null,
    val recipeId: String? = null,
    val recipeTitle: String? = null,
    val recipeImageResId: Int? = null,
    val recipeDuration: String? = null,
    val recipeServing: String? = null,
    val recipeCalories: Int? = null,
    val recipeCategory: String? = null,
    val recipeSourceTag: String? = null,
    val recipeDescription: String? = null,
    val recipeIngredients: List<String> = emptyList(),
    val recipeSteps: List<String> = emptyList(),
    val isMine: Boolean = false
)

data class SocialComment(
    val id: String = "",
    val postId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userPhotoUrl: String? = null,
    val text: String = "",
    val parentCommentId: String? = null,
    val likesCount: Int = 0,
    val replyCount: Int = 0,
    val likedByMe: Boolean = false,
    val createdAt: Long = 0L
)
