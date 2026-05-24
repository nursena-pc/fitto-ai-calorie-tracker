package com.example.fitto.feature_home.presentation.profile

import android.view.View
import android.widget.EditText
import android.widget.TextView
import com.example.fitto.R
import com.example.fitto.feature_home.presentation.profile.health.ProfileHealthData
import com.example.fitto.feature_home.presentation.profile.health.formatWeightKg
import com.example.fitto.feature_home.presentation.profile.view.WeightTrendChartView
import com.google.android.material.appbar.MaterialToolbar

object ProfileWeightBinder {

    fun bind(
        root: View,
        healthData: ProfileHealthData,
        onBackClick: () -> Unit,
        onAddWeightClick: () -> Unit,
        onReminderClick: () -> Unit
    ) {
        val toolbar = root.findViewById<MaterialToolbar>(R.id.toolbarWeight)

        val editCurrentWeightValue =
            root.findViewById<EditText>(R.id.editCurrentWeightValue)

        val textLastWeighDate =
            root.findViewById<TextView>(R.id.textLastWeighDate)

        val textInitialWeightValue =
            root.findViewById<TextView>(R.id.textInitialWeightValue)

        val textTargetWeightValue =
            root.findViewById<TextView>(R.id.textTargetWeightValue)

        val textLostSoFar =
            root.findViewById<TextView>(R.id.textLostSoFar)

        val textRemainingWeight =
            root.findViewById<TextView>(R.id.textRemainingWeight)

        val reminderBanner =
            root.findViewById<View>(R.id.cardReminderBanner)

        val reminderButton =
            root.findViewById<View>(R.id.buttonReminder)

        val weightTrendChartView =
            root.findViewById<WeightTrendChartView>(R.id.weightTrendChartView)

        toolbar.setNavigationOnClickListener {
            onBackClick()
        }

        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_weight_history -> {
                    onAddWeightClick()
                    true
                }

                else -> false
            }
        }

        reminderBanner.setOnClickListener {
            onReminderClick()
        }

        reminderButton.setOnClickListener {
            onReminderClick()
        }

        editCurrentWeightValue.setText(formatWeightInput(healthData.currentWeightKg))
        textInitialWeightValue.text = formatWeight(root, healthData.initialWeightKg)
        textTargetWeightValue.text = formatWeight(root, healthData.targetWeightKg)

        textLastWeighDate.text = if (healthData.lastWeighDateText.isNotBlank()) {
            root.context.getString(
                R.string.weight_last_weigh_format,
                healthData.lastWeighDateText
            )
        } else {
            root.context.getString(R.string.weight_last_weigh_default)
        }

        textLostSoFar.text = root.context.getString(
            R.string.weight_lost_so_far_format,
            formatWeightKg(healthData.lostSoFarKg)
        )

        textRemainingWeight.text = root.context.getString(
            R.string.weight_remaining_format,
            formatWeightKg(healthData.remainingKg)
        )

        weightTrendChartView.submitData(healthData.chartPoints)
    }

    private fun formatWeightInput(value: Double): String {
        return if (value > 0.0) formatWeightKg(value) else ""
    }

    private fun formatWeight(root: View, value: Double): String {
        return if (value > 0.0) {
            root.context.getString(R.string.weight_value_format, formatWeightKg(value))
        } else {
            root.context.getString(R.string.profile_data_not_saved_yet)
        }
    }
}