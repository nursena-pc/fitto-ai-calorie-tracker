package com.example.fitto.feature_recipes.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeRepositoryStateResolverTest {

    @Test
    fun favoriteToggleDecision_addsWhenRecipeIsNotFavorite() {
        val decision = RecipeRepositoryStateResolver.favoriteToggleDecision(
            recipeId = "recipe-1",
            favoriteIds = setOf("recipe-2")
        )

        assertEquals(
            RecipeRepositoryStateResolver.FavoriteToggleDecision.ADD_TO_FAVORITES,
            decision
        )
    }

    @Test
    fun favoriteToggleDecision_removesWhenRecipeIsFavorite() {
        val decision = RecipeRepositoryStateResolver.favoriteToggleDecision(
            recipeId = "recipe-1",
            favoriteIds = setOf("recipe-1", "recipe-2")
        )

        assertEquals(
            RecipeRepositoryStateResolver.FavoriteToggleDecision.REMOVE_FROM_FAVORITES,
            decision
        )
    }

    @Test
    fun bookRemovalDecision_keepsSeedRecipesAndOnlyRemovesFavorite() {
        val decision = RecipeRepositoryStateResolver.bookRemovalDecision(
            sourceType = "SEED"
        )

        assertEquals(
            RecipeRepositoryStateResolver.BookRemovalDecision.REMOVE_FAVORITE_ONLY,
            decision
        )
    }

    @Test
    fun bookRemovalDecision_deletesOwnedRecipesForNonSeedOrMissingRecipes() {
        assertEquals(
            RecipeRepositoryStateResolver.BookRemovalDecision.DELETE_OWNED_RECIPE,
            RecipeRepositoryStateResolver.bookRemovalDecision("USER")
        )
        assertEquals(
            RecipeRepositoryStateResolver.BookRemovalDecision.DELETE_OWNED_RECIPE,
            RecipeRepositoryStateResolver.bookRemovalDecision(null)
        )
    }

    @Test
    fun shouldRefreshSeedRecipes_refreshesWhenVersionIsOutdated() {
        assertTrue(
            RecipeRepositoryStateResolver.shouldRefreshSeedRecipes(
                RecipeRepositoryStateResolver.SeedRefreshInput(
                    savedSeedVersion = 2,
                    requiredSeedVersion = 3,
                    currentSeedCount = 10,
                    expectedSeedCount = 10
                )
            )
        )
    }

    @Test
    fun shouldRefreshSeedRecipes_refreshesWhenSeedCountChanged() {
        assertTrue(
            RecipeRepositoryStateResolver.shouldRefreshSeedRecipes(
                RecipeRepositoryStateResolver.SeedRefreshInput(
                    savedSeedVersion = 3,
                    requiredSeedVersion = 3,
                    currentSeedCount = 9,
                    expectedSeedCount = 10
                )
            )
        )
    }

    @Test
    fun shouldRefreshSeedRecipes_skipsWhenVersionAndCountMatch() {
        assertFalse(
            RecipeRepositoryStateResolver.shouldRefreshSeedRecipes(
                RecipeRepositoryStateResolver.SeedRefreshInput(
                    savedSeedVersion = 3,
                    requiredSeedVersion = 3,
                    currentSeedCount = 10,
                    expectedSeedCount = 10
                )
            )
        )
    }
}
