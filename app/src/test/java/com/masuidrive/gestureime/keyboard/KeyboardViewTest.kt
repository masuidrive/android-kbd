package com.masuidrive.gestureime.keyboard

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.MotionEvent
import android.view.accessibility.AccessibilityNodeInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class KeyboardViewTest {
    private lateinit var view: KeyboardView
    private val actions = mutableListOf<KeyAction>()

    @Before fun setUp() {
        view = KeyboardView(RuntimeEnvironment.getApplication()).apply {
            actionSink = KeyboardActionSink { actions += it }
            measure(exact(400), exact(180))
            layout(0, 0, 400, 180)
            draw(Canvas(Bitmap.createBitmap(400, 180, Bitmap.Config.ARGB_8888)))
        }
    }

    @Test fun `space tap commits one space`() {
        touch(MotionEvent.ACTION_DOWN, 200f, 157f)
        touch(MotionEvent.ACTION_UP, 200f, 157f, 10)
        assertEquals(listOf(KeyAction.CommitText(" ")), actions)
    }

    @Test fun `space cursor gesture does not also commit space on release`() {
        touch(MotionEvent.ACTION_DOWN, 200f, 157f)
        touch(MotionEvent.ACTION_MOVE, 216f, 158f, 10)
        touch(MotionEvent.ACTION_MOVE, 208f, 170f, 20)
        touch(MotionEvent.ACTION_UP, 208f, 170f, 30)
        assertEquals(listOf(KeyAction.MoveCursor(Direction.RIGHT, 2), KeyAction.MoveCursor(Direction.LEFT, 1)), actions)
        assertTrue(actions.none { it == KeyAction.CommitText(" ") })
    }

    @Test fun `cancel emits no input and clears active pointer`() {
        touch(MotionEvent.ACTION_DOWN, 200f, 157f)
        touch(MotionEvent.ACTION_CANCEL, 200f, 157f, 10)
        assertTrue(actions.isEmpty())
    }

    @Test fun `modifier uses only vertical selection and tap clears it`() {
        touch(MotionEvent.ACTION_DOWN, 10f, 67f)
        touch(MotionEvent.ACTION_MOVE, 10f, 40f, 10)
        touch(MotionEvent.ACTION_UP, 10f, 40f, 20)
        touch(MotionEvent.ACTION_DOWN, 10f, 67f, 30)
        touch(MotionEvent.ACTION_UP, 10f, 67f, 40)
        assertEquals(listOf(KeyAction.SetModifier(Modifier.ALT), KeyAction.SetModifier(null)), actions)
    }

    @Test fun `backspace tap and up are inert while down deletes`() {
        touch(MotionEvent.ACTION_DOWN, 390f, 67f)
        touch(MotionEvent.ACTION_UP, 390f, 67f, 5)
        touch(MotionEvent.ACTION_DOWN, 390f, 67f, 10)
        touch(MotionEvent.ACTION_MOVE, 390f, 40f, 20)
        touch(MotionEvent.ACTION_UP, 390f, 40f, 25)
        touch(MotionEvent.ACTION_DOWN, 390f, 67f, 30)
        touch(MotionEvent.ACTION_MOVE, 390f, 90f, 40)
        touch(MotionEvent.ACTION_UP, 390f, 90f, 45)
        assertEquals(listOf(KeyAction.Backspace()), actions)
    }

    @Test fun `accessibility exposes individual keys and activates focused key`() {
        val provider = view.accessibilityNodeProvider
        val host = requireNotNull(provider.createAccessibilityNodeInfo(-1))
        assertTrue(host.childCount >= 30)
        val firstKey = requireNotNull(provider.createAccessibilityNodeInfo(0))
        assertTrue(firstKey.contentDescription.toString().contains("タップ q"))
        assertTrue(provider.performAction(0, AccessibilityNodeInfo.ACTION_CLICK, null))
        assertEquals(listOf(KeyAction.CommitText("q")), actions)
    }

    private fun touch(action: Int, x: Float, y: Float, time: Long = 0) {
        val event = MotionEvent.obtain(0, time, action, x, y, 0)
        view.onTouchEvent(event)
        event.recycle()
    }

    private fun exact(size: Int) = android.view.View.MeasureSpec.makeMeasureSpec(size, android.view.View.MeasureSpec.EXACTLY)
}
