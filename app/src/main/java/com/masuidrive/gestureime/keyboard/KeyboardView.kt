package com.masuidrive.gestureime.keyboard

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.provider.Settings
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.animation.PathInterpolator
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.customview.widget.ExploreByTouchHelper
import kotlin.math.min

class KeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    companion object {
        const val LABEL_ANIMATION_MS = 90L
        const val ACCENT_DELAY_MS = 450L
        const val DELETE_REPEAT_DELAY_MS = 420L
        const val DELETE_REPEAT_INTERVAL_MS = 65L
        const val DUAL_FLICK_MIN_WIDTH_DP = 600f
        const val VOICE_HOLD_DELAY_MS = 1_000L
        private const val ACTION_FLICK_LEFT = 0x01020001
        private const val ACTION_FLICK_UP = 0x01020002
        private const val ACTION_FLICK_RIGHT = 0x01020003
        private const val ACTION_FLICK_DOWN = 0x01020004
    }

    var actionSink: KeyboardActionSink? = null
    var voiceHoldSink: VoiceHoldSink? = null
    private var state = KeyboardUiState()
    private val density = resources.displayMetrics.density
    private val interpreter = GestureInterpreter()
    private val hitTargets = mutableListOf<HitTarget>()
    private val active = mutableMapOf<Int, HitTarget>()
    private val directions = mutableMapOf<Int, Direction>()
    private val cursorMoved = mutableSetOf<Int>()
    private val accentActive = mutableSetOf<Int>()
    private val accentSelected = mutableMapOf<Int, Int>()
    private val repeated = mutableSetOf<Int>()
    private val timers = mutableMapOf<Int, Runnable>()
    private data class LabelFrame(
        val mainDy: Float = 0f,
        val mainAlpha: Float = 1f,
        val secondaryDy: Float = 0f,
        val secondaryScale: Float = 1f,
        val secondaryAlpha: Float = 1f,
    )
    private val labelFrames = mutableMapOf<Int, LabelFrame>()
    private val labelAnimators = mutableMapOf<Int, ValueAnimator>()
    private var qwertyLabelStyle = QwertyLabelStyle.DEFAULT
    private var previewOnly = false
    private val voiceHoldTimers = mutableMapOf<Int, Runnable>()
    private var voiceHoldOwner: Pair<Int, Long>? = null
    private var voiceHoldRequestId = 0L
    private var voiceHoldMultiPointer = false
    private var voiceReadyRequestId: Long? = null
    private var secondVoiceHaptic: Runnable? = null
    private val popupController = KeyboardPopupController(context)
    private val accessibilityHelper = KeyboardAccessibilityHelper(this)

    private val keyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }

    private data class HitTarget(val spec: KeySpec, val bounds: RectF)

    init {
        isFocusable = true
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        ViewCompat.setAccessibilityDelegate(this, accessibilityHelper)
        ViewCompatInsets.install(this)
    }

    fun setMode(mode: KeyboardMode) {
        if (state.mode == mode) return
        cancelActiveGestures()
        state = state.copy(mode = mode)
        contentDescription = "${mode.displayName}キーボード"
        rebuildLayout()
    }

    fun setModifier(modifier: Modifier?) {
        state = state.copy(pendingModifier = modifier)
        accessibilityHelper.invalidateRoot()
        invalidate()
    }

    fun setCandidates(candidates: List<String>, selectedIndex: Int = -1) {
        state = state.copy(candidates = candidates, selectedCandidateIndex = selectedIndex)
        invalidate()
    }

    fun setConversionActive(active: Boolean) {
        if (state.conversionActive == active) return
        state = state.copy(conversionActive = active)
        rebuildLayout()
    }

    fun setDualFlickEnabled(enabled: Boolean) {
        if (state.dualFlickEnabled == enabled) return
        cancelActiveGestures()
        state = state.copy(dualFlickEnabled = enabled)
        rebuildLayout()
    }

    fun setQwertyLabelStyle(style: QwertyLabelStyle) {
        qwertyLabelStyle = style.sanitized()
        invalidate()
    }

    fun setPreviewOnly(enabled: Boolean) {
        cancelActiveGestures()
        previewOnly = enabled
        isFocusable = !enabled
        importantForAccessibility = if (enabled) IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS else IMPORTANT_FOR_ACCESSIBILITY_YES
        accessibilityHelper.invalidateRoot()
    }

    fun onVoiceRecordingReady(requestId: Long) {
        val owner = voiceHoldOwner ?: return
        if (owner.second != requestId || owner.first !in active || voiceReadyRequestId == requestId) return
        voiceReadyRequestId = requestId
        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        secondVoiceHaptic = Runnable { performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) }.also {
            postDelayed(it, 70L)
        }
    }

    private fun rebuildLayout() {
        if (width > 0 && height > 0) buildHitTargets(paddingTop.toFloat())
        requestLayout()
        accessibilityHelper.invalidateRoot()
        invalidate()
    }

    internal fun updateBottomInset(bottom: Int) {
        if (paddingBottom == bottom) return
        cancelActiveGestures()
        setPadding(paddingLeft, paddingTop, paddingRight, bottom)
        rebuildLayout()
    }

    fun cancelActiveGestures() {
        voiceHoldOwner?.second?.let { voiceHoldSink?.onVoiceHold(VoiceHoldEvent.Cancel(it)) }
        voiceHoldOwner = null
        voiceReadyRequestId = null
        voiceHoldTimers.values.forEach(::removeCallbacks)
        voiceHoldTimers.clear()
        secondVoiceHaptic?.let(::removeCallbacks)
        secondVoiceHaptic = null
        voiceHoldMultiPointer = false
        timers.values.forEach(::removeCallbacks)
        timers.clear()
        active.clear()
        directions.clear()
        cursorMoved.clear()
        accentActive.clear()
        accentSelected.clear()
        dismissPopup()
        repeated.clear()
        interpreter.cancelAll()
        if (state.pendingModifier != null) {
            state = state.copy(pendingModifier = null)
            actionSink?.onKeyAction(KeyAction.SetModifier(null))
        }
        invalidate()
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        if (!hasWindowFocus) cancelActiveGestures()
        super.onWindowFocusChanged(hasWindowFocus)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        if (oldw != 0 && (w != oldw || h != oldh)) cancelActiveGestures()
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) buildHitTargets(paddingTop.toFloat())
        accessibilityHelper.invalidateRoot()
    }

    override fun onDetachedFromWindow() {
        cancelActiveGestures()
        labelAnimators.values.forEach(ValueAnimator::cancel)
        labelAnimators.clear()
        labelFrames.clear()
        dismissPopup()
        super.onDetachedFromWindow()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val wanted = (dp(rowPitchDp(width / density)) * 4 + dp(8f) + paddingTop + paddingBottom).toInt()
        setMeasuredDimension(width, resolveSize(wanted, heightMeasureSpec))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.rgb(41, 41, 44))
        hitTargets.forEach { target ->
            if (target.spec.kind == KeyKind.EMPTY) return@forEach
            drawKey(canvas, target, active.entries.firstOrNull { it.value == target }?.key)
        }
    }

    private fun buildHitTargets(top: Float) {
        hitTargets.clear()
        val keyboardTop = top + dp(8f)
        val dualKana = state.dualFlickEnabled && width / density >= DUAL_FLICK_MIN_WIDTH_DP
        val rows = KeyboardLayouts.layout(state.mode, dualKana, state.conversionActive).rows
        val rowPitch = min((height - keyboardTop - paddingBottom) / 4f, dp(rowPitchDp(width / density)))
        val rowGap = dp(if (state.mode in setOf(KeyboardMode.QWERTY, KeyboardMode.SYMBOLS)) 10f else 6f)
        val sharedUnits = rows.maxOf { row -> row.keys.sumOf { it.widthUnits.toDouble() }.toFloat() }
        val keyboardInset = dp(if (width / density >= DUAL_FLICK_MIN_WIDTH_DP) 10f else 3f)
        val contentLeft = paddingLeft + keyboardInset
        val contentWidth = width - paddingLeft - paddingRight - keyboardInset * 2
        rows.forEachIndexed { rowIndex, row ->
            val layoutUnits = if (state.mode in setOf(KeyboardMode.QWERTY, KeyboardMode.SYMBOLS)) {
                row.keys.sumOf { it.widthUnits.toDouble() }.toFloat()
            } else sharedUnits
            val unit = contentWidth / layoutUnits
            var x = contentLeft
            row.keys.forEach { key ->
                val right = x + unit * key.widthUnits
                val keyTop = keyboardTop + rowPitch * rowIndex
                val bottom = min(height - paddingBottom.toFloat(), keyTop + rowPitch * key.rowSpan - rowGap)
                hitTargets += HitTarget(key, RectF(x + dp(3f), keyTop, right - dp(3f), bottom))
                x = right
            }
        }
    }

    private fun rowPitchDp(widthDp: Float) = when {
        widthDp >= DUAL_FLICK_MIN_WIDTH_DP && state.mode in setOf(KeyboardMode.QWERTY, KeyboardMode.SYMBOLS) -> 62f
        widthDp >= DUAL_FLICK_MIN_WIDTH_DP && state.mode == KeyboardMode.KANA -> 58f
        widthDp >= DUAL_FLICK_MIN_WIDTH_DP -> 64f
        state.mode in setOf(KeyboardMode.QWERTY, KeyboardMode.SYMBOLS) -> 55f
        state.mode == KeyboardMode.KANA -> 51f
        else -> 57f
    }

    private fun drawKey(canvas: Canvas, target: HitTarget, pointerId: Int?) {
        val selected = pointerId != null
        val modifierActive = target.spec.kind == KeyKind.MODIFIER && state.pendingModifier != null
        val faceColor = when { selected || modifierActive -> Color.rgb(168, 206, 255); target.spec.dark -> Color.rgb(48, 48, 52); else -> Color.rgb(65, 65, 68) }
        keyPaint.color = Color.rgb(20, 20, 22)
        canvas.drawRoundRect(RectF(target.bounds).apply { offset(0f, dp(1f)) }, dp(5f), dp(5f), keyPaint)
        keyPaint.color = faceColor
        keyPaint.alpha = 255
        canvas.drawRoundRect(target.bounds, dp(5f), dp(5f), keyPaint)
        val textSave = canvas.save()
        canvas.clipRect(target.bounds)
        textPaint.color = if (selected || modifierActive) Color.rgb(16, 40, 68) else Color.rgb(244, 244, 246)
        textPaint.alpha = 255
        val primaryAdjustment = labelAdjustment(target.spec, secondary = false)
        textPaint.textSize = sp(mainTextSize(target.spec)) * primaryAdjustment.scale
        val direction = pointerId?.let { directions[it] } ?: Direction.CENTER
        val frame = pointerId?.let { labelFrames[it] } ?: LabelFrame()
        val spec = target.spec
        val label = when {
            spec.kind == KeyKind.MODIFIER && state.pendingModifier != null -> if (state.pendingModifier == Modifier.ALT) "A" else "C"
            spec.kind == KeyKind.BACKSPACE && spec.center != null -> spec.center.label
            selected && direction != Direction.CENTER -> spec.value(direction)?.label
            else -> spec.center?.label ?: modifierLabel(spec)
        } ?: ""
        val centerY = target.bounds.centerY() - (textPaint.ascent() + textPaint.descent()) / 2
        val secondary = when {
            spec.kind == KeyKind.ENTER && !state.conversionActive -> "paste"
            spec.kind == KeyKind.SPACE && state.mode == KeyboardMode.QWERTY -> "←↓↑→"
            spec.kind == KeyKind.CHARACTER -> spec.down?.label
            else -> null
        }
        val downLike = direction == Direction.DOWN || frame.secondaryScale > 1.001f || frame.mainDy > 0.001f
        val upLike = direction == Direction.UP || frame.mainDy < -0.001f || frame.secondaryAlpha < .999f
        val animatedEnglish = selected && state.mode == KeyboardMode.QWERTY && spec.kind == KeyKind.CHARACTER &&
            secondary != null && (downLike || upLike)
        val animatedEnterPaste = selected && spec.kind == KeyKind.ENTER &&
            downLike && secondary != null
        val animatedSpecial = selected && direction != Direction.CENTER &&
            spec.kind in setOf(KeyKind.SPACE, KeyKind.MODIFIER)
        val idleModifier = spec.kind == KeyKind.MODIFIER && state.pendingModifier == null && direction == Direction.CENTER
        val idleMainBaseline = when {
            state.mode == KeyboardMode.QWERTY && spec.kind == KeyKind.CHARACTER && secondary != null ->
                baselineAtVisualCenter(target.bounds.centerY() + dp(5f + primaryAdjustment.yOffsetDp))
            spec.kind == KeyKind.ENTER && !state.conversionActive ->
                baselineAtVisualCenter(target.bounds.centerY() + dp(6.5f + primaryAdjustment.yOffsetDp))
            spec.kind == KeyKind.SPACE && state.mode == KeyboardMode.QWERTY ->
                baselineAtVisualCenter(target.bounds.centerY() + dp(6.5f + primaryAdjustment.yOffsetDp))
            else -> centerY + dp(primaryAdjustment.yOffsetDp)
        }
        if (spec.id == "mode-↔" && !selected) {
            drawCursorCross(canvas, target.bounds)
        } else if (idleModifier) {
            textPaint.textSize = sp(10f) * primaryAdjustment.scale
            val cX = safeCenterX(target.bounds, target.bounds.centerX() + dp(primaryAdjustment.xOffsetDp), "C")
            val aX = safeCenterX(target.bounds, target.bounds.centerX() + dp(primaryAdjustment.xOffsetDp), "A")
            canvas.drawText("C", cX, safeBaseline(target.bounds, target.bounds.top + dp(13f + primaryAdjustment.yOffsetDp)), textPaint)
            canvas.drawText("A", aX, safeBaseline(target.bounds, target.bounds.bottom - dp(6f - primaryAdjustment.yOffsetDp)), textPaint)
        } else if (!animatedEnglish && !animatedEnterPaste && !animatedSpecial) {
            drawMainLabel(canvas, label, target.bounds, safeBaseline(target.bounds, idleMainBaseline), direction == Direction.CENTER, primaryAdjustment.xOffsetDp)
        }
        if (animatedEnglish) {
            val secondaryLabel = requireNotNull(secondary)
            val secondaryAdjustment = labelAdjustment(spec, secondary = true)
            textPaint.textSize = sp(11f) * secondaryAdjustment.scale * frame.secondaryScale
            val baseline = baselineAtVisualCenter(target.bounds.top + dp(9f + frame.secondaryDy + secondaryAdjustment.yOffsetDp))
            textPaint.color = Color.rgb(16, 40, 68)
            textPaint.alpha = (255 * frame.secondaryAlpha).toInt()
            canvas.drawText(secondaryLabel, target.bounds.centerX() + dp(secondaryAdjustment.xOffsetDp), baseline, textPaint)
            textPaint.textSize = sp(22f) * primaryAdjustment.scale
            textPaint.alpha = (255 * frame.mainAlpha).toInt()
            val mainBaseline = baselineAtVisualCenter(target.bounds.centerY() + dp(5f + frame.mainDy + primaryAdjustment.yOffsetDp))
            val main = if (direction == Direction.UP) spec.up?.label ?: label else spec.center?.label.orEmpty()
            drawFittedText(canvas, main, target.bounds.centerX() + dp(primaryAdjustment.xOffsetDp), mainBaseline, availableWidth(target.bounds, primaryAdjustment.xOffsetDp))
        } else if (animatedEnterPaste) {
            drawDownLabelTransition(canvas, target.bounds, spec, spec.center?.label.orEmpty(), secondary, primaryAdjustment, frame)
        } else if (animatedSpecial) {
            val adjustment = labelAdjustment(spec, secondary = direction == Direction.DOWN)
            textPaint.textSize = sp(11f) * adjustment.scale
            val centered = visualCenterBaseline(target.bounds) + dp(adjustment.yOffsetDp)
            textPaint.color = Color.rgb(16, 40, 68)
            drawFittedText(canvas, label, target.bounds.centerX() + dp(adjustment.xOffsetDp), safeBaseline(target.bounds, centered), availableWidth(target.bounds, adjustment.xOffsetDp))
        } else if (secondary != null) {
            val adjustment = labelAdjustment(spec, secondary = true)
            textPaint.textSize = sp(secondaryTextSize(spec)) * adjustment.scale
            textPaint.color = if (selected) Color.rgb(16, 40, 68) else Color.rgb(181, 181, 191)
            val visualCenter = when {
                spec.kind == KeyKind.ENTER -> target.bounds.height() / density / 2f - 10f
                spec.kind == KeyKind.SPACE && state.mode == KeyboardMode.QWERTY ->
                    target.bounds.height() / density / 2f - 10.5f
                state.mode == KeyboardMode.QWERTY && spec.kind == KeyKind.CHARACTER -> 9f
                else -> 9f
            }
            val baseline = safeBaseline(target.bounds, baselineAtVisualCenter(target.bounds.top + dp(visualCenter + adjustment.yOffsetDp)))
            drawFittedText(canvas, secondary, target.bounds.centerX() + dp(adjustment.xOffsetDp), baseline, availableWidth(target.bounds, adjustment.xOffsetDp))
        }
        canvas.restoreToCount(textSave)
    }

    private fun drawDownLabelTransition(
        canvas: Canvas,
        bounds: RectF,
        spec: KeySpec,
        main: String,
        secondary: String,
        primaryAdjustment: LabelAdjustment,
        frame: LabelFrame,
    ) {
        val secondaryAdjustment = labelAdjustment(spec, secondary = true)
        textPaint.textSize = sp(10f) * secondaryAdjustment.scale * frame.secondaryScale
        textPaint.color = Color.rgb(16, 40, 68)
        textPaint.alpha = (255 * frame.secondaryAlpha).toInt()
        val secondaryBaseline = baselineAtVisualCenter(bounds.centerY() + dp(-10f + frame.secondaryDy + secondaryAdjustment.yOffsetDp))
        drawFittedText(canvas, secondary, bounds.centerX() + dp(secondaryAdjustment.xOffsetDp), secondaryBaseline, availableWidth(bounds, secondaryAdjustment.xOffsetDp))
        textPaint.textSize = sp(15f) * primaryAdjustment.scale
        textPaint.alpha = (255 * frame.mainAlpha).toInt()
        val mainBaseline = baselineAtVisualCenter(bounds.centerY() + dp(6.5f + frame.mainDy + primaryAdjustment.yOffsetDp))
        drawFittedText(canvas, main, bounds.centerX() + dp(primaryAdjustment.xOffsetDp), mainBaseline, availableWidth(bounds, primaryAdjustment.xOffsetDp))
        textPaint.alpha = 255
    }

    private fun labelAdjustment(spec: KeySpec, secondary: Boolean): LabelAdjustment {
        if (state.mode != KeyboardMode.QWERTY) return LabelAdjustment()
        val group = when {
            secondary && spec.kind == KeyKind.CHARACTER -> QwertyLabelGroup.LETTER_SECONDARY
            secondary && spec.kind in setOf(KeyKind.SPACE, KeyKind.ENTER) -> QwertyLabelGroup.SPACE_ENTER_SECONDARY
            !secondary && spec.kind == KeyKind.CHARACTER -> QwertyLabelGroup.LETTER_PRIMARY
            !secondary && spec.kind in setOf(KeyKind.SPACE, KeyKind.ENTER) -> QwertyLabelGroup.SPACE_ENTER_PRIMARY
            else -> QwertyLabelGroup.COMPOSITE_SMALL
        }
        return qwertyLabelStyle[group]
    }

    private fun availableWidth(bounds: RectF, xOffsetDp: Float): Float =
        (bounds.width() - dp(8f) - 2f * kotlin.math.abs(dp(xOffsetDp))).coerceAtLeast(dp(4f))

    private fun safeBaseline(bounds: RectF, desired: Float): Float {
        val metrics = textPaint.fontMetrics
        val minimum = bounds.top + dp(3f) - metrics.top
        val maximum = bounds.bottom - dp(3f) - metrics.bottom
        return if (minimum <= maximum) desired.coerceIn(minimum, maximum) else bounds.centerY()
    }

    private fun safeCenterX(bounds: RectF, desired: Float, label: String): Float {
        val half = textPaint.measureText(label) / 2f
        val minimum = bounds.left + dp(3f) + half
        val maximum = bounds.right - dp(3f) - half
        return if (minimum <= maximum) desired.coerceIn(minimum, maximum) else bounds.centerX()
    }

    private fun visualCenterBaseline(bounds: RectF) =
        bounds.centerY() - (textPaint.ascent() + textPaint.descent()) / 2f

    private fun baselineAtVisualCenter(centerY: Float) =
        centerY - (textPaint.ascent() + textPaint.descent()) / 2f

    private fun mainTextSize(spec: KeySpec) = when {
        spec.kind == KeyKind.ENTER -> 15f
        spec.kind in setOf(KeyKind.SPACE, KeyKind.MODE, KeyKind.LAYER_SWITCH) -> 16f
        spec.kind == KeyKind.MODIFIER || spec.kind == KeyKind.ACCENT -> 18f
        spec.kind == KeyKind.CURSOR -> 18f
        state.mode == KeyboardMode.QWERTY && (spec.up != null || spec.down != null) -> 22f
        state.mode in setOf(KeyboardMode.QWERTY, KeyboardMode.SYMBOLS) -> 24f
        else -> 25f
    }

    private fun secondaryTextSize(spec: KeySpec) = if (spec.kind == KeyKind.CHARACTER) 11f else 10f

    private fun modifierLabel(spec: KeySpec) = if (spec.kind == KeyKind.MODIFIER) "C/A" else ""

    private fun drawCursorCross(canvas: Canvas, bounds: RectF) {
        textPaint.textSize = sp(13f)
        val x = bounds.centerX()
        val y = bounds.centerY()
        canvas.drawText("↑", x, y - dp(8f), textPaint)
        canvas.drawText("←", x - dp(11f), y + dp(5f), textPaint)
        canvas.drawText("→", x + dp(11f), y + dp(5f), textPaint)
        canvas.drawText("↓", x, y + dp(18f), textPaint)
    }

    private fun drawMainLabel(canvas: Canvas, label: String, bounds: RectF, y: Float, allowComposite: Boolean, xOffsetDp: Float = 0f) {
        val x = bounds.centerX() + dp(xOffsetDp)
        if (!allowComposite || label !in setOf("#!", "あん", "AZ", "19")) {
            drawFittedText(canvas, label, x, y, availableWidth(bounds, xOffsetDp))
            return
        }
        val main = label.substring(0, 1)
        val ghost = label.substring(1)
        val originalSize = textPaint.textSize
        val originalColor = textPaint.color
        val originalAlpha = textPaint.alpha
        val mainVisualCenter = y + (textPaint.ascent() + textPaint.descent()) / 2f
        textPaint.textSize = originalSize * .75f
        if (originalColor != Color.rgb(16, 40, 68)) {
            textPaint.color = Color.rgb(181, 181, 191)
        }
        textPaint.alpha = (originalAlpha * .72f).toInt()
        val ghostLineHeight = textPaint.descent() - textPaint.ascent()
        val ghostCenter = mainVisualCenter + ghostLineHeight * .15f
        val ghostX = safeCenterX(bounds, x + dp(2f) + textPaint.measureText(ghost) / 2f, ghost)
        canvas.drawText(ghost, ghostX, safeBaseline(bounds, baselineAtVisualCenter(ghostCenter)), textPaint)
        textPaint.textSize = originalSize
        textPaint.color = originalColor
        textPaint.alpha = originalAlpha
        canvas.drawText(main, safeCenterX(bounds, x - dp(3f), main), safeBaseline(bounds, y), textPaint)
    }

    private fun drawFittedText(canvas: Canvas, label: String, x: Float, y: Float, maxWidth: Float) {
        val original = textPaint.textSize
        val measured = textPaint.measureText(label)
        if (measured > maxWidth && measured > 0f) textPaint.textSize = original * maxWidth / measured
        canvas.drawText(label, x, y, textPaint)
        textPaint.textSize = original
    }

    private fun isTextPreview(spec: KeySpec): Boolean =
        spec.kind == KeyKind.CHARACTER && spec.center?.action is KeyAction.CommitText

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (previewOnly) return true
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> pointerDown(event, event.actionIndex)
            MotionEvent.ACTION_MOVE -> for (i in 0 until event.pointerCount) pointerMove(event, i)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> pointerUp(event.getPointerId(event.actionIndex))
            MotionEvent.ACTION_CANCEL -> cancelActiveGestures()
        }
        return true
    }

    override fun dispatchHoverEvent(event: MotionEvent): Boolean {
        return accessibilityHelper.dispatchHoverEvent(event) || super.dispatchHoverEvent(event)
    }

    private fun directionLabel(direction: Direction) = when (direction) {
        Direction.CENTER -> "タップ"; Direction.LEFT -> "左"; Direction.UP -> "上"; Direction.RIGHT -> "右"; Direction.DOWN -> "下"
    }

    private fun describe(spec: KeySpec): String = Direction.entries.mapNotNull { direction ->
        spec.value(direction)?.label?.let { label ->
            when (direction) {
                else -> "${directionLabel(direction)} $label"
            }
        }
    }.joinToString("、").ifEmpty { if (spec.kind == KeyKind.MODIFIER) "上 Alt、下 Ctrl" else "入力なし" }

    private inner class KeyboardAccessibilityHelper(host: View) : ExploreByTouchHelper(host) {
        override fun getVirtualViewAt(x: Float, y: Float): Int = hitTargets.indexOfLast {
            it.spec.kind != KeyKind.EMPTY && it.bounds.contains(x, y)
        }.takeIf { it >= 0 } ?: INVALID_ID

        override fun getVisibleVirtualViews(virtualViewIds: MutableList<Int>) {
            hitTargets.indices.filterTo(virtualViewIds) { hitTargets[it].spec.kind != KeyKind.EMPTY }
        }

        override fun onPopulateNodeForVirtualView(virtualViewId: Int, node: AccessibilityNodeInfoCompat) {
            val target = hitTargets.getOrNull(virtualViewId) ?: return
            node.className = "android.widget.Button"
            node.contentDescription = describe(target.spec)
            node.setBoundsInParent(android.graphics.Rect(
                target.bounds.left.toInt(), target.bounds.top.toInt(), target.bounds.right.toInt(), target.bounds.bottom.toInt()))
            target.spec.center?.let {
                node.isClickable = true
                node.addAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CLICK)
            }
            listOf(
                ACTION_FLICK_LEFT to Direction.LEFT,
                ACTION_FLICK_UP to Direction.UP,
                ACTION_FLICK_RIGHT to Direction.RIGHT,
                ACTION_FLICK_DOWN to Direction.DOWN,
            ).forEach { (id, direction) ->
                target.spec.value(direction)?.let {
                    node.addAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat(id, "${directionLabel(direction)} ${it.label}"))
                }
            }
        }

        override fun onPerformActionForVirtualView(virtualViewId: Int, action: Int, arguments: android.os.Bundle?): Boolean {
            if (previewOnly) return false
            val target = hitTargets.getOrNull(virtualViewId) ?: return false
            val direction = when (action) {
                AccessibilityNodeInfoCompat.ACTION_CLICK -> Direction.CENTER
                ACTION_FLICK_LEFT -> Direction.LEFT
                ACTION_FLICK_UP -> Direction.UP
                ACTION_FLICK_RIGHT -> Direction.RIGHT
                ACTION_FLICK_DOWN -> Direction.DOWN
                else -> return false
            }
            if (target.spec.value(direction) == null && !(target.spec.kind == KeyKind.MODIFIER && direction == Direction.CENTER)) return false
            dispatch(target.spec, direction)
            sendEventForVirtualView(virtualViewId, AccessibilityEvent.TYPE_VIEW_CLICKED)
            invalidateVirtualView(virtualViewId)
            return true
        }
    }

    private fun pointerDown(event: MotionEvent, index: Int) {
        val id = event.getPointerId(index)
        val hit = hitTargets.lastOrNull { it.spec.kind != KeyKind.EMPTY && it.bounds.contains(event.getX(index), event.getY(index)) } ?: return
        if (active.isNotEmpty()) {
            voiceHoldMultiPointer = true
            cancelVoiceHoldArms()
        }
        active[id] = hit
        directions[id] = Direction.CENTER
        labelFrames[id] = LabelFrame()
        val verticalOnly = hit.spec.kind == KeyKind.MODIFIER ||
            (hit.spec.kind == KeyKind.BACKSPACE && hit.spec.center == null) ||
            (hit.spec.kind == KeyKind.CHARACTER && (hit.spec.up != null || hit.spec.down != null))
        interpreter.start(id, event.getX(index) / density, event.getY(index) / density,
            trackpad = hit.spec.kind == KeyKind.SPACE, verticalOnly = verticalOnly)
        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        if (hit.spec.kind == KeyKind.CHARACTER || (hit.spec.kind == KeyKind.BACKSPACE && hit.spec.center != null)) {
            scheduleTimer(id, hit, Direction.CENTER)
        }
        if (hit.spec.kind == KeyKind.LAYER_SWITCH && !voiceHoldMultiPointer) scheduleVoiceHold(id, hit)
        sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_HOVER_ENTER)
        syncPopup(id)
        invalidate()
    }

    private fun pointerMove(event: MotionEvent, index: Int) {
        val id = event.getPointerId(index)
        if (voiceHoldOwner?.first == id) return
        if (id in accentActive) {
            val hit = active[id]
            val choices = hit?.spec?.center?.label?.let(::accentChoices)
            if (hit != null && choices != null) {
                accentSelected[id] = popupController.accentIndexFor(this, hit.bounds, choices.size, event.getX(index))
            }
        }
        when (val update = interpreter.move(id, event.getX(index) / density, event.getY(index) / density)) {
            is GestureUpdate.Selection -> {
                if (directions[id] != update.direction) performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                directions[id] = update.direction
                cancelTimer(id)
                if (update.direction != Direction.CENTER) cancelVoiceHoldArm(id)
                if (active[id]?.spec?.down?.action is KeyAction.Backspace && update.direction == Direction.DOWN) {
                    active[id]?.let { scheduleTimer(id, it, Direction.DOWN) }
                }
                animateLabels(id, update.direction)
            }
            is GestureUpdate.CursorDelta -> {
                cursorMoved += id
                if (directions[id] != update.direction) animateLabels(id, update.direction)
                directions[id] = update.direction
                actionSink?.onKeyAction(KeyAction.MoveCursor(update.direction, update.units))
                performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            }
            null -> Unit
        }
        syncPopup(id)
        invalidate()
    }

    private fun pointerUp(id: Int) {
        cancelTimer(id)
        cancelVoiceHoldArm(id)
        val hit = active.remove(id) ?: return
        labelAnimators.remove(id)?.cancel()
        labelFrames.remove(id)
        val voiceOwner = voiceHoldOwner
        if (voiceOwner?.first == id) {
            voiceHoldOwner = null
            voiceReadyRequestId = null
            directions.remove(id)
            interpreter.cancel(id)
            voiceHoldSink?.onVoiceHold(VoiceHoldEvent.End(voiceOwner.second))
            if (active.isEmpty()) voiceHoldMultiPointer = false
            dismissPopup()
            invalidate()
            return
        }
        val direction = directions.remove(id) ?: Direction.CENTER
        interpreter.finish(id)
        val accents = if (accentActive.remove(id)) accentChoices(hit.spec.center?.label.orEmpty()) else null
        if (accents != null) actionSink?.onKeyAction(KeyAction.CommitText(accents[accentSelected.remove(id) ?: 0]))
        else if (!cursorMoved.remove(id) && !repeated.remove(id)) dispatch(hit.spec, direction)
        cursorMoved.remove(id)
        if (active.isEmpty()) voiceHoldMultiPointer = false
        dismissPopup()
        active.keys.firstOrNull()?.let(::syncPopup)
        invalidate()
    }

    private fun scheduleVoiceHold(id: Int, hit: HitTarget) {
        val task = Runnable {
            voiceHoldTimers.remove(id)
            if (voiceHoldMultiPointer || active.size != 1 || active[id] != hit || directions[id] != Direction.CENTER) return@Runnable
            val requestId = ++voiceHoldRequestId
            voiceHoldOwner = id to requestId
            interpreter.cancel(id)
            voiceHoldSink?.onVoiceHold(VoiceHoldEvent.Begin(requestId))
        }
        voiceHoldTimers[id] = task
        postDelayed(task, VOICE_HOLD_DELAY_MS)
    }

    private fun cancelVoiceHoldArm(id: Int) {
        voiceHoldTimers.remove(id)?.let(::removeCallbacks)
    }

    private fun cancelVoiceHoldArms() {
        voiceHoldTimers.values.forEach(::removeCallbacks)
        voiceHoldTimers.clear()
    }

    private fun dispatch(spec: KeySpec, direction: Direction) {
        if (spec.kind == KeyKind.MODIFIER && direction == Direction.CENTER) {
            if (state.pendingModifier != null) updateModifier(null)
            return
        }
        val action = spec.value(direction)?.action ?: return
        when (action) {
            is KeyAction.SwitchLayer -> { setMode(action.target); actionSink?.onKeyAction(action) }
            is KeyAction.SetModifier -> updateModifier(action.modifier)
            is KeyAction.CommitText -> {
                val modifier = state.pendingModifier
                if (spec.kind == KeyKind.SPACE && state.mode == KeyboardMode.KANA) {
                    actionSink?.onKeyAction(KeyAction.CycleCandidate)
                } else if (modifier != null && action.text.length == 1) {
                    actionSink?.onKeyAction(KeyAction.ModifiedKey(action.text, modifier)); updateModifier(null)
                } else actionSink?.onKeyAction(action)
            }
            else -> actionSink?.onKeyAction(action)
        }
        announceForAccessibility(spec.value(direction)?.label ?: "")
    }

    private fun updateModifier(modifier: Modifier?) {
        state = state.copy(pendingModifier = modifier)
        actionSink?.onKeyAction(KeyAction.SetModifier(modifier))
        invalidate()
    }

    private fun scheduleTimer(id: Int, hit: HitTarget, expectedDirection: Direction) {
        if (hit.spec.kind != KeyKind.BACKSPACE && hit.spec.kind != KeyKind.CHARACTER) return
        val delay = if (hit.spec.kind == KeyKind.BACKSPACE) DELETE_REPEAT_DELAY_MS else ACCENT_DELAY_MS
        val task = object : Runnable {
            override fun run() {
                if (active[id] != hit || directions[id] != expectedDirection) return
                if (hit.spec.kind == KeyKind.BACKSPACE) {
                    repeated += id
                    actionSink?.onKeyAction(KeyAction.Backspace(repeat = true))
                    performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    postDelayed(this, DELETE_REPEAT_INTERVAL_MS)
                } else if (accentChoices(hit.spec.center?.label.orEmpty()) != null) {
                    accentActive += id
                    accentSelected[id] = 0
                    performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    announceForAccessibility("アクセント候補")
                    syncPopup(id)
                    invalidate()
                }
            }
        }
        timers[id] = task
        postDelayed(task, delay)
    }

    private fun cancelTimer(id: Int) { timers.remove(id)?.let(::removeCallbacks) }

    private fun syncPopup(id: Int) {
        val target = active[id] ?: return
        val direction = directions[id] ?: Direction.CENTER
        val show = id in accentActive || target.spec.kind == KeyKind.KANA ||
            isTextPreview(target.spec) || (target.spec.kind == KeyKind.MODIFIER && direction != Direction.CENTER)
        if (!show) {
            if (active.size <= 1) dismissPopup()
            return
        }
        val choices = if (id in accentActive) accentChoices(target.spec.center?.label.orEmpty()).orEmpty() else emptyList()
        popupController.show(this, target.bounds, target.spec, direction, choices, accentSelected[id] ?: 0)
    }

    private fun dismissPopup() = popupController.dismiss()

    private fun animateLabels(id: Int, direction: Direction) {
        labelAnimators.remove(id)?.cancel()
        val start = labelFrames[id] ?: LabelFrame()
        val end = when (direction) {
            Direction.UP -> LabelFrame(mainDy = -3f, secondaryAlpha = 0f)
            Direction.DOWN -> LabelFrame(mainDy = 22f, mainAlpha = 0f, secondaryDy = 13f, secondaryScale = 1.7f)
            else -> LabelFrame()
        }
        if (Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f) {
            labelFrames[id] = end
            invalidate()
            return
        }
        labelAnimators[id] = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = LABEL_ANIMATION_MS; interpolator = PathInterpolator(.25f, .1f, .25f, 1f)
            addUpdateListener {
                val p = it.animatedValue as Float
                labelFrames[id] = LabelFrame(
                    mainDy = start.mainDy + (end.mainDy - start.mainDy) * p,
                    mainAlpha = start.mainAlpha + (end.mainAlpha - start.mainAlpha) * p,
                    secondaryDy = start.secondaryDy + (end.secondaryDy - start.secondaryDy) * p,
                    secondaryScale = start.secondaryScale + (end.secondaryScale - start.secondaryScale) * p,
                    secondaryAlpha = start.secondaryAlpha + (end.secondaryAlpha - start.secondaryAlpha) * p,
                )
                invalidate()
            }
            start()
        }
    }

    private fun dp(value: Float) = value * density
    // The keyboard rows have fixed dp heights. Letting Canvas text follow an
    // unbounded system font scale makes primary and secondary labels overlap.
    private fun sp(value: Float) = value * min(resources.displayMetrics.scaledDensity, density)

    private fun accentChoices(letter: String): List<String>? = when (letter) {
        "a" -> "àáâäæãåā".map(Char::toString)
        "e" -> "èéêëēėę".map(Char::toString)
        "i" -> "îïíīįì".map(Char::toString)
        "o" -> "ôöòóœøōõ".map(Char::toString)
        "u" -> "ûüùúū".map(Char::toString)
        "c" -> "çćč".map(Char::toString)
        "n" -> "ñń".map(Char::toString)
        "s" -> "ßśš".map(Char::toString)
        "y" -> listOf("ÿ")
        else -> null
    }
}

private object ViewCompatInsets {
    fun install(view: View) {
        view.setOnApplyWindowInsetsListener { v, insets ->
            @Suppress("DEPRECATION") val bottom = insets.systemWindowInsetBottom
            (v as? KeyboardView)?.updateBottomInset(bottom)
            insets
        }
    }
}
