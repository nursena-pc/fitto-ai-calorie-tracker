package com.example.fitto.feature_home.presentation.social

import java.io.File

internal sealed interface ChatVoiceRecordingActionResult {
    data object Started : ChatVoiceRecordingActionResult

    data class ReadyToSend(
        val audioFile: File,
        val durationMillis: Long
    ) : ChatVoiceRecordingActionResult

    data object TooShort : ChatVoiceRecordingActionResult

    data class Failed(
        val cause: Throwable? = null
    ) : ChatVoiceRecordingActionResult
}

internal interface ChatVoiceRecorderSession {
    fun prepareAndStart(outputFile: File)

    fun stop()

    fun reset()

    fun release()
}

internal fun interface ChatVoiceRecorderFactory {
    fun create(): ChatVoiceRecorderSession
}

internal interface ChatAudioPlayerSession {
    val isPlaying: Boolean

    fun prepare(
        url: String,
        onPrepared: () -> Unit,
        onCompletion: () -> Unit,
        onError: (Throwable) -> Unit
    )

    fun start()

    fun clearListeners()

    fun stop()

    fun release()
}

internal fun interface ChatAudioPlayerFactory {
    fun create(): ChatAudioPlayerSession
}

internal class ChatMediaController(
    private val voiceFileFactory: () -> File,
    private val voiceRecorderFactory: ChatVoiceRecorderFactory,
    private val audioPlayerFactory: ChatAudioPlayerFactory,
    private val elapsedRealtime: () -> Long,
    private val minimumVoiceDurationMillis: Long,
    private val onVoiceRecordingStateChanged: (Boolean) -> Unit,
    private val onAudioPlaybackStateChanged: (String?) -> Unit,
    private val onAudioPlaybackError: (Throwable) -> Unit
) {

    private var voiceRecorder: ChatVoiceRecorderSession? = null
    private var currentVoiceFile: File? = null
    private var voiceRecordStartedAt: Long = 0L
    private var isRecordingVoice = false

    private var audioPlayer: ChatAudioPlayerSession? = null
    private var playingAudioUrl: String? = null

    val isVoiceRecording: Boolean
        get() = isRecordingVoice

    fun toggleVoiceRecording(): ChatVoiceRecordingActionResult {
        return if (isRecordingVoice) {
            stopVoiceRecording()
        } else {
            startVoiceRecording()
        }
    }

    fun playOrStopAudio(url: String) {
        if (url.isBlank()) return

        val currentPlayer = audioPlayer
        val isCurrentUrlPlaying = playingAudioUrl == url && runCatching {
            currentPlayer?.isPlaying == true
        }.getOrDefault(false)

        if (isCurrentUrlPlaying) {
            stopAudioPlayback()
            return
        }

        stopAudioPlayback()
        onAudioPlaybackStateChanged(url)

        val newPlayer = runCatching {
            audioPlayerFactory.create()
        }.getOrElse { error ->
            onAudioPlaybackStateChanged(null)
            onAudioPlaybackError(error)
            return
        }

        audioPlayer = newPlayer

        runCatching {
            newPlayer.prepare(
                url = url,
                onPrepared = {
                    if (audioPlayer !== newPlayer) return@prepare

                    playingAudioUrl = url
                    onAudioPlaybackStateChanged(url)

                    runCatching {
                        newPlayer.start()
                    }.onFailure { error ->
                        handleAudioPlaybackFailure(newPlayer, error)
                    }
                },
                onCompletion = {
                    if (audioPlayer === newPlayer) {
                        stopAudioPlayback()
                    }
                },
                onError = { error ->
                    handleAudioPlaybackFailure(newPlayer, error)
                }
            )
        }.onFailure { error ->
            handleAudioPlaybackFailure(newPlayer, error)
        }
    }

    fun release() {
        releaseVoiceRecorder(deleteCurrentFile = false, notifyState = false)
        stopAudioPlayback()
    }

    private fun startVoiceRecording(): ChatVoiceRecordingActionResult {
        val outputFile = runCatching {
            voiceFileFactory()
        }.getOrElse { error ->
            return ChatVoiceRecordingActionResult.Failed(error)
        }

        val recorder = runCatching {
            voiceRecorderFactory.create()
        }.getOrElse { error ->
            outputFile.delete()
            return ChatVoiceRecordingActionResult.Failed(error)
        }

        currentVoiceFile = outputFile
        voiceRecordStartedAt = elapsedRealtime()
        voiceRecorder = recorder

        return runCatching {
            recorder.prepareAndStart(outputFile)
        }.fold(
            onSuccess = {
                isRecordingVoice = true
                onVoiceRecordingStateChanged(true)
                ChatVoiceRecordingActionResult.Started
            },
            onFailure = { error ->
                releaseVoiceRecorder(deleteCurrentFile = true, notifyState = false)
                ChatVoiceRecordingActionResult.Failed(error)
            }
        )
    }

    private fun stopVoiceRecording(): ChatVoiceRecordingActionResult {
        val recordedFile = currentVoiceFile
        val durationMillis = (elapsedRealtime() - voiceRecordStartedAt).coerceAtLeast(0L)
        val stopResult = runCatching {
            voiceRecorder?.stop()
        }

        releaseVoiceRecorder(
            deleteCurrentFile = stopResult.isFailure,
            notifyState = true
        )

        stopResult.exceptionOrNull()?.let { error ->
            return ChatVoiceRecordingActionResult.Failed(error)
        }

        if (recordedFile == null || !recordedFile.exists() || recordedFile.length() <= 0L) {
            recordedFile?.delete()
            return ChatVoiceRecordingActionResult.Failed()
        }

        if (durationMillis < minimumVoiceDurationMillis) {
            recordedFile.delete()
            return ChatVoiceRecordingActionResult.TooShort
        }

        return ChatVoiceRecordingActionResult.ReadyToSend(
            audioFile = recordedFile,
            durationMillis = durationMillis
        )
    }

    private fun releaseVoiceRecorder(
        deleteCurrentFile: Boolean,
        notifyState: Boolean
    ) {
        val recorder = voiceRecorder
        val voiceFile = currentVoiceFile

        runCatching {
            recorder?.reset()
        }
        runCatching {
            recorder?.release()
        }

        if (deleteCurrentFile) {
            voiceFile?.delete()
        }

        voiceRecorder = null
        currentVoiceFile = null
        voiceRecordStartedAt = 0L
        isRecordingVoice = false

        if (notifyState) {
            onVoiceRecordingStateChanged(false)
        }
    }

    private fun handleAudioPlaybackFailure(
        failedPlayer: ChatAudioPlayerSession,
        error: Throwable
    ) {
        if (audioPlayer !== failedPlayer) return

        stopAudioPlayback()
        onAudioPlaybackError(error)
    }

    private fun stopAudioPlayback() {
        val player = audioPlayer

        runCatching {
            player?.clearListeners()
        }
        runCatching {
            if (player?.isPlaying == true) {
                player.stop()
            }
        }
        runCatching {
            player?.release()
        }

        audioPlayer = null
        playingAudioUrl = null
        onAudioPlaybackStateChanged(null)
    }
}
