package com.example.fitto.feature_dietplans

import androidx.annotation.StringRes
import com.example.fitto.R

object DietPlanFilterResultAction {

    fun reset(): DietPlanFilterResultEffect {
        return DietPlanFilterResultEffect(
            clearFilters = true,
            state = null,
            toastResId = R.string.diet_filter_reset_mock,
            closeScreen = true
        )
    }

    fun apply(state: DietPlanFilterState): DietPlanFilterResultEffect {
        return DietPlanFilterResultEffect(
            clearFilters = false,
            state = state,
            toastResId = R.string.diet_filter_apply_success,
            closeScreen = true
        )
    }
}

data class DietPlanFilterResultEffect(
    val clearFilters: Boolean,
    val state: DietPlanFilterState?,
    @StringRes val toastResId: Int,
    val closeScreen: Boolean
)
