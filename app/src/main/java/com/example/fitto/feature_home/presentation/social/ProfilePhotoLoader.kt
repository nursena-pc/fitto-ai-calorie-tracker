package com.example.fitto.feature_home.presentation.social

import android.content.Context
import android.net.Uri
import android.widget.ImageView
import coil.load
import coil.transform.CircleCropTransformation
import com.example.fitto.R

object ProfilePhotoLoader {

    fun load(
        imageView: ImageView,
        photoUrl: String?,
        placeholderResId: Int = R.drawable.ic_profile_person,
        errorResId: Int = R.drawable.ic_profile_person,
        circleCrop: Boolean = true
    ) {
        val cleanUrl = photoUrl?.trim().orEmpty()

        if (cleanUrl.isBlank()) {
            imageView.setImageResource(placeholderResId)
            return
        }

        val resourceId = resolveDrawableResourceId(
            context = imageView.context,
            photoUrl = cleanUrl
        )

        if (resourceId != null) {
            imageView.setImageResource(resourceId)
            return
        }

        imageView.load(cleanUrl) {
            crossfade(true)
            placeholder(placeholderResId)
            error(errorResId)
            if (circleCrop) {
                transformations(CircleCropTransformation())
            }
        }
    }

    fun resolveDrawableResourceId(
        context: Context,
        photoUrl: String?
    ): Int? {
        val cleanUrl = photoUrl?.trim().orEmpty()
        if (cleanUrl.isBlank()) return null

        val uri = runCatching { Uri.parse(cleanUrl) }.getOrNull() ?: return null
        if (uri.scheme != "android.resource") return null

        val packageName = uri.authority
            ?.takeIf { it.isNotBlank() }
            ?: context.packageName

        val segments = uri.pathSegments.orEmpty()
        val drawableName = when {
            segments.size >= 2 && segments[0] == "drawable" -> segments[1]
            segments.isNotEmpty() -> segments.last()
            else -> null
        }?.substringBeforeLast('.')
            ?.takeIf { it.isNotBlank() }
            ?: return null

        return context.resources
            .getIdentifier(drawableName, "drawable", packageName)
            .takeIf { it != 0 }
    }
}
