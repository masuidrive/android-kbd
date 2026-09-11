package com.masuidrive.gestureime.suggestion

import android.content.Context
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface EnglishSuggestionEngine {
    suspend fun suggest(prefix: String, limit: Int = 5): List<String>
}

class EnglishPrefixLookup(private val wordsByFrequency: List<String>) : EnglishSuggestionEngine {
    override suspend fun suggest(prefix: String, limit: Int): List<String> {
        if (prefix.isEmpty() || prefix.length > MAX_PREFIX_LENGTH || limit <= 0 || !prefix.all(::isAsciiLetter)) {
            return emptyList()
        }
        val normalized = prefix.lowercase(Locale.ROOT)
        return wordsByFrequency.asSequence()
            .filter { it.startsWith(normalized) }
            .take(limit.coerceAtMost(MAX_RESULTS))
            .map { preserveTypedCase(prefix, it) }
            .toList()
    }

    private fun preserveTypedCase(prefix: String, word: String): String = when {
        prefix.all(::isAsciiUppercase) -> word.uppercase(Locale.ROOT)
        else -> prefix + word.substring(prefix.length)
    }

    private fun isAsciiLetter(char: Char): Boolean = char in 'A'..'Z' || char in 'a'..'z'
    private fun isAsciiUppercase(char: Char): Boolean = char in 'A'..'Z'

    private companion object {
        const val MAX_PREFIX_LENGTH = 64
        const val MAX_RESULTS = 5
    }
}

class BundledEnglishSuggestionEngine(context: Context) : EnglishSuggestionEngine {
    private val lookup by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        val words = context.assets.open(ASSET_PATH).bufferedReader(Charsets.US_ASCII).use { it.readLines() }
        EnglishPrefixLookup(words)
    }

    override suspend fun suggest(prefix: String, limit: Int): List<String> = withContext(Dispatchers.IO) {
        lookup.suggest(prefix, limit)
    }

    private companion object {
        const val ASSET_PATH = "english-words.txt"
    }
}
