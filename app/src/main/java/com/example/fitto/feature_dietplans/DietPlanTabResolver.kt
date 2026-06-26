package com.example.fitto.feature_dietplans

import com.example.fitto.R

object DietPlanTabResolver {

    fun fromChipId(chipId: Int?): DietPlanTab {
        return when (chipId) {
            R.id.chipMyPlans -> DietPlanTab.MY
            R.id.chipDrafts -> DietPlanTab.DRAFTS
            R.id.chipFavorites -> DietPlanTab.SAVED
            else -> DietPlanTab.READY
        }
    }
}
