package com.example.fitto.feature_home.presentation.profile.reminders.alarm

import android.util.Log

object ReminderDebugLogger {

    private const val TAG = "FittoReminder"

    fun d(message: String) {
        Log.d(TAG, message)
    }

    fun w(message: String, throwable: Throwable? = null) {
        if (throwable == null) {
            Log.w(TAG, message)
        } else {
            Log.w(TAG, message, throwable)
        }
    }
}
