package com.masuidrive.gestureime

import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import com.masuidrive.gestureime.keyboard.QwertyLabelGroup
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class QwertyLabelAdjustmentActivityTest {
    @Before fun clearPreferences() {
        RuntimeEnvironment.getApplication().getSharedPreferences("gesture_ime_preferences", 0).edit().clear().commit()
    }

    @Test fun `slider changes save selected group and reset restores defaults`() {
        val activity = Robolectric.buildActivity(QwertyLabelAdjustmentActivity::class.java).setup().get()
        val root = activity.findViewById<ViewGroup>(android.R.id.content)
        val spinner = root.descendants().filterIsInstance<Spinner>().single()
        val bars = root.descendants().filterIsInstance<SeekBar>().toList()

        bars[0].setProgress(60, true)
        bars[1].setProgress(0, true)
        bars[2].setProgress(16, true)

        assertEquals(QwertyLabelGroup.LETTER_PRIMARY.ordinal, spinner.selectedItemPosition)
        val saved = ImePreferences.getQwertyLabelStyle(activity)[QwertyLabelGroup.LETTER_PRIMARY]
        assertEquals(1.3f, saved.scale)
        assertEquals(-6f, saved.xOffsetDp)
        assertEquals(8f, saved.yOffsetDp)

        root.descendants().filterIsInstance<Button>().single { it.text == "初期値に戻して保存" }.performClick()
        assertEquals(1f, ImePreferences.getQwertyLabelStyle(activity)[QwertyLabelGroup.LETTER_PRIMARY].scale)
    }

    @Test fun `wide preview uses actual width and controls remain present`() {
        val activity = Robolectric.buildActivity(QwertyLabelAdjustmentActivity::class.java).setup().get()
        val root = activity.findViewById<ViewGroup>(android.R.id.content)
        root.descendants().filterIsInstance<Button>().single { it.text == "広い 840" }.performClick()
        val preview = root.descendants().filterIsInstance<com.masuidrive.gestureime.keyboard.KeyboardView>().single()

        assertEquals((840 * activity.resources.displayMetrics.density).toInt(), preview.layoutParams.width)
        assertEquals(3, root.descendants().count { it is SeekBar })
        assertEquals(1, root.descendants().count { it is TextView && it.text.toString().contains("自動保存") })
    }

    private fun View.descendants(): Sequence<View> = sequence {
        yield(this@descendants)
        if (this@descendants is ViewGroup) repeat(childCount) { yieldAll(getChildAt(it).descendants()) }
    }
}
