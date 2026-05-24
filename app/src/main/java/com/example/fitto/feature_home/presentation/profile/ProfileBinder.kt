package com.example.fitto.feature_home.presentation.profile

import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.example.fitto.R
import com.example.fitto.feature_home.presentation.profile.health.ProfileHealthData
import com.example.fitto.feature_home.presentation.profile.health.formatWeightKg
import com.example.fitto.feature_home.presentation.profile.view.ProfileDonutChartView
import com.example.fitto.feature_home.presentation.social.SocialUser
import com.example.fitto.feature_home.presentation.social.ProfilePhotoLoader
import kotlin.math.roundToInt

object ProfileBinder {

    fun bind(
        root: View,
        user: SocialUser?,
        postCount: Int,
        healthData: ProfileHealthData,
        weeklySummary: ProfileWeeklySummary,
        onProfileImageClick: () -> Unit,
        onNotificationClick: () -> Unit,
        onWeeklySummaryClick: () -> Unit,
        onGoalsClick: () -> Unit,
        onMenuItemClick: (ProfileMenuId) -> Unit
    ) {
        bindHero(
            root = root,
            user = user,
            healthData = healthData,
            onProfileImageClick = onProfileImageClick,
            onNotificationClick = onNotificationClick
        )

        bindSummary(
            root = root,
            postCount = postCount,
            healthData = healthData,
            weeklySummary = weeklySummary,
            onWeeklySummaryClick = onWeeklySummaryClick,
            onGoalsClick = onGoalsClick
        )

        bindMenu(root = root, onMenuItemClick = onMenuItemClick)
    }

    private fun bindHero(
        root: View,
        user: SocialUser?,
        healthData: ProfileHealthData,
        onProfileImageClick: () -> Unit,
        onNotificationClick: () -> Unit
    ) {
        val imageProfileAvatar =
            root.findViewById<ImageView>(R.id.imageProfileAvatar)

        val notificationButton =
            root.findViewById<View>(R.id.buttonProfileNotifications)

        val textProfileUserName =
            root.findViewById<TextView>(R.id.textProfileUserName)

        val textProfileProgress =
            root.findViewById<TextView>(R.id.textProfileProgress)

        val displayName = user?.personalName
            ?.takeIf { it.isNotBlank() && !it.contains("@") }
            ?: user?.displayName
                ?.takeIf { it.isNotBlank() && !it.contains("@") }
            ?: user?.userName
                ?.takeIf { it.isNotBlank() && !it.contains("@") }
            ?: user?.email
                ?.takeIf { it.isNotBlank() }
                ?.substringBefore("@")
                ?.replaceFirstChar { it.uppercase() }

        if (displayName.isNullOrBlank()) {
            textProfileUserName.text = ""
            textProfileUserName.visibility = View.INVISIBLE
        } else {
            textProfileUserName.text = displayName
            textProfileUserName.visibility = View.VISIBLE
        }

        val progressText = when {
            healthData.dailyTargetCalories > 0 -> {
                root.context.getString(
                    R.string.profile_daily_target_calorie_format,
                    healthData.dailyTargetCalories
                )
            }

            healthData.hasWeightGoal -> {
                root.context.getString(
                    R.string.profile_weight_goal_short_format,
                    formatWeightKg(healthData.currentWeightKg),
                    formatWeightKg(healthData.targetWeightKg)
                )
            }

            else -> null
        }

        if (progressText.isNullOrBlank()) {
            textProfileProgress.text = ""
            textProfileProgress.visibility = View.INVISIBLE
        } else {
            textProfileProgress.text = progressText
            textProfileProgress.visibility = View.VISIBLE
        }

        imageProfileAvatar.setOnClickListener(null)
        imageProfileAvatar.isClickable = false
        imageProfileAvatar.isFocusable = false

        notificationButton.setOnClickListener {
            onNotificationClick()
        }

        val photoUrl = user?.photoUrl

        ProfilePhotoLoader.load(
            imageView = imageProfileAvatar,
            photoUrl = photoUrl,
            placeholderResId = R.drawable.ic_profile_person,
            errorResId = R.drawable.ic_profile_person
        )
    }

    private fun bindSummary(
        root: View,
        postCount: Int,
        healthData: ProfileHealthData,
        weeklySummary: ProfileWeeklySummary,
        onWeeklySummaryClick: () -> Unit,
        onGoalsClick: () -> Unit
    ) {
        bindWeeklySummary(
            root = root,
            weeklySummary = weeklySummary,
            onWeeklySummaryClick = onWeeklySummaryClick
        )

        bindGoals(
            root = root,
            postCount = postCount,
            healthData = healthData,
            weeklySummary = weeklySummary,
            onGoalsClick = onGoalsClick
        )
    }

    private fun bindWeeklySummary(
        root: View,
        weeklySummary: ProfileWeeklySummary,
        onWeeklySummaryClick: () -> Unit
    ) {
        val context = root.context

        val cardWeeklySummary =
            root.findViewById<View>(R.id.cardWeeklySummary)

        val profileDonutChart =
            root.findViewById<ProfileDonutChartView>(R.id.profileDonutChart)

        val textWeeklySummarySubtitle =
            root.findViewById<TextView>(R.id.textWeeklySummarySubtitle)

        val textWeeklySummaryPeriodChip =
            root.findViewById<TextView>(R.id.textWeeklySummaryPeriodChip)

        val textWeeklyRecordedDaysValue =
            root.findViewById<TextView>(R.id.textWeeklyRecordedDaysValue)

        val textWeeklyTotalCaloriesValue =
            root.findViewById<TextView>(R.id.textWeeklyTotalCaloriesValue)

        val textWeeklyAverageCaloriesValue =
            root.findViewById<TextView>(R.id.textWeeklyAverageCaloriesValue)

        val textWeeklyTodayCaloriesValue =
            root.findViewById<TextView>(R.id.textWeeklyTodayCaloriesValue)

        val textWeeklyMacroTotalValue =
            root.findViewById<TextView>(R.id.textWeeklyMacroTotalValue)

        val textWeeklyProteinValue =
            root.findViewById<TextView>(R.id.textWeeklyProteinValue)

        val textWeeklyCarbsValue =
            root.findViewById<TextView>(R.id.textWeeklyCarbsValue)

        val textWeeklyFatValue =
            root.findViewById<TextView>(R.id.textWeeklyFatValue)

        val progressWeeklyProtein =
            root.findViewById<ProgressBar>(R.id.progressWeeklyProtein)

        val progressWeeklyCarbs =
            root.findViewById<ProgressBar>(R.id.progressWeeklyCarbs)

        val progressWeeklyFat =
            root.findViewById<ProgressBar>(R.id.progressWeeklyFat)

        val textWeeklySummaryHint =
            root.findViewById<TextView>(R.id.textWeeklySummaryHint)

        cardWeeklySummary.setOnClickListener {
            onWeeklySummaryClick()
        }

        textWeeklySummarySubtitle.text = context.getString(
            R.string.profile_weekly_summary_source
        )

        textWeeklySummaryPeriodChip.text = context.getString(
            R.string.profile_weekly_summary_period
        )

        if (!weeklySummary.hasNutritionData) {
            profileDonutChart.setValues(
                proteinPercent = 0f,
                carbPercent = 0f,
                fatPercent = 0f,
                centerTitle = context.getString(R.string.profile_weekly_summary_no_macro_short),
                centerSubtitle = context.getString(R.string.profile_weekly_summary_macro_center)
            )

            textWeeklyRecordedDaysValue.text = context.getString(
                R.string.profile_weekly_summary_recorded_days_format,
                0,
                7
            )
            textWeeklyTotalCaloriesValue.text = context.getString(
                R.string.profile_weekly_summary_kcal_format,
                0
            )
            textWeeklyAverageCaloriesValue.text = context.getString(
                R.string.profile_weekly_summary_kcal_format,
                0
            )
            textWeeklyTodayCaloriesValue.text = context.getString(
                R.string.profile_weekly_summary_kcal_format,
                0
            )
            textWeeklyMacroTotalValue.text = context.getString(
                R.string.profile_weekly_summary_empty
            )
            textWeeklyProteinValue.text = context.getString(
                R.string.profile_weekly_summary_macro_value_format,
                0,
                0
            )
            textWeeklyCarbsValue.text = context.getString(
                R.string.profile_weekly_summary_macro_value_format,
                0,
                0
            )
            textWeeklyFatValue.text = context.getString(
                R.string.profile_weekly_summary_macro_value_format,
                0,
                0
            )

            progressWeeklyProtein.progress = 0
            progressWeeklyCarbs.progress = 0
            progressWeeklyFat.progress = 0

            textWeeklySummaryHint.text = context.getString(
                R.string.profile_weekly_summary_add_food_hint
            )
            return
        }

        val proteinPercent = weeklySummary.proteinSharePercent.roundToInt().coerceIn(0, 100)
        val carbPercent = weeklySummary.carbSharePercent.roundToInt().coerceIn(0, 100)
        val fatPercent = weeklySummary.fatSharePercent.roundToInt().coerceIn(0, 100)
        val weeklyMacroTotal = formatGram(weeklySummary.weeklyMacroTotal)

        profileDonutChart.setValues(
            proteinPercent = weeklySummary.proteinSharePercent,
            carbPercent = weeklySummary.carbSharePercent,
            fatPercent = weeklySummary.fatSharePercent,
            centerTitle = context.getString(
                R.string.profile_weekly_summary_total_macro_format,
                weeklyMacroTotal
            ),
            centerSubtitle = context.getString(R.string.profile_weekly_summary_macro_center)
        )

        textWeeklyRecordedDaysValue.text = context.getString(
            R.string.profile_weekly_summary_recorded_days_format,
            weeklySummary.recordedDays,
            7
        )
        textWeeklyTotalCaloriesValue.text = context.getString(
            R.string.profile_weekly_summary_kcal_format,
            weeklySummary.weeklyTotalCalories
        )
        textWeeklyAverageCaloriesValue.text = context.getString(
            R.string.profile_weekly_summary_kcal_format,
            weeklySummary.weeklyAverageCalories
        )
        textWeeklyTodayCaloriesValue.text = context.getString(
            R.string.profile_weekly_summary_kcal_format,
            weeklySummary.todayCalories
        )
        textWeeklyMacroTotalValue.text = context.getString(
            R.string.profile_weekly_summary_total_macro_format,
            weeklyMacroTotal
        )

        textWeeklyProteinValue.text = context.getString(
            R.string.profile_weekly_summary_macro_value_format,
            formatGram(weeklySummary.weeklyProtein),
            proteinPercent
        )
        textWeeklyCarbsValue.text = context.getString(
            R.string.profile_weekly_summary_macro_value_format,
            formatGram(weeklySummary.weeklyCarbs),
            carbPercent
        )
        textWeeklyFatValue.text = context.getString(
            R.string.profile_weekly_summary_macro_value_format,
            formatGram(weeklySummary.weeklyFat),
            fatPercent
        )

        progressWeeklyProtein.progress = proteinPercent
        progressWeeklyCarbs.progress = carbPercent
        progressWeeklyFat.progress = fatPercent

        textWeeklySummaryHint.text = context.getString(
            R.string.profile_weekly_summary_open_reports_hint
        )
    }

