package com.masuidrive.gestureime

import android.text.InputType
import android.view.inputmethod.EditorInfo
import com.masuidrive.gestureime.keyboard.KeyboardMode

internal fun selectInitialKeyboardMode(
    inputType: Int,
    imeOptions: Int,
    lastExplicitMode: KeyboardMode,
): KeyboardMode {
    val inputClass = inputType and InputType.TYPE_MASK_CLASS
    if (inputClass == InputType.TYPE_CLASS_NUMBER ||
        inputClass == InputType.TYPE_CLASS_PHONE ||
        inputClass == InputType.TYPE_CLASS_DATETIME
    ) {
        return KeyboardMode.NUMBERS
    }

    if (imeOptions and EditorInfo.IME_FLAG_FORCE_ASCII != 0) return KeyboardMode.QWERTY

    if (inputClass != InputType.TYPE_CLASS_TEXT) return lastExplicitMode
    return when (inputType and InputType.TYPE_MASK_VARIATION) {
        InputType.TYPE_TEXT_VARIATION_URI,
        InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
        InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS,
        InputType.TYPE_TEXT_VARIATION_PASSWORD,
        InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
        InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
        -> KeyboardMode.QWERTY
        else -> lastExplicitMode
    }
}
