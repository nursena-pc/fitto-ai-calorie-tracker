package com.example.fitto.feature_daily

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.example.fitto.R
import com.example.fitto.databinding.FragmentDailyBinding
import com.example.fitto.feature_daily.ai.AiDailyAnalysisResult
import com.example.fitto.feature_daily.ai.AiDailyMealComment
import com.example.fitto.feature_daily.ai.AiDailyStatusLevel

class DailyAiCardRenderer(
    private val binding: FragmentDailyBinding,
    private val context: Context
) {

    fun renderDefaultState() = with(binding) {
        applyStatusStyle(AiDailyStatusLevel.GREEN)
        dailyAiProgressIndicator.isVisible = false
        dailyAiTitleText.text = context.getString(R.string.daily_ai_title)
        dailyAiMessageText.text = context.getString(R.string.daily_ai_default_message)
        dailyAiSubtitleText.text = context.getString(R.string.daily_ai_default_subtitle)
    }

    fun renderPreparingState() = with(binding) {
        dailyAiProgressIndicator.isVisible = true
        dailyAiTitleText.text = context.getString(R.string.daily_ai_title)
        dailyAiMessageText.text = context.getString(R.string.daily_ai_preparing)
        dailyAiSubtitleText.text = context.getString(R.string.daily_ai_default_subtitle)
    }

    fun renderLoading(status: AiDailyStatusLevel) = with(binding) {
        applyStatusStyle(status)
        dailyAiProgressIndicator.isVisible = true
        dailyAiTitleText.text = context.getString(R.string.daily_ai_title)
        dailyAiMessageText.text = context.getString(R.string.daily_ai_loading)
        dailyAiSubtitleText.text = context.getString(R.string.daily_ai_default_subtitle)
    }

    fun renderMessage(
        message: String,
        subtitle: String,
        status: AiDailyStatusLevel
    ) = with(binding) {
        applyStatusStyle(status)
        dailyAiProgressIndicator.isVisible = false
        dailyAiTitleText.text = context.getString(R.string.daily_ai_title)
        dailyAiMessageText.text = message
        dailyAiSubtitleText.text = subtitle
    }

    fun renderStructuredResult(
        analysis: AiDailyAnalysisResult,
        fallbackMessage: String,
        subtitle: String,
        status: AiDailyStatusLevel
    ) {
        val message = buildStructuredMessage(analysis)
            .ifBlank { fallbackMessage }

        renderMessage(
            message = message,
            subtitle = subtitle,
            status = status
        )
    }

    fun buildStructuredMessage(analysis: AiDailyAnalysisResult): String {
        return buildStructuredMessage(
            mealComments = analysis.mealComments,
            waterComment = analysis.waterComment,
            exerciseComment = analysis.exerciseComment,
            generalComment = analysis.generalComment,
            suggestion = analysis.suggestion,
            plainText = analysis.plainText
        )
    }

    private fun buildStructuredMessage(
        mealComments: List<AiDailyMealComment>,
        waterComment: String,
        exerciseComment: String,
        generalComment: String,
        suggestion: String,
        plainText: String? = null
    ): String {
        plainText
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }

        val builder = StringBuilder()

        val usefulMealComments = mealComments.filter { it.comment.isNotBlank() }
        if (usefulMealComments.isNotEmpty()) {
            builder.append(context.getString(R.string.daily_ai_meal_comments_label)).append('\n')
            usefulMealComments.forEach { meal ->
                val name = meal.mealName.trim()
                    .ifBlank { context.getString(R.string.daily_ai_entry_unknown) }
                builder.append("\u2022 ")
                    .append(name)
                    .append(": ")
                    .append(meal.comment.trim())
                    .append('\n')
            }
        }

        waterComment.trim().takeIf { it.isNotBlank() }?.let { comment ->
            if (builder.isNotEmpty()) builder.append('\n')
            builder.append(context.getString(R.string.daily_ai_water_label)).append('\n')
            builder.append(comment).append('\n')
        }

        exerciseComment.trim().takeIf { it.isNotBlank() }?.let { comment ->
            if (builder.isNotEmpty()) builder.append('\n')
            builder.append(context.getString(R.string.daily_ai_exercise_label)).append('\n')
            builder.append(comment).append('\n')
        }

        generalComment.trim().takeIf { it.isNotBlank() }?.let { comment ->
            if (builder.isNotEmpty()) builder.append('\n')
            builder.append(context.getString(R.string.daily_ai_general_comment_label)).append('\n')
            builder.append(comment).append('\n')
        }

        suggestion.trim().takeIf { it.isNotBlank() }?.let { comment ->
            if (builder.isNotEmpty()) builder.append('\n')
            builder.append(context.getString(R.string.daily_ai_suggestion_label)).append('\n')
            builder.append(comment)
        }

        return builder.toString().trim()
    }

    private fun applyStatusStyle(status: AiDailyStatusLevel) = with(binding) {
        val (bgColorRes, textColorRes) = when (status) {
            AiDailyStatusLevel.GREEN -> R.color.ai_status_green_bg to R.color.ai_status_green_text
            AiDailyStatusLevel.YELLOW -> R.color.ai_status_yellow_bg to R.color.ai_status_yellow_text
            AiDailyStatusLevel.RED -> R.color.ai_status_red_bg to R.color.ai_status_red_text
        }

        val bgColor = ContextCompat.getColor(context, bgColorRes)
        val textColor = ContextCompat.getColor(context, textColorRes)
        dailyAiCard.setCardBackgroundColor(bgColor)
        dailyAiCard.strokeColor = textColor
        dailyAiTitleText.setTextColor(textColor)
        dailyAiMessageText.setTextColor(textColor)
        dailyAiSubtitleText.setTextColor(textColor)
        dailyAiProgressIndicator.setIndicatorColor(textColor)
    }
}