    private fun bindGoals(
        root: View,
        postCount: Int,
        healthData: ProfileHealthData,
        weeklySummary: ProfileWeeklySummary,
        onGoalsClick: () -> Unit
    ) {
        val cardGoals =
            root.findViewById<View>(R.id.cardGoals)

        val progressWeekly =
            root.findViewById<ProgressBar>(R.id.progressWeekly)

        val progressWater =
            root.findViewById<ProgressBar>(R.id.progressWater)

        val textWeeklyProgressLabel =
            root.findViewById<TextView>(R.id.textWeeklyProgressLabel)

        val textWaterProgressLabel =
            root.findViewById<TextView>(R.id.textWaterProgressLabel)

        cardGoals.setOnClickListener {
            onGoalsClick()
        }

        val hasAnyRealData = healthData.hasMainData ||
                weeklySummary.hasNutritionData ||
                postCount > 0

        if (!hasAnyRealData) {
            textWeeklyProgressLabel.text = root.context.getString(
                R.string.profile_goals_empty_weight
            )
            textWaterProgressLabel.text = root.context.getString(
                R.string.profile_goals_empty_nutrition
            )

            progressWeekly.progress = 0
            progressWater.progress = 0
            return
        }

        textWeeklyProgressLabel.text = if (healthData.hasWeightGoal) {
            root.context.getString(
                R.string.profile_weight_goal_full_format,
                formatWeightKg(healthData.currentWeightKg),
                formatWeightKg(healthData.targetWeightKg),
                formatWeightKg(healthData.weightDifferenceKg)
            )
        } else if (healthData.dailyTargetCalories > 0) {
            root.context.getString(
                R.string.profile_goals_calorie_target_format,
                healthData.dailyTargetCalories
            )
        } else {
            root.context.getString(R.string.profile_goals_empty_weight)
        }

        textWaterProgressLabel.text = if (weeklySummary.weeklyMacroTotal > 0.0) {
            root.context.getString(
                R.string.profile_weekly_summary_macro_line_format,
                formatGram(weeklySummary.weeklyProtein),
                formatGram(weeklySummary.weeklyCarbs),
                formatGram(weeklySummary.weeklyFat)
            )
        } else if (healthData.waterTargetLiter > 0.0) {
            root.context.getString(
                R.string.profile_goals_water_target_format,
                formatLiter(healthData.waterTargetLiter)
            )
        } else {
            root.context.getString(R.string.profile_goals_empty_nutrition)
        }

        progressWeekly.progress = when {
            healthData.hasWeightGoal -> healthData.weightGoalProgressPercent
            healthData.dailyTargetCalories > 0 -> weeklySummary.calorieProgressPercent(
                healthData.dailyTargetCalories
            )
            else -> 0
        }

        progressWater.progress = if (weeklySummary.weeklyMacroTotal > 0.0) {
            100
        } else {
            0
        }
    }

    private fun bindMenu(
        root: View,
        onMenuItemClick: (ProfileMenuId) -> Unit
    ) {
        val recyclerView =
            root.findViewById<RecyclerView>(R.id.profileMenuRecyclerView)

        if (recyclerView.layoutManager == null) {
            recyclerView.layoutManager = LinearLayoutManager(root.context)
            recyclerView.isNestedScrollingEnabled = false
            recyclerView.setHasFixedSize(false)
        }

        if (recyclerView.adapter == null) {
            recyclerView.adapter = ProfileMenuAdapter(
                items = ProfileMenuItem.defaultItems(),
                onItemClick = onMenuItemClick
            )
        }
    }

    private fun formatGram(value: Double): Int {
        return value.roundToInt().coerceAtLeast(0)
    }

    private fun formatLiter(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toInt().toString()
        } else {
            String.format(java.util.Locale.getDefault(), "%.1f", value)
        }
    }
}
