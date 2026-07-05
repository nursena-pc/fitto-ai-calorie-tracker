package com.example.fitto.feature_auth.register

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UsernameProfilePayloadFactoryTest {

    @Test
    fun create_buildsUsernameAndCompletedProfileDocuments() {
        val payload = UsernameProfilePayloadFactory().create(
            uid = "uid-1",
            email = "user@example.com",
            username = "fitto.user",
            displayName = "Fitto User",
            state = OnboardingState(
                name = "Nursena",
                height = 170,
                weight = 70,
                age = 25,
                gender = Gender.FEMALE,
                goalType = GoalType.LOSE_WEIGHT,
                barriers = listOf(BarrierType.TIME),
                targetWeight = 62,
                activityLevel = ActivityLevel.MODERATE
            ),
            dailyNeeds = DailyNeedsResult(
                bmr = 1500,
                maintenanceCalories = 2200,
                targetCalories = 1800,
                recommendedCalories = 1850,
                appliedCalories = 1800,
                isUserTargetApplied = true,
                weeklyWeightChangeKg = -0.4,
                estimatedWeeks = 20,
                requiredWeeklyChangeKg = -0.4,
                status = PlanStatus.HEALTHY
            ),
            now = 1234L
        )

        assertEquals("uid-1", payload.usernameDocument["uid"])
        assertEquals("fitto.user", payload.usernameDocument["usernameLowercase"])
        assertEquals(true, payload.userDocument["profileCompleted"])
        assertEquals("Nursena", payload.userDocument["name"])
        assertEquals(1234L, payload.userDocument["onboardingCompletedAt"])

        @Suppress("UNCHECKED_CAST")
        val targets = payload.userDocument["targets"] as Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val current = targets["current"] as Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val macros = current["macroTargets"] as Map<String, Any?>

        assertEquals(112, macros["proteinGrams"])
        assertEquals(50, macros["fatGrams"])
        assertEquals(226, macros["carbGrams"])
    }

    @Test
    fun create_withoutDailyNeeds_keepsTargetsNull() {
        val payload = UsernameProfilePayloadFactory().create(
            uid = "uid-1",
            email = "user@example.com",
            username = "fitto.user",
            displayName = "Fitto User",
            state = OnboardingState(),
            dailyNeeds = null,
            now = 1234L
        )

        assertNull(payload.userDocument["targets"])
        assertTrue(payload.userDocument.containsKey("targets"))
    }
}
