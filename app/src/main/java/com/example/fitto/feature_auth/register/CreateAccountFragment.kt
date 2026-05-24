package com.example.fitto.feature_auth.register

import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.fitto.R
import com.example.fitto.databinding.FragmentCreateAccountBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import java.util.Locale
import android.app.Dialog
import android.view.LayoutInflater
import android.view.Window
import android.view.WindowManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.fitto.databinding.DialogEmailVerificationWaitBinding
import com.example.fitto.data.local.session.UserSessionDataGuard
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
class CreateAccountFragment : Fragment(R.layout.fragment_create_account) {

    private var _binding: FragmentCreateAccountBinding? = null
    private val binding get() = _binding!!
    private var emailVerificationJob: Job? = null
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private var isRegistering = false

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
        if (isRegistering) return@with

        val email = emailEditText.text?.toString()
            ?.trim()
            ?.lowercase(Locale.ROOT)
            .orEmpty()
        val password = passwordEditText.text?.toString().orEmpty()
        val passwordRepeat = passwordRepeatEditText.text?.toString().orEmpty()

        val emailError = validateEmail(email)
        val passwordError = validatePassword(password)
        val passwordRepeatError = validatePasswordRepeat(password, passwordRepeat)

        emailInputLayout.error = emailError
        passwordInputLayout.error = passwordError
        passwordRepeatInputLayout.error = passwordRepeatError

        val hasError = emailError != null ||
                passwordError != null ||
                passwordRepeatError != null

        if (hasError) return@with

        setLoadingState(true)

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val user = authResult.user

                if (user == null) {
                    setLoadingState(false)
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.toast_register_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@addOnSuccessListener
                }

                user.sendEmailVerification()
                    .addOnSuccessListener {
                        setLoadingState(false)

                        Toast.makeText(
                            requireContext(),
                            getString(R.string.toast_verification_sent),
                            Toast.LENGTH_SHORT
                        ).show()

                        showEmailVerificationWaitingDialog(
                            uid = user.uid,
                            email = email
                        )
                    }
                    .addOnFailureListener {
                        setLoadingState(false)

                        Toast.makeText(
                            requireContext(),
                            getString(R.string.toast_verification_failed),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
            .addOnFailureListener { exception ->
                setLoadingState(false)

                if (exception is FirebaseAuthUserCollisionException) {
                    handleExistingEmailDuringRegister(
                        email = email,
                        password = password
                    )
                    return@addOnFailureListener
                }

                Toast.makeText(
                    requireContext(),
                    exception.localizedMessage ?: getString(R.string.toast_register_failed),
                    Toast.LENGTH_LONG
                ).show()
            }
    }
    private fun showEmailAlreadyRegisteredError() = with(binding) {
        emailInputLayout.error = getString(R.string.error_email_already_registered)
        emailEditText.requestFocus()

        Toast.makeText(
            requireContext(),
            getString(R.string.error_email_already_registered),
            Toast.LENGTH_LONG
        ).show()
    }
    private fun handleExistingEmailDuringRegister(
        email: String,
        password: String
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val user = result.user

                if (user == null) {
                    setLoadingState(false)
                    showEmailAlreadyRegisteredError()
                    return@addOnSuccessListener
                }

                user.reload()
                    .addOnSuccessListener {
                        val refreshedUser = auth.currentUser

                        if (refreshedUser?.isEmailVerified == true) {
                            setLoadingState(false)
                            auth.signOut()
                            showEmailAlreadyRegisteredError()
                        } else {
                            refreshedUser?.sendEmailVerification()
                                ?.addOnSuccessListener {
                                    setLoadingState(false)

                                    Toast.makeText(
                                        requireContext(),
                                        getString(R.string.register_verify_email_resent),
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    showEmailVerificationWaitingDialog(
                                        uid = refreshedUser.uid,
                                        email = email
                                    )
                                }
                                ?.addOnFailureListener {
                                    setLoadingState(false)

                                    Toast.makeText(
                                        requireContext(),
                                        getString(R.string.register_verify_email_resend_error),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
                    }
                    .addOnFailureListener {
                        setLoadingState(false)
                        showEmailAlreadyRegisteredError()
                    }
            }
            .addOnFailureListener {
                setLoadingState(false)
                showEmailAlreadyRegisteredError()
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

            if (_binding != null) {
                onFinished()
            }
        }
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

            deleteUnverifiedAuthUserIfPossible {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.register_verify_email_cancel_message1),
                    Toast.LENGTH_SHORT
                ).show()

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
    private fun deleteUnverifiedAuthUserIfPossible(
        onFinished: () -> Unit
    ) {
        val user = auth.currentUser

        if (user == null || user.isEmailVerified) {
            auth.signOut()
            onFinished()
            return
        }

        user.delete()
            .addOnCompleteListener {
                auth.signOut()
                onFinished()
            }
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
                    val verified = checkCurrentUserEmailVerified()

                    if (verified) {
                        dialogBinding.verificationStatusText.text =
                            getString(R.string.register_verify_email_verified)

                        createUserProfile(
                            uid = uid,
                            email = email,
                            onSuccess = {
                                runUserSessionGuardThenContinue(uid) {
                                    setLoadingState(false)
                                    emailVerificationJob?.cancel()

                                    if (dialog.isShowing) {
                                        dialog.dismiss()
                                    }

                                    findNavController().navigate(
                                        R.id.action_createAccountFragment_to_usernameSetupFragment
                                    )
                                }
                            },
                            onError = {
                                Toast.makeText(
                                    requireContext(),
                                    getString(R.string.toast_register_failed),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )

                        break
                    }

                    delay(2500)
                }
            }
        }
    }

    private suspend fun checkCurrentUserEmailVerified(): Boolean {
        val user = auth.currentUser ?: return false

        return runCatching {
            user.reload().await()
            auth.currentUser?.isEmailVerified == true
        }.getOrDefault(false)
    }

    private fun resendVerificationEmailFromDialog(
        dialogBinding: DialogEmailVerificationWaitBinding
    ) {
        val user = auth.currentUser ?: return

        dialogBinding.verificationResendButton.isEnabled = false
        dialogBinding.verificationResendButton.alpha = 0.55f

        user.sendEmailVerification()
            .addOnSuccessListener {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.register_verify_email_resent),
                    Toast.LENGTH_SHORT
                ).show()

                dialogBinding.verificationResendButton.postDelayed({
                    dialogBinding.verificationResendButton.isEnabled = true
                    dialogBinding.verificationResendButton.alpha = 1f
                }, 30000)
            }
            .addOnFailureListener {
                dialogBinding.verificationResendButton.isEnabled = true
                dialogBinding.verificationResendButton.alpha = 1f

                Toast.makeText(
                    requireContext(),
                    getString(R.string.register_verify_email_resend_error),
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun createUserProfile(
        uid: String,
        email: String,
        onSuccess: () -> Unit,
        onError: () -> Unit
    ) {
        val username = email.substringBefore("@")
            .lowercase()
            .replace(Regex("[^a-z0-9._]"), "")
            .ifBlank { "fitto_user" }

        val userData = hashMapOf(
            "uid" to uid,
            "realName" to username,
            "userName" to username,
            "email" to email,
            "photoUrl" to null,
            "bio" to "",
            "followersCount" to 0,
            "followingCount" to 0,
            "profileCompleted" to false,
            "createdAt" to System.currentTimeMillis(),
            "updatedAt" to System.currentTimeMillis()
        )

        firestore.collection("users")
            .document(uid)
            .set(userData)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener {
                onError()
            }
    }

    private fun validateEmail(email: String): String? {
        if (email.isBlank()) {
            return getString(R.string.login_error_email_required)
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return getString(R.string.error_invalid_email)
        }

        return null
    }

    private fun validatePassword(password: String): String? {
        if (password.isBlank()) {
            return getString(R.string.login_error_password_required)
        }

        if (password.length < MIN_PASSWORD_LENGTH) {
            return getString(R.string.error_password_min)
        }

        val hasSpecialChar = password.any { !it.isLetterOrDigit() }

        if (!hasSpecialChar) {
            return getString(R.string.error_password_special)
        }

        return null
    }

    private fun validatePasswordRepeat(
        password: String,
        passwordRepeat: String
    ): String? {
        if (passwordRepeat.isBlank()) {
            return getString(R.string.login_error_password_required)
        }

        if (password != passwordRepeat) {
            return getString(R.string.error_password_match)
        }

        return null
    }

    private fun setLoadingState(isLoading: Boolean) = with(binding) {
        isRegistering = isLoading

        createAccountButton.isEnabled = !isLoading
        createAccountButton.alpha = if (isLoading) 0.65f else 1f

        emailEditText.isEnabled = !isLoading
        passwordEditText.isEnabled = !isLoading
        passwordRepeatEditText.isEnabled = !isLoading
    }

    override fun onDestroyView() {
        emailVerificationJob?.cancel()
        emailVerificationJob = null
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val MIN_PASSWORD_LENGTH = 6
    }
}