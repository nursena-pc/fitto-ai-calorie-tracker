package com.example.fitto.feature_recipes.data

object RecipeRepositoryStateResolver {

    enum class FavoriteToggleDecision {
        ADD_TO_FAVORITES,
        REMOVE_FROM_FAVORITES
    }

    enum class BookRemovalDecision {
        REMOVE_FAVORITE_ONLY,
        DELETE_OWNED_RECIPE
    }

    data class SeedRefreshInput(
        val savedSeedVersion: Int,
        val requiredSeedVersion: Int,
        val currentSeedCount: Int,
        val expectedSeedCount: Int
    )

    fun favoriteToggleDecision(
        recipeId: String,
        favoriteIds: Set<String>
    ): FavoriteToggleDecision {
        return if (recipeId in favoriteIds) {
            FavoriteToggleDecision.REMOVE_FROM_FAVORITES
        } else {
            FavoriteToggleDecision.ADD_TO_FAVORITES
        }
    }

    fun bookRemovalDecision(sourceType: String?): BookRemovalDecision {
        return if (sourceType == SOURCE_TYPE_SEED) {
            BookRemovalDecision.REMOVE_FAVORITE_ONLY
        } else {
            BookRemovalDecision.DELETE_OWNED_RECIPE
        }
    }

    fun shouldRefreshSeedRecipes(input: SeedRefreshInput): Boolean {
        return input.savedSeedVersion < input.requiredSeedVersion ||
                input.currentSeedCount != input.expectedSeedCount
    }

    private const val SOURCE_TYPE_SEED = "SEED"
}
