package com.example.fitto.feature_daily

import android.content.Context
import android.view.View
import android.widget.TextView
import com.example.fitto.R
import com.example.fitto.databinding.FragmentDailyBinding
import java.time.format.TextStyle
import java.util.Locale

internal class DailyWeeklyProgressRenderer(
    private val binding: FragmentDailyBinding,
    private val context: Context
) {

    private val dayViews: List<DayViews> = listOf(
        DayViews(binding.weekBar1, binding.weekDay1, binding.weekValue1),
        DayViews(binding.weekBar2, binding.weekDay2, binding.weekValue2),
        DayViews(binding.weekBar3, binding.weekDay3, binding.weekValue3),
        DayViews(binding.weekBar4, binding.weekDay4, binding.weekValue4),
        DayViews(binding.weekBar5, binding.weekDay5, binding.weekValue5),
        DayViews(binding.weekBar6, binding.weekDay6, binding.weekValue6),
        DayViews(binding.weekBar7, binding.weekDay7, binding.weekValue7)
    )

    fun render(
        result: DailyWeeklyProgressLoadResult,
        locale: Locale = Locale.getDefault()
    ) {
        val progress = result.progress

        binding.weeklyProgressDescriptionText.text = if (progress.daysWithFood == 0) {
            context.getString(R.string.daily_no_weekly_data)
        } else {
            context.getString(
                R.string.daily_weekly_calorie_summary,
                progress.daysWithFood,
                progress.averageProgress
            )
        }
        binding.weeklyWaterProgress.progress = progress.progressBarPercent

        dayViews.forEachIndexed { index, views ->
            val date = result.days.getOrNull(index) ?: return@forEachIndexed
            val dayProgress = progress.days.getOrNull(index) ?: return@forEachIndexed

            views.bar.layoutParams = views.bar.layoutParams.apply {
                height = dayProgress.heightPx
            }
            views.day.text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
            views.value.text = if (dayProgress.hasFood) {
                context.getString(
                    R.string.daily_weekly_calorie_percent,
                    dayProgress.progressPercent
                )
            } else {
                EMPTY_VALUE
            }
            views.bar.alpha = dayProgress.alpha
        }
    }

    private data class DayViews(
        val bar: View,
        val day: TextView,
        val value: TextView
    )

    private companion object {
        const val EMPTY_VALUE = "-"
    }
}
