package com.masuidrive.gestureime.voice

import android.speech.SpeechRecognizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
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

        assertEquals(VoiceBackendState.Preview("日本語") to 7L, states.last())
        assertEquals("日本語", controller.confirm(7))
        assertTrue(recognizer.destroyed)
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
        assertEquals(VoiceBackendState.Preview("停止後") to 5L, states.last())
    }

    @Test
    fun previewCannotCommitIntoAnotherEditorSession() {
        val controller = controller()
        controller.start(21)
        recognizer.supportCallback?.invoke(true)
        recognizer.listener?.onResults(listOf("元の欄"))

        assertNull(controller.confirm(22))
        assertEquals(VoiceBackendState.Preview("元の欄") to 21L, states.last())
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
