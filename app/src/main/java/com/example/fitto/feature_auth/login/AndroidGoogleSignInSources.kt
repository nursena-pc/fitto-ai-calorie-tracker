package com.example.fitto.feature_auth.login

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.NoCredentialException
import com.example.fitto.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

class AndroidGoogleCredentialSource(
    private val context: Context,
    private val credentialManager: CredentialManager = CredentialManager.create(context)
) : GoogleCredentialSource {

    override suspend fun requestIdToken(
        filterByAuthorizedAccounts: Boolean
    ): GoogleIdToken {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(filterByAuthorizedAccounts)
            .setServerClientId(context.getString(R.string.default_web_client_id))
            .setAutoSelectEnabled(false)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val result = try {
            credentialManager.getCredential(
                context = context,
                request = request
            )
        } catch (exception: NoCredentialException) {
            throw GoogleCredentialUnavailableException(exception)
        }
        val credential = result.credential

        if (
            credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            try {
                val tokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                return GoogleIdToken(tokenCredential.idToken)
            } catch (exception: GoogleIdTokenParsingException) {
                throw IllegalStateException(
                    context.getString(R.string.google_error_parse),
                    exception
                )
            }
        }

        throw IllegalStateException(
            context.getString(R.string.google_error_unexpected_response)
        )
    }
}

class FirebaseGoogleAuthenticationSource(
    private val context: Context,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : GoogleAuthenticationSource {

    override suspend fun signIn(idToken: GoogleIdToken) {
        val credential = GoogleAuthProvider.getCredential(idToken.value, null)
        auth.signInWithCredential(credential).await()
    }

    override suspend fun reauthenticateCurrentUser(idToken: GoogleIdToken) {
        val currentUser = auth.currentUser
            ?: throw IllegalStateException(
                context.getString(R.string.profile_delete_account_error)
            )
        val credential = GoogleAuthProvider.getCredential(idToken.value, null)
        currentUser.reauthenticate(credential).await()
    }
}
