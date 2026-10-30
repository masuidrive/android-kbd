package com.masuidrive.gestureime

import android.text.InputType
import org.junit.Assert.assertEquals
import org.junit.Test

class PersonalInfoSuggestionsTest {
    @Test
    fun recognizesOnlyExplicitTextVariations() {
        assertEquals(
            PersonalInfoField.NAME,
            personalInfoFieldForInputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PERSON_NAME),
        )
        assertEquals(
            PersonalInfoField.EMAIL,
            personalInfoFieldForInputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS),
        )
        assertEquals(
            PersonalInfoField.EMAIL,
            personalInfoFieldForInputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS),
        )
        assertEquals(
            PersonalInfoField.ADDRESS,
            personalInfoFieldForInputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS),
        )
        assertEquals(null, personalInfoFieldForInputType(InputType.TYPE_CLASS_TEXT))
        assertEquals(null, personalInfoFieldForInputType(InputType.TYPE_CLASS_NUMBER))
    }
}
