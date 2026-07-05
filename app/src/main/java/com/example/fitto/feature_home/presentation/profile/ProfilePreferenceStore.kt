package com.example.fitto.feature_home.presentation.profile

import com.example.fitto.core.platform.ActivityTransitionCompat
import com.example.fitto.core.text.AppLocales
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import com.example.fitto.MainActivity
import com.example.fitto.R
import com.example.fitto.data.local.session.UserSessionDataGuard
import com.google.firebase.auth.FirebaseAuth
import java.util.Locale

enum class FittoThemeMode(
    val value: String
) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark")
}

enum class ProfileEnergyUnit(val value: String) {
    KCAL("kcal"),
    KJ("kj")
}

enum class ProfileAccountVisibility(val value: String) {
    PUBLIC("public"),
    FOLLOWERS("followers"),
    PRIVATE("private")
}

enum class ProfileNotificationSound(val value: String) {
    SOFT("soft"),
    CLASSIC("classic"),
    NATURE("nature"),
    CUSTOM("custom"),
    SILENT("silent")
}

enum class NutritionDietPreference(val value: String) {
    STANDARD("standard"),
    VEGETARIAN("vegetarian"),
    VEGAN("vegan"),
    GLUTEN_FREE("gluten_free"),
    LACTOSE_FREE("lactose_free"),
    HIGH_PROTEIN("high_protein"),
    LOW_CARB("low_carb")
}

enum class NutritionMealRoutine(val value: String) {
    THREE_MEALS("three_meals"),
    TWO_MEALS_SNACKS("two_meals_snacks"),
    FLEXIBLE("flexible")
}

data class PendingHomeRestore(
    val tabId: Int,
    val profileScreen: String?
)

object ProfilePreferenceStore {

    private const val PREF_NAME = "fitto_profile_preferences"

    private const val KEY_REGION = "region"
    private const val KEY_LANGUAGE = "language"
    private const val KEY_THEME = "theme"
    private const val KEY_ENERGY_UNIT = "energy_unit"
    private const val KEY_ACCOUNT_VISIBILITY = "account_visibility"
    private const val KEY_COMMENTS_ENABLED = "comments_enabled"
    private const val KEY_NOTIFICATIONS_ALLOWED = "notifications_allowed"
    private const val KEY_ACHIEVEMENT_NOTIFICATIONS_ENABLED = "achievement_notifications_enabled"
    private const val KEY_REMINDER_NOTIFICATIONS_ENABLED = "reminder_notifications_enabled"
    private const val KEY_SOCIAL_NOTIFICATIONS_ENABLED = "social_notifications_enabled"
    private const val KEY_DIRECT_MESSAGE_NOTIFICATIONS_ENABLED = "direct_message_notifications_enabled"
    private const val KEY_GROUP_MESSAGE_NOTIFICATIONS_ENABLED = "group_message_notifications_enabled"
    private const val KEY_NOTIFICATION_SOUND = "notification_sound"
    private const val KEY_NOTIFICATION_CUSTOM_SOUND_URI = "notification_custom_sound_uri"
    private const val KEY_NOTIFICATION_CUSTOM_SOUND_NAME = "notification_custom_sound_name"
    private const val KEY_VIBRATION_ENABLED = "vibration_enabled"
    private const val KEY_SILENT_HOURS = "silent_hours"
    private const val KEY_EXACT_ALARM_PROMPT_SEEN = "exact_alarm_prompt_seen"
    private const val KEY_NUTRITION_DIET_PREFERENCES = "nutrition_diet_preferences"
    private const val KEY_NUTRITION_ALLERGIES = "nutrition_allergies"
    private const val KEY_NUTRITION_DISLIKED_FOODS = "nutrition_disliked_foods"
    private const val KEY_NUTRITION_MEAL_ROUTINE = "nutrition_meal_routine"

