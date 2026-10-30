package com.masuidrive.gestureime

import android.text.InputType

enum class PersonalInfoField {
    NAME,
    NAME_HIRAGANA,
    NAME_KATAKANA,
    NAME_LATIN,
    EMAIL,
    ADDRESS,
    POSTAL_CODE,
    PREFECTURE,
    CITY,
    ADDRESS_LINE1,
    ADDRESS_LINE2,
}

data class PersonalInfoSuggestion(
    val field: PersonalInfoField,
    val value: String,
)

fun personalInfoFieldForInputType(inputType: Int): PersonalInfoField? = when (
    inputType and InputType.TYPE_MASK_CLASS
) {
    InputType.TYPE_CLASS_TEXT -> when (inputType and InputType.TYPE_MASK_VARIATION) {
        InputType.TYPE_TEXT_VARIATION_PERSON_NAME -> PersonalInfoField.NAME
        InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
        InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS -> PersonalInfoField.EMAIL
        InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS -> PersonalInfoField.ADDRESS
        else -> null
    }
    else -> null
}
