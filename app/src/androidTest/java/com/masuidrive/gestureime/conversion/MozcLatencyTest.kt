package com.masuidrive.gestureime.conversion

import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MozcLatencyTest {
    @Test
    fun measuresColdAndTenRapidUpdates() = runBlocking {
        val engine = MozcConversionEngine(ApplicationProvider.getApplicationContext())
        val coldStart = System.nanoTime()
        val coldState = engine.update("か")
        val coldMs = (System.nanoTime() - coldStart) / 1_000_000.0
        assertTrue(coldState.candidates.isNotEmpty())

        engine.reset()
        val reading = "きょうはいいてんきだ"
        val samples = reading.indices.map { end ->
            val start = System.nanoTime()
            val state = engine.update(reading.substring(0, end + 1))
            val elapsed = (System.nanoTime() - start) / 1_000_000.0
            assertTrue(state.reading.isNotEmpty())
            elapsed
        }
        Log.i(
            "MozcLatency",
            "cold_update_ms=%.3f rapid_10_total_ms=%.3f rapid_10_max_ms=%.3f samples_ms=%s".format(
                coldMs,
                samples.sum(),
                samples.maxOrNull(),
                samples.joinToString(",") { "%.3f".format(it) },
            ),
        )
        engine.reset()
    }
}
