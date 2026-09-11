package com.masuidrive.gestureime

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ImePreferencesTest {
    @Test
    fun dualFlickDefaultsOffAndPersists() {
        val context = RuntimeEnvironment.getApplication()
        assertFalse(ImePreferences.isDualFlickEnabled(context))

        ImePreferences.setDualFlickEnabled(context, true)

        assertTrue(ImePreferences.isDualFlickEnabled(context))
        ImePreferences.setDualFlickEnabled(context, false)
    }
}
