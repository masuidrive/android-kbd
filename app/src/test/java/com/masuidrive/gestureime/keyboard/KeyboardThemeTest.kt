package com.masuidrive.gestureime.keyboard

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class KeyboardThemeTest {
    @Test @Config(qualifiers = "notnight") fun lightThemeUsesReferenceKeyboardBackground() {
        assertEquals(0xffd3d5db.toInt(), renderBackground())
    }

    @Test @Config(qualifiers = "night") fun darkThemeKeepsExistingKeyboardBackground() {
        assertEquals(0xff29292c.toInt(), renderBackground())
    }

    private fun renderBackground(): Int {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val view = KeyboardView(activity)
        activity.setContentView(view)
        view.measure(exact(412), exact(220))
        view.layout(0, 0, 412, 220)
        val bitmap = Bitmap.createBitmap(412, 220, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        return bitmap.getPixel(0, 0)
    }

    private fun exact(value: Int) = android.view.View.MeasureSpec.makeMeasureSpec(
        value,
        android.view.View.MeasureSpec.EXACTLY,
    )
}
