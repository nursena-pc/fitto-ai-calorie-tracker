package com.example.fitto.feature_home.presentation.profile

import com.example.fitto.feature_home.presentation.social.SocialUser
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ProfileDataManagementCoordinator(
    private val scope: CoroutineScope,
    private val historyClearAction: ProfileHistoryClearAction,
    private val dataExportAction: ProfileDataExportAction,
    private val blockedUsersAction: ProfileBlockedUsersAction,
    private val onHistoryCleared: (ProfileHistoryClearResult) -> Unit,
    private val onHistoryClearFailed: (Throwable) -> Unit,
    private val onExportReady: (File) -> Unit,
    private val onExportFailed: (Throwable) -> Unit,
    private val onBlockedUsersLoaded: (List<SocialUser>) -> Unit,
    private val onBlockedUsersLoadFailed: (Throwable) -> Unit,
    private val onUserUnblocked: (ProfileBlockedUsersUnblockResult) -> Unit,
    private val onUserUnblockFailed: (Throwable) -> Unit
) {
    private var clearHistoryJob: Job? = null
    private var exportJob: Job? = null
    private var blockedUsersLoadJob: Job? = null
    private var unblockUserJob: Job? = null

    fun clearHistory() {
        clearHistoryJob?.cancel()
        clearHistoryJob = scope.launch {
            try {
                onHistoryCleared(historyClearAction.clear())
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                onHistoryClearFailed(error)
            }
        }
    }

    fun exportData(request: ProfileDataExportRequest) {
        exportJob?.cancel()
        exportJob = scope.launch {
            try {
                onExportReady(dataExportAction.export(request))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                onExportFailed(error)
            }
        }
    }

    fun loadBlockedUsers() {
        blockedUsersLoadJob?.cancel()
        blockedUsersLoadJob = scope.launch {
            try {
                onBlockedUsersLoaded(blockedUsersAction.load())
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                onBlockedUsersLoadFailed(error)
            }
        }
    }

    fun unblockUser(
        userId: String,
        currentUsers: List<SocialUser>
    ) {
        unblockUserJob?.cancel()
        unblockUserJob = scope.launch {
            try {
                val result = blockedUsersAction.unblock(currentUsers, userId)
                onUserUnblocked(result)
                loadBlockedUsers()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                onUserUnblockFailed(error)
            }
        }
    }

    fun clear() {
        clearHistoryJob?.cancel()
        exportJob?.cancel()
        blockedUsersLoadJob?.cancel()
        unblockUserJob?.cancel()
        clearHistoryJob = null
        exportJob = null
        blockedUsersLoadJob = null
        unblockUserJob = null
    }
}
