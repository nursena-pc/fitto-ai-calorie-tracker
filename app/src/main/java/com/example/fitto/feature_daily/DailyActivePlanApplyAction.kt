package com.example.fitto.feature_daily

class DailyActivePlanApplyAction(
    private val applyActivePlanToday: () -> Unit
) {

    sealed interface Result {
        data object OpenPlanList : Result
        data class Applied(val planTitle: String) : Result
    }

    fun execute(activePlanTitle: String?): Result {
        val normalizedTitle = activePlanTitle
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: return Result.OpenPlanList

        applyActivePlanToday()

        return Result.Applied(normalizedTitle)
    }
}
