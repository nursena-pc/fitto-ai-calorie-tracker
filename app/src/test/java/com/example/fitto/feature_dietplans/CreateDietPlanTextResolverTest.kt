package com.example.fitto.feature_dietplans

import com.example.fitto.feature_dietplans.model.CustomDietPlanLocalModel
import org.junit.Assert.assertEquals
import org.junit.Test

class CreateDietPlanTextResolverTest {

    @Test
    fun defaultExclusions_returnsLocalizedExclusionsInDisplayOrder() {
        val resolver = CreateDietPlanTextResolver(FakeStrings())

        assertEquals(
            listOf("Brokoli", "Mantar", "Süt", "Yer fıstığı", "Deniz ürünü"),
            resolver.defaultExclusions()
        )
    }

    @Test
    fun socialShareText_usesGoalTitlePlanValuesAndLocalizedTags() {
        val resolver = CreateDietPlanTextResolver(FakeStrings())

        val text = resolver.socialShareText(
            goal = CreatePlanGoal.MUSCLE,
            plan = plan(dailyCalories = 2400, durationDays = 45)
        )

        assertEquals(
            CreateDietPlanSocialShareText(
                content = "Goal: Muscle, Calories: 2400, Duration: 45",
                mealInfo = "Custom diet plan",
                customTag = "Custom",
                goalTag = "Muscle"
            ),
            text
        )
    }

    @Test
    fun socialShareText_keepsResolvedGoalTitleAsGoalTag() {
        val resolver = CreateDietPlanTextResolver(FakeStrings(goalTitles = mapOf(
            CreatePlanGoal.WEIGHT_LOSS to "Weight loss"
        )))

        val text = resolver.socialShareText(
            goal = CreatePlanGoal.WEIGHT_LOSS,
            plan = plan(dailyCalories = 1600, durationDays = 30)
        )

        assertEquals("Weight loss", text.goalTag)
        assertEquals("Goal: Weight loss, Calories: 1600, Duration: 30", text.content)
    }

    private fun plan(
        dailyCalories: Int,
        durationDays: Int
    ): CustomDietPlanLocalModel {
        return CustomDietPlanLocalModel(
            id = "custom_plan_1",
            title = "My plan",
            description = "Description",
            goalType = CreatePlanGoal.MUSCLE.name,
            dailyCalories = dailyCalories,
            mealCount = 4,
            durationDays = durationDays,
            isPublic = true,
            preferences = emptyList(),
            exclusions = emptyList(),
            mealOrder = emptyList(),
            isDraft = false,
            createdAt = 1_000L,
            updatedAt = 1_000L
        )
    }

    private class FakeStrings(
        private val goalTitles: Map<CreatePlanGoal, String> = mapOf(
            CreatePlanGoal.MUSCLE to "Muscle"
        )
    ) : CreateDietPlanTextResolver.Strings {
        override fun defaultExclusionBroccoli(): String = "Brokoli"
        override fun defaultExclusionMushroom(): String = "Mantar"
        override fun defaultExclusionMilk(): String = "Süt"
        override fun defaultExclusionPeanut(): String = "Yer fıstığı"
        override fun defaultExclusionSeafood(): String = "Deniz ürünü"

        override fun goalTitle(goal: CreatePlanGoal): String {
            return goalTitles.getValue(goal)
        }

        override fun socialShareContent(
            goalTitle: String,
            dailyCalories: Int,
            durationDays: Int
        ): String {
            return "Goal: $goalTitle, Calories: $dailyCalories, Duration: $durationDays"
        }

        override fun socialShareMealInfo(): String = "Custom diet plan"
        override fun customDietPlanTag(): String = "Custom"
    }
}