    private const val KEY_PENDING_HOME_TAB_ID = "pending_home_tab_id"
    private const val KEY_PENDING_PROFILE_SCREEN = "pending_profile_screen"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    private fun currentUid(context: Context? = null): String? {
        return FirebaseAuth.getInstance().currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: context?.let { UserSessionDataGuard.getActiveUid(it) }
    }

    private fun accountScopedKey(baseKey: String, uidOverride: String?): String {
        return uidOverride?.let { uid -> "${uid}_$baseKey" } ?: baseKey
    }

    private fun accountScopedKey(context: Context, baseKey: String): String {
        return accountScopedKey(baseKey, currentUid(context))
    }

    fun getRegion(context: Context): String {
        return prefs(context).getString(
            KEY_REGION,
            context.getString(R.string.region_suggested_turkey)
        ) ?: context.getString(R.string.region_suggested_turkey)
    }

    fun setRegion(context: Context, region: String) {
        prefs(context).edit()
            .putString(KEY_REGION, region)
            .apply()
    }

    fun getLanguage(context: Context): ProfileLanguage {
        val saved = prefs(context).getString(
            KEY_LANGUAGE,
            ProfileLanguage.TURKISH.name
        )

        return runCatching {
            ProfileLanguage.valueOf(saved ?: ProfileLanguage.TURKISH.name)
        }.getOrDefault(ProfileLanguage.TURKISH)
    }

    fun setLanguage(context: Context, language: ProfileLanguage) {
        val oldLanguage = getLanguage(context)

        if (oldLanguage != language) {
            context.findActivity()?.rememberCurrentHomeScreenForSettingsRecreate()
        }

        prefs(context).edit()
            .putString(KEY_LANGUAGE, language.name)
            .apply()

        if (oldLanguage != language) {
            context.findActivity()?.recreateWithoutAnimation()
        }
    }

    fun getTheme(context: Context): FittoThemeMode {
        val saved = prefs(context).getString(
            KEY_THEME,
            FittoThemeMode.SYSTEM.value
        )

        return FittoThemeMode.entries.firstOrNull { it.value == saved }
            ?: FittoThemeMode.SYSTEM
    }

    fun setTheme(context: Context, theme: FittoThemeMode) {
        val oldTheme = getTheme(context)

        if (oldTheme != theme) {
            context.findActivity()?.rememberCurrentHomeScreenForSettingsRecreate()
        }

        prefs(context).edit()
            .putString(KEY_THEME, theme.value)
            .apply()

        if (oldTheme != theme) {
            val activity = context.findActivity()

            if (activity == null) {
                AppCompatDelegate.setDefaultNightMode(nightModeFor(theme))
            } else {
                activity.window.decorView.post {
                    ActivityTransitionCompat.disableAnimations(activity)
                    AppCompatDelegate.setDefaultNightMode(nightModeFor(theme))
                }
            }
        }
    }

    fun applySavedTheme(context: Context) {
        val savedMode = nightModeFor(getTheme(context))

        if (AppCompatDelegate.getDefaultNightMode() != savedMode) {
            AppCompatDelegate.setDefaultNightMode(savedMode)
        }
    }

    fun rememberPendingHomeRestore(
        context: Context,
        tabId: Int,
        profileScreen: String?
    ) {
        prefs(context).edit()
            .putInt(KEY_PENDING_HOME_TAB_ID, tabId)
            .putString(KEY_PENDING_PROFILE_SCREEN, profileScreen)
            .apply()
    }

    fun consumePendingHomeRestore(context: Context): PendingHomeRestore? {
        val preferences = prefs(context)

        if (!preferences.contains(KEY_PENDING_HOME_TAB_ID)) {
            return null
        }

        val restore = PendingHomeRestore(
            tabId = preferences.getInt(KEY_PENDING_HOME_TAB_ID, 0),
            profileScreen = preferences.getString(KEY_PENDING_PROFILE_SCREEN, null)
        )

        preferences.edit()
            .remove(KEY_PENDING_HOME_TAB_ID)
            .remove(KEY_PENDING_PROFILE_SCREEN)
            .apply()

        return restore
    }

