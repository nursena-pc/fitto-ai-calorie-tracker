package com.example.fitto.feature_home.presentation.profile

import com.example.fitto.feature_home.presentation.profile.health.ProfileHealthData
import com.example.fitto.feature_home.presentation.profile.health.WeightChartPoint
import com.example.fitto.feature_home.presentation.profile.health.WeightHistoryItem
import com.example.fitto.feature_home.presentation.profile.health.WeightTrend
import com.example.fitto.feature_home.presentation.social.SocialUser
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.text.DateFormat
import java.util.Date

class ProfileRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    fun loadProfile(
        onSuccess: (ProfileUiState) -> Unit,
        onError: (String) -> Unit
    ) {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            onError("Oturum bilgisi bulunamadı.")
            return
        }

        val uid = currentUser.uid

        firestore.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { document ->
                val data = document.data.orEmpty()

                val publicUserName = data.stringValue("userName")
                    .ifBlank { data.stringValue("username") }
                    .ifBlank { data.stringValue("displayName") }

                val personalName = data.stringValue("realName")
                    .ifBlank { data.stringValue("name") }

                val user = SocialUser(
                    id = uid,
                    userName = publicUserName,
                    displayName = data.stringValue("displayName")
                        .ifBlank { publicUserName }
                        .takeIf { it.isNotBlank() },
                    personalName = personalName
                        .takeIf { it.isNotBlank() },
                    email = currentUser.email.orEmpty()
                        .ifBlank { data.stringValue("email") },
                    photoUrl = data.stringValueOrNull("photoUrl"),
                    bio = data.stringValueOrNull("bio"),
                    followersCount = data.intValue("followersCount"),
                    followingCount = data.intValue("followingCount")
                )

                val healthData = data.toProfileHealthData()
                val bodyMeasurements = BodyMeasurementsData.fromMap(
                    data[BodyMeasurementsData.FIELD_NAME] as? Map<*, *>
                )

                val bodyMeasurementsHistory = (data[BodyMeasurementsData.HISTORY_FIELD_NAME] as? List<*>)
                    .orEmpty()
                    .mapNotNull { raw -> BodyMeasurementsData.historyFromRaw(raw) }
                    .sortedByDescending { item -> item.createdAt }

                loadPostCount(
                    uid = uid,
                    user = user,
                    healthData = healthData,
                    bodyMeasurements = bodyMeasurements,
                    bodyMeasurementsHistory = bodyMeasurementsHistory,
                    onSuccess = onSuccess
                )
            }
            .addOnFailureListener { exception ->
                onError(exception.localizedMessage ?: "Profil bilgileri alınamadı.")
            }
    }

    fun updateBodyMeasurements(
        data: BodyMeasurementsData,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            onError("Oturum bilgisi bulunamadı.")
            return
        }

        val userRef = firestore.collection("users").document(currentUser.uid)
        val cleanData = if (data.updatedAt > 0L) data else data.withUpdatedAtNow()
        val historyMap = cleanData.toHistoryMap(cleanData.updatedAt.toString())

        userRef.set(
            mapOf(
                BodyMeasurementsData.FIELD_NAME to cleanData.toMap(),
                BodyMeasurementsData.HISTORY_FIELD_NAME to FieldValue.arrayUnion(historyMap)
            ),
            SetOptions.merge()
        )
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onError(exception.localizedMessage ?: "Ölçüler kaydedilemedi.")
            }
    }


    fun updateWeight(
        weightKg: Double,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            onError("Oturum bilgisi bulunamadı.")
            return
        }

        val timestamp = System.currentTimeMillis()
        val roundedWeight = kotlin.math.round(weightKg * 1000.0) / 1000.0
        val historyItem = mapOf(
            "id" to timestamp.toString(),
            "createdAt" to timestamp,
            "weightKg" to roundedWeight
        )

        firestore.collection("users")
            .document(currentUser.uid)
            .set(
                mapOf(
                    "weightKg" to roundedWeight,
                    "updatedAt" to timestamp,
                    WEIGHT_HISTORY_FIELD_NAME to FieldValue.arrayUnion(historyItem)
                ),
                SetOptions.merge()
            )
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onError(exception.localizedMessage ?: "Kilo kaydedilemedi.")
            }
    }

    private fun loadPostCount(
        uid: String,
        user: SocialUser,
        healthData: ProfileHealthData,
        bodyMeasurements: BodyMeasurementsData,
        bodyMeasurementsHistory: List<BodyMeasurementsHistoryItem>,
        onSuccess: (ProfileUiState) -> Unit
    ) {
        firestore.collection("posts")
            .whereEqualTo("userId", uid)
            .get()
            .addOnSuccessListener { snapshot ->
                val visiblePostCount = snapshot.documents.count { document ->
                    document.getBoolean("isDraft") != true
                }

                onSuccess(
                    ProfileUiState(
                        isLoading = false,
                        user = user,
                        postCount = visiblePostCount,
                        healthData = healthData,
                        bodyMeasurements = bodyMeasurements,
                        bodyMeasurementsHistory = bodyMeasurementsHistory
                    )
                )
            }
            .addOnFailureListener {
                onSuccess(
                    ProfileUiState(
                        isLoading = false,
                        user = user,
                        postCount = 0,
                        healthData = healthData,
                        bodyMeasurements = bodyMeasurements,
                        bodyMeasurementsHistory = bodyMeasurementsHistory
                    )
                )
            }
    }

    private fun Map<String, Any?>.toProfileHealthData(): ProfileHealthData {
        val currentWeight = doubleValue("weightKg")
        val targetWeight = doubleValue("targetWeightKg")
        val updatedAt = longValue("updatedAt")

        val targets = this["targets"] as? Map<*, *>
        val currentTarget = targets?.get("current") as? Map<*, *>

        val bmr = currentTarget.intValue("bmr")
        val maintenanceCalories = currentTarget.intValue("maintenanceCalories")
        val appliedCalories = currentTarget.intValue("appliedCalories")
        val weeklyWeightChangeKg = currentTarget.doubleValue("weeklyWeightChangeKg")
        val estimatedWeeks = currentTarget.intValueOrNull("estimatedWeeks")
        val macroTargets = currentTarget?.get("macroTargets") as? Map<*, *>

        val fallbackHistory = if (currentWeight > 0.0) {
            val timestamp = updatedAt.takeIf { it > 0L } ?: System.currentTimeMillis()
            listOf(
                WeightHistoryRaw(
                    id = timestamp.toString(),
                    createdAt = timestamp,
                    weightKg = currentWeight
                )
            )
        } else {
            emptyList()
        }

        val parsedHistory = (this[WEIGHT_HISTORY_FIELD_NAME] as? List<*>)
            .orEmpty()
            .mapNotNull { raw -> weightHistoryFromRaw(raw) }

        val cleanHistory = (parsedHistory.ifEmpty { fallbackHistory })
            .filter { item -> item.createdAt > 0L && item.weightKg > 0.0 }
            .distinctBy { item -> item.id }
            .sortedByDescending { item -> item.createdAt }
            .take(WEIGHT_HISTORY_LIMIT)

        val history = cleanHistory.mapIndexed { index, item ->
            val olderItem = cleanHistory.getOrNull(index + 1)
            WeightHistoryItem(
                id = item.id,
                createdAt = item.createdAt,
                dateText = formatDate(item.createdAt),
                weightKg = item.weightKg,
                trend = weightTrend(current = item.weightKg, older = olderItem?.weightKg)
            )
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

        val lastDateText = if (updatedAt > 0L) {
            formatDate(updatedAt)
        } else {
            history.firstOrNull()?.dateText.orEmpty()
        }

        val firstWeight = history.minByOrNull { it.createdAt }?.weightKg ?: currentWeight
        val latestWeight = history.maxByOrNull { it.createdAt }?.weightKg ?: currentWeight

        return ProfileHealthData(
            initialWeightKg = firstWeight,
            currentWeightKg = latestWeight,
            targetWeightKg = targetWeight,
            heightCm = intValue("heightCm"),
            age = intValue("age"),
            genderKey = stringValue("gender"),
            goalTypeKey = stringValue("goalType"),
            activityLevelKey = stringValue("activityLevel"),
            bmr = bmr,
            maintenanceCalories = maintenanceCalories,
            dailyTargetCalories = appliedCalories,
            weeklyWeightChangeKg = weeklyWeightChangeKg,
            estimatedWeeks = estimatedWeeks,
            waterTargetLiter = currentTarget.doubleValue("waterTargetLiter"),
            proteinGoalGrams = macroTargets.intValue("proteinGrams"),
            carbGoalGrams = macroTargets.intValue("carbGrams"),
            fatGoalGrams = macroTargets.intValue("fatGrams"),
            updatedAtMillis = updatedAt,
            lastWeighDateText = lastDateText,
            chartPoints = chartPoints,
            history = history
        )
    }

    private fun weightHistoryFromRaw(raw: Any?): WeightHistoryRaw? {
        val map = raw as? Map<*, *> ?: return null
        val createdAt = map.rawLongValue("createdAt")
        val weight = map.doubleValue("weightKg")
        if (createdAt <= 0L || weight <= 0.0) return null

        return WeightHistoryRaw(
            id = map.rawStringValue("id").ifBlank { createdAt.toString() },
            createdAt = createdAt,
            weightKg = weight
        )
    }

    private fun weightTrend(current: Double, older: Double?): WeightTrend {
        if (older == null) return WeightTrend.SAME
        val delta = current - older
        return when {
            delta > 0.001 -> WeightTrend.UP
            delta < -0.001 -> WeightTrend.DOWN
            else -> WeightTrend.SAME
        }
    }

    private fun formatDate(timestamp: Long): String {
        return DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(timestamp))
    }

    private fun Map<String, Any?>.stringValue(key: String): String {
        return this[key]?.toString().orEmpty()
    }

    private fun Map<String, Any?>.stringValueOrNull(key: String): String? {
        return this[key]?.toString()?.takeIf { it.isNotBlank() }
    }

    private fun Map<String, Any?>.longValue(key: String): Long {
        return (this[key] as? Number)?.toLong() ?: 0L
    }

    private fun Map<*, *>?.intValue(key: String): Int {
        return (this?.get(key) as? Number)?.toInt() ?: 0
    }

    private fun Map<*, *>?.intValueOrNull(key: String): Int? {
        return (this?.get(key) as? Number)?.toInt()
    }

    private fun Map<*, *>?.doubleValue(key: String): Double {
        return (this?.get(key) as? Number)?.toDouble() ?: 0.0
    }

    private fun Map<*, *>?.rawLongValue(key: String): Long {
        return (this?.get(key) as? Number)?.toLong() ?: 0L
    }

    private fun Map<*, *>?.rawStringValue(key: String): String {
        return this?.get(key)?.toString().orEmpty()
    }

    private data class WeightHistoryRaw(
        val id: String,
        val createdAt: Long,
        val weightKg: Double
    )

    companion object {
        private const val WEIGHT_HISTORY_FIELD_NAME = "weightHistory"
        private const val WEIGHT_HISTORY_LIMIT = 30
    }
}
