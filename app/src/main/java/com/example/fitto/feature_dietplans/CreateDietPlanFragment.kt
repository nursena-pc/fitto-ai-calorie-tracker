package com.example.fitto.feature_dietplans

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.fitto.R
import com.example.fitto.data.local.repository.ProfileCacheRepository
import com.example.fitto.databinding.FragmentCreateDietPlanBinding
import com.example.fitto.feature_dietplans.data.CustomDietPlanStore
import com.example.fitto.feature_dietplans.data.DietPlanPreferenceStore
import com.example.fitto.feature_dietplans.model.CustomDietPlanLocalModel
import com.example.fitto.feature_home.presentation.social.SocialRepository
import com.example.fitto.feature_meal.model.MealType
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CreateDietPlanFragment :
    Fragment(R.layout.fragment_create_diet_plan),
    CreateDietPlanInteractionBinder.Listener {

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
    private lateinit var textResolver: CreateDietPlanTextResolver
    private val completionAction = CreateDietPlanCompletionAction()
    private var exclusionController: CreateDietPlanExclusionController? = null
    private var uiRenderer: CreateDietPlanUiRenderer? = null
    private var interactionBinder: CreateDietPlanInteractionBinder? = null
    private val planStateStore = CreateDietPlanStateStore()
    private val socialShareAction by lazy {
        CreateDietPlanSocialShareAction.from(SocialRepository())
    }
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
        textResolver = CreateDietPlanTextResolver.from(requireContext())
        uiRenderer = CreateDietPlanUiRenderer(
            context = requireContext(),
            binding = binding
        )
        interactionBinder = CreateDietPlanInteractionBinder(binding)
        exclusionController = CreateDietPlanExclusionController(
            context = requireContext(),
            chipGroup = binding.exclusionsChipGroup,
            onChanged = ::renderState
        )

        val hasEditingDraft = loadEditingDraftIfNeeded()

        setupInitialValues(useProfileDefaults = !hasEditingDraft)
        interactionBinder?.bind(this)

        if (!hasEditingDraft) {
            setupDefaultExclusions()
        }

        renderDraftEditMode()
        renderState()
    }

    private fun setupInitialValues(useProfileDefaults: Boolean = true) {
        if (!useProfileDefaults) return

        viewLifecycleOwner.lifecycleScope.launch {
            val profile = withContext(Dispatchers.IO) {
                profileCacheRepository.getCurrentUserProfile()
                    ?: profileCacheRepository.syncCurrentUserProfileFromFirebase()
            }

            if (_binding == null) return@launch

            planStateStore.applyDefaults(CreateDietPlanDefaultResolver.resolve(profile))
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

        planStateStore.applyEditableState(state)
        activeExclusionController.setValues(state.exclusions)
    }

    private fun renderDraftEditMode() = with(binding) {
        deleteDraftButton.isVisible = editingDraftPlan != null
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
                handleCompletionEffect(completionAction.draftDeleted())
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
        activeExclusionController.setValues(textResolver.defaultExclusions())
    }
    private fun showCalorieInputDialog() {
        CreateDietPlanNumberInputDialog(requireContext()).show(
            config = CreateDietPlanNumberInputConfigs.calories(planStateStore.dailyCalories)
        ) { value ->
            planStateStore.setDailyCalories(value)
            renderState()
        }
    }

    private fun showDurationInputDialog() {
        CreateDietPlanNumberInputDialog(requireContext()).show(
            config = CreateDietPlanNumberInputConfigs.duration(planStateStore.durationDays)
        ) { value ->
            planStateStore.setDurationDays(value)
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
        return planStateStore.toRenderState()
    }

    private fun currentFormState() = with(binding) {
        planStateStore.toFormState(
            planName = planNameInput.text?.toString()?.trim().orEmpty(),
            description = planDescriptionInput.text?.toString()?.trim().orEmpty(),
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
        handleCompletionEffect(completionAction.draftSaved(result.wasUpdate))
    }

    private suspend fun handlePlanCreated(result: CreateDietPlanSaveResult.PlanCreated) {
        socialShareAction.share(
            plan = result.plan,
            text = textResolver.socialShareText(result.goal, result.plan)
        )

        handleCompletionEffect(completionAction.planCreated(result.applyResult.activePlanId))
    }

    private fun handleCompletionEffect(effect: CreateDietPlanCompletionEffect) {
        publishDietPlanChanged(effect.result)

        Toast.makeText(
            requireContext(),
            getString(effect.message.stringRes),
            Toast.LENGTH_SHORT
        ).show()

        if (effect.closeScreen) {
            parentFragmentManager.popBackStack()
        }
    }

    private fun publishDietPlanChanged(result: CreateDietPlanChangeResult) {
        parentFragmentManager.setFragmentResult(
            DietPlansFragment.REQUEST_DIET_PLAN_CHANGED,
            Bundle().apply {
                result.activePlanId?.let { activePlanId ->
                    putString(DietPlansFragment.BUNDLE_ACTIVE_PLAN_ID, activePlanId)
                }

                putString(DietPlansFragment.BUNDLE_SELECT_TAB, result.selectedTab)
            }
        )
    }

    private val CreateDietPlanCompletionMessage.stringRes: Int
        get() = when (this) {
            CreateDietPlanCompletionMessage.DRAFT_SAVED -> R.string.create_diet_plan_saved
            CreateDietPlanCompletionMessage.DRAFT_UPDATED -> R.string.create_diet_plan_draft_updated
            CreateDietPlanCompletionMessage.DRAFT_DELETED -> R.string.create_diet_plan_delete_draft_success
            CreateDietPlanCompletionMessage.PLAN_CREATED -> R.string.create_diet_plan_created
        }


    override fun onPlanNameChanged() = with(binding) {
        planNameInput.error = null
    }

    override fun onPlanDescriptionChanged() {
        renderState()
    }

    override fun onBackClicked() {
        parentFragmentManager.popBackStack()
    }

    override fun onSaveDraftClicked() {
        savePlan(isDraft = true)
    }

    override fun onCreatePlanClicked() {
        savePlan(isDraft = false)
    }

    override fun onDeleteDraftClicked() {
        confirmDeleteEditingDraft()
    }

    override fun onPrivacySelected(privacy: CreatePlanPrivacy) {
        planStateStore.setPrivacy(privacy)
        renderState()
    }

    override fun onGoalSelected(goal: CreatePlanGoal) {
        planStateStore.setGoal(goal)
        renderState()
    }

    override fun onCalorieDecreaseClicked() {
        planStateStore.decreaseCalories()
        renderState()
    }

    override fun onCalorieIncreaseClicked() {
        planStateStore.increaseCalories()
        renderState()
    }

    override fun onCalorieValueClicked() {
        showCalorieInputDialog()
    }

    override fun onDurationDecreaseClicked() {
        planStateStore.decreaseDuration()
        renderState()
    }

    override fun onDurationIncreaseClicked() {
        planStateStore.increaseDuration()
        renderState()
    }

    override fun onDurationValueClicked() {
        showDurationInputDialog()
    }

    override fun onAddExclusionClicked() {
        activeExclusionController.showAddDialog()
    }

    override fun onPreferenceToggled(preferenceKey: String, isChecked: Boolean) {
        if (isRenderingState) return

        planStateStore.togglePreference(preferenceKey, isChecked)
        renderState()
    }

    override fun onMealTypeToggled(mealType: MealType, isChecked: Boolean) {
        if (isRenderingState) return

        planStateStore.toggleMealType(mealType, isChecked)
        renderState()
    }

    override fun onDestroyView() {
        exclusionController = null
        uiRenderer = null
        interactionBinder = null
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
