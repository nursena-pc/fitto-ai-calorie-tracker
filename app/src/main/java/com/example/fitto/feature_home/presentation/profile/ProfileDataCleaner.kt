package com.example.fitto.feature_home.presentation.profile

import android.content.Context
import com.example.fitto.data.local.FittoDatabaseProvider
import com.example.fitto.data.local.session.UserSessionDataGuard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ProfileDataCleaner {

    suspend fun clearCurrentUserData(
        context: Context,
        uid: String
    ) = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val database = FittoDatabaseProvider.getDatabase(appContext)

        database.mealDao().clearMealLogsForUser(uid)
        database.favoriteFoodDao().clearFavoritesForUser(uid)
        database.foodDao().clearFoodsForOwner(uid)
        database.profileDao().deleteProfile(uid)
        database.recipeDao().deleteOwnedRecipes(uid)

        ProfilePreferenceStore.clearAccountScopedPreferencesForUid(appContext, uid)

        DATA_PREF_NAMES.forEach { prefName ->
            val scopedName = UserSessionDataGuard.scopedPreferenceNameForUid(prefName, uid)
            appContext.getSharedPreferences(scopedName, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit()
        }
    }

    private val DATA_PREF_NAMES = listOf(
        "fitto_water_tracking",
        "fitto_custom_diet_plans",
        "fitto_diet_plan_preferences",
        "fitto_fasting_session",
        "fitto_user_nutrition_targets",
        "fitto_step_counter",
        "fitto_exercise_logs",
        "fitto_user_body_metrics",
        "fitto_sleep_logs",
        "fitto_daily_ai_cache",
        "fitto_fridge_inventory",
        "fitto_recipe_shopping_list",
        "social_search_prefs",
        "fitto_social_observer_state"
    )
}
