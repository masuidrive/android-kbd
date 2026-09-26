package com.masuidrive.gestureime.keyboard

import android.graphics.Rect
import android.os.SystemClock
import android.view.MotionEvent
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.masuidrive.gestureime.ImeTestActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class KeyboardViewVoicePunctuationTest {
    @Test fun qwertyAccentHorizontalFlickCancelsTheHoldTimerOnAndroidViewAtPhoneAndTabletWidths() {
        ActivityScenario.launch(ImeTestActivity::class.java).use { scenario ->
            val actions = mutableListOf<KeyAction>()
            lateinit var view: KeyboardView
            lateinit var a: Rect
            var density = 0f
            val activeAccents = KeyboardView::class.java.getDeclaredField("accentActive").also { it.isAccessible = true }

            scenario.onActivity { activity ->
                density = activity.resources.displayMetrics.density
                view = KeyboardView(activity).apply {
                    actionSink = KeyboardActionSink { actions += it }
                    setMode(KeyboardMode.QWERTY)
                }
                activity.setContentView(view)
            }

            listOf(412f, 840f).forEachIndexed { widthIndex, widthDp ->
                scenario.onActivity {
                    val width = (widthDp * density).toInt()
                    val height = (228f * density).toInt()
                    view.measure(exact(width), exact(height)); view.layout(0, 0, width, height)
                    val aId = KeyboardLayouts.layout(KeyboardMode.QWERTY).rows.flatMap { it.keys }
                        .indexOfFirst { it.id == "key-a" }
                    a = bounds(view, aId)
                }

                listOf(-24f, 24f).forEachIndexed { directionIndex, dxDp ->
                    val downTime = SystemClock.uptimeMillis() + (widthIndex * 10L + directionIndex) * 10L
                    scenario.onActivity {
                        actions.clear()
                        dispatch(view, MotionEvent.ACTION_DOWN, downTime, downTime, a.exactCenterX(), a.exactCenterY())
                        dispatch(view, MotionEvent.ACTION_MOVE, downTime, downTime + 1, a.exactCenterX() + dxDp * density, a.exactCenterY())
                    }
                    Thread.sleep(KeyboardView.ACCENT_DELAY_MS + 100)
                    scenario.onActivity {
                        assertFalse("$widthDp dp $dxDp dp horizontal move opens accent choices", (activeAccents.get(view) as Set<*>).isNotEmpty())
                        dispatch(view, MotionEvent.ACTION_UP, downTime, downTime + 2, a.exactCenterX() + dxDp * density, a.exactCenterY())
                        assertEquals("$widthDp dp $dxDp dp horizontal move commits center once", listOf(KeyAction.CommitText("a")), actions)
                    }
                }

                val downTime = SystemClock.uptimeMillis() + 100L
                scenario.onActivity {
                    actions.clear()
                    dispatch(view, MotionEvent.ACTION_DOWN, downTime, downTime, a.exactCenterX(), a.exactCenterY())
                }
                Thread.sleep(KeyboardView.ACCENT_DELAY_MS + 100)
                scenario.onActivity {
                    assertTrue("$widthDp dp center hold opens accent choices", (activeAccents.get(view) as Set<*>).isNotEmpty())
                    dispatch(view, MotionEvent.ACTION_UP, downTime, downTime + 1, a.exactCenterX(), a.exactCenterY())
                    assertEquals("$widthDp dp center hold commits the first accent", listOf(KeyAction.CommitText("à")), actions)
                }
            }
        }
    }

    @Test fun emojiRailAndSymbolBackspaceUseProductionMotionEventsAtPhoneAndTabletWidths() {
        ActivityScenario.launch(ImeTestActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val actions = mutableListOf<KeyAction>()
                val density = activity.resources.displayMetrics.density
                val view = KeyboardView(activity).apply {
                    actionSink = KeyboardActionSink { actions += it }
                    setDualFlickEnabled(true)
                    setFlickSensitivities(FlickSensitivity.HIGH, FlickSensitivity.LOW)
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
                    val emojiRailWidth = bounds(view, 0).width()
                    assertTrue(
                        "$widthDp dp rail width expected $kanaRailWidth±1 but was $emojiRailWidth",
                        kotlin.math.abs(kanaRailWidth - emojiRailWidth) <= 1,
                    )
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
                        view.setMode(KeyboardMode.EMOJI)
                        actions.clear()
                        val time = SystemClock.uptimeMillis() + index * 10L
                        dispatch(view, MotionEvent.ACTION_DOWN, time, time, railCenterX, railCenterY)
                        dispatch(view, MotionEvent.ACTION_MOVE, time, time + 1, railCenterX + dx, railCenterY + dy)
                        dispatch(view, MotionEvent.ACTION_UP, time, time + 2, railCenterX + dx, railCenterY + dy)
                        assertEquals("$widthDp dp emoji rail falls back to its tap for ${index} direction", listOf(KeyAction.SwitchLayer(KeyboardMode.KANA)), actions)
                    }
                    view.setMode(KeyboardMode.EMOJI)
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
                    dispatch(view, MotionEvent.ACTION_MOVE, returnTime, returnTime + 2, railCenterX + 9f * density, railCenterY)
                    dispatch(view, MotionEvent.ACTION_UP, returnTime, returnTime + 3, railCenterX + 9f * density, railCenterY)
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
                    val azFixedThresholdTime = SystemClock.uptimeMillis()
                    dispatch(view, MotionEvent.ACTION_DOWN, azFixedThresholdTime, azFixedThresholdTime, azCenterX, azCenterY)
                    dispatch(view, MotionEvent.ACTION_MOVE, azFixedThresholdTime, azFixedThresholdTime + 1, azCenterX, azCenterY - 18.5f * density)
                    dispatch(view, MotionEvent.ACTION_UP, azFixedThresholdTime, azFixedThresholdTime + 2, azCenterX, azCenterY - 18.5f * density)
                    assertEquals("$widthDp dp emoji AZ retains fixed threshold", listOf(KeyAction.SwitchLayer(KeyboardMode.KANA)), actions)
                    view.setMode(KeyboardMode.EMOJI)
                    actions.clear()
                    val azReturnTime = SystemClock.uptimeMillis()
                    dispatch(view, MotionEvent.ACTION_DOWN, azReturnTime, azReturnTime, azCenterX, azCenterY)
                    dispatch(view, MotionEvent.ACTION_MOVE, azReturnTime, azReturnTime + 1, azCenterX, azCenterY + distance)
                    dispatch(view, MotionEvent.ACTION_MOVE, azReturnTime, azReturnTime + 2, azCenterX, azCenterY + 9f * density)
                    dispatch(view, MotionEvent.ACTION_UP, azReturnTime, azReturnTime + 3, azCenterX, azCenterY + 9f * density)
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
                        assertEquals("$widthDp dp symbol backspace falls back to its tap for direction $index", listOf(KeyAction.Backspace()), actions)
                    }
                    actions.clear()
                    val backspaceThresholdTime = SystemClock.uptimeMillis()
                    dispatch(view, MotionEvent.ACTION_DOWN, backspaceThresholdTime, backspaceThresholdTime, centerX, centerY)
                    dispatch(view, MotionEvent.ACTION_MOVE, backspaceThresholdTime, backspaceThresholdTime + 1, centerX, centerY + 15f * density)
                    dispatch(view, MotionEvent.ACTION_UP, backspaceThresholdTime, backspaceThresholdTime + 2, centerX, centerY + 15f * density)
                    assertEquals("$widthDp dp symbol backspace stays tap below threshold", listOf(KeyAction.Backspace()), actions)
                    actions.clear()
                    val backspaceReturnTime = SystemClock.uptimeMillis()
                    dispatch(view, MotionEvent.ACTION_DOWN, backspaceReturnTime, backspaceReturnTime, centerX, centerY)
                    dispatch(view, MotionEvent.ACTION_MOVE, backspaceReturnTime, backspaceReturnTime + 1, centerX, centerY + backspaceDistance)
                    dispatch(view, MotionEvent.ACTION_MOVE, backspaceReturnTime, backspaceReturnTime + 2, centerX, centerY + 9f * density)
                    dispatch(view, MotionEvent.ACTION_UP, backspaceReturnTime, backspaceReturnTime + 3, centerX, centerY + 9f * density)
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

    @Test fun unassignedBackspaceFlickCancelsRepeatAndReleasesAsOneTapThroughTheAttachedProductionView() {
        ActivityScenario.launch(ImeTestActivity::class.java).use { scenario ->
            val actions = mutableListOf<KeyAction>()
            lateinit var view: KeyboardView
            lateinit var backspace: Rect
            scenario.onActivity { activity ->
                view = KeyboardView(activity).apply {
                    actionSink = KeyboardActionSink { actions += it }
                    setMode(KeyboardMode.QWERTY)
                }
                activity.setContentView(view)
            }
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity { activity ->
                val density = activity.resources.displayMetrics.density
                view.measure(exact((412f * density).toInt()), exact((228f * density).toInt()))
                view.layout(0, 0, view.measuredWidth, view.measuredHeight)
                val backspaceId = KeyboardLayouts.layout(KeyboardMode.QWERTY).rows.flatMap { it.keys }
                    .indexOfFirst { it.kind == KeyKind.BACKSPACE }
                backspace = bounds(view, backspaceId)
                val time = SystemClock.uptimeMillis()
                dispatch(view, MotionEvent.ACTION_DOWN, time, time, backspace.exactCenterX(), backspace.exactCenterY())
                dispatch(view, MotionEvent.ACTION_MOVE, time, time + 1, backspace.exactCenterX() - 30f * density, backspace.exactCenterY())
            }
            Thread.sleep(KeyboardView.DELETE_REPEAT_DELAY_MS + KeyboardView.DELETE_REPEAT_INTERVAL_MS * 2)
            scenario.onActivity {
                val time = SystemClock.uptimeMillis()
                val dragX = backspace.exactCenterX() - 30f * view.resources.displayMetrics.density
                dispatch(view, MotionEvent.ACTION_UP, time - 1, time, dragX, backspace.exactCenterY())
                assertEquals(listOf(KeyAction.Backspace()), actions)
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
                    setFlickSensitivities(FlickSensitivity.HIGH, FlickSensitivity.LOW)
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
                listOf(17f to "、", 18.5f to "。").forEachIndexed { index, (dxDp, expected) ->
                    actions.clear()
                    val downTime = SystemClock.uptimeMillis() + 100L + index * 10L
                    dispatch(view, MotionEvent.ACTION_DOWN, downTime, downTime, centerX, centerY)
                    dispatch(view, MotionEvent.ACTION_MOVE, downTime, downTime + 1, centerX - dxDp * density, centerY)
                    dispatch(view, MotionEvent.ACTION_UP, downTime, downTime + 2, centerX - dxDp * density, centerY)
                    assertEquals("voice punctuation retains fixed threshold $dxDp", listOf(KeyAction.CommitText(expected)), actions)
                }
            }
        }
    }

    @Test fun numberPeriodAndSymbolSwitchesUseProductionMotionEventsAtPhoneAndTabletWidths() {
        ActivityScenario.launch(ImeTestActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val actions = mutableListOf<KeyAction>()
                val density = activity.resources.displayMetrics.density
                val view = KeyboardView(activity).apply {
                    actionSink = KeyboardActionSink { actions += it }
                }
                activity.setContentView(view)
                val distance = 30f * density

                listOf(412f, 840f).forEach { widthDp ->
                    val width = (widthDp * density).toInt()
                    val height = (228f * density).toInt()
                    view.setMode(KeyboardMode.NUMBERS)
                    view.measure(exact(width), exact(height))
                    view.layout(0, 0, width, height)
                    val periodId = KeyboardLayouts.layout(KeyboardMode.NUMBERS).rows.flatMap { it.keys }
                        .indexOfFirst { it.id == "number-period" }
                    check(periodId >= 0) { "Numbers layout has no period key" }
                    val period = bounds(view, periodId)
                    val gestures: List<Pair<String, Triple<Float, Float, KeyAction?>>> = listOf(
                        "tap" to Triple(0f, 0f, KeyAction.CommitText(".")),
                        "left" to Triple(-distance, 0f, KeyAction.CommitText(",")),
                        "up" to Triple(0f, -distance, KeyAction.CommitText(".")),
                        "right" to Triple(distance, 0f, KeyAction.CommitText("=")),
                        "down" to Triple(0f, distance, KeyAction.CommitText(".")),
                    )
                    gestures.forEachIndexed { index, (name, gesture) ->
                        val (dx, dy, expected) = gesture
                        actions.clear()
                        val time = SystemClock.uptimeMillis() + index * 10L
                        dispatch(view, MotionEvent.ACTION_DOWN, time, time, period.exactCenterX(), period.exactCenterY())
                        if (dx != 0f || dy != 0f) {
                            dispatch(view, MotionEvent.ACTION_MOVE, time, time + 1, period.exactCenterX() + dx, period.exactCenterY() + dy)
                        }
                        dispatch(view, MotionEvent.ACTION_UP, time, time + 2, period.exactCenterX() + dx, period.exactCenterY() + dy)
                        assertEquals("$widthDp dp number period $name", listOf(requireNotNull(expected)), actions)
                    }

                    actions.clear()
                    var time = SystemClock.uptimeMillis()
                    dispatch(view, MotionEvent.ACTION_DOWN, time, time, period.exactCenterX(), period.exactCenterY())
                    dispatch(view, MotionEvent.ACTION_MOVE, time, time + 1, period.exactCenterX() + 15f * density, period.exactCenterY())
                    dispatch(view, MotionEvent.ACTION_UP, time, time + 2, period.exactCenterX() + 15f * density, period.exactCenterY())
                    assertEquals("$widthDp dp number period stays tap below threshold", listOf(KeyAction.CommitText(".")), actions)

                    actions.clear()
                    time = SystemClock.uptimeMillis()
                    dispatch(view, MotionEvent.ACTION_DOWN, time, time, period.exactCenterX(), period.exactCenterY())
                    dispatch(view, MotionEvent.ACTION_MOVE, time, time + 1, period.exactCenterX() + distance, period.exactCenterY())
                    dispatch(view, MotionEvent.ACTION_MOVE, time, time + 2, period.exactCenterX() + 8f * density, period.exactCenterY())
                    dispatch(view, MotionEvent.ACTION_UP, time, time + 3, period.exactCenterX() + 8f * density, period.exactCenterY())
                    assertEquals("$widthDp dp number period returns to center", listOf(KeyAction.CommitText(".")), actions)

                    actions.clear()
                    time = SystemClock.uptimeMillis()
                    dispatch(view, MotionEvent.ACTION_DOWN, time, time, period.exactCenterX(), period.exactCenterY())
                    dispatch(view, MotionEvent.ACTION_MOVE, time, time + 1, period.exactCenterX() - distance, period.exactCenterY())
                    dispatch(view, MotionEvent.ACTION_CANCEL, time, time + 2, period.exactCenterX() - distance, period.exactCenterY())
                    assertTrue("$widthDp dp number period cancel", actions.isEmpty())

                    val numberMinusId = KeyboardLayouts.layout(KeyboardMode.NUMBERS).rows.flatMap { it.keys }
                        .indexOfFirst { it.id == "five--" }
                    check(numberMinusId >= 0) { "Numbers layout has no minus key" }
                    val minus = bounds(view, numberMinusId)
                    listOf(
                        "tap" to Triple(0f, 0f, KeyAction.CommitText("-")),
                        "left" to Triple(-distance, 0f, KeyAction.CommitText("+")),
                        "up" to Triple(0f, -distance, KeyAction.CommitText("/")),
                        "right" to Triple(distance, 0f, KeyAction.CommitText("*")),
                        "down" to Triple(0f, distance, KeyAction.CommitText(",")),
                    ).forEachIndexed { index, (name, gesture) ->
                        val (dx, dy, expected) = gesture
                        actions.clear()
                        time = SystemClock.uptimeMillis() + index * 10L
                        dispatch(view, MotionEvent.ACTION_DOWN, time, time, minus.exactCenterX(), minus.exactCenterY())
                        if (dx != 0f || dy != 0f) {
                            dispatch(view, MotionEvent.ACTION_MOVE, time, time + 1, minus.exactCenterX() + dx, minus.exactCenterY() + dy)
                        }
                        dispatch(view, MotionEvent.ACTION_UP, time, time + 2, minus.exactCenterX() + dx, minus.exactCenterY() + dy)
                        assertEquals("$widthDp dp number minus $name", listOf(expected), actions)
                    }

                    listOf(KeyboardMode.KANA, KeyboardMode.NUMBERS, KeyboardMode.QWERTY, KeyboardMode.EMOJI).forEach { mode ->
                        view.setMode(mode)
                        view.measure(exact(width), exact(height))
                        view.layout(0, 0, width, height)
                        val symbolId = KeyboardLayouts.layout(mode).rows.flatMap { it.keys }
                            .indexOfFirst { it.center?.action == KeyAction.SwitchLayer(KeyboardMode.SYMBOLS) }
                        check(symbolId >= 0) { "$mode layout has no symbol switch" }
                        val symbol = bounds(view, symbolId)
                        assertTrue(
                            "$widthDp dp $mode symbol label",
                            view.accessibilityNodeProvider.createAccessibilityNodeInfo(symbolId)
                                ?.contentDescription?.toString()?.startsWith("タップ ?}") == true,
                        )
                        actions.clear()
                        time = SystemClock.uptimeMillis()
                        dispatch(view, MotionEvent.ACTION_DOWN, time, time, symbol.exactCenterX(), symbol.exactCenterY())
                        dispatch(view, MotionEvent.ACTION_UP, time, time + 1, symbol.exactCenterX(), symbol.exactCenterY())
                        assertEquals("$widthDp dp $mode symbol tap", listOf(KeyAction.SwitchLayer(KeyboardMode.SYMBOLS)), actions)
                    }

                    view.setMode(KeyboardMode.SYMBOLS)
                    view.measure(exact(width), exact(height))
                    view.layout(0, 0, width, height)
                    val symbolKeys = KeyboardLayouts.layout(KeyboardMode.SYMBOLS).rows.flatMap { it.keys }
                    val tabId = symbolKeys.indexOfFirst { it.id == "tab" }
                    val minusId = symbolKeys.indexOfFirst { it.id == "key--" }
                    check(tabId >= 0 && minusId >= 0) { "Symbols layout is missing Tab or minus" }
                    val tab = bounds(view, tabId)
                    assertTrue("$widthDp dp Tab follows minus", tab.left >= bounds(view, minusId).right)
                    actions.clear()
                    time = SystemClock.uptimeMillis()
                    dispatch(view, MotionEvent.ACTION_DOWN, time, time, tab.exactCenterX(), tab.exactCenterY())
                    dispatch(view, MotionEvent.ACTION_UP, time, time + 1, tab.exactCenterX(), tab.exactCenterY())
                    assertEquals("$widthDp dp rightmost Tab tap", listOf(KeyAction.Tab), actions)
                }
            }
        }
    }

    @Test fun nonconvertingEnterUsesPasteUpAndControlJDownOnEveryProductionSurface() {
        ActivityScenario.launch(ImeTestActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val actions = mutableListOf<KeyAction>()
                val density = activity.resources.displayMetrics.density
                val view = KeyboardView(activity).apply {
                    actionSink = KeyboardActionSink { actions += it }
                }
                activity.setContentView(view)
                val distance = 30f * density
                val modes = listOf(
                    KeyboardMode.KANA,
                    KeyboardMode.NUMBERS,
                    KeyboardMode.QWERTY,
                    KeyboardMode.SYMBOLS,
                    KeyboardMode.VOICE,
                )

                listOf(412f, 840f).forEach { widthDp ->
                    val width = (widthDp * density).toInt()
                    val height = (228f * density).toInt()
                    modes.forEach { mode ->
                        view.setMode(mode)
                        view.setConversionState(active = false, candidateSelected = false)
                        view.measure(exact(width), exact(height))
                        view.layout(0, 0, width, height)
                        val enterId = KeyboardLayouts.layout(mode).rows.flatMap { it.keys }
                            .indexOfFirst { it.kind == KeyKind.ENTER }
                        check(enterId >= 0) { "$mode layout has no Enter key" }
                        val enter = bounds(view, enterId)
                        val centerX = enter.exactCenterX()
                        val centerY = enter.exactCenterY()
                        val description = view.accessibilityNodeProvider.createAccessibilityNodeInfo(enterId)
                            ?.contentDescription?.toString().orEmpty()
                        assertTrue("$widthDp dp $mode accessibility Paste up", description.contains("上 paste"))
                        assertTrue("$widthDp dp $mode accessibility C-j down", description.contains("下 C-j"))

                        val gestures: List<Pair<String, Triple<Float, Float, KeyAction?>>> = listOf(
                            "tap" to Triple(0f, 0f, KeyAction.Enter),
                            "up" to Triple(0f, -distance, KeyAction.Paste),
                            "down" to Triple(0f, distance, KeyAction.ModifiedKey("j", Modifier.CTRL)),
                            "left" to Triple(-distance, 0f, KeyAction.Enter),
                            "right" to Triple(distance, 0f, KeyAction.Enter),
                        )
                        gestures.forEachIndexed { index, (name, gesture) ->
                            val (dx, dy, expected) = gesture
                            actions.clear()
                            val time = SystemClock.uptimeMillis() + index * 10L
                            dispatch(view, MotionEvent.ACTION_DOWN, time, time, centerX, centerY)
                            if (dx != 0f || dy != 0f) {
                                dispatch(view, MotionEvent.ACTION_MOVE, time, time + 1, centerX + dx, centerY + dy)
                            }
                            dispatch(view, MotionEvent.ACTION_UP, time, time + 2, centerX + dx, centerY + dy)
                            assertEquals("$widthDp dp $mode Enter $name", expected?.let(::listOf).orEmpty(), actions)
                        }

                        actions.clear()
                        var time = SystemClock.uptimeMillis()
                        dispatch(view, MotionEvent.ACTION_DOWN, time, time, centerX, centerY)
                        dispatch(view, MotionEvent.ACTION_MOVE, time, time + 1, centerX, centerY - 15f * density)
                        dispatch(view, MotionEvent.ACTION_UP, time, time + 2, centerX, centerY - 15f * density)
                        assertEquals("$widthDp dp $mode Enter stays tap below threshold", listOf(KeyAction.Enter), actions)

                        actions.clear()
                        time = SystemClock.uptimeMillis()
                        dispatch(view, MotionEvent.ACTION_DOWN, time, time, centerX, centerY)
                        dispatch(view, MotionEvent.ACTION_MOVE, time, time + 1, centerX, centerY - distance)
                        dispatch(view, MotionEvent.ACTION_MOVE, time, time + 2, centerX, centerY - 9f * density)
                        dispatch(view, MotionEvent.ACTION_UP, time, time + 3, centerX, centerY - 9f * density)
                        assertEquals("$widthDp dp $mode Enter returns to center", listOf(KeyAction.Enter), actions)

                        actions.clear()
                        time = SystemClock.uptimeMillis()
                        dispatch(view, MotionEvent.ACTION_DOWN, time, time, centerX, centerY)
                        dispatch(view, MotionEvent.ACTION_MOVE, time, time + 1, centerX, centerY + distance)
                        dispatch(view, MotionEvent.ACTION_CANCEL, time, time + 2, centerX, centerY + distance)
                        assertTrue("$widthDp dp $mode Enter cancel", actions.isEmpty())
                    }
                }
            }
        }
    }

    @Test fun conversionCandidateAndConfirmButtonsUseProductionMotionEventsAtPhoneAndTabletWidths() {
        ActivityScenario.launch(ImeTestActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val actions = mutableListOf<KeyAction>()
                val density = activity.resources.displayMetrics.density
                val view = KeyboardView(activity).apply {
                    actionSink = KeyboardActionSink { actions += it }
                    setMode(KeyboardMode.KANA)
                    setConversionState(active = true, candidateSelected = false)
                }
                activity.setContentView(view)
                val enterId = KeyboardLayouts.layout(KeyboardMode.KANA, false, true).rows
                    .flatMap { it.keys }
                    .indexOfFirst { it.kind == KeyKind.ENTER }
                val spaceId = KeyboardLayouts.layout(KeyboardMode.KANA, false, true).rows
                    .flatMap { it.keys }
                    .indexOfFirst { it.kind == KeyKind.SPACE }
                check(enterId >= 0) { "Kana conversion layout has no Enter key" }
                check(spaceId >= 0) { "Kana conversion layout has no Space key" }

                val distance = 30f * density
                listOf(412f, 840f).forEach { widthDp ->
                    val width = (widthDp * density).toInt()
                    val height = (228f * density).toInt()
                    view.measure(exact(width), exact(height))
                    view.layout(0, 0, width, height)
                    var enter = bounds(view, enterId)
                    val space = bounds(view, spaceId)
                    assertTrue(
                        "$widthDp dp unselected Enter label",
                        view.accessibilityNodeProvider.createAccessibilityNodeInfo(enterId)
                            ?.contentDescription?.toString()?.startsWith("タップ 無変換") == true,
                    )

                    actions.clear()
                    var downTime = SystemClock.uptimeMillis()
                    dispatch(view, MotionEvent.ACTION_DOWN, downTime, downTime, enter.exactCenterX(), enter.exactCenterY())
                    dispatch(view, MotionEvent.ACTION_UP, downTime, downTime + 1, enter.exactCenterX(), enter.exactCenterY())
                    assertEquals("$widthDp dp unselected Enter tap", listOf(KeyAction.CommitWithoutConversion), actions)

                    actions.clear()
                    downTime += 10
                    dispatch(view, MotionEvent.ACTION_DOWN, downTime, downTime, space.exactCenterX(), space.exactCenterY())
                    dispatch(view, MotionEvent.ACTION_UP, downTime, downTime + 1, space.exactCenterX(), space.exactCenterY())
                    assertEquals("$widthDp dp candidate tap", listOf(KeyAction.CycleCandidate), actions)

                    view.setConversionState(active = true, candidateSelected = true)
                    view.measure(exact(width), exact(height))
                    view.layout(0, 0, width, height)
                    enter = bounds(view, enterId)
                    assertTrue(
                        "$widthDp dp selected Enter label",
                        view.accessibilityNodeProvider.createAccessibilityNodeInfo(enterId)
                            ?.contentDescription?.toString()?.startsWith("タップ 確定") == true,
                    )

                    val gestures = listOf(
                        "tap" to Triple(0f, 0f, KeyAction.CommitConversion),
                        "up" to Triple(0f, -distance, KeyAction.ConvertToKatakana),
                        "left" to Triple(-distance, 0f, KeyAction.ConvertToKatakana),
                    )
                    gestures.forEachIndexed { index, (name, gesture) ->
                        val (dx, dy, expected) = gesture
                        downTime = SystemClock.uptimeMillis() + index * 10L
                        actions.clear()
                        dispatch(view, MotionEvent.ACTION_DOWN, downTime, downTime, enter.exactCenterX(), enter.exactCenterY())
                        if (dx != 0f || dy != 0f) {
                            dispatch(view, MotionEvent.ACTION_MOVE, downTime, downTime + 1, enter.exactCenterX() + dx, enter.exactCenterY() + dy)
                        }
                        dispatch(view, MotionEvent.ACTION_UP, downTime, downTime + 2, enter.exactCenterX() + dx, enter.exactCenterY() + dy)
                        assertEquals("$widthDp dp selected Enter $name", listOf(expected), actions)
                    }

                    listOf("right" to (distance to 0f), "down" to (0f to distance)).forEachIndexed { index, (name, delta) ->
                        actions.clear()
                        downTime = SystemClock.uptimeMillis() + index * 10L
                        dispatch(view, MotionEvent.ACTION_DOWN, downTime, downTime, enter.exactCenterX(), enter.exactCenterY())
                        dispatch(view, MotionEvent.ACTION_MOVE, downTime, downTime + 1, enter.exactCenterX() + delta.first, enter.exactCenterY() + delta.second)
                        dispatch(view, MotionEvent.ACTION_UP, downTime, downTime + 2, enter.exactCenterX() + delta.first, enter.exactCenterY() + delta.second)
                        assertEquals("$widthDp dp selected Enter falls back to confirm for $name", listOf(KeyAction.CommitConversion), actions)
                    }

                    actions.clear()
                    downTime = SystemClock.uptimeMillis()
                    dispatch(view, MotionEvent.ACTION_DOWN, downTime, downTime, enter.exactCenterX(), enter.exactCenterY())
                    dispatch(view, MotionEvent.ACTION_MOVE, downTime, downTime + 1, enter.exactCenterX() + 15f * density, enter.exactCenterY())
                    dispatch(view, MotionEvent.ACTION_UP, downTime, downTime + 2, enter.exactCenterX() + 15f * density, enter.exactCenterY())
                    assertEquals("$widthDp dp selected Enter stays tap below threshold", listOf(KeyAction.CommitConversion), actions)

                    actions.clear()
                    downTime = SystemClock.uptimeMillis()
                    dispatch(view, MotionEvent.ACTION_DOWN, downTime, downTime, enter.exactCenterX(), enter.exactCenterY())
                    dispatch(view, MotionEvent.ACTION_MOVE, downTime, downTime + 1, enter.exactCenterX() - distance, enter.exactCenterY())
                    dispatch(view, MotionEvent.ACTION_MOVE, downTime, downTime + 2, enter.exactCenterX() - 8f * density, enter.exactCenterY())
                    dispatch(view, MotionEvent.ACTION_UP, downTime, downTime + 3, enter.exactCenterX() - 8f * density, enter.exactCenterY())
                    assertEquals("$widthDp dp selected Enter returns to confirm", listOf(KeyAction.CommitConversion), actions)

                    actions.clear()
                    downTime = SystemClock.uptimeMillis()
                    dispatch(view, MotionEvent.ACTION_DOWN, downTime, downTime, enter.exactCenterX(), enter.exactCenterY())
                    dispatch(view, MotionEvent.ACTION_MOVE, downTime, downTime + 1, enter.exactCenterX(), enter.exactCenterY() - distance)
                    dispatch(view, MotionEvent.ACTION_CANCEL, downTime, downTime + 2, enter.exactCenterX(), enter.exactCenterY() - distance)
                    assertTrue("$widthDp dp selected Enter cancel", actions.isEmpty())

                    actions.clear()
                    downTime = SystemClock.uptimeMillis()
                    dispatch(view, MotionEvent.ACTION_DOWN, downTime, downTime, space.exactCenterX(), space.exactCenterY())
                    dispatch(view, MotionEvent.ACTION_UP, downTime, downTime + 1, space.exactCenterX(), space.exactCenterY())
                    assertEquals("$widthDp dp selected candidate advances", listOf(KeyAction.CycleCandidate), actions)

                    view.setConversionState(active = true, candidateSelected = false)
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
                    "down" to Triple(0f, distance, KeyAction.TransformKana(KanaTransform.CYCLE)),
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
                            assertEquals("$widthDp dp voice backspace drag $index falls back to tap", listOf(KeyAction.Backspace()), actions)
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

    @Test fun flickSensitivityGroupsChangeOnlyTheirAssignedLayoutsThroughAttachedMotionEvents() {
        ActivityScenario.launch(ImeTestActivity::class.java).use { scenario ->
            val actions = mutableListOf<KeyAction>()
            lateinit var view: KeyboardView
            var density = 0f
            scenario.onActivity { activity ->
                density = activity.resources.displayMetrics.density
                view = KeyboardView(activity).apply {
                    actionSink = KeyboardActionSink { actions += it }
                }
                activity.setContentView(view)
            }

            listOf(412f, 840f).forEachIndexed { widthIndex, widthDp ->
                scenario.onActivity {
                    val width = (widthDp * density).toInt()
                    val height = (228f * density).toInt()
                    fun key(mode: KeyboardMode, keyId: String): Rect {
                        view.setMode(mode)
                        view.measure(exact(width), exact(height)); view.layout(0, 0, width, height)
                        val id = KeyboardLayouts.layout(mode).rows.flatMap { it.keys }.indexOfFirst { it.id == keyId }
                        check(id >= 0) { "missing $keyId in $mode" }
                        return bounds(view, id)
                    }
                    fun gesture(mode: KeyboardMode, keyId: String, dxDp: Float, dyDp: Float, endAction: Int = MotionEvent.ACTION_UP): List<KeyAction> {
                        actions.clear()
                        val bounds = key(mode, keyId)
                        val time = SystemClock.uptimeMillis() + widthIndex * 100L
                        val x = bounds.exactCenterX(); val y = bounds.exactCenterY()
                        dispatch(view, MotionEvent.ACTION_DOWN, time, time, x, y)
                        dispatch(view, MotionEvent.ACTION_MOVE, time, time + 1, x + dxDp * density, y + dyDp * density)
                        dispatch(view, endAction, time, time + 2, x + dxDp * density, y + dyDp * density)
                        return actions.toList()
                    }

                    view.setFlickSensitivities(FlickSensitivity.HIGH, FlickSensitivity.LOW)
                    assertEquals("$widthDp dp kana high new boundary", listOf(KeyAction.KanaInput("う")), gesture(KeyboardMode.KANA, "kana-あ", 0f, -11f))
                    assertEquals("$widthDp dp kana high", listOf(KeyAction.KanaInput("う")), gesture(KeyboardMode.KANA, "kana-あ", 0f, -13f))
                    assertEquals("$widthDp dp qwerty remains low", listOf(KeyAction.CommitText("q")), gesture(KeyboardMode.QWERTY, "key-q", 0f, 20f))
                    assertEquals("$widthDp dp number unassigned", listOf(KeyAction.CommitText(".")), gesture(KeyboardMode.NUMBERS, "number-period", 0f, -13f))

                    view.setFlickSensitivities(FlickSensitivity.LOW, FlickSensitivity.HIGH)
                    assertEquals("$widthDp dp kana remains low", listOf(KeyAction.KanaInput("あ")), gesture(KeyboardMode.KANA, "kana-あ", 0f, -20f))
                    assertEquals("$widthDp dp kana low new boundary", listOf(KeyAction.KanaInput("う")), gesture(KeyboardMode.KANA, "kana-あ", 0f, -25f))
                    assertEquals("$widthDp dp qwerty high new boundary", listOf(KeyAction.CommitText("1")), gesture(KeyboardMode.QWERTY, "key-q", 0f, 11f))
                    assertEquals("$widthDp dp qwerty high", listOf(KeyAction.CommitText("1")), gesture(KeyboardMode.QWERTY, "key-q", 0f, 20f))
                    view.setFlickSensitivities(FlickSensitivity.STANDARD, FlickSensitivity.STANDARD)
                    assertEquals("$widthDp dp kana standard new boundary", listOf(KeyAction.KanaInput("う")), gesture(KeyboardMode.KANA, "kana-あ", 0f, -17f))
                    assertEquals("$widthDp dp qwerty standard new boundary", listOf(KeyAction.CommitText("1")), gesture(KeyboardMode.QWERTY, "key-q", 0f, 17f))
                    listOf(
                        FlickSensitivity.HIGH to 10f,
                        FlickSensitivity.STANDARD to 16f,
                        FlickSensitivity.LOW to 24f,
                    ).forEach { (sensitivity, selectionDp) ->
                        view.setFlickSensitivities(sensitivity, sensitivity)
                        assertEquals("$widthDp dp $sensitivity kana below", listOf(KeyAction.KanaInput("あ")), gesture(KeyboardMode.KANA, "kana-あ", 0f, -selectionDp + .5f))
                        assertEquals("$widthDp dp $sensitivity kana above", listOf(KeyAction.KanaInput("う")), gesture(KeyboardMode.KANA, "kana-あ", 0f, -selectionDp - .5f))
                        assertEquals("$widthDp dp $sensitivity qwerty below", listOf(KeyAction.CommitText("q")), gesture(KeyboardMode.QWERTY, "key-q", 0f, selectionDp - .5f))
                        assertEquals("$widthDp dp $sensitivity qwerty above", listOf(KeyAction.CommitText("1")), gesture(KeyboardMode.QWERTY, "key-q", 0f, selectionDp + .5f))
                    }
                    view.setFlickSensitivities(FlickSensitivity.LOW, FlickSensitivity.HIGH)
                    assertTrue("$widthDp dp cancel", gesture(KeyboardMode.KANA, "kana-あ", 0f, -13f, MotionEvent.ACTION_CANCEL).isEmpty())

                    view.setMode(KeyboardMode.QWERTY)
                    view.measure(exact(width), exact(height)); view.layout(0, 0, width, height)
                    val spaceId = KeyboardLayouts.layout(KeyboardMode.QWERTY).rows.flatMap { it.keys }
                        .indexOfFirst { it.kind == KeyKind.SPACE }
                    val space = bounds(view, spaceId)
                    actions.clear()
                    val time = SystemClock.uptimeMillis() + widthIndex * 100L + 50L
                    dispatch(view, MotionEvent.ACTION_DOWN, time, time, space.exactCenterX(), space.exactCenterY())
                    dispatch(view, MotionEvent.ACTION_MOVE, time, time + 1, space.exactCenterX() + 10f * density, space.exactCenterY())
                    dispatch(view, MotionEvent.ACTION_UP, time, time + 2, space.exactCenterX() + 10f * density, space.exactCenterY())
                    assertEquals("$widthDp dp space remains fixed", listOf(KeyAction.MoveCursor(Direction.RIGHT)), actions)
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
