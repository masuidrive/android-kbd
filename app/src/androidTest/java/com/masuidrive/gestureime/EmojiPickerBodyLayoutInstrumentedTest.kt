package com.masuidrive.gestureime

import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Rect
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import androidx.emoji2.emojipicker.EmojiPickerView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.masuidrive.gestureime.conversion.ConversionCommit
import com.masuidrive.gestureime.conversion.ConversionEngine
import com.masuidrive.gestureime.conversion.ConversionState
import com.masuidrive.gestureime.keyboard.KeyAction
import com.masuidrive.gestureime.keyboard.KeyboardMode
import com.masuidrive.gestureime.keyboard.KeyboardView
import com.masuidrive.gestureime.keyboard.emojiLayerHorizontalGeometry
import com.masuidrive.gestureime.voice.VoiceRecognitionController
import com.masuidrive.gestureime.voice.VoiceRecognizerFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.roundToInt

@RunWith(AndroidJUnit4::class)
class EmojiPickerBodyLayoutInstrumentedTest {
    @Test
    fun attachedImePickerCentersRealEmojiViewsAndTapCommitsThroughImeCallback() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        ActivityScenario.launch(ImeTestActivity::class.java).use { scenario ->
            scenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
            instrumentation.waitForIdleSync()
            Thread.sleep(300)
            lateinit var service: AttachedImeService
            lateinit var input: RecordingConnection
            lateinit var imeRoot: View
            scenario.onActivity { activity ->
                service = AttachedImeService().apply {
                    attachForTest(activity)
                    onCreate()
                }
                input = RecordingConnection(View(activity))
                service.installTestDependencies(
                    voice = VoiceRecognitionController(
                        sdkInt = 30,
                        hasPermission = { false },
                        onDeviceAvailable = { false },
                        factory = VoiceRecognizerFactory { error("voice is not used") },
                        onState = service::onVoiceState,
                        onInputLevel = service::onVoiceInputLevel,
                    ),
                    text = TextInputController(
                        connection = { input }, context = service,
                        clipboard = service.getSystemService(android.content.ClipboardManager::class.java),
                    ),
                    conversion = NoopConversion,
                )
                service.onStartInput(EditorInfo(), false)
                ImePreferences.recordEmojiRecent(service, "👍")
                activity.setContentView(service.onCreateInputView())
                imeRoot = activity.window.decorView
                    .findViewById<ViewGroup>(android.R.id.content)
                    .getChildAt(0)
                service.onKeyAction(KeyAction.SwitchLayer(KeyboardMode.EMOJI))
            }

            var body: RecyclerView? = null
            repeat(20) {
                instrumentation.waitForIdleSync()
                Thread.sleep(50)
                scenario.onActivity { activity ->
                    imeRoot.measure(exact(1_499), exact(1_000)); imeRoot.layout(0, 0, 1_499, 1_000)
                    body = imeRoot.findViewById<RecyclerView>(androidx.emoji2.emojipicker.R.id.emoji_picker_body)
                }
                if (body != null && body!!.childCount > 0) return@repeat
            }
            val realBody = requireNotNull(body)
            instrumentation.waitForIdleSync()
            Thread.sleep(100)
            val pickerRoot = requireNotNull(findEmojiPicker(imeRoot))
            val cells = emojiViews(realBody)
            require(cells.isNotEmpty())
            lateinit var wideCell: View
            lateinit var wideCellEmoji: String
            instrumentation.runOnMainSync {
                val expected = emojiPickerCellTranslationX(
                    realBody.width, realBody.paddingLeft, realBody.paddingRight, cells.first().width,
                )
                assertTrue(expected > 0f)
                cells.forEach { assertEquals(expected, it.translationX, 0.01f) }

                wideCell = cells.first()
                wideCellEmoji = emojiValue(wideCell)
                val bounds = Rect().also { wideCell.createAccessibilityNodeInfo().getBoundsInScreen(it) }
                val location = IntArray(2).also(wideCell::getLocationOnScreen)
                assertEquals(location[0], bounds.left)
            }
            val commitsBeforeAccessibility = input.committedValues.size
            var accessibilityHandled = false
            instrumentation.runOnMainSync {
                accessibilityHandled = wideCell.performAccessibilityAction(
                    android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK,
                    null,
                )
            }
            instrumentation.waitForIdleSync()
            Thread.sleep(100)
            assertTrue(accessibilityHandled)
            assertEquals(commitsBeforeAccessibility + 1, input.committedValues.size)
            assertEquals(wideCellEmoji, input.committedValues.last())

            var currentBody = realBody
            repeat(20) {
                instrumentation.runOnMainSync {
                    pickerRoot.findViewById<RecyclerView>(androidx.emoji2.emojipicker.R.id.emoji_picker_body)
                        ?.takeIf { it.isAttachedToWindow && it.childCount > 0 }
                        ?.let { currentBody = it }
                }
                if (currentBody.isAttachedToWindow && currentBody.childCount > 0) return@repeat
                Thread.sleep(50)
            }
            instrumentation.runOnMainSync {
                wideCell = emojiViews(currentBody).first { it.isAttachedToWindow }
                wideCellEmoji = emojiValue(wideCell)
            }
            val wideCoordinates = screenCoordinates(instrumentation, pickerRoot, wideCell)
            val screen = instrumentation.targetContext.resources.displayMetrics
            assertTrue(
                "wide target $wideCoordinates must be on ${screen.widthPixels}x${screen.heightPixels}",
                wideCoordinates.x in 0f..screen.widthPixels.toFloat() &&
                    wideCoordinates.y in 0f..screen.heightPixels.toFloat(),
            )
            dispatchTapThroughInputDispatcher(instrumentation, pickerRoot, wideCell)
            instrumentation.waitForIdleSync()
            Thread.sleep(100)
            val commitsAfterScreenTap = input.committedValues.size
            assertEquals(commitsBeforeAccessibility + 2, commitsAfterScreenTap)
            assertEquals(wideCellEmoji, input.committedValues.last())
            repeat(20) {
                instrumentation.runOnMainSync {
                    pickerRoot.findViewById<RecyclerView>(androidx.emoji2.emojipicker.R.id.emoji_picker_body)
                        ?.takeIf { it.isAttachedToWindow && it.childCount > 0 }
                        ?.let { currentBody = it }
                }
                if (currentBody.isAttachedToWindow && currentBody.childCount > 0) return@repeat
                Thread.sleep(50)
            }
            lateinit var variantCell: View
            lateinit var variantToSelect: View
            lateinit var selectedVariant: String
            val variantTouchActions = mutableListOf<Int>()
            var currentVariantCell: View? = null
            repeat(20) {
                instrumentation.runOnMainSync {
                    currentVariantCell = emojiViews(currentBody).firstOrNull {
                        it.isAttachedToWindow && it.isLongClickable
                    }
                }
                if (currentVariantCell != null) return@repeat
                Thread.sleep(50)
            }
            variantCell = requireNotNull(currentVariantCell)
            instrumentation.runOnMainSync {
                variantCell.setOnTouchListener { _, event ->
                    variantTouchActions += event.actionMasked
                    false
                }
            }
            dispatchLongPressThroughInputDispatcher(instrumentation, pickerRoot, variantCell)
            Thread.sleep(1_200)
            instrumentation.waitForIdleSync()
            Thread.sleep(100)
            lateinit var popupRoot: View
            instrumentation.runOnMainSync {
                assertTrue(variantTouchActions.contains(MotionEvent.ACTION_DOWN))
                assertTrue(variantTouchActions.none { it == MotionEvent.ACTION_CANCEL })
                val holder = currentBody.getChildViewHolder(variantCell)
                val controller = holder.javaClass.getDeclaredField("emojiPickerPopupViewController").apply {
                    isAccessible = true
                }.get(holder)
                val popupWindow = controller.javaClass.getDeclaredField("popupWindow").apply {
                    isAccessible = true
                }.get(controller) as android.widget.PopupWindow
                assertTrue(popupWindow.isShowing)
                assertTrue(
                    controller.javaClass.getDeclaredField("clickedEmojiView").apply { isAccessible = true }
                        .get(controller) === variantCell,
                )
                val popup = popupWindow.contentView
                popupRoot = popup
                val targetEmoji = emojiValue(variantCell)
                val variant = emojiViews(popup).first { emojiValue(it) != targetEmoji }
                selectedVariant = emojiValue(variant)
                variantToSelect = variant
            }
            dispatchTapThroughInputDispatcher(instrumentation, popupRoot, variantToSelect)
            instrumentation.waitForIdleSync()
            Thread.sleep(100)
            assertEquals(commitsAfterScreenTap + 1, input.committedValues.size)
            assertEquals(selectedVariant, input.committedValues.last())
            repeat(20) {
                instrumentation.runOnMainSync {
                    pickerRoot.findViewById<RecyclerView>(androidx.emoji2.emojipicker.R.id.emoji_picker_body)
                        ?.takeIf { it.isAttachedToWindow && it.childCount > 0 }
                        ?.let { currentBody = it }
                }
                if (currentBody.isAttachedToWindow && currentBody.childCount > 0) return@repeat
                Thread.sleep(50)
            }
            lateinit var positionsBeforeScroll: Map<View, Int>
            var offsetBeforeScroll = 0
            instrumentation.runOnMainSync {
                offsetBeforeScroll = currentBody.computeVerticalScrollOffset()
                positionsBeforeScroll = emojiViews(currentBody).associateWith(currentBody::getChildAdapterPosition)
            }
            scenario.onActivity {
                currentBody.scrollBy(0, currentBody.height)
            }
            instrumentation.waitForIdleSync()
            Thread.sleep(100)
            lateinit var phoneCellEmoji: String
            val commitsBeforePhoneTap = input.committedValues.size
            instrumentation.runOnMainSync {
                assertTrue(currentBody.computeVerticalScrollOffset() > offsetBeforeScroll)
                val positionsAfterScroll = emojiViews(currentBody).associateWith(currentBody::getChildAdapterPosition)
                assertTrue(
                    positionsAfterScroll.keys.any { it !in positionsBeforeScroll } ||
                        positionsAfterScroll.any { (view, position) -> positionsBeforeScroll[view] != position },
                )
                emojiViews(currentBody).forEach { recycledCell ->
                    assertEquals(
                        emojiPickerCellTranslationX(
                            currentBody.width, currentBody.paddingLeft, currentBody.paddingRight, recycledCell.width,
                        ),
                        recycledCell.translationX,
                        0.01f,
                    )
                }
            }
            scenario.onActivity { activity ->
                val root = activity.window.decorView.findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
                val phoneWidth = (412 * activity.resources.displayMetrics.density).toInt()
                root.measure(exact(phoneWidth), exact(1_000)); root.layout(0, 0, phoneWidth, 1_000)
            }
            instrumentation.waitForIdleSync()
            Thread.sleep(100)
            repeat(20) {
                instrumentation.runOnMainSync {
                    findEmojiPicker(imeRoot)
                        ?.findViewById<RecyclerView>(androidx.emoji2.emojipicker.R.id.emoji_picker_body)
                        ?.takeIf { it.isAttachedToWindow && it.childCount > 0 }
                        ?.let { currentBody = it }
                }
                if (currentBody.isAttachedToWindow && currentBody.childCount > 0) return@repeat
                Thread.sleep(50)
            }
            lateinit var phoneCell: View
            instrumentation.runOnMainSync {
                emojiViews(currentBody).forEach { relaidOutCell ->
                    assertEquals(
                        emojiPickerCellTranslationX(
                            currentBody.width, currentBody.paddingLeft, currentBody.paddingRight, relaidOutCell.width,
                        ),
                        relaidOutCell.translationX,
                        0.01f,
                    )
                }
                phoneCell = emojiViews(currentBody).first()
                phoneCellEmoji = emojiValue(phoneCell)
            }
            dispatchTapThroughInputDispatcher(instrumentation, pickerRoot, phoneCell)
            instrumentation.waitForIdleSync()
            Thread.sleep(100)
            assertEquals(commitsBeforePhoneTap + 1, input.committedValues.size)
            assertEquals(phoneCellEmoji, input.committedValues.last())

            // Recent updates replace AndroidX cells, so obtain a current phone-layout cell before
            // sending the second real long press. This is independent from the wide popup path.
            repeat(20) {
                instrumentation.runOnMainSync {
                    findEmojiPicker(imeRoot)
                        ?.findViewById<RecyclerView>(androidx.emoji2.emojipicker.R.id.emoji_picker_body)
                        ?.takeIf { it.isAttachedToWindow && it.childCount > 0 }
                        ?.let { currentBody = it }
                }
                if (currentBody.isAttachedToWindow && currentBody.childCount > 0) return@repeat
                Thread.sleep(50)
            }
            lateinit var phoneVariantCell: View
            lateinit var phoneVariantToSelect: View
            lateinit var phoneVariantEmoji: String
            instrumentation.runOnMainSync {
                phoneVariantCell = emojiViews(currentBody).first {
                    it.isAttachedToWindow && it.isLongClickable
                }
            }
            val commitsBeforePhoneVariant = input.committedValues.size
            dispatchLongPressThroughInputDispatcher(instrumentation, pickerRoot, phoneVariantCell)
            Thread.sleep(1_200)
            instrumentation.waitForIdleSync()
            Thread.sleep(100)
            lateinit var phonePopupRoot: View
            instrumentation.runOnMainSync {
                val holder = currentBody.getChildViewHolder(phoneVariantCell)
                val controller = holder.javaClass.getDeclaredField("emojiPickerPopupViewController").apply {
                    isAccessible = true
                }.get(holder)
                val popupWindow = controller.javaClass.getDeclaredField("popupWindow").apply {
                    isAccessible = true
                }.get(controller) as android.widget.PopupWindow
                assertTrue(popupWindow.isShowing)
                assertTrue(
                    controller.javaClass.getDeclaredField("clickedEmojiView").apply { isAccessible = true }
                        .get(controller) === phoneVariantCell,
                )
                val popup = popupWindow.contentView
                phonePopupRoot = popup
                val targetEmoji = emojiValue(phoneVariantCell)
                val variant = emojiViews(popup).first { emojiValue(it) != targetEmoji }
                phoneVariantEmoji = emojiValue(variant)
                phoneVariantToSelect = variant
            }
            dispatchTapThroughInputDispatcher(instrumentation, phonePopupRoot, phoneVariantToSelect)
            instrumentation.waitForIdleSync()
            Thread.sleep(100)
            assertEquals(commitsBeforePhoneVariant + 1, input.committedValues.size)
            assertEquals(phoneVariantEmoji, input.committedValues.last())

            var replacementBody: RecyclerView? = null
            scenario.onActivity {
                findEmojiPicker(imeRoot)?.emojiGridColumns = 8
            }
            repeat(20) {
                instrumentation.waitForIdleSync()
                Thread.sleep(50)
                scenario.onActivity {
                    imeRoot.findViewById<RecyclerView>(androidx.emoji2.emojipicker.R.id.emoji_picker_body)
                        ?.takeIf { it !== currentBody && it.childCount > 0 }?.let { replacementBody = it }
                }
                if (replacementBody != null) return@repeat
            }
            val newBody = requireNotNull(replacementBody)
            instrumentation.runOnMainSync {
                emojiViews(newBody).forEach { replacementCell ->
                    assertEquals(
                        emojiPickerCellTranslationX(
                            newBody.width, newBody.paddingLeft, newBody.paddingRight,
                            replacementCell.width,
                        ),
                        replacementCell.translationX,
                        0.01f,
                    )
                }
            }
            scenario.onActivity { service.onDestroy() }
        }
    }

    @Test
    fun nativeLayoutMovesBodyFromPhoneToWideContentBounds() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val context = instrumentation.targetContext
            val keyboard = KeyboardView(context)
            val body = RecyclerView(context)
            val content = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                addView(body, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 200))
            }
            val density = context.resources.displayMetrics.density
            val phoneWidth = (412 * density).roundToInt()
            val wideWidth = (840 * density).roundToInt()

            val phone = keyboard.emojiLayerHorizontalGeometryForWidth(phoneWidth)
            assertTrue(applyEmojiPickerBodyHorizontalLayout(content, body, phoneWidth, phone))
            content.measure(exact(phoneWidth), exact(200))
            content.layout(0, 0, phoneWidth, 200)
            assertEquals(phone.railRight, body.left)
            assertEquals(phone.bodyWidth, body.width)
            assertEquals(phone.contentRight, body.right)

            val wide = keyboard.emojiLayerHorizontalGeometryForWidth(wideWidth)
            assertTrue(applyEmojiPickerBodyHorizontalLayout(content, body, wideWidth, wide))
            content.measure(exact(wideWidth), exact(200))
            content.layout(0, 0, wideWidth, 200)
            assertEquals(wide.railRight, body.left)
            assertEquals(wide.bodyWidth, body.width)
            assertEquals(wide.contentRight, body.right)
        }
    }

    @Test
    fun syntheticWideSevenColumnCellTranslationsKeepCategoryHeaderAndBodyBounds() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val context = instrumentation.targetContext
            val body = RecyclerView(context)
            val header = View(context)
            val content = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                addView(header, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 32))
                addView(body, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 200))
            }
            val width = 1_499
            val geometry = emojiLayerHorizontalGeometry(
                totalWidth = width,
                density = context.resources.displayMetrics.density,
                dualKana = true,
            )
            assertTrue(applyEmojiPickerBodyHorizontalLayout(content, body, width, geometry))
            content.measure(exact(width), exact(232)); content.layout(0, 0, width, 232)
            val headerLeft = header.left
            val headerRight = header.right

            val emojiViewWidth = 170
            val translation = emojiPickerCellTranslationX(
                bodyWidth = body.width,
                bodyPaddingLeft = body.paddingLeft,
                bodyPaddingRight = body.paddingRight,
                emojiViewWidth = emojiViewWidth,
            )

            assertEquals(headerLeft, header.left)
            assertEquals(headerRight, header.right)
            assertEquals(geometry.railRight, body.left)
            assertEquals(geometry.contentRight, body.right)
            assertEquals(geometry.bodyWidth, body.width)
            assertEquals((geometry.bodyWidth / 7f - emojiViewWidth) / 2f, translation, 0.001f)

            val cells = List(7) { View(context).apply { layout(0, 0, emojiViewWidth, emojiViewWidth) } }
            applyEmojiPickerCellTranslations(body, cells)
            cells.forEach { assertEquals(translation, it.translationX, 0.001f) }
        }
    }

    private fun exact(size: Int) = View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY)

    private fun emojiViews(root: View): List<View> {
        val result = mutableListOf<View>()
        fun collect(view: View) {
            if (view.javaClass.name == "androidx.emoji2.emojipicker.EmojiView") result += view
            if (view is ViewGroup) repeat(view.childCount) { collect(view.getChildAt(it)) }
        }
        collect(root)
        return result
    }

    private fun findEmojiPicker(root: View): EmojiPickerView? {
        if (root is EmojiPickerView) return root
        if (root !is ViewGroup) return null
        repeat(root.childCount) { index -> findEmojiPicker(root.getChildAt(index))?.let { return it } }
        return null
    }

    private fun emojiValue(view: View): String = requireNotNull(
        view.javaClass.getMethod("getEmoji").invoke(view),
    ).toString()

    private data class ScreenCoordinates(val x: Float, val y: Float)

    /**
     * Sends the translated cell's screen centre through Android's InputDispatcher.  This avoids a
     * cell-local callback and exercises window, RecyclerView, and child hit testing after
     * [View.translationX] has changed the AndroidX cell's visual position.
     */
    private fun screenCoordinates(
        instrumentation: android.app.Instrumentation,
        root: View,
        target: View,
    ): ScreenCoordinates {
        lateinit var result: ScreenCoordinates
        instrumentation.runOnMainSync {
            val rootLocation = IntArray(2).also(root::getLocationOnScreen)
            val targetBounds = Rect()
            check(target.getGlobalVisibleRect(targetBounds))
            result = ScreenCoordinates(
                x = rootLocation[0] + (targetBounds.exactCenterX() - rootLocation[0]),
                y = rootLocation[1] + (targetBounds.exactCenterY() - rootLocation[1]),
            )
        }
        return result
    }

    private fun dispatchTapThroughInputDispatcher(
        instrumentation: android.app.Instrumentation,
        root: View,
        target: View,
    ) {
        val coordinates = screenCoordinates(instrumentation, root, target)
        val downTime = android.os.SystemClock.uptimeMillis()
        sendScreenEvent(instrumentation, MotionEvent.ACTION_DOWN, coordinates, downTime, downTime)
        Thread.sleep(30)
        sendScreenEvent(
            instrumentation, MotionEvent.ACTION_UP, coordinates, downTime, android.os.SystemClock.uptimeMillis(),
        )
    }

    private fun dispatchLongPressThroughInputDispatcher(
        instrumentation: android.app.Instrumentation,
        root: View,
        target: View,
    ) {
        val coordinates = screenCoordinates(instrumentation, root, target)
        val x = coordinates.x.roundToInt()
        val y = coordinates.y.roundToInt()
        // `input swipe` keeps a touchscreen pointer at this translated screen coordinate for the
        // requested duration, so Android's normal long-press timer and popup anchor are used.
        instrumentation.uiAutomation.executeShellCommand("input swipe $x $y $x $y 1000").close()
    }

    private fun sendScreenEvent(
        instrumentation: android.app.Instrumentation,
        action: Int,
        coordinates: ScreenCoordinates,
        downTime: Long,
        eventTime: Long,
    ) {
        MotionEvent.obtain(downTime, eventTime, action, coordinates.x, coordinates.y, 0).also { event ->
            event.source = android.view.InputDevice.SOURCE_TOUCHSCREEN
            instrumentation.uiAutomation.injectInputEvent(event, true)
            event.recycle()
        }
    }

    private class AttachedImeService : ImeService() {
        fun attachForTest(context: Context) = attachBaseContext(context)
    }

    private class RecordingConnection(view: View) : BaseInputConnection(view, true) {
        val committedValues = mutableListOf<String>()
        override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
            committedValues += text?.toString().orEmpty()
            return true
        }
    }

    private object NoopConversion : ConversionEngine {
        override suspend fun start(reading: String) = ConversionState(reading, emptyList(), -1)
        override suspend fun update(reading: String) = start(reading)
        override suspend fun nextCandidate() = start("")
        override suspend fun commit(index: Int): ConversionCommit? = null
        override suspend fun reset() = Unit
    }
}
