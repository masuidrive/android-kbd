package com.masuidrive.gestureime.keyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardLayoutsTest {
    @Test fun `all five named layers have four rows`() {
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

    @Test fun `kana directions cover every specified kana input`() {
        val kana = keys(KeyboardMode.KANA).filter { it.kind == KeyKind.KANA }
        val actual = kana.flatMap { Direction.entries.mapNotNull(it::value) }.map { it.label }.toSet()
        val expected = listOf("あいうえお", "かきくけこ", "さしすせそ", "たちつてと", "なにぬねの", "はひふへほ", "まみむめも", "や（ゆ）よ", "らりるれろ", "、。？！", "わをんー〜").flatMap(String::toList).map(Char::toString).toSet()
        assertEquals(expected, actual)
        val transform = keys(KeyboardMode.KANA).single { it.kind == KeyKind.ACCENT }
        assertEquals("小", transform.center?.label)
        assertEquals(KeyAction.TransformKana(KanaTransform.CYCLE), transform.center?.action)
        assertEquals(KeyAction.TransformKana(KanaTransform.DAKUTEN), transform.left?.action)
        assertEquals(KeyAction.TransformKana(KanaTransform.SMALL), transform.up?.action)
        assertEquals(KeyAction.TransformKana(KanaTransform.HANDAKUTEN), transform.right?.action)
        assertNull(transform.down)
    }

    @Test fun `layer keys expose voice left and the fixed up right down layer map`() {
        KeyboardLayouts.all.values.flatMap { it.rows }.flatMap { it.keys }.filter { it.kind == KeyKind.LAYER_SWITCH }.forEach { key ->
            assertEquals("音声", key.left?.label)
            assertEquals(KeyAction.VoiceHold, key.left?.action)
            assertEquals(KeyAction.SwitchLayer(KeyboardMode.KANA), key.up?.action)
            assertEquals(KeyAction.SwitchLayer(KeyboardMode.QWERTY), key.right?.action)
            assertEquals(KeyAction.SwitchLayer(KeyboardMode.NUMBERS), key.down?.action)
        }
        KeyboardLayouts.all.values.flatMap { it.rows }.flatMap { it.keys }.filter { it.kind == KeyKind.MODE }.forEach { key ->
            assertNull(key.left); assertNull(key.up); assertNull(key.right); assertNull(key.down)
        }
    }

    @Test fun `symbol layer contains ASCII only`() {
        keys(KeyboardMode.SYMBOLS).filter { it.kind == KeyKind.CHARACTER }.flatMap { Direction.entries.mapNotNull(it::value) }
            .forEach { value -> assertTrue(value.label.all { it.code in 0..127 }) }
        assertFalse(keys(KeyboardMode.SYMBOLS).isEmpty())
        assertEquals(KeyAction.Escape, keys(KeyboardMode.SYMBOLS).single { it.id == "escape" }.center?.action)
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

    @Test fun `symbol layer exposes backtick and minus as direct taps without symbol flicks`() {
        val symbols = keys(KeyboardMode.SYMBOLS)
        assertEquals(KeyAction.CommitText("`"), symbols.single { it.center?.label == "`" }.center?.action)
        assertEquals(KeyAction.CommitText("-"), symbols.single { it.center?.label == "-" }.center?.action)
        symbols.filter { it.kind == KeyKind.CHARACTER }.forEach { key ->
            assertNull(key.left); assertNull(key.up); assertNull(key.right); assertNull(key.down)
        }
        assertEquals(KeyAction.CommitText("\""), keys(KeyboardMode.QWERTY).single { it.center?.label == "l" }.down?.action)
        assertEquals(KeyAction.CommitText("/"), keys(KeyboardMode.QWERTY).single { it.center?.label == "b" }.down?.action)
    }

    @Test fun `number minus key exposes its five specified ASCII values`() {
        val minus = keys(KeyboardMode.NUMBERS).single { it.id == "five--" }
        assertEquals(listOf("-", "+", "/", "*", ","), Direction.entries.map { minus.value(it)?.label })
    }

    @Test fun `cursor layer contains boundary arrows trackpad and spanning enter`() {
        val rows = KeyboardLayouts.layout(KeyboardMode.CURSOR).rows
        assertEquals(KeyAction.MoveToBoundary(CursorBoundary.START), rows[1].keys[1].center?.action)
        assertEquals(KeyAction.MoveToBoundary(CursorBoundary.END), rows[1].keys[3].center?.action)
        assertEquals(KeyKind.SPACE, rows[2].keys[2].kind)
        assertEquals(2, rows[2].keys[4].rowSpan)
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

    @Test fun `active conversion enter has only explicit conversion choices`() {
        val enter = KeyboardLayouts.layout(KeyboardMode.KANA, conversionActive = true).rows[2].keys.last()
        assertEquals("確定", enter.center?.label)
        assertEquals(KeyAction.CommitConversion, enter.center?.action)
        assertEquals(KeyAction.CommitWithoutConversion, enter.up?.action)
        assertEquals(KeyAction.ConvertToKatakana, enter.left?.action)
        assertNull(enter.right); assertNull(enter.down)
    }

    private fun keys(mode: KeyboardMode) = KeyboardLayouts.layout(mode).rows.flatMap { it.keys }
}
