package com.example.fitto.feature_home.presentation.notifications

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.fitto.MainActivity
import com.example.fitto.R
import com.example.fitto.feature_home.presentation.profile.ProfilePreferenceStore
import com.example.fitto.feature_home.presentation.profile.reminders.ProfileReminderItem
import com.example.fitto.feature_home.presentation.profile.reminders.ReminderCategory
import com.example.fitto.feature_home.presentation.profile.reminders.ReminderNotifyMode
import com.example.fitto.feature_home.presentation.social.FittoNotification
import java.util.Calendar
import java.util.Locale
import kotlin.math.absoluteValue

object FittoNotificationPublisher {

    const val EXTRA_OPENED_FROM_NOTIFICATION = "com.example.fitto.extra.OPENED_FROM_NOTIFICATION"
    const val EXTRA_OPENED_SOCIAL_NOTIFICATION = "com.example.fitto.extra.OPENED_SOCIAL_NOTIFICATION"
    const val EXTRA_SOCIAL_NOTIFICATION_TYPE = "com.example.fitto.extra.SOCIAL_NOTIFICATION_TYPE"
    const val EXTRA_SOCIAL_NOTIFICATION_TARGET_ID = "com.example.fitto.extra.SOCIAL_NOTIFICATION_TARGET_ID"
    const val EXTRA_SOCIAL_NOTIFICATION_SECONDARY_TARGET_ID = "com.example.fitto.extra.SOCIAL_NOTIFICATION_SECONDARY_TARGET_ID"
    const val EXTRA_OPENED_REMINDER_NOTIFICATION = "com.example.fitto.extra.OPENED_REMINDER_NOTIFICATION"
    const val EXTRA_REMINDER_ID = "com.example.fitto.extra.REMINDER_ID"
    const val EXTRA_REMINDER_CATEGORY = "com.example.fitto.extra.REMINDER_CATEGORY"


    @SuppressLint("MissingPermission")
    fun showSocialNotification(
        context: Context,
        notification: FittoNotification
    ) {
        if (!ProfilePreferenceStore.areNotificationsAllowed(context)) return
        if (!isNotificationTypeEnabled(context, notification)) return

        if (notification.isMessageType() && FittoInAppNotificationCenter.isAppVisible()) {
            FittoNotificationStore.addInAppSocialNotification(
                context = context,
                notification = notification
            )
            FittoInAppNotificationCenter.publish(notification)
            return
        }

        if (!FittoNotificationPermission.hasPermission(context)) {
            showInAppSocialNotification(context, notification)
            return
        }

        val notificationManager = NotificationManagerCompat.from(context)
        if (!notificationManager.areNotificationsEnabled()) {
            showInAppSocialNotification(context, notification)
            return
        }

        val channelId = FittoNotificationChannels.socialChannelId(context)
        val systemNotificationId = socialNotificationId(notification.id)
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_OPENED_FROM_NOTIFICATION, true)
            putExtra(EXTRA_OPENED_SOCIAL_NOTIFICATION, true)
            putExtra(EXTRA_SOCIAL_NOTIFICATION_TYPE, notification.type)
            putExtra(EXTRA_SOCIAL_NOTIFICATION_TARGET_ID, notification.targetId)
            putExtra(EXTRA_SOCIAL_NOTIFICATION_SECONDARY_TARGET_ID, notification.secondaryTargetId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            systemNotificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(notification.title)
            .setContentText(notification.description)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notification.description))
            .setCategory(if (notification.type == FittoNotification.TYPE_MESSAGE || notification.type == FittoNotification.TYPE_GROUP_MESSAGE) NotificationCompat.CATEGORY_MESSAGE else NotificationCompat.CATEGORY_SOCIAL)
            .setGroup(groupKeyFor(notification))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setWhen(notification.createdAt.takeIf { it > 0L } ?: System.currentTimeMillis())
            .setShowWhen(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        notificationManager.notify(systemNotificationId, builder.build())
        FittoNotificationStore.addSocialNotification(
            context = context,
            notification = notification,
            systemNotificationId = systemNotificationId
        )
    }

    fun showInAppSocialNotification(
        context: Context,
        notification: FittoNotification
    ) {
        if (!ProfilePreferenceStore.areNotificationsAllowed(context)) return
        if (!isNotificationTypeEnabled(context, notification)) return

        FittoNotificationStore.addInAppSocialNotification(
            context = context,
            notification = notification
        )
        FittoInAppNotificationCenter.publish(notification)
    }

    @SuppressLint("MissingPermission")
    fun showReminderNotification(
        context: Context,
        item: ProfileReminderItem,
        occurrenceMinute: Int
    ) {
        if (!ProfilePreferenceStore.areNotificationsAllowed(context)) return
        if (!ProfilePreferenceStore.areReminderNotificationsEnabled(context)) return
        if (!FittoNotificationPermission.hasPermission(context)) return

        val notificationManager = NotificationManagerCompat.from(context)
        if (!notificationManager.areNotificationsEnabled()) return

        val effectiveMode = effectiveNotifyMode(
            context = context,
            originalMode = item.notifyMode,
            itemVibrationEnabled = item.vibrationEnabled
        )
        val channelId = FittoNotificationChannels.ensureChannelFor(
            context = context,
            mode = effectiveMode,
            item = item
        )
        val systemNotificationId = notificationId(item.id, occurrenceMinute)
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_OPENED_FROM_NOTIFICATION, true)
            putExtra(EXTRA_OPENED_REMINDER_NOTIFICATION, true)
            putExtra(EXTRA_REMINDER_ID, item.id)
            putExtra(EXTRA_REMINDER_CATEGORY, item.category.name)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            systemNotificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = context.getString(
            R.string.notification_reminder_body,
            minuteToText(occurrenceMinute)
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(item.title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)
            .setOnlyAlertOnce(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(priorityFor(effectiveMode))

        applyLegacySoundAndVibration(
            builder = builder,
            mode = effectiveMode,
            item = item
        )

        notificationManager.notify(
            systemNotificationId,
            builder.build()
        )

        FittoReminderSoundPlayer.playSoundIfNeeded(
            context = context,
            mode = effectiveMode,
            item = item
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            FittoReminderVibrator.vibrateIfNeeded(context, effectiveMode)
        }

        FittoNotificationStore.addReminderNotification(
            context = context,
            title = item.title,
            scheduledTimeText = minuteToText(occurrenceMinute),
            iconRes = iconFor(item.category),
            systemNotificationId = systemNotificationId,
            stableId = "reminder_${item.id}_$occurrenceMinute"
        )
    }

    private fun isNotificationTypeEnabled(
        context: Context,
        notification: FittoNotification
    ): Boolean {
        return when (notification.type) {
            FittoNotification.TYPE_MESSAGE -> ProfilePreferenceStore.areDirectMessageNotificationsEnabled(context)
            FittoNotification.TYPE_GROUP_MESSAGE,
            FittoNotification.TYPE_GROUP_ADDED -> ProfilePreferenceStore.areGroupMessageNotificationsEnabled(context)
            else -> ProfilePreferenceStore.areSocialNotificationsEnabled(context)
        }
    }

    private fun effectiveNotifyMode(
        context: Context,
        originalMode: ReminderNotifyMode,
        itemVibrationEnabled: Boolean
    ): ReminderNotifyMode {
        if (isInSilentHours(context)) return ReminderNotifyMode.SILENT

        val vibrationAllowed = ProfilePreferenceStore.isVibrationEnabled(context) && itemVibrationEnabled

        return when {
            vibrationAllowed -> originalMode
            originalMode == ReminderNotifyMode.SOUND_AND_VIBRATION -> ReminderNotifyMode.SOUND_ONLY
            originalMode == ReminderNotifyMode.VIBRATION_ONLY -> ReminderNotifyMode.SILENT
            else -> originalMode
        }
    }

    private fun applyLegacySoundAndVibration(
        builder: NotificationCompat.Builder,
        mode: ReminderNotifyMode,
        item: ProfileReminderItem
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) return

        when (mode) {
            ReminderNotifyMode.SOUND_AND_VIBRATION -> {
                builder.setSound(null)
                builder.setVibrate(longArrayOf(0L, 220L, 120L, 260L))
            }

            ReminderNotifyMode.SOUND_ONLY -> {
                builder.setSound(null)
                builder.setVibrate(null)
            }

            ReminderNotifyMode.VIBRATION_ONLY -> {
                builder.setSound(null)
                builder.setVibrate(longArrayOf(0L, 240L, 120L, 260L))
            }

            ReminderNotifyMode.SILENT -> {
                builder.setSilent(true)
                builder.setSound(null)
                builder.setVibrate(null)
            }
        }
    }

    private fun defaultOrCustomSound(item: ProfileReminderItem) =
        item.customSoundUri
            ?.takeIf { it.isNotBlank() }
            ?.let { android.net.Uri.parse(it) }
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

    private fun priorityFor(mode: ReminderNotifyMode): Int {
        return when (mode) {
            ReminderNotifyMode.SOUND_AND_VIBRATION,
            ReminderNotifyMode.SOUND_ONLY,
            ReminderNotifyMode.VIBRATION_ONLY -> NotificationCompat.PRIORITY_HIGH
            ReminderNotifyMode.SILENT -> NotificationCompat.PRIORITY_LOW
        }
    }

    private fun iconFor(category: ReminderCategory): Int {
        return when (category) {
            ReminderCategory.MEAL -> R.drawable.ic_chef_hat
            ReminderCategory.WATER -> R.drawable.ic_water_bottle
            ReminderCategory.EXERCISE -> R.drawable.ic_fitness
            ReminderCategory.WEIGHT -> R.drawable.ic_monitor_weight
            ReminderCategory.CUSTOM,
            ReminderCategory.ALL -> R.drawable.ic_notifications
        }
    }

    private fun FittoNotification.isMessageType(): Boolean {
        return type == FittoNotification.TYPE_MESSAGE ||
                type == FittoNotification.TYPE_GROUP_MESSAGE ||
                type == FittoNotification.TYPE_GROUP_ADDED
    }

    private fun groupKeyFor(notification: FittoNotification): String {
        return when (notification.type) {
            FittoNotification.TYPE_MESSAGE,
            FittoNotification.TYPE_GROUP_MESSAGE -> "fitto_social_thread_${notification.targetId.orEmpty()}"
            else -> "fitto_social"
        }
    }

    private fun socialNotificationId(notificationId: String): Int {
        return "social_$notificationId".hashCode().absoluteValue
    }

    private fun notificationId(
        reminderId: String,
        occurrenceMinute: Int
    ): Int {
        return "${reminderId}_$occurrenceMinute".hashCode().absoluteValue
    }

    private fun isInSilentHours(context: Context): Boolean {
        val silentHours = ProfilePreferenceStore.silentHoursText(context)

        if (silentHours == context.getString(R.string.profile_silent_hours_off)) {
            return false
        }

        val parts = silentHours.split("-").map { it.trim() }
        if (parts.size != 2) return false

        val start = parseMinute(parts[0]) ?: return false
        val end = parseMinute(parts[1]) ?: return false

        val calendar = Calendar.getInstance()
        val currentMinute = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)

        return if (start <= end) {
            currentMinute in start until end
        } else {
            currentMinute >= start || currentMinute < end
        }
    }

    private fun parseMinute(value: String): Int? {
        val parts = value.split(":")
        if (parts.size != 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null
        return hour * 60 + minute
    }

    private fun minuteToText(minuteOfDay: Int): String {
        val hour = minuteOfDay / 60
        val minute = minuteOfDay % 60
        return String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
    }
}
