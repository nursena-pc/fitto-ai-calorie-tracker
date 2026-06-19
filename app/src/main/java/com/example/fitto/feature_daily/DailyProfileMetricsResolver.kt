package com.example.fitto.feature_daily

object DailyProfileMetricsResolver {

    fun resolveWeightKg(profile: Any?): Double? {
        if (profile == null) return null

        return WEIGHT_FIELD_NAMES.firstNotNullOfOrNull { fieldName ->
            profile.readNumberProperty(fieldName)
                ?.takeIf { weightKg -> weightKg in MIN_WEIGHT_KG..MAX_WEIGHT_KG }
        }
    }

    fun resolveWaterGoalMl(profile: Any?): Int {
        val explicitGoal = WATER_GOAL_FIELD_NAMES.firstNotNullOfOrNull { fieldName ->
            profile?.readNumberProperty(fieldName)?.toInt()
        }

        if (explicitGoal != null && explicitGoal > 0) {
            return DailyMetricCalculator.roundUpToBottleSize(explicitGoal)
        }

        val weightKg = resolveWeightKg(profile) ?: DEFAULT_WEIGHT_KG
        val calculatedGoal = (weightKg * WATER_ML_PER_KG).toInt()
            .coerceIn(MIN_WATER_GOAL_ML, MAX_WATER_GOAL_ML)

        return DailyMetricCalculator.roundUpToBottleSize(calculatedGoal)
    }

    private fun Any.readNumberProperty(name: String): Double? {
        val fieldValue = runCatching {
            javaClass.getDeclaredField(name).apply {
                isAccessible = true
            }.get(this)
        }.getOrNull()

        return when (fieldValue) {
            is Number -> fieldValue.toDouble()
            is String -> fieldValue.replace(",", ".").toDoubleOrNull()
            else -> null
        }
    }

    private val WEIGHT_FIELD_NAMES = listOf(
        "weightKg",
        "currentWeightKg",
        "weight",
        "currentWeight",
        "initialWeightKg",
        "initialWeight"
    )

    private val WATER_GOAL_FIELD_NAMES = listOf(
        "waterGoalMl",
        "dailyWaterGoalMl",
        "waterTargetMl",
        "waterIntakeGoalMl"
    )

    private const val MIN_WEIGHT_KG = 25.0
    private const val MAX_WEIGHT_KG = 300.0
    private const val DEFAULT_WEIGHT_KG = 70.0
    private const val WATER_ML_PER_KG = 35.0
    private const val MIN_WATER_GOAL_ML = 1500
    private const val MAX_WATER_GOAL_ML = 4500
}
