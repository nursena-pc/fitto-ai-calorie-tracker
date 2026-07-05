package com.example.fitto.feature_exercise

import com.example.fitto.feature_exercise.data.ExerciseDailySummary
import com.example.fitto.feature_exercise.data.ExerciseLogLocalModel
import com.example.fitto.feature_exercise.model.ExerciseCategory
import com.example.fitto.feature_exercise.model.ExerciseUiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AddExerciseCoordinatorTest {

    @Test
    fun loadActivities_forwardsCatalogResult() {
        val source = FakeAddExerciseDataSource()
        val coordinator = coordinator(source)

        assertSame(source.activities, coordinator.loadActivities())
    }

    @Test
    fun loadUserWeightKg_forwardsProfileWeight() = runBlocking {
        val source = FakeAddExerciseDataSource(userWeightKg = 74.5)
        val coordinator = coordinator(source)

        assertEquals(74.5, coordinator.loadUserWeightKg() ?: 0.0, 0.0)
    }

    @Test
    fun loadLogs_forwardsRequestedDateAndSnapshot() {
        val source = FakeAddExerciseDataSource()
        val coordinator = coordinator(source)

        val result = coordinator.loadLogs("2026-07-05")

        assertEquals("2026-07-05", source.loadedDate)
        assertSame(source.snapshot, result)
    }

    @Test
    fun saveLog_withoutEditingId_addsLog() {
        val source = FakeAddExerciseDataSource()
        val coordinator = coordinator(source)
        val request = request(editingLogId = null)

        val result = coordinator.saveLog(request)

        assertSame(request, source.addedRequest)
        assertTrue(result is ExerciseLogSaveResult.Added)
    }

    @Test
    fun saveLog_withEditingId_updatesLog() {
        val source = FakeAddExerciseDataSource()
        val coordinator = coordinator(source)
        val request = request(editingLogId = "log-1")

        val result = coordinator.saveLog(request)

        assertSame(request, source.updatedRequest)
        assertEquals(ExerciseLogSaveResult.Updated, result)
    }

    @Test
    fun deleteLog_forwardsIdentifier() {
        val source = FakeAddExerciseDataSource()
        val coordinator = coordinator(source)

        coordinator.deleteLog("log-1")

        assertEquals("log-1", source.deletedLogId)
    }

    private fun coordinator(source: AddExerciseDataSource): AddExerciseCoordinator {
        return AddExerciseCoordinator(
            dataSource = source,
            ioDispatcher = Dispatchers.Unconfined
        )
    }

    private fun request(editingLogId: String?): ExerciseLogSaveRequest {
        return ExerciseLogSaveRequest(
            editingLogId = editingLogId,
            entryDate = "2026-07-05",
            exerciseId = "walk",
            exerciseTitle = "Walk",
            categoryName = "Cardio",
            durationMinutes = 30,
            intensityName = "Moderate",
            metValue = 4.0,
            calories = 140
        )
    }

    private class FakeAddExerciseDataSource(
        private val userWeightKg: Double? = 70.0
    ) : AddExerciseDataSource {
        val activities = listOf(
            ExerciseUiModel(
                id = "walk",
                title = "Walk",
                description = "",
                category = ExerciseCategory.CARDIO,
                metValue = 4.0,
                defaultDurationMinutes = 30,
                iconEmoji = ""
            )
        )
        val savedLog = ExerciseLogLocalModel(
            id = "saved",
            entryDate = "2026-07-05",
            exerciseId = "walk",
            exerciseTitle = "Walk",
            categoryName = "Cardio",
            durationMinutes = 30,
            intensityName = "Moderate",
            metValue = 4.0,
            calories = 140,
            createdAt = 1L
        )
        val snapshot = ExerciseLogSnapshot(
            logs = listOf(savedLog),
            summary = ExerciseDailySummary(
                totalCalories = 140,
                totalDurationMinutes = 30,
                logCount = 1
            )
        )
        var loadedDate: String? = null
        var addedRequest: ExerciseLogSaveRequest? = null
        var updatedRequest: ExerciseLogSaveRequest? = null
        var deletedLogId: String? = null

        override fun loadActivities(): List<ExerciseUiModel> = activities

        override suspend fun loadUserWeightKg(): Double? = userWeightKg

        override fun loadLogs(entryDate: String): ExerciseLogSnapshot {
            loadedDate = entryDate
            return snapshot
        }

        override fun addLog(request: ExerciseLogSaveRequest): ExerciseLogLocalModel {
            addedRequest = request
            return savedLog
        }

        override fun updateLog(request: ExerciseLogSaveRequest) {
            updatedRequest = request
        }

        override fun deleteLog(logId: String) {
            deletedLogId = logId
        }
    }
}
