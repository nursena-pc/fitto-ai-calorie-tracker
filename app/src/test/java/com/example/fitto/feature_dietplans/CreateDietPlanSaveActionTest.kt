package com.example.fitto.feature_dietplans

import com.example.fitto.feature_dietplans.model.CustomDietPlanLocalModel
import com.example.fitto.feature_meal.model.MealType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateDietPlanSaveActionTest {

    @Test
    fun save_returnsMissingNameWithoutSavingPlan() {
        val customPlanStore = FakeCustomPlanStore()

        val result = action(customPlanStore).save(
            formState = formState(planName = " "),
            existingPlan = null,
            isDraft = false
        )

        assertEquals(
            CreateDietPlanSaveValidation.MISSING_NAME,
            (result as CreateDietPlanSaveResult.ValidationFailed).validation
        )
        assertTrue(customPlanStore.savedPlans.isEmpty())
    }

    @Test
    fun save_returnsDuplicateNameWithoutSavingPlan() {
        val customPlanStore = FakeCustomPlanStore(hasDuplicateTitle = true)

        val result = action(customPlanStore).save(
            formState = formState(planName = "My plan"),
            existingPlan = existingPlan(),
            isDraft = false
        )

        assertEquals(
            CreateDietPlanSaveValidation.DUPLICATE_NAME,
            (result as CreateDietPlanSaveResult.ValidationFailed).validation
        )
        assertTrue(customPlanStore.savedPlans.isEmpty())
        assertEquals("custom_plan_existing", customPlanStore.duplicateChecks.single().excludingPlanId)
    }

    @Test
    fun save_savesDraftAndPreservesExistingIdentity() {
        val customPlanStore = FakeCustomPlanStore()

        val result = action(customPlanStore).save(
            formState = formState(),
            existingPlan = existingPlan(),
            isDraft = true
        ) as CreateDietPlanSaveResult.DraftSaved

        assertTrue(result.wasUpdate)
        assertEquals("custom_plan_existing", result.plan.id)
        assertEquals(500L, result.plan.createdAt)
        assertEquals(NOW, result.plan.updatedAt)
        assertTrue(result.plan.isDraft)
        assertEquals(result.plan, customPlanStore.savedPlans.single())
    }

    @Test
    fun save_createsPlanAndAppliesIt() {
        val customPlanStore = FakeCustomPlanStore()
        val preferenceAction = FakePreferenceAction()

        val result = action(
            customPlanStore = customPlanStore,
            preferenceAction = preferenceAction
        ).save(
            formState = formState(privacy = CreatePlanPrivacy.PUBLIC),
            existingPlan = null,
            isDraft = false
        ) as CreateDietPlanSaveResult.PlanCreated

        assertEquals("custom_plan_$NOW", result.plan.id)
        assertEquals(CreatePlanGoal.MUSCLE, result.goal)
        assertEquals(DietPlanApplyResult(activePlanId = result.plan.id), result.applyResult)
        assertEquals(result.plan, customPlanStore.savedPlans.single())
        assertEquals(listOf(result.plan.id), preferenceAction.appliedPlanIds)
    }

    private fun action(
        customPlanStore: FakeCustomPlanStore = FakeCustomPlanStore(),
        preferenceAction: FakePreferenceAction = FakePreferenceAction()
    ): CreateDietPlanSaveAction {
        return CreateDietPlanSaveAction(
            customPlanStore = customPlanStore,
            preferenceAction = preferenceAction,
            nowProvider = { NOW }
        )
    }

    private fun formState(
        planName: String = "My plan",
        privacy: CreatePlanPrivacy = CreatePlanPrivacy.PRIVATE
    ): CreateDietPlanFormState {
        return CreateDietPlanFormState(
            planName = planName,
            description = "Description",
            goal = CreatePlanGoal.MUSCLE,
            privacy = privacy,
            dailyCalories = 2100,
            durationDays = 30,
            mealTypes = listOf(MealType.BREAKFAST, MealType.DINNER),
            preferences = listOf("high_protein"),
            exclusions = listOf("milk")
        )
    }

    private fun existingPlan(): CustomDietPlanLocalModel {
        return CustomDietPlanLocalModel(
            id = "custom_plan_existing",
            title = "Old title",
            description = "Old description",
            goalType = CreatePlanGoal.BALANCED.name,
            dailyCalories = 1800,
            mealCount = 4,
            durationDays = 14,
            isPublic = true,
            preferences = emptyList(),
            exclusions = emptyList(),
            mealOrder = emptyList(),
            isDraft = true,
            createdAt = 500L,
            updatedAt = 500L
        )
    }

    private class FakeCustomPlanStore(
        private val hasDuplicateTitle: Boolean = false
    ) : CreateDietPlanSaveAction.CustomPlanStore {
        val savedPlans = mutableListOf<CustomDietPlanLocalModel>()
        val duplicateChecks = mutableListOf<DuplicateCheck>()

        override fun hasPlanWithTitle(title: String, excludingPlanId: String?): Boolean {
            duplicateChecks += DuplicateCheck(title, excludingPlanId)
            return hasDuplicateTitle
        }

        override fun savePlan(plan: CustomDietPlanLocalModel) {
            savedPlans += plan
        }
    }

    private class FakePreferenceAction : CreateDietPlanSaveAction.PreferenceAction {
        val appliedPlanIds = mutableListOf<String>()

        override fun applyPlan(planId: String): DietPlanApplyResult {
            appliedPlanIds += planId
            return DietPlanApplyResult(activePlanId = planId)
        }
    }

    private data class DuplicateCheck(
        val title: String,
        val excludingPlanId: String?
    )

    private companion object {
        const val NOW = 1_000L
    }
}
