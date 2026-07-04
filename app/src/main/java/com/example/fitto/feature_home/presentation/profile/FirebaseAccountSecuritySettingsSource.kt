package com.example.fitto.feature_home.presentation.profile

import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

internal class FirebaseAccountSecuritySettingsSource(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AccountSecuritySettingsSource {

    override suspend fun reauthenticateWithPassword(password: String) {
        val currentUser = auth.currentUser
            ?: throw AccountSettingsException(AccountSettingsError.NOT_SIGNED_IN)
        val email = currentUser.email?.takeIf { it.isNotBlank() }
            ?: throw AccountSettingsException(AccountSettingsError.EMAIL_MISSING)
        val currentPassword = AccountSettingsInputPolicy.requireCurrentPassword(password)

        try {
            currentUser.reauthenticate(
                EmailAuthProvider.getCredential(email, currentPassword)
            ).await()
        } catch (exception: FirebaseAuthInvalidCredentialsException) {
            throw AccountSettingsException(AccountSettingsError.WRONG_PASSWORD, exception)
        }
    }

    override suspend fun updatePasswordAfterReauth(
        newPassword: String,
        confirmPassword: String
    ) {
        val currentUser = auth.currentUser
            ?: throw AccountSettingsException(AccountSettingsError.NOT_SIGNED_IN)

        AccountSettingsInputPolicy.validateNewPassword(
            newPassword = newPassword,
            confirmPassword = confirmPassword
        )

        currentUser.updatePassword(newPassword).await()
    }

    override fun isPasswordProvider(): Boolean {
        return auth.currentUser?.providerData
            ?.any { providerInfo -> providerInfo.providerId == EmailAuthProvider.PROVIDER_ID } == true
    }

    override fun isGoogleProvider(): Boolean {
        return auth.currentUser?.providerData
            ?.any { providerInfo -> providerInfo.providerId == GOOGLE_PROVIDER_ID } == true
    }

    override suspend fun clearCurrentUserHistory(): String {
        val currentUser = auth.currentUser
            ?: throw AccountSettingsException(AccountSettingsError.NOT_SIGNED_IN)
        val uid = currentUser.uid
        val now = System.currentTimeMillis()

        firestore.collection(USERS_COLLECTION)
            .document(uid)
            .set(
                mapOf(
                    BodyMeasurementsData.FIELD_NAME to FieldValue.delete(),
                    BodyMeasurementsData.HISTORY_FIELD_NAME to FieldValue.delete(),
                    WEIGHT_HISTORY_FIELD to FieldValue.delete(),
                    MEAL_HISTORY_FIELD to FieldValue.delete(),
                    NUTRITION_HISTORY_FIELD to FieldValue.delete(),
                    "weightKg" to FieldValue.delete(),
                    "currentWeightKg" to FieldValue.delete(),
                    "waterHistory" to FieldValue.delete(),
                    "exerciseHistory" to FieldValue.delete(),
                    "sleepHistory" to FieldValue.delete(),
                    "fastingHistory" to FieldValue.delete(),
                    "dailyCheckins" to FieldValue.delete(),
                    "weeklyReports" to FieldValue.delete(),
                    "activeDietPlanId" to FieldValue.delete(),
                    "activePlanId" to FieldValue.delete(),
                    "lastSelectedDietPlanId" to FieldValue.delete(),
                    "historyClearedAt" to now,
                    "updatedAt" to now
                ),
                SetOptions.merge()
            )
            .await()

        return uid
    }

    private companion object {
        const val USERS_COLLECTION = "users"
        const val GOOGLE_PROVIDER_ID = "google.com"
        const val WEIGHT_HISTORY_FIELD = "weightHistory"
        const val MEAL_HISTORY_FIELD = "mealHistory"
        const val NUTRITION_HISTORY_FIELD = "nutritionHistory"
    }
}
