package com.example.fitto.feature_dietplans

import com.example.fitto.feature_dietplans.data.DietPlanPreferenceStore
import com.example.fitto.feature_dietplans.model.FastingPlanType

class DietPlanPreferenceAction(
    private val preferenceStore: PreferenceStore
) {

    fun toggleFavorite(planId: String): DietPlanFavoriteResult {
        val isFavorite = preferenceStore.toggleFavorite(planId)
        return DietPlanFavoriteResult(
            planId = planId,
            isFavorite = isFavorite,
            activePlanId = preferenceStore.getActivePlanId()
        )
    }

    fun applyPlan(
        planId: String,
        fastingType: FastingPlanType? = null,
        customFastingHours: Int? = null
    ): DietPlanApplyResult {
        preferenceStore.saveActivePlan(
            planId = planId,
            fastingType = fastingType,
            customFastingHours = customFastingHours
        )
        preferenceStore.applyActivePlanToday()

        return DietPlanApplyResult(activePlanId = planId)
    }

    interface PreferenceStore {
        fun toggleFavorite(planId: String): Boolean

        fun saveActivePlan(
            planId: String,
            fastingType: FastingPlanType? = null,
            customFastingHours: Int? = null
        )

        fun applyActivePlanToday()
        fun getActivePlanId(): String?
    }

    companion object {
        fun from(preferenceStore: DietPlanPreferenceStore): DietPlanPreferenceAction {
            return DietPlanPreferenceAction(
                preferenceStore = object : PreferenceStore {
                    override fun toggleFavorite(planId: String): Boolean {
                        return preferenceStore.toggleFavorite(planId)
                    }

                    override fun saveActivePlan(
                        planId: String,
                        fastingType: FastingPlanType?,
                        customFastingHours: Int?
                    ) {
                        preferenceStore.saveActivePlan(
                            planId = planId,
                            fastingType = fastingType,
                            customFastingHours = customFastingHours
                        )
                    }

                    override fun applyActivePlanToday() {
                        preferenceStore.applyActivePlanToday()
                    }

                    override fun getActivePlanId(): String? {
                        return preferenceStore.getActivePlanId()
                    }
                }
            )
        }
    }
}

data class DietPlanFavoriteResult(
    val planId: String,
    val isFavorite: Boolean,
    val activePlanId: String?
)

data class DietPlanApplyResult(
    val activePlanId: String
)
