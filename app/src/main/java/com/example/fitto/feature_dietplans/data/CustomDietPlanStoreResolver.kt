package com.example.fitto.feature_dietplans.data

import com.example.fitto.R
import com.example.fitto.feature_dietplans.model.CustomDietPlanLocalModel
import com.example.fitto.feature_dietplans.model.DietPlanUiModel
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

object CustomDietPlanStoreResolver {

    fun decodePlans(
        rawJson: String,
        includeDrafts: Boolean = false,
        nowProvider: () -> Long = { System.currentTimeMillis() }
    ): List<CustomDietPlanLocalModel> {
        val array = runCatching { JSONArray(rawJson) }.getOrDefault(JSONArray())

        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val plan = item.toCustomPlanOrNull(nowProvider) ?: continue

                if (includeDrafts || !plan.isDraft) {
                    add(plan)
                }
            }
        }.sortedByDescending { it.updatedAt }
    }

    fun encodePlans(plans: List<CustomDietPlanLocalModel>): String {
        val array = JSONArray()
        plans.forEach { customPlan ->
            array.put(customPlan.toJsonObject())
        }
        return array.toString()
    }

    fun savePlan(
        existingPlans: List<CustomDietPlanLocalModel>,
        plan: CustomDietPlanLocalModel
    ): List<CustomDietPlanLocalModel> {
        return listOf(plan) + existingPlans.filterNot { it.id == plan.id }
    }

    fun deletePlan(
        existingPlans: List<CustomDietPlanLocalModel>,
        planId: String
    ): List<CustomDietPlanLocalModel> {
        return existingPlans.filterNot { it.id == planId }
    }

    fun hasPlanWithTitle(
        plans: List<CustomDietPlanLocalModel>,
        title: String,
        excludingPlanId: String? = null
    ): Boolean {
        val normalizedTitle = title.normalizePlanTitle()

        if (normalizedTitle.isBlank()) return false

        return plans.any { plan ->
            plan.id != excludingPlanId &&
                    plan.title.normalizePlanTitle() == normalizedTitle
        }
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

    fun String.normalizePlanTitle(): String {
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

    private fun JSONObject.toCustomPlanOrNull(
        nowProvider: () -> Long
    ): CustomDietPlanLocalModel? {
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
            createdAt = optLong("createdAt", nowProvider()),
            updatedAt = optLong("updatedAt", nowProvider())
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
}
