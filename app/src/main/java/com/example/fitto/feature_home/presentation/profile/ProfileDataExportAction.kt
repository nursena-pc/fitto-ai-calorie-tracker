package com.example.fitto.feature_home.presentation.profile

import android.content.Context
import com.example.fitto.feature_home.presentation.notifications.FittoNotificationStore
import com.example.fitto.feature_home.presentation.social.FittoNotification
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProfileDataExportAction(
    context: Context,
    private val snapshotFactory: ProfileDataExportSnapshotFactory = ProfileDataExportSnapshotFactory(),
    private val fileWriter: ProfileDataExportFileWriter = ProfileDataExportFileWriter(),
    private val notificationsProvider: () -> List<FittoNotification> = {
        FittoNotificationStore.getNotifications(context.applicationContext)
    },
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val exportDirectory = File(context.applicationContext.cacheDir, EXPORT_DIRECTORY)

    suspend fun export(request: ProfileDataExportRequest): File = withContext(ioDispatcher) {
        val snapshot = snapshotFactory.create(
            request = request,
            notifications = notificationsProvider(),
            exportedAt = currentTimeMillis()
        )
        fileWriter.write(exportDirectory, snapshot)
    }

    private companion object {
        const val EXPORT_DIRECTORY = "exports"
    }
}
