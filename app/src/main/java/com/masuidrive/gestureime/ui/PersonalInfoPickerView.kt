package com.masuidrive.gestureime.ui

import android.content.Context
import android.content.res.Configuration
import android.graphics.drawable.GradientDrawable
import android.graphics.RectF
import android.text.TextUtils
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.MotionEvent
import android.view.HapticFeedbackConstants
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.masuidrive.gestureime.R
import com.masuidrive.gestureime.ImePreferences
import com.masuidrive.gestureime.keyboard.KeyAction
import com.masuidrive.gestureime.keyboard.Direction
import com.masuidrive.gestureime.keyboard.GestureInterpreter
import com.masuidrive.gestureime.keyboard.GestureUpdate
import com.masuidrive.gestureime.keyboard.Modifier
import com.masuidrive.gestureime.keyboard.KeyboardMode
import com.masuidrive.gestureime.keyboard.KeyboardLayouts
import com.masuidrive.gestureime.keyboard.KeyboardPopupController
import com.masuidrive.gestureime.keyboard.KeySpec
import com.masuidrive.gestureime.keyboard.FlickValue
import com.masuidrive.gestureime.keyboard.keyboardContentHorizontalGeometry
import androidx.core.view.ViewCompat
import kotlin.math.max
import kotlin.math.min

