package com.masuidrive.gestureime

import android.app.Activity
import android.graphics.Rect
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
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
import com.masuidrive.gestureime.keyboard.KeyboardUiState
import com.masuidrive.gestureime.keyboard.KeyboardView
import com.masuidrive.gestureime.ui.CandidateStripView
import com.masuidrive.gestureime.ui.CandidateUiSnapshot
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
    fun emojiLayerReplacesCandidateSlotAndKeepsTheFixedRailWithTheControlRow() {
        val service = Robolectric.buildService(HidingImeService::class.java).create().get()
        val root = service.onCreateInputView() as FrameLayout
        val content = root.getChildAt(0) as LinearLayout
        val candidate = content.getChildAt(0) as CandidateStripView
        val keyboard = content.getChildAt(1) as KeyboardView
        val picker = root.getChildAt(1) as EmojiPickerView
        val mask = root.getChildAt(3)
        val railProxy = root.getChildAt(5)

        service.onKeyAction(KeyAction.SwitchLayer(KeyboardMode.EMOJI))
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        keyboard.measure(exact(412), exact(1_000)); keyboard.layout(0, 0, 412, keyboard.measuredHeight)

        assertEquals(View.INVISIBLE, candidate.visibility)
        assertEquals(View.VISIBLE, picker.visibility)
        assertEquals(View.VISIBLE, mask.visibility)
        assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_NO, mask.importantForAccessibility)
        assertEquals(4, keyboard.accessibilityNodeProvider.createAccessibilityNodeInfo(-1)!!.childCount)
        assertEquals(View.VISIBLE, railProxy.visibility)

        KeyboardHeightPreset.entries.forEach { preset ->
            keyboard.setHeightPreset(preset)
            root.measure(exact(412), exact(1_000)); root.layout(0, 0, 412, 1_000)
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            root.measure(exact(412), exact(1_000)); root.layout(0, 0, 412, 1_000)
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            root.measure(exact(412), exact(1_000)); root.layout(0, 0, 412, 1_000)
            val expectedControlTop = (50 * service.resources.displayMetrics.density).toInt() +
                (8 * service.resources.displayMetrics.density).toInt() +
                (preset.rowPitchDp * service.resources.displayMetrics.density * 4).toInt()
            assertEquals(0, mask.height)
            assertEquals(expectedControlTop, mask.top)
            assertEquals(expectedControlTop, picker.layoutParams.height)
            assertEquals(keyboard.emojiLayerHorizontalGeometryForWidth(412).railRight, railProxy.width)
            assertEquals(candidate.height, railProxy.top)
            assertEquals(expectedControlTop, railProxy.bottom)
        }
    }

    @Test
    fun emojiRailProxyForwardsLayerTapsToTheProductionKeyboardView() {
        val service = Robolectric.buildService(HidingImeService::class.java).create().get()
        val root = service.onCreateInputView() as FrameLayout
        val content = root.getChildAt(0) as LinearLayout
        val keyboard = content.getChildAt(1) as KeyboardView
        val railProxy = root.getChildAt(5)
        service.onKeyAction(KeyAction.SwitchLayer(KeyboardMode.EMOJI))
        measureAndLayout(root, 412)

        fun tapRail(y: Float) {
            listOf(android.view.MotionEvent.ACTION_DOWN, android.view.MotionEvent.ACTION_UP).forEachIndexed { index, action ->
                android.view.MotionEvent.obtain(0, index * 10L, action, railProxy.width / 2f, y, 0).also {
                    railProxy.dispatchTouchEvent(it)
                    it.recycle()
                }
            }
        }

        tapRail(80f)
        assertEquals("記号キーボード", keyboard.contentDescription)

        service.onKeyAction(KeyAction.SwitchLayer(KeyboardMode.EMOJI))
        tapRail(135f)
        assertEquals("テンキーキーボード", keyboard.contentDescription)
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
                val overlayHeight = (8 * density).toInt() + (preset.rowPitchDp * density * 4).toInt()
                val controlTop = keyboard.top + overlayHeight
                assertEquals(controlTop, picker.bottom)
                assertEquals(controlTop, mask.top + mask.height)
            }
        } finally {
            ImePreferences.setKeyboardHeightPreset(context, originalPreset)
        }
    }

    @Test
    fun freshPickerClipsAtTheControlBoundaryBeforePostedSettling() {
        val context = Robolectric.buildService(HidingImeService::class.java).create().get()
        val originalPreset = ImePreferences.getKeyboardHeightPreset(context)
        try {
            listOf(412, 840).forEach { width ->
                KeyboardHeightPreset.entries.forEach { preset ->
                    ImePreferences.setKeyboardHeightPreset(context, preset)
                    val service = Robolectric.buildService(HidingImeService::class.java).create().get()
                    val root = service.onCreateInputView() as FrameLayout
                    val content = root.getChildAt(0) as LinearLayout
                    val keyboard = content.getChildAt(1) as KeyboardView
                    val picker = root.getChildAt(1) as EmojiPickerView
                    service.onKeyAction(KeyAction.SwitchLayer(KeyboardMode.EMOJI))
                    // AndroidX may have only its provisional body at this point. The root must
                    // already be a hard boundary, before its posted RecyclerView work runs.
                    measureAndLayout(root, width)
                    assertEmojiPickerRootBoundary(picker, keyboard, preset, service)

                    Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
                    measureAndLayout(root, width)
                    assertEmojiPickerRootBoundary(picker, keyboard, preset, service)
                }
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
        val keyboard = (root.getChildAt(0) as LinearLayout).getChildAt(1) as KeyboardView

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

        assertEquals(KeyboardMode.QWERTY, keyboard.mode())
        assertEquals(View.GONE, publicPicker.visibility)
        assertEquals(View.GONE, privatePicker.visibility)
        assertEquals(View.GONE, mask.visibility)
        assertEquals(KeyboardMode.EMOJI, ImePreferences.getLastKeyboardMode(service))

        service.onKeyAction(KeyAction.SwitchLayer(KeyboardMode.EMOJI))
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

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
    fun shrunkenEmojiViewportMaskStartsAfterRailAndEndsAtKeyboardContentRight() {
        val service = Robolectric.buildService(HidingImeService::class.java).create().get()
        val root = service.onCreateInputView() as FrameLayout
        val content = root.getChildAt(0) as LinearLayout
        val keyboard = content.getChildAt(1) as KeyboardView
        val picker = root.getChildAt(1) as EmojiPickerView
        val mask = root.getChildAt(3)
        val railProxy = root.getChildAt(5)
        service.onKeyAction(KeyAction.SwitchLayer(KeyboardMode.EMOJI))
        measureAndLayout(root, 412)
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

        @Suppress("UNCHECKED_CAST")
        val viewports = ImeService::class.java.getDeclaredField("pickerViewportHeights").apply { isAccessible = true }
            .get(service) as MutableMap<EmojiPickerView, Int>
        viewports[picker] = 100
        service.onStartInput(EditorInfo().apply { inputType = InputType.TYPE_CLASS_TEXT }, false)
        measureAndLayout(root, 412)

        val geometry = keyboard.emojiLayerHorizontalGeometryForWidth(412)
        assertTrue(mask.height > 0)
        assertEquals(geometry.railRight, mask.left)
        assertEquals(geometry.contentRight, mask.right)
        assertEquals(railProxy.right, mask.left)
    }

    @Test
    fun pickerBodyGeometryApplicationReplacesPhoneInsetsBeforeWideMeasure() {
        val service = Robolectric.buildService(HidingImeService::class.java).create().get()
        val keyboard = KeyboardView(service)
        val body = RecyclerView(service)
        val content = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            addView(body, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 200))
        }
        val phoneGeometry = keyboard.emojiLayerHorizontalGeometryForWidth(412)
        assertTrue(applyEmojiPickerBodyHorizontalLayout(content, body, 412, phoneGeometry))
        content.measure(exact(412), exact(200)); content.layout(0, 0, 412, 200)
        assertEquals(phoneGeometry.railRight, body.left)
        assertEquals(phoneGeometry.bodyWidth, body.width)
        assertEquals(phoneGeometry.contentRight, body.right)

        val geometry = keyboard.emojiLayerHorizontalGeometryForWidth(840)
        assertTrue(applyEmojiPickerBodyHorizontalLayout(content, body, 840, geometry))
        content.measure(exact(840), exact(200)); content.layout(0, 0, 840, 200)
        val bodyParams = body.layoutParams as ViewGroup.MarginLayoutParams
        assertEquals(geometry.railRight, body.left)
        assertEquals(geometry.bodyWidth, body.width)
        assertEquals(geometry.contentRight, body.right)
        assertEquals(ViewGroup.LayoutParams.MATCH_PARENT, bodyParams.width)
        assertEquals(0, bodyParams.leftMargin)
        assertEquals(0, bodyParams.rightMargin)
        assertFalse(applyEmojiPickerBodyHorizontalLayout(content, body, 840, geometry))
    }

    @Test
    fun categoryTransitionRestoresAndroidXFullWidthBodyToKeyboardContentBounds() {
        val service = Robolectric.buildService(HidingImeService::class.java).create().get()
        val root = service.onCreateInputView() as FrameLayout
        val content = root.getChildAt(0) as LinearLayout
        val keyboard = content.getChildAt(1) as KeyboardView
        val picker = root.getChildAt(1) as EmojiPickerView
        service.onKeyAction(KeyAction.SwitchLayer(KeyboardMode.EMOJI))
        root.measure(exact(412), exact(1_000)); root.layout(0, 0, 412, 1_000)
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        root.measure(exact(413), exact(1_000)); root.layout(0, 0, 413, 1_000)
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        root.measure(exact(412), exact(1_000)); root.layout(0, 0, 412, 1_000)
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

        val header = picker.findViewById<RecyclerView>(androidx.emoji2.emojipicker.R.id.emoji_picker_header)
        val body = picker.findViewById<RecyclerView>(androidx.emoji2.emojipicker.R.id.emoji_picker_body)
        val androidXParams = body.layoutParams as ViewGroup.MarginLayoutParams
        androidXParams.width = ViewGroup.LayoutParams.MATCH_PARENT
        androidXParams.leftMargin = 0
        androidXParams.rightMargin = 0
        body.layoutParams = androidXParams
        assertEquals(ViewGroup.LayoutParams.MATCH_PARENT, body.layoutParams.width)

        header.getChildAt(header.childCount - 1).performClick()
        root.measure(exact(412), exact(1_000)); root.layout(0, 0, 412, 1_000)
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

        val currentBody = picker.findViewById<RecyclerView>(androidx.emoji2.emojipicker.R.id.emoji_picker_body)
        val geometry = keyboard.emojiLayerHorizontalGeometryForWidth(412)
        val restored = currentBody.layoutParams as ViewGroup.MarginLayoutParams
        assertEquals(geometry.railRight, currentBody.left)
        assertEquals(geometry.bodyWidth, currentBody.width)
        assertEquals(ViewGroup.LayoutParams.MATCH_PARENT, restored.width)
        assertEquals(0, restored.leftMargin)
        assertEquals(0, restored.rightMargin)
        assertEquals(7, picker.emojiGridColumns)
        assertTrue((currentBody.adapter?.itemCount ?: 0) > 0)
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
    fun pickerBodyShowsOnlyFourFullTouchRowsAndHeaderKeepsTen48dpCategories() {
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
            val expectedViewport = (8 * density).toInt() + (preset.rowPitchDp * density * 4).toInt()
            assertEquals(10, header.adapter!!.itemCount)
            assertEquals(7, picker.emojiGridColumns)
            val expectedHeaderWidth = (48 * density).toInt()
            val attachedHeaders = (0 until header.childCount).map(header::getChildAt)
            assertTrue(attachedHeaders.isNotEmpty())
            assertTrue(attachedHeaders.all {
                it.layoutParams.width == expectedHeaderWidth && it.minimumWidth == expectedHeaderWidth
            })
            attachedHeaders.last().performClick()
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            assertTrue((0 until header.childCount).map(header::getChildAt).all {
                it.layoutParams.width == expectedHeaderWidth && it.minimumWidth == expectedHeaderWidth
            })
            // The physical RecyclerView and its clip finish at the fixed control boundary;
            // category relocks can only shrink the clip, never move controls or overlap them.
            assertEquals(expectedViewport, body.height)
            assertEquals(expectedViewport, body.clipBounds!!.bottom)
            val geometry = keyboard.emojiLayerHorizontalGeometryForWidth(picker.width)
            assertEquals(0, header.left)
            assertEquals(picker.width, header.width)
            assertEquals(geometry.railRight, body.left)
            assertEquals(geometry.bodyWidth, body.width)
            assertEquals(geometry.contentRight, body.right)
            assertTrue(kotlin.math.abs(body.width / 7f - geometry.bodyWidth / 7f) < 1f)
            assertEquals(keyboard.top + expectedViewport, picker.bottom)
            // Four full attached rows use the preset pitch; the separate bounds regression
            // below fixes their exact lower edge and excludes a fifth row from accessibility.
            val rowPitch = (preset.rowPitchDp * density).toInt()
            assertEquals(4, (expectedViewport - (8 * density).toInt()) / rowPitch)
            assertTrue(rowPitch >= (48 * density).toInt())
            assertTrue(body.clipChildren && body.clipToPadding)
            assertTrue(body.adapter != null)
        }
    }

    @Test
    fun attachedEmojiRowsDefineTheExactFourRowViewport() {
        val bounds = listOf(
            Rect(0, 8, 50, 58), Rect(50, 8, 100, 58),
            Rect(0, 58, 50, 108), Rect(50, 58, 100, 108),
            Rect(0, 108, 50, 158), Rect(50, 108, 100, 158),
            Rect(0, 158, 50, 208),
        )
        assertEquals(208, fourthEmojiRowBottom(bounds))
        assertEquals(208, resolveEmojiViewport(208, 150))
    }

    @Test
    fun roundedCategoryStartLocksButAnEmptyRecentPlaceholderDoesNot() {
        fun rowsAt(firstTop: Int) = listOf(
            Rect(0, firstTop, 50, firstTop + 50),
            Rect(0, firstTop + 50, 50, firstTop + 100),
            Rect(0, firstTop + 100, 50, firstTop + 150),
            Rect(0, firstTop + 150, 50, firstTop + 200),
        )

        // An 8px spacer can round to 9px at the start of a normal category.
        assertEquals(209, fourthEmojiRowBottomAtCategoryStart(rowsAt(9), 8))
        // The empty-Recent placeholder puts the next category far below that tolerance.
        assertNull(fourthEmojiRowBottomAtCategoryStart(rowsAt(71), 8))
    }

    @Test
    fun emptyRecentPlaceholderIsRecognizedOnlyWhileItIntersectsTheViewport() {
        val viewport = Rect(0, 0, 412, 173)
        assertTrue(isEmojiPlaceholderInViewport(View.VISIBLE, Rect(0, 8, 412, 58), viewport))
        assertFalse(isEmojiPlaceholderInViewport(View.GONE, Rect(0, 8, 412, 58), viewport))
        assertFalse(isEmojiPlaceholderInViewport(View.VISIBLE, Rect(0, 173, 412, 223), viewport))
    }

    @Test
    fun emptyRecentViewportEndsAfterItsPlaceholderAndThreeCompleteFollowingRows() {
        fun row(top: Int) = List(7) { column -> Rect(column * 50, top, column * 50 + 50, top + 130) }
        val placeholder = Rect(0, 21, 412, 165)
        val first = row(186)
        val second = row(316)
        val third = row(446)

        assertEquals(576, emptyRecentViewportFromBounds(placeholder, first + second + third))
        assertNull(emptyRecentViewportFromBounds(placeholder, first))
        assertNull(emptyRecentViewportFromBounds(placeholder, first + second.take(6)))
    }

    @Test
    fun returningFromAnotherCategoryLocksEmptyRecentAfterThreeCompleteSevenColumnRows() {
        fun row(top: Int) = List(7) { column -> Rect(column * 50, top, column * 50 + 50, top + 130) }
        val placeholder = Rect(0, 21, 355, 165)
        val observedViewport = emptyRecentViewportFromBounds(placeholder, row(186) + row(316) + row(446))

        assertEquals(576, observedViewport)
        assertTrue(isEmojiCategoryContentReady(0, observedViewport, true))
        assertFalse(isEmojiCategoryContentReady(1, observedViewport, true))
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
    fun unchangedPickerMaskGeometryDoesNotScheduleAnotherLayout() {
        assertFalse(shouldUpdateEmojiPickerMask(496, 7, 496, 7))
        assertTrue(shouldUpdateEmojiPickerMask(496, 7, 503, 0))
    }

    @Test
    fun visibleEmptyRecentDoesNotExpandAnExistingCategoryViewportLock() {
        assertEquals(155, resolveEmojiViewportWithPlaceholder(155, 453, 446, true))
        assertEquals(446, resolveEmojiViewportWithPlaceholder(null, 453, 446, true))
        // Before the three rows attach, maximum is a temporary clip rather than a lock.
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
    fun emojiRecentResetsOnlyWhenTheLayerBecomesVisible() {
        assertTrue(shouldResetEmojiPickerOnVisibilityTransition(false, true))
        assertFalse(shouldResetEmojiPickerOnVisibilityTransition(true, true))
        assertFalse(shouldResetEmojiPickerOnVisibilityTransition(true, false))
        assertFalse(shouldResetEmojiPickerOnVisibilityTransition(false, false))
    }

    @Test
    fun recreatedInputViewInEmojiModeQueuesRecentResetForItsNewPickerOnly() {
        val service = Robolectric.buildService(HidingImeService::class.java).create().get()
        val firstRoot = service.onCreateInputView() as FrameLayout
        service.onKeyAction(KeyAction.SwitchLayer(KeyboardMode.EMOJI))
        measureAndLayout(firstRoot, 412)
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        val firstPicker = firstRoot.getChildAt(1) as EmojiPickerView

        val recreatedRoot = service.onCreateInputView() as FrameLayout
        val recreatedPicker = recreatedRoot.getChildAt(1) as EmojiPickerView
        @Suppress("UNCHECKED_CAST")
        val pending = ImeService::class.java.getDeclaredField("pickersAwaitingRecentReset").apply { isAccessible = true }
            .get(service) as MutableSet<EmojiPickerView>
        assertFalse(firstPicker in pending)
        assertTrue(recreatedPicker in pending)

        // AndroidX can create its header after the first layout; the fresh picker keeps this
        // request until its Recent holder is attached, instead of inheriting the old view's
        // already-visible flag and silently skipping AC1.
        measureAndLayout(recreatedRoot, 412)
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        val headerAttached = (recreatedPicker.findViewById<RecyclerView>(
            androidx.emoji2.emojipicker.R.id.emoji_picker_header,
        )?.childCount ?: 0) > 0
        assertTrue(recreatedPicker in pending || headerAttached)
        assertEquals(KeyboardMode.EMOJI, ((recreatedRoot.getChildAt(0) as LinearLayout).getChildAt(1) as KeyboardView).mode())
    }

    @Test
    fun emojiReentrySelectsRecentAdapterPositionZeroAfterTheHeaderWasScrolledAway() {
        val service = Robolectric.buildService(HidingImeService::class.java).create().get()
        val root = service.onCreateInputView() as FrameLayout
        service.onKeyAction(KeyAction.SwitchLayer(KeyboardMode.EMOJI))
        repeat(3) {
            measureAndLayout(root, 412)
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        }
        val picker = root.getChildAt(1) as EmojiPickerView
        val header = requireNotNull(picker.findViewById<RecyclerView>(
            androidx.emoji2.emojipicker.R.id.emoji_picker_header,
        ))
        val body = requireNotNull(picker.findViewById<RecyclerView>(
            androidx.emoji2.emojipicker.R.id.emoji_picker_body,
        ))
        val lastCategory = requireNotNull(header.adapter).itemCount - 1
        assertTrue(lastCategory > 0)
        header.scrollToPosition(lastCategory)
        measureAndLayout(root, 412)
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        assertTrue("header should have a non-Recent attached holder before re-entry", (0 until header.childCount)
            .any { header.getChildAdapterPosition(header.getChildAt(it)) > 0 })

        @Suppress("UNCHECKED_CAST")
        val pending = ImeService::class.java.getDeclaredField("pickersAwaitingRecentReset").apply { isAccessible = true }
            .get(service) as MutableSet<EmojiPickerView>
        pending += picker
        val reset = ImeService::class.java.getDeclaredMethod(
            "resetEmojiPickerToRecentIfNeeded",
            EmojiPickerView::class.java,
            RecyclerView::class.java,
            RecyclerView::class.java,
            Boolean::class.javaPrimitiveType,
        ).apply { isAccessible = true }
        reset.invoke(service, picker, header, body, true)
        measureAndLayout(root, 412)
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        measureAndLayout(root, 412)
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        // The first call can run before RecyclerView has attached position zero; retrying
        // after layout mirrors the production child-attachment callback.
        reset.invoke(service, picker, header, body, true)
        measureAndLayout(root, 412)
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

        val attachedPositions = (0 until header.childCount)
            .map { header.getChildAdapterPosition(header.getChildAt(it)) }
        assertTrue("Recent holder is adapter position zero; attached=$attachedPositions", 0 in attachedPositions)
        assertEquals(0, (body.layoutManager as androidx.recyclerview.widget.LinearLayoutManager)
            .findFirstVisibleItemPosition())
    }

    @Test
    fun reusedEmojiInputViewTreatsHideShowAndEditorFinishAsNewVisibilityTransitions() {
        val service = Robolectric.buildService(HidingImeService::class.java).create().get()
        val root = service.onCreateInputView() as FrameLayout
        service.onKeyAction(KeyAction.SwitchLayer(KeyboardMode.EMOJI))
        measureAndLayout(root, 412)
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        val visible = ImeService::class.java.getDeclaredField("emojiPickerVisible").apply { isAccessible = true }

        assertTrue(visible.getBoolean(service))
        service.onFinishInputView(false)
        assertFalse(visible.getBoolean(service))
        // No new root is created here. Reusing the picker must still make the visible emoji
        // layer an entry, while an ordinary category selection remains in the same session.
        service.onStartInputView(EditorInfo(), true)
        assertTrue(visible.getBoolean(service))

        service.onFinishInput()
        assertFalse(visible.getBoolean(service))
        service.onStartInput(EditorInfo(), false)
        assertTrue(visible.getBoolean(service))
    }

    @Test
    fun categoryRelockUsesThePresetMaximumInsteadOfThePreviousCategoryViewport() {
        // Faces can fit four rows in 155px; returning to Recent needs the full 173px.
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
    fun recycledEmojiCategoryHolderIsRestoredToTheExactConfiguredWidth() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val holder = View(activity).apply {
            layoutParams = RecyclerView.LayoutParams(96, 50)
            minimumWidth = 72
        }

        enforceEmojiCategoryHolderWidth(holder, 48)

        assertEquals(48, holder.layoutParams.width)
        assertEquals(48, holder.minimumWidth)

        holder.measure(exact(48), exact(50))
        holder.layout(0, 0, 48, 50)
        val normalizedParams = holder.layoutParams
        assertFalse(holder.isLayoutRequested)

        enforceEmojiCategoryHolderWidth(holder, 48)
        enforceEmojiCategoryHolderWidth(holder, 48)

        assertSame(normalizedParams, holder.layoutParams)
        assertFalse(holder.isLayoutRequested)
    }

    @Test
    fun seededNavigationInsetAndFirstCandidatesKeepTheImeRootAndKeysFixed() {
        listOf(412, 840).forEach { width ->
            val service = Robolectric.buildService(SeededNavigationInsetImeService::class.java).create().get()
            val root = service.onCreateInputView() as FrameLayout
            val content = root.getChildAt(0) as LinearLayout
            val candidate = content.getChildAt(0) as CandidateStripView
            val keyboard = content.getChildAt(1) as KeyboardView
            measureAndLayout(root, width)

            val initialRootHeight = root.measuredHeight
            val initialKeyboardHeight = keyboard.measuredHeight
            val initialFirstKey = firstKeyBottomInRoot(keyboard)
            assertEquals("$width first measure keeps the navigation area", 31, keyboard.paddingBottom)
            val density = service.resources.displayMetrics.density
            val expectedKeyboardHeight =
                (KeyboardHeightPreset.STANDARD.rowPitchDp * density).toInt() * 4 +
                    (8 * density).toInt() +
                    keyboard.paddingBottom
            assertEquals("$width first measure includes all four rows and the navigation area", expectedKeyboardHeight, initialKeyboardHeight)
            assertEquals("$width first measure includes the fixed candidate row", (50 * density).toInt() + expectedKeyboardHeight, initialRootHeight)

            // The same inset may arrive again after attach, then the candidate replaces its
            // children and requests another host measure. Neither transition may move keys.
            ViewCompat.dispatchApplyWindowInsets(
                keyboard,
                WindowInsetsCompat.Builder()
                    .setInsets(WindowInsetsCompat.Type.navigationBars(), Insets.of(0, 0, 0, 31))
                    .setInsetsIgnoringVisibility(WindowInsetsCompat.Type.navigationBars(), Insets.of(0, 0, 0, 31))
                    .build(),
            )
            measureAndLayout(root, width)
            assertEquals("$width delayed inset stays at its seeded height", 31, keyboard.paddingBottom)
            assertEquals("$width delayed inset must not grow the IME root", initialRootHeight, root.measuredHeight)
            assertEquals("$width delayed inset must not move key faces", initialFirstKey, firstKeyBottomInRoot(keyboard))

            // CandidateStripView replaces its child views, which requests this next host layout.
            candidate.showCandidates(CandidateUiSnapshot(1L, listOf("候補", "変換候補")))
            measureAndLayout(root, width)
            assertEquals("$width first candidates must not grow the IME root", initialRootHeight, root.measuredHeight)
            assertEquals("$width first candidates must not resize four key rows", initialKeyboardHeight, keyboard.measuredHeight)
            assertEquals("$width first candidates must not move key faces", initialFirstKey, firstKeyBottomInRoot(keyboard))
        }
    }

    @Test
    fun initialNavigationInsetPrefersCurrentMetricsThenDecorThenLegacy() {
        assertEquals(
            "current metrics replace a stale decor value after rotation or Fold reconstruction",
            24,
            resolveInitialKeyboardBottomInset(metricsBottom = 24, decorBottom = 48, legacyBottom = 42),
        )
        assertEquals(
            "a zero current metric keeps the software navigation resource reservation",
            42,
            resolveInitialKeyboardBottomInset(metricsBottom = 0, decorBottom = 48, legacyBottom = 42),
        )
        assertEquals(
            "hardware navigation remains zero when the software navigation resource is absent",
            0,
            resolveInitialKeyboardBottomInset(metricsBottom = 0, decorBottom = 48, legacyBottom = 0),
        )
        assertEquals(
            "missing current metrics falls back to the decor inset",
            63,
            resolveInitialKeyboardBottomInset(metricsBottom = null, decorBottom = 63, legacyBottom = 42),
        )
        assertEquals(
            "when neither API 30 source is available, retain the legacy navigation reservation",
            42,
            resolveInitialKeyboardBottomInset(metricsBottom = null, decorBottom = 0, legacyBottom = 42),
        )
    }

    private fun measureAndLayout(root: View, width: Int) {
        root.measure(
            exact(width),
            View.MeasureSpec.makeMeasureSpec(1_000, View.MeasureSpec.AT_MOST),
        )
        root.layout(0, 0, root.measuredWidth, root.measuredHeight)
    }

    private fun firstKeyBottomInRoot(keyboard: KeyboardView): Int {
        val keyBounds = Rect().also {
            requireNotNull(keyboard.accessibilityNodeProvider.createAccessibilityNodeInfo(0)).getBoundsInParent(it)
        }
        return keyboard.top + keyBounds.bottom
    }

    private fun assertEmojiPickerRootBoundary(
        picker: EmojiPickerView,
        keyboard: KeyboardView,
        preset: KeyboardHeightPreset,
        service: ImeService,
    ) {
        val density = service.resources.displayMetrics.density
        val rowPitchDp = if (
            preset == KeyboardHeightPreset.LARGE &&
            keyboard.width / density >= KeyboardView.DUAL_FLICK_MIN_WIDTH_DP
        ) 62f else preset.rowPitchDp
        val viewport = (8 * density).toInt() + (rowPitchDp * density * 4).toInt()
        assertTrue("picker clips AndroidX children at the fixed control row", picker.clipChildren && picker.clipToPadding)
        assertEquals(Rect(0, 0, picker.width, picker.height), picker.clipBounds)
        assertEquals("picker ends after the four-row emoji body", keyboard.top + viewport, picker.bottom)
    }

    private fun exact(size: Int) = View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY)

    private fun KeyboardView.mode(): KeyboardMode {
        val field = KeyboardView::class.java.getDeclaredField("state").apply { isAccessible = true }
        return (field.get(this) as KeyboardUiState).mode
    }

    private fun View.descendants(): List<View> {
        val result = mutableListOf<View>()
        fun visit(view: View) {
            result += view
            if (view is ViewGroup) repeat(view.childCount) { visit(view.getChildAt(it)) }
        }
        visit(this)
        return result
    }

    class HidingImeService : ImeService()

    class SeededNavigationInsetImeService : ImeService() {
        override fun initialKeyboardBottomInset() = 31
    }
}
