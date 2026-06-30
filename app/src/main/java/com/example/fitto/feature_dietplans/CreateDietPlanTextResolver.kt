package com.example.fitto.feature_dietplans

import android.content.Context
import com.example.fitto.R
import com.example.fitto.feature_dietplans.model.CustomDietPlanLocalModel

class CreateDietPlanTextResolver(
    private val strings: Strings
) {

    fun defaultExclusions(): List<String> {
        return listOf(
            strings.defaultExclusionBroccoli(),
            strings.defaultExclusionMushroom(),
            strings.defaultExclusionMilk(),
            strings.defaultExclusionPeanut(),
            strings.defaultExclusionSeafood()
        )
    }

    fun socialShareText(
        goal: CreatePlanGoal,
        plan: CustomDietPlanLocalModel
    ): CreateDietPlanSocialShareText {
        val goalTitle = strings.goalTitle(goal)

        return CreateDietPlanSocialShareText(
            content = strings.socialShareContent(
                goalTitle = goalTitle,
                dailyCalories = plan.dailyCalories,
                durationDays = plan.durationDays
            ),
            mealInfo = strings.socialShareMealInfo(),
            customTag = strings.customDietPlanTag(),
            goalTag = goalTitle
        )
    }

    interface Strings {
        fun defaultExclusionBroccoli(): String
        fun defaultExclusionMushroom(): String
        fun defaultExclusionMilk(): String
        fun defaultExclusionPeanut(): String
        fun defaultExclusionSeafood(): String
        fun goalTitle(goal: CreatePlanGoal): String
        fun socialShareContent(
            goalTitle: String,
            dailyCalories: Int,
            durationDays: Int
        ): String
        fun socialShareMealInfo(): String
        fun customDietPlanTag(): String
    }

    companion object {
        fun from(context: Context): CreateDietPlanTextResolver {
            return CreateDietPlanTextResolver(
                strings = object : Strings {
                    override fun defaultExclusionBroccoli(): String {
                        return context.getString(R.string.create_diet_plan_exclusion_broccoli)
                    }

                    override fun defaultExclusionMushroom(): String {
                        return context.getString(R.string.create_diet_plan_exclusion_mushroom)
                    }

                    override fun defaultExclusionMilk(): String {
                        return context.getString(R.string.create_diet_plan_exclusion_milk)
                    }

                    override fun defaultExclusionPeanut(): String {
                        return context.getString(R.string.create_diet_plan_exclusion_peanut)
                    }

                    override fun defaultExclusionSeafood(): String {
                        return context.getString(R.string.create_diet_plan_exclusion_seafood)
                    }

                    override fun goalTitle(goal: CreatePlanGoal): String {
                        return context.getString(goal.titleRes)
                    }

                    override fun socialShareContent(
                        goalTitle: String,
                        dailyCalories: Int,
                        durationDays: Int
                    ): String {
                        return context.getString(
                            R.string.create_diet_plan_social_share_content,
                            goalTitle,
                            dailyCalories,
                            durationDays
                        )
                    }

                    override fun socialShareMealInfo(): String {
                        return context.getString(R.string.create_diet_plan_social_share_meal_info)
                    }

                    override fun customDietPlanTag(): String {
                        return context.getString(R.string.diet_plan_tag_custom)
                    }
                }
            )
        }
    }
}
