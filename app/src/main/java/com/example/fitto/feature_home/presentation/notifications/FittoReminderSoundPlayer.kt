package com.example.fitto.feature_home.presentation.notifications

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.example.fitto.feature_home.presentation.profile.reminders.ProfileReminderItem
import com.example.fitto.feature_home.presentation.profile.reminders.ReminderNotifyMode
import com.example.fitto.feature_home.presentation.profile.reminders.alarm.ReminderDebugLogger

object FittoReminderSoundPlayer {

    private const val MAX_PLAYBACK_MS = 60_000L

    private val handler = Handler(Looper.getMainLooper())
    private var currentPlayer: MediaPlayer? = null
    private var stopRunnable: Runnable? = null

    fun playSoundIfNeeded(
        context: Context,
        mode: ReminderNotifyMode,
        item: ProfileReminderItem
    ) {
        if (!mode.usesSound()) return

        val uri = item.customSoundUri
            ?.takeIf { it.isNotBlank() }
            ?.let { Uri.parse(it) }
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: return

        stopCurrent()

        val volume = item.soundVolumePercent.coerceIn(0, 100) / 100f
        val player = MediaPlayer()
        currentPlayer = player

        runCatching {
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )

            player.setDataSource(context.applicationContext, uri)
            player.setVolume(volume, volume)

            player.setOnPreparedListener { mediaPlayer ->
                mediaPlayer.start()
                scheduleMaxStop(mediaPlayer, uri)
                ReminderDebugLogger.d(
                    "reminder sound started uri=$uri volume=${item.soundVolumePercent} maxMs=$MAX_PLAYBACK_MS"
                )
            }

            player.setOnCompletionListener { mediaPlayer ->
                releasePlayer(mediaPlayer)
            }

            player.setOnErrorListener { mediaPlayer, _, _ ->
                releasePlayer(mediaPlayer)
                ReminderDebugLogger.w("reminder sound playback failed uri=$uri")
                true
            }

            player.prepareAsync()
        }.onFailure {
            releasePlayer(player)
            ReminderDebugLogger.w("reminder sound playback exception uri=$uri")
        }
    }

    private fun scheduleMaxStop(
        player: MediaPlayer,
        uri: Uri
    ) {
        val runnable = Runnable {
            if (currentPlayer == player) {
                runCatching {
                    if (player.isPlaying) {
                        player.stop()
                    }
                }

                releasePlayer(player)

                ReminderDebugLogger.d(
                    "reminder sound stopped at max duration uri=$uri maxMs=$MAX_PLAYBACK_MS"
                )
            }
        }

        stopRunnable = runnable
        handler.postDelayed(runnable, MAX_PLAYBACK_MS)
    }

    private fun stopCurrent() {
        stopRunnable?.let { handler.removeCallbacks(it) }
        stopRunnable = null

        currentPlayer?.let { player ->
            runCatching {
                if (player.isPlaying) {
                    player.stop()
                }
            }

            runCatching {
                player.release()
            }
        }

        currentPlayer = null
    }

    private fun releasePlayer(player: MediaPlayer) {
        if (currentPlayer == player) {
            stopRunnable?.let { handler.removeCallbacks(it) }
            stopRunnable = null
            currentPlayer = null
        }

        runCatching {
            player.release()
        }
    }

    private fun ReminderNotifyMode.usesSound(): Boolean {
        return this == ReminderNotifyMode.SOUND_AND_VIBRATION ||
                this == ReminderNotifyMode.SOUND_ONLY
    }
}