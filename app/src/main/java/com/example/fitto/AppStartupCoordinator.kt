package com.example.fitto

/** Runs application startup responsibilities without coupling [FittoApplication] to implementations. */
internal class AppStartupCoordinator(
    private val importSeedData: () -> Unit,
    private val ensureNotificationChannels: () -> Unit,
    private val startSocialNotificationObserver: () -> Unit
) {
    fun start() {
        importSeedData()
        ensureNotificationChannels()
        startSocialNotificationObserver()
    }
}
