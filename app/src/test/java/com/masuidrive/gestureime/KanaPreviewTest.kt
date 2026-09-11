package com.masuidrive.gestureime

import org.junit.Assert.assertEquals
import org.junit.Test

class KanaPreviewTest {
    @Test
    fun hiraganaReadingConvertsToKatakanaWithoutChangingOtherCharacters() {
        assertEquals("ニホンゴー", "にほんごー".toKatakana())
    }
}
