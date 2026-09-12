package com.masuidrive.gestureime

import android.content.Context
import com.masuidrive.gestureime.keyboard.KeyboardHeightPreset
import com.masuidrive.gestureime.keyboard.KeyboardMode

object ImePreferences {
    private const val FILE_NAME = "gesture_ime_preferences"
    private const val DUAL_FLICK = "dual_flick_enabled"
    private const val TERMINAL_CURSOR = "terminal_cursor_key_events"
    private const val LAST_KEYBOARD_MODE = "last_keyboard_mode"
    private const val ENGLISH_SUGGESTIONS = "english_suggestions_enabled"
    private const val ANDROID_USER_DICTIONARY = "android_user_dictionary_enabled"
    private const val KEYBOARD_HEIGHT_PRESET = "keyboard_height_preset"
    private const val EMOJI_RECENT_PREFIX = "emoji_recent_"
    private const val SLASH_COMMAND_PREFIX = "slash_command_"
    const val SLASH_COMMAND_SLOTS = 6
    const val EMOJI_RECENT_LIMIT = 100
    val DEFAULT_SLASH_COMMANDS = listOf("/compact", "/clear", "/quit", "", "", "")

    fun isEnglishSuggestionsEnabled(context: Context): Boolean = runCatching {
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
            .getBoolean(ENGLISH_SUGGESTIONS, true)
    }.getOrDefault(false)

    fun setEnglishSuggestionsEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(ENGLISH_SUGGESTIONS, enabled)
            .apply()
    }

    fun isAndroidUserDictionaryEnabled(context: Context): Boolean = runCatching {
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
            .getBoolean(ANDROID_USER_DICTIONARY, true)
    }.getOrDefault(false)

    fun setAndroidUserDictionaryEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(ANDROID_USER_DICTIONARY, enabled)
            .apply()
    }

    fun getSlashCommands(context: Context): List<String> {
        val preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
        return List(SLASH_COMMAND_SLOTS) { index ->
            val fallback = DEFAULT_SLASH_COMMANDS[index]
            val stored = runCatching {
                if (preferences.contains("$SLASH_COMMAND_PREFIX$index")) {
                    preferences.getString("$SLASH_COMMAND_PREFIX$index", fallback)
                } else fallback
            }.getOrDefault(fallback)
            normalizeSlashCommand(stored.orEmpty())
        }
    }

    fun getSlashCommandCandidates(context: Context): List<String> =
        getSlashCommands(context).filter(String::isNotEmpty).distinct()

    fun setSlashCommands(context: Context, commands: List<String>) {
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE).edit().apply {
            repeat(SLASH_COMMAND_SLOTS) { index ->
                putString("$SLASH_COMMAND_PREFIX$index", normalizeSlashCommand(commands.getOrNull(index).orEmpty()))
            }
        }.apply()
    }
    fun isTerminalCursorEnabled(context: Context) = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE).getBoolean(TERMINAL_CURSOR, false)
    fun setTerminalCursorEnabled(context: Context, enabled: Boolean) { context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE).edit().putBoolean(TERMINAL_CURSOR, enabled).apply() }

    fun isDualFlickEnabled(context: Context): Boolean =
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
            .getBoolean(DUAL_FLICK, true)

    fun setDualFlickEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(DUAL_FLICK, enabled)
            .apply()
    }

    fun getKeyboardHeightPreset(context: Context): KeyboardHeightPreset {
        val stored = runCatching {
            context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
                .getString(KEYBOARD_HEIGHT_PRESET, null)
        }.getOrNull()
        return stored?.let { value -> KeyboardHeightPreset.entries.firstOrNull { it.name == value } }
            ?: KeyboardHeightPreset.STANDARD
    }

    fun setKeyboardHeightPreset(context: Context, preset: KeyboardHeightPreset) {
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEYBOARD_HEIGHT_PRESET, preset.name)
            .apply()
    }

    fun getLastKeyboardMode(context: Context): KeyboardMode {
        val stored = runCatching {
            context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
                .getString(LAST_KEYBOARD_MODE, null)
        }.getOrNull()
        return if (stored == "CURSOR") KeyboardMode.KANA
        else stored?.let { value -> KeyboardMode.entries.firstOrNull { it.name == value } }
            ?: KeyboardMode.QWERTY
    }

    fun setLastKeyboardMode(context: Context, mode: KeyboardMode) {
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(LAST_KEYBOARD_MODE, mode.name)
            .apply()
    }

    fun getEmojiRecents(context: Context): List<String> {
        val preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
        return List(EMOJI_RECENT_LIMIT) { index ->
            runCatching { preferences.getString("$EMOJI_RECENT_PREFIX$index", "") }.getOrDefault("").orEmpty()
        }.filter(String::isNotEmpty).distinct().take(EMOJI_RECENT_LIMIT)
    }

    fun recordEmojiRecent(context: Context, emoji: String): List<String> {
        val recents = (listOf(emoji) + getEmojiRecents(context).filter { it != emoji }).take(EMOJI_RECENT_LIMIT)
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE).edit().apply {
            repeat(EMOJI_RECENT_LIMIT) { index -> putString("$EMOJI_RECENT_PREFIX$index", recents.getOrElse(index) { "" }) }
        }.apply()
        return recents
    }

    private fun normalizeSlashCommand(value: String): String {
        val trimmed = value.trim()
        return when {
            trimmed.isEmpty() -> ""
            trimmed.startsWith('/') -> trimmed
            else -> "/$trimmed"
        }
    }
}
