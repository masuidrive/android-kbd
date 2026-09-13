package com.masuidrive.gestureime

import android.graphics.Rect
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
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
        assertEquals(228, keyboard.measuredHeight)
        assertEquals(278, root.measuredHeight)
        controller.destroy()
    }

    @Test
    fun persistedHeightPresetIsAppliedWhenTheInputViewStartsAndReopens() {
        val controller = Robolectric.buildService(ImeService::class.java).create()
        val service = controller.get()
        ImePreferences.setKeyboardHeightPreset(service, KeyboardHeightPreset.LARGE)
        val root = service.onCreateInputView() as ViewGroup
        val width = View.MeasureSpec.makeMeasureSpec(840, View.MeasureSpec.EXACTLY)
        val height = View.MeasureSpec.makeMeasureSpec(1_000, View.MeasureSpec.AT_MOST)

        root.measure(width, height)
        assertEquals(256, root.keyboardView().measuredHeight)

        ImePreferences.setKeyboardHeightPreset(service, KeyboardHeightPreset.SMALL)
        service.onStartInputView(EditorInfo(), true)
        root.measure(width, height)
        assertEquals(208, root.keyboardView().measuredHeight)

        ImePreferences.setKeyboardHeightPreset(service, KeyboardHeightPreset.STANDARD)
        controller.destroy()
    }

    @Test
    fun explicitlySavedLargePresetSurvivesAStaleStandardHeightAcrossInputViewRecreation() {
        val controller = Robolectric.buildService(RelayoutRecordingImeService::class.java).create()
        val service = controller.get()
        val preferences = service.getSharedPreferences("gesture_ime_preferences", 0)
        preferences.edit().clear().commit()
        ImePreferences.setKeyboardHeightPreset(service, KeyboardHeightPreset.LARGE)
        ImePreferences.setLastKeyboardMode(service, KeyboardMode.KANA)
        ImePreferences.setDualFlickEnabled(service, true)
        try {
            listOf(412, 840).forEach { widthPixels ->
                val root = service.onCreateInputView() as ViewGroup
                val host = imeHost(service, root)
                val candidate = root.candidateStripView()
                // AOSP's mInputFrame is exact, but our returned root is WRAP_CONTENT inside it,
                // so it receives the previous IME frame as an AT_MOST child constraint.
                measureAndLayout(host, widthPixels, View.MeasureSpec.EXACTLY, 1_000)
                assertLargePresetMeasurement(service, root.keyboardView(), widthPixels)

                // A host can reuse the previous Standard-sized IME frame on hide/show or an
                // editor/app transition. First constrain the candidate-empty state, then add
                // candidates and measure again: neither order may shrink a saved Large preset.
                val staleHeight = staleStandardRootHeight(service, root.keyboardView())
                service.relayoutRequests = 0
                measureAndLayout(host, widthPixels, View.MeasureSpec.EXACTLY, staleHeight)
                assertLargePresetMeasurement(service, root.keyboardView(), widthPixels)
                assertEquals("$widthPixels stale root height", expectedLargeRootHeight(service, root.keyboardView()), root.measuredHeight)
                assertEquals("$widthPixels stale host requests a WRAP_CONTENT window relayout", 1, service.relayoutRequests)
                assertTrue("$widthPixels stale host clips the expanded root before WindowManager remeasures", root.bottom > host.height)
                remeasureHostToIntrinsicHeight(host, root, widthPixels)
                candidate.showCandidates(CandidateUiSnapshot(1L, listOf("候補", "変換候補")))
                measureAndLayout(host, widthPixels, View.MeasureSpec.EXACTLY, staleHeight)
                assertLargePresetMeasurement(service, root.keyboardView(), widthPixels)
                assertEquals("$widthPixels candidate root height", expectedLargeRootHeight(service, root.keyboardView()), root.measuredHeight)
                remeasureHostToIntrinsicHeight(host, root, widthPixels)

                // This is approximately the 45dp row pitch reported by the short screenshot,
                // rather than merely the preceding Standard 55dp frame. A saved Large value
                // must still restore its own four-row geometry.
                val shortRootHeight = candidateHeight(service) +
                    (45 * service.resources.displayMetrics.density).toInt() * 4 +
                    (8 * service.resources.displayMetrics.density).toInt()
                measureAndLayout(host, widthPixels, View.MeasureSpec.EXACTLY, shortRootHeight)
                assertLargePresetMeasurement(service, root.keyboardView(), widthPixels)
                assertEquals("$widthPixels short root height", expectedLargeRootHeight(service, root.keyboardView()), root.measuredHeight)
                remeasureHostToIntrinsicHeight(host, root, widthPixels)

                service.onFinishInputView(false)
                service.onStartInput(EditorInfo(), true)
                service.onStartInputView(EditorInfo(), true)
                val recreated = service.onCreateInputView() as ViewGroup
                val recreatedHost = imeHost(service, recreated)
                recreated.candidateStripView().showCandidates(CandidateUiSnapshot(2L, listOf("候補")))
                measureAndLayout(recreatedHost, widthPixels, View.MeasureSpec.EXACTLY, staleHeight)
                assertLargePresetMeasurement(service, recreated.keyboardView(), widthPixels)
                assertEquals("$widthPixels recreated root height", expectedLargeRootHeight(service, recreated.keyboardView()), recreated.measuredHeight)
                remeasureHostToIntrinsicHeight(recreatedHost, recreated, widthPixels)
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

    private fun assertLargePresetMeasurement(service: ImeService, keyboard: KeyboardView, widthPixels: Int) {
        val state = KeyboardView::class.java.getDeclaredField("state").apply { isAccessible = true }
            .get(keyboard) as KeyboardUiState
        assertEquals("$widthPixels saved preference", KeyboardHeightPreset.LARGE, ImePreferences.getKeyboardHeightPreset(service))
        assertEquals("$widthPixels state", KeyboardHeightPreset.LARGE, state.heightPreset)
        assertEquals("$widthPixels mode", KeyboardMode.KANA, state.mode)
        assertTrue("$widthPixels Dual Flick preference", state.dualFlickEnabled)
        assertTrue("$widthPixels density", service.resources.displayMetrics.density > 0f)
        assertEquals(
            "$widthPixels measured large rows",
            expectedLargeKeyboardHeight(service, keyboard),
            keyboard.measuredHeight,
        )
    }

    private fun staleStandardRootHeight(service: ImeService, keyboard: KeyboardView): Int =
        candidateHeight(service) + keyboardHeight(service, KeyboardHeightPreset.STANDARD, keyboard)

    private fun expectedLargeRootHeight(service: ImeService, keyboard: KeyboardView): Int =
        candidateHeight(service) + expectedLargeKeyboardHeight(service, keyboard)

    private fun expectedLargeKeyboardHeight(service: ImeService, keyboard: KeyboardView): Int =
        keyboardHeight(service, KeyboardHeightPreset.LARGE, keyboard)

    private fun keyboardHeight(service: ImeService, preset: KeyboardHeightPreset, keyboard: KeyboardView): Int {
        val density = service.resources.displayMetrics.density
        val wideLargePitch = if (
            preset == KeyboardHeightPreset.LARGE &&
            keyboard.width / density >= KeyboardView.DUAL_FLICK_MIN_WIDTH_DP
        ) 62f else preset.rowPitchDp
        return (wideLargePitch * density).toInt() * 4 + (8 * density).toInt() + keyboard.paddingBottom
    }

    private fun candidateHeight(service: ImeService): Int = (50 * service.resources.displayMetrics.density).toInt()

    private fun imeHost(service: ImeService, root: ViewGroup): FrameLayout = FrameLayout(service).apply {
        addView(root, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ))
    }

    private fun remeasureHostToIntrinsicHeight(host: FrameLayout, root: ViewGroup, width: Int) {
        val expectedHeight = root.measuredHeight
        measureAndLayout(host, width, View.MeasureSpec.EXACTLY, expectedHeight)
        assertEquals("WindowManager remeasures the host to the returned WRAP_CONTENT root", expectedHeight, host.measuredHeight)
        assertEquals("returned root is no longer clipped by the host", expectedHeight, root.bottom)
        val keyboard = root.keyboardView()
        val keyboardNode = requireNotNull(keyboard.accessibilityNodeProvider.createAccessibilityNodeInfo(-1))
        val lastKey = Rect().also {
            requireNotNull(keyboard.accessibilityNodeProvider.createAccessibilityNodeInfo(keyboardNode.childCount - 1))
                .getBoundsInParent(it)
        }
        assertTrue("last key target remains visible in the remeasured IME frame", keyboard.top + lastKey.bottom <= host.height)
    }

    private fun measureAndLayout(root: ViewGroup, width: Int, heightMode: Int, height: Int) {
        root.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, heightMode),
        )
        root.layout(0, 0, root.measuredWidth, root.measuredHeight)
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

class RelayoutRecordingImeService : ImeService() {
    var relayoutRequests = 0

    override fun requestInputWindowRelayout() {
        relayoutRequests++
    }
}
