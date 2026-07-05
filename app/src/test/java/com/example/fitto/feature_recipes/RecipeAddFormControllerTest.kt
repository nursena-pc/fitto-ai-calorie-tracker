package com.example.fitto.feature_recipes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeAddFormControllerTest {

    @Test
    fun chipSelections_toggleOnAndOff() {
        val controller = RecipeAddFormController()

        controller.toggleCategory("ana yemek")
        controller.toggleDuration("30")
        controller.toggleDifficulty("kolay")

        assertEquals("ana yemek", controller.state.selectedCategoryKey)
        assertEquals("30", controller.state.selectedDurationKey)
        assertEquals("kolay", controller.state.selectedDifficultyKey)

        controller.toggleCategory("ana yemek")
        controller.toggleDuration("30")
        controller.toggleDifficulty("kolay")

        assertEquals(null, controller.state.selectedCategoryKey)
        assertEquals(null, controller.state.selectedDurationKey)
        assertEquals(null, controller.state.selectedDifficultyKey)
    }

    @Test
    fun invalidSubmission_reportsEveryMissingRequiredField() {
        val controller = RecipeAddFormController()

        val result = controller.prepareSubmission(
            title = "   ",
            description = "Açıklama",
            ingredientValues = listOf(" "),
            stepValues = listOf(""),
            defaultDurationKey = "30",
            defaultDifficultyKey = "kolay"
        )

        val issues = (result as RecipeAddSubmission.Invalid).issues
        assertEquals(
            setOf(
                RecipeAddValidationIssue.TITLE,
                RecipeAddValidationIssue.CATEGORY,
                RecipeAddValidationIssue.INGREDIENTS,
                RecipeAddValidationIssue.STEPS
            ),
            issues
        )
        assertFalse(controller.state.isSaving)
    }

    @Test
    fun validSubmission_trimsRequiredValuesAndUsesDefaults() {
        val controller = RecipeAddFormController()
        controller.toggleCategory("ana yemek")

        val result = controller.prepareSubmission(
            title = "  Test Tarifi  ",
            description = " açıklama ",
            ingredientValues = listOf(" 1 adet havuç ", " "),
            stepValues = listOf(" Karıştır ", ""),
            defaultDurationKey = "30",
            defaultDifficultyKey = "kolay"
        )

        val draft = (result as RecipeAddSubmission.Ready).draft
        assertEquals("Test Tarifi", draft.title)
        assertEquals(" açıklama ", draft.description)
        assertEquals("ana yemek", draft.mainCategoryKey)
        assertEquals("30", draft.durationLabel)
        assertEquals("kolay", draft.difficultyKey)
        assertEquals(listOf("1 adet havuç"), draft.ingredients)
        assertEquals(listOf("Karıştır"), draft.steps)
        assertTrue(controller.state.isSaving)
    }

    @Test
    fun validSubmission_usesSelectedDurationAndDifficulty() {
        val controller = RecipeAddFormController()
        controller.toggleCategory("çorba")
        controller.toggleDuration("60")
        controller.toggleDifficulty("zor")

        val result = controller.prepareSubmission(
            title = "Çorba",
            description = "",
            ingredientValues = listOf("Su"),
            stepValues = listOf("Kaynat"),
            defaultDurationKey = "30",
            defaultDifficultyKey = "kolay"
        )

        val draft = (result as RecipeAddSubmission.Ready).draft
        assertEquals("60", draft.durationLabel)
        assertEquals("zor", draft.difficultyKey)
    }

    @Test
    fun secondSubmission_isRejectedWhileSaving() {
        val controller = validController()
        controller.prepareSubmission(
            title = "Tarif",
            description = "",
            ingredientValues = listOf("Malzeme"),
            stepValues = listOf("Adım"),
            defaultDurationKey = "30",
            defaultDifficultyKey = "kolay"
        )

        val second = controller.prepareSubmission(
            title = "Başka tarif",
            description = "",
            ingredientValues = listOf("Malzeme"),
            stepValues = listOf("Adım"),
            defaultDurationKey = "30",
            defaultDifficultyKey = "kolay"
        )

        assertEquals(RecipeAddSubmission.AlreadySaving, second)
    }

    @Test
    fun completeSave_reenablesSubmission() {
        val controller = validController()
        controller.prepareSubmission(
            title = "Tarif",
            description = "",
            ingredientValues = listOf("Malzeme"),
            stepValues = listOf("Adım"),
            defaultDurationKey = "30",
            defaultDifficultyKey = "kolay"
        )

        val state = controller.completeSave()

        assertFalse(state.isSaving)
    }

    private fun validController(): RecipeAddFormController {
        return RecipeAddFormController().apply { toggleCategory("ana yemek") }
    }
}
