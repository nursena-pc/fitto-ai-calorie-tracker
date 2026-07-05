package com.example.fitto.feature_recipes

import com.example.fitto.feature_recipes.model.RecipeFilterOption
import com.example.fitto.feature_recipes.model.RecipeFilterState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeFilterControllerTest {

    @Test
    fun initialState_isPreservedAndActiveFilterStateIsExposed() {
        val initial = RecipeFilterState(
            selectedMainCategoryKey = "ana yemek",
            selectedDietKeys = setOf("vegan")
        )

        val controller = RecipeFilterController(initial)

        assertEquals(initial, controller.state)
        assertTrue(controller.hasActiveFilters)
    }

    @Test
    fun clear_resetsEveryFilter() {
        val controller = RecipeFilterController(
            RecipeFilterState(
                selectedMainCategoryKey = "ana yemek",
                selectedIncludedIngredientKeys = setOf("domates")
            )
        )

        val state = controller.clear()

        assertEquals(RecipeFilterState(), state)
        assertFalse(controller.hasActiveFilters)
    }

    @Test
    fun selectingMainCategory_clearsPreviousSubcategory() {
        val controller = RecipeFilterController(
            RecipeFilterState(
                selectedMainCategoryKey = "çorba",
                selectedSubcategoryKey = "mercimek"
            )
        )

        val state = controller.selectMainCategory("ana yemek")

        assertEquals("ana yemek", state.selectedMainCategoryKey)
        assertNull(state.selectedSubcategoryKey)
    }

    @Test
    fun selectionMethods_updateOnlyTheirOwnFilterGroups() {
        val controller = RecipeFilterController()

        controller.selectSubcategory("sebze")
        controller.selectMainIngredients(setOf("tavuk"))
        controller.selectDiets(setOf("protein"))
        controller.selectMethods(setOf("fırın"))
        controller.selectDifficulty("kolay")
        controller.selectDuration("30")

        val state = controller.state
        assertEquals("sebze", state.selectedSubcategoryKey)
        assertEquals(setOf("tavuk"), state.selectedMainIngredientKeys)
        assertEquals(setOf("protein"), state.selectedDietKeys)
        assertEquals(setOf("fırın"), state.selectedMethodKeys)
        assertEquals("kolay", state.selectedDifficultyKey)
        assertEquals("30", state.selectedDurationKey)
    }

    @Test
    fun includePicker_excludesCurrentlyExcludedIngredients() {
        val controller = controllerWithOptions(
            initialState = RecipeFilterState(
                selectedIncludedIngredientKeys = setOf("domates"),
                selectedExcludedIngredientKeys = setOf("sogan")
            )
        )

        val picker = controller.ingredientPickerState(RecipeIngredientFilterMode.INCLUDE)

        assertEquals(setOf("domates"), picker.selectedKeys)
        assertEquals(listOf("domates", "biber"), picker.availableOptions.map { it.key })
    }

    @Test
    fun excludePicker_excludesCurrentlyIncludedIngredients() {
        val controller = controllerWithOptions(
            initialState = RecipeFilterState(
                selectedIncludedIngredientKeys = setOf("domates"),
                selectedExcludedIngredientKeys = setOf("sogan")
            )
        )

        val picker = controller.ingredientPickerState(RecipeIngredientFilterMode.EXCLUDE)

        assertEquals(setOf("sogan"), picker.selectedKeys)
        assertEquals(listOf("sogan", "biber"), picker.availableOptions.map { it.key })
    }

    @Test
    fun applyingIncludeSelection_removesOverlapFromExcludedSelection() {
        val controller = controllerWithOptions(
            initialState = RecipeFilterState(
                selectedExcludedIngredientKeys = setOf("sogan", "biber")
            )
        )

        val state = controller.applyIngredientSelection(
            mode = RecipeIngredientFilterMode.INCLUDE,
            selectedKeys = setOf("domates", "biber")
        )

        assertEquals(setOf("domates", "biber"), state.selectedIncludedIngredientKeys)
        assertEquals(setOf("sogan"), state.selectedExcludedIngredientKeys)
    }

    @Test
    fun applyingExcludeSelection_removesOverlapFromIncludedSelection() {
        val controller = controllerWithOptions(
            initialState = RecipeFilterState(
                selectedIncludedIngredientKeys = setOf("domates", "biber")
            )
        )

        val state = controller.applyIngredientSelection(
            mode = RecipeIngredientFilterMode.EXCLUDE,
            selectedKeys = setOf("sogan", "biber")
        )

        assertEquals(setOf("sogan", "biber"), state.selectedExcludedIngredientKeys)
        assertEquals(setOf("domates"), state.selectedIncludedIngredientKeys)
    }

    @Test
    fun ingredientSearch_matchesNormalizedLabelsAndKeys() {
        val controller = controllerWithOptions()
        val options = controller.ingredientPickerState(RecipeIngredientFilterMode.INCLUDE).availableOptions

        val labelMatch = controller.filterIngredientOptions(options, "SOĞAN")
        val keyMatch = controller.filterIngredientOptions(options, "biber")

        assertEquals(listOf("sogan"), labelMatch.map { it.key })
        assertEquals(listOf("biber"), keyMatch.map { it.key })
    }

    @Test
    fun blankIngredientSearch_preservesAvailableOptionOrder() {
        val controller = controllerWithOptions()
        val options = controller.ingredientPickerState(RecipeIngredientFilterMode.INCLUDE).availableOptions

        val result = controller.filterIngredientOptions(options, "   ")

        assertEquals(options, result)
    }

    @Test
    fun removingIngredient_changesOnlyRequestedSelection() {
        val controller = controllerWithOptions(
            initialState = RecipeFilterState(
                selectedIncludedIngredientKeys = setOf("domates", "biber"),
                selectedExcludedIngredientKeys = setOf("sogan")
            )
        )

        controller.removeIngredient(RecipeIngredientFilterMode.INCLUDE, "domates")

        assertEquals(setOf("biber"), controller.state.selectedIncludedIngredientKeys)
        assertEquals(setOf("sogan"), controller.state.selectedExcludedIngredientKeys)
    }

    @Test
    fun selectedIngredientOptions_useCatalogLabelsAndFallbackToKey() {
        val controller = controllerWithOptions(
            initialState = RecipeFilterState(
                selectedIncludedIngredientKeys = linkedSetOf("domates", "bilinmeyen")
            )
        )

        val options = controller.selectedIngredientOptions(RecipeIngredientFilterMode.INCLUDE)

        assertEquals(listOf("Domates", "bilinmeyen"), options.map { it.label })
    }

    private fun controllerWithOptions(
        initialState: RecipeFilterState = RecipeFilterState()
    ): RecipeFilterController {
        return RecipeFilterController(initialState).apply {
            setIngredientOptions(
                listOf(
                    RecipeFilterOption("domates", "Domates"),
                    RecipeFilterOption("sogan", "Soğan"),
                    RecipeFilterOption("biber", "Biber")
                )
            )
        }
    }
}
