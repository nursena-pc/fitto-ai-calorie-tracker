package com.example.fitto.feature_auth.register

import android.view.View
import com.example.fitto.R
import com.google.android.material.progressindicator.LinearProgressIndicator

private const val TOTAL_ONBOARDING_STEPS = 12

fun View.setOnboardingProgress(activeStepCount: Int) {
    val progressIndicator = findViewById<LinearProgressIndicator>(
        R.id.onboardingProgressContainer
    ) ?: return

    progressIndicator.max = TOTAL_ONBOARDING_STEPS
    progressIndicator.progress = activeStepCount.coerceIn(
        0,
        TOTAL_ONBOARDING_STEPS
    )
}