package com.example.fitto.feature_recipes.data

import android.content.Context
import com.example.fitto.R
import com.example.fitto.data.local.FittoDatabaseProvider
import com.example.fitto.data.local.dao.RecipeDao
import com.example.fitto.data.local.entity.RecipeEntity
import com.example.fitto.feature_recipes.model.RecipeFilterOption
import com.example.fitto.feature_recipes.model.RecipeFilterState
import com.example.fitto.feature_recipes.model.RecipeUiModel
import com.example.fitto.feature_home.presentation.social.SocialRepository
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONArray


data class UserRecipeDraft(
    val title: String,
    val description: String,
    val mainCategoryKey: String,
    val durationLabel: String,
    val difficultyKey: String,
    val ingredients: List<String>,
    val steps: List<String>
)

open class RecipeRepository(context: Context) {

    private val appContext: Context = context.applicationContext
    private val recipeDao: RecipeDao = FittoDatabaseProvider.getDatabase(appContext).recipeDao()

    fun recipes(): List<RecipeUiModel> {
        ensureSeeded()
        val entities: List<RecipeEntity> = recipeDao.getVisibleRecipes(currentOwnerUid())
        return RecipePresentationResolver.toUiModels(
            entities = entities,
            favoriteIds = favoriteIds,
            imageResIdResolver = { entity -> entity.resolvedImageResId() }
        )
    }

    fun userRecipes(): List<RecipeUiModel> {
        ensureSeeded()
        return RecipePresentationResolver.toUiModels(
            entities = recipeDao.getUserRecipes(currentOwnerUid()),
            favoriteIds = favoriteIds,
            imageResIdResolver = { entity -> entity.resolvedImageResId() }
        )
    }

    fun communityRecipes(): List<RecipeUiModel> {
        ensureSeeded()
        return RecipePresentationResolver.toUiModels(
            entities = recipeDao.getCommunityRecipes(currentOwnerUid()),
            favoriteIds = favoriteIds,
            imageResIdResolver = { entity -> entity.resolvedImageResId() }
        )
    }

    fun favoriteOriginalRecipes(): List<RecipeUiModel> {
        ensureSeeded()
        return RecipePresentationResolver.favoriteOriginalRecipes(
            entities = recipeDao.getVisibleRecipes(currentOwnerUid()),
            favoriteIds = favoriteIds
        ).map { entity -> entity.toUiModel(isFavorite = true) }
    }

    fun addUserRecipe(draft: UserRecipeDraft): String {
        ensureSeeded()

        val now = System.currentTimeMillis()
        val entity = RecipeUserBookResolver.buildUserRecipeEntity(
            RecipeUserBookResolver.UserRecipeEntityInput(
                draft = draft,
                ownerUid = currentOwnerUid(),
                now = now,
                defaultImageResId = R.drawable.profile_avatar_04
            )
        )

        recipeDao.upsertRecipe(entity)
        return entity.id
    }

    fun findById(recipeId: String): RecipeUiModel? {
        ensureSeeded()
        return recipeDao.findById(recipeId)?.toUiModel(isFavorite = recipeId in favoriteIds)
    }

    fun toggleFavorite(recipeId: String) {
        if (recipeId in favoriteIds) {
            favoriteIds.remove(recipeId)
        } else {
            favoriteIds.add(recipeId)
        }
    }

    fun saveFavorite(recipeId: String) {
        favoriteIds.add(recipeId)
    }

    fun removeFromBook(recipeId: String): Boolean {
        ensureSeeded()

        val recipe: RecipeEntity? = recipeDao.findById(recipeId)
        if (recipe?.sourceType == RecipeEntity.SOURCE_TYPE_SEED) {
            favoriteIds.remove(recipeId)
            return true
        }

        favoriteIds.remove(recipeId)
        val ownerUid = currentOwnerUid()
        return recipeDao.deleteOwnedBookRecipe(recipeId, ownerUid) > 0
    }

