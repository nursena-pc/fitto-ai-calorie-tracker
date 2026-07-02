package com.example.fitto.feature_home.presentation.profile

import androidx.annotation.StringRes
import com.example.fitto.R
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException

object ProfileAccountErrorResolver {
    @StringRes
    fun resolve(error: Throwable): Int {
        return when (error) {
            is AccountSettingsException -> resolve(error.error)
            is FirebaseAuthInvalidCredentialsException -> R.string.profile_account_error_wrong_password
            is FirebaseAuthWeakPasswordException -> R.string.profile_account_error_password_too_short
            is FirebaseAuthUserCollisionException -> R.string.profile_account_error_email_in_use
            is FirebaseAuthRecentLoginRequiredException -> R.string.profile_delete_account_requires_recent_login
            else -> R.string.profile_account_error_unknown
        }
    }

    @StringRes
    fun resolve(error: AccountSettingsError): Int {
        return when (error) {
            AccountSettingsError.NOT_SIGNED_IN -> R.string.profile_delete_account_error
            AccountSettingsError.EMAIL_MISSING -> R.string.profile_account_error_email_missing
            AccountSettingsError.DISPLAY_NAME_TOO_SHORT -> R.string.profile_account_error_display_name_too_short
            AccountSettingsError.DISPLAY_NAME_TOO_LONG -> R.string.profile_account_error_display_name_too_long
            AccountSettingsError.USERNAME_TOO_SHORT -> R.string.profile_account_error_username_too_short
            AccountSettingsError.USERNAME_TOO_LONG -> R.string.profile_account_error_username_too_long
            AccountSettingsError.USERNAME_INVALID -> R.string.profile_account_error_username_invalid
            AccountSettingsError.USERNAME_TAKEN -> R.string.profile_account_error_username_taken
            AccountSettingsError.EMAIL_INVALID -> R.string.profile_account_error_email_invalid
            AccountSettingsError.EMAIL_SAME_AS_CURRENT -> R.string.profile_account_email_same_error
            AccountSettingsError.EMAIL_VERIFICATION_SENT -> R.string.profile_account_email_verification_sent_message
            AccountSettingsError.CURRENT_PASSWORD_EMPTY -> R.string.profile_account_field_required
            AccountSettingsError.WRONG_PASSWORD -> R.string.profile_account_error_wrong_password
            AccountSettingsError.NEW_PASSWORD_TOO_SHORT -> R.string.profile_account_error_password_too_short
            AccountSettingsError.PASSWORD_CONFIRM_MISMATCH -> R.string.profile_account_error_password_mismatch
            AccountSettingsError.REQUIRES_RECENT_LOGIN -> R.string.profile_delete_account_requires_recent_login
            AccountSettingsError.PROVIDER_NOT_SUPPORTED -> R.string.profile_account_provider_not_supported
            AccountSettingsError.UNKNOWN -> R.string.profile_account_error_unknown
        }
    }
}
