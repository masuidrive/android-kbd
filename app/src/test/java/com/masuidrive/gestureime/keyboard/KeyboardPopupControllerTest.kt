package com.masuidrive.gestureime.keyboard

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.graphics.RectF
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class KeyboardPopupControllerTest {
    private val geometry = PopupGeometry(1f)

    @Test fun `accent hit index uses key left minus eight then padded tiles`() {
        val target = RectF(100f, 80f, 150f, 125f)
        val controller = KeyboardPopupController(RuntimeEnvironment.getApplication())

        val anchor = android.view.View(RuntimeEnvironment.getApplication()).apply { layout(0, 0, 400, 220) }
        assertEquals(0, controller.accentIndexFor(anchor, target, 3, 94f))
        assertEquals(1, controller.accentIndexFor(anchor, target, 3, 94f + 3f + 34f + 2f))
        assertEquals(2, controller.accentIndexFor(anchor, target, 3, 1_000f))
    }

    @Test fun `accent parent clamps from key left rather than key center`() {
        val parent = geometry.accentParent(RectF(2f, 70f, 40f, 115f), 3, 150f)
        assertEquals(0f, parent.left)
        assertEquals(108f, parent.width())
        assertEquals(14f, parent.top)
    }

    @Test fun `popup placement clamps a screen anchored kana cross`() {
        val anchor = android.view.View(RuntimeEnvironment.getApplication()).apply { layout(0, 0, 400, 220) }
        val size = geometry.windowSize(PopupKind.KANA, 0)
        val position = geometry.placement(anchor, RectF(360f, 5f, 400f, 50f), PopupKind.KANA, 0, size, Rect(0, 0, 400, 800)).position

        assertEquals(236, position.x)
        assertEquals(0, position.y)
    }

    @Test fun `kana popup is five independent tiles with transparent corner`() {
        val spec = KeyboardLayouts.layout(KeyboardMode.KANA).rows[0].keys[1]
        val size = geometry.windowSize(PopupKind.KANA, 0)
        val view = KeyboardPopupRenderView(RuntimeEnvironment.getApplication(), geometry)
        // Robolectric does not rasterize a software-layer-only detached View.
        view.setLayerType(android.view.View.LAYER_TYPE_NONE, null)
        view.bind(PopupKind.KANA, spec, Direction.UP, emptyList(), 0, size, 7f, 7f)
        view.measure(exact(size.width), exact(size.height)); view.layout(0, 0, size.width, size.height)
        val bitmap = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))

        assertEquals(Color.TRANSPARENT, bitmap.getPixel(5, 5))
        assertEquals(0xff55555a.toInt(), bitmap.getPixel(geometry.tileRect(Direction.LEFT, size).centerX().toInt(), geometry.tileRect(Direction.LEFT, size).centerY().toInt()))
        assertEquals(0xffa8ceff.toInt(), bitmap.getPixel(geometry.tileRect(Direction.UP, size).centerX().toInt(), geometry.tileRect(Direction.UP, size).centerY().toInt()))
    }

    @Test fun `letter and modifier use separate source dimensions`() {
        assertEquals(PopupSize(72, 83), geometry.windowSize(PopupKind.LETTER, 0))
        assertEquals(PopupSize(104, 83), geometry.windowSize(PopupKind.MODIFIER, 0))
        assertEquals(PopupSize(122, 71), geometry.windowSize(PopupKind.ACCENT, 3))
        assertTrue(geometry.tileRect(Direction.CENTER, geometry.windowSize(PopupKind.KANA, 0)).width() == 50f)
    }

    private fun exact(value: Int) = android.view.View.MeasureSpec.makeMeasureSpec(value, android.view.View.MeasureSpec.EXACTLY)
}