/** Fixed-height, scrollable picker of saved values. Labels are for accessibility only. */
class PersonalInfoPickerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : LinearLayout(context, attrs) {
    private var snapshot = CandidateUiSnapshot(0L, emptyList())
    private var onCandidateSelected: ((CandidateUiEvent) -> Unit)? = null
    private var onEditAction: ((KeyAction) -> Unit)? = null
    private var onMoreRequested: (() -> Unit)? = null
    private var layerMode = KeyboardMode.QWERTY
    private val punctuationPopupController = KeyboardPopupController(context)
    private val punctuationPreviewSpec = KeyboardLayouts.layout(KeyboardMode.VOICE).rows.last()
        .keys.first { it.id == "voice-punct" }
    private lateinit var layerKey: TextView
    private lateinit var moreKey: TextView
    private val flow = ValueFlowLayout(context)
    private val footer = editRow()
    private val scroll = ScrollView(context).apply {
        isFillViewport = true
        addView(flow, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }

    init {
        orientation = VERTICAL
        setPadding(0, dp(8), 0, dp(5))
        addView(scroll, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
        addView(footer, LayoutParams(LayoutParams.MATCH_PARENT,
            dp(ImePreferences.getKeyboardHeightPreset(context).rowPitchDp.toInt())))
        applyTheme()
    }

    fun setOnCandidateSelected(listener: (CandidateUiEvent) -> Unit) {
        onCandidateSelected = listener
    }

    fun setOnEditAction(listener: (KeyAction) -> Unit) { onEditAction = listener }

    fun setOnMoreRequested(listener: () -> Unit) { onMoreRequested = listener }

    fun setMoreVisible(visible: Boolean) {
        // Keep the slot so the editing keys do not shift when More is opened.
        moreKey.visibility = if (visible) View.VISIBLE else View.INVISIBLE
    }

    fun setLayerMode(mode: KeyboardMode) {
        layerMode = mode
        layerKey.text = layerLabel(mode)
        layerKey.contentDescription = "タップで${mode.displayName}へ戻る。左フリックで音声、上フリックで日本語、右フリックでQWERTY、下フリックでテンキー"
    }

    /** Keep the footer at the same effective row pitch as the keyboard after width changes. */
    fun setFooterRowPitch(pitchPx: Int) {
        if (footer.layoutParams.height != pitchPx) {
            footer.layoutParams = footer.layoutParams.apply { height = pitchPx }
        }
        val keyHeight = (pitchPx - dp(10)).coerceAtLeast(1)
        for (index in 0 until footer.childCount) {
            val key = footer.getChildAt(index)
            if (key !is TextView || key.layoutParams.height == keyHeight) continue
            key.layoutParams = key.layoutParams.apply { height = keyHeight }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val inset = keyboardContentHorizontalGeometry(width, resources.displayMetrics.density).left.toInt()
        if (footer.paddingLeft != inset || footer.paddingRight != inset) {
            footer.setPadding(inset, 0, inset, 0)
        }
        val valueInset = inset + dp(3)
        if (flow.paddingLeft != valueInset || flow.paddingRight != valueInset) {
            flow.setPadding(valueInset, 0, valueInset, 0)
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    private fun layerLabel(mode: KeyboardMode): String = when (mode) {
        KeyboardMode.KANA -> "あん"
        KeyboardMode.NUMBERS -> "19"
        KeyboardMode.EMOJI -> "☺"
        KeyboardMode.QWERTY -> "Az"
        KeyboardMode.SYMBOLS -> "?}"
        KeyboardMode.VOICE -> "Az"
    }

    private fun layerPreviewSpec(): KeySpec = KeyboardLayouts.layout(KeyboardMode.KANA).rows.last().keys.first().copy(
        center = FlickValue(layerLabel(layerMode), KeyAction.SwitchLayer(layerMode)),
    )

    private fun showFlickPreview(key: View, direction: Direction, spec: KeySpec) {
        val anchorLocation = IntArray(2).also(::getLocationOnScreen)
        val keyLocation = IntArray(2).also(key::getLocationOnScreen)
        val left = (keyLocation[0] - anchorLocation[0]).toFloat()
        val top = (keyLocation[1] - anchorLocation[1]).toFloat()
        punctuationPopupController.show(this, RectF(left, top, left + key.width, top + key.height),
            spec, direction)
    }

    private fun editRow() = LinearLayout(context).apply {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        fun addKey(label: String, action: KeyAction, widthUnits: Float = 1f): TextView {
            val key = TextView(context).apply {
                text = label
                contentDescription = label
                gravity = Gravity.CENTER
                textSize = 16f
                setTextColor(context.getColor(R.color.keyboard_text))
                background = GradientDrawable().apply {
                    cornerRadius = dp(7).toFloat()
                    setColor(context.getColor(R.color.keyboard_key))
                }
                isClickable = true
                isFocusable = true
                setOnClickListener { onEditAction?.invoke(action) }
            }
            addView(key, LayoutParams(0, dp(ImePreferences.getKeyboardHeightPreset(context).rowPitchDp.toInt() - 10), widthUnits).apply {
                marginStart = dp(3)
                marginEnd = dp(3)
            })
            return key
        }
        fun TextView.withFlickActions(
            actions: Map<Direction, KeyAction>,
            trackpad: Boolean = false,
            previewSpec: (() -> KeySpec?)? = null,
        ) {
            val accessibleActions = if (trackpad) mapOf(
                Direction.LEFT to KeyAction.MoveCursor(Direction.LEFT),
                Direction.UP to KeyAction.MoveCursor(Direction.UP),
                Direction.RIGHT to KeyAction.MoveCursor(Direction.RIGHT),
                Direction.DOWN to KeyAction.MoveCursor(Direction.DOWN),
            ) else actions
            accessibleActions.forEach { (direction, action) ->
                ViewCompat.addAccessibilityAction(this,
                    "${when (direction) {
                        Direction.LEFT -> "左"
                        Direction.UP -> "上"
                        Direction.RIGHT -> "右"
                        Direction.DOWN -> "下"
                        Direction.CENTER -> "タップ"
                    }}フリック") { _, _ ->
                    onEditAction?.invoke(action)
                    true
                }
            }
            setOnTouchListener(object : OnTouchListener {
                val interpreter = GestureInterpreter()
                var direction = Direction.CENTER
                var cursorMoved = false
                val density = resources.displayMetrics.density
                fun updatePreview(view: View) {
                    previewSpec?.invoke()?.let { showFlickPreview(view, direction, it) }
                }
                fun update(event: MotionEvent) {
                    when (val result = interpreter.move(event.getPointerId(0), event.x / density, event.y / density)) {
                        is GestureUpdate.Selection -> {
                            val next = result.direction.takeIf { it == Direction.CENTER || actions.containsKey(it) }
                                ?: Direction.CENTER
                            if (next != direction) {
                                direction = next
                                updatePreview(this@withFlickActions)
                                if (next != Direction.CENTER) performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            }
                        }
                        is GestureUpdate.CursorDelta -> {
                            cursorMoved = true
                            onEditAction?.invoke(KeyAction.MoveCursor(result.direction, result.units))
                            performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        }
                        null -> Unit
                    }
                }
                override fun onTouch(view: View, event: MotionEvent): Boolean {
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            val sensitivity = if (layerMode == KeyboardMode.KANA || layerMode == KeyboardMode.NUMBERS) {
                                ImePreferences.getKanaNumberFlickSensitivity(context)
                            } else ImePreferences.getQwertySymbolFlickSensitivity(context)
                            interpreter.start(event.getPointerId(0), event.x / density, event.y / density,
                                trackpad = trackpad, pointerThresholds = sensitivity.thresholds())
                            direction = Direction.CENTER
                            cursorMoved = false
                            view.isPressed = true
                            updatePreview(view)
                        }
                        MotionEvent.ACTION_MOVE -> update(event)
                        MotionEvent.ACTION_UP -> {
                            view.isPressed = false
                            update(event)
                            if (previewSpec != null) punctuationPopupController.dismiss()
                            interpreter.finish(event.getPointerId(0))
                            if (!cursorMoved) {
                                if (direction == Direction.CENTER) view.performClick()
                                else actions[direction]?.let { onEditAction?.invoke(it) }
                            }
                        }
                        MotionEvent.ACTION_CANCEL -> {
                            view.isPressed = false
                            if (previewSpec != null) punctuationPopupController.dismiss()
                            interpreter.cancelAll()
                        }
                    }
                    return true
                }
            })
        }
        layerKey = addKey("Az", KeyAction.SwitchLayer(KeyboardMode.QWERTY)).apply {
            setOnClickListener { onEditAction?.invoke(KeyAction.SwitchLayer(layerMode)) }
            withFlickActions(mapOf(
                Direction.UP to KeyAction.SwitchLayer(KeyboardMode.KANA),
                Direction.RIGHT to KeyAction.SwitchLayer(KeyboardMode.QWERTY),
                Direction.DOWN to KeyAction.SwitchLayer(KeyboardMode.NUMBERS),
                Direction.LEFT to KeyAction.VoiceHold,
            ), previewSpec = ::layerPreviewSpec)
        }
        moreKey = addKey("more..", KeyAction.Enter).apply {
            textSize = 12f
            contentDescription = "その他の個人情報を表示"
            setOnClickListener { onMoreRequested?.invoke() }
            withFlickActions(emptyMap())
            visibility = View.INVISIBLE
        }
        addView(View(context), LayoutParams(0, 1, 1f))
        val punctuation = addKey("、。？！", KeyAction.CommitText("、"))
        punctuation.contentDescription = "タップで、。左フリックで。上フリックで？右フリックで！"
        punctuation.withFlickActions(mapOf(
            Direction.LEFT to KeyAction.CommitText("。"), Direction.UP to KeyAction.CommitText("？"),
            Direction.RIGHT to KeyAction.CommitText("！"), Direction.DOWN to KeyAction.CommitText("、"),
        ), previewSpec = { punctuationPreviewSpec })
        addKey("Space", KeyAction.CommitText(" "), widthUnits = 2f).apply {
            contentDescription = "Space。上下左右フリックでカーソル移動"
            withFlickActions(emptyMap(), trackpad = true)
        }
        addKey("⌫", KeyAction.Backspace()).apply {
            contentDescription = "削除"
            withFlickActions(emptyMap())
        }
        addKey("Enter", KeyAction.Enter).apply {
            contentDescription = "Enter。上フリックでpaste、下フリックでC-j"
            withFlickActions(mapOf(Direction.UP to KeyAction.Paste,
                Direction.DOWN to KeyAction.ModifiedKey("j", Modifier.CTRL)))
        }
    }

    fun showCandidates(next: CandidateUiSnapshot) {
        snapshot = next
        render()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applyTheme()
        punctuationPopupController.refreshTheme()
        render()
    }

    override fun onDetachedFromWindow() {
        punctuationPopupController.dismiss()
        super.onDetachedFromWindow()
    }

    private fun render() {
        flow.removeAllViews()
        snapshot.candidates.forEachIndexed { index, value ->
            val label = snapshot.labels.getOrNull(index) ?: value
            flow.addView(card(value, "個人情報 ${index + 1}: $label").apply {
                setOnClickListener { onCandidateSelected?.invoke(CandidateUiEvent(snapshot.token, index)) }
            })
        }
        scroll.scrollTo(0, 0)
    }

    private fun card(text: String, description: String) = TextView(context).apply {
        this.text = text.replace('\n', ' ')
        contentDescription = description
        gravity = Gravity.CENTER
        setPadding(dp(12), 0, dp(12), 0)
        setTextColor(context.getColor(R.color.keyboard_text))
        textSize = 16f
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(7).toFloat()
            setColor(context.getColor(R.color.candidate_background))
        }
        isClickable = true
        isFocusable = true
    }

