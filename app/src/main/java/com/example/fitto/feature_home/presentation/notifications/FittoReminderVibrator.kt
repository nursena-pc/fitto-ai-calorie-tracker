package com.example.fitto.feature_home.presentation.notifications

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.fitto.feature_home.presentation.profile.reminders.ReminderNotifyMode
import com.example.fitto.feature_home.presentation.profile.reminders.alarm.ReminderDebugLogger

object FittoReminderVibrator {

    private val PATTERN = longArrayOf(0L, 450L, 150L, 450L, 150L, 700L)

    fun vibrateIfNeeded(
        context: Context,
        mode: ReminderNotifyMode
    ) {
        if (mode != ReminderNotifyMode.SOUND_AND_VIBRATION && mode != ReminderNotifyMode.VIBRATION_ONLY) {
            ReminderDebugLogger.d("vibration skipped: mode=$mode")
            return
        }

        val appContext = context.applicationContext
        val vibrator = getVibrator(appContext) ?: run {
            ReminderDebugLogger.d("vibration skipped: vibrator null")
            return
        }

        if (!vibrator.hasVibrator()) {
            ReminderDebugLogger.d("vibration skipped: device has no vibrator")
            return
        }

        val ringerMode = getRingerMode(appContext)

        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createWaveform(PATTERN, -1)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    vibrator.vibrate(
                        effect,
                        VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM)
                    )
                } else {
                    vibrateWithLegacyAttributes(vibrator, effect)
                }
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(PATTERN, -1)
            }

            ReminderDebugLogger.d(
                "manual vibration requested usage=alarm ringerMode=$ringerMode patternMs=${PATTERN.joinToString()}"
            )
        }.onFailure { throwable ->
            ReminderDebugLogger.w("manual vibration failed", throwable)
        }
    }

    @Suppress("DEPRECATION")
    private fun vibrateWithLegacyAttributes(
        vibrator: Vibrator,
        effect: VibrationEffect
    ) {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        vibrator.vibrate(effect, attributes)
    }

    private fun getVibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private fun getRingerMode(context: Context): Int {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        return audioManager?.ringerMode ?: -1
    }
}
