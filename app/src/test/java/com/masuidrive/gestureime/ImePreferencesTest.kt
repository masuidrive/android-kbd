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
    fun androidUserDictionaryDefaultsOffAndPersistsExplicitConsent() {
        val context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("gesture_ime_preferences", 0).edit()
            .remove("android_user_dictionary_enabled")
            .apply()
        assertFalse(ImePreferences.isAndroidUserDictionaryEnabled(context))

        ImePreferences.setAndroidUserDictionaryEnabled(context, true)
        assertTrue(ImePreferences.isAndroidUserDictionaryEnabled(context))

        context.getSharedPreferences("gesture_ime_preferences", 0).edit()
            .putString("android_user_dictionary_enabled", "bad")
            .apply()
        assertFalse(ImePreferences.isAndroidUserDictionaryEnabled(context))
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
