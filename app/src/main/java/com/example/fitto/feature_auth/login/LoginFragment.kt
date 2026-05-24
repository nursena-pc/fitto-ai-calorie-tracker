package com.example.fitto.feature_auth.login

import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.fitto.R
import com.example.fitto.databinding.FragmentLoginBinding
import android.app.Dialog
import android.view.LayoutInflater
import android.view.Window
import android.view.WindowManager
import androidx.lifecycle.lifecycleScope
import com.example.fitto.databinding.DialogResetPasswordBinding
import com.example.fitto.data.local.session.UserSessionDataGuard
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import androidx.core.content.ContextCompat
class LoginFragment : Fragment(R.layout.fragment_login) {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

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

        val spannable = SpannableString(fullText)
        val actionStart = fullText.indexOf(actionText)

        if (actionStart >= 0) {
            spannable.setSpan(
                ForegroundColorSpan(
                    ContextCompat.getColor(requireContext(), R.color.fitto_green)
                ),
                actionStart,
                actionStart + actionText.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        goRegisterText.text = spannable
    }
    private fun setupClicks() {
        binding.loginBackButton.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.loginButton.setOnClickListener {
            login()
        }

        binding.forgotPasswordText.setOnClickListener {
            showResetPasswordDialog()
        }

        binding.goRegisterText.setOnClickListener {
            findNavController().navigate(R.id.consentFragment)
        }

        binding.loginGoogleButton.setOnClickListener {
            loginWithGoogle()
        }

    }

    private fun runUserSessionGuardThenNavigateHome() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid

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

    private fun loginWithGoogle() {
        if (isLoading) return

        clearErrors()
        setLoading(true)

        viewLifecycleOwner.lifecycleScope.launch {
            GoogleSignInHelper.signIn(
                context = requireContext(),
                onSuccess = {
                    runUserSessionGuardThenNavigateHome()
                },
                onError = { message ->
                    setLoading(false)
                    showGeneralError(message)
                }
            )
        }
    }

    private fun blockUnverifiedEmailUserIfNeeded(): Boolean {
        val user = FirebaseAuth.getInstance().currentUser ?: return false

        val isPasswordUser = user.providerData.any { provider ->
            provider.providerId == "password"
        }

        if (!isPasswordUser || user.isEmailVerified) {
            return false
        }

        user.sendEmailVerification()
        FirebaseAuth.getInstance().signOut()

        showGeneralError(getString(R.string.login_email_not_verified))
        return true
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

        if (!validate(emailOrUsername, password)) return

        setLoading(true)

        FirebaseAuthHelper.loginWithEmailOrUsername(
            context = requireContext(),
            emailOrUsername = emailOrUsername,
            password = password,
            onSuccess = {
                if (blockUnverifiedEmailUserIfNeeded()) {
                    setLoading(false)
                    return@loginWithEmailOrUsername
                }

                runUserSessionGuardThenNavigateHome()
            },
            onError = { message ->
                setLoading(false)
                showGeneralError(message)
            }
        )
    }

    private fun validate(emailOrUsername: String, password: String): Boolean {
        var isValid = true

        if (emailOrUsername.isBlank()) {
            binding.emailUsernameInputLayout.error =
                getString(R.string.login_error_required_field)
            isValid = false
        } else if (
            emailOrUsername.contains("@") &&
            !Patterns.EMAIL_ADDRESS.matcher(emailOrUsername).matches()
        ) {
            binding.emailUsernameInputLayout.error =
                getString(R.string.login_error_invalid_email)
            isValid = false
        } else if (
            !emailOrUsername.contains("@") &&
            emailOrUsername.removePrefix("@").length < 3
        ) {
            binding.emailUsernameInputLayout.error =
                getString(R.string.login_error_username_min)
            isValid = false
        }

        if (password.isBlank()) {
            binding.passwordInputLayout.error =
                getString(R.string.login_error_password_required)
            isValid = false
        } else if (password.length < 6) {
            binding.passwordInputLayout.error =
                getString(R.string.login_error_password_min)
            isValid = false
        }

        return isValid
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

            FirebaseAuthHelper.sendResetPasswordEmail(
                context = requireContext(),
                email = email,
                onSuccess = {
                    dialog.dismiss()
                    showGeneralError(getString(R.string.login_reset_password_success))
                },
                onError = { message ->
                    dialogBinding.resetSendButton.isEnabled = true
                    dialogBinding.resetSendButton.alpha = 1f
                    dialogBinding.resetEmailInputLayout.error = message
                }
            )
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

    private fun sendResetPasswordEmail(email: String) {
        val cleanEmail = email.trim()

        if (!Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            showGeneralError(getString(R.string.login_error_invalid_email))
            return
        }

        FirebaseAuthHelper.sendResetPasswordEmail(
            context = requireContext(),
            email = cleanEmail,
            onSuccess = {
                showGeneralError(getString(R.string.login_reset_password_success))
            },
            onError = { message ->
                showGeneralError(message)
            }
        )
    }

    private fun clearErrors() {
        binding.emailUsernameInputLayout.error = null
        binding.passwordInputLayout.error = null
        binding.generalErrorText.isVisible = false
        binding.generalErrorText.text = ""
    }

    private fun showGeneralError(message: String) {
        binding.generalErrorText.text = message
        binding.generalErrorText.isVisible = true
    }

    private fun setLoading(loading: Boolean) {
        isLoading = loading

        binding.loginProgress.isVisible = loading
        binding.loginButton.text = if (loading) {
            ""
        } else {
            getString(R.string.login_button)
        }

        binding.loginButton.isEnabled = !loading
        binding.emailUsernameEditText.isEnabled = !loading
        binding.passwordEditText.isEnabled = !loading
        binding.forgotPasswordText.isEnabled = !loading
        binding.goRegisterText.isEnabled = !loading
        binding.loginGoogleButton.isEnabled = !loading
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}