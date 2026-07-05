package com.example.fitto.feature_recipes

import com.example.fitto.feature_recipes.model.RecipeFilterOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class RecipeFilterCoordinatorTest {

    @Test
    fun loadIngredientOptions_returnsSourceOptions() = runBlocking {
        val options = listOf(
            RecipeFilterOption("domates", "Domates"),
            RecipeFilterOption("sogan", "Soğan")
        )
        val source = FakeRecipeFilterDataSource(options)
        val coordinator = RecipeFilterCoordinator(
            dataSource = source,
            ioDispatcher = Dispatchers.Unconfined
        )

        val result = coordinator.loadIngredientOptions()

        assertEquals(1, source.loadCount)
        assertEquals(options, result)
    }

    private class FakeRecipeFilterDataSource(
        private val options: List<RecipeFilterOption>
    ) : RecipeFilterDataSource {

        var loadCount: Int = 0

        override fun allIngredientOptions(): List<RecipeFilterOption> {
            loadCount += 1
            return options
        }
    }
}
