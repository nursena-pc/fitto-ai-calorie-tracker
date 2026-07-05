package com.example.fitto.feature_exercise

import com.example.fitto.feature_exercise.data.ExerciseLogLocalModel
import com.example.fitto.feature_exercise.model.ExerciseCategory
import com.example.fitto.feature_exercise.model.ExerciseFilterState
import com.example.fitto.feature_exercise.model.ExerciseIntensity
import com.example.fitto.feature_exercise.model.ExerciseUiModel

data class AddExerciseState(
    val activities: List<ExerciseUiModel> = emptyList(),
    val searchableTextById: Map<String, String> = emptyMap(),
    val selectedCategory: ExerciseCategory = ExerciseCategory.CARDIO,
    val selectedActivity: ExerciseUiModel? = null,
    val selectedIntensity: ExerciseIntensity = ExerciseIntensity.MODERATE,
    val durationMinutes: Int = DEFAULT_EXERCISE_DURATION_MINUTES,
    val filterState: ExerciseFilterState = ExerciseFilterState(),
    val rawSearchQuery: String = "",
    val userWeightKg: Double = DEFAULT_EXERCISE_USER_WEIGHT_KG,
    val editingLogId: String? = null
) {
    val isEditing: Boolean
        get() = editingLogId != null
}

class AddExerciseController(
    private val defaultUserWeightKg: Double = DEFAULT_EXERCISE_USER_WEIGHT_KG
) {

    var state: AddExerciseState = AddExerciseState(userWeightKg = defaultUserWeightKg)
        private set

    fun initialize(activities: List<ExerciseUiModel>): AddExerciseState {
        val selectedCategory = ExerciseCategory.CARDIO
        val selectedActivity = activities.firstOrNull { it.category == selectedCategory }

        state = AddExerciseState(
            activities = activities,
            searchableTextById = ExerciseActivitySearchCache.build(activities),
            selectedCategory = selectedCategory,
            selectedActivity = selectedActivity,
            durationMinutes = selectedActivity?.defaultDurationMinutes
                ?: DEFAULT_EXERCISE_DURATION_MINUTES,
            userWeightKg = defaultUserWeightKg
        )
        return state
    }

    fun selectCategory(category: ExerciseCategory): AddExerciseState {
        val selectedActivity = state.activities.firstOrNull { it.category == category }
        state = state.copy(
            selectedCategory = category,
            selectedActivity = selectedActivity,
            selectedIntensity = ExerciseIntensity.MODERATE,
            durationMinutes = selectedActivity?.defaultDurationMinutes
                ?: DEFAULT_EXERCISE_DURATION_MINUTES
        )
        return state
    }

    fun updateSearchQuery(rawQuery: String): AddExerciseState {
        state = state.copy(rawSearchQuery = rawQuery.trim())
        return state
    }

    fun applyFilter(filterState: ExerciseFilterState): AddExerciseState {
        state = state.copy(
            filterState = filterState,
            selectedIntensity = filterState.intensity ?: state.selectedIntensity
        )

        val visibleActivities = visibleActivities()
        if (visibleActivities.none { it.id == state.selectedActivity?.id }) {
            val firstVisible = visibleActivities.firstOrNull()
            state = state.copy(
                selectedActivity = firstVisible,
                durationMinutes = firstVisible?.defaultDurationMinutes
                    ?: DEFAULT_EXERCISE_DURATION_MINUTES
            )
        }
        return state
    }

    fun selectActivity(activity: ExerciseUiModel): AddExerciseState {
        state = state.copy(
            selectedCategory = activity.category,
            selectedActivity = activity,
            selectedIntensity = state.filterState.intensity ?: ExerciseIntensity.MODERATE,
            durationMinutes = activity.defaultDurationMinutes,
            editingLogId = null
        )
        return state
    }

    fun decreaseDuration(): AddExerciseState {
        state = state.copy(
            durationMinutes = (state.durationMinutes - DURATION_STEP_MINUTES)
                .coerceAtLeast(MIN_STEP_DURATION_MINUTES)
        )
        return state
    }

    fun increaseDuration(): AddExerciseState {
        state = state.copy(
            durationMinutes = (state.durationMinutes + DURATION_STEP_MINUTES)
                .coerceAtMost(MAX_EXERCISE_DURATION_MINUTES)
        )
        return state
    }

    fun setDuration(rawValue: String): Boolean {
        val duration = rawValue.trim().toIntOrNull()
            ?.takeIf { it in MIN_EXERCISE_DURATION_MINUTES..MAX_EXERCISE_DURATION_MINUTES }
            ?: return false

        state = state.copy(durationMinutes = duration)
        return true
    }

    fun cycleIntensity(): AddExerciseState {
        state = state.copy(selectedIntensity = state.selectedIntensity.next())
        return state
    }

    fun updateUserWeight(weightKg: Double?): AddExerciseState {
        state = state.copy(userWeightKg = weightKg ?: defaultUserWeightKg)
        return state
    }

    fun startEditing(
        log: ExerciseLogLocalModel,
        fallbackTitle: String,
        fallbackCategoryName: String,
        estimatedCaloriesText: String,
        intensityTitleProvider: (ExerciseIntensity) -> String
    ): AddExerciseState {
        val activity = ExerciseLegacyActivityResolver.resolveActivityForLog(
            log = log,
            activities = state.activities,
            fallbackTitle = fallbackTitle,
            fallbackCategoryName = fallbackCategoryName,
            estimatedCaloriesText = estimatedCaloriesText
        )
        val intensity = ExerciseIntensity.entries.firstOrNull { candidate ->
            intensityTitleProvider(candidate) == log.intensityName
        } ?: ExerciseIntensity.MODERATE

        state = state.copy(
            selectedCategory = activity.category,
            selectedActivity = activity,
            selectedIntensity = intensity,
            durationMinutes = log.durationMinutes.coerceIn(
                MIN_EXERCISE_DURATION_MINUTES,
                MAX_EXERCISE_DURATION_MINUTES
            ),
            filterState = ExerciseFilterState(),
            rawSearchQuery = "",
            editingLogId = log.id
        )
        return state
    }

    fun completeSave(): AddExerciseState {
        state = state.copy(editingLogId = null)
        return state
    }

    fun visibleActivities(): List<ExerciseUiModel> {
        return ExerciseActivityFilter.resolveVisibleActivities(
            activities = state.activities,
            searchableTextById = state.searchableTextById,
            selectedCategory = state.selectedCategory,
            filterState = state.filterState,
            selectedIntensity = state.selectedIntensity,
            rawQuery = state.rawSearchQuery,
            userWeightKg = state.userWeightKg
        )
    }

    fun caloriesForListActivity(activity: ExerciseUiModel): Int {
        return ExerciseCalorieCalculator.calculate(
            activity = activity,
            minutes = activity.defaultDurationMinutes,
            intensity = state.filterState.intensity ?: state.selectedIntensity,
            userWeightKg = state.userWeightKg
        )
    }

    fun selectedCalories(): Int {
        val activity = state.selectedActivity ?: return 0
        return ExerciseCalorieCalculator.calculate(
            activity = activity,
            minutes = state.durationMinutes,
            intensity = state.selectedIntensity,
            userWeightKg = state.userWeightKg
        )
    }
}

const val DEFAULT_EXERCISE_USER_WEIGHT_KG = 70.0
const val DEFAULT_EXERCISE_DURATION_MINUTES = 30
private const val MIN_EXERCISE_DURATION_MINUTES = 1
private const val MIN_STEP_DURATION_MINUTES = 5
private const val MAX_EXERCISE_DURATION_MINUTES = 240
private const val DURATION_STEP_MINUTES = 5
