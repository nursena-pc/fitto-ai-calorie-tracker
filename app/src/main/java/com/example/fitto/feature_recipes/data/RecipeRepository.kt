package com.example.fitto.feature_recipes.data

import android.content.Context
import com.example.fitto.R
import com.example.fitto.data.local.FittoDatabaseProvider
import com.example.fitto.data.local.dao.RecipeDao
import com.example.fitto.data.local.entity.RecipeEntity
import com.example.fitto.feature_recipes.model.RecipeFilterOption
import com.example.fitto.feature_recipes.model.RecipeFilterState
import com.example.fitto.feature_recipes.model.RecipeUiModel
import com.example.fitto.feature_recipes.util.normalizeRecipeText
import com.example.fitto.feature_home.presentation.social.SocialRepository
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale


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
        return entities.map { entity -> entity.toUiModel(isFavorite = entity.id in favoriteIds) }
    }

    fun userRecipes(): List<RecipeUiModel> {
        ensureSeeded()
        return recipeDao.getUserRecipes(currentOwnerUid())
            .map { entity -> entity.toUiModel(isFavorite = entity.id in favoriteIds) }
    }

    fun communityRecipes(): List<RecipeUiModel> {
        ensureSeeded()
        return recipeDao.getCommunityRecipes(currentOwnerUid())
            .map { entity -> entity.toUiModel(isFavorite = entity.id in favoriteIds) }
    }

    fun favoriteOriginalRecipes(): List<RecipeUiModel> {
        ensureSeeded()
        return recipeDao.getVisibleRecipes(currentOwnerUid())
            .filter { entity ->
                entity.id in favoriteIds && entity.sourceType == RecipeEntity.SOURCE_TYPE_SEED
            }
            .map { entity -> entity.toUiModel(isFavorite = true) }
    }

    fun addUserRecipe(draft: UserRecipeDraft): String {
        ensureSeeded()

        val now = System.currentTimeMillis()
        val ownerUid = currentOwnerUid()
        val cleanTitle = draft.title.trim().ifBlank { "Yeni Tarif" }
        val cleanIngredients = draft.ingredients.map { it.trim() }.filter { it.isNotBlank() }
        val cleanSteps = draft.steps.map { it.trim() }.filter { it.isNotBlank() }

        val recipeId = "user_${ownerUid}_${now}"
            .replace("/", "_")
            .replace(".", "_")
            .replace("#", "_")
            .replace("$", "_")
            .replace("[", "_")
            .replace("]", "_")

        val entity = RecipeEntity(
            id = recipeId,
            ownerUid = ownerUid,
            sourceType = RecipeEntity.SOURCE_TYPE_USER,
            sourceRecipeId = null,
            sourceUserId = null,
            sourcePostId = null,
            title = cleanTitle,
            description = draft.description.trim().ifBlank {
                "Kullanıcı tarafından eklenen tarif"
            },
            mainCategoryKey = draft.mainCategoryKey,
            subcategoryKey = "",
            subcategoryKeys = emptySet(),
            mainIngredientKeys = emptySet(),
            dietKeys = emptySet(),
            methodKeys = emptySet(),
            durationMinutes = draft.durationLabel.toRepresentativeMinutes(),
            durationLabel = draft.durationLabel,
            difficultyKey = draft.difficultyKey,
            calories = 0,
            imageResId = R.drawable.profile_avatar_04,
            servings = 1,
            servingDisplay = "1 porsiyon",
            proteinGrams = 0,
            carbGrams = 0,
            fatGrams = 0,
            ingredientNames = cleanIngredients.map { it.substringBefore(" - ").trim() },
            ingredients = cleanIngredients,
            steps = cleanSteps,
            tip = null,
            createdAt = now,
            updatedAt = now
        )

        recipeDao.upsertRecipe(entity)
        return recipeId
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
        val ownerUid = currentOwnerUid()
        val cleanIngredients: List<String> = ingredientsOverride
            ?.map { ingredient -> ingredient.trim() }
            ?.filter { ingredient -> ingredient.isNotBlank() }
            ?: source.ingredients

        val copyId = "copied_${ownerUid}_${source.id}_${now}".toSafeRecipeId()
        val sourceId = source.sourceRecipeId ?: source.id

        val copiedEntity = source.copy(
            id = copyId,
            ownerUid = ownerUid,
            sourceType = RecipeEntity.SOURCE_TYPE_COPIED,
            sourceRecipeId = sourceId,
            title = if (ingredientsOverride == null) source.title else "${source.title} (Bana Özel)",
            imageResId = source.resolvedImageResId(),
            ingredients = cleanIngredients,
            ingredientNames = if (ingredientsOverride == null) {
                source.ingredientNames
            } else {
                cleanIngredients.map { ingredientLine -> ingredientLine.toIngredientNameForCopy() }
            },
            calories = source.calories,
            proteinGrams = source.proteinGrams,
            carbGrams = source.carbGrams,
            fatGrams = source.fatGrams,
            createdAt = now,
            updatedAt = now
        )

        recipeDao.upsertRecipe(copiedEntity)
        return copyId
    }


    suspend fun shareRecipeToCommunity(recipeId: String, ingredientsOverride: List<String>? = null, comment: String = ""): String? {
        ensureSeeded()

        val source: RecipeEntity = recipeDao.findById(recipeId) ?: return null
        val cleanIngredients: List<String> = ingredientsOverride
            ?.map { ingredient -> ingredient.trim() }
            ?.filter { ingredient -> ingredient.isNotBlank() }
            ?: source.ingredients

        val content = buildString {
            val cleanComment = comment.trim()
            if (cleanComment.isNotBlank()) {
                append(cleanComment)
                append("\n\n")
            }
            append("Denediğim tarifi toplulukla paylaşıyorum: ")
            append(source.title)
            if (cleanIngredients.isNotEmpty()) {
                append("\n\nMalzemeler: ")
                append(cleanIngredients.take(8).joinToString(", ") { it.toIngredientNameForCopy() })
            }
        }

        val recipeSourceTag = if (source.sourceType == RecipeEntity.SOURCE_TYPE_SEED) {
            RECIPE_SOURCE_TAG_APP
        } else {
            RECIPE_SOURCE_TAG_COMMUNITY
        }

        val postId = SocialRepository().createRecipePost(
            content = content,
            recipeId = source.id,
            recipeTitle = source.title,
            recipeImageResId = source.resolvedImageResId(),
            recipeDuration = source.durationLabel,
            recipeServing = source.servingDisplay.ifBlank { "${source.servings} kişilik" },
            recipeCalories = source.calories.takeIf { it > 0 },
            recipeCategory = source.mainCategoryKey,
            tags = listOf("Tarif", source.mainCategoryKey),
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
                durationMinutes = values.durationLabel.toRepresentativeMinutes(),
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
                durationMinutes = values.durationLabel.toRepresentativeMinutes(),
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
        return recipes()
            .flatMap { recipe -> recipe.ingredientNames }
            .mapNotNull { ingredientName -> ingredientName.toIngredientOptionOrNull() }
            .distinctBy { option -> option.key }
            .sortedBy { option -> option.label.normalizeRecipeText() }
    }

    fun filterRecipes(
        query: String,
        filterState: RecipeFilterState
    ): List<RecipeUiModel> {
        val normalizedQuery: String = query.normalizeRecipeText()

        return recipes().filter { recipe: RecipeUiModel ->
            val queryMatches: Boolean = normalizedQuery.isBlank() ||
                    recipe.searchableText().contains(normalizedQuery)

            val categoryMatches: Boolean = filterState.selectedMainCategoryKey
                ?.let { selectedCategory -> recipe.mainCategoryKey == selectedCategory }
                ?: true

            val subcategoryMatches: Boolean = filterState.selectedSubcategoryKey
                ?.let { selectedSubcategory -> selectedSubcategory in recipe.subcategoryKeys }
                ?: true

            val ingredientMatches: Boolean = filterState.selectedMainIngredientKeys.isEmpty() ||
                    recipe.mainIngredientKeys.any { ingredientKey -> ingredientKey in filterState.selectedMainIngredientKeys }

            val dietMatches: Boolean = filterState.selectedDietKeys.isEmpty() ||
                    filterState.selectedDietKeys.all { dietKey -> dietKey in recipe.dietKeys }

            val methodMatches: Boolean = filterState.selectedMethodKeys.isEmpty() ||
                    recipe.methodKeys.any { methodKey -> methodKey in filterState.selectedMethodKeys }

            val durationMatches: Boolean = filterState.selectedDurationKey
                ?.let { selectedDuration -> recipe.durationLabel == selectedDuration }
                ?: true

            val difficultyMatches: Boolean = filterState.selectedDifficultyKey
                ?.let { selectedDifficulty -> recipe.difficultyKey == selectedDifficulty }
                ?: true

            val ingredientSearchPool: List<String> = recipe.ingredientNames.map { ingredientName ->
                ingredientName.normalizeRecipeText()
            }

            val includedIngredientMatches: Boolean =
                filterState.selectedIncludedIngredientKeys.isEmpty() ||
                        filterState.selectedIncludedIngredientKeys.all { selectedIngredientKey ->
                            ingredientSearchPool.any { ingredient -> ingredient.contains(selectedIngredientKey) }
                        }

            val excludedIngredientMatches: Boolean =
                filterState.selectedExcludedIngredientKeys.isEmpty() ||
                        filterState.selectedExcludedIngredientKeys.none { selectedIngredientKey ->
                            ingredientSearchPool.any { ingredient -> ingredient.contains(selectedIngredientKey) }
                        }

            queryMatches &&
                    categoryMatches &&
                    subcategoryMatches &&
                    ingredientMatches &&
                    dietMatches &&
                    methodMatches &&
                    durationMatches &&
                    difficultyMatches &&
                    includedIngredientMatches &&
                    excludedIngredientMatches
        }
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

    private fun JSONObject.toSeedRecipeEntity(index: Int): RecipeEntity {
        val labels: JSONObject = optJSONObject("labels") ?: JSONObject()
        val rawId: String = optInt("id", index + 1).toString()
        val recipeId = "seed_$rawId"
        val title: String = optString("tarif_adi", "Tarif $rawId").trim()
        val mainCategory: String = labels.optString("ana_kategori", optString("kategori", "")).trim()
        val subcategoryKeys: Set<String> = labels.optStringArray("alt_kategori_etiketleri").toSet()
        val mainIngredientKeys: Set<String> = labels.optStringArray("ana_malzeme_etiketleri").toSet()
        val dietKeys: Set<String> = labels.optStringArray("diyet_etiketleri").toSet()
        val methodKeys: Set<String> = optStringArray("hazirlama_yontemi").toSet()
        val durationLabel: String = optString("sure_etiketi", "").ifBlank { "15-30 dk" }
        val servingDisplay: String = optString("porsiyon_gosterim", "").ifBlank {
            val portion: Int = optInt("porsiyon", 1)
            "$portion kişilik"
        }
        val servings: Int = optInt("porsiyon_min", optInt("porsiyon", 1)).coerceAtLeast(1)
        val ingredients: List<RecipeIngredient> = optIngredientArray("malzemeler")
        val ingredientNames: List<String> = ingredients.map { ingredient -> ingredient.name }
        val ingredientLines: List<String> = ingredients.map { ingredient -> ingredient.toDisplayText() }
        val steps: List<String> = optStringArray("yapilis_adimlari")
        val description: String = buildDescription(
            mainCategory = mainCategory,
            durationLabel = durationLabel,
            servingDisplay = servingDisplay,
            subcategories = subcategoryKeys
        )
        val now = System.currentTimeMillis()

        return RecipeEntity(
            id = recipeId,
            ownerUid = null,
            sourceType = RecipeEntity.SOURCE_TYPE_SEED,
            sourceRecipeId = rawId,
            sourceUserId = null,
            sourcePostId = null,
            title = title,
            description = description,
            mainCategoryKey = mainCategory,
            subcategoryKey = subcategoryKeys.firstOrNull().orEmpty(),
            subcategoryKeys = subcategoryKeys,
            mainIngredientKeys = mainIngredientKeys,
            dietKeys = dietKeys,
            methodKeys = methodKeys,
            durationMinutes = durationLabel.toRepresentativeMinutes(),
            durationLabel = durationLabel,
            difficultyKey = optString("zorluk", "kolay").ifBlank { "kolay" },
            calories = 0,
            imageResId = imageForSeedRecipe(rawId),
            servings = servings,
            servingDisplay = servingDisplay,
            proteinGrams = 0,
            carbGrams = 0,
            fatGrams = 0,
            ingredientNames = ingredientNames,
            ingredients = ingredientLines,
            steps = steps,
            tip = labels.optStringArray("kontrol_notlari").firstOrNull(),
            createdAt = now,
            updatedAt = now
        )
    }

    private fun RecipeEntity.toUiModel(isFavorite: Boolean): RecipeUiModel {
        return RecipeUiModel(
            id = id,
            title = title,
            description = description,
            mainCategoryKey = mainCategoryKey,
            subcategoryKey = subcategoryKey,
            subcategoryKeys = subcategoryKeys,
            mainIngredientKeys = mainIngredientKeys,
            dietKeys = dietKeys,
            methodKeys = methodKeys,
            durationMinutes = durationMinutes,
            durationLabel = durationLabel,
            difficultyKey = difficultyKey,
            calories = calories,
            imageResId = resolvedImageResId(),
            servings = servings,
            servingDisplay = servingDisplay,
            proteinGrams = proteinGrams,
            carbGrams = carbGrams,
            fatGrams = fatGrams,
            ingredientNames = ingredientNames,
            ingredients = ingredients,
            steps = steps,
            tip = tip,
            isFavorite = isFavorite
        )
    }


    private fun String.toSafeRecipeId(): String {
        return replace("/", "_")
            .replace(".", "_")
            .replace("#", "_")
            .replace("$", "_")
            .replace("[", "_")
            .replace("]", "_")
    }

    private fun String.toIngredientNameForCopy(): String {
        val tokens = trim().split(Regex("\\s+")).filter { token -> token.isNotBlank() }
        if (tokens.isEmpty()) return trim()

        var nameStart = 0
        if (tokens.first().any { char -> char.isDigit() } || "/" in tokens.first()) {
            nameStart = 1
            while (nameStart < tokens.size && tokens[nameStart].lowercase(TURKISH_LOCALE) in INGREDIENT_AMOUNT_WORDS && nameStart < 5) {
                nameStart++
            }
        }

        return tokens.drop(nameStart).joinToString(" ").ifBlank { trim() }
    }

    private fun buildDescription(
        mainCategory: String,
        durationLabel: String,
        servingDisplay: String,
        subcategories: Set<String>
    ): String {
        val readableCategory: String = mainCategory.replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase(TURKISH_LOCALE) else char.toString()
        }
        val firstSubcategory: String? = subcategories.firstOrNull()
        val prefix: String = firstSubcategory?.replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase(TURKISH_LOCALE) else char.toString()
        } ?: readableCategory
        return "$prefix • $durationLabel • $servingDisplay"
    }

    private fun RecipeEntity.resolvedImageResId(): Int {
        val rawSeedId: String? = when {
            sourceType == RecipeEntity.SOURCE_TYPE_SEED -> sourceRecipeId ?: id.removePrefix("seed_")
            sourceRecipeId?.toIntOrNull() != null -> sourceRecipeId
            id.startsWith("seed_") -> id.removePrefix("seed_")
            else -> null
        }

        if (rawSeedId != null) {
            return recipeImageResourceIdOrNull(rawSeedId) ?: R.drawable.profile_avatar_04
        }

        return imageResId.takeIf { it != 0 } ?: R.drawable.profile_avatar_04
    }

    private fun imageForSeedRecipe(rawId: String): Int {
        return recipeImageResourceIdOrNull(rawId) ?: R.drawable.profile_avatar_04
    }

    private fun imageForRecipeIdOrDefault(recipeId: String): Int {
        val cleanRecipeId = recipeId.trim()
        val rawId = when {
            cleanRecipeId.startsWith("seed_") -> cleanRecipeId.removePrefix("seed_")
            cleanRecipeId.toIntOrNull() != null -> cleanRecipeId
            else -> null
        }

        return rawId
            ?.let { id -> recipeImageResourceIdOrNull(id) }
            ?: R.drawable.profile_avatar_04
    }

    private fun recipeImageResourceIdOrNull(rawId: String): Int? {
        val numericId: Int = rawId.toIntOrNull() ?: return null
        val resourceName = "recipe_${numericId.toString().padStart(3, '0')}"

        val resourceId = appContext.resources.getIdentifier(
            resourceName,
            "drawable",
            appContext.packageName
        )

        return resourceId.takeIf { it != 0 }
    }

    private fun JSONObject.optStringArray(key: String): List<String> {
        val array: JSONArray = optJSONArray(key) ?: return emptyList()
        return List(array.length()) { index -> array.optString(index).trim() }
            .filter { value -> value.isNotBlank() }
    }

    private fun JSONObject.optIngredientArray(key: String): List<RecipeIngredient> {
        val array: JSONArray = optJSONArray(key) ?: return emptyList()
        return List(array.length()) { index ->
            val ingredientObject: JSONObject = array.optJSONObject(index) ?: JSONObject()
            RecipeIngredient(
                name = ingredientObject.optString("isim", "").trim(),
                amount = ingredientObject.readAmountText(),
                unit = ingredientObject.optNullableString("birim")
            )
        }.filter { ingredient -> ingredient.name.isNotBlank() }
    }

    private fun JSONObject.readAmountText(): String? {
        if (!has("miktar") || isNull("miktar")) return null
        val rawValue: Any = opt("miktar") ?: return null
        return when (rawValue) {
            is Int -> rawValue.toString()
            is Long -> rawValue.toString()
            is Double -> rawValue.toReadableNumber()
            is Number -> rawValue.toDouble().toReadableNumber()
            else -> rawValue.toString().takeIf { value -> value.isNotBlank() }
        }
    }

    private fun JSONObject.optNullableString(key: String): String? {
        if (!has(key) || isNull(key)) return null
        return optString(key).trim().takeIf { value -> value.isNotBlank() }
    }

    private fun Double.toReadableNumber(): String {
        return if (this % 1.0 == 0.0) toInt().toString() else toString().trimEnd('0').trimEnd('.')
    }

    private fun String.toRepresentativeMinutes(): Int {
        return when (this) {
            "15 dk altı" -> 10
            "15-30 dk" -> 25
            "30-60 dk" -> 45
            "60 dk üzeri" -> 75
            else -> 30
        }
    }

    private fun RecipeUiModel.searchableText(): String {
        return buildString {
            append(title).append(' ')
            append(description).append(' ')
            append(mainCategoryKey).append(' ')
            append(subcategoryKeys.joinToString(" ")).append(' ')
            append(mainIngredientKeys.joinToString(" ")).append(' ')
            append(dietKeys.joinToString(" ")).append(' ')
            append(methodKeys.joinToString(" ")).append(' ')
            append(ingredientNames.joinToString(" ")).append(' ')
            append(ingredients.joinToString(" ")).append(' ')
            append(steps.joinToString(" ")).append(' ')
            append(durationLabel).append(' ')
            append(durationMinutes).append(" dk ")
            append(servingDisplay).append(' ')
            append(difficultyKey).append(' ')
            append(tip.orEmpty())
        }.normalizeRecipeText()
    }

    private fun String.toIngredientOptionOrNull(): RecipeFilterOption? {
        val label: String = substringBefore(" - ")
            .substringBefore("(")
            .trim()
            .replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase(TURKISH_LOCALE) else char.toString()
            }
        if (label.isBlank()) return null
        return RecipeFilterOption(key = label.normalizeRecipeText(), label = label)
    }

    private data class RecipeIngredient(
        val name: String,
        val amount: String?,
        val unit: String?
    ) {
        fun toDisplayText(): String {
            val amountPart: String = listOfNotNull(amount, unit).joinToString(" ").trim()
            return if (amountPart.isBlank()) name else "$amountPart $name"
        }
    }

    companion object {
        private const val RECIPES_ASSET_NAME = "recipes.json"
        private const val LOCAL_USER_UID = "local_user"

        private const val SEED_VERSION = 3
        private const val SEED_PREFS_NAME = "recipe_seed_prefs"
        private const val KEY_SEED_VERSION = "seed_version"

        const val RECIPE_SOURCE_TAG_APP = "app_recipe"
        const val RECIPE_SOURCE_TAG_COMMUNITY = "community_recipe"

        private val TURKISH_LOCALE = Locale("tr", "TR")
        private val seedLock = Any()

        @Volatile private var seedChecked: Boolean = false

        private val favoriteIds: MutableSet<String> = mutableSetOf()
        private val communitySharedPostIds: MutableSet<String> = mutableSetOf()

        private val INGREDIENT_AMOUNT_WORDS = setOf(
            "adet", "g", "gr", "gram", "kg", "ml", "lt", "litre", "su", "bardağı", "bardak",
            "küçük", "yemek", "tatlı", "çay", "kaşığı", "kaşık", "dilim", "demet", "tutam",
            "avuç", "paket", "kase", "diş"
        )
    }
}