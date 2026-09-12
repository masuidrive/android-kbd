package com.masuidrive.gestureime

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.emoji2.emojipicker.EmojiPickerView
import com.masuidrive.gestureime.keyboard.KeyboardHeightPreset
import com.masuidrive.gestureime.keyboard.KeyAction
import com.masuidrive.gestureime.keyboard.KeyboardMode
import com.masuidrive.gestureime.keyboard.KeyboardView
import com.masuidrive.gestureime.ui.CandidateStripView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ImeHideBarTest {
    @Test
    fun emojiLayerReplacesCandidateSlotWithThePickerAndLeavesOnlyTheControlRowInKeyboardView() {
        val service = Robolectric.buildService(HidingImeService::class.java).create().get()
        val root = service.onCreateInputView() as FrameLayout
        val content = root.getChildAt(0) as LinearLayout
        val candidate = content.getChildAt(0) as CandidateStripView
        val keyboard = content.getChildAt(1) as KeyboardView
        val picker = root.getChildAt(1) as EmojiPickerView

        service.onKeyAction(KeyAction.SwitchLayer(KeyboardMode.EMOJI))
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        keyboard.measure(exact(412), exact(1_000)); keyboard.layout(0, 0, 412, keyboard.measuredHeight)

        assertEquals(View.INVISIBLE, candidate.visibility)
        assertEquals(View.VISIBLE, picker.visibility)
        assertEquals(2, keyboard.accessibilityNodeProvider.createAccessibilityNodeInfo(-1)!!.childCount)
    }

    @Test
    fun fourRowsAndCandidateStripKeepTheirHeightsWhileHideBarOwnsBottomInset() {
        val service = Robolectric.buildService(HidingImeService::class.java).create().get()
        val root = service.onCreateInputView() as FrameLayout
        val content = root.getChildAt(0) as LinearLayout
        val candidate = content.getChildAt(0) as CandidateStripView
        val keyboard = content.getChildAt(1) as KeyboardView
        val hideBar = content.getChildAt(2) as FrameLayout
        val picker = root.getChildAt(1) as EmojiPickerView
        val density = service.resources.displayMetrics.density
        val expectedCandidateHeight = (50 * density).toInt()
        val expectedHideHeight = (28 * density).toInt()

        assertEquals(2, root.childCount)
        assertEquals(3, content.childCount)
        assertSame(candidate, content.getChildAt(0))
        assertSame(keyboard, content.getChildAt(1))
        assertSame(hideBar, content.getChildAt(2))
        assertEquals(View.GONE, picker.visibility)
        assertEquals(expectedCandidateHeight, candidate.layoutParams.height)
        listOf(412, 840).forEach { width ->
            keyboard.measure(exact(width), exact(1_000))
            assertEquals((KeyboardHeightPreset.STANDARD.rowPitchDp * 4 + 8).toInt(), keyboard.measuredHeight)
        }

        val firstInset = WindowInsetsCompat.Builder()
            .setInsets(WindowInsetsCompat.Type.systemBars(), Insets.of(0, 0, 0, 31))
            .setInsets(WindowInsetsCompat.Type.displayCutout(), Insets.of(0, 0, 0, 12))
            .build()
        val secondInset = WindowInsetsCompat.Builder()
            .setInsets(WindowInsetsCompat.Type.systemBars(), Insets.of(0, 0, 0, 17))
            .setInsets(WindowInsetsCompat.Type.displayCutout(), Insets.of(0, 0, 0, 40))
            .build()
        val noInset = WindowInsetsCompat.Builder().build()

        ViewCompat.dispatchApplyWindowInsets(root, firstInset)
        assertHideBarInset(hideBar, keyboard, expectedHideHeight, 31)
        ViewCompat.dispatchApplyWindowInsets(root, firstInset)
        assertHideBarInset(hideBar, keyboard, expectedHideHeight, 31)
        ViewCompat.dispatchApplyWindowInsets(root, secondInset)
        assertHideBarInset(hideBar, keyboard, expectedHideHeight, 40)
        ViewCompat.dispatchApplyWindowInsets(root, noInset)
        assertHideBarInset(hideBar, keyboard, expectedHideHeight, 0)
    }

    private fun assertHideBarInset(
        hideBar: FrameLayout,
        keyboard: KeyboardView,
        hideBarHeight: Int,
        bottomInset: Int,
    ) {
        assertEquals(0, keyboard.paddingBottom)
        assertEquals(bottomInset, hideBar.paddingBottom)
        assertEquals(hideBarHeight + bottomInset, hideBar.layoutParams.height)
        assertEquals((KeyboardHeightPreset.STANDARD.rowPitchDp * 4 + 8).toInt(), keyboard.measuredHeight)
    }

    @Test
    fun centeredChevronRequestsImeHide() {
        val service = Robolectric.buildService(HidingImeService::class.java).create().get()
        val hideButton = service.onCreateInputView().findViewById<ImageButton>(R.id.ime_hide_button)

        assertEquals("キーボードを閉じる", hideButton.contentDescription)
        assertEquals(ImageView.ScaleType.CENTER, hideButton.scaleType)
        assertTrue(hideButton.performClick())
        assertEquals(0, service.hideFlags)
    }

    private fun exact(size: Int) = View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY)

    class HidingImeService : ImeService() {
        var hideFlags: Int? = null

        override fun requestHideSelf(flags: Int) {
            hideFlags = flags
        }
    }
}
