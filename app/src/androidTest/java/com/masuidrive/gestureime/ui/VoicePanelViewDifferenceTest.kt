package com.masuidrive.gestureime.ui

import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.masuidrive.gestureime.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VoicePanelViewDifferenceTest {
    @Test
    fun similarVoiceCandidatesHighlightOnlyTheirDifferentCharacters() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        lateinit var rows: List<TextView>
        instrumentation.runOnMainSync {
            val view = VoicePanelView(instrumentation.targetContext)
            val candidates = listOf(
                "タブレットとかでも横幅を大きめにしてほしい",
                "タブレットとかでも横幅を大きめにして欲しい",
                "タブレットとかでも横幅が大きめにして欲しい",
            )
            view.showCandidates(CandidateUiSnapshot(1, candidates))
            view.setVoiceState(VoiceUiSnapshot(1, VoiceUiState.Preview(candidates.first())))
            rows = view.descendants().filterIsInstance<TextView>()
        }

        assertEquals(listOf(listOf("を", "ほ"), listOf("を", "欲"), listOf("が", "欲")), rows.map(::highlightedText))
        rows.forEach { row ->
            val text = row.text as Spanned
            assertEquals(2, text.getSpans(0, text.length, StyleSpan::class.java).size)
            text.getSpans(0, text.length, ForegroundColorSpan::class.java).forEach {
                assertEquals(row.context.getColor(R.color.candidate_difference_text), it.foregroundColor)
            }
        }
    }

    private fun highlightedText(view: TextView): List<String> {
        val text = view.text as Spanned
        return text.getSpans(0, text.length, ForegroundColorSpan::class.java)
            .sortedBy(text::getSpanStart)
            .map { text.subSequence(text.getSpanStart(it), text.getSpanEnd(it)).toString() }
    }

    private fun View.descendants(): List<View> = buildList {
        fun visit(view: View) {
            add(view)
            if (view is ViewGroup) repeat(view.childCount) { visit(view.getChildAt(it)) }
        }
        visit(this@descendants)
    }
}
