package com.example.fitto.feature_reports

import android.content.Context

internal class ReportsPageRenderer(
    context: Context,
    caloriePage: CaloriePageRefs,
    macroPage: MacroPageRefs,
    weightPage: WeightPageRefs
) {
    private val renderResources = ReportRenderResources(context)
    private val chartStyler = ReportChartStyler(renderResources)
    private val calorieRenderer = CalorieReportPageRenderer(
        page = caloriePage,
        resources = renderResources,
        chartStyler = chartStyler
    )
    private val macroRenderer = MacroReportPageRenderer(
        page = macroPage,
        resources = renderResources,
        chartStyler = chartStyler
    )
    private val weightRenderer = WeightReportPageRenderer(
        page = weightPage,
        resources = renderResources,
        chartStyler = chartStyler
    )

    fun setupCharts() {
        calorieRenderer.setupChart()
        macroRenderer.setupChart()
        weightRenderer.setupChart()
    }

    fun render(state: ReportsRenderState) {
        calorieRenderer.render(state.calorie)
        macroRenderer.render(state.macro)
        weightRenderer.render(state.weight)
    }
}
