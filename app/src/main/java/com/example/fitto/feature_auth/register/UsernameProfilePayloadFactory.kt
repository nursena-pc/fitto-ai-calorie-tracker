package com.example.fitto.feature_auth.register

import java.util.Locale
import kotlin.math.roundToInt

data class UsernameProfilePayload(
    val usernameDocument: Map<String, Any?>,
    val userDocument: Map<String, Any?>
)

class UsernameProfilePayloadFactory {

    fun create(
        uid: String,
        email: String,
        username: String,
        displayName: String,
        state: OnboardingState,
        dailyNeeds: DailyNeedsResult?,
        now: Long
    ): UsernameProfilePayload {
        val proteinGrams = (state.weight * 1.6).roundToInt().coerceAtLeast(0)
        val fatGrams = dailyNeeds?.let { result ->
            (result.appliedCalories * 0.25 / 9.0).roundToInt().coerceAtLeast(0)
        }
        val carbGrams = dailyNeeds?.let { result ->
            val caloriesAfterProteinAndFat =
                result.appliedCalories - (proteinGrams * 4) - ((fatGrams ?: 0) * 9)
            (caloriesAfterProteinAndFat / 4.0).roundToInt().coerceAtLeast(0)
        }
        val normalizedUsername = username.lowercase(Locale.ROOT)

        return UsernameProfilePayload(
            usernameDocument = mapOf(
                "uid" to uid,
                "email" to email,
                "username" to username,
                "usernameLowercase" to normalizedUsername,
                "updatedAt" to now
            ),
            userDocument = mapOf(
                "uid" to uid,
                "email" to email,
                "realName" to displayName,
                "userName" to username,
                "usernameLowercase" to normalizedUsername,
                "bio" to "",
                "photoUrl" to null,
                "followersCount" to 0,
                "followingCount" to 0,
                "profileCompleted" to true,
                "onboardingSaved" to true,
                "name" to state.name.ifBlank { displayName },
                "heightCm" to state.height,
                "weightKg" to state.weight,
                "age" to state.age,
                "gender" to state.gender?.name,
                "goalType" to state.goalType?.name,
                "barriers" to state.barriers.map { barrier -> barrier.name },
                "otherBarrierText" to state.otherBarrierText,
                "targetWeightKg" to state.targetWeight.takeIf { it > 0 },
                "eventTimeMillis" to state.eventTimeMillis,
                "activityLevel" to state.activityLevel?.name,
                "onboardingCompleted" to true,
                "onboardingCompletedAt" to now,
                "targets" to dailyNeeds?.let { result ->
                    mapOf(
                        "current" to mapOf(
                            "bmr" to result.bmr,
                            "maintenanceCalories" to result.maintenanceCalories,
                            "targetCalories" to result.targetCalories,
                            "recommendedCalories" to result.recommendedCalories,
                            "appliedCalories" to result.appliedCalories,
                            "isUserTargetApplied" to result.isUserTargetApplied,
                            "weeklyWeightChangeKg" to result.weeklyWeightChangeKg,
                            "estimatedWeeks" to result.estimatedWeeks,
                            "requiredWeeklyChangeKg" to result.requiredWeeklyChangeKg,
                            "planStatus" to result.status.name,
                            "macroTargets" to mapOf(
                                "proteinGrams" to proteinGrams,
                                "fatGrams" to fatGrams,
                                "carbGrams" to carbGrams
                            ),
                            "updatedAt" to now
                        )
                    )
                },
                "updatedAt" to now
            )
        )
    }
}