    fun languageText(context: Context): String {
        return when (getLanguage(context)) {
            ProfileLanguage.TURKISH -> context.getString(R.string.language_turkish)
            ProfileLanguage.ENGLISH -> context.getString(R.string.language_english)
        }
    }

    fun themeText(context: Context): String {
        return when (getTheme(context)) {
            FittoThemeMode.SYSTEM -> context.getString(R.string.profile_theme_system)
            FittoThemeMode.LIGHT -> context.getString(R.string.profile_theme_light)
            FittoThemeMode.DARK -> context.getString(R.string.profile_theme_dark)
        }
    }


    fun getEnergyUnit(context: Context): ProfileEnergyUnit {
        val saved = prefs(context).getString(KEY_ENERGY_UNIT, ProfileEnergyUnit.KCAL.value)
        return ProfileEnergyUnit.entries.firstOrNull { it.value == saved } ?: ProfileEnergyUnit.KCAL
    }

    fun setEnergyUnit(context: Context, unit: ProfileEnergyUnit) {
        prefs(context).edit().putString(KEY_ENERGY_UNIT, unit.value).apply()
    }

    fun energyUnitText(context: Context): String {
        return when (getEnergyUnit(context)) {
            ProfileEnergyUnit.KCAL -> context.getString(R.string.profile_energy_unit_kcal)
            ProfileEnergyUnit.KJ -> context.getString(R.string.profile_energy_unit_kj)
        }
    }

    fun getAccountVisibility(context: Context): ProfileAccountVisibility {
        val saved = prefs(context).getString(KEY_ACCOUNT_VISIBILITY, ProfileAccountVisibility.PUBLIC.value)
        return ProfileAccountVisibility.entries.firstOrNull { it.value == saved } ?: ProfileAccountVisibility.PUBLIC
    }

    fun setAccountVisibility(context: Context, visibility: ProfileAccountVisibility) {
        prefs(context).edit().putString(KEY_ACCOUNT_VISIBILITY, visibility.value).apply()
    }

    fun accountVisibilityText(context: Context): String {
        return when (getAccountVisibility(context)) {
            ProfileAccountVisibility.PUBLIC -> context.getString(R.string.profile_visibility_public)
            ProfileAccountVisibility.FOLLOWERS -> context.getString(R.string.profile_visibility_followers)
            ProfileAccountVisibility.PRIVATE -> context.getString(R.string.profile_visibility_private)
        }
    }

