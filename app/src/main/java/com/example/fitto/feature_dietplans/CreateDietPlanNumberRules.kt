package com.example.fitto.feature_dietplans

object CreateDietPlanNumberRules {
    val CALORIE_RANGE = 800..5000
    const val CALORIE_STEP = 50

    val DURATION_RANGE = 1..365
    const val DURATION_STEP = 1

    fun decreaseCalories(value: Int): Int {
        return decrease(value, CALORIE_STEP, CALORIE_RANGE)
    }

    fun increaseCalories(value: Int): Int {
        return increase(value, CALORIE_STEP, CALORIE_RANGE)
    }

    fun decreaseDuration(value: Int): Int {
        return decrease(value, DURATION_STEP, DURATION_RANGE)
    }

    fun increaseDuration(value: Int): Int {
        return increase(value, DURATION_STEP, DURATION_RANGE)
    }

    private fun decrease(value: Int, step: Int, range: IntRange): Int {
        return (value - step).coerceAtLeast(range.first)
    }

    private fun increase(value: Int, step: Int, range: IntRange): Int {
        return (value + step).coerceAtMost(range.last)
    }
}
