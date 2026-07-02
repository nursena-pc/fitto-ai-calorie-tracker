package com.example.fitto.feature_daily

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import com.example.fitto.feature_dietplans.DietPlansFragment
import com.example.fitto.feature_exercise.AddExerciseFragment
import com.example.fitto.feature_health.HealthConnectBottomSheetFragment
import com.example.fitto.feature_meal.AddMealFragment
import com.example.fitto.feature_meal.MealDetailFragment
import com.example.fitto.feature_meal.model.MealType
import java.time.LocalDate

/** Registers all external Daily screen Fragment Result contracts in one place. */
internal class DailyDashboardResultRegistrar(
    private val parentFragmentManager: FragmentManager,
    private val childFragmentManager: FragmentManager,
    private val lifecycleOwner: LifecycleOwner,
    private val callbacks: Callbacks
) {

    data class Callbacks(
        val currentDate: () -> LocalDate,
        val onQuickActionDestination: (DailyQuickActionDestination) -> Unit,
        val onDateSelection: (DailyResultEffectResolver.DateSelectionEffect) -> Unit,
        val onMealAdded: (MealType) -> Unit,
        val onRefresh: (DailyDashboardRefreshPlan) -> Unit
    )

    fun register() {
        registerQuickActions()
        registerHealthConnectChanges()
        registerDietPlanChanges()
        registerMealAdditions()
        registerMealDetailChanges()
        registerExerciseAdditions()
    }

    private fun registerQuickActions() {
        childFragmentManager.setFragmentResultListener(
            DailyQuickAddBottomSheet.REQUEST_QUICK_ACTION,
            lifecycleOwner
        ) { _, bundle ->
            val action = DailyQuickAction.fromValue(
                bundle.getString(DailyQuickAddBottomSheet.BUNDLE_QUICK_ACTION)
            ) ?: return@setFragmentResultListener

            callbacks.onQuickActionDestination(
                DailyQuickActionDestinationResolver.resolve(action)
            )
        }
    }

    private fun registerHealthConnectChanges() {
        childFragmentManager.setFragmentResultListener(
            HealthConnectBottomSheetFragment.REQUEST_HEALTH_CONNECT_CHANGED,
            lifecycleOwner
        ) { _, _ ->
            callbacks.onRefresh(DailyDashboardRefreshPlans.HEALTH_CONNECT_CHANGED)
        }
    }

    private fun registerDietPlanChanges() {
        parentFragmentManager.setFragmentResultListener(
            DietPlansFragment.REQUEST_DIET_PLAN_CHANGED,
            lifecycleOwner
        ) { _, _ ->
            callbacks.onRefresh(DailyResultEffectResolver.resolveDietPlanChanged())
        }
    }

    private fun registerMealAdditions() {
        parentFragmentManager.setFragmentResultListener(
            AddMealFragment.REQUEST_MEAL_ENTRY_ADDED,
            lifecycleOwner
        ) { _, bundle ->
            val effect = DailyResultEffectResolver.resolveMealAdded(
                currentDate = callbacks.currentDate(),
                mealTypeArgument = bundle.getString(AddMealFragment.BUNDLE_MEAL_TYPE),
                entryDate = bundle.getString(AddMealFragment.BUNDLE_ENTRY_DATE)
            )

            callbacks.onDateSelection(
                DailyResultEffectResolver.DateSelectionEffect(effect.dateSelection)
            )
            callbacks.onMealAdded(effect.mealType)
            callbacks.onRefresh(effect.refreshPlan)
        }
    }

    private fun registerMealDetailChanges() {
        parentFragmentManager.setFragmentResultListener(
            MealDetailFragment.REQUEST_MEAL_DETAIL_CHANGED,
            lifecycleOwner
        ) { _, _ ->
            callbacks.onRefresh(DailyResultEffectResolver.resolveMealDetailChanged())
        }
    }

    private fun registerExerciseAdditions() {
        parentFragmentManager.setFragmentResultListener(
            AddExerciseFragment.REQUEST_EXERCISE_ADDED,
            lifecycleOwner
        ) { _, bundle ->
            val effect = DailyResultEffectResolver.resolveExerciseAdded(
                currentDate = callbacks.currentDate(),
                entryDate = bundle.getString(AddExerciseFragment.BUNDLE_EXERCISE_DATE)
            )

            callbacks.onDateSelection(effect.dateSelectionEffect)
            callbacks.onRefresh(effect.refreshPlan)
        }
    }
}
