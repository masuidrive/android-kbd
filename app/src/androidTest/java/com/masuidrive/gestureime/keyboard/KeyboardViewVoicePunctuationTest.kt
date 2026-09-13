package com.masuidrive.gestureime.keyboard

import android.graphics.Rect
import android.os.SystemClock
import android.view.MotionEvent
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.masuidrive.gestureime.ImeTestActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KeyboardViewVoicePunctuationTest {
    @Test fun voicePunctuationDispatchesEveryDirectionOnceOnAndroidView() {
        ActivityScenario.launch(ImeTestActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val actions = mutableListOf<KeyAction>()
                val density = activity.resources.displayMetrics.density
                val view = KeyboardView(activity).apply {
                    actionSink = KeyboardActionSink { actions += it }
                    setMode(KeyboardMode.VOICE)
                }
                activity.setContentView(view)
                val width = (412f * density).toInt()
                val height = (228f * density).toInt()
                view.measure(exact(width), exact(height))
                view.layout(0, 0, width, height)
                val punctuation = Rect().also {
                    requireNotNull(view.accessibilityNodeProvider.createAccessibilityNodeInfo(5)).getBoundsInParent(it)
                }
                val centerX = punctuation.exactCenterX()
                val centerY = punctuation.exactCenterY()
                val distance = 30f * density

                listOf(
                    Triple(0f, 0f, "、"),
                    Triple(-distance, 0f, "。"),
                    Triple(0f, -distance, "？"),
                    Triple(distance, 0f, "！"),
                    Triple(0f, distance, "、"),
                ).forEachIndexed { index, (dx, dy, expected) ->
                    actions.clear()
                    val downTime = SystemClock.uptimeMillis() + index * 10L
                    dispatch(view, MotionEvent.ACTION_DOWN, downTime, downTime, centerX, centerY)
                    if (dx != 0f || dy != 0f) {
                        dispatch(view, MotionEvent.ACTION_MOVE, downTime, downTime + 1, centerX + dx, centerY + dy)
                    }
                    dispatch(view, MotionEvent.ACTION_UP, downTime, downTime + 2, centerX + dx, centerY + dy)
                    assertEquals("voice punctuation $expected", listOf(KeyAction.CommitText(expected)), actions)
                }
            }
        }
    }

    @Test fun kanaSmallKeyDispatchesEveryConfiguredFlickOnAndroidViewAtPhoneAndTabletWidths() {
        ActivityScenario.launch(ImeTestActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val actions = mutableListOf<KeyAction>()
                val density = activity.resources.displayMetrics.density
                val view = KeyboardView(activity).apply {
                    actionSink = KeyboardActionSink { actions += it }
                    setMode(KeyboardMode.KANA)
                }
                activity.setContentView(view)
                val accentId = KeyboardLayouts.layout(KeyboardMode.KANA, false, false).rows
                    .flatMap { it.keys }
                    .indexOfFirst { it.kind == KeyKind.ACCENT }
                check(accentId >= 0) { "Kana layout has no small-key accent" }
                val distance = 30f * density
                val flicks = listOf(
                    "tap" to Triple(0f, 0f, KeyAction.TransformKana(KanaTransform.CYCLE)),
                    "up" to Triple(0f, -distance, KeyAction.TransformKana(KanaTransform.DAKUTEN)),
                    "left" to Triple(-distance, 0f, KeyAction.TransformKana(KanaTransform.DAKUTEN)),
                    "right" to Triple(distance, 0f, KeyAction.TransformKana(KanaTransform.HANDAKUTEN)),
                    "down" to Triple(0f, distance, null),
                )

                listOf(412f, 840f).forEach { widthDp ->
                    val width = (widthDp * density).toInt()
                    val height = (228f * density).toInt()
                    view.measure(exact(width), exact(height))
                    view.layout(0, 0, width, height)
                    val accent = bounds(view, accentId)
                    val centerX = accent.exactCenterX()
                    val centerY = accent.exactCenterY()
                    flicks.forEachIndexed { index, (name, flick) ->
                        val (dx, dy, expected) = flick
                        actions.clear()
                        val downTime = SystemClock.uptimeMillis() + index * 10L
                        dispatch(view, MotionEvent.ACTION_DOWN, downTime, downTime, centerX, centerY)
                        if (dx != 0f || dy != 0f) {
                            dispatch(view, MotionEvent.ACTION_MOVE, downTime, downTime + 1, centerX + dx, centerY + dy)
                        }
                        dispatch(view, MotionEvent.ACTION_UP, downTime, downTime + 2, centerX + dx, centerY + dy)
                        val expectedActions = expected?.let(::listOf) ?: emptyList()
                        assertEquals("$widthDp dp Kana small-key $name", expectedActions, actions)
                    }
                }
            }
        }
    }

    @Test fun voiceBackspaceIsTapOnlyAcrossSixEqualColumnsOnAndroidView() {
        ActivityScenario.launch(ImeTestActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val actions = mutableListOf<KeyAction>()
                val density = activity.resources.displayMetrics.density
                val view = KeyboardView(activity).apply {
                    actionSink = KeyboardActionSink { actions += it }
                    setMode(KeyboardMode.VOICE)
                }
                activity.setContentView(view)
                listOf(412f, 840f).forEach { widthDp ->
                    val width = (widthDp * density).toInt()
                    val height = (228f * density).toInt()
                    view.measure(exact(width), exact(height))
                    view.layout(0, 0, width, height)
                    val columns = (3..8).map { bounds(view, it) }
                    assertTrue("$widthDp dp voice columns have equal face widths", columns.all {
                        kotlin.math.abs(it.width() - columns.first().width()) <= 1
                    })
                    assertTrue("$widthDp dp voice columns share their bottom row", columns.all {
                        it.top == columns.first().top && it.bottom == columns.first().bottom
                    })
                    val backspace = columns[4]
                    val centerX = backspace.exactCenterX()
                    val centerY = backspace.exactCenterY()
                    val distance = 30f * density

                    actions.clear()
                    val tapTime = SystemClock.uptimeMillis()
                    dispatch(view, MotionEvent.ACTION_DOWN, tapTime, tapTime, centerX, centerY)
                    dispatch(view, MotionEvent.ACTION_UP, tapTime, tapTime + 1, centerX, centerY)
                    assertEquals("$widthDp dp voice backspace tap", listOf(KeyAction.Backspace()), actions)

                    listOf(-distance to 0f, 0f to -distance, distance to 0f, 0f to distance)
                        .forEachIndexed { index, (dx, dy) ->
                            actions.clear()
                            val downTime = SystemClock.uptimeMillis() + index * 10L
                            dispatch(view, MotionEvent.ACTION_DOWN, downTime, downTime, centerX, centerY)
                            dispatch(view, MotionEvent.ACTION_MOVE, downTime, downTime + 1, centerX + dx, centerY + dy)
                            dispatch(view, MotionEvent.ACTION_UP, downTime, downTime + 2, centerX + dx, centerY + dy)
                            assertTrue("$widthDp dp voice backspace drag $index", actions.isEmpty())
                        }

                    actions.clear()
                    val returnTime = SystemClock.uptimeMillis()
                    dispatch(view, MotionEvent.ACTION_DOWN, returnTime, returnTime, centerX, centerY)
                    dispatch(view, MotionEvent.ACTION_MOVE, returnTime, returnTime + 1, centerX + distance, centerY)
                    dispatch(view, MotionEvent.ACTION_MOVE, returnTime, returnTime + 2, centerX + 10f * density, centerY)
                    dispatch(view, MotionEvent.ACTION_UP, returnTime, returnTime + 3, centerX + 10f * density, centerY)
                    assertEquals("$widthDp dp voice backspace returns to center", listOf(KeyAction.Backspace()), actions)
                }
            }
        }
    }

    private fun dispatch(view: KeyboardView, action: Int, downTime: Long, eventTime: Long, x: Float, y: Float) {
        MotionEvent.obtain(downTime, eventTime, action, x, y, 0).also {
            view.dispatchTouchEvent(it)
            it.recycle()
        }
    }

    private fun bounds(view: KeyboardView, viewId: Int): Rect = Rect().also {
        requireNotNull(view.accessibilityNodeProvider.createAccessibilityNodeInfo(viewId)).getBoundsInParent(it)
    }

    private fun exact(size: Int) = android.view.View.MeasureSpec.makeMeasureSpec(size, android.view.View.MeasureSpec.EXACTLY)
}
