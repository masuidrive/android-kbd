package com.masuidrive.gestureime.keyboard

import android.graphics.Bitmap
import android.app.Activity
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.os.Looper
import android.provider.Settings
import android.content.res.Configuration
import android.view.MotionEvent
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityNodeInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Robolectric
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class KeyboardViewTest {
    private lateinit var view: KeyboardView
    private val actions = mutableListOf<KeyAction>()

    @Before fun setUp() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        view = KeyboardView(activity).apply {
            actionSink = KeyboardActionSink { actions += it }
        }
        activity.setContentView(view)
        shadowOf(Looper.getMainLooper()).idle()
        view.apply {
            measure(exact(400), exact(228))
            layout(0, 0, 400, 228)
            draw(Canvas(Bitmap.createBitmap(400, 228, Bitmap.Config.ARGB_8888)))
        }
    }

    @Test fun `space tap commits one space`() {
        touch(MotionEvent.ACTION_DOWN, 200f, 190f)
        touch(MotionEvent.ACTION_UP, 200f, 190f, 10)
        assertEquals(listOf(KeyAction.CommitText(" ")), actions)
    }

    @Test fun `space cursor gesture does not also commit space on release`() {
        touch(MotionEvent.ACTION_DOWN, 200f, 190f)
        touch(MotionEvent.ACTION_MOVE, 216f, 191f, 10)
        touch(MotionEvent.ACTION_MOVE, 208f, 203f, 20)
        touch(MotionEvent.ACTION_UP, 208f, 203f, 30)
        assertEquals(listOf(KeyAction.MoveCursor(Direction.RIGHT, 2), KeyAction.MoveCursor(Direction.LEFT, 1)), actions)
        assertTrue(actions.none { it == KeyAction.CommitText(" ") })
    }

    @Test fun `cancel emits no input and clears active pointer`() {
        touch(MotionEvent.ACTION_DOWN, 200f, 190f)
        touch(MotionEvent.ACTION_CANCEL, 200f, 190f, 10)
        assertTrue(actions.isEmpty())
    }

    @Test fun `modifier uses only vertical selection and tap clears it`() {
        touch(MotionEvent.ACTION_DOWN, 10f, 67f)
        touch(MotionEvent.ACTION_MOVE, 10f, 40f, 10)
        touch(MotionEvent.ACTION_UP, 10f, 40f, 20)
        touch(MotionEvent.ACTION_DOWN, 10f, 67f, 30)
        touch(MotionEvent.ACTION_UP, 10f, 67f, 40)
        assertEquals(listOf(KeyAction.SetModifier(Modifier.ALT), KeyAction.SetModifier(null)), actions)
    }

    @Test fun `qwerty period and backspace use their swapped production touch targets`() {
        val period = keyBounds(20)
        val backspace = keyBounds(30)
        assertTrue(period.width() < backspace.width())

        performGesture(period, time = 0)
        performGesture(period, listOf(0f to 28f), time = 10)
        assertEquals(listOf(KeyAction.CommitText("."), KeyAction.CommitText("?")), actions)

        actions.clear()
        performGesture(backspace, time = 20)
        performGesture(backspace, listOf(0f to 28f), time = 30)
        performGesture(backspace, listOf(-28f to 0f), time = 40)
        performGesture(backspace, listOf(0f to -28f), time = 50)
        performGesture(backspace, listOf(28f to 0f), time = 60)
        assertEquals(
            listOf(
                KeyAction.Backspace(), KeyAction.Escape,
                KeyAction.Backspace(), KeyAction.Backspace(), KeyAction.Backspace(),
            ),
            actions,
        )
    }

    @Test @LooperMode(LooperMode.Mode.PAUSED) fun `qwerty swapped targets retain full production gesture lifecycle`() {
        val looper = shadowOf(Looper.getMainLooper()).apply { pause() }
        val period = keyBounds(20)
        val backspace = keyBounds(30)
        val centerReturn = listOf(0f to 24f, 0f to 0f)

        listOf(
            Triple(period, listOf(0f to 17f), KeyAction.CommitText(".")),
            Triple(backspace, listOf(0f to 17f), KeyAction.Backspace()),
            Triple(period, centerReturn, KeyAction.CommitText(".")),
            Triple(backspace, centerReturn, KeyAction.Backspace()),
        ).forEachIndexed { index, (bounds, moves, expected) ->
            actions.clear()
            performGesture(bounds, moves, time = index * 10L)
            assertEquals("case $index", listOf(expected), actions)
        }

        listOf(-24f to 0f, 0f to -24f, 24f to 0f).forEachIndexed { index, move ->
            actions.clear()
            performGesture(period, listOf(move), time = 100L + index * 10L)
            assertEquals("period unassigned direction $move", listOf(KeyAction.CommitText(".")), actions)
        }

        listOf(period to KeyAction.CommitText("."), backspace to KeyAction.Backspace()).forEachIndexed { index, (bounds, expected) ->
            actions.clear()
            performGesture(bounds, listOf(0f to 24f), MotionEvent.ACTION_CANCEL, 140L + index * 10L)
            assertTrue("cancel $index", actions.isEmpty())
            performGesture(bounds, time = 160L + index * 10L)
            assertEquals("post-cancel $index", listOf(expected), actions)
        }

        actions.clear()
        val x = backspace.exactCenterX(); val y = backspace.exactCenterY()
        touch(MotionEvent.ACTION_DOWN, x, y, 200)
        looper.idleFor(KeyboardView.DELETE_REPEAT_DELAY_MS + KeyboardView.DELETE_REPEAT_INTERVAL_MS, TimeUnit.MILLISECONDS)
        val repeatsBeforeRelease = actions.toList()
        assertTrue(repeatsBeforeRelease.size >= 2)
        assertTrue(repeatsBeforeRelease.all { it == KeyAction.Backspace(repeat = true) })
        touch(MotionEvent.ACTION_UP, x, y, 300)
        assertEquals(repeatsBeforeRelease, actions)
        looper.idleFor(KeyboardView.DELETE_REPEAT_INTERVAL_MS * 2, TimeUnit.MILLISECONDS)
        assertEquals(repeatsBeforeRelease, actions)
    }

    @Test fun `qwerty swapped right edge stays within phone and tablet widths`() {
        listOf(412, 840).forEach { width ->
            view.measure(exact(width), exact(228))
            view.layout(0, 0, width, 228)
            val secondRow = (10..20).map(::keyBounds)
            val thirdRow = (21..30).map(::keyBounds)

            assertTrue("$width second row left", secondRow.first().left >= 0)
            assertTrue("$width second row right", secondRow.last().right <= width)
            assertTrue("$width third row left", thirdRow.first().left >= 0)
            assertTrue("$width third row right", thirdRow.last().right <= width)
            assertTrue("$width period is half target", secondRow.last().width() < thirdRow.last().width())
        }
    }

    @Test fun `accessibility exposes individual keys and activates focused key`() {
        val provider = view.accessibilityNodeProvider
        val host = requireNotNull(provider.createAccessibilityNodeInfo(-1))
        assertTrue(host.childCount >= 30)
        val firstKey = requireNotNull(provider.createAccessibilityNodeInfo(0))
        assertTrue(firstKey.contentDescription.toString().contains("タップ q"))
        assertTrue(provider.performAction(0, AccessibilityNodeInfo.ACTION_CLICK, null))
        assertEquals(listOf(KeyAction.CommitText("q")), actions)
    }

    @Test fun `fixed key labels keep their bounds at large system font scales`() {
        val normal = renderAtFontScale(1f)
        val enlarged = renderAtFontScale(1.3f)
        val accessibility = renderAtFontScale(2f)

        assertTrue("font scale 1.3 must not expand labels beyond their key bounds", normal.sameAs(enlarged))
        assertTrue("font scale 2.0 must not expand labels beyond their key bounds", normal.sameAs(accessibility))
    }

    @Test fun `transient exact parent height cannot move the four row keyboard`() {
        val modes = listOf(
            KeyboardMode.QWERTY,
            KeyboardMode.KANA,
            KeyboardMode.NUMBERS,
            KeyboardMode.SYMBOLS,
            KeyboardMode.VOICE,
        )

        modes.forEach { mode ->
            view.setMode(mode)
            view.measure(exact(400), exact(500))
            view.layout(0, 0, view.measuredWidth, view.measuredHeight)
            val transientBounds = keyBounds(0)

            assertEquals("$mode intrinsic height", 228, view.measuredHeight)

            view.measure(exact(400), exact(228))
            view.layout(0, 0, view.measuredWidth, view.measuredHeight)
            assertEquals("$mode first key bounds", transientBounds, keyBounds(0))
        }
    }

    @Test fun `height presets keep every layer and dual kana at a width independent four row height`() {
        val modes = KeyboardMode.entries.toList()
        KeyboardHeightPreset.entries.forEach { preset ->
            listOf(412, 840).forEach { width ->
                val rowPitch = expectedRowPitch(preset, width)
                val dimensions = Triple(rowPitch * 4 + 8, rowPitch - 10, 10)
                modes.forEach { mode ->
                    view.setHeightPreset(preset)
                    view.setMode(mode)
                    view.measure(exact(width), View.MeasureSpec.makeMeasureSpec(1_000, View.MeasureSpec.AT_MOST))
                    view.layout(0, 0, width, view.measuredHeight)
                    val first = keyBounds(0)
                    val nextRowId = KeyboardLayouts.layout(mode).rows.first().keys.size
                    val secondRow = keyBounds(nextRowId)
                    assertEquals("$preset $width $mode outer height", dimensions.first, view.measuredHeight)
                    assertEquals("$preset $width $mode key face", dimensions.second, first.height())
                    assertEquals("$preset $width $mode row gap", dimensions.third, secondRow.top - first.bottom)
                }
                view.setHeightPreset(preset)
                view.setMode(KeyboardMode.KANA)
                view.setDualFlickEnabled(true)
                view.measure(exact(width), View.MeasureSpec.makeMeasureSpec(1_000, View.MeasureSpec.AT_MOST))
                view.layout(0, 0, width, view.measuredHeight)
                assertEquals("$preset $width dual kana outer height", dimensions.first, view.measuredHeight)
                view.setDualFlickEnabled(false)
            }
        }
    }

    @Test fun `height preset survives transient exact parents and delayed bottom inset without moving keys`() {
        KeyboardHeightPreset.entries.forEach { preset ->
            view.setHeightPreset(preset)
            view.measure(exact(840), exact(500))
            view.layout(0, 0, view.measuredWidth, view.measuredHeight)
            val initial = keyBounds(0)
            val expectedHeight = expectedRowPitch(preset, 840) * 4 + 8
            assertEquals(expectedHeight, view.measuredHeight)

            view.updateBottomInset(24)
            view.measure(exact(840), exact(500))
            view.layout(0, 0, view.measuredWidth, view.measuredHeight)
            assertEquals(expectedHeight + 24, view.measuredHeight)
            assertEquals(initial, keyBounds(0))
            view.updateBottomInset(24)
            assertEquals(initial, keyBounds(0))
            view.updateBottomInset(0)
        }
    }

    @Test fun `inset listener uses the complete system bar bottom instead of the IME bottom`() {
        val insets = WindowInsetsCompat.Builder()
            .setInsets(WindowInsetsCompat.Type.systemBars(), Insets.of(0, 0, 0, 47))
            .setInsetsIgnoringVisibility(WindowInsetsCompat.Type.systemBars(), Insets.of(0, 0, 0, 47))
            .setInsets(WindowInsetsCompat.Type.ime(), Insets.of(0, 0, 0, 149))
            .build()

        assertEquals(47, systemBarBottomInset(insets))
    }

    @Test fun `home gesture inset reserves space when system bars report zero`() {
        val insets = WindowInsetsCompat.Builder()
            .setInsets(WindowInsetsCompat.Type.systemBars(), Insets.NONE)
            .setInsetsIgnoringVisibility(WindowInsetsCompat.Type.systemBars(), Insets.NONE)
            .setInsets(WindowInsetsCompat.Type.systemGestures(), Insets.of(0, 0, 0, 68))
            .build()

        assertEquals(68, systemBarBottomInset(insets))
    }

    @Test fun `transient zero system bar inset keeps the seeded OEM navigation reservation`() {
        view.setSystemBottomInsetFallback(96)
        view.updateBottomInset(96)

        ViewCompat.dispatchApplyWindowInsets(
            view,
            WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.systemBars(), Insets.NONE)
                .setInsetsIgnoringVisibility(WindowInsetsCompat.Type.systemBars(), Insets.NONE)
                .build(),
        )
        assertEquals(96, view.paddingBottom)

        ViewCompat.dispatchApplyWindowInsets(
            view,
            WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.systemBars(), Insets.of(0, 0, 0, 72))
                .setInsetsIgnoringVisibility(WindowInsetsCompat.Type.systemBars(), Insets.of(0, 0, 0, 72))
                .build(),
        )
        assertEquals(72, view.paddingBottom)

        ViewCompat.dispatchApplyWindowInsets(
            view,
            WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.systemBars(), Insets.NONE)
                .setInsetsIgnoringVisibility(WindowInsetsCompat.Type.systemBars(), Insets.NONE)
                .build(),
        )
        assertEquals(72, view.paddingBottom)
    }

    @Test fun `larger posture inset becomes the fallback for a later zero dispatch`() {
        view.setSystemBottomInsetFallback(72)
        view.updateBottomInset(72)

        ViewCompat.dispatchApplyWindowInsets(
            view,
            WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.systemBars(), Insets.of(0, 0, 0, 96))
                .setInsetsIgnoringVisibility(WindowInsetsCompat.Type.systemBars(), Insets.of(0, 0, 0, 96))
                .build(),
        )
        ViewCompat.dispatchApplyWindowInsets(
            view,
            WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.systemBars(), Insets.NONE)
                .setInsetsIgnoringVisibility(WindowInsetsCompat.Type.systemBars(), Insets.NONE)
                .build(),
        )

        assertEquals(96, view.paddingBottom)
    }

    @Test fun `smaller posture inset becomes the fallback for a later zero dispatch`() {
        view.setSystemBottomInsetFallback(96)
        view.updateBottomInset(96)

        ViewCompat.dispatchApplyWindowInsets(
            view,
            WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.systemBars(), Insets.of(0, 0, 0, 72))
                .setInsetsIgnoringVisibility(WindowInsetsCompat.Type.systemBars(), Insets.of(0, 0, 0, 72))
                .build(),
        )
        ViewCompat.dispatchApplyWindowInsets(
            view,
            WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.systemBars(), Insets.NONE)
                .setInsetsIgnoringVisibility(WindowInsetsCompat.Type.systemBars(), Insets.NONE)
                .build(),
        )

        assertEquals(72, view.paddingBottom)
    }

    private fun expectedRowPitch(preset: KeyboardHeightPreset, width: Int): Int = when {
        preset == KeyboardHeightPreset.LARGE && width >= KeyboardView.DUAL_FLICK_MIN_WIDTH_DP -> 62
        else -> preset.rowPitchDp.toInt()
    }

    @Test fun `smaller parent height constrains every mode and its accessible keys`() {
        val modes = listOf(
            KeyboardMode.QWERTY,
            KeyboardMode.KANA,
            KeyboardMode.NUMBERS,
            KeyboardMode.SYMBOLS,
            KeyboardMode.VOICE,
        )

        modes.forEach { mode ->
            view.setMode(mode)
            listOf(View.MeasureSpec.AT_MOST, View.MeasureSpec.EXACTLY).forEach { constraint ->
                view.measure(exact(400), View.MeasureSpec.makeMeasureSpec(160, constraint))
                view.layout(0, 0, view.measuredWidth, view.measuredHeight)

                assertEquals("$mode constraint=$constraint", 160, view.measuredHeight)
                val provider = view.accessibilityNodeProvider
                val host = requireNotNull(provider.createAccessibilityNodeInfo(-1))
                repeat(host.childCount) { childIndex ->
                    val bounds = Rect().also {
                        requireNotNull(provider.createAccessibilityNodeInfo(childIndex)).getBoundsInParent(it)
                    }
                    assertTrue("$mode key $childIndex top=$bounds", bounds.top >= 0)
                    assertTrue("$mode key $childIndex bottom=$bounds", bounds.bottom <= view.measuredHeight)
                }
            }
        }
    }

    @Test fun `preview consumes touch and accessibility without changing input state`() {
        view.setPreviewOnly(true)
        touch(MotionEvent.ACTION_DOWN, 40f, 20f)
        touch(MotionEvent.ACTION_UP, 40f, 20f, 10)
        val provider = view.accessibilityNodeProvider

        assertTrue(actions.isEmpty())
        assertTrue(!provider.performAction(0, AccessibilityNodeInfo.ACTION_CLICK, null))
        assertTrue(actions.isEmpty())
    }

    @Test fun `fixed labels preserve bounded nonoverlapping keys at narrow and wide widths`() {
        listOf(412 to 228, 840 to 248).forEach { (width, height) ->
            view.measure(exact(width), exact(height)); view.layout(0, 0, width, height)
            view.draw(Canvas(Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)))
            val provider = view.accessibilityNodeProvider
            val host = requireNotNull(provider.createAccessibilityNodeInfo(-1))
            val bounds = (0 until host.childCount).map { id -> Rect().also { provider.createAccessibilityNodeInfo(id)!!.getBoundsInParent(it) } }
            bounds.forEach { assertTrue("key left bounds at $width px", it.left >= 0); assertTrue("key right bounds at $width px", it.right <= width) }
            for (i in bounds.indices) for (j in i + 1 until bounds.size) assertFalse("keys overlap at $width px", Rect.intersects(bounds[i], bounds[j]))
        }
    }

    @Test fun `modifier and composite glyphs remain wholly inside narrow and wide keys`() {
        listOf(412 to 220, 840 to 248).forEach { (width, height) ->
            view.measure(exact(width), exact(height)); view.layout(0, 0, width, height)
            val canvas = Canvas(Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888))
            view.draw(canvas)
            val shadow = shadowOf(canvas)
            val events = (0 until shadow.textHistoryCount).map(shadow::getDrawnTextEvent)

            assertGlyphInside(events.last { it.text == "C" }.x, events.last { it.text == "C" }.y, "C", 10f, keyBounds(10))
            assertGlyphInside(events.last { it.text == "A" }.x, events.last { it.text == "A" }.y, "A", 10f, keyBounds(10))
            assertGlyphInside(events.last { it.text == "ん" }.x, events.last { it.text == "ん" }.y, "ん", 16f * .64f, keyBounds(31))
            assertGlyphInside(events.last { it.text == "あ" }.x, events.last { it.text == "あ" }.y, "あ", 16f, keyBounds(31))
        }
    }

    @Test @LooperMode(LooperMode.Mode.PAUSED) fun `center layer hold no longer starts voice and release performs its tap action`() {
        val layer = keyCenter(31)
        touch(MotionEvent.ACTION_DOWN, layer.first, layer.second)
        shadowOf(Looper.getMainLooper()).idleFor(1_500, TimeUnit.MILLISECONDS)
        assertTrue(actions.isEmpty())
        touch(MotionEvent.ACTION_UP, layer.first, layer.second, 1_510)

        assertEquals(listOf(KeyAction.SwitchLayer(KeyboardMode.KANA)), actions)
    }

    @Test fun `left layer swipe enters voice once and release does not switch layer`() {
        val layer = keyCenter(31)

        touch(MotionEvent.ACTION_DOWN, layer.first, layer.second)
        touch(MotionEvent.ACTION_MOVE, layer.first - 35f, layer.second, 20)
        assertEquals(listOf(KeyAction.VoiceHold), actions)
        touch(MotionEvent.ACTION_UP, layer.first - 35f, layer.second, 30)
        assertEquals(listOf(KeyAction.VoiceHold), actions)
    }

    @Test fun `accessibility does not advertise an unexecutable voice hold flick`() {
        val events = mutableListOf<VoiceHoldEvent>()
        view.voiceHoldSink = VoiceHoldSink(events::add)
        val provider = view.accessibilityNodeProvider
        val layer = requireNotNull(provider.createAccessibilityNodeInfo(31))

        assertFalse(layer.contentDescription.toString().contains("音声"))
        assertFalse(provider.performAction(31, 0x01020001, null))
        assertTrue(events.isEmpty())
        assertTrue(actions.isEmpty())
    }

    @Test fun `voice layer keeps four row geometry and cancel key switches or cancels`() {
        view.setMode(KeyboardMode.VOICE)
        val cancel = keyBounds(3)
        touch(MotionEvent.ACTION_DOWN, cancel.exactCenterX(), cancel.exactCenterY())
        touch(MotionEvent.ACTION_UP, cancel.exactCenterX(), cancel.exactCenterY(), 10)
        assertEquals(listOf(KeyAction.CancelVoice), actions)

        actions.clear()
        view.setMode(KeyboardMode.VOICE)
        touch(MotionEvent.ACTION_DOWN, cancel.exactCenterX(), cancel.exactCenterY(), 20)
        touch(MotionEvent.ACTION_MOVE, cancel.exactCenterX(), cancel.exactCenterY() - 30f, 30)
        touch(MotionEvent.ACTION_UP, cancel.exactCenterX(), cancel.exactCenterY() - 30f, 40)
        assertEquals(listOf(KeyAction.SwitchLayer(KeyboardMode.KANA)), actions)

        actions.clear()
        view.setMode(KeyboardMode.VOICE)
        touch(MotionEvent.ACTION_DOWN, cancel.exactCenterX(), cancel.exactCenterY(), 50)
        touch(MotionEvent.ACTION_MOVE, cancel.exactCenterX() + 30f, cancel.exactCenterY(), 60)
        touch(MotionEvent.ACTION_UP, cancel.exactCenterX() + 30f, cancel.exactCenterY(), 70)
        assertEquals(listOf(KeyAction.SwitchLayer(KeyboardMode.QWERTY)), actions)

        actions.clear()
        view.setMode(KeyboardMode.VOICE)
        touch(MotionEvent.ACTION_DOWN, cancel.exactCenterX(), cancel.exactCenterY(), 80)
        touch(MotionEvent.ACTION_MOVE, cancel.exactCenterX(), cancel.exactCenterY() + 30f, 90)
        touch(MotionEvent.ACTION_UP, cancel.exactCenterX(), cancel.exactCenterY() + 30f, 100)
        assertEquals(listOf(KeyAction.SwitchLayer(KeyboardMode.NUMBERS)), actions)
        assertEquals(228, view.measuredHeight)
    }

    @Test fun `voice status owns its sixth-sized slot with six equal touch targets and fixed four row geometry`() {
        listOf(
            Triple(412, KeyboardHeightPreset.SMALL, 208),
            Triple(840, KeyboardHeightPreset.SMALL, 208),
            Triple(412, KeyboardHeightPreset.STANDARD, 228),
            Triple(840, KeyboardHeightPreset.STANDARD, 228),
            Triple(412, KeyboardHeightPreset.LARGE, 248),
            Triple(840, KeyboardHeightPreset.LARGE, 248),
        ).forEach { (width, preset, height) ->
            view.setHeightPreset(preset)
            view.measure(exact(width), exact(height)); view.layout(0, 0, width, height)
            view.setMode(KeyboardMode.VOICE)
            val provider = view.accessibilityNodeProvider
            val cancelBefore = keyBounds(3)
            val voiceStatus = keyBounds(4)
            val punctuation = keyBounds(5)
            val space = keyBounds(6)
            val backspace = keyBounds(7)
            val enter = keyBounds(8)
            val cancelActionsBefore = requireNotNull(provider.createAccessibilityNodeInfo(3)).actions
            val nodesBefore = requireNotNull(provider.createAccessibilityNodeInfo(-1)).childCount
            val accessibility = shadowOf(view.context.getSystemService(AccessibilityManager::class.java)).apply {
                setEnabled(true)
                setTouchExplorationEnabled(true)
            }
            val announcementsBefore = accessibility.sentAccessibilityEvents.count {
                it.eventType == android.view.accessibility.AccessibilityEvent.TYPE_ANNOUNCEMENT
            }

            view.setVoiceSessionActive(true)
            val canvas = CaptureCanvas(Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)).also(view::draw)
            val status = requireNotNull(provider.createAccessibilityNodeInfo(KeyboardView.VOICE_SESSION_STATUS_VIRTUAL_ID))

            assertTrue(canvas.draws.any { it.text == "認識中" && it.x > cancelBefore.right && it.x < punctuation.left })
            assertTrue(listOf(cancelBefore, voiceStatus, punctuation, space, backspace, enter).all { kotlin.math.abs(it.width() - cancelBefore.width()) <= 1 })
            assertTrue(listOf(cancelBefore, voiceStatus, punctuation, space, backspace, enter).all { it.top == cancelBefore.top && it.bottom == cancelBefore.bottom })
            assertTrue(cancelBefore.right <= voiceStatus.left)
            assertTrue(voiceStatus.right <= punctuation.left)
            assertTrue(punctuation.right <= space.left)
            assertTrue(space.right <= backspace.left)
            assertTrue(backspace.right <= enter.left)
            assertEquals(cancelBefore, keyBounds(3))
            assertEquals(cancelActionsBefore, requireNotNull(provider.createAccessibilityNodeInfo(3)).actions)
            assertEquals(height, view.measuredHeight)
            assertEquals(nodesBefore + 1, requireNotNull(provider.createAccessibilityNodeInfo(-1)).childCount)
            assertEquals("認識中", status.contentDescription)
            assertFalse(status.isClickable)
            assertFalse(provider.performAction(KeyboardView.VOICE_SESSION_STATUS_VIRTUAL_ID, AccessibilityNodeInfo.ACTION_CLICK, null))
            val backspaceNode = requireNotNull(provider.createAccessibilityNodeInfo(7))
            assertEquals("タップ ⌫", backspaceNode.contentDescription)
            assertTrue(backspaceNode.isClickable)
            val statusBounds = Rect().also(status::getBoundsInParent)
            assertTrue(statusBounds.left >= cancelBefore.right)
            assertTrue(statusBounds.right <= punctuation.left)
            val hoverEnter = MotionEvent.obtain(0, 1, MotionEvent.ACTION_HOVER_ENTER, statusBounds.exactCenterX().toFloat(), statusBounds.exactCenterY().toFloat(), 0)
            try {
                assertTrue(view.dispatchHoverEvent(hoverEnter))
            } finally {
                hoverEnter.recycle()
            }
            assertTrue(accessibility.sentAccessibilityEvents.any {
                it.eventType == android.view.accessibility.AccessibilityEvent.TYPE_VIEW_HOVER_ENTER
            })
            assertEquals(announcementsBefore + 1, accessibility.sentAccessibilityEvents.count {
                it.eventType == android.view.accessibility.AccessibilityEvent.TYPE_ANNOUNCEMENT
            })

            view.setVoiceSessionActive(true)
            assertEquals(announcementsBefore + 1, accessibility.sentAccessibilityEvents.count {
                it.eventType == android.view.accessibility.AccessibilityEvent.TYPE_ANNOUNCEMENT
            })

            assertTrue(provider.performAction(KeyboardView.VOICE_SESSION_STATUS_VIRTUAL_ID, AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS, null))
            assertTrue(view.isVoiceSessionStatusAccessibilityFocused())
            view.setVoiceSessionActive(false)
            assertFalse(view.isVoiceSessionStatusAccessibilityFocused())
            val delayedStatus = requireNotNull(runCatching {
                provider.createAccessibilityNodeInfo(KeyboardView.VOICE_SESSION_STATUS_VIRTUAL_ID)
            }.getOrThrow())
            assertFalse(delayedStatus.isVisibleToUser)
            val exitsAfterClear = accessibility.sentAccessibilityEvents.count {
                it.eventType == android.view.accessibility.AccessibilityEvent.TYPE_VIEW_HOVER_EXIT
            }
            val staleHover = MotionEvent.obtain(2, 3, MotionEvent.ACTION_HOVER_MOVE, statusBounds.exactCenterX().toFloat(), statusBounds.exactCenterY().toFloat(), 0)
            try {
                view.dispatchHoverEvent(staleHover)
            } finally {
                staleHover.recycle()
            }
            assertEquals(exitsAfterClear, accessibility.sentAccessibilityEvents.count {
                it.eventType == android.view.accessibility.AccessibilityEvent.TYPE_VIEW_HOVER_EXIT
            })
            val idle = CaptureCanvas(Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)).also(view::draw)
            assertFalse(idle.draws.any { it.text == "認識中" })
            assertEquals(nodesBefore, requireNotNull(provider.createAccessibilityNodeInfo(-1)).childCount)

            actions.clear()
            touch(MotionEvent.ACTION_DOWN, backspace.exactCenterX(), backspace.exactCenterY())
            touch(MotionEvent.ACTION_UP, backspace.exactCenterX(), backspace.exactCenterY(), 10)
            assertEquals(listOf(KeyAction.Backspace()), actions)
            actions.clear()
            touch(MotionEvent.ACTION_DOWN, backspace.exactCenterX(), backspace.exactCenterY(), 20)
            touch(MotionEvent.ACTION_MOVE, backspace.exactCenterX() + 30f, backspace.exactCenterY(), 21)
            touch(MotionEvent.ACTION_UP, backspace.exactCenterX() + 30f, backspace.exactCenterY(), 22)
            assertEquals(listOf(KeyAction.Backspace()), actions)
        }
    }

    @Test fun `voice waveform clamps provider levels grows with sound and clears without changing geometry`() {
        view.setMode(KeyboardMode.VOICE)
        view.measure(exact(412), exact(228)); view.layout(0, 0, 412, 228)
        view.setVoiceSessionActive(true)
        val statusBounds = keyBounds(4)
        val originalHeight = view.measuredHeight

        fun waveformState(): List<RoundDraw> {
            return CaptureCanvas(Bitmap.createBitmap(412, 228, Bitmap.Config.ARGB_8888)).also(view::draw)
                .roundRects.filter { draw ->
                    draw.rect.left >= statusBounds.left && draw.rect.right <= statusBounds.right && draw.rect.width() <= 3f
                }
        }
        fun waveform(level: Float?): List<RoundDraw> {
            view.setVoiceInputLevel(level)
            return waveformState()
        }

        view.showVoiceInputLevelBaseline()
        val baseline = waveformState()
        val quiet = waveform(-40f)
        val mid = waveform(-10f)
        val loud = waveform(10f)
        assertEquals(4, baseline.size)
        assertEquals(2f, baseline.maxOf { it.rect.height() }, .01f)
        assertEquals(4, quiet.size)
        assertEquals(4, mid.size)
        assertEquals(4, loud.size)
        assertTrue(quiet.maxOf { it.rect.height() } > baseline.maxOf { it.rect.height() })
        assertTrue(mid.maxOf { it.rect.height() } > quiet.maxOf { it.rect.height() })
        assertTrue(loud.maxOf { it.rect.height() } > mid.maxOf { it.rect.height() })
        assertEquals(0, waveform(null).size)
        assertEquals(originalHeight, view.measuredHeight)
        assertTrue(normalizeVoiceInputLevel(-40f) < normalizeVoiceInputLevel(-10f))
        assertTrue(normalizeVoiceInputLevel(-10f) < normalizeVoiceInputLevel(5f))
        assertTrue(normalizeVoiceInputLevel(5f) < normalizeVoiceInputLevel(40f))
        assertEquals(0f, normalizeVoiceInputLevel(-Float.MAX_VALUE), 0f)
        assertEquals(1f, normalizeVoiceInputLevel(Float.MAX_VALUE), 0f)
        val orderedLevels = listOf(
            -Float.MAX_VALUE, -1_000_000f, -1_000f, -100f, -40f, -10f, -1f,
            0f, 1f, 5f, 10f, 40f, 100f, 1_000f, 1_000_000f, Float.MAX_VALUE,
        ).map(::normalizeVoiceInputLevel)
        assertTrue(orderedLevels.all { it in 0f..1f })
        assertTrue(orderedLevels.zipWithNext().all { (left, right) -> left <= right })

        view.setVoiceInputLevel(-10f)
        val quantized = view.voiceInputLevel()
        assertFalse(view.setVoiceInputLevel(-10.1f))
        assertEquals(quantized, view.voiceInputLevel())
        assertFalse(view.setVoiceInputLevel(Float.NaN))
        assertFalse(view.setVoiceInputLevel(Float.POSITIVE_INFINITY))
        assertFalse(view.setVoiceInputLevel(Float.NEGATIVE_INFINITY))
        assertEquals(quantized, view.voiceInputLevel())
    }

    @Test fun `voice punctuation has the full idle label and commits every flick direction`() {
        view.setMode(KeyboardMode.VOICE)
        view.measure(exact(412), exact(228)); view.layout(0, 0, 412, 228)
        val canvas = CaptureCanvas(Bitmap.createBitmap(412, 228, Bitmap.Config.ARGB_8888)).also(view::draw)

        assertTrue(canvas.draws.any { it.text == "、。？！" })
        assertFalse(canvas.draws.any { it.text == "、" })
        val punctuation = keyBounds(5)
        listOf(
            Triple(0f, 0f, "、"),
            Triple(-30f, 0f, "。"),
            Triple(0f, -30f, "？"),
            Triple(30f, 0f, "！"),
            Triple(0f, 30f, "、"),
        ).forEachIndexed { index, (dx, dy, text) ->
            actions.clear()
            val startX = punctuation.exactCenterX()
            val startY = punctuation.exactCenterY()
            val time = index * 10L
            touch(MotionEvent.ACTION_DOWN, startX, startY, time)
            if (dx != 0f || dy != 0f) touch(MotionEvent.ACTION_MOVE, startX + dx, startY + dy, time + 1)
            touch(MotionEvent.ACTION_UP, startX + dx, startY + dy, time + 2)
            assertEquals("voice punctuation $text", listOf(KeyAction.CommitText(text)), actions)
        }
    }

    @Test fun `leaving voice clears a focused listening status before its virtual node disappears`() {
        shadowOf(view.context.getSystemService(AccessibilityManager::class.java)).apply {
            setEnabled(true)
            setTouchExplorationEnabled(true)
        }
        view.setMode(KeyboardMode.VOICE)
        view.measure(exact(400), exact(228)); view.layout(0, 0, 400, 228)
        view.setVoiceSessionActive(true)
        val provider = view.accessibilityNodeProvider

        assertTrue(provider.performAction(KeyboardView.VOICE_SESSION_STATUS_VIRTUAL_ID, AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS, null))
        assertTrue(view.isVoiceSessionStatusAccessibilityFocused())
        view.setMode(KeyboardMode.QWERTY)

        assertFalse(view.isVoiceSessionStatusAccessibilityFocused())
        assertTrue(runCatching { provider.createAccessibilityNodeInfo(KeyboardView.VOICE_SESSION_STATUS_VIRTUAL_ID) }.isSuccess)
    }

    @Test fun `right layer swipe stays qwerty and second pointer does not duplicate voice entry`() {
        val events = mutableListOf<VoiceHoldEvent>()
        view.voiceHoldSink = VoiceHoldSink(events::add)
        val layer = keyCenter(31)
        touch(MotionEvent.ACTION_DOWN, layer.first, layer.second)
        touch(MotionEvent.ACTION_MOVE, layer.first + 35f, layer.second, 20)
        touch(MotionEvent.ACTION_UP, layer.first + 35f, layer.second, 30)
        assertTrue(actions.contains(KeyAction.SwitchLayer(KeyboardMode.QWERTY)))

        actions.clear(); view.setMode(KeyboardMode.QWERTY)
        touch(MotionEvent.ACTION_DOWN, layer.first, layer.second, 100)
        touch(MotionEvent.ACTION_MOVE, layer.first - 35f, layer.second, 120)
        assertEquals(listOf(KeyAction.VoiceHold), actions)
        multiTouch(MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT), listOf(0 to layer, 1 to (20f to 20f)))
        multiTouch(MotionEvent.ACTION_POINTER_UP or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT), listOf(0 to layer, 1 to (20f to 20f)))
        multiTouch(MotionEvent.ACTION_UP, listOf(0 to layer))
        assertEquals(listOf(KeyAction.VoiceHold), actions)
    }

    @Test @LooperMode(LooperMode.Mode.PAUSED) fun `mode change after voice entry does not dispatch a stale layer action`() {
        val layer = keyCenter(31)
        touch(MotionEvent.ACTION_DOWN, layer.first, layer.second)
        touch(MotionEvent.ACTION_MOVE, layer.first - 35f, layer.second, 20)

        view.setMode(KeyboardMode.KANA)
        assertEquals(listOf(KeyAction.VoiceHold), actions)
    }

    @Test @LooperMode(LooperMode.Mode.PAUSED) fun `qwerty labels preserve css anchors and animate actual canvas frames`() {
        Settings.Global.putFloat(view.context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        val looper = shadowOf(Looper.getMainLooper())
        looper.pause()
        fun frame() = CaptureCanvas(Bitmap.createBitmap(400, 228, Bitmap.Config.ARGB_8888)).also(view::draw)
        fun center(draw: TextDraw) = draw.y + (draw.ascent + draw.descent) / 2f
        val q = keyBounds(0)
        val idle = frame()
        val idleMain = idle.draws.last { it.text == "q" }
        val idleAux = idle.draws.last { it.text == "1" }
        assertEquals(q.top + 27.5f, center(idleMain), .6f)
        assertEquals(q.top + 9f, center(idleAux), .6f)
        assertEquals(22f, idleMain.textSize, .1f)
        assertEquals(11f, idleAux.textSize, .1f)
        touch(MotionEvent.ACTION_DOWN, q.centerX().toFloat(), q.centerY().toFloat())
        val pressed = frame()
        assertEquals(center(idleMain), center(pressed.draws.last { it.text == "q" }), .1f)
        assertEquals(center(idleAux), center(pressed.draws.last { it.text == "1" }), .1f)
        touch(MotionEvent.ACTION_MOVE, q.centerX().toFloat(), q.centerY() - 24f, 10)
        assertEquals(0, frame().draws.last { it.text == "1" }.alpha)
        looper.idleFor(100, TimeUnit.MILLISECONDS)
        val up = frame()
        val uppercase = up.draws.last { it.text == "Q" }
        assertEquals(22f, uppercase.textSize, .1f)
        assertEquals(center(idleMain) - 3f, center(uppercase), .6f)
        assertEquals(0, up.draws.last { it.text == "1" }.alpha)
        touch(MotionEvent.ACTION_MOVE, q.centerX().toFloat(), q.centerY() + 24f, 120)
        looper.idleFor(100, TimeUnit.MILLISECONDS)
        val down = frame()
        val downAux = down.draws.last { it.text == "1" }
        assertEquals(q.exactCenterY(), center(downAux), .6f)
        assertEquals(11f * 1.7f, downAux.textSize, .2f)
        assertEquals(0, down.draws.last { it.text == "q" }.alpha)
        touch(MotionEvent.ACTION_MOVE, q.centerX().toFloat(), q.centerY().toFloat(), 230)
        assertEquals(255, frame().draws.last { it.text == "1" }.alpha)
        val reverseStart = center(frame().draws.last { it.text == "1" })
        looper.idleFor(45, TimeUnit.MILLISECONDS)
        val reverseMiddle = center(frame().draws.last { it.text == "1" })
        looper.idleFor(60, TimeUnit.MILLISECONDS)
        val reverseEnd = center(frame().draws.last { it.text == "1" })
        assertTrue(reverseMiddle < reverseStart)
        assertEquals(center(idleAux), reverseEnd, .6f)
        touch(MotionEvent.ACTION_CANCEL, q.centerX().toFloat(), q.centerY().toFloat(), 300)
    }

    @Test @LooperMode(LooperMode.Mode.PAUSED) fun `mode change cancels an old label animator before pointer id reuse`() {
        Settings.Global.putFloat(view.context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        val looper = shadowOf(Looper.getMainLooper()).apply { pause() }
        val q = keyCenter(0)
        touch(MotionEvent.ACTION_DOWN, q.first, q.second)
        touch(MotionEvent.ACTION_MOVE, q.first, q.second + 24f, 10)
        view.setMode(KeyboardMode.KANA)
        view.setMode(KeyboardMode.QWERTY)
        val freshQ = keyCenter(0)
        touch(MotionEvent.ACTION_DOWN, freshQ.first, freshQ.second, 20)
        looper.idleFor(100, TimeUnit.MILLISECONDS)
        val canvas = CaptureCanvas(Bitmap.createBitmap(400, 228, Bitmap.Config.ARGB_8888)).also(view::draw)
        val drawn = canvas.draws.last { it.text == "q" }
        assertEquals(keyBounds(0).exactCenterY() + 5f, drawn.y + (drawn.ascent + drawn.descent) / 2f, .6f)
        touch(MotionEvent.ACTION_CANCEL, freshQ.first, freshQ.second, 130)
    }

    @Test @LooperMode(LooperMode.Mode.PAUSED) fun `enter up animates paste and dispatches paste only on release`() {
        Settings.Global.putFloat(view.context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        val looper = shadowOf(Looper.getMainLooper()).apply { pause() }
        fun frame() = CaptureCanvas(Bitmap.createBitmap(400, 228, Bitmap.Config.ARGB_8888)).also(view::draw)
        fun center(draw: TextDraw) = draw.y + (draw.ascent + draw.descent) / 2f
        val enter = keyBounds(33)
        val idle = frame()
        val idleMain = idle.draws.last { it.text == "Enter" }
        val idleHint = idle.draws.last { it.text == "paste" }
        val idleControlJ = idle.draws.last { it.text == "C-j" }
        assertEquals(enter.exactCenterY(), center(idleMain), .6f)
        assertEquals(enter.top + 8f, center(idleControlJ), .6f)
        assertEquals(enter.bottom - 8f, center(idleHint), .6f)
        assertEquals((255 * .7f).toInt(), idleHint.alpha)
        assertEquals(.7f / idleHint.textSize, idleHint.letterSpacing, .001f)
        assertEquals(android.graphics.Color.rgb(25, 25, 27), idleHint.color)
        touch(MotionEvent.ACTION_DOWN, enter.centerX().toFloat(), enter.centerY().toFloat())
        touch(MotionEvent.ACTION_MOVE, enter.centerX().toFloat(), enter.centerY() - 24f, 10)
        assertTrue(actions.isEmpty())
        looper.idleFor(100, TimeUnit.MILLISECONDS)
        val selected = frame()
        val paste = selected.draws.last { it.text == "paste" }
        assertEquals(enter.exactCenterY(), center(paste), .6f)
        assertEquals(10f * 1.7f, paste.textSize, .2f)
        assertEquals(0, selected.draws.last { it.text == "Enter" }.alpha)
        touch(MotionEvent.ACTION_UP, enter.centerX().toFloat(), enter.centerY() - 24f, 120)
        assertEquals(listOf(KeyAction.Paste), actions)
    }

    @Test @LooperMode(LooperMode.Mode.PAUSED) fun `two pointers keep independent label animation frames`() {
        Settings.Global.putFloat(view.context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        val looper = shadowOf(Looper.getMainLooper()).apply { pause() }
        val q = keyCenter(0)
        val w = keyCenter(1)
        multiTouch(MotionEvent.ACTION_DOWN, listOf(0 to q))
        multiTouch(MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT), listOf(0 to q, 1 to w))
        multiTouch(MotionEvent.ACTION_MOVE, listOf(0 to (q.first to q.second + 24f), 1 to (w.first to w.second - 24f)))
        looper.idleFor(100, TimeUnit.MILLISECONDS)
        val canvas = CaptureCanvas(Bitmap.createBitmap(400, 228, Bitmap.Config.ARGB_8888)).also(view::draw)
        assertEquals(11f * 1.7f, canvas.draws.last { it.text == "1" }.textSize, .2f)
        assertEquals(22f, canvas.draws.last { it.text == "W" }.textSize, .1f)
        assertEquals(0, canvas.draws.last { it.text == "2" }.alpha)
        multiTouch(MotionEvent.ACTION_CANCEL, listOf(0 to q, 1 to w))
    }

    @Test fun `regular key has one pixel dark edge shadow and preserves open gap`() {
        val canvas = CaptureCanvas(Bitmap.createBitmap(400, 228, Bitmap.Config.ARGB_8888)).also(view::draw)
        val q = keyBounds(0)
        val qShadow = canvas.roundRects.first { it.rect.left == q.left.toFloat() && it.rect.top == q.top + 1f }
        assertEquals(android.graphics.Color.rgb(137, 140, 148), qShadow.color)
        assertEquals(q.bottom + 1f, qShadow.rect.bottom, .01f)
    }

    @Test @LooperMode(LooperMode.Mode.PAUSED) fun `wide qwerty keeps relative label anchors and centers down swipe`() {
        Settings.Global.putFloat(view.context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        val looper = shadowOf(Looper.getMainLooper()).apply { pause() }
        view.measure(exact(840), exact(248))
        view.layout(0, 0, 840, 248)
        val q = keyBounds(0)
        val canvas = CaptureCanvas(Bitmap.createBitmap(840, 248, Bitmap.Config.ARGB_8888)).also(view::draw)
        fun center(draw: TextDraw) = draw.y + (draw.ascent + draw.descent) / 2f
        assertEquals(q.exactCenterY() + 5f, center(canvas.draws.last { it.text == "q" }), .6f)
        assertEquals(q.top + 9f, center(canvas.draws.last { it.text == "1" }), .6f)
        touch(MotionEvent.ACTION_DOWN, q.exactCenterX(), q.exactCenterY())
        touch(MotionEvent.ACTION_MOVE, q.exactCenterX(), q.exactCenterY() + 24f, 10)
        looper.idleFor(KeyboardView.LABEL_ANIMATION_MS + 10, TimeUnit.MILLISECONDS)
        val down = CaptureCanvas(Bitmap.createBitmap(840, 248, Bitmap.Config.ARGB_8888)).also(view::draw)
        assertEquals(q.exactCenterY(), center(down.draws.last { it.text == "1" }), .6f)
        touch(MotionEvent.ACTION_CANCEL, q.exactCenterX(), q.exactCenterY(), 120)
        val canceled = CaptureCanvas(Bitmap.createBitmap(840, 248, Bitmap.Config.ARGB_8888)).also(view::draw)
        assertEquals(center(canvas.draws.last { it.text == "1" }), center(canceled.draws.last { it.text == "1" }), .6f)
        assertTrue(actions.isEmpty())
    }

    @Test fun `kana punctuation key shows its four choices while idle`() {
        view.setMode(KeyboardMode.KANA)
        val idle = CaptureCanvas(Bitmap.createBitmap(400, 228, Bitmap.Config.ARGB_8888)).also(view::draw)
        val label = idle.draws.last { it.text == "、。?!" }
        assertEquals(18f, label.textSize, .1f)
        assertFalse(idle.draws.any { it.text == "、" })
        val accentId = KeyboardLayouts.layout(KeyboardMode.KANA, false, false).rows.flatMap { it.keys }
            .indexOfFirst { it.kind == KeyKind.ACCENT }
        val accentBounds = android.graphics.RectF(keyBounds(accentId))
        assertEquals(android.graphics.Color.WHITE, idle.roundRects.last {
            kotlin.math.abs(it.rect.centerX() - accentBounds.centerX()) < 1f &&
                kotlin.math.abs(it.rect.centerY() - accentBounds.centerY()) < 1f
        }.color)
    }

    @Test fun `number symbol keys show only assigned directional hints and select them through the production view`() {
        view.setMode(KeyboardMode.NUMBERS)
        val keys = KeyboardLayouts.layout(KeyboardMode.NUMBERS).rows.flatMap { it.keys }
        val minusId = keys.indexOfFirst { it.id == "five--" }
        val periodId = keys.indexOfFirst { it.id == "number-period" }
        val minus = keyBounds(minusId)
        val period = keyBounds(periodId)
        val canvas = CaptureCanvas(Bitmap.createBitmap(400, 228, Bitmap.Config.ARGB_8888)).also(view::draw)

        fun hintsIn(key: Rect) = canvas.draws.filter { draw ->
            draw.x in key.left.toFloat()..key.right.toFloat() && draw.y in key.top.toFloat()..key.bottom.toFloat()
        }
        val minusHints = hintsIn(minus)
        assertTrue(minusHints.any { it.text == "+" && it.x < minus.exactCenterX() })
        assertTrue(minusHints.any { it.text == "/" && it.y < minus.exactCenterY() })
        assertTrue(minusHints.any { it.text == "*" && it.x > minus.exactCenterX() })
        assertTrue(minusHints.any { it.text == "," && it.y > minus.exactCenterY() })
        assertEquals(1, minusHints.count { it.text == "," })
        val periodHints = hintsIn(period)
        assertTrue(periodHints.any { it.text == "," && it.x < period.exactCenterX() })
        assertTrue(periodHints.any { it.text == "=" && it.x > period.exactCenterX() })
        assertEquals(listOf(",", ".", "="), periodHints.map { it.text }.filter { it in setOf(",", ".", "=") }.sorted())

        touch(MotionEvent.ACTION_DOWN, minus.exactCenterX(), minus.exactCenterY(), 0)
        touch(MotionEvent.ACTION_MOVE, minus.exactCenterX(), minus.exactCenterY() - 24f, 10)
        val selected = CaptureCanvas(Bitmap.createBitmap(400, 228, Bitmap.Config.ARGB_8888)).also(view::draw)
        val slash = selected.draws.last { it.text == "/" }
        assertEquals(minus.exactCenterY(), slash.y + (slash.ascent + slash.descent) / 2f, .6f)
        touch(MotionEvent.ACTION_UP, minus.exactCenterX(), minus.exactCenterY() - 24f, 20)
        assertEquals(listOf(KeyAction.CommitText("/")), actions)

        actions.clear()
        touch(MotionEvent.ACTION_DOWN, period.exactCenterX(), period.exactCenterY(), 30)
        touch(MotionEvent.ACTION_MOVE, period.exactCenterX() - 24f, period.exactCenterY(), 40)
        val periodSelected = CaptureCanvas(Bitmap.createBitmap(400, 228, Bitmap.Config.ARGB_8888)).also(view::draw)
        val selectedPeriodLabels = periodSelected.draws.filter { draw ->
            draw.x in period.left.toFloat()..period.right.toFloat() && draw.y in period.top.toFloat()..period.bottom.toFloat()
        }
        assertEquals(1, selectedPeriodLabels.count { it.text == "," })
        assertEquals(period.exactCenterX(), selectedPeriodLabels.single { it.text == "," }.x, .6f)
        touch(MotionEvent.ACTION_UP, period.exactCenterX() - 24f, period.exactCenterY(), 50)
        assertEquals(listOf(KeyAction.CommitText(",")), actions)
    }

    @Test fun `unassigned production move keeps the center selection and center tap haptic`() {
        view.setMode(KeyboardMode.NUMBERS)
        val periodId = KeyboardLayouts.layout(KeyboardMode.NUMBERS).rows.flatMap { it.keys }
            .indexOfFirst { it.id == "number-period" }
        val period = keyBounds(periodId)
        touch(MotionEvent.ACTION_DOWN, period.exactCenterX(), period.exactCenterY(), 0)
        assertEquals(android.view.HapticFeedbackConstants.KEYBOARD_TAP, shadowOf(view).lastHapticFeedbackPerformed())
        touch(MotionEvent.ACTION_MOVE, period.exactCenterX(), period.exactCenterY() - 24f, 10)
        val directions = KeyboardView::class.java.getDeclaredField("directions").also { it.isAccessible = true }
            .get(view) as Map<*, *>
        assertEquals(Direction.CENTER, directions[0])
        assertEquals(android.view.HapticFeedbackConstants.KEYBOARD_TAP, shadowOf(view).lastHapticFeedbackPerformed())
        touch(MotionEvent.ACTION_UP, period.exactCenterX(), period.exactCenterY() - 24f, 20)
        assertEquals(listOf(KeyAction.CommitText(".")), actions)
    }

    @Test fun `production motion events apply independent flick sensitivities and preserve center cursor behavior`() {
        val density = view.resources.displayMetrics.density

        fun keyBoundsFor(mode: KeyboardMode, keyId: String): Rect {
            view.setMode(mode)
            view.measure(exact(400), exact(228)); view.layout(0, 0, 400, 228)
            val id = KeyboardLayouts.layout(mode).rows.flatMap { it.keys }.indexOfFirst { it.id == keyId }
            check(id >= 0) { "missing $keyId in $mode" }
            return keyBounds(id)
        }
        fun gesture(mode: KeyboardMode, keyId: String, dxDp: Float, dyDp: Float, end: Int = MotionEvent.ACTION_UP): List<KeyAction> {
            actions.clear()
            val key = keyBoundsFor(mode, keyId)
            val x = key.exactCenterX(); val y = key.exactCenterY()
            touch(MotionEvent.ACTION_DOWN, x, y)
            touch(MotionEvent.ACTION_MOVE, x + dxDp * density, y + dyDp * density, 1)
            touch(end, x + dxDp * density, y + dyDp * density, 2)
            return actions.toList()
        }

        view.setFlickSensitivities(FlickSensitivity.HIGH, FlickSensitivity.LOW)
        assertEquals(listOf(KeyAction.KanaInput("う")), gesture(KeyboardMode.KANA, "kana-あ", 0f, -13f))
        assertEquals(listOf(KeyAction.CommitText("q")), gesture(KeyboardMode.QWERTY, "key-q", 0f, 20f))
        assertEquals(listOf(KeyAction.CommitText(".")), gesture(KeyboardMode.NUMBERS, "number-period", 0f, -13f))

        view.setFlickSensitivities(FlickSensitivity.LOW, FlickSensitivity.HIGH)
        assertEquals(listOf(KeyAction.KanaInput("あ")), gesture(KeyboardMode.KANA, "kana-あ", 0f, -20f))
        assertEquals(listOf(KeyAction.CommitText("1")), gesture(KeyboardMode.QWERTY, "key-q", 0f, 20f))
        assertTrue(gesture(KeyboardMode.KANA, "kana-あ", 0f, -13f, MotionEvent.ACTION_CANCEL).isEmpty())

        view.setMode(KeyboardMode.QWERTY)
        view.measure(exact(400), exact(228)); view.layout(0, 0, 400, 228)
        val spaceId = KeyboardLayouts.layout(KeyboardMode.QWERTY).rows.flatMap { it.keys }
            .indexOfFirst { it.kind == KeyKind.SPACE }
        val space = keyBounds(spaceId)
        actions.clear()
        touch(MotionEvent.ACTION_DOWN, space.exactCenterX(), space.exactCenterY())
        touch(MotionEvent.ACTION_MOVE, space.exactCenterX() + 10f * density, space.exactCenterY(), 1)
        touch(MotionEvent.ACTION_UP, space.exactCenterX() + 10f * density, space.exactCenterY(), 2)
        assertEquals(listOf(KeyAction.MoveCursor(Direction.RIGHT)), actions)
    }

    @Test @LooperMode(LooperMode.Mode.PAUSED) fun `qwerty accent keys cancel their timer only after an unassigned horizontal selection`() {
        val looper = shadowOf(Looper.getMainLooper()).apply { pause() }
        val aId = KeyboardLayouts.layout(KeyboardMode.QWERTY).rows.flatMap { it.keys }
            .indexOfFirst { it.id == "key-a" }
        val a = keyBounds(aId)
        val activeAccents = KeyboardView::class.java.getDeclaredField("accentActive").also { it.isAccessible = true }

        listOf(-24f, 24f).forEachIndexed { index, dx ->
            actions.clear()
            touch(MotionEvent.ACTION_DOWN, a.exactCenterX(), a.exactCenterY(), index * 100L)
            touch(MotionEvent.ACTION_MOVE, a.exactCenterX() + dx, a.exactCenterY(), index * 100L + 1)
            looper.idleFor(KeyboardView.ACCENT_DELAY_MS + 1, TimeUnit.MILLISECONDS)
            assertTrue("$dx dp horizontal move must not open accent choices", (activeAccents.get(view) as Set<*>).isEmpty())
            assertEquals(android.view.HapticFeedbackConstants.KEYBOARD_TAP, shadowOf(view).lastHapticFeedbackPerformed())
            touch(MotionEvent.ACTION_UP, a.exactCenterX() + dx, a.exactCenterY(), index * 100L + 2)
            assertEquals("$dx dp horizontal move commits the center once", listOf(KeyAction.CommitText("a")), actions)
        }

        listOf(0f, 17f).forEachIndexed { index, dx ->
            actions.clear()
            val time = 300L + index * 100L
            touch(MotionEvent.ACTION_DOWN, a.exactCenterX(), a.exactCenterY(), time)
            if (dx != 0f) touch(MotionEvent.ACTION_MOVE, a.exactCenterX() + dx, a.exactCenterY(), time + 1)
            looper.idleFor(KeyboardView.ACCENT_DELAY_MS + 1, TimeUnit.MILLISECONDS)
            assertTrue("$dx dp horizontal move must retain accent choices", (activeAccents.get(view) as Set<*>).isNotEmpty())
            assertEquals(android.view.HapticFeedbackConstants.LONG_PRESS, shadowOf(view).lastHapticFeedbackPerformed())
            touch(MotionEvent.ACTION_UP, a.exactCenterX() + dx, a.exactCenterY(), time + 2)
            assertEquals("$dx dp horizontal move commits the first accent", listOf(KeyAction.CommitText("à")), actions)
        }
    }

    @Test fun `emoji layer reserves picker rows and keeps the fixed four row geometry`() {
        view.setEmojiRecents(listOf("❤️", "😀"))
        view.setMode(KeyboardMode.EMOJI)
        val canvas = CaptureCanvas(Bitmap.createBitmap(400, 228, Bitmap.Config.ARGB_8888)).also(view::draw)
        assertFalse(canvas.draws.any { it.text == "❤️" || it.text == "😀" })
        assertTrue(canvas.draws.any { it.text == "A" })
        assertEquals(228, view.measuredHeight)
    }

    @Test fun `emoji picker overlay leaves the fixed four-row rail touchable and accessible`() {
        view.setMode(KeyboardMode.EMOJI)
        val provider = view.accessibilityNodeProvider
        assertEquals(4, provider.createAccessibilityNodeInfo(-1)!!.childCount)
        touch(MotionEvent.ACTION_DOWN, 200f, 25f)
        touch(MotionEvent.ACTION_UP, 200f, 25f, 10)
        assertTrue(actions.isEmpty())
        val control = keyCenter(6)
        touch(MotionEvent.ACTION_DOWN, control.first, control.second, 20)
        touch(MotionEvent.ACTION_UP, control.first, control.second, 30)
        assertEquals(listOf(KeyAction.SwitchLayer(KeyboardMode.QWERTY)), actions)
    }

    @Test fun `emoji rail reaches the same left edge in every row`() {
        view.setMode(KeyboardMode.EMOJI)
        view.measure(exact(400), exact(228))
        view.layout(0, 0, 400, 228)
        val first = keyBounds(0)
        val second = keyBounds(2)
        val third = keyBounds(4)
        val left = keyBounds(6)
        assertEquals(first.left, second.left)
        assertEquals(second.left, third.left)
        assertEquals(third.left, left.left)
        assertEquals(first.width(), left.width())
    }

    @Test fun `emoji overlay geometry follows the keyboard content insets at phone and wide widths`() {
        val phone = view.emojiLayerHorizontalGeometryForWidth(412)
        assertEquals(3, phone.contentLeft)
        assertEquals(85, phone.railRight)
        assertEquals(409, phone.contentRight)
        assertEquals(324, phone.bodyWidth)

        view.setDualFlickEnabled(true)
        val wide = view.emojiLayerHorizontalGeometryForWidth(840)
        assertEquals(10, wide.contentLeft)
        assertEquals(113, wide.railRight)
        assertEquals(830, wide.contentRight)
        assertEquals(717, wide.bodyWidth)
    }

    @Test fun `emoji rail routes taps and treats unassigned flicks as center taps through production motion events`() {
        val rail = listOf(
            0 to KeyboardMode.KANA,
            2 to KeyboardMode.SYMBOLS,
            4 to KeyboardMode.NUMBERS,
            6 to KeyboardMode.QWERTY,
        )
        rail.forEachIndexed { index, (virtualId, mode) ->
            actions.clear()
            view.setMode(KeyboardMode.EMOJI)
            val key = keyBounds(virtualId)
            val x = key.exactCenterX()
            val y = key.exactCenterY()
            touch(MotionEvent.ACTION_DOWN, x, y, index * 100L)
            touch(MotionEvent.ACTION_UP, x, y, index * 100L + 10)
            assertEquals(listOf(KeyAction.SwitchLayer(mode)), actions)
        }

        view.setMode(KeyboardMode.EMOJI)
        val symbol = keyBounds(2)
        listOf(-30f to 0f, 0f to -30f, 30f to 0f, 0f to 30f).forEachIndexed { index, (dx, dy) ->
            actions.clear()
            view.setMode(KeyboardMode.EMOJI)
            val x = symbol.exactCenterX()
            val y = symbol.exactCenterY()
            val start = 400L + index * 100L
            touch(MotionEvent.ACTION_DOWN, x, y, start)
            touch(MotionEvent.ACTION_MOVE, x + dx, y + dy, start + 10)
            touch(MotionEvent.ACTION_UP, x + dx, y + dy, start + 20)
            assertEquals(listOf(KeyAction.SwitchLayer(KeyboardMode.SYMBOLS)), actions)
        }

        view.setMode(KeyboardMode.EMOJI)
        val returnedSymbol = keyBounds(2)
        actions.clear()
        val x = returnedSymbol.exactCenterX()
        val y = returnedSymbol.exactCenterY()
        touch(MotionEvent.ACTION_DOWN, x, y, 900)
        touch(MotionEvent.ACTION_MOVE, x + 30f, y, 910)
        touch(MotionEvent.ACTION_MOVE, x + 5f, y, 920)
        touch(MotionEvent.ACTION_UP, x + 5f, y, 930)
        assertEquals(listOf(KeyAction.SwitchLayer(KeyboardMode.SYMBOLS)), actions)

        actions.clear()
        view.setMode(KeyboardMode.EMOJI)
        touch(MotionEvent.ACTION_DOWN, x, y, 1_000)
        touch(MotionEvent.ACTION_CANCEL, x, y, 1_010)
        assertTrue(actions.isEmpty())
    }

    @Test fun `paste uses the same selected label composition in every nonconverting layer`() {
        Settings.Global.putFloat(view.context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        listOf(KeyboardMode.QWERTY, KeyboardMode.SYMBOLS, KeyboardMode.KANA, KeyboardMode.NUMBERS, KeyboardMode.VOICE).forEach { mode ->
            val modeActions = mutableListOf<KeyAction>()
            val modeView = KeyboardView(Robolectric.buildActivity(Activity::class.java).setup().get()).apply {
                actionSink = KeyboardActionSink { modeActions += it }
                setMode(mode)
                setConversionActive(false)
                measure(exact(400), exact(228)); layout(0, 0, 400, 228)
            }
            val provider = modeView.accessibilityNodeProvider
            val enterId = KeyboardLayouts.layout(mode, false, false).rows.flatMap { it.keys }
                .indexOfFirst { it.kind == KeyKind.ENTER }
            check(enterId >= 0) { "$mode has no Enter key" }
            val enter = Rect().also { provider.createAccessibilityNodeInfo(enterId)!!.getBoundsInParent(it) }
            val startX = enter.left + 2f
            val startY = enter.top + 2f
            fun center(draw: TextDraw) = draw.y + (draw.ascent + draw.descent) / 2f
            val idleCanvas = CaptureCanvas(Bitmap.createBitmap(400, 228, Bitmap.Config.ARGB_8888)).also(modeView::draw)
            val idlePasteCenter = center(idleCanvas.draws.last { it.text == "paste" })
            fun modeTouch(action: Int, y: Float, time: Long) = MotionEvent.obtain(0, time, action, startX, y, 0).also {
                modeView.onTouchEvent(it); it.recycle()
            }
            modeTouch(MotionEvent.ACTION_DOWN, startY, 0)
            modeTouch(MotionEvent.ACTION_MOVE, startY - 24f * modeView.resources.displayMetrics.density, 10)
            val selectedDirections = KeyboardView::class.java.getDeclaredField("directions").also { it.isAccessible = true }
                .get(modeView) as Map<*, *>
            assertEquals("$mode selected direction", Direction.UP, selectedDirections[0])
            val canvas = CaptureCanvas(Bitmap.createBitmap(400, 228, Bitmap.Config.ARGB_8888)).also(modeView::draw)
            val paste = canvas.draws.lastOrNull { it.text == "paste" } ?: error("$mode did not draw paste")
            val main = canvas.draws.lastOrNull { it.text == "Enter" } ?: error("$mode did not draw Enter")
            assertEquals("$mode paste size", 17f, paste.textSize, .2f)
            assertTrue("$mode paste starts below center", idlePasteCenter > enter.exactCenterY())
            assertEquals("$mode paste selected center", enter.exactCenterY(), center(paste), .6f)
            assertEquals("$mode Enter hidden", 0, main.alpha)
            modeTouch(MotionEvent.ACTION_UP, startY - 24f * modeView.resources.displayMetrics.density, 20)
            assertEquals("$mode action", listOf(KeyAction.Paste), modeActions)
        }
    }

    @Test fun `enter down centers the upper control j hint and dispatches it`() {
        Settings.Global.putFloat(view.context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        val provider = view.accessibilityNodeProvider
        val enterId = KeyboardLayouts.layout(KeyboardMode.QWERTY).rows.flatMap { it.keys }
            .indexOfFirst { it.kind == KeyKind.ENTER }
        val enter = Rect().also { provider.createAccessibilityNodeInfo(enterId)!!.getBoundsInParent(it) }
        val x = enter.centerX().toFloat()
        val y = enter.centerY().toFloat()

        val idle = CaptureCanvas(Bitmap.createBitmap(400, 228, Bitmap.Config.ARGB_8888)).also(view::draw)
        val idleControlJ = idle.draws.single { it.text == "C-j" }
        assertEquals(enter.top + 8f, idleControlJ.y + (idleControlJ.ascent + idleControlJ.descent) / 2f, .6f)
        assertTrue(idle.draws.single { it.text == "paste" }.y > idleControlJ.y)

        touch(MotionEvent.ACTION_DOWN, x, y, 0)
        touch(MotionEvent.ACTION_MOVE, x, y + 24f * view.resources.displayMetrics.density, 10)
        val selected = CaptureCanvas(Bitmap.createBitmap(400, 228, Bitmap.Config.ARGB_8888)).also(view::draw)
        val controlJ = selected.draws.single { it.text == "C-j" }
        assertEquals(17f, controlJ.textSize, .2f)
        assertEquals(enter.centerY().toFloat(), controlJ.y + (controlJ.ascent + controlJ.descent) / 2f, .6f)
        assertTrue(actions.isEmpty())

        touch(MotionEvent.ACTION_UP, x, y + 24f * view.resources.displayMetrics.density, 20)
        assertEquals(listOf(KeyAction.ModifiedKey("j", Modifier.CTRL)), actions)
    }

    @Test fun `conversion start discards only a held Enter and the next Enter commits`() {
        view.setMode(KeyboardMode.KANA)
        view.setDualFlickEnabled(true)
        view.measure(exact(840), exact(256)); view.layout(0, 0, 840, 256)
        val keys = KeyboardLayouts.layout(KeyboardMode.KANA, true, false).rows.flatMap { it.keys }
        val enterId = keys.indexOfFirst { it.kind == KeyKind.ENTER }
        val kanaId = keys.indexOfFirst { it.kind == KeyKind.KANA }
        val enter = keyCenter(enterId)
        val kana = keyCenter(kanaId)
        view.actionSink = KeyboardActionSink { action ->
            actions += action
            if (action is KeyAction.KanaInput) view.setConversionActive(true)
        }
        multiTouch(MotionEvent.ACTION_DOWN, listOf(0 to enter))
        multiTouch(MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT), listOf(0 to enter, 1 to kana))
        multiTouch(MotionEvent.ACTION_MOVE, listOf(0 to (enter.first to enter.second + 24f), 1 to kana))
        multiTouch(MotionEvent.ACTION_POINTER_UP or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT), listOf(0 to enter, 1 to kana))
        multiTouch(MotionEvent.ACTION_UP, listOf(0 to enter))
        assertEquals(listOf(KeyAction.KanaInput("あ")), actions)

        val convertingEnter = keyCenter(KeyboardLayouts.layout(KeyboardMode.KANA, true, true).rows.flatMap { it.keys }
            .indexOfFirst { it.kind == KeyKind.ENTER })
        multiTouch(MotionEvent.ACTION_DOWN, listOf(0 to convertingEnter))
        multiTouch(MotionEvent.ACTION_UP, listOf(0 to convertingEnter))
        assertEquals(listOf(KeyAction.KanaInput("あ"), KeyAction.CommitWithoutConversion), actions)
    }

    @Test fun `dual kana exposes two twelve-key groups only on wide layouts`() {
        view.setMode(KeyboardMode.KANA)
        view.setDualFlickEnabled(true)
        assertEquals(19, requireNotNull(view.accessibilityNodeProvider.createAccessibilityNodeInfo(-1)).childCount)

        view.measure(exact(599), exact(228))
        view.layout(0, 0, 599, 228)
        assertEquals(19, requireNotNull(view.accessibilityNodeProvider.createAccessibilityNodeInfo(-1)).childCount)

        view.measure(exact(600), exact(256))
        view.layout(0, 0, 600, 256)
        view.draw(Canvas(Bitmap.createBitmap(600, 256, Bitmap.Config.ARGB_8888)))
        assertEquals(31, requireNotNull(view.accessibilityNodeProvider.createAccessibilityNodeInfo(-1)).childCount)
    }

    @Test fun `qwerty geometry matches css gaps heights and bottom row proportions`() {
        view.measure(exact(412), exact(228))
        view.layout(0, 0, 412, 228)
        val provider = view.accessibilityNodeProvider
        fun bounds(id: Int) = Rect().also { provider.createAccessibilityNodeInfo(id)!!.getBoundsInParent(it) }
        val q = bounds(0)
        val w = bounds(1)
        assertEquals(45, q.height())
        assertEquals(6, w.left - q.right)
        assertEquals(10, bounds(10).top - q.bottom)
        val mode = bounds(31).width() + 6
        val space = bounds(32).width() + 6
        val enter = bounds(33).width() + 6
        val total = mode + space + enter
        assertEquals(.19f, mode.toFloat() / total, .01f)
        assertEquals(.55f, space.toFloat() / total, .01f)
        assertEquals(.26f, enter.toFloat() / total, .01f)
        val canvas = CaptureCanvas(Bitmap.createBitmap(412, 228, Bitmap.Config.ARGB_8888)).also(view::draw)
        assertTrue("half-width delete icon remains readable", canvas.draws.last { it.text == "⌫" }.textSize >= 11f)
    }

    @Test fun `horizontal and vertical visual gaps assign their halves without changing faces`() {
        view.measure(exact(412), exact(228))
        view.layout(0, 0, 412, 228)
        val q = keyBounds(0)
        val w = keyBounds(1)
        val a = keyBounds(11)
        val qBefore = Rect(q)
        val wBefore = Rect(w)

        assertEquals(6, w.left - q.right)
        assertEquals(0, view.hitTargetIndexAt(q.right + 1f, q.exactCenterY()))
        assertEquals(1, view.hitTargetIndexAt(w.left - 1f, w.exactCenterY()))
        touch(MotionEvent.ACTION_DOWN, q.right + 1f, q.exactCenterY())
        touch(MotionEvent.ACTION_UP, q.right + 1f, q.exactCenterY(), 1)
        touch(MotionEvent.ACTION_DOWN, w.left - 1f, w.exactCenterY(), 2)
        touch(MotionEvent.ACTION_UP, w.left - 1f, w.exactCenterY(), 3)

        val sharedX = maxOf(q.left, a.left) + 1f
        assertTrue(sharedX < minOf(q.right, a.right))
        assertEquals(10, a.top - q.bottom)
        assertEquals(0, view.hitTargetIndexAt(sharedX, q.bottom + 1f))
        assertEquals(11, view.hitTargetIndexAt(sharedX, a.top - 1f))
        touch(MotionEvent.ACTION_DOWN, sharedX, q.bottom + 1f, 4)
        touch(MotionEvent.ACTION_UP, sharedX, q.bottom + 1f, 5)
        touch(MotionEvent.ACTION_DOWN, sharedX, a.top - 1f, 6)
        touch(MotionEvent.ACTION_UP, sharedX, a.top - 1f, 7)

        assertEquals(listOf(KeyAction.CommitText("q"), KeyAction.CommitText("w"), KeyAction.CommitText("q"), KeyAction.CommitText("a")), actions)
        assertEquals(qBefore, keyBounds(0))
        assertEquals(wBefore, keyBounds(1))
    }

    @Test fun `accessibility resolves visual gaps and keeps emoji content outside its viewport inert`() {
        view.measure(exact(412), exact(228))
        view.layout(0, 0, 412, 228)
        val q = keyBounds(0)
        val w = keyBounds(1)
        val qGap = q.right + 1f
        // ExploreByTouchHelper resolves its virtual id through hitTargetIndexAt;
        // this proves the same gap geometry without relying on a protected View API.
        assertEquals(0, view.hitTargetIndexAt(qGap, q.exactCenterY()))

        view.setMode(KeyboardMode.EMOJI)
        val emojiMode = keyBounds(0)
        val bodyX = emojiMode.right + emojiMode.width() * 2f
        assertEquals(-1, view.hitTargetIndexAt(bodyX, emojiMode.exactCenterY()))
        touch(MotionEvent.ACTION_DOWN, bodyX, emojiMode.exactCenterY(), 1)
        touch(MotionEvent.ACTION_UP, bodyX, emojiMode.exactCenterY(), 2)
        assertTrue(actions.isEmpty())
    }

    @Test fun `spanning enter and dual kana gaps have one unambiguous owner`() {
        view.setMode(KeyboardMode.KANA)
        view.measure(exact(400), exact(228))
        view.layout(0, 0, 400, 228)
        val space = keyBounds(9)
        val enter = keyBounds(14)
        val enterX = enter.exactCenterX()
        assertEquals(10, enter.top - space.bottom)
        assertEquals(9, view.hitTargetIndexAt(enterX, space.bottom + 1f))
        assertEquals(14, view.hitTargetIndexAt(enterX, enter.top - 1f))
        touch(MotionEvent.ACTION_DOWN, enterX, space.bottom + 1f)
        touch(MotionEvent.ACTION_UP, enterX, space.bottom + 1f, 1)
        touch(MotionEvent.ACTION_DOWN, enterX, enter.top - 1f, 2)
        touch(MotionEvent.ACTION_UP, enterX, enter.top - 1f, 3)
        assertEquals(listOf(KeyAction.CycleCandidate, KeyAction.Enter), actions)

        actions.clear()
        view.setDualFlickEnabled(true)
        view.measure(exact(840), exact(256))
        view.layout(0, 0, 840, 256)
        val first = keyBounds(1)
        val second = keyBounds(2)
        assertEquals(6, second.left - first.right)
        assertEquals(1, view.hitTargetIndexAt(first.right + 1f, first.exactCenterY()))
        assertEquals(2, view.hitTargetIndexAt(second.left - 1f, second.exactCenterY()))
        touch(MotionEvent.ACTION_DOWN, first.right + 1f, first.exactCenterY(), 4)
        touch(MotionEvent.ACTION_UP, first.right + 1f, first.exactCenterY(), 5)
        touch(MotionEvent.ACTION_DOWN, second.left - 1f, second.exactCenterY(), 6)
        touch(MotionEvent.ACTION_UP, second.left - 1f, second.exactCenterY(), 7)
        assertEquals(listOf(KeyAction.KanaInput("あ"), KeyAction.KanaInput("か")), actions)
    }

    @Test fun `kana uses qwerty vertical gap and face height at inner width`() {
        view.setMode(KeyboardMode.KANA)
        view.measure(exact(840), exact(256))
        view.layout(0, 0, 840, 256)
        val provider = view.accessibilityNodeProvider
        fun bounds(id: Int) = Rect().also { provider.createAccessibilityNodeInfo(id)!!.getBoundsInParent(it) }
        val first = bounds(0)
        assertEquals(13, first.left)
        assertEquals(45, first.height())
        assertEquals(10, bounds(5).top - first.bottom)
        assertEquals(6, bounds(1).left - first.right)
    }

    @Test fun `all six four row layers share outer height face height and vertical gaps`() {
        listOf(412 to 228, 840 to 228).forEach { (width, height) ->
            val geometry = KeyboardMode.entries.associateWith { mode ->
                view.setMode(mode)
                view.measure(exact(width), View.MeasureSpec.makeMeasureSpec(1_000, View.MeasureSpec.AT_MOST))
                val measuredHeight = view.measuredHeight
                view.layout(0, 0, width, measuredHeight)
                val first = keyBounds(0)
                val nextRowId = KeyboardLayouts.layout(mode, false, false).rows.first().keys.size
                val secondRow = keyBounds(nextRowId)
                Triple(measuredHeight, first.height(), secondRow.top - first.bottom)
            }
            assertEquals("$width outer heights", 1, geometry.values.map { it.first }.distinct().size)
            assertTrue("$width key faces", geometry.values.all { it.second == 45 })
            assertTrue("$width vertical gaps", geometry.values.all { it.third == 10 })
            if (width >= 600) {
                view.setMode(KeyboardMode.KANA)
                view.setDualFlickEnabled(true)
                view.measure(exact(width), View.MeasureSpec.makeMeasureSpec(1_000, View.MeasureSpec.AT_MOST))
                view.layout(0, 0, width, view.measuredHeight)
                val first = keyBounds(0)
                val nextRowId = KeyboardLayouts.layout(KeyboardMode.KANA, true, false).rows.first().keys.size
                val secondRow = keyBounds(nextRowId)
                assertEquals("dual kana face", 45, first.height())
                assertEquals("dual kana vertical gap", 10, secondRow.top - first.bottom)
                view.setDualFlickEnabled(false)
            }
        }
    }

    @Test fun `kana popup leaves surrounding key backgrounds undimmed`() {
        view.setMode(KeyboardMode.KANA)
        view.measure(exact(400), exact(228))
        view.layout(0, 0, 400, 228)
        fun render() = Bitmap.createBitmap(400, 228, Bitmap.Config.ARGB_8888).also { view.draw(Canvas(it)) }
        val idle = render()
        touch(MotionEvent.ACTION_DOWN, 120f, 20f)
        val popup = render()
        assertEquals(idle.getPixel(250, 200), popup.getPixel(250, 200))
    }

    @Test fun `simultaneous dual kana pointers dispatch in pointer up order`() {
        view.setMode(KeyboardMode.KANA)
        view.setDualFlickEnabled(true)
        view.measure(exact(840), exact(220))
        view.layout(0, 0, 840, 220)
        view.draw(Canvas(Bitmap.createBitmap(840, 220, Bitmap.Config.ARGB_8888)))
        view.actionSink = KeyboardActionSink { action ->
            actions += action
            if (action is KeyAction.KanaInput) view.setConversionActive(true)
        }

        multiTouch(MotionEvent.ACTION_DOWN, listOf(0 to (150f to 20f)))
        multiTouch(MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
            listOf(0 to (150f to 20f), 1 to (470f to 20f)))
        multiTouch(MotionEvent.ACTION_POINTER_UP or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
            listOf(0 to (150f to 20f), 1 to (470f to 20f)))
        multiTouch(MotionEvent.ACTION_UP, listOf(0 to (150f to 20f)))

        assertEquals(listOf(KeyAction.KanaInput("あ"), KeyAction.KanaInput("あ")), actions)
    }

    @Test fun `single and dual kana targets never overlap spanning enter`() {
        view.setMode(KeyboardMode.KANA)
        assertNoVisibleTargetsOverlap(width = 400, height = 228, expectedCount = 19)
        view.setDualFlickEnabled(true)
        assertNoVisibleTargetsOverlap(width = 840, height = 256, expectedCount = 31)
    }

    @Test fun `both halves of spanning kana enter dispatch enter`() {
        view.setMode(KeyboardMode.KANA)
        view.measure(exact(400), exact(228))
        view.layout(0, 0, 400, 228)
        val enterId = KeyboardLayouts.layout(KeyboardMode.KANA, false, false).rows.flatMap { it.keys }
            .indexOfFirst { it.kind == KeyKind.ENTER }
        val enter = keyBounds(enterId)
        val x = enter.exactCenterX()
        touch(MotionEvent.ACTION_DOWN, x, enter.top + 2f)
        touch(MotionEvent.ACTION_UP, x, enter.top + 2f, 5)
        assertEquals("upper half", listOf(KeyAction.Enter), actions)
        touch(MotionEvent.ACTION_DOWN, x, enter.bottom - 2f, 10)
        touch(MotionEvent.ACTION_UP, x, enter.bottom - 2f, 15)
        assertEquals(listOf(KeyAction.Enter, KeyAction.Enter), actions)
    }

    private fun renderAtFontScale(fontScale: Float): Bitmap {
        val base = RuntimeEnvironment.getApplication()
        val configuration = Configuration(base.resources.configuration).apply { this.fontScale = fontScale }
        val context = base.createConfigurationContext(configuration)
        return Bitmap.createBitmap(400, 228, Bitmap.Config.ARGB_8888).also { bitmap ->
            KeyboardView(context).apply {
                measure(exact(400), exact(228))
                layout(0, 0, 400, 228)
                draw(Canvas(bitmap))
            }
        }
    }

    private fun multiTouch(action: Int, pointers: List<Pair<Int, Pair<Float, Float>>>) {
        val properties = pointers.map { (id, _) -> MotionEvent.PointerProperties().apply { this.id = id } }.toTypedArray()
        val coordinates = pointers.map { (_, point) -> MotionEvent.PointerCoords().apply { x = point.first; y = point.second } }.toTypedArray()
        MotionEvent.obtain(0, 0, action, pointers.size, properties, coordinates, 0, 0, 1f, 1f, 0, 0, 0, 0).also {
            view.onTouchEvent(it)
            it.recycle()
        }
    }

    private fun assertNoVisibleTargetsOverlap(width: Int, height: Int, expectedCount: Int) {
        view.measure(exact(width), exact(height))
        view.layout(0, 0, width, height)
        view.draw(Canvas(Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)))
        val provider = view.accessibilityNodeProvider
        val bounds = (0 until expectedCount).map { id ->
            Rect().also { provider.createAccessibilityNodeInfo(id)!!.getBoundsInParent(it) }
        }
        bounds.forEachIndexed { index, first ->
            bounds.drop(index + 1).forEach { second ->
                assertTrue("virtual key bounds overlap: $first and $second", !Rect.intersects(first, second))
            }
        }
    }

    private fun touch(action: Int, x: Float, y: Float, time: Long = 0) {
        val event = MotionEvent.obtain(0, time, action, x, y, 0)
        view.onTouchEvent(event)
        event.recycle()
    }

    private fun performGesture(
        bounds: Rect,
        moves: List<Pair<Float, Float>> = emptyList(),
        endAction: Int = MotionEvent.ACTION_UP,
        time: Long = 0,
    ) {
        val startX = bounds.exactCenterX(); val startY = bounds.exactCenterY()
        touch(MotionEvent.ACTION_DOWN, startX, startY, time)
        moves.forEachIndexed { index, (dx, dy) -> touch(MotionEvent.ACTION_MOVE, startX + dx, startY + dy, time + index + 1) }
        val (endDx, endDy) = moves.lastOrNull() ?: (0f to 0f)
        touch(endAction, startX + endDx, startY + endDy, time + moves.size + 1)
    }

    private fun keyCenter(virtualId: Int): Pair<Float, Float> {
        val bounds = keyBounds(virtualId)
        return bounds.exactCenterX() to bounds.exactCenterY()
    }

    private fun keyBounds(virtualId: Int) = Rect().also {
        view.accessibilityNodeProvider.createAccessibilityNodeInfo(virtualId)!!.getBoundsInParent(it)
    }

    private fun assertGlyphInside(x: Float, baseline: Float, label: String, textSize: Float, key: Rect) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.textSize = textSize; textAlign = Paint.Align.CENTER }
        val glyph = Rect()
        paint.getTextBounds(label, 0, label.length, glyph)
        val left = x - paint.measureText(label) / 2f + glyph.left
        val right = x - paint.measureText(label) / 2f + glyph.right
        assertTrue("$label left=$left key=$key", left >= key.left)
        assertTrue("$label right=$right key=$key", right <= key.right)
        assertTrue("$label top=${baseline + glyph.top} key=$key", baseline + glyph.top >= key.top)
        assertTrue("$label bottom=${baseline + glyph.bottom} key=$key", baseline + glyph.bottom <= key.bottom)
    }

    private data class TextDraw(
        val text: String,
        val x: Float,
        val y: Float,
        val textSize: Float,
        val alpha: Int,
        val ascent: Float,
        val descent: Float,
        val letterSpacing: Float,
        val color: Int,
    )

    private data class RoundDraw(val rect: android.graphics.RectF, val color: Int)

    private class CaptureCanvas(bitmap: Bitmap) : Canvas(bitmap) {
        val draws = mutableListOf<TextDraw>()
        val roundRects = mutableListOf<RoundDraw>()

        override fun drawText(text: String, x: Float, y: Float, paint: Paint) {
            draws += TextDraw(text, x, y, paint.textSize, paint.alpha, paint.ascent(), paint.descent(), paint.letterSpacing, paint.color)
            super.drawText(text, x, y, paint)
        }

        override fun drawRoundRect(rect: android.graphics.RectF, rx: Float, ry: Float, paint: Paint) {
            roundRects += RoundDraw(android.graphics.RectF(rect), paint.color)
            super.drawRoundRect(rect, rx, ry, paint)
        }
    }

    private fun KeyboardView.voiceInputLevel(): Float? =
        (KeyboardView::class.java.getDeclaredField("state").apply { isAccessible = true }.get(this) as KeyboardUiState).voiceInputLevel

    private fun exact(size: Int) = android.view.View.MeasureSpec.makeMeasureSpec(size, android.view.View.MeasureSpec.EXACTLY)
}
