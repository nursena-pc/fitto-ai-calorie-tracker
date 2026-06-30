package com.example.fitto.feature_meal

import com.example.fitto.feature_meal.model.SelectedFoodUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AddMealSelectionStateResolverTest {

    private val resolver = AddMealSelectionStateResolver()

    @Test
    fun `add selected food appends to current selection`() {
        val current = listOf(selectedFood(id = "one"))
        val added = selectedFood(id = "two")

        val result = resolver.addSelectedFood(current, added)

        assertEquals(listOf("one", "two"), result.map { it.id })
    }

    @Test
    fun `add detected foods keeps current selection when detected list is empty`() {
        val current = listOf(selectedFood(id = "existing"))

        val result = resolver.addDetectedFoods(
            currentSelection = current,
            detectedFoods = emptyList()
        )

        assertEquals(current, result)
    }

    @Test
    fun `add detected foods appends all detected selections`() {
        val result = resolver.addDetectedFoods(
            currentSelection = listOf(selectedFood(id = "manual")),
            detectedFoods = listOf(selectedFood(id = "camera-one"), selectedFood(id = "camera-two"))
        )

        assertEquals(listOf("manual", "camera-one", "camera-two"), result.map { it.id })
    }

    @Test
    fun `remove selected food removes all rows matching selected row id`() {
        val result = resolver.removeSelectedFood(
            currentSelection = listOf(
                selectedFood(id = "keep", foodId = "banana"),
                selectedFood(id = "remove", foodId = "oat"),
                selectedFood(id = "remove", foodId = "oat-copy")
            ),
            food = selectedFood(id = "remove", foodId = "different")
        )

        assertEquals(listOf("keep"), result.map { it.id })
    }

    @Test
    fun `can save only when selection is not empty`() {
        assertFalse(resolver.canSave(emptyList()))
        assertTrue(resolver.canSave(listOf(selectedFood())))
    }

    @Test
    fun `empty selection state hides selected list and disables save button`() {
        val state = resolver.resolveUiState(emptyList())

        assertEquals(0, state.selectedCount)
        assertEquals(0, state.totalCalories)
        assertFalse(state.selectedListVisible)
        assertTrue(state.emptyStateVisible)
        assertFalse(state.saveButtonEnabled)
        assertEquals(0.55f, state.saveButtonAlpha)
    }

    @Test
    fun `non empty selection state sums calories and enables save button`() {
        val state = resolver.resolveUiState(
            listOf(
                selectedFood(id = "banana", calories = 90),
                selectedFood(id = "oat", calories = 150)
            )
        )

        assertEquals(2, state.selectedCount)
        assertEquals(240, state.totalCalories)
        assertTrue(state.selectedListVisible)
        assertFalse(state.emptyStateVisible)
        assertTrue(state.saveButtonEnabled)
        assertEquals(1f, state.saveButtonAlpha)
    }

    private fun selectedFood(
        id: String = "selected-banana",
        foodId: String = "banana",
        calories: Int = 90
    ): SelectedFoodUiModel {
        return SelectedFoodUiModel(
            id = id,
            foodId = foodId,
            name = "Banana",
            amountLabel = "100 g",
            amountGrams = 100.0,
            calories = calories,
            protein = 1.0,
            carbs = 22.0,
            fat = 0.3,
            emoji = "🍌"
        )
    }
}
