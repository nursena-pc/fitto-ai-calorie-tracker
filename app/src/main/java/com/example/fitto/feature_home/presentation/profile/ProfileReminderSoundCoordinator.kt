package com.example.fitto.feature_home.presentation.profile

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import com.example.fitto.R

class ProfileReminderSoundCoordinator {

    companion object {
        const val REQUEST_PICK_REMINDER_SOUND = 7051
        private const val PREVIEW_DURATION_MILLIS = 1_200L
    }

    private var previewPlayer: MediaPlayer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var pendingSoundResult: ((name: String, uri: String) -> Unit)? = null

    fun requestSoundPicker(
        context: Context,
        onPicked: (name: String, uri: String) -> Unit
    ) {
        pendingSoundResult = onPicked

        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "audio/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }

        findActivity(context)?.startActivityForResult(
            intent,
            REQUEST_PICK_REMINDER_SOUND
        )
    }

    fun handlePickedSound(
        context: Context,
        uri: Uri?
    ) {
        if (uri == null) return

        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }

        val displayName = queryDisplayName(context, uri)
            ?: context.getString(R.string.reminder_sound_custom)

        pendingSoundResult?.invoke(displayName, uri.toString())
        pendingSoundResult = null
    }

    fun playPreview(
        context: Context,
        customUriText: String?,
        volumePercent: Int
    ) {
        stopPreview()

        val volume = volumePercent.coerceIn(0, 100) / 100f
        val uri = customUriText
            ?.takeIf { it.isNotBlank() }
            ?.let(Uri::parse)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val player = MediaPlayer()
        previewPlayer = player

        runCatching {
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            player.setDataSource(context, uri)
            player.setVolume(volume, volume)
            player.setOnPreparedListener {
                it.start()
                mainHandler.postDelayed(::stopPreview, PREVIEW_DURATION_MILLIS)
            }
            player.setOnCompletionListener {
                stopPreview()
            }
            player.prepareAsync()
        }.onFailure {
            stopPreview()
        }
    }

    fun stopPreview() {
        mainHandler.removeCallbacksAndMessages(null)
        runCatching { previewPlayer?.stop() }
        runCatching { previewPlayer?.release() }
        previewPlayer = null
    }

    fun clear() {
        stopPreview()
        pendingSoundResult = null
    }

    private fun queryDisplayName(
        context: Context,
        uri: Uri
    ): String? {
        val cursor = context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )

        cursor?.use {
            val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && it.moveToFirst()) {
                return it.getString(index)
            }
        }

        return null
    }

    private fun findActivity(context: Context): Activity? {
        var currentContext = context

        while (currentContext is ContextWrapper) {
            if (currentContext is Activity) return currentContext
            currentContext = currentContext.baseContext
        }

        return null
    }
}
