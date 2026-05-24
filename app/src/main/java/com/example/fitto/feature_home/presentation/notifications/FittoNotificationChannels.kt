package com.example.fitto.feature_home.presentation.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import com.example.fitto.R
import com.example.fitto.feature_home.presentation.profile.ProfileNotificationSound
import com.example.fitto.feature_home.presentation.profile.ProfilePreferenceStore
import com.example.fitto.feature_home.presentation.profile.reminders.ProfileReminderItem
import com.example.fitto.feature_home.presentation.profile.reminders.ReminderNotifyMode
import java.util.Locale
import kotlin.math.absoluteValue

object FittoNotificationChannels {

    const val CHANNEL_REMINDERS_SOUND_VIBRATION = "fitto_reminders_sound_vibration_v8"
    const val CHANNEL_REMINDERS_SOUND = "fitto_reminders_sound_v8"
    const val CHANNEL_REMINDERS_VIBRATION = "fitto_reminders_vibration_v8"
    const val CHANNEL_REMINDERS_SILENT = "fitto_reminders_silent_v8"
    const val CHANNEL_SOCIAL = "fitto_social_v7"

    private const val CHANNEL_SOCIAL_DYNAMIC_PREFIX = "fitto_social_dynamic_v12_"
    private const val CHANNEL_CUSTOM_SOUND_PREFIX = "fitto_reminders_custom_sound_v7_"
    private const val CHANNEL_CUSTOM_SOUND_VIBRATION_PREFIX = "fitto_reminders_custom_sound_vibration_v7_"

    fun ensureCreated(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val notificationManager = context.getSystemService(NotificationManager::class.java)
            ?: return

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val audioAttributes = notificationAudioAttributes()

        val soundVibrationChannel = NotificationChannel(
            CHANNEL_REMINDERS_SOUND_VIBRATION,
            context.getString(R.string.notification_channel_reminders_sound_vibration_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notification_channel_reminders_description)
            setSound(null, null)
            enableVibration(true)
            vibrationPattern = reminderVibrationPattern()
            setShowBadge(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        val soundChannel = NotificationChannel(
            CHANNEL_REMINDERS_SOUND,
            context.getString(R.string.notification_channel_reminders_sound_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notification_channel_reminders_description)
            setSound(null, null)
            enableVibration(false)
            setShowBadge(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        val vibrationChannel = NotificationChannel(
            CHANNEL_REMINDERS_VIBRATION,
            context.getString(R.string.notification_channel_reminders_vibration_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notification_channel_reminders_description)
            setSound(null, null)
            enableVibration(true)
            vibrationPattern = reminderVibrationPattern()
            setShowBadge(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        val silentChannel = NotificationChannel(
            CHANNEL_REMINDERS_SILENT,
            context.getString(R.string.notification_channel_reminders_silent_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.notification_channel_reminders_description)
            setSound(null, null)
            enableVibration(false)
            setShowBadge(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        val socialChannel = NotificationChannel(
            CHANNEL_SOCIAL,
            context.getString(R.string.notification_channel_social_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_social_description)
            setSound(defaultSoundUri, audioAttributes)
            enableVibration(true)
            vibrationPattern = longArrayOf(0L, 220L, 120L, 260L)
            setShowBadge(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        notificationManager.createNotificationChannels(
            listOf(
                soundVibrationChannel,
                soundChannel,
                vibrationChannel,
                silentChannel,
                socialChannel
            )
        )
    }

    fun ensureChannelFor(
        context: Context,
        mode: ReminderNotifyMode,
        item: ProfileReminderItem
    ): String {
        ensureCreated(context)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return channelIdFor(mode)
        }

        val customSoundUri = item.customSoundUri
            ?.takeIf { it.isNotBlank() }
            ?.let { Uri.parse(it) }

        if (customSoundUri == null || !mode.usesSound()) {
            return channelIdFor(mode)
        }

        val channelId = customSoundChannelId(
            mode = mode,
            customSoundUri = customSoundUri
        )

        val notificationManager = context.getSystemService(NotificationManager::class.java)
            ?: return channelId

        if (notificationManager.getNotificationChannel(channelId) != null) {
            return channelId
        }

        val channelName = if (mode == ReminderNotifyMode.SOUND_AND_VIBRATION) {
            context.getString(
                R.string.notification_channel_reminders_custom_sound_vibration_name,
                item.soundName.ifBlank { context.getString(R.string.reminder_sound_custom) }
            )
        } else {
            context.getString(
                R.string.notification_channel_reminders_custom_sound_name,
                item.soundName.ifBlank { context.getString(R.string.reminder_sound_custom) }
            )
        }

        val channel = NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notification_channel_reminders_description)
            setSound(null, null)
            enableVibration(mode == ReminderNotifyMode.SOUND_AND_VIBRATION)
            if (mode == ReminderNotifyMode.SOUND_AND_VIBRATION) {
                vibrationPattern = reminderVibrationPattern()
            }
            setShowBadge(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        notificationManager.createNotificationChannel(channel)
        return channelId
    }

    fun socialChannelId(context: Context): String {
        ensureCreated(context)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return CHANNEL_SOCIAL
        }

        val sound = ProfilePreferenceStore.getNotificationSound(context)
        val customUri = ProfilePreferenceStore.getCustomNotificationSoundUri(context)
            ?.takeIf { sound == ProfileNotificationSound.CUSTOM }
            ?.let { Uri.parse(it) }
        val vibrationEnabled = ProfilePreferenceStore.isVibrationEnabled(context)
        val channelId = socialDynamicChannelId(sound, customUri, vibrationEnabled)

        val notificationManager = context.getSystemService(NotificationManager::class.java)
            ?: return channelId

        if (notificationManager.getNotificationChannel(channelId) != null) {
            return channelId
        }

        val audioAttributes = notificationAudioAttributes()
        val soundUri = when (sound) {
            ProfileNotificationSound.SILENT -> null
            ProfileNotificationSound.CUSTOM -> customUri ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            else -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }

        val channelName = when (sound) {
            ProfileNotificationSound.CUSTOM -> context.getString(
                R.string.notification_channel_social_custom_name,
                ProfilePreferenceStore.getCustomNotificationSoundName(context)
                    ?: context.getString(R.string.profile_notification_sound_custom)
            )
            ProfileNotificationSound.SILENT -> context.getString(R.string.notification_channel_social_silent_name)
            else -> context.getString(R.string.notification_channel_social_name)
        }

        val channel = NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_social_description)
            setSound(soundUri, if (soundUri == null) null else audioAttributes)
            enableVibration(vibrationEnabled)
            if (vibrationEnabled) {
                vibrationPattern = longArrayOf(0L, 220L, 120L, 260L)
            }
            setShowBadge(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        notificationManager.createNotificationChannel(channel)
        return channelId
    }

    fun channelIdFor(mode: ReminderNotifyMode): String {
        return when (mode) {
            ReminderNotifyMode.SOUND_AND_VIBRATION -> CHANNEL_REMINDERS_SOUND_VIBRATION
            ReminderNotifyMode.SOUND_ONLY -> CHANNEL_REMINDERS_SOUND
            ReminderNotifyMode.VIBRATION_ONLY -> CHANNEL_REMINDERS_VIBRATION
            ReminderNotifyMode.SILENT -> CHANNEL_REMINDERS_SILENT
        }
    }

    private fun socialDynamicChannelId(
        sound: ProfileNotificationSound,
        customSoundUri: Uri?,
        vibrationEnabled: Boolean
    ): String {
        val soundPart = when (sound) {
            ProfileNotificationSound.CUSTOM -> "custom_" + (customSoundUri?.toString()?.hashCode()?.absoluteValue ?: 0)
            else -> sound.value
        }
        val vibrationPart = if (vibrationEnabled) "vibration" else "no_vibration"
        return "$CHANNEL_SOCIAL_DYNAMIC_PREFIX${soundPart}_$vibrationPart"
    }

    private fun customSoundChannelId(
        mode: ReminderNotifyMode,
        customSoundUri: Uri
    ): String {
        val safeHash = customSoundUri.toString().hashCode().absoluteValue
        val prefix = if (mode == ReminderNotifyMode.SOUND_AND_VIBRATION) {
            CHANNEL_CUSTOM_SOUND_VIBRATION_PREFIX
        } else {
            CHANNEL_CUSTOM_SOUND_PREFIX
        }
        return "$prefix${mode.name.lowercase(Locale.US)}_$safeHash"
    }

    private fun ReminderNotifyMode.usesSound(): Boolean {
        return this == ReminderNotifyMode.SOUND_AND_VIBRATION ||
                this == ReminderNotifyMode.SOUND_ONLY
    }

    private fun notificationAudioAttributes(): AudioAttributes {
        return AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
    }

    private fun reminderVibrationPattern(): LongArray {
        return longArrayOf(0L, 450L, 150L, 450L, 150L, 700L)
    }
}
