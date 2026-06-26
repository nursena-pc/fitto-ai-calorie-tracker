package com.example.fitto.feature_dietplans

import com.example.fitto.feature_dietplans.model.CustomDietPlanLocalModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateDietPlanSocialShareActionTest {

    @Test
    fun share_createsCommunityPostForPublicPlan() = runBlocking {
        val postCreator = FakeSocialPostCreator()
        val action = CreateDietPlanSocialShareAction(postCreator)

        action.share(
            plan = plan(isPublic = true),
            text = socialShareText()
        )

        assertEquals(
            listOf(
                CreatedCommunityPost(
                    content = "I created a muscle plan",
                    mealInfo = "Custom diet plan",
                    calories = 2100,
                    tags = listOf("Custom", "Muscle")
                )
            ),
            postCreator.createdPosts
        )
    }

    @Test
    fun share_skipsPrivatePlan() = runBlocking {
        val postCreator = FakeSocialPostCreator()
        val action = CreateDietPlanSocialShareAction(postCreator)

        action.share(
            plan = plan(isPublic = false),
            text = socialShareText()
        )

        assertTrue(postCreator.createdPosts.isEmpty())
    }

    @Test
    fun share_ignoresPostCreationFailure() = runBlocking {
        val postCreator = FakeSocialPostCreator(shouldFail = true)
        val action = CreateDietPlanSocialShareAction(postCreator)

        action.share(
            plan = plan(isPublic = true),
            text = socialShareText()
        )

        assertTrue(postCreator.createdPosts.isEmpty())
    }

    private fun socialShareText(): CreateDietPlanSocialShareText {
        return CreateDietPlanSocialShareText(
            content = "I created a muscle plan",
            mealInfo = "Custom diet plan",
            customTag = "Custom",
            goalTag = "Muscle"
        )
    }

    private fun plan(isPublic: Boolean): CustomDietPlanLocalModel {
        return CustomDietPlanLocalModel(
            id = "custom_plan_1",
            title = "My plan",
            description = "Description",
            goalType = CreatePlanGoal.MUSCLE.name,
            dailyCalories = 2100,
            mealCount = 4,
            durationDays = 30,
            isPublic = isPublic,
            preferences = emptyList(),
            exclusions = emptyList(),
            mealOrder = emptyList(),
            isDraft = false,
            createdAt = 1_000L,
            updatedAt = 1_000L
        )
    }

    private class FakeSocialPostCreator(
        private val shouldFail: Boolean = false
    ) : CreateDietPlanSocialShareAction.SocialPostCreator {
        val createdPosts = mutableListOf<CreatedCommunityPost>()

        override suspend fun createCommunityPost(
            content: String,
            mealInfo: String,
            calories: Int,
            tags: List<String>
        ) {
            if (shouldFail) error("Post creation failed")

            createdPosts += CreatedCommunityPost(
                content = content,
                mealInfo = mealInfo,
                calories = calories,
                tags = tags
            )
        }
    }

    private data class CreatedCommunityPost(
        val content: String,
        val mealInfo: String,
        val calories: Int,
        val tags: List<String>
    )
}
