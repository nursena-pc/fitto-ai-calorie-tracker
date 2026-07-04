package com.example.fitto.feature_reports

import android.content.Context
import com.example.fitto.data.local.FittoDatabaseProvider
import com.example.fitto.data.local.session.UserSessionDataGuard
import com.example.fitto.feature_home.presentation.profile.ProfileRepository
import com.example.fitto.feature_home.presentation.profile.health.ProfileHealthData
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

internal interface ReportNutritionDataSource {
    suspend fun loadDays(
        dates: List<LocalDate>,
        locale: Locale
    ): List<ReportDay>
}

internal class LocalReportNutritionDataSource(
    context: Context
) : ReportNutritionDataSource {

    private val appContext = context.applicationContext
    private val mealDao = FittoDatabaseProvider.getDatabase(appContext).mealDao()

    override suspend fun loadDays(
        dates: List<LocalDate>,
        locale: Locale
    ): List<ReportDay> {
        val activeUserId = UserSessionDataGuard.getActiveUid(appContext)
            ?: LOCAL_ANONYMOUS_USER_ID

        return dates.map { date ->
            val summary = mealDao.getNutritionSummaryForDate(
                userId = activeUserId,
                date = date.toString()
            )

            ReportDay(
                date = date,
                label = date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale),
                calories = summary.totalCalories,
                protein = summary.totalProtein,
                carbs = summary.totalCarbs,
                fat = summary.totalFat
            )
        }
    }
}

internal interface ReportHealthDataSource {
    fun load(
        onSuccess: (ProfileHealthData) -> Unit,
        onError: () -> Unit
    )
}

internal class ProfileReportHealthDataSource(
    private val profileRepository: ProfileRepository = ProfileRepository()
) : ReportHealthDataSource {

    override fun load(
        onSuccess: (ProfileHealthData) -> Unit,
        onError: () -> Unit
    ) {
        profileRepository.loadProfile(
            onSuccess = { profile -> onSuccess(profile.healthData) },
            onError = { onError() }
        )
    }
}

private const val LOCAL_ANONYMOUS_USER_ID = "local_anonymous"
