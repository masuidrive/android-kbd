package com.masuidrive.gestureime

import android.text.InputType
import android.view.inputmethod.EditorInfo
import com.masuidrive.gestureime.keyboard.KeyboardMode
import org.junit.Assert.assertEquals
import org.junit.Test

class EditorKeyboardModeSelectorTest {
    @Test
    fun numberPhoneAndDatetimeClassesUseNumbersForEverySupportedVariation() {
        val numericTypes = listOf(
            InputType.TYPE_CLASS_NUMBER,
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED,
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL,
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD,
            InputType.TYPE_CLASS_PHONE,
            InputType.TYPE_CLASS_DATETIME,
            InputType.TYPE_CLASS_DATETIME or InputType.TYPE_DATETIME_VARIATION_DATE,
            InputType.TYPE_CLASS_DATETIME or InputType.TYPE_DATETIME_VARIATION_TIME,
        )

        numericTypes.forEach { inputType ->
            assertEquals(
                "inputType=$inputType",
                KeyboardMode.NUMBERS,
                selectInitialKeyboardMode(inputType, 0, KeyboardMode.KANA),
            )
        }
    }

    @Test
    fun addressAndPasswordTextVariationsUseQwerty() {
        val variations = listOf(
            InputType.TYPE_TEXT_VARIATION_URI,
            InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
            InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS,
            InputType.TYPE_TEXT_VARIATION_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
        )

        variations.forEach { variation ->
            assertEquals(
                "variation=$variation",
                KeyboardMode.QWERTY,
                selectInitialKeyboardMode(InputType.TYPE_CLASS_TEXT or variation, 0, KeyboardMode.KANA),
            )
        }
    }

    @Test
    fun forceAsciiUsesQwertyRegardlessOfInputClass() {
        assertEquals(
            KeyboardMode.QWERTY,
            selectInitialKeyboardMode(
                InputType.TYPE_CLASS_NUMBER,
                EditorInfo.IME_FLAG_FORCE_ASCII,
                KeyboardMode.KANA,
            ),
        )
    }

    @Test
    fun ordinaryUnspecifiedAndUnknownTypesKeepLastExplicitMode() {
        val fallbackTypes = listOf(
            InputType.TYPE_NULL,
            InputType.TYPE_CLASS_TEXT,
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PERSON_NAME,
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS,
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE,
            0x0f,
        )

        fallbackTypes.forEach { inputType ->
            assertEquals(
                "inputType=$inputType",
                KeyboardMode.SYMBOLS,
                selectInitialKeyboardMode(inputType, 0, KeyboardMode.SYMBOLS),
            )
        }
    }
}
