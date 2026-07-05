package com.example.fitto.feature_dietplans

import android.content.Context
import com.example.fitto.data.local.repository.ProfileCacheRepository
import com.example.fitto.feature_dietplans.data.CustomDietPlanStore
import com.example.fitto.feature_dietplans.data.DietPlanPreferenceStore
import com.example.fitto.feature_home.presentation.social.SocialRepository
import kotlinx.coroutines.CoroutineDispatcher

/** Screen-level dependency graph for the diet-plan feature. */
class DietPlanFeatureDependencies(
    context: Context,
    val ioDispatcher: CoroutineDispatcher
) {
    private val appContext = context.applicationContext

    val customPlanStore by lazy { CustomDietPlanStore(appContext) }
    val preferenceStore by lazy { DietPlanPreferenceStore(appContext) }
    val profileLoader by lazy {
        DietPlanProfileLoader(
            repository = ProfileCacheRepository(appContext),
            ioDispatcher = ioDispatcher
        )
    }
    val preferenceAction by lazy { DietPlanPreferenceAction.from(preferenceStore) }
    val deletionAction by lazy {
        CustomDietPlanDeletionAction.from(
            customPlanStore = customPlanStore,
            preferenceStore = preferenceStore
        )
    }
    val draftCompletionAction by lazy {
        DietPlanDraftCompletionAction.from(customPlanStore)
    }
    val listLoader by lazy {
        DietPlanListLoader.from(
            customPlanStore = customPlanStore,
            preferenceStore = preferenceStore
        )
    }
    val createSaveAction by lazy {
        CreateDietPlanSaveAction.from(
            customPlanStore = customPlanStore,
            preferenceAction = preferenceAction
        )
    }
    val socialShareAction by lazy {
        CreateDietPlanSocialShareAction.from(SocialRepository())
    }
}
