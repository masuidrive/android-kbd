package com.masuidrive.gestureime.conversion

import android.database.MatrixCursor
import android.provider.UserDictionary
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AndroidUserDictionaryCandidatesTest {
    @Test
    fun matchingShortcutRowsKeepFrequencyOrderAndRemoveDuplicateWords() {
        val cursor = MatrixCursor(arrayOf(UserDictionary.Words.WORD, UserDictionary.Words.SHORTCUT)).apply {
            addRow(arrayOf("候補A", "こうほ"))
            addRow(arrayOf("候補A", "こうほ"))
            addRow(arrayOf("候補B", "こうほ"))
            addRow(arrayOf("無関係", "むかんけい"))
        }

        val words = cursor.use { rows -> AndroidUserDictionaryCandidates.wordsFromCursor(rows, "こうほ") }

        assertEquals(listOf("候補A", "候補B"), words)
    }
}
