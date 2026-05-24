package com.example.fitto.feature_auth.login

import android.content.Context
import com.example.fitto.R
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

object FirebaseAuthHelper {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    fun loginWithEmailOrUsername(
        context: Context,
        emailOrUsername: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val value = emailOrUsername.trim()

        if (value.isBlank()) {
            onError(context.getString(R.string.auth_error_email_or_username_required))
            return
        }

        if (value.contains("@")) {
            loginWithEmail(
                context = context,
                email = value,
                password = password,
                onSuccess = onSuccess,
                onError = onError
            )
        } else {
            loginWithUsername(
                context = context,
                username = value,
                password = password,
                onSuccess = onSuccess,
                onError = onError
            )
        }
    }

    fun sendResetPasswordEmail(
        context: Context,
        email: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        auth.sendPasswordResetEmail(email.trim())
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onSuccess()
                } else {
                    onError(getFriendlyResetPasswordErrorMessage(context, task.exception))
                }
            }
    }

    private fun loginWithUsername(
        context: Context,
        username: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val normalizedUsername = normalizeUsername(username)

        if (normalizedUsername.length < 3) {
            onError(context.getString(R.string.auth_error_username_min))
            return
        }

        firestore.collection("usernames")
            .document(normalizedUsername)
            .get()
            .addOnSuccessListener { usernameDoc ->
                if (!usernameDoc.exists()) {
                    onError(context.getString(R.string.auth_error_username_not_found))
                    return@addOnSuccessListener
                }

                val email = usernameDoc.getString("email").orEmpty()

                if (email.isBlank()) {
                    onError(context.getString(R.string.auth_error_username_profile_missing))
                    return@addOnSuccessListener
                }

                loginWithEmail(
                    context = context,
                    email = email,
                    password = password,
                    onSuccess = onSuccess,
                    onError = onError
                )
            }
            .addOnFailureListener { exception ->
                onError(getFriendlyUsernameLookupErrorMessage(context, exception))
            }
    }

    private fun loginWithEmail(
        context: Context,
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email.trim(), password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onSuccess()
                } else {
                    onError(getFriendlyLoginErrorMessage(context, task.exception))
                }
            }
    }

    private fun normalizeUsername(value: String): String {
        return value
            .removePrefix("@")
            .trim()
            .lowercase(Locale.ROOT)
            .replace("ğ", "g")
            .replace("ü", "u")
            .replace("ş", "s")
            .replace("ı", "i")
            .replace("ö", "o")
            .replace("ç", "c")
            .replace(Regex("\\s+"), ".")
            .replace(Regex("[^a-z0-9._]"), "")
            .replace(Regex("[.]{2,}"), ".")
            .replace(Regex("[_]{2,}"), "_")
            .trim()
    }

    private fun getFriendlyLoginErrorMessage(
        context: Context,
        exception: Exception?
    ): String {
        return when (exception) {
            is FirebaseAuthInvalidCredentialsException -> {
                context.getString(R.string.auth_error_invalid_login_credentials)
            }

            is FirebaseAuthInvalidUserException -> {
                context.getString(R.string.auth_error_account_not_found)
            }

            is FirebaseNetworkException -> {
                context.getString(R.string.auth_error_network)
            }

            else -> {
                context.getString(R.string.auth_error_login_unknown)
            }
        }
    }

    private fun getFriendlyResetPasswordErrorMessage(
        context: Context,
        exception: Exception?
    ): String {
        return when (exception) {
            is FirebaseAuthInvalidUserException -> {
                context.getString(R.string.auth_error_reset_account_not_found)
            }

            is FirebaseAuthInvalidCredentialsException -> {
                context.getString(R.string.auth_error_reset_invalid_email)
            }

            is FirebaseNetworkException -> {
                context.getString(R.string.auth_error_network)
            }

            else -> {
                context.getString(R.string.auth_error_reset_unknown)
            }
        }
    }

    private fun getFriendlyUsernameLookupErrorMessage(
        context: Context,
        exception: Exception?
    ): String {
        return when (exception) {
            is FirebaseNetworkException -> {
                context.getString(R.string.auth_error_network)
            }

            else -> {
                context.getString(R.string.auth_error_username_lookup_unknown)
            }
        }
    }
}