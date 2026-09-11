package com.masuidrive.gestureime

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ImePreferencesTest {
    @Test
    fun dualFlickDefaultsOffAndPersists() {
        val context = RuntimeEnvironment.getApplication()
        assertFalse(ImePreferences.isDualFlickEnabled(context))

        ImePreferences.setDualFlickEnabled(context, true)

        assertTrue(ImePreferences.isDualFlickEnabled(context))
        ImePreferences.setDualFlickEnabled(context, false)
    }

    @Test
    fun qwertyStylePersistsSanitizesAndResets() {
        val context = RuntimeEnvironment.getApplication()
        val group = com.masuidrive.gestureime.keyboard.QwertyLabelGroup.LETTER_PRIMARY
        val changed = com.masuidrive.gestureime.keyboard.QwertyLabelStyle.DEFAULT.with(
            group,
            com.masuidrive.gestureime.keyboard.LabelAdjustment(2f, 99f, -99f),
        )

        ImePreferences.setQwertyLabelStyle(context, changed)
        assertEquals(1.3f, ImePreferences.getQwertyLabelStyle(context)[group].scale, 0f)
        assertEquals(6f, ImePreferences.getQwertyLabelStyle(context)[group].xOffsetDp, 0f)
        assertEquals(-8f, ImePreferences.getQwertyLabelStyle(context)[group].yOffsetDp, 0f)

        ImePreferences.resetQwertyLabelStyle(context)
        assertEquals(
            com.masuidrive.gestureime.keyboard.QwertyLabelStyle.DEFAULT[group],
            ImePreferences.getQwertyLabelStyle(context)[group],
        )
    }
}
