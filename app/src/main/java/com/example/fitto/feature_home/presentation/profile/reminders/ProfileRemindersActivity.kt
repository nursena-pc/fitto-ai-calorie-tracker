package com.example.fitto.feature_home.presentation.profile.reminders

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.fitto.R
import com.example.fitto.feature_home.presentation.notifications.FittoNotificationPermission
import com.example.fitto.feature_home.presentation.notifications.FittoNotificationPublisher
import com.example.fitto.feature_home.presentation.notifications.FittoNotificationStore
import com.example.fitto.feature_home.presentation.profile.ProfilePreferenceStore
import com.example.fitto.feature_home.presentation.profile.ProfileRemindersBinder
import com.example.fitto.feature_home.presentation.profile.ReminderPreferenceStore
import com.example.fitto.feature_home.presentation.profile.reminders.alarm.ReminderAlarmScheduler
import com.example.fitto.feature_home.presentation.profile.reminders.service.ReminderTaskRemovedService
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ProfileRemindersActivity : AppCompatActivity() {

    private var exactAlarmPromptShown = false

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(
            ProfilePreferenceStore.wrapContextWithSavedLanguage(newBase)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ProfilePreferenceStore.applySavedTheme(this)

        super.onCreate(savedInstanceState)
        ReminderTaskRemovedService.ensureStarted(this)

        setContentView(R.layout.activity_profile_reminders)

        clearSystemNotificationsOnAppOpen(intent)

        ProfileRemindersBinder.bind(
            root = findViewById(R.id.remindersScreenRoot),
            onBackClick = {
                finish()
            }
        )

        if (FittoNotificationPermission.requestIfNeeded(this)) {
            ReminderAlarmScheduler.scheduleAll(this)
            maybeShowExactAlarmPermissionDialog()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        clearSystemNotificationsOnAppOpen(intent)
    }

    private fun clearSystemNotificationsOnAppOpen(intent: Intent?) {
        FittoNotificationStore.cancelActiveSystemNotifications(this)
        if (intent?.getBooleanExtra(FittoNotificationPublisher.EXTRA_OPENED_FROM_NOTIFICATION, false) == true) {
            intent.removeExtra(FittoNotificationPublisher.EXTRA_OPENED_FROM_NOTIFICATION)
        }
    }

    override fun onResume() {
        super.onResume()
        FittoNotificationStore.cancelActiveSystemNotifications(this)
        if (hasExactAlarmPermission()) {
            ReminderAlarmScheduler.scheduleAll(this)
        } else if (FittoNotificationPermission.hasPermission(this)) {
            maybeShowExactAlarmPermissionDialog()
        }
    }

    private fun maybeShowExactAlarmPermissionDialog() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        if (exactAlarmPromptShown) return
        if (ProfilePreferenceStore.hasSeenExactAlarmPrompt(this)) return
        if (!ReminderPreferenceStore.hasSavedReminders(this)) return
        if (hasExactAlarmPermission()) return
        if (!FittoNotificationPermission.hasPermission(this)) return

        exactAlarmPromptShown = true
        findViewById<View>(R.id.remindersScreenRoot).post {
            if (isFinishing || isDestroyed || hasExactAlarmPermission()) return@post

            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.reminder_exact_alarm_permission_title)
                .setMessage(R.string.reminder_exact_alarm_permission_message)
                .setPositiveButton(R.string.reminder_exact_alarm_permission_action) { _, _ ->
                    ProfilePreferenceStore.setExactAlarmPromptSeen(this)
                    openExactAlarmPermissionSettings()
                }
                .setNegativeButton(R.string.reminder_exact_alarm_permission_later) { _, _ ->
                    ProfilePreferenceStore.setExactAlarmPromptSeen(this)
                }
                .show()
        }
    }

    private fun hasExactAlarmPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = getSystemService(AlarmManager::class.java) ?: return false
        return alarmManager.canScheduleExactAlarms()
    }

    private fun openExactAlarmPermissionSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val intent = Intent(
            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
            Uri.parse("package:$packageName")
        )
        runCatching { startActivity(intent) }
            .onFailure {
                runCatching {
                    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
                }
            }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (
            requestCode == FittoNotificationPermission.REQUEST_POST_NOTIFICATIONS &&
            FittoNotificationPermission.hasPermission(this)
        ) {
            ReminderAlarmScheduler.scheduleAll(this)
            maybeShowExactAlarmPermissionDialog()
        }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: android.content.Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (
            requestCode == ProfileRemindersBinder.REQUEST_PICK_REMINDER_SOUND &&
            resultCode == RESULT_OK
        ) {
            ProfileRemindersBinder.handlePickedSound(
                context = this,
                uri = data?.data
            )
        }
    }
}
