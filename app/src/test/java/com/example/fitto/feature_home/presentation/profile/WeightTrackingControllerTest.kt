package com.example.fitto.feature_home.presentation.profile

import com.example.fitto.feature_home.presentation.profile.health.ProfileHealthData
import com.example.fitto.feature_home.presentation.profile.health.WeightChartPoint
import com.example.fitto.feature_home.presentation.profile.health.WeightHistoryItem
import com.example.fitto.feature_home.presentation.profile.health.WeightTrend
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WeightTrackingControllerTest {

    @Test
    fun `profile update initializes draft history and chart summary`() {
        val controller = WeightTrackingController()
        val healthData = healthData(
            currentWeight = 72.5,
            history = listOf(historyItem("latest", 300L, 72.5)),
            chartWeights = listOf(75.0, 72.5)
        )

        val state = controller.updateProfile(healthData, allowDraftReplacement = true)

        assertEquals(72.5, state.draftWeightKg ?: 0.0, 0.0)
        assertEquals(healthData.history, state.historyItems)
        assertEquals(75.0, state.chartSummary?.firstWeightKg ?: 0.0, 0.0)
        assertEquals(72.5, state.chartSummary?.lastWeightKg ?: 0.0, 0.0)
        assertEquals(-2.5, state.chartSummary?.changeKg ?: 0.0, 0.0)
        assertFalse(state.hasUnsavedWeightInput)
        assertTrue(state.canSave)
    }

    @Test
    fun `weight input accepts comma decimals and marks draft as unsaved`() {
        val controller = controllerWithCurrentWeight(72.0)

        val result = controller.onWeightInput("73,125")

        assertTrue(result.accepted)
        assertEquals(73.125, result.parsedWeightKg ?: 0.0, 0.0)
        assertEquals(73.125, result.state.draftWeightKg ?: 0.0, 0.0)
        assertTrue(result.state.hasUnsavedWeightInput)
        assertTrue(result.state.canSave)
    }

    @Test
    fun `blank input clears draft and disables save without unsaved marker`() {
        val controller = controllerWithCurrentWeight(72.0)

        val result = controller.onWeightInput("")

        assertTrue(result.accepted)
        assertNull(result.parsedWeightKg)
        assertNull(result.state.draftWeightKg)
        assertFalse(result.state.hasUnsavedWeightInput)
        assertFalse(result.state.canSave)
    }

    @Test
    fun `invalid input preserves previous draft`() {
        val controller = controllerWithCurrentWeight(72.0)

        val result = controller.onWeightInput("1234.5")

        assertFalse(result.accepted)
        assertEquals(72.0, result.state.draftWeightKg ?: 0.0, 0.0)
        assertFalse(result.state.hasUnsavedWeightInput)
    }

    @Test
    fun `profile refresh preserves focused unsaved draft`() {
        val controller = controllerWithCurrentWeight(72.0)
        controller.onWeightInput("73")

        val state = controller.updateProfile(
            healthData = healthData(currentWeight = 74.0),
            allowDraftReplacement = false
        )

        assertEquals(74.0, state.healthData.currentWeightKg, 0.0)
        assertEquals(73.0, state.draftWeightKg ?: 0.0, 0.0)
        assertTrue(state.hasUnsavedWeightInput)
    }

    @Test
    fun `profile refresh replaces draft when replacement is allowed`() {
        val controller = controllerWithCurrentWeight(72.0)
        controller.onWeightInput("73")

        val state = controller.updateProfile(
            healthData = healthData(currentWeight = 74.0),
            allowDraftReplacement = true
        )

        assertEquals(74.0, state.draftWeightKg ?: 0.0, 0.0)
        assertFalse(state.hasUnsavedWeightInput)
    }

    @Test
    fun `history preview is read only and follows refreshed item`() {
        val controller = WeightTrackingController()
        controller.updateProfile(
            healthData = healthData(
                currentWeight = 72.0,
                history = listOf(historyItem("selected", 100L, 74.0))
            ),
            allowDraftReplacement = true
        )

        val selected = controller.selectHistoryItem("selected")
        val rejectedInput = controller.onWeightInput("80")
        val refreshed = controller.updateProfile(
            healthData = healthData(
                currentWeight = 72.0,
                history = listOf(historyItem("selected", 200L, 73.5))
            ),
            allowDraftReplacement = true
        )

        assertTrue(selected.isReadOnly)
        assertEquals(74.0, selected.displayedWeightKg, 0.0)
        assertFalse(rejectedInput.accepted)
        assertEquals(73.5, refreshed.displayedWeightKg, 0.0)
        assertEquals(200L, refreshed.previewItem?.createdAt)
    }

    @Test
    fun `removed selected history item clears preview`() {
        val controller = WeightTrackingController()
        controller.updateProfile(
            healthData = healthData(
                currentWeight = 72.0,
                history = listOf(historyItem("selected", 100L, 74.0))
            ),
            allowDraftReplacement = true
        )
        controller.selectHistoryItem("selected")

        val state = controller.updateProfile(
            healthData = healthData(currentWeight = 72.0),
            allowDraftReplacement = true
        )

        assertFalse(state.isReadOnly)
        assertNull(state.selectedHistoryItemId)
        assertEquals(72.0, state.displayedWeightKg, 0.0)
    }

    @Test
    fun `save lifecycle prevents duplicate save and restores availability on error`() {
        val controller = controllerWithCurrentWeight(72.0)
        controller.onWeightInput("73")

        val weightToSave = controller.beginSave()

        assertEquals(73.0, weightToSave ?: 0.0, 0.0)
        assertTrue(controller.state.isSavingWeight)
        assertFalse(controller.state.canSave)
        assertNull(controller.beginSave())

        val failedState = controller.failSave()
        assertFalse(failedState.isSavingWeight)
        assertTrue(failedState.canSave)
    }

    @Test
    fun `successful save clears preview and unsaved state`() {
        val controller = controllerWithCurrentWeight(72.0)
        controller.onWeightInput("73")
        val savedWeight = requireNotNull(controller.beginSave())

        val state = controller.completeSave(savedWeight)

        assertEquals(73.0, state.draftWeightKg ?: 0.0, 0.0)
        assertFalse(state.hasUnsavedWeightInput)
        assertFalse(state.isSavingWeight)
        assertNull(state.previewItem)
        assertTrue(state.canSave)
    }

    @Test
    fun `screen and history preview state are coordinated`() {
        val controller = WeightTrackingController()
        controller.updateProfile(
            healthData = healthData(
                currentWeight = 72.0,
                history = listOf(historyItem("selected", 100L, 74.0))
            ),
            allowDraftReplacement = true
        )

        assertEquals(WeightTrackingScreen.HISTORY, controller.showHistory().screen)
        val selected = controller.selectHistoryItem("selected")
        assertEquals(WeightTrackingScreen.CURRENT, selected.screen)
        assertTrue(selected.isReadOnly)
        assertFalse(controller.clearHistoryPreview().isReadOnly)
        assertEquals(WeightTrackingScreen.CURRENT, controller.showCurrent().screen)
    }

    @Test
    fun `history state exposes only first thirty items`() {
        val controller = WeightTrackingController()
        val history = (1..35).map { index ->
            historyItem(index.toString(), index.toLong(), 70.0 + index)
        }

        val state = controller.updateProfile(
            healthData = healthData(currentWeight = 72.0, history = history),
            allowDraftReplacement = true
        )

        assertEquals(35, state.historyItems.size)
        assertEquals(30, state.visibleHistoryItems.size)
        assertEquals("1", state.visibleHistoryItems.first().id)
        assertEquals("30", state.visibleHistoryItems.last().id)
    }

    @Test
    fun `input formatter resolves valid localized values and rejects empty values`() {
        val controller = WeightTrackingController()

        assertEquals(81.25, controller.formatInputAfterEditing("81,25") ?: 0.0, 0.0)
        assertNull(controller.formatInputAfterEditing("0"))
        assertNull(controller.formatInputAfterEditing("."))
    }

    private fun controllerWithCurrentWeight(weight: Double): WeightTrackingController {
        return WeightTrackingController().also { controller ->
            controller.updateProfile(
                healthData = healthData(currentWeight = weight),
                allowDraftReplacement = true
            )
        }
    }

    private fun healthData(
        currentWeight: Double,
        history: List<WeightHistoryItem> = emptyList(),
        chartWeights: List<Double> = emptyList()
    ): ProfileHealthData {
        return ProfileHealthData(
            initialWeightKg = 80.0,
            currentWeightKg = currentWeight,
            targetWeightKg = 65.0,
            history = history,
            chartPoints = chartWeights.mapIndexed { index, weight ->
                WeightChartPoint(
                    timestamp = index.toLong(),
                    label = index.toString(),
                    weightKg = weight
                )
            }
        )
    }

    private fun historyItem(
        id: String,
        createdAt: Long,
        weightKg: Double
    ): WeightHistoryItem {
        return WeightHistoryItem(
            id = id,
            createdAt = createdAt,
            dateText = createdAt.toString(),
            weightKg = weightKg,
            trend = WeightTrend.SAME
        )
    }
}
