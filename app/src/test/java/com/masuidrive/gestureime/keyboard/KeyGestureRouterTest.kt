package com.masuidrive.gestureime.keyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyGestureRouterTest {
    private val space = KeyboardLayouts.personalFooter(KeyboardMode.QWERTY)[3]
    private val layer = KeyboardLayouts.personalFooter(KeyboardMode.QWERTY)[0]

    @Test fun `short vertical release is enabled only for the personal footer`() {
        val router = KeyGestureRouter()
        router.begin(1, space, 0f, 0f, GestureThresholds(), shortVerticalCursor = true)
        assertNull(router.move(1, 0f, -15f))
        assertEquals(KeyGestureRouter.Step.Cursor(Direction.UP, 1),
            router.end(1, 0f, -15f)?.releaseCursor)

        router.begin(2, space, 0f, 0f, GestureThresholds())
        assertNull(router.move(2, 0f, -15f))
        val ordinary = router.end(2, 0f, -15f)
        assertEquals(Direction.CENTER, ordinary?.direction)
        assertFalse(ordinary?.cursorMoved ?: true)
        assertNull(ordinary?.releaseCursor)
    }

    @Test fun `direction resolves from the key specification and returns to center`() {
        val router = KeyGestureRouter()
        router.begin(1, layer, 0f, 0f, GestureThresholds())
        assertEquals(KeyGestureRouter.Step.Selection(Direction.UP, true, true), router.move(1, 0f, -30f))
        assertEquals(KeyGestureRouter.Step.Selection(Direction.CENTER, true, true), router.move(1, 0f, 0f))
        assertEquals(Direction.CENTER, router.end(1, 0f, 0f)?.direction)

        val more = KeyboardLayouts.personalFooter(KeyboardMode.QWERTY)[1]
        router.begin(2, more, 0f, 0f, GestureThresholds())
        assertEquals(KeyGestureRouter.Step.Selection(Direction.CENTER, false, false), router.move(2, 30f, 0f))
        assertEquals(Direction.CENTER, router.end(2, 30f, 0f)?.direction)
    }

    @Test fun `cancel clears the key without committing a release cursor`() {
        val router = KeyGestureRouter()
        router.begin(1, space, 0f, 0f, GestureThresholds(), shortVerticalCursor = true)
        router.move(1, 0f, -15f)
        router.cancel(1)
        assertNull(router.end(1, 0f, -15f))
        router.begin(2, space, 0f, 0f, GestureThresholds(), shortVerticalCursor = true)
        assertTrue(router.end(2, 0f, 0f) != null)
    }
}
