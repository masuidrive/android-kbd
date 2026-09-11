package com.masuidrive.gestureime

import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import com.masuidrive.gestureime.keyboard.KeyAction
import com.masuidrive.gestureime.keyboard.KeyboardMode
import com.masuidrive.gestureime.keyboard.KeyboardUiState
import com.masuidrive.gestureime.keyboard.KeyboardView
import com.masuidrive.gestureime.ui.CandidateStripView
import com.masuidrive.gestureime.ui.CandidateUiSnapshot
import com.masuidrive.gestureime.ui.VoiceUiSnapshot
import com.masuidrive.gestureime.ui.VoiceUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ImeServiceVoiceLifecycleTest {
    @Test
    fun inputViewIncludesKeyboardIntrinsicHeightWithoutAnExactParent() {
        val controller = Robolectric.buildService(ImeService::class.java).create()
        val root = controller.get().onCreateInputView() as ViewGroup

        root.measure(
            View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1_000, View.MeasureSpec.AT_MOST),
        )

        val keyboard = root.keyboardView()
        assertEquals(ViewGroup.LayoutParams.WRAP_CONTENT, keyboard.layoutParams.height)
        assertEquals(228, keyboard.measuredHeight)
        assertEquals(278, root.measuredHeight)
        controller.destroy()
    }

    @Test
    fun candidateAndVoiceContentDoNotChangeInputViewHeight() {
        val controller = Robolectric.buildService(ImeService::class.java).create()
        val root = controller.get().onCreateInputView() as ViewGroup
        val strip = root.candidateStripView()
        val width = View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY)
        val height = View.MeasureSpec.makeMeasureSpec(1_000, View.MeasureSpec.AT_MOST)

        fun measuredHeight(): Int {
            root.measure(width, height)
            return root.measuredHeight
        }

        val emptyHeight = measuredHeight()
        strip.showCandidates(CandidateUiSnapshot(1L, listOf("候補", "変換候補")))
        assertEquals(emptyHeight, measuredHeight())
        strip.showCandidates(CandidateUiSnapshot(2L, listOf("compact", "clear", "candidate")))
        assertEquals(emptyHeight, measuredHeight())
        strip.setVoiceState(VoiceUiSnapshot(3L, VoiceUiState.Recording))
        assertEquals(emptyHeight, measuredHeight())
        strip.setVoiceState(VoiceUiSnapshot(4L, VoiceUiState.PermissionRequired))
        assertEquals(emptyHeight, measuredHeight())
        controller.destroy()
    }

    @Test
    fun privateEditorHidesCandidateContentWithoutMovingKeyboard() {
        val controller = Robolectric.buildService(ImeService::class.java).create()
        val service = controller.get()
        val root = service.onCreateInputView() as ViewGroup
        val strip = root.candidateStripView()
        val width = View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY)
        val height = View.MeasureSpec.makeMeasureSpec(1_000, View.MeasureSpec.AT_MOST)

        root.measure(width, height)
        val normalHeight = root.measuredHeight
        val normalKeyboardHeight = root.keyboardView().measuredHeight

        service.onStartInput(EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }, false)
        root.measure(width, height)
        assertEquals(View.INVISIBLE, strip.visibility)
        assertEquals(normalHeight, root.measuredHeight)
        assertEquals(normalKeyboardHeight, root.keyboardView().measuredHeight)

        service.onStartInput(EditorInfo().apply { inputType = InputType.TYPE_CLASS_TEXT }, false)
        root.measure(width, height)
        assertEquals(View.VISIBLE, strip.visibility)
        assertEquals(normalHeight, root.measuredHeight)
        assertEquals(normalKeyboardHeight, root.keyboardView().measuredHeight)
        controller.destroy()
    }

    @Test
    fun initiallyPrivateEditorCreatesAnInvisibleFixedHeightCandidateStrip() {
        val controller = Robolectric.buildService(ImeService::class.java).create()
        val service = controller.get()
        service.onStartInput(EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }, false)
        val root = service.onCreateInputView() as ViewGroup

        root.measure(
            View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1_000, View.MeasureSpec.AT_MOST),
        )

        assertEquals(View.INVISIBLE, root.candidateStripView().visibility)
        assertEquals(228, root.keyboardView().measuredHeight)
        assertEquals(278, root.measuredHeight)
        controller.destroy()
    }

    @Test
    fun switchedLayerIsRestoredWhenInputViewIsRecreated() {
        val controller = Robolectric.buildService(ImeService::class.java).create()
        val service = controller.get()
        service.onCreateInputView()

        service.onKeyAction(KeyAction.SwitchLayer(KeyboardMode.SYMBOLS))
        assertEquals(KeyboardMode.SYMBOLS, ImePreferences.getLastKeyboardMode(service))

        val recreated = service.onCreateInputView().keyboardView()
        assertEquals(KeyboardMode.SYMBOLS, recreated.mode())
        controller.destroy()
    }

    @Test
    fun closingAndReopeningInputViewHidesThenRecreatesVoiceControl() {
        val controller = Robolectric.buildService(ImeService::class.java).create()
        val service = controller.get()
        val firstView = service.onCreateInputView()
        assertTrue(firstView.hasVoiceControl())

        service.onFinishInputView(false)
        assertFalse(firstView.hasVoiceControl())

        service.onStartInputView(EditorInfo(), true)
        assertTrue(firstView.hasVoiceControl())

        val reopenedView = service.onCreateInputView()
        assertTrue(reopenedView.hasVoiceControl())
        controller.destroy()
    }

    @Test
    fun closingVoiceLayerRestoresItsPreviousModeAndClearsCandidates() {
        val controller = Robolectric.buildService(ImeService::class.java).create()
        val service = controller.get()
        service.onStartInput(EditorInfo(), false)
        val root = service.onCreateInputView()
        val keyboard = root.keyboardView()
        val strip = root.candidateStripView()

        service.setModeForLifecycleTest(KeyboardMode.VOICE, returnMode = KeyboardMode.KANA)
        keyboard.setMode(KeyboardMode.VOICE)
        assertEquals(KeyboardMode.VOICE, keyboard.mode())
        strip.showCandidates(CandidateUiSnapshot(99, listOf("古い音声候補")))

        service.onFinishInputView(false)
        service.onStartInputView(EditorInfo(), true)

        assertEquals(KeyboardMode.KANA, keyboard.mode())
        assertFalse(root.containsText("古い音声候補"))
        controller.destroy()
    }

    @Test
    fun finishingInputAloneCannotLeaveTheKeyboardInVoiceMode() {
        val controller = Robolectric.buildService(ImeService::class.java).create()
        val service = controller.get()
        val keyboard = service.onCreateInputView().keyboardView()
        service.setModeForLifecycleTest(KeyboardMode.VOICE, returnMode = KeyboardMode.SYMBOLS)
        keyboard.setMode(KeyboardMode.VOICE)

        service.onFinishInput()

        assertEquals(KeyboardMode.SYMBOLS, keyboard.mode())
        controller.destroy()
    }

    private fun View.hasVoiceControl(): Boolean {
        if (contentDescription?.let { it.contains("音声") || it.contains("マイク") } == true) return true
        val group = this as? ViewGroup ?: return false
        return (0 until group.childCount).any { group.getChildAt(it).hasVoiceControl() }
    }

    private fun View.containsText(value: String): Boolean {
        if (this is android.widget.TextView && text.toString() == value) return true
        val group = this as? ViewGroup ?: return false
        return (0 until group.childCount).any { group.getChildAt(it).containsText(value) }
    }

    private fun ImeService.setModeForLifecycleTest(mode: KeyboardMode, returnMode: KeyboardMode) {
        ImeService::class.java.getDeclaredField("keyboardMode").apply { isAccessible = true }.set(this, mode)
        ImeService::class.java.getDeclaredField("voiceReturnMode").apply { isAccessible = true }.set(this, returnMode)
    }

    private fun View.keyboardView(): KeyboardView {
        if (this is KeyboardView) return this
        val group = this as? ViewGroup ?: error("KeyboardView not found")
        return (0 until group.childCount)
            .asSequence()
            .mapNotNull { runCatching { group.getChildAt(it).keyboardView() }.getOrNull() }
            .first()
    }

    private fun View.candidateStripView(): CandidateStripView {
        if (this is CandidateStripView) return this
        val group = this as? ViewGroup ?: error("CandidateStripView not found")
        return (0 until group.childCount)
            .asSequence()
            .mapNotNull { runCatching { group.getChildAt(it).candidateStripView() }.getOrNull() }
            .first()
    }

    private fun KeyboardView.mode(): KeyboardMode {
        val field = KeyboardView::class.java.getDeclaredField("state").apply { isAccessible = true }
        return (field.get(this) as KeyboardUiState).mode
    }
}
