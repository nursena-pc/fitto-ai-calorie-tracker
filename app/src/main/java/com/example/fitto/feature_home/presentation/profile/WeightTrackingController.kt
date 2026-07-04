package com.example.fitto.feature_home.presentation.profile

import com.example.fitto.feature_home.presentation.profile.health.ProfileHealthData
import com.example.fitto.feature_home.presentation.profile.health.WeightHistoryItem
import kotlin.math.abs

private const val WEIGHT_CHANGE_EPSILON = 0.0005
private const val MAX_VISIBLE_WEIGHT_HISTORY_ITEMS = 30

private val WEIGHT_INPUT_PATTERN = Regex("^\\d{0,3}([,.]\\d{0,3})?$")

enum class WeightTrackingScreen {
    CURRENT,
    HISTORY
}

data class WeightChartSummary(
    val firstWeightKg: Double,
    val lastWeightKg: Double,
    val changeKg: Double
)

data class WeightTrackingState(
    val healthData: ProfileHealthData,
    val draftWeightKg: Double?,
    val historyItems: List<WeightHistoryItem>,
    val previewItem: WeightHistoryItem?,
    val screen: WeightTrackingScreen,
    val isSavingWeight: Boolean,
    val hasUnsavedWeightInput: Boolean,
    val chartSummary: WeightChartSummary?
) {
    val displayedWeightKg: Double
        get() = previewItem?.weightKg ?: draftWeightKg ?: 0.0

    val selectedHistoryItemId: String?
        get() = previewItem?.id

    val isReadOnly: Boolean
        get() = previewItem != null

    val canSave: Boolean
        get() = !isReadOnly && (draftWeightKg ?: 0.0) > 0.0 && !isSavingWeight

    val visibleHistoryItems: List<WeightHistoryItem>
        get() = historyItems.take(MAX_VISIBLE_WEIGHT_HISTORY_ITEMS)
}

data class WeightInputResult(
    val accepted: Boolean,
    val parsedWeightKg: Double?,
    val state: WeightTrackingState
)

class WeightTrackingController {
    private var healthData: ProfileHealthData = ProfileHealthData.Empty
    private var draftWeightKg: Double? = null
    private var historyItems: List<WeightHistoryItem> = emptyList()
    private var selectedHistoryItemId: String? = null
    private var previewItem: WeightHistoryItem? = null
    private var screen: WeightTrackingScreen = WeightTrackingScreen.CURRENT
    private var isSavingWeight: Boolean = false
    private var hasUnsavedWeightInput: Boolean = false

    val state: WeightTrackingState
        get() = buildState()

    fun updateProfile(
        healthData: ProfileHealthData,
        allowDraftReplacement: Boolean
    ): WeightTrackingState {
        this.healthData = healthData
        historyItems = healthData.history

        if (allowDraftReplacement) {
            draftWeightKg = healthData.currentWeightKg.takeIf { it > 0.0 }
            hasUnsavedWeightInput = false
        } else {
            hasUnsavedWeightInput = draftWeightKg != null &&
                abs(draftWeightKg.orZero() - healthData.currentWeightKg) > WEIGHT_CHANGE_EPSILON
        }

        refreshSelectedPreview()
        return state
    }

    fun onWeightInput(rawInput: String): WeightInputResult {
        if (previewItem != null) {
            return WeightInputResult(
                accepted = false,
                parsedWeightKg = draftWeightKg,
                state = state
            )
        }

        val trimmedInput = rawInput.trim()
        if (!isAcceptedInput(trimmedInput)) {
            return WeightInputResult(
                accepted = false,
                parsedWeightKg = draftWeightKg,
                state = state
            )
        }

        val parsedWeight = parseWeight(trimmedInput)
        draftWeightKg = parsedWeight
        hasUnsavedWeightInput = parsedWeight != null &&
            abs(parsedWeight - healthData.currentWeightKg) > WEIGHT_CHANGE_EPSILON

        return WeightInputResult(
            accepted = true,
            parsedWeightKg = parsedWeight,
            state = state
        )
    }

    fun formatInputAfterEditing(rawInput: String): Double? {
        return parseWeight(rawInput.trim())?.takeIf { it > 0.0 }
    }

    fun selectHistoryItem(itemId: String): WeightTrackingState {
        selectedHistoryItemId = itemId
        refreshSelectedPreview()
        screen = WeightTrackingScreen.CURRENT
        return state
    }

    fun clearHistoryPreview(): WeightTrackingState {
        selectedHistoryItemId = null
        previewItem = null
        return state
    }

    fun showHistory(): WeightTrackingState {
        screen = WeightTrackingScreen.HISTORY
        return state
    }

    fun showCurrent(): WeightTrackingState {
        screen = WeightTrackingScreen.CURRENT
        return state
    }

    fun beginSave(): Double? {
        if (!state.canSave) return null

        isSavingWeight = true
        return draftWeightKg
    }

    fun completeSave(savedWeightKg: Double): WeightTrackingState {
        selectedHistoryItemId = null
        previewItem = null
        draftWeightKg = savedWeightKg
        hasUnsavedWeightInput = false
        isSavingWeight = false
        return state
    }

    fun failSave(): WeightTrackingState {
        isSavingWeight = false
        return state
    }

    private fun refreshSelectedPreview() {
        val selectedId = selectedHistoryItemId
        if (selectedId == null) {
            previewItem = null
            return
        }

        val selectedItem = historyItems.firstOrNull { it.id == selectedId }
        if (selectedItem == null) {
            selectedHistoryItemId = null
            previewItem = null
        } else {
            previewItem = selectedItem
        }
    }

    private fun buildState(): WeightTrackingState {
        val chartPoints = healthData.chartPoints
        val chartSummary = chartPoints.takeIf { it.isNotEmpty() }?.let { points ->
            val firstWeight = points.first().weightKg
            val lastWeight = points.last().weightKg
            WeightChartSummary(
                firstWeightKg = firstWeight,
                lastWeightKg = lastWeight,
                changeKg = lastWeight - firstWeight
            )
        }

        return WeightTrackingState(
            healthData = healthData,
            draftWeightKg = draftWeightKg,
            historyItems = historyItems,
            previewItem = previewItem,
            screen = screen,
            isSavingWeight = isSavingWeight,
            hasUnsavedWeightInput = hasUnsavedWeightInput,
            chartSummary = chartSummary
        )
    }

    private fun isAcceptedInput(rawInput: String): Boolean {
        return rawInput.isEmpty() || WEIGHT_INPUT_PATTERN.matches(rawInput)
    }

    private fun parseWeight(rawInput: String): Double? {
        if (rawInput.isBlank() || rawInput == "." || rawInput == ",") return null
        return rawInput.replace(',', '.').toDoubleOrNull()
    }

    private fun Double?.orZero(): Double = this ?: 0.0
}
