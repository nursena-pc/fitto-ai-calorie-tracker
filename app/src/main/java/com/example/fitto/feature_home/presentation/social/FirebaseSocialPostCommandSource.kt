package com.example.fitto.feature_home.presentation.social

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

internal class FirebaseSocialPostCommandSource(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val ensureCurrentUserProfile: suspend () -> Unit,
    private val loadCurrentUserProfile: suspend () -> SocialUser?
) : SocialPostCommandSource {

    override suspend fun createPost(request: SocialPostCreateRequest) {
        val currentAuthor = resolveCurrentAuthor() ?: return
        val imageUrl = request.imageUri?.let { uploadPostImage(it) }

        firestore.collection(POSTS_COLLECTION)
            .add(
                SocialPostPayloadResolver.createPostPayload(
                    userId = currentAuthor.userId,
                    author = currentAuthor.author,
                    content = request.content,
                    imageUrl = imageUrl,
                    mealInfo = request.mealInfo,
                    calories = request.calories,
                    allowComments = request.allowComments,
                    visibility = request.visibility,
                    tags = request.tags,
                    isDraft = request.isDraft,
                    createdAt = System.currentTimeMillis()
                )
            )
            .await()
    }

    override suspend fun createRecipePost(request: SocialRecipePostCreateRequest): String? {
        val currentAuthor = resolveCurrentAuthor() ?: return null
        val postRef = firestore.collection(POSTS_COLLECTION).document()

        postRef.set(
            SocialPostPayloadResolver.createRecipePostPayload(
                userId = currentAuthor.userId,
                author = currentAuthor.author,
                content = request.content,
                recipeId = request.recipeId,
                recipeTitle = request.recipeTitle,
                recipeImageResId = request.recipeImageResId,
                recipeDuration = request.recipeDuration,
                recipeServing = request.recipeServing,
                recipeCalories = request.recipeCalories,
                recipeCategory = request.recipeCategory,
                tags = request.tags,
                recipeSourceTag = request.recipeSourceTag,
                recipeDescription = request.recipeDescription,
                recipeIngredients = request.recipeIngredients,
                recipeSteps = request.recipeSteps,
                createdAt = System.currentTimeMillis()
            )
        ).await()

        return postRef.id
    }

    override suspend fun updateDraftPost(request: SocialDraftPostUpdateRequest) {
        val currentUserId = auth.currentUser?.uid ?: return
        if (request.draftId.isBlank()) return

        val postRef = firestore.collection(POSTS_COLLECTION).document(request.draftId)
        val snapshot = postRef.get().await()

        if (!snapshot.exists()) {
            throw IllegalStateException("DRAFT_NOT_FOUND")
        }

        if (snapshot.getString("userId") != currentUserId) {
            throw IllegalStateException("DRAFT_OWNER_MISMATCH")
        }

        val finalImageUrl = request.imageUri?.let { uri ->
            uploadPostImage(uri)
        } ?: request.existingImageUrl ?: snapshot.getString("imageUrl")

        val updatedAt = System.currentTimeMillis()
        val updates = SocialPostDraftUpdateResolver.createUpdatePayload(
            content = request.content,
            imageUrl = finalImageUrl,
            mealInfo = request.mealInfo,
            calories = request.calories,
            allowComments = request.allowComments,
            visibility = request.visibility,
            tags = request.tags,
            publish = request.publish,
            updatedAt = updatedAt
        )

        postRef.set(updates, SetOptions.merge()).await()
    }

    private suspend fun resolveCurrentAuthor(): CurrentPostAuthor? {
        val user = auth.currentUser ?: return null

        ensureCurrentUserProfile()
        val profile = loadCurrentUserProfile()

        return CurrentPostAuthor(
            userId = user.uid,
            author = SocialPostPayloadResolver.resolveAuthor(
                profileUserName = profile?.userName,
                profilePhotoUrl = profile?.photoUrl,
                email = user.email,
                authPhotoUrl = user.photoUrl?.toString()
            )
        )
    }

    private suspend fun uploadPostImage(uri: Uri): String? {
        return try {
            val imageRef = storage.reference.child(
                SocialMediaUploadResolver.postImageStoragePath(
                    fileId = UUID.randomUUID().toString()
                )
            )

            imageRef.putFile(uri).await()
            imageRef.downloadUrl.await().toString()
        } catch (exception: Exception) {
            exception.printStackTrace()
            null
        }
    }

    private data class CurrentPostAuthor(
        val userId: String,
        val author: SocialPostAuthor
    )

    private companion object {
        const val POSTS_COLLECTION = "posts"
    }
}
