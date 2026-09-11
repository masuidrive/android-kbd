package com.masuidrive.gestureime

import android.content.Context
import com.masuidrive.gestureime.keyboard.LabelAdjustment
import com.masuidrive.gestureime.keyboard.KeyboardMode
import com.masuidrive.gestureime.keyboard.QwertyLabelGroup
import com.masuidrive.gestureime.keyboard.QwertyLabelStyle

object ImePreferences {
    private const val FILE_NAME = "gesture_ime_preferences"
    private const val DUAL_FLICK = "dual_flick_enabled"
    private const val LABEL_PREFIX = "qwerty_label_"
    private const val TERMINAL_CURSOR = "terminal_cursor_key_events"
    private const val LAST_KEYBOARD_MODE = "last_keyboard_mode"
    private const val ENGLISH_SUGGESTIONS = "english_suggestions_enabled"
    private const val ANDROID_USER_DICTIONARY = "android_user_dictionary_enabled"
    private const val SLASH_COMMAND_PREFIX = "slash_command_"
    const val SLASH_COMMAND_SLOTS = 6
    val DEFAULT_SLASH_COMMANDS = listOf("/compact", "/clear", "/quit", "", "", "")

    fun isEnglishSuggestionsEnabled(context: Context): Boolean = runCatching {
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
            .getBoolean(ENGLISH_SUGGESTIONS, false)
    }.getOrDefault(false)

    fun setEnglishSuggestionsEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(ENGLISH_SUGGESTIONS, enabled)
            .apply()
    }

    fun isAndroidUserDictionaryEnabled(context: Context): Boolean = runCatching {
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
            .getBoolean(ANDROID_USER_DICTIONARY, false)
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
            .getBoolean(DUAL_FLICK, false)

    fun setDualFlickEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(DUAL_FLICK, enabled)
            .apply()
    }

    fun getLastKeyboardMode(context: Context): KeyboardMode {
        val stored = runCatching {
            context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
                .getString(LAST_KEYBOARD_MODE, null)
        }.getOrNull()
        return stored?.let { value -> KeyboardMode.entries.firstOrNull { it.name == value } }
            ?: KeyboardMode.QWERTY
    }

    fun setLastKeyboardMode(context: Context, mode: KeyboardMode) {
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(LAST_KEYBOARD_MODE, mode.name)
            .apply()
    }

    fun getQwertyLabelStyle(context: Context): QwertyLabelStyle {
        val preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
        var style = QwertyLabelStyle.DEFAULT
        QwertyLabelGroup.entries.forEach { group ->
            val fallback = QwertyLabelStyle.DEFAULT[group]
            style = style.with(
                group,
                LabelAdjustment(
                    scale = runCatching { preferences.getFloat(key(group, "scale"), fallback.scale) }.getOrDefault(fallback.scale),
                    xOffsetDp = runCatching { preferences.getFloat(key(group, "x"), fallback.xOffsetDp) }.getOrDefault(fallback.xOffsetDp),
                    yOffsetDp = runCatching { preferences.getFloat(key(group, "y"), fallback.yOffsetDp) }.getOrDefault(fallback.yOffsetDp),
                ),
            )
        }
        return style.sanitized()
    }

    fun setQwertyLabelStyle(context: Context, style: QwertyLabelStyle) {
        val safeStyle = style.sanitized()
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE).edit().apply {
            QwertyLabelGroup.entries.forEach { group ->
                val adjustment = safeStyle[group]
                putFloat(key(group, "scale"), adjustment.scale)
                putFloat(key(group, "x"), adjustment.xOffsetDp)
                putFloat(key(group, "y"), adjustment.yOffsetDp)
            }
        }.apply()
    }

    fun resetQwertyLabelStyle(context: Context) {
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE).edit().apply {
            QwertyLabelGroup.entries.forEach { group ->
                remove(key(group, "scale"))
                remove(key(group, "x"))
                remove(key(group, "y"))
            }
        }.apply()
    }

    private fun key(group: QwertyLabelGroup, field: String): String =
        "$LABEL_PREFIX${group.name.lowercase()}_$field"

    private fun normalizeSlashCommand(value: String): String {
        val trimmed = value.trim()
        return when {
            trimmed.isEmpty() -> ""
            trimmed.startsWith('/') -> trimmed
            else -> "/$trimmed"
        }
    }
}
