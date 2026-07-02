package com.example.fitto.feature_meal

import com.example.fitto.data.local.entity.FoodEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AddMealDetectedFoodsActionTest {

    @Test
    fun `empty food ids return no foods without repository access`() = runBlocking {
        var repositoryCalls = 0
        val action = action { _ ->
            repositoryCalls += 1
            food(id = "unexpected")
        }

        val result = action.load(AddMealDetectedFoodsRequest(emptyList()))

        assertSame(AddMealDetectedFoodsResult.NoFoods, result)
        assertEquals(0, repositoryCalls)
    }

    @Test
    fun `recognized food ids are loaded in order and missing foods are skipped`() = runBlocking {
        val requestedIds = mutableListOf<String>()
        val foodsById = mapOf(
            "banana" to food(id = "banana", name = "Muz"),
            "chicken" to food(id = "chicken", name = "Tavuk")
        )
        val action = action { foodId ->
            requestedIds += foodId
            foodsById[foodId]
        }

        val result = action.load(
            AddMealDetectedFoodsRequest(
                foodIds = listOf("banana", "missing", "chicken")
            )
        ) as AddMealDetectedFoodsResult.Imported

        assertEquals(listOf("banana", "missing", "chicken"), requestedIds)
        assertEquals(listOf("banana", "chicken"), result.detectedFoods.map { it.foodId })
        assertEquals(listOf("Muz", "Tavuk"), result.detectedFoods.map { it.name })
    }

    @Test
    fun `recognized foods use detected serving presentation`() = runBlocking {
        val action = action { _ ->
            food(
                id = "chicken",
                name = "Tavuk Göğsü",
                servingAmount = 150.0,
                caloriesPer100g = 165.0,
                proteinPer100g = 31.0,
                carbsPer100g = 0.0,
                fatPer100g = 3.6
            )
        }

        val result = action.load(
            AddMealDetectedFoodsRequest(listOf("chicken"))
        ) as AddMealDetectedFoodsResult.Imported
        val selectedFood = result.detectedFoods.single()

        assertEquals("chicken-fixed", selectedFood.id)
        assertEquals("150 g", selectedFood.amountLabel)
        assertEquals(248, selectedFood.calories)
        assertEquals(46.5, selectedFood.protein, 0.01)
        assertEquals(5.4, selectedFood.fat, 0.01)
    }

    @Test
    fun `all missing food ids return no foods`() = runBlocking {
        val action = action { null }

        val result = action.load(
            AddMealDetectedFoodsRequest(listOf("missing-1", "missing-2"))
        )

        assertTrue(result is AddMealDetectedFoodsResult.NoFoods)
    }

    private fun action(
        getFoodById: suspend (String) -> FoodEntity?
    ): AddMealDetectedFoodsAction {
        return AddMealDetectedFoodsAction(
            getFoodById = getFoodById,
            presentationResolver = AddMealFoodPresentationResolver(
                idSuffixProvider = { "fixed" }
            )
        )
    }

    private fun food(
        id: String,
        name: String = id,
        servingAmount: Double = 100.0,
        caloriesPer100g: Double = 100.0,
        proteinPer100g: Double = 10.0,
        carbsPer100g: Double = 15.0,
        fatPer100g: Double = 3.0
    ): FoodEntity {
        return FoodEntity(
            id = id,
            nameTr = name,
            nameEn = null,
            category = "test",
            defaultUnit = "g",
            defaultServingAmount = servingAmount,
            caloriesPer100g = caloriesPer100g,
            proteinPer100g = proteinPer100g,
            carbsPer100g = carbsPer100g,
            fatPer100g = fatPer100g,
            fiberPer100g = null,
            source = "test",
            isVerified = true,
            allergens = "",
            dietTags = "",
            mealTypes = ""
        )
    }
}
