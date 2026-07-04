package com.example.fitto.feature_home.presentation.social

import com.google.firebase.firestore.DocumentSnapshot

internal object FirebaseSocialPostSnapshotMapper {

    fun map(
        document: DocumentSnapshot,
        currentUserId: String
    ): SocialPost? {
        return SocialPostDocumentResolver.resolve(
            id = document.id,
            currentUserId = currentUserId,
            userId = document.getString("userId"),
            userName = document.getString("userName"),
            userPhotoUrl = document.getString("userPhotoUrl"),
            fallbackPhotoUrl = document.getString("photoUrl"),
            content = document.getString("content"),
            imageUrl = document.getString("imageUrl"),
            mealInfo = document.getString("mealInfo"),
            calories = document.getLong("calories"),
            likesCount = document.getLong("likesCount"),
            commentsCount = document.getLong("commentsCount"),
            createdAt = document.getLong("createdAt"),
            likedBy = document.get("likedBy"),
            savedBy = document.get("savedBy"),
            allowComments = document.getBoolean("allowComments"),
            visibility = document.getString("visibility"),
            tags = document.get("tags"),
            isDraft = document.getBoolean("isDraft"),
            postType = document.getString("postType"),
            recipeId = document.getString("recipeId"),
            recipeTitle = document.getString("recipeTitle"),
            recipeImageResId = document.getLong("recipeImageResId"),
            recipeDuration = document.getString("recipeDuration"),
            recipeServing = document.getString("recipeServing"),
            recipeCalories = document.getLong("recipeCalories"),
            recipeCategory = document.getString("recipeCategory"),
            recipeSourceTag = document.getString("recipeSourceTag"),
            recipeDescription = document.getString("recipeDescription"),
            recipeIngredients = document.get("recipeIngredients"),
            recipeSteps = document.get("recipeSteps")
        )
    }
}
