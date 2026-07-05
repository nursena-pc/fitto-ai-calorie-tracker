package com.example.fitto.feature_meal

import android.os.Bundle
import androidx.core.os.BundleCompat
import com.example.fitto.data.remote.logmeal.LogMealFoodCandidate
import java.io.Serializable

data class LogMealCandidatePayload(
    val candidates: ArrayList<LogMealFoodCandidate>
) : Serializable {
    constructor(candidates: List<LogMealFoodCandidate>) : this(ArrayList(candidates))

    companion object {
        @JvmField
        val serialVersionUID: Long = 1L
    }
}

internal fun Bundle.putLogMealCandidatePayload(
    key: String,
    candidates: List<LogMealFoodCandidate>
) {
    putSerializable(key, LogMealCandidatePayload(candidates))
}

internal fun Bundle.getLogMealCandidatePayload(key: String): List<LogMealFoodCandidate> {
    return BundleCompat.getSerializable(
        this,
        key,
        LogMealCandidatePayload::class.java
    )?.candidates.orEmpty()
}
