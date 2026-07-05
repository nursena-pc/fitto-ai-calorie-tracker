package com.example.fitto.feature_auth.register

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingPlanEngineTest {

    private val engine = OnboardingPlanEngine(nowMillis = { NOW_MILLIS })

    @Test
    fun basicInfoAndCalculationReadiness_requireExpectedFields() {
        val basicState = baseState(goalType = null, activityLevel = null)

        assertTrue(engine.hasBasicUserInfo(basicState))
        assertFalse(engine.canCalculateDailyNeeds(basicState))
        assertFalse(
            engine.canCalculateDailyNeeds(
                basicState.copy(
                    goalType = GoalType.LOSE_WEIGHT,
                    activityLevel = ActivityLevel.LOW,
                    targetWeight = 0
                )
            )
        )
        assertTrue(
            engine.canCalculateDailyNeeds(
                basicState.copy(
                    goalType = GoalType.LOSE_WEIGHT,
                    activityLevel = ActivityLevel.LOW,
                    targetWeight = 70
                )
            )
        )
    }

    @Test
    fun maintainGoal_usesMaintenanceCaloriesAndNoWeightChangeStatus() {
        val result = engine.calculateDailyNeeds(
            baseState(goalType = GoalType.MAINTAIN_WEIGHT)
        )

        assertEquals(1780, result.bmr)
        assertEquals(2136, result.maintenanceCalories)
        assertEquals(2136, result.targetCalories)
        assertEquals(2136, result.appliedCalories)
        assertEquals(0.0, result.weeklyWeightChangeKg, 0.0)
        assertNull(result.estimatedWeeks)
        assertEquals(PlanStatus.NO_WEIGHT_CHANGE, result.status)
    }

    @Test
    fun weightLossWithoutEvent_usesRecommendedWeeklyChange() {
        val result = engine.calculateDailyNeeds(
            baseState(
                gender = Gender.FEMALE,
                height = 165,
                weight = 70,
                targetWeight = 60,
                goalType = GoalType.LOSE_WEIGHT,
                activityLevel = ActivityLevel.MODERATE
            )
        )

        assertEquals(1420, result.bmr)
        assertEquals(1953, result.maintenanceCalories)
        assertEquals(1403, result.targetCalories)
        assertEquals(1453, result.recommendedCalories)
        assertEquals(1403, result.appliedCalories)
        assertEquals(-0.5, result.weeklyWeightChangeKg, 0.0)
        assertEquals(20, result.estimatedWeeks)
        assertNull(result.requiredWeeklyChangeKg)
        assertEquals(PlanStatus.HEALTHY, result.status)
    }

    @Test
    fun aggressiveEventPlan_fallsBackToRecommendedCaloriesAndTimeline() {
        val result = engine.calculateDailyNeeds(
            baseState(
                targetWeight = 70,
                goalType = GoalType.LOSE_WEIGHT,
                eventTimeMillis = NOW_MILLIS + days(70)
            )
        )

        assertEquals(1.0, result.requiredWeeklyChangeKg ?: 0.0, 0.0001)
        assertEquals(PlanStatus.AGGRESSIVE, result.status)
        assertFalse(result.isUserTargetApplied)
        assertEquals(1636, result.appliedCalories)
        assertEquals(-0.5, result.weeklyWeightChangeKg, 0.0)
        assertEquals(20, result.estimatedWeeks)
    }

    @Test
    fun unsafeEventPlan_usesSafeFallback() {
        val result = engine.calculateDailyNeeds(
            baseState(
                targetWeight = 70,
                goalType = GoalType.LOSE_WEIGHT,
                eventTimeMillis = NOW_MILLIS + days(35)
            )
        )

        assertEquals(2.0, result.requiredWeeklyChangeKg ?: 0.0, 0.0001)
        assertEquals(PlanStatus.NOT_RECOMMENDED, result.status)
        assertFalse(result.isUserTargetApplied)
        assertEquals(1636, result.appliedCalories)
        assertEquals(-0.5, result.weeklyWeightChangeKg, 0.0)
    }

    @Test
    fun weightGainWithoutEvent_usesGainRecommendation() {
        val result = engine.calculateDailyNeeds(
            baseState(
                targetWeight = 90,
                goalType = GoalType.GAIN_WEIGHT
            )
        )

        assertEquals(2466, result.targetCalories)
        assertEquals(2436, result.recommendedCalories)
        assertEquals(2466, result.appliedCalories)
        assertEquals(0.3, result.weeklyWeightChangeKg, 0.0)
        assertEquals(34, result.estimatedWeeks)
        assertEquals(PlanStatus.HEALTHY, result.status)
    }

    @Test
    fun daysUntilEvent_usesInjectedClockAndRejectsPastDates() {
        assertEquals(
            14L,
            engine.daysUntilEvent(baseState(eventTimeMillis = NOW_MILLIS + days(14)))
        )
        assertNull(engine.daysUntilEvent(baseState(eventTimeMillis = NOW_MILLIS)))
        assertNull(engine.daysUntilEvent(baseState(eventTimeMillis = NOW_MILLIS - days(1))))
    }

    @Test
    fun aiTargetValidation_acceptsSafeDirectionalSuggestion() {
        val accepted = engine.validateAiTargetSuggestion(
            state = baseState(targetWeight = 70, goalType = GoalType.LOSE_WEIGHT),
            suggestion = targetSuggestion(weightKg = 70)
        )

        assertTrue(accepted)
    }

    @Test
    fun aiTargetValidation_rejectsDirectionRangeAndRiskViolations() {
        val state = baseState(targetWeight = 70, goalType = GoalType.LOSE_WEIGHT)

        assertFalse(engine.validateAiTargetSuggestion(state, targetSuggestion(weightKg = 80)))
        assertFalse(engine.validateAiTargetSuggestion(state, targetSuggestion(weightKg = 24)))
        assertFalse(
            engine.validateAiTargetSuggestion(
                state,
                targetSuggestion(
                    weightKg = 70,
                    riskLevel = AiTargetRiskLevel.NOT_RECOMMENDED
                )
            )
        )
    }

    @Test
    fun aiTargetValidation_rejectsExcessiveEventTimeline() {
        val accepted = engine.validateAiTargetSuggestion(
            state = baseState(
                targetWeight = 70,
                goalType = GoalType.LOSE_WEIGHT,
                eventTimeMillis = NOW_MILLIS + days(42)
            ),
            suggestion = targetSuggestion(weightKg = 70)
        )

        assertFalse(accepted)
    }

    @Test
    fun aiPlanValidation_appliesSafeNumbersAndSignedWeightChange() {
        val validation = engine.validateAiPlanSuggestion(
            state = baseState(targetWeight = 70, goalType = GoalType.LOSE_WEIGHT),
            localResult = localResult(),
            suggestion = planSuggestion(
                status = AiPlanStatus.SAFE,
                calories = 1900,
                weeklyChangeKg = 0.6
            )
        )

        assertEquals(PlanAiMode.AI_VERIFIED, validation.mode)
        assertEquals(1900, validation.displayResult.appliedCalories)
        assertEquals(-0.6, validation.displayResult.weeklyWeightChangeKg, 0.0)
        assertEquals(17, validation.displayResult.estimatedWeeks)
        assertEquals(PlanStatus.HEALTHY, validation.displayResult.status)
    }

    @Test
    fun aiPlanValidation_mapsCautionToAggressiveStatus() {
        val validation = engine.validateAiPlanSuggestion(
            state = baseState(targetWeight = 70, goalType = GoalType.LOSE_WEIGHT),
            localResult = localResult(),
            suggestion = planSuggestion(
                status = AiPlanStatus.CAUTION,
                calories = 1800,
                weeklyChangeKg = 0.8
            )
        )

        assertEquals(PlanAiMode.AI_VERIFIED, validation.mode)
        assertEquals(PlanStatus.AGGRESSIVE, validation.displayResult.status)
    }

    @Test
    fun aiPlanValidation_keepsLocalNumbersWhenCaloriesAreTooDifferent() {
        val localResult = localResult()
        val validation = engine.validateAiPlanSuggestion(
            state = baseState(targetWeight = 70, goalType = GoalType.LOSE_WEIGHT),
            localResult = localResult,
            suggestion = planSuggestion(
                status = AiPlanStatus.SAFE,
                calories = 2400,
                weeklyChangeKg = 0.6
            )
        )

        assertEquals(PlanAiMode.AI_ADJUSTED, validation.mode)
        assertEquals(localResult, validation.displayResult)
    }

    @Test
    fun aiPlanValidation_keepsLocalNumbersForNotRecommendedPlan() {
        val localResult = localResult()
        val validation = engine.validateAiPlanSuggestion(
            state = baseState(targetWeight = 70, goalType = GoalType.LOSE_WEIGHT),
            localResult = localResult,
            suggestion = planSuggestion(
                status = AiPlanStatus.NOT_RECOMMENDED,
                calories = 1800,
                weeklyChangeKg = 0.6
            )
        )

        assertEquals(PlanAiMode.AI_ADJUSTED, validation.mode)
        assertEquals(localResult, validation.displayResult)
    }

    @Test
    fun aiPlanValidation_preservesNoWeightChangeStatusForMaintainGoal() {
        val localResult = localResult(status = PlanStatus.NO_WEIGHT_CHANGE)
        val validation = engine.validateAiPlanSuggestion(
            state = baseState(goalType = GoalType.MAINTAIN_WEIGHT, targetWeight = 0),
            localResult = localResult,
            suggestion = planSuggestion(
                status = AiPlanStatus.SAFE,
                calories = 1850,
                weeklyChangeKg = 0.2
            )
        )

        assertEquals(PlanAiMode.AI_VERIFIED, validation.mode)
        assertEquals(0.0, validation.displayResult.weeklyWeightChangeKg, 0.0)
        assertNull(validation.displayResult.estimatedWeeks)
        assertEquals(PlanStatus.NO_WEIGHT_CHANGE, validation.displayResult.status)
    }

    private fun baseState(
        name: String = "Nursena",
        height: Int = 180,
        weight: Int = 80,
        age: Int = 30,
        gender: Gender = Gender.MALE,
        goalType: GoalType? = GoalType.MAINTAIN_WEIGHT,
        targetWeight: Int = 80,
        eventTimeMillis: Long? = null,
        activityLevel: ActivityLevel? = ActivityLevel.LOW
    ): OnboardingState {
        return OnboardingState(
            name = name,
            height = height,
            weight = weight,
            age = age,
            gender = gender,
            goalType = goalType,
            targetWeight = targetWeight,
            eventTimeMillis = eventTimeMillis,
            activityLevel = activityLevel
        )
    }

    private fun targetSuggestion(
        weightKg: Int,
        riskLevel: AiTargetRiskLevel = AiTargetRiskLevel.SAFE
    ): AiTargetSuggestion {
        return AiTargetSuggestion(
            suggestedTargetWeightKg = weightKg,
            title = "Target",
            explanation = "Explanation",
            riskLevel = riskLevel,
            warning = ""
        )
    }

    private fun planSuggestion(
        status: AiPlanStatus,
        calories: Int,
        weeklyChangeKg: Double
    ): AiPlanSuggestion {
        return AiPlanSuggestion(
            planStatus = status,
            recommendedDailyCalories = calories,
            recommendedWeeklyChangeKg = weeklyChangeKg,
            targetComment = "",
            timelineComment = "",
            calorieComment = "",
            motivationComment = "",
            summary = "",
            warning = ""
        )
    }

    private fun localResult(
        status: PlanStatus = PlanStatus.HEALTHY
    ): DailyNeedsResult {
        return DailyNeedsResult(
            bmr = 1700,
            maintenanceCalories = 2200,
            targetCalories = 1800,
            recommendedCalories = 1800,
            appliedCalories = 1800,
            isUserTargetApplied = true,
            weeklyWeightChangeKg = -0.5,
            estimatedWeeks = 20,
            requiredWeeklyChangeKg = null,
            status = status
        )
    }

    private fun days(count: Long): Long = count * DAY_MILLIS

    private companion object {
        const val NOW_MILLIS = 1_800_000_000_000L
        const val DAY_MILLIS = 24L * 60L * 60L * 1000L
    }
}
