package com.example.fitto.feature_recipes.util

import android.net.Uri
import android.widget.ImageView
import androidx.annotation.DrawableRes
import coil.load
import com.example.fitto.R
import com.example.fitto.feature_recipes.model.RecipeUiModel
import com.google.firebase.storage.FirebaseStorage

object RecipeImageLoader {

    private const val STORAGE_FOLDER = "recipe_images"

    private val urlCache = mutableMapOf<String, Uri>()
    private val pendingRequests = mutableMapOf<String, MutableList<(Uri?) -> Unit>>()

    fun load(
        imageView: ImageView,
        recipe: RecipeUiModel,
        @DrawableRes fallbackResId: Int = recipe.imageResId.takeIf { it != 0 }
            ?: R.drawable.profile_avatar_04
    ) {
        load(
            imageView = imageView,
            recipeId = recipe.id,
            fallbackResId = fallbackResId
        )
    }

    fun load(
        imageView: ImageView,
        recipeId: String?,
        @DrawableRes fallbackResId: Int = R.drawable.profile_avatar_04
    ) {
        val seedId = extractSeedId(recipeId.orEmpty())

        if (seedId == null) {
            imageView.load(fallbackResId)
            return
        }

        val fileName = "recipe_${seedId.toString().padStart(3, '0')}.webp"
        val storagePath = "$STORAGE_FOLDER/$fileName"

        imageView.tag = storagePath

        val cachedUri = urlCache[storagePath]

        if (cachedUri != null) {
            imageView.load(cachedUri) {
                placeholder(fallbackResId)
                error(fallbackResId)
                memoryCacheKey(storagePath)
                diskCacheKey(storagePath)
                crossfade(false)
            }
            return
        }

        imageView.load(fallbackResId) {
            memoryCacheKey("${storagePath}_placeholder")
            diskCacheKey("${storagePath}_placeholder")
            crossfade(false)
        }

        getDownloadUrl(storagePath) { uri ->
            if (imageView.tag != storagePath) return@getDownloadUrl

            if (uri == null) {
                imageView.load(fallbackResId) {
                    crossfade(false)
                }
                return@getDownloadUrl
            }

            imageView.load(uri) {
                placeholder(fallbackResId)
                error(fallbackResId)
                memoryCacheKey(storagePath)
                diskCacheKey(storagePath)
                crossfade(false)
            }
        }
    }

    fun prefetch(recipeIds: List<String>) {
        recipeIds.forEach { recipeId ->
            val seedId = extractSeedId(recipeId) ?: return@forEach
            val fileName = "recipe_${seedId.toString().padStart(3, '0')}.webp"
            val storagePath = "$STORAGE_FOLDER/$fileName"

            if (urlCache.containsKey(storagePath)) return@forEach

            getDownloadUrl(storagePath) {
                // Sadece URL cache'i ısınsın diye çağırıyoruz.
            }
        }
    }

    private fun getDownloadUrl(
        storagePath: String,
        callback: (Uri?) -> Unit
    ) {
        urlCache[storagePath]?.let { cachedUri ->
            callback(cachedUri)
            return
        }

        pendingRequests[storagePath]?.let { callbacks ->
            callbacks.add(callback)
            return
        }

        pendingRequests[storagePath] = mutableListOf(callback)

        FirebaseStorage
            .getInstance()
            .reference
            .child(storagePath)
            .downloadUrl
            .addOnSuccessListener { uri ->
                urlCache[storagePath] = uri

                val callbacks = pendingRequests.remove(storagePath).orEmpty()
                callbacks.forEach { it(uri) }
            }
            .addOnFailureListener {
                val callbacks = pendingRequests.remove(storagePath).orEmpty()
                callbacks.forEach { it(null) }
            }
    }

    private fun extractSeedId(recipeId: String): Int? {
        val cleanId = recipeId.trim()

        if (cleanId.isBlank()) return null

        cleanId.removePrefix("seed_").toIntOrNull()?.let { return it }

        cleanId.toIntOrNull()?.let { return it }

        Regex("(?:^|_)seed_(\\d+)(?:_|$)")
            .find(cleanId)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
            ?.let { return it }

        Regex("recipe_(\\d+)")
            .find(cleanId)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
            ?.let { return it }

        return null
    }
}