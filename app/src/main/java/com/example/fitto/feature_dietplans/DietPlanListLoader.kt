package com.example.fitto.feature_dietplans

import com.example.fitto.feature_dietplans.data.CustomDietPlanStore
import com.example.fitto.feature_dietplans.data.DietPlanCatalog
import com.example.fitto.feature_dietplans.data.DietPlanPreferenceStore
import com.example.fitto.feature_dietplans.model.CustomDietPlanLocalModel
import com.example.fitto.feature_dietplans.model.DietPlanUiModel

class DietPlanListLoader(
    private val customPlanSource: CustomPlanSource,
    private val preferenceSource: PreferenceSource,
    private val readyPlanSource: ReadyPlanSource = CatalogReadyPlanSource
) {

    fun load(includeDrafts: Boolean): DietPlanListSnapshot {
        val favoriteIds = preferenceSource.getFavoriteIds()
        val activePlanId = preferenceSource.getActivePlanId()
        val readyPlans = loadReadyPlans(
            favoriteIds = favoriteIds,
            activePlanId = activePlanId
        )
        val customPlans = customPlanSource.getPlans(includeDrafts = includeDrafts)

        return DietPlanListSnapshotResolver.resolve(
            customPlans = customPlans,
            readyPlans = readyPlans,
            favoriteIds = favoriteIds,
            activePlanId = activePlanId,
            customPlanMapper = { customPlan, isFavorite ->
                customPlanSource.toDietPlanUiModel(
                    plan = customPlan,
                    isFavorite = isFavorite
                )
            }
        )
    }

    fun loadReadyPlans(
        favoriteIds: Set<String> = preferenceSource.getFavoriteIds(),
        activePlanId: String? = preferenceSource.getActivePlanId()
    ): List<DietPlanUiModel> {
        return readyPlanSource.getReadyPlans(
            favoriteIds = favoriteIds,
            activePlanId = activePlanId
        )
    }

    interface CustomPlanSource {
        fun getPlans(includeDrafts: Boolean): List<CustomDietPlanLocalModel>

        fun toDietPlanUiModel(
            plan: CustomDietPlanLocalModel,
            isFavorite: Boolean
        ): DietPlanUiModel
    }

    interface PreferenceSource {
        fun getFavoriteIds(): Set<String>
        fun getActivePlanId(): String?
    }

    interface ReadyPlanSource {
        fun getReadyPlans(
            favoriteIds: Set<String>,
            activePlanId: String?
        ): List<DietPlanUiModel>
    }

    companion object {
        fun from(
            customPlanStore: CustomDietPlanStore,
            preferenceStore: DietPlanPreferenceStore
        ): DietPlanListLoader {
            return DietPlanListLoader(
                customPlanSource = object : CustomPlanSource {
                    override fun getPlans(includeDrafts: Boolean): List<CustomDietPlanLocalModel> {
                        return customPlanStore.getPlans(includeDrafts = includeDrafts)
                    }

                    override fun toDietPlanUiModel(
                        plan: CustomDietPlanLocalModel,
                        isFavorite: Boolean
                    ): DietPlanUiModel {
                        return customPlanStore.toDietPlanUiModel(
                            plan = plan,
                            isFavorite = isFavorite
                        )
                    }
                },
                preferenceSource = object : PreferenceSource {
                    override fun getFavoriteIds(): Set<String> {
                        return preferenceStore.getFavoriteIds()
                    }

                    override fun getActivePlanId(): String? {
                        return preferenceStore.getActivePlanId()
                    }
                }
            )
        }
    }
}

data class DietPlanListSnapshot(
    val customPlans: List<DietPlanUiModel>,
    val readyPlans: List<DietPlanUiModel>,
    val allPlans: List<DietPlanUiModel>,
    val favoriteIds: Set<String>,
    val activePlanId: String?
)

private object CatalogReadyPlanSource : DietPlanListLoader.ReadyPlanSource {
    override fun getReadyPlans(
        favoriteIds: Set<String>,
        activePlanId: String?
    ): List<DietPlanUiModel> {
        return DietPlanCatalog.getReadyPlans(
            favoriteIds = favoriteIds,
            activePlanId = activePlanId
        )
    }
}