    fun areCommentsEnabled(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_COMMENTS_ENABLED, true)
    }

    fun setCommentsEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_COMMENTS_ENABLED, enabled).apply()
    }

    fun areNotificationsAllowed(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_NOTIFICATIONS_ALLOWED, true)
    }

    fun setNotificationsAllowed(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_NOTIFICATIONS_ALLOWED, enabled).apply()
    }

    fun areAchievementNotificationsEnabled(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_ACHIEVEMENT_NOTIFICATIONS_ENABLED, true)
    }

    fun setAchievementNotificationsEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ACHIEVEMENT_NOTIFICATIONS_ENABLED, enabled).apply()
    }

    fun areReminderNotificationsEnabled(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_REMINDER_NOTIFICATIONS_ENABLED, true)
    }

    fun setReminderNotificationsEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_REMINDER_NOTIFICATIONS_ENABLED, enabled).apply()
    }

    fun areSocialNotificationsEnabled(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_SOCIAL_NOTIFICATIONS_ENABLED, true)
    }

    fun setSocialNotificationsEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_SOCIAL_NOTIFICATIONS_ENABLED, enabled).apply()
    }

    fun areDirectMessageNotificationsEnabled(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_DIRECT_MESSAGE_NOTIFICATIONS_ENABLED, true)
    }

    fun setDirectMessageNotificationsEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_DIRECT_MESSAGE_NOTIFICATIONS_ENABLED, enabled).apply()
    }

    fun areGroupMessageNotificationsEnabled(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_GROUP_MESSAGE_NOTIFICATIONS_ENABLED, true)
    }

    fun setGroupMessageNotificationsEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_GROUP_MESSAGE_NOTIFICATIONS_ENABLED, enabled).apply()
    }

    fun getNotificationSound(context: Context): ProfileNotificationSound {
        val saved = prefs(context).getString(KEY_NOTIFICATION_SOUND, ProfileNotificationSound.SOFT.value)
        return ProfileNotificationSound.entries.firstOrNull { it.value == saved } ?: ProfileNotificationSound.SOFT
    }

    fun setNotificationSound(context: Context, sound: ProfileNotificationSound) {
        prefs(context).edit().putString(KEY_NOTIFICATION_SOUND, sound.value).apply()
    }

    fun setCustomNotificationSound(context: Context, uri: String, displayName: String) {
        prefs(context).edit()
            .putString(KEY_NOTIFICATION_SOUND, ProfileNotificationSound.CUSTOM.value)
            .putString(KEY_NOTIFICATION_CUSTOM_SOUND_URI, uri)
            .putString(KEY_NOTIFICATION_CUSTOM_SOUND_NAME, displayName)
            .apply()
    }

    fun getCustomNotificationSoundUri(context: Context): String? {
        return prefs(context).getString(KEY_NOTIFICATION_CUSTOM_SOUND_URI, null)
            ?.takeIf { it.isNotBlank() }
    }

    fun getCustomNotificationSoundName(context: Context): String? {
        return prefs(context).getString(KEY_NOTIFICATION_CUSTOM_SOUND_NAME, null)
            ?.takeIf { it.isNotBlank() }
    }

    fun notificationSoundText(context: Context): String {
        return when (getNotificationSound(context)) {
            ProfileNotificationSound.SOFT -> context.getString(R.string.profile_notification_sound_soft)
            ProfileNotificationSound.CLASSIC -> context.getString(R.string.profile_notification_sound_classic)
            ProfileNotificationSound.NATURE -> context.getString(R.string.profile_notification_sound_nature)
            ProfileNotificationSound.CUSTOM -> getCustomNotificationSoundName(context)
                ?: context.getString(R.string.profile_notification_sound_custom)
            ProfileNotificationSound.SILENT -> context.getString(R.string.profile_notification_sound_silent)
        }
    }

    fun isVibrationEnabled(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_VIBRATION_ENABLED, true)
    }

    fun setVibrationEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_VIBRATION_ENABLED, enabled).apply()
    }

    fun silentHoursText(context: Context): String {
        return prefs(context).getString(KEY_SILENT_HOURS, context.getString(R.string.profile_silent_hours_default))
            ?: context.getString(R.string.profile_silent_hours_default)
    }

    fun setSilentHoursText(context: Context, value: String) {
        prefs(context).edit().putString(KEY_SILENT_HOURS, value).apply()
    }

    fun onOffText(context: Context, enabled: Boolean): String {
        return if (enabled) context.getString(R.string.profile_setting_on) else context.getString(R.string.profile_setting_off)
    }

    fun hasSeenExactAlarmPrompt(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_EXACT_ALARM_PROMPT_SEEN, false)
    }

    fun setExactAlarmPromptSeen(context: Context, seen: Boolean = true) {
        prefs(context).edit()
            .putBoolean(KEY_EXACT_ALARM_PROMPT_SEEN, seen)
            .apply()
    }

    fun connectedAppsSummary(context: Context): String {
        return context.getString(R.string.profile_connected_apps_empty)
    }

    fun accountEmailText(context: Context): String {
        return com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email
            ?: context.getString(R.string.profile_data_not_saved_yet)
    }

    fun accountUsernameText(context: Context): String {
        return context.getString(R.string.profile_data_not_saved_yet)
    }

    fun accountDisplayNameText(context: Context): String {
        return context.getString(R.string.profile_data_not_saved_yet)
    }


    fun getNutritionDietPreferences(context: Context): Set<NutritionDietPreference> {
        val raw = prefs(context).getStringSet(
            accountScopedKey(context, KEY_NUTRITION_DIET_PREFERENCES),
            setOf(NutritionDietPreference.STANDARD.value)
        ).orEmpty()

        val resolved = raw.mapNotNull { value ->
            NutritionDietPreference.entries.firstOrNull { it.value == value }
        }.toSet()

        return resolved.ifEmpty { setOf(NutritionDietPreference.STANDARD) }
    }

    fun setNutritionDietPreferences(context: Context, preferences: Set<NutritionDietPreference>) {
        val safePreferences = when {
            preferences.isEmpty() -> setOf(NutritionDietPreference.STANDARD)
            preferences.contains(NutritionDietPreference.STANDARD) -> setOf(NutritionDietPreference.STANDARD)
            else -> preferences - NutritionDietPreference.STANDARD
        }

        prefs(context).edit()
            .putStringSet(accountScopedKey(context, KEY_NUTRITION_DIET_PREFERENCES), safePreferences.map { it.value }.toSet())
            .apply()
    }

    fun getNutritionAllergies(context: Context): String {
        return prefs(context).getString(accountScopedKey(context, KEY_NUTRITION_ALLERGIES), "").orEmpty()
    }

    fun setNutritionAllergies(context: Context, value: String) {
        prefs(context).edit()
            .putString(accountScopedKey(context, KEY_NUTRITION_ALLERGIES), value.trim())
            .apply()
    }

    fun getNutritionDislikedFoods(context: Context): String {
        return prefs(context).getString(accountScopedKey(context, KEY_NUTRITION_DISLIKED_FOODS), "").orEmpty()
    }

    fun setNutritionDislikedFoods(context: Context, value: String) {
        prefs(context).edit()
            .putString(accountScopedKey(context, KEY_NUTRITION_DISLIKED_FOODS), value.trim())
            .apply()
    }

    fun getNutritionMealRoutine(context: Context): NutritionMealRoutine {
        val saved = prefs(context).getString(accountScopedKey(context, KEY_NUTRITION_MEAL_ROUTINE), NutritionMealRoutine.FLEXIBLE.value)
        return NutritionMealRoutine.entries.firstOrNull { it.value == saved } ?: NutritionMealRoutine.FLEXIBLE
    }

    fun setNutritionMealRoutine(context: Context, routine: NutritionMealRoutine) {
        prefs(context).edit()
            .putString(accountScopedKey(context, KEY_NUTRITION_MEAL_ROUTINE), routine.value)
            .apply()
    }

    fun nutritionDietPreferenceText(context: Context, preference: NutritionDietPreference): String {
        return when (preference) {
            NutritionDietPreference.STANDARD -> context.getString(R.string.nutrition_pref_diet_standard)
            NutritionDietPreference.VEGETARIAN -> context.getString(R.string.nutrition_pref_diet_vegetarian)
            NutritionDietPreference.VEGAN -> context.getString(R.string.nutrition_pref_diet_vegan)
            NutritionDietPreference.GLUTEN_FREE -> context.getString(R.string.nutrition_pref_diet_gluten_free)
            NutritionDietPreference.LACTOSE_FREE -> context.getString(R.string.nutrition_pref_diet_lactose_free)
            NutritionDietPreference.HIGH_PROTEIN -> context.getString(R.string.nutrition_pref_diet_high_protein)
            NutritionDietPreference.LOW_CARB -> context.getString(R.string.nutrition_pref_diet_low_carb)
        }
    }

    fun nutritionDietPreferencesText(context: Context): String {
        return getNutritionDietPreferences(context)
            .sortedBy { it.ordinal }
            .joinToString(separator = ", ") { nutritionDietPreferenceText(context, it) }
            .ifBlank { context.getString(R.string.nutrition_pref_diet_standard) }
    }

    fun nutritionMealRoutineText(context: Context): String {
        return when (getNutritionMealRoutine(context)) {
            NutritionMealRoutine.THREE_MEALS -> context.getString(R.string.nutrition_pref_routine_three_meals)
            NutritionMealRoutine.TWO_MEALS_SNACKS -> context.getString(R.string.nutrition_pref_routine_two_meals_snacks)
            NutritionMealRoutine.FLEXIBLE -> context.getString(R.string.nutrition_pref_routine_flexible)
        }
    }

    fun nutritionPreferencesSummary(context: Context): String {
        val items = mutableListOf<String>()
        items += nutritionDietPreferencesText(context)
        getNutritionAllergies(context).takeIf { it.isNotBlank() }?.let {
            items += context.getString(R.string.nutrition_pref_summary_allergy, it)
        }
        getNutritionDislikedFoods(context).takeIf { it.isNotBlank() }?.let {
            items += context.getString(R.string.nutrition_pref_summary_disliked, it)
        }
        items += context.getString(R.string.nutrition_pref_summary_routine, nutritionMealRoutineText(context))
        return items.joinToString(separator = "\n")
    }


    fun clearAccountScopedPreferences(context: Context) {
        clearAccountScopedPreferencesForUid(context, currentUid(context))
    }

    fun clearAccountScopedPreferencesForUid(context: Context, uid: String?) {
        prefs(context).edit()
            .remove(KEY_REGION)
            .remove(KEY_ENERGY_UNIT)
            .remove(KEY_ACCOUNT_VISIBILITY)
            .remove(KEY_COMMENTS_ENABLED)
            .remove(KEY_NOTIFICATIONS_ALLOWED)
            .remove(KEY_ACHIEVEMENT_NOTIFICATIONS_ENABLED)
            .remove(KEY_REMINDER_NOTIFICATIONS_ENABLED)
            .remove(KEY_NOTIFICATION_SOUND)
            .remove(KEY_VIBRATION_ENABLED)
            .remove(KEY_SILENT_HOURS)
            .remove(KEY_NUTRITION_DIET_PREFERENCES)
            .remove(KEY_NUTRITION_ALLERGIES)
            .remove(KEY_NUTRITION_DISLIKED_FOODS)
            .remove(KEY_NUTRITION_MEAL_ROUTINE)
            .remove(accountScopedKey(KEY_NUTRITION_DIET_PREFERENCES, uid))
            .remove(accountScopedKey(KEY_NUTRITION_ALLERGIES, uid))
            .remove(accountScopedKey(KEY_NUTRITION_DISLIKED_FOODS, uid))
            .remove(accountScopedKey(KEY_NUTRITION_MEAL_ROUTINE, uid))
            .remove(KEY_PENDING_HOME_TAB_ID)
            .remove(KEY_PENDING_PROFILE_SCREEN)
            .apply()
    }

    fun wrapContextWithSavedLanguage(context: Context): Context {
        val language = getLanguage(context)
        val locale = AppLocales.fromLanguageTag(languageTag(language))

        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(LocaleList(locale))
        } else {
            @Suppress("DEPRECATION")
            config.setLocale(locale)
        }

        return context.createConfigurationContext(config)
    }

    private fun nightModeFor(theme: FittoThemeMode): Int {
        return when (theme) {
            FittoThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            FittoThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            FittoThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
        }
    }

    private fun languageTag(language: ProfileLanguage): String {
        return when (language) {
            ProfileLanguage.TURKISH -> "tr"
            ProfileLanguage.ENGLISH -> "en"
        }
    }

    private tailrec fun Context.findActivity(): Activity? {
        return when (this) {
            is Activity -> this
            is ContextWrapper -> baseContext.findActivity()
            else -> null
        }
    }

    private fun Activity.rememberCurrentHomeScreenForSettingsRecreate() {
        (this as? MainActivity)?.rememberCurrentHomeScreenForSettingsRecreate()
    }

    private fun Activity.recreateWithoutAnimation() {
        window.decorView.post {
            recreate()
            ActivityTransitionCompat.disableAnimations(this)
        }
    }
}
