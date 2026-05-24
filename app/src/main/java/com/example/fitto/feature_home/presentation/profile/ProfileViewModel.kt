package com.example.fitto.feature_home.presentation.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.fitto.feature_home.presentation.profile.health.ProfileHealthData
import com.example.fitto.feature_home.presentation.profile.health.WeightChartPoint
import com.example.fitto.feature_home.presentation.profile.health.WeightHistoryItem
import com.example.fitto.feature_home.presentation.profile.health.WeightTrend
import java.text.DateFormat
import java.util.Date

class ProfileViewModel(
    private val repository: ProfileRepository = ProfileRepository()
) : ViewModel() {

    private val _uiState = MutableLiveData(ProfileUiState())
    val uiState: LiveData<ProfileUiState> = _uiState

    private var hasLoadedProfile = false
    private var isRequestInFlight = false

    fun loadProfile(forceRefresh: Boolean = false) {
        if (isRequestInFlight) return
        if (!forceRefresh && hasLoadedProfile) return

        val currentState = _uiState.value ?: ProfileUiState()
        val hasVisibleContent = currentState.user != null ||
                currentState.healthData != ProfileHealthData.Empty ||
                currentState.postCount > 0

        isRequestInFlight = true
        _uiState.value = currentState.copy(
            isLoading = !hasVisibleContent,
            errorMessage = null
        )

        repository.loadProfile(
            onSuccess = { state ->
                isRequestInFlight = false
                hasLoadedProfile = true
                _uiState.value = state.copy(isLoading = false)
            },
            onError = { message ->
                isRequestInFlight = false
                _uiState.value = currentState.copy(
                    isLoading = false,
                    errorMessage = message
                )
            }
        )
    }

    fun refreshProfile() {
        loadProfile(forceRefresh = true)
    }

    fun updateBodyMeasurements(
        data: BodyMeasurementsData,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val currentState = _uiState.value ?: ProfileUiState()
        val savedData = data.withUpdatedAtNow()
        val historyItem = BodyMeasurementsHistoryItem(
            id = savedData.updatedAt.toString(),
            createdAt = savedData.updatedAt,
            data = savedData
        )

        _uiState.value = currentState.copy(
            bodyMeasurements = savedData,
            bodyMeasurementsHistory = (listOf(historyItem) + currentState.bodyMeasurementsHistory)
                .distinctBy { item -> item.id }
                .sortedByDescending { item -> item.createdAt }
                .take(30),
            errorMessage = null
        )

        hasLoadedProfile = true
        onSuccess()

        repository.updateBodyMeasurements(
            data = savedData,
            onSuccess = {
                hasLoadedProfile = false
                refreshProfile()
            },
            onError = { message ->
                _uiState.value = (_uiState.value ?: ProfileUiState()).copy(
                    errorMessage = message
                )
                onError(message)
            }
        )
    }

    @Suppress("UNUSED_PARAMETER")
    fun updateWeight(
        weightKg: Double,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val currentState = _uiState.value ?: ProfileUiState()
        val timestamp = System.currentTimeMillis()
        val dateText = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(timestamp))
        val roundedWeight = kotlin.math.round(weightKg * 1000.0) / 1000.0

        val newItem = WeightHistoryItem(
            id = timestamp.toString(),
            createdAt = timestamp,
            dateText = dateText,
            weightKg = roundedWeight,
            trend = WeightTrend.SAME
        )

        val mergedHistory = (listOf(newItem) + currentState.healthData.history)
            .distinctBy { item -> item.id }
            .sortedByDescending { item -> item.createdAt }
            .take(30)

        val history = mergedHistory.mapIndexed { index, item ->
            val older = mergedHistory.getOrNull(index + 1)
            item.copy(trend = resolveTrend(item.weightKg, older?.weightKg))
        }

        val chartPoints = history
            .sortedBy { item -> item.createdAt }
            .map { item ->
                WeightChartPoint(
                    timestamp = item.createdAt,
                    label = item.dateText.ifBlank { "-" },
                    weightKg = item.weightKg
                )
            }

        val initialWeight = chartPoints.firstOrNull()?.weightKg ?: roundedWeight
        val updatedHealthData = currentState.healthData.copy(
            initialWeightKg = initialWeight,
            currentWeightKg = roundedWeight,
            updatedAtMillis = timestamp,
            lastWeighDateText = dateText,
            chartPoints = chartPoints,
            history = history
        )

        _uiState.value = currentState.copy(
            healthData = updatedHealthData,
            errorMessage = null
        )

        repository.updateWeight(
            weightKg = roundedWeight,
            onSuccess = {
                hasLoadedProfile = false
                refreshProfile()
                onSuccess()
            },
            onError = { message ->
                _uiState.value = (_uiState.value ?: ProfileUiState()).copy(
                    errorMessage = message
                )
                onError(message)
            }
        )
    }

    private fun resolveTrend(current: Double, older: Double?): WeightTrend {
        if (older == null) return WeightTrend.SAME
        val delta = current - older
        return when {
            delta > 0.001 -> WeightTrend.UP
            delta < -0.001 -> WeightTrend.DOWN
            else -> WeightTrend.SAME
        }
    }
}
