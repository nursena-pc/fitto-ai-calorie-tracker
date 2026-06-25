package com.example.fitto.feature_daily

import org.junit.Assert.assertEquals
import org.junit.Test

class DailyHealthActiveCaloriesResolverTest {

    @Test
    fun resolve_returnsActiveCaloriesOnlyWhenAvailableAndPermitted() {
        assertEquals(
            350,
            DailyHealthActiveCaloriesResolver.resolve(
                isAvailable = true,
                hasPermission = true,
                activeCaloriesKcal = 350
            )
        )
        assertEquals(
            0,
            DailyHealthActiveCaloriesResolver.resolve(
                isAvailable = true,
                hasPermission = false,
                activeCaloriesKcal = 350
            )
        )
        assertEquals(
            0,
            DailyHealthActiveCaloriesResolver.resolve(
                isAvailable = false,
                hasPermission = true,
                activeCaloriesKcal = 350
            )
        )
    }

    @Test
    fun resolve_clampsNegativeActiveCalories() {
        assertEquals(
            0,
            DailyHealthActiveCaloriesResolver.resolve(
                isAvailable = true,
                hasPermission = true,
                activeCaloriesKcal = -10
            )
        )
    }
}
