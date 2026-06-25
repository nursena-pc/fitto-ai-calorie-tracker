package com.example.fitto.feature_exercise

import com.example.fitto.feature_exercise.data.ExerciseLogLocalModel
import com.example.fitto.feature_exercise.model.ExerciseCategory
import com.example.fitto.feature_exercise.model.ExerciseUiModel

object ExerciseLegacyActivityResolver {

    fun resolveActivityForLog(
        log: ExerciseLogLocalModel,
        activities: List<ExerciseUiModel>,
        fallbackTitle: String,
        fallbackCategoryName: String,
        estimatedCaloriesText: String,
        currentTimeMillis: () -> Long = System::currentTimeMillis
    ): ExerciseUiModel {
        activities.firstOrNull { activity -> activity.id == log.exerciseId }?.let {
            return it
        }

        val normalizedLogTitle = ExerciseSearchTextNormalizer.normalize(log.exerciseTitle)

        activities.firstOrNull { activity ->
            ExerciseSearchTextNormalizer.normalize(activity.title) == normalizedLogTitle
        }?.let { return it }

        activities.firstOrNull { activity ->
            val normalizedTitle = ExerciseSearchTextNormalizer.normalize(activity.title)
            normalizedTitle.contains(normalizedLogTitle) ||
                normalizedLogTitle.contains(normalizedTitle)
        }?.let { return it }

        return createLegacyActivityFromLog(
            log = log,
            fallbackTitle = fallbackTitle,
            fallbackCategoryName = fallbackCategoryName,
            estimatedCaloriesText = estimatedCaloriesText,
            currentTimeMillis = currentTimeMillis
        )
    }

    fun resolveEmoji(
        exerciseId: String,
        activities: List<ExerciseUiModel>
    ): String {
        return activities
            .firstOrNull { activity -> activity.id == exerciseId }
            ?.iconEmoji
            ?: resolveLegacyEmoji(exerciseId)
    }

    private fun createLegacyActivityFromLog(
        log: ExerciseLogLocalModel,
        fallbackTitle: String,
        fallbackCategoryName: String,
        estimatedCaloriesText: String,
        currentTimeMillis: () -> Long
    ): ExerciseUiModel {
        return ExerciseUiModel(
            id = log.exerciseId.ifBlank { "legacy_${currentTimeMillis()}" },
            title = log.exerciseTitle.ifBlank { fallbackTitle },
            description = buildString {
                append(log.categoryName.ifBlank { fallbackCategoryName })
                append(" · ")
                append(estimatedCaloriesText)
            },
            category = resolveLegacyCategory(log),
            metValue = log.metValue.takeIf { metValue -> metValue > 0.0 } ?: DEFAULT_LEGACY_MET,
            defaultDurationMinutes = log.durationMinutes.coerceIn(MIN_DURATION, MAX_DURATION),
            iconEmoji = resolveLegacyEmoji(log.exerciseId),
            searchKeywords = listOf(
                log.exerciseTitle,
                log.categoryName,
                log.exerciseId
            ),
            sourceCode = "",
            sourceMajorHeading = "Legacy",
            sourceDescription = log.exerciseTitle
        )
    }

    private fun resolveLegacyCategory(log: ExerciseLogLocalModel): ExerciseCategory {
        val raw = ExerciseSearchTextNormalizer.normalize(
            "${log.categoryName} ${log.exerciseId} ${log.exerciseTitle}"
        )

        return when {
            raw.contains("kuvvet") ||
                raw.contains("strength") ||
                raw.contains("weight") ||
                raw.contains("bodyweight") ||
                raw.contains("calisthenics") -> ExerciseCategory.STRENGTH

            raw.contains("esneme") ||
                raw.contains("stretch") ||
                raw.contains("yoga") ||
                raw.contains("pilates") ||
                raw.contains("mobility") -> ExerciseCategory.STRETCHING

            raw.contains("yasam") ||
                raw.contains("lifestyle") ||
                raw.contains("cleaning") ||
                raw.contains("cooking") ||
                raw.contains("knitting") ||
                raw.contains("stairs") -> ExerciseCategory.LIFESTYLE

            else -> ExerciseCategory.CARDIO
        }
    }

    private fun resolveLegacyEmoji(exerciseId: String): String {
        val id = ExerciseSearchTextNormalizer.normalize(exerciseId)

        return when {
            id.contains("walk") || id.contains("walking") || id.contains("brisk") -> "🚶"
            id.contains("run") || id.contains("running") -> "🏃"
            id.contains("cycl") || id.contains("bicy") -> "🚴"
            id.contains("yoga") -> "🧘"
            id.contains("pilates") -> "🧎"
            id.contains("clean") -> "🧹"
            id.contains("cook") -> "🍳"
            id.contains("knit") -> "🧶"
            id.contains("stairs") -> "🪜"
            else -> "🔥"
        }
    }

    private const val DEFAULT_LEGACY_MET = 4.3
    private const val MIN_DURATION = 1
    private const val MAX_DURATION = 240
}
