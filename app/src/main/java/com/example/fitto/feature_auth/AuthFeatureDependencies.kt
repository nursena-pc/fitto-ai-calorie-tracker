package com.example.fitto.feature_auth

import android.content.Context
import com.example.fitto.feature_auth.login.AuthLoginCoordinator
import com.example.fitto.feature_auth.login.FirebaseAuthLoginDataSource
import com.example.fitto.feature_auth.register.AccountRegistrationCoordinator
import com.example.fitto.feature_auth.register.FirebaseAccountRegistrationDataSource
import com.example.fitto.feature_auth.register.FirebaseGoogleOnboardingDataSource
import com.example.fitto.feature_auth.register.FirebaseUsernameSetupDataSource
import com.example.fitto.feature_auth.register.GeminiAiRepository
import com.example.fitto.feature_auth.register.OnboardingGoogleSignUpCoordinator
import com.example.fitto.feature_auth.register.OnboardingViewModelFactory
import com.example.fitto.feature_auth.register.UsernameSetupCoordinator
import com.example.fitto.feature_dietplans.data.UserNutritionTargetStore
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineDispatcher

/** Shared authentication and onboarding dependency graph owned by the application container. */
internal class AuthFeatureDependencies(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher
) {
    private val appContext = context.applicationContext

    val loginCoordinator: AuthLoginCoordinator by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        AuthLoginCoordinator(FirebaseAuthLoginDataSource())
    }

    val accountRegistrationCoordinator: AccountRegistrationCoordinator by lazy(
        LazyThreadSafetyMode.SYNCHRONIZED
    ) {
        AccountRegistrationCoordinator(FirebaseAccountRegistrationDataSource())
    }

    val usernameSetupCoordinator: UsernameSetupCoordinator by lazy(
        LazyThreadSafetyMode.SYNCHRONIZED
    ) {
        UsernameSetupCoordinator(
            dataSource = FirebaseUsernameSetupDataSource(appContext),
            ioDispatcher = ioDispatcher
        )
    }

    val googleSignUpCoordinator: OnboardingGoogleSignUpCoordinator by lazy(
        LazyThreadSafetyMode.SYNCHRONIZED
    ) {
        OnboardingGoogleSignUpCoordinator(FirebaseGoogleOnboardingDataSource())
    }

    val onboardingViewModelFactory: OnboardingViewModelFactory by lazy(
        LazyThreadSafetyMode.SYNCHRONIZED
    ) {
        OnboardingViewModelFactory(
            aiRepository = GeminiAiRepository(ioDispatcher = ioDispatcher)
        )
    }

    fun shouldOpenHomeForCurrentSession(): Boolean {
        val user = FirebaseAuth.getInstance().currentUser ?: return false
        return AuthHomeReadinessPolicy.shouldOpenHome(
            AuthSessionSnapshot(
                hasUser = true,
                email = user.email,
                isEmailVerified = user.isEmailVerified
            )
        )
    }

    fun saveDailyTargetCalories(calories: Int) {
        UserNutritionTargetStore(appContext).saveDailyTargetCalories(calories)
    }
}
