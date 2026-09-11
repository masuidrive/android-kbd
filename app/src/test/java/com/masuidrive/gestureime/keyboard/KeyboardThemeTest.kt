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
import com.masuidrive.gestureime.R

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class KeyboardThemeTest {
    @Test @Config(qualifiers = "notnight") fun lightThemeUsesReferenceKeyboardBackground() {
        val activity = activity()
        assertEquals(0xffd3d5db.toInt(), renderBackground(activity))
        assertEquals(0xffffffff.toInt(), activity.getColor(R.color.keyboard_key))
        assertEquals(0xffaeb3bd.toInt(), activity.getColor(R.color.keyboard_special))
        assertEquals(0xff19191b.toInt(), activity.getColor(R.color.keyboard_text))
        assertEquals(0xff174ea6.toInt(), activity.getColor(R.color.keyboard_selected))
        assertEquals(0xffffffff.toInt(), activity.getColor(R.color.keyboard_selected_text))
        assertEquals(0xff898c94.toInt(), activity.getColor(R.color.keyboard_shadow))
    }

    @Test @Config(qualifiers = "night") fun darkThemeKeepsExistingKeyboardBackground() {
        val activity = activity()
        assertEquals(0xff29292c.toInt(), renderBackground(activity))
        assertEquals(0xff414144.toInt(), activity.getColor(R.color.keyboard_key))
        assertEquals(0xff303034.toInt(), activity.getColor(R.color.keyboard_special))
        assertEquals(0xfff4f4f6.toInt(), activity.getColor(R.color.keyboard_text))
        assertEquals(0xffa8ceff.toInt(), activity.getColor(R.color.keyboard_selected))
        assertEquals(0xff102844.toInt(), activity.getColor(R.color.keyboard_selected_text))
        assertEquals(0xff141416.toInt(), activity.getColor(R.color.keyboard_shadow))
    }

    private fun activity() = Robolectric.buildActivity(Activity::class.java).setup().get()

    private fun renderBackground(activity: Activity): Int {
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
