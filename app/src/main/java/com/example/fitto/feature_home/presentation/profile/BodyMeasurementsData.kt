package com.example.fitto.feature_home.presentation.profile

import com.example.fitto.R
import java.util.UUID

data class BodyMeasurementsData(
    val shoulderCm: Float? = null,
    val chestCm: Float? = null,
    val waistCm: Float? = null,
    val abdomenCm: Float? = null,
    val hipCm: Float? = null,
    val basinCm: Float? = null,
    val armCm: Float? = null,
    val thighCm: Float? = null,
    val innerLegCm: Float? = null,
    val calfCm: Float? = null,
    val updatedAt: Long = 0L
) {
    val filledCount: Int
        get() = BodyMeasurementField.entries.count { field -> getValue(field) != null }

    fun isEmpty(): Boolean = filledCount == 0

    fun withUpdatedAtNow(): BodyMeasurementsData {
        return copy(updatedAt = System.currentTimeMillis())
    }

    fun getValue(field: BodyMeasurementField): Float? {
        return when (field) {
            BodyMeasurementField.SHOULDER -> shoulderCm
            BodyMeasurementField.CHEST -> chestCm
            BodyMeasurementField.WAIST -> waistCm
            BodyMeasurementField.ABDOMEN -> abdomenCm
            BodyMeasurementField.HIP -> hipCm
            BodyMeasurementField.BASIN -> basinCm
            BodyMeasurementField.ARM -> armCm
            BodyMeasurementField.THIGH -> thighCm
            BodyMeasurementField.INNER_LEG -> innerLegCm
            BodyMeasurementField.CALF -> calfCm
        }
    }

    fun withValue(field: BodyMeasurementField, value: Float?): BodyMeasurementsData {
        return when (field) {
            BodyMeasurementField.SHOULDER -> copy(shoulderCm = value)
            BodyMeasurementField.CHEST -> copy(chestCm = value)
            BodyMeasurementField.WAIST -> copy(waistCm = value)
            BodyMeasurementField.ABDOMEN -> copy(abdomenCm = value)
            BodyMeasurementField.HIP -> copy(hipCm = value)
            BodyMeasurementField.BASIN -> copy(basinCm = value)
            BodyMeasurementField.ARM -> copy(armCm = value)
            BodyMeasurementField.THIGH -> copy(thighCm = value)
            BodyMeasurementField.INNER_LEG -> copy(innerLegCm = value)
            BodyMeasurementField.CALF -> copy(calfCm = value)
        }
    }

    fun toMap(): Map<String, Any?> {
        return mapOf<String, Any?>(
            KEY_SHOULDER to shoulderCm,
            KEY_CHEST to chestCm,
            KEY_WAIST to waistCm,
            KEY_ABDOMEN to abdomenCm,
            KEY_HIP to hipCm,
            KEY_BASIN to basinCm,
            KEY_ARM to armCm,
            KEY_THIGH to thighCm,
            KEY_INNER_LEG to innerLegCm,
            KEY_CALF to calfCm,
            KEY_UPDATED_AT to (updatedAt.takeIf { it > 0L } ?: System.currentTimeMillis())
        )
    }

    fun toHistoryMap(id: String = UUID.randomUUID().toString()): Map<String, Any?> {
        return mapOf<String, Any?>(
            HISTORY_ID to id,
            HISTORY_CREATED_AT to (updatedAt.takeIf { it > 0L } ?: System.currentTimeMillis()),
            HISTORY_MEASUREMENTS to toMap()
        )
    }

    companion object {
        const val FIELD_NAME = "bodyMeasurements"
        const val HISTORY_FIELD_NAME = "bodyMeasurementsHistory"

        private const val KEY_SHOULDER = "shoulderCm"
        private const val KEY_CHEST = "chestCm"
        private const val KEY_WAIST = "waistCm"
        private const val KEY_ABDOMEN = "abdomenCm"
        private const val KEY_HIP = "hipCm"
        private const val KEY_BASIN = "basinCm"
        private const val KEY_ARM = "armCm"
        private const val KEY_THIGH = "thighCm"
        private const val KEY_INNER_LEG = "innerLegCm"
        private const val KEY_CALF = "calfCm"
        private const val KEY_UPDATED_AT = "updatedAt"

        private const val HISTORY_ID = "id"
        private const val HISTORY_CREATED_AT = "createdAt"
        private const val HISTORY_MEASUREMENTS = "measurements"

        fun fromMap(rawMap: Map<*, *>?): BodyMeasurementsData {
            if (rawMap == null) return BodyMeasurementsData()

            return BodyMeasurementsData(
                shoulderCm = rawMap.floatValue(KEY_SHOULDER),
                chestCm = rawMap.floatValue(KEY_CHEST),
                waistCm = rawMap.floatValue(KEY_WAIST),
                abdomenCm = rawMap.floatValue(KEY_ABDOMEN),
                hipCm = rawMap.floatValue(KEY_HIP),
                basinCm = rawMap.floatValue(KEY_BASIN),
                armCm = rawMap.floatValue(KEY_ARM),
                thighCm = rawMap.floatValue(KEY_THIGH),
                innerLegCm = rawMap.floatValue(KEY_INNER_LEG),
                calfCm = rawMap.floatValue(KEY_CALF),
                updatedAt = rawMap.longValue(KEY_UPDATED_AT)
            )
        }

        fun historyFromRaw(raw: Any?): BodyMeasurementsHistoryItem? {
            val map = raw as? Map<*, *> ?: return null
            val measurementsMap = map[HISTORY_MEASUREMENTS] as? Map<*, *> ?: map
            val data = fromMap(measurementsMap)
            val createdAt = map.longValue(HISTORY_CREATED_AT).takeIf { it > 0L } ?: data.updatedAt
            val id = map[HISTORY_ID]?.toString().orEmpty().ifBlank { createdAt.toString() }
            return if (createdAt > 0L || !data.isEmpty()) {
                BodyMeasurementsHistoryItem(
                    id = id,
                    createdAt = createdAt,
                    data = data.copy(updatedAt = createdAt.takeIf { it > 0L } ?: data.updatedAt)
                )
            } else {
                null
            }
        }

        private fun Map<*, *>.floatValue(key: String): Float? {
            return when (val value = this[key]) {
                is Number -> value.toFloat()
                is String -> value.replace(',', '.').toFloatOrNull()
                else -> null
            }
        }

        private fun Map<*, *>.longValue(key: String): Long {
            return when (val value = this[key]) {
                is Number -> value.toLong()
                is String -> value.toLongOrNull() ?: 0L
                else -> 0L
            }
        }
    }
}

data class BodyMeasurementsHistoryItem(
    val id: String,
    val createdAt: Long,
    val data: BodyMeasurementsData
)

enum class BodyMeasurementField(
    val labelResId: Int
) {
    SHOULDER(R.string.profile_body_measurements_shoulder),
    CHEST(R.string.profile_body_measurements_chest),
    WAIST(R.string.profile_body_measurements_waist),
    ABDOMEN(R.string.profile_body_measurements_abdomen),
    HIP(R.string.profile_body_measurements_hip),
    BASIN(R.string.profile_body_measurements_basin),
    ARM(R.string.profile_body_measurements_arm),
    THIGH(R.string.profile_body_measurements_thigh),
    INNER_LEG(R.string.profile_body_measurements_inner_leg),
    CALF(R.string.profile_body_measurements_calf)
}
