package com.masuidrive.gestureime.keyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GestureInterpreterTest {
    @Test fun `selection starts at 18dp and returns only inside 10dp`() {
        val subject = GestureInterpreter()
        subject.start(1, 0f, 0f)
        assertNull(subject.move(1, 17.99f, 0f))
        assertEquals(GestureUpdate.Selection(Direction.RIGHT), subject.move(1, 18f, 0f))
        assertNull(subject.move(1, 10.01f, 0f))
        assertEquals(GestureUpdate.Selection(Direction.CENTER), subject.move(1, 10f, 0f))
    }

    @Test fun `direction follows current location after selection`() {
        val subject = GestureInterpreter()
        subject.start(7, 10f, 10f)
        subject.move(7, 30f, 10f)
        assertEquals(GestureUpdate.Selection(Direction.UP), subject.move(7, 10f, -10f))
        assertEquals(Direction.UP, subject.finish(7))
    }

    @Test fun `space locks dominant axis and emits incremental units`() {
        val subject = GestureInterpreter()
        subject.start(2, 0f, 0f, trackpad = true)
        assertNull(subject.move(2, 7f, 6f))
        assertEquals(GestureUpdate.CursorDelta(Direction.RIGHT, 1), subject.move(2, 10f, 4f))
        assertEquals(GestureUpdate.CursorDelta(Direction.RIGHT, 2), subject.move(2, 25f, 100f))
        assertEquals(GestureUpdate.CursorDelta(Direction.LEFT, 7), subject.move(2, -33f, -200f))
    }

    @Test fun `space reports fine reverse movement and crossing the origin`() {
        val subject = GestureInterpreter()
        subject.start(4, 0f, 0f, trackpad = true)
        assertEquals(GestureUpdate.CursorDelta(Direction.RIGHT, 2), subject.move(4, 16f, 1f))
        assertEquals(GestureUpdate.CursorDelta(Direction.LEFT, 1), subject.move(4, 8f, 80f))
        assertEquals(GestureUpdate.CursorDelta(Direction.LEFT, 1), subject.move(4, 0f, 80f))
        assertEquals(GestureUpdate.CursorDelta(Direction.LEFT, 1), subject.move(4, -8f, 80f))
    }

    @Test fun `vertical space movement uses 24dp units`() {
        val subject = GestureInterpreter()
        subject.start(3, 0f, 0f, trackpad = true)
        assertNull(subject.move(3, 1f, 10f))
        assertEquals(GestureUpdate.CursorDelta(Direction.DOWN, 1), subject.move(3, 2f, 24f))
        assertEquals(GestureUpdate.CursorDelta(Direction.DOWN, 1), subject.move(3, 99f, 48f))
    }

    @Test fun `vertical-only horizontal gesture reports an unassigned selection without changing its direction`() {
        val subject = GestureInterpreter()
        subject.start(5, 0f, 0f, verticalOnly = true)
        assertNull(subject.move(5, 12f, 2f))
        assertEquals(GestureUpdate.Selection(Direction.RIGHT), subject.move(5, 18f, 2f))
        assertNull(subject.move(5, 1f, 30f))
        assertEquals(Direction.CENTER, subject.finish(5))
    }

    @Test fun `cancel discards every pointer without a result`() {
        val subject = GestureInterpreter()
        subject.start(1, 0f, 0f)
        subject.start(2, 4f, 4f)
        subject.cancelAll()
        assertEquals(0, subject.activePointerCount())
        assertNull(subject.finish(1))
        assertNull(subject.move(2, 100f, 100f))
    }
}
