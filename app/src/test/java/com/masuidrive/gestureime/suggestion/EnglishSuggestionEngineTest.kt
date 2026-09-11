package com.masuidrive.gestureime.suggestion

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.security.MessageDigest

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class EnglishSuggestionEngineTest {
    private val engine = EnglishPrefixLookup(listOf("the", "to", "that", "this", "they", "their", "there"))

    @Test
    fun `returns frequency ordered prefix matches up to five`() = runTest {
        assertEquals(listOf("the", "that", "this", "they", "their"), engine.suggest("th", limit = 5))
        assertEquals(listOf("the", "that"), engine.suggest("th", limit = 2))
    }

    @Test
    fun `preserves typed prefix case and extends all uppercase`() = runTest {
        assertEquals(listOf("The", "That"), engine.suggest("Th", limit = 2))
        assertEquals(listOf("THE", "THAT"), engine.suggest("TH", limit = 2))
        assertEquals(listOf("tHe", "tHat"), engine.suggest("tH", limit = 2))
    }

    @Test
    fun `rejects digits non ascii and invalid limits without suggestions`() = runTest {
        listOf("abc1", "é", "a".repeat(65)).forEach { assertEquals(emptyList<String>(), engine.suggest(it, 5)) }
        assertEquals(emptyList<String>(), engine.suggest("th", 0))
    }

    @Test
    fun `bundled lexicon is fixed and ranked by source frequency`() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val bytes = context.assets.open("english-words.txt").use { it.readBytes() }
        assertEquals(
            "eb5e60159b5860301a5d581364ff8f4cc9319472905f20cc3fef4ecc85150346",
            MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) },
        )
        assertEquals(
            listOf("the", "that", "this", "they", "their"),
            BundledEnglishSuggestionEngine(context).suggest("th"),
        )
    }
}
