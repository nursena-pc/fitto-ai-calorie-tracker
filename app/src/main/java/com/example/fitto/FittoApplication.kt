package com.example.fitto

import android.app.Application

class FittoApplication : Application() {

    internal lateinit var appContainer: FittoAppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = FittoAppContainer(this)
        appContainer.startupCoordinator.start()
    }
}
