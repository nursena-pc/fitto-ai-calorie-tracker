package com.example.fitto.feature_home.presentation.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BodyMeasurementsControllerTest {

    @Test
    fun `profile update initializes editable draft and chart state`() {
        val current = BodyMeasurementsData(waistCm = 82f, updatedAt = 400L)
        val history = listOf(historyItem("latest", 300L, waist = 81f))
        val controller = BodyMeasurementsController(nowProvider = { 1_000L })

        val state = controller.updateProfile(
            genderKey = "female",
            currentMeasurements = current,
            historyItems = history,
            allowDraftReplacement = true
        )

        assertEquals("female", state.genderKey)
        assertEquals(current, state.currentMeasurements)
        assertEquals(current, state.draftMeasurements)
        assertEquals(history, state.historyItems)
        assertFalse(state.isReadOnly)
        assertFalse(state.hasUnsavedChanges)
        assertTrue(state.canSave)
        assertEquals(listOf(81f), state.chartPoints.map { it.value })
    }

    @Test
    fun `measurement input is normalized and updates unsaved draft`() {
        val controller = controllerWithCurrent(BodyMeasurementsData(waistCm = 80f))

        val result = controller.onMeasurementInput(
            field = BodyMeasurementField.WAIST,
            rawInput = "12,345 cm"
        )

        assertEquals("12.34", result.normalizedInput)
        assertTrue(result.accepted)
        assertEquals(12.34f, result.state.draftMeasurements.waistCm ?: 0f, 0.001f)
        assertTrue(result.state.hasUnsavedChanges)
        assertTrue(result.state.canSave)
    }

    @Test
    fun `blank input clears field while invalid value preserves previous draft`() {
        val controller = controllerWithCurrent(BodyMeasurementsData(waistCm = 80f))

        val cleared = controller.onMeasurementInput(BodyMeasurementField.WAIST, "")
        val invalid = controller.onMeasurementInput(BodyMeasurementField.WAIST, "999")

        assertTrue(cleared.accepted)
        assertNull(cleared.state.draftMeasurements.waistCm)
        assertFalse(invalid.accepted)
        assertNull(invalid.state.draftMeasurements.waistCm)
    }

    @Test
    fun `profile refresh preserves unsaved draft and refreshes current value`() {
        val controller = controllerWithCurrent(BodyMeasurementsData(waistCm = 80f))
        controller.onMeasurementInput(BodyMeasurementField.WAIST, "81")

        val state = controller.updateProfile(
            genderKey = "male",
            currentMeasurements = BodyMeasurementsData(waistCm = 82f),
            historyItems = emptyList(),
            allowDraftReplacement = true
        )

        assertEquals(82f, state.currentMeasurements.waistCm ?: 0f, 0f)
        assertEquals(81f, state.draftMeasurements.waistCm ?: 0f, 0f)
        assertTrue(state.hasUnsavedChanges)
    }

    @Test
    fun `profile refresh can keep clean draft while an input owns focus`() {
        val controller = controllerWithCurrent(BodyMeasurementsData(waistCm = 80f))

        val state = controller.updateProfile(
            genderKey = "male",
            currentMeasurements = BodyMeasurementsData(waistCm = 84f),
            historyItems = emptyList(),
            allowDraftReplacement = false
        )

        assertEquals(84f, state.currentMeasurements.waistCm ?: 0f, 0f)
        assertEquals(80f, state.draftMeasurements.waistCm ?: 0f, 0f)
        assertFalse(state.hasUnsavedChanges)
    }

    @Test
    fun `history preview is read only and follows refreshed history item`() {
        val first = historyItem("selected", 100L, waist = 75f)
        val controller = BodyMeasurementsController(nowProvider = { 1_000L })
        controller.updateProfile("", BodyMeasurementsData(waistCm = 80f), listOf(first), true)

        val selected = controller.selectHistoryItem("selected")
        val rejectedInput = controller.onMeasurementInput(BodyMeasurementField.WAIST, "90")
        val refreshed = historyItem("selected", 200L, waist = 76f)
        val refreshedState = controller.updateProfile(
            genderKey = "",
            currentMeasurements = BodyMeasurementsData(waistCm = 80f),
            historyItems = listOf(refreshed),
            allowDraftReplacement = true
        )

        assertTrue(selected.isReadOnly)
        assertEquals(75f, selected.displayedMeasurements.waistCm ?: 0f, 0f)
        assertFalse(rejectedInput.accepted)
        assertEquals(76f, refreshedState.displayedMeasurements.waistCm ?: 0f, 0f)
        assertEquals(200L, refreshedState.displayedTimestamp)
    }

    @Test
    fun `removed selected history item clears preview`() {
        val controller = BodyMeasurementsController(nowProvider = { 1_000L })
        controller.updateProfile(
            genderKey = "",
            currentMeasurements = BodyMeasurementsData(waistCm = 80f),
            historyItems = listOf(historyItem("selected", 100L, waist = 75f)),
            allowDraftReplacement = true
        )
        controller.selectHistoryItem("selected")

        val state = controller.updateProfile(
            genderKey = "",
            currentMeasurements = BodyMeasurementsData(waistCm = 80f),
            historyItems = emptyList(),
            allowDraftReplacement = true
        )

        assertFalse(state.isReadOnly)
        assertNull(state.selectedHistoryItemId)
        assertEquals(80f, state.displayedMeasurements.waistCm ?: 0f, 0f)
    }

    @Test
    fun `chart range filters points inclusively and keeps chronological order`() {
        val day = 24L * 60L * 60L * 1000L
        val now = 100L * day
        val controller = BodyMeasurementsController(nowProvider = { now })
        controller.updateProfile(
            genderKey = "",
            currentMeasurements = BodyMeasurementsData(),
            historyItems = listOf(
                historyItem("new", now - 5L * day, waist = 83f),
                historyItem("boundary", now - 30L * day, waist = 81f),
                historyItem("old", now - 40L * day, waist = 79f)
            ),
            allowDraftReplacement = true
        )

        val state = controller.selectChartRange(BodyMeasurementsChartRange.ONE_MONTH)

        assertEquals(listOf("boundary", "new"), state.chartPoints.map { point ->
            when (point.timestamp) {
                now - 30L * day -> "boundary"
                else -> "new"
            }
        })
        assertEquals(listOf(81f, 83f), state.chartPoints.map { it.value })
        assertEquals(81f, state.chartSummary?.firstValue ?: 0f, 0f)
        assertEquals(83f, state.chartSummary?.lastValue ?: 0f, 0f)
        assertEquals(2f, state.chartSummary?.change ?: 0f, 0f)
    }

    @Test
    fun `chart field selection closes picker and resolves selected values`() {
        val controller = BodyMeasurementsController(nowProvider = { 1_000L })
        controller.updateProfile(
            genderKey = "",
            currentMeasurements = BodyMeasurementsData(),
            historyItems = listOf(
                BodyMeasurementsHistoryItem(
                    id = "latest",
                    createdAt = 200L,
                    data = BodyMeasurementsData(waistCm = 82f, chestCm = 95f)
                ),
                BodyMeasurementsHistoryItem(
                    id = "oldest",
                    createdAt = 100L,
                    data = BodyMeasurementsData(waistCm = 80f, chestCm = 90f)
                )
            ),
            allowDraftReplacement = true
        )
        controller.toggleChartPicker()

        val state = controller.selectChartField(BodyMeasurementField.CHEST)

        assertEquals(BodyMeasurementField.CHEST, state.selectedChartField)
        assertFalse(state.isChartPickerExpanded)
        assertEquals(listOf(90f, 95f), state.chartPoints.map { it.value })
        assertEquals(5f, state.chartSummary?.change ?: 0f, 0f)
    }

    @Test
    fun `save lifecycle timestamps draft and restores save availability after error`() {
        val controller = BodyMeasurementsController(nowProvider = { 5_000L })
        controller.updateProfile("", BodyMeasurementsData(), emptyList(), true)
        controller.onMeasurementInput(BodyMeasurementField.WAIST, "81")

        val saveData = controller.beginSave()

        assertEquals(5_000L, saveData?.updatedAt)
        assertTrue(controller.state.isSavingSnapshot)
        assertFalse(controller.state.canSave)

        val failedState = controller.failSave()
        assertFalse(failedState.isSavingSnapshot)
        assertTrue(failedState.canSave)
    }

    @Test
    fun `successful save clears preview and unsaved state`() {
        val controller = BodyMeasurementsController(nowProvider = { 5_000L })
        controller.updateProfile("", BodyMeasurementsData(waistCm = 80f), emptyList(), true)
        controller.onMeasurementInput(BodyMeasurementField.WAIST, "81")
        val saved = requireNotNull(controller.beginSave())

        val state = controller.completeSave(saved)

        assertEquals(saved, state.currentMeasurements)
        assertEquals(saved, state.draftMeasurements)
        assertFalse(state.hasUnsavedChanges)
        assertFalse(state.isSavingSnapshot)
        assertTrue(state.canSave)
        assertNull(state.previewItem)
    }

    @Test
    fun `screen and chart picker state are coordinated`() {
        val controller = BodyMeasurementsController(nowProvider = { 1_000L })

        assertEquals(BodyMeasurementsScreen.HISTORY, controller.showHistory().screen)
        assertEquals(BodyMeasurementsScreen.EDITOR, controller.showEditor().screen)
        assertTrue(controller.toggleChartPicker().isChartPickerExpanded)
        assertFalse(controller.toggleChartPicker().isChartPickerExpanded)
    }

    @Test
    fun `input formatting preserves valid values and clears invalid values`() {
        val controller = BodyMeasurementsController(nowProvider = { 1_000L })

        assertEquals("81.20", controller.formatInputAfterEditing("81.2"))
        assertEquals("", controller.formatInputAfterEditing("0"))
        assertEquals("", controller.formatInputAfterEditing("301"))
        assertEquals("", controller.formatInputAfterEditing("invalid"))
    }

    private fun controllerWithCurrent(current: BodyMeasurementsData): BodyMeasurementsController {
        return BodyMeasurementsController(nowProvider = { 1_000L }).also { controller ->
            controller.updateProfile(
                genderKey = "",
                currentMeasurements = current,
                historyItems = emptyList(),
                allowDraftReplacement = true
            )
        }
    }

    private fun historyItem(
        id: String,
        timestamp: Long,
        waist: Float
    ): BodyMeasurementsHistoryItem {
        return BodyMeasurementsHistoryItem(
            id = id,
            createdAt = timestamp,
            data = BodyMeasurementsData(waistCm = waist, updatedAt = timestamp)
        )
    }
}
