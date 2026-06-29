package com.example.fitto.feature_dietplans.data

import android.content.Context
import com.example.fitto.data.local.session.UserSessionDataGuard
import com.example.fitto.feature_dietplans.DietPlanPreferenceStateResolver
import com.example.fitto.feature_dietplans.model.FastingPlanType
import com.example.fitto.feature_meal.model.MealType

class DietPlanPreferenceStore(context: Context) {

    private val prefs = UserSessionDataGuard.scopedSharedPreferences(context, PREF_NAME)

    fun getActivePlanId(): String? {
        return prefs.getString(KEY_ACTIVE_PLAN_ID, null)
    }

    fun getActivePlanStartedAt(): Long {
        return prefs.getLong(KEY_ACTIVE_PLAN_STARTED_AT, 0L)
    }

    fun getActiveFastingType(): FastingPlanType? {
        val rawValue = prefs.getString(KEY_FASTING_TYPE, null) ?: return null
        return runCatching { FastingPlanType.valueOf(rawValue) }.getOrNull()
    }

    fun getCustomFastingHours(): Int? {
        return DietPlanPreferenceStateResolver.customFastingHoursOrNull(
            prefs.getInt(KEY_CUSTOM_FASTING_HOURS, CUSTOM_HOURS_NOT_SET)
        )
    }

    fun saveActivePlan(
        planId: String,
        fastingType: FastingPlanType? = null,
        customFastingHours: Int? = null
    ) {
        val now = System.currentTimeMillis()
        prefs.edit()
            .putString(KEY_ACTIVE_PLAN_ID, planId)
            .putString(KEY_FASTING_TYPE, fastingType?.name)
            .putInt(
                KEY_CUSTOM_FASTING_HOURS,
                DietPlanPreferenceStateResolver.storedCustomFastingHours(customFastingHours)
            )
            .putLong(KEY_ACTIVE_PLAN_STARTED_AT, now)
            .remove(KEY_TODAY_APPLIED_PLAN_ID)
            .remove(KEY_TODAY_APPLIED_AT)
            .apply()
    }

    fun applyActivePlanToday() {
        val activePlanId = getActivePlanId() ?: return
        prefs.edit()
            .putString(KEY_TODAY_APPLIED_PLAN_ID, activePlanId)
            .putLong(KEY_TODAY_APPLIED_AT, System.currentTimeMillis())
            .apply()
    }

    fun getTodayAppliedPlanId(): String? {
        return prefs.getString(KEY_TODAY_APPLIED_PLAN_ID, null)
    }

    fun getCurrentPlanDay(durationDays: Int): Int {
        return DietPlanPreferenceStateResolver.currentPlanDay(
            durationDays = durationDays,
            startedAt = getActivePlanStartedAt(),
            nowMillis = System.currentTimeMillis()
        )
    }

    fun getCompletedPlannedMealIds(planId: String, dayNumber: Int): Set<String> {
        return prefs.getStringSet(completedMealsKey(planId, dayNumber), emptySet()).orEmpty()
    }

    fun markPlannedMealCompleted(planId: String, dayNumber: Int, plannedMealId: String) {
        val key = completedMealsKey(planId, dayNumber)
        val updated = prefs.getStringSet(key, emptySet()).orEmpty().toMutableSet()
        updated.add(plannedMealId)
        prefs.edit()
            .putStringSet(key, updated)
            .apply()
    }

    fun markMealTypeCompletedIfMatches(
        planId: String,
        dayNumber: Int,
        mealType: MealType,
        plannedMealId: String
    ) {
        if (DietPlanPreferenceStateResolver.shouldMarkMealTypeCompleted(mealType, plannedMealId)) {
            markPlannedMealCompleted(planId, dayNumber, plannedMealId)
        }
    }

    fun isFavorite(planId: String): Boolean {
        return getFavoriteIds().contains(planId)
    }

    fun getFavoriteIds(): Set<String> {
        return prefs.getStringSet(KEY_FAVORITE_PLAN_IDS, emptySet()).orEmpty()
    }

    fun toggleFavorite(planId: String): Boolean {
        val resolution = DietPlanPreferenceStateResolver.toggleFavorite(
            favoriteIds = getFavoriteIds(),
            planId = planId
        )

        prefs.edit()
            .putStringSet(KEY_FAVORITE_PLAN_IDS, resolution.favoriteIds)
            .apply()

        return resolution.isFavorite
    }

    fun getRecentDietSearches(): List<String> {
        return DietPlanPreferenceStateResolver.recentSearches(
            prefs.getString(KEY_RECENT_DIET_SEARCHES, "")
        )
    }

    fun saveRecentDietSearch(query: String) {
        val updatedSearches = DietPlanPreferenceStateResolver.updatedRecentSearches(
            currentSearches = getRecentDietSearches(),
            query = query
        ) ?: return

        prefs.edit()
            .putString(
                KEY_RECENT_DIET_SEARCHES,
                DietPlanPreferenceStateResolver.encodeRecentSearches(updatedSearches)
            )
            .apply()
    }

    fun removeFavorite(planId: String) {
        val updatedFavorites = getFavoriteIds().toMutableSet()
        updatedFavorites.remove(planId)

        prefs.edit()
            .putStringSet(KEY_FAVORITE_PLAN_IDS, updatedFavorites)
            .apply()
    }

    fun clearActivePlanIf(planId: String) {
        if (getActivePlanId() != planId) return

        prefs.edit()
            .remove(KEY_ACTIVE_PLAN_ID)
            .remove(KEY_FASTING_TYPE)
            .remove(KEY_CUSTOM_FASTING_HOURS)
            .remove(KEY_ACTIVE_PLAN_STARTED_AT)
            .remove(KEY_TODAY_APPLIED_PLAN_ID)
            .remove(KEY_TODAY_APPLIED_AT)
            .apply()
    }
    private fun completedMealsKey(planId: String, dayNumber: Int): String {
        return "${KEY_COMPLETED_PLANNED_MEALS}_${planId}_day_$dayNumber"
    }


    companion object {
        private const val PREF_NAME = "fitto_diet_plan_preferences"
        private const val KEY_ACTIVE_PLAN_ID = "active_plan_id"
        private const val KEY_FASTING_TYPE = "active_fasting_type"
        private const val KEY_CUSTOM_FASTING_HOURS = "custom_fasting_hours"
        private const val KEY_ACTIVE_PLAN_STARTED_AT = "active_plan_started_at"
        private const val KEY_TODAY_APPLIED_PLAN_ID = "today_applied_plan_id"
        private const val KEY_TODAY_APPLIED_AT = "today_applied_at"
        private const val KEY_FAVORITE_PLAN_IDS = "favorite_plan_ids"
        private const val KEY_COMPLETED_PLANNED_MEALS = "completed_planned_meals"
        private const val CUSTOM_HOURS_NOT_SET = -1
        private const val KEY_RECENT_DIET_SEARCHES = "recent_diet_searches"
    }
}
