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

    @Test fun `backspace tap deletes down escapes and other directions are inert`() {
        touch(MotionEvent.ACTION_DOWN, 390f, 67f)
        touch(MotionEvent.ACTION_UP, 390f, 67f, 5)
        touch(MotionEvent.ACTION_DOWN, 390f, 67f, 10)
        touch(MotionEvent.ACTION_MOVE, 390f, 40f, 20)
        touch(MotionEvent.ACTION_UP, 390f, 40f, 25)
        touch(MotionEvent.ACTION_DOWN, 390f, 67f, 30)
        touch(MotionEvent.ACTION_MOVE, 390f, 90f, 40)
        touch(MotionEvent.ACTION_UP, 390f, 90f, 45)
        assertEquals(listOf(KeyAction.Backspace(), KeyAction.Escape), actions)
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

    @Test fun `preview consumes touch and accessibility without changing input state`() {
        view.setPreviewOnly(true)
        touch(MotionEvent.ACTION_DOWN, 40f, 20f)
        touch(MotionEvent.ACTION_UP, 40f, 20f, 10)
        val provider = view.accessibilityNodeProvider

        assertTrue(actions.isEmpty())
        assertTrue(!provider.performAction(0, AccessibilityNodeInfo.ACTION_CLICK, null))
        assertTrue(actions.isEmpty())
    }

    @Test fun `extreme label adjustments preserve bounded nonoverlapping keys at narrow and wide widths`() {
        val extreme = QwertyLabelGroup.entries.fold(QwertyLabelStyle.DEFAULT) { style, group ->
            style.with(group, LabelAdjustment(1.3f, 6f, 8f))
        }
        view.setQwertyLabelStyle(extreme)

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

    @Test fun `label style does not change key hit bounds`() {
        fun bounds() = Rect().also { view.accessibilityNodeProvider.createAccessibilityNodeInfo(0)!!.getBoundsInParent(it) }
        val before = bounds()
        view.setQwertyLabelStyle(QwertyLabelStyle.DEFAULT.with(QwertyLabelGroup.LETTER_PRIMARY, LabelAdjustment(.7f, -6f, -8f)))
        view.draw(Canvas(Bitmap.createBitmap(400, 228, Bitmap.Config.ARGB_8888)))
        assertEquals(before, bounds())
    }

    @Test fun `modifier and composite glyphs remain wholly inside narrow and wide keys at adjustment limits`() {
        listOf(412 to 220, 840 to 248).forEach { (width, height) ->
            listOf(-1f, 1f).forEach { sign ->
                val adjustment = LabelAdjustment(1.3f, 6f * sign, 8f * sign)
                view.setQwertyLabelStyle(QwertyLabelStyle.DEFAULT.with(QwertyLabelGroup.COMPOSITE_SMALL, adjustment))
                view.measure(exact(width), exact(height)); view.layout(0, 0, width, height)
                val canvas = Canvas(Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888))
                view.draw(canvas)
                val shadow = shadowOf(canvas)
                val events = (0 until shadow.textHistoryCount).map(shadow::getDrawnTextEvent)

                assertGlyphInside(events.last { it.text == "C" }.x, events.last { it.text == "C" }.y, "C", 13f, keyBounds(10))
                assertGlyphInside(events.last { it.text == "A" }.x, events.last { it.text == "A" }.y, "A", 13f, keyBounds(10))
                assertGlyphInside(events.last { it.text == "ん" }.x, events.last { it.text == "ん" }.y, "ん", 16f * 1.3f * .64f, keyBounds(31))
                assertGlyphInside(events.last { it.text == "あ" }.x, events.last { it.text == "あ" }.y, "あ", 16f * 1.3f, keyBounds(31))
            }
        }
    }

    @Test @LooperMode(LooperMode.Mode.PAUSED) fun `one second layer hold begins voice and release ends without layer action`() {
        val events = mutableListOf<VoiceHoldEvent>()
        view.voiceHoldSink = VoiceHoldSink(events::add)
        val layer = keyCenter(31)
        touch(MotionEvent.ACTION_DOWN, layer.first, layer.second)
        shadowOf(Looper.getMainLooper()).idleFor(KeyboardView.VOICE_HOLD_DELAY_MS, TimeUnit.MILLISECONDS)
        assertEquals(listOf(VoiceHoldEvent.Begin(1)), events)
        touch(MotionEvent.ACTION_MOVE, layer.first + 60f, layer.second - 90f, 1_010)
        touch(MotionEvent.ACTION_UP, layer.first + 60f, layer.second - 90f, 1_020)

        assertEquals(listOf(VoiceHoldEvent.Begin(1), VoiceHoldEvent.End(1)), events)
        assertTrue(actions.isEmpty())
    }

    @Test @LooperMode(LooperMode.Mode.PAUSED) fun `direction before hold and second pointer cancel voice arming`() {
        val events = mutableListOf<VoiceHoldEvent>()
        view.voiceHoldSink = VoiceHoldSink(events::add)
        val layer = keyCenter(31)
        touch(MotionEvent.ACTION_DOWN, layer.first, layer.second)
        touch(MotionEvent.ACTION_MOVE, layer.first + 35f, layer.second, 20)
        shadowOf(Looper.getMainLooper()).idleFor(KeyboardView.VOICE_HOLD_DELAY_MS, TimeUnit.MILLISECONDS)
        touch(MotionEvent.ACTION_UP, layer.first + 35f, layer.second, 1_120)
        assertTrue("events=$events", events.isEmpty())
        assertTrue(actions.contains(KeyAction.SwitchLayer(KeyboardMode.QWERTY)))

        actions.clear(); view.setMode(KeyboardMode.QWERTY)
        multiTouch(MotionEvent.ACTION_DOWN, listOf(0 to layer))
        multiTouch(MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT), listOf(0 to layer, 1 to (20f to 20f)))
        shadowOf(Looper.getMainLooper()).idleFor(KeyboardView.VOICE_HOLD_DELAY_MS, TimeUnit.MILLISECONDS)
        multiTouch(MotionEvent.ACTION_POINTER_UP or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT), listOf(0 to layer, 1 to (20f to 20f)))
        multiTouch(MotionEvent.ACTION_UP, listOf(0 to layer))
        assertTrue("events after second pointer=$events", events.isEmpty())
    }

    @Test @LooperMode(LooperMode.Mode.PAUSED) fun `mode change cancels active voice hold and stale ready is inert`() {
        val events = mutableListOf<VoiceHoldEvent>()
        view.voiceHoldSink = VoiceHoldSink(events::add)
        val layer = keyCenter(31)
        touch(MotionEvent.ACTION_DOWN, layer.first, layer.second)
        shadowOf(Looper.getMainLooper()).idleFor(KeyboardView.VOICE_HOLD_DELAY_MS, TimeUnit.MILLISECONDS)

        view.setMode(KeyboardMode.KANA)
        view.onVoiceRecordingReady(1)

        assertEquals(listOf(VoiceHoldEvent.Begin(1), VoiceHoldEvent.Cancel(1)), events)
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
        assertEquals(center(idleAux) + 13f, center(downAux), .6f)
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

    @Test @LooperMode(LooperMode.Mode.PAUSED) fun `enter down animates paste and dispatches paste only on release`() {
        Settings.Global.putFloat(view.context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        val looper = shadowOf(Looper.getMainLooper()).apply { pause() }
        fun frame() = CaptureCanvas(Bitmap.createBitmap(400, 228, Bitmap.Config.ARGB_8888)).also(view::draw)
        fun center(draw: TextDraw) = draw.y + (draw.ascent + draw.descent) / 2f
        val enter = keyBounds(33)
        val idle = frame()
        val idleMain = idle.draws.last { it.text == "Enter" }
        val idleHint = idle.draws.last { it.text == "paste" }
        assertEquals(enter.top + 29f, center(idleMain), .6f)
        assertEquals(enter.top + 12.5f, center(idleHint), .6f)
        assertEquals((255 * .7f).toInt(), idleHint.alpha)
        assertEquals(.7f / idleHint.textSize, idleHint.letterSpacing, .001f)
        assertEquals(android.graphics.Color.rgb(244, 244, 246), idleHint.color)
        touch(MotionEvent.ACTION_DOWN, enter.centerX().toFloat(), enter.centerY().toFloat())
        touch(MotionEvent.ACTION_MOVE, enter.centerX().toFloat(), enter.centerY() + 24f, 10)
        assertTrue(actions.isEmpty())
        looper.idleFor(100, TimeUnit.MILLISECONDS)
        val selected = frame()
        val paste = selected.draws.last { it.text == "paste" }
        assertEquals(center(idleHint) + 13f, center(paste), .6f)
        assertEquals(10f * 1.7f, paste.textSize, .2f)
        assertEquals(0, selected.draws.last { it.text == "Enter" }.alpha)
        touch(MotionEvent.ACTION_UP, enter.centerX().toFloat(), enter.centerY() + 24f, 120)
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
        assertEquals(android.graphics.Color.rgb(20, 20, 22), qShadow.color)
        assertEquals(q.bottom + 1f, qShadow.rect.bottom, .01f)
    }

    @Test fun `wide qwerty keeps relative label anchors as rows grow`() {
        view.measure(exact(840), exact(248))
        view.layout(0, 0, 840, 248)
        val q = keyBounds(0)
        val canvas = CaptureCanvas(Bitmap.createBitmap(840, 248, Bitmap.Config.ARGB_8888)).also(view::draw)
        fun center(draw: TextDraw) = draw.y + (draw.ascent + draw.descent) / 2f
        assertEquals(q.exactCenterY() + 5f, center(canvas.draws.last { it.text == "q" }), .6f)
        assertEquals(q.top + 9f, center(canvas.draws.last { it.text == "1" }), .6f)
    }

    @Test fun `kana punctuation key shows its four choices while idle`() {
        view.setMode(KeyboardMode.KANA)
        val idle = CaptureCanvas(Bitmap.createBitmap(400, 228, Bitmap.Config.ARGB_8888)).also(view::draw)
        val label = idle.draws.last { it.text == "、。?!" }
        assertEquals(18f, label.textSize, .1f)
        assertFalse(idle.draws.any { it.text == "、" })
    }

    @Test fun `cursor layer uses generic labels and the reference space hint stack`() {
        view.setMode(KeyboardMode.CURSOR)
        val canvas = CaptureCanvas(Bitmap.createBitmap(400, 228, Bitmap.Config.ARGB_8888)).also(view::draw)
        assertEquals(25f, canvas.draws.last { it.text == "先頭" }.textSize, .1f)
        assertEquals(25f, canvas.draws.last { it.text == "↑" }.textSize, .1f)
        assertEquals(16f, canvas.draws.last { it.text == "space" }.textSize, .1f)
        val hint = canvas.draws.last { it.text == "←↓↑→" }
        assertEquals(10f, hint.textSize, .1f)
        assertEquals((255 * .7f).toInt(), hint.alpha)
    }

    @Test fun `paste uses the same selected label composition in every nonconverting layer`() {
        Settings.Global.putFloat(view.context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        listOf(KeyboardMode.QWERTY, KeyboardMode.SYMBOLS, KeyboardMode.KANA, KeyboardMode.NUMBERS, KeyboardMode.CURSOR).forEach { mode ->
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
            fun modeTouch(action: Int, y: Float, time: Long) = MotionEvent.obtain(0, time, action, startX, y, 0).also {
                modeView.onTouchEvent(it); it.recycle()
            }
            modeTouch(MotionEvent.ACTION_DOWN, startY, 0)
            modeTouch(MotionEvent.ACTION_MOVE, startY + 24f * modeView.resources.displayMetrics.density, 10)
            val selectedDirections = KeyboardView::class.java.getDeclaredField("directions").also { it.isAccessible = true }
                .get(modeView) as Map<*, *>
            assertEquals("$mode selected direction", Direction.DOWN, selectedDirections[0])
            val canvas = CaptureCanvas(Bitmap.createBitmap(400, 228, Bitmap.Config.ARGB_8888)).also(modeView::draw)
            val paste = canvas.draws.lastOrNull { it.text == "paste" } ?: error("$mode did not draw paste")
            val main = canvas.draws.lastOrNull { it.text == "Enter" } ?: error("$mode did not draw Enter")
            assertEquals("$mode paste size", 17f, paste.textSize, .2f)
            assertEquals("$mode Enter hidden", 0, main.alpha)
            modeTouch(MotionEvent.ACTION_UP, startY + 24f * modeView.resources.displayMetrics.density, 20)
            assertEquals("$mode action", listOf(KeyAction.Paste), modeActions)
        }
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
        assertEquals(listOf(KeyAction.KanaInput("あ"), KeyAction.CommitConversion), actions)
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

    @Test fun `inner width uses css outer inset and taller kana keys`() {
        view.setMode(KeyboardMode.KANA)
        view.measure(exact(840), exact(256))
        view.layout(0, 0, 840, 256)
        val provider = view.accessibilityNodeProvider
        fun bounds(id: Int) = Rect().also { provider.createAccessibilityNodeInfo(id)!!.getBoundsInParent(it) }
        val first = bounds(0)
        assertEquals(13, first.left)
        assertEquals(52, first.height())
        assertEquals(6, bounds(5).top - first.bottom)
        assertEquals(6, bounds(1).left - first.right)
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

    private fun exact(size: Int) = android.view.View.MeasureSpec.makeMeasureSpec(size, android.view.View.MeasureSpec.EXACTLY)
}
