package com.example.fitto

import android.app.Application
import com.example.fitto.data.seed.SeedDataImporter
import com.example.fitto.feature_home.presentation.notifications.FittoNotificationChannels
import com.example.fitto.feature_home.presentation.social.SocialNotificationObserver

class FittoApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        SeedDataImporter.importIfNeeded(this)
        FittoNotificationChannels.ensureCreated(this)
        SocialNotificationObserver.start(this)
    }
}
