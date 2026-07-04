package com.example.fitto.feature_home.presentation.profile

private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L
private const val MAX_MEASUREMENT_CM = 300f

enum class BodyMeasurementsChartRange {
    ONE_MONTH,
    THREE_MONTHS,
    ALL
}

enum class BodyMeasurementsScreen {
    EDITOR,
    HISTORY
}

data class BodyMeasurementChartPoint(
    val timestamp: Long,
    val value: Float
)

data class BodyMeasurementsChartSummary(
    val firstValue: Float,
    val lastValue: Float,
    val change: Float
)

data class BodyMeasurementsState(
    val genderKey: String,
    val currentMeasurements: BodyMeasurementsData,
    val draftMeasurements: BodyMeasurementsData,
    val historyItems: List<BodyMeasurementsHistoryItem>,
    val previewItem: BodyMeasurementsHistoryItem?,
    val selectedChartField: BodyMeasurementField,
    val selectedChartRange: BodyMeasurementsChartRange,
    val isChartPickerExpanded: Boolean,
    val screen: BodyMeasurementsScreen,
    val isSavingSnapshot: Boolean,
    val hasUnsavedChanges: Boolean,
    val chartPoints: List<BodyMeasurementChartPoint>,
    val chartSummary: BodyMeasurementsChartSummary?
) {
    val displayedMeasurements: BodyMeasurementsData
        get() = previewItem?.data ?: draftMeasurements

    val isReadOnly: Boolean
        get() = previewItem != null

    val displayedTimestamp: Long
        get() = previewItem?.createdAt ?: currentMeasurements.updatedAt

    val selectedHistoryItemId: String?
        get() = previewItem?.id

    val canSave: Boolean
        get() = !isReadOnly && draftMeasurements.filledCount > 0 && !isSavingSnapshot
}

data class BodyMeasurementInputResult(
    val normalizedInput: String,
    val accepted: Boolean,
    val state: BodyMeasurementsState
)

