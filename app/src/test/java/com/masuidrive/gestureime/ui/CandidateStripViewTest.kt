package com.masuidrive.gestureime.ui

import android.graphics.Color
import android.app.Activity
import android.graphics.Typeface
import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.HorizontalScrollView
import android.os.Looper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Robolectric
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
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

    @Test fun partialCandidateUsesTheCandidateFaceButCannotBeSelected() {
        val view = view()
        val events = mutableListOf<CandidateUiEvent>()
        view.setOnCandidateSelected(events::add)
        view.showCandidates(CandidateUiSnapshot(41, listOf("認識の途中"), selectable = false))

        val partial = view.textView("認識の途中")
        assertFalse(partial.isEnabled)
        assertFalse(partial.isClickable)
        assertFalse(partial.isFocusable)
        assertFalse(partial.performClick())
        assertEquals(emptyList<CandidateUiEvent>(), events)

        view.showCandidates(CandidateUiSnapshot(42, listOf("最終候補")))
        val final = view.textView("最終候補")
        assertTrue(final.isEnabled)
        assertTrue(final.isClickable)
        assertTrue(final.performClick())
        assertEquals(listOf(CandidateUiEvent(42, 0)), events)
    }

    @Test fun voiceCandidatesWrapAtTwoLinesWithinTheVisibleStripWhileNormalCandidatesStaySingleLine() {
        val view = view()
        view.measure(
            View.MeasureSpec.makeMeasureSpec(180, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(50, View.MeasureSpec.EXACTLY),
        )
        view.layout(0, 0, 180, 50)

        view.showCandidates(CandidateUiSnapshot(
            43,
            listOf("これは候補欄より十分に長い音声認識結果なので二行で表示されます"),
            presentation = CandidatePresentation.VOICE,
        ))
        shadowOf(Looper.getMainLooper()).idle()
        val voice = view.textView("これは候補欄より十分に長い音声認識結果なので二行で表示されます")
        assertEquals(2, voice.maxLines)
        assertEquals(android.text.TextUtils.TruncateAt.END, voice.ellipsize)
        assertTrue(voice.maxWidth <= 174)
        assertEquals(50, view.measuredHeight)

        view.showCandidates(CandidateUiSnapshot(44, listOf("通常候補は従来どおり一行表示")))
        val normal = view.textView("通常候補は従来どおり一行表示")
        assertEquals(1, normal.maxLines)
        assertEquals(null, normal.ellipsize)
        assertEquals(50, view.measuredHeight)
    }

    @Test fun recordingAndRecognizingKeepTheCandidateAreaEmptyWithoutLegacyControls() {
        val view = view()
        view.setVoiceState(VoiceUiSnapshot(2, VoiceUiState.Recording))
        assertTrue(view.allTextViews().isEmpty())
        view.setVoiceState(VoiceUiSnapshot(3, VoiceUiState.Recognizing))
        assertTrue(view.allTextViews().isEmpty())
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

    @Test fun partialResultUsesCandidateFaceWithoutLegacyCancelControl() {
        val view = view()
        view.setVoiceState(VoiceUiSnapshot(12, VoiceUiState.Partial("認識途中の文章")))
        view.measure(View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(50, View.MeasureSpec.EXACTLY))
        view.layout(0, 0, 400, 50)

        val partial = view.textView("認識途中の文章")
        assertEquals(34, partial.height)
        assertEquals(7f, partial.faceLayer().cornerRadius, .1f)
        assertEquals("認識途中: 認識途中の文章", partial.contentDescription)
        assertTrue(view.allTextViews().none { it.text == "取消" })
    }

    @Test fun permissionAndUnavailableControlsShareCandidateGeometryAndThemeColors() {
        val view = view()
        listOf(
            VoiceUiState.PermissionRequired to "許可",
            VoiceUiState.Unavailable("利用不可") to "非対応",
        ).forEach { (state, text) ->
            view.setVoiceState(VoiceUiSnapshot(13, state))
            view.measure(View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(50, View.MeasureSpec.EXACTLY))
            view.layout(0, 0, 400, 50)
            val control = view.textView(text)
            assertEquals(82, control.width)
            assertEquals(34, control.height)
            assertEquals(7f, control.faceLayer().cornerRadius, .1f)
            assertEquals(Color.WHITE, control.faceColor())
            assertEquals(Color.rgb(137, 140, 148), control.shadowLayer().color!!.defaultColor)
            assertEquals(Color.rgb(25, 25, 27), control.currentTextColor)
            assertTrue(control.isEnabled)
            assertTrue(control.isClickable)
            assertTrue(control.isFocusable)
        }
    }

    @Test @Config(qualifiers = "night") fun permissionAndUnavailableUseDarkNormalCandidateFace() {
        val view = view()
        listOf(
            VoiceUiState.PermissionRequired to "許可",
            VoiceUiState.Unavailable("利用不可") to "非対応",
        ).forEach { (state, text) ->
            view.setVoiceState(VoiceUiSnapshot(14, state))
            val control = view.textView(text)
            assertEquals(Color.rgb(65, 65, 68), control.faceColor())
            assertEquals(Color.rgb(20, 20, 22), control.shadowLayer().color!!.defaultColor)
            assertEquals(Color.rgb(244, 244, 246), control.currentTextColor)
            assertTrue(control.isClickable)
            assertTrue(control.isFocusable)
        }
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
        assertEquals(6, view.paddingLeft)
        assertEquals(14, view.paddingTop)
        assertEquals(6, view.paddingRight)
        assertEquals(2, view.paddingBottom)
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

    @Test fun candidatePresentationsUseTheKeyFaceInsetsAtPhoneAndWideWidths() {
        val view = view()
        listOf(400 to 6, 840 to 13).forEach { (width, expectedInset) ->
            fun layout() {
                view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(50, View.MeasureSpec.EXACTLY))
                view.layout(0, 0, width, 50)
            }
            fun assertFace(text: String) {
                val face = view.textView(text)
                assertEquals("$width $text left", expectedInset, face.leftIn(view))
                assertEquals("$width $text top", 14, face.topIn(view))
                assertEquals("$width $text bottom", 48, face.bottomIn(view))
                assertEquals("$width $text height", 34, face.height)
            }

            view.setVoiceState(VoiceUiSnapshot(50, VoiceUiState.Hidden))
            view.showCandidates(CandidateUiSnapshot(50, listOf("通常", "次")))
            layout()
            assertFace("通常")
            assertEquals(5, view.textView("次").leftIn(view) - view.textView("通常").rightIn(view))

            view.showCandidates(CandidateUiSnapshot(51, listOf("音声"), presentation = CandidatePresentation.VOICE))
            layout()
            assertFace("音声")

            view.showStatus("状態")
            layout()
            assertFace("状態")

            view.setVoiceState(VoiceUiSnapshot(52, VoiceUiState.PermissionRequired))
            layout()
            val control = view.textView("許可")
            assertEquals("$width control right", width - expectedInset, control.rightIn(view))
            assertEquals("$width control top", 14, control.topIn(view))
            assertEquals("$width control bottom", 48, control.bottomIn(view))
            assertEquals("$width control height", 34, control.height)
        }
    }

    @Test fun candidateTextKeepsHtmlFixedSizeAtLargeSystemFontScale() {
        val base = RuntimeEnvironment.getApplication()
        val configuration = Configuration(base.resources.configuration).apply { fontScale = 2f }
        val view = CandidateStripView(base.createConfigurationContext(configuration))
        view.showCandidates(CandidateUiSnapshot(24, listOf("candidate")))

        assertEquals(15f * view.resources.displayMetrics.density, view.textView("candidate").textSize, .1f)
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

    @Test fun longPressUsesRenderedTokenAndOnlyConsumesWhenTheServiceAcceptsIt() {
        val view = view()
        Robolectric.buildActivity(Activity::class.java).setup().get().setContentView(view)
        val events = mutableListOf<CandidateUiLongPressEvent>()
        view.setOnCandidateLongPressed { event -> events += event; event.index == 0 }
        view.showCandidates(CandidateUiSnapshot(32, listOf("履歴", "個人辞書")))
        val oldHistory = view.textView("履歴")

        assertTrue(oldHistory.performLongClick())
        assertEquals(listOf(CandidateUiLongPressEvent(32, 0)), events)
        assertTrue(!view.textView("個人辞書").performLongClick())
        assertEquals(listOf(CandidateUiLongPressEvent(32, 0), CandidateUiLongPressEvent(32, 1)), events)

        view.showCandidates(CandidateUiSnapshot(33, listOf("履歴")))
        assertTrue(oldHistory.performLongClick())
        assertEquals(CandidateUiLongPressEvent(32, 0), events.last())
    }

    @Test fun candidateContentChangeResetsScrollButSelectionOnlyChangeKeepsIt() {
        val view = view()
        val candidates = listOf("alpha", "bravo", "charlie", "delta", "echo")
        fun layout() {
            view.measure(
                View.MeasureSpec.makeMeasureSpec(180, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(50, View.MeasureSpec.EXACTLY),
            )
            view.layout(0, 0, 180, 50)
        }

        view.showCandidates(CandidateUiSnapshot(40, candidates, 0))
        layout()
        shadowOf(Looper.getMainLooper()).idle()
        val scroll = view.getChildAt(0) as HorizontalScrollView
        scroll.scrollTo(140, 0)
        assertEquals(140, scroll.scrollX)

        view.showCandidates(CandidateUiSnapshot(41, candidates, 3))
        layout()
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(140, scroll.scrollX)

        view.showCandidates(CandidateUiSnapshot(42, listOf("foxtrot", "golf", "hotel", "india"), 0))
        layout()
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(0, scroll.scrollX)
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
    private fun View.leftIn(ancestor: View): Int = horizontalIn(ancestor) { left }
    private fun View.rightIn(ancestor: View): Int = horizontalIn(ancestor) { right }
    private fun View.topIn(ancestor: View): Int = verticalIn(ancestor) { top }
    private fun View.bottomIn(ancestor: View): Int = verticalIn(ancestor) { bottom }
    private fun View.horizontalIn(ancestor: View, edge: View.() -> Int): Int {
        var current = this
        var result = edge(current)
        while (current.parent is View && current.parent !== ancestor) {
            current = current.parent as View
            result += current.left
        }
        return result
    }
    private fun View.verticalIn(ancestor: View, edge: View.() -> Int): Int {
        var current = this
        var result = edge(current)
        while (current.parent is View && current.parent !== ancestor) {
            current = current.parent as View
            result += current.top
        }
        return result
    }
}
