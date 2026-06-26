package com.example.fitto.feature_daily

import com.example.fitto.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyAiGoalTextResolverTest {

    @Test
    fun resolveLabelRes_mapsKnownGoalValues() {
        assertEquals(
            R.string.daily_ai_goal_label_lose_weight,
            DailyAiGoalTextResolver.resolveLabelRes("lose_weight")
        )
        assertEquals(
            R.string.daily_ai_goal_label_gain_weight,
            DailyAiGoalTextResolver.resolveLabelRes("GAIN-WEIGHT")
        )
        assertEquals(
            R.string.daily_ai_goal_label_maintain_weight,
            DailyAiGoalTextResolver.resolveLabelRes("maintain weight")
        )
        assertEquals(
            R.string.daily_ai_goal_label_learn_nutrition,
            DailyAiGoalTextResolver.resolveLabelRes("LEARN_NUTRITION")
        )
        assertEquals(
            R.string.daily_ai_goal_label_eat_healthy,
            DailyAiGoalTextResolver.resolveLabelRes("eat healthy")
        )
    }

    @Test
    fun resolveLabelRes_usesDefaultForUnknownGoal() {
        assertEquals(
            R.string.daily_ai_goal_label_not_specified,
            DailyAiGoalTextResolver.resolveLabelRes("custom")
        )
    }

    @Test
    fun resolveGuidance_usesEnglishWhenLanguageStartsWithEn() {
        val guidance = DailyAiGoalTextResolver.resolveGuidance(
            goalTypeValue = "LOSE_WEIGHT",
            appLanguage = "en-US"
        )

        assertTrue(guidance.contains("calorie excess"))
    }

    @Test
    fun resolveGuidance_usesTurkishForNonEnglishLanguages() {
        val guidance = DailyAiGoalTextResolver.resolveGuidance(
            goalTypeValue = "GAIN_WEIGHT",
            appLanguage = "tr"
        )

        assertTrue(guidance.contains("D\u00fc\u015f\u00fck enerjili"))
    }

    @Test
    fun resolveGuidance_usesDefaultForUnknownGoal() {
        val guidance = DailyAiGoalTextResolver.resolveGuidance(
            goalTypeValue = "custom",
            appLanguage = "en"
        )

        assertTrue(guidance.contains("Do not assume"))
    }
}
