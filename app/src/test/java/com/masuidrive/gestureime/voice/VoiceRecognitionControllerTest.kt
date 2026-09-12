package com.masuidrive.gestureime.voice

import android.speech.SpeechRecognizer
import android.os.Looper
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class VoiceRecognitionControllerTest {
    private val states = mutableListOf<Pair<VoiceBackendState, Long>>()
    private val recognizer = FakeRecognizer()

    @Test
    fun unavailableAndPermissionStatesDoNotCreateRecognizer() {
        val oldAndroid = controller(sdk = 30)
        assertTrue(oldAndroid.initialState() is VoiceBackendState.Unavailable)

        val denied = controller(permission = false)
        denied.start(4)
        assertEquals(VoiceBackendState.PermissionRequired to 4L, states.last())
        assertEquals(0, recognizer.startCount)
    }

    @Test
    fun installedJapaneseModelProducesPreviewOnlyUntilConfirm() {
        val controller = controller()
        controller.start(7)
        recognizer.supportCallback?.invoke(true)
        recognizer.listener?.onReady()
        recognizer.listener?.onResults(listOf("日本語"))

        assertEquals(VoiceBackendState.Preview(listOf("日本語")) to 7L, states.last())
        assertEquals("日本語", controller.confirm(7))
        assertTrue(recognizer.destroyed)
    }

    @Test
    fun endOfSpeechPromotesTheLatestOfMultipleLongSpeechPartialsAfterGracePeriod() {
        val controller = controller()
        controller.start(8)
        recognizer.supportCallback?.invoke(true)
        recognizer.listener?.onReady()
        recognizer.listener?.onPartialResults(listOf("", "途中"))
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(11))
        recognizer.listener?.onPartialResults(listOf("最新の途中結果"))

        assertEquals(VoiceBackendState.Partial("最新の途中結果") to 8L, states.last())
        assertNull(controller.confirm(8))

        recognizer.listener?.onEndOfSpeech()
        assertEquals(VoiceBackendState.Partial("最新の途中結果") to 8L, states.last())
        assertNull(controller.confirm(8))

        Shadows.shadowOf(Looper.getMainLooper()).idleFor(
            Duration.ofMillis(VoiceRecognitionController.END_OF_SPEECH_GRACE_MS),
        )

        assertEquals(VoiceBackendState.Preview(listOf("最新の途中結果")) to 8L, states.last())
        assertEquals("最新の途中結果", controller.confirm(8))
    }

    @Test
    fun finalResultDuringGraceWinsOverThePartialFallback() {
        val controller = controller()
        controller.start(9)
        recognizer.supportCallback?.invoke(true)
        recognizer.listener?.onPartialResults(listOf("途中結果"))
        recognizer.listener?.onEndOfSpeech()
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(
            Duration.ofMillis(VoiceRecognitionController.END_OF_SPEECH_GRACE_MS - 1),
        )
        recognizer.listener?.onResults(listOf("遅れて届いた最終結果"))

        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1))

        assertEquals(VoiceBackendState.Preview(listOf("遅れて届いた最終結果")) to 9L, states.last())
        assertEquals("遅れて届いた最終結果", controller.confirm(9))
    }

    @Test
    fun cancelInvalidatesTheScheduledPartialFallback() {
        val controller = controller()
        controller.start(10)
        recognizer.supportCallback?.invoke(true)
        recognizer.listener?.onPartialResults(listOf("破棄される途中結果"))
        recognizer.listener?.onEndOfSpeech()
        controller.cancel()

        Shadows.shadowOf(Looper.getMainLooper()).idleFor(
            Duration.ofMillis(VoiceRecognitionController.END_OF_SPEECH_GRACE_MS),
        )

        assertEquals(VoiceBackendState.Idle to 10L, states.last())
        assertNull(controller.confirm(10))
    }

    @Test
    fun eligibleTerminalErrorAfterEndOfSpeechPromotesTheLastPartialAndInvalidatesLateCallbacks() {
        val controller = controller()
        controller.start(12)
        recognizer.supportCallback?.invoke(true)
        val finishedListener = recognizer.listener
        finishedListener?.onPartialResults(listOf("最初", "候補"))
        finishedListener?.onPartialResults(listOf("エラー直前の途中結果"))
        finishedListener?.onEndOfSpeech()
        finishedListener?.onError(SpeechRecognizer.ERROR_NO_MATCH)
        finishedListener?.onResults(listOf("遅すぎる最終結果"))

        Shadows.shadowOf(Looper.getMainLooper()).idleFor(
            Duration.ofMillis(VoiceRecognitionController.END_OF_SPEECH_GRACE_MS),
        )

        assertEquals(VoiceBackendState.Preview(listOf("エラー直前の途中結果")) to 12L, states.last())
        assertEquals("エラー直前の途中結果", controller.confirm(12))
    }

    @Test
    fun busyErrorDoesNotPromoteAPartial() {
        val controller = controller()
        controller.start(13)
        recognizer.supportCallback?.invoke(true)
        recognizer.listener?.onPartialResults(listOf("途中結果"))
        recognizer.listener?.onError(SpeechRecognizer.ERROR_RECOGNIZER_BUSY)

        assertTrue(states.last().first is VoiceBackendState.Unavailable)
        assertNull(controller.confirm(13))
    }

    @Test
    fun missingJapaneseModelNeverStartsListening() {
        val controller = controller()
        controller.start(2)
        recognizer.supportCallback?.invoke(false)

        assertEquals(0, recognizer.startCount)
        assertTrue(states.last().first is VoiceBackendState.Unavailable)
    }

    @Test
    fun cancelDropsDelayedResultAndStaleConfirm() {
        val controller = controller()
        controller.start(11)
        recognizer.supportCallback?.invoke(true)
        val delayedListener = recognizer.listener
        controller.cancel()
        delayedListener?.onPartialResults(listOf("古い途中結果"))
        delayedListener?.onResults(listOf("古い結果"))

        assertEquals(VoiceBackendState.Idle to 11L, states.last())
        assertNull(controller.confirm(11))
    }

    @Test
    fun stopWaitsForFinalResult() {
        val controller = controller()
        controller.start(5)
        recognizer.supportCallback?.invoke(true)
        controller.stop()

        assertTrue(recognizer.stopped)
        assertEquals(VoiceBackendState.Recognizing to 5L, states.last())
        recognizer.listener?.onResults(listOf("停止後"))
        assertEquals(VoiceBackendState.Preview(listOf("停止後")) to 5L, states.last())
    }

    @Test
    fun previewCannotCommitIntoAnotherEditorSession() {
        val controller = controller()
        controller.start(21)
        recognizer.supportCallback?.invoke(true)
        recognizer.listener?.onResults(listOf("元の欄"))

        assertNull(controller.confirm(22))
        assertEquals(VoiceBackendState.Preview(listOf("元の欄")) to 21L, states.last())
    }

    @Test
    fun confirmThenSameEditorStartCreatesANewSessionAndDropsThePreviousCallback() {
        val controller = controller()
        controller.start(31)
        recognizer.supportCallback?.invoke(true)
        val firstListener = recognizer.listener
        firstListener?.onResults(listOf("一回目"))

        assertEquals("一回目", controller.confirm(31))
        controller.start(31)
        recognizer.supportCallback?.invoke(true)
        firstListener?.onResults(listOf("古い結果"))
        recognizer.listener?.onResults(listOf("二回目"))

        assertEquals(VoiceBackendState.Preview(listOf("二回目")) to 31L, states.last())
        assertEquals(2, recognizer.startCount)
    }

    private fun controller(
        sdk: Int = 35,
        permission: Boolean = true,
        available: Boolean = true,
    ) = VoiceRecognitionController(
        sdkInt = sdk,
        hasPermission = { permission },
        onDeviceAvailable = { available },
        factory = VoiceRecognizerFactory { listener -> recognizer.apply { this.listener = listener } },
        onState = { state, token -> states += state to token },
    )

    private class FakeRecognizer : VoiceRecognizer {
        var listener: VoiceRecognizerListener? = null
        var supportCallback: ((Boolean?) -> Unit)? = null
        var startCount = 0
        var stopped = false
        var destroyed = false
        override fun checkJapaneseSupport(callback: (Boolean?) -> Unit) { supportCallback = callback }
        override fun start() { startCount++ }
        override fun stop() { stopped = true }
        override fun cancel() = Unit
        override fun destroy() { destroyed = true }
    }
}
