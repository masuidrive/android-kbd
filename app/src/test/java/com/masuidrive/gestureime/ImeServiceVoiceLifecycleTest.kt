package com.masuidrive.gestureime

import android.graphics.Rect
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import com.masuidrive.gestureime.keyboard.KeyAction
import com.masuidrive.gestureime.keyboard.KeyboardMode
import com.masuidrive.gestureime.keyboard.KeyboardHeightPreset
import com.masuidrive.gestureime.keyboard.KeyboardUiState
import com.masuidrive.gestureime.keyboard.KeyboardView
import com.masuidrive.gestureime.ui.CandidateStripView
import com.masuidrive.gestureime.ui.CandidateUiSnapshot
import com.masuidrive.gestureime.ui.VoicePanelView
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
        assertEquals(248, keyboard.measuredHeight)
        assertEquals(298, root.measuredHeight)
        controller.destroy()
    }

    @Test
    fun missingHeightPresetKeepsLargeDualKanaGeometryAcrossInputViewRecreation() {
        val controller = Robolectric.buildService(ImeService::class.java).create()
        val service = controller.get()
        val preferences = service.getSharedPreferences("gesture_ime_preferences", 0)
        preferences.edit().clear().commit()
        ImePreferences.setLastKeyboardMode(service, KeyboardMode.KANA)
        ImePreferences.setDualFlickEnabled(service, true)
        try {
            listOf(412, 840).forEach { widthPixels ->
                val width = View.MeasureSpec.makeMeasureSpec(widthPixels, View.MeasureSpec.EXACTLY)
                val height = View.MeasureSpec.makeMeasureSpec(1_000, View.MeasureSpec.AT_MOST)

                val first = service.onCreateInputView() as ViewGroup
                first.measure(width, height)
                assertLargeKanaGeometry(first.keyboardView(), widthPixels)

                // InputMethodService creates a new tree after hide/show, app changes, and
                // configuration recreation. An absent pre-preset preference must not fall back.
                val recreated = service.onCreateInputView() as ViewGroup
                recreated.measure(width, height)
                assertLargeKanaGeometry(recreated.keyboardView(), widthPixels)
            }
        } finally {
            preferences.edit().clear().commit()
            controller.destroy()
        }
    }

    @Test
    fun candidateAndVoiceContentDoNotChangeInputViewHeight() {
        val controller = Robolectric.buildService(ImeService::class.java).create()
        val root = controller.get().onCreateInputView() as ViewGroup
        val strip = root.candidateStripView()
        val panel = root.voicePanelView()
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
        panel.setVoiceState(VoiceUiSnapshot(3L, VoiceUiState.Recording))
        assertEquals(emptyHeight, measuredHeight())
        panel.setVoiceState(VoiceUiSnapshot(4L, VoiceUiState.PermissionRequired))
        assertEquals(emptyHeight, measuredHeight())
        controller.destroy()
    }

    @Test
    fun candidateFacesAndFirstKeyFacesShareTheTenDpVerticalGapAtBothWidths() {
        val controller = Robolectric.buildService(ImeService::class.java).create()
        val root = controller.get().onCreateInputView() as ViewGroup
        val strip = root.candidateStripView()
        val keyboard = root.keyboardView()

        listOf(400 to 6, 840 to 13).forEach { (widthPixels, expectedLeft) ->
            strip.showCandidates(CandidateUiSnapshot(61, listOf("候補", "次")))
            root.measure(
                View.MeasureSpec.makeMeasureSpec(widthPixels, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(1_000, View.MeasureSpec.AT_MOST),
            )
            root.layout(0, 0, widthPixels, root.measuredHeight)

            val candidate = strip.allTextViews().single { it.text.toString() == "候補" }
            val firstKey = Rect().also { keyboard.accessibilityNodeProvider.createAccessibilityNodeInfo(0)!!.getBoundsInParent(it) }
            assertEquals("$widthPixels candidate left", expectedLeft, candidate.leftIn(strip))
            assertEquals("$widthPixels key left", expectedLeft, firstKey.left)
            assertEquals("$widthPixels candidate to key gap", 10, keyboard.top + firstKey.top - (strip.top + candidate.bottomIn(strip)))
        }

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
        assertEquals(248, root.keyboardView().measuredHeight)
        assertEquals(298, root.measuredHeight)
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
    fun voicePanelStartsHiddenOutsideVoiceLayerAndIsRecreatedWithTheInputView() {
        val controller = Robolectric.buildService(ImeService::class.java).create()
        val service = controller.get()
        val firstView = service.onCreateInputView()
        assertEquals(View.GONE, firstView.voicePanelView().visibility)
        assertEquals(View.VISIBLE, firstView.candidateStripView().visibility)

        service.onFinishInputView(false)
        val reopenedView = service.onCreateInputView()
        assertFalse(firstView.voicePanelView() === reopenedView.voicePanelView())
        assertEquals(View.GONE, reopenedView.voicePanelView().visibility)
        controller.destroy()
    }

    @Test
    fun closingVoiceLayerRestoresItsPreviousModeAndClearsCandidates() {
        val controller = Robolectric.buildService(ImeService::class.java).create()
        val service = controller.get()
        service.onStartInput(EditorInfo(), false)
        val root = service.onCreateInputView()
        val keyboard = root.keyboardView()
        val panel = root.voicePanelView()

        service.setModeForLifecycleTest(KeyboardMode.VOICE, returnMode = KeyboardMode.KANA)
        keyboard.setMode(KeyboardMode.VOICE)
        assertEquals(KeyboardMode.VOICE, keyboard.mode())
        panel.showCandidates(CandidateUiSnapshot(99, listOf("古い音声候補")))
        panel.setVoiceState(VoiceUiSnapshot(99, VoiceUiState.Preview("古い音声候補")))

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

    private fun View.containsText(value: String): Boolean {
        if (this is android.widget.TextView && text.toString() == value) return true
        val group = this as? ViewGroup ?: return false
        return (0 until group.childCount).any { group.getChildAt(it).containsText(value) }
    }

    private fun View.allTextViews(): List<android.widget.TextView> {
        val result = mutableListOf<android.widget.TextView>()
        fun collect(current: View) {
            if (current is android.widget.TextView) result += current
            if (current is ViewGroup) repeat(current.childCount) { collect(current.getChildAt(it)) }
        }
        collect(this)
        return result
    }

    private fun View.leftIn(ancestor: View): Int {
        var current = this
        var result = current.left
        while (current.parent is View && current.parent !== ancestor) {
            current = current.parent as View
            result += current.left
        }
        return result
    }

    private fun View.bottomIn(ancestor: View): Int {
        var current = this
        var result = current.bottom
        while (current.parent is View && current.parent !== ancestor) {
            current = current.parent as View
            result += current.top
        }
        return result
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

    private fun assertLargeKanaGeometry(keyboard: KeyboardView, widthPixels: Int) {
        val state = KeyboardView::class.java.getDeclaredField("state").apply { isAccessible = true }
            .get(keyboard) as KeyboardUiState
        assertEquals("$widthPixels mode", KeyboardMode.KANA, state.mode)
        assertTrue("$widthPixels Dual Flick preference", state.dualFlickEnabled)
        assertEquals("$widthPixels preset", KeyboardHeightPreset.LARGE, state.heightPreset)
        assertEquals(
            "$widthPixels four large rows",
            (KeyboardHeightPreset.LARGE.rowPitchDp * 4 + 8).toInt() + keyboard.paddingBottom,
            keyboard.measuredHeight,
        )
    }

    private fun View.voicePanelView(): VoicePanelView {
        if (this is VoicePanelView) return this
        val group = this as? ViewGroup ?: error("VoicePanelView not found")
        return (0 until group.childCount)
            .asSequence()
            .mapNotNull { runCatching { group.getChildAt(it).voicePanelView() }.getOrNull() }
            .first()
    }

    private fun KeyboardView.mode(): KeyboardMode {
        val field = KeyboardView::class.java.getDeclaredField("state").apply { isAccessible = true }
        return (field.get(this) as KeyboardUiState).mode
    }
}
