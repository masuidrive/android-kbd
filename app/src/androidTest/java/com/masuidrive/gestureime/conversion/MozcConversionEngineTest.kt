package com.masuidrive.gestureime.conversion

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MozcConversionEngineTest {
    @Test
    fun kanaProducesJapaneseCandidateAndCommitsIt() = runBlocking {
        val engine = MozcConversionEngine(ApplicationProvider.getApplicationContext())
        val initial = engine.update("にほんご")
        assertEquals("にほんご", initial.reading)
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
        val initial = engine.update(reading)
        assertEquals(reading, initial.reading)
        val state = engine.nextCandidate()

        assertTrue("Mozc returned no candidates", state.candidates.isNotEmpty())
        val committed = requireNotNull(engine.commit(state.selectedIndex)).value
        assertTrue("Mozc dropped the later segment: $committed", committed.contains("天気"))
        assertTrue("Mozc dropped the sentence ending: $committed", committed.endsWith("です"))
        engine.reset()
    }

    @Test
    fun suggestionCommitKeepsTheWholeLongReading() = runBlocking {
        val reading = "きょうはいいてんきです"
        val engine = MozcConversionEngine(ApplicationProvider.getApplicationContext())
        val state = engine.update(reading)

        assertTrue("Mozc returned no suggestion candidates", state.candidates.isNotEmpty())
        val committed = requireNotNull(engine.commit(0)).value
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

    @Test
    fun deletingMozcCandidateHistoryKeepsTheActiveReading() = runBlocking {
        val engine = MozcConversionEngine(ApplicationProvider.getApplicationContext(), userDictionaryEnabled = { false })
        val state = engine.update("にほんご")
        val index = state.candidates.indexOfFirst { it.source == ConversionCandidateSource.MOZC }
        assertTrue("Mozc returned no deletable candidate", index >= 0)

        val updated = requireNotNull(engine.deleteCandidateFromHistory(index))

        assertEquals("にほんご", updated.reading)
        assertTrue("Mozc removed the complete candidate window", updated.candidates.isNotEmpty())
        engine.reset()
    }

    @Test
    fun committedCandidateIsLearnedAndDeletingHistoryRestoresItsRank() = runBlocking {
        val engine = MozcConversionEngine(ApplicationProvider.getApplicationContext(), userDictionaryEnabled = { false })
        val reading = "にほんご"
        val initial = engine.update(reading)
        val learnedValue = initial.candidates.drop(1).firstOrNull {
            it.source == ConversionCandidateSource.MOZC
        }?.value
        requireNotNull(learnedValue) {
            "Mozc returned fewer than two candidates: ${initial.candidates.map { it.value }}"
        }

        repeat(3) {
            val state = engine.update(reading)
            val learnedIndex = state.candidates.indexOfFirst { it.value == learnedValue }
            assertTrue("Mozc no longer returned $learnedValue", learnedIndex >= 0)
            assertEquals(learnedValue, requireNotNull(engine.commit(learnedIndex)).value)
        }

        val learned = engine.update(reading)
        assertEquals(learnedValue, learned.candidates.first().value)
        val learnedIndex = learned.candidates.indexOfFirst { it.value == learnedValue }
        requireNotNull(engine.deleteCandidateFromHistory(learnedIndex))

        val restored = engine.update(reading)
        assertNotEquals(learnedValue, restored.candidates.first().value)
        engine.reset()
    }

}
