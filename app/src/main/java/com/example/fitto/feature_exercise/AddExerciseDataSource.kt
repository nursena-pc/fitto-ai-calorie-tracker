package com.example.fitto.feature_exercise

import android.content.Context
import com.example.fitto.data.local.repository.ProfileCacheRepository
import com.example.fitto.feature_exercise.data.ExerciseCatalog
import com.example.fitto.feature_exercise.data.ExerciseDailySummary
import com.example.fitto.feature_exercise.data.ExerciseLogLocalModel
import com.example.fitto.feature_exercise.data.ExerciseLogStore
import com.example.fitto.feature_exercise.model.ExerciseUiModel

data class ExerciseLogSnapshot(
    val logs: List<ExerciseLogLocalModel>,
    val summary: ExerciseDailySummary
)

data class ExerciseLogSaveRequest(
    val editingLogId: String?,
    val entryDate: String,
    val exerciseId: String,
    val exerciseTitle: String,
    val categoryName: String,
    val durationMinutes: Int,
    val intensityName: String,
    val metValue: Double,
    val calories: Int
)

sealed interface ExerciseLogSaveResult {
    data class Added(val log: ExerciseLogLocalModel) : ExerciseLogSaveResult
    data object Updated : ExerciseLogSaveResult
}

interface AddExerciseDataSource {
    fun loadActivities(): List<ExerciseUiModel>
    suspend fun loadUserWeightKg(): Double?
    fun loadLogs(entryDate: String): ExerciseLogSnapshot
    fun addLog(request: ExerciseLogSaveRequest): ExerciseLogLocalModel
    fun updateLog(request: ExerciseLogSaveRequest)
    fun deleteLog(logId: String)
}

class LocalAddExerciseDataSource(
    context: Context,
    private val exerciseLogStore: ExerciseLogStore = ExerciseLogStore(context),
    private val profileCacheRepository: ProfileCacheRepository = ProfileCacheRepository(
        context.applicationContext
    )
) : AddExerciseDataSource {

    private val applicationContext = context.applicationContext

    override fun loadActivities(): List<ExerciseUiModel> {
        return ExerciseCatalog.load(
            context = applicationContext,
            forceReload = true
        )
    }

    override suspend fun loadUserWeightKg(): Double? {
        val profile = profileCacheRepository.getCurrentUserProfile()
            ?: profileCacheRepository.syncCurrentUserProfileFromFirebase()
        return ExerciseProfileWeightResolver.resolveWeightKg(profile)
    }

    override fun loadLogs(entryDate: String): ExerciseLogSnapshot {
        return ExerciseLogSnapshot(
            logs = exerciseLogStore.getLogsForDate(entryDate),
            summary = exerciseLogStore.getSummaryForDate(entryDate)
        )
    }

    override fun addLog(request: ExerciseLogSaveRequest): ExerciseLogLocalModel {
        return exerciseLogStore.addLog(
            entryDate = request.entryDate,
            exerciseId = request.exerciseId,
            exerciseTitle = request.exerciseTitle,
            categoryName = request.categoryName,
            durationMinutes = request.durationMinutes,
            intensityName = request.intensityName,
            metValue = request.metValue,
            calories = request.calories
        )
    }

    override fun updateLog(request: ExerciseLogSaveRequest) {
        exerciseLogStore.updateLog(
            logId = requireNotNull(request.editingLogId),
            exerciseId = request.exerciseId,
            exerciseTitle = request.exerciseTitle,
            categoryName = request.categoryName,
            durationMinutes = request.durationMinutes,
            intensityName = request.intensityName,
            metValue = request.metValue,
            calories = request.calories
        )
    }

    override fun deleteLog(logId: String) {
        exerciseLogStore.deleteLog(logId)
    }
}
