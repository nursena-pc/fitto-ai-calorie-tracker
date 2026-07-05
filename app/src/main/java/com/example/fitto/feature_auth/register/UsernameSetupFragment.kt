package com.example.fitto.feature_auth.register

import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.fitto.R
import com.example.fitto.fittoAppContainer
import com.example.fitto.databinding.FragmentUsernameSetupBinding
import kotlinx.coroutines.launch
import kotlin.random.Random

class UsernameSetupFragment : Fragment(R.layout.fragment_username_setup) {

    private var _binding: FragmentUsernameSetupBinding? = null
    private val binding get() = _binding!!

    private val controller = UsernameSetupController()
    private val coordinator: UsernameSetupCoordinator by lazy {
        requireContext().fittoAppContainer.authFeatureDependencies.usernameSetupCoordinator
    }

    private lateinit var onboardingViewModel: OnboardingViewModel
    private val handler = Handler(Looper.getMainLooper())
    private val usernameCheckRunnable = Runnable(::checkUsernameAvailability)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentUsernameSetupBinding.bind(view)
        onboardingViewModel = obtainOnboardingViewModel()

        setupUser()
        setupSuggestions()
        setupInput()
        setupClicks()
        renderContinueButton()
    }

    private fun setupUser() {
        binding.usernameWelcomeText.text = getString(
            R.string.username_setup_welcome,
            getDisplayName()
        )
    }

    private fun setupSuggestions() = with(binding) {
        val identity = coordinator.currentIdentity()
        val base = controller.createBaseUsername(
            email = identity?.email,
            displayName = identity?.displayName
        )
        val suggestions = controller.suggestions(
            baseUsername = base,
            randomSuffixes = listOf(
                Random.nextInt(10, 99),
                Random.nextInt(10, 99),
                Random.nextInt(10, 99)
            )
        )

        bindSuggestion(usernameSuggestionOne, suggestions[0])
        bindSuggestion(usernameSuggestionTwo, suggestions[1])
        bindSuggestion(usernameSuggestionThree, suggestions[2])
    }

    private fun bindSuggestion(view: TextView, username: String) {
        view.text = "@$username"
        view.setOnClickListener {
            binding.usernameEditText.setText(username)
            binding.usernameEditText.setSelection(username.length)
        }
    }

    private fun setupInput() = with(binding) {
        usernameEditText.doAfterTextChanged { editable ->
            val current = editable?.toString().orEmpty()
            val update = controller.onInputChanged(current)

            if (current != update.state.input) {
                usernameEditText.setText(update.state.input)
                usernameEditText.setSelection(update.state.input.length)
                return@doAfterTextChanged
            }

            usernameInputLayout.error = update.state.validationIssue
                ?.takeIf { update.state.input.isNotBlank() }
                ?.let(::validationMessage)
            usernameStatusText.isVisible = false
            usernameSuggestionsContainer.isVisible = update.showSuggestions
            renderContinueButton()

            handler.removeCallbacks(usernameCheckRunnable)
            if (update.shouldCheckAvailability) {
                usernameStatusText.text = getString(R.string.username_checking)
                usernameStatusText.setTextColor(
                    requireContext().getColor(R.color.fitto_green)
                )
                usernameStatusText.isVisible = true
                handler.postDelayed(
                    usernameCheckRunnable,
                    USERNAME_CHECK_DELAY_MILLIS
                )
            }
        }
    }

    private fun setupClicks() {
        binding.usernameContinueButton.setOnClickListener {
            submitUsername()
        }
    }

    private fun checkUsernameAvailability() {
        val username = controller.state.input
        val validationIssue = controller.validate(username)
        if (validationIssue != null) {
            binding.usernameInputLayout.error = validationMessage(validationIssue)
            binding.usernameStatusText.isVisible = false
            controller.applyAvailabilityFailure()
            renderContinueButton()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                coordinator.checkAvailability(username)
            }.onSuccess { availability ->
                if (_binding == null) return@onSuccess
                val available = availability == UsernameAvailability.AVAILABLE
                val updatedState = controller.applyAvailability(username, available)
                if (
                    updatedState.input != username ||
                    updatedState.lastCheckedUsername != username
                ) {
                    return@onSuccess
                }

                binding.usernameStatusText.isVisible = true
                binding.usernameStatusText.text = getString(
                    if (available) {
                        R.string.username_available
                    } else {
                        R.string.username_taken_inline
                    }
                )
                binding.usernameStatusText.setTextColor(
                    requireContext().getColor(
                        if (available) {
                            R.color.fitto_green
                        } else {
                            android.R.color.holo_red_dark
                        }
                    )
                )
                renderContinueButton()
            }.onFailure { exception ->
                if (_binding == null) return@onFailure
                Log.e(TAG, "Username availability check failed", exception)
                controller.applyAvailabilityFailure()
                binding.usernameStatusText.isVisible = false
                binding.usernameInputLayout.error =
                    getString(R.string.username_error_unknown)
                renderContinueButton()
            }
        }
    }

    private fun submitUsername() {
        when (val decision = controller.prepareSubmission()) {
            UsernameSubmissionDecision.AlreadySubmitting -> Unit
            is UsernameSubmissionDecision.Invalid -> {
                binding.usernameInputLayout.error = validationMessage(decision.issue)
            }

            UsernameSubmissionDecision.NeedsAvailabilityCheck -> {
                checkUsernameAvailability()
            }

            is UsernameSubmissionDecision.Ready -> {
                val identity = coordinator.currentIdentity()
                if (identity == null) {
                    controller.completeSubmission()
                    binding.usernameInputLayout.error =
                        getString(R.string.username_error_unknown)
                    renderContinueButton()
                    return
                }

                setLoading(true)
                val state = onboardingViewModel.state
                val dailyNeeds = if (onboardingViewModel.canCalculateDailyNeeds()) {
                    onboardingViewModel.calculateDailyNeeds()
                } else {
                    null
                }

                viewLifecycleOwner.lifecycleScope.launch {
                    val result = coordinator.saveProfile(
                        username = decision.username,
                        displayName = getDisplayName(),
                        state = state,
                        dailyNeeds = dailyNeeds
                    )
                    if (_binding == null) return@launch

                    when (result) {
                        is UsernameProfileSaveResult.Success -> {
                            controller.completeSubmission()
                            setLoading(false)
                            findNavController().navigate(
                                R.id.action_usernameSetupFragment_to_homeFragment
                            )
                        }

                        UsernameProfileSaveResult.UsernameTaken -> {
                            controller.markTaken()
                            setLoading(false)
                            binding.usernameInputLayout.error =
                                getString(R.string.username_error_taken)
                            binding.usernameStatusText.text =
                                getString(R.string.username_taken_inline)
                            binding.usernameStatusText.setTextColor(
                                requireContext().getColor(
                                    android.R.color.holo_red_dark
                                )
                            )
                            binding.usernameStatusText.isVisible = true
                            renderContinueButton()
                        }

                        is UsernameProfileSaveResult.Failure -> {
                            controller.completeSubmission()
                            setLoading(false)
                            Log.e(TAG, "Profile creation failed", result.cause)
                            binding.usernameInputLayout.error =
                                getString(R.string.username_error_unknown)
                            renderContinueButton()
                        }
                    }
                }
            }
        }
    }

    private fun renderContinueButton() = with(binding) {
        val canContinue = controller.state.canContinue
        usernameContinueButton.isEnabled = canContinue

        if (canContinue) {
            usernameContinueButton.setTextColor(
                requireContext().getColor(android.R.color.white)
            )
            usernameContinueButton.backgroundTintList =
                ColorStateList.valueOf(requireContext().getColor(R.color.fitto_green))
        } else {
            usernameContinueButton.setTextColor(0xFF9FA09A.toInt())
            usernameContinueButton.backgroundTintList =
                ColorStateList.valueOf(0xFFD6D8CD.toInt())
        }
    }

    private fun setLoading(isLoading: Boolean) = with(binding) {
        usernameContinueButton.isEnabled = !isLoading
        usernameContinueButton.alpha = if (isLoading) 0.65f else 1f
        usernameEditText.isEnabled = !isLoading
        usernameSuggestionOne.isEnabled = !isLoading
        usernameSuggestionTwo.isEnabled = !isLoading
        usernameSuggestionThree.isEnabled = !isLoading
    }

    private fun getDisplayName(): String {
        val identity = coordinator.currentIdentity()
        return controller.resolveDisplayName(
            onboardingName = onboardingViewModel.state.name,
            authDisplayName = identity?.displayName,
            email = identity?.email,
            appName = getString(R.string.app_name)
        )
    }

    private fun validationMessage(issue: UsernameValidationIssue): String {
        return getString(
            when (issue) {
                UsernameValidationIssue.TOO_SHORT -> R.string.username_error_too_short
                UsernameValidationIssue.TOO_LONG -> R.string.username_error_too_long
                UsernameValidationIssue.INVALID_CHARACTERS -> {
                    R.string.username_error_invalid_chars
                }
            }
        )
    }

    override fun onDestroyView() {
        handler.removeCallbacks(usernameCheckRunnable)
        _binding = null
        super.onDestroyView()
    }

    private companion object {
        const val TAG = "UsernameSetupFragment"
        const val USERNAME_CHECK_DELAY_MILLIS = 450L
    }
}
