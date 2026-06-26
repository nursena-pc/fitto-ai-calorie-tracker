package com.example.fitto.feature_daily

object DailyHealthActiveCaloriesResolver {

    fun resolve(
        isAvailable: Boolean,
        hasPermission: Boolean,
        activeCaloriesKcal: Int
    ): Int {
        return if (isAvailable && hasPermission) {
            activeCaloriesKcal.coerceAtLeast(0)
        } else {
            0
        }
    }
}
