package com.masuidrive.gestureime

import android.text.InputType
import android.widget.EditText
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PersonalInfoInputTypeInstrumentedTest {
    @Test
    fun testScreenExposesExplicitPersonalInfoInputTypesToTheIme() {
        ActivityScenario.launch(ImeTestActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertEquals(
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PERSON_NAME,
                    activity.findViewById<EditText>(R.id.ime_test_name_field).inputType,
                )
                assertEquals(
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS,
                    activity.findViewById<EditText>(R.id.ime_test_address_field).inputType,
                )
            }
        }
    }
}
