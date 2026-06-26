package com.example.fitto.feature_daily

import androidx.annotation.StringRes
import com.example.fitto.R
import com.example.fitto.feature_auth.register.GoalType
import java.util.Locale

object DailyAiGoalTextResolver {

    @StringRes
    fun resolveLabelRes(goalTypeValue: String): Int {
        return when (normalizeGoalTypeName(goalTypeValue)) {
            GoalType.LOSE_WEIGHT.name -> R.string.daily_ai_goal_label_lose_weight
            GoalType.GAIN_WEIGHT.name -> R.string.daily_ai_goal_label_gain_weight
            GoalType.MAINTAIN_WEIGHT.name -> R.string.daily_ai_goal_label_maintain_weight
            GoalType.LEARN_NUTRITION.name -> R.string.daily_ai_goal_label_learn_nutrition
            GoalType.EAT_HEALTHY.name -> R.string.daily_ai_goal_label_eat_healthy
            else -> R.string.daily_ai_goal_label_not_specified
        }
    }

    fun resolveGuidance(
        goalTypeValue: String,
        appLanguage: String
    ): String {
        val normalized = normalizeGoalTypeName(goalTypeValue)
        val isEnglish = appLanguage.startsWith("en", ignoreCase = true)

        return when (normalized) {
            GoalType.LOSE_WEIGHT.name -> if (isEnglish) {
                LOSE_WEIGHT_GUIDANCE_EN
            } else {
                LOSE_WEIGHT_GUIDANCE_TR
            }
            GoalType.GAIN_WEIGHT.name -> if (isEnglish) {
                GAIN_WEIGHT_GUIDANCE_EN
            } else {
                GAIN_WEIGHT_GUIDANCE_TR
            }
            GoalType.MAINTAIN_WEIGHT.name -> if (isEnglish) {
                MAINTAIN_WEIGHT_GUIDANCE_EN
            } else {
                MAINTAIN_WEIGHT_GUIDANCE_TR
            }
            GoalType.LEARN_NUTRITION.name -> if (isEnglish) {
                LEARN_NUTRITION_GUIDANCE_EN
            } else {
                LEARN_NUTRITION_GUIDANCE_TR
            }
            GoalType.EAT_HEALTHY.name -> if (isEnglish) {
                EAT_HEALTHY_GUIDANCE_EN
            } else {
                EAT_HEALTHY_GUIDANCE_TR
            }
            else -> if (isEnglish) {
                DEFAULT_GUIDANCE_EN
            } else {
                DEFAULT_GUIDANCE_TR
            }
        }
    }

    private fun normalizeGoalTypeName(goalTypeValue: String): String {
        val raw = goalTypeValue.trim()
        GoalType.entries.firstOrNull { goalType ->
            goalType.name.equals(raw, ignoreCase = true)
        }?.let { goalType ->
            return goalType.name
        }

        return raw
            .uppercase(Locale.ROOT)
            .replace(" ", "_")
            .replace("-", "_")
    }

    private const val LOSE_WEIGHT_GUIDANCE_EN =
        "Gently mention calorie excess, also notice overly low or insufficient meals, and suggest filling balanced choices."
    private const val LOSE_WEIGHT_GUIDANCE_TR =
        "Kalori a\u015f\u0131m\u0131n\u0131 nazik\u00e7e belirt, \u00e7ok d\u00fc\u015f\u00fck veya yetersiz \u00f6\u011f\u00fcnleri de fark et, tok tutan ve dengeli se\u00e7imler \u00f6ner."

    private const val GAIN_WEIGHT_GUIDANCE_EN =
        "Notice low-energy meals more clearly and suggest supportive energy and protein sources without strict dieting language."
    private const val GAIN_WEIGHT_GUIDANCE_TR =
        "D\u00fc\u015f\u00fck enerjili \u00f6\u011f\u00fcnleri daha net fark et, enerji ve protein deste\u011fi \u00f6ner, kesin diyet dili kullanma."

    private const val MAINTAIN_WEIGHT_GUIDANCE_EN =
        "Emphasize balance and sustainable habits; gently mention very low or very high days."
    private const val MAINTAIN_WEIGHT_GUIDANCE_TR =
        "Denge ve s\u00fcrd\u00fcr\u00fclebilir al\u0131\u015fkanl\u0131klar\u0131 vurgula; a\u015f\u0131r\u0131 d\u00fc\u015f\u00fck veya a\u015f\u0131r\u0131 y\u00fcksek g\u00fcnleri nazik\u00e7e belirt."

    private const val LEARN_NUTRITION_GUIDANCE_EN =
        "Use a more educational tone and briefly explain the stronger and weaker sides of meals to build nutrition awareness."
    private const val LEARN_NUTRITION_GUIDANCE_TR =
        "Daha \u00f6\u011fretici konu\u015f; \u00f6\u011f\u00fcnlerin g\u00fc\u00e7l\u00fc ve zay\u0131f taraf\u0131n\u0131 k\u0131sa a\u00e7\u0131klayarak beslenme fark\u0131ndal\u0131\u011f\u0131 kazand\u0131r."

    private const val EAT_HEALTHY_GUIDANCE_EN =
        "Focus on variety, balance, water, movement, and sustainable choices without over-focusing on calories."
    private const val EAT_HEALTHY_GUIDANCE_TR =
        "Kaloriye fazla tak\u0131lmadan \u00e7e\u015fitlilik, denge, su, hareket ve s\u00fcrd\u00fcr\u00fclebilir se\u00e7imleri vurgula."

    private const val DEFAULT_GUIDANCE_EN =
        "Do not assume a special goal; comment on the visible daily balance safely and briefly."
    private const val DEFAULT_GUIDANCE_TR =
        "\u00d6zel hedef varsayma; g\u00f6r\u00fcnen g\u00fcnl\u00fck dengeyi g\u00fcvenli ve k\u0131sa yorumla."
}