    fun copyRecipeToUserBook(recipeId: String, ingredientsOverride: List<String>? = null): String? {
        ensureSeeded()

        val source: RecipeEntity = recipeDao.findById(recipeId) ?: return null
        val now = System.currentTimeMillis()
        val copiedEntity = RecipeUserBookResolver.buildCopiedRecipeEntity(
            RecipeUserBookResolver.CopyRecipeEntityInput(
                source = source,
                ownerUid = currentOwnerUid(),
                now = now,
                ingredientsOverride = ingredientsOverride,
                resolvedImageResId = source.resolvedImageResId()
            )
        )

        recipeDao.upsertRecipe(copiedEntity)
        return copiedEntity.id
    }


    suspend fun shareRecipeToCommunity(recipeId: String, ingredientsOverride: List<String>? = null, comment: String = ""): String? {
        ensureSeeded()

        val source: RecipeEntity = recipeDao.findById(recipeId) ?: return null
        val cleanIngredients: List<String> = RecipeCommunityShareResolver.cleanShareIngredients(
            ingredientsOverride = ingredientsOverride,
            sourceIngredients = source.ingredients
        )
        val content = RecipeCommunityShareResolver.buildShareContent(
            RecipeCommunityShareResolver.ShareContentInput(
                comment = comment,
                recipeTitle = source.title,
                ingredients = cleanIngredients
            )
        )
        val recipeSourceTag = RecipeCommunityShareResolver.sourceTagForRecipe(source.sourceType)

        val postId = SocialRepository().createRecipePost(
            content = content,
            recipeId = source.id,
            recipeTitle = source.title,
            recipeImageResId = source.resolvedImageResId(),
            recipeDuration = source.durationLabel,
            recipeServing = RecipeCommunityShareResolver.servingText(source.servingDisplay, source.servings),
            recipeCalories = source.calories.takeIf { it > 0 },
            recipeCategory = source.mainCategoryKey,
            tags = RecipeCommunityShareResolver.tagsForRecipe(source.mainCategoryKey),
            recipeSourceTag = recipeSourceTag,
            recipeDescription = source.description,
            recipeIngredients = cleanIngredients,
            recipeSteps = source.steps
        )

        postId?.let { communitySharedPostIds.add(it) }
        return postId
    }


