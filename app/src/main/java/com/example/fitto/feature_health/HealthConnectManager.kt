package com.example.fitto.feature_health

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

internal class HealthConnectManager(
    private val context: Context
) : HealthConnectConnectionSource {

    override val permissions: Set<String> = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class)
    )

    fun availabilityStatus(): Int {
        return HealthConnectClient.getSdkStatus(
            context,
            PROVIDER_PACKAGE_NAME
        )
    }

    override fun isAvailable(): Boolean {
        return availabilityStatus() == HealthConnectClient.SDK_AVAILABLE
    }

    override fun needsInstallOrUpdate(): Boolean {
        return availabilityStatus() == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED
    }

    fun permissionContract() =
        PermissionController.createRequestPermissionResultContract()

    private fun clientOrNull(): HealthConnectClient? {
        return if (isAvailable()) {
            HealthConnectClient.getOrCreate(context)
        } else {
            null
        }
    }

    override suspend fun hasAllPermissions(): Boolean {
        val client = clientOrNull() ?: return false
        val granted = client.permissionController.getGrantedPermissions()
        return granted.containsAll(permissions)
    }

    suspend fun readDailySummary(
        date: LocalDate
    ): HealthConnectDailySummary {
        val client = clientOrNull()
            ?: return HealthConnectDailySummary(isAvailable = false)

        if (!hasAllPermissions()) {
            return HealthConnectDailySummary(
                hasPermission = false,
                isAvailable = true
            )
        }

        val zoneId = ZoneId.systemDefault()
        val start = date.atStartOfDay(zoneId).toInstant()
        val end = date.plusDays(1).atStartOfDay(zoneId).toInstant()

        return runCatching {
            val response = client.aggregate(
                AggregateRequest(
                    metrics = setOf(
                        StepsRecord.COUNT_TOTAL,
                        ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL
                    ),
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                )
            )

            val steps = response[StepsRecord.COUNT_TOTAL] ?: 0L
            val activeCalories = response[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]
                ?.inKilocalories
                ?.roundToInt()
                ?: 0

            HealthConnectDailySummary(
                steps = steps,
                activeCaloriesKcal = activeCalories,
                hasPermission = true,
                isAvailable = true
            )
        }.getOrElse {
            HealthConnectDailySummary(
                hasPermission = true,
                isAvailable = true
            )
        }
    }

    fun openInstallOrUpdateScreen() {
        val uriString = "market://details?id=$PROVIDER_PACKAGE_NAME&url=healthconnect%3A%2F%2Fonboarding"

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setPackage("com.android.vending")
            data = Uri.parse(uriString)
            putExtra("overlay", true)
            putExtra("callerId", context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        runCatching {
            context.startActivity(intent)
        }.onFailure {
            val fallbackIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=$PROVIDER_PACKAGE_NAME")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallbackIntent)
        }
    }

    companion object {
        const val PROVIDER_PACKAGE_NAME = "com.google.android.apps.healthdata"
    }
}