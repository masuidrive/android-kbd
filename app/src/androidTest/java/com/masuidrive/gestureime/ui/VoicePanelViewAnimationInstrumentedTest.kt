package com.masuidrive.gestureime.ui

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.masuidrive.gestureime.ImeTestActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VoicePanelViewAnimationInstrumentedTest {
    @Test
    fun attachedPanelKeepsItsHeightAndAnimatesOnlyTheSelectedCandidate() {
        ActivityScenario.launch(ImeTestActivity::class.java).use { scenario ->
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val events = mutableListOf<CandidateUiEvent>()
            lateinit var panel: VoicePanelView
            lateinit var selected: TextView
            var panelHeight = 0
            var initialTop = 0
            scenario.onActivity { activity ->
                val root = FrameLayout(activity)
                panel = VoicePanelView(activity).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        (165 * activity.resources.displayMetrics.density).toInt(),
                    )
                    setOnCandidateSelected(events::add)
                    showCandidates(CandidateUiSnapshot(71, listOf("第一候補", "第二候補", "第三候補")))
                    setVoiceState(VoiceUiSnapshot(71, VoiceUiState.Preview("第一候補")))
                }
                root.addView(panel)
                activity.setContentView(root)
            }
            instrumentation.waitForIdleSync()
            scenario.onActivity {
                selected = panel.textView("第三候補")
                panelHeight = panel.height
                initialTop = selected.top
                assertTrue(panelHeight > 0)
                selected.performClick()
                assertEquals(listOf("候補を選択", "第三候補"), panel.allTextViews().map { it.text.toString() })
                assertEquals(initialTop, selected.top)
                assertEquals(1f, selected.alpha, 0f)
                assertFalse(selected.isClickable)
            }

            Thread.sleep(250)
            scenario.onActivity {
                assertTrue(selected.top + selected.translationY <= 1f)
                assertTrue(selected.alpha in 0.19f..0.3f)
                assertEquals(panelHeight, panel.height)
            }

            Thread.sleep(80)
            instrumentation.waitForIdleSync()
            scenario.onActivity {
                assertEquals(listOf(CandidateUiEvent(71, 2)), events)
                assertEquals(listOf("候補を選択"), panel.allTextViews().map { it.text.toString() })
                assertEquals(panelHeight, panel.height)
            }
        }
    }

    @Test
    fun cancelingAnAttachedCandidateAnimationSuppressesItsCallback() {
        ActivityScenario.launch(ImeTestActivity::class.java).use { scenario ->
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val events = mutableListOf<CandidateUiEvent>()
            lateinit var panel: VoicePanelView
            scenario.onActivity { activity ->
                panel = VoicePanelView(activity).apply {
                    setOnCandidateSelected(events::add)
                    showCandidates(CandidateUiSnapshot(72, listOf("候補")))
                    setVoiceState(VoiceUiSnapshot(72, VoiceUiState.Preview("候補")))
                }
                activity.setContentView(panel)
            }
            instrumentation.waitForIdleSync()
            scenario.onActivity {
                panel.textView("候補").performClick()
                panel.cancelCandidateSelection()
            }

            Thread.sleep(300)
            instrumentation.waitForIdleSync()
            scenario.onActivity {
                assertEquals(emptyList<CandidateUiEvent>(), events)
                assertEquals(listOf("候補を選択"), panel.allTextViews().map { it.text.toString() })
            }
        }
    }

    private fun VoicePanelView.textView(value: String): TextView =
        allTextViews().single { it.text.toString() == value }

    private fun VoicePanelView.allTextViews(): List<TextView> = buildList {
        fun visit(view: View) {
            if (view is TextView) add(view)
            if (view is ViewGroup) repeat(view.childCount) { visit(view.getChildAt(it)) }
        }
        visit(this@allTextViews)
    }
}
