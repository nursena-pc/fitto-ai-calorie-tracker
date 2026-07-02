package com.example.fitto.feature_home.presentation.profile

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.example.fitto.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputLayout

class ProfileAccountLifecycleDialogCoordinator(
    private val context: Context,
    private val inflater: LayoutInflater
) {
    private var activeDialog: Dialog? = null
    private var finalDeleteDialog: Dialog? = null
    private var finalDeleteConfirmButton: MaterialButton? = null
    private var finalDeleteCancelButton: MaterialButton? = null

    fun showLogoutConfirmation(onConfirmed: () -> Unit) {
        showActionDialog(
            config = ActionDialogConfig(
                titleResId = R.string.profile_detail_settings_logout,
                messageResId = R.string.profile_logout_confirm_message,
                positiveResId = R.string.profile_detail_settings_logout,
                negativeResId = R.string.profile_dialog_cancel,
                iconResId = R.drawable.ic_logout,
                iconColorResId = R.color.profile_subtext,
                confirmTextColorResId = R.color.reminder_delete
            ),
            onConfirmed = { onConfirmed() }
        )
    }

    fun showDeleteIntroduction(onConfirmed: () -> Unit) {
        showActionDialog(
            config = ActionDialogConfig(
                titleResId = R.string.profile_detail_delete_account,
                warningResId = R.string.profile_delete_account_warning,
                messageResId = R.string.profile_delete_account_intro_message,
                positiveResId = R.string.profile_delete_account_continue,
                negativeResId = R.string.profile_delete_account_cancel,
                iconResId = R.drawable.ic_warning,
                iconColorResId = R.color.reminder_delete,
                confirmTextColorResId = R.color.profile_subtext,
                cancelTextColorResId = R.color.fitto_green
            ),
            onConfirmed = { onConfirmed() }
        )
    }

    fun showPasswordReauthentication(onConfirmed: (String) -> Unit) {
        showActionDialog(
            config = ActionDialogConfig(
                titleResId = R.string.profile_delete_account_auth_title,
                messageResId = R.string.profile_delete_account_password_message,
                positiveResId = R.string.profile_delete_account_auth_confirm,
                negativeResId = R.string.profile_delete_account_cancel,
                iconResId = R.drawable.ic_lock,
                iconColorResId = R.color.fitto_green,
                confirmTextColorResId = R.color.fitto_green,
                showPasswordInput = true
            ),
            onConfirmed = { password -> onConfirmed(password.orEmpty()) }
        )
    }

    fun showGoogleReauthentication(onConfirmed: () -> Unit) {
        showActionDialog(
            config = ActionDialogConfig(
                titleResId = R.string.profile_delete_account_auth_title,
                messageResId = R.string.profile_delete_account_google_message,
                positiveResId = R.string.profile_delete_account_google_confirm,
                negativeResId = R.string.profile_delete_account_cancel,
                iconResId = R.drawable.ic_google_g,
                iconColorResId = R.color.fitto_green,
                confirmTextColorResId = R.color.fitto_green
            ),
            onConfirmed = { onConfirmed() }
        )
    }

    fun showFinalDeleteConfirmation(onConfirmed: () -> Unit) {
        dismissActiveDialog()

        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val dialogView = inflater.inflate(R.layout.dialog_fitto_action, null, false)

        val iconImage = dialogView.findViewById<ImageView>(R.id.imageActionIcon)
        val titleText = dialogView.findViewById<TextView>(R.id.textActionTitle)
        val warningText = dialogView.findViewById<TextView>(R.id.textActionWarning)
        val messageText = dialogView.findViewById<TextView>(R.id.textActionMessage)
        val passwordArea = dialogView.findViewById<LinearLayout>(R.id.layoutPasswordArea)
        val cancelButton = dialogView.findViewById<MaterialButton>(R.id.buttonActionCancel)
        val confirmButton = dialogView.findViewById<MaterialButton>(R.id.buttonActionConfirm)

        titleText.setText(R.string.profile_delete_account_final_title)
        messageText.setText(R.string.profile_delete_account_final_message)
        warningText.isVisible = false
        passwordArea.isVisible = false
        cancelButton.setText(R.string.profile_delete_account_cancel)
        confirmButton.setText(R.string.profile_delete_account_confirm)
        iconImage.setImageResource(R.drawable.ic_warning)
        iconImage.setColorFilter(context.getColor(R.color.reminder_delete))
        cancelButton.setTextColor(context.getColor(R.color.profile_subtext))
        confirmButton.setTextColor(context.getColor(R.color.reminder_delete))

        cancelButton.setOnClickListener { dialog.dismiss() }
        confirmButton.setOnClickListener {
            showDeleteInProgress()
            onConfirmed()
        }

        dialog.setOnDismissListener {
            if (finalDeleteDialog === dialog) {
                finalDeleteDialog = null
                finalDeleteConfirmButton = null
                finalDeleteCancelButton = null
            }
            if (activeDialog === dialog) activeDialog = null
        }

        dialog.setContentView(dialogView)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCanceledOnTouchOutside(false)
        dialog.show()
        dialog.window?.setLayout(
            (context.resources.displayMetrics.widthPixels * DIALOG_WIDTH_RATIO).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        activeDialog = dialog
        finalDeleteDialog = dialog
        finalDeleteConfirmButton = confirmButton
        finalDeleteCancelButton = cancelButton
    }

    fun showDeleteInProgress() {
        finalDeleteConfirmButton?.apply {
            isEnabled = false
            setText(R.string.profile_delete_account_deleting)
        }
        finalDeleteCancelButton?.isEnabled = false
    }

    fun restoreDeleteConfirmation() {
        finalDeleteConfirmButton?.apply {
            isEnabled = true
            setText(R.string.profile_delete_account_confirm)
        }
        finalDeleteCancelButton?.isEnabled = true
    }

    fun dismissDeleteConfirmation() {
        finalDeleteDialog?.dismiss()
    }

    fun clear() {
        dismissActiveDialog()
        finalDeleteDialog = null
        finalDeleteConfirmButton = null
        finalDeleteCancelButton = null
    }

    private fun showActionDialog(
        config: ActionDialogConfig,
        onConfirmed: (String?) -> Unit
    ) {
        dismissActiveDialog()

        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val dialogView = inflater.inflate(R.layout.dialog_fitto_action, null, false)

        val iconImage = dialogView.findViewById<ImageView>(R.id.imageActionIcon)
        val titleText = dialogView.findViewById<TextView>(R.id.textActionTitle)
        val warningText = dialogView.findViewById<TextView>(R.id.textActionWarning)
        val messageText = dialogView.findViewById<TextView>(R.id.textActionMessage)
        val passwordArea = dialogView.findViewById<LinearLayout>(R.id.layoutPasswordArea)
        val passwordLabel = dialogView.findViewById<TextView>(R.id.textPasswordLabel)
        val passwordInputLayout = dialogView.findViewById<TextInputLayout>(R.id.layoutPasswordInput)
        val passwordEditText = dialogView.findViewById<EditText>(R.id.editActionPassword)
        val cancelButton = dialogView.findViewById<MaterialButton>(R.id.buttonActionCancel)
        val confirmButton = dialogView.findViewById<MaterialButton>(R.id.buttonActionConfirm)

        titleText.setText(config.titleResId)
        messageText.setText(config.messageResId)
        cancelButton.setText(config.negativeResId)
        confirmButton.setText(config.positiveResId)
        warningText.isVisible = config.warningResId != null
        config.warningResId?.let(warningText::setText)
        passwordArea.isVisible = config.showPasswordInput
        passwordLabel.setText(R.string.profile_delete_account_password_label)

        if (config.showPasswordInput) {
            passwordInputLayout.hint = context.getString(R.string.profile_delete_account_password_hint)
            configurePasswordInput(passwordInputLayout, passwordEditText)
        } else {
            passwordInputLayout.endIconMode = TextInputLayout.END_ICON_NONE
        }

        iconImage.setImageResource(config.iconResId)
        iconImage.setColorFilter(context.getColor(config.iconColorResId))
        cancelButton.setTextColor(context.getColor(config.cancelTextColorResId))
        confirmButton.setTextColor(context.getColor(config.confirmTextColorResId))

        cancelButton.setOnClickListener { dialog.dismiss() }
        confirmButton.setOnClickListener {
            val password = passwordEditText.text?.toString().orEmpty()
            if (config.showPasswordInput && password.isBlank()) {
                passwordInputLayout.error = context.getString(
                    R.string.profile_delete_account_empty_password
                )
                return@setOnClickListener
            }

            passwordInputLayout.error = null
            dialog.dismiss()
            onConfirmed(password.takeIf { config.showPasswordInput })
        }

        dialog.setOnDismissListener {
            if (activeDialog === dialog) activeDialog = null
        }
        dialog.setContentView(dialogView)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
        dialog.window?.setLayout(
            (context.resources.displayMetrics.widthPixels * DIALOG_WIDTH_RATIO).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        activeDialog = dialog
    }

    private fun configurePasswordInput(
        inputLayout: TextInputLayout,
        editText: EditText
    ) {
        editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        editText.transformationMethod = PasswordTransformationMethod.getInstance()
        inputLayout.endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
    }

    private fun dismissActiveDialog() {
        activeDialog?.dismiss()
        activeDialog = null
    }

    private data class ActionDialogConfig(
        val titleResId: Int,
        val warningResId: Int? = null,
        val messageResId: Int,
        val positiveResId: Int,
        val negativeResId: Int,
        val iconResId: Int,
        val iconColorResId: Int,
        val confirmTextColorResId: Int = R.color.profile_subtext,
        val cancelTextColorResId: Int = R.color.profile_subtext,
        val showPasswordInput: Boolean = false
    )

    private companion object {
        const val DIALOG_WIDTH_RATIO = 0.90f
    }
}
