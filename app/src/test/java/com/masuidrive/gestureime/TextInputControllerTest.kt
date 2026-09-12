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
    fun predictionContextIsBoundedByCodePointAndKeepsTheCursorSides() {
        controller.beginInput(EditorInfo().apply { inputType = InputType.TYPE_CLASS_TEXT })
        input.beforeCursor = "前".repeat(130)
        input.afterCursor = "後".repeat(130)

        val context = requireNotNull(controller.predictionContext())

        assertEquals("前".repeat(128), context.precedingText)
        assertEquals("後".repeat(128), context.followingText)
        assertEquals(1, input.beforeCursorReads)
        assertEquals(1, input.afterCursorReads)
    }

    @Test
    fun predictionContextDoesNotReadPrivateEditorText() {
        controller.beginInput(EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        })

        assertEquals(null, controller.predictionContext())
        assertEquals(0, input.beforeCursorReads)
        assertEquals(0, input.afterCursorReads)

        controller.beginInput(EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT
            imeOptions = EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
        })
        assertEquals(null, controller.predictionContext())
        assertEquals(0, input.beforeCursorReads)
        assertEquals(0, input.afterCursorReads)
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
    fun verticalCursorMovementDoesNotEscapePastKnownEditorLines() {
        input.extracted = ExtractedText().apply {
            text = "first\nsecond"
            selectionStart = 1
            selectionEnd = 1
        }

        controller.moveCursor(Direction.DOWN, 4)

        assertEquals(7 to 7, input.selection)
        assertTrue(input.keyEvents.isEmpty())
    }

    @Test
    fun verticalCursorMovementStopsAtDocumentBoundaries() {
        input.extracted = ExtractedText().apply {
            text = "first\nsecond"
            selectionStart = 0
            selectionEnd = 0
        }
        controller.moveCursor(Direction.UP, 2)

        input.extracted = ExtractedText().apply {
            text = "first\nsecond"
            selectionStart = text.length
            selectionEnd = text.length
        }
        controller.moveCursor(Direction.DOWN, 2)

        assertEquals(12 to 12, input.selection)
        assertTrue(input.keyEvents.isEmpty())
    }

    @Test
    fun verticalCursorMovementKeepsPartialExtractedTextOffset() {
        input.extracted = ExtractedText().apply {
            text = "ab\nc"
            startOffset = 40
            selectionStart = 1
            selectionEnd = 1
        }

        controller.moveCursor(Direction.DOWN, 1)

        assertEquals(44 to 44, input.selection)
    }

    @Test
    fun terminalCursorModeSendsArrowAndMoveBoundaryKeyPairs() {
        controller.beginInput(EditorInfo().apply { inputType = InputType.TYPE_CLASS_TEXT })
        controller.terminalCursorEnabled = true
        controller.moveCursor(Direction.LEFT, 1)
        controller.moveToBoundary(CursorBoundary.END)
        assertEquals(listOf(KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_MOVE_END, KeyEvent.KEYCODE_MOVE_END), input.keyEvents)
    }

    @Test
    fun terminalCursorModeDoesNotSendKeysInPrivateField() {
        controller.beginInput(EditorInfo().apply { inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD })
        controller.terminalCursorEnabled = true
        controller.moveCursor(Direction.RIGHT, 1)
        assertTrue(input.keyEvents.isEmpty())
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
        var beforeCursor = ""
        var afterCursor = ""
        var beforeCursorReads = 0
        var afterCursorReads = 0

        override fun getTextBeforeCursor(length: Int, flags: Int): CharSequence {
            beforeCursorReads++
            return beforeCursor
        }

        override fun getTextAfterCursor(length: Int, flags: Int): CharSequence {
            afterCursorReads++
            return afterCursor
        }

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
