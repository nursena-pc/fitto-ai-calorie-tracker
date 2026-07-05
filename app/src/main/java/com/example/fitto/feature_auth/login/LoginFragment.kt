package com.example.fitto.feature_auth.login

import android.app.Dialog
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.fitto.R
import com.example.fitto.data.local.session.UserSessionDataGuard
import com.example.fitto.databinding.DialogResetPasswordBinding
import com.example.fitto.databinding.FragmentLoginBinding
import kotlinx.coroutines.launch

class LoginFragment : Fragment(R.layout.fragment_login) {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val loginCoordinator: AuthLoginCoordinator by lazy {
        AuthLoginCoordinator(FirebaseAuthLoginDataSource())
    }

    private var isLoading = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLoginBinding.bind(view)

        setupClicks()
        setupKeyboard()
        renderRegisterText()
    }

    private fun renderRegisterText() = with(binding) {
        val fullText = getString(R.string.login_not_member_cta)
        val actionText = getString(R.string.login_create_account_action)
        val actionStart = fullText.indexOf(actionText)

        goRegisterText.text = SpannableString(fullText).apply {
            if (actionStart >= 0) {
                setSpan(
                    ForegroundColorSpan(
                        ContextCompat.getColor(requireContext(), R.color.fitto_green)
                    ),
                    actionStart,
                    actionStart + actionText.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
    }

    private fun setupClicks() = with(binding) {
        loginBackButton.setOnClickListener {
            findNavController().popBackStack()
        }
        loginButton.setOnClickListener {
            login()
        }
        forgotPasswordText.setOnClickListener {
            showResetPasswordDialog()
        }
        goRegisterText.setOnClickListener {
            findNavController().navigate(R.id.consentFragment)
        }
        loginGoogleButton.setOnClickListener {
            loginWithGoogle()
        }
    }

    private fun setupKeyboard() {
        binding.passwordEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                login()
                true
            } else {
                false
            }
        }
    }

    private fun login() {
        if (isLoading) return
        clearErrors()

        val emailOrUsername = binding.emailUsernameEditText.text
            ?.toString()
            ?.trim()
            .orEmpty()
        val password = binding.passwordEditText.text
            ?.toString()
            .orEmpty()
        val validation = LoginInputPolicy.validate(
            emailOrUsername = emailOrUsername,
            password = password,
            isValidEmail = { value -> Patterns.EMAIL_ADDRESS.matcher(value).matches() }
        )

        renderValidation(validation)
        if (!validation.isValid) return

        setLoading(true)
        viewLifecycleOwner.lifecycleScope.launch {
            when (val result = loginCoordinator.signIn(emailOrUsername, password)) {
                AuthLoginResult.Success -> {
                    if (loginCoordinator.blockUnverifiedPasswordUserIfNeeded()) {
                        setLoading(false)
                        showGeneralError(getString(R.string.login_email_not_verified))
                    } else {
                        runUserSessionGuardThenNavigateHome()
                    }
                }

                is AuthLoginResult.Failure -> {
                    setLoading(false)
                    showGeneralError(getString(result.reason.messageRes()))
                }
            }
        }
    }

    private fun loginWithGoogle() {
        if (isLoading) return

        clearErrors()
        setLoading(true)

        viewLifecycleOwner.lifecycleScope.launch {
            GoogleSignInHelper.signIn(
                context = requireContext(),
                onSuccess = ::runUserSessionGuardThenNavigateHome,
                onError = { message ->
                    setLoading(false)
                    showGeneralError(message)
                }
            )
        }
    }

    private fun runUserSessionGuardThenNavigateHome() {
        val uid = loginCoordinator.currentUserId()

        viewLifecycleOwner.lifecycleScope.launch {
            if (!uid.isNullOrBlank()) {
                UserSessionDataGuard.ensureScopedDataForUser(
                    context = requireContext().applicationContext,
                    currentUid = uid
                )
            }

            if (_binding != null) {
                setLoading(false)
                findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
            }
        }
    }

    private fun renderValidation(validation: LoginInputValidation) = with(binding) {
        emailUsernameInputLayout.error = validation.identifierIssue?.let { issue ->
            getString(
                when (issue) {
                    LoginIdentifierIssue.REQUIRED -> R.string.login_error_required_field
                    LoginIdentifierIssue.INVALID_EMAIL -> R.string.login_error_invalid_email
                    LoginIdentifierIssue.USERNAME_TOO_SHORT -> R.string.login_error_username_min
                }
            )
        }
        passwordInputLayout.error = validation.passwordIssue?.let { issue ->
            getString(
                when (issue) {
                    LoginPasswordIssue.REQUIRED -> R.string.login_error_password_required
                    LoginPasswordIssue.TOO_SHORT -> R.string.login_error_password_min
                }
            )
        }
    }

    private fun showResetPasswordDialog() {
        val dialogBinding = DialogResetPasswordBinding.inflate(
            LayoutInflater.from(requireContext())
        )
        dialogBinding.resetEmailEditText.setText(
            binding.emailUsernameEditText.text
                ?.toString()
                ?.takeIf { it.contains("@") }
                .orEmpty()
        )
        val dialog = Dialog(requireContext()).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(dialogBinding.root)
            setCanceledOnTouchOutside(true)
        }

        dialogBinding.resetCancelButton.setOnClickListener {
            dialog.dismiss()
        }
        dialogBinding.resetSendButton.setOnClickListener {
            val email = dialogBinding.resetEmailEditText.text
                ?.toString()
                ?.trim()
                .orEmpty()

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                dialogBinding.resetEmailInputLayout.error =
                    getString(R.string.login_error_invalid_email)
                return@setOnClickListener
            }

            dialogBinding.resetEmailInputLayout.error = null
            dialogBinding.resetSendButton.isEnabled = false
            dialogBinding.resetSendButton.alpha = 0.65f

            viewLifecycleOwner.lifecycleScope.launch {
                when (val result = loginCoordinator.sendPasswordReset(email)) {
                    PasswordResetResult.Success -> {
                        dialog.dismiss()
                        showGeneralError(getString(R.string.login_reset_password_success))
                    }

                    is PasswordResetResult.Failure -> {
                        dialogBinding.resetSendButton.isEnabled = true
                        dialogBinding.resetSendButton.alpha = 1f
                        dialogBinding.resetEmailInputLayout.error =
                            getString(result.reason.messageRes())
                    }
                }
            }
        }

        dialog.show()
        dialog.window?.let { window ->
            window.setBackgroundDrawableResource(android.R.color.transparent)
            window.setLayout(
                (resources.displayMetrics.widthPixels * 0.90f).toInt(),
                WindowManager.LayoutParams.WRAP_CONTENT
            )
        }
    }

    private fun clearErrors() = with(binding) {
        emailUsernameInputLayout.error = null
        passwordInputLayout.error = null
        generalErrorText.isVisible = false
        generalErrorText.text = ""
    }

    private fun showGeneralError(message: String) = with(binding) {
        generalErrorText.text = message
        generalErrorText.isVisible = true
    }

    private fun setLoading(loading: Boolean) = with(binding) {
        isLoading = loading
        loginProgress.isVisible = loading
        loginButton.text = if (loading) "" else getString(R.string.login_button)
        loginButton.isEnabled = !loading
        emailUsernameEditText.isEnabled = !loading
        passwordEditText.isEnabled = !loading
        forgotPasswordText.isEnabled = !loading
        goRegisterText.isEnabled = !loading
        loginGoogleButton.isEnabled = !loading
    }

    private fun AuthLoginFailure.messageRes(): Int {
        return when (this) {
            AuthLoginFailure.EMAIL_OR_USERNAME_REQUIRED -> {
                R.string.auth_error_email_or_username_required
            }

            AuthLoginFailure.USERNAME_TOO_SHORT -> R.string.auth_error_username_min
            AuthLoginFailure.USERNAME_NOT_FOUND -> R.string.auth_error_username_not_found
            AuthLoginFailure.USERNAME_PROFILE_MISSING -> {
                R.string.auth_error_username_profile_missing
            }

            AuthLoginFailure.INVALID_CREDENTIALS -> {
                R.string.auth_error_invalid_login_credentials
            }

            AuthLoginFailure.ACCOUNT_NOT_FOUND -> R.string.auth_error_account_not_found
            AuthLoginFailure.NETWORK -> R.string.auth_error_network
            AuthLoginFailure.USERNAME_LOOKUP_UNKNOWN -> {
                R.string.auth_error_username_lookup_unknown
            }
            AuthLoginFailure.UNKNOWN -> R.string.auth_error_login_unknown
        }
    }

    private fun PasswordResetFailure.messageRes(): Int {
        return when (this) {
            PasswordResetFailure.ACCOUNT_NOT_FOUND -> {
                R.string.auth_error_reset_account_not_found
            }

            PasswordResetFailure.INVALID_EMAIL -> R.string.auth_error_reset_invalid_email
            PasswordResetFailure.NETWORK -> R.string.auth_error_network
            PasswordResetFailure.UNKNOWN -> R.string.auth_error_reset_unknown
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
