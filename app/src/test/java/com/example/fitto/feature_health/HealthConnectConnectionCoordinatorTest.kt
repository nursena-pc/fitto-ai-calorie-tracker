package com.example.fitto.feature_health

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HealthConnectConnectionCoordinatorTest {

    @Test
    fun loadState_withAvailableProviderAndPermissions_returnsConnected() = runBlocking {
        val coordinator = coordinator(
            isAvailable = true,
            hasAllPermissions = true
        )

        val state = coordinator.loadState()

        assertEquals(HealthConnectConnectionStatus.CONNECTED, state.status)
        assertFalse(state.isPrimaryActionEnabled)
    }

    @Test
    fun loadState_withAvailableProviderWithoutPermissions_returnsReadyToConnect() = runBlocking {
        val coordinator = coordinator(
            isAvailable = true,
            hasAllPermissions = false
        )

        val state = coordinator.loadState()

        assertEquals(HealthConnectConnectionStatus.READY_TO_CONNECT, state.status)
        assertTrue(state.isPrimaryActionEnabled)
    }

    @Test
    fun loadState_withProviderUpdateRequirement_returnsInstallOrUpdateRequired() = runBlocking {
        val coordinator = coordinator(
            needsInstallOrUpdate = true
        )

        val state = coordinator.loadState()

        assertEquals(
            HealthConnectConnectionStatus.INSTALL_OR_UPDATE_REQUIRED,
            state.status
        )
        assertTrue(state.isPrimaryActionEnabled)
    }

    @Test
    fun loadState_withoutProvider_returnsUnavailableWithoutPermissionLookup() = runBlocking {
        val source = FakeHealthConnectConnectionSource()
        val coordinator = HealthConnectConnectionCoordinator(source)

        val state = coordinator.loadState()

        assertEquals(HealthConnectConnectionStatus.UNAVAILABLE, state.status)
        assertFalse(state.isPrimaryActionEnabled)
        assertEquals(0, source.permissionLookupCount)
    }

    @Test
    fun resolvePrimaryAction_mapsConnectionStateToExpectedAction() {
        val coordinator = coordinator()

        assertEquals(
            HealthConnectPrimaryAction.REQUEST_PERMISSIONS,
            coordinator.resolvePrimaryAction(
                HealthConnectConnectionState(
                    HealthConnectConnectionStatus.READY_TO_CONNECT
                )
            )
        )
        assertEquals(
            HealthConnectPrimaryAction.OPEN_INSTALL_OR_UPDATE,
            coordinator.resolvePrimaryAction(
                HealthConnectConnectionState(
                    HealthConnectConnectionStatus.INSTALL_OR_UPDATE_REQUIRED
                )
            )
        )
        assertEquals(
            HealthConnectPrimaryAction.NONE,
            coordinator.resolvePrimaryAction(
                HealthConnectConnectionState(HealthConnectConnectionStatus.CONNECTED)
            )
        )
        assertEquals(
            HealthConnectPrimaryAction.NONE,
            coordinator.resolvePrimaryAction(
                HealthConnectConnectionState(HealthConnectConnectionStatus.UNAVAILABLE)
            )
        )
    }

    @Test
    fun resolvePermissionOutcome_requiresEveryRequestedPermission() {
        val coordinator = coordinator(permissions = setOf("steps", "calories"))

        assertEquals(
            HealthConnectPermissionOutcome.CONNECTED,
            coordinator.resolvePermissionOutcome(setOf("steps", "calories", "extra"))
        )
        assertEquals(
            HealthConnectPermissionOutcome.DENIED,
            coordinator.resolvePermissionOutcome(setOf("steps"))
        )
    }

    private fun coordinator(
        permissions: Set<String> = setOf("steps", "calories"),
        isAvailable: Boolean = false,
        needsInstallOrUpdate: Boolean = false,
        hasAllPermissions: Boolean = false
    ): HealthConnectConnectionCoordinator {
        return HealthConnectConnectionCoordinator(
            FakeHealthConnectConnectionSource(
                permissions = permissions,
                isAvailable = isAvailable,
                needsInstallOrUpdate = needsInstallOrUpdate,
                hasAllPermissions = hasAllPermissions
            )
        )
    }

    private class FakeHealthConnectConnectionSource(
        override val permissions: Set<String> = setOf("steps", "calories"),
        private val isAvailable: Boolean = false,
        private val needsInstallOrUpdate: Boolean = false,
        private val hasAllPermissions: Boolean = false
    ) : HealthConnectConnectionSource {
        var permissionLookupCount: Int = 0
            private set

        override fun isAvailable(): Boolean = isAvailable

        override fun needsInstallOrUpdate(): Boolean = needsInstallOrUpdate

        override suspend fun hasAllPermissions(): Boolean {
            permissionLookupCount += 1
            return hasAllPermissions
        }
    }
}
