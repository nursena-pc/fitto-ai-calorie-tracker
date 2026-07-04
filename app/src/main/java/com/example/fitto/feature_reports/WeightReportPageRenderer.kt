package com.example.fitto.feature_reports

import com.example.fitto.R
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

internal class WeightReportPageRenderer(
    private val page: WeightPageRefs,
    private val resources: ReportRenderResources,
    private val chartStyler: ReportChartStyler
) {

    fun setupChart() {
        chartStyler.setupLineChart(
            chart = page.chart,
            noDataText = resources.string(R.string.reports_no_weight_data)
        )
    }

    fun render(state: ReportWeightSummaryResolver.State) {
        if (!state.hasData) {
            page.valueText.text = resources.string(R.string.reports_value_empty)
            page.infoText.text = resources.string(R.string.reports_weight_missing)
            page.startStat.text = resources.string(R.string.reports_value_empty)
            page.currentStat.text = resources.string(R.string.reports_value_empty)
            page.targetStat.text = resources.formatWeightOrDash(state.targetWeight)
            page.detailsContainer.removeAllViews()
            page.detailsContainer.addView(resources.emptyDetailRow())

            page.chart.clear()
            page.chart.invalidate()
            return
        }

        page.valueText.text = resources.formatWeightOrDash(state.currentWeight)
        page.infoText.text = if (state.targetWeight > 0.0) {
            resources.string(R.string.reports_weight_goal_info)
        } else {
            resources.string(R.string.reports_weight_subtitle)
        }
        page.startStat.text = resources.formatWeightOrDash(state.firstWeight)
        page.currentStat.text = resources.formatWeightOrDash(state.currentWeight)
        page.targetStat.text = resources.formatWeightOrDash(state.targetWeight)

        val entries = state.visiblePoints.mapIndexed { index, point ->
            Entry(index.toFloat(), point.weightKg.toFloat())
        }
        val labels = state.visiblePoints.map { point ->
            point.label.ifBlank { resources.string(R.string.reports_value_empty) }
        }
        val dataSet = LineDataSet(
            entries,
            resources.string(R.string.reports_weight_title)
        ).apply {
            color = resources.color(R.color.reports_weight_line)
            lineWidth = resources.chartDp(R.dimen.reports_chart_line_width)
            circleRadius = resources.chartDp(R.dimen.reports_chart_circle_radius)
            circleHoleRadius = resources.chartDp(R.dimen.reports_chart_circle_hole_radius)
            setCircleColor(resources.color(R.color.reports_weight_line))
            valueTextColor = resources.color(R.color.reports_chart_value_text)
            valueTextSize = resources.chartSp(R.dimen.reports_chart_value_text_size)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true)
            fillColor = resources.color(R.color.reports_weight_fill)
            fillAlpha = resources.integer(R.integer.reports_chart_fill_alpha)
        }

        page.chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        page.chart.axisLeft.axisMinimum = state.axisMin
        page.chart.axisLeft.axisMaximum = state.axisMax
        page.chart.data = LineData(dataSet)
        page.chart.invalidate()

        page.detailsContainer.removeAllViews()
        state.visiblePoints.reversed().forEach { point ->
            page.detailsContainer.addView(
                resources.detailRow(
                    title = point.label,
                    subtitle = resources.string(R.string.reports_weight_detail_measurement),
                    value = resources.formatWeightOrDash(point.weightKg)
                )
            )
        }
    }
}
