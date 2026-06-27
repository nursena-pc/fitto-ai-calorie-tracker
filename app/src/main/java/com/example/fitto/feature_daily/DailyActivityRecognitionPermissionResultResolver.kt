package com.example.fitto.feature_daily

import androidx.annotation.StringRes
import com.example.fitto.R

object DailyActivityRecognitionPermissionResultResolver {

    data class Effect(
        @StringRes val messageRes: Int,
        val shouldStartStepCounter: Boolean,
        val shouldRenderSteps: Boolean
    )

    fun resolve(granted: Boolean): Effect {
        return if (granted) {
            Effect(
                messageRes = R.string.step_counter_permission_granted,
                shouldStartStepCounter = true,
                shouldRenderSteps = false
            )
        } else {
            Effect(
                messageRes = R.string.step_counter_permission_denied,
                shouldStartStepCounter = false,
                shouldRenderSteps = true
            )
        }
    }
}
