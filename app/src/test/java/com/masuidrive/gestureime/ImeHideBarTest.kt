package com.masuidrive.gestureime

import android.app.Activity
import android.graphics.Rect
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.view.inputmethod.EditorInfo
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.emoji2.emojipicker.EmojiPickerView
import androidx.recyclerview.widget.RecyclerView
import com.masuidrive.gestureime.keyboard.KeyboardHeightPreset
import com.masuidrive.gestureime.keyboard.KeyAction
import com.masuidrive.gestureime.keyboard.KeyboardMode
import com.masuidrive.gestureime.keyboard.KeyboardView
import com.masuidrive.gestureime.ui.CandidateStripView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
        val mask = root.getChildAt(3)

        service.onKeyAction(KeyAction.SwitchLayer(KeyboardMode.EMOJI))
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        keyboard.measure(exact(412), exact(1_000)); keyboard.layout(0, 0, 412, keyboard.measuredHeight)

        assertEquals(View.INVISIBLE, candidate.visibility)
        assertEquals(View.VISIBLE, picker.visibility)
        assertEquals(View.VISIBLE, mask.visibility)
        assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_NO, mask.importantForAccessibility)
        assertEquals(2, keyboard.accessibilityNodeProvider.createAccessibilityNodeInfo(-1)!!.childCount)

        KeyboardHeightPreset.entries.forEach { preset ->
            keyboard.setHeightPreset(preset)
            root.measure(exact(412), exact(1_000)); root.layout(0, 0, 412, 1_000)
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            root.measure(exact(412), exact(1_000)); root.layout(0, 0, 412, 1_000)
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            root.measure(exact(412), exact(1_000)); root.layout(0, 0, 412, 1_000)
            val expectedControlTop = (50 * service.resources.displayMetrics.density).toInt() +
                (8 * service.resources.displayMetrics.density).toInt() +
                (preset.rowPitchDp * service.resources.displayMetrics.density * 3).toInt()
            assertEquals(0, mask.height)
            assertEquals(expectedControlTop, mask.top)
            assertEquals(expectedControlTop, picker.layoutParams.height)
        }
    }

    @Test
    fun freshPickerEndsAtTheKeyboardControlTopWithOnlyInternalOverscan() {
        val context = Robolectric.buildService(HidingImeService::class.java).create().get()
        val originalPreset = ImePreferences.getKeyboardHeightPreset(context)
        try {
            KeyboardHeightPreset.entries.forEach { preset ->
                ImePreferences.setKeyboardHeightPreset(context, preset)
                val service = Robolectric.buildService(HidingImeService::class.java).create().get()
                val root = service.onCreateInputView() as FrameLayout
                val content = root.getChildAt(0) as LinearLayout
                val keyboard = content.getChildAt(1) as KeyboardView
                val picker = root.getChildAt(1) as EmojiPickerView
                val mask = root.getChildAt(3)
                service.onKeyAction(KeyAction.SwitchLayer(KeyboardMode.EMOJI))
                root.measure(exact(412), exact(1_000)); root.layout(0, 0, 412, 1_000)
                Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
                root.measure(exact(412), exact(1_000)); root.layout(0, 0, 412, 1_000)

                val density = service.resources.displayMetrics.density
                val overlayHeight = (8 * density).toInt() + (preset.rowPitchDp * density * 3).toInt()
                val controlTop = keyboard.top + overlayHeight
                assertEquals(controlTop, picker.bottom)
                assertEquals(controlTop, mask.top + mask.height)
            }
        } finally {
            ImePreferences.setKeyboardHeightPreset(context, originalPreset)
        }
    }

    @Test
    fun privateEditorSwitchesToItsDedicatedEmptyPickerWithoutReusingPublicPicker() {
        val service = Robolectric.buildService(HidingImeService::class.java).create().get()
        val root = service.onCreateInputView() as FrameLayout
        val publicPicker = root.getChildAt(1) as EmojiPickerView
        val privatePicker = root.getChildAt(2) as EmojiPickerView
        val mask = root.getChildAt(3)

        service.onKeyAction(KeyAction.SwitchLayer(KeyboardMode.EMOJI))
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        @Suppress("UNCHECKED_CAST")
        val viewports = ImeService::class.java.getDeclaredField("pickerViewportHeights").apply { isAccessible = true }
            .get(service) as MutableMap<EmojiPickerView, Int>
        viewports[publicPicker] = 100
        viewports[privatePicker] = 120
        service.onStartInput(EditorInfo().apply { inputType = InputType.TYPE_CLASS_TEXT }, false)
        assertEquals((50 * service.resources.displayMetrics.density).toInt() + 100,
            (mask.layoutParams as FrameLayout.LayoutParams).topMargin)
        assertEquals(View.VISIBLE, publicPicker.visibility)
        assertEquals(View.GONE, privatePicker.visibility)
        assertEquals(View.VISIBLE, mask.visibility)
        assertEquals(publicPicker.bottom, mask.top + mask.height)

        service.onStartInput(EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }, false)
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

        assertEquals(View.GONE, publicPicker.visibility)
        assertEquals(View.VISIBLE, privatePicker.visibility)
        assertEquals(View.VISIBLE, mask.visibility)
        assertEquals((50 * service.resources.displayMetrics.density).toInt() + 120,
            (mask.layoutParams as FrameLayout.LayoutParams).topMargin)
        assertEquals(privatePicker.bottom, mask.top + mask.height)

        // A second editor switch must synchronously restore the public instance;
        // the private provider is never swapped onto the public picker.
        service.onStartInput(EditorInfo().apply { inputType = InputType.TYPE_CLASS_TEXT }, false)
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

        assertEquals(View.VISIBLE, publicPicker.visibility)
        assertEquals(View.GONE, privatePicker.visibility)
        assertEquals(View.VISIBLE, mask.visibility)
        assertEquals(publicPicker.bottom, mask.top + mask.height)
    }

    @Test
    fun pickerGeometryRefreshSettlesAfterAnExternalWidthChange() {
        val service = Robolectric.buildService(HidingImeService::class.java).create().get()
        val root = service.onCreateInputView() as FrameLayout
        val picker = root.getChildAt(1) as EmojiPickerView
        var layoutChanges = 0
        picker.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> layoutChanges++ }
        service.onKeyAction(KeyAction.SwitchLayer(KeyboardMode.EMOJI))
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

        root.measure(exact(412), exact(1_000)); root.layout(0, 0, 412, 1_000)
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        assertTrue(picker.findViewById<View>(androidx.emoji2.emojipicker.R.id.emoji_picker_body) != null)
        root.measure(exact(840), exact(1_000)); root.layout(0, 0, 840, 1_000)
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

        assertEquals(840, picker.width)
        assertTrue(layoutChanges > 0)
        assertTrue(!picker.isLayoutRequested)
        val settledChanges = layoutChanges
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        assertEquals(settledChanges, layoutChanges)
        assertTrue(!picker.isLayoutRequested)
    }

    @Test
    fun initialPickerLayoutPostDoesNotRebuildBeforeTheAsyncContentExists() {
        val service = Robolectric.buildService(HidingImeService::class.java).create().get()
        val root = service.onCreateInputView() as FrameLayout
        val picker = root.getChildAt(1) as EmojiPickerView

        // The loader has not attached a header/body at construction.  Laying the fresh
        // picker out and flushing its post queue must remain safe while it completes.
        assertEquals(0, picker.childCount)
        service.onKeyAction(KeyAction.SwitchLayer(KeyboardMode.EMOJI))
        root.measure(exact(412), exact(1_000)); root.layout(0, 0, 412, 1_000)
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

        assertEquals(412, picker.width)
    }

    @Test
    fun pickerBodyShowsOnlyThreeFullTouchRowsAndHeaderKeepsTen48dpCategories() {
        val service = Robolectric.buildService(HidingImeService::class.java).create().get()
        val root = service.onCreateInputView() as FrameLayout
        val content = root.getChildAt(0) as LinearLayout
        val keyboard = content.getChildAt(1) as KeyboardView
        val picker = root.getChildAt(1) as EmojiPickerView
        service.onKeyAction(KeyAction.SwitchLayer(KeyboardMode.EMOJI))

        KeyboardHeightPreset.entries.forEach { preset ->
            keyboard.setHeightPreset(preset)
            root.measure(exact(412), exact(1_000)); root.layout(0, 0, 412, 1_000)
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            // The bundled loader has attached the body. An external width change is the
            // production path that rebuilds AndroidX's cached adapter geometry.
            root.measure(exact(413), exact(1_000)); root.layout(0, 0, 413, 1_000)
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            root.measure(exact(412), exact(1_000)); root.layout(0, 0, 412, 1_000)

            val header = picker.findViewById<RecyclerView>(androidx.emoji2.emojipicker.R.id.emoji_picker_header)
            val body = picker.findViewById<RecyclerView>(androidx.emoji2.emojipicker.R.id.emoji_picker_body)
            val density = service.resources.displayMetrics.density
            val expectedViewport = (8 * density).toInt() + (preset.rowPitchDp * density * 3).toInt()
            val measurementSpacer = (8 * density).toInt()
            assertEquals(10, header.adapter!!.itemCount)
            val headerWidths = (0 until header.childCount).map { header.getChildAt(it).width }
            assertTrue("header widths=$headerWidths", headerWidths.filter { it > 0 }.all { it >= (48 * density).toInt() })
            // AndroidX cells are measured from the preset maximum plus one spacer. Category
            // viewport changes must only alter the clip/mask, never this physical body height.
            assertEquals(expectedViewport + measurementSpacer, body.height)
            assertEquals(expectedViewport, body.clipBounds!!.bottom)
            // BodyAdapter calculates a cell from (measured body - two spacers) / 3. The
            // measured one-spacer overscan makes this exactly the preset pitch; the actual
            // RecyclerView child is shrunk and clips touch/drawing/A11y at three rows.
            val rowPitch = (preset.rowPitchDp * density).toInt()
            assertEquals(3, (expectedViewport - (8 * density).toInt()) / rowPitch)
            assertTrue(rowPitch >= (48 * density).toInt())
            assertTrue(body.clipChildren && body.clipToPadding)
            assertTrue(body.adapter != null)
        }
    }

    @Test
    fun attachedEmojiRowsDefineTheExactThreeRowViewport() {
        val bounds = listOf(
            Rect(0, 8, 50, 58), Rect(50, 8, 100, 58),
            Rect(0, 58, 50, 108), Rect(50, 58, 100, 108),
            Rect(0, 108, 50, 158), Rect(50, 108, 100, 158),
            Rect(0, 158, 50, 208),
        )
        assertEquals(158, thirdEmojiRowBottom(bounds))
        assertEquals(158, resolveEmojiViewport(158, 150))
    }

    @Test
    fun roundedCategoryStartLocksButAnEmptyRecentPlaceholderDoesNot() {
        fun rowsAt(firstTop: Int) = listOf(
            Rect(0, firstTop, 50, firstTop + 50),
            Rect(0, firstTop + 50, 50, firstTop + 100),
            Rect(0, firstTop + 100, 50, firstTop + 150),
        )

        // An 8px spacer can round to 9px at the start of a normal category.
        assertEquals(159, thirdEmojiRowBottomAtCategoryStart(rowsAt(9), 8))
        // The empty-Recent placeholder puts the next category far below that tolerance.
        assertNull(thirdEmojiRowBottomAtCategoryStart(rowsAt(71), 8))
    }

    @Test
    fun emptyRecentPlaceholderIsRecognizedOnlyWhileItIntersectsTheViewport() {
        val viewport = Rect(0, 0, 412, 173)
        assertTrue(isEmojiPlaceholderInViewport(View.VISIBLE, Rect(0, 8, 412, 58), viewport))
        assertFalse(isEmojiPlaceholderInViewport(View.GONE, Rect(0, 8, 412, 58), viewport))
        assertFalse(isEmojiPlaceholderInViewport(View.VISIBLE, Rect(0, 173, 412, 223), viewport))
    }

    @Test
    fun emptyRecentViewportEndsAfterItsPlaceholderAndTwoCompleteFollowingRows() {
        fun row(top: Int) = List(8) { column -> Rect(column * 50, top, column * 50 + 50, top + 130) }
        val placeholder = Rect(0, 21, 412, 165)
        val first = row(186)
        val second = row(316)
        val fourth = row(446)

        assertEquals(446, emptyRecentViewportFromBounds(placeholder, first + second + fourth))
        assertNull(emptyRecentViewportFromBounds(placeholder, first))
        assertNull(emptyRecentViewportFromBounds(placeholder, first + second.take(7)))
    }

    @Test
    fun onlyFullyVisibleEmojiCellsRemainAccessibilityTargets() {
        val viewport = Rect(0, 0, 412, 173)
        assertTrue(isEmojiCellFullyVisibleInViewport(Rect(0, 8, 50, 58), viewport))
        // A fourth row beginning at the viewport bottom, and a partially clipped row, are
        // both hidden from TalkBack along with their touch/drawing clip.
        assertFalse(isEmojiCellFullyVisibleInViewport(Rect(0, 173, 50, 223), viewport))
        assertFalse(isEmojiCellFullyVisibleInViewport(Rect(0, 160, 50, 210), viewport))
    }

    @Test
    fun visibleEmptyRecentDoesNotExpandAnExistingCategoryViewportLock() {
        assertEquals(155, resolveEmojiViewportWithPlaceholder(155, 453, 446, true))
        assertEquals(446, resolveEmojiViewportWithPlaceholder(null, 453, 446, true))
        // Before the two rows attach, maximum is a temporary clip rather than a lock.
        assertEquals(453, resolveEmojiViewportWithPlaceholder(null, 453, null, true))
    }

    @Test
    fun categoryTransitionWaitsForTargetContentInsteadOfSettlingThePreviousPlaceholder() {
        assertFalse(isEmojiCategoryContentReady(1, null, true))
        assertTrue(isEmojiCategoryContentReady(1, 411, false))
        assertFalse(isEmojiCategoryContentReady(0, null, true))
        assertTrue(isEmojiCategoryContentReady(0, 446, true))
    }

    @Test
    fun categoryTransitionAcceptsRowsOnlyAfterTheRequestedCategoryTitleIsFirst() {
        // AndroidX header selection scrolls to the CATEGORY_TITLE item for that category.
        // Rows from Recent can remain attached while Faces is becoming the first item.
        val categoryTitlePositions = listOf(0, 18, 36)
        assertFalse(isEmojiCategoryAtBodyStart(0, categoryTitlePositions, 1))
        assertTrue(isEmojiCategoryAtBodyStart(18, categoryTitlePositions, 1))
        assertFalse(isEmojiCategoryAtBodyStart(36, categoryTitlePositions, 1))
        assertFalse(isEmojiCategoryAtBodyStart(18, categoryTitlePositions, 4))
    }

    @Test
    fun categoryRelockUsesThePresetMaximumInsteadOfThePreviousCategoryViewport() {
        // Faces can fit three rows in 155px; returning to Recent needs the full 173px.
        // The latter must not be clamped by Faces' previous actual viewport.
        assertEquals(155, boundedEmojiViewport(173, 155))
        assertEquals(173, boundedEmojiViewport(173, 173))
    }

    @Test
    fun categoryActivationAcceptsTouchEquivalentA11yAndKeyboardKeysOnlyOnRelease() {
        assertTrue(isEmojiCategoryAccessibilityAction(
            androidx.core.view.accessibility.AccessibilityNodeInfoCompat.ACTION_CLICK))
        assertFalse(isEmojiCategoryAccessibilityAction(
            androidx.core.view.accessibility.AccessibilityNodeInfoCompat.ACTION_LONG_CLICK))
        listOf(
            android.view.KeyEvent.KEYCODE_ENTER,
            android.view.KeyEvent.KEYCODE_DPAD_CENTER,
            android.view.KeyEvent.KEYCODE_SPACE,
        ).forEach { keyCode ->
            assertTrue(isEmojiCategoryActivationKey(keyCode, android.view.KeyEvent.ACTION_UP))
            assertFalse(isEmojiCategoryActivationKey(keyCode, android.view.KeyEvent.ACTION_DOWN))
        }
        assertFalse(isEmojiCategoryActivationKey(android.view.KeyEvent.KEYCODE_DEL, android.view.KeyEvent.ACTION_UP))
    }

    @Test
    fun latestCategoryActivationWinsOverAnOlderPendingRelock() {
        assertFalse(isCurrentEmojiCategoryTransition(41L, 42L))
        assertTrue(isCurrentEmojiCategoryTransition(42L, 42L))
    }

    @Test
    fun rapidCategoryActivationInheritsTheOriginalStableViewportBaseline() {
        val stable = EmojiViewportBaseline(
            viewportHeight = 155,
            wasLocked = true,
            clipBounds = Rect(0, 0, 412, 155),
        )
        val unlockedIntermediate = EmojiViewportBaseline(
            viewportHeight = 155,
            wasLocked = false,
            clipBounds = Rect(0, 0, 412, 155),
        )

        assertEquals(stable, nextEmojiCategoryBaseline(stable, unlockedIntermediate))
        assertEquals(unlockedIntermediate, nextEmojiCategoryBaseline(null, unlockedIntermediate))
    }

    @Test
    fun staleCategoryCallbackCannotApplyViewportToAReplacementPickerBody() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val oldBody = RecyclerView(activity)
        val replacementBody = RecyclerView(activity)

        assertFalse(shouldApplyEmojiPickerViewport(replacementBody, oldBody))
        assertTrue(shouldApplyEmojiPickerViewport(replacementBody, replacementBody))
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

        val mask = root.getChildAt(3)
        assertEquals(4, root.childCount)
        assertEquals(3, content.childCount)
        assertSame(candidate, content.getChildAt(0))
        assertSame(keyboard, content.getChildAt(1))
        assertSame(hideBar, content.getChildAt(2))
        assertEquals(View.GONE, picker.visibility)
        assertEquals(View.GONE, mask.visibility)
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

    private fun View.descendants(): List<View> {
        val result = mutableListOf<View>()
        fun visit(view: View) {
            result += view
            if (view is ViewGroup) repeat(view.childCount) { visit(view.getChildAt(it)) }
        }
        visit(this)
        return result
    }

    class HidingImeService : ImeService() {
        var hideFlags: Int? = null

        override fun requestHideSelf(flags: Int) {
            hideFlags = flags
        }
    }
}
