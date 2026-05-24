package com.example.fitto.data.local.session

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object UserSessionDataGuard {

    suspend fun ensureScopedDataForUser(
        context: Context,
        currentUid: String
    ): Boolean = withContext(Dispatchers.IO) {
        if (currentUid.isBlank()) return@withContext false

        val appContext = context.applicationContext
        val prefs = sessionPrefs(appContext)
        val lastUid = prefs.getString(KEY_LAST_LOGGED_IN_UID, null)

        if (lastUid == currentUid) {
            migrateLegacyPreferencesToUserScope(appContext, currentUid, shouldMigrate = true)
            return@withContext false
        }

        if (lastUid.isNullOrBlank()) {
            Log.d(LOG_TAG, "Active local user scope initialized. uid=$currentUid")
            migrateLegacyPreferencesToUserScope(appContext, currentUid, shouldMigrate = true)
        } else {
            Log.d(LOG_TAG, "Active local user scope switched. old=$lastUid new=$currentUid")
            // Kullanıcı değişirken eski global kayıtları yeni kullanıcıya kopyalamıyoruz.
            migrateLegacyPreferencesToUserScope(appContext, currentUid, shouldMigrate = false)
        }

        prefs.edit()
            .putString(KEY_LAST_LOGGED_IN_UID, currentUid)
            .commit()

        return@withContext true
    }

    fun getActiveUid(context: Context): String? {
        val firebaseUid = FirebaseAuth.getInstance()
            .currentUser
            ?.uid
            ?.takeIf { it.isNotBlank() }

        if (firebaseUid != null) return firebaseUid

        return sessionPrefs(context.applicationContext)
            .getString(KEY_LAST_LOGGED_IN_UID, null)
            ?.takeIf { it.isNotBlank() }
    }

    fun scopedSharedPreferences(
        context: Context,
        baseName: String
    ): SharedPreferences {
        val appContext = context.applicationContext
        val scopedName = scopedPreferenceName(appContext, baseName)
        return appContext.getSharedPreferences(scopedName, Context.MODE_PRIVATE)
    }

    fun scopedPreferenceName(
        context: Context,
        baseName: String
    ): String {
        return scopedPreferenceNameForUid(baseName, getActiveUid(context))
    }

    fun scopedPreferenceNameForUid(
        baseName: String,
        uid: String?
    ): String {
        return if (uid.isNullOrBlank()) {
            baseName
        } else {
            "${baseName}_${uid.toSafeScopeId()}"
        }
    }


    private fun migrateLegacyPreferencesToUserScope(
        context: Context,
        uid: String,
        shouldMigrate: Boolean
    ) {
        if (!shouldMigrate) return

        LEGACY_ACCOUNT_PREF_NAMES.forEach { baseName ->
            val legacyPrefs = context.getSharedPreferences(baseName, Context.MODE_PRIVATE)
            val legacyValues = legacyPrefs.all
            if (legacyValues.isEmpty()) return@forEach

            val scopedPrefs = context.getSharedPreferences(
                scopedPreferenceNameForUid(baseName, uid),
                Context.MODE_PRIVATE
            )

            val editor = scopedPrefs.edit()
            legacyValues.forEach { (key, value) ->
                if (scopedPrefs.contains(key)) return@forEach
                when (value) {
                    is String -> editor.putString(key, value)
                    is Int -> editor.putInt(key, value)
                    is Long -> editor.putLong(key, value)
                    is Float -> editor.putFloat(key, value)
                    is Boolean -> editor.putBoolean(key, value)
                    is Set<*> -> {
                        @Suppress("UNCHECKED_CAST")
                        editor.putStringSet(key, value.filterIsInstance<String>().toSet())
                    }
                }
            }
            editor.apply()
        }
    }

    private fun sessionPrefs(context: Context): SharedPreferences {
        return context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    private fun String.toSafeScopeId(): String {
        return replace(Regex("[^A-Za-z0-9_.-]"), "_")
    }

    private const val PREF_NAME = "fitto_user_session_guard"
    private const val KEY_LAST_LOGGED_IN_UID = "last_logged_in_uid"
    private const val LOG_TAG = "UserSessionGuard"

    private val LEGACY_ACCOUNT_PREF_NAMES = listOf(
        "fitto_water_tracking",
        "fitto_custom_diet_plans",
        "fitto_diet_plan_preferences",
        "fitto_fasting_session",
        "fitto_user_nutrition_targets",
        "fitto_step_counter",
        "fitto_exercise_logs",
        "fitto_user_body_metrics",
        "fitto_sleep_logs",
        "fitto_fridge_inventory",
        "fitto_recipe_shopping_list",
        "fitto_daily_ai_cache",
        "fitto_reminder_preferences",
        "fitto_reminder_delivery_guard"
    )
}
