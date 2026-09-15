package com.masuidrive.gestureime

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.masuidrive.gestureime.keyboard.KeyboardView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.roundToInt

@RunWith(AndroidJUnit4::class)
class EmojiPickerBodyLayoutInstrumentedTest {
    @Test
    fun nativeLayoutMovesBodyFromPhoneToWideContentBounds() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val context = instrumentation.targetContext
            val keyboard = KeyboardView(context)
            val body = RecyclerView(context)
            val content = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                addView(body, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 200))
            }
            val density = context.resources.displayMetrics.density
            val phoneWidth = (412 * density).roundToInt()
            val wideWidth = (840 * density).roundToInt()

            val phone = keyboard.emojiLayerHorizontalGeometryForWidth(phoneWidth)
            assertTrue(applyEmojiPickerBodyHorizontalLayout(content, body, phoneWidth, phone))
            content.measure(exact(phoneWidth), exact(200))
            content.layout(0, 0, phoneWidth, 200)
            assertEquals(phone.railRight, body.left)
            assertEquals(phone.bodyWidth, body.width)
            assertEquals(phone.contentRight, body.right)

            val wide = keyboard.emojiLayerHorizontalGeometryForWidth(wideWidth)
            assertTrue(applyEmojiPickerBodyHorizontalLayout(content, body, wideWidth, wide))
            content.measure(exact(wideWidth), exact(200))
            content.layout(0, 0, wideWidth, 200)
            assertEquals(wide.railRight, body.left)
            assertEquals(wide.bodyWidth, body.width)
            assertEquals(wide.contentRight, body.right)
        }
    }

    private fun exact(size: Int) = View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY)
}
