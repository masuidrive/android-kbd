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
import android.view.animation.DecelerateInterpolator
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
        private const val ACTION_FLICK_LEFT = 0x01020001
        private const val ACTION_FLICK_UP = 0x01020002
        private const val ACTION_FLICK_RIGHT = 0x01020003
        private const val ACTION_FLICK_DOWN = 0x01020004
    }

    var actionSink: KeyboardActionSink? = null
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
    private var animationProgress = 1f
    private var animator: ValueAnimator? = null
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
        requestLayout()
        accessibilityHelper.invalidateRoot()
        invalidate()
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
        accessibilityHelper.invalidateRoot()
        invalidate()
    }

    fun setDualFlickEnabled(enabled: Boolean) {
        if (state.dualFlickEnabled == enabled) return
        cancelActiveGestures()
        state = state.copy(dualFlickEnabled = enabled)
        rebuildLayout()
    }

    private fun rebuildLayout() {
        if (width > 0 && height > 0) buildHitTargets(paddingTop.toFloat())
        requestLayout()
        accessibilityHelper.invalidateRoot()
        invalidate()
    }

    fun cancelActiveGestures() {
        timers.values.forEach(::removeCallbacks)
        timers.clear()
        active.clear()
        directions.clear()
        cursorMoved.clear()
        accentActive.clear()
        accentSelected.clear()
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
        animator?.cancel()
        super.onDetachedFromWindow()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val wanted = (dp(rowPitchDp(width / density)) * 4 + paddingTop + paddingBottom).toInt()
        setMeasuredDimension(width, resolveSize(wanted, heightMeasureSpec))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.rgb(41, 41, 44))
        buildHitTargets(paddingTop.toFloat())
        val popupOwner = active.entries.firstOrNull { (_, hit) -> hit.spec.kind == KeyKind.KANA || hit.spec.kind == KeyKind.ACCENT }?.key
        hitTargets.forEach { target ->
            if (target.spec.kind == KeyKind.EMPTY) return@forEach
            drawKey(canvas, target, active.entries.firstOrNull { it.value == target }?.key)
        }
        if (popupOwner != null) active[popupOwner]?.let { drawPopup(canvas, it, directions[popupOwner] ?: Direction.CENTER) }
        accentActive.firstOrNull()?.let { id -> active[id]?.let { drawAccentPopup(canvas, it, accentSelected[id] ?: 0) } }
    }

    private fun buildHitTargets(top: Float) {
        hitTargets.clear()
        val dualKana = state.dualFlickEnabled && width / density >= DUAL_FLICK_MIN_WIDTH_DP
        val rows = KeyboardLayouts.layout(state.mode, dualKana, state.conversionActive).rows
        val rowPitch = (height - top - paddingBottom) / 4f
        val rowGap = dp(if (state.mode in setOf(KeyboardMode.QWERTY, KeyboardMode.SYMBOLS)) 10f else 6f)
        val sharedUnits = rows.maxOf { row -> row.keys.sumOf { it.widthUnits.toDouble() }.toFloat() }
        val wideInset = if (width / density >= DUAL_FLICK_MIN_WIDTH_DP) dp(7f) else 0f
        val contentLeft = paddingLeft + wideInset
        val contentWidth = width - paddingLeft - paddingRight - wideInset * 2
        rows.forEachIndexed { rowIndex, row ->
            val layoutUnits = if (state.mode in setOf(KeyboardMode.QWERTY, KeyboardMode.SYMBOLS)) {
                row.keys.sumOf { it.widthUnits.toDouble() }.toFloat()
            } else sharedUnits
            val unit = contentWidth / layoutUnits
            var x = contentLeft
            row.keys.forEach { key ->
                val right = x + unit * key.widthUnits
                val keyTop = top + rowPitch * rowIndex
                val bottom = min(height - paddingBottom.toFloat(), keyTop + rowPitch * key.rowSpan - rowGap)
                hitTargets += HitTarget(key, RectF(x + dp(3f), keyTop, right - dp(3f), bottom))
                x = right
            }
        }
    }

    private fun rowPitchDp(widthDp: Float) = when {
        widthDp >= DUAL_FLICK_MIN_WIDTH_DP && state.mode in setOf(KeyboardMode.QWERTY, KeyboardMode.SYMBOLS) -> 62f
        widthDp >= DUAL_FLICK_MIN_WIDTH_DP -> 64f
        state.mode in setOf(KeyboardMode.QWERTY, KeyboardMode.SYMBOLS) -> 55f
        else -> 57f
    }

    private fun drawKey(canvas: Canvas, target: HitTarget, pointerId: Int?) {
        val selected = pointerId != null
        val modifierActive = target.spec.kind == KeyKind.MODIFIER && state.pendingModifier != null
        keyPaint.color = when { selected || modifierActive -> Color.rgb(168, 206, 255); target.spec.dark -> Color.rgb(48, 48, 52); else -> Color.rgb(65, 65, 68) }
        keyPaint.alpha = 255
        canvas.drawRoundRect(target.bounds, dp(5f), dp(5f), keyPaint)
        textPaint.color = if (selected || modifierActive) Color.rgb(16, 40, 68) else Color.rgb(244, 244, 246)
        textPaint.alpha = 255
        textPaint.textSize = sp(mainTextSize(target.spec))
        val direction = pointerId?.let { directions[it] } ?: Direction.CENTER
        val spec = target.spec
        val label = when {
            spec.kind == KeyKind.MODIFIER && state.pendingModifier != null -> if (state.pendingModifier == Modifier.ALT) "A" else "C"
            spec.kind == KeyKind.BACKSPACE && spec.center != null -> spec.center.label
            selected && direction != Direction.CENTER -> spec.value(direction)?.label
            else -> spec.center?.label ?: modifierLabel(spec)
        } ?: ""
        val centerY = target.bounds.centerY() - (textPaint.ascent() + textPaint.descent()) / 2
        val animatedEnglish = selected && spec.kind == KeyKind.CHARACTER && (direction == Direction.UP || direction == Direction.DOWN)
        val animatedSpecial = selected && direction != Direction.CENTER && spec.kind in setOf(KeyKind.ENTER, KeyKind.SPACE, KeyKind.MODIFIER)
        val idleModifier = spec.kind == KeyKind.MODIFIER && state.pendingModifier == null && direction == Direction.CENTER
        if (spec.id == "mode-↔" && !selected) {
            drawCursorCross(canvas, target.bounds)
        } else if (idleModifier) {
            textPaint.textSize = sp(10f)
            canvas.drawText("C", target.bounds.centerX(), target.bounds.top + dp(13f), textPaint)
            canvas.drawText("A", target.bounds.centerX(), target.bounds.bottom - dp(6f), textPaint)
        } else if (!animatedEnglish && !animatedSpecial) {
            drawMainLabel(canvas, label, target.bounds, centerY, !selected && direction == Direction.CENTER)
        }
        val secondary = when {
            spec.kind == KeyKind.ENTER && !state.conversionActive -> "paste"
            spec.kind == KeyKind.SPACE && state.mode == KeyboardMode.QWERTY -> "←↓↑→"
            spec.kind == KeyKind.CHARACTER -> spec.down?.label
            else -> null
        }
        if (animatedEnglish && direction == Direction.DOWN && secondary != null) {
            textPaint.textSize = sp(12f) * (1f + .7f * animationProgress)
            textPaint.color = Color.rgb(16, 40, 68)
            canvas.drawText(secondary, target.bounds.centerX(), target.bounds.top + dp(14f + 13f * animationProgress), textPaint)
            textPaint.textSize = sp(22f)
            textPaint.alpha = (255 * (1f - animationProgress)).toInt()
            canvas.drawText(spec.center?.label.orEmpty(), target.bounds.centerX(), centerY + dp(22f) * animationProgress, textPaint)
        } else if (animatedEnglish && direction == Direction.UP) {
            textPaint.textSize = sp(22f)
            textPaint.color = Color.rgb(16, 40, 68)
            canvas.drawText(label, target.bounds.centerX(), centerY - dp(3f) * animationProgress, textPaint)
        } else if (animatedSpecial) {
            textPaint.textSize = sp(11f) * (1f + .7f * animationProgress)
            textPaint.color = Color.rgb(16, 40, 68)
            drawFittedText(canvas, label, target.bounds.centerX(), centerY, target.bounds.width() - dp(8f))
        } else if (!selected && secondary != null) {
            textPaint.textSize = sp(secondaryTextSize(spec))
            textPaint.color = Color.rgb(190, 192, 200)
            canvas.drawText(secondary, target.bounds.centerX(), target.bounds.top + dp(14f), textPaint)
        }
    }

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

    private fun drawMainLabel(canvas: Canvas, label: String, bounds: RectF, y: Float, allowComposite: Boolean) {
        val x = bounds.centerX()
        if (!allowComposite || label !in setOf("#!", "あん", "AZ", "19")) {
            drawFittedText(canvas, label, x, y, bounds.width() - dp(8f))
            return
        }
        val main = label.substring(0, 1)
        val ghost = label.substring(1)
        val originalSize = textPaint.textSize
        val originalColor = textPaint.color
        val originalAlpha = textPaint.alpha
        textPaint.textSize = originalSize * .75f
        if (originalColor != Color.rgb(16, 40, 68)) {
            textPaint.color = Color.rgb(181, 181, 191)
            textPaint.alpha = (originalAlpha * .72f).toInt()
        }
        val ghostX = x + dp(2f) + textPaint.measureText(ghost) / 2f
        canvas.drawText(ghost, ghostX, y + dp(4f), textPaint)
        textPaint.textSize = originalSize
        textPaint.color = originalColor
        textPaint.alpha = originalAlpha
        canvas.drawText(main, x - dp(3f), y, textPaint)
    }

    private fun drawFittedText(canvas: Canvas, label: String, x: Float, y: Float, maxWidth: Float) {
        val original = textPaint.textSize
        val measured = textPaint.measureText(label)
        if (measured > maxWidth && measured > 0f) textPaint.textSize = original * maxWidth / measured
        canvas.drawText(label, x, y, textPaint)
        textPaint.textSize = original
    }

    private fun drawPopup(canvas: Canvas, target: HitTarget, selected: Direction) {
        val tile = dp(50f)
        val box = RectF(target.bounds.centerX() - tile * 1.5f, target.bounds.centerY() - tile * 1.5f,
            target.bounds.centerX() + tile * 1.5f, target.bounds.centerY() + tile * 1.5f)
        keyPaint.color = Color.rgb(64, 66, 74)
        keyPaint.setShadowLayer(dp(8f), 0f, dp(3f), Color.BLACK)
        setLayerType(LAYER_TYPE_SOFTWARE, keyPaint)
        canvas.drawRoundRect(box, dp(8f), dp(8f), keyPaint)
        keyPaint.clearShadowLayer()
        Direction.entries.forEach { direction ->
            val cx = box.centerX() + when (direction) { Direction.LEFT -> -tile; Direction.RIGHT -> tile; else -> 0f }
            val cy = box.centerY() + when (direction) { Direction.UP -> -tile; Direction.DOWN -> tile; else -> 0f }
            if (direction == selected) {
                keyPaint.color = Color.rgb(47, 111, 224)
                canvas.drawRoundRect(RectF(cx - tile / 2, cy - tile / 2, cx + tile / 2, cy + tile / 2), dp(4f), dp(4f), keyPaint)
            }
            val label = target.spec.value(direction)?.label ?: return@forEach
            textPaint.color = Color.WHITE; textPaint.alpha = 255; textPaint.textSize = sp(20f)
            drawFittedText(canvas, label, cx, cy - (textPaint.ascent() + textPaint.descent()) / 2, tile - dp(6f))
        }
    }

    private fun drawAccentPopup(canvas: Canvas, target: HitTarget, selected: Int) {
        val choices = accentChoices(target.spec.center?.label ?: return) ?: return
        val tile = dp(34f)
        val left = accentPopupLeft(target, choices.size, tile)
        val top = (target.bounds.top - tile - dp(4f)).coerceAtLeast(0f)
        choices.forEachIndexed { index, choice ->
            val rect = RectF(left + tile * index, top, left + tile * (index + 1), top + tile)
            keyPaint.color = if (index == selected) Color.rgb(47, 111, 224) else Color.rgb(64, 66, 74)
            canvas.drawRoundRect(rect, dp(4f), dp(4f), keyPaint)
            textPaint.color = Color.WHITE; textPaint.alpha = 255; textPaint.textSize = sp(20f)
            canvas.drawText(choice, rect.centerX(), rect.centerY() - (textPaint.ascent() + textPaint.descent()) / 2, textPaint)
        }
    }

    private fun accentPopupLeft(target: HitTarget, count: Int, tile: Float): Float =
        (target.bounds.centerX() - tile * count / 2f).coerceIn(0f, (width - tile * count).coerceAtLeast(0f))

    override fun onTouchEvent(event: MotionEvent): Boolean {
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
        active[id] = hit
        directions[id] = Direction.CENTER
        val verticalOnly = hit.spec.kind == KeyKind.MODIFIER ||
            (hit.spec.kind == KeyKind.BACKSPACE && hit.spec.center == null) ||
            (hit.spec.kind == KeyKind.CHARACTER && (hit.spec.up != null || hit.spec.down != null))
        interpreter.start(id, event.getX(index) / density, event.getY(index) / density,
            trackpad = hit.spec.kind == KeyKind.SPACE, verticalOnly = verticalOnly)
        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        if (hit.spec.kind == KeyKind.CHARACTER || (hit.spec.kind == KeyKind.BACKSPACE && hit.spec.center != null)) {
            scheduleTimer(id, hit, Direction.CENTER)
        }
        sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_HOVER_ENTER)
        invalidate()
    }

    private fun pointerMove(event: MotionEvent, index: Int) {
        val id = event.getPointerId(index)
        if (id in accentActive) {
            val hit = active[id]
            val choices = hit?.spec?.center?.label?.let(::accentChoices)
            if (hit != null && choices != null) {
                val tile = dp(34f)
                accentSelected[id] = ((event.getX(index) - accentPopupLeft(hit, choices.size, tile)) / tile).toInt().coerceIn(0, choices.lastIndex)
            }
        }
        when (val update = interpreter.move(id, event.getX(index) / density, event.getY(index) / density)) {
            is GestureUpdate.Selection -> {
                if (directions[id] != update.direction) performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                directions[id] = update.direction
                cancelTimer(id)
                if (active[id]?.spec?.down?.action is KeyAction.Backspace && update.direction == Direction.DOWN) {
                    active[id]?.let { scheduleTimer(id, it, Direction.DOWN) }
                }
                animateLabels()
            }
            is GestureUpdate.CursorDelta -> {
                cursorMoved += id
                if (directions[id] != update.direction) animateLabels()
                directions[id] = update.direction
                actionSink?.onKeyAction(KeyAction.MoveCursor(update.direction, update.units))
                performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            }
            null -> Unit
        }
        invalidate()
    }

    private fun pointerUp(id: Int) {
        cancelTimer(id)
        val hit = active.remove(id) ?: return
        val direction = directions.remove(id) ?: Direction.CENTER
        interpreter.finish(id)
        val accents = if (accentActive.remove(id)) accentChoices(hit.spec.center?.label.orEmpty()) else null
        if (accents != null) actionSink?.onKeyAction(KeyAction.CommitText(accents[accentSelected.remove(id) ?: 0]))
        else if (!cursorMoved.remove(id) && !repeated.remove(id)) dispatch(hit.spec, direction)
        cursorMoved.remove(id)
        invalidate()
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
                    invalidate()
                }
            }
        }
        timers[id] = task
        postDelayed(task, delay)
    }

    private fun cancelTimer(id: Int) { timers.remove(id)?.let(::removeCallbacks) }

    private fun animateLabels() {
        animator?.cancel()
        if (Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f) {
            animationProgress = 1f; return
        }
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = LABEL_ANIMATION_MS; interpolator = DecelerateInterpolator()
            addUpdateListener { animationProgress = it.animatedValue as Float; invalidate() }
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
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, bottom)
            insets
        }
    }
}
