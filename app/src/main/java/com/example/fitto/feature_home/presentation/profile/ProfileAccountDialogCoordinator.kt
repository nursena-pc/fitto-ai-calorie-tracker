package com.example.fitto.feature_home.presentation.profile

import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.Window
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import com.example.fitto.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class ProfileAccountDialogCoordinator(
    private val context: Context,
    private val inflater: LayoutInflater
) {
    private var activeDialog: Dialog? = null

    fun showDisplayName(
        initialValue: String,
        onConfirmed: (String) -> Unit
    ) {
        showSingleInput(
            titleResId = R.string.profile_account_update_display_name_title,
            messageResId = R.string.profile_account_update_display_name_message,
            labelResId = R.string.profile_account_display_name_label,
            hintResId = R.string.profile_account_display_name_hint,
            positiveResId = R.string.profile_account_save_changes,
            initialValue = initialValue,
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS,
            iconResId = R.drawable.ic_edit,
            iconColorResId = R.color.fitto_green,
            onConfirmed = onConfirmed
        )
    }

    fun showUsername(
        initialValue: String,
        onConfirmed: (String) -> Unit
    ) {
        showSingleInput(
            titleResId = R.string.profile_account_update_username_title,
            messageResId = R.string.profile_account_update_username_message,
            labelResId = R.string.profile_account_username_label,
            hintResId = R.string.profile_account_username_hint,
            positiveResId = R.string.profile_account_save_changes,
            initialValue = initialValue,
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_NORMAL,
            iconResId = R.drawable.ic_person_outline,
            iconColorResId = R.color.fitto_green,
            onConfirmed = onConfirmed
        )
    }

    fun showEmail(
        initialValue: String,
        onConfirmed: (String) -> Unit
    ) {
        showSingleInput(
            titleResId = R.string.profile_account_update_email_title,
            messageResId = R.string.profile_account_update_email_message,
            labelResId = R.string.profile_account_email_label,
            hintResId = R.string.profile_account_email_hint,
            positiveResId = R.string.profile_account_continue,
            initialValue = initialValue,
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
            iconResId = R.drawable.ic_email,
            iconColorResId = R.color.fitto_green,
            onConfirmed = onConfirmed
        )
    }

    fun showEmailPassword(
        onConfirmed: (String) -> Unit
    ) {
        showSingleInput(
            titleResId = R.string.profile_account_verify_password_title,
            messageResId = R.string.profile_account_verify_password_message,
            labelResId = R.string.profile_delete_account_password_label,
            hintResId = R.string.profile_delete_account_password_hint,
            positiveResId = R.string.profile_delete_account_auth_confirm,
            initialValue = "",
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD,
            iconResId = R.drawable.ic_lock,
            iconColorResId = R.color.fitto_green,
            isPassword = true,
            onConfirmed = onConfirmed
        )
    }

    fun showPasswordChange(
        onConfirmed: (currentPassword: String, newPassword: String, confirmPassword: String) -> Unit
    ) {
        dismissActiveDialog()

        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val density = context.resources.displayMetrics.density
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                (22 * density).toInt(),
                (22 * density).toInt(),
                (22 * density).toInt(),
                (14 * density).toInt()
            )
            background = context.getDrawable(R.drawable.bg_profile_card)
        }

        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val icon = ImageView(context).apply {
            setImageResource(R.drawable.ic_lock)
            setColorFilter(context.getColor(R.color.fitto_green))
        }

        header.addView(
            icon,
            LinearLayout.LayoutParams(
                (20 * density).toInt(),
                (20 * density).toInt()
            ).apply {
                marginEnd = (10 * density).toInt()
            }
        )

        val title = TextView(context).apply {
            setText(R.string.profile_account_change_password_title)
            setTextColor(context.getColor(R.color.profile_text))
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            includeFontPadding = false
        }

        header.addView(
            title,
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        )
        container.addView(header)

        val message = TextView(context).apply {
            setText(R.string.profile_account_change_password_message)
            setTextColor(context.getColor(R.color.profile_subtext))
            textSize = 14f
            setLineSpacing(3f, 1f)
        }

        container.addView(
            message,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (14 * density).toInt()
            }
        )

        fun addPasswordField(
            @StringRes labelResId: Int,
            @StringRes hintResId: Int
        ): Pair<TextInputLayout, EditText> {
            val label = TextView(context).apply {
                setText(labelResId)
                setTextColor(context.getColor(R.color.profile_text))
                textSize = 13f
                setTypeface(null, Typeface.BOLD)
                includeFontPadding = false
            }

            container.addView(
                label,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = (16 * density).toInt()
                }
            )

            val inputLayout = TextInputLayout(context).apply {
                hint = context.getString(hintResId)
                boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
                setBoxCornerRadii(16f, 16f, 16f, 16f)
                endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
            }

            val editText = TextInputEditText(context).apply {
                maxLines = 1
                isSingleLine = true
                textSize = 15f
                setTextColor(context.getColor(R.color.profile_text))
            }

            configurePasswordInput(inputLayout, editText)
            applySoftInputHintStyle(inputLayout, editText)
            inputLayout.addView(editText)

            container.addView(
                inputLayout,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = (8 * density).toInt()
                }
            )

            return inputLayout to editText
        }

        val (currentPasswordLayout, currentPasswordEditText) = addPasswordField(
            R.string.profile_account_current_password_label,
            R.string.profile_account_current_password_hint
        )
        val (newPasswordLayout, newPasswordEditText) = addPasswordField(
            R.string.profile_account_new_password_label,
            R.string.profile_account_new_password_hint
        )
        val (confirmPasswordLayout, confirmPasswordEditText) = addPasswordField(
            R.string.profile_account_confirm_password_label,
            R.string.profile_account_confirm_password_hint
        )

        val buttons = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
        }
        val cancelButton = transparentButton(
            textResId = R.string.profile_delete_account_cancel,
            textColorResId = R.color.profile_subtext
        )
        val confirmButton = transparentButton(
            textResId = R.string.profile_account_save_changes,
            textColorResId = R.color.fitto_green
        )

        buttons.addView(cancelButton)
        buttons.addView(confirmButton)
        container.addView(
            buttons,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (22 * density).toInt()
            }
        )

        cancelButton.setOnClickListener { dialog.dismiss() }
        confirmButton.setOnClickListener {
            currentPasswordLayout.error = null
            newPasswordLayout.error = null
            confirmPasswordLayout.error = null

            val currentPassword = currentPasswordEditText.text?.toString().orEmpty()
            val newPassword = newPasswordEditText.text?.toString().orEmpty()
            val confirmPassword = confirmPasswordEditText.text?.toString().orEmpty()

            when {
                currentPassword.isBlank() -> {
                    currentPasswordLayout.error = context.getString(R.string.profile_account_field_required)
                }

                newPassword.isBlank() -> {
                    newPasswordLayout.error = context.getString(R.string.profile_account_field_required)
                }

                confirmPassword.isBlank() -> {
                    confirmPasswordLayout.error = context.getString(R.string.profile_account_field_required)
                }

                else -> {
                    dialog.dismiss()
                    onConfirmed(currentPassword, newPassword, confirmPassword)
                }
            }
        }

        showDialog(dialog, container)
    }

    fun showGoogleManagedAccount() {
        showInfo(
            titleResId = R.string.profile_google_managed_account_title,
            messageResId = R.string.profile_google_managed_account_message,
            iconResId = R.drawable.ic_google_g,
            iconColorResId = R.color.fitto_green
        )
    }

    fun clear() {
        dismissActiveDialog()
    }

    private fun showSingleInput(
        @StringRes titleResId: Int,
        @StringRes messageResId: Int,
        @StringRes labelResId: Int,
        @StringRes hintResId: Int,
        @StringRes positiveResId: Int,
        initialValue: String,
        inputType: Int,
        iconResId: Int,
        iconColorResId: Int,
        isPassword: Boolean = false,
        onConfirmed: (String) -> Unit
    ) {
        dismissActiveDialog()
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val dialogView = inflater.inflate(R.layout.dialog_fitto_action, null, false)

        val iconImage = dialogView.findViewById<ImageView>(R.id.imageActionIcon)
        val titleText = dialogView.findViewById<TextView>(R.id.textActionTitle)
        val warningText = dialogView.findViewById<TextView>(R.id.textActionWarning)
        val messageText = dialogView.findViewById<TextView>(R.id.textActionMessage)
        val inputArea = dialogView.findViewById<LinearLayout>(R.id.layoutPasswordArea)
        val inputLabel = dialogView.findViewById<TextView>(R.id.textPasswordLabel)
        val inputLayout = dialogView.findViewById<TextInputLayout>(R.id.layoutPasswordInput)
        val inputEditText = dialogView.findViewById<EditText>(R.id.editActionPassword)
        val cancelButton = dialogView.findViewById<MaterialButton>(R.id.buttonActionCancel)
        val confirmButton = dialogView.findViewById<MaterialButton>(R.id.buttonActionConfirm)

        titleText.setText(titleResId)
        messageText.setText(messageResId)
        warningText.isVisible = false
        inputArea.isVisible = true
        inputLabel.setText(labelResId)
        inputLayout.hint = context.getString(hintResId)

        if (isPassword) {
            configurePasswordInput(inputLayout, inputEditText)
        } else {
            inputLayout.endIconMode = TextInputLayout.END_ICON_NONE
            inputEditText.inputType = inputType
        }

        applySoftInputHintStyle(inputLayout, inputEditText)
        inputEditText.setText(initialValue)
        inputEditText.setSelection(inputEditText.text?.length ?: 0)

        cancelButton.setText(R.string.profile_delete_account_cancel)
        confirmButton.setText(positiveResId)
        iconImage.setImageResource(iconResId)
        iconImage.setColorFilter(context.getColor(iconColorResId))
        cancelButton.setTextColor(context.getColor(R.color.profile_subtext))
        confirmButton.setTextColor(context.getColor(R.color.fitto_green))

        cancelButton.setOnClickListener { dialog.dismiss() }
        confirmButton.setOnClickListener {
            val value = inputEditText.text?.toString().orEmpty()
            if (value.isBlank()) {
                inputLayout.error = context.getString(R.string.profile_account_field_required)
                return@setOnClickListener
            }
            inputLayout.error = null
            dialog.dismiss()
            onConfirmed(value)
        }

        showDialog(dialog, dialogView)
    }

    private fun showInfo(
        @StringRes titleResId: Int,
        @StringRes messageResId: Int,
        iconResId: Int,
        iconColorResId: Int
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
        val cancelButton = dialogView.findViewById<MaterialButton>(R.id.buttonActionCancel)
        val confirmButton = dialogView.findViewById<MaterialButton>(R.id.buttonActionConfirm)

        titleText.setText(titleResId)
        messageText.setText(messageResId)
        warningText.isVisible = false
        passwordArea.isVisible = false
        cancelButton.isVisible = false
        confirmButton.setText(R.string.profile_dialog_understood)
        confirmButton.setTextColor(context.getColor(R.color.fitto_green))
        iconImage.setImageResource(iconResId)
        iconImage.setColorFilter(context.getColor(iconColorResId))
        confirmButton.setOnClickListener { dialog.dismiss() }

        showDialog(dialog, dialogView)
    }

    private fun showDialog(
        dialog: Dialog,
        contentView: android.view.View
    ) {
        activeDialog = dialog
        dialog.setContentView(contentView)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setOnDismissListener {
            if (activeDialog === dialog) {
                activeDialog = null
            }
        }
        dialog.show()
        dialog.window?.setLayout(
            (context.resources.displayMetrics.widthPixels * DIALOG_WIDTH_RATIO).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun transparentButton(
        @StringRes textResId: Int,
        @ColorRes textColorResId: Int
    ): MaterialButton {
        return MaterialButton(context).apply {
            setText(textResId)
            setTextColor(context.getColor(textColorResId))
            backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
            elevation = 0f
            isAllCaps = false
        }
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

    private fun dismissActiveDialog() {
        activeDialog?.setOnDismissListener(null)
        if (activeDialog?.isShowing == true) {
            activeDialog?.dismiss()
        }
        activeDialog = null
    }

    companion object {
        private const val DIALOG_WIDTH_RATIO = 0.90f
        private const val HINT_ALPHA = 0.58f
    }
}
