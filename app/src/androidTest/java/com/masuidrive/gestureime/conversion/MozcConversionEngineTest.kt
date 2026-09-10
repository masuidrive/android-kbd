package com.masuidrive.gestureime.conversion

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MozcConversionEngineTest {
    @Test
    fun kanaProducesJapaneseCandidateAndCommitsIt() = runBlocking {
        val engine = MozcConversionEngine(ApplicationProvider.getApplicationContext())
        engine.update("にほんご")
        val state = engine.nextCandidate()

        assertTrue("Mozc returned no candidates", state.candidates.isNotEmpty())
        val japaneseIndex = state.candidates.indexOfFirst { it.value == "日本語" }
        assertTrue("Mozc returned no 日本語 candidate", japaneseIndex >= 0)
        assertEquals("日本語", requireNotNull(engine.commit(japaneseIndex)).value)
        engine.reset()
    }

    @Test
    fun committingLongReadingDoesNotDropLaterSegments() = runBlocking {
        val reading = "きょうはいいてんきです"
        val engine = MozcConversionEngine(ApplicationProvider.getApplicationContext())
        engine.update(reading)
        val state = engine.nextCandidate()

        assertTrue("Mozc returned no candidates", state.candidates.isNotEmpty())
        val committed = requireNotNull(engine.commit(state.selectedIndex)).value
        assertTrue("Mozc dropped the later segment: $committed", committed.contains("天気"))
        assertTrue("Mozc dropped the sentence ending: $committed", committed.endsWith("です"))
        engine.reset()
    }

    @Test
    fun emptyUpdateClearsReadingAndSelection() = runBlocking {
        val engine = MozcConversionEngine(ApplicationProvider.getApplicationContext())
        engine.update("にほんご")

        val state = engine.update("")

        assertTrue(state.reading.isEmpty())
        assertTrue(state.candidates.isEmpty())
        assertEquals(-1, state.selectedIndex)
        engine.reset()
    }
}
