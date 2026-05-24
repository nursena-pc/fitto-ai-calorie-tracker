package com.example.fitto.feature_dietplans

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.fitto.R
import com.example.fitto.databinding.FragmentCustomDietPlanDetailBinding
import com.example.fitto.feature_dietplans.data.CustomDietPlanStore
import com.example.fitto.feature_dietplans.data.DietPlanPreferenceStore
import com.example.fitto.feature_dietplans.model.CustomDietPlanLocalModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlin.math.roundToInt

class CustomDietPlanDetailFragment : Fragment(R.layout.fragment_custom_diet_plan_detail) {

    private var _binding: FragmentCustomDietPlanDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var customDietPlanStore: CustomDietPlanStore
    private lateinit var preferenceStore: DietPlanPreferenceStore

    private val planId: String by lazy {
        requireArguments().getString(ARG_PLAN_ID).orEmpty()
    }

    private var currentPlan: CustomDietPlanLocalModel? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCustomDietPlanDetailBinding.bind(view)

        customDietPlanStore = CustomDietPlanStore(requireContext().applicationContext)
        preferenceStore = DietPlanPreferenceStore(requireContext())

        currentPlan = customDietPlanStore.getPlan(planId)

        if (currentPlan == null) {
            Toast.makeText(
                requireContext(),
                getString(R.string.custom_diet_detail_not_found),
                Toast.LENGTH_SHORT
            ).show()
            parentFragmentManager.popBackStack()
            return
        }