    fun saveRecipeFromCommunityPost(
        postId: String,
        postUserId: String,
        recipeId: String?,
        recipeTitle: String?,
        recipeImageResId: Int?,
        recipeDuration: String?,
        recipeServing: String?,
        recipeCalories: Int?,
        recipeCategory: String?,
        recipeSourceTag: String?,
        recipeDescription: String? = null,
        recipeIngredients: List<String> = emptyList(),
        recipeSteps: List<String> = emptyList()
    ): String? {
        ensureSeeded()

        val ownerUid = currentOwnerUid()
        val cleanPostId = postId.trim()
        val cleanRecipeId = recipeId.orEmpty().trim()
        val source: RecipeEntity? = cleanRecipeId.takeIf { it.isNotBlank() }?.let { id ->
            recipeDao.findById(id)
        }

        when (
            RecipeCommunitySaveResolver.resolveSaveDecision(
                RecipeCommunitySaveResolver.SaveDecisionInput(
                    ownerUid = ownerUid,
                    postUserId = postUserId,
                    recipeTitle = recipeTitle,
                    recipeIngredients = recipeIngredients,
                    recipeSteps = recipeSteps,
                    recipeSourceTag = recipeSourceTag,
                    sourceOwnerUid = source?.ownerUid,
                    sourceType = source?.sourceType
                )
            )
        ) {
            RecipeCommunitySaveResolver.SaveDecision.FAVORITE_SOURCE -> {
                source?.let { recipe ->
                    saveFavorite(recipe.id)
                    return recipe.id
                }
                return null
            }
            RecipeCommunitySaveResolver.SaveDecision.IGNORE -> {
                source?.let { recipe ->
                    saveFavorite(recipe.id)
                    return recipe.id
                }
                return null
            }
            RecipeCommunitySaveResolver.SaveDecision.STORE_COMMUNITY -> Unit
        }

        if (cleanPostId.isNotBlank()) {
            recipeDao.findByOwnerAndSourcePost(ownerUid, cleanPostId)?.let { existing ->
                return existing.id
            }
        }

        val now = System.currentTimeMillis()
        val sharedId = RecipeCommunitySaveResolver.buildSharedRecipeId(
            ownerUid = ownerUid,
            postId = cleanPostId,
            recipeId = cleanRecipeId,
            now = now
        )
        val values = RecipeCommunitySaveResolver.resolveCommunityValues(
            recipeTitle = recipeTitle,
            recipeDescription = recipeDescription,
            recipeDuration = recipeDuration,
            recipeServing = recipeServing,
            recipeCalories = recipeCalories,
            recipeCategory = recipeCategory,
            recipeIngredients = recipeIngredients,
            recipeSteps = recipeSteps,
            source = source,
            defaultDescription = "Topluluktan kaydedilen tarif"
        )

        val communityEntity = if (source != null) {
            source.copy(
                id = sharedId,
                ownerUid = ownerUid,
                sourceType = RecipeEntity.SOURCE_TYPE_COMMUNITY,
                sourceRecipeId = RecipeCommunitySaveResolver.sourceRecipeIdForCommunityCopy(cleanRecipeId, source),
                sourceUserId = RecipeCommunitySaveResolver.sourceUserIdForCommunityCopy(postUserId, source),
                sourcePostId = RecipeCommunitySaveResolver.sourcePostIdForCommunityCopy(cleanPostId, source),
                title = values.title,
                description = values.description,
                calories = values.calories,
                imageResId = source.resolvedImageResId(),
                ingredients = values.ingredients,
                ingredientNames = values.ingredientNames,
                steps = values.steps,
                createdAt = now,
                updatedAt = now
            )
        } else {
            RecipeEntity(
                id = sharedId,
                ownerUid = ownerUid,
                sourceType = RecipeEntity.SOURCE_TYPE_COMMUNITY,
                sourceRecipeId = RecipeCommunitySaveResolver.sourceRecipeIdForCommunityCopy(cleanRecipeId, null),
                sourceUserId = RecipeCommunitySaveResolver.sourceUserIdForCommunityCopy(postUserId, null),
                sourcePostId = RecipeCommunitySaveResolver.sourcePostIdForCommunityCopy(cleanPostId, null),
                title = values.title,
                description = values.description,
                mainCategoryKey = values.category,
                subcategoryKey = "",
                subcategoryKeys = emptySet(),
                mainIngredientKeys = emptySet(),
                dietKeys = emptySet(),
                methodKeys = emptySet(),
                durationMinutes = RecipeValueResolver.representativeMinutes(values.durationLabel),
                durationLabel = values.durationLabel,
                difficultyKey = "kolay",
                calories = values.calories,
                imageResId = recipeImageResId ?: imageForRecipeIdOrDefault(cleanRecipeId),
                servings = 1,
                servingDisplay = values.servingDisplay,
                proteinGrams = 0,
                carbGrams = 0,
                fatGrams = 0,
                ingredientNames = values.ingredientNames,
                ingredients = values.ingredients,
                steps = values.steps,
                tip = null,
                createdAt = now,
                updatedAt = now
            )
        }

        recipeDao.upsertRecipe(communityEntity)
        return sharedId
    }

    fun hasCommunityRecipeFromPost(postId: String): Boolean {
        val cleanPostId = postId.trim()
        if (cleanPostId.isBlank()) return false
        ensureSeeded()
        return recipeDao.findByOwnerAndSourcePost(currentOwnerUid(), cleanPostId) != null
    }

