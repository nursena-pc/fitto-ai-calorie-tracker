package com.example.fitto.feature_meal

import com.example.fitto.data.local.entity.FoodEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AddMealCustomFoodActionTest {

    @Test
    fun `create passes request to repository and returns created food`() = runBlocking {
        val request = request(name = "Ev Yoğurdu")
        val expectedFood = food(id = "custom-yogurt", name = "Ev Yoğurdu")
        var capturedRequest: AddMealCustomFoodRequest? = null
        val action = AddMealCustomFoodAction { receivedRequest ->
            capturedRequest = receivedRequest
            expectedFood
        }

        val result = action.create(request)

        assertTrue(result is AddMealCustomFoodResult.Created)
        val created = result as AddMealCustomFoodResult.Created
        assertSame(expectedFood, created.food)
        assertEquals(request, capturedRequest)
    }

    @Test
    fun `create returns failed result when repository throws`() = runBlocking {
        val error = IllegalStateException("custom food failed")
        val action = AddMealCustomFoodAction { throw error }

        val result = action.create(request())

        assertTrue(result is AddMealCustomFoodResult.Failed)
        val failed = result as AddMealCustomFoodResult.Failed
        assertSame(error, failed.throwable)
    }

    @Test
    fun `request keeps custom food values unchanged`() {
        val request = request(
            name = "Protein Pankek",
            servingAmount = 125.0,
            unit = "porsiyon",
            caloriesPer100g = 205.0,
            proteinPer100g = 18.0,
            carbsPer100g = 22.0,
            fatPer100g = 5.0,
            mealType = "breakfast"
        )

        assertEquals("Protein Pankek", request.name)
        assertEquals(125.0, request.servingAmount, 0.0)
        assertEquals("porsiyon", request.unit)
        assertEquals(205.0, request.caloriesPer100g, 0.0)
        assertEquals(18.0, request.proteinPer100g, 0.0)
        assertEquals(22.0, request.carbsPer100g, 0.0)
        assertEquals(5.0, request.fatPer100g, 0.0)
        assertEquals("breakfast", request.mealType)
    }

    private fun request(
        name: String = "Custom Food",
        servingAmount: Double = 100.0,
        unit: String = "g",
        caloriesPer100g: Double = 120.0,
        proteinPer100g: Double = 10.0,
        carbsPer100g: Double = 8.0,
        fatPer100g: Double = 4.0,
        mealType: String = "lunch"
    ): AddMealCustomFoodRequest {
        return AddMealCustomFoodRequest(
            name = name,
            servingAmount = servingAmount,
            unit = unit,
            caloriesPer100g = caloriesPer100g,
            proteinPer100g = proteinPer100g,
            carbsPer100g = carbsPer100g,
            fatPer100g = fatPer100g,
            mealType = mealType
        )
    }

    private fun food(id: String, name: String): FoodEntity {
        return FoodEntity(
            id = id,
            nameTr = name,
            nameEn = null,
            category = "user_custom",
            defaultUnit = "g",
            defaultServingAmount = 100.0,
            caloriesPer100g = 120.0,
            proteinPer100g = 10.0,
            carbsPer100g = 8.0,
            fatPer100g = 4.0,
            fiberPer100g = null,
            source = "user_custom",
            isVerified = false,
            allergens = "",
            dietTags = "custom",
            mealTypes = "lunch",
            ownerUserId = "user",
            createdAt = 1L,
            updatedAt = 1L
        )
    }
}
