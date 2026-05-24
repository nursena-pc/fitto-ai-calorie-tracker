package com.example.fitto.feature_home.presentation.social

import android.content.Context
import android.content.Intent
import com.example.fitto.R

object PostShareHelper {

    fun sharePost(
        context: Context,
        post: SocialPost
    ) {
        val shareText = buildString {
            if (post.content.isNotBlank()) {
                append(post.content)
                append("\n\n")
            }

            if (!post.mealInfo.isNullOrBlank()) {
                append(context.getString(R.string.social_meal_label))
                append(": ")
                append(post.mealInfo)
                append("\n")
            }

            post.calories?.let {
                append(context.getString(R.string.social_calorie_label))
                append(": ")
                append(it)
                append(" kcal\n")
            }

            post.imageUrl?.takeIf { it.isNotBlank() }?.let {
                append("\n")
                append(it)
                append("\n")
            }

            append("\n")
            append(context.getString(R.string.social_shared_from_fitto))
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.app_name))
        }

        context.startActivity(
            Intent.createChooser(
                intent,
                context.getString(R.string.social_share_post)
            )
        )
    }
}