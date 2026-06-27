package com.example.fitto.feature_daily

import com.example.fitto.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyActivityRecognitionPermissionResultResolverTest {

    @Test
    fun resolve_grantedStartsStepCounter() {
        val effect = DailyActivityRecognitionPermissionResultResolver.resolve(granted = true)

        assertEquals(R.string.step_counter_permission_granted, effect.messageRes)
        assertTrue(effect.shouldStartStepCounter)
        assertFalse(effect.shouldRenderSteps)
    }

    @Test
    fun resolve_deniedRendersExistingSteps() {
        val effect = DailyActivityRecognitionPermissionResultResolver.resolve(granted = false)

        assertEquals(R.string.step_counter_permission_denied, effect.messageRes)
        assertFalse(effect.shouldStartStepCounter)
        assertTrue(effect.shouldRenderSteps)
    }
}
