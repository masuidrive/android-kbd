package com.masuidrive.gestureime.conversion

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.mozc.android.inputmethod.japanese.protobuf.ProtoConfig
import org.junit.Test

class MozcConversionEngineTest {
    @Test
    fun rejectsEmptyAndMinimalEngineDataVersions() {
        assertFalse(MozcConversionEngine.isUsableDataVersion(""))
        assertFalse(MozcConversionEngine.isUsableDataVersion("0.0.0"))
        assertTrue(MozcConversionEngine.isUsableDataVersion("1.2.3"))
    }

    @Test
    fun normalConversionUsesPersistentMozcHistory() {
        val config = MozcConversionEngine.learningConfig()

        assertFalse(config.incognitoMode)
        assertEquals(ProtoConfig.Config.HistoryLearningLevel.DEFAULT_HISTORY, config.historyLearningLevel)
    }

    @Test
    fun userDictionaryCandidatesPrecedeMozcAndKeepTheMozcSelection() {
        val state = ConversionState(
            reading = "こうほ",
            candidates = listOf(ConversionCandidate(7, "Mozc候補")),
            selectedIndex = 0,
        )

        val merged = MozcConversionEngine.mergeAndroidUserDictionaryCandidates(
            state,
            listOf("個人候補", "個人候補", "Mozc候補"),
        )

        assertEquals(listOf("個人候補", "Mozc候補"), merged.candidates.map { it.value })
        assertEquals(ConversionCandidateSource.ANDROID_USER_DICTIONARY, merged.candidates.first().source)
        assertEquals(1, merged.selectedIndex)
    }
}
