package com.example.fitto.feature_exercise.data

import android.content.Context
import android.util.Log
import com.example.fitto.feature_exercise.model.ExerciseCategory
import com.example.fitto.feature_exercise.model.ExerciseUiModel
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.util.Locale

object ExerciseCatalog {

    private const val TAG = "ExerciseCatalog"
    private const val ASSET_FILE_NAME = "exercise_catalog.json"

    private var cachedActivities: List<ExerciseUiModel>? = null

    fun load(
        context: Context,
        forceReload: Boolean = false
    ): List<ExerciseUiModel> {
        if (!forceReload) {
            cachedActivities?.let { return it }
        }

        val activities = runCatching {
            val json = context.assets.open(ASSET_FILE_NAME)
                .bufferedReader()
                .use { it.readText() }

            val array = readActivitiesArray(json)
            val isTurkish = Locale.getDefault().language.equals("tr", ignoreCase = true)

            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val activity = item.toExerciseUiModel(isTurkish)

                    if (activity.id.isNotBlank() && activity.title.isNotBlank()) {
                        add(activity)
                    }
                }
            }
        }.onFailure { error ->
            Log.e(TAG, "Exercise catalog could not be loaded from assets/$ASSET_FILE_NAME", error)
        }.getOrDefault(emptyList())

        Log.d(TAG, "Loaded exercise count: ${activities.size}")

        val finalActivities = ensureStretchingFallback(activities)

        cachedActivities = finalActivities
        return finalActivities
    }
    private fun ensureStretchingFallback(
        activities: List<ExerciseUiModel>
    ): List<ExerciseUiModel> {
        if (activities.any { it.category == ExerciseCategory.STRETCHING }) {
            return activities
        }

        return activities + listOf(
            ExerciseUiModel(
                id = "fallback_yoga",
                title = "Yoga",
                description = "Esneme • Düşük efor",
                category = ExerciseCategory.STRETCHING,
                metValue = 2.3,
                defaultDurationMinutes = 30,
                iconEmoji = "🧘",
                searchKeywords = listOf("yoga", "esneme", "stretching", "mobility"),
                sourceCode = "fitto_fallback",
                sourceMajorHeading = "Stretching",
                sourceDescription = "Yoga"
            ),
            ExerciseUiModel(
                id = "fallback_pilates",
                title = "Pilates",
                description = "Esneme • Orta efor",
                category = ExerciseCategory.STRETCHING,
                metValue = 3.0,
                defaultDurationMinutes = 30,
                iconEmoji = "🧎",
                searchKeywords = listOf("pilates", "core", "esneme"),
                sourceCode = "fitto_fallback",
                sourceMajorHeading = "Stretching",
                sourceDescription = "Pilates"
            ),
            ExerciseUiModel(
                id = "fallback_mobility",
                title = "Mobilite",
                description = "Eklem hareket açıklığı • Hafif tempo",
                category = ExerciseCategory.STRETCHING,
                metValue = 2.0,
                defaultDurationMinutes = 20,
                iconEmoji = "🌀",
                searchKeywords = listOf("mobilite", "mobility", "esneme", "stretch"),
                sourceCode = "fitto_fallback",
                sourceMajorHeading = "Stretching",
                sourceDescription = "Mobility"
            ),
            ExerciseUiModel(
                id = "fallback_stretching",
                title = "Tüm Vücut Esneme",
                description = "Esneme • Düşük efor",
                category = ExerciseCategory.STRETCHING,
                metValue = 2.3,
                defaultDurationMinutes = 20,
                iconEmoji = "🤸",
                searchKeywords = listOf("esneme", "stretch", "stretching", "full body"),
                sourceCode = "fitto_fallback",
                sourceMajorHeading = "Stretching",
                sourceDescription = "Full body stretching"
            )
        )
    }

    fun findById(context: Context, id: String): ExerciseUiModel? {
        return load(context).firstOrNull { it.id == id }
    }

    private fun readActivitiesArray(json: String): JSONArray {
        val root = JSONTokener(json).nextValue()

        return when (root) {
            is JSONArray -> root

            is JSONObject -> {
                root.optJSONArray("activities")
                    ?: root.optJSONArray("exercises")
                    ?: root.optJSONArray("data")
                    ?: JSONArray()
            }

            else -> JSONArray()
        }
    }

    private fun JSONObject.toExerciseUiModel(isTurkish: Boolean): ExerciseUiModel {
        val titleTr = optString("titleTr")
            .ifBlank { optString("nameTr") }
            .ifBlank { optString("title") }

        val titleEn = optString("titleEn")
            .ifBlank { optString("nameEn") }
            .ifBlank { optString("title") }

        val descriptionTr = optString("descriptionTr")
            .ifBlank { optString("descTr") }
            .ifBlank { optString("sourceDescription") }

        val descriptionEn = optString("descriptionEn")
            .ifBlank { optString("descEn") }
            .ifBlank { optString("sourceDescription") }

        val title = if (isTurkish) {
            titleTr.ifBlank { titleEn }
        } else {
            titleEn.ifBlank { titleTr }
        }

        val description = if (isTurkish) {
            descriptionTr.ifBlank { descriptionEn }
        } else {
            descriptionEn.ifBlank { descriptionTr }
        }

        val sourceMajorHeading = optString("sourceMajorHeading")
            .ifBlank { optString("majorHeading") }

        val category = resolveCategory(
            categoryValue = optString("category"),
            sourceMajorHeading = sourceMajorHeading,
            title = title,
            description = description
        )

        val keywords = buildList {
            add(titleTr)
            add(titleEn)
            add(descriptionTr)
            add(descriptionEn)
            add(sourceMajorHeading)
            add(optString("sourceDescription"))
            addAll(readStringList("keywordsTr"))
            addAll(readStringList("keywordsEn"))
            addAll(readStringList("searchKeywords"))
            addAll(readStringList("keywords"))
        }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase(Locale.getDefault()) }

        return ExerciseUiModel(
            id = optString("id").ifBlank {
                title.lowercase(Locale.US)
                    .replace("[^a-z0-9]+".toRegex(), "_")
                    .trim('_')
            },
            title = title,
            description = description,
            category = category,
            metValue = optDouble("metValue", optDouble("met", 1.0)).coerceAtLeast(1.0),
            defaultDurationMinutes = optInt("defaultDurationMinutes", 30).coerceIn(1, 240),
            iconEmoji = optString("iconEmoji", optString("emoji", "✨")),
            searchKeywords = keywords,
            sourceCode = optString("sourceCode").ifBlank { optString("code") },
            sourceMajorHeading = sourceMajorHeading,
            sourceDescription = optString("sourceDescription")
        )
    }

    private fun JSONObject.readStringList(key: String): List<String> {
        val value = opt(key) ?: return emptyList()

        return when (value) {
            is JSONArray -> buildList {
                for (index in 0 until value.length()) {
                    val item = value.optString(index)
                    if (item.isNotBlank()) add(item)
                }
            }

            is String -> value
                .split(",", "·", ";")
                .map { it.trim() }
                .filter { it.isNotBlank() }

            else -> emptyList()
        }
    }

    private fun resolveCategory(
        categoryValue: String,
        sourceMajorHeading: String,
        title: String,
        description: String
    ): ExerciseCategory {
        val raw = "$categoryValue $sourceMajorHeading $title $description"
            .uppercase(Locale.US)

        return when {
            raw.contains("STRETCH") ||
                    raw.contains("STRETCHING") ||
                    raw.contains("YOGA") ||
                    raw.contains("PILATES") ||
                    raw.contains("MOBILITY") ||
                    raw.contains("TAI CHI") ||
                    raw.contains("ESNEME") ||
                    raw.contains("MOBİLİTE") ||
                    raw.contains("MOBILITE") -> ExerciseCategory.STRETCHING

            raw.contains("HOME") ||
                    raw.contains("OCCUPATION") ||
                    raw.contains("LAWN") ||
                    raw.contains("GARDEN") ||
                    raw.contains("CLEANING") ||
                    raw.contains("COOKING") ||
                    raw.contains("LIFESTYLE") ||
                    raw.contains("HOUSEWORK") ||
                    raw.contains("YAŞAM") ||
                    raw.contains("YASAM") -> ExerciseCategory.LIFESTYLE

            raw.contains("STRENGTH") ||
                    raw.contains("CONDITIONING") ||
                    raw.contains("WEIGHT") ||
                    raw.contains("RESISTANCE") ||
                    raw.contains("CALISTHENICS") ||
                    raw.contains("KUVVET") -> ExerciseCategory.STRENGTH

            else -> ExerciseCategory.CARDIO
        }
    }
}