package com.example.fitto.core.platform

import android.app.Activity
import android.os.Build

object ActivityTransitionCompat {

    fun disableAnimations(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            activity.overrideActivityTransition(
                Activity.OVERRIDE_TRANSITION_OPEN,
                NO_ANIMATION,
                NO_ANIMATION
            )
            activity.overrideActivityTransition(
                Activity.OVERRIDE_TRANSITION_CLOSE,
                NO_ANIMATION,
                NO_ANIMATION
            )
        } else {
            disableAnimationsLegacy(activity)
        }
    }

    @Suppress("DEPRECATION")
    private fun disableAnimationsLegacy(activity: Activity) {
        activity.overridePendingTransition(NO_ANIMATION, NO_ANIMATION)
    }

    private const val NO_ANIMATION = 0
}
