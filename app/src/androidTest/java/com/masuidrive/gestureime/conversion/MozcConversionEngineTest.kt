package com.masuidrive.gestureime.conversion

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MozcConversionEngineTest {
    @Test
    fun kanaProducesJapaneseCandidateAndCommitsIt() = runBlocking {
        val engine = MozcConversionEngine(ApplicationProvider.getApplicationContext())
        val state = engine.update("にほんご")

        assertTrue("Mozc returned no candidates", state.candidates.isNotEmpty())
        assertTrue(
            "Mozc returned no Japanese conversion candidate",
            state.candidates.any { it.value.any(Char::isLetter) },
        )
        assertNotNull(engine.commit(state.selectedIndex))
        engine.reset()
    }

    @Test
    fun committingLongReadingDoesNotDropLaterSegments() = runBlocking {
        val reading = "きょうはいいてんきです"
        val engine = MozcConversionEngine(ApplicationProvider.getApplicationContext())
        val state = engine.update(reading)

        assertTrue("Mozc returned no candidates", state.candidates.isNotEmpty())
        val committed = requireNotNull(engine.commit(state.selectedIndex)).value
        assertTrue("Mozc dropped text while committing: $committed", committed.length >= reading.length)
        engine.reset()
    }
}
