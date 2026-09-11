package com.masuidrive.gestureime.ui

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.junit.Assert.assertEquals
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

    @Test fun idleShowsCandidatesWithoutAnOldVoiceStartControl() {
        val view = view(); val candidates = mutableListOf<CandidateUiEvent>(); val actions = mutableListOf<VoiceUiEvent>()
        view.setOnCandidateSelected(candidates::add); view.setOnVoiceActionListener(actions::add)
        view.showCandidates(CandidateUiSnapshot(20, listOf("日本語", "日本語の"), 0)); view.setVoiceState(VoiceUiSnapshot(1, VoiceUiState.Idle))
        view.textView("日本語の").performClick()
        assertEquals(listOf(CandidateUiEvent(20, 1)), candidates); assertTrue(actions.isEmpty())
        assertTrue(view.allTextViews().none { it.text == "音声" })
    }

    @Test fun recordingAndRecognizingOfferCancelWithoutStopOrConfirmControls() {
        val view = view(); val actions = mutableListOf<VoiceUiEvent>(); view.setOnVoiceActionListener(actions::add)
        view.setVoiceState(VoiceUiSnapshot(2, VoiceUiState.Recording))
        assertTrue(view.allTextViews().none { it.text == "停止" }); view.textView("取消").performClick()
        view.setVoiceState(VoiceUiSnapshot(3, VoiceUiState.Recognizing))
        assertTrue(view.allTextViews().none { it.text == "停止" || it.text == "確定" }); view.textView("取消").performClick()
        assertEquals(listOf(VoiceUiEvent(2, VoiceUiAction.Cancel), VoiceUiEvent(3, VoiceUiAction.Cancel)), actions)
    }

    @Test fun permissionStillOffersItsRequiredSetupAction() {
        val view = view(); val actions = mutableListOf<VoiceUiEvent>(); view.setOnVoiceActionListener(actions::add)
        view.setVoiceState(VoiceUiSnapshot(4, VoiceUiState.PermissionRequired)); view.textView("許可").performClick()
        assertEquals(listOf(VoiceUiEvent(4, VoiceUiAction.RequestPermission)), actions)
    }

    @Test fun backendPreviewHasNoManualConfirmationControls() {
        val view = view(); view.setVoiceState(VoiceUiSnapshot(5, VoiceUiState.Preview("自動入力される結果")))
        assertEquals("音声を認識しました", view.textView("音声を認識しました").text.toString())
        assertTrue(view.allTextViews().none { it.text == "確定" || it.text == "取消" })
    }

    @Test fun unavailableExplainsReasonAndPreservesCandidateInput() {
        val view = view(); val actions = mutableListOf<VoiceUiEvent>(); val selected = mutableListOf<CandidateUiEvent>()
        view.setOnVoiceActionListener(actions::add); view.setOnCandidateSelected(selected::add)
        view.showCandidates(CandidateUiSnapshot(21, listOf("候補"), 0))
        view.setVoiceState(VoiceUiSnapshot(6, VoiceUiState.Unavailable("端末内の日本語モデルがありません")))
        val button = view.textView("非対応")
        assertTrue(button.isEnabled); assertTrue(button.contentDescription.toString().contains("日本語モデル"))
        view.textView("候補").performClick(); button.performClick()
        assertEquals(listOf(CandidateUiEvent(21, 0)), selected); assertEquals(listOf(VoiceUiEvent(6, VoiceUiAction.ExplainUnavailable)), actions)
    }

    @Test fun permissionRequiredPreservesCandidateInput() {
        val view = view(); val selected = mutableListOf<CandidateUiEvent>(); view.setOnCandidateSelected(selected::add)
        view.showCandidates(CandidateUiSnapshot(22, listOf("日本語"), 0)); view.setVoiceState(VoiceUiSnapshot(7, VoiceUiState.PermissionRequired))
        view.textView("日本語").performClick()
        assertEquals(listOf(CandidateUiEvent(22, 0)), selected); assertTrue(view.textView("許可").isEnabled)
    }

    @Test fun hiddenRemovesVoiceControlsButPreservesCandidateInput() {
        val view = view(); val selected = mutableListOf<CandidateUiEvent>(); view.setOnCandidateSelected(selected::add)
        view.showCandidates(CandidateUiSnapshot(23, listOf("候補"), 0)); view.setVoiceState(VoiceUiSnapshot(8, VoiceUiState.Hidden))
        assertTrue(view.allTextViews().none { it.text == "音声" }); view.textView("候補").performClick()
        assertEquals(listOf(CandidateUiEvent(23, 0)), selected)
    }

    @Test fun detachedPermissionButtonKeepsItsRenderedSessionToken() {
        val view = view(); val events = mutableListOf<VoiceUiEvent>(); view.setOnVoiceActionListener(events::add)
        view.setVoiceState(VoiceUiSnapshot(10, VoiceUiState.PermissionRequired))
        val oldPermission = view.textView("許可")

        view.setVoiceState(VoiceUiSnapshot(11, VoiceUiState.PermissionRequired))
        oldPermission.performClick()
        view.textView("許可").performClick()

        assertEquals(listOf(VoiceUiEvent(10, VoiceUiAction.RequestPermission), VoiceUiEvent(11, VoiceUiAction.RequestPermission)), events)
    }

    @Test fun bottomGapUsesTheKeyboardBackgroundColor() {
        val view = view()
        assertEquals(Color.rgb(211, 213, 219), (view.background as ColorDrawable).color)
        assertEquals(3, view.paddingLeft)
        assertEquals(8, view.paddingTop)
        assertEquals(3, view.paddingRight)
        assertEquals(8, view.paddingBottom)
    }

    @Test fun candidateGeometryMatchesHtmlReference() {
        val view = view()
        view.showCandidates(CandidateUiSnapshot(24, listOf("one", "two"), 1))
        view.measure(View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(50, View.MeasureSpec.EXACTLY))
        view.layout(0, 0, 400, 50)
        val first = view.textView("one")
        val second = view.textView("two")

        assertEquals(82, first.width)
        assertEquals(34, first.height)
        assertEquals(14, first.paddingLeft)
        assertEquals(14, first.paddingRight)
        assertEquals(5, (second.layoutParams as android.widget.LinearLayout.LayoutParams).marginStart)
        assertEquals(15f, first.textSize, .1f)
        assertEquals(Typeface.NORMAL, second.typeface.style)
        assertEquals(7f, first.faceLayer().cornerRadius, .1f)
        assertEquals(Color.rgb(137, 140, 148), first.shadowLayer().color!!.defaultColor)
    }

    @Test fun showingCandidatesKeepsTheStripBackgroundStable() {
        val view = view()
        view.showCandidates(CandidateUiSnapshot(24, listOf("未選択", "選択"), 1))

        assertEquals(Color.WHITE, view.textView("未選択").faceColor())
        assertEquals(Color.rgb(23, 78, 166), view.textView("選択").faceColor())
        assertEquals(Color.rgb(211, 213, 219), (view.background as ColorDrawable).color)
    }

    @Test @Config(qualifiers = "night") fun darkThemeKeepsExistingCandidateColors() {
        val view = view()
        view.showCandidates(CandidateUiSnapshot(25, listOf("未選択", "選択"), 1))

        assertEquals(Color.rgb(65, 65, 68), view.textView("未選択").faceColor())
        assertEquals(Color.rgb(168, 206, 255), view.textView("選択").faceColor())
        assertEquals(Color.rgb(41, 41, 44), (view.background as ColorDrawable).color)
    }

    @Test fun configurationChangeRepaintsCandidatesAndKeepsTheirRenderedToken() {
        RuntimeEnvironment.setQualifiers("notnight")
        val view = view()
        val events = mutableListOf<CandidateUiEvent>()
        view.setOnCandidateSelected(events::add)
        view.showCandidates(CandidateUiSnapshot(26, listOf("候補"), 0))
        assertEquals(Color.rgb(23, 78, 166), view.textView("候補").faceColor())

        RuntimeEnvironment.setQualifiers("night")
        view.dispatchConfigurationChanged(view.resources.configuration)
        view.textView("候補").performClick()

        assertEquals(Color.rgb(168, 206, 255), view.textView("候補").faceColor())
        assertEquals(listOf(CandidateUiEvent(26, 0)), events)
    }

    @Test fun detachedCandidateKeepsItsRenderedToken() {
        val view = view(); val events = mutableListOf<CandidateUiEvent>(); view.setOnCandidateSelected(events::add)
        view.showCandidates(CandidateUiSnapshot(30, listOf("same")))
        val oldCandidate = view.textView("same")

        view.showCandidates(CandidateUiSnapshot(31, listOf("same")))
        oldCandidate.performClick()
        view.textView("same").performClick()

        assertEquals(listOf(CandidateUiEvent(30, 0), CandidateUiEvent(31, 0)), events)
    }

    private fun CandidateStripView.textView(text: String): TextView = allTextViews().single { it.text.toString() == text }
    private fun TextView.layers() = background as LayerDrawable
    private fun TextView.shadowLayer() = layers().getDrawable(0) as GradientDrawable
    private fun TextView.faceLayer() = layers().getDrawable(1) as GradientDrawable
    private fun TextView.faceColor() = faceLayer().color!!.defaultColor
    private fun View.allTextViews(): List<TextView> {
        val result = mutableListOf<TextView>()
        fun visit(view: View) { if (view is TextView) result += view; if (view is ViewGroup) repeat(view.childCount) { visit(view.getChildAt(it)) } }
        visit(this); return result
    }
}
