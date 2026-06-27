package com.example.fitto.feature_dietplans

import com.example.fitto.feature_dietplans.DietPlanFilterOptions as FilterOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DietPlanFilterResultMapperTest {

    @Test
    fun `fromResult returns null when filters are cleared`() {
        val state = DietPlanFilterResultMapper.fromResult(
            clearFilters = true,
            goal = FilterOptions.GOAL_BALANCED,
            duration = 30,
            type = FilterOptions.TYPE_DASH,
            features = listOf(FilterOptions.FEATURE_EASY),
            minCalories = 1200,
            maxCalories = 2200
        )

        assertNull(state)
    }

    @Test
    fun `fromResult maps filter result values into state`() {
        val state = DietPlanFilterResultMapper.fromResult(
            clearFilters = false,
            goal = FilterOptions.GOAL_MUSCLE_GAIN,
            duration = 21,
            type = FilterOptions.TYPE_KETOGENIC,
            features = listOf(
                FilterOptions.FEATURE_HIGH_PROTEIN,
                FilterOptions.FEATURE_EASY
            ),
            minCalories = 1600,
            maxCalories = 2600
        )

        assertEquals(
            DietPlanFilterState(
                goal = FilterOptions.GOAL_MUSCLE_GAIN,
                duration = 21,
                type = FilterOptions.TYPE_KETOGENIC,
                features = setOf(
                    FilterOptions.FEATURE_HIGH_PROTEIN,
                    FilterOptions.FEATURE_EASY
                ),
                minCalories = 1600,
                maxCalories = 2600
            ),
            state
        )
    }

    @Test
    fun `fromResult handles missing optional values`() {
        val state = DietPlanFilterResultMapper.fromResult(
            clearFilters = false,
            goal = null,
            duration = 0,
            type = null,
            features = null,
            minCalories = 0,
            maxCalories = Int.MAX_VALUE
        )

        assertEquals(
            DietPlanFilterState(
                goal = "",
                duration = 0,
                type = "",
                features = emptySet(),
                minCalories = 0,
                maxCalories = Int.MAX_VALUE
            ),
            state
        )
    }
}
