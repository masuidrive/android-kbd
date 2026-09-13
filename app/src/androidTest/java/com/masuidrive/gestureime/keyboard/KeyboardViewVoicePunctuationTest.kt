package com.masuidrive.gestureime.keyboard

import android.graphics.Rect
import android.os.SystemClock
import android.view.MotionEvent
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.masuidrive.gestureime.ImeTestActivity
import org.junit.Assert.assertEquals
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

    private fun dispatch(view: KeyboardView, action: Int, downTime: Long, eventTime: Long, x: Float, y: Float) {
        MotionEvent.obtain(downTime, eventTime, action, x, y, 0).also {
            view.dispatchTouchEvent(it)
            it.recycle()
        }
    }

    private fun exact(size: Int) = android.view.View.MeasureSpec.makeMeasureSpec(size, android.view.View.MeasureSpec.EXACTLY)
}
