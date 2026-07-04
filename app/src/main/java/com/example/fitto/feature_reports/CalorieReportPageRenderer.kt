package com.example.fitto.feature_reports

import com.example.fitto.R
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlin.math.roundToInt

internal class CalorieReportPageRenderer(
    private val page: CaloriePageRefs,
    private val resources: ReportRenderResources,
    private val chartStyler: ReportChartStyler
) {

    fun setupChart() {
        chartStyler.setupLineChart(
            chart = page.chart,
            noDataText = resources.string(R.string.reports_no_calorie_data)
        )
    }

    fun render(state: ReportCalorieSummaryResolver.State) {
        if (state.days.isEmpty()) return

        page.valueText.text = resources.string(
            R.string.reports_format_kcal,
            state.todayCalories
        )
        page.infoText.text = resources.string(R.string.reports_calorie_info_selected_date)
        page.todayStat.text = resources.string(
            R.string.reports_format_kcal,
            state.todayCalories
        )
        page.averageStat.text = resources.string(
            R.string.reports_format_kcal,
            state.averageCalories
        )
        page.targetStat.text = resources.string(
            R.string.reports_format_kcal,
            state.targetCalories
        )

        val entries = state.days.mapIndexed { index, day ->
            Entry(index.toFloat(), day.calories.toFloat())
        }
        val labels = state.days.map { day -> day.label }

        val dataSet = LineDataSet(
            entries,
            resources.string(R.string.reports_calorie_title)
        ).apply {
            color = resources.color(R.color.reports_calorie_line)
            lineWidth = resources.chartDp(R.dimen.reports_chart_line_width)
            circleRadius = resources.chartDp(R.dimen.reports_chart_circle_radius)
            circleHoleRadius = resources.chartDp(R.dimen.reports_chart_circle_hole_radius)
            setCircleColor(resources.color(R.color.reports_calorie_line))
            valueTextColor = resources.color(R.color.reports_chart_value_text)
            valueTextSize = resources.chartSp(R.dimen.reports_chart_value_text_size)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true)
            fillColor = resources.color(R.color.reports_calorie_fill)
            fillAlpha = resources.integer(R.integer.reports_chart_fill_alpha)
        }

        page.chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        page.chart.axisLeft.removeAllLimitLines()
        page.chart.axisLeft.addLimitLine(
            LimitLine(
                state.targetCalories.toFloat(),
                resources.string(R.string.reports_chart_target_label)
            ).apply {
                lineColor = resources.color(R.color.reports_target_line)
                lineWidth = resources.chartDp(R.dimen.reports_chart_limit_line_width)
                textColor = resources.color(R.color.reports_target_line)
                textSize = resources.chartSp(R.dimen.reports_chart_value_text_size)
                enableDashedLine(
                    resources.chartDp(R.dimen.reports_chart_dash_line_length),
                    resources.chartDp(R.dimen.reports_chart_dash_gap_length),
                    0f
                )
            }
        )

        val axisMultiplier = resources.fraction(
            R.fraction.reports_axis_max_padding_multiplier
        )
        page.chart.axisLeft.axisMinimum = 0f
        page.chart.axisLeft.axisMaximum = maxOf(
            state.targetCalories.toFloat(),
            state.maxCalories.toFloat(),
            resources.integer(R.integer.reports_chart_min_axis_max).toFloat()
        ) * axisMultiplier

        page.chart.data = LineData(dataSet)
        page.chart.invalidate()

        page.detailsContainer.removeAllViews()
        state.days.forEach { day ->
            page.detailsContainer.addView(
                resources.detailRow(
                    title = resources.displayDate(day.date),
                    subtitle = resources.string(
                        R.string.reports_format_macro_detail,
                        day.protein.roundToInt(),
                        day.carbs.roundToInt(),
                        day.fat.roundToInt()
                    ),
                    value = resources.string(R.string.reports_format_kcal, day.calories)
                )
            )
        }
    }
}
