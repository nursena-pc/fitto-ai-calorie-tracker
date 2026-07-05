package com.example.fitto

import android.content.Context
import com.example.fitto.core.coroutines.AppCoroutineDispatchers
import com.example.fitto.feature_auth.AuthFeatureDependencies
import com.example.fitto.data.seed.SeedDataImporter
import com.example.fitto.feature_daily.DailyDashboardDefaultTargets
import com.example.fitto.feature_daily.DailyDashboardDependencies
import com.example.fitto.feature_daily.DailyDashboardDependenciesFactory
import com.example.fitto.feature_daily.DailyTrackingSources
import com.example.fitto.feature_dietplans.DietPlanFeatureDependencies
import com.example.fitto.feature_exercise.AddExerciseCoordinator
import com.example.fitto.feature_exercise.LocalAddExerciseDataSource
import com.example.fitto.feature_health.FittoStepCounterManager
import com.example.fitto.feature_health.HealthConnectConnectionCoordinator
import com.example.fitto.feature_health.HealthConnectConnectionDependencies
import com.example.fitto.feature_health.HealthConnectManager
import com.example.fitto.feature_home.presentation.notifications.FittoNotificationChannels
import com.example.fitto.feature_home.presentation.profile.ProfileFeatureDependencies
import com.example.fitto.feature_home.presentation.profile.reminders.ReminderFeatureDependencies
import com.example.fitto.feature_home.presentation.social.RealtimePresenceManager
import com.example.fitto.feature_home.presentation.social.SocialFeatureDependencies
import com.example.fitto.feature_home.presentation.social.SocialNotificationObserver
import com.example.fitto.feature_home.presentation.social.SocialRepository
import com.example.fitto.feature_meal.AddMealCoordinator
import com.example.fitto.feature_meal.LocalAddMealDataSource
import com.example.fitto.feature_recipes.RecipeFeatureDependencies
import com.example.fitto.feature_recipes.data.RecipeRepository
import com.example.fitto.feature_reports.ReportsFeatureDependencies
import com.example.fitto.feature_sleep.data.SleepLogStore

/** Application-level composition root for shared infrastructure and feature dependency graphs. */
internal class FittoAppContainer(
    context: Context,
    val coroutineDispatchers: AppCoroutineDispatchers = AppCoroutineDispatchers.production()
) {
    private val appContext = context.applicationContext

    val startupCoordinator = AppStartupCoordinator(
        importSeedData = { SeedDataImporter.importIfNeeded(appContext) },
        ensureNotificationChannels = { FittoNotificationChannels.ensureCreated(appContext) },
        startSocialNotificationObserver = { SocialNotificationObserver.start(appContext) }
    )

    val authFeatureDependencies: AuthFeatureDependencies by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        AuthFeatureDependencies(
            context = appContext,
            ioDispatcher = coroutineDispatchers.io
        )
    }

    val socialFeatureDependencies: SocialFeatureDependencies by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        SocialFeatureDependencies(
            repositoryFactory = ::SocialRepository,
            recipeRepositoryFactory = { RecipeRepository(appContext) },
            ioDispatcher = coroutineDispatchers.io,
            startPresence = RealtimePresenceManager::start
        )
    }

    val profileFeatureDependencies: ProfileFeatureDependencies by lazy(
        LazyThreadSafetyMode.SYNCHRONIZED
    ) {
        ProfileFeatureDependencies(
            context = appContext,
            ioDispatcher = coroutineDispatchers.io,
            socialRepository = socialFeatureDependencies.repository
        )
    }

    val reportsFeatureDependencies: ReportsFeatureDependencies by lazy(
        LazyThreadSafetyMode.SYNCHRONIZED
    ) {
        ReportsFeatureDependencies(
            context = appContext,
            profileRepository = profileFeatureDependencies.repository,
            ioDispatcher = coroutineDispatchers.io
        )
    }

    val reminderFeatureDependencies: ReminderFeatureDependencies by lazy(
        LazyThreadSafetyMode.SYNCHRONIZED
    ) {
        ReminderFeatureDependencies(appContext)
    }

    fun createAddExerciseCoordinator(): AddExerciseCoordinator {
        return AddExerciseCoordinator(
            dataSource = LocalAddExerciseDataSource(appContext),
            ioDispatcher = coroutineDispatchers.io
        )
    }

    fun createHealthConnectManager(): HealthConnectManager {
        return HealthConnectManager(appContext)
    }

    fun createHealthConnectConnectionDependencies(): HealthConnectConnectionDependencies {
        val manager = createHealthConnectManager()
        return HealthConnectConnectionDependencies(
            manager = manager,
            coordinator = HealthConnectConnectionCoordinator(manager)
        )
    }

    fun createAddMealCoordinator(): AddMealCoordinator {
        return AddMealCoordinator(
            dataSource = LocalAddMealDataSource(appContext),
            ioDispatcher = coroutineDispatchers.io
        )
    }


    fun createDietPlanFeatureDependencies(): DietPlanFeatureDependencies {
        return DietPlanFeatureDependencies(
            context = appContext,
            ioDispatcher = coroutineDispatchers.io
        )
    }

    fun createRecipeFeatureDependencies(): RecipeFeatureDependencies {
        return RecipeFeatureDependencies(
            context = appContext,
            ioDispatcher = coroutineDispatchers.io
        )
    }

    fun createDailyDashboardDependencies(
        defaultTargets: DailyDashboardDefaultTargets,
        onFastingTick: (planId: String, remainingMillis: Long) -> Unit,
        onFastingFinish: (planId: String) -> Unit
    ): DailyDashboardDependencies {
        return DailyDashboardDependenciesFactory(
            context = appContext,
            defaultTargets = defaultTargets,
            coroutineDispatchers = coroutineDispatchers,
            trackingSources = createDailyTrackingSources(),
            onFastingTick = onFastingTick,
            onFastingFinish = onFastingFinish
        ).create()
    }

    private fun createDailyTrackingSources(): DailyTrackingSources {
        val healthConnectManager = createHealthConnectManager()
        val stepCounterManager = FittoStepCounterManager(appContext)
        val sleepLogStore = SleepLogStore(appContext)
        return DailyTrackingSources(
            readHealthConnectSummary = healthConnectManager::readDailySummary,
            getCachedSteps = stepCounterManager::getCachedSteps,
            isStepCounterAvailable = stepCounterManager::isStepCounterAvailable,
            startStepCounter = stepCounterManager::start,
            stopStepCounter = stepCounterManager::stop,
            getSleepLogForDate = sleepLogStore::getLogForDate,
            saveSleepLog = sleepLogStore::saveLog
        )
    }
}

internal val Context.fittoAppContainer: FittoAppContainer
    get() = (applicationContext as FittoApplication).appContainer
