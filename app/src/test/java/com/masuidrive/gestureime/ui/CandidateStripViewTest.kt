package com.masuidrive.gestureime.ui

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
        val view = view(); val candidates = mutableListOf<Int>(); val actions = mutableListOf<VoiceUiAction>()
        view.setOnCandidateSelected(candidates::add); view.setOnVoiceActionListener(actions::add)
        view.showCandidates(listOf("日本語", "日本語の"), 0); view.setVoiceState(VoiceUiState.Idle)
        view.textView("日本語の").performClick(); view.textView("音声").performClick()
        assertEquals(listOf(1), candidates); assertEquals(listOf(VoiceUiAction.Start), actions)
        assertEquals("音声入力を開始", view.textView("音声").contentDescription)
    }

    @Test fun recordingRecognizingAndPermissionExposeOnlyValidAction() {
        val view = view(); val actions = mutableListOf<VoiceUiAction>(); view.setOnVoiceActionListener(actions::add)
        view.setVoiceState(VoiceUiState.Recording); view.textView("停止").performClick()
        view.setVoiceState(VoiceUiState.Recognizing); assertFalse(view.textView("処理中").isEnabled)
        view.setVoiceState(VoiceUiState.PermissionRequired); view.textView("許可").performClick()
        assertEquals(listOf(VoiceUiAction.Stop, VoiceUiAction.RequestPermission), actions)
    }

    @Test fun previewKeepsFullTextForScrollingAndAccessibilityUntilChoice() {
        val view = view(); val actions = mutableListOf<VoiceUiAction>(); val transcript = "これは横幅より長い日本語の音声認識結果です"
        view.setOnVoiceActionListener(actions::add); view.setVoiceState(VoiceUiState.Preview(transcript))
        val preview = view.textView(transcript)
        assertEquals(transcript, preview.text.toString()); assertEquals("認識結果: $transcript", preview.contentDescription)
        assertEquals(1, preview.maxLines)
        view.textView("確定").performClick(); view.textView("取消").performClick()
        assertEquals(listOf(VoiceUiAction.Confirm, VoiceUiAction.Cancel), actions)
    }

    @Test fun unavailableExplainsReasonAndDoesNotDispatchStart() {
        val view = view(); val actions = mutableListOf<VoiceUiAction>(); view.setOnVoiceActionListener(actions::add)
        view.setVoiceState(VoiceUiState.Unavailable("端末内の日本語モデルがありません"))
        val button = view.textView("音声")
        assertFalse(button.isEnabled); assertTrue(button.contentDescription.toString().contains("日本語モデル"))
        button.performClick(); assertTrue(actions.isEmpty())
    }

    @Test fun hiddenRemovesVoiceControlsButPreservesCandidateInput() {
        val view = view(); val selected = mutableListOf<Int>(); view.setOnCandidateSelected(selected::add)
        view.showCandidates(listOf("候補"), 0); view.setVoiceState(VoiceUiState.Hidden)
        assertTrue(view.allTextViews().none { it.text == "音声" }); view.textView("候補").performClick()
        assertEquals(listOf(0), selected)
    }

    private fun CandidateStripView.textView(text: String): TextView = allTextViews().single { it.text.toString() == text }
    private fun View.allTextViews(): List<TextView> {
        val result = mutableListOf<TextView>()
        fun visit(view: View) { if (view is TextView) result += view; if (view is ViewGroup) repeat(view.childCount) { visit(view.getChildAt(it)) } }
        visit(this); return result
    }
}
