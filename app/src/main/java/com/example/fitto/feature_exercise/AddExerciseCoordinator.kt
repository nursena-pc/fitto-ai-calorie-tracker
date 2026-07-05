package com.example.fitto.feature_exercise

import com.example.fitto.feature_exercise.model.ExerciseUiModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AddExerciseCoordinator(
    private val dataSource: AddExerciseDataSource,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    fun loadActivities(): List<ExerciseUiModel> = dataSource.loadActivities()

    suspend fun loadUserWeightKg(): Double? = withContext(ioDispatcher) {
        dataSource.loadUserWeightKg()
    }

    fun loadLogs(entryDate: String): ExerciseLogSnapshot {
        return dataSource.loadLogs(entryDate)
    }

    fun saveLog(request: ExerciseLogSaveRequest): ExerciseLogSaveResult {
        return if (request.editingLogId == null) {
            ExerciseLogSaveResult.Added(dataSource.addLog(request))
        } else {
            dataSource.updateLog(request)
            ExerciseLogSaveResult.Updated
        }
    }

    fun deleteLog(logId: String) {
        dataSource.deleteLog(logId)
    }
}
