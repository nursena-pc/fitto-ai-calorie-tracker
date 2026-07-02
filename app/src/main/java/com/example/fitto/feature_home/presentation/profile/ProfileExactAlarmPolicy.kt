package com.example.fitto.feature_home.presentation.profile

sealed interface ProfileExactAlarmState {
    data object NotRequired : ProfileExactAlarmState
    data object AlreadyEnabled : ProfileExactAlarmState
    data object PermissionRequired : ProfileExactAlarmState
}

object ProfileExactAlarmPolicy {

    private const val ANDROID_S_API_LEVEL = 31

    fun resolve(
        sdkInt: Int,
        canScheduleExactAlarms: Boolean
    ): ProfileExactAlarmState {
        return when {
            sdkInt < ANDROID_S_API_LEVEL -> ProfileExactAlarmState.NotRequired
            canScheduleExactAlarms -> ProfileExactAlarmState.AlreadyEnabled
            else -> ProfileExactAlarmState.PermissionRequired
        }
    }
}
