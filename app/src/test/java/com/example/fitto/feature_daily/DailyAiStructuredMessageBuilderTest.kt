package com.example.fitto.feature_daily

import com.example.fitto.feature_daily.ai.AiDailyAnalysisResult
import com.example.fitto.feature_daily.ai.AiDailyMealComment
import com.example.fitto.feature_daily.ai.AiDailyStatusLevel
import org.junit.Assert.assertEquals
import org.junit.Test

class DailyAiStructuredMessageBuilderTest {

    private val labels = DailyAiStructuredMessageBuilder.Labels(
        mealComments = "Meals",
        unknownEntry = "Unknown",
        water = "Water",
        exercise = "Exercise",
        generalComment = "General",
        suggestion = "Suggestion"
    )

    @Test
    fun `plain text takes priority over structured sections`() {
        assertEquals(
            "Plain response",
            DailyAiStructuredMessageBuilder.build(
                analysis = analysis(plainText = "  Plain response  "),
                labels = labels
            )
        )
    }

    @Test
    fun `all available sections are rendered in existing order`() {
        val result = DailyAiStructuredMessageBuilder.build(
            analysis = analysis(
                mealComments = listOf(
                    AiDailyMealComment(
                        mealName = "Lunch",
                        comment = "Protein is balanced.",
                        level = AiDailyStatusLevel.GREEN
                    )
                ),
                waterComment = "Drink one more glass.",
                exerciseComment = "Add a short walk.",
                generalComment = "Your day is balanced.",
                suggestion = "Prepare dinner early."
            ),
            labels = labels
        )

        assertEquals(
            """Meals
• Lunch: Protein is balanced.

Water
Drink one more glass.

Exercise
Add a short walk.

General
Your day is balanced.

Suggestion
Prepare dinner early.""",
            result
        )
    }

    @Test
    fun `blank meal name uses unknown label and blank comments are omitted`() {
        val result = DailyAiStructuredMessageBuilder.build(
            analysis = analysis(
                mealComments = listOf(
                    AiDailyMealComment("", "Useful comment", AiDailyStatusLevel.YELLOW),
                    AiDailyMealComment("Dinner", "   ", AiDailyStatusLevel.GREEN)
                )
            ),
            labels = labels
        )

        assertEquals("Meals\n• Unknown: Useful comment", result)
    }

    @Test
    fun `empty analysis produces empty message`() {
        assertEquals(
            "",
            DailyAiStructuredMessageBuilder.build(analysis(), labels)
        )
    }

    private fun analysis(
        mealComments: List<AiDailyMealComment> = emptyList(),
        waterComment: String = "",
        exerciseComment: String = "",
        generalComment: String = "",
        suggestion: String = "",
        plainText: String? = null
    ): AiDailyAnalysisResult {
        return AiDailyAnalysisResult(
            status = AiDailyStatusLevel.GREEN,
            mealComments = mealComments,
            waterComment = waterComment,
            exerciseComment = exerciseComment,
            generalComment = generalComment,
            suggestion = suggestion,
            plainText = plainText
        )
    }
}
