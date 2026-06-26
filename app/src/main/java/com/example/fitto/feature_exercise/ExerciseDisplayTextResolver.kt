package com.example.fitto.feature_exercise

import com.example.fitto.feature_exercise.model.ExerciseUiModel

object ExerciseDisplayTextResolver {

    fun resolveTitle(activity: ExerciseUiModel): String {
        val raw = activity.title.trim()
        val normalized = ExerciseSearchTextNormalizer.normalize(raw)

        return when {
            normalized.contains("bicycl") ||
                normalized.contains("cycling") ||
                normalized.contains("bisiklet") -> {
                "Bisiklet • ${effortLabel(activity.metValue)}"
            }

            normalized.contains("running") ||
                normalized.contains("jogging") ||
                normalized.contains("kosu") ||
                normalized.contains("run") -> {
                "Koşu • ${effortLabel(activity.metValue)}"
            }

            normalized.contains("walking") ||
                normalized.contains("walk") ||
                normalized.contains("yuruyus") -> {
                "Yürüyüş • ${effortLabel(activity.metValue)}"
            }

            normalized.contains("swimming") ||
                normalized.contains("swim") ||
                normalized.contains("yuzme") -> {
                "Yüzme • ${effortLabel(activity.metValue)}"
            }

            normalized.contains("yoga") -> "Yoga"
            normalized.contains("pilates") -> "Pilates"
            normalized.contains("stretch") ||
                normalized.contains("stretching") ||
                normalized.contains("esneme") -> "Esneme"

            normalized.contains("cleaning") ||
                normalized.contains("housework") ||
                normalized.contains("temizlik") -> {
                "Ev temizliği • ${effortLabel(activity.metValue)}"
            }

            normalized.contains("gardening") ||
                normalized.contains("garden") ||
                normalized.contains("bahce") -> {
                "Bahçe işi • ${effortLabel(activity.metValue)}"
            }

            normalized.contains("shopping") ||
                normalized.contains("grocery") ||
                normalized.contains("market") -> {
                "Alışveriş yürüyüşü"
            }

            else -> raw
                .substringBefore(";")
                .substringBefore(" veya ")
                .substringBefore(",")
                .trim()
                .ifBlank { raw }
                .take(MAX_TITLE_LENGTH)
        }
    }

    fun resolveDescription(
        activity: ExerciseUiModel,
        categoryName: String
    ): String {
        return "$categoryName • ${resolveIntensityText(activity.metValue)}"
    }

    fun effortLabel(metValue: Double): String {
        return when {
            metValue < 2.5 -> "Hafif tempo"
            metValue < 4.5 -> "Orta tempo"
            metValue < 7.0 -> "Canlı tempo"
            metValue < 10.0 -> "Yüksek tempo"
            else -> "Çok yüksek tempo"
        }
    }

    fun resolveIntensityText(metValue: Double): String {
        return when {
            metValue < 2.5 -> "Düşük efor"
            metValue < 4.5 -> "Orta efor"
            metValue < 7.0 -> "Aktif tempo"
            metValue < 10.0 -> "Yoğun efor"
            else -> "Çok yoğun efor"
        }
    }

    private const val MAX_TITLE_LENGTH = 42
}
