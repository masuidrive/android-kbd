package com.masuidrive.gestureime.keyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardLayoutsTest {
    @Test fun `all six named modes have four rows`() {
        assertEquals(KeyboardMode.entries.toSet(), KeyboardLayouts.all.keys)
        KeyboardLayouts.all.values.forEach { assertEquals(4, it.rows.size) }
    }

    @Test fun `every qwerty letter has lower upper and specified secondary input`() {
        val keys = keys(KeyboardMode.QWERTY).associateBy { it.center?.label }
        val secondary = mapOf(
            'q' to "1", 'w' to "2", 'e' to "3", 'r' to "4", 't' to "5", 'y' to "6", 'u' to "7", 'i' to "8", 'o' to "9", 'p' to "0",
            'a' to "@", 's' to "#", 'd' to "\$", 'f' to "&", 'g' to "*", 'h' to "(", 'j' to ")", 'k' to "'", 'l' to "\"",
            'z' to "%", 'x' to "-", 'c' to "+", 'v' to "=", 'b' to "/", 'n' to ";", 'm' to ":", ',' to "!", '.' to "?",
        )
        secondary.forEach { (letter, down) ->
            val key = keys.getValue(letter.toString())
            if (letter.isLetter()) assertEquals(letter.uppercase(), key.up?.label)
            assertEquals(down, key.down?.label)
        }
    }

    @Test fun `modifier and backspace expose only their allowed directions`() {
        val qwerty = keys(KeyboardMode.QWERTY)
        val modifier = qwerty.single { it.kind == KeyKind.MODIFIER }
        assertNull(modifier.center); assertNull(modifier.left); assertNull(modifier.right)
        assertEquals(KeyAction.SetModifier(Modifier.ALT), modifier.up?.action)
        assertEquals(KeyAction.SetModifier(Modifier.CTRL), modifier.down?.action)
        val backspace = qwerty.single { it.kind == KeyKind.BACKSPACE }
        assertTrue(backspace.center?.action is KeyAction.Backspace); assertNull(backspace.up)
        assertEquals("⌫", backspace.center?.label)
        assertEquals(KeyAction.Escape, backspace.down?.action)
    }

    @Test fun `qwerty swaps the half width period and full width backspace without changing symbols`() {
        val qwerty = KeyboardLayouts.layout(KeyboardMode.QWERTY).rows
        val second = qwerty[1].keys
        val third = qwerty[2].keys
        val period = second.last()
        val backspace = third.last()

        assertEquals(10f, second.sumOf { it.widthUnits.toDouble() }.toFloat(), 0f)
        assertEquals(10f, third.sumOf { it.widthUnits.toDouble() }.toFloat(), 0f)
        assertEquals("key-.", period.id)
        assertEquals(.5f, period.widthUnits, 0f)
        assertEquals(KeyAction.CommitText("."), period.center?.action)
        assertEquals(KeyAction.CommitText("?"), period.down?.action)
        assertEquals("backspace", backspace.id)
        assertEquals(1f, backspace.widthUnits, 0f)
        assertEquals(KeyAction.Backspace(), backspace.center?.action)
        assertEquals(KeyAction.Escape, backspace.down?.action)
        assertNull(backspace.left); assertNull(backspace.up); assertNull(backspace.right)

        val symbols = KeyboardLayouts.layout(KeyboardMode.SYMBOLS).rows
        assertEquals(10f, symbols[1].keys.sumOf { it.widthUnits.toDouble() }.toFloat(), 0f)
        assertEquals(.5f, symbols[1].keys.last().widthUnits, 0f)
        assertEquals("backspace", symbols[1].keys.last().id)
        assertEquals(
            listOf("mode-AZ", "escape", "key-`", "key-!", "key-?", "key-;", "tab", "key-<", "key->", "key--"),
            symbols[2].keys.map { it.id },
        )
    }

    @Test fun `kana directions cover every specified kana input`() {
        val kana = keys(KeyboardMode.KANA).filter { it.kind == KeyKind.KANA }
        val actual = kana.flatMap { Direction.entries.mapNotNull(it::value) }.map { it.label }.toSet()
        val expected = listOf("あいうえお", "かきくけこ", "さしすせそ", "たちつてと", "なにぬねの", "はひふへほ", "まみむめも", "や（ゆ）よ", "らりるれろ", "、。？！", "わをんー〜").flatMap(String::toList).map(Char::toString).toSet()
        assertEquals(expected, actual)
        val transform = keys(KeyboardMode.KANA).single { it.kind == KeyKind.ACCENT }
        assertEquals("小", transform.center?.label)
        assertEquals(KeyAction.TransformKana(KanaTransform.CYCLE), transform.center?.action)
        assertEquals("゛", transform.left?.label)
        assertEquals(KeyAction.TransformKana(KanaTransform.DAKUTEN), transform.left?.action)
        assertEquals("゛", transform.up?.label)
        assertEquals(KeyAction.TransformKana(KanaTransform.DAKUTEN), transform.up?.action)
        assertEquals("゜", transform.right?.label)
        assertEquals(KeyAction.TransformKana(KanaTransform.HANDAKUTEN), transform.right?.action)
        assertNull(transform.down)
    }

    @Test fun `layer keys expose voice left and the fixed up right down layer map`() {
        KeyboardLayouts.all.filterKeys { it != KeyboardMode.VOICE }.values.flatMap { it.rows }.flatMap { it.keys }.filter { it.kind == KeyKind.LAYER_SWITCH }.forEach { key ->
            assertEquals("音声", key.left?.label)
            assertEquals(KeyAction.VoiceHold, key.left?.action)
            assertEquals(KeyAction.SwitchLayer(KeyboardMode.KANA), key.up?.action)
            assertEquals(KeyAction.SwitchLayer(KeyboardMode.QWERTY), key.right?.action)
            assertEquals(KeyAction.SwitchLayer(KeyboardMode.NUMBERS), key.down?.action)
        }
        val voice = keys(KeyboardMode.VOICE).single { it.kind == KeyKind.LAYER_SWITCH }
        assertEquals("キャンセル", voice.center?.label)
        assertEquals(1f, voice.widthUnits)
        assertEquals(KeyAction.CancelVoice, voice.center?.action)
        assertNull(voice.left)
        assertEquals(KeyAction.SwitchLayer(KeyboardMode.KANA), voice.up?.action)
        assertEquals(KeyAction.SwitchLayer(KeyboardMode.QWERTY), voice.right?.action)
        assertEquals(KeyAction.SwitchLayer(KeyboardMode.NUMBERS), voice.down?.action)
        KeyboardLayouts.all.values.flatMap { it.rows }.flatMap { it.keys }.filter { it.kind == KeyKind.MODE }.forEach { key ->
            assertNull(key.left); assertNull(key.up); assertNull(key.right); assertNull(key.down)
        }
    }

    @Test fun `voice bottom row has six equal columns and direct punctuation`() {
        val rows = KeyboardLayouts.layout(KeyboardMode.VOICE).rows
        assertEquals(4, rows.size)
        assertTrue(rows.take(3).all { it.keys.single().widthUnits == 6f })

        val bottom = rows.last().keys
        assertEquals(listOf("voice-cancel", "voice-status", "voice-punct", "space", "voice-backspace", "enter"), bottom.map { it.id })
        assertTrue(bottom.all { it.widthUnits == 1f })
        val punctuation = bottom[2]
        assertEquals(KeyKind.CHARACTER, punctuation.kind)
        assertEquals(
            listOf("、", "。", "？", "！", "、"),
            Direction.entries.map { punctuation.value(it)?.label },
        )
        assertEquals(
            listOf("、", "。", "？", "！", "、"),
            Direction.entries.mapNotNull { (punctuation.value(it)?.action as? KeyAction.CommitText)?.text },
        )
        assertEquals(KeyAction.CommitText(" "), bottom[3].center?.action)
        assertEquals(KeyAction.MoveCursor(Direction.LEFT), bottom[3].left?.action)
        assertEquals(KeyAction.MoveCursor(Direction.UP), bottom[3].up?.action)
        assertEquals(KeyAction.MoveCursor(Direction.RIGHT), bottom[3].right?.action)
        assertEquals(KeyAction.MoveCursor(Direction.DOWN), bottom[3].down?.action)
        val backspace = bottom[4]
        assertEquals(KeyKind.BACKSPACE, backspace.kind)
        assertEquals(KeyAction.Backspace(), backspace.center?.action)
        assertNull(backspace.left); assertNull(backspace.up); assertNull(backspace.right); assertNull(backspace.down)
        assertEquals(KeyAction.Enter, bottom[5].center?.action)
        assertEquals(KeyAction.Paste, bottom[5].down?.action)
        assertEquals(KeyAction.ModifiedKey("j", Modifier.CTRL), bottom[5].up?.action)
    }

    @Test fun `symbol layer contains ASCII only`() {
        keys(KeyboardMode.SYMBOLS).filter { it.kind == KeyKind.CHARACTER }.flatMap { Direction.entries.mapNotNull(it::value) }
            .forEach { value -> assertTrue(value.label.all { it.code in 0..127 }) }
        assertFalse(keys(KeyboardMode.SYMBOLS).isEmpty())
        val escape = keys(KeyboardMode.SYMBOLS).single { it.id == "escape" }
        assertEquals("Esc", escape.center?.label)
        assertEquals(KeyAction.Escape, escape.center?.action)
    }

    @Test fun `qwerty and symbol layers cover every printable ASCII character`() {
        val actual = setOf(KeyboardMode.QWERTY, KeyboardMode.SYMBOLS)
            .flatMap(::keys)
            .flatMap { key -> Direction.entries.mapNotNull(key::value) }
            .mapNotNull { (it.action as? KeyAction.CommitText)?.text }
            .flatMap(String::toList)
            .toSet()
        val expected = (0x20..0x7e).map(Int::toChar).toSet()

        assertEquals(expected, actual)
    }

    @Test fun `symbol layer exposes escape tab backtick and minus as direct taps without symbol flicks`() {
        val symbols = keys(KeyboardMode.SYMBOLS)
        assertEquals(KeyAction.Escape, symbols.single { it.center?.label == "Esc" }.center?.action)
        assertEquals(KeyAction.Tab, symbols.single { it.center?.label == "Tab" }.center?.action)
        assertFalse(symbols.any { it.center?.label == ":" })
        assertEquals(KeyAction.CommitText("`"), symbols.single { it.center?.label == "`" }.center?.action)
        assertEquals(KeyAction.CommitText("-"), symbols.single { it.center?.label == "-" }.center?.action)
        val backspace = symbols.single { it.kind == KeyKind.BACKSPACE }
        assertEquals("⌫", backspace.center?.label)
        assertEquals(KeyAction.Backspace(), backspace.center?.action)
        assertEquals(KeyAction.Escape, backspace.down?.action)
        symbols.filter { it.kind == KeyKind.CHARACTER }.forEach { key ->
            assertNull(key.left); assertNull(key.up); assertNull(key.right); assertNull(key.down)
        }
        assertEquals(KeyAction.CommitText("\""), keys(KeyboardMode.QWERTY).single { it.center?.label == "l" }.down?.action)
        assertEquals(KeyAction.CommitText("/"), keys(KeyboardMode.QWERTY).single { it.center?.label == "b" }.down?.action)
        assertEquals(KeyAction.CommitText(":"), keys(KeyboardMode.QWERTY).single { it.center?.label == "m" }.down?.action)
    }

    @Test fun `number minus key exposes its five specified ASCII values`() {
        val minus = keys(KeyboardMode.NUMBERS).single { it.id == "five--" }
        assertEquals(listOf("-", "+", "/", "*", ","), Direction.entries.map { minus.value(it)?.label })
    }

    @Test fun `emoji layer keeps the kana layer switch rail beside seven picker columns`() {
        val contentRows = KeyboardLayouts.emojiContentRows(listOf("😀", "❤️", "😀"))
        val rows = KeyboardLayouts.layout(KeyboardMode.EMOJI, emojiRecents = listOf("😀", "❤️", "😀")).rows
        assertEquals(4, rows.size)
        assertEquals(4, contentRows.size)
        assertTrue(contentRows.all { row ->
            row.keys.size == 2 && row.keys[0].widthUnits == 1f && row.keys[1].widthUnits == 7f
        })
        assertEquals(listOf("あ", "#!", "19", "AZ"), contentRows.map { it.keys.first().center?.label })
        assertEquals(
            listOf(KeyboardMode.KANA, KeyboardMode.SYMBOLS, KeyboardMode.NUMBERS, KeyboardMode.QWERTY),
            contentRows.map { (it.keys.first().center?.action as KeyAction.SwitchLayer).target },
        )
        assertEquals(contentRows, rows)
        assertEquals(KeyboardMode.QWERTY, (rows[3].keys[0].center?.action as KeyAction.SwitchLayer).target)
        assertEquals(KeyAction.VoiceHold, rows[3].keys[0].left?.action)
        assertEquals(KeyAction.SwitchLayer(KeyboardMode.KANA), rows[3].keys[0].up?.action)
        assertEquals(KeyAction.SwitchLayer(KeyboardMode.QWERTY), rows[3].keys[0].right?.action)
        assertEquals(KeyAction.SwitchLayer(KeyboardMode.NUMBERS), rows[3].keys[0].down?.action)
    }

    @Test fun `emoji recent normalization keeps exactly the newest one hundred distinct entries`() {
        val newestFirst = (100 downTo 0).map { "emoji-$it" }

        val bounded = EmojiCatalog.visibleRecents(newestFirst)
        assertEquals(100, bounded.size)
        assertEquals("emoji-100", bounded.first())
        assertEquals("emoji-1", bounded.last())
        assertFalse(bounded.contains("emoji-0"))

        val repeated = EmojiCatalog.visibleRecents(listOf("emoji-50") + newestFirst)
        assertEquals(100, repeated.size)
        assertEquals("emoji-50", repeated.first())
        assertEquals(1, repeated.count { it == "emoji-50" })
    }

    @Test fun `dual kana duplicates only the central twelve keys`() {
        val single = KeyboardLayouts.layout(KeyboardMode.KANA).rows
        val dual = KeyboardLayouts.layout(KeyboardMode.KANA, dualKana = true).rows
        dual.indices.forEach { row ->
            assertEquals(single[row].keys.first(), dual[row].keys.first())
            assertEquals(single[row].keys.drop(1).take(3), dual[row].keys.slice(1..3))
            assertEquals(single[row].keys.drop(1).take(3), dual[row].keys.slice(4..6))
            assertEquals(single[row].keys.drop(4), dual[row].keys.drop(7))
        }
    }

    @Test fun `active conversion labels candidate and changes enter only after space selection`() {
        val unselected = KeyboardLayouts.layout(KeyboardMode.KANA, conversionActive = true)
        val unselectedSpace = unselected.rows[1].keys.last()
        val unselectedEnter = unselected.rows[2].keys.last()
        assertEquals("候補", unselectedSpace.center?.label)
        assertEquals(KeyAction.CycleCandidate, unselectedSpace.center?.action)
        assertEquals("無変換", unselectedEnter.center?.label)
        assertEquals(KeyAction.CommitWithoutConversion, unselectedEnter.center?.action)

        val selectedEnter = KeyboardLayouts.layout(
            KeyboardMode.KANA,
            conversionActive = true,
            conversionCandidateSelected = true,
        ).rows[2].keys.last()
        assertEquals("確定", selectedEnter.center?.label)
        assertEquals(KeyAction.CommitConversion, selectedEnter.center?.action)
        listOf(unselectedEnter, selectedEnter).forEach { enter ->
            assertEquals(KeyAction.ConvertToKatakana, enter.up?.action)
            assertEquals(KeyAction.ConvertToKatakana, enter.left?.action)
            assertNull(enter.right)
            assertNull(enter.down)
        }
    }

    @Test fun `nonconverting enter exposes control j only on up`() {
        KeyboardMode.entries.filter { it !in setOf(KeyboardMode.VOICE, KeyboardMode.EMOJI) }.forEach { mode ->
            val enter = keys(mode).single { it.kind == KeyKind.ENTER }
            assertEquals(FlickValue("C-j", KeyAction.ModifiedKey("j", Modifier.CTRL)), enter.up)
            assertEquals(KeyAction.Paste, enter.down?.action)
        }

        val converting = KeyboardLayouts.layout(KeyboardMode.KANA, conversionActive = true)
            .rows.flatMap { it.keys }.single { it.kind == KeyKind.ENTER }
        assertEquals(FlickValue("カタカナ", KeyAction.ConvertToKatakana), converting.up)
    }

    private fun keys(mode: KeyboardMode) = KeyboardLayouts.layout(mode).rows.flatMap { it.keys }
}
