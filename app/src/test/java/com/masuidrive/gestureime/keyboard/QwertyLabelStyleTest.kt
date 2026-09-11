package com.masuidrive.gestureime.keyboard

import org.junit.Assert.assertEquals
import org.junit.Test

class QwertyLabelStyleTest {
    @Test fun `invalid and extreme fields sanitize independently`() {
        val style = QwertyLabelStyle.DEFAULT.with(
            QwertyLabelGroup.LETTER_PRIMARY,
            LabelAdjustment(Float.NaN, Float.POSITIVE_INFINITY, -99f),
        )

        assertEquals(LabelAdjustment(1f, 0f, -8f), style[QwertyLabelGroup.LETTER_PRIMARY])
        assertEquals(LabelAdjustment(), style[QwertyLabelGroup.LETTER_SECONDARY])
    }

    @Test fun `all adjustment limits are inclusive`() {
        val high = LabelAdjustment(9f, 9f, 9f).sanitized()
        val low = LabelAdjustment(-9f, -9f, -9f).sanitized()

        assertEquals(LabelAdjustment(1.3f, 6f, 8f), high)
        assertEquals(LabelAdjustment(.7f, -6f, -8f), low)
    }
}
