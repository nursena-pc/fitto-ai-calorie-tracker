package com.example.fitto.feature_home.presentation.profile

import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.Window
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import com.example.fitto.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputLayout

internal class ProfileActionDialogCoordinator(
    private val context: Context,
    private val inflater: LayoutInflater
) {

    private var activeDialog: Dialog? = null

    fun showConfirmation(
        @StringRes titleResId: Int,
        @StringRes messageResId: Int,
        @StringRes positiveResId: Int,
        @DrawableRes iconResId: Int,
        @ColorRes iconColorResId: Int = R.color.profile_subtext,
        @ColorRes confirmTextColorResId: Int = R.color.profile_subtext,
        onConfirmed: () -> Unit
    ) {
        show(
            titleResId = titleResId,
            warningResId = null,
            messageResId = messageResId,
            positiveResId = positiveResId,
            negativeResId = R.string.profile_dialog_cancel,
            iconResId = iconResId,
            iconColorResId = iconColorResId,
            confirmTextColorResId = confirmTextColorResId,
            showPasswordInput = false,
            onConfirmed = { onConfirmed() }
        )
    }

    fun show(
        @StringRes titleResId: Int,
        @StringRes warningResId: Int?,
        @StringRes messageResId: Int,
        @StringRes positiveResId: Int,
        @StringRes negativeResId: Int,
        @DrawableRes iconResId: Int,
        @ColorRes iconColorResId: Int,
        @ColorRes confirmTextColorResId: Int = R.color.profile_subtext,
        @ColorRes cancelTextColorResId: Int = R.color.profile_subtext,
        showPasswordInput: Boolean,
        onConfirmed: (String?) -> Unit
    ) {
        activeDialog?.dismiss()

        val dialog = Dialog(context)
        activeDialog = dialog
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setOnDismissListener {
            if (activeDialog === dialog) activeDialog = null
        }

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

        titleText.setText(titleResId)
        messageText.setText(messageResId)
        cancelButton.setText(negativeResId)
        confirmButton.setText(positiveResId)
        warningText.isVisible = warningResId != null
        warningResId?.let { warningText.setText(it) }
        passwordArea.isVisible = showPasswordInput
        passwordLabel.setText(R.string.profile_delete_account_password_label)

        if (showPasswordInput) {
            passwordInputLayout.hint = context.getString(R.string.profile_delete_account_password_hint)
            configurePasswordInput(passwordInputLayout, passwordEditText)
            applySoftInputHintStyle(passwordInputLayout, passwordEditText)
        } else {
            passwordInputLayout.endIconMode = TextInputLayout.END_ICON_NONE
        }

        iconImage.setImageResource(iconResId)
        iconImage.setColorFilter(context.getColor(iconColorResId))
        cancelButton.setTextColor(context.getColor(cancelTextColorResId))
        confirmButton.setTextColor(context.getColor(confirmTextColorResId))
        cancelButton.setOnClickListener { dialog.dismiss() }
        confirmButton.setOnClickListener {
            val password = passwordEditText.text?.toString().orEmpty()
            if (showPasswordInput && password.isBlank()) {
                passwordInputLayout.error = context.getString(
                    R.string.profile_delete_account_empty_password
                )
                return@setOnClickListener
            }

            passwordInputLayout.error = null
            dialog.dismiss()
            onConfirmed(password.takeIf { showPasswordInput })
        }

        dialog.setContentView(dialogView)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
        dialog.window?.setLayout(
            (context.resources.displayMetrics.widthPixels * DIALOG_WIDTH_RATIO).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    fun clear() {
        activeDialog?.dismiss()
        activeDialog = null
    }

    private fun configurePasswordInput(
        inputLayout: TextInputLayout,
        editText: EditText
    ) {
        inputLayout.endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
        editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        editText.transformationMethod = PasswordTransformationMethod.getInstance()
        editText.typeface = Typeface.DEFAULT
        editText.maxLines = 1
        editText.isSingleLine = true
    }

    private fun applySoftInputHintStyle(
        inputLayout: TextInputLayout,
        editText: EditText
    ) {
        val color = context.getColor(R.color.profile_subtext)
        val hintColor = Color.argb(
            (Color.alpha(color) * HINT_ALPHA).toInt().coerceIn(0, 255),
            Color.red(color),
            Color.green(color),
            Color.blue(color)
        )
        inputLayout.defaultHintTextColor = ColorStateList.valueOf(hintColor)
        editText.setHintTextColor(hintColor)
    }

    private companion object {
        const val DIALOG_WIDTH_RATIO = 0.90f
        const val HINT_ALPHA = 0.58f
    }
}
