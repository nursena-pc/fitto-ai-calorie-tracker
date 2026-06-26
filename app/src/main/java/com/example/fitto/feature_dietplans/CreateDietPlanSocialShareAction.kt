package com.example.fitto.feature_dietplans

import com.example.fitto.feature_dietplans.model.CustomDietPlanLocalModel
import com.example.fitto.feature_home.presentation.social.POST_VISIBILITY_COMMUNITY
import com.example.fitto.feature_home.presentation.social.SocialRepository

class CreateDietPlanSocialShareAction(
    private val postCreator: SocialPostCreator
) {

    suspend fun share(
        plan: CustomDietPlanLocalModel,
        text: CreateDietPlanSocialShareText
    ) {
        if (!plan.isPublic) return

        runCatching {
            postCreator.createCommunityPost(
                content = text.content,
                mealInfo = text.mealInfo,
                calories = plan.dailyCalories,
                tags = listOf(text.customTag, text.goalTag)
            )
        }
    }

    interface SocialPostCreator {
        suspend fun createCommunityPost(
            content: String,
            mealInfo: String,
            calories: Int,
            tags: List<String>
        )
    }

    companion object {
        fun from(socialRepository: SocialRepository): CreateDietPlanSocialShareAction {
            return CreateDietPlanSocialShareAction(
                postCreator = object : SocialPostCreator {
                    override suspend fun createCommunityPost(
                        content: String,
                        mealInfo: String,
                        calories: Int,
                        tags: List<String>
                    ) {
                        socialRepository.createPost(
                            content = content,
                            imageUri = null,
                            mealInfo = mealInfo,
                            calories = calories,
                            allowComments = true,
                            visibility = POST_VISIBILITY_COMMUNITY,
                            tags = tags,
                            isDraft = false
                        )
                    }
                }
            )
        }
    }
}

data class CreateDietPlanSocialShareText(
    val content: String,
    val mealInfo: String,
    val customTag: String,
    val goalTag: String
)
