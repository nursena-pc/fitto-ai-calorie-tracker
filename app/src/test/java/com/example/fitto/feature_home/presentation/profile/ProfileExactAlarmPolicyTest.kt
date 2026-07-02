package com.example.fitto.feature_home.presentation.profile

import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileExactAlarmPolicyTest {

    @Test
    fun `pre Android S devices do not require exact alarm permission`() {
        assertEquals(
            ProfileExactAlarmState.NotRequired,
            ProfileExactAlarmPolicy.resolve(
                sdkInt = 30,
                canScheduleExactAlarms = false
            )
        )
    }

    @Test
    fun `Android S device with permission reports already enabled`() {
        assertEquals(
            ProfileExactAlarmState.AlreadyEnabled,
            ProfileExactAlarmPolicy.resolve(
                sdkInt = 31,
                canScheduleExactAlarms = true
            )
        )
    }

    @Test
    fun `Android S device without permission requires settings flow`() {
        assertEquals(
            ProfileExactAlarmState.PermissionRequired,
            ProfileExactAlarmPolicy.resolve(
                sdkInt = 31,
                canScheduleExactAlarms = false
            )
        )
    }
}
