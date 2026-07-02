package com.example.fitto.feature_daily

import com.example.fitto.feature_dietplans.planning.DailyPlannedMealsUiModel
import com.example.fitto.feature_meal.model.MealType

class DailyPlannedMealAction(
    private val addPlannedMealEntry: suspend (
        request: DailyPlannedMealActionResolver.ApplyRequest,
        entryDate: String
    ) -> Unit,
    private val markPlannedMealCompleted: (DailyPlannedMealActionResolver.Completion) -> Unit
) {

    fun prepare(dailyPlan: DailyPlannedMealsUiModel?): DailyPlannedMealActionResolver.ApplyRequest? {
        return DailyPlannedMealActionResolver.resolveApplyRequest(dailyPlan)
    }

    suspend fun execute(
        request: DailyPlannedMealActionResolver.ApplyRequest,
        entryDate: String
    ): DailyPlannedMealExecutionResult {
        val saveResult = runCatching {
            addPlannedMealEntry(request, entryDate)
        }

        if (saveResult.isFailure) {
            return DailyPlannedMealExecutionResult.Failure(
                DailyPlannedMealApplyResultResolver.resolveFailure()
            )
        }

        markPlannedMealCompleted(request.completion)
        return DailyPlannedMealExecutionResult.Success(
            DailyPlannedMealApplyResultResolver.resolveSuccess(request)
        )
    }

    fun completeNextMealIfMatches(
        dailyPlan: DailyPlannedMealsUiModel?,
        mealType: MealType
    ): Boolean {
        val completion = DailyPlannedMealActionResolver.resolveCompletionForMealType(
            dailyPlan = dailyPlan,
            mealType = mealType
        ) ?: return false

        markPlannedMealCompleted(completion)
        return true
    }
}

sealed interface DailyPlannedMealExecutionResult {
    data class Success(
        val effect: DailyPlannedMealApplyResultResolver.SuccessEffect
    ) : DailyPlannedMealExecutionResult

    data class Failure(
        val effect: DailyPlannedMealApplyResultResolver.FailureEffect
    ) : DailyPlannedMealExecutionResult
}
