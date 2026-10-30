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
import android.widget.ImageButton
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

    @Test fun candidatesDispatchTheirRenderedIndex() {
        val view = view(); val candidates = mutableListOf<CandidateUiEvent>()
        view.setOnCandidateSelected(candidates::add)
        view.showCandidates(CandidateUiSnapshot(20, listOf("日本語", "日本語の"), 0))
        view.textView("日本語の").performClick()
        assertEquals(listOf(CandidateUiEvent(20, 1)), candidates)
    }

    @Test fun inlineProviderViewTemporarilyReplacesNormalCandidatesAndLeavesTheirTextUntouched() {
        val view = view()
        val providerView = TextView(view.context).apply { text = "provider-owned" }
        view.showCandidates(CandidateUiSnapshot(20, listOf("normal")))

        view.showInlineSuggestionViews(listOf(providerView))

        assertEquals(providerView, view.getChildAt(1).let { it as HorizontalScrollView }.getChildAt(0).let {
            it as ViewGroup
        }.getChildAt(0))
        assertEquals("provider-owned", providerView.text.toString())
        assertEquals(null, runCatching { view.textView("normal") }.getOrNull())

        view.showCandidates(CandidateUiSnapshot(21, listOf("normal")))

        assertEquals(null, runCatching { view.textView("normal") }.getOrNull())
        val row = (view.getChildAt(1) as HorizontalScrollView).getChildAt(0) as ViewGroup
        assertEquals(providerView, row.getChildAt(0))
        assertEquals(1, row.childCount)
        view.clearInlineSuggestionViews()
        assertEquals("normal", view.textView("normal").text.toString())
    }

    @Test fun savedFieldCandidateStaysBesideAnInlineProviderAcrossUpdates() {
        val view = view()
        val providerView = TextView(view.context).apply { text = "provider-owned-1" }
        val secondProvider = TextView(view.context).apply { text = "provider-owned-2" }
        val email = "long.saved.address@example.com"
        view.showCandidates(CandidateUiSnapshot(20, listOf(email)), alongsideInline = true)
        view.showInlineSuggestionViews(listOf(providerView, secondProvider))
        view.showCandidates(CandidateUiSnapshot(21, listOf(email)), alongsideInline = true)

        val row = (view.getChildAt(1) as HorizontalScrollView).getChildAt(0) as ViewGroup
        val saved = view.textView(email)
        assertEquals(saved, row.getChildAt(0))
        assertEquals(providerView, row.getChildAt(1))
        assertEquals(secondProvider, row.getChildAt(2))
        assertEquals(3, row.childCount)
        assertEquals((160 * view.resources.displayMetrics.density).toInt(), saved.maxWidth)
        assertEquals("候補 1: $email", saved.contentDescription)
    }

    @Test fun inlineProviderViewRetainsItsFrameworkSuppliedSizeWhenItHasNoIntrinsicSize() {
        val view = view()
        val providerView = View(view.context).apply {
            layoutParams = ViewGroup.LayoutParams(315, 33)
        }

        view.showInlineSuggestionViews(listOf(providerView))
        view.measure(
            View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(50, View.MeasureSpec.EXACTLY),
        )
        view.layout(0, 0, 400, 50)

        assertEquals(315, providerView.width)
        assertEquals(33, providerView.height)
    }

    @Test fun personalInfoActionDispatchesWithoutBecomingCandidateText() {
        val view = view()
        var requests = 0
        var dismissals = 0
        view.setOnPersonalInfoRequested { requests++ }
        view.setOnPersonalInfoDismissed { dismissals++ }

        view.showPersonalInfoAction(true)

        view.personalInfoAction().performClick()
        assertEquals(1, requests)
        view.showPersonalInfoAction(true, pickerOpen = true)
        assertEquals("個人情報の選択を戻る", view.personalInfoAction().contentDescription)
        assertEquals(view.personalInfoAction(), view.getChildAt(0))
        view.personalInfoAction().performClick()
        assertEquals(1, dismissals)
        assertEquals(1, requests)
        view.showPersonalInfoAction(true)
        assertEquals("登録した個人情報を選ぶ", view.personalInfoAction().contentDescription)
        view.showPersonalInfoAction(false)
        assertEquals(View.GONE, view.personalInfoAction().visibility)
    }

    @Test fun personalInfoActionStaysOutsideTheScrollableInlineProviderRow() {
        val view = view()
        val providerView = TextView(view.context).apply { text = "provider-owned" }

        view.showPersonalInfoAction(true)
        view.showInlineSuggestionViews(listOf(providerView))

        val row = view.getChildAt(1).let { it as HorizontalScrollView }.getChildAt(0) as ViewGroup
        assertEquals(providerView, row.getChildAt(0))
        assertEquals(1, row.childCount)
        assertEquals("登録した個人情報を選ぶ", view.personalInfoAction().contentDescription)
        assertEquals(view, view.personalInfoAction().parent)
        assertEquals(view.personalInfoAction(), view.getChildAt(0))
    }

    @Test fun personalInfoActionUsesLabelsWithoutChangingItsSelectedValueIndex() {
        val view = view()
        val events = mutableListOf<CandidateUiEvent>()
        view.setOnCandidateSelected(events::add)

        view.showCandidates(CandidateUiSnapshot(
            token = 34,
            candidates = listOf("山田 太郎", "東京都千代田区"),
            labels = listOf("氏名: 山田 太郎", "住所: 東京都千代田区"),
        ))
        view.textView("住所: 東京都千代田区").performClick()

        assertEquals(listOf(CandidateUiEvent(34, 1)), events)
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

    @Test fun bottomGapUsesTheKeyboardBackgroundColor() {
        val view = view()
        assertEquals(Color.rgb(211, 213, 219), (view.background as ColorDrawable).color)
        assertEquals(6, view.paddingLeft)
        assertEquals(10, view.paddingTop)
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
        assertEquals(38, first.height)
        assertEquals(14, first.paddingLeft)
        assertEquals(14, first.paddingRight)
        assertEquals(5, (second.layoutParams as android.widget.LinearLayout.LayoutParams).marginStart)
        assertEquals(15f, first.textSize, .1f)
        assertEquals(Typeface.NORMAL, second.typeface.style)
        assertEquals(7f, first.faceLayer().cornerRadius, .1f)
        assertEquals(Color.rgb(137, 140, 148), first.shadowLayer().color!!.defaultColor)
    }

    @Test fun candidateFacesUseTheKeyFaceInsetsAtPhoneAndWideWidths() {
        val view = view()
        listOf(400 to 6, 840 to 13).forEach { (width, expectedInset) ->
            fun layout() {
                view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(50, View.MeasureSpec.EXACTLY))
                view.layout(0, 0, width, 50)
            }
            fun assertFace(text: String) {
                val face = view.textView(text)
                assertEquals("$width $text left", expectedInset, face.leftIn(view))
                assertEquals("$width $text top", 10, face.topIn(view))
                assertEquals("$width $text bottom", 48, face.bottomIn(view))
                assertEquals("$width $text height", 38, face.height)
            }

            view.showCandidates(CandidateUiSnapshot(50, listOf("通常", "次")))
            layout()
            assertFace("通常")
            assertEquals(5, view.textView("次").leftIn(view) - view.textView("通常").rightIn(view))

            view.showStatus("状態")
            layout()
            assertFace("状態")
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
        val scroll = view.getChildAt(1) as HorizontalScrollView
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
    private fun CandidateStripView.personalInfoAction(): ImageButton = findView { it is ImageButton } as ImageButton
    private fun TextView.layers() = background as LayerDrawable
    private fun TextView.shadowLayer() = layers().getDrawable(0) as GradientDrawable
    private fun TextView.faceLayer() = layers().getDrawable(1) as GradientDrawable
    private fun TextView.faceColor() = faceLayer().color!!.defaultColor
    private fun View.allTextViews(): List<TextView> {
        val result = mutableListOf<TextView>()
        fun visit(view: View) { if (view is TextView) result += view; if (view is ViewGroup) repeat(view.childCount) { visit(view.getChildAt(it)) } }
        visit(this); return result
    }
    private fun View.findView(predicate: (View) -> Boolean): View? {
        if (predicate(this)) return this
        if (this is ViewGroup) repeat(childCount) {
            getChildAt(it).findView(predicate)?.let { return it }
        }
        return null
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
