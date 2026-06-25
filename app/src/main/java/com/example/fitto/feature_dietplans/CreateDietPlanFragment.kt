package com.example.fitto.feature_dietplans

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.fitto.R
import com.example.fitto.data.local.repository.ProfileCacheRepository
import com.example.fitto.databinding.DialogPlanNumberPickerBinding
import com.example.fitto.databinding.FragmentCreateDietPlanBinding
import com.example.fitto.feature_dietplans.data.CustomDietPlanStore
import com.example.fitto.feature_dietplans.data.DietPlanPreferenceStore
import com.example.fitto.feature_dietplans.data.UserNutritionTargetStore
import com.example.fitto.feature_dietplans.model.CustomDietPlanLocalModel
import com.example.fitto.feature_dietplans.planning.CustomDietMealPlanGenerator
import com.example.fitto.feature_home.presentation.social.POST_VISIBILITY_COMMUNITY
import com.example.fitto.feature_home.presentation.social.SocialRepository
import com.example.fitto.feature_meal.model.MealType
import com.google.android.material.card.MaterialCardView
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
    private lateinit var nutritionTargetStore: UserNutritionTargetStore
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
    private val exclusions = mutableSetOf<String>()

    private val socialRepository by lazy { SocialRepository() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCreateDietPlanBinding.bind(view)

        val appContext = requireContext().applicationContext
        customDietPlanStore = CustomDietPlanStore(appContext)
        dietPlanStore = DietPlanPreferenceStore(requireContext())
        profileCacheRepository = ProfileCacheRepository(appContext)
        nutritionTargetStore = UserNutritionTargetStore(requireContext())

        val hasEditingDraft = loadEditingDraftIfNeeded()

        setupInitialValues(useProfileDefaults = !hasEditingDraft)
        setupInputs()
        setupClicks()
        setupPreferenceChips()
        setupMealOrderChips()

        if (hasEditingDraft) {
            renderExclusionChipsFromState()
        } else {
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

            if (isChecked) {
                selectedMealTypes.add(mealType)
            } else {
                if (selectedMealTypes.size == 1) {
                    chip.isChecked = true
                    return@setOnCheckedChangeListener
                }

                selectedMealTypes.remove(mealType)
            }

            renderState()
        }
    }
    private fun showNumberInputDialog(
        icon: String,
        title: String,
        helperText: String,
        unitText: String,
        initialValue: Int,
        validRange: IntRange,
        step: Int,
        errorText: String,
        onValueSaved: (Int) -> Unit
    ) {
        val dialogBinding = DialogPlanNumberPickerBinding.inflate(
            LayoutInflater.from(requireContext())
        )

        fun currentValue(): Int {
            return dialogBinding.numberDialogInput.text
                ?.toString()
                ?.trim()
                ?.toIntOrNull()
                ?: initialValue
        }

        fun setValue(value: Int) {
            val safeValue = value.coerceIn(validRange)
            dialogBinding.numberDialogInput.setText(safeValue.toString())
            dialogBinding.numberDialogInput.setSelection(
                dialogBinding.numberDialogInput.text?.length ?: 0
            )
            dialogBinding.numberDialogRangeText.text = "${validRange.first} - ${validRange.last} $unitText aralığında olmalı"
            dialogBinding.numberDialogRangeText.setTextColor(
                requireContext().getColor(R.color.fitto_text_secondary)
            )
        }

        val dialog = Dialog(requireContext()).apply {
            requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
            setContentView(dialogBinding.root)
            setCanceledOnTouchOutside(true)
        }

        with(dialogBinding) {
            numberDialogIconText.text = icon
            numberDialogTitleText.text = title
            numberDialogHelperText.text = helperText
            numberDialogUnitText.text = unitText

            setValue(initialValue)

            numberDialogMinusButton.setOnClickListener {
                setValue(currentValue() - step)
            }

            numberDialogPlusButton.setOnClickListener {
                setValue(currentValue() + step)
            }

            numberDialogCancelButton.setOnClickListener {
                dialog.dismiss()
            }

            numberDialogSaveButton.setOnClickListener {
                val value = numberDialogInput.text
                    ?.toString()
                    ?.trim()
                    ?.toIntOrNull()

                if (value == null || value !in validRange) {
                    numberDialogRangeText.text = errorText
                    numberDialogRangeText.setTextColor(
                        requireContext().getColor(R.color.fitto_error)
                    )
                    return@setOnClickListener
                }

                onValueSaved(value)
                dialog.dismiss()
            }
        }

        dialog.setOnShowListener {
            dialog.window?.let { window ->
                window.setBackgroundDrawableResource(android.R.color.transparent)
                window.setLayout(
                    (resources.displayMetrics.widthPixels * 0.90f).toInt(),
                    WindowManager.LayoutParams.WRAP_CONTENT
                )
            }
        }

        dialog.show()
    }

    private fun bindPreferenceChip(chip: Chip, preferenceKey: String) {
        chip.setOnCheckedChangeListener { _, isChecked ->
            if (isRenderingState) return@setOnCheckedChangeListener

            if (isChecked) {
                if (preferenceKey == CustomDietMealPlanGenerator.PREF_VEGAN) {
                    binding.preferenceVegetarianChip.isChecked = false
                } else if (preferenceKey == CustomDietMealPlanGenerator.PREF_VEGETARIAN) {
                    binding.preferenceVeganChip.isChecked = false
                }

                selectedPreferences.add(preferenceKey)
            } else {
                selectedPreferences.remove(preferenceKey)
            }

            renderState()
        }
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
    private fun loadEditingDraftIfNeeded(): Boolean = with(binding) {
        val planId = arguments?.getString(ARG_EDIT_DRAFT_PLAN_ID).orEmpty()
        if (planId.isBlank()) return false

        val draft = customDietPlanStore.getPlan(planId)
            ?.takeIf { it.isDraft }
            ?: return false

        editingDraftPlan = draft

        planNameInput.setText(draft.title)
        planDescriptionInput.setText(draft.description)

        selectedGoal = runCatching {
            CreatePlanGoal.valueOf(draft.goalType)
        }.getOrDefault(CreatePlanGoal.BALANCED)

        selectedPrivacy = if (draft.isPublic) {
            CreatePlanPrivacy.PUBLIC
        } else {
            CreatePlanPrivacy.PRIVATE
        }

        dailyCalories = draft.dailyCalories
        durationDays = draft.durationDays

        selectedPreferences.clear()
        selectedPreferences.addAll(draft.preferences)

        exclusions.clear()
        exclusions.addAll(draft.exclusions)

        selectedMealTypes.clear()
        val draftMeals = draft.mealOrder.mapNotNull { mealKey ->
            MealType.entries.firstOrNull { it.argumentValue == mealKey }
        }

        selectedMealTypes.addAll(
            draftMeals.ifEmpty {
                listOf(
                    MealType.BREAKFAST,
                    MealType.SNACK,
                    MealType.LUNCH,
                    MealType.DINNER
                )
            }
        )

        true
    }

    private fun renderDraftEditMode() = with(binding) {
        deleteDraftButton.isVisible = editingDraftPlan != null
    }

    private fun renderExclusionChipsFromState() = with(binding) {
        val currentExclusions = exclusions.toList()

        exclusionsChipGroup.removeAllViews()
        exclusions.clear()

        currentExclusions.forEach { exclusion ->
            addExclusionChip(exclusion)
        }
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
            dailyCalories = (dailyCalories - 50).coerceAtLeast(800)
            renderState()
        }

        caloriePlusButton.setOnClickListener {
            dailyCalories = (dailyCalories + 50).coerceAtMost(5000)
            renderState()
        }
        calorieValueText.setOnClickListener {
            showCalorieInputDialog()
        }
        durationMinusButton.setOnClickListener {
            durationDays = (durationDays - 1).coerceAtLeast(1)
            renderState()
        }

        durationPlusButton.setOnClickListener {
            durationDays = (durationDays + 1).coerceAtMost(365)
            renderState()
        }

        durationValueText.setOnClickListener {
            showDurationInputDialog()
        }

        addExcludedFoodButton.setOnClickListener {
            showAddExclusionDialog()
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
        runCatching {
            customDietPlanStore.deletePlan(draft.id)
            dietPlanStore.removeFavorite(draft.id)
            dietPlanStore.clearActivePlanIf(draft.id)
        }.onSuccess {
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
        }.onFailure {
            Toast.makeText(
                requireContext(),
                getString(R.string.create_diet_plan_delete_draft_error),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun setupDefaultExclusions() = with(binding) {
        exclusionsChipGroup.removeAllViews()

        listOf(
            getString(R.string.create_diet_plan_exclusion_broccoli),
            getString(R.string.create_diet_plan_exclusion_mushroom),
            getString(R.string.create_diet_plan_exclusion_milk),
            getString(R.string.create_diet_plan_exclusion_peanut),
            getString(R.string.create_diet_plan_exclusion_seafood)
        ).forEach { exclusion ->
            addExclusionChip(exclusion)
        }
    }
    private fun showCalorieInputDialog() {
        showNumberInputDialog(
            icon = "🔥",
            title = getString(R.string.create_diet_plan_calorie_dialog_title),
            helperText = "Günlük hedefini hızlıca ayarla. Bu değer planın öğün dağılımını belirler.",
            unitText = "kcal",
            initialValue = dailyCalories,
            validRange = 800..5000,
            step = 50,
            errorText = getString(R.string.create_diet_plan_calorie_dialog_error)
        ) { value ->
            dailyCalories = value
            renderState()
        }
    }

    private fun showDurationInputDialog() {
        showNumberInputDialog(
            icon = "📅",
            title = getString(R.string.create_diet_plan_duration_dialog_title),
            helperText = "Planının kaç gün süreceğini seç. Kısa planlar takibi kolaylaştırır.",
            unitText = "gün",
            initialValue = durationDays,
            validRange = 1..365,
            step = 1,
            errorText = getString(R.string.create_diet_plan_duration_dialog_error)
        ) { value ->
            durationDays = value
            renderState()
        }
    }
    private fun showAddExclusionDialog() {
        val input = EditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            hint = getString(R.string.create_diet_plan_add_exclusion_hint)
            setSingleLine(true)
            setPadding(32, 20, 32, 20)
        }

        val dialog = MaterialAlertDialogBuilder(
            requireContext(),
            R.style.ThemeOverlay_Fitto_WhiteAlertDialog
        )
            .setTitle(getString(R.string.create_diet_plan_add_exclusion_title))
            .setView(input)
            .setPositiveButton(getString(R.string.create_diet_plan_add_exclusion_positive), null)
            .setNegativeButton(getString(R.string.create_diet_plan_add_exclusion_negative), null)
            .create()

        dialog.setOnShowListener {
            forceWhiteDialog(dialog.window)

            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                val value = input.text?.toString()?.trim().orEmpty()
                if (value.isNotBlank()) {
                    addExclusionChip(value)
                    renderState()
                }
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun forceWhiteDialog(window: Window?) {
        window?.setBackgroundDrawableResource(R.drawable.bg_dialog_white)
    }

    private fun addExclusionChip(value: String) {
        if (value.isBlank()) return

        exclusions.add(value)

        val chip = Chip(requireContext()).apply {
            text = value
            isCloseIconVisible = true
            setOnCloseIconClickListener {
                exclusions.remove(value)
                binding.exclusionsChipGroup.removeView(this)
                renderState()
            }
        }

        binding.exclusionsChipGroup.addView(chip)
    }
    private fun renderState() = with(binding) {
        isRenderingState = true

        try {
            calorieValueText.text = dailyCalories.toString()
            durationValueText.text = getString(
                R.string.create_diet_plan_duration_day_value,
                durationDays
            )

            selectedMealCountText.text = getString(
                R.string.create_diet_plan_selected_meal_count,
                selectedMealTypes.size
            )

            renderGoalCards()
            renderPrivacyCards()
            renderPreferenceChips()
            renderMealOrderChips()
            renderPreview()
        } finally {
            isRenderingState = false
        }
    }
    private fun renderPreferenceChips() = with(binding) {
        renderPreferenceChip(preferenceVegetarianChip, CustomDietMealPlanGenerator.PREF_VEGETARIAN)
        renderPreferenceChip(preferenceVeganChip, CustomDietMealPlanGenerator.PREF_VEGAN)
        renderPreferenceChip(preferenceGlutenFreeChip, CustomDietMealPlanGenerator.PREF_GLUTEN_FREE)
        renderPreferenceChip(preferenceLactoseFreeChip, CustomDietMealPlanGenerator.PREF_LACTOSE_FREE)
        renderPreferenceChip(preferenceHighProteinChip, CustomDietMealPlanGenerator.PREF_HIGH_PROTEIN)
        renderPreferenceChip(preferenceLowCarbChip, CustomDietMealPlanGenerator.PREF_LOW_CARB)
    }

    private fun renderPreferenceChip(chip: Chip, preferenceKey: String) {
        val selected = preferenceKey in selectedPreferences

        chip.isChecked = selected
        chip.setTextColor(
            requireContext().getColor(
                if (selected) R.color.fitto_green else R.color.fitto_text_dark
            )
        )
    }

    private fun renderGoalCards() = with(binding) {
        renderSelectableCard(
            card = goalWeightLossCard,
            title = goalWeightLossTitle,
            selected = selectedGoal == CreatePlanGoal.WEIGHT_LOSS
        )
        renderSelectableCard(
            card = goalWeightGainCard,
            title = goalWeightGainTitle,
            selected = selectedGoal == CreatePlanGoal.WEIGHT_GAIN
        )
        renderSelectableCard(
            card = goalMuscleCard,
            title = goalMuscleTitle,
            selected = selectedGoal == CreatePlanGoal.MUSCLE
        )
        renderSelectableCard(
            card = goalMaintainCard,
            title = goalMaintainTitle,
            selected = selectedGoal == CreatePlanGoal.MAINTAIN
        )
        renderSelectableCard(
            card = goalBalancedCard,
            title = goalBalancedTitle,
            selected = selectedGoal == CreatePlanGoal.BALANCED
        )
    }

    private fun renderPrivacyCards() = with(binding) {
        renderSelectableCard(
            card = privacyPrivateCard,
            title = privacyPrivateTitle,
            selected = selectedPrivacy == CreatePlanPrivacy.PRIVATE
        )
        renderSelectableCard(
            card = privacyPublicCard,
            title = privacyPublicTitle,
            selected = selectedPrivacy == CreatePlanPrivacy.PUBLIC
        )
    }

    private fun renderSelectableCard(
        card: MaterialCardView,
        title: TextView,
        selected: Boolean
    ) {
        val selectedColor = requireContext().getColor(R.color.fitto_green)
        val normalTextColor = requireContext().getColor(R.color.fitto_text_dark)

        card.setCardBackgroundColor(
            requireContext().getColor(
                if (selected) R.color.fitto_primary_soft else R.color.fitto_surface
            )
        )
        card.strokeColor = requireContext().getColor(
            if (selected) R.color.fitto_green else R.color.fitto_divider_soft
        )
        card.strokeWidth = if (selected) 2 else 1

        title.setTextColor(
            if (selected) selectedColor else normalTextColor
        )
    }
    private fun renderMealOrderChips() = with(binding) {
        renderMealChip(mealBreakfastChip, MealType.BREAKFAST)
        renderMealChip(mealSnackChip, MealType.SNACK)
        renderMealChip(mealLunchChip, MealType.LUNCH)
        renderMealChip(mealDinnerChip, MealType.DINNER)
    }

    private fun renderMealChip(chip: Chip, mealType: MealType) {
        val selected = mealType in selectedMealTypes

        chip.isChecked = selected
        chip.setTextColor(
            requireContext().getColor(
                if (selected) R.color.fitto_green else R.color.fitto_text_dark
            )
        )
    }
    private fun renderPreview() = with(binding) {
        previewDailyGoalText.text = getString(
            R.string.create_diet_plan_preview_daily_goal_dynamic,
            dailyCalories
        )

        previewMealCountText.text = getString(
            R.string.create_diet_plan_preview_meal_count_dynamic,
            selectedMealTypes.size
        )

        previewDurationText.text = getString(
            R.string.create_diet_plan_preview_duration_dynamic,
            durationDays
        )

        previewGoalText.text = getString(
            R.string.create_diet_plan_preview_goal_dynamic,
            getString(selectedGoal.titleRes)
        )

        previewPrivacyText.text = getString(
            R.string.create_diet_plan_preview_privacy_dynamic,
            getString(selectedPrivacy.titleRes)
        )

        val previewMeals = buildPreviewMeals()

        renderPreviewMealText(previewBreakfastText, previewMeals[MealType.BREAKFAST])
        renderPreviewMealText(previewSnackText, previewMeals[MealType.SNACK])
        renderPreviewMealText(previewLunchText, previewMeals[MealType.LUNCH])
        renderPreviewMealText(previewDinnerText, previewMeals[MealType.DINNER])
    }

    private fun renderPreviewMealText(
        textView: TextView,
        meal: PreviewMealUiModel?
    ) {
        if (meal == null) {
            textView.visibility = View.GONE
            return
        }

        textView.visibility = View.VISIBLE
        textView.text = buildString {
            append(meal.icon)
            append('\n')
            append(getString(meal.mealType.titleRes))
            append('\n')
            append(meal.percent)
            append('%')
            append('\n')
            append(meal.calories)
            append(" kcal")
        }
    }

    private fun buildPreviewMeals(): Map<MealType, PreviewMealUiModel> {
        return CreateDietPlanPreviewResolver.buildPreviewMeals(
            selectedMealTypes = selectedMealTypes,
            dailyCalories = dailyCalories
        )
    }
    private fun savePlan(isDraft: Boolean) = with(binding) {
        val planName = planNameInput.text?.toString()?.trim().orEmpty()
        val description = planDescriptionInput.text?.toString()?.trim().orEmpty()

        if (planName.isBlank()) {
            planNameInput.error = getString(R.string.create_diet_plan_name_required)
            planNameInput.requestFocus()
            return@with
        }
        if (
            customDietPlanStore.hasPlanWithTitle(
                title = planName,
                excludingPlanId = editingDraftPlan?.id
            )
        ) {
            planNameInput.error = getString(R.string.create_diet_plan_name_duplicate)
            planNameInput.requestFocus()
            return@with
        }
        val now = System.currentTimeMillis()
        val existingPlan = editingDraftPlan
        val isPublic = selectedPrivacy == CreatePlanPrivacy.PUBLIC

        val plan = CustomDietPlanLocalModel(
            id = existingPlan?.id ?: "${CustomDietPlanStore.CUSTOM_PLAN_ID_PREFIX}$now",
            title = planName,
            description = description,
            goalType = selectedGoal.name,
            dailyCalories = dailyCalories,
            mealCount = selectedMealTypes.size,
            durationDays = durationDays,
            isPublic = isPublic,
            preferences = selectedPreferences.toList(),
            exclusions = exclusions.toList(),
            mealOrder = buildMealOrder(),
            isDraft = isDraft,
            createdAt = existingPlan?.createdAt ?: now,
            updatedAt = now
        )

        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                customDietPlanStore.savePlan(plan)
            }

            if (isDraft) {
                editingDraftPlan = plan

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
                        if (existingPlan != null) {
                            R.string.create_diet_plan_draft_updated
                        } else {
                            R.string.create_diet_plan_saved
                        }
                    ),
                    Toast.LENGTH_SHORT
                ).show()

                parentFragmentManager.popBackStack()
                return@launch
            }

            dietPlanStore.saveActivePlan(plan.id)
            dietPlanStore.applyActivePlanToday()
            if (isPublic) {
                shareCustomPlanToSocial(plan)
            }

            parentFragmentManager.setFragmentResult(
                DietPlansFragment.REQUEST_DIET_PLAN_CHANGED,
                Bundle().apply {
                    putString(DietPlansFragment.BUNDLE_ACTIVE_PLAN_ID, plan.id)
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
    }

    private suspend fun shareCustomPlanToSocial(plan: CustomDietPlanLocalModel) {
        runCatching {
            socialRepository.createPost(
                content = getString(
                    R.string.create_diet_plan_social_share_content,
                    getString(selectedGoal.titleRes),
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
                    getString(selectedGoal.titleRes)
                ),
                isDraft = false
            )
        }
    }

    private fun buildMealOrder(): List<String> {
        return selectedMealTypes.map { mealType -> mealType.argumentValue }
    }

    override fun onDestroyView() {
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
