package com.example.fitto.feature_auth.register

import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.fitto.R
import com.example.fitto.fittoAppContainer
import com.example.fitto.databinding.BottomSheetCreateAccountBinding
import com.example.fitto.databinding.FragmentDailyNeedsResultBinding
import com.example.fitto.databinding.ItemResultSummaryRowBinding
import com.example.fitto.feature_home.presentation.profile.ProfileLanguage
import com.example.fitto.feature_home.presentation.profile.ProfilePreferenceStore
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class DailyNeedsResultFragment : Fragment(R.layout.fragment_daily_needs_result) {

    private var _binding: FragmentDailyNeedsResultBinding? = null
    private val binding get() = _binding!!
    private val authDependencies by lazy {
        requireContext().fittoAppContainer.authFeatureDependencies
    }
    private lateinit var credentialManager: CredentialManager

    private var isGoogleAuthInProgress = false
    private lateinit var onboardingViewModel: OnboardingViewModel
    private lateinit var baseResult: DailyNeedsResult

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentDailyNeedsResultBinding.bind(view)
        onboardingViewModel = obtainOnboardingViewModel()
        credentialManager = CredentialManager.create(requireContext())

        binding.root.setOnboardingProgress(12)

        baseResult = onboardingViewModel.calculateDailyNeeds()
        bindResult(baseResult)
        bindPlanAiState(AiPlanUiState(isLoading = true, planMode = PlanAiMode.LOADING, displayResult = baseResult))
        setupClicks()
        observePlanAi()
        requestPlanAi()
    }

    private fun setupClicks() = with(binding) {
        resultStartPlanButton.setOnClickListener {
            showCreateAccountBottomSheet()
        }
    }

    private fun observePlanAi() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                onboardingViewModel.aiPlanUiState.collect { state ->
                    bindPlanAiState(state)
                    state.displayResult?.let { result ->
                        if (!state.isLoading) bindResult(result)
                    }
                }
            }
        }
    }

    private fun requestPlanAi() {
        onboardingViewModel.requestAiPlanSuggestion(
            localResult = baseResult,
            appLanguage = activeAiLanguage(),
            nutritionContext = currentNutritionContext()
        )
    }

    private fun bindPlanAiState(state: AiPlanUiState) = with(binding) {
        val mode = when {
            state.isLoading -> PlanAiMode.LOADING
            state.aiError != null -> PlanAiMode.STANDARD
            state.planMode == PlanAiMode.AI_VERIFIED -> PlanAiMode.AI_VERIFIED
            state.planMode == PlanAiMode.AI_ADJUSTED -> PlanAiMode.AI_ADJUSTED
            else -> PlanAiMode.STANDARD
        }

        applyPlanAiBadgeStyle(mode)

        resultAiBadgeText.text = when (mode) {
            PlanAiMode.LOADING -> getString(R.string.result_ai_badge_loading)
            PlanAiMode.AI_VERIFIED -> getString(R.string.result_ai_badge_verified)
            PlanAiMode.AI_ADJUSTED -> getString(R.string.result_ai_badge_adjusted)
            PlanAiMode.STANDARD -> getString(R.string.result_ai_badge_standard)
        }

        resultAiBadgeDescriptionText.text = when (mode) {
            PlanAiMode.LOADING -> getString(R.string.result_ai_badge_loading_desc)
            PlanAiMode.AI_VERIFIED -> getString(R.string.result_ai_badge_verified_desc)
            PlanAiMode.AI_ADJUSTED -> getString(R.string.result_ai_badge_adjusted_desc)
            PlanAiMode.STANDARD -> getString(R.string.result_ai_badge_standard_desc)
        }

        resultAiCommentCard.isVisible = state.isLoading || state.aiPlanSuggestion != null || state.aiError != null
        resultAiCommentTitleText.text = getString(R.string.result_ai_comment_title)
        resultAiCommentBodyText.text = when {
            state.isLoading -> getString(R.string.result_ai_comment_loading)
            state.aiError != null -> getString(R.string.result_ai_comment_fallback)
            state.aiPlanSuggestion != null -> buildPlanAiComment(state.aiPlanSuggestion)
            else -> getString(R.string.result_ai_comment_fallback)
        }
    }

    private fun applyPlanAiBadgeStyle(mode: PlanAiMode) = with(binding) {
        val backgroundColorRes: Int
        val textColorRes: Int
        val strokeColorRes: Int

        when (mode) {
            PlanAiMode.LOADING -> {
                backgroundColorRes = R.color.result_ai_badge_loading_bg
                textColorRes = R.color.result_ai_badge_loading_text
                strokeColorRes = R.color.result_ai_badge_loading_stroke
            }

            PlanAiMode.AI_VERIFIED -> {
                backgroundColorRes = R.color.result_ai_badge_verified_bg
                textColorRes = R.color.result_ai_badge_verified_text
                strokeColorRes = R.color.result_ai_badge_verified_stroke
            }

            PlanAiMode.AI_ADJUSTED -> {
                backgroundColorRes = R.color.result_ai_badge_adjusted_bg
                textColorRes = R.color.result_ai_badge_adjusted_text
                strokeColorRes = R.color.result_ai_badge_adjusted_stroke
            }

            PlanAiMode.STANDARD -> {
                backgroundColorRes = R.color.result_ai_badge_standard_bg
                textColorRes = R.color.result_ai_badge_standard_text
                strokeColorRes = R.color.result_ai_badge_standard_stroke
            }
        }

        resultAiBadgeCard.setCardBackgroundColor(ContextCompat.getColor(requireContext(), backgroundColorRes))
        resultAiBadgeCard.strokeColor = ContextCompat.getColor(requireContext(), strokeColorRes)
        resultAiBadgeText.setTextColor(ContextCompat.getColor(requireContext(), textColorRes))
        resultAiBadgeDescriptionText.setTextColor(ContextCompat.getColor(requireContext(), textColorRes))
    }

    private fun buildPlanAiComment(suggestion: AiPlanSuggestion): String {
        return buildString {
            append(suggestion.summary)
            appendIfPresent(suggestion.targetComment)
            appendIfPresent(suggestion.timelineComment)
            appendIfPresent(suggestion.calorieComment)
            appendIfPresent(suggestion.motivationComment)
            appendIfPresent(suggestion.warning)
        }.trim()
    }

    private fun StringBuilder.appendIfPresent(value: String) {
        val cleaned = value.trim()
        if (cleaned.isBlank()) return
        if (isNotEmpty()) append("\n\n")
        append(cleaned)
    }

    private fun bindResult(result: DailyNeedsResult) = with(binding) {
        authDependencies.saveDailyTargetCalories(result.appliedCalories)

        calorieGaugeView.setCalories(
            targetCalories = result.appliedCalories,
            maintenanceCalories = result.maintenanceCalories
        )

        resultStatusTitleText.text = getStatusTitle(result.status)
        resultStatusTitleText.setTextColor(getStatusColor(result.status))

        bindSummaryRow(
            summaryDailyNeedsRow,
            getString(R.string.result_daily_needs),
            "${result.maintenanceCalories} kcal"
        )

        bindSummaryRow(
            summaryTargetCalorieRow,
            getString(R.string.result_target_calorie),
            "${result.targetCalories} kcal"
        )

        summaryRecommendedCalorieRow.root.isVisible = !result.isUserTargetApplied
        bindSummaryRow(
            summaryRecommendedCalorieRow,
            getString(R.string.result_recommended_calorie),
            "${result.recommendedCalories} kcal"
        )

        bindSummaryRow(
            summaryWeeklyChangeRow,
            getString(R.string.result_weekly_change),
            "${formatWeightChange(result.weeklyWeightChangeKg)} kg"
        )

        summaryEstimatedTimeRow.root.isVisible = result.estimatedWeeks != null
        result.estimatedWeeks?.let { weeks ->
            bindSummaryRow(
                summaryEstimatedTimeRow,
                getString(R.string.result_estimated_time),
                getString(R.string.result_weeks, weeks)
            )
        }

        bindStatusCard(result.status)
    }

    private fun bindSummaryRow(
        rowBinding: ItemResultSummaryRowBinding,
        title: String,
        value: String
    ) {
        rowBinding.summaryTitleText.text = title
        rowBinding.summaryValueText.text = value
    }

    private fun getStatusTitle(status: PlanStatus): String {
        return when (status) {
            PlanStatus.HEALTHY -> getString(R.string.result_status_healthy)
            PlanStatus.AGGRESSIVE -> getString(R.string.result_status_aggressive)
            PlanStatus.NOT_RECOMMENDED -> getString(R.string.result_status_not_recommended)
            PlanStatus.NO_WEIGHT_CHANGE -> getString(R.string.result_status_balance)
        }
    }

    private fun getStatusDescription(status: PlanStatus): String {
        return when (status) {
            PlanStatus.HEALTHY -> getString(R.string.result_status_healthy_desc)
            PlanStatus.AGGRESSIVE -> getString(R.string.result_status_aggressive_desc)
            PlanStatus.NOT_RECOMMENDED -> getString(R.string.result_status_not_recommended_desc)
            PlanStatus.NO_WEIGHT_CHANGE -> getString(R.string.result_status_balance_desc)
        }
    }

    private fun getStatusColor(status: PlanStatus): Int {
        return when (status) {
            PlanStatus.HEALTHY,
            PlanStatus.NO_WEIGHT_CHANGE -> requireContext().getColor(R.color.fitto_green)

            PlanStatus.AGGRESSIVE -> requireContext().getColor(R.color.fitto_orange)
            PlanStatus.NOT_RECOMMENDED -> ContextCompat.getColor(requireContext(), R.color.fitto_error)
        }
    }

    private fun formatWeightChange(value: Double): String {
        return if (value == value.roundToInt().toDouble()) {
            value.roundToInt().toString()
        } else {
            String.format("%.1f", value)
        }
    }

    private fun bindStatusCard(status: PlanStatus) = with(binding) {
        val icon = if (
            status == PlanStatus.HEALTHY ||
            status == PlanStatus.NO_WEIGHT_CHANGE
        ) {
            "✓"
        } else {
            "!"
        }

        val message = getStatusDescription(status)
        val fullText = "$icon  $message"

        val spannable = SpannableString(fullText)
        spannable.setSpan(
            ForegroundColorSpan(getStatusColor(status)),
            0,
            icon.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannable.setSpan(
            StyleSpan(Typeface.BOLD),
            0,
            icon.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        resultStatusCard.text = spannable
        resultStatusCard.setTextColor(ContextCompat.getColor(requireContext(), R.color.fitto_text_secondary))

        resultStatusCard.setBackgroundResource(
            when (status) {
                PlanStatus.HEALTHY,
                PlanStatus.NO_WEIGHT_CHANGE -> R.drawable.bg_result_status_healthy

                PlanStatus.AGGRESSIVE -> R.drawable.bg_result_status_warning
                PlanStatus.NOT_RECOMMENDED -> R.drawable.bg_result_status_error
            }
        )
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
    private fun showCreateAccountBottomSheet() {
        val dialog = BottomSheetDialog(requireContext())
        val sheetBinding = BottomSheetCreateAccountBinding.inflate(
            LayoutInflater.from(requireContext())
        )

        dialog.setContentView(sheetBinding.root)
        setupCreateAccountLoginText(sheetBinding)

        sheetBinding.createAccountEmailButton.setOnClickListener {
            dialog.dismiss()

            findNavController().navigate(
                R.id.action_dailyNeedsResultFragment_to_createAccountFragment
            )
        }

        sheetBinding.createAccountGoogleButton.setOnClickListener {
            dialog.dismiss()
            startGoogleSignUp()
        }
        sheetBinding.createAccountLoginText.setOnClickListener {
            dialog.dismiss()

            findNavController().navigate(R.id.loginFragment)
        }

        dialog.show()
    }
    private fun setupCreateAccountLoginText(
        sheetBinding: BottomSheetCreateAccountBinding
    ) {
        val fullText = getString(R.string.create_account_already_have_account)
        val loginText = fullText.substringAfter("?").trim()

        val spannable = SpannableString(fullText)
        val loginStartIndex = fullText.indexOf(loginText)

        if (loginStartIndex >= 0) {
            spannable.setSpan(
                ForegroundColorSpan(
                    ContextCompat.getColor(requireContext(), R.color.fitto_green)
                ),
                loginStartIndex,
                fullText.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        sheetBinding.createAccountLoginText.text = spannable
    }

    private fun startGoogleSignUp() {
        if (isGoogleAuthInProgress) return

        isGoogleAuthInProgress = true
        requestGoogleCredential(filterByAuthorizedAccounts = true)
    }

    private fun requestGoogleCredential(filterByAuthorizedAccounts: Boolean) {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(filterByAuthorizedAccounts)
            .setServerClientId(getString(R.string.default_web_client_id))
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(
                    context = requireContext(),
                    request = request
                )

                handleGoogleCredential(result.credential)
            } catch (exception: NoCredentialException) {
                if (filterByAuthorizedAccounts) {
                    requestGoogleCredential(filterByAuthorizedAccounts = false)
                } else {
                    isGoogleAuthInProgress = false
                    showGoogleError(getString(R.string.google_error_general))
                }
            } catch (exception: GetCredentialException) {
                isGoogleAuthInProgress = false
                showGoogleError(getString(R.string.google_error_general))
            }
        }
    }

    private fun handleGoogleCredential(credential: androidx.credentials.Credential) {
        if (
            credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            try {
                val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                completeGoogleSignUp(googleCredential.idToken)
            } catch (exception: GoogleIdTokenParsingException) {
                isGoogleAuthInProgress = false
                showGoogleError(getString(R.string.google_error_parse))
            }
        } else {
            isGoogleAuthInProgress = false
            showGoogleError(getString(R.string.google_error_unexpected_response))
        }
    }

    private fun completeGoogleSignUp(idToken: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            when (
                val result = authDependencies.googleSignUpCoordinator.signIn(idToken)
            ) {
                is GoogleOnboardingResult.Success -> {
                    isGoogleAuthInProgress = false
                    when (result.destination) {
                        GoogleOnboardingDestination.HOME -> navigateToHome()
                        GoogleOnboardingDestination.USERNAME_SETUP -> navigateToUsernameSetup()
                    }
                }

                is GoogleOnboardingResult.Failure -> {
                    isGoogleAuthInProgress = false
                    showGoogleError(getString(R.string.google_error_firebase_failed))
                }
            }
        }
    }
    private fun navigateToUsernameSetup() {
        findNavController().navigate(
            R.id.action_dailyNeedsResultFragment_to_usernameSetupFragment
        )
    }
    private fun navigateToHome() {
        findNavController().navigate(
            R.id.action_dailyNeedsResultFragment_to_homeFragment
        )
    }

    private fun showGoogleError(message: String) {
        android.widget.Toast.makeText(
            requireContext(),
            message,
            android.widget.Toast.LENGTH_LONG
        ).show()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}