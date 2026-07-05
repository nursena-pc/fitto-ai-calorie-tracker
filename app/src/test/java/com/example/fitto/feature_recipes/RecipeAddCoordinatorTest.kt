package com.example.fitto.feature_recipes

import com.example.fitto.feature_recipes.data.UserRecipeDraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class RecipeAddCoordinatorTest {

    @Test
    fun saveRecipe_forwardsDraftAndReturnsRecipeId() = runBlocking {
        val draft = UserRecipeDraft(
            title = "Tarif",
            description = "Açıklama",
            mainCategoryKey = "ana yemek",
            durationLabel = "30",
            difficultyKey = "kolay",
            ingredients = listOf("Malzeme"),
            steps = listOf("Adım")
        )
        val source = FakeRecipeAddDataSource("recipe-42")
        val coordinator = RecipeAddCoordinator(source, Dispatchers.Unconfined)

        val result = coordinator.saveRecipe(draft)

        assertEquals(draft, source.savedDraft)
        assertEquals("recipe-42", result)
    }

    private class FakeRecipeAddDataSource(
        private val result: String
    ) : RecipeAddDataSource {

        var savedDraft: UserRecipeDraft? = null

        override fun addUserRecipe(draft: UserRecipeDraft): String {
            savedDraft = draft
            return result
        }
    }
}
