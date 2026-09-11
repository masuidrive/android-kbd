package com.masuidrive.gestureime.ui

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CandidateStripViewTest {
    private fun view() = CandidateStripView(RuntimeEnvironment.getApplication())

    @Test fun idleCoexistsWithCandidatesAndDispatchesActions() {
        val view = view(); val candidates = mutableListOf<Int>(); val actions = mutableListOf<VoiceUiEvent>()
        view.setOnCandidateSelected(candidates::add); view.setOnVoiceActionListener(actions::add)
        view.showCandidates(listOf("日本語", "日本語の"), 0); view.setVoiceState(VoiceUiSnapshot(1, VoiceUiState.Idle))
        view.textView("日本語の").performClick(); view.textView("音声").performClick()
        assertEquals(listOf(1), candidates); assertEquals(listOf(VoiceUiEvent(1, VoiceUiAction.Start)), actions)
        assertEquals("音声入力を開始", view.textView("音声").contentDescription)
    }

    @Test fun recordingRecognizingAndPermissionExposeOnlyValidAction() {
        val view = view(); val actions = mutableListOf<VoiceUiEvent>(); view.setOnVoiceActionListener(actions::add)
        view.setVoiceState(VoiceUiSnapshot(2, VoiceUiState.Recording)); view.textView("停止").performClick()
        view.textView("取消").performClick()
        view.setVoiceState(VoiceUiSnapshot(3, VoiceUiState.Recognizing)); assertFalse(view.textView("処理中").isEnabled)
        view.textView("取消").performClick()
        view.setVoiceState(VoiceUiSnapshot(4, VoiceUiState.PermissionRequired)); view.textView("許可").performClick()
        assertEquals(listOf(VoiceUiEvent(2, VoiceUiAction.Stop), VoiceUiEvent(2, VoiceUiAction.Cancel), VoiceUiEvent(3, VoiceUiAction.Cancel), VoiceUiEvent(4, VoiceUiAction.RequestPermission)), actions)
    }

    @Test fun previewKeepsFullTextForScrollingAndAccessibilityUntilChoice() {
        val view = view(); val actions = mutableListOf<VoiceUiEvent>(); val transcript = "これは横幅より長い日本語の音声認識結果です"
        view.setOnVoiceActionListener(actions::add); view.setVoiceState(VoiceUiSnapshot(5, VoiceUiState.Preview(transcript)))
        val preview = view.textView(transcript)
        assertEquals(transcript, preview.text.toString()); assertEquals("認識結果: $transcript", preview.contentDescription)
        assertEquals(1, preview.maxLines)
        view.textView("確定").performClick(); view.textView("取消").performClick()
        assertEquals(listOf(VoiceUiEvent(5, VoiceUiAction.Confirm), VoiceUiEvent(5, VoiceUiAction.Cancel)), actions)
    }

    @Test fun unavailableExplainsReasonAndPreservesCandidateInput() {
        val view = view(); val actions = mutableListOf<VoiceUiEvent>(); val selected = mutableListOf<Int>()
        view.setOnVoiceActionListener(actions::add); view.setOnCandidateSelected(selected::add)
        view.showCandidates(listOf("候補"), 0)
        view.setVoiceState(VoiceUiSnapshot(6, VoiceUiState.Unavailable("端末内の日本語モデルがありません")))
        val button = view.textView("非対応")
        assertTrue(button.isEnabled); assertTrue(button.contentDescription.toString().contains("日本語モデル"))
        view.textView("候補").performClick(); button.performClick()
        assertEquals(listOf(0), selected); assertEquals(listOf(VoiceUiEvent(6, VoiceUiAction.ExplainUnavailable)), actions)
    }

    @Test fun permissionRequiredPreservesCandidateInput() {
        val view = view(); val selected = mutableListOf<Int>(); view.setOnCandidateSelected(selected::add)
        view.showCandidates(listOf("日本語"), 0); view.setVoiceState(VoiceUiSnapshot(7, VoiceUiState.PermissionRequired))
        view.textView("日本語").performClick()
        assertEquals(listOf(0), selected); assertTrue(view.textView("許可").isEnabled)
    }

    @Test fun hiddenRemovesVoiceControlsButPreservesCandidateInput() {
        val view = view(); val selected = mutableListOf<Int>(); view.setOnCandidateSelected(selected::add)
        view.showCandidates(listOf("候補"), 0); view.setVoiceState(VoiceUiSnapshot(8, VoiceUiState.Hidden))
        assertTrue(view.allTextViews().none { it.text == "音声" }); view.textView("候補").performClick()
        assertEquals(listOf(0), selected)
    }

    @Test fun detachedPreviewButtonKeepsItsRenderedSessionToken() {
        val view = view(); val events = mutableListOf<VoiceUiEvent>(); view.setOnVoiceActionListener(events::add)
        view.setVoiceState(VoiceUiSnapshot(10, VoiceUiState.Preview("古い結果")))
        val oldConfirm = view.textView("確定")

        view.setVoiceState(VoiceUiSnapshot(11, VoiceUiState.Preview("新しい結果")))
        oldConfirm.performClick()
        view.textView("確定").performClick()

        assertEquals(listOf(VoiceUiEvent(10, VoiceUiAction.Confirm), VoiceUiEvent(11, VoiceUiAction.Confirm)), events)
    }

    @Test fun bottomGapUsesTheKeyboardBackgroundColor() {
        val view = view()
        assertEquals(Color.rgb(41, 41, 44), (view.background as ColorDrawable).color)
        assertEquals(8, view.paddingBottom)
    }

    private fun CandidateStripView.textView(text: String): TextView = allTextViews().single { it.text.toString() == text }
    private fun View.allTextViews(): List<TextView> {
        val result = mutableListOf<TextView>()
        fun visit(view: View) { if (view is TextView) result += view; if (view is ViewGroup) repeat(view.childCount) { visit(view.getChildAt(it)) } }
        visit(this); return result
    }
}
