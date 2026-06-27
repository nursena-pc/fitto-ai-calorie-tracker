package com.example.fitto.feature_dietplans

import com.example.fitto.feature_dietplans.DietPlanFilterOptions as FilterOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DietPlanFilterStateStoreTest {

    @Test
    fun `default state uses filter defaults`() {
        val state = DietPlanFilterStateStore().current()

        assertEquals(FilterOptions.DEFAULT_GOAL, state.goal)
        assertEquals(FilterOptions.DEFAULT_DURATION, state.duration)
        assertEquals(FilterOptions.DEFAULT_TYPE, state.type)
        assertEquals(FilterOptions.DEFAULT_FEATURES, state.features)
        assertEquals(FilterOptions.DEFAULT_MIN_CALORIES, state.minCalories)
        assertEquals(FilterOptions.DEFAULT_MAX_CALORIES, state.maxCalories)
    }

    @Test
    fun `selection setters update current filter state`() {
        val store = DietPlanFilterStateStore()

        store.selectGoal(FilterOptions.GOAL_MUSCLE_GAIN)
        store.selectDuration(30)
        store.selectType(FilterOptions.TYPE_KETOGENIC)
        store.setCalorieRange(1600, 2400)

        val state = store.current()
        assertEquals(FilterOptions.GOAL_MUSCLE_GAIN, state.goal)
        assertEquals(30, state.duration)
        assertEquals(FilterOptions.TYPE_KETOGENIC, state.type)
        assertEquals(1600, state.minCalories)
        assertEquals(2400, state.maxCalories)
    }

    @Test
    fun `toggleFeature adds and removes feature`() {
        val store = DietPlanFilterStateStore()

        val selected = store.toggleFeature(FilterOptions.FEATURE_HIGH_PROTEIN)
        assertTrue(selected)
        assertTrue(FilterOptions.FEATURE_HIGH_PROTEIN in store.current().features)

        val removed = store.toggleFeature(FilterOptions.FEATURE_HIGH_PROTEIN)
        assertFalse(removed)
        assertFalse(FilterOptions.FEATURE_HIGH_PROTEIN in store.current().features)
    }

    @Test
    fun `reset restores default filter state`() {
        val store = DietPlanFilterStateStore()
        store.selectGoal(FilterOptions.GOAL_BALANCED)
        store.selectDuration(7)
        store.selectType(FilterOptions.TYPE_DASH)
        store.toggleFeature(FilterOptions.FEATURE_EASY)
        store.setCalorieRange(1000, 3000)

        val resetState = store.reset()

        assertEquals(DietPlanFilterStateStore.defaultState(), resetState)
        assertEquals(DietPlanFilterStateStore.defaultState(), store.current())
    }

    @Test
    fun `initial state is copied before mutation`() {
        val features = mutableSetOf(FilterOptions.FEATURE_EASY)
        val store = DietPlanFilterStateStore(
            DietPlanFilterState(
                goal = FilterOptions.GOAL_BALANCED,
                duration = 21,
                type = FilterOptions.TYPE_DASH,
                features = features,
                minCalories = 1200,
                maxCalories = 2100
            )
        )

        features.add(FilterOptions.FEATURE_LOW_SODIUM)

        assertEquals(setOf(FilterOptions.FEATURE_EASY), store.current().features)
    }
}
