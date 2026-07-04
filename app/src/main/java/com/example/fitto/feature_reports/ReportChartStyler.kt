package com.example.fitto.feature_reports

import com.example.fitto.R
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis

internal class ReportChartStyler(
    private val renderResources: ReportRenderResources
) {

    fun setupLineChart(
        chart: LineChart,
        noDataText: String
    ) {
        chart.apply {
            setBackgroundColor(renderResources.color(R.color.reports_chart_background))
            description.isEnabled = false
            setNoDataText(noDataText)
            setNoDataTextColor(renderResources.color(R.color.reports_text_secondary))

            setTouchEnabled(true)
            setScaleEnabled(true)
            setPinchZoom(true)
            setDrawGridBackground(false)

            extraBottomOffset = renderResources.chartDp(R.dimen.reports_chart_extra_bottom_offset)
            extraTopOffset = renderResources.chartDp(R.dimen.reports_chart_extra_top_offset)

            axisRight.isEnabled = false

            axisLeft.textColor = renderResources.color(R.color.reports_chart_axis_text)
            axisLeft.gridColor = renderResources.color(R.color.reports_chart_grid)
            axisLeft.axisLineColor = renderResources.color(R.color.reports_chart_grid)

            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.granularity = renderResources.integer(
                R.integer.reports_chart_x_granularity
            ).toFloat()
            xAxis.textColor = renderResources.color(R.color.reports_chart_axis_text)
            xAxis.axisLineColor = renderResources.color(R.color.reports_chart_grid)
            xAxis.setDrawGridLines(false)

            legend.isEnabled = false
        }
    }

    fun setupPieChart(chart: PieChart) {
        chart.apply {
            setBackgroundColor(renderResources.color(R.color.reports_chart_background))
            description.isEnabled = false
            setNoDataText(renderResources.string(R.string.reports_no_macro_data))
            setNoDataTextColor(renderResources.color(R.color.reports_text_secondary))

            setUsePercentValues(true)
            setDrawEntryLabels(false)
            isDrawHoleEnabled = true
            holeRadius = renderResources.integer(
                R.integer.reports_pie_hole_radius_percent
            ).toFloat()
            transparentCircleRadius = renderResources.integer(
                R.integer.reports_pie_transparent_radius_percent
            ).toFloat()
            setHoleColor(renderResources.color(R.color.reports_chart_background))
            centerText = renderResources.string(R.string.reports_macro_center_text)
            setCenterTextSize(renderResources.chartSp(R.dimen.reports_pie_center_text_size))
            setCenterTextColor(renderResources.color(R.color.reports_text_primary))

            legend.isEnabled = true
            legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
            legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
            legend.orientation = Legend.LegendOrientation.HORIZONTAL
            legend.setDrawInside(false)
            legend.textColor = renderResources.color(R.color.reports_text_secondary)
            legend.textSize = renderResources.chartSp(R.dimen.reports_chart_legend_text_size)
        }
    }
}
