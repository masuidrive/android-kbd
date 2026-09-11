package com.masuidrive.gestureime.conversion

import android.content.ContentResolver
import android.provider.UserDictionary

/**
 * Reads only entries whose shortcut exactly matches the kana reading.
 *
 * Android restricts this provider to the currently selected IME on modern
 * devices.  Provider absence or denial must not make Japanese conversion fail.
 */
internal class AndroidUserDictionaryCandidates(private val resolver: ContentResolver) {
    fun forReading(reading: String): List<String> {
        if (reading.isBlank()) return emptyList()
        return runCatching {
            resolver.query(
                UserDictionary.Words.CONTENT_URI,
                PROJECTION,
                SELECTION,
                arrayOf(reading, JAPANESE_LOCALE_PREFIX),
                UserDictionary.Words.DEFAULT_SORT_ORDER,
            )?.use { cursor -> wordsFromCursor(cursor, reading) }.orEmpty()
        }.getOrDefault(emptyList())
    }

    companion object {
        const val MAX_CANDIDATES = 6
        const val JAPANESE_LOCALE_PREFIX = "ja%"
        val PROJECTION = arrayOf(UserDictionary.Words.WORD, UserDictionary.Words.SHORTCUT)
        const val SELECTION = "${UserDictionary.Words.SHORTCUT} = ? AND " +
            "(${UserDictionary.Words.LOCALE} IS NULL OR ${UserDictionary.Words.LOCALE} LIKE ?)"

        internal fun wordsFromCursor(cursor: android.database.Cursor, reading: String): List<String> {
            val wordColumn = cursor.getColumnIndexOrThrow(UserDictionary.Words.WORD)
            val shortcutColumn = cursor.getColumnIndexOrThrow(UserDictionary.Words.SHORTCUT)
            return buildList {
                while (cursor.moveToNext() && size < MAX_CANDIDATES) {
                    if (cursor.getString(shortcutColumn) == reading) {
                        val word = cursor.getString(wordColumn)
                        if (!word.isNullOrBlank() && word !in this) {
                            add(word)
                        }
                    }
                }
            }
        }
    }
}
