package com.example.fitto.feature_auth.login

import android.content.Context
import android.util.Log
import com.example.fitto.R

object GoogleSignInHelper {

    suspend fun signIn(
        context: Context,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        runCatching {
            coordinator(context).signIn()
        }.onSuccess {
            Log.d(TAG, "Google sign-in succeeded")
            onSuccess()
        }.onFailure { exception ->
            Log.e(TAG, "Google sign-in failed", exception)
            onError(
                exception.localizedMessage
                    ?: context.getString(R.string.google_error_general)
            )
        }
    }

    suspend fun reauthenticateCurrentUser(context: Context) {
        coordinator(context).reauthenticateCurrentUser()
        Log.d(TAG, "Google reauthentication succeeded")
    }

    private fun coordinator(context: Context): GoogleSignInCoordinator {
        return GoogleSignInCoordinator(
            credentialSource = AndroidGoogleCredentialSource(context),
            authenticationSource = FirebaseGoogleAuthenticationSource(context)
        )
    }

    private const val TAG = "GoogleSignInHelper"
}
