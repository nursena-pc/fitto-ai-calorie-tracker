package com.example.fitto.feature_health

internal interface HealthConnectConnectionSource {
    val permissions: Set<String>

    fun isAvailable(): Boolean

    fun needsInstallOrUpdate(): Boolean

    suspend fun hasAllPermissions(): Boolean
}

internal enum class HealthConnectConnectionStatus {
    CONNECTED,
    READY_TO_CONNECT,
    INSTALL_OR_UPDATE_REQUIRED,
    UNAVAILABLE
}

internal data class HealthConnectConnectionState(
    val status: HealthConnectConnectionStatus
) {
    val isPrimaryActionEnabled: Boolean
        get() = status == HealthConnectConnectionStatus.READY_TO_CONNECT ||
            status == HealthConnectConnectionStatus.INSTALL_OR_UPDATE_REQUIRED
}

internal enum class HealthConnectPrimaryAction {
    REQUEST_PERMISSIONS,
    OPEN_INSTALL_OR_UPDATE,
    NONE
}

internal enum class HealthConnectPermissionOutcome {
    CONNECTED,
    DENIED
}

internal class HealthConnectConnectionCoordinator(
    private val source: HealthConnectConnectionSource
) {

    val requestedPermissions: Set<String>
        get() = source.permissions

    suspend fun loadState(): HealthConnectConnectionState {
        val isAvailable = source.isAvailable()
        val needsInstallOrUpdate = source.needsInstallOrUpdate()
        val hasAllPermissions = isAvailable && source.hasAllPermissions()

        val status = when {
            hasAllPermissions -> HealthConnectConnectionStatus.CONNECTED
            isAvailable -> HealthConnectConnectionStatus.READY_TO_CONNECT
            needsInstallOrUpdate -> HealthConnectConnectionStatus.INSTALL_OR_UPDATE_REQUIRED
            else -> HealthConnectConnectionStatus.UNAVAILABLE
        }
        return HealthConnectConnectionState(status)
    }

    fun resolvePrimaryAction(
        state: HealthConnectConnectionState
    ): HealthConnectPrimaryAction {
        return when (state.status) {
            HealthConnectConnectionStatus.READY_TO_CONNECT -> {
                HealthConnectPrimaryAction.REQUEST_PERMISSIONS
            }

            HealthConnectConnectionStatus.INSTALL_OR_UPDATE_REQUIRED -> {
                HealthConnectPrimaryAction.OPEN_INSTALL_OR_UPDATE
            }

            HealthConnectConnectionStatus.CONNECTED,
            HealthConnectConnectionStatus.UNAVAILABLE -> HealthConnectPrimaryAction.NONE
        }
    }

    fun resolvePermissionOutcome(
        grantedPermissions: Set<String>
    ): HealthConnectPermissionOutcome {
        return if (grantedPermissions.containsAll(requestedPermissions)) {
            HealthConnectPermissionOutcome.CONNECTED
        } else {
            HealthConnectPermissionOutcome.DENIED
        }
    }
}