    fun ensureRecipeAvailableFromCommunityPost(
        postId: String,
        postUserId: String,
        recipeId: String?,
        recipeTitle: String?,
        recipeImageResId: Int?,
        recipeDuration: String?,
        recipeServing: String?,
        recipeCalories: Int?,
        recipeCategory: String?,
        recipeSourceTag: String?,
        recipeDescription: String? = null,
        recipeIngredients: List<String> = emptyList(),
        recipeSteps: List<String> = emptyList()
    ): String? {
        ensureSeeded()

        val cleanRecipeId = recipeId.orEmpty().trim()
        recipeDao.findById(cleanRecipeId)?.let { existing -> return existing.id }

        val cleanPostId = postId.trim()
        val previewId = RecipeCommunitySaveResolver.buildPreviewRecipeId(
            postId = cleanPostId,
            recipeId = cleanRecipeId
        )
        recipeDao.findById(previewId)?.let { existing -> return existing.id }

        val cleanTitle = recipeTitle.orEmpty().trim()
        if (cleanTitle.isBlank()) return null

        val values = RecipeCommunitySaveResolver.resolveCommunityValues(
            recipeTitle = cleanTitle,
            recipeDescription = recipeDescription,
            recipeDuration = recipeDuration,
            recipeServing = recipeServing,
            recipeCalories = recipeCalories,
            recipeCategory = recipeCategory,
            recipeIngredients = recipeIngredients,
            recipeSteps = recipeSteps,
            source = null,
            defaultDescription = "Topluluktan açılan tarif"
        )
        if (values.title.isBlank()) return null
        val now = System.currentTimeMillis()

        recipeDao.upsertRecipe(
            RecipeEntity(
                id = previewId,
                ownerUid = null,
                sourceType = RecipeEntity.SOURCE_TYPE_COMMUNITY,
                sourceRecipeId = RecipeCommunitySaveResolver.sourceRecipeIdForCommunityCopy(cleanRecipeId, null),
                sourceUserId = RecipeCommunitySaveResolver.sourceUserIdForCommunityCopy(postUserId, null),
                sourcePostId = RecipeCommunitySaveResolver.sourcePostIdForCommunityCopy(cleanPostId, null),
                title = values.title,
                description = values.description,
                mainCategoryKey = values.category,
                subcategoryKey = "",
                subcategoryKeys = emptySet(),
                mainIngredientKeys = emptySet(),
                dietKeys = emptySet(),
                methodKeys = emptySet(),
                durationMinutes = RecipeValueResolver.representativeMinutes(values.durationLabel),
                durationLabel = values.durationLabel,
                difficultyKey = "kolay",
                calories = values.calories,
                imageResId = recipeImageResId ?: imageForRecipeIdOrDefault(cleanRecipeId),
                servings = 1,
                servingDisplay = values.servingDisplay,
                proteinGrams = 0,
                carbGrams = 0,
                fatGrams = 0,
                ingredientNames = values.ingredientNames,
                ingredients = values.ingredients,
                steps = values.steps,
                tip = null,
                createdAt = now,
                updatedAt = now
            )
        )

        return previewId
    }

    fun allIngredientOptions(): List<RecipeFilterOption> {
        return RecipeFilterResolver.ingredientOptions(recipes())
    }

    fun filterRecipes(
        query: String,
        filterState: RecipeFilterState
    ): List<RecipeUiModel> {
        return RecipeFilterResolver.filterRecipes(
            recipes = recipes(),
            query = query,
            filterState = filterState
        )
    }

    private fun currentOwnerUid(): String {
        return FirebaseAuth.getInstance().currentUser?.uid
            ?.takeIf { uid -> uid.isNotBlank() }
            ?: LOCAL_USER_UID
    }

    private fun ensureSeeded() {
        if (seedChecked) return

        synchronized(seedLock) {
            if (seedChecked) return

            val seedRecipes: List<RecipeEntity> = parseSeedRecipes()
            val currentSeedCount: Int = recipeDao.countSeedRecipes()

            val prefs = appContext.getSharedPreferences(SEED_PREFS_NAME, Context.MODE_PRIVATE)
            val savedSeedVersion = prefs.getInt(KEY_SEED_VERSION, 0)

            if (savedSeedVersion < SEED_VERSION || currentSeedCount != seedRecipes.size) {
                recipeDao.deleteSeedRecipes()
                recipeDao.insertRecipes(seedRecipes)

                prefs.edit()
                    .putInt(KEY_SEED_VERSION, SEED_VERSION)
                    .apply()
            }

            seedChecked = true
        }
    }

    private fun parseSeedRecipes(): List<RecipeEntity> {
        appContext.assets.open(RECIPES_ASSET_NAME).use { inputStream ->
            val jsonText: String = inputStream.bufferedReader(Charsets.UTF_8).readText()
            val jsonArray = JSONArray(jsonText)
            return List(jsonArray.length()) { index ->
                jsonArray.getJSONObject(index).toSeedRecipeEntity(index)
            }
        }
    }

    private fun org.json.JSONObject.toSeedRecipeEntity(index: Int): RecipeEntity {
        val rawId: String = optInt("id", index + 1).toString()
        return RecipeSeedResolver.buildSeedRecipeEntity(
            RecipeSeedResolver.SeedRecipeInput(
                jsonObject = this,
                index = index,
                now = System.currentTimeMillis(),
                imageResId = imageForSeedRecipe(rawId)
            )
        )
    }

    private fun RecipeEntity.toUiModel(isFavorite: Boolean): RecipeUiModel {
        return RecipePresentationResolver.toUiModel(
            entity = this,
            isFavorite = isFavorite,
            imageResId = resolvedImageResId()
        )
    }


    private fun RecipeEntity.resolvedImageResId(): Int {
        return RecipePresentationResolver.resolvedImageResId(
            entity = this,
            resourceIdForRawId = ::recipeImageResourceIdOrNull,
            defaultImageResId = R.drawable.profile_avatar_04
        )
    }

    private fun imageForSeedRecipe(rawId: String): Int {
        return RecipePresentationResolver.imageResIdForRawId(
            rawId = rawId,
            resourceIdForRawId = ::recipeImageResourceIdOrNull,
            defaultImageResId = R.drawable.profile_avatar_04
        )
    }

    private fun imageForRecipeIdOrDefault(recipeId: String): Int {
        return RecipePresentationResolver.imageResIdForRecipeId(
            recipeId = recipeId,
            resourceIdForRawId = ::recipeImageResourceIdOrNull,
            defaultImageResId = R.drawable.profile_avatar_04
        )
    }

    private fun recipeImageResourceIdOrNull(rawId: String): Int? {
        val resourceName = RecipePresentationResolver.recipeImageResourceName(rawId) ?: return null

        val resourceId = appContext.resources.getIdentifier(
            resourceName,
            "drawable",
            appContext.packageName
        )

        return resourceId.takeIf { it != 0 }
    }



    companion object {
        private const val RECIPES_ASSET_NAME = "recipes.json"
        private const val LOCAL_USER_UID = "local_user"

        private const val SEED_VERSION = 3
        private const val SEED_PREFS_NAME = "recipe_seed_prefs"
        private const val KEY_SEED_VERSION = "seed_version"

        const val RECIPE_SOURCE_TAG_APP = "app_recipe"
        const val RECIPE_SOURCE_TAG_COMMUNITY = "community_recipe"

        private val seedLock = Any()

        @Volatile private var seedChecked: Boolean = false

        private val favoriteIds: MutableSet<String> = mutableSetOf()
        private val communitySharedPostIds: MutableSet<String> = mutableSetOf()

    }
}