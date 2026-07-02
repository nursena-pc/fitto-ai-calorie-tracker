package com.example.fitto.feature_daily

import android.content.Context
import com.example.fitto.data.local.entity.UserProfileEntity
import com.example.fitto.data.local.repository.DietPlanSeedRepository
import com.example.fitto.data.local.repository.MealRepository
import com.example.fitto.data.local.repository.ProfileCacheRepository
import com.example.fitto.feature_daily.data.FastingSessionStore
import com.example.fitto.feature_daily.data.WaterTrackingStore
import com.example.fitto.feature_dietplans.data.CustomDietPlanStore
import com.example.fitto.feature_dietplans.data.DietPlanPreferenceStore
import com.example.fitto.feature_dietplans.data.UserNutritionTargetStore
import com.example.fitto.feature_dietplans.data.DietPlanCatalog
import com.example.fitto.feature_dietplans.planning.CustomDietMealPlanGenerator
import com.example.fitto.feature_dietplans.planning.DietMealPlanCatalog
import com.example.fitto.feature_exercise.data.ExerciseLogStore
import com.example.fitto.feature_health.FittoStepCounterManager
import com.example.fitto.feature_health.HealthConnectManager
import com.example.fitto.feature_sleep.data.SleepLogStore

/**
 * Dependencies retained by [DailyFragment] after construction.
 *
 * Repository and store details that are only needed to wire actions remain private to the
 * factory instead of leaking into the Fragment as long-lived fields.
 */
internal data class DailyDashboardDependencies(
    val initialDailyTargetCalories: Int,
    val profileMetricsLoadAction: DailyProfileMetricsLoadAction,
    val dailySummaryLoadAction: DailySummaryLoadAction,
    val weeklyProgressLoadAction: DailyWeeklyProgressLoadAction,
    val waterTrackingAction: DailyWaterTrackingAction,
    val activePlanLoadAction: DailyActivePlanLoadAction,
    val plannedMealAction: DailyPlannedMealAction,
    val activePlanApplyAction: DailyActivePlanApplyAction,
    val fastingAction: DailyFastingAction,
    val fastingCountdownCoordinator: DailyFastingCountdownCoordinator,
    val healthConnectSummaryAction: DailyHealthConnectSummaryAction,
    val stepCounterAction: DailyStepCounterAction,
    val sleepTrackingAction: DailySleepTrackingAction,
    val dailyAiAnalysisAction: DailyAiAnalysisAction
)

internal data class DailyDashboardDefaultTargets(
    val calories: Int,
    val proteinGrams: Int,
    val carbGrams: Int,
    val fatGrams: Int
)

/** Builds the complete Daily dashboard action graph in one place. */
internal class DailyDashboardDependenciesFactory(
    private val context: Context,
    private val defaultTargets: DailyDashboardDefaultTargets,
    private val onFastingTick: (planId: String, remainingMillis: Long) -> Unit,
    private val onFastingFinish: (planId: String) -> Unit
) {

    fun create(): DailyDashboardDependencies {
        val appContext = context.applicationContext
        val dietPlanStore = DietPlanPreferenceStore(context)
        val nutritionTargetStore = UserNutritionTargetStore(context)
        val customDietPlanStore = CustomDietPlanStore(appContext)
        val mealRepository = MealRepository(appContext)
        val dietPlanSeedRepository = DietPlanSeedRepository(appContext)
        val profileCacheRepository = ProfileCacheRepository(appContext)
        val waterTrackingStore = WaterTrackingStore(appContext)
        val fastingSessionStore = FastingSessionStore(context)
        val exerciseLogStore = ExerciseLogStore(context)

        val profileLoadAction = DailyProfileLoadAction(
            getCachedProfile = profileCacheRepository::getCurrentUserProfile,
            syncProfile = profileCacheRepository::syncCurrentUserProfileFromFirebase
        )
        val resolveNutritionTargets: (UserProfileEntity?) -> DailyNutritionTargetResolver.Targets = {
                profile ->
            DailyNutritionTargetResolver.resolveTargets(
                activeCustomPlanCalories = resolveActiveCustomPlanCalories(
                    dietPlanStore = dietPlanStore,
                    customDietPlanStore = customDietPlanStore
                ),
                profileDailyCalories = profile?.dailyCalorieTarget,
                storedDailyCalories = nutritionTargetStore.getDailyTargetCalories(),
                defaultDailyCalories = defaultTargets.calories,
                profileProteinGrams = profile?.proteinTargetGrams,
                defaultProteinGrams = defaultTargets.proteinGrams,
                profileCarbGrams = profile?.carbTargetGrams,
                defaultCarbGrams = defaultTargets.carbGrams,
                profileFatGrams = profile?.fatTargetGrams,
                defaultFatGrams = defaultTargets.fatGrams
            )
        }

        val profileMetricsLoadAction = DailyProfileMetricsLoadAction(
            loadProfile = profileLoadAction::load
        )
        val dailySummaryLoadAction = DailySummaryLoadAction(
            loadProfile = profileLoadAction::load,
            getMealSummariesForDate = mealRepository::getMealSummariesForDate,
            getNutritionSummaryForDate = mealRepository::getNutritionSummaryForDate,
            getExerciseSummaryForDate = exerciseLogStore::getSummaryForDate,
            getExerciseLogsForDate = exerciseLogStore::getLogsForDate,
            getMealEntryDetailsForDate = mealRepository::getMealEntryDetailsForDate,
            resolveNutritionTargets = resolveNutritionTargets
        )
        val weeklyProgressLoadAction = DailyWeeklyProgressLoadAction(
            loadProfile = profileLoadAction::load,
            getCaloriesForDate = { dateIso ->
                mealRepository.getNutritionSummaryForDate(dateIso).totalCalories
            },
            resolveNutritionTargets = resolveNutritionTargets
        )
        val waterTrackingAction = DailyWaterTrackingAction(
            getWaterMl = waterTrackingStore::getWaterMl,
            setWaterMl = waterTrackingStore::setWaterMl,
            addWaterMl = waterTrackingStore::addWaterMl,
            getVisibleBottleCount = waterTrackingStore::getVisibleBottleCount,
            increaseVisibleBottleCount = waterTrackingStore::increaseVisibleBottleCount,
            decreaseVisibleBottleCount = waterTrackingStore::decreaseVisibleBottleCount
        )

        val customMealPlanGenerator = CustomDietMealPlanGenerator(context)
        val customPlanLoadAction = DailyCustomPlanLoadAction(
            getAllFoods = mealRepository::getAllFoods,
            getCurrentPlanDay = dietPlanStore::getCurrentPlanDay,
            getCompletedMealIds = dietPlanStore::getCompletedPlannedMealIds,
            createActivePlan = customMealPlanGenerator::createActivePlan,
            buildDailyPlan = customMealPlanGenerator::buildDailyPlan
        )
        val seedPlanLoadAction = DailySeedPlanLoadAction(
            getDietPlanById = dietPlanSeedRepository::getDietPlanById,
            getPlannedMealsForDay = dietPlanSeedRepository::getPlannedMealUiModelsForDay,
            getCurrentPlanDay = dietPlanStore::getCurrentPlanDay,
            getCompletedMealIds = dietPlanStore::getCompletedPlannedMealIds
        )
        val catalogPlanLoadAction = DailyCatalogPlanLoadAction(
            getPlanDurationDays = DietMealPlanCatalog::getPlanDurationDays,
            getCurrentPlanDay = dietPlanStore::getCurrentPlanDay,
            getMealsForDay = DietMealPlanCatalog::getMealsForDay,
            getCompletedMealIds = dietPlanStore::getCompletedPlannedMealIds,
            getPlanTitle = { planId -> DietMealPlanCatalog.getPlanTitle(context, planId) },
            getPlanBadge = { planId -> DietMealPlanCatalog.getPlanBadge(context, planId) },
            getPlanIcon = DietMealPlanCatalog::getPlanIcon
        )
        val activePlanLoadAction = DailyActivePlanLoadAction(
            getActivePlanId = dietPlanStore::getActivePlanId,
            getActiveFastingType = dietPlanStore::getActiveFastingType,
            getCustomFastingHours = dietPlanStore::getCustomFastingHours,
            getCustomPlan = customDietPlanStore::getPlan,
            createEmptyActivePlan = { DietPlanCatalog.createEmptyActivePlan(context) },
            createCatalogActivePlan = { planId, fastingType, customFastingHours ->
                DietPlanCatalog.getActivePlan(
                    context = context,
                    planId = planId,
                    fastingType = fastingType,
                    customFastingHours = customFastingHours
                )
            },
            loadCustomPlan = customPlanLoadAction::load,
            loadSeedPlan = seedPlanLoadAction::load,
            loadCatalogPlan = catalogPlanLoadAction::load
        )
        val plannedMealAction = DailyPlannedMealAction(
            addPlannedMealEntry = { request, entryDate ->
                mealRepository.addPlannedMealEntry(
                    mealType = request.mealType,
                    title = request.title,
                    targetCalories = request.targetCalories,
                    plannedMealCalories = request.plannedMealCalories,
                    suggestedFoodNames = request.suggestedFoodNames,
                    planId = request.planId,
                    plannedMealId = request.plannedMealId,
                    entryDate = entryDate
                )
            },
            markPlannedMealCompleted = { completion ->
                dietPlanStore.markPlannedMealCompleted(
                    planId = completion.planId,
                    dayNumber = completion.dayNumber,
                    plannedMealId = completion.plannedMealId
                )
            }
        )
        val fastingAction = DailyFastingAction(
            isFastingPlan = DailyActivePlanPolicy::isFastingPlan,
            getFastingTypeName = { dietPlanStore.getActiveFastingType()?.name },
            getCustomFastingHours = dietPlanStore::getCustomFastingHours,
            getRemainingMillis = { planId ->
                fastingSessionStore.getRemainingMillis(planId)
            },
            startFasting = { planId, fastingHours ->
                fastingSessionStore.startFasting(planId, fastingHours)
            },
            clearFasting = fastingSessionStore::clear
        )
        val fastingCountdownCoordinator = DailyFastingCountdownCoordinator(
            timerFactory = AndroidDailyCountdownTimerFactory(),
            onTick = onFastingTick,
            onFinish = onFastingFinish
        )

        val healthConnectManager = HealthConnectManager(appContext)
        val stepCounterManager = FittoStepCounterManager(appContext)
        val sleepLogStore = SleepLogStore(appContext)

        return DailyDashboardDependencies(
            initialDailyTargetCalories = nutritionTargetStore
                .getDailyTargetCalories()
                .takeIf { it > 0 }
                ?: defaultTargets.calories,
            profileMetricsLoadAction = profileMetricsLoadAction,
            dailySummaryLoadAction = dailySummaryLoadAction,
            weeklyProgressLoadAction = weeklyProgressLoadAction,
            waterTrackingAction = waterTrackingAction,
            activePlanLoadAction = activePlanLoadAction,
            plannedMealAction = plannedMealAction,
            activePlanApplyAction = DailyActivePlanApplyAction(
                applyActivePlanToday = dietPlanStore::applyActivePlanToday
            ),
            fastingAction = fastingAction,
            fastingCountdownCoordinator = fastingCountdownCoordinator,
            healthConnectSummaryAction = DailyHealthConnectSummaryAction(
                readDailySummary = healthConnectManager::readDailySummary
            ),
            stepCounterAction = DailyStepCounterAction(
                getCachedSteps = stepCounterManager::getCachedSteps,
                isStepCounterAvailable = stepCounterManager::isStepCounterAvailable,
                startStepCounter = stepCounterManager::start,
                stopStepCounter = stepCounterManager::stop
            ),
            sleepTrackingAction = DailySleepTrackingAction(
                getLogForDate = sleepLogStore::getLogForDate,
                saveLog = sleepLogStore::saveLog
            ),
            dailyAiAnalysisAction = DailyAiAnalysisActionFactory.create(appContext)
        )
    }

    private fun resolveActiveCustomPlanCalories(
        dietPlanStore: DietPlanPreferenceStore,
        customDietPlanStore: CustomDietPlanStore
    ): Int? {
        val activePlanId = dietPlanStore.getActivePlanId() ?: return null
        if (!DailyActivePlanPolicy.isCustomPlan(activePlanId)) return null

        return customDietPlanStore.getPlan(activePlanId)
            ?.dailyCalories
            ?.takeIf { it > 0 }
    }
}
