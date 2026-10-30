package com.masuidrive.gestureime.ui

import android.view.View
import android.view.ViewGroup
import android.view.MotionEvent
import android.app.Activity
import android.widget.TextView
import com.masuidrive.gestureime.keyboard.KeyAction
import com.masuidrive.gestureime.keyboard.Direction
import com.masuidrive.gestureime.keyboard.Modifier
import com.masuidrive.gestureime.keyboard.KeyboardPopupController
import com.masuidrive.gestureime.keyboard.KeyboardPopupRenderView
import com.masuidrive.gestureime.keyboard.KeyboardMode
import com.masuidrive.gestureime.keyboard.PopupKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Robolectric
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PersonalInfoPickerViewTest {
    @Test fun moreButtonAppearsInFooterAndDoesNotChangePickerHeight() {
        val view = PersonalInfoPickerView(RuntimeEnvironment.getApplication())
        Robolectric.buildActivity(Activity::class.java).setup().get().setContentView(view)
        val actions = mutableListOf<KeyAction>()
        view.setOnEditAction(actions::add)
        val width = 412
        val height = 210
        fun measure(): Int {
            view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
            view.layout(0, 0, width, view.measuredHeight)
            return view.measuredHeight
        }
        val initialHeight = measure()
        val more = view.findText("more..")!!
        assertEquals(View.INVISIBLE, more.visibility)
        view.setMoreVisible(true)
        assertEquals(initialHeight, measure())
        more.performClick()
        assertEquals(listOf(KeyAction.ExpandPersonalInfo), actions)
        view.setMoreVisible(false)
        assertEquals(initialHeight, measure())
        assertEquals(View.INVISIBLE, more.visibility)
    }

    @Test fun footerUsesKeyboardInsetsDoubleSpaceAndStableSlotsAtPhoneAndTabletWidths() {
        for (width in listOf(412, 840)) {
            val view = PersonalInfoPickerView(RuntimeEnvironment.getApplication())
            Robolectric.buildActivity(Activity::class.java).setup().get().setContentView(view)
            view.showCandidates(CandidateUiSnapshot(1, listOf("a@example.com")))
            view.setMoreVisible(true)
            view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(210, View.MeasureSpec.EXACTLY))
            view.layout(0, 0, width, 210)
            val layer = view.findText("Az")!!
            val more = view.findText("more..")!!
            val space = view.findText("Space")!!
            val backspace = view.findText("⌫")!!
            val enter = view.findText("Enter")!!
            val inset = if (width >= 600) 10 else 3
            assertEquals(inset + 3, layer.left)
            assertEquals(width - inset - 3, enter.right)
            assertEquals(6, more.left - layer.right)
            assertEquals(6, backspace.left - space.right)
            assertTrue(space.width >= backspace.width * 2 - 6)
            val normalLeft = layer.left
            val normalRight = enter.right
            view.setMoreVisible(false)
            view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(210, View.MeasureSpec.EXACTLY))
            view.layout(0, 0, width, 210)
            assertEquals(normalLeft, layer.left)
            assertEquals(normalRight, enter.right)
            assertEquals(210, view.height)
        }
    }

    @Test fun attachedPickerRoutesFlicksAndUnassignedDirectionsThroughRealTouchPath() {
        val view = PersonalInfoPickerView(RuntimeEnvironment.getApplication())
        Robolectric.buildActivity(Activity::class.java).setup().get().setContentView(view)
        view.setMoreVisible(true)
        view.measure(View.MeasureSpec.makeMeasureSpec(412, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(210, View.MeasureSpec.EXACTLY))
        view.layout(0, 0, 412, 210)
        val actions = mutableListOf<KeyAction>()
        view.setOnEditAction(actions::add)
        fun gesture(label: String, dx: Float, dy: Float) {
            val key = view.findText(label)!!
            val footer = key.parent as ViewGroup
            val x = (footer.left + key.left + key.width / 2).toFloat()
            val y = (footer.top + key.top + key.height / 2).toFloat()
            view.dispatchTouchEvent(MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, x, y, 0))
            view.dispatchTouchEvent(MotionEvent.obtain(0, 50, MotionEvent.ACTION_MOVE, x + dx, y + dy, 0))
            view.dispatchTouchEvent(MotionEvent.obtain(0, 100, MotionEvent.ACTION_UP, x + dx, y + dy, 0))
        }
        gesture("Az", 0f, -30f)
        gesture("、。？！", -30f, 0f)
        gesture("Space", 30f, 0f)
        gesture("⌫", 30f, 0f)
        gesture("Enter", 0f, -30f)
        gesture("more..", 30f, 0f)
        assertEquals(KeyAction.SwitchLayer(com.masuidrive.gestureime.keyboard.KeyboardMode.KANA), actions[0])
        assertEquals(KeyAction.CommitText("。"), actions[1])
        assertEquals(Direction.RIGHT, (actions[2] as KeyAction.MoveCursor).direction)
        assertEquals(KeyAction.Backspace(), actions[3])
        assertEquals(KeyAction.Paste, actions[4])
        assertEquals(KeyAction.ExpandPersonalInfo, actions[5])
    }

    @Test fun layerKeySwitchesOnTapAndFlickFromThePicker() {
        val view = PersonalInfoPickerView(RuntimeEnvironment.getApplication())
        Robolectric.buildActivity(Activity::class.java).setup().get().setContentView(view)
        val actions = mutableListOf<KeyAction>()
        view.setOnEditAction(actions::add)
        view.setLayerMode(KeyboardMode.QWERTY)
        val layer = view.findText("Az")!!
        layer.performClick()
        val density = view.resources.displayMetrics.density
        layer.dispatchTouchEvent(MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 30f, 30f, 0))
        layer.dispatchTouchEvent(MotionEvent.obtain(0, 50, MotionEvent.ACTION_UP, 30f + 30f * density, 30f, 0))
        assertEquals(listOf(KeyAction.SwitchLayer(KeyboardMode.QWERTY),
            KeyAction.SwitchLayer(KeyboardMode.QWERTY)), actions)

        view.setLayerMode(KeyboardMode.SYMBOLS)
        assertEquals("?}", layer.text.toString())
        layer.performClick()
        view.setLayerMode(KeyboardMode.KANA)
        assertEquals("あん", layer.text.toString())
        layer.performClick()
        view.setLayerMode(KeyboardMode.NUMBERS)
        assertEquals("19", layer.text.toString())
        layer.performClick()
        assertEquals(listOf(
            KeyAction.SwitchLayer(KeyboardMode.QWERTY),
            KeyAction.SwitchLayer(KeyboardMode.QWERTY),
            KeyAction.SwitchLayer(KeyboardMode.SYMBOLS),
            KeyAction.SwitchLayer(KeyboardMode.KANA),
            KeyAction.SwitchLayer(KeyboardMode.NUMBERS),
        ), actions)
        fun flick(dx: Float, dy: Float) {
            layer.dispatchTouchEvent(MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 30f, 30f, 0))
            layer.dispatchTouchEvent(MotionEvent.obtain(0, 50, MotionEvent.ACTION_UP,
                30f + dx * density, 30f + dy * density, 0))
        }
        flick(-30f, 0f)
        flick(0f, -30f)
        flick(0f, 30f)
        assertEquals(listOf(KeyAction.VoiceHold,
            KeyAction.SwitchLayer(KeyboardMode.KANA),
            KeyAction.SwitchLayer(KeyboardMode.NUMBERS)), actions.takeLast(3))
    }

    @Test fun layerKeyShowsSelectedFlickAndDismissesOnRelease() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val view = PersonalInfoPickerView(activity)
        activity.setContentView(view)
        view.setLayerMode(KeyboardMode.NUMBERS)
        view.measure(View.MeasureSpec.makeMeasureSpec(412, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(210, View.MeasureSpec.EXACTLY))
        view.layout(0, 0, 412, 210)
        val key = view.findText("19")!!
        val popup = PersonalInfoPickerView::class.java.getDeclaredField("punctuationPopupController")
            .apply { isAccessible = true }.get(view) as KeyboardPopupController
        val x = key.width / 2f
        val y = key.height / 2f
        key.dispatchTouchEvent(MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, x, y, 0))
        assertTrue(popup.isShowingForTest())
        val renderer = popup.popupForTest()!!.contentView as KeyboardPopupRenderView
        assertEquals(PopupKind.LETTER, renderer.kindForTest())
        key.dispatchTouchEvent(MotionEvent.obtain(0, 25, MotionEvent.ACTION_MOVE, x, y - 40f, 0))
        assertEquals(Direction.UP, renderer.selectedDirectionForTest())
        key.dispatchTouchEvent(MotionEvent.obtain(0, 50, MotionEvent.ACTION_UP, x, y - 40f, 0))
        assertTrue(!popup.isShowingForTest())
    }

    @Test fun layerKeyCenterReturnAndCancelDoNotChooseAStaleFlick() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val view = PersonalInfoPickerView(activity)
        activity.setContentView(view)
        view.setLayerMode(KeyboardMode.QWERTY)
        val actions = mutableListOf<KeyAction>()
        view.setOnEditAction(actions::add)
        val key = view.findText("Az")!!
        val popup = PersonalInfoPickerView::class.java.getDeclaredField("punctuationPopupController")
            .apply { isAccessible = true }.get(view) as KeyboardPopupController
        val x = key.width / 2f
        val y = key.height / 2f
        val distance = 35f * view.resources.displayMetrics.density
        key.dispatchTouchEvent(MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, x, y, 0))
        key.dispatchTouchEvent(MotionEvent.obtain(0, 20, MotionEvent.ACTION_MOVE, x, y - distance, 0))
        key.dispatchTouchEvent(MotionEvent.obtain(0, 40, MotionEvent.ACTION_MOVE, x, y, 0))
        key.dispatchTouchEvent(MotionEvent.obtain(0, 60, MotionEvent.ACTION_UP, x, y, 0))
        assertEquals(listOf(KeyAction.SwitchLayer(KeyboardMode.QWERTY)), actions)
        assertTrue(!popup.isShowingForTest())

        key.dispatchTouchEvent(MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, x, y, 0))
        key.dispatchTouchEvent(MotionEvent.obtain(0, 20, MotionEvent.ACTION_MOVE, x, y - distance, 0))
        key.dispatchTouchEvent(MotionEvent.obtain(0, 40, MotionEvent.ACTION_CANCEL, x, y - distance, 0))
        assertEquals(1, actions.size)
        assertTrue(!popup.isShowingForTest())
    }

    @Test fun shortVerticalSpaceFlickMovesOnceInsteadOfTypingSpace() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val view = PersonalInfoPickerView(activity)
        activity.setContentView(view)
        val actions = mutableListOf<KeyAction>()
        view.setOnEditAction(actions::add)
        val key = view.findText("Space")!!
        val x = key.width / 2f
        val y = key.height / 2f
        val density = view.resources.displayMetrics.density
        fun gesture(dyDp: Float, cancel: Boolean = false) {
            key.dispatchTouchEvent(MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, x, y, 0))
            key.dispatchTouchEvent(MotionEvent.obtain(0, 20, MotionEvent.ACTION_MOVE, x, y + dyDp * density, 0))
            key.dispatchTouchEvent(MotionEvent.obtain(0, 40,
                if (cancel) MotionEvent.ACTION_CANCEL else MotionEvent.ACTION_UP,
                x, y + dyDp * density, 0))
        }
        gesture(-15f)
        gesture(15f)
        gesture(0f)
        gesture(-15f, cancel = true)
        assertEquals(listOf(KeyAction.MoveCursor(Direction.UP), KeyAction.MoveCursor(Direction.DOWN),
            KeyAction.CommitText(" ")), actions)
    }
    @Test fun punctuationKeyDispatchesTapAndAllFourFlicks() {
        val view = PersonalInfoPickerView(RuntimeEnvironment.getApplication())
        Robolectric.buildActivity(Activity::class.java).setup().get().setContentView(view)
        val actions = mutableListOf<KeyAction>()
        view.setOnEditAction(actions::add)
        val punctuation = view.findText("、。？！")!!
        fun gesture(dx: Float, dy: Float) {
            punctuation.dispatchTouchEvent(MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 50f, 50f, 0))
            punctuation.dispatchTouchEvent(MotionEvent.obtain(0, 50, MotionEvent.ACTION_UP, 50f + dx, 50f + dy, 0))
        }
        gesture(0f, 0f)
        gesture(-50f, 0f)
        gesture(0f, -50f)
        gesture(50f, 0f)
        gesture(0f, 50f)
        assertEquals(listOf("、", "。", "？", "！", "、").map(KeyAction::CommitText), actions)
    }

    @Test fun punctuationFlickShowsTheVoiceCrossAndDismissesOnReleaseOrCancel() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val view = PersonalInfoPickerView(activity)
        activity.setContentView(view)
        view.measure(View.MeasureSpec.makeMeasureSpec(412, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(210, View.MeasureSpec.EXACTLY))
        view.layout(0, 0, 412, 210)
        val key = view.findText("、。？！")!!
        val popup = PersonalInfoPickerView::class.java.getDeclaredField("punctuationPopupController")
            .apply { isAccessible = true }.get(view) as KeyboardPopupController
        val x = key.width / 2f
        val y = key.height / 2f

        key.dispatchTouchEvent(MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, x, y, 0))
        assertTrue(popup.isShowingForTest())
        val renderer = popup.popupForTest()!!.contentView as KeyboardPopupRenderView
        assertEquals(PopupKind.KANA, renderer.kindForTest())
        assertEquals(mapOf(Direction.CENTER to "、", Direction.LEFT to "。", Direction.UP to "？",
            Direction.RIGHT to "！", Direction.DOWN to "、"), renderer.tileLabelsForTest())
        key.dispatchTouchEvent(MotionEvent.obtain(0, 25, MotionEvent.ACTION_MOVE, x - 50f, y, 0))
        assertEquals(Direction.LEFT, renderer.selectedDirectionForTest())
        key.dispatchTouchEvent(MotionEvent.obtain(0, 50, MotionEvent.ACTION_UP, x - 50f, y, 0))
        assertTrue(!popup.isShowingForTest())

        key.dispatchTouchEvent(MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, x, y, 0))
        assertTrue(popup.isShowingForTest())
        key.dispatchTouchEvent(MotionEvent.obtain(0, 50, MotionEvent.ACTION_CANCEL, x, y, 0))
        assertTrue(!popup.isShowingForTest())

        key.dispatchTouchEvent(MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, x, y, 0))
        assertTrue(popup.isShowingForTest())
        activity.setContentView(View(activity))
        assertTrue(!popup.isShowingForTest())
    }

    @Test fun bottomRowEditsWithoutACancelKey() {
        val view = PersonalInfoPickerView(RuntimeEnvironment.getApplication())
        val actions = mutableListOf<KeyAction>()
        view.setOnEditAction(actions::add)
        view.findText("Space")!!.performClick()
        view.findText("⌫")!!.performClick()
        view.findText("Enter")!!.performClick()
        assertEquals(listOf(KeyAction.CommitText(" "), KeyAction.Backspace(), KeyAction.Enter), actions)
        assertEquals(null, view.findText("キャンセル"))
    }

    @Test fun bottomRowSpaceAndEnterFlicksMatchVoiceKeys() {
        val view = PersonalInfoPickerView(RuntimeEnvironment.getApplication())
        Robolectric.buildActivity(Activity::class.java).setup().get().setContentView(view)
        val actions = mutableListOf<KeyAction>()
        view.setOnEditAction(actions::add)
        fun gesture(key: String, dx: Float, dy: Float) {
            val target = view.findText(key)!!
            target.dispatchTouchEvent(MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 50f, 50f, 0))
            target.dispatchTouchEvent(MotionEvent.obtain(0, 50, MotionEvent.ACTION_UP, 50f + dx, 50f + dy, 0))
        }
        gesture("Space", -50f, 0f)
        gesture("Space", 0f, -50f)
        gesture("Space", 50f, 0f)
        gesture("Space", 0f, 50f)
        gesture("Enter", 0f, -50f)
        gesture("Enter", 0f, 50f)
        assertEquals(listOf(Direction.LEFT, Direction.UP, Direction.RIGHT, Direction.DOWN),
            actions.filterIsInstance<KeyAction.MoveCursor>().map { it.direction })
        assertEquals(listOf(KeyAction.Paste, KeyAction.ModifiedKey("j", Modifier.CTRL)), actions.takeLast(2))
    }

    @Test fun footerThresholdReturnUnassignedAndCancellationFollowKeyboardGestures() {
        val view = PersonalInfoPickerView(RuntimeEnvironment.getApplication())
        Robolectric.buildActivity(Activity::class.java).setup().get().setContentView(view)
        val actions = mutableListOf<KeyAction>()
        view.setOnEditAction(actions::add)
        val enter = view.findText("Enter")!!
        val density = view.resources.displayMetrics.density
        fun gesture(vararg points: Pair<Int, Float>, cancel: Boolean = false) {
            enter.dispatchTouchEvent(MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 50f, 50f, 0))
            points.forEach { (time, yDp) ->
                enter.dispatchTouchEvent(MotionEvent.obtain(0, time.toLong(), MotionEvent.ACTION_MOVE,
                    50f, 50f + yDp * density, 0))
            }
            val last = points.lastOrNull()?.second ?: 0f
            enter.dispatchTouchEvent(MotionEvent.obtain(0, 100,
                if (cancel) MotionEvent.ACTION_CANCEL else MotionEvent.ACTION_UP,
                50f, 50f + last * density, 0))
        }
        gesture(20 to -17f) // Below the shared 18dp selection threshold.
        gesture(20 to -25f, 40 to -5f) // Return to center cancels the flick.
        gesture(20 to -25f, cancel = true)
        enter.dispatchTouchEvent(MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 50f, 50f, 0))
        enter.dispatchTouchEvent(MotionEvent.obtain(0, 100, MotionEvent.ACTION_UP,
            50f + 40f * density, 50f, 0)) // Unassigned right direction is a tap.
        gesture(20 to -25f)
        assertEquals(listOf(KeyAction.Enter, KeyAction.Enter, KeyAction.Enter, KeyAction.Paste), actions)
    }

    @Test fun footerPitchUpdatesOnlyWhenKeyboardPitchChanges() {
        val view = PersonalInfoPickerView(RuntimeEnvironment.getApplication())
        val footer = view.findText("Enter")!!.parent as ViewGroup
        val pitch = 72
        view.setFooterRowPitch(pitch)
        val params = footer.layoutParams
        val keyParams = view.findText("Enter")!!.layoutParams
        view.setFooterRowPitch(pitch)
        assertEquals(pitch, footer.layoutParams.height)
        assertEquals(params, footer.layoutParams)
        assertEquals(keyParams, view.findText("Enter")!!.layoutParams)
    }

    @Test fun displaysOnlyValuesAndDispatchesTheRawCandidateIndex() {
        val view = PersonalInfoPickerView(RuntimeEnvironment.getApplication())
        val events = mutableListOf<CandidateUiEvent>()
        view.setOnCandidateSelected(events::add)
        view.showCandidates(CandidateUiSnapshot(
            token = 51,
            candidates = listOf("山田 太郎", "東京都千代田区"),
            labels = listOf("氏名: 山田 太郎", "住所: 東京都千代田区"),
        ))

        view.findText("東京都千代田区")!!.performClick()

        assertEquals(listOf(CandidateUiEvent(51, 1)), events)
        assertEquals(null, view.findText("住所: 東京都千代田区"))
        assertEquals(null, view.findText("戻る"))
    }

    @Test fun valuesWrapWithinNarrowWidthAndKeepThePickerHeight() {
        val view = PersonalInfoPickerView(RuntimeEnvironment.getApplication())
        view.showCandidates(CandidateUiSnapshot(53, listOf("山田 太郎", "name@example.com", "東京都千代田区", "いつもありがとうございます。")))
        val width = 240
        val height = 210
        view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
        view.layout(0, 0, width, height)

        val first = view.findText("山田 太郎")!!
        val last = view.findText("いつもありがとうございます。")!!
        assertEquals(height, view.height)
        assertTrue(last.top > first.top)
        assertTrue(last.right <= width)
    }

    private fun View.findText(value: String): TextView? {
        if (this is TextView && text.toString() == value) return this
        if (this is ViewGroup) repeat(childCount) { getChildAt(it).findText(value)?.let { return it } }
        return null
    }
}
