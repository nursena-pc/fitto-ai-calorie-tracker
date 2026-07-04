package com.example.fitto.feature_home.presentation.social

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatMediaControllerTest {

    @Test
    fun toggleVoiceRecordingStartsThenReturnsReadyAudio() {
        val audioFile = newAudioFile()
        val recorder = FakeVoiceRecorderSession(audioFile)
        val recordingStates = mutableListOf<Boolean>()
        val elapsedTimes = ArrayDeque(listOf(1_000L, 2_500L))
        val controller = createController(
            audioFile = audioFile,
            recorder = recorder,
            elapsedRealtime = { elapsedTimes.removeFirst() },
            recordingStates = recordingStates
        )

        val startResult = controller.toggleVoiceRecording()
        val stopResult = controller.toggleVoiceRecording()

        assertEquals(ChatVoiceRecordingActionResult.Started, startResult)
        assertTrue(stopResult is ChatVoiceRecordingActionResult.ReadyToSend)
        stopResult as ChatVoiceRecordingActionResult.ReadyToSend
        assertEquals(audioFile, stopResult.audioFile)
        assertEquals(1_500L, stopResult.durationMillis)
        assertEquals(listOf(true, false), recordingStates)
        assertTrue(recorder.wasPrepared)
        assertTrue(recorder.wasStopped)
        assertTrue(recorder.wasReset)
        assertTrue(recorder.wasReleased)
        audioFile.delete()
    }

    @Test
    fun shortVoiceRecordingIsDeletedAndReported() {
        val audioFile = newAudioFile()
        val recorder = FakeVoiceRecorderSession(audioFile)
        val elapsedTimes = ArrayDeque(listOf(1_000L, 1_500L))
        val controller = createController(
            audioFile = audioFile,
            recorder = recorder,
            elapsedRealtime = { elapsedTimes.removeFirst() }
        )

        controller.toggleVoiceRecording()
        val stopResult = controller.toggleVoiceRecording()

        assertEquals(ChatVoiceRecordingActionResult.TooShort, stopResult)
        assertFalse(audioFile.exists())
    }

    @Test
    fun failedVoiceStopDeletesIncompleteRecording() {
        val audioFile = newAudioFile()
        val recorder = FakeVoiceRecorderSession(
            outputFile = audioFile,
            stopFailure = IllegalStateException("stop failed")
        )
        val elapsedTimes = ArrayDeque(listOf(1_000L, 2_000L))
        val controller = createController(
            audioFile = audioFile,
            recorder = recorder,
            elapsedRealtime = { elapsedTimes.removeFirst() }
        )

        controller.toggleVoiceRecording()
        val stopResult = controller.toggleVoiceRecording()

        assertTrue(stopResult is ChatVoiceRecordingActionResult.Failed)
        assertFalse(audioFile.exists())
        assertTrue(recorder.wasReleased)
    }

    @Test
    fun playingSameAudioAgainStopsCurrentPlayback() {
        val audioFactory = FakeAudioPlayerFactory()
        val playbackStates = mutableListOf<String?>()
        val controller = createController(
            audioPlayerFactory = audioFactory,
            playbackStates = playbackStates
        )

        controller.playOrStopAudio("https://fitto/audio.m4a")
        val player = audioFactory.sessions.single()
        player.completePreparation()

        controller.playOrStopAudio("https://fitto/audio.m4a")

        assertTrue(player.wasStarted)
        assertTrue(player.wasStopped)
        assertTrue(player.wasReleased)
        assertNull(playbackStates.last())
    }

    @Test
    fun audioCompletionReleasesPlayerAndClearsPlaybackState() {
        val audioFactory = FakeAudioPlayerFactory()
        val playbackStates = mutableListOf<String?>()
        val controller = createController(
            audioPlayerFactory = audioFactory,
            playbackStates = playbackStates
        )

        controller.playOrStopAudio("https://fitto/audio.m4a")
        val player = audioFactory.sessions.single()
        player.completePreparation()
        player.completePlayback()

        assertTrue(player.wasReleased)
        assertNull(playbackStates.last())
    }

    @Test
    fun audioFailureClearsPlaybackAndReportsError() {
        val audioFactory = FakeAudioPlayerFactory()
        val playbackStates = mutableListOf<String?>()
        val playbackErrors = mutableListOf<Throwable>()
        val controller = createController(
            audioPlayerFactory = audioFactory,
            playbackStates = playbackStates,
            playbackErrors = playbackErrors
        )

        controller.playOrStopAudio("https://fitto/audio.m4a")
        val player = audioFactory.sessions.single()
        player.failPlayback(IllegalStateException("play failed"))

        assertTrue(player.wasReleased)
        assertNull(playbackStates.last())
        assertEquals("play failed", playbackErrors.single().message)
    }

    private fun createController(
        audioFile: File = newAudioFile(),
        recorder: FakeVoiceRecorderSession = FakeVoiceRecorderSession(audioFile),
        elapsedRealtime: () -> Long = { 0L },
        recordingStates: MutableList<Boolean> = mutableListOf(),
        audioPlayerFactory: FakeAudioPlayerFactory = FakeAudioPlayerFactory(),
        playbackStates: MutableList<String?> = mutableListOf(),
        playbackErrors: MutableList<Throwable> = mutableListOf()
    ): ChatMediaController {
        return ChatMediaController(
            voiceFileFactory = { audioFile },
            voiceRecorderFactory = ChatVoiceRecorderFactory { recorder },
            audioPlayerFactory = audioPlayerFactory,
            elapsedRealtime = elapsedRealtime,
            minimumVoiceDurationMillis = 800L,
            onVoiceRecordingStateChanged = recordingStates::add,
            onAudioPlaybackStateChanged = playbackStates::add,
            onAudioPlaybackError = playbackErrors::add
        )
    }

    private fun newAudioFile(): File {
        return File.createTempFile("fitto_voice_", ".m4a").apply {
            delete()
            deleteOnExit()
        }
    }

    private class FakeVoiceRecorderSession(
        private val outputFile: File,
        private val stopFailure: Throwable? = null
    ) : ChatVoiceRecorderSession {
        var wasPrepared = false
        var wasStopped = false
        var wasReset = false
        var wasReleased = false

        override fun prepareAndStart(outputFile: File) {
            wasPrepared = true
            outputFile.writeBytes(byteArrayOf(1, 2, 3))
        }

        override fun stop() {
            stopFailure?.let { throw it }
            wasStopped = true
        }

        override fun reset() {
            wasReset = true
        }

        override fun release() {
            wasReleased = true
        }
    }

    private class FakeAudioPlayerFactory : ChatAudioPlayerFactory {
        val sessions = mutableListOf<FakeAudioPlayerSession>()

        override fun create(): ChatAudioPlayerSession {
            return FakeAudioPlayerSession().also(sessions::add)
        }
    }

    private class FakeAudioPlayerSession : ChatAudioPlayerSession {
        override var isPlaying: Boolean = false
        var wasStarted = false
        var wasStopped = false
        var wasReleased = false
        private var onPrepared: (() -> Unit)? = null
        private var onCompletion: (() -> Unit)? = null
        private var onError: ((Throwable) -> Unit)? = null

        override fun prepare(
            url: String,
            onPrepared: () -> Unit,
            onCompletion: () -> Unit,
            onError: (Throwable) -> Unit
        ) {
            this.onPrepared = onPrepared
            this.onCompletion = onCompletion
            this.onError = onError
        }

        override fun start() {
            wasStarted = true
            isPlaying = true
        }

        override fun clearListeners() {
            onPrepared = null
            onCompletion = null
            onError = null
        }

        override fun stop() {
            wasStopped = true
            isPlaying = false
        }

        override fun release() {
            wasReleased = true
            isPlaying = false
        }

        fun completePreparation() {
            onPrepared?.invoke()
        }

        fun completePlayback() {
            onCompletion?.invoke()
        }

        fun failPlayback(error: Throwable) {
            onError?.invoke(error)
        }
    }
}
