package com.example.fitto.feature_home.presentation.profile

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.TextView
import androidx.core.view.isVisible
import com.example.fitto.R
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ProfileEmailChangeDialogCoordinator(
    private val context: Context,
    private val inflater: LayoutInflater,
    private val scope: CoroutineScope,
    action: ProfileEmailChangeAction,
    languageCode: () -> String,
    private val onVerified: () -> Unit,
    private val onVerificationFinished: () -> Unit,
    private val onCheckFailed: (Throwable) -> Unit,
    private val onResendFailed: (Throwable) -> Unit
) {
    private var dialog: Dialog? = null
    private var statusText: TextView? = null
    private var progressView: View? = null
    private var resendButton: MaterialButton? = null
    private var closeButton: MaterialButton? = null
    private var pendingEmail: String? = null
    private var verifiedJob: Job? = null

    private val emailChangeCoordinator = ProfileEmailChangeCoordinator(
        scope = scope,
        action = action,
        languageCode = languageCode,
        onStateChanged = ::renderState,
        onResendAvailabilityChanged = ::renderResendAvailability,
        onCheckFailed = onCheckFailed,
        onResendFailed = onResendFailed
    )

    val isRecoveryActive: Boolean
        get() = emailChangeCoordinator.isRecoveryActive

    fun show(request: ProfilePendingEmailChange) {
        dismissCurrentDialog(stopCoordinator = true)
        pendingEmail = request.email

        val currentDialog = Dialog(context).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setCanceledOnTouchOutside(false)
            setCancelable(false)
        }

        val dialogView = inflater.inflate(
            R.layout.dialog_email_verification_wait,
            null,
            false
        )

        val titleText = dialogView.findViewById<TextView>(R.id.verificationTitleText)
        val messageText = dialogView.findViewById<TextView>(R.id.verificationMessageText)
        val currentStatusText = dialogView.findViewById<TextView>(R.id.verificationStatusText)
        val currentProgressView = dialogView.findViewById<View>(R.id.verificationProgress)
        val currentResendButton = dialogView.findViewById<MaterialButton>(R.id.verificationResendButton)
        val currentCloseButton = dialogView.findViewById<MaterialButton>(R.id.verificationCloseButton)

        titleText.setText(R.string.profile_account_email_wait_title)
        messageText.text = context.getString(
            R.string.profile_account_email_wait_message,
            request.email
        )
        currentStatusText.setText(R.string.profile_account_email_wait_checking)
        currentResendButton.setText(R.string.profile_account_email_wait_resend)
        currentCloseButton.setText(R.string.profile_account_email_wait_close)

        currentResendButton.setOnClickListener {
            pendingEmail?.let(emailChangeCoordinator::resend)
        }
        currentCloseButton.setOnClickListener {
            dismissCurrentDialog(stopCoordinator = true)
        }

        currentDialog.setContentView(dialogView)
        currentDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        currentDialog.setOnDismissListener {
            if (dialog === currentDialog) {
                releaseDialogReferences()
            }
        }

        dialog = currentDialog
        statusText = currentStatusText
        progressView = currentProgressView
        resendButton = currentResendButton
        closeButton = currentCloseButton

        currentDialog.show()
        currentDialog.window?.setLayout(
            (context.resources.displayMetrics.widthPixels * DIALOG_WIDTH_RATIO).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        emailChangeCoordinator.start(request)
    }

    fun clear() {
        dismissCurrentDialog(stopCoordinator = true)
    }

    private fun renderState(state: ProfileEmailChangeState) {
        when (state) {
            ProfileEmailChangeState.Checking -> {
                progressView?.isVisible = true
                resendButton?.isVisible = true
                closeButton?.isEnabled = true
                statusText?.setText(R.string.profile_account_email_wait_checking)
            }

            ProfileEmailChangeState.RestoringSession -> {
                progressView?.isVisible = true
                resendButton?.isVisible = true
                closeButton?.isEnabled = true
                statusText?.setText(R.string.profile_account_email_wait_restoring_session)
            }

            ProfileEmailChangeState.SessionLost -> {
                progressView?.isVisible = false
                resendButton?.isVisible = false
                closeButton?.isEnabled = true
                statusText?.setText(R.string.profile_account_email_wait_session_lost)
            }

            ProfileEmailChangeState.Resent -> {
                statusText?.setText(R.string.profile_account_email_wait_resent)
            }

            ProfileEmailChangeState.Verified -> renderVerifiedState()
        }
    }

    private fun renderVerifiedState() {
        progressView?.isVisible = false
        resendButton?.isVisible = false
        closeButton?.isEnabled = false
        statusText?.setText(R.string.profile_account_email_wait_verified)
        onVerified()

        verifiedJob?.cancel()
        verifiedJob = scope.launch {
            delay(VERIFIED_DISMISS_DELAY_MILLIS)
            val currentDialog = dialog
            if (currentDialog?.isShowing == true) {
                currentDialog.dismiss()
            }
            emailChangeCoordinator.stop()
            onVerificationFinished()
        }
    }

    private fun renderResendAvailability(isAvailable: Boolean) {
        resendButton?.isEnabled = isAvailable
        resendButton?.alpha = if (isAvailable) ENABLED_ALPHA else DISABLED_ALPHA
    }

    private fun dismissCurrentDialog(stopCoordinator: Boolean) {
        verifiedJob?.cancel()
        verifiedJob = null
        if (stopCoordinator) {
            emailChangeCoordinator.stop()
        }
        dialog?.setOnDismissListener(null)
        if (dialog?.isShowing == true) {
            dialog?.dismiss()
        }
        releaseDialogReferences()
    }

    private fun releaseDialogReferences() {
        dialog = null
        statusText = null
        progressView = null
        resendButton = null
        closeButton = null
        pendingEmail = null
    }

    companion object {
        private const val DIALOG_WIDTH_RATIO = 0.90f
        private const val VERIFIED_DISMISS_DELAY_MILLIS = 900L
        private const val ENABLED_ALPHA = 1f
        private const val DISABLED_ALPHA = 0.55f
    }
}
