package com.example.fitto.feature_recipes

import android.content.Context
import com.example.fitto.feature_recipes.data.RecipeRepository
import kotlinx.coroutines.CoroutineDispatcher

/** Screen-level dependency graph for recipe browsing, editing and shopping flows. */
class RecipeFeatureDependencies(
    context: Context,
    val ioDispatcher: CoroutineDispatcher
) {
    private val appContext = context.applicationContext

    val repository by lazy { RecipeRepository(appContext) }
    val shoppingListStore: RecipeShoppingListStore by lazy {
        SharedPreferencesRecipeShoppingListStore(appContext)
    }
    val fridgeInventoryStore: FridgeInventoryStore by lazy {
        SharedPreferencesFridgeInventoryStore(appContext)
    }

    fun createListCoordinator(): RecipeListCoordinator {
        return RecipeListCoordinator(
            dataSource = RepositoryRecipeListDataSource(repository),
            ioDispatcher = ioDispatcher
        )
    }

    fun createDetailCoordinator(): RecipeDetailCoordinator {
        return RecipeDetailCoordinator(
            dataSource = LocalRecipeDetailDataSource(repository),
            ioDispatcher = ioDispatcher
        )
    }

    fun createAddCoordinator(): RecipeAddCoordinator {
        return RecipeAddCoordinator(
            dataSource = LocalRecipeAddDataSource(repository),
            ioDispatcher = ioDispatcher
        )
    }

    fun createFilterCoordinator(): RecipeFilterCoordinator {
        return RecipeFilterCoordinator(
            dataSource = LocalRecipeFilterDataSource(repository),
            ioDispatcher = ioDispatcher
        )
    }

    fun createFridgeCoordinator(): FridgeInventoryCoordinator {
        return FridgeInventoryCoordinator(
            dataSource = LocalFridgeRecipeSuggestionDataSource(repository),
            ioDispatcher = ioDispatcher
        )
    }
}
