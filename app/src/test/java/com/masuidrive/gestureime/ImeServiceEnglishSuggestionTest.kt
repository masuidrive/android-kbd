package com.masuidrive.gestureime

import android.content.ClipboardManager
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import com.masuidrive.gestureime.conversion.ConversionEngine
import com.masuidrive.gestureime.conversion.ConversionState
import com.masuidrive.gestureime.keyboard.KeyAction
import com.masuidrive.gestureime.keyboard.KeyboardMode
import com.masuidrive.gestureime.keyboard.VoiceHoldEvent
import com.masuidrive.gestureime.suggestion.EnglishSuggestionEngine
import com.masuidrive.gestureime.voice.VoiceRecognitionController
import com.masuidrive.gestureime.voice.VoiceRecognizerFactory
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ImeServiceEnglishSuggestionTest {
    @Test
    fun candidateTapReplacesComposingTextWithoutDuplicatingRawPrefix() {
        val harness = Harness { prefix, _ -> if (prefix == "he") listOf("hello", "help") else emptyList() }

        harness.key("h")
        harness.key("e")
        harness.idle()
        harness.service.onKeyAction(KeyAction.SelectCandidate(0))
        harness.idle()

        assertEquals("hello", harness.input.visibleText)
    }

    @Test
    fun enterCommitsRawBufferWithoutAddingNewlineAndSpaceCommitsThenInsertsSpace() {
        val enter = Harness { _, _ -> emptyList() }
        enter.key("h")
        enter.key("i")
        enter.service.onKeyAction(KeyAction.Enter)
        enter.idle()
        assertEquals("hi", enter.input.visibleText)

        val space = Harness { _, _ -> emptyList() }
        space.key("h")
        space.key("i")
        space.key(" ")
        space.idle()
        assertEquals("hi ", space.input.visibleText)
    }

    @Test
    fun staleCandidateTapQueuedBehindNewCharacterDoesNotReplaceNewerBuffer() {
        val harness = Harness { prefix, _ -> listOf(if (prefix == "h") "hello" else "help") }
        harness.key("h")
        harness.idle()
        val staleCandidate = harness.root.findView { it.contentDescription?.toString() == "候補 1: hello" }

        harness.key("e")
        harness.idle()
        staleCandidate!!.performClick()
        harness.idle()

        assertEquals("he", harness.input.visibleText)
        assertEquals("", harness.input.committed)
    }

    @Test
    fun lateLookupAfterEditorSwitchAndPrivateEditorNeverCreatesCandidateState() {
        val pending = CompletableDeferred<List<String>>()
        val harness = Harness { _, _ -> pending.await() }
        harness.key("h")
        harness.service.onStartInput(EditorInfo(), false)
        pending.complete(listOf("hello"))
        harness.idle()
        harness.service.onKeyAction(KeyAction.SelectCandidate(0))
        harness.idle()
        assertEquals("", harness.input.committed)

        harness.service.onStartInput(EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }, false)
        harness.key("x")
        harness.idle()
        assertEquals("x", harness.input.committed)
    }

    @Test
    fun backspaceRefreshesBufferAndLayerSwitchFlushesRawBeforeChangingMode() {
        val harness = Harness { prefix, _ -> if (prefix == "he") listOf("hello") else emptyList() }
        harness.key("h")
        harness.key("e")
        harness.key("x")
        harness.service.onKeyAction(KeyAction.Backspace())
        harness.idle()
        assertEquals("he", harness.input.visibleText)

        harness.service.onKeyAction(KeyAction.SwitchLayer(KeyboardMode.KANA))
        harness.idle()
        assertEquals("he", harness.input.committed)
        assertEquals(KeyboardMode.KANA, ImePreferences.getLastKeyboardMode(harness.service))
    }

    @Test
    fun kanaAndNumberModeBoundariesKeepCompositionsSeparate() {
        val harness = Harness { _, _ -> emptyList() }
        harness.service.onKeyAction(KeyAction.KanaInput("か"))
        harness.key("a")
        harness.idle()
        assertEquals("かa", harness.input.visibleText)
        assertEquals("か", harness.input.committed)

        harness.service.onKeyAction(KeyAction.SwitchLayer(KeyboardMode.NUMBERS))
        harness.key("1")
        harness.service.onKeyAction(KeyAction.Enter)
        harness.idle()
        assertEquals("かa1", harness.input.committed)
    }

    @Test
    fun externalSelectionMoveInvalidatesCandidateAndNextTextStartsAtNewSelection() {
        val harness = Harness { _, _ -> listOf("hello") }
        harness.key("h")
        harness.key("e")
        harness.idle()
        val staleCandidate = harness.root.findView { it.contentDescription?.toString() == "候補 1: hello" }

        harness.service.onUpdateSelection(2, 2, 1, 1, 0, 2)
        harness.key("x")
        staleCandidate!!.performClick()
        harness.idle()

        assertEquals("x", harness.input.visibleText)
    }

    @Test
    fun voiceHoldBeginFlushesRawEnglishBeforeStartingVoiceSession() {
        val harness = Harness { _, _ -> emptyList() }
        harness.key("h")
        harness.key("i")
        harness.idle()

        harness.service.onVoiceHold(VoiceHoldEvent.Begin(41L))
        harness.idle()

        assertEquals("hi", harness.input.committed)
    }

    private class Harness(english: suspend (String, Int) -> List<String>) {
        private val controller = Robolectric.buildService(ImeService::class.java).create()
        val service = controller.get()
        val input = RecordingConnection(View(RuntimeEnvironment.getApplication()))
        lateinit var root: View

        init {
            ImePreferences.setEnglishSuggestionsEnabled(service, true)
            val text = TextInputController(
                connection = { input },
                context = service,
                clipboard = service.getSystemService(ClipboardManager::class.java),
            )
            service.installTestDependencies(
                voice = VoiceRecognitionController(
                    sdkInt = 30,
                    hasPermission = { false },
                    onDeviceAvailable = { false },
                    factory = VoiceRecognizerFactory { error("voice is not used") },
                    onState = service::onVoiceState,
                ),
                text = text,
                conversion = FakeConversion(),
                english = object : EnglishSuggestionEngine {
                    override suspend fun suggest(prefix: String, limit: Int) = english(prefix, limit)
                },
            )
            service.onStartInput(EditorInfo(), false)
            root = service.onCreateInputView()
            idle()
        }

        fun key(text: String) {
            service.onKeyAction(KeyAction.CommitText(text))
        }

        fun idle() = Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
    }

    private fun View.findView(predicate: (View) -> Boolean): View? {
        if (predicate(this)) return this
        if (this !is ViewGroup) return null
        for (index in 0 until childCount) getChildAt(index).findView(predicate)?.let { return it }
        return null
    }

    private class RecordingConnection(view: View) : BaseInputConnection(view, true) {
        var committed = ""
        private var composing = ""
        val visibleText get() = committed + composing

        override fun setComposingText(text: CharSequence?, newCursorPosition: Int): Boolean {
            composing = text?.toString().orEmpty()
            return true
        }

        override fun finishComposingText(): Boolean {
            committed += composing
            composing = ""
            return true
        }

        override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
            committed += text?.toString().orEmpty()
            composing = ""
            return true
        }
    }

    private class FakeConversion : ConversionEngine {
        override suspend fun start(reading: String) = ConversionState(reading, emptyList(), -1)
        override suspend fun update(reading: String) = start(reading)
        override suspend fun nextCandidate() = start("")
        override suspend fun commit(index: Int) = null
        override suspend fun reset() = Unit
    }
}
