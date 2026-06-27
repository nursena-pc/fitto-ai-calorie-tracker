package com.example.fitto.feature_dietplans

import com.example.fitto.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomDietPlanDetailButtonStateResolverTest {

    @Test
    fun `save button uses saved label when plan is favorite`() {
        val state = CustomDietPlanDetailButtonStateResolver.save(isSaved = true)

        assertEquals(R.string.custom_diet_detail_saved, state.textRes)
        assertTrue(state.isEnabled)
        assertEquals(CustomDietPlanDetailButtonStateResolver.ENABLED_ALPHA, state.alpha, 0f)
    }

    @Test
    fun `save button uses save label when plan is not favorite`() {
        val state = CustomDietPlanDetailButtonStateResolver.save(isSaved = false)

        assertEquals(R.string.diet_plan_detail_save_plan, state.textRes)
        assertTrue(state.isEnabled)
        assertEquals(CustomDietPlanDetailButtonStateResolver.ENABLED_ALPHA, state.alpha, 0f)
    }

    @Test
    fun `apply button is enabled when plan is not active`() {
        val state = CustomDietPlanDetailButtonStateResolver.apply(isActive = false)

        assertEquals(R.string.diet_plans_apply, state.textRes)
        assertTrue(state.isEnabled)
        assertEquals(CustomDietPlanDetailButtonStateResolver.ENABLED_ALPHA, state.alpha, 0f)
    }

    @Test
    fun `apply button is disabled when plan is already active`() {
        val state = CustomDietPlanDetailButtonStateResolver.apply(isActive = true)

        assertEquals(R.string.diet_plans_applied, state.textRes)
        assertFalse(state.isEnabled)
        assertEquals(CustomDietPlanDetailButtonStateResolver.DISABLED_ALPHA, state.alpha, 0f)
    }
}
