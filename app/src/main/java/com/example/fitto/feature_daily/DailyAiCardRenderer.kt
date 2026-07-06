package com.example.fitto.feature_daily

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.example.fitto.R
import com.example.fitto.databinding.FragmentDailyBinding
import com.example.fitto.feature_daily.ai.AiDailyAnalysisResult
import com.example.fitto.feature_daily.ai.AiDailyStatusLevel

class DailyAiCardRenderer(
    private val binding: FragmentDailyBinding,
    private val context: Context
) {

    fun renderDefaultState() = with(binding) {
        applyStatusStyle(AiDailyStatusLevel.GREEN)
        dailyAiProgressIndicator.isVisible = false
        dailyAiApplyButton.isEnabled = true
        dailyAiTitleText.text = context.getString(R.string.daily_ai_recommendation_title)
        dailyAiMessageText.text = context.getString(R.string.daily_ai_recommendation_message)
        dailyAiSubtitleText.text = context.getString(R.string.daily_ai_default_subtitle)
        dailyAiApplyButton.text = context.getString(R.string.daily_ai_recommendation_apply)
        dailyAiReasonButton.text = context.getString(R.string.daily_ai_recommendation_reason_button)
    }

    fun renderPreparingState() = with(binding) {
        applyStatusStyle(AiDailyStatusLevel.GREEN)
        dailyAiProgressIndicator.isVisible = true
        dailyAiApplyButton.isEnabled = false
        dailyAiTitleText.text = context.getString(R.string.daily_ai_recommendation_title)
        dailyAiMessageText.text = context.getString(R.string.daily_ai_preparing)
        dailyAiSubtitleText.text = context.getString(R.string.daily_ai_default_subtitle)
        dailyAiApplyButton.text = context.getString(R.string.daily_ai_recommendation_apply)
        dailyAiReasonButton.text = context.getString(R.string.daily_ai_recommendation_reason_button)
    }

    fun renderLoading(status: AiDailyStatusLevel) = with(binding) {
        applyStatusStyle(status)
        dailyAiProgressIndicator.isVisible = true
        dailyAiApplyButton.isEnabled = false
        dailyAiTitleText.text = context.getString(R.string.daily_ai_recommendation_title)
        dailyAiMessageText.text = context.getString(R.string.daily_ai_loading)
        dailyAiSubtitleText.text = context.getString(R.string.daily_ai_default_subtitle)
        dailyAiApplyButton.text = context.getString(R.string.daily_ai_recommendation_apply)
        dailyAiReasonButton.text = context.getString(R.string.daily_ai_recommendation_reason_button)
    }

    fun renderMessage(
        message: String,
        subtitle: String,
        status: AiDailyStatusLevel
    ) = with(binding) {
        applyStatusStyle(status)
        dailyAiProgressIndicator.isVisible = false
        dailyAiApplyButton.isEnabled = true
        dailyAiTitleText.text = context.getString(R.string.daily_ai_recommendation_title)
        dailyAiMessageText.text = message.ifBlank {
            context.getString(R.string.daily_ai_recommendation_message)
        }
        dailyAiSubtitleText.text = subtitle
        dailyAiApplyButton.text = context.getString(R.string.daily_ai_recommendation_apply)
        dailyAiReasonButton.text = context.getString(R.string.daily_ai_recommendation_reason_button)
    }

    fun renderAppliedState() = with(binding) {
        applyStatusStyle(AiDailyStatusLevel.GREEN)
        dailyAiProgressIndicator.isVisible = false
        dailyAiApplyButton.isEnabled = true
        dailyAiTitleText.text = context.getString(R.string.daily_ai_recommendation_title)
        if (dailyAiMessageText.text.isNullOrBlank()) {
            dailyAiMessageText.text = context.getString(R.string.daily_ai_recommendation_message)
        }
        dailyAiApplyButton.text = context.getString(R.string.daily_ai_recommendation_applied)
        dailyAiReasonButton.text = context.getString(R.string.daily_ai_recommendation_detail_button)
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
        return DailyAiStructuredMessageBuilder.build(
            analysis = analysis,
            labels = DailyAiStructuredMessageBuilder.Labels(
                mealComments = context.getString(R.string.daily_ai_meal_comments_label),
                unknownEntry = context.getString(R.string.daily_ai_entry_unknown),
                water = context.getString(R.string.daily_ai_water_label),
                exercise = context.getString(R.string.daily_ai_exercise_label),
                generalComment = context.getString(R.string.daily_ai_general_comment_label),
                suggestion = context.getString(R.string.daily_ai_suggestion_label)
            )
        )
    }

    private fun applyStatusStyle(status: AiDailyStatusLevel) = with(binding) {
        val badgeTextColorRes = when (status) {
            AiDailyStatusLevel.GREEN -> R.color.fitto_green
            AiDailyStatusLevel.YELLOW -> R.color.ai_status_yellow_text
            AiDailyStatusLevel.RED -> R.color.ai_status_red_text
        }
        val badgeTextColor = ContextCompat.getColor(context, badgeTextColorRes)
        val green = ContextCompat.getColor(context, R.color.fitto_green)
        val surface = ContextCompat.getColor(context, R.color.fitto_surface_soft)
        val bodyText = ContextCompat.getColor(context, R.color.fitto_text_dark)

        dailyAiCard.setCardBackgroundColor(surface)
        dailyAiCard.strokeColor = green
        dailyAiTitleText.setTextColor(green)
        dailyAiMessageText.setTextColor(bodyText)
        dailyAiSubtitleText.setTextColor(ContextCompat.getColor(context, R.color.fitto_text_secondary))
        dailyAiProgressIndicator.setIndicatorColor(green)
        dailyAiStatusBadgeText.setTextColor(badgeTextColor)
        dailyAiStatusBadgeText.text = context.getString(R.string.daily_ai_recommendation_badge_active)
    }
}
