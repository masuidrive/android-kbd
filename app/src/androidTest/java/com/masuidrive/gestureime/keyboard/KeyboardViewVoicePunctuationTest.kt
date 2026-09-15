package com.masuidrive.gestureime.keyboard

import android.graphics.Rect
import android.os.SystemClock
import android.view.MotionEvent
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.masuidrive.gestureime.ImeTestActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class KeyboardViewVoicePunctuationTest {
    @Test fun emojiRailAndSymbolBackspaceUseProductionMotionEventsAtPhoneAndTabletWidths() {
        ActivityScenario.launch(ImeTestActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val actions = mutableListOf<KeyAction>()
                val density = activity.resources.displayMetrics.density
                val view = KeyboardView(activity).apply {
                    actionSink = KeyboardActionSink { actions += it }
                    setDualFlickEnabled(true)
                }
                activity.setContentView(view)
                listOf(412f, 840f).forEach { widthDp ->
                    val width = (widthDp * density).toInt()
                    val height = (228f * density).toInt()
                    view.setMode(KeyboardMode.KANA)
                    view.measure(exact(width), exact(height)); view.layout(0, 0, width, height)
                    val kanaRailWidth = bounds(view, 0).width()

                    view.setMode(KeyboardMode.EMOJI)
                    view.measure(exact(width), exact(height)); view.layout(0, 0, width, height)
                    val rail = listOf(0 to KeyboardMode.KANA, 2 to KeyboardMode.SYMBOLS, 4 to KeyboardMode.NUMBERS, 6 to KeyboardMode.QWERTY)
                    assertEquals("$widthDp dp rail width", kanaRailWidth, bounds(view, 0).width())
                    rail.forEachIndexed { index, (id, expected) ->
                        val key = bounds(view, id)
                        actions.clear()
                        val time = SystemClock.uptimeMillis() + index * 10L
                        dispatch(view, MotionEvent.ACTION_DOWN, time, time, key.exactCenterX(), key.exactCenterY())
                        dispatch(view, MotionEvent.ACTION_UP, time, time + 1, key.exactCenterX(), key.exactCenterY())
                        assertEquals("$widthDp dp emoji rail $expected", listOf(KeyAction.SwitchLayer(expected)), actions)
                        view.setMode(KeyboardMode.EMOJI)
                    }
                    val emojiRailKey = bounds(view, 0)
                    val railCenterX = emojiRailKey.exactCenterX()
                    val railCenterY = emojiRailKey.exactCenterY()
                    val distance = 30f * density
                    listOf(-distance to 0f, 0f to -distance, distance to 0f, 0f to distance).forEachIndexed { index, (dx, dy) ->
                        actions.clear()
                        val time = SystemClock.uptimeMillis() + index * 10L
                        dispatch(view, MotionEvent.ACTION_DOWN, time, time, railCenterX, railCenterY)
                        dispatch(view, MotionEvent.ACTION_MOVE, time, time + 1, railCenterX + dx, railCenterY + dy)
                        dispatch(view, MotionEvent.ACTION_UP, time, time + 2, railCenterX + dx, railCenterY + dy)
                        assertTrue("$widthDp dp emoji rail ignores ${index} direction", actions.isEmpty())
                    }
                    actions.clear()
                    val thresholdTime = SystemClock.uptimeMillis()
                    dispatch(view, MotionEvent.ACTION_DOWN, thresholdTime, thresholdTime, railCenterX, railCenterY)
                    dispatch(view, MotionEvent.ACTION_MOVE, thresholdTime, thresholdTime + 1, railCenterX + 17f * density, railCenterY)
                    dispatch(view, MotionEvent.ACTION_UP, thresholdTime, thresholdTime + 2, railCenterX + 17f * density, railCenterY)
                    assertEquals("$widthDp dp emoji rail stays a tap below threshold", listOf(KeyAction.SwitchLayer(KeyboardMode.KANA)), actions)
                    view.setMode(KeyboardMode.EMOJI)
                    actions.clear()
                    val returnTime = SystemClock.uptimeMillis()
                    dispatch(view, MotionEvent.ACTION_DOWN, returnTime, returnTime, railCenterX, railCenterY)
                    dispatch(view, MotionEvent.ACTION_MOVE, returnTime, returnTime + 1, railCenterX + distance, railCenterY)
                    dispatch(view, MotionEvent.ACTION_MOVE, returnTime, returnTime + 2, railCenterX + 10f * density, railCenterY)
                    dispatch(view, MotionEvent.ACTION_UP, returnTime, returnTime + 3, railCenterX + 10f * density, railCenterY)
                    assertEquals("$widthDp dp emoji rail returns to center", listOf(KeyAction.SwitchLayer(KeyboardMode.KANA)), actions)
                    view.setMode(KeyboardMode.EMOJI)
                    actions.clear()
                    val cancelTime = SystemClock.uptimeMillis()
                    dispatch(view, MotionEvent.ACTION_DOWN, cancelTime, cancelTime, railCenterX, railCenterY)
                    dispatch(view, MotionEvent.ACTION_MOVE, cancelTime, cancelTime + 1, railCenterX, railCenterY + distance)
                    dispatch(view, MotionEvent.ACTION_CANCEL, cancelTime, cancelTime + 2, railCenterX, railCenterY + distance)
                    assertTrue("$widthDp dp emoji rail cancel", actions.isEmpty())

                    val azRailKey = bounds(view, 6)
                    val azCenterX = azRailKey.exactCenterX()
                    val azCenterY = azRailKey.exactCenterY()
                    val azFlicks = listOf(
                        "tap" to Triple(0f, 0f, KeyAction.SwitchLayer(KeyboardMode.QWERTY)),
                        "left" to Triple(-distance, 0f, KeyAction.VoiceHold),
                        "up" to Triple(0f, -distance, KeyAction.SwitchLayer(KeyboardMode.KANA)),
                        "right" to Triple(distance, 0f, KeyAction.SwitchLayer(KeyboardMode.QWERTY)),
                        "down" to Triple(0f, distance, KeyAction.SwitchLayer(KeyboardMode.NUMBERS)),
                    )
                    azFlicks.forEachIndexed { index, (name, flick) ->
                        val (dx, dy, expected) = flick
                        actions.clear()
                        val time = SystemClock.uptimeMillis() + index * 10L
                        dispatch(view, MotionEvent.ACTION_DOWN, time, time, azCenterX, azCenterY)
                        if (dx != 0f || dy != 0f) {
                            dispatch(view, MotionEvent.ACTION_MOVE, time, time + 1, azCenterX + dx, azCenterY + dy)
                        }
                        dispatch(view, MotionEvent.ACTION_UP, time, time + 2, azCenterX + dx, azCenterY + dy)
                        assertEquals("$widthDp dp emoji AZ $name", listOf(expected), actions)
                        view.setMode(KeyboardMode.EMOJI)
                    }
                    actions.clear()
                    val azThresholdTime = SystemClock.uptimeMillis()
                    dispatch(view, MotionEvent.ACTION_DOWN, azThresholdTime, azThresholdTime, azCenterX, azCenterY)
                    dispatch(view, MotionEvent.ACTION_MOVE, azThresholdTime, azThresholdTime + 1, azCenterX, azCenterY - 17f * density)
                    dispatch(view, MotionEvent.ACTION_UP, azThresholdTime, azThresholdTime + 2, azCenterX, azCenterY - 17f * density)
                    assertEquals("$widthDp dp emoji AZ stays tap below threshold", listOf(KeyAction.SwitchLayer(KeyboardMode.QWERTY)), actions)
                    view.setMode(KeyboardMode.EMOJI)
                    actions.clear()
                    val azReturnTime = SystemClock.uptimeMillis()
                    dispatch(view, MotionEvent.ACTION_DOWN, azReturnTime, azReturnTime, azCenterX, azCenterY)
                    dispatch(view, MotionEvent.ACTION_MOVE, azReturnTime, azReturnTime + 1, azCenterX, azCenterY + distance)
                    dispatch(view, MotionEvent.ACTION_MOVE, azReturnTime, azReturnTime + 2, azCenterX, azCenterY + 10f * density)
                    dispatch(view, MotionEvent.ACTION_UP, azReturnTime, azReturnTime + 3, azCenterX, azCenterY + 10f * density)
                    assertEquals("$widthDp dp emoji AZ returns to center", listOf(KeyAction.SwitchLayer(KeyboardMode.QWERTY)), actions)
                    view.setMode(KeyboardMode.EMOJI)
                    actions.clear()
                    val azCancelTime = SystemClock.uptimeMillis()
                    dispatch(view, MotionEvent.ACTION_DOWN, azCancelTime, azCancelTime, azCenterX, azCenterY)
                    dispatch(view, MotionEvent.ACTION_MOVE, azCancelTime, azCancelTime + 1, azCenterX - distance, azCenterY)
                    dispatch(view, MotionEvent.ACTION_CANCEL, azCancelTime, azCancelTime + 2, azCenterX - distance, azCenterY)
                    // VoiceHold begins when its left direction crosses threshold, before
                    // release. CANCEL must not add a second layer action or commit a tap.
                    assertEquals("$widthDp dp emoji AZ cancel", listOf(KeyAction.VoiceHold), actions)

                    view.setMode(KeyboardMode.SYMBOLS)
                    view.measure(exact(width), exact(height)); view.layout(0, 0, width, height)
                    val backspaceId = KeyboardLayouts.layout(KeyboardMode.SYMBOLS).rows.flatMap { it.keys }
                        .indexOfFirst { it.kind == KeyKind.BACKSPACE }
                    val backspace = bounds(view, backspaceId)
                    actions.clear()
                    val tapTime = SystemClock.uptimeMillis()
                    dispatch(view, MotionEvent.ACTION_DOWN, tapTime, tapTime, backspace.exactCenterX(), backspace.exactCenterY())
                    dispatch(view, MotionEvent.ACTION_UP, tapTime, tapTime + 1, backspace.exactCenterX(), backspace.exactCenterY())
                    assertEquals(listOf(KeyAction.Backspace()), actions)
                    actions.clear()
                    val flickTime = SystemClock.uptimeMillis()
                    dispatch(view, MotionEvent.ACTION_DOWN, flickTime, flickTime, backspace.exactCenterX(), backspace.exactCenterY())
                    dispatch(view, MotionEvent.ACTION_MOVE, flickTime, flickTime + 1, backspace.exactCenterX(), backspace.exactCenterY() + 30f * density)
                    dispatch(view, MotionEvent.ACTION_UP, flickTime, flickTime + 2, backspace.exactCenterX(), backspace.exactCenterY() + 30f * density)
                    assertEquals(listOf(KeyAction.Escape), actions)
                    val centerX = backspace.exactCenterX()
                    val centerY = backspace.exactCenterY()
                    val backspaceDistance = 30f * density
                    listOf(-backspaceDistance to 0f, 0f to -backspaceDistance, backspaceDistance to 0f).forEachIndexed { index, (dx, dy) ->
                        actions.clear()
                        val time = SystemClock.uptimeMillis() + index * 10L
                        dispatch(view, MotionEvent.ACTION_DOWN, time, time, centerX, centerY)
                        dispatch(view, MotionEvent.ACTION_MOVE, time, time + 1, centerX + dx, centerY + dy)
                        dispatch(view, MotionEvent.ACTION_UP, time, time + 2, centerX + dx, centerY + dy)
                        assertTrue("$widthDp dp symbol backspace ignores direction $index", actions.isEmpty())
                    }
                    actions.clear()
                    val backspaceThresholdTime = SystemClock.uptimeMillis()
                    dispatch(view, MotionEvent.ACTION_DOWN, backspaceThresholdTime, backspaceThresholdTime, centerX, centerY)
                    dispatch(view, MotionEvent.ACTION_MOVE, backspaceThresholdTime, backspaceThresholdTime + 1, centerX, centerY + 17f * density)
                    dispatch(view, MotionEvent.ACTION_UP, backspaceThresholdTime, backspaceThresholdTime + 2, centerX, centerY + 17f * density)
                    assertEquals("$widthDp dp symbol backspace stays tap below threshold", listOf(KeyAction.Backspace()), actions)
                    actions.clear()
                    val backspaceReturnTime = SystemClock.uptimeMillis()
                    dispatch(view, MotionEvent.ACTION_DOWN, backspaceReturnTime, backspaceReturnTime, centerX, centerY)
                    dispatch(view, MotionEvent.ACTION_MOVE, backspaceReturnTime, backspaceReturnTime + 1, centerX, centerY + backspaceDistance)
                    dispatch(view, MotionEvent.ACTION_MOVE, backspaceReturnTime, backspaceReturnTime + 2, centerX, centerY + 10f * density)
                    dispatch(view, MotionEvent.ACTION_UP, backspaceReturnTime, backspaceReturnTime + 3, centerX, centerY + 10f * density)
                    assertEquals("$widthDp dp symbol backspace returns to center", listOf(KeyAction.Backspace()), actions)
                    actions.clear()
                    val backspaceCancelTime = SystemClock.uptimeMillis()
                    dispatch(view, MotionEvent.ACTION_DOWN, backspaceCancelTime, backspaceCancelTime, centerX, centerY)
                    dispatch(view, MotionEvent.ACTION_MOVE, backspaceCancelTime, backspaceCancelTime + 1, centerX, centerY + backspaceDistance)
                    dispatch(view, MotionEvent.ACTION_CANCEL, backspaceCancelTime, backspaceCancelTime + 2, centerX, centerY + backspaceDistance)
                    assertTrue("$widthDp dp symbol backspace cancel", actions.isEmpty())
                }
            }
        }
    }

    @Test fun symbolBackspaceLongPressRepeatsThroughTheAttachedProductionView() {
        ActivityScenario.launch(ImeTestActivity::class.java).use { scenario ->
            val actions = mutableListOf<KeyAction>()
            val repeated = CountDownLatch(1)
            lateinit var view: KeyboardView
            lateinit var backspace: Rect
            scenario.onActivity { activity ->
                val density = activity.resources.displayMetrics.density
                view = KeyboardView(activity).apply {
                    actionSink = KeyboardActionSink {
                        actions += it
                        if (it == KeyAction.Backspace(repeat = true)) repeated.countDown()
                    }
                    setMode(KeyboardMode.SYMBOLS)
                }
                activity.setContentView(view)
            }
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity { activity ->
                val density = activity.resources.displayMetrics.density
                val width = (412f * density).toInt()
                val height = (228f * density).toInt()
                view.measure(exact(width), exact(height)); view.layout(0, 0, width, height)
                val id = KeyboardLayouts.layout(KeyboardMode.SYMBOLS).rows.flatMap { it.keys }
                    .indexOfFirst { it.kind == KeyKind.BACKSPACE }
                backspace = bounds(view, id)
                val time = SystemClock.uptimeMillis()
                dispatch(view, MotionEvent.ACTION_DOWN, time, time, backspace.exactCenterX(), backspace.exactCenterY())
            }
            assertTrue("center long-press dispatches repeat while the pointer remains down", repeated.await(2, TimeUnit.SECONDS))
            scenario.onActivity {
                val time = SystemClock.uptimeMillis()
                dispatch(view, MotionEvent.ACTION_UP, time - 1, time, backspace.exactCenterX(), backspace.exactCenterY())
                assertTrue("center long-press repeats deletion", actions.contains(KeyAction.Backspace(repeat = true)))
            }
        }
    }

    @Test fun voicePunctuationDispatchesEveryDirectionOnceOnAndroidView() {
        ActivityScenario.launch(ImeTestActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val actions = mutableListOf<KeyAction>()
                val density = activity.resources.displayMetrics.density
                val view = KeyboardView(activity).apply {
                    actionSink = KeyboardActionSink { actions += it }
                    setMode(KeyboardMode.VOICE)
                }
                activity.setContentView(view)
                val width = (412f * density).toInt()
                val height = (228f * density).toInt()
                view.measure(exact(width), exact(height))
                view.layout(0, 0, width, height)
                val punctuation = Rect().also {
                    requireNotNull(view.accessibilityNodeProvider.createAccessibilityNodeInfo(5)).getBoundsInParent(it)
                }
                val centerX = punctuation.exactCenterX()
                val centerY = punctuation.exactCenterY()
                val distance = 30f * density

                listOf(
                    Triple(0f, 0f, "、"),
                    Triple(-distance, 0f, "。"),
                    Triple(0f, -distance, "？"),
                    Triple(distance, 0f, "！"),
                    Triple(0f, distance, "、"),
                ).forEachIndexed { index, (dx, dy, expected) ->
                    actions.clear()
                    val downTime = SystemClock.uptimeMillis() + index * 10L
                    dispatch(view, MotionEvent.ACTION_DOWN, downTime, downTime, centerX, centerY)
                    if (dx != 0f || dy != 0f) {
                        dispatch(view, MotionEvent.ACTION_MOVE, downTime, downTime + 1, centerX + dx, centerY + dy)
                    }
                    dispatch(view, MotionEvent.ACTION_UP, downTime, downTime + 2, centerX + dx, centerY + dy)
                    assertEquals("voice punctuation $expected", listOf(KeyAction.CommitText(expected)), actions)
                }
            }
        }
    }

    @Test fun conversionEnterDispatchesRawTapAndKatakanaFlicksOnAndroidViewAtPhoneAndTabletWidths() {
        ActivityScenario.launch(ImeTestActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val actions = mutableListOf<KeyAction>()
                val density = activity.resources.displayMetrics.density
                val view = KeyboardView(activity).apply {
                    actionSink = KeyboardActionSink { actions += it }
                    setMode(KeyboardMode.KANA)
                    setConversionActive(true)
                }
                activity.setContentView(view)
                val enterId = KeyboardLayouts.layout(KeyboardMode.KANA, false, true).rows
                    .flatMap { it.keys }
                    .indexOfFirst { it.kind == KeyKind.ENTER }
                check(enterId >= 0) { "Kana conversion layout has no Enter key" }

                val distance = 30f * density
                val gestures = listOf(
                    "tap" to Triple(0f, 0f, KeyAction.CommitWithoutConversion),
                    "up" to Triple(0f, -distance, KeyAction.ConvertToKatakana),
                    "left" to Triple(-distance, 0f, KeyAction.ConvertToKatakana),
                )
                listOf(412f, 840f).forEach { widthDp ->
                    val width = (widthDp * density).toInt()
                    val height = (228f * density).toInt()
                    view.measure(exact(width), exact(height))
                    view.layout(0, 0, width, height)
                    val enter = bounds(view, enterId)
                    gestures.forEachIndexed { index, (name, gesture) ->
                        val (dx, dy, expected) = gesture
                        val downTime = SystemClock.uptimeMillis() + index * 10L
                        actions.clear()
                        dispatch(view, MotionEvent.ACTION_DOWN, downTime, downTime, enter.exactCenterX(), enter.exactCenterY())
                        if (dx != 0f || dy != 0f) {
                            dispatch(view, MotionEvent.ACTION_MOVE, downTime, downTime + 1, enter.exactCenterX() + dx, enter.exactCenterY() + dy)
                        }
                        dispatch(view, MotionEvent.ACTION_UP, downTime, downTime + 2, enter.exactCenterX() + dx, enter.exactCenterY() + dy)
                        assertEquals("$widthDp dp conversion Enter $name", listOf(expected), actions)
                    }
                }
            }
        }
    }

    @Test fun kanaSmallKeyDispatchesEveryConfiguredFlickOnAndroidViewAtPhoneAndTabletWidths() {
        ActivityScenario.launch(ImeTestActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val actions = mutableListOf<KeyAction>()
                val density = activity.resources.displayMetrics.density
                val view = KeyboardView(activity).apply {
                    actionSink = KeyboardActionSink { actions += it }
                    setMode(KeyboardMode.KANA)
                }
                activity.setContentView(view)
                val accentId = KeyboardLayouts.layout(KeyboardMode.KANA, false, false).rows
                    .flatMap { it.keys }
                    .indexOfFirst { it.kind == KeyKind.ACCENT }
                check(accentId >= 0) { "Kana layout has no small-key accent" }
                val distance = 30f * density
                val flicks = listOf(
                    "tap" to Triple(0f, 0f, KeyAction.TransformKana(KanaTransform.CYCLE)),
                    "up" to Triple(0f, -distance, KeyAction.TransformKana(KanaTransform.DAKUTEN)),
                    "left" to Triple(-distance, 0f, KeyAction.TransformKana(KanaTransform.DAKUTEN)),
                    "right" to Triple(distance, 0f, KeyAction.TransformKana(KanaTransform.HANDAKUTEN)),
                    "down" to Triple(0f, distance, null),
                )

                listOf(412f, 840f).forEach { widthDp ->
                    val width = (widthDp * density).toInt()
                    val height = (228f * density).toInt()
                    view.measure(exact(width), exact(height))
                    view.layout(0, 0, width, height)
                    val accent = bounds(view, accentId)
                    val centerX = accent.exactCenterX()
                    val centerY = accent.exactCenterY()
                    flicks.forEachIndexed { index, (name, flick) ->
                        val (dx, dy, expected) = flick
                        actions.clear()
                        val downTime = SystemClock.uptimeMillis() + index * 10L
                        dispatch(view, MotionEvent.ACTION_DOWN, downTime, downTime, centerX, centerY)
                        if (dx != 0f || dy != 0f) {
                            dispatch(view, MotionEvent.ACTION_MOVE, downTime, downTime + 1, centerX + dx, centerY + dy)
                        }
                        dispatch(view, MotionEvent.ACTION_UP, downTime, downTime + 2, centerX + dx, centerY + dy)
                        val expectedActions = expected?.let(::listOf) ?: emptyList()
                        assertEquals("$widthDp dp Kana small-key $name", expectedActions, actions)
                    }
                }
            }
        }
    }

    @Test fun voiceBackspaceIsTapOnlyAcrossSixEqualColumnsOnAndroidView() {
        ActivityScenario.launch(ImeTestActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val actions = mutableListOf<KeyAction>()
                val density = activity.resources.displayMetrics.density
                val view = KeyboardView(activity).apply {
                    actionSink = KeyboardActionSink { actions += it }
                    setMode(KeyboardMode.VOICE)
                }
                activity.setContentView(view)
                listOf(412f, 840f).forEach { widthDp ->
                    val width = (widthDp * density).toInt()
                    val height = (228f * density).toInt()
                    view.measure(exact(width), exact(height))
                    view.layout(0, 0, width, height)
                    val columns = (3..8).map { bounds(view, it) }
                    assertTrue("$widthDp dp voice columns have equal face widths", columns.all {
                        kotlin.math.abs(it.width() - columns.first().width()) <= 1
                    })
                    assertTrue("$widthDp dp voice columns share their bottom row", columns.all {
                        it.top == columns.first().top && it.bottom == columns.first().bottom
                    })
                    val backspace = columns[4]
                    val centerX = backspace.exactCenterX()
                    val centerY = backspace.exactCenterY()
                    val distance = 30f * density

                    actions.clear()
                    val tapTime = SystemClock.uptimeMillis()
                    dispatch(view, MotionEvent.ACTION_DOWN, tapTime, tapTime, centerX, centerY)
                    dispatch(view, MotionEvent.ACTION_UP, tapTime, tapTime + 1, centerX, centerY)
                    assertEquals("$widthDp dp voice backspace tap", listOf(KeyAction.Backspace()), actions)

                    listOf(-distance to 0f, 0f to -distance, distance to 0f, 0f to distance)
                        .forEachIndexed { index, (dx, dy) ->
                            actions.clear()
                            val downTime = SystemClock.uptimeMillis() + index * 10L
                            dispatch(view, MotionEvent.ACTION_DOWN, downTime, downTime, centerX, centerY)
                            dispatch(view, MotionEvent.ACTION_MOVE, downTime, downTime + 1, centerX + dx, centerY + dy)
                            dispatch(view, MotionEvent.ACTION_UP, downTime, downTime + 2, centerX + dx, centerY + dy)
                            assertTrue("$widthDp dp voice backspace drag $index", actions.isEmpty())
                        }

                    actions.clear()
                    val returnTime = SystemClock.uptimeMillis()
                    dispatch(view, MotionEvent.ACTION_DOWN, returnTime, returnTime, centerX, centerY)
                    dispatch(view, MotionEvent.ACTION_MOVE, returnTime, returnTime + 1, centerX + distance, centerY)
                    dispatch(view, MotionEvent.ACTION_MOVE, returnTime, returnTime + 2, centerX + 10f * density, centerY)
                    dispatch(view, MotionEvent.ACTION_UP, returnTime, returnTime + 3, centerX + 10f * density, centerY)
                    assertEquals("$widthDp dp voice backspace returns to center", listOf(KeyAction.Backspace()), actions)
                }
            }
        }
    }

    private fun dispatch(view: KeyboardView, action: Int, downTime: Long, eventTime: Long, x: Float, y: Float) {
        MotionEvent.obtain(downTime, eventTime, action, x, y, 0).also {
            view.dispatchTouchEvent(it)
            it.recycle()
        }
    }

    private fun bounds(view: KeyboardView, viewId: Int): Rect = Rect().also {
        requireNotNull(view.accessibilityNodeProvider.createAccessibilityNodeInfo(viewId)).getBoundsInParent(it)
    }

    private fun exact(size: Int) = android.view.View.MeasureSpec.makeMeasureSpec(size, android.view.View.MeasureSpec.EXACTLY)
}
