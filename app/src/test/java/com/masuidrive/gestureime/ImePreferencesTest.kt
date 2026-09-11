package com.masuidrive.gestureime

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import com.masuidrive.gestureime.keyboard.KeyboardMode
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ImePreferencesTest {
    @Test
    fun lastKeyboardModeDefaultsToQwertyAndPersistsEveryMode() {
        val context = RuntimeEnvironment.getApplication()
        assertEquals(KeyboardMode.QWERTY, ImePreferences.getLastKeyboardMode(context))

        KeyboardMode.entries.forEach { mode ->
            ImePreferences.setLastKeyboardMode(context, mode)
            assertEquals(mode, ImePreferences.getLastKeyboardMode(context))
        }
    }

    @Test
    fun malformedLastKeyboardModeFallsBackToQwerty() {
        val context = RuntimeEnvironment.getApplication()
        val preferences = context.getSharedPreferences("gesture_ime_preferences", 0)
        preferences.edit().putString("last_keyboard_mode", "UNKNOWN").apply()
        assertEquals(KeyboardMode.QWERTY, ImePreferences.getLastKeyboardMode(context))

        preferences.edit().putInt("last_keyboard_mode", 3).apply()
        assertEquals(KeyboardMode.QWERTY, ImePreferences.getLastKeyboardMode(context))
    }

    @Test
    fun dualFlickDefaultsOffAndPersists() {
        val context = RuntimeEnvironment.getApplication()
        assertFalse(ImePreferences.isDualFlickEnabled(context))

        ImePreferences.setDualFlickEnabled(context, true)

        assertTrue(ImePreferences.isDualFlickEnabled(context))
        ImePreferences.setDualFlickEnabled(context, false)
    }

    @Test
    fun englishSuggestionsDefaultOffPersistAndRejectMalformedValue() {
        val context = RuntimeEnvironment.getApplication()
        assertFalse(ImePreferences.isEnglishSuggestionsEnabled(context))

        ImePreferences.setEnglishSuggestionsEnabled(context, true)
        assertTrue(ImePreferences.isEnglishSuggestionsEnabled(context))

        context.getSharedPreferences("gesture_ime_preferences", 0).edit()
            .putString("english_suggestions_enabled", "bad")
            .apply()
        assertFalse(ImePreferences.isEnglishSuggestionsEnabled(context))
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

    @Test
    fun malformedStoredQwertyValuesFallBackWithoutCrashing() {
        val context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("gesture_ime_preferences", 0).edit()
            .putString("qwerty_label_letter_primary_scale", "bad")
            .putBoolean("qwerty_label_letter_primary_x", true)
            .putInt("qwerty_label_letter_primary_y", 9)
            .apply()
        val group = com.masuidrive.gestureime.keyboard.QwertyLabelGroup.LETTER_PRIMARY
        assertEquals(
            com.masuidrive.gestureime.keyboard.QwertyLabelStyle.DEFAULT[group],
            ImePreferences.getQwertyLabelStyle(context)[group],
        )
    }
}
