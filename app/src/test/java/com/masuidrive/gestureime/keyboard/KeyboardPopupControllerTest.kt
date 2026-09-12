package com.masuidrive.gestureime.keyboard

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.graphics.RectF
import android.app.Activity
import android.view.View
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class KeyboardPopupControllerTest {
    private val geometry = PopupGeometry(1f)

    @Test fun `accent hit index uses key left minus eight then padded tiles`() {
        val target = RectF(100f, 80f, 150f, 125f)
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val controller = KeyboardPopupController(activity)
        val anchor = View(activity)
        activity.setContentView(anchor)
        anchor.measure(exact(400), exact(220)); anchor.layout(0, 0, 400, 220)
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

    @Test fun `popup placement clamps the content as well as its shadow surface`() {
        val anchor = android.view.View(RuntimeEnvironment.getApplication()).apply { layout(0, 0, 400, 220) }
        val size = geometry.windowSize(PopupKind.KANA, 0)
        val placement = geometry.placement(anchor, RectF(360f, 5f, 400f, 50f), PopupKind.KANA, 0, size, Rect(0, 0, 400, 800))
        val position = placement.position
        val up = geometry.tileRect(Direction.UP, size, placement.contentOffsetX, placement.contentOffsetY)

        assertEquals(236, position.x)
        assertEquals(0, position.y)
        assertTrue(up.top >= 0f)
        assertTrue(up.bottom <= size.height)
    }

    @Test fun `screen-clamped popup position is converted through the anchor window origin`() {
        val surfaceScreenPosition = PopupPosition(236, 1_540)

        assertEquals(PopupPosition(236, 90), geometry.windowPosition(
            surfaceScreenPosition,
            anchorScreenLeft = 10,
            anchorScreenTop = 1_500,
            anchorWindowLeft = 10,
            anchorWindowTop = 50,
        ))
    }

    @Test @Config(qualifiers = "night") @GraphicsMode(GraphicsMode.Mode.NATIVE) fun `dark kana popup keeps its existing colors`() {
        val spec = KeyboardLayouts.layout(KeyboardMode.KANA).rows[0].keys[1]
        val size = geometry.windowSize(PopupKind.KANA, 0)
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val view = KeyboardPopupRenderView(activity, geometry)
        // Robolectric does not rasterize a software-layer-only detached View.
        view.setLayerType(android.view.View.LAYER_TYPE_NONE, null)
        view.bind(PopupKind.KANA, spec, Direction.UP, emptyList(), 0, size, 7f, 7f)
        activity.setContentView(view)
        view.measure(exact(size.width), exact(size.height)); view.layout(0, 0, size.width, size.height)
        val bitmap = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))

        assertEquals(Color.TRANSPARENT, bitmap.getPixel(5, 5))
        val left = geometry.tileRect(Direction.LEFT, size)
        val up = geometry.tileRect(Direction.UP, size)
        assertEquals(0xff55555a.toInt(), bitmap.getPixel((left.left + 6f).toInt(), left.centerY().toInt()))
        assertEquals(0xffa8ceff.toInt(), bitmap.getPixel((up.left + 6f).toInt(), up.centerY().toInt()))
    }

    @Test @Config(qualifiers = "notnight") @GraphicsMode(GraphicsMode.Mode.NATIVE) fun `light kana popup uses the reference colors`() {
        val spec = KeyboardLayouts.layout(KeyboardMode.KANA).rows[0].keys[1]
        val size = geometry.windowSize(PopupKind.KANA, 0)
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val view = KeyboardPopupRenderView(activity, geometry)
        view.setLayerType(android.view.View.LAYER_TYPE_NONE, null)
        view.bind(PopupKind.KANA, spec, Direction.UP, emptyList(), 0, size, 7f, 7f)
        activity.setContentView(view)
        view.measure(exact(size.width), exact(size.height)); view.layout(0, 0, size.width, size.height)
        val bitmap = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))

        val left = geometry.tileRect(Direction.LEFT, size)
        val up = geometry.tileRect(Direction.UP, size)
        assertEquals(0xffffffff.toInt(), bitmap.getPixel((left.left + 6f).toInt(), left.centerY().toInt()))
        assertEquals(0xff174ea6.toInt(), bitmap.getPixel((up.left + 6f).toInt(), up.centerY().toInt()))
    }

    @Test @GraphicsMode(GraphicsMode.Mode.NATIVE) fun `visible popup follows a runtime theme change`() {
        RuntimeEnvironment.setQualifiers("notnight")
        val spec = KeyboardLayouts.layout(KeyboardMode.KANA).rows[0].keys[1]
        val size = geometry.windowSize(PopupKind.KANA, 0)
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val view = KeyboardPopupRenderView(activity, geometry)
        view.setLayerType(android.view.View.LAYER_TYPE_NONE, null)
        view.bind(PopupKind.KANA, spec, Direction.UP, emptyList(), 0, size, 7f, 7f)
        activity.setContentView(view)
        view.measure(exact(size.width), exact(size.height)); view.layout(0, 0, size.width, size.height)
        val up = geometry.tileRect(Direction.UP, size)

        RuntimeEnvironment.setQualifiers("night")
        view.dispatchConfigurationChanged(view.resources.configuration)
        val bitmap = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))

        assertEquals(0xffa8ceff.toInt(), bitmap.getPixel((up.left + 6f).toInt(), up.centerY().toInt()))
    }

    @Test fun `letter and modifier use separate source dimensions`() {
        assertEquals(PopupSize(72, 83), geometry.windowSize(PopupKind.LETTER, 0))
        assertEquals(PopupSize(104, 83), geometry.windowSize(PopupKind.MODIFIER, 0))
        assertEquals(PopupSize(122, 71), geometry.windowSize(PopupKind.ACCENT, 3))
        assertTrue(geometry.tileRect(Direction.CENTER, geometry.windowSize(PopupKind.KANA, 0)).width() == 50f)
    }

    @Test fun `voice punctuation uses a five direction cross popup with down selected`() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val anchor = View(activity)
        activity.setContentView(anchor)
        anchor.measure(exact(400), exact(220)); anchor.layout(0, 0, 400, 220)
        val controller = KeyboardPopupController(activity)
        val spec = KeyboardLayouts.layout(KeyboardMode.VOICE).rows.last().keys[2]

        assertEquals(PopupKind.KANA, controller.popupKindForTest(spec))
        controller.show(anchor, RectF(160f, 160f, 200f, 205f), spec, Direction.DOWN)
        val popup = requireNotNull(controller.popupForTest())
        assertEquals(geometry.windowSize(PopupKind.KANA, 0).width, popup.width)
        assertEquals(geometry.windowSize(PopupKind.KANA, 0).height, popup.height)
        val renderer = popup.contentView as KeyboardPopupRenderView
        assertEquals(PopupKind.KANA, renderer.kindForTest())
        assertEquals(
            mapOf(
                Direction.CENTER to "、",
                Direction.LEFT to "。",
                Direction.UP to "？",
                Direction.RIGHT to "！",
                Direction.DOWN to "、",
            ),
            renderer.tileLabelsForTest(),
        )
        assertEquals(Direction.DOWN, renderer.selectedDirectionForTest())
    }

    @Test fun `attached popup updates one window then dismisses on detach`() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val anchor = View(activity)
        activity.setContentView(anchor)
        anchor.measure(exact(400), exact(220)); anchor.layout(0, 0, 400, 220)
        val controller = KeyboardPopupController(activity)
        val spec = KeyboardLayouts.layout(KeyboardMode.KANA).rows[0].keys[1]

        controller.show(anchor, RectF(80f, 60f, 130f, 111f), spec, Direction.CENTER)
        val first = controller.popupForTest()
        assertTrue(controller.isShowingForTest())
        controller.show(anchor, RectF(80f, 60f, 130f, 111f), spec, Direction.UP)
        assertSame(first, controller.popupForTest())
        activity.setContentView(View(activity))
        assertFalse(controller.isShowingForTest())
    }

    @Test @Config(sdk = [28]) fun `api 28 can show and dismiss without screen clipping API`() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val anchor = View(activity)
        activity.setContentView(anchor)
        anchor.measure(exact(400), exact(220)); anchor.layout(0, 0, 400, 220)
        val controller = KeyboardPopupController(activity)
        val spec = KeyboardLayouts.layout(KeyboardMode.KANA).rows[0].keys[1]

        controller.show(anchor, RectF(80f, 60f, 130f, 111f), spec, Direction.CENTER)
        assertTrue(controller.isShowingForTest())
        controller.dismiss()
        assertFalse(controller.isShowingForTest())
    }

    private fun exact(value: Int) = android.view.View.MeasureSpec.makeMeasureSpec(value, android.view.View.MeasureSpec.EXACTLY)
}
