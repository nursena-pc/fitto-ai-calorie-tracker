package com.example.fitto.feature_recipes

import com.example.fitto.feature_recipes.data.UserRecipeDraft

data class RecipeAddFormState(
    val selectedCategoryKey: String? = null,
    val selectedDurationKey: String? = null,
    val selectedDifficultyKey: String? = null,
    val isSaving: Boolean = false
)

enum class RecipeAddValidationIssue {
    TITLE,
    CATEGORY,
    INGREDIENTS,
    STEPS
}

sealed interface RecipeAddSubmission {
    data class Ready(val draft: UserRecipeDraft) : RecipeAddSubmission
    data class Invalid(val issues: Set<RecipeAddValidationIssue>) : RecipeAddSubmission
    data object AlreadySaving : RecipeAddSubmission
}

class RecipeAddFormController {

    var state: RecipeAddFormState = RecipeAddFormState()
        private set

    fun toggleCategory(key: String): RecipeAddFormState {
        state = state.copy(
            selectedCategoryKey = key.takeUnless { it == state.selectedCategoryKey }
        )
        return state
    }

    fun toggleDuration(key: String): RecipeAddFormState {
        state = state.copy(
            selectedDurationKey = key.takeUnless { it == state.selectedDurationKey }
        )
        return state
    }

    fun toggleDifficulty(key: String): RecipeAddFormState {
        state = state.copy(
            selectedDifficultyKey = key.takeUnless { it == state.selectedDifficultyKey }
        )
        return state
    }

    fun prepareSubmission(
        title: String,
        description: String,
        ingredientValues: List<String>,
        stepValues: List<String>,
        defaultDurationKey: String,
        defaultDifficultyKey: String
    ): RecipeAddSubmission {
        if (state.isSaving) return RecipeAddSubmission.AlreadySaving

        val normalizedTitle = title.trim()
        val ingredients = ingredientValues.map(String::trim).filter(String::isNotBlank)
        val steps = stepValues.map(String::trim).filter(String::isNotBlank)
        val issues = buildSet {
            if (normalizedTitle.isBlank()) add(RecipeAddValidationIssue.TITLE)
            if (state.selectedCategoryKey == null) add(RecipeAddValidationIssue.CATEGORY)
            if (ingredients.isEmpty()) add(RecipeAddValidationIssue.INGREDIENTS)
            if (steps.isEmpty()) add(RecipeAddValidationIssue.STEPS)
        }

        if (issues.isNotEmpty()) return RecipeAddSubmission.Invalid(issues)

        state = state.copy(isSaving = true)
        return RecipeAddSubmission.Ready(
            UserRecipeDraft(
                title = normalizedTitle,
                description = description,
                mainCategoryKey = state.selectedCategoryKey.orEmpty(),
                durationLabel = state.selectedDurationKey ?: defaultDurationKey,
                difficultyKey = state.selectedDifficultyKey ?: defaultDifficultyKey,
                ingredients = ingredients,
                steps = steps
            )
        )
    }

    fun completeSave(): RecipeAddFormState {
        state = state.copy(isSaving = false)
        return state
    }
}