class BodyMeasurementsController(
    private val nowProvider: () -> Long = System::currentTimeMillis
) {
    private var genderKey: String = ""
    private var currentMeasurements: BodyMeasurementsData = BodyMeasurementsData()
    private var draftMeasurements: BodyMeasurementsData = BodyMeasurementsData()
    private var historyItems: List<BodyMeasurementsHistoryItem> = emptyList()
    private var selectedHistoryItemId: String? = null
    private var previewItem: BodyMeasurementsHistoryItem? = null
    private var selectedChartField: BodyMeasurementField = BodyMeasurementField.WAIST
    private var selectedChartRange: BodyMeasurementsChartRange = BodyMeasurementsChartRange.ALL
    private var isChartPickerExpanded: Boolean = false
    private var screen: BodyMeasurementsScreen = BodyMeasurementsScreen.EDITOR
    private var isSavingSnapshot: Boolean = false
    private var hasUnsavedChanges: Boolean = false

    val state: BodyMeasurementsState
        get() = buildState()

    fun updateProfile(
        genderKey: String,
        currentMeasurements: BodyMeasurementsData,
        historyItems: List<BodyMeasurementsHistoryItem>,
        allowDraftReplacement: Boolean
    ): BodyMeasurementsState {
        this.genderKey = genderKey
        this.currentMeasurements = currentMeasurements
        this.historyItems = historyItems

        if (!hasUnsavedChanges && allowDraftReplacement) {
            draftMeasurements = currentMeasurements
        }

        refreshSelectedPreview()
        return state
    }

    fun onMeasurementInput(
        field: BodyMeasurementField,
        rawInput: String
    ): BodyMeasurementInputResult {
        if (previewItem != null) {
            return BodyMeasurementInputResult(
                normalizedInput = rawInput,
                accepted = false,
                state = state
            )
        }

        val normalizedInput = normalizeInput(rawInput)
        val resolvedValue = resolveMeasurementValue(normalizedInput)
        if (!resolvedValue.accepted) {
            return BodyMeasurementInputResult(
                normalizedInput = normalizedInput,
                accepted = false,
                state = state
            )
        }

        draftMeasurements = draftMeasurements.withValue(field, resolvedValue.value)
        hasUnsavedChanges = hasMeasurementChanges(
            draft = draftMeasurements,
            current = currentMeasurements
        )

        return BodyMeasurementInputResult(
            normalizedInput = normalizedInput,
            accepted = true,
            state = state
        )
    }

    fun formatInputAfterEditing(rawInput: String): String {
        val value = rawInput.toFloatOrNull()
        return value
            ?.takeIf { it > 0f && it <= MAX_MEASUREMENT_CM }
            ?.let { String.format(java.util.Locale.US, "%.2f", it) }
            .orEmpty()
    }

    fun toggleChartPicker(): BodyMeasurementsState {
        isChartPickerExpanded = !isChartPickerExpanded
        return state
    }

    fun selectChartField(field: BodyMeasurementField): BodyMeasurementsState {
        selectedChartField = field
        isChartPickerExpanded = false
        return state
    }

    fun selectChartRange(range: BodyMeasurementsChartRange): BodyMeasurementsState {
        selectedChartRange = range
        return state
    }

    fun selectHistoryItem(itemId: String): BodyMeasurementsState {
        selectedHistoryItemId = itemId
        refreshSelectedPreview()
        screen = BodyMeasurementsScreen.EDITOR
        return state
    }

    fun clearHistoryPreview(): BodyMeasurementsState {
        selectedHistoryItemId = null
        previewItem = null
        return state
    }

    fun showHistory(): BodyMeasurementsState {
        screen = BodyMeasurementsScreen.HISTORY
        return state
    }

    fun showEditor(): BodyMeasurementsState {
        screen = BodyMeasurementsScreen.EDITOR
        return state
    }

    fun beginSave(): BodyMeasurementsData? {
        if (!state.canSave) return null

        val dataToSave = draftMeasurements.copy(updatedAt = nowProvider())
        isSavingSnapshot = true
        return dataToSave
    }

    fun completeSave(savedMeasurements: BodyMeasurementsData): BodyMeasurementsState {
        selectedHistoryItemId = null
        previewItem = null
        currentMeasurements = savedMeasurements
        draftMeasurements = savedMeasurements
        hasUnsavedChanges = false
        isSavingSnapshot = false
        return state
    }

    fun failSave(): BodyMeasurementsState {
        isSavingSnapshot = false
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

    private fun buildState(): BodyMeasurementsState {
        val chartPoints = resolveChartPoints()
        val chartSummary = chartPoints.takeIf { it.isNotEmpty() }?.let { points ->
            val firstValue = points.first().value
            val lastValue = points.last().value
            BodyMeasurementsChartSummary(
                firstValue = firstValue,
                lastValue = lastValue,
                change = lastValue - firstValue
            )
        }

        return BodyMeasurementsState(
            genderKey = genderKey,
            currentMeasurements = currentMeasurements,
            draftMeasurements = draftMeasurements,
            historyItems = historyItems,
            previewItem = previewItem,
            selectedChartField = selectedChartField,
            selectedChartRange = selectedChartRange,
            isChartPickerExpanded = isChartPickerExpanded,
            screen = screen,
            isSavingSnapshot = isSavingSnapshot,
            hasUnsavedChanges = hasUnsavedChanges,
            chartPoints = chartPoints,
            chartSummary = chartSummary
        )
    }

    private fun resolveChartPoints(): List<BodyMeasurementChartPoint> {
        val minimumTimestamp = when (selectedChartRange) {
            BodyMeasurementsChartRange.ONE_MONTH -> nowProvider() - 30L * MILLIS_PER_DAY
            BodyMeasurementsChartRange.THREE_MONTHS -> nowProvider() - 90L * MILLIS_PER_DAY
            BodyMeasurementsChartRange.ALL -> 0L
        }

        return historyItems
            .asReversed()
            .mapNotNull { item ->
                val value = item.data.getValue(selectedChartField) ?: return@mapNotNull null
                BodyMeasurementChartPoint(
                    timestamp = item.createdAt,
                    value = value
                )
            }
            .filter { point -> minimumTimestamp == 0L || point.timestamp >= minimumTimestamp }
    }

    private fun normalizeInput(rawInput: String): String {
        val builder = StringBuilder()
        var hasDecimalSeparator = false
        var decimalCount = 0

        rawInput.replace(',', '.').forEach { char ->
            when {
                char.isDigit() && !hasDecimalSeparator && builder.length < 3 -> {
                    builder.append(char)
                }

                char.isDigit() && hasDecimalSeparator && decimalCount < 2 -> {
                    builder.append(char)
                    decimalCount++
                }

                char == '.' && !hasDecimalSeparator && builder.isNotEmpty() -> {
                    builder.append(char)
                    hasDecimalSeparator = true
                }

                char == '.' && !hasDecimalSeparator && builder.isEmpty() -> {
                    builder.append("0.")
                    hasDecimalSeparator = true
                }
            }
        }

        return builder.toString()
    }

    private fun resolveMeasurementValue(normalizedInput: String): ResolvedMeasurementValue {
        if (normalizedInput.isBlank()) {
            return ResolvedMeasurementValue(value = null, accepted = true)
        }

        val value = normalizedInput.toFloatOrNull()
        return if (value != null && value > 0f && value <= MAX_MEASUREMENT_CM) {
            ResolvedMeasurementValue(value = value, accepted = true)
        } else {
            ResolvedMeasurementValue(value = null, accepted = false)
        }
    }

    private fun hasMeasurementChanges(
        draft: BodyMeasurementsData,
        current: BodyMeasurementsData
    ): Boolean {
        return BodyMeasurementField.entries.any { field ->
            draft.getValue(field) != current.getValue(field)
        }
    }

    private data class ResolvedMeasurementValue(
        val value: Float?,
        val accepted: Boolean
    )
}
