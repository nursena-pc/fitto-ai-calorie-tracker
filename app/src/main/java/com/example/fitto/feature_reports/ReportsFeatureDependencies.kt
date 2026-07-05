package com.example.fitto.feature_reports

import android.content.Context
import com.example.fitto.feature_home.presentation.profile.ProfileRepository
import kotlinx.coroutines.CoroutineDispatcher
import java.time.LocalDate

/** Reports dependency graph supplied by the application composition root. */
internal class ReportsFeatureDependencies(
    context: Context,
    private val profileRepository: ProfileRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    private val appContext = context.applicationContext

    fun createDataCoordinator(
        daysBack: Int,
        initialSelectedDate: LocalDate = LocalDate.now()
    ): ReportsDataCoordinator {
        return ReportsDataCoordinator(
            nutritionDataSource = LocalReportNutritionDataSource(appContext),
            healthDataSource = ProfileReportHealthDataSource(profileRepository),
            daysBack = daysBack,
            initialSelectedDate = initialSelectedDate,
            ioDispatcher = ioDispatcher
        )
    }
}
