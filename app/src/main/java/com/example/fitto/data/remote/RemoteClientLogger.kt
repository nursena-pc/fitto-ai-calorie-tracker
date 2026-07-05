package com.example.fitto.data.remote

import android.util.Log

internal interface RemoteClientLogger {
    fun debug(tag: String, message: String)

    fun error(tag: String, message: String, throwable: Throwable? = null)
}

internal object NoOpRemoteClientLogger : RemoteClientLogger {
    override fun debug(tag: String, message: String) = Unit

    override fun error(tag: String, message: String, throwable: Throwable?) = Unit
}

internal object AndroidRemoteClientLogger : RemoteClientLogger {
    override fun debug(tag: String, message: String) {
        Log.d(tag, message)
    }

    override fun error(tag: String, message: String, throwable: Throwable?) {
        if (throwable == null) {
            Log.e(tag, message)
        } else {
            Log.e(tag, message, throwable)
        }
    }
}
