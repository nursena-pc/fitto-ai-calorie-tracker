package com.example.fitto.feature_home.presentation.profile

import android.content.Context
import com.example.fitto.data.local.FittoDatabaseProvider
import com.example.fitto.data.local.session.UserSessionDataGuard
import com.example.fitto.feature_home.presentation.social.SocialRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineDispatcher

/** Shared profile dependency graph owned by the application composition root. */
internal class ProfileFeatureDependencies(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher,
    val socialRepository: SocialRepository
) {
    private val appContext = context.applicationContext

    val repository: ProfileRepository by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        ProfileRepository()
    }

    val accountSettingsRepository: AccountSettingsRepository by lazy(
        LazyThreadSafetyMode.SYNCHRONIZED
    ) {
        AccountSettingsRepository()
    }

    val viewModelFactory: ProfileViewModelFactory by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        ProfileViewModelFactory(repository)
    }

    val weeklySummaryLoader: ProfileWeeklySummaryLoader by lazy(
        LazyThreadSafetyMode.SYNCHRONIZED
    ) {
        val mealDao = FittoDatabaseProvider.getDatabase(appContext).mealDao()
        ProfileWeeklySummaryLoader(
            activeUserId = { UserSessionDataGuard.getActiveUid(appContext) },
            nutritionSource = ProfileNutritionSummarySource { userId, dateIso ->
                mealDao.getNutritionSummaryForDate(userId, dateIso)
            },
            ioDispatcher = ioDispatcher
        )
    }

    fun currentUserId(): String? = FirebaseAuth.getInstance().currentUser?.uid
}