    private fun applyTheme() {
        setBackgroundColor(context.getColor(R.color.keyboard_background))
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private class ValueFlowLayout(context: Context) : ViewGroup(context) {
        private val gap = (6 * resources.displayMetrics.density).toInt()
        private val chipHeight = (44 * resources.displayMetrics.density).toInt()
        private val minimumChipWidth = (64 * resources.displayMetrics.density).toInt()

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            val width = MeasureSpec.getSize(widthMeasureSpec)
            val available = max(1, width - paddingLeft - paddingRight)
            var x = 0
            var y = 0
            for (index in 0 until childCount) {
                val child = getChildAt(index) as TextView
                val desired = max(minimumChipWidth, child.paint.measureText(child.text.toString()).toInt() + child.paddingLeft + child.paddingRight)
                val chipWidth = min(available, desired)
                if (x > 0 && x + chipWidth > available) { x = 0; y += chipHeight + gap }
                child.measure(MeasureSpec.makeMeasureSpec(chipWidth, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(chipHeight, MeasureSpec.EXACTLY))
                x += chipWidth + gap
            }
            val height = y + if (childCount == 0) 0 else chipHeight
            setMeasuredDimension(resolveSize(width, widthMeasureSpec), resolveSize(height, heightMeasureSpec))
        }

        override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
            val available = right - left - paddingLeft - paddingRight
            var x = paddingLeft
            var y = paddingTop
            for (index in 0 until childCount) {
                val child = getChildAt(index)
                if (x > paddingLeft && x + child.measuredWidth > paddingLeft + available) {
                    x = paddingLeft
                    y += chipHeight + gap
                }
                child.layout(x, y, x + child.measuredWidth, y + chipHeight)
                x += child.measuredWidth + gap
            }
        }
    }
}
