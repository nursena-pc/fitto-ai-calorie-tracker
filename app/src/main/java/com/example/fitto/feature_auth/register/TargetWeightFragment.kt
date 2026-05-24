package com.example.fitto.feature_auth.register

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.fitto.R
import com.example.fitto.databinding.FragmentTargetWeightBinding
import com.example.fitto.feature_home.presentation.profile.ProfileLanguage
import com.example.fitto.feature_home.presentation.profile.ProfilePreferenceStore
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

class TargetWeightFragment : Fragment(R.layout.fragment_target_weight) {

    private var _binding: FragmentTargetWeightBinding? = null
    private val binding get() = _binding!!

    private lateinit var onboardingViewModel: OnboardingViewModel
    private var localFeedbackJob: Job? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentTargetWeightBinding.bind(view)
        onboardingViewModel = ViewModelProvider(requireActivity())[OnboardingViewModel::class.java]

        binding.root.setOnboardingProgress(10)

        setupInitialState()
        setupClicks()
        setupInput()
        observeAiSuggestion()
        requestInitialAiSuggestion()
    }

    private fun setupInitialState() = with(binding) {
        val savedTargetWeight = onboardingViewModel.state.targetWeight
        if (savedTargetWeight > 0) {
            targetWeightEditText.setText(savedTargetWeight.toString())
        }

        targetWeightLiveFeedbackCard.isVisible = false
        targetWeightLocalFeedbackProgressIndicator.isVisible = false
        targetWeightAiProgressIndicator.isVisible = false
        targetWeightAiErrorText.isVisible = false
        targetWeightAiRetryButton.isVisible = false
        targetWeightAiResultCard.isVisible = false
    }

    private fun setupClicks() = with(binding) {
        targetWeightBackButton.setOnClickListener {
            findNavController().popBackStack()
        }

        targetWeightAiRetryButton.setOnClickListener {
            targetWeightAiErrorText.isVisible = false
            targetWeightAiRetryButton.isVisible = false
            onboardingViewModel.clearAiError()
            onboardingViewModel.requestAiTargetWeightSuggestion(
                force = true,
                appLanguage = activeAiLanguage(),
                nutritionContext = currentNutritionContext()
            )
        }

        targetWeightUseAiButton.setOnClickListener {
            val suggestion = onboardingViewModel.aiTargetUiState.value.aiTargetSuggestion ?: return@setOnClickListener
            val suggestionText = suggestion.suggestedTargetWeightKg.toString()

            if (targetWeightEditText.text?.toString() == suggestionText) {
                hideLiveFeedback()
                return@setOnClickListener
            }

            targetWeightEditText.setText(suggestionText)
            targetWeightEditText.setSelection(targetWeightEditText.text?.length ?: 0)
            targetWeightInputLayout.error = null
            hideLiveFeedback()
        }

        targetWeightNextButton.setOnClickListener {
            submitTargetWeight()
        }
    }

    private fun setupInput() = with(binding) {
        targetWeightEditText.setOnFocusChangeListener { _, hasFocus ->
            targetWeightInfoCard.isVisible = hasFocus
        }

        targetWeightEditText.doAfterTextChanged { editable ->
            val current = editable?.toString().orEmpty()
            val filtered = current.filter { it.isDigit() }

            if (filtered != current) {
                targetWeightEditText.setText(filtered)
                targetWeightEditText.setSelection(filtered.length)
                return@doAfterTextChanged
            }

            targetWeightInputLayout.error = null
            scheduleTargetWeightLiveFeedback()
        }

        scheduleTargetWeightLiveFeedback()
    }

    private fun requestInitialAiSuggestion() {
        onboardingViewModel.requestAiTargetWeightSuggestion(
            appLanguage = activeAiLanguage(),
            nutritionContext = currentNutritionContext()
        )
    }

    private fun observeAiSuggestion() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                onboardingViewModel.aiTargetUiState.collect { state ->
                    renderAiState(state)
                }
            }
        }
    }

    private fun renderAiState(state: AiTargetUiState) = with(binding) {
        targetWeightAiProgressIndicator.isVisible = state.isAiLoading
        targetWeightAiStatusText.isVisible = state.isAiLoading

        val nutritionContext = currentNutritionContext()
        val suggestion = state.aiTargetSuggestion
            ?.takeIf {
                state.aiSuggestionContextSignature == onboardingViewModel.currentAiSuggestionContextSignature(nutritionContext) &&
                        state.aiSuggestionLanguage == activeAiLanguage()
            }

        targetWeightAiResultCard.isVisible = suggestion != null
        if (suggestion != null) {
            targetWeightAiResultTitleText.text = getString(R.string.target_weight_ai_result_title)
            targetWeightAiResultWeightText.text = getString(
                R.string.target_weight_ai_result_weight_format,
                suggestion.suggestedTargetWeightKg
            )
            targetWeightAiResultExplanationText.text = buildAiExplanationText(suggestion)
            targetWeightAiResultWarningText.text = getString(R.string.target_weight_ai_medical_warning)
            targetWeightUseAiButton.isVisible = true

            scheduleTargetWeightLiveFeedback()
        }

        val hasError = state.aiError != null
        targetWeightAiErrorText.isVisible = hasError
        targetWeightAiRetryButton.isVisible = hasError
        targetWeightAiErrorText.text = state.aiError?.let { aiErrorMessage(it) }.orEmpty()

        if (hasError && suggestion == null) {
            Snackbar.make(root, targetWeightAiErrorText.text, Snackbar.LENGTH_LONG).show()
        }

        renderTargetWeightLiveFeedbackState()
    }

    private fun buildAiExplanationText(suggestion: AiTargetSuggestion): String {
        return buildString {
            append(suggestion.explanation)
            if (suggestion.timelineComment.isNotBlank()) {
                append("\n\n")
                append(suggestion.timelineComment)
            }
            if (suggestion.motivationComment.isNotBlank()) {
                append("\n\n")
                append(suggestion.motivationComment)
            }
        }.trim()
    }

    private fun scheduleTargetWeightLiveFeedback(targetWeightOverride: Int? = null) {
        val targetWeight = targetWeightOverride ?: currentValidTargetWeight()
        if (targetWeight == null) {
            hideLiveFeedback()
            return
        }

        val suggestion = freshAiSuggestion()
        if (suggestion != null && targetWeight == suggestion.suggestedTargetWeightKg) {
            hideLiveFeedback()
            return
        }

        localFeedbackJob?.cancel()
        localFeedbackJob = viewLifecycleOwner.lifecycleScope.launch {
            delay(LOCAL_FEEDBACK_DEBOUNCE_MS)
            if (_binding == null) return@launch

            val latestTargetWeight = currentValidTargetWeight()
            if (latestTargetWeight == targetWeight) {
                showLocalTargetWeightFeedback(targetWeight)
            }
        }
    }

    private fun aiErrorMessage(errorCode: String): String {
        return when (errorCode) {
            "missing_api_key" -> getString(R.string.target_weight_ai_error_api_key)
            "missing_onboarding_info" -> getString(R.string.target_weight_ai_error_missing_info)
            "network_error" -> getString(R.string.target_weight_ai_error_network)
            "invalid_response" -> getString(R.string.target_weight_ai_error_invalid_response)
            "rate_limited" -> getString(R.string.target_weight_ai_error_rate_limited)
            "unsafe_ai_suggestion" -> getString(R.string.target_weight_ai_error_unsafe_suggestion)
            else -> getString(R.string.target_weight_ai_error)
        }
    }

    private fun renderTargetWeightLiveFeedbackState() {
        val currentTargetWeight = currentValidTargetWeight()
        if (currentTargetWeight == null) {
            binding.targetWeightLiveFeedbackCard.isVisible = false
            return
        }

        val suggestion = freshAiSuggestion()
        if (suggestion != null && currentTargetWeight == suggestion.suggestedTargetWeightKg) {
            binding.targetWeightLiveFeedbackCard.isVisible = false
            return
        }

        showLocalTargetWeightFeedback(currentTargetWeight)
    }

    private fun hideLiveFeedback() {
        localFeedbackJob?.cancel()
        binding.targetWeightLiveFeedbackCard.isVisible = false
    }

    private fun showLocalTargetWeightFeedback(targetWeight: Int) = with(binding) {
        val safety = evaluateSafety(targetWeight)
        targetWeightLiveFeedbackCard.isVisible = true
        targetWeightLocalFeedbackProgressIndicator.isVisible = false
        targetWeightLiveFeedbackTitleText.text = localFeedbackTitle(targetWeight, safety)
        targetWeightLiveFeedbackText.text = buildLocalTargetWeightMessage(targetWeight, safety)

        val iconRes = when (safety.status) {
            TargetWeightSafetyStatus.SAFE -> R.drawable.ic_check_circle
            TargetWeightSafetyStatus.WARNING -> R.drawable.ic_warning
            TargetWeightSafetyStatus.BLOCKED -> R.drawable.ic_error_outline
        }
        targetWeightLiveFeedbackIcon.setImageResource(iconRes)
        applyLocalFeedbackCardAccent(cardAccentColorRes(safety.status))
    }

    private fun localFeedbackTitle(
        targetWeight: Int,
        safety: TargetWeightSafetyResult
    ): String {
        return when {
            safety.status == TargetWeightSafetyStatus.BLOCKED -> getString(R.string.target_weight_local_feedback_title_blocked)
            safety.status == TargetWeightSafetyStatus.WARNING -> getString(R.string.target_weight_local_feedback_title_warning)
            isMoreAmbitiousThanAiSuggestion(targetWeight) -> getString(R.string.target_weight_local_feedback_title_ambitious)
            isMoreCautiousThanAiSuggestion(targetWeight) -> getString(R.string.target_weight_local_feedback_title_cautious)
            isCloseToAiSuggestion(targetWeight) -> getString(R.string.target_weight_local_feedback_title_close)
            else -> getString(R.string.target_weight_local_feedback_title_safe)
        }
    }

    private fun buildLocalTargetWeightMessage(
        targetWeight: Int,
        safety: TargetWeightSafetyResult
    ): String {
        if (safety.status == TargetWeightSafetyStatus.BLOCKED) {
            return getString(safety.messageRes)
        }

        val baseMessage = getString(safety.messageRes)
        val aiComparisonNote = buildAiComparisonNote(targetWeight)

        return buildString {
            append(baseMessage)
            if (safety.status == TargetWeightSafetyStatus.SAFE && !aiComparisonNote.isNullOrBlank()) {
                append("\n\n")
                append(aiComparisonNote)
            }
        }
    }

    private fun applyLocalFeedbackCardAccent(@ColorRes colorRes: Int) = with(binding) {
        val accentColor = ContextCompat.getColor(requireContext(), colorRes)
        targetWeightLiveFeedbackCard.setStrokeColor(accentColor)
        targetWeightLiveFeedbackIcon.imageTintList = ColorStateList.valueOf(accentColor)
        targetWeightLocalFeedbackProgressIndicator.setIndicatorColor(accentColor)
    }

    @ColorRes
    private fun cardAccentColorRes(safetyStatus: TargetWeightSafetyStatus): Int {
        return when (safetyStatus) {
            TargetWeightSafetyStatus.SAFE -> R.color.fitto_green
            TargetWeightSafetyStatus.WARNING -> R.color.fitto_orange
            TargetWeightSafetyStatus.BLOCKED -> R.color.fitto_error
        }
    }

    private fun submitTargetWeight() = with(binding) {
        val value = targetWeightEditText.text?.toString()?.trim().orEmpty()
        val error = validateTargetWeight(value)

        targetWeightInputLayout.error = error

        if (error != null) {
            targetWeightInfoCard.isVisible = true
            targetWeightLiveFeedbackCard.isVisible = false
            return@with
        }

        val targetWeight = value.toInt()
        val safety = evaluateSafety(targetWeight)

        when (safety.status) {
            TargetWeightSafetyStatus.BLOCKED -> showBlockedDialog(targetWeight)
            TargetWeightSafetyStatus.WARNING -> showWarningDialog(targetWeight, safety)
            TargetWeightSafetyStatus.SAFE -> {
                onboardingViewModel.saveTargetWeight(targetWeight)
                navigateToLoading()
            }
        }
    }

    private fun showWarningDialog(
        targetWeight: Int,
        safety: TargetWeightSafetyResult
    ) {
        val message = buildLocalTargetWeightMessage(targetWeight, safety)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.target_weight_warning_dialog_title)
            .setMessage(message)
            .setNegativeButton(R.string.target_weight_edit_button, null)
            .setPositiveButton(R.string.target_weight_continue_anyway_button) { _, _ ->
                onboardingViewModel.saveTargetWeight(targetWeight)
                navigateToLoading()
            }
            .show()
    }

    private fun showBlockedDialog(targetWeight: Int) {
        val message = buildLocalTargetWeightMessage(targetWeight, evaluateSafety(targetWeight))

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.target_weight_blocked_dialog_title)
            .setMessage(message)
            .setPositiveButton(R.string.target_weight_edit_button, null)
            .show()
    }

    private fun navigateToLoading() {
        findNavController().navigate(R.id.action_targetWeightFragment_to_dailyNeedsLoadingFragment)
    }

    private fun buildAiComparisonNote(targetWeight: Int): String? {
        val suggestionWeight = freshAiSuggestion()?.suggestedTargetWeightKg ?: return null
        val aiDiffPercent = abs(targetWeight - suggestionWeight) / suggestionWeight.toDouble()

        return when {
            aiDiffPercent <= 0.05 -> getString(
                R.string.target_weight_ai_comparison_close,
                suggestionWeight
            )

            isMoreAmbitiousThanAiSuggestion(targetWeight) -> getString(
                R.string.target_weight_ai_comparison_ambitious,
                suggestionWeight,
                targetWeight
            )

            isMoreCautiousThanAiSuggestion(targetWeight) -> getString(
                R.string.target_weight_ai_comparison_cautious,
                suggestionWeight,
                targetWeight
            )

            else -> getString(
                R.string.target_weight_ai_comparison_neutral,
                suggestionWeight,
                targetWeight
            )
        }
    }

    private fun isCloseToAiSuggestion(targetWeight: Int): Boolean {
        val suggestionWeight = freshAiSuggestion()?.suggestedTargetWeightKg ?: return false
        return abs(targetWeight - suggestionWeight) / suggestionWeight.toDouble() <= 0.05
    }

    private fun isMoreAmbitiousThanAiSuggestion(targetWeight: Int): Boolean {
        val suggestionWeight = freshAiSuggestion()?.suggestedTargetWeightKg ?: return false
        val aiDiffPercent = abs(targetWeight - suggestionWeight) / suggestionWeight.toDouble()
        if (aiDiffPercent <= 0.05) return false

        return when (onboardingViewModel.state.goalType) {
            GoalType.LOSE_WEIGHT -> targetWeight < suggestionWeight
            GoalType.GAIN_WEIGHT -> targetWeight > suggestionWeight
            else -> false
        }
    }

    private fun isMoreCautiousThanAiSuggestion(targetWeight: Int): Boolean {
        val suggestionWeight = freshAiSuggestion()?.suggestedTargetWeightKg ?: return false
        val aiDiffPercent = abs(targetWeight - suggestionWeight) / suggestionWeight.toDouble()
        if (aiDiffPercent <= 0.05) return false

        return when (onboardingViewModel.state.goalType) {
            GoalType.LOSE_WEIGHT -> targetWeight > suggestionWeight
            GoalType.GAIN_WEIGHT -> targetWeight < suggestionWeight
            else -> false
        }
    }

    private fun freshAiSuggestion(): AiTargetSuggestion? {
        val nutritionContext = currentNutritionContext()
        val uiState = onboardingViewModel.aiTargetUiState.value
        if (uiState.aiSuggestionContextSignature != onboardingViewModel.currentAiSuggestionContextSignature(nutritionContext)) {
            return null
        }
        if (uiState.aiSuggestionLanguage != activeAiLanguage()) return null
        return uiState.aiTargetSuggestion
    }

    private fun currentValidTargetWeight(): Int? {
        val value = binding.targetWeightEditText.text
            ?.toString()
            ?.trim()
            ?.toIntOrNull()
            ?: return null

        if (value !in 25..300) return null
        if (validateTargetWeight(value.toString()) != null) return null

        return value
    }

    private fun evaluateSafety(targetWeight: Int): TargetWeightSafetyResult {
        return TargetWeightSafetyChecker.evaluate(
            age = onboardingViewModel.state.age,
            heightCm = onboardingViewModel.state.height,
            currentWeightKg = onboardingViewModel.state.weight,
            targetWeightKg = targetWeight,
            goalType = onboardingViewModel.state.goalType,
            daysUntilEvent = daysUntilEvent(),
            aiSuggestedTargetWeightKg = freshAiSuggestion()?.suggestedTargetWeightKg
        )
    }

    private fun daysUntilEvent(): Long? {
        val eventTime = onboardingViewModel.state.eventTimeMillis ?: return null
        val days = (eventTime - System.currentTimeMillis()) / (1000L * 60L * 60L * 24L)
        return days.takeIf { it > 0L }
    }

    private fun activeAiLanguage(): String {
        return when (ProfilePreferenceStore.getLanguage(requireContext())) {
            ProfileLanguage.ENGLISH -> "en"
            ProfileLanguage.TURKISH -> "tr"
        }
    }

    private fun currentNutritionContext(): OnboardingNutritionPromptContext {
        return OnboardingNutritionPromptContext(
            dietPreferences = ProfilePreferenceStore.nutritionDietPreferencesText(requireContext()),
            allergies = ProfilePreferenceStore.getNutritionAllergies(requireContext()),
            dislikedFoods = ProfilePreferenceStore.getNutritionDislikedFoods(requireContext()),
            mealRoutine = ProfilePreferenceStore.nutritionMealRoutineText(requireContext())
        )
    }

    private fun validateTargetWeight(value: String): String? {
        if (value.isBlank()) {
            return getString(R.string.target_weight_error_empty)
        }

        val parsed = value.toIntOrNull()
            ?: return getString(R.string.target_weight_error_invalid)

        if (parsed !in 25..300) {
            return getString(R.string.target_weight_error_range)
        }

        val currentWeight = onboardingViewModel.state.weight
        val goalType = onboardingViewModel.state.goalType

        return when (goalType) {
            GoalType.LOSE_WEIGHT -> {
                if (parsed >= currentWeight) {
                    getString(R.string.target_weight_error_should_be_lower)
                } else {
                    null
                }
            }

            GoalType.GAIN_WEIGHT -> {
                if (parsed <= currentWeight) {
                    getString(R.string.target_weight_error_should_be_higher)
                } else {
                    null
                }
            }

            else -> null
        }
    }

    override fun onDestroyView() {
        localFeedbackJob?.cancel()
        _binding = null
        super.onDestroyView()
    }

    private companion object {
        const val LOCAL_FEEDBACK_DEBOUNCE_MS = 450L
    }
}
