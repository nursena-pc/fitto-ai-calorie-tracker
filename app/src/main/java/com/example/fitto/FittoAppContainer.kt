package com.example.fitto

import android.content.Context
import com.example.fitto.core.coroutines.AppCoroutineDispatchers
import com.example.fitto.data.seed.SeedDataImporter
import com.example.fitto.feature_daily.DailyDashboardDefaultTargets
import com.example.fitto.feature_daily.DailyDashboardDependencies
import com.example.fitto.feature_daily.DailyDashboardDependenciesFactory
import com.example.fitto.feature_home.presentation.notifications.FittoNotificationChannels
import com.example.fitto.feature_home.presentation.social.SocialNotificationObserver
import com.example.fitto.feature_meal.AddMealCoordinator
import com.example.fitto.feature_meal.LocalAddMealDataSource

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

    fun createAddMealCoordinator(): AddMealCoordinator {
        return AddMealCoordinator(
            dataSource = LocalAddMealDataSource(appContext),
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
            onFastingTick = onFastingTick,
            onFastingFinish = onFastingFinish
        ).create()
    }
}

internal val Context.fittoAppContainer: FittoAppContainer
    get() = (applicationContext as FittoApplication).appContainer
