package com.example.fitto.feature_home.presentation.social

import android.content.Context
import com.example.fitto.R
import java.util.Locale

object SocialTextLocalizer {

    fun localizeTag(context: Context, value: String): String {
        return localizeKnownSocialText(context, value) ?: value
    }

    fun localizeMealInfo(context: Context, value: String?): String? {
        if (value.isNullOrBlank()) return null

        return value
            .split(MEAL_INFO_SEPARATOR)
            .map { part -> part.trim() }
            .filter { part -> part.isNotBlank() }
            .joinToString(MEAL_INFO_SEPARATOR) { part ->
                localizeKnownSocialText(context, part) ?: part
            }
            .takeIf { it.isNotBlank() }
    }

    private fun localizeKnownSocialText(context: Context, value: String): String? {
        return when (normalize(value)) {
            // Create-post default tags
            "ev yapimi", "homemade" -> context.getString(R.string.create_post_home_made)
            "aksam ogunu", "aksam yemegi", "dinner", "dinner meal" -> context.getString(R.string.create_post_evening_meal)
            "dengeli", "balanced" -> context.getString(R.string.create_post_balanced)
            "hafif", "light" -> context.getString(R.string.create_post_light)
            "proteinli", "high protein", "high-protein" -> context.getString(R.string.create_post_high_protein)
            "fit tarif", "fit recipe" -> context.getString(R.string.create_post_fit_recipe)

            // Meal tags
            "kahvalti", "breakfast" -> context.getString(R.string.create_post_breakfast)
            "ogle ogunu", "ogle yemegi", "lunch" -> context.getString(R.string.create_post_lunch)
            "ara ogun", "snack" -> context.getString(R.string.create_post_snack)

            // Time tags
            "sabah", "morning" -> context.getString(R.string.create_post_morning)
            "oglen", "noon" -> context.getString(R.string.create_post_noon)
            "aksam", "evening" -> context.getString(R.string.create_post_evening)
            "gece", "night" -> context.getString(R.string.create_post_night)

            // Custom diet-plan social share tags / meal info
            "kisisel", "personal" -> context.getString(R.string.diet_plan_tag_custom)
            "kisisel plan", "personal plan", "custom plan" -> context.getString(R.string.custom_diet_plan_active_badge)
            "kisisel diyet plani", "personal diet plan", "custom diet plan" -> {
                context.getString(R.string.create_diet_plan_social_share_meal_info)
            }

            // Custom diet-plan goals saved as social post tags
            "kilo verme", "weight loss" -> context.getString(R.string.create_diet_plan_goal_weight_loss)
            "kilo alma", "weight gain" -> context.getString(R.string.create_diet_plan_goal_weight_gain)
            "kas kazanimi", "muscle gain" -> context.getString(R.string.create_diet_plan_goal_muscle)
            "form koruma", "maintain shape", "maintenance", "maintain" -> {
                context.getString(R.string.create_diet_plan_goal_maintain)
            }
            "dengeli beslenme", "balanced eating" -> context.getString(R.string.create_diet_plan_goal_balanced)

            else -> null
        }
    }

    private fun normalize(value: String): String {
        return value
            .trim()
            .lowercase(Locale("tr", "TR"))
            .replace('ı', 'i')
            .replace('ğ', 'g')
            .replace('ü', 'u')
            .replace('ş', 's')
            .replace('ö', 'o')
            .replace('ç', 'c')
            .replace(Regex("\\s+"), " ")
    }

    private const val MEAL_INFO_SEPARATOR = " • "
}
