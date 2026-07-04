package com.example.fitto.feature_reports

internal data class ReportsRenderConfig(
    val defaultTargetCalories: Int,
    val weightVisiblePointCount: Int,
    val weightAxisPaddingKg: Double
)

internal data class ReportsRenderState(
    val calorie: ReportCalorieSummaryResolver.State,
    val macro: ReportMacroSummaryResolver.State,
    val weight: ReportWeightSummaryResolver.State
)

internal object ReportsRenderStateResolver {

    fun resolve(
        dataState: ReportsDataState,
        config: ReportsRenderConfig
    ): ReportsRenderState {
        return ReportsRenderState(
            calorie = ReportCalorieSummaryResolver.resolve(
                days = dataState.nutritionDays,
                profileTargetCalories = dataState.healthData.dailyTargetCalories,
                defaultTargetCalories = config.defaultTargetCalories
            ),
            macro = ReportMacroSummaryResolver.resolve(dataState.nutritionDays),
            weight = ReportWeightSummaryResolver.resolve(
                healthData = dataState.healthData,
                visibleCount = config.weightVisiblePointCount,
                axisPaddingKg = config.weightAxisPaddingKg
            )
        )
    }
}
