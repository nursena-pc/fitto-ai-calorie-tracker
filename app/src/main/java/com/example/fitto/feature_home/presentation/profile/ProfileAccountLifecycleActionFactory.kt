package com.example.fitto.feature_home.presentation.profile

import android.content.Context
import com.example.fitto.feature_auth.login.GoogleSignInHelper
import com.example.fitto.feature_home.presentation.notifications.FittoNotificationStore
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

class ProfileAccountLifecycleActionFactory(
    private val context: Context
) {

    fun create(): ProfileAccountLifecycleAction {
        val appContext = context.applicationContext
        val auth = FirebaseAuth.getInstance()
        val deletionRepository = AccountDeletionRepository()

        return ProfileAccountLifecycleAction(
            currentSession = {
                auth.currentUser?.let { user ->
                    val providerIds = user.providerData
                        .map { providerInfo -> providerInfo.providerId }
                        .toSet()

                    ProfileAccountSession(
                        email = user.email,
                        hasPasswordProvider = EmailAuthProvider.PROVIDER_ID in providerIds,
                        hasGoogleProvider = GoogleAuthProvider.PROVIDER_ID in providerIds
                    )
                }
            },
            reauthenticateWithPassword = { email, password ->
                val currentUser = auth.currentUser
                    ?: throw IllegalStateException("AUTH_USER_NOT_FOUND")
                val credential = EmailAuthProvider.getCredential(email, password)
                currentUser.reauthenticate(credential).await()
            },
            reauthenticateWithGoogle = {
                GoogleSignInHelper.reauthenticateCurrentUser(context)
            },
            deleteCurrentAccount = deletionRepository::deleteCurrentAccount,
            clearAccountScopedPreferences = {
                ProfilePreferenceStore.clearAccountScopedPreferences(appContext)
            },
            clearReminderPreferences = {
                ReminderPreferenceStore.clear(appContext)
            },
            cancelActiveNotifications = {
                FittoNotificationStore.cancelAllActiveSystemNotifications(appContext)
            },
            clearAccountNotifications = {
                FittoNotificationStore.clearCurrentAccountNotifications(appContext)
            },
            signOut = auth::signOut,
            isInvalidCredentialsError = { error ->
                error is FirebaseAuthInvalidCredentialsException
            },
            isRecentLoginRequiredError = { error ->
                error is FirebaseAuthRecentLoginRequiredException
            }
        )
    }
}
