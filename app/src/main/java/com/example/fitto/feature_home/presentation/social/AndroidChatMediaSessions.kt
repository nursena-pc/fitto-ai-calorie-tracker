package com.example.fitto.feature_home.presentation.social

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import java.io.File

internal class AndroidChatVoiceRecorderFactory(
    private val context: Context,
    private val maximumDurationMillis: Long
) : ChatVoiceRecorderFactory {

    override fun create(): ChatVoiceRecorderSession {
        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        return AndroidChatVoiceRecorderSession(
            recorder = recorder,
            maximumDurationMillis = maximumDurationMillis
        )
    }
}

private class AndroidChatVoiceRecorderSession(
    private val recorder: MediaRecorder,
    private val maximumDurationMillis: Long
) : ChatVoiceRecorderSession {

    override fun prepareAndStart(outputFile: File) {
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        recorder.setAudioEncodingBitRate(128_000)
        recorder.setAudioSamplingRate(44_100)
        recorder.setMaxDuration(maximumDurationMillis.toInt())
        recorder.setOutputFile(outputFile.absolutePath)
        recorder.prepare()
        recorder.start()
    }

    override fun stop() {
        recorder.stop()
    }

    override fun reset() {
        recorder.reset()
    }

    override fun release() {
        recorder.release()
    }
}

internal class AndroidChatAudioPlayerFactory : ChatAudioPlayerFactory {
    override fun create(): ChatAudioPlayerSession {
        return AndroidChatAudioPlayerSession(MediaPlayer())
    }
}

private class AndroidChatAudioPlayerSession(
    private val player: MediaPlayer
) : ChatAudioPlayerSession {

    override val isPlaying: Boolean
        get() = player.isPlaying

    override fun prepare(
        url: String,
        onPrepared: () -> Unit,
        onCompletion: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        player.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
        )
        player.setDataSource(url)
        player.setOnPreparedListener {
            onPrepared()
        }
        player.setOnCompletionListener {
            onCompletion()
        }
        player.setOnErrorListener { _, what, extra ->
            onError(
                IllegalStateException(
                    "MediaPlayer error what=$what extra=$extra url=$url"
                )
            )
            true
        }
        player.prepareAsync()
    }

    override fun start() {
        player.start()
    }

    override fun clearListeners() {
        player.setOnPreparedListener(null)
        player.setOnCompletionListener(null)
        player.setOnErrorListener(null)
    }

    override fun stop() {
        player.stop()
    }

    override fun release() {
        player.release()
    }
}
