package com.example.fitto.feature_home.presentation.notifications

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationManagerCompat
import com.example.fitto.R
import com.example.fitto.feature_home.presentation.social.FittoNotification
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONArray
import org.json.JSONObject
import java.text.DateFormat
import java.util.Date
import java.util.UUID
import kotlin.math.max
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object FittoNotificationStore {

    private const val PREF_NAME = "fitto_notification_history"
    private const val KEY_NOTIFICATIONS = "notifications"
    private const val KEY_ACTIVE_SYSTEM_NOTIFICATION_IDS = "active_system_notification_ids"
    private const val MAX_HISTORY_SIZE = 50
    private const val SIGNED_OUT_SCOPE = "signed_out"

    private val _changeToken = MutableStateFlow(0L)
    val changeToken = _changeToken.asStateFlow()

    private fun notifyChanged() {
        _changeToken.value = _changeToken.value + 1L
    }

    private fun preferences(context: Context) =
        context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    private fun currentScope(): String {
        return FirebaseAuth.getInstance().currentUser?.uid
            ?.takeIf { it.isNotBlank() }
            ?: SIGNED_OUT_SCOPE
    }

    private fun scopedKey(baseKey: String, scope: String = currentScope()): String {
        return "$baseKey:$scope"
    }

    fun addReminderNotification(
        context: Context,
        title: String,
        scheduledTimeText: String,
        @DrawableRes iconRes: Int,
        systemNotificationId: Int,
        stableId: String? = null
    ) {
        rememberActiveSystemNotification(context, systemNotificationId)
        val newItem = JSONObject()
            .put("id", stableId?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString())
            .put("title", title)
            .put(
                "detail",
                context.getString(R.string.notification_reminder_history_detail, scheduledTimeText)
            )
            .put("iconRes", iconRes)
            .put("type", FittoNotification.TYPE_REMINDER)
            .put("targetId", JSONObject.NULL)
            .put("secondaryTargetId", JSONObject.NULL)
            .put("isRead", false)
            .put("createdAt", System.currentTimeMillis())

        prependNotification(context, newItem)
    }

    fun addSocialNotification(
        context: Context,
        notification: FittoNotification,
        systemNotificationId: Int
    ) {
        rememberActiveSystemNotification(context, systemNotificationId)
        addInAppSocialNotification(context, notification)
    }

    fun addInAppSocialNotification(
        context: Context,
        notification: FittoNotification
    ) {
        val newItem = JSONObject()
            .put("id", notification.id)
            .put("title", notification.title)
            .put("detail", notification.description)
            .put("iconRes", notification.iconRes)
            .put("type", notification.type)
            .put("targetId", notification.targetId ?: JSONObject.NULL)
            .put("secondaryTargetId", notification.secondaryTargetId ?: JSONObject.NULL)
            .put("isRead", false)
            .put("createdAt", notification.createdAt.takeIf { it > 0L } ?: System.currentTimeMillis())

        prependNotification(context, newItem)
    }

    private fun prependNotification(context: Context, newItem: JSONObject) {
        val array = readRawArray(context)
        val updated = JSONArray()
        val newId = newItem.optString("id")

        for (index in 0 until array.length()) {
            val existing = array.optJSONObject(index) ?: continue
            if (existing.optString("id") == newId) {
                newItem.put("isRead", existing.optBoolean("isRead", false))
                break
            }
        }

        updated.put(newItem)

        var addedCount = 1
        for (index in 0 until array.length()) {
            if (addedCount >= MAX_HISTORY_SIZE) break
            val existing = array.optJSONObject(index) ?: continue
            if (existing.optString("id") == newId) continue
            updated.put(existing)
            addedCount++
        }

        preferences(context)
            .edit()
            .putString(scopedKey(KEY_NOTIFICATIONS), updated.toString())
            .apply()
        notifyChanged()
    }

    fun cancelActiveSystemNotifications(context: Context) {
        cancelActiveSystemNotificationsForKeys(
            context = context,
            keys = listOf(scopedKey(KEY_ACTIVE_SYSTEM_NOTIFICATION_IDS))
        )
    }

    fun cancelAllActiveSystemNotifications(context: Context) {
        val prefs = preferences(context)
        val keys = prefs.all.keys.filter { key ->
            key == KEY_ACTIVE_SYSTEM_NOTIFICATION_IDS ||
                    key.startsWith("$KEY_ACTIVE_SYSTEM_NOTIFICATION_IDS:")
        }
        cancelActiveSystemNotificationsForKeys(context, keys)
    }

    private fun cancelActiveSystemNotificationsForKeys(
        context: Context,
        keys: List<String>
    ) {
        if (keys.isEmpty()) return

        val appContext = context.applicationContext
        val prefs = preferences(appContext)
        val notificationManager = NotificationManagerCompat.from(appContext)

        keys.forEach { key ->
            val raw = prefs.getString(key, null)
            val array = runCatching { JSONArray(raw ?: "[]") }.getOrDefault(JSONArray())

            for (index in 0 until array.length()) {
                notificationManager.cancel(array.optInt(index))
            }
        }

        val editor = prefs.edit()
        keys.forEach { key -> editor.remove(key) }
        editor.apply()
        notifyChanged()
    }

    fun rememberActiveSystemNotification(
        context: Context,
        systemNotificationId: Int
    ) {
        val appContext = context.applicationContext
        val prefs = preferences(appContext)
        val key = scopedKey(KEY_ACTIVE_SYSTEM_NOTIFICATION_IDS)
        val raw = prefs.getString(key, null)
        val existing = runCatching { JSONArray(raw ?: "[]") }.getOrDefault(JSONArray())
        val ids = linkedSetOf(systemNotificationId)

        for (index in 0 until existing.length()) {
            ids.add(existing.optInt(index))
        }

        val updated = JSONArray()
        ids.take(MAX_HISTORY_SIZE).forEach { updated.put(it) }

        prefs.edit()
            .putString(key, updated.toString())
            .apply()
    }

    fun getUnreadCount(context: Context): Int {
        val array = readRawArray(context)
        var count = 0

        for (index in 0 until array.length()) {
            val json = array.optJSONObject(index) ?: continue
            if (!json.optBoolean("isRead", true)) count++
        }

        return count
    }

    fun markLocalNotificationRead(context: Context, notificationId: String) {
        if (notificationId.isBlank()) return
        val array = readRawArray(context)
        var changed = false

        for (index in 0 until array.length()) {
            val json = array.optJSONObject(index) ?: continue
            if (json.optString("id") == notificationId) {
                json.put("isRead", true)
                changed = true
            }
        }

        if (changed) {
            preferences(context)
                .edit()
                .putString(scopedKey(KEY_NOTIFICATIONS), array.toString())
                .apply()
            notifyChanged()
        }
    }

    fun markAllLocalNotificationsRead(context: Context) {
        val array = readRawArray(context)
        var changed = false

        for (index in 0 until array.length()) {
            val json = array.optJSONObject(index) ?: continue
            if (!json.optBoolean("isRead", true)) {
                json.put("isRead", true)
                changed = true
            }
        }

        if (changed) {
            preferences(context)
                .edit()
                .putString(scopedKey(KEY_NOTIFICATIONS), array.toString())
                .apply()
            notifyChanged()
        }
    }


    fun removeMessageNotificationsForThread(context: Context, threadId: String) {
        if (threadId.isBlank()) return
        removeNotifications(context) { json ->
            val type = json.optString("type")
            val targetId = json.optStringOrNull("targetId")
            targetId == threadId && (
                    type == FittoNotification.TYPE_MESSAGE ||
                            type == FittoNotification.TYPE_GROUP_MESSAGE ||
                            type == FittoNotification.TYPE_GROUP_ADDED
                    )
        }
    }

    fun markMessageNotificationsReadForThread(context: Context, threadId: String) {
        if (threadId.isBlank()) return
        mutateNotifications(context) { json ->
            val type = json.optString("type")
            val targetId = json.optStringOrNull("targetId")
            if (targetId == threadId && (
                    type == FittoNotification.TYPE_MESSAGE ||
                            type == FittoNotification.TYPE_GROUP_MESSAGE ||
                            type == FittoNotification.TYPE_GROUP_ADDED
                    ) && !json.optBoolean("isRead", true)
            ) {
                json.put("isRead", true)
                true
            } else {
                false
            }
        }
    }

    private fun mutateNotifications(
        context: Context,
        mutator: (JSONObject) -> Boolean
    ) {
        val array = readRawArray(context)
        var changed = false

        for (index in 0 until array.length()) {
            val json = array.optJSONObject(index) ?: continue
            if (mutator(json)) changed = true
        }

        if (changed) {
            preferences(context)
                .edit()
                .putString(scopedKey(KEY_NOTIFICATIONS), array.toString())
                .apply()
            notifyChanged()
        }
    }

    private fun removeNotifications(
        context: Context,
        shouldRemove: (JSONObject) -> Boolean
    ) {
        val array = readRawArray(context)
        val updated = JSONArray()
        var changed = false

        for (index in 0 until array.length()) {
            val json = array.optJSONObject(index) ?: continue
            if (shouldRemove(json)) {
                changed = true
            } else {
                updated.put(json)
            }
        }

        if (changed) {
            preferences(context)
                .edit()
                .putString(scopedKey(KEY_NOTIFICATIONS), updated.toString())
                .apply()
            notifyChanged()
        }
    }

    fun clearCurrentAccountNotifications(context: Context) {
        preferences(context)
            .edit()
            .remove(scopedKey(KEY_NOTIFICATIONS))
            .remove(scopedKey(KEY_ACTIVE_SYSTEM_NOTIFICATION_IDS))
            .apply()
        notifyChanged()
    }

    fun clearLegacyUnscopedNotifications(context: Context) {
        preferences(context)
            .edit()
            .remove(KEY_NOTIFICATIONS)
            .remove(KEY_ACTIVE_SYSTEM_NOTIFICATION_IDS)
            .apply()
        notifyChanged()
    }

    fun handleAccountChanged(context: Context) {
        cancelAllActiveSystemNotifications(context)
        clearLegacyUnscopedNotifications(context)
    }

    fun getNotifications(context: Context): List<FittoNotification> {
        val array = readRawArray(context)
        val result = mutableListOf<FittoNotification>()

        for (index in 0 until array.length()) {
            val json = array.optJSONObject(index) ?: continue
            result.add(
                FittoNotification(
                    id = json.optString("id"),
                    title = json.optString("title"),
                    description = formatDescription(
                        context = context,
                        createdAtMillis = json.optLong("createdAt", 0L),
                        detail = json.optString("detail")
                    ),
                    iconRes = json.optInt("iconRes", R.drawable.ic_notifications),
                    type = json.optString("type", FittoNotification.TYPE_GENERAL),
                    targetId = json.optStringOrNull("targetId"),
                    secondaryTargetId = json.optStringOrNull("secondaryTargetId"),
                    isRead = json.optBoolean("isRead", true),
                    createdAt = json.optLong("createdAt", 0L)
                )
            )
        }

        return result
    }

    private fun readRawArray(context: Context): JSONArray {
        val raw = preferences(context).getString(scopedKey(KEY_NOTIFICATIONS), null)
        return runCatching { JSONArray(raw ?: "[]") }.getOrDefault(JSONArray())
    }

    private fun JSONObject.optStringOrNull(key: String): String? {
        if (!has(key) || isNull(key)) return null
        return optString(key).takeIf { it.isNotBlank() }
    }

    private fun formatDescription(
        context: Context,
        createdAtMillis: Long,
        detail: String
    ): String {
        if (createdAtMillis <= 0L) return detail

        val elapsedMillis = max(0L, System.currentTimeMillis() - createdAtMillis)
        val elapsedMinutes = elapsedMillis / 60_000L
        val elapsedHours = elapsedMillis / 3_600_000L

        val timeText = when {
            elapsedMinutes < 1L -> context.getString(R.string.notifications_time_just_now)
            elapsedMinutes < 60L -> context.getString(
                R.string.notifications_time_minutes_ago,
                elapsedMinutes
            )
            elapsedHours < 24L -> context.getString(
                R.string.notifications_time_hours_ago,
                elapsedHours
            )
            else -> DateFormat.getDateTimeInstance(
                DateFormat.SHORT,
                DateFormat.SHORT
            ).format(Date(createdAtMillis))
        }

        return if (detail.isBlank()) timeText else "$detail · $timeText"
    }
}
