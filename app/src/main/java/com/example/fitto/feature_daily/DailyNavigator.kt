package com.example.fitto.feature_daily

import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.fitto.feature_dietplans.DietPlanDetailFragment
import com.example.fitto.feature_dietplans.DietPlansFragment
import com.example.fitto.feature_dietplans.IntermittentFastingDetailFragment
import com.example.fitto.feature_exercise.AddExerciseFragment
import com.example.fitto.feature_meal.AddMealFragment
import com.example.fitto.feature_meal.CameraFoodEntryFragment
import com.example.fitto.feature_meal.MealDetailFragment
import com.example.fitto.feature_meal.model.MealType

class DailyNavigator(
    private val fragmentManager: FragmentManager,
    @IdRes private val containerId: Int
) {
    fun openDietPlans() {
        replace(
            fragment = DietPlansFragment(),
            backStackName = "diet_plans"
        )
    }

    fun openActivePlanDetail(planId: String) {
        when (val destination = DailyActivePlanDestinationResolver.resolve(planId)) {
            DailyActivePlanDestination.DietPlans -> openDietPlans()
            DailyActivePlanDestination.IntermittentFastingDetail -> replace(
                fragment = IntermittentFastingDetailFragment(),
                backStackName = "active_diet_plan_detail"
            )
            is DailyActivePlanDestination.DietPlanDetail -> replace(
                fragment = DietPlanDetailFragment.newInstance(destination.planId),
                backStackName = "active_diet_plan_detail"
            )
        }
    }

    fun openMealDetail(mealType: MealType) {
        replace(
            fragment = MealDetailFragment.newInstance(mealType),
            backStackName = MealDetailFragment.BACK_STACK_NAME
        )
    }

    fun openAddMeal(mealType: MealType, entryDate: String) {
        replace(
            fragment = AddMealFragment.newInstance(
                mealType = mealType,
                entryDate = entryDate
            ),
            backStackName = AddMealFragment.BACK_STACK_NAME
        )
    }

    fun openAddExercise(entryDate: String) {
        replace(
            fragment = AddExerciseFragment.newInstance(entryDate = entryDate),
            backStackName = "add_exercise"
        )
    }

    fun openCameraFoodEntry(mealType: MealType) {
        replace(
            fragment = CameraFoodEntryFragment.newInstance(mealType = mealType),
            backStackName = CameraFoodEntryFragment.BACK_STACK_NAME
        )
    }

    private fun replace(fragment: Fragment, backStackName: String) {
        fragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .replace(containerId, fragment)
            .addToBackStack(backStackName)
            .commit()
    }
}
