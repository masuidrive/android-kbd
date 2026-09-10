package com.masuidrive.gestureime.conversion

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MozcConversionEngineTest {
    @Test
    fun rejectsEmptyAndMinimalEngineDataVersions() {
        assertFalse(MozcConversionEngine.isUsableDataVersion(""))
        assertFalse(MozcConversionEngine.isUsableDataVersion("0.0.0"))
        assertTrue(MozcConversionEngine.isUsableDataVersion("1.2.3"))
    }
}
