package com.masuidrive.gestureime

import android.content.Context

object ImePreferences {
    private const val FILE_NAME = "gesture_ime_preferences"
    private const val DUAL_FLICK = "dual_flick_enabled"

    fun isDualFlickEnabled(context: Context): Boolean =
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
            .getBoolean(DUAL_FLICK, false)

    fun setDualFlickEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(DUAL_FLICK, enabled)
            .apply()
    }
}