        bindPlan(requireNotNull(currentPlan))
        setupClicks()
    }

    private fun setupClicks() = with(binding) {
        backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        savePlanButton.setOnClickListener {
            toggleSavePlan()
        }

        applyPlanButton.setOnClickListener {
            applyPlan()
        }

        deletePlanButton.setOnClickListener {
            confirmDelete()
        }
    }

    private fun bindPlan(plan: CustomDietPlanLocalModel) = with(binding) {
        heroCoverImage.setImageResource(R.drawable.my_plan)
        heroTitle.text = plan.title

        badgeCaloriesText.text = getString(
            R.string.custom_diet_detail_badge_calorie,
            plan.dailyCalories
        )

        badgeDurationText.text = resources.getQuantityString(
            R.plurals.diet_plans_day_count,
            plan.durationDays,
            plan.durationDays
        )

        badgeMealsText.text = getString(
            R.string.custom_diet_detail_badge_meal,
            plan.mealCount
        )

        descriptionText.text = plan.description
            .takeIf { it.isNotBlank() }
            ?: getString(R.string.diet_plan_custom_default_desc)

        goalText.text = goalLabel(plan.goalType)

        macroCarbValueText.text = getString(
            R.string.custom_diet_detail_macro_gram,
            carbTarget(plan)
        )
        macroProteinValueText.text = getString(
            R.string.custom_diet_detail_macro_gram,
            proteinTarget(plan)
        )
        macroFatValueText.text = getString(
            R.string.custom_diet_detail_macro_gram,
            fatTarget(plan)
        )

        preferencesText.text = plan.preferences
            .map { preferenceLabel(it) }
            .takeIf { it.isNotEmpty() }
            ?.joinToString(" • ")
            ?: getString(R.string.custom_diet_detail_none)

        exclusionsText.text = plan.exclusions
            .takeIf { it.isNotEmpty() }
            ?.joinToString(" • ")
            ?: getString(R.string.custom_diet_detail_no_exclusion)

        mealOrderText.text = plan.mealOrder
            .mapIndexed { index, meal ->
                "${index + 1}. ${mealLabel(meal)}"
            }
            .joinToString("\n")

        renderSaveState()
        renderApplyState()
    }

    private fun toggleSavePlan() {
        val plan = currentPlan ?: return
        val isSavedNow = preferenceStore.toggleFavorite(plan.id)

        renderSaveState()

        Toast.makeText(
            requireContext(),
            getString(
                if (isSavedNow) {
                    R.string.diet_plans_favorite_added_message
                } else {
                    R.string.diet_plans_favorite_removed_message
                },
                plan.title
            ),
            Toast.LENGTH_SHORT
        ).show()

        notifyDietPlanChanged()
    }

    private fun applyPlan() {
        val plan = currentPlan ?: return

        preferenceStore.saveActivePlan(plan.id)
        preferenceStore.applyActivePlanToday()

        Toast.makeText(
            requireContext(),
            getString(R.string.diet_plans_apply_success, plan.title),
            Toast.LENGTH_SHORT
        ).show()

        notifyDietPlanChanged()
        renderApplyState()
    }

    private fun confirmDelete() {
        val plan = currentPlan ?: return

        MaterialAlertDialogBuilder(
            requireContext(),
            R.style.ThemeOverlay_Fitto_WhiteAlertDialog
        )
            .setTitle(getString(R.string.diet_plans_delete_custom_plan_title))
            .setMessage(getString(R.string.diet_plans_delete_custom_plan_message, plan.title))
            .setNegativeButton(getString(R.string.common_cancel), null)
            .setPositiveButton(getString(R.string.common_delete)) { _, _ ->
                deletePlan(plan)
            }
            .show()
    }

    private fun deletePlan(plan: CustomDietPlanLocalModel) {
        runCatching {
            customDietPlanStore.deletePlan(plan.id)
            preferenceStore.removeFavorite(plan.id)
            preferenceStore.clearActivePlanIf(plan.id)
        }.onSuccess {
            notifyDietPlanChanged()

            Toast.makeText(
                requireContext(),
                getString(R.string.diet_plans_delete_custom_plan_success),
                Toast.LENGTH_SHORT
            ).show()

            parentFragmentManager.popBackStack()
        }.onFailure {
            Toast.makeText(
                requireContext(),
                getString(R.string.diet_plans_delete_custom_plan_error),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun renderSaveState() = with(binding) {
        val plan = currentPlan ?: return@with
        val isSaved = preferenceStore.isFavorite(plan.id)

        savePlanButton.text = getString(
            if (isSaved) {
                R.string.custom_diet_detail_saved
            } else {
                R.string.diet_plan_detail_save_plan
            }
        )
    }

    private fun renderApplyState() = with(binding) {
        val plan = currentPlan ?: return@with
        val isActive = preferenceStore.getActivePlanId() == plan.id

        applyPlanButton.text = getString(
            if (isActive) {
                R.string.diet_plans_applied
            } else {
                R.string.diet_plans_apply
            }
        )

        applyPlanButton.isEnabled = !isActive
        applyPlanButton.alpha = if (isActive) 0.72f else 1f
    }

    private fun notifyDietPlanChanged() {
        parentFragmentManager.setFragmentResult(
            DietPlansFragment.REQUEST_DIET_PLAN_CHANGED,
            Bundle().apply {
                putString(
                    DietPlansFragment.BUNDLE_ACTIVE_PLAN_ID,
                    preferenceStore.getActivePlanId()
                )
            }
        )
    }

    private fun goalLabel(goalType: String): String {
        return when (goalType.uppercase()) {
            "WEIGHT_LOSS" -> getString(R.string.create_diet_plan_goal_weight_loss)
            "WEIGHT_GAIN" -> getString(R.string.create_diet_plan_goal_weight_gain)
            "MUSCLE" -> getString(R.string.create_diet_plan_goal_muscle)
            "MAINTAIN" -> getString(R.string.create_diet_plan_goal_maintain)
            "BALANCED" -> getString(R.string.create_diet_plan_goal_balanced)
            else -> getString(R.string.create_diet_plan_goal_balanced)
        }
    }

    private fun preferenceLabel(preference: String): String {
        return when (preference) {
            "vegetarian" -> getString(R.string.custom_diet_detail_preference_vegetarian)
            "vegan" -> getString(R.string.custom_diet_detail_preference_vegan)
            "gluten_free" -> getString(R.string.custom_diet_detail_preference_gluten_free)
            "lactose_free" -> getString(R.string.custom_diet_detail_preference_lactose_free)
            "high_protein" -> getString(R.string.custom_diet_detail_preference_high_protein)
            "low_carb" -> getString(R.string.custom_diet_detail_preference_low_carb)
            else -> preference
        }
    }

    private fun mealLabel(meal: String): String {
        return when (meal) {
            "breakfast" -> getString(R.string.create_post_breakfast)
            "snack" -> getString(R.string.create_post_snack)
            "lunch" -> getString(R.string.create_post_lunch)
            "dinner" -> getString(R.string.create_post_dinner)
            else -> meal
        }
    }

    private fun carbTarget(plan: CustomDietPlanLocalModel): Int {
        return ((plan.dailyCalories * 0.45) / 4.0).roundToInt()
    }

    private fun proteinTarget(plan: CustomDietPlanLocalModel): Int {
        return ((plan.dailyCalories * 0.25) / 4.0).roundToInt()
    }

    private fun fatTarget(plan: CustomDietPlanLocalModel): Int {
        return ((plan.dailyCalories * 0.30) / 9.0).roundToInt()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_PLAN_ID = "arg_plan_id"

        fun newInstance(planId: String): CustomDietPlanDetailFragment {
            return CustomDietPlanDetailFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PLAN_ID, planId)
                }
            }
        }
    }
}