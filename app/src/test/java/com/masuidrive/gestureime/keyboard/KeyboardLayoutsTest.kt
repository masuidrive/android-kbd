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
        assertNull(backspace.center); assertNull(backspace.up)
        assertTrue(backspace.down?.action is KeyAction.Backspace)
    }

    @Test fun `kana directions cover every specified kana input`() {
        val kana = keys(KeyboardMode.KANA).filter { it.kind == KeyKind.KANA }
        val actual = kana.flatMap { Direction.entries.mapNotNull(it::value) }.map { it.label }.toSet()
        val expected = listOf("あいうえお", "かきくけこ", "さしすせそ", "たちつてと", "なにぬねの", "はひふへほ", "まみむめも", "や（ゆ）よ", "らりるれろ", "、。？！", "わをんー〜").flatMap(String::toList).map(Char::toString).toSet()
        assertEquals(expected, actual)
        assertEquals(KeyAction.TransformKana, keys(KeyboardMode.KANA).single { it.kind == KeyKind.ACCENT }.center?.action)
    }

    @Test fun `mode keys share the fixed four-direction layer map`() {
        KeyboardLayouts.all.values.flatMap { it.rows }.flatMap { it.keys }.filter { it.kind == KeyKind.LAYER_SWITCH }.forEach { key ->
            assertEquals(KeyAction.SwitchLayer(KeyboardMode.SYMBOLS), key.left?.action)
            assertEquals(KeyAction.SwitchLayer(KeyboardMode.KANA), key.up?.action)
            assertEquals(KeyAction.SwitchLayer(KeyboardMode.NUMBERS), key.right?.action)
            assertEquals(KeyAction.SwitchLayer(KeyboardMode.QWERTY), key.down?.action)
        }
        KeyboardLayouts.all.values.flatMap { it.rows }.flatMap { it.keys }.filter { it.kind == KeyKind.MODE }.forEach { key ->
            assertNull(key.left); assertNull(key.up); assertNull(key.right); assertNull(key.down)
        }
    }

    @Test fun `symbol layer contains ASCII only`() {
        keys(KeyboardMode.SYMBOLS).filter { it.kind == KeyKind.CHARACTER }.flatMap { Direction.entries.mapNotNull(it::value) }
            .forEach { value -> assertTrue(value.label.all { it.code in 0..127 }) }
        assertFalse(keys(KeyboardMode.SYMBOLS).isEmpty())
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

    private fun keys(mode: KeyboardMode) = KeyboardLayouts.layout(mode).rows.flatMap { it.keys }
}
