package com.example.fitto.feature_meal

import com.example.fitto.feature_meal.model.MealType
import com.example.fitto.feature_meal.model.SelectedFoodUiModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AddMealSaveActionTest {

    @Test
    fun `canSave returns false when selected foods are empty`() {
        val action = AddMealSaveAction { _, _, _ -> }

        assertFalse(action.canSave(emptyList()))
    }

    @Test
    fun `canSave returns true when selected foods exist`() {
        val action = AddMealSaveAction { _, _, _ -> }

        assertTrue(action.canSave(listOf(selectedFood())))
    }

    @Test
    fun `save returns empty selection and skips repository when selected foods are empty`() = runBlocking {
        var saveCallCount = 0
        val action = AddMealSaveAction { _, _, _ -> saveCallCount++ }

        val result = action.save(
            AddMealSaveRequest(
                mealType = MealType.BREAKFAST,
                selectedFoods = emptyList(),
                entryDate = "2026-06-30"
            )
        )

        assertEquals(AddMealSaveResult.EmptySelection, result)
        assertEquals(0, saveCallCount)
    }

    @Test
    fun `save persists selected foods and returns success metadata`() = runBlocking {
        var capturedMealType: MealType? = null
        var capturedFoods: List<SelectedFoodUiModel>? = null
        var capturedEntryDate: String? = null
        val selectedFoods = listOf(selectedFood(id = "one"), selectedFood(id = "two"))
        val action = AddMealSaveAction { mealType, foods, entryDate ->
            capturedMealType = mealType
            capturedFoods = foods
            capturedEntryDate = entryDate
        }

        val result = action.save(
            AddMealSaveRequest(
                mealType = MealType.LUNCH,
                selectedFoods = selectedFoods,
                entryDate = "2026-06-30"
            )
        )

        assertTrue(result is AddMealSaveResult.Saved)
        val saved = result as AddMealSaveResult.Saved
        assertEquals(2, saved.selectedCount)
        assertEquals(MealType.LUNCH, saved.mealType)
        assertEquals("2026-06-30", saved.entryDate)
        assertEquals(MealType.LUNCH, capturedMealType)
        assertEquals(selectedFoods, capturedFoods)
        assertEquals("2026-06-30", capturedEntryDate)
    }

    @Test
    fun `save returns failed result when repository throws`() = runBlocking {
        val error = IllegalStateException("save failed")
        val action = AddMealSaveAction { _, _, _ -> throw error }

        val result = action.save(
            AddMealSaveRequest(
                mealType = MealType.DINNER,
                selectedFoods = listOf(selectedFood()),
                entryDate = "2026-06-30"
            )
        )

        assertTrue(result is AddMealSaveResult.Failed)
        val failed = result as AddMealSaveResult.Failed
        assertSame(error, failed.throwable)
    }

    private fun selectedFood(
        id: String = "selected-banana",
        foodId: String = "banana"
    ): SelectedFoodUiModel {
        return SelectedFoodUiModel(
            id = id,
            foodId = foodId,
            name = "Banana",
            amountLabel = "100 g",
            amountGrams = 100.0,
            calories = 90,
            protein = 1.0,
            carbs = 22.0,
            fat = 0.3,
            emoji = "🍌"
        )
    }
}
