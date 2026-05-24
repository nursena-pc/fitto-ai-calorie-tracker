package com.example.fitto.feature_dietplans.data

import android.content.Context
import com.example.fitto.data.local.session.UserSessionDataGuard
import com.example.fitto.R
import com.example.fitto.feature_dietplans.model.CustomDietPlanLocalModel
import com.example.fitto.feature_dietplans.model.DietPlanUiModel
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
class CustomDietPlanStore(context: Context) {

    private val preferences = UserSessionDataGuard.scopedSharedPreferences(context, PREF_NAME)

    fun getPlans(includeDrafts: Boolean = false): List<CustomDietPlanLocalModel> {
        val rawJson = preferences.getString(KEY_CUSTOM_PLANS, "[]").orEmpty()
        val array = runCatching { JSONArray(rawJson) }.getOrDefault(JSONArray())

        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val plan = item.toCustomPlanOrNull() ?: continue

                if (includeDrafts || !plan.isDraft) {
                    add(plan)
                }
            }
        }.sortedByDescending { it.updatedAt }
    }

    fun getPlan(planId: String): CustomDietPlanLocalModel? {
        return getPlans(includeDrafts = true).firstOrNull { it.id == planId }
    }
    fun hasPlanWithTitle(
        title: String,
        excludingPlanId: String? = null
    ): Boolean {
        val normalizedTitle = title.normalizePlanTitle()

        if (normalizedTitle.isBlank()) return false

        return getPlans(includeDrafts = true).any { plan ->
            plan.id != excludingPlanId &&
                    plan.title.normalizePlanTitle() == normalizedTitle
        }
    }
    fun savePlan(plan: CustomDietPlanLocalModel) {
        val existingPlans = getPlans(includeDrafts = true)
            .filterNot { it.id == plan.id }

        val updatedPlans = listOf(plan) + existingPlans

        val array = JSONArray()
        updatedPlans.forEach { customPlan ->
            array.put(customPlan.toJsonObject())
        }

        preferences.edit()
            .putString(KEY_CUSTOM_PLANS, array.toString())
            .apply()
    }

    fun deletePlan(planId: String) {
        val updatedPlans = getPlans(includeDrafts = true)
            .filterNot { it.id == planId }

        val array = JSONArray()
        updatedPlans.forEach { customPlan ->
            array.put(customPlan.toJsonObject())
        }

        preferences.edit()
            .putString(KEY_CUSTOM_PLANS, array.toString())
            .apply()
    }

    fun toDietPlanUiModel(
        plan: CustomDietPlanLocalModel,
        isFavorite: Boolean
    ): DietPlanUiModel {
        return DietPlanUiModel(
            id = plan.id,
            titleText = plan.title,
            descriptionText = plan.description.takeIf { it.isNotBlank() },
            kcalPerDay = plan.dailyCalories,
            durationDays = plan.durationDays,
            isFavorite = isFavorite,
            tagRes = if (plan.isDraft) {
                R.string.diet_plan_tag_draft
            } else {
                R.string.diet_plan_tag_custom
            },
            imageResId = R.drawable.my_plan,
            isDraft = plan.isDraft
        )
    }

    private fun CustomDietPlanLocalModel.toJsonObject(): JSONObject {
        return JSONObject()
            .put("id", id)
            .put("title", title)
            .put("description", description)
            .put("goalType", goalType)
            .put("dailyCalories", dailyCalories)
            .put("mealCount", mealCount)
            .put("durationDays", durationDays)
            .put("isPublic", isPublic)
            .put("preferences", JSONArray(preferences))
            .put("exclusions", JSONArray(exclusions))
            .put("mealOrder", JSONArray(mealOrder))
            .put("isDraft", isDraft)
            .put("createdAt", createdAt)
            .put("updatedAt", updatedAt)
    }

    private fun JSONObject.toCustomPlanOrNull(): CustomDietPlanLocalModel? {
        val id = optString("id")
        val title = optString("title")

        if (id.isBlank() || title.isBlank()) return null

        return CustomDietPlanLocalModel(
            id = id,
            title = title,
            description = optString("description"),
            goalType = optString("goalType"),
            dailyCalories = optInt("dailyCalories", 1800),
            mealCount = optInt("mealCount", 4),
            durationDays = optInt("durationDays", 30),
            isPublic = optBoolean("isPublic", false),
            preferences = optJSONArray("preferences").toStringList(),
            exclusions = optJSONArray("exclusions").toStringList(),
            mealOrder = optJSONArray("mealOrder").toStringList(),
            isDraft = optBoolean("isDraft", false),
            createdAt = optLong("createdAt", System.currentTimeMillis()),
            updatedAt = optLong("updatedAt", System.currentTimeMillis())
        )
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()

        return buildList {
            for (index in 0 until length()) {
                val value = optString(index).trim()
                if (value.isNotBlank()) {
                    add(value)
                }
            }
        }
    }
    private fun String.normalizePlanTitle(): String {
        return trim()
            .lowercase(Locale.ROOT)
            .replace("ı", "i")
            .replace("ğ", "g")
            .replace("ü", "u")
            .replace("ş", "s")
            .replace("ö", "o")
            .replace("ç", "c")
            .replace(Regex("\\s+"), " ")
    }
    companion object {
        const val CUSTOM_PLAN_ID_PREFIX = "custom_plan_"

        private const val PREF_NAME = "fitto_custom_diet_plans"
        private const val KEY_CUSTOM_PLANS = "custom_plans"
    }
}