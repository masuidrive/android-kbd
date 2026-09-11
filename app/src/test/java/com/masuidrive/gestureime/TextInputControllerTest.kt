package com.masuidrive.gestureime

import android.content.ClipboardManager
import android.text.InputType
import android.view.View
import android.view.KeyEvent
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedText
import android.view.inputmethod.ExtractedTextRequest
import com.masuidrive.gestureime.keyboard.Direction
import com.masuidrive.gestureime.keyboard.Modifier
import com.masuidrive.gestureime.keyboard.CursorBoundary
import com.masuidrive.gestureime.keyboard.KanaTransform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TextInputControllerTest {
    private val context = RuntimeEnvironment.getApplication()
    private lateinit var input: RecordingInputConnection
    private lateinit var controller: TextInputController

    @Before
    fun setUp() {
        input = RecordingInputConnection(View(context))
        controller = TextInputController(
            connection = { input },
            context = context,
            clipboard = context.getSystemService(ClipboardManager::class.java),
        )
    }

    @Test
    fun passwordAndNoLearningFieldsArePrivate() {
        controller.beginInput(EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        })
        assertTrue(controller.isPrivateField)

        controller.beginInput(EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT
            imeOptions = EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
        })
        assertTrue(controller.isPrivateField)

        controller.beginInput(EditorInfo().apply { inputType = InputType.TYPE_CLASS_TEXT })
        assertFalse(controller.isPrivateField)
    }

    @Test
    fun partialExtractedTextAddsStartOffsetWhenMovingCursor() {
        input.extracted = ExtractedText().apply {
            text = "bcde"
            startOffset = 100
            selectionStart = 2
            selectionEnd = 2
        }

        controller.moveCursor(Direction.RIGHT, 1)

        assertEquals(103 to 103, input.selection)
    }

    @Test
    fun ctrlAUsesEditorContextAction() {
        controller.sendModifiedKey("a", Modifier.CTRL)

        assertEquals(android.R.id.selectAll, input.contextAction)
        assertTrue(input.keyEvents.isEmpty())
    }

    @Test
    fun ctrlVPasteIsSuppressedInPrivateFields() {
        controller.beginInput(EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        })

        controller.sendModifiedKey("v", Modifier.CTRL)

        assertEquals(null, input.contextAction)
        assertTrue(input.keyEvents.isEmpty())
    }

    @Test
    fun abandoningCompositionDoesNotMutateTheNewSelection() {
        controller.appendComposing("かな")
        controller.abandonComposition()
        controller.commitText("x")

        assertEquals(listOf("compose:かな", "commit:x"), input.operations)
    }

    @Test
    fun staleEditorSessionTokenIsRejected() {
        val gate = EditorSessionGate()
        val queuedToken = gate.capture()

        gate.advance()

        assertFalse(gate.isCurrent(queuedToken))
        assertTrue(gate.isCurrent(gate.capture()))
    }

    @Test
    fun actionResumingAfterSlowResetCannotMutateNewEditor() = runBlocking {
        val gate = EditorSessionGate()
        val resetStarted = CompletableDeferred<Unit>()
        val finishReset = CompletableDeferred<Unit>()
        val oldToken = gate.capture()
        var newEditorText = ""
        val oldAction = async {
            resetStarted.complete(Unit)
            finishReset.await()
            gate.runIfCurrent(oldToken) { newEditorText = "stale input" }
        }

        resetStarted.await()
        gate.advance()
        finishReset.complete(Unit)

        assertFalse(oldAction.await())
        assertEquals("", newEditorText)
    }

    @Test
    fun endBoundaryUsesExtractedTextOffset() {
        input.extracted = ExtractedText().apply {
            text = "partial"
            startOffset = 40
            selectionStart = 0
            selectionEnd = 0
        }

        controller.moveToBoundary(CursorBoundary.END)

        assertEquals(47 to 47, input.selection)
    }

    @Test
    fun committingCandidateReplacesCompositionWithoutFinishingFirst() {
        controller.beginInput(EditorInfo().apply { inputType = InputType.TYPE_CLASS_TEXT })
        controller.appendComposing("かな")
        controller.commitCandidate("仮名")

        assertEquals(listOf("compose:かな", "commit:仮名"), input.operations)
    }

    @Test
    fun escapeSendsDownAndUpKeyEvents() {
        controller.escape()

        assertEquals(listOf(KeyEvent.KEYCODE_ESCAPE, KeyEvent.KEYCODE_ESCAPE), input.keyEvents)
    }

    @Test
    fun directedKanaTransformsOnlyApplyToMatchingKana() {
        controller.appendComposing("は")
        assertEquals("ば", controller.transformKana(KanaTransform.DAKUTEN))
        assertEquals("ば", controller.transformKana(KanaTransform.HANDAKUTEN))

        controller.beginInput(EditorInfo())
        controller.appendComposing("は")
        assertEquals("ぱ", controller.transformKana(KanaTransform.HANDAKUTEN))
    }

    private class RecordingInputConnection(view: View) : BaseInputConnection(view, true) {
        var extracted = ExtractedText().apply { text = ""; startOffset = 0; selectionStart = 0; selectionEnd = 0 }
        var selection: Pair<Int, Int>? = null
        var contextAction: Int? = null
        val keyEvents = mutableListOf<Int>()
        val operations = mutableListOf<String>()

        override fun getExtractedText(request: ExtractedTextRequest?, flags: Int): ExtractedText = extracted

        override fun setSelection(start: Int, end: Int): Boolean {
            selection = start to end
            return true
        }

        override fun performContextMenuAction(id: Int): Boolean {
            contextAction = id
            return true
        }

        override fun sendKeyEvent(event: android.view.KeyEvent): Boolean {
            keyEvents += event.keyCode
            return true
        }

        override fun setComposingText(text: CharSequence?, newCursorPosition: Int): Boolean {
            operations += "compose:$text"
            return true
        }

        override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
            operations += "commit:$text"
            return true
        }

        override fun finishComposingText(): Boolean {
            operations += "finish"
            return true
        }
    }
}
