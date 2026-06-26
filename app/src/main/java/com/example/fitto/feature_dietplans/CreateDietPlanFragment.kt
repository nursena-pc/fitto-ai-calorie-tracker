package com.example.fitto.feature_dietplans

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.fitto.R
import com.example.fitto.data.local.repository.ProfileCacheRepository
import com.example.fitto.databinding.FragmentCreateDietPlanBinding
import com.example.fitto.feature_dietplans.data.CustomDietPlanStore
import com.example.fitto.feature_dietplans.data.DietPlanPreferenceStore
import com.example.fitto.feature_dietplans.model.CustomDietPlanLocalModel
import com.example.fitto.feature_dietplans.planning.CustomDietMealPlanGenerator
import com.example.fitto.feature_home.presentation.social.POST_VISIBILITY_COMMUNITY
import com.example.fitto.feature_home.presentation.social.SocialRepository
import com.example.fitto.feature_meal.model.MealType
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CreateDietPlanFragment : Fragment(R.layout.fragment_create_diet_plan) {

    private var _binding: FragmentCreateDietPlanBinding? = null
    private val binding get() = _binding!!
    private var editingDraftPlan: CustomDietPlanLocalModel? = null
    private var isRenderingState: Boolean = false

    private lateinit var customDietPlanStore: CustomDietPlanStore
    private lateinit var dietPlanStore: DietPlanPreferenceStore
    private lateinit var profileCacheRepository: ProfileCacheRepository
    private lateinit var customPlanDeletionAction: CustomDietPlanDeletionAction
    private lateinit var dietPlanPreferenceAction: DietPlanPreferenceAction
    private lateinit var createDietPlanSaveAction: CreateDietPlanSaveAction
    private var exclusionController: CreateDietPlanExclusionController? = null
    private var uiRenderer: CreateDietPlanUiRenderer? = null
    private var selectedGoal: CreatePlanGoal = CreatePlanGoal.WEIGHT_LOSS
    private var selectedPrivacy: CreatePlanPrivacy = CreatePlanPrivacy.PRIVATE
    private var dailyCalories: Int = 1800
    private var durationDays: Int = 30

    private val selectedMealTypes = linkedSetOf(
        MealType.BREAKFAST,
        MealType.SNACK,
        MealType.LUNCH,
        MealType.DINNER
    )

    private val selectedPreferences = mutableSetOf<String>()
    private val socialRepository by lazy { SocialRepository() }
    private val activeExclusionController: CreateDietPlanExclusionController
        get() = requireNotNull(exclusionController)
    private val activeUiRenderer: CreateDietPlanUiRenderer
        get() = requireNotNull(uiRenderer)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCreateDietPlanBinding.bind(view)

        val appContext = requireContext().applicationContext
        customDietPlanStore = CustomDietPlanStore(appContext)
        dietPlanStore = DietPlanPreferenceStore(requireContext())
        profileCacheRepository = ProfileCacheRepository(appContext)
        customPlanDeletionAction = CustomDietPlanDeletionAction.from(
            customPlanStore = customDietPlanStore,
            preferenceStore = dietPlanStore
        )
        dietPlanPreferenceAction = DietPlanPreferenceAction.from(dietPlanStore)
        createDietPlanSaveAction = CreateDietPlanSaveAction.from(
            customPlanStore = customDietPlanStore,
            preferenceAction = dietPlanPreferenceAction
        )
        uiRenderer = CreateDietPlanUiRenderer(
            context = requireContext(),
            binding = binding
        )
        exclusionController = CreateDietPlanExclusionController(
            context = requireContext(),
            chipGroup = binding.exclusionsChipGroup,
            onChanged = ::renderState
        )

        val hasEditingDraft = loadEditingDraftIfNeeded()

        setupInitialValues(useProfileDefaults = !hasEditingDraft)
        setupInputs()
        setupClicks()
        setupPreferenceChips()
        setupMealOrderChips()

        if (!hasEditingDraft) {
            setupDefaultExclusions()
        }

        renderDraftEditMode()
        renderState()
    }
    private fun setupPreferenceChips() = with(binding) {
        bindPreferenceChip(preferenceVegetarianChip, CustomDietMealPlanGenerator.PREF_VEGETARIAN)
        bindPreferenceChip(preferenceVeganChip, CustomDietMealPlanGenerator.PREF_VEGAN)
        bindPreferenceChip(preferenceGlutenFreeChip, CustomDietMealPlanGenerator.PREF_GLUTEN_FREE)
        bindPreferenceChip(preferenceLactoseFreeChip, CustomDietMealPlanGenerator.PREF_LACTOSE_FREE)
        bindPreferenceChip(preferenceHighProteinChip, CustomDietMealPlanGenerator.PREF_HIGH_PROTEIN)
        bindPreferenceChip(preferenceLowCarbChip, CustomDietMealPlanGenerator.PREF_LOW_CARB)
    }
    private fun setupMealOrderChips() = with(binding) {
        bindMealOrderChip(mealBreakfastChip, MealType.BREAKFAST)
        bindMealOrderChip(mealSnackChip, MealType.SNACK)
        bindMealOrderChip(mealLunchChip, MealType.LUNCH)
        bindMealOrderChip(mealDinnerChip, MealType.DINNER)
    }

    private fun bindMealOrderChip(chip: Chip, mealType: MealType) {
        chip.setOnCheckedChangeListener { _, isChecked ->
            if (isRenderingState) return@setOnCheckedChangeListener

            updateSelectedMealTypes(
                CreateDietPlanSelectionReducer.toggleMealType(
                    currentMealTypes = selectedMealTypes,
                    mealType = mealType,
                    isChecked = isChecked
                )
            )

            renderState()
        }
    }
    private fun bindPreferenceChip(chip: Chip, preferenceKey: String) {
        chip.setOnCheckedChangeListener { _, isChecked ->
            if (isRenderingState) return@setOnCheckedChangeListener

            updateSelectedPreferences(
                CreateDietPlanSelectionReducer.togglePreference(
                    currentPreferences = selectedPreferences,
                    preferenceKey = preferenceKey,
                    isChecked = isChecked
                )
            )

            renderState()
        }
    }

    private fun updateSelectedMealTypes(mealTypes: Collection<MealType>) {
        selectedMealTypes.clear()
        selectedMealTypes.addAll(mealTypes)
    }

    private fun updateSelectedPreferences(preferences: Collection<String>) {
        selectedPreferences.clear()
        selectedPreferences.addAll(preferences)
    }
    private fun setupInitialValues(useProfileDefaults: Boolean = true) {
        if (!useProfileDefaults) return

        viewLifecycleOwner.lifecycleScope.launch {
            val profile = withContext(Dispatchers.IO) {
                profileCacheRepository.getCurrentUserProfile()
                    ?: profileCacheRepository.syncCurrentUserProfileFromFirebase()
            }

            if (_binding == null) return@launch

            val defaults = CreateDietPlanDefaultResolver.resolve(profile)
            dailyCalories = defaults.dailyCalories
            selectedGoal = defaults.goal

            renderState()
        }
    }
    private fun loadEditingDraftIfNeeded(): Boolean {
        val planId = arguments?.getString(ARG_EDIT_DRAFT_PLAN_ID).orEmpty()
        if (planId.isBlank()) return false

        val draft = customDietPlanStore.getPlan(planId)
            ?.takeIf { it.isDraft }
            ?: return false

        editingDraftPlan = draft
        applyEditableState(CreateDietPlanDraftMapper.toEditableState(draft))

        return true
    }

    private fun applyEditableState(state: CreateDietPlanEditableState) = with(binding) {
        planNameInput.setText(state.planName)
        planDescriptionInput.setText(state.description)

        selectedGoal = state.goal
        selectedPrivacy = state.privacy
        dailyCalories = state.dailyCalories
        durationDays = state.durationDays

        updateSelectedPreferences(state.preferences)
        activeExclusionController.setValues(state.exclusions)
        updateSelectedMealTypes(state.mealTypes)
    }

    private fun renderDraftEditMode() = with(binding) {
        deleteDraftButton.isVisible = editingDraftPlan != null
    }

    private fun setupInputs() = with(binding) {
        planNameInput.doAfterTextChanged {
            planNameInput.error = null
        }

        planDescriptionInput.doAfterTextChanged {
            renderState()
        }
    }

    private fun setupClicks() = with(binding) {
        backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        topDraftSaveButton.setOnClickListener {
            savePlan(isDraft = true)
        }

        createListButton.setOnClickListener {
            savePlan(isDraft = false)
        }
        deleteDraftButton.setOnClickListener {
            confirmDeleteEditingDraft()
        }

        privacyPrivateCard.setOnClickListener {
            selectedPrivacy = CreatePlanPrivacy.PRIVATE
            renderState()
        }

        privacyPublicCard.setOnClickListener {
            selectedPrivacy = CreatePlanPrivacy.PUBLIC
            renderState()
        }

        goalWeightLossCard.setOnClickListener {
            selectedGoal = CreatePlanGoal.WEIGHT_LOSS
            renderState()
        }

        goalWeightGainCard.setOnClickListener {
            selectedGoal = CreatePlanGoal.WEIGHT_GAIN
            renderState()
        }

        goalMuscleCard.setOnClickListener {
            selectedGoal = CreatePlanGoal.MUSCLE
            renderState()
        }

        goalMaintainCard.setOnClickListener {
            selectedGoal = CreatePlanGoal.MAINTAIN
            renderState()
        }

        goalBalancedCard.setOnClickListener {
            selectedGoal = CreatePlanGoal.BALANCED
            renderState()
        }

        calorieMinusButton.setOnClickListener {
            dailyCalories = CreateDietPlanNumberRules.decreaseCalories(dailyCalories)
            renderState()
        }

        caloriePlusButton.setOnClickListener {
            dailyCalories = CreateDietPlanNumberRules.increaseCalories(dailyCalories)
            renderState()
        }
        calorieValueText.setOnClickListener {
            showCalorieInputDialog()
        }
        durationMinusButton.setOnClickListener {
            durationDays = CreateDietPlanNumberRules.decreaseDuration(durationDays)
            renderState()
        }

        durationPlusButton.setOnClickListener {
            durationDays = CreateDietPlanNumberRules.increaseDuration(durationDays)
            renderState()
        }

        durationValueText.setOnClickListener {
            showDurationInputDialog()
        }

        addExcludedFoodButton.setOnClickListener {
            activeExclusionController.showAddDialog()
        }
    }

    private fun confirmDeleteEditingDraft() {
        val draft = editingDraftPlan ?: return

        MaterialAlertDialogBuilder(
            requireContext(),
            R.style.ThemeOverlay_Fitto_WhiteAlertDialog
        )
            .setTitle(getString(R.string.create_diet_plan_delete_draft_title))
            .setMessage(getString(R.string.create_diet_plan_delete_draft_message, draft.title))
            .setNegativeButton(getString(R.string.common_cancel), null)
            .setPositiveButton(getString(R.string.common_delete)) { _, _ ->
                deleteEditingDraft(draft)
            }
            .show()
    }

    private fun deleteEditingDraft(draft: CustomDietPlanLocalModel) {
        when (customPlanDeletionAction.delete(draft.id)) {
            is CustomDietPlanDeletionResult.Deleted -> {
                parentFragmentManager.setFragmentResult(
                    DietPlansFragment.REQUEST_DIET_PLAN_CHANGED,
                    Bundle().apply {
                        putString(
                            DietPlansFragment.BUNDLE_SELECT_TAB,
                            DietPlansFragment.TAB_DRAFTS
                        )
                    }
                )

                Toast.makeText(
                    requireContext(),
                    getString(R.string.create_diet_plan_delete_draft_success),
                    Toast.LENGTH_SHORT
                ).show()

                parentFragmentManager.popBackStack()
            }

            is CustomDietPlanDeletionResult.Failed -> {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.create_diet_plan_delete_draft_error),
                    Toast.LENGTH_SHORT
                ).show()
            }

            CustomDietPlanDeletionResult.NotCustomPlan -> Unit
        }
    }

    private fun setupDefaultExclusions() {
        activeExclusionController.setValues(defaultExclusions())
    }

    private fun defaultExclusions(): List<String> {
        return listOf(
            getString(R.string.create_diet_plan_exclusion_broccoli),
            getString(R.string.create_diet_plan_exclusion_mushroom),
            getString(R.string.create_diet_plan_exclusion_milk),
            getString(R.string.create_diet_plan_exclusion_peanut),
            getString(R.string.create_diet_plan_exclusion_seafood)
        )
    }
    private fun showCalorieInputDialog() {
        CreateDietPlanNumberInputDialog(requireContext()).show(
            config = CreateDietPlanNumberInputConfigs.calories(dailyCalories)
        ) { value ->
            dailyCalories = value
            renderState()
        }
    }

    private fun showDurationInputDialog() {
        CreateDietPlanNumberInputDialog(requireContext()).show(
            config = CreateDietPlanNumberInputConfigs.duration(durationDays)
        ) { value ->
            durationDays = value
            renderState()
        }
    }
    private fun renderState() {
        isRenderingState = true

        try {
            activeUiRenderer.render(currentRenderState())
        } finally {
            isRenderingState = false
        }
    }

    private fun currentRenderState(): CreateDietPlanRenderState {
        return CreateDietPlanRenderState(
            dailyCalories = dailyCalories,
            durationDays = durationDays,
            selectedGoal = selectedGoal,
            selectedPrivacy = selectedPrivacy,
            selectedMealTypes = selectedMealTypes.toList(),
            selectedPreferences = selectedPreferences.toSet()
        )
    }

    private fun currentFormState() = with(binding) {
        CreateDietPlanFormState(
            planName = planNameInput.text?.toString()?.trim().orEmpty(),
            description = planDescriptionInput.text?.toString()?.trim().orEmpty(),
            goal = selectedGoal,
            privacy = selectedPrivacy,
            dailyCalories = dailyCalories,
            durationDays = durationDays,
            mealTypes = selectedMealTypes.toList(),
            preferences = selectedPreferences.toList(),
            exclusions = activeExclusionController.values()
        )
    }

    private fun savePlan(isDraft: Boolean) = with(binding) {
        val formState = currentFormState()
        val existingPlan = editingDraftPlan

        viewLifecycleOwner.lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                createDietPlanSaveAction.save(
                    formState = formState,
                    existingPlan = existingPlan,
                    isDraft = isDraft
                )
            }

            if (_binding == null) return@launch
            handleSaveResult(result)
        }
    }

    private suspend fun handleSaveResult(result: CreateDietPlanSaveResult) {
        when (result) {
            is CreateDietPlanSaveResult.ValidationFailed -> {
                handleSaveValidation(result.validation)
            }

            is CreateDietPlanSaveResult.DraftSaved -> {
                handleDraftSaved(result)
            }

            is CreateDietPlanSaveResult.PlanCreated -> {
                handlePlanCreated(result)
            }
        }
    }

    private fun handleSaveValidation(validation: CreateDietPlanSaveValidation) = with(binding) {
        when (validation) {
            CreateDietPlanSaveValidation.MISSING_NAME -> {
                planNameInput.error = getString(R.string.create_diet_plan_name_required)
                planNameInput.requestFocus()
            }

            CreateDietPlanSaveValidation.DUPLICATE_NAME -> {
                planNameInput.error = getString(R.string.create_diet_plan_name_duplicate)
                planNameInput.requestFocus()
            }

            CreateDietPlanSaveValidation.VALID -> Unit
        }
    }

    private fun handleDraftSaved(result: CreateDietPlanSaveResult.DraftSaved) {
        editingDraftPlan = result.plan

        parentFragmentManager.setFragmentResult(
            DietPlansFragment.REQUEST_DIET_PLAN_CHANGED,
            Bundle().apply {
                putString(
                    DietPlansFragment.BUNDLE_SELECT_TAB,
                    DietPlansFragment.TAB_DRAFTS
                )
            }
        )

        Toast.makeText(
            requireContext(),
            getString(
                if (result.wasUpdate) {
                    R.string.create_diet_plan_draft_updated
                } else {
                    R.string.create_diet_plan_saved
                }
            ),
            Toast.LENGTH_SHORT
        ).show()

        parentFragmentManager.popBackStack()
    }

    private suspend fun handlePlanCreated(result: CreateDietPlanSaveResult.PlanCreated) {
        if (result.plan.isPublic) {
            shareCustomPlanToSocial(
                plan = result.plan,
                goal = result.goal
            )
        }

        parentFragmentManager.setFragmentResult(
            DietPlansFragment.REQUEST_DIET_PLAN_CHANGED,
            Bundle().apply {
                putString(
                    DietPlansFragment.BUNDLE_ACTIVE_PLAN_ID,
                    result.applyResult.activePlanId
                )
                putString(DietPlansFragment.BUNDLE_SELECT_TAB, DietPlansFragment.TAB_MY)
            }
        )

        Toast.makeText(
            requireContext(),
            getString(R.string.create_diet_plan_created),
            Toast.LENGTH_SHORT
        ).show()

        parentFragmentManager.popBackStack()
    }

    private suspend fun shareCustomPlanToSocial(
        plan: CustomDietPlanLocalModel,
        goal: CreatePlanGoal
    ) {
        runCatching {
            socialRepository.createPost(
                content = getString(
                    R.string.create_diet_plan_social_share_content,
                    getString(goal.titleRes),
                    plan.dailyCalories,
                    plan.durationDays
                ),
                imageUri = null,
                mealInfo = getString(R.string.create_diet_plan_social_share_meal_info),
                calories = plan.dailyCalories,
                allowComments = true,
                visibility = POST_VISIBILITY_COMMUNITY,
                tags = listOf(
                    getString(R.string.diet_plan_tag_custom),
                    getString(goal.titleRes)
                ),
                isDraft = false
            )
        }
    }

    override fun onDestroyView() {
        exclusionController = null
        uiRenderer = null
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_EDIT_DRAFT_PLAN_ID = "arg_edit_draft_plan_id"

        fun newEditDraftInstance(planId: String): CreateDietPlanFragment {
            return CreateDietPlanFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_EDIT_DRAFT_PLAN_ID, planId)
                }
            }
        }
    }
}
