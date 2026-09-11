package com.masuidrive.gestureime.keyboard

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.content.res.Configuration
import android.view.MotionEvent
import android.view.accessibility.AccessibilityNodeInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
            measure(exact(400), exact(220))
            layout(0, 0, 400, 220)
            draw(Canvas(Bitmap.createBitmap(400, 220, Bitmap.Config.ARGB_8888)))
        }
    }

    @Test fun `space tap commits one space`() {
        touch(MotionEvent.ACTION_DOWN, 200f, 190f)
        touch(MotionEvent.ACTION_UP, 200f, 190f, 10)
        assertEquals(listOf(KeyAction.CommitText(" ")), actions)
    }

    @Test fun `space cursor gesture does not also commit space on release`() {
        touch(MotionEvent.ACTION_DOWN, 200f, 190f)
        touch(MotionEvent.ACTION_MOVE, 216f, 191f, 10)
        touch(MotionEvent.ACTION_MOVE, 208f, 203f, 20)
        touch(MotionEvent.ACTION_UP, 208f, 203f, 30)
        assertEquals(listOf(KeyAction.MoveCursor(Direction.RIGHT, 2), KeyAction.MoveCursor(Direction.LEFT, 1)), actions)
        assertTrue(actions.none { it == KeyAction.CommitText(" ") })
    }

    @Test fun `cancel emits no input and clears active pointer`() {
        touch(MotionEvent.ACTION_DOWN, 200f, 190f)
        touch(MotionEvent.ACTION_CANCEL, 200f, 190f, 10)
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

    @Test fun `backspace tap deletes down escapes and other directions are inert`() {
        touch(MotionEvent.ACTION_DOWN, 390f, 67f)
        touch(MotionEvent.ACTION_UP, 390f, 67f, 5)
        touch(MotionEvent.ACTION_DOWN, 390f, 67f, 10)
        touch(MotionEvent.ACTION_MOVE, 390f, 40f, 20)
        touch(MotionEvent.ACTION_UP, 390f, 40f, 25)
        touch(MotionEvent.ACTION_DOWN, 390f, 67f, 30)
        touch(MotionEvent.ACTION_MOVE, 390f, 90f, 40)
        touch(MotionEvent.ACTION_UP, 390f, 90f, 45)
        assertEquals(listOf(KeyAction.Backspace(), KeyAction.Escape), actions)
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

    @Test fun `fixed key labels keep their bounds at large system font scales`() {
        val normal = renderAtFontScale(1f)
        val enlarged = renderAtFontScale(1.3f)
        val accessibility = renderAtFontScale(2f)

        assertTrue("font scale 1.3 must not expand labels beyond their key bounds", normal.sameAs(enlarged))
        assertTrue("font scale 2.0 must not expand labels beyond their key bounds", normal.sameAs(accessibility))
    }

    @Test fun `preview consumes touch and accessibility without changing input state`() {
        view.setPreviewOnly(true)
        touch(MotionEvent.ACTION_DOWN, 40f, 20f)
        touch(MotionEvent.ACTION_UP, 40f, 20f, 10)
        val provider = view.accessibilityNodeProvider

        assertTrue(actions.isEmpty())
        assertTrue(!provider.performAction(0, AccessibilityNodeInfo.ACTION_CLICK, null))
        assertTrue(actions.isEmpty())
    }

    @Test fun `extreme label adjustments preserve bounded nonoverlapping keys at narrow and wide widths`() {
        val extreme = QwertyLabelGroup.entries.fold(QwertyLabelStyle.DEFAULT) { style, group ->
            style.with(group, LabelAdjustment(1.3f, 6f, 8f))
        }
        view.setQwertyLabelStyle(extreme)

        listOf(412 to 220, 840 to 248).forEach { (width, height) ->
            view.measure(exact(width), exact(height)); view.layout(0, 0, width, height)
            view.draw(Canvas(Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)))
            val provider = view.accessibilityNodeProvider
            val host = requireNotNull(provider.createAccessibilityNodeInfo(-1))
            val bounds = (0 until host.childCount).map { id -> Rect().also { provider.createAccessibilityNodeInfo(id)!!.getBoundsInParent(it) } }
            bounds.forEach { assertTrue("key left bounds at $width px", it.left >= 0); assertTrue("key right bounds at $width px", it.right <= width) }
            for (i in bounds.indices) for (j in i + 1 until bounds.size) assertFalse("keys overlap at $width px", Rect.intersects(bounds[i], bounds[j]))
        }
    }

    @Test fun `label style does not change key hit bounds`() {
        fun bounds() = Rect().also { view.accessibilityNodeProvider.createAccessibilityNodeInfo(0)!!.getBoundsInParent(it) }
        val before = bounds()
        view.setQwertyLabelStyle(QwertyLabelStyle.DEFAULT.with(QwertyLabelGroup.LETTER_PRIMARY, LabelAdjustment(.7f, -6f, -8f)))
        view.draw(Canvas(Bitmap.createBitmap(400, 220, Bitmap.Config.ARGB_8888)))
        assertEquals(before, bounds())
    }

    @Test fun `dual kana exposes two twelve-key groups only on wide layouts`() {
        view.setMode(KeyboardMode.KANA)
        view.setDualFlickEnabled(true)
        assertEquals(19, requireNotNull(view.accessibilityNodeProvider.createAccessibilityNodeInfo(-1)).childCount)

        view.measure(exact(599), exact(228))
        view.layout(0, 0, 599, 228)
        assertEquals(19, requireNotNull(view.accessibilityNodeProvider.createAccessibilityNodeInfo(-1)).childCount)

        view.measure(exact(600), exact(256))
        view.layout(0, 0, 600, 256)
        view.draw(Canvas(Bitmap.createBitmap(600, 256, Bitmap.Config.ARGB_8888)))
        assertEquals(31, requireNotNull(view.accessibilityNodeProvider.createAccessibilityNodeInfo(-1)).childCount)
    }

    @Test fun `qwerty geometry matches css gaps heights and bottom row proportions`() {
        view.measure(exact(412), exact(220))
        view.layout(0, 0, 412, 220)
        val provider = view.accessibilityNodeProvider
        fun bounds(id: Int) = Rect().also { provider.createAccessibilityNodeInfo(id)!!.getBoundsInParent(it) }
        val q = bounds(0)
        val w = bounds(1)
        assertEquals(45, q.height())
        assertEquals(6, w.left - q.right)
        assertEquals(10, bounds(10).top - q.bottom)
        val mode = bounds(31).width() + 6
        val space = bounds(32).width() + 6
        val enter = bounds(33).width() + 6
        val total = mode + space + enter
        assertEquals(.19f, mode.toFloat() / total, .01f)
        assertEquals(.55f, space.toFloat() / total, .01f)
        assertEquals(.26f, enter.toFloat() / total, .01f)
    }

    @Test fun `inner width uses css outer inset and taller kana keys`() {
        view.setMode(KeyboardMode.KANA)
        view.measure(exact(840), exact(256))
        view.layout(0, 0, 840, 256)
        val provider = view.accessibilityNodeProvider
        fun bounds(id: Int) = Rect().also { provider.createAccessibilityNodeInfo(id)!!.getBoundsInParent(it) }
        val first = bounds(0)
        assertEquals(10, first.left)
        assertEquals(58, first.height())
        assertEquals(6, bounds(5).top - first.bottom)
        assertEquals(6, bounds(1).left - first.right)
    }

    @Test fun `kana popup leaves surrounding key backgrounds undimmed`() {
        view.setMode(KeyboardMode.KANA)
        view.measure(exact(400), exact(228))
        view.layout(0, 0, 400, 228)
        fun render() = Bitmap.createBitmap(400, 228, Bitmap.Config.ARGB_8888).also { view.draw(Canvas(it)) }
        val idle = render()
        touch(MotionEvent.ACTION_DOWN, 120f, 20f)
        val popup = render()
        assertEquals(idle.getPixel(250, 200), popup.getPixel(250, 200))
    }

    @Test fun `simultaneous dual kana pointers dispatch in pointer up order`() {
        view.setMode(KeyboardMode.KANA)
        view.setDualFlickEnabled(true)
        view.measure(exact(840), exact(220))
        view.layout(0, 0, 840, 220)
        view.draw(Canvas(Bitmap.createBitmap(840, 220, Bitmap.Config.ARGB_8888)))
        view.actionSink = KeyboardActionSink { action ->
            actions += action
            if (action is KeyAction.KanaInput) view.setConversionActive(true)
        }

        multiTouch(MotionEvent.ACTION_DOWN, listOf(0 to (150f to 20f)))
        multiTouch(MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
            listOf(0 to (150f to 20f), 1 to (470f to 20f)))
        multiTouch(MotionEvent.ACTION_POINTER_UP or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
            listOf(0 to (150f to 20f), 1 to (470f to 20f)))
        multiTouch(MotionEvent.ACTION_UP, listOf(0 to (150f to 20f)))

        assertEquals(listOf(KeyAction.KanaInput("あ"), KeyAction.KanaInput("あ")), actions)
    }

    @Test fun `single and dual kana targets never overlap spanning enter`() {
        view.setMode(KeyboardMode.KANA)
        assertNoVisibleTargetsOverlap(width = 400, height = 228, expectedCount = 19)
        view.setDualFlickEnabled(true)
        assertNoVisibleTargetsOverlap(width = 840, height = 256, expectedCount = 31)
    }

    @Test fun `both halves of spanning kana enter dispatch enter`() {
        view.setMode(KeyboardMode.KANA)
        view.measure(exact(400), exact(228))
        view.layout(0, 0, 400, 228)
        touch(MotionEvent.ACTION_DOWN, 360f, 130f)
        touch(MotionEvent.ACTION_UP, 360f, 130f, 5)
        touch(MotionEvent.ACTION_DOWN, 360f, 190f, 10)
        touch(MotionEvent.ACTION_UP, 360f, 190f, 15)
        assertEquals(listOf(KeyAction.Enter, KeyAction.Enter), actions)
    }

    private fun renderAtFontScale(fontScale: Float): Bitmap {
        val base = RuntimeEnvironment.getApplication()
        val configuration = Configuration(base.resources.configuration).apply { this.fontScale = fontScale }
        val context = base.createConfigurationContext(configuration)
        return Bitmap.createBitmap(400, 220, Bitmap.Config.ARGB_8888).also { bitmap ->
            KeyboardView(context).apply {
                measure(exact(400), exact(220))
                layout(0, 0, 400, 220)
                draw(Canvas(bitmap))
            }
        }
    }

    private fun multiTouch(action: Int, pointers: List<Pair<Int, Pair<Float, Float>>>) {
        val properties = pointers.map { (id, _) -> MotionEvent.PointerProperties().apply { this.id = id } }.toTypedArray()
        val coordinates = pointers.map { (_, point) -> MotionEvent.PointerCoords().apply { x = point.first; y = point.second } }.toTypedArray()
        MotionEvent.obtain(0, 0, action, pointers.size, properties, coordinates, 0, 0, 1f, 1f, 0, 0, 0, 0).also {
            view.onTouchEvent(it)
            it.recycle()
        }
    }

    private fun assertNoVisibleTargetsOverlap(width: Int, height: Int, expectedCount: Int) {
        view.measure(exact(width), exact(height))
        view.layout(0, 0, width, height)
        view.draw(Canvas(Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)))
        val provider = view.accessibilityNodeProvider
        val bounds = (0 until expectedCount).map { id ->
            Rect().also { provider.createAccessibilityNodeInfo(id)!!.getBoundsInParent(it) }
        }
        bounds.forEachIndexed { index, first ->
            bounds.drop(index + 1).forEach { second ->
                assertTrue("virtual key bounds overlap: $first and $second", !Rect.intersects(first, second))
            }
        }
    }

    private fun touch(action: Int, x: Float, y: Float, time: Long = 0) {
        val event = MotionEvent.obtain(0, time, action, x, y, 0)
        view.onTouchEvent(event)
        event.recycle()
    }

    private fun exact(size: Int) = android.view.View.MeasureSpec.makeMeasureSpec(size, android.view.View.MeasureSpec.EXACTLY)
}
