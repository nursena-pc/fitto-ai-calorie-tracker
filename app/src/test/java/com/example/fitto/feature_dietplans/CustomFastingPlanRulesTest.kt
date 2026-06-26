package com.example.fitto.feature_dietplans

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class CustomFastingPlanRulesTest {

    @Test
    fun sanitizeFastingHours_keepsValueInSupportedRange() {
        assertEquals(12, CustomFastingPlanRules.sanitizeFastingHours(10))
        assertEquals(16, CustomFastingPlanRules.sanitizeFastingHours(16))
        assertEquals(20, CustomFastingPlanRules.sanitizeFastingHours(22))
    }

    @Test
    fun eatingHoursFor_usesSanitizedFastingHours() {
        assertEquals(12, CustomFastingPlanRules.eatingHoursFor(10))
        assertEquals(8, CustomFastingPlanRules.eatingHoursFor(16))
        assertEquals(4, CustomFastingPlanRules.eatingHoursFor(22))
    }

    @Test
    fun resolveSchedule_buildsDefaultEveningSchedule() {
        val schedule = CustomFastingPlanRules.resolveSchedule(18, Locale.US)

        assertEquals(18, schedule.fastingHours)
        assertEquals(6, schedule.eatingHours)
        assertEquals(20, schedule.fastingStartHour)
        assertEquals(14, schedule.eatingStartHour)
        assertEquals("20:00", schedule.fastingStartText)
        assertEquals("14:00", schedule.eatingStartText)
        assertEquals("20:00", schedule.fastingEndText)
    }

    @Test
    fun formatHour_normalizesOutOfRangeHours() {
        assertEquals("23:00", CustomFastingPlanRules.formatHour(-1, Locale.US))
        assertEquals("01:00", CustomFastingPlanRules.formatHour(25, Locale.US))
    }
}
