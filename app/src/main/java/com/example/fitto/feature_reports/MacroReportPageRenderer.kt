package com.example.fitto.feature_reports

import com.example.fitto.R
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import kotlin.math.roundToInt

internal class MacroReportPageRenderer(
    private val page: MacroPageRefs,
    private val resources: ReportRenderResources,
    private val chartStyler: ReportChartStyler
) {

    fun setupChart() {
        chartStyler.setupPieChart(page.chart)
    }

    fun render(state: ReportMacroSummaryResolver.State) {
        page.valueText.text = resources.string(
            R.string.reports_format_gram,
            state.total.roundToInt()
        )
        page.infoText.text = resources.string(R.string.reports_macro_info_from_meals)
        page.proteinStat.text = resources.string(
            R.string.reports_format_gram,
            state.protein.roundToInt()
        )
        page.carbStat.text = resources.string(
            R.string.reports_format_gram,
            state.carbs.roundToInt()
        )
        page.fatStat.text = resources.string(
            R.string.reports_format_gram,
            state.fat.roundToInt()
        )

        page.detailsContainer.removeAllViews()

        if (!state.hasData) {
            page.chart.clear()
            page.chart.invalidate()
            page.detailsContainer.addView(resources.emptyDetailRow())
            return
        }

        val entries = state.chartEntries.map { entry ->
            PieEntry(entry.grams.toFloat(), resources.macroLabel(entry.type))
        }
        val dataSet = PieDataSet(
            entries,
            resources.string(R.string.reports_macro_title)
        ).apply {
            setColors(
                listOf(
                    resources.color(R.color.reports_macro_protein),
                    resources.color(R.color.reports_macro_carbs),
                    resources.color(R.color.reports_macro_fat)
                )
            )
            sliceSpace = resources.chartDp(R.dimen.reports_pie_slice_space)
            valueTextColor = resources.color(R.color.reports_text_primary)
            valueTextSize = resources.chartSp(R.dimen.reports_pie_value_text_size)
        }
        val data = PieData(dataSet).apply {
            setValueFormatter(PercentFormatter(page.chart))
        }

        page.chart.data = data
        page.chart.invalidate()

        state.days.forEach { day ->
            val dayTotal = day.protein + day.carbs + day.fat
            page.detailsContainer.addView(
                resources.detailRow(
                    title = resources.displayDate(day.date),
                    subtitle = resources.string(
                        R.string.reports_format_macro_detail,
                        day.protein.roundToInt(),
                        day.carbs.roundToInt(),
                        day.fat.roundToInt()
                    ),
                    value = resources.string(
                        R.string.reports_format_gram,
                        dayTotal.roundToInt()
                    )
                )
            )
        }
    }
}
