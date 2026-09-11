package com.masuidrive.gestureime

import android.content.Context
import com.masuidrive.gestureime.keyboard.LabelAdjustment
import com.masuidrive.gestureime.keyboard.QwertyLabelGroup
import com.masuidrive.gestureime.keyboard.QwertyLabelStyle

object ImePreferences {
    private const val FILE_NAME = "gesture_ime_preferences"
    private const val DUAL_FLICK = "dual_flick_enabled"
    private const val LABEL_PREFIX = "qwerty_label_"
    private const val TERMINAL_CURSOR = "terminal_cursor_key_events"
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
}
