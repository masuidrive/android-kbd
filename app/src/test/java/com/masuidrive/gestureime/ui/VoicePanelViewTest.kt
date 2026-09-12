package com.masuidrive.gestureime.ui

import android.graphics.drawable.GradientDrawable
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

    private fun View.allTextViews(): List<TextView> = descendants().filterIsInstance<TextView>()

    private fun View.descendants(): List<View> = buildList {
        fun visit(view: View) {
            add(view)
            if (view is ViewGroup) repeat(view.childCount) { visit(view.getChildAt(it)) }
        }
        visit(this@descendants)
    }
}
