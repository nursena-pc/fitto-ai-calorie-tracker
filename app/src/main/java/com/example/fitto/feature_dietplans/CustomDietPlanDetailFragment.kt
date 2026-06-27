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
            .map { resolveLabel(CustomDietPlanDetailLabelResolver.preference(it)) }
            .takeIf { it.isNotEmpty() }
            ?.joinToString(" • ")
            ?: getString(R.string.custom_diet_detail_none)

        exclusionsText.text = detail.exclusions
            .takeIf { it.isNotEmpty() }
            ?.joinToString(" • ")
            ?: getString(R.string.custom_diet_detail_no_exclusion)

        mealOrderText.text = CustomDietPlanDetailLabelResolver.mealOrder(detail.mealOrder)
            .joinToString("\n") { meal ->
                "${meal.order}. ${resolveLabel(meal.label)}"
            }

        renderSaveState()
        renderApplyState()
    }

    private fun toggleSavePlan() {
        val plan = currentPlan ?: return
        val favoriteResult = dietPlanPreferenceAction.toggleFavorite(plan.id)

        renderDetailEffect(
            CustomDietPlanDetailEffectResolver.favoriteToggled(
                planTitle = plan.title,
                result = favoriteResult
            )
        )
    }

    private fun applyPlan() {
        val plan = currentPlan ?: return
        val applyResult = dietPlanPreferenceAction.applyPlan(plan.id)

        renderDetailEffect(
            CustomDietPlanDetailEffectResolver.planApplied(
                planTitle = plan.title,
                result = applyResult
            )
        )
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
        val result = customPlanDeletionAction.delete(plan.id)
        renderDetailEffect(CustomDietPlanDetailEffectResolver.planDeleted(result))
    }

    private fun renderDetailEffect(effect: CustomDietPlanDetailEffect) {
        if (effect.shouldRenderSaveState) {
            renderSaveState()
        }

        if (effect.shouldNotifyDietPlanChanged) {
            notifyDietPlanChanged(activePlanId = effect.activePlanId)
        }

        effect.toastMessageRes?.let { messageRes ->
            Toast.makeText(
                requireContext(),
                effect.toastPlanTitle?.let { planTitle ->
                    getString(messageRes, planTitle)
                } ?: getString(messageRes),
                Toast.LENGTH_SHORT
            ).show()
        }

        if (effect.shouldRenderApplyState) {
            renderApplyState()
        }

        if (effect.shouldCloseScreen) {
            parentFragmentManager.popBackStack()
        }
    }

    private fun renderSaveState() = with(binding) {
        val plan = currentPlan ?: return@with
        val isSaved = preferenceStore.isFavorite(plan.id)

        savePlanButton.text = getString(
            CustomDietPlanDetailButtonStateResolver.save(isSaved).textRes
        )
    }

    private fun renderApplyState() = with(binding) {
        val plan = currentPlan ?: return@with
        val isActive = preferenceStore.getActivePlanId() == plan.id

        val buttonState = CustomDietPlanDetailButtonStateResolver.apply(isActive)

        applyPlanButton.text = getString(buttonState.textRes)
        applyPlanButton.isEnabled = buttonState.isEnabled
        applyPlanButton.alpha = buttonState.alpha
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

    private fun resolveLabel(label: CustomDietPlanDetailLabel): String {
        return label.stringRes?.let(::getString) ?: label.fallback
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
