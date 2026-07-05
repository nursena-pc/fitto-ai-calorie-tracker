package com.example.fitto.feature_auth.register

import android.app.Dialog
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.fitto.R
import com.example.fitto.data.local.session.UserSessionDataGuard
import com.example.fitto.databinding.DialogEmailVerificationWaitBinding
import com.example.fitto.databinding.FragmentCreateAccountBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class CreateAccountFragment : Fragment(R.layout.fragment_create_account) {

    private var _binding: FragmentCreateAccountBinding? = null
    private val binding get() = _binding!!

    private val registrationController = AccountRegistrationController()
    private val registrationCoordinator: AccountRegistrationCoordinator by lazy {
        AccountRegistrationCoordinator(FirebaseAccountRegistrationDataSource())
    }

    private var emailVerificationJob: Job? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCreateAccountBinding.bind(view)

        setupClicks()
        setupInputs()
    }

    private fun setupClicks() = with(binding) {
        createAccountBackButton.setOnClickListener {
            findNavController().popBackStack()
        }
        loginText.setOnClickListener {
            findNavController().navigate(R.id.action_createAccountFragment_to_loginFragment)
        }
        createAccountButton.setOnClickListener {
            submitCreateAccount()
        }
    }

    private fun setupInputs() = with(binding) {
        emailEditText.doAfterTextChanged {
            emailInputLayout.error = null
        }
        passwordEditText.doAfterTextChanged {
            passwordInputLayout.error = null
            if (!passwordRepeatEditText.text.isNullOrBlank()) {
                passwordRepeatInputLayout.error = null
            }
        }
        passwordRepeatEditText.doAfterTextChanged {
            passwordRepeatInputLayout.error = null
        }
    }

    private fun submitCreateAccount() = with(binding) {
        val submission = registrationController.prepareSubmission(
            email = emailEditText.text?.toString().orEmpty(),
            password = passwordEditText.text?.toString().orEmpty(),
            passwordRepeat = passwordRepeatEditText.text?.toString().orEmpty(),
            isValidEmail = { value -> Patterns.EMAIL_ADDRESS.matcher(value).matches() }
        )

        when (submission) {
            AccountRegistrationSubmission.AlreadySubmitting -> Unit
            is AccountRegistrationSubmission.Invalid -> {
                renderValidation(submission.validation)
            }

            is AccountRegistrationSubmission.Ready -> {
                renderValidation(
                    AccountRegistrationValidation(normalizedEmail = submission.email)
                )
                setLoadingState(true)
                viewLifecycleOwner.lifecycleScope.launch {
                    val result = registrationCoordinator.startRegistration(
                        email = submission.email,
                        password = submission.password
                    )
                    registrationController.completeSubmission()
                    if (_binding == null) return@launch

                    setLoadingState(false)
                    handleRegistrationResult(result)
                }
            }
        }
    }

    private fun handleRegistrationResult(result: AccountRegistrationStartResult) {
        when (result) {
            is AccountRegistrationStartResult.VerificationSent -> {
                Toast.makeText(
                    requireContext(),
                    getString(
                        if (result.wasResent) {
                            R.string.register_verify_email_resent
                        } else {
                            R.string.toast_verification_sent
                        }
                    ),
                    Toast.LENGTH_SHORT
                ).show()
                showEmailVerificationWaitingDialog(
                    uid = result.uid,
                    email = result.email
                )
            }

            AccountRegistrationStartResult.EmailAlreadyRegistered -> {
                showEmailAlreadyRegisteredError()
            }

            AccountRegistrationStartResult.VerificationSendFailed -> {
                showToast(R.string.toast_verification_failed)
            }

            AccountRegistrationStartResult.VerificationResendFailed -> {
                showToast(R.string.register_verify_email_resend_error)
            }

            is AccountRegistrationStartResult.Failure -> {
                Toast.makeText(
                    requireContext(),
                    result.message ?: getString(R.string.toast_register_failed),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun renderValidation(validation: AccountRegistrationValidation) = with(binding) {
        emailInputLayout.error = validation.emailIssue?.let { issue ->
            getString(
                when (issue) {
                    RegistrationEmailIssue.REQUIRED -> R.string.login_error_email_required
                    RegistrationEmailIssue.INVALID -> R.string.error_invalid_email
                }
            )
        }
        passwordInputLayout.error = validation.passwordIssue?.let { issue ->
            getString(
                when (issue) {
                    RegistrationPasswordIssue.REQUIRED -> {
                        R.string.login_error_password_required
                    }

                    RegistrationPasswordIssue.TOO_SHORT -> R.string.error_password_min
                    RegistrationPasswordIssue.SPECIAL_CHARACTER_REQUIRED -> {
                        R.string.error_password_special
                    }
                }
            )
        }
        passwordRepeatInputLayout.error = validation.passwordRepeatIssue?.let { issue ->
            getString(
                when (issue) {
                    RegistrationPasswordRepeatIssue.REQUIRED -> {
                        R.string.login_error_password_required
                    }

                    RegistrationPasswordRepeatIssue.DOES_NOT_MATCH -> {
                        R.string.error_password_match
                    }
                }
            )
        }
    }

    private fun showEmailAlreadyRegisteredError() = with(binding) {
        emailInputLayout.error = getString(R.string.error_email_already_registered)
        emailEditText.requestFocus()
        showToast(R.string.error_email_already_registered, Toast.LENGTH_LONG)
    }

    private fun showEmailVerificationWaitingDialog(
        uid: String,
        email: String
    ) {
        val dialogBinding = DialogEmailVerificationWaitBinding.inflate(
            LayoutInflater.from(requireContext())
        )
        val dialog = Dialog(requireContext()).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(dialogBinding.root)
            setCanceledOnTouchOutside(false)
            setCancelable(false)
        }

        dialogBinding.verificationCloseButton.setOnClickListener {
            emailVerificationJob?.cancel()
            viewLifecycleOwner.lifecycleScope.launch {
                registrationCoordinator.cancelRegistration()
                if (_binding == null) return@launch

                showToast(R.string.register_verify_email_cancel_message1)
                dialog.dismiss()
            }
        }
        dialogBinding.verificationResendButton.setOnClickListener {
            resendVerificationEmailFromDialog(dialogBinding)
        }

        dialog.show()
        dialog.window?.let { window ->
            window.setBackgroundDrawableResource(android.R.color.transparent)
            window.setLayout(
                (resources.displayMetrics.widthPixels * 0.90f).toInt(),
                WindowManager.LayoutParams.WRAP_CONTENT
            )
        }

        startEmailVerificationWatcher(
            uid = uid,
            email = email,
            dialog = dialog,
            dialogBinding = dialogBinding
        )
    }

    private fun startEmailVerificationWatcher(
        uid: String,
        email: String,
        dialog: Dialog,
        dialogBinding: DialogEmailVerificationWaitBinding
    ) {
        emailVerificationJob?.cancel()
        emailVerificationJob = viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (isActive) {
                    if (registrationCoordinator.isEmailVerified()) {
                        dialogBinding.verificationStatusText.text =
                            getString(R.string.register_verify_email_verified)

                        val profileCreated = registrationCoordinator.createInitialProfile(
                            uid = uid,
                            email = email
                        )
                        if (profileCreated) {
                            runUserSessionGuardThenContinue(uid) {
                                setLoadingState(false)
                                emailVerificationJob?.cancel()
                                if (dialog.isShowing) dialog.dismiss()
                                findNavController().navigate(
                                    R.id.action_createAccountFragment_to_usernameSetupFragment
                                )
                            }
                        } else {
                            showToast(R.string.toast_register_failed)
                        }
                        break
                    }
                    delay(EMAIL_VERIFICATION_POLL_MILLIS)
                }
            }
        }
    }

    private fun resendVerificationEmailFromDialog(
        dialogBinding: DialogEmailVerificationWaitBinding
    ) {
        dialogBinding.verificationResendButton.isEnabled = false
        dialogBinding.verificationResendButton.alpha = 0.55f

        viewLifecycleOwner.lifecycleScope.launch {
            val sent = registrationCoordinator.resendVerificationEmail()
            if (_binding == null) return@launch

            if (sent) {
                showToast(R.string.register_verify_email_resent)
                dialogBinding.verificationResendButton.postDelayed({
                    dialogBinding.verificationResendButton.isEnabled = true
                    dialogBinding.verificationResendButton.alpha = 1f
                }, VERIFICATION_RESEND_COOLDOWN_MILLIS)
            } else {
                dialogBinding.verificationResendButton.isEnabled = true
                dialogBinding.verificationResendButton.alpha = 1f
                showToast(R.string.register_verify_email_resend_error)
            }
        }
    }

    private fun runUserSessionGuardThenContinue(
        uid: String,
        onFinished: () -> Unit
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            UserSessionDataGuard.ensureScopedDataForUser(
                context = requireContext().applicationContext,
                currentUid = uid
            )
            if (_binding != null) onFinished()
        }
    }

    private fun setLoadingState(isLoading: Boolean) = with(binding) {
        createAccountButton.isEnabled = !isLoading
        createAccountButton.alpha = if (isLoading) 0.65f else 1f
        emailEditText.isEnabled = !isLoading
        passwordEditText.isEnabled = !isLoading
        passwordRepeatEditText.isEnabled = !isLoading
    }

    private fun showToast(stringRes: Int, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(requireContext(), getString(stringRes), duration).show()
    }

    override fun onDestroyView() {
        emailVerificationJob?.cancel()
        emailVerificationJob = null
        _binding = null
        super.onDestroyView()
    }

    private companion object {
        const val EMAIL_VERIFICATION_POLL_MILLIS = 2_500L
        const val VERIFICATION_RESEND_COOLDOWN_MILLIS = 30_000L
    }
}
