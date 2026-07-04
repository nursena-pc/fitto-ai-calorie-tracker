package com.example.fitto.feature_reports

import android.view.View
import android.widget.LinearLayout
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.google.android.material.textview.MaterialTextView

internal data class CaloriePageRefs(
    val root: View,
    val valueText: MaterialTextView,
    val infoText: MaterialTextView,
    val todayStat: MaterialTextView,
    val averageStat: MaterialTextView,
    val targetStat: MaterialTextView,
    val chart: LineChart,
    val detailsContainer: LinearLayout
)

internal data class MacroPageRefs(
    val root: View,
    val valueText: MaterialTextView,
    val infoText: MaterialTextView,
    val proteinStat: MaterialTextView,
    val carbStat: MaterialTextView,
    val fatStat: MaterialTextView,
    val chart: PieChart,
    val detailsContainer: LinearLayout
)

internal data class WeightPageRefs(
    val root: View,
    val valueText: MaterialTextView,
    val infoText: MaterialTextView,
    val startStat: MaterialTextView,
    val currentStat: MaterialTextView,
    val targetStat: MaterialTextView,
    val chart: LineChart,
    val detailsContainer: LinearLayout
)
