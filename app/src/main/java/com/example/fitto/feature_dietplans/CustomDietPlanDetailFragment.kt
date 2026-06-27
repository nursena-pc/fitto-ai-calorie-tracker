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

class CustomDietPlanDetailFragment : Fragment(R.layout.fragment_custom_diet_plan_detail) {

    private var _binding: FragmentCustomDietPlanDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var customDietPlanStore: CustomDietPlanStore
    private lateinit var preferenceStore: DietPlanPreferenceStore
    private lateinit var customPlanDeletionAction: CustomDietPlanDeletionAction
    private lateinit var dietPlanPreferenceAction: DietPlanPreferenceAction

    private val planId: String by lazy {
        requireArguments().getString(ARG_PLAN_ID).orEmpty()
    }

    private var currentPlan: CustomDietPlanLocalModel? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCustomDietPlanDetailBinding.bind(view)

        customDietPlanStore = CustomDietPlanStore(requireContext().applicationContext)
        preferenceStore = DietPlanPreferenceStore(requireContext())
        customPlanDeletionAction = CustomDietPlanDeletionAction.from(
            customPlanStore = customDietPlanStore,
            preferenceStore = preferenceStore
        )
        dietPlanPreferenceAction = DietPlanPreferenceAction.from(preferenceStore)

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
        val detail = CustomDietPlanDetailMapper.map(plan)

        heroCoverImage.setImageResource(R.drawable.my_plan)
        heroTitle.text = detail.title

        badgeCaloriesText.text = getString(
            R.string.custom_diet_detail_badge_calorie,
            detail.dailyCalories
        )

        badgeDurationText.text = resources.getQuantityString(
            R.plurals.diet_plans_day_count,
            detail.durationDays,
            detail.durationDays
        )

        badgeMealsText.text = getString(
            R.string.custom_diet_detail_badge_meal,
            detail.mealCount
        )

        descriptionText.text = detail.description
            .takeIf { it.isNotBlank() }
            ?: getString(R.string.diet_plan_custom_default_desc)

        goalText.setText(detail.goal.titleRes)

        macroCarbValueText.text = getString(
            R.string.custom_diet_detail_macro_gram,
            detail.macroTargets.carbGrams
        )
        macroProteinValueText.text = getString(
            R.string.custom_diet_detail_macro_gram,
            detail.macroTargets.proteinGrams
        )
        macroFatValueText.text = getString(
            R.string.custom_diet_detail_macro_gram,
            detail.macroTargets.fatGrams
        )

        preferencesText.text = detail.preferences
            .map { preferenceLabel(it) }
            .takeIf { it.isNotEmpty() }
            ?.joinToString(" • ")
            ?: getString(R.string.custom_diet_detail_none)

        exclusionsText.text = detail.exclusions
            .takeIf { it.isNotEmpty() }
            ?.joinToString(" • ")
            ?: getString(R.string.custom_diet_detail_no_exclusion)

        mealOrderText.text = detail.mealOrder
            .mapIndexed { index, meal ->
                "${index + 1}. ${mealLabel(meal)}"
            }
            .joinToString("\n")

        renderSaveState()
        renderApplyState()
    }

    private fun toggleSavePlan() {
        val plan = currentPlan ?: return
        val favoriteResult = dietPlanPreferenceAction.toggleFavorite(plan.id)

        renderSaveState()

        Toast.makeText(
            requireContext(),
            getString(
                if (favoriteResult.isFavorite) {
                    R.string.diet_plans_favorite_added_message
                } else {
                    R.string.diet_plans_favorite_removed_message
                },
                plan.title
            ),
            Toast.LENGTH_SHORT
        ).show()

        notifyDietPlanChanged(activePlanId = favoriteResult.activePlanId)
    }

    private fun applyPlan() {
        val plan = currentPlan ?: return

        val applyResult = dietPlanPreferenceAction.applyPlan(plan.id)

        Toast.makeText(
            requireContext(),
            getString(R.string.diet_plans_apply_success, plan.title),
            Toast.LENGTH_SHORT
        ).show()

        notifyDietPlanChanged(activePlanId = applyResult.activePlanId)
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
        when (val result = customPlanDeletionAction.delete(plan.id)) {
            is CustomDietPlanDeletionResult.Deleted -> {
                notifyDietPlanChanged(activePlanId = result.activePlanId)

                Toast.makeText(
                    requireContext(),
                    getString(R.string.diet_plans_delete_custom_plan_success),
                    Toast.LENGTH_SHORT
                ).show()

                parentFragmentManager.popBackStack()
            }

            is CustomDietPlanDeletionResult.Failed -> {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.diet_plans_delete_custom_plan_error),
                    Toast.LENGTH_SHORT
                ).show()
            }

            CustomDietPlanDeletionResult.NotCustomPlan -> Unit
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

    private fun notifyDietPlanChanged(
        activePlanId: String? = preferenceStore.getActivePlanId()
    ) {
        parentFragmentManager.setFragmentResult(
            DietPlansFragment.REQUEST_DIET_PLAN_CHANGED,
            Bundle().apply {
                putString(
                    DietPlansFragment.BUNDLE_ACTIVE_PLAN_ID,
                    activePlanId
                )
            }
        )
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
