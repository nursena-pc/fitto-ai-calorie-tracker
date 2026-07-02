package com.example.fitto.feature_daily

import android.content.Context
import com.example.fitto.R
import com.example.fitto.databinding.FragmentDailyBinding

internal class DailySleepWeightRenderer(
    private val binding: FragmentDailyBinding,
    private val context: Context
) {

    fun renderSleep(state: DailySleepCardStateResolver.State) {
        when (state) {
            DailySleepCardStateResolver.State.Empty -> {
                binding.sleepValueText.text = context.getString(R.string.daily_no_sleep_data_short)
                binding.sleepStatusText.text = context.getString(R.string.daily_no_sleep_data_desc)
            }

            is DailySleepCardStateResolver.State.Logged -> {
                binding.sleepValueText.text = DailyValueFormatter.formatDuration(
                    state.durationMinutes
                )
                binding.sleepStatusText.text = context.getString(
                    R.string.sleep_time_range_format,
                    DailyValueFormatter.formatHourMinute(
                        state.selection.bedtimeHour,
                        state.selection.bedtimeMinute
                    ),
                    DailyValueFormatter.formatHourMinute(
                        state.selection.wakeHour,
                        state.selection.wakeMinute
                    )
                )
            }
        }
    }

    fun renderWeight(state: DailyWeightCardStateResolver.State) {
        when (state) {
            DailyWeightCardStateResolver.State.Unknown -> {
                binding.weightValueText.text = context.getString(
                    R.string.daily_weight_unknown_short
                )
                binding.weightStatusText.text = context.getString(
                    R.string.daily_weight_not_saved
                )
            }

            is DailyWeightCardStateResolver.State.Available -> {
                binding.weightValueText.text = context.getString(
                    R.string.daily_weight_value_format,
                    DailyValueFormatter.formatWeightValue(state.weightKg)
                )
                binding.weightStatusText.text = context.getString(
                    R.string.daily_weight_from_profile
                )
            }
        }
    }
}
