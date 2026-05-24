package com.example.fitto.feature_auth.register

import com.example.fitto.R
import kotlin.math.abs

enum class TargetWeightSafetyStatus {
    SAFE,
    WARNING,
    BLOCKED
}

enum class TargetWeightSafetyReason {
    SAFE,
    INVALID_INPUT,
    WRONG_DIRECTION_FOR_GOAL,
    TOO_LOW_BMI,
    TOO_HIGH_BMI,
    UNDER_20_CAUTION,
    TOO_BIG_CHANGE_PERCENT,
    TOO_FAST_FOR_EVENT_DATE,
    TOO_AMBITIOUS_FOR_WEIGHT_LOSS,
    TOO_AMBITIOUS_FOR_WEIGHT_GAIN,
    AI_SUGGESTION_DIFFERENT
}

data class TargetWeightSafetyResult(
    val status: TargetWeightSafetyStatus,
    val reason: TargetWeightSafetyReason,
    val messageRes: Int,
    val targetBmi: Double? = null,
    val changePercent: Double? = null,
    val weeklyChangeKg: Double? = null,
    val aiDiffPercent: Double? = null
)

object TargetWeightSafetyChecker {

    /**
     * Yerel kontrol güvenlik amaçlıdır; AI sonucundan bağımsız olarak çalışır.
     * Öncelik: yön, BMI, tarih/haftalık değişim, yüzdesel değişim, sonra AI yakınlığı bilgisidir.
     */
    fun evaluate(
        age: Int,
        heightCm: Int,
        currentWeightKg: Int,
        targetWeightKg: Int,
        goalType: GoalType?,
        daysUntilEvent: Long? = null,
        aiSuggestedTargetWeightKg: Int? = null
    ): TargetWeightSafetyResult {
        if (age <= 0 || heightCm <= 0 || currentWeightKg <= 0 || targetWeightKg <= 0) {
            return blocked(TargetWeightSafetyReason.INVALID_INPUT, R.string.target_weight_safety_blocked_message)
        }

        val heightMeter = heightCm / 100.0
        if (heightMeter <= 0.0) {
            return blocked(TargetWeightSafetyReason.INVALID_INPUT, R.string.target_weight_safety_blocked_message)
        }

        val targetBmi = targetWeightKg / (heightMeter * heightMeter)
        val currentWeight = currentWeightKg.toDouble()
        val targetWeight = targetWeightKg.toDouble()
        val weightDifferenceKg = abs(targetWeight - currentWeight)
        val changePercent = weightDifferenceKg / currentWeight
        val isWeightLoss = goalType == GoalType.LOSE_WEIGHT
        val isWeightGain = goalType == GoalType.GAIN_WEIGHT
        val isWeightLossProgress = isWeightLoss && targetWeightKg < currentWeightKg
        val isWeightGainProgress = isWeightGain && targetWeightKg > currentWeightKg
        val weeklyChangeKg = daysUntilEvent
            ?.takeIf { it > 0L }
            ?.let { days -> weightDifferenceKg / (days / 7.0) }
        val aiDiffPercent = aiSuggestedTargetWeightKg
            ?.takeIf { it > 0 }
            ?.let { suggested -> abs(targetWeightKg - suggested).toDouble() / suggested.toDouble() }

        fun result(
            status: TargetWeightSafetyStatus,
            reason: TargetWeightSafetyReason,
            messageRes: Int
        ): TargetWeightSafetyResult {
            return TargetWeightSafetyResult(
                status = status,
                reason = reason,
                messageRes = messageRes,
                targetBmi = targetBmi,
                changePercent = changePercent,
                weeklyChangeKg = weeklyChangeKg,
                aiDiffPercent = aiDiffPercent
            )
        }

        if (isWeightLoss && targetWeightKg >= currentWeightKg) {
            return result(
                TargetWeightSafetyStatus.BLOCKED,
                TargetWeightSafetyReason.WRONG_DIRECTION_FOR_GOAL,
                R.string.target_weight_error_should_be_lower
            )
        }

        if (isWeightGain && targetWeightKg <= currentWeightKg) {
            return result(
                TargetWeightSafetyStatus.BLOCKED,
                TargetWeightSafetyReason.WRONG_DIRECTION_FOR_GOAL,
                R.string.target_weight_error_should_be_higher
            )
        }

        // Açık riskli uç BMI değerleri her durumda önceliklidir.
        if (targetBmi < 16.0) {
            return result(
                TargetWeightSafetyStatus.BLOCKED,
                TargetWeightSafetyReason.TOO_LOW_BMI,
                R.string.target_weight_safety_too_low_bmi
            )
        }

        if (targetBmi < 18.0) {
            return result(
                TargetWeightSafetyStatus.BLOCKED,
                TargetWeightSafetyReason.TOO_LOW_BMI,
                R.string.target_weight_safety_too_low_bmi
            )
        }

        if (targetBmi > 55.0 && !isWeightLossProgress) {
            return result(
                TargetWeightSafetyStatus.BLOCKED,
                TargetWeightSafetyReason.TOO_HIGH_BMI,
                R.string.target_weight_safety_too_high_bmi
            )
        }

        if (targetBmi > 45.0) {
            return result(
                TargetWeightSafetyStatus.WARNING,
                TargetWeightSafetyReason.TOO_HIGH_BMI,
                R.string.target_weight_safety_warning_message
            )
        }

        weeklyChangeKg?.let { weekly ->
            if (isWeightLoss) {
                if (weekly > 1.8 || (weekly > 1.2 && changePercent > 0.25)) {
                    return result(
                        TargetWeightSafetyStatus.BLOCKED,
                        TargetWeightSafetyReason.TOO_FAST_FOR_EVENT_DATE,
                        R.string.target_weight_safety_too_fast_for_date_blocked
                    )
                }
                if (weekly > 0.9) {
                    return result(
                        TargetWeightSafetyStatus.WARNING,
                        TargetWeightSafetyReason.TOO_FAST_FOR_EVENT_DATE,
                        R.string.target_weight_safety_too_fast_for_date_warning
                    )
                }
            }

            if (isWeightGain) {
                if (weekly > 1.2 || (weekly > 0.8 && changePercent > 0.25)) {
                    return result(
                        TargetWeightSafetyStatus.BLOCKED,
                        TargetWeightSafetyReason.TOO_FAST_FOR_EVENT_DATE,
                        R.string.target_weight_safety_too_fast_for_date_blocked
                    )
                }
                if (weekly > 0.5) {
                    return result(
                        TargetWeightSafetyStatus.WARNING,
                        TargetWeightSafetyReason.TOO_FAST_FOR_EVENT_DATE,
                        R.string.target_weight_safety_too_fast_for_date_warning
                    )
                }
            }
        }

        if (isWeightGainProgress && (changePercent > 0.40 || targetBmi >= 40.0)) {
            return result(
                TargetWeightSafetyStatus.BLOCKED,
                TargetWeightSafetyReason.TOO_AMBITIOUS_FOR_WEIGHT_GAIN,
                R.string.target_weight_safety_gain_too_ambitious_blocked
            )
        }

        if (changePercent > 0.30) {
            return result(
                TargetWeightSafetyStatus.WARNING,
                TargetWeightSafetyReason.TOO_BIG_CHANGE_PERCENT,
                R.string.target_weight_safety_big_change_warning
            )
        }

        if (isWeightLossProgress && changePercent > 0.15) {
            return result(
                TargetWeightSafetyStatus.WARNING,
                TargetWeightSafetyReason.TOO_AMBITIOUS_FOR_WEIGHT_LOSS,
                R.string.target_weight_safety_loss_ambitious_warning
            )
        }

        if (isWeightGainProgress && changePercent > 0.15) {
            return result(
                TargetWeightSafetyStatus.WARNING,
                TargetWeightSafetyReason.TOO_AMBITIOUS_FOR_WEIGHT_GAIN,
                R.string.target_weight_safety_gain_ambitious_warning
            )
        }

        if (targetBmi < 19.0 || targetBmi > 35.0) {
            return result(
                TargetWeightSafetyStatus.WARNING,
                if (targetBmi < 19.0) TargetWeightSafetyReason.TOO_LOW_BMI else TargetWeightSafetyReason.TOO_HIGH_BMI,
                R.string.target_weight_safety_warning_message
            )
        }

        if (age < 20) {
            return result(
                TargetWeightSafetyStatus.WARNING,
                TargetWeightSafetyReason.UNDER_20_CAUTION,
                R.string.target_weight_safety_under_20_warning
            )
        }

        if (aiDiffPercent != null && aiDiffPercent > 0.12) {
            return result(
                TargetWeightSafetyStatus.SAFE,
                TargetWeightSafetyReason.AI_SUGGESTION_DIFFERENT,
                R.string.target_weight_safety_safe_message
            )
        }

        return result(
            TargetWeightSafetyStatus.SAFE,
            TargetWeightSafetyReason.SAFE,
            R.string.target_weight_safety_safe_message
        )
    }

    private fun blocked(reason: TargetWeightSafetyReason, messageRes: Int): TargetWeightSafetyResult {
        return TargetWeightSafetyResult(
            status = TargetWeightSafetyStatus.BLOCKED,
            reason = reason,
            messageRes = messageRes
        )
    }
}
