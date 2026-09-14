package com.masuidrive.gestureime.ui

import android.graphics.drawable.GradientDrawable
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TextView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class VoicePanelViewTest {
    private fun view() = VoicePanelView(RuntimeEnvironment.getApplication())

    @Test
    fun finalCandidatesAreOneSelectableItemPerVerticallyScrollableRow() {
        val view = view()
        val events = mutableListOf<CandidateUiEvent>()
        view.setOnCandidateSelected(events::add)
        view.showCandidates(CandidateUiSnapshot(41, listOf("第一候補", "第二候補", "第三候補")))
        view.setVoiceState(VoiceUiSnapshot(51, VoiceUiState.Preview("第一候補")))
        view.measure(exact(320), exact(110))
        view.layout(0, 0, 320, 110)

        val candidates = view.allTextViews()
        assertEquals(listOf("第一候補", "第二候補", "第三候補"), candidates.map { it.text.toString() })
        assertTrue(candidates.zipWithNext().all { (first, second) -> second.top >= first.bottom })
        assertTrue(candidates.all { it.isClickable && it.isFocusable && it.background is GradientDrawable })
        assertTrue(view.descendants().filterIsInstance<ScrollView>().single().getChildAt(0).height > view.height)

        candidates[1].performClick()
        assertEquals(listOf(CandidateUiEvent(41, 1)), events)
    }

    @Test
    fun finalCandidatesHighlightOnlyCharactersThatDifferAcrossAlternatives() {
        val view = view()
        view.showCandidates(CandidateUiSnapshot(42, listOf(
            "タブレットとかでも横幅を大きめにしてほしい",
            "タブレットとかでも横幅を大きめにして欲しい",
            "タブレットとかでも横幅が大きめにして欲しい",
        )))
        view.setVoiceState(VoiceUiSnapshot(52, VoiceUiState.Preview("候補あり")))

        val rows = view.allTextViews()
        assertEquals(listOf(listOf("を", "ほ"), listOf("を", "欲"), listOf("が", "欲")), rows.map(::highlightedText))
        rows.forEach { row ->
            val text = row.text as Spanned
            assertEquals(2, text.getSpans(0, text.length, StyleSpan::class.java).size)
        }
    }

    @Test
    fun oneOrIdenticalCandidatesKeepNormalTextStyle() {
        listOf(
            listOf("候補はひとつ"),
            listOf("同じ候補", "同じ候補"),
        ).forEachIndexed { index, candidates ->
            val view = view()
            view.showCandidates(CandidateUiSnapshot(43L + index, candidates))
            view.setVoiceState(VoiceUiSnapshot(53L + index, VoiceUiState.Preview(candidates.first())))
            assertTrue(view.allTextViews().all { highlightedText(it).isEmpty() })
        }
    }

    @Test
    fun insertedTextAndSupplementaryCharactersAreHighlightedAsWholeCharacters() {
        listOf(
            listOf("今日は晴れ", "今日はよく晴れ") to listOf(emptyList(), listOf("よく")),
            listOf("候補😀です", "候補😃です") to listOf(listOf("😀"), listOf("😃")),
        ).forEachIndexed { index, (candidates, expected) ->
            val view = view()
            view.showCandidates(CandidateUiSnapshot(45L + index, candidates))
            view.setVoiceState(VoiceUiSnapshot(55L + index, VoiceUiState.Preview(candidates.first())))
            assertEquals(expected, view.allTextViews().map(::highlightedText))
        }
    }

    @Test
    fun repeatedCharactersUseTheSameDeterministicAlignmentAsTheMock() {
        val view = view()
        view.showCandidates(CandidateUiSnapshot(47, listOf("aa", "ab")))
        view.setVoiceState(VoiceUiSnapshot(57, VoiceUiState.Preview("aa")))
        assertEquals(listOf(listOf(1 to 2), listOf(1 to 2)), view.allTextViews().map(::highlightedRanges))
    }

    @Test
    fun excessiveDiffWorkKeepsFullCandidatesVisibleWithoutBlockingForHighlighting() {
        listOf(
            listOf("あ".repeat(600), "い".repeat(600)),
            listOf("あ".repeat(131_072), "い"),
        ).forEachIndexed { index, candidates ->
            val view = view()
            view.showCandidates(CandidateUiSnapshot(48L + index, candidates))
            view.setVoiceState(VoiceUiSnapshot(58L + index, VoiceUiState.Preview(candidates.first())))
            assertEquals(candidates, view.allTextViews().map { it.text.toString() })
            assertTrue(view.allTextViews().all { highlightedText(it).isEmpty() })
        }
    }

    @Test
    fun partialAndUnavailableArePlainNonClickablePanelText() {
        val view = view()
        listOf(
            VoiceUiState.Partial("長い発話の途中") to "長い発話の途中",
            VoiceUiState.Unavailable("端末内音声認識を利用できません") to "端末内音声認識を利用できません",
        ).forEach { (state, expected) ->
            view.setVoiceState(VoiceUiSnapshot(61, state))
            val text = view.textView(expected)
            assertFalse(text.isClickable)
            assertFalse(text.isFocusable)
            assertFalse(text.performClick())
            assertNull(text.background)
        }
    }

    @Test
    fun permissionIsPlainNonActionText() {
        val view = view()
        view.setVoiceState(VoiceUiSnapshot(71, VoiceUiState.PermissionRequired))
        view.measure(exact(320), exact(160))
        view.layout(0, 0, 320, 160)

        val permission = view.allTextViews().single()
        assertEquals("マイクの許可が必要です", permission.text.toString())
        assertEquals(308, permission.width)
        assertFalse(permission.isClickable)
        assertFalse(permission.isFocusable)
        assertFalse(permission.performClick())
        assertNull(permission.background)
    }

    @Test
    fun recognizingClearsThePreviousCandidatePanel() {
        val view = view()
        view.showCandidates(CandidateUiSnapshot(81, listOf("確定候補")))
        view.setVoiceState(VoiceUiSnapshot(82, VoiceUiState.Preview("確定候補")))
        assertTrue(view.allTextViews().isNotEmpty())

        view.showCandidates(CandidateUiSnapshot(83, emptyList(), selectable = false))
        view.setVoiceState(VoiceUiSnapshot(84, VoiceUiState.Recognizing))

        assertTrue(view.allTextViews().isEmpty())
    }

    private fun exact(size: Int) = View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY)

    private fun View.textView(value: String): TextView =
        allTextViews().single { it.text.toString() == value }

    private fun highlightedText(view: TextView): List<String> {
        val text = view.text
        if (text !is Spanned) return emptyList()
        return text.getSpans(0, text.length, ForegroundColorSpan::class.java)
            .sortedBy(text::getSpanStart)
            .map { text.subSequence(text.getSpanStart(it), text.getSpanEnd(it)).toString() }
    }

    private fun highlightedRanges(view: TextView): List<Pair<Int, Int>> {
        val text = view.text as Spanned
        return text.getSpans(0, text.length, ForegroundColorSpan::class.java)
            .sortedBy(text::getSpanStart)
            .map { text.getSpanStart(it) to text.getSpanEnd(it) }
    }

    private fun View.allTextViews(): List<TextView> = descendants().filterIsInstance<TextView>()

    private fun View.descendants(): List<View> = buildList {
        fun visit(view: View) {
            add(view)
            if (view is ViewGroup) repeat(view.childCount) { visit(view.getChildAt(it)) }
        }
        visit(this@descendants)
    }
}
