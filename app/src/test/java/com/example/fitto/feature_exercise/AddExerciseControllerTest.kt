package com.example.fitto.feature_exercise

import com.example.fitto.feature_exercise.data.ExerciseLogLocalModel
import com.example.fitto.feature_exercise.model.ExerciseCategory
import com.example.fitto.feature_exercise.model.ExerciseDurationRange
import com.example.fitto.feature_exercise.model.ExerciseFilterState
import com.example.fitto.feature_exercise.model.ExerciseIntensity
import com.example.fitto.feature_exercise.model.ExerciseUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AddExerciseControllerTest {

    @Test
    fun initialize_selectsFirstCardioActivityAndItsDefaultDuration() {
        val controller = AddExerciseController()

        controller.initialize(
            listOf(
                activity("strength", ExerciseCategory.STRENGTH, 20),
                activity("walk", ExerciseCategory.CARDIO, 35)
            )
        )

        assertEquals(ExerciseCategory.CARDIO, controller.state.selectedCategory)
        assertEquals("walk", controller.state.selectedActivity?.id)
        assertEquals(35, controller.state.durationMinutes)
        assertEquals(2, controller.state.searchableTextById.size)
    }

    @Test
    fun initializeIfNeeded_loadsCatalogForUninitializedState() {
        val controller = AddExerciseController()
        var loadCount = 0

        controller.initializeIfNeeded {
            loadCount += 1
            listOf(activity("walk", ExerciseCategory.CARDIO, 30))
        }

        assertEquals(1, loadCount)
        assertTrue(controller.state.isInitialized)
        assertEquals("walk", controller.state.selectedActivity?.id)
    }

    @Test
    fun initializeIfNeeded_preservesExistingScreenState() {
        val controller = controller()
        controller.selectCategory(ExerciseCategory.STRENGTH)
        controller.updateSearchQuery("weights")
        controller.increaseDuration()

        var reloadCount = 0
        controller.initializeIfNeeded {
            reloadCount += 1
            listOf(activity("replacement", ExerciseCategory.CARDIO, 10))
        }

        assertEquals(0, reloadCount)
        assertTrue(controller.state.isInitialized)
        assertEquals("weights", controller.state.selectedActivity?.id)
        assertEquals("weights", controller.state.rawSearchQuery)
        assertEquals(50, controller.state.durationMinutes)
        assertEquals(3, controller.state.activities.size)
    }

    @Test
    fun selectCategory_resetsSelectionDurationAndIntensity() {
        val controller = controller()
        controller.cycleIntensity()

        controller.selectCategory(ExerciseCategory.STRENGTH)

        assertEquals("weights", controller.state.selectedActivity?.id)
        assertEquals(45, controller.state.durationMinutes)
        assertEquals(ExerciseIntensity.MODERATE, controller.state.selectedIntensity)
    }

    @Test
    fun searchQuery_canRevealActivityOutsideSelectedCategory() {
        val controller = controller()

        controller.updateSearchQuery("ağırlık")

        assertEquals(listOf("weights"), controller.visibleActivities().map { it.id })
    }

    @Test
    fun applyFilter_updatesIntensityAndSelectsFirstVisibleActivity() {
        val controller = controller()

        controller.applyFilter(
            ExerciseFilterState(
                durationRange = ExerciseDurationRange.LONG,
                intensity = ExerciseIntensity.VIGOROUS
            )
        )

        assertEquals(ExerciseIntensity.VIGOROUS, controller.state.selectedIntensity)
        assertEquals("run", controller.state.selectedActivity?.id)
        assertEquals(60, controller.state.durationMinutes)
    }

    @Test
    fun selectActivity_clearsEditModeAndUsesFilterIntensity() {
        val controller = controller()
        controller.startEditing(
            log = log(),
            fallbackTitle = "Exercise",
            fallbackCategoryName = "Cardio",
            estimatedCaloriesText = "Estimated",
            intensityTitleProvider =(::intensityTitle)
        )
        controller.applyFilter(
            ExerciseFilterState(intensity = ExerciseIntensity.LIGHT)
        )

        controller.selectActivity(controller.state.activities.first { it.id == "weights" })

        assertNull(controller.state.editingLogId)
        assertEquals(ExerciseCategory.STRENGTH, controller.state.selectedCategory)
        assertEquals(ExerciseIntensity.LIGHT, controller.state.selectedIntensity)
        assertEquals(45, controller.state.durationMinutes)
    }

    @Test
    fun durationChanges_preserveExistingBoundsAndManualValidation() {
        val controller = controller()
        repeat(20) { controller.decreaseDuration() }
        assertEquals(5, controller.state.durationMinutes)

        repeat(60) { controller.increaseDuration() }
        assertEquals(240, controller.state.durationMinutes)

        assertTrue(controller.setDuration(" 1 "))
        assertEquals(1, controller.state.durationMinutes)
        assertFalse(controller.setDuration("241"))
        assertFalse(controller.setDuration("invalid"))
        assertEquals(1, controller.state.durationMinutes)
    }

    @Test
    fun updateUserWeight_usesConfiguredFallbackForMissingWeight() {
        val controller = AddExerciseController(defaultUserWeightKg = 72.0)
        controller.initialize(listOf(activity("walk", ExerciseCategory.CARDIO, 30)))

        controller.updateUserWeight(68.5)
        assertEquals(68.5, controller.state.userWeightKg, 0.0)

        controller.updateUserWeight(null)
        assertEquals(72.0, controller.state.userWeightKg, 0.0)
    }

    @Test
    fun selectedCalories_usesDurationIntensityAndUserWeight() {
        val controller = AddExerciseController()
        controller.initialize(
            listOf(activity("walk", ExerciseCategory.CARDIO, 30, metValue = 6.0))
        )
        controller.updateUserWeight(70.0)
        controller.cycleIntensity()

        assertEquals(263, controller.selectedCalories())
    }

    @Test
    fun startEditing_resolvesActivityAndClearsSearchAndFilters() {
        val controller = controller()
        controller.updateSearchQuery("run")
        controller.applyFilter(
            ExerciseFilterState(
                durationRange = ExerciseDurationRange.SHORT,
                intensity = ExerciseIntensity.LIGHT
            )
        )

        controller.startEditing(
            log = log(
                exerciseId = "missing",
                exerciseTitle = "Ağırlık",
                durationMinutes = 999,
                intensityName = "High"
            ),
            fallbackTitle = "Exercise",
            fallbackCategoryName = "Cardio",
            estimatedCaloriesText = "Estimated",
            intensityTitleProvider =(::intensityTitle)
        )

        assertEquals("log-1", controller.state.editingLogId)
        assertEquals("weights", controller.state.selectedActivity?.id)
        assertEquals(ExerciseCategory.STRENGTH, controller.state.selectedCategory)
        assertEquals(ExerciseIntensity.VIGOROUS, controller.state.selectedIntensity)
        assertEquals(240, controller.state.durationMinutes)
        assertEquals("", controller.state.rawSearchQuery)
        assertEquals(ExerciseFilterState(), controller.state.filterState)
    }

    @Test
    fun startEditing_unknownIntensityFallsBackToModerate() {
        val controller = controller()

        controller.startEditing(
            log = log(intensityName = "Unknown"),
            fallbackTitle = "Exercise",
            fallbackCategoryName = "Cardio",
            estimatedCaloriesText = "Estimated",
            intensityTitleProvider =(::intensityTitle)
        )

        assertEquals(ExerciseIntensity.MODERATE, controller.state.selectedIntensity)
    }

    @Test
    fun completeSave_clearsEditingState() {
        val controller = controller()
        controller.startEditing(
            log = log(),
            fallbackTitle = "Exercise",
            fallbackCategoryName = "Cardio",
            estimatedCaloriesText = "Estimated",
            intensityTitleProvider =(::intensityTitle)
        )

        controller.completeSave()

        assertFalse(controller.state.isEditing)
        assertNull(controller.state.editingLogId)
    }

    @Test
    fun listCalories_usesFilterIntensityWhenPresent() {
        val controller = controller()
        controller.updateUserWeight(70.0)
        controller.applyFilter(
            ExerciseFilterState(intensity = ExerciseIntensity.VIGOROUS)
        )
        val activity = controller.state.activities.first { it.id == "walk" }

        assertEquals(219, controller.caloriesForListActivity(activity))
    }

    private fun controller(): AddExerciseController {
        return AddExerciseController().apply {
            initialize(
                listOf(
                    activity("walk", ExerciseCategory.CARDIO, 30, 5.0, "Yürüyüş"),
                    activity("run", ExerciseCategory.CARDIO, 60, 8.0, "Koşu"),
                    activity("weights", ExerciseCategory.STRENGTH, 45, 6.0, "Ağırlık")
                )
            )
        }
    }

    private fun activity(
        id: String,
        category: ExerciseCategory,
        duration: Int,
        metValue: Double = 4.0,
        title: String = id
    ): ExerciseUiModel {
        return ExerciseUiModel(
            id = id,
            title = title,
            description = title,
            category = category,
            metValue = metValue,
            defaultDurationMinutes = duration,
            iconEmoji = "",
            searchKeywords = listOf(title)
        )
    }

    private fun log(
        exerciseId: String = "walk",
        exerciseTitle: String = "Yürüyüş",
        durationMinutes: Int = 30,
        intensityName: String = "Medium"
    ): ExerciseLogLocalModel {
        return ExerciseLogLocalModel(
            id = "log-1",
            entryDate = "2026-07-05",
            exerciseId = exerciseId,
            exerciseTitle = exerciseTitle,
            categoryName = "Cardio",
            durationMinutes = durationMinutes,
            intensityName = intensityName,
            metValue = 5.0,
            calories = 100,
            createdAt = 1L
        )
    }

    private fun intensityTitle(intensity: ExerciseIntensity): String {
        return when (intensity) {
            ExerciseIntensity.LIGHT -> "Low"
            ExerciseIntensity.MODERATE -> "Medium"
            ExerciseIntensity.VIGOROUS -> "High"
        }
    }
}
