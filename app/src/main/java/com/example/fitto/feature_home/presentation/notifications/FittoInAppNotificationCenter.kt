package com.example.fitto.feature_home.presentation.notifications

import com.example.fitto.feature_home.presentation.social.FittoNotification
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object FittoInAppNotificationCenter {

    @Volatile
    private var appVisible: Boolean = false

    private val _events = MutableSharedFlow<FittoNotification>(
        replay = 0,
        extraBufferCapacity = 16
    )
    val events = _events.asSharedFlow()

    fun setAppVisible(visible: Boolean) {
        appVisible = visible
    }

    fun isAppVisible(): Boolean = appVisible

    fun publish(notification: FittoNotification) {
        _events.tryEmit(notification)
    }
}
