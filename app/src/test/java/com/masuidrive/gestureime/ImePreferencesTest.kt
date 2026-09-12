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
    fun nonTerminalInputSettingsDefaultOnButExplicitFalsePersists() {
        val context = RuntimeEnvironment.getApplication()
        val preferences = context.getSharedPreferences("gesture_ime_preferences", 0)
        preferences.edit().clear().commit()

        assertTrue(ImePreferences.isDualFlickEnabled(context))
        assertTrue(ImePreferences.isEnglishSuggestionsEnabled(context))
        assertTrue(ImePreferences.isAndroidUserDictionaryEnabled(context))
        assertFalse(ImePreferences.isTerminalCursorEnabled(context))

        ImePreferences.setDualFlickEnabled(context, false)
        ImePreferences.setEnglishSuggestionsEnabled(context, false)
        ImePreferences.setAndroidUserDictionaryEnabled(context, false)

        assertFalse(ImePreferences.isDualFlickEnabled(context))
        assertFalse(ImePreferences.isEnglishSuggestionsEnabled(context))
        assertFalse(ImePreferences.isAndroidUserDictionaryEnabled(context))
        assertTrue(preferences.contains("dual_flick_enabled"))
        assertTrue(preferences.contains("english_suggestions_enabled"))
        assertTrue(preferences.contains("android_user_dictionary_enabled"))
    }

    @Test
    fun malformedBooleanValuesUseSafeOffFallbackWithoutOverwritingStorage() {
        val context = RuntimeEnvironment.getApplication()
        val preferences = context.getSharedPreferences("gesture_ime_preferences", 0)
        preferences.edit()
            .putString("english_suggestions_enabled", "bad")
            .putString("android_user_dictionary_enabled", "bad")
            .apply()

        assertFalse(ImePreferences.isEnglishSuggestionsEnabled(context))
        assertFalse(ImePreferences.isAndroidUserDictionaryEnabled(context))
        assertEquals("bad", preferences.getString("english_suggestions_enabled", null))
        assertEquals("bad", preferences.getString("android_user_dictionary_enabled", null))
    }

    @Test
    fun slashCommandsDefaultToThreeValuesNormalizeSixSlotsAndHideDuplicates() {
        val context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("gesture_ime_preferences", 0).edit().clear().commit()
        assertEquals(
            listOf("/compact", "/clear", "/quit", "", "", ""),
            ImePreferences.getSlashCommands(context),
        )

        ImePreferences.setSlashCommands(
            context,
            listOf(" compact ", "/clear", "", "/clear", "quit", "  ", "/ignored"),
        )

        assertEquals(
            listOf("/compact", "/clear", "", "/clear", "/quit", ""),
            ImePreferences.getSlashCommands(context),
        )
        assertEquals(
            listOf("/compact", "/clear", "/quit"),
            ImePreferences.getSlashCommandCandidates(context),
        )
    }

}
