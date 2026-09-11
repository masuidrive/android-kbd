package com.masuidrive.gestureime

import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SetupActivitySlashCommandsTest {
    @Test
    fun setupShowsSixEditableSlotsAndSavePersistsNormalizedCommands() {
        val context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("gesture_ime_preferences", 0).edit().clear().commit()
        val activity = Robolectric.buildActivity(SetupActivity::class.java).setup().get()
        val root = activity.findViewById<View>(android.R.id.content)
        val inputs = root.descendants().filterIsInstance<EditText>()

        assertEquals(6, inputs.size)
        assertEquals(listOf("/compact", "/clear", "/quit", "", "", ""), inputs.map { it.text.toString() })

        inputs[0].setText("review")
        inputs[1].setText("/ship")
        root.descendants().filterIsInstance<Button>()
            .single { it.text.toString() == "スラッシュ候補を保存" }
            .performClick()

        assertEquals(listOf("/review", "/ship", "/quit", "", "", ""), ImePreferences.getSlashCommands(activity))
        assertEquals("/review", inputs[0].text.toString())
        assertNotNull(root)
    }

    private fun View.descendants(): List<View> {
        val result = mutableListOf(this)
        if (this is ViewGroup) repeat(childCount) { result += getChildAt(it).descendants() }
        return result
    }
}
