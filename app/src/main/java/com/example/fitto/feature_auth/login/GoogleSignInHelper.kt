package com.example.fitto.feature_auth.login

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.fitto.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

object GoogleSignInHelper {

    suspend fun signIn(
        context: Context,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val result = requestGoogleCredential(
                context = context,
                filterByAuthorizedAccounts = true
            )

            val credential = firebaseCredentialFromGoogleResult(
                context = context,
                result = result
            )

            FirebaseAuth.getInstance()
                .signInWithCredential(credential)
                .await()

            Log.d(TAG, "Google sign-in succeeded")
            onSuccess()
        } catch (exception: NoCredentialException) {
            Log.w(TAG, "No authorized Google credential found. Retrying with all accounts.", exception)

            try {
                val result = requestGoogleCredential(
                    context = context,
                    filterByAuthorizedAccounts = false
                )

                val credential = firebaseCredentialFromGoogleResult(
                    context = context,
                    result = result
                )

                FirebaseAuth.getInstance()
                    .signInWithCredential(credential)
                    .await()

                Log.d(TAG, "Google sign-in succeeded after fallback")
                onSuccess()
            } catch (fallbackException: Exception) {
                Log.e(TAG, "Google sign-in fallback failed", fallbackException)
                onError(
                    fallbackException.localizedMessage
                        ?: context.getString(R.string.google_error_general)
                )
            }
        } catch (exception: GetCredentialException) {
            Log.e(TAG, "Google credential request failed", exception)
            onError(
                exception.localizedMessage
                    ?: context.getString(R.string.google_error_general)
            )
        } catch (exception: Exception) {
            Log.e(TAG, "Google sign-in failed", exception)
            onError(
                exception.localizedMessage
                    ?: context.getString(R.string.google_error_general)
            )
        }
    }

    suspend fun reauthenticateCurrentUser(context: Context) {
        val currentUser = FirebaseAuth.getInstance().currentUser
            ?: throw IllegalStateException(context.getString(R.string.profile_delete_account_error))

        try {
            val result = requestGoogleCredential(
                context = context,
                filterByAuthorizedAccounts = true
            )

            val credential = firebaseCredentialFromGoogleResult(
                context = context,
                result = result
            )

            currentUser.reauthenticate(credential).await()
            Log.d(TAG, "Google reauthentication succeeded")
        } catch (exception: NoCredentialException) {
            Log.w(TAG, "No authorized Google credential found for reauth. Retrying with all accounts.", exception)

            val result = requestGoogleCredential(
                context = context,
                filterByAuthorizedAccounts = false
            )

            val credential = firebaseCredentialFromGoogleResult(
                context = context,
                result = result
            )

            currentUser.reauthenticate(credential).await()
            Log.d(TAG, "Google reauthentication succeeded after fallback")
        }
    }

    private suspend fun requestGoogleCredential(
        context: Context,
        filterByAuthorizedAccounts: Boolean
    ): GetCredentialResponse {
        val credentialManager = CredentialManager.create(context)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(filterByAuthorizedAccounts)
            .setServerClientId(context.getString(R.string.default_web_client_id))
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return credentialManager.getCredential(
            context = context,
            request = request
        )
    }

    private fun firebaseCredentialFromGoogleResult(
        context: Context,
        result: GetCredentialResponse
    ): AuthCredential {
        val credential = result.credential

        if (
            credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            try {
                val googleIdTokenCredential = GoogleIdTokenCredential
                    .createFrom(credential.data)

                return GoogleAuthProvider.getCredential(
                    googleIdTokenCredential.idToken,
                    null
                )
            } catch (exception: GoogleIdTokenParsingException) {
                Log.e(TAG, "Google ID token parse failed", exception)
                throw IllegalStateException(
                    context.getString(R.string.google_error_parse),
                    exception
                )
            }
        }

        throw IllegalStateException(context.getString(R.string.google_error_unexpected_response))
    }

    private const val TAG = "GoogleSignInHelper"
}