package com.masuidrive.gestureime.keyboard

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.provider.Settings
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.animation.PathInterpolator
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.customview.widget.ExploreByTouchHelper
import com.masuidrive.gestureime.R
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val VOICE_INPUT_LEVEL_STEPS = 24
private const val VOICE_INPUT_LEVEL_SCALE_DB = 20.0
internal const val KEY_ROW_GAP_DP = 10f

/** Maps any finite provider-specific RMS value monotonically into a bounded display level. */
internal fun normalizeVoiceInputLevel(rmsDb: Float): Float {
    require(rmsDb.isFinite()) { "RMS input level must be finite" }
    val bounded = atan(rmsDb.toDouble() / VOICE_INPUT_LEVEL_SCALE_DB) / Math.PI + 0.5
    return (bounded * VOICE_INPUT_LEVEL_STEPS).roundToInt().toFloat() / VOICE_INPUT_LEVEL_STEPS
}

internal data class EmojiLayerHorizontalGeometry(
    val contentLeft: Int,
    val railRight: Int,
    val contentRight: Int,
) {
    val bodyWidth: Int get() = (contentRight - railRight).coerceAtLeast(0)
}

internal data class KeyboardContentHorizontalGeometry(val left: Float, val right: Float) {
    val width: Float get() = (right - left).coerceAtLeast(0f)
}

/** The one source of horizontal keyboard content insets for every native layer and overlay. */
internal fun keyboardContentHorizontalGeometry(
    totalWidth: Int,
    density: Float,
    paddingLeft: Int = 0,
    paddingRight: Int = 0,
): KeyboardContentHorizontalGeometry {
    val safeDensity = density.takeIf { it.isFinite() && it > 0f } ?: 1f
    val inset = (if (totalWidth / safeDensity >= KeyboardView.DUAL_FLICK_MIN_WIDTH_DP) 10f else 3f) * safeDensity
    return KeyboardContentHorizontalGeometry(
        left = (paddingLeft + inset).coerceIn(0f, totalWidth.toFloat()),
        right = (totalWidth - paddingRight - inset).coerceIn(0f, totalWidth.toFloat()),
    )
}

/** Pixel-snapped bounds shared by KeyboardView and the overlaid AndroidX emoji picker. */
internal fun emojiLayerHorizontalGeometry(
    totalWidth: Int,
    density: Float,
    dualKana: Boolean = false,
    paddingLeft: Int = 0,
    paddingRight: Int = 0,
): EmojiLayerHorizontalGeometry {
    val content = keyboardContentHorizontalGeometry(totalWidth, density, paddingLeft, paddingRight)
    val railRight = content.left + content.width / if (dualKana) 8f else 5f
    return EmojiLayerHorizontalGeometry(
        contentLeft = ceil(content.left).toInt().coerceIn(0, totalWidth),
        railRight = ceil(railRight).toInt().coerceIn(0, totalWidth),
        contentRight = content.right.toInt().coerceIn(0, totalWidth),
    )
}

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
        private const val QWERTY_SECONDARY_IDLE_CENTER_DP = 9f
        private const val ACTION_FLICK_LEFT = 0x01020001
        private const val ACTION_FLICK_UP = 0x01020002
        private const val ACTION_FLICK_RIGHT = 0x01020003
        private const val ACTION_FLICK_DOWN = 0x01020004
        internal const val VOICE_SESSION_STATUS_VIRTUAL_ID = 0x7fff0001
    }

    var actionSink: KeyboardActionSink? = null
    var voiceHoldSink: VoiceHoldSink? = null
    private var state = KeyboardUiState()
    private val density = resources.displayMetrics.density
    private val interpreter = GestureInterpreter()
    private var kanaNumberFlickSensitivity = FlickSensitivity.STANDARD
    private var qwertySymbolFlickSensitivity = FlickSensitivity.STANDARD
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
    private var previewOnly = false
    private var voiceHoldOwner: Pair<Int, Long>? = null
    private var voiceHoldRequestId = 0L
    private var voiceHoldMultiPointer = false
    private val voiceGestureCancelledPointers = mutableSetOf<Int>()
    private var voiceEntryPointer: Int? = null
    private var voiceReadyRequestId: Long? = null
    private var secondVoiceHaptic: Runnable? = null
    private val popupController = KeyboardPopupController(context)
    private val accessibilityHelper = KeyboardAccessibilityHelper(this)

    private val keyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }

    private data class HitTarget(
        val spec: KeySpec,
        val bounds: RectF,
        val tapBounds: RectF,
        val scrollable: Boolean = false,
    )
    private data class EmojiScrollGesture(
        val startY: Float,
        val startOffset: Float,
        var scrolling: Boolean = false,
    )

    private var emojiScrollOffset = 0f
    private val emojiScrollGestures = mutableMapOf<Int, EmojiScrollGesture>()
    private val emojiViewport = RectF()
    private var voiceSessionStatusHovered = false
    private var ownsSystemBottomInset = true
    private var systemBottomInsetFallback = 0
    private var retainsIntrinsicHeightInIme = false

    internal fun hitTargetIndexAt(x: Float, y: Float): Int = hitTargets.indexOfLast {
        it.spec.kind != KeyKind.EMPTY && isTargetVisible(it) && it.tapBounds.contains(x, y) &&
            (!it.scrollable || emojiViewport.contains(x, y))
    }

    init {
        isFocusable = true
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        ViewCompat.setAccessibilityDelegate(this, accessibilityHelper)
        ViewCompatInsets.install(this)
    }

    fun setMode(mode: KeyboardMode) {
        if (state.mode == mode) return
        if (state.mode == KeyboardMode.VOICE && mode != KeyboardMode.VOICE && state.voiceSessionActive) {
            setVoiceSessionActive(false)
        }
        cancelActiveGestures()
        if (mode == KeyboardMode.EMOJI) emojiScrollOffset = 0f
        state = state.copy(mode = mode)
        contentDescription = "${mode.displayName}キーボード"
        rebuildLayout()
    }

    /** Height covered by the picker below its 50dp category header: top inset plus four rows. */
    internal fun emojiPickerOverlayHeight(): Float = dp(8f) + currentEmojiRowPitch() * 4f

    /** Uses the pending parent width before this view has received its first layout. */
    internal fun emojiPickerOverlayHeightForWidth(measuredWidth: Int): Float =
        dp(8f) + rowPitch(measuredWidth) * 4f

    /** Height covered by the voice panel through the end of the third row's touch region. */
    internal fun voicePanelOverlayHeight(): Float =
        paddingTop + dp(8f) + currentEmojiRowPitch() * 3f - dp(KEY_ROW_GAP_DP / 2f)

    /** Uses the pending parent width before this view has received its first layout. */
    internal fun voicePanelOverlayHeightForWidth(measuredWidth: Int): Float =
        paddingTop + dp(8f) + rowPitch(measuredWidth) * 3f - dp(KEY_ROW_GAP_DP / 2f)

    internal fun emojiLayerHorizontalGeometryForWidth(measuredWidth: Int): EmojiLayerHorizontalGeometry =
        emojiLayerHorizontalGeometry(
            totalWidth = measuredWidth,
            density = density,
            dualKana = state.dualFlickEnabled && measuredWidth / density >= DUAL_FLICK_MIN_WIDTH_DP,
            paddingLeft = paddingLeft,
            paddingRight = paddingRight,
        )

    private fun keyboardContentHorizontalGeometryForWidth(measuredWidth: Int): KeyboardContentHorizontalGeometry =
        keyboardContentHorizontalGeometry(measuredWidth, density, paddingLeft, paddingRight)

    fun setModifier(modifier: Modifier?) {
        state = state.copy(pendingModifier = modifier)
        accessibilityHelper.invalidateRoot()
        invalidate()
    }

    fun setCandidates(candidates: List<String>, selectedIndex: Int = -1) {
        state = state.copy(candidates = candidates, selectedCandidateIndex = selectedIndex)
        invalidate()
    }

    fun setConversionActive(active: Boolean) = setConversionState(active, candidateSelected = false)

    fun setConversionState(active: Boolean, candidateSelected: Boolean) {
        val selected = active && candidateSelected
        if (state.conversionActive == active && state.conversionCandidateSelected == selected) return
        this.active.filterValues { it.spec.kind == KeyKind.ENTER }.keys.toList().forEach(::discardPointer)
        state = state.copy(conversionActive = active, conversionCandidateSelected = selected)
        rebuildLayout()
    }

    fun setDualFlickEnabled(enabled: Boolean) {
        if (state.dualFlickEnabled == enabled) return
        cancelActiveGestures()
        state = state.copy(dualFlickEnabled = enabled)
        rebuildLayout()
    }

    fun setHeightPreset(preset: KeyboardHeightPreset) {
        if (state.heightPreset == preset) return
        cancelActiveGestures()
        state = state.copy(heightPreset = preset)
        rebuildLayout()
    }

    /** Applies to the next pointer sequence; Space keeps its fixed trackpad thresholds. */
    fun setFlickSensitivities(
        kanaNumber: FlickSensitivity,
        qwertySymbol: FlickSensitivity,
    ) {
        if (kanaNumberFlickSensitivity == kanaNumber && qwertySymbolFlickSensitivity == qwertySymbol) return
        cancelActiveGestures()
        kanaNumberFlickSensitivity = kanaNumber
        qwertySymbolFlickSensitivity = qwertySymbol
    }

    /** The IME input root must replace a stale host height with this four-row view's own size. */
    internal fun setRetainsIntrinsicHeightInIme(enabled: Boolean) {
        if (retainsIntrinsicHeightInIme == enabled) return
        retainsIntrinsicHeightInIme = enabled
        requestLayout()
    }

    fun setEmojiRecents(recents: List<String>) {
        val normalized = EmojiCatalog.visibleRecents(recents)
        if (state.emojiRecents == normalized) return
        state = state.copy(emojiRecents = normalized)
        emojiScrollOffset = emojiScrollOffset.coerceIn(0f, emojiScrollRange())
        if (state.mode == KeyboardMode.EMOJI) rebuildLayout() else invalidate()
    }

    /** Keeps the non-action voice status in its dedicated bottom-row fifth. */
    fun setVoiceSessionActive(active: Boolean) {
        if (state.voiceSessionActive == active) return
        if (!active) accessibilityHelper.clearVoiceSessionStatusFocus()
        state = state.copy(voiceSessionActive = active, voiceInputLevel = if (active) state.voiceInputLevel else null)
        accessibilityHelper.invalidateRoot()
        if (active) accessibilityHelper.announceVoiceSessionStarted()
        invalidate()
    }

    /** SpeechRecognizer RMS has no documented range, so the renderer owns bounded quantization. */
    fun setVoiceInputLevel(rmsDb: Float?): Boolean {
        if (rmsDb != null && !rmsDb.isFinite()) return false
        val normalized = rmsDb?.takeIf { state.voiceSessionActive }?.let(::normalizeVoiceInputLevel)
        if (state.voiceInputLevel == normalized) return false
        state = state.copy(voiceInputLevel = normalized)
        invalidate()
        return true
    }

    fun showVoiceInputLevelBaseline() {
        if (!state.voiceSessionActive || state.voiceInputLevel == 0f) return
        state = state.copy(voiceInputLevel = 0f)
        invalidate()
    }

    internal fun isVoiceSessionStatusAccessibilityFocused(): Boolean =
        accessibilityHelper.getAccessibilityFocusedVirtualViewId() == VOICE_SESSION_STATUS_VIRTUAL_ID

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

    internal fun setSystemBottomInsetFallback(bottom: Int) {
        systemBottomInsetFallback = bottom.coerceAtLeast(0)
    }

    /** The navigation inset remains below the four key rows and does not change their pitch. */
    internal fun setOwnsSystemBottomInset(ownsInset: Boolean) {
        if (ownsSystemBottomInset == ownsInset) return
        ownsSystemBottomInset = ownsInset
        if (!ownsInset) updateBottomInset(0)
    }

    internal fun applySystemBottomInset(bottom: Int) {
        if (ownsSystemBottomInset) {
            if (bottom > 0) systemBottomInsetFallback = bottom
            updateBottomInset(bottom.takeIf { it > 0 } ?: systemBottomInsetFallback)
        }
    }

    internal fun refreshIntrinsicLayout() {
        cancelActiveGestures()
        requestLayout()
        if (width > 0 && height > 0) buildHitTargets(paddingTop.toFloat())
        accessibilityHelper.invalidateRoot()
        invalidate()
    }

    fun cancelActiveGestures() {
        voiceHoldOwner?.second?.let { voiceHoldSink?.onVoiceHold(VoiceHoldEvent.Cancel(it)) }
        voiceHoldOwner = null
        voiceReadyRequestId = null
        secondVoiceHaptic?.let(::removeCallbacks)
        secondVoiceHaptic = null
        voiceHoldMultiPointer = false
        voiceGestureCancelledPointers.clear()
        voiceEntryPointer = null
        timers.values.forEach(::removeCallbacks)
        timers.clear()
        labelAnimators.values.forEach(ValueAnimator::cancel)
        labelAnimators.clear()
        labelFrames.clear()
        active.clear()
        emojiScrollGestures.clear()
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

    private fun discardPointer(id: Int) {
        cancelTimer(id)
        labelAnimators.remove(id)?.cancel()
        labelFrames.remove(id)
        active.remove(id)
        directions.remove(id)
        cursorMoved.remove(id)
        accentActive.remove(id)
        accentSelected.remove(id)
        repeated.remove(id)
        interpreter.cancel(id)
        dismissPopup()
        active.keys.firstOrNull()?.let(::syncPopup)
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
        dismissPopup()
        super.onDetachedFromWindow()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val wanted = (rowPitch(width) * 4 + dp(8f) + paddingTop + paddingBottom).toInt()
        // Standalone previews must respect a genuinely smaller parent. InputMethodService
        // input roots opt in below: the host can carry a previous editor's short exact frame
        // through hide/show or an editor switch, and that stale value is not usable geometry.
        val measuredHeight = if (retainsIntrinsicHeightInIme) {
            // InputMethodService can carry a previous IME frame through hide/show and editor
            // changes. Its parent turns that stale value into an AT_MOST child constraint.
            wanted
        } else {
            when (MeasureSpec.getMode(heightMeasureSpec)) {
                MeasureSpec.UNSPECIFIED -> wanted
                else -> min(wanted, MeasureSpec.getSize(heightMeasureSpec))
            }
        }
        setMeasuredDimension(resolveSize(width, widthMeasureSpec), measuredHeight)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(context.getColor(R.color.keyboard_background))
        if (state.mode == KeyboardMode.EMOJI) {
            val clipped = canvas.save()
            canvas.clipRect(emojiViewport)
            hitTargets.filter { it.scrollable && it.spec.kind != KeyKind.EMPTY }.forEach { target ->
                drawKey(canvas, target, active.entries.firstOrNull { it.value == target }?.key)
            }
            canvas.restoreToCount(clipped)
            hitTargets.filter { !it.scrollable && it.spec.kind != KeyKind.EMPTY }.forEach { target ->
                drawKey(canvas, target, active.entries.firstOrNull { it.value == target }?.key)
            }
        } else {
            hitTargets.forEach { target ->
                if (target.spec.kind == KeyKind.EMPTY) return@forEach
                drawKey(canvas, target, active.entries.firstOrNull { it.value == target }?.key)
            }
        }
        if (state.mode == KeyboardMode.VOICE && state.voiceSessionActive) drawVoiceSessionStatus(canvas)
    }

    private fun drawVoiceSessionStatus(canvas: Canvas) {
        val status = hitTargets.firstOrNull { it.spec.id == "voice-status" } ?: return
        textPaint.color = context.getColor(R.color.keyboard_muted_text)
        textPaint.alpha = 255
        textPaint.textSize = sp(13f)
        textPaint.textAlign = Paint.Align.CENTER
        val labelWidth = textPaint.measureText("認識中")
        val amplitude = state.voiceInputLevel
        val barWidth = dp(1.5f)
        val barGap = dp(1.5f)
        val barCount = 4
        val waveWidth = barWidth * barCount + barGap * (barCount - 1)
        val groupWidth = if (amplitude == null) labelWidth else labelWidth + dp(4f) + waveWidth
        val x = status.bounds.centerX() - groupWidth / 2f + labelWidth / 2f
        val y = status.bounds.centerY() - (textPaint.ascent() + textPaint.descent()) / 2f
        canvas.drawText("認識中", x, y, textPaint)
        if (amplitude == null) return

        keyPaint.color = context.getColor(R.color.keyboard_muted_text)
        keyPaint.alpha = 255
        val waveLeft = x + labelWidth / 2f + dp(4f)
        val waveCenterY = status.bounds.centerY()
        val shape = floatArrayOf(0.55f, 1f, 0.72f, 0.42f)
        shape.forEachIndexed { index, multiplier ->
            val height = dp(2f) + dp(12f) * amplitude * multiplier
            val left = waveLeft + index * (barWidth + barGap)
            canvas.drawRoundRect(
                RectF(left, waveCenterY - height / 2f, left + barWidth, waveCenterY + height / 2f),
                barWidth / 2f,
                barWidth / 2f,
                keyPaint,
            )
        }
    }

    private fun voiceSessionStatusBounds(): android.graphics.Rect? {
        if (state.mode != KeyboardMode.VOICE || !state.voiceSessionActive) return null
        val status = hitTargets.firstOrNull { it.spec.id == "voice-status" } ?: return null
        val bounds = android.graphics.Rect(
            status.bounds.left.toInt(), status.bounds.top.toInt(),
            status.bounds.right.toInt(), status.bounds.bottom.toInt(),
        )
        return bounds.takeUnless { it.isEmpty }
    }

    private fun buildHitTargets(top: Float) {
        hitTargets.clear()
        val keyboardTop = top + dp(8f)
        val dualKana = state.dualFlickEnabled && width / density >= DUAL_FLICK_MIN_WIDTH_DP
        val rowPitch = min((height - keyboardTop - paddingBottom) / 4f, rowPitch())
        val rowGap = dp(KEY_ROW_GAP_DP)
        val horizontal = keyboardContentHorizontalGeometryForWidth(width)
        if (state.mode == KeyboardMode.EMOJI) {
            buildEmojiHitTargets(keyboardTop, rowPitch, rowGap, emojiLayerHorizontalGeometryForWidth(width))
            return
        }
        emojiViewport.setEmpty()
        val rows = KeyboardLayouts.layout(
            state.mode,
            dualKana,
            state.conversionActive,
            state.conversionCandidateSelected,
            state.emojiRecents,
        ).rows
        val sharedUnits = rows.maxOf { row -> row.keys.sumOf { it.widthUnits.toDouble() }.toFloat() }
        rows.forEachIndexed { rowIndex, row ->
            val layoutUnits = if (state.mode in setOf(KeyboardMode.QWERTY, KeyboardMode.SYMBOLS)) {
                row.keys.sumOf { it.widthUnits.toDouble() }.toFloat()
            } else sharedUnits
            val unit = horizontal.width / layoutUnits
            var x = horizontal.left
            row.keys.forEach { key ->
                val right = x + unit * key.widthUnits
                val keyTop = keyboardTop + rowPitch * rowIndex
                val bottom = min(height - paddingBottom.toFloat(), keyTop + rowPitch * key.rowSpan - rowGap)
                val tapTop = if (rowIndex == 0) keyTop else keyTop - rowGap / 2f
                val occupiedRowsEnd = rowIndex + key.rowSpan
                val tapBottom = if (occupiedRowsEnd < rows.size) {
                    keyboardTop + rowPitch * occupiedRowsEnd - rowGap / 2f
                } else {
                    bottom
                }
                hitTargets += HitTarget(
                    key,
                    bounds = RectF(x + dp(3f), keyTop, right - dp(3f), bottom),
                    tapBounds = RectF(x, tapTop, right, tapBottom),
                )
                x = right
            }
        }
    }

    private fun buildEmojiHitTargets(
        keyboardTop: Float,
        rowPitch: Float,
        rowGap: Float,
        geometry: EmojiLayerHorizontalGeometry,
    ) {
        emojiScrollOffset = emojiScrollOffset.coerceIn(0f, emojiScrollRange(rowPitch))
        emojiViewport.set(
            geometry.railRight.toFloat(),
            keyboardTop,
            geometry.contentRight.toFloat(),
            keyboardTop + rowPitch * 4f - rowGap,
        )
        fun addRow(row: KeyboardRow, top: Float, scrollable: Boolean) {
            var x = geometry.contentLeft.toFloat()
            var consumedUnits = 0f
            row.keys.forEachIndexed { index, key ->
                val nextUnits = consumedUnits + key.widthUnits
                val right = when {
                    nextUnits <= 1f -> geometry.contentLeft +
                        (geometry.railRight - geometry.contentLeft) * nextUnits
                    index == row.keys.lastIndex -> geometry.contentRight.toFloat()
                    else -> geometry.railRight + geometry.bodyWidth * ((nextUnits - 1f) / 7f)
                }
                val bottom = min(height - paddingBottom.toFloat(), top + rowPitch - rowGap)
                hitTargets += HitTarget(
                    key,
                    bounds = RectF(x + dp(3f), top, right - dp(3f), bottom),
                    tapBounds = RectF(x, top - rowGap / 2f, right, top + rowPitch - rowGap / 2f),
                    scrollable = scrollable,
                )
                x = right
                consumedUnits = nextUnits
            }
        }
        KeyboardLayouts.emojiContentRows(state.emojiRecents).forEachIndexed { index, row ->
            // AndroidX scrolls the seven-column body above these rows. The first-column
            // layer rail is part of KeyboardView and must remain fixed and touchable.
            addRow(row, keyboardTop + rowPitch * index, scrollable = false)
        }
    }

    private fun emojiScrollRange(rowPitch: Float = currentEmojiRowPitch()): Float =
        ((KeyboardLayouts.emojiContentRows(state.emojiRecents).size - 4).coerceAtLeast(0) * rowPitch)

    private fun currentEmojiRowPitch(): Float {
        if (height <= 0) return rowPitch()
        val keyboardTop = paddingTop + dp(8f)
        return min((height - keyboardTop - paddingBottom) / 4f, rowPitch())
    }

    private fun isTargetVisible(target: HitTarget): Boolean =
        !target.scrollable || target.bounds.intersects(emojiViewport.left, emojiViewport.top, emojiViewport.right, emojiViewport.bottom)

    private fun visibleBounds(target: HitTarget): RectF? {
        if (!isTargetVisible(target)) return null
        return if (!target.scrollable) target.bounds else RectF(target.bounds).apply { intersect(emojiViewport) }
    }

    private fun scrollEmojiTo(offset: Float): Boolean {
        val clamped = offset.coerceIn(0f, emojiScrollRange())
        if (emojiScrollOffset == clamped) return false
        emojiScrollOffset = clamped
        if (width > 0 && height > 0) buildHitTargets(paddingTop.toFloat())
        accessibilityHelper.invalidateRoot()
        invalidate()
        return true
    }

    /** Restores the pre-preset 62dp Dual Flick geometry for the user's explicit Large choice. */
    private fun rowPitch(measuredWidth: Int = width): Float {
        val wideLarge = state.heightPreset == KeyboardHeightPreset.LARGE &&
            measuredWidth / density >= DUAL_FLICK_MIN_WIDTH_DP
        return dp(if (wideLarge) 62f else state.heightPreset.rowPitchDp)
    }

    private fun drawKey(canvas: Canvas, target: HitTarget, pointerId: Int?) {
        val selected = pointerId != null
        val modifierActive = target.spec.kind == KeyKind.MODIFIER && state.pendingModifier != null
        val faceColor = when {
            selected || modifierActive -> context.getColor(R.color.keyboard_selected)
            target.spec.dark && target.spec.kind != KeyKind.ACCENT -> context.getColor(R.color.keyboard_special)
            else -> context.getColor(R.color.keyboard_key)
        }
        keyPaint.color = context.getColor(R.color.keyboard_shadow)
        canvas.drawRoundRect(RectF(target.bounds).apply { offset(0f, dp(1f)) }, dp(5f), dp(5f), keyPaint)
        keyPaint.color = faceColor
        keyPaint.alpha = 255
        canvas.drawRoundRect(target.bounds, dp(5f), dp(5f), keyPaint)
        val textSave = canvas.save()
        canvas.clipRect(target.bounds)
        textPaint.color = if (selected || modifierActive) {
            context.getColor(R.color.keyboard_selected_text)
        } else {
            context.getColor(R.color.keyboard_text)
        }
        textPaint.alpha = 255
        textPaint.textSize = sp(mainTextSize(target.spec))
        val direction = pointerId?.let { directions[it] } ?: Direction.CENTER
        val frame = pointerId?.let { labelFrames[it] } ?: LabelFrame()
        val spec = target.spec
        val label = when {
            spec.kind == KeyKind.MODIFIER && state.pendingModifier != null -> if (state.pendingModifier == Modifier.ALT) "A" else "C"
            spec.kind == KeyKind.BACKSPACE && spec.center != null -> spec.center.label
            spec.id == "punct" && direction == Direction.CENTER -> "、。?!"
            spec.id == "voice-punct" && direction == Direction.CENTER -> "、。？！"
            selected && direction != Direction.CENTER -> spec.value(direction)?.label
            else -> spec.center?.label ?: modifierLabel(spec)
        } ?: ""
        val centerY = target.bounds.centerY() - (textPaint.ascent() + textPaint.descent()) / 2
        val secondary = when {
            spec.kind == KeyKind.ENTER && !state.conversionActive ->
                Direction.entries.firstNotNullOfOrNull { direction ->
                    spec.value(direction)?.takeIf { it.action == KeyAction.Paste }?.label
                }
            spec.kind == KeyKind.SPACE && state.mode == KeyboardMode.QWERTY -> "←↓↑→"
            spec.kind == KeyKind.CHARACTER && spec.id != "voice-punct" &&
                spec.id != "five--" && spec.id != "number-period" -> spec.down?.label
            spec.kind == KeyKind.BACKSPACE -> spec.down?.label
            else -> null
        }
        val downLike = direction == Direction.DOWN || frame.secondaryScale > 1.001f || frame.mainDy > 0.001f
        val upLike = direction == Direction.UP || frame.mainDy < -0.001f || frame.secondaryAlpha < .999f
        val animatedEnglish = selected && secondary != null && when {
            state.mode == KeyboardMode.QWERTY && spec.kind == KeyKind.CHARACTER -> downLike || upLike
            spec.kind == KeyKind.BACKSPACE -> downLike
            else -> false
        }
        val selectedEnterAction = if (selected) spec.value(direction)?.action else null
        val animatedEnterPaste = selected && spec.kind == KeyKind.ENTER && secondary != null &&
            selectedEnterAction == KeyAction.Paste
        val selectedEnterControlJ = selected && !state.conversionActive && spec.kind == KeyKind.ENTER &&
            selectedEnterAction == KeyAction.ModifiedKey("j", Modifier.CTRL)
        val animatedSpecial = selected && direction != Direction.CENTER &&
            spec.kind in setOf(KeyKind.SPACE, KeyKind.MODIFIER)
        val idleModifier = spec.kind == KeyKind.MODIFIER && state.pendingModifier == null && direction == Direction.CENTER
        val idleMainBaseline = when {
            usesDownLabelAnimation(spec) && secondary != null ->
                baselineAtVisualCenter(target.bounds.centerY() + dp(5f))
            spec.kind == KeyKind.ENTER && !state.conversionActive ->
                visualCenterBaseline(target.bounds)
            spec.kind == KeyKind.SPACE && state.mode == KeyboardMode.QWERTY ->
                baselineAtVisualCenter(target.bounds.centerY() + dp(6.5f))
            else -> centerY
        }
        if (idleModifier) {
            textPaint.textSize = sp(10f)
            val cX = safeCenterX(target.bounds, target.bounds.centerX(), "C")
            val aX = safeCenterX(target.bounds, target.bounds.centerX(), "A")
            canvas.drawText("C", cX, safeBaseline(target.bounds, target.bounds.top + dp(13f)), textPaint)
            canvas.drawText("A", aX, safeBaseline(target.bounds, target.bounds.bottom - dp(6f)), textPaint)
        } else if (!animatedEnglish && !animatedEnterPaste && !selectedEnterControlJ && !animatedSpecial) {
            drawMainLabel(canvas, label, target.bounds, safeBaseline(target.bounds, idleMainBaseline), direction == Direction.CENTER,
                0f, if (spec.kind == KeyKind.BACKSPACE) 1f else 4f)
        }
        if (animatedEnglish) {
            val secondaryLabel = requireNotNull(secondary)
            textPaint.textSize = sp(11f) * frame.secondaryScale
            val baseline = baselineAtVisualCenter(target.bounds.top + dp(QWERTY_SECONDARY_IDLE_CENTER_DP + frame.secondaryDy))
            textPaint.color = context.getColor(R.color.keyboard_selected_text)
            textPaint.alpha = (255 * frame.secondaryAlpha).toInt()
            canvas.drawText(secondaryLabel, target.bounds.centerX(), baseline, textPaint)
            textPaint.textSize = sp(22f)
            textPaint.alpha = (255 * frame.mainAlpha).toInt()
            val mainBaseline = baselineAtVisualCenter(target.bounds.centerY() + dp(5f + frame.mainDy))
            val main = if (direction == Direction.UP) spec.up?.label ?: label else spec.center?.label.orEmpty()
            drawFittedText(canvas, main, target.bounds.centerX(), mainBaseline, availableWidth(target.bounds, 0f))
        } else if (animatedEnterPaste) {
            drawSecondaryLabelTransition(canvas, target.bounds, spec.center?.label.orEmpty(), secondary, frame)
        } else if (selectedEnterControlJ) {
            textPaint.textSize = sp(17f)
            textPaint.color = context.getColor(R.color.keyboard_selected_text)
            drawFittedText(
                canvas,
                requireNotNull(spec.value(direction)).label,
                target.bounds.centerX(),
                safeBaseline(target.bounds, visualCenterBaseline(target.bounds)),
                availableWidth(target.bounds, 0f),
            )
        } else if (animatedSpecial) {
            textPaint.textSize = sp(11f)
            val centered = visualCenterBaseline(target.bounds)
            textPaint.color = context.getColor(R.color.keyboard_selected_text)
            drawFittedText(canvas, label, target.bounds.centerX(), safeBaseline(target.bounds, centered), availableWidth(target.bounds, 0f))
        } else if (secondary != null && spec.kind != KeyKind.ENTER) {
            textPaint.textSize = sp(secondaryTextSize(spec))
            val isStackHint = spec.kind in setOf(KeyKind.ENTER, KeyKind.SPACE)
            textPaint.color = when {
                selected -> context.getColor(R.color.keyboard_selected_text)
                isStackHint -> context.getColor(R.color.keyboard_text)
                else -> context.getColor(R.color.keyboard_muted_text)
            }
            textPaint.alpha = if (isStackHint) (255 * .7f).toInt() else 255
            textPaint.letterSpacing = if (isStackHint) dp(.7f) / textPaint.textSize else 0f
            val visualCenter = when {
                spec.kind == KeyKind.ENTER -> target.bounds.height() / density / 2f - 10f
                spec.kind == KeyKind.SPACE && state.mode == KeyboardMode.QWERTY ->
                    target.bounds.height() / density / 2f - 10.5f
                state.mode == KeyboardMode.QWERTY && spec.kind == KeyKind.CHARACTER -> 9f
                else -> 9f
            }
            val baseline = safeBaseline(target.bounds, baselineAtVisualCenter(target.bounds.top + dp(visualCenter)))
            drawFittedText(canvas, secondary, target.bounds.centerX(), baseline, availableWidth(target.bounds, 0f))
            textPaint.letterSpacing = 0f
            textPaint.alpha = 255
        }
        if (spec.kind == KeyKind.ENTER && !state.conversionActive && direction == Direction.CENTER) {
            textPaint.textSize = sp(10f)
            textPaint.color = if (selected) context.getColor(R.color.keyboard_selected_text)
                else context.getColor(R.color.keyboard_text)
            textPaint.alpha = (255 * .7f).toInt()
            textPaint.letterSpacing = dp(.7f) / textPaint.textSize
            listOf("C-j" to target.bounds.top + dp(8f), "paste" to target.bounds.bottom - dp(8f)).forEach { (hint, center) ->
                drawFittedText(canvas, hint, target.bounds.centerX(),
                    safeBaseline(target.bounds, baselineAtVisualCenter(center)), availableWidth(target.bounds, 0f))
            }
            textPaint.letterSpacing = 0f
            textPaint.alpha = 255
        }
        if ((spec.id == "five--" || spec.id == "number-period") && direction == Direction.CENTER) {
            drawNumberFlickHints(canvas, target.bounds, spec, selected)
        }
        canvas.restoreToCount(textSave)
    }

    private fun drawNumberFlickHints(canvas: Canvas, bounds: RectF, spec: KeySpec, selected: Boolean) {
        textPaint.textSize = sp(10f)
        textPaint.color = if (selected) context.getColor(R.color.keyboard_selected_text)
            else context.getColor(R.color.keyboard_text)
        textPaint.alpha = (255 * .7f).toInt()
        textPaint.letterSpacing = dp(.7f) / textPaint.textSize
        listOf(
            Direction.LEFT to bounds.left + dp(9f),
            Direction.RIGHT to bounds.right - dp(9f),
        ).forEach { (direction, x) ->
            spec.value(direction)?.label?.let { hint ->
                drawFittedText(canvas, hint, x, safeBaseline(bounds, visualCenterBaseline(bounds)), availableWidth(bounds, 0f))
            }
        }
        listOf(
            Direction.UP to bounds.top + dp(8f),
            Direction.DOWN to bounds.bottom - dp(8f),
        ).forEach { (direction, centerY) ->
            spec.value(direction)?.label?.let { hint ->
                drawFittedText(
                    canvas,
                    hint,
                    bounds.centerX(),
                    safeBaseline(bounds, baselineAtVisualCenter(centerY)),
                    availableWidth(bounds, 0f),
                )
            }
        }
        textPaint.letterSpacing = 0f
        textPaint.alpha = 255
    }

    private fun drawSecondaryLabelTransition(
        canvas: Canvas,
        bounds: RectF,
        main: String,
        secondary: String,
        frame: LabelFrame,
    ) {
        textPaint.textSize = sp(10f) * frame.secondaryScale
        textPaint.color = context.getColor(R.color.keyboard_selected_text)
        textPaint.alpha = (255 * .7f * frame.secondaryAlpha).toInt()
        textPaint.letterSpacing = dp(.7f * frame.secondaryScale) / textPaint.textSize
        val secondaryBaseline = baselineAtVisualCenter(bounds.bottom - dp(8f - frame.secondaryDy))
        drawFittedText(canvas, secondary, bounds.centerX(), secondaryBaseline, availableWidth(bounds, 0f))
        textPaint.letterSpacing = 0f
        textPaint.textSize = sp(15f)
        textPaint.alpha = (255 * frame.mainAlpha).toInt()
        val mainBaseline = baselineAtVisualCenter(bounds.centerY() + dp(frame.mainDy))
        drawFittedText(canvas, main, bounds.centerX(), mainBaseline, availableWidth(bounds, 0f))
        textPaint.alpha = 255
    }


    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        popupController.refreshTheme()
        invalidate()
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
        spec.id == "punct" -> 18f
        spec.kind == KeyKind.ENTER -> 15f
        spec.kind in setOf(KeyKind.SPACE, KeyKind.MODE, KeyKind.LAYER_SWITCH) -> 16f
        spec.kind == KeyKind.MODIFIER || spec.kind == KeyKind.ACCENT -> 18f
        state.mode == KeyboardMode.QWERTY && (spec.up != null || spec.down != null) -> 22f
        state.mode in setOf(KeyboardMode.QWERTY, KeyboardMode.SYMBOLS) -> 24f
        else -> 25f
    }

    private fun secondaryTextSize(spec: KeySpec) =
        if (spec.kind == KeyKind.CHARACTER || spec.kind == KeyKind.BACKSPACE) 11f else 10f

    private fun usesDownLabelAnimation(spec: KeySpec): Boolean =
        (state.mode == KeyboardMode.QWERTY && spec.kind == KeyKind.CHARACTER) ||
            (spec.kind == KeyKind.BACKSPACE && spec.down != null)

    private fun modifierLabel(spec: KeySpec) = if (spec.kind == KeyKind.MODIFIER) "C/A" else ""

    private fun drawMainLabel(
        canvas: Canvas, label: String, bounds: RectF, y: Float, allowComposite: Boolean,
        xOffsetDp: Float = 0f, edgePaddingDp: Float = 4f,
    ) {
        val x = bounds.centerX() + dp(xOffsetDp)
        if (!allowComposite || label !in setOf("?}", "あん", "AZ", "19")) {
            val maxWidth = (bounds.width() - dp(edgePaddingDp * 2f) - 2f * kotlin.math.abs(dp(xOffsetDp)))
                .coerceAtLeast(dp(4f))
            drawFittedText(canvas, label, x, y, maxWidth)
            return
        }
        val main = label.substring(0, 1)
        val ghost = label.substring(1)
        val originalSize = textPaint.textSize
        val originalColor = textPaint.color
        val originalAlpha = textPaint.alpha
        val mainVisualCenter = y + (textPaint.ascent() + textPaint.descent()) / 2f
        textPaint.textSize = originalSize * .75f
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
        spec.kind == KeyKind.CHARACTER && (spec.center?.action is KeyAction.CommitText || spec.center?.action is KeyAction.CommitEmoji)

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (previewOnly) return true
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> pointerDown(event, event.actionIndex)
            MotionEvent.ACTION_MOVE -> for (i in 0 until event.pointerCount) pointerMove(event, i)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                // The final coordinate may arrive only with UP (without a preceding MOVE).
                // Reconcile the selected direction before committing the key.
                pointerMove(event, event.actionIndex, updateAccentSelection = false)
                pointerUp(event.getPointerId(event.actionIndex))
            }
            MotionEvent.ACTION_CANCEL -> cancelActiveGestures()
        }
        return true
    }

    public override fun dispatchHoverEvent(event: MotionEvent): Boolean {
        val handled = accessibilityHelper.dispatchHoverEvent(event) || super.dispatchHoverEvent(event)
        voiceSessionStatusHovered = when (event.actionMasked) {
            MotionEvent.ACTION_HOVER_EXIT -> false
            MotionEvent.ACTION_HOVER_ENTER, MotionEvent.ACTION_HOVER_MOVE ->
                voiceSessionStatusBounds()?.contains(event.x.toInt(), event.y.toInt()) == true
            else -> voiceSessionStatusHovered
        }
        return handled
    }

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        if (state.mode == KeyboardMode.EMOJI && emojiScrollRange() > 0f) {
            info.isScrollable = true
            if (emojiScrollOffset < emojiScrollRange()) {
                info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD)
            }
            if (emojiScrollOffset > 0f) {
                info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD)
            }
        }
    }

    override fun performAccessibilityAction(action: Int, arguments: android.os.Bundle?): Boolean {
        if (state.mode == KeyboardMode.EMOJI && action in setOf(
                AccessibilityNodeInfo.ACTION_SCROLL_FORWARD,
                AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD,
            )) {
            val delta = if (action == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) currentEmojiRowPitch() else -currentEmojiRowPitch()
            return scrollEmojiTo(emojiScrollOffset + delta)
        }
        return super.performAccessibilityAction(action, arguments)
    }

    private fun directionLabel(direction: Direction) = when (direction) {
        Direction.CENTER -> "タップ"; Direction.LEFT -> "左"; Direction.UP -> "上"; Direction.RIGHT -> "右"; Direction.DOWN -> "下"
    }

    private fun describe(spec: KeySpec): String = Direction.entries.mapNotNull { direction ->
        spec.value(direction)?.takeUnless { it.action == KeyAction.VoiceHold }?.label?.let { label ->
            "${directionLabel(direction)} $label"
        }
    }.joinToString("、").ifEmpty { if (spec.kind == KeyKind.MODIFIER) "上 Alt、下 Ctrl" else "入力なし" }

    private inner class KeyboardAccessibilityHelper(host: View) : ExploreByTouchHelper(host) {
        override fun getVirtualViewAt(x: Float, y: Float): Int {
            val statusBounds = voiceSessionStatusBounds()
            if (statusBounds != null && statusBounds.contains(x.toInt(), y.toInt())) {
                return VOICE_SESSION_STATUS_VIRTUAL_ID
            }
            return hitTargetIndexAt(x, y).takeIf { it >= 0 } ?: INVALID_ID
        }

        override fun getVisibleVirtualViews(virtualViewIds: MutableList<Int>) {
            hitTargets.indices.forEach { id ->
                val target = hitTargets[id]
                if (target.spec.kind == KeyKind.EMPTY || !isTargetVisible(target)) return@forEach
                virtualViewIds += id
                if (target.spec.id == "voice-cancel" && voiceSessionStatusBounds() != null) {
                    virtualViewIds += VOICE_SESSION_STATUS_VIRTUAL_ID
                }
            }
        }

        override fun onPopulateNodeForVirtualView(virtualViewId: Int, node: AccessibilityNodeInfoCompat) {
            if (virtualViewId == VOICE_SESSION_STATUS_VIRTUAL_ID) {
                // ExploreByTouchHelper may ask for an ID which was focused just before the
                // listening state disappeared. Keep that delayed lookup well-formed while
                // making it non-visible; otherwise it throws before the focus-clear event can
                // be delivered.
                val bounds = voiceSessionStatusBounds()
                node.className = "android.widget.TextView"
                node.contentDescription = "認識中"
                node.isFocusable = bounds != null
                node.isClickable = false
                node.isVisibleToUser = bounds != null
                node.setBoundsInParent(bounds ?: android.graphics.Rect(-2, -2, -1, -1))
                return
            }
            val target = hitTargets.getOrNull(virtualViewId) ?: return
            val bounds = visibleBounds(target) ?: return
            node.className = "android.widget.Button"
            node.contentDescription = describe(target.spec)
            node.setBoundsInParent(android.graphics.Rect(
                bounds.left.toInt(), bounds.top.toInt(), bounds.right.toInt(), bounds.bottom.toInt()))
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
                    if (it.action != KeyAction.VoiceHold) {
                        node.addAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat(id, "${directionLabel(direction)} ${it.label}"))
                    }
                }
            }
        }

        override fun onPerformActionForVirtualView(virtualViewId: Int, action: Int, arguments: android.os.Bundle?): Boolean {
            if (previewOnly) return false
            if (virtualViewId == VOICE_SESSION_STATUS_VIRTUAL_ID) return false
            val target = hitTargets.getOrNull(virtualViewId) ?: return false
            if (!isTargetVisible(target)) return false
            val direction = when (action) {
                AccessibilityNodeInfoCompat.ACTION_CLICK -> Direction.CENTER
                ACTION_FLICK_LEFT -> Direction.LEFT
                ACTION_FLICK_UP -> Direction.UP
                ACTION_FLICK_RIGHT -> Direction.RIGHT
                ACTION_FLICK_DOWN -> Direction.DOWN
                else -> return false
            }
            val selected = target.spec.value(direction)
            if (selected?.action == KeyAction.VoiceHold) return false
            if (selected == null && !(target.spec.kind == KeyKind.MODIFIER && direction == Direction.CENTER)) return false
            dispatch(target.spec, direction)
            sendEventForVirtualView(virtualViewId, AccessibilityEvent.TYPE_VIEW_CLICKED)
            invalidateVirtualView(virtualViewId)
            return true
        }

        fun announceVoiceSessionStarted() {
            sendEventForVirtualView(VOICE_SESSION_STATUS_VIRTUAL_ID, AccessibilityEvent.TYPE_ANNOUNCEMENT)
        }

        fun clearVoiceSessionStatusFocus() {
            if (voiceSessionStatusHovered) {
                // ExploreByTouchHelper owns the hovered-ID state. Send it an actual exit so
                // a later hover cannot emit a second exit for this vanished status node.
                val exit = MotionEvent.obtain(0, 0, MotionEvent.ACTION_HOVER_EXIT, -1f, -1f, 0)
                try {
                    this@KeyboardView.dispatchHoverEvent(exit)
                } finally {
                    exit.recycle()
                }
            }
            if (getAccessibilityFocusedVirtualViewId() == VOICE_SESSION_STATUS_VIRTUAL_ID) {
                getAccessibilityNodeProvider(this@KeyboardView)?.performAction(
                    VOICE_SESSION_STATUS_VIRTUAL_ID,
                    AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS,
                    null,
                )
            }
            clearKeyboardFocusForVirtualView(VOICE_SESSION_STATUS_VIRTUAL_ID)
            invalidateVirtualView(VOICE_SESSION_STATUS_VIRTUAL_ID)
        }
    }

    private fun pointerDown(event: MotionEvent, index: Int) {
        val id = event.getPointerId(index)
        if (voiceEntryPointer != null) return
        val hit = hitTargets.getOrNull(hitTargetIndexAt(event.getX(index), event.getY(index))) ?: return
        if (active.isNotEmpty()) {
            voiceHoldMultiPointer = true
            if (voiceHoldOwner != null) {
                voiceGestureCancelledPointers.addAll(active.keys)
                voiceGestureCancelledPointers += id
            }
            cancelVoiceGesture()
        }
        active[id] = hit
        if (hit.scrollable) emojiScrollGestures[id] = EmojiScrollGesture(event.getY(index), emojiScrollOffset)
        directions[id] = Direction.CENTER
        labelFrames[id] = LabelFrame()
        val verticalOnly = hit.spec.kind == KeyKind.MODIFIER ||
            (hit.spec.kind == KeyKind.BACKSPACE && hit.spec.center == null) ||
            (hit.spec.kind == KeyKind.CHARACTER && hit.spec.up != null && hit.spec.down != null &&
                hit.spec.left == null && hit.spec.right == null)
        interpreter.start(
            id,
            event.getX(index) / density,
            event.getY(index) / density,
            trackpad = hit.spec.kind == KeyKind.SPACE,
            verticalOnly = verticalOnly,
            pointerThresholds = flickThresholdsFor(state.mode),
        )
        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        if (hit.spec.kind == KeyKind.CHARACTER ||
            (hit.spec.kind == KeyKind.BACKSPACE && hit.spec.center != null && hit.spec.id != "voice-backspace")) {
            scheduleTimer(id, hit, Direction.CENTER)
        }
        sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_HOVER_ENTER)
        syncPopup(id)
        invalidate()
    }

    private fun flickThresholdsFor(mode: KeyboardMode): GestureThresholds = when (mode) {
        KeyboardMode.KANA, KeyboardMode.NUMBERS -> kanaNumberFlickSensitivity.thresholds()
        KeyboardMode.QWERTY, KeyboardMode.SYMBOLS -> qwertySymbolFlickSensitivity.thresholds()
        KeyboardMode.EMOJI, KeyboardMode.VOICE -> GestureThresholds()
    }

    private fun pointerMove(event: MotionEvent, index: Int, updateAccentSelection: Boolean = true) {
        val id = event.getPointerId(index)
        if (voiceHoldOwner?.first == id) return
        emojiScrollGestures[id]?.let { gesture ->
            val dy = event.getY(index) - gesture.startY
            if (gesture.scrolling || abs(dy) >= dp(12f)) {
                if (!gesture.scrolling) {
                    gesture.scrolling = true
                    discardPointer(id)
                }
                scrollEmojiTo(gesture.startOffset - dy)
                return
            }
        }
        if (updateAccentSelection && id in accentActive) {
            val hit = active[id]
            val choices = hit?.spec?.center?.label?.let(::accentChoices)
            if (hit != null && choices != null) {
                accentSelected[id] = popupController.accentIndexFor(this, hit.bounds, choices.size, event.getX(index))
            }
        }
        when (val update = interpreter.move(id, event.getX(index) / density, event.getY(index) / density)) {
            is GestureUpdate.Selection -> {
                val activeHit = active[id]
                val assigned = activeHit?.spec?.value(update.direction) != null
                val direction = update.direction.takeIf { assigned } ?: Direction.CENTER
                if (!assigned && update.direction != Direction.CENTER) cancelTimer(id)
                if (directions[id] != direction) {
                    if (direction == update.direction) {
                        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    }
                    directions[id] = direction
                    cancelTimer(id)
                    if (activeHit?.spec?.down?.action is KeyAction.Backspace && direction == Direction.DOWN) {
                        scheduleTimer(id, activeHit, Direction.DOWN)
                    }
                    animateLabels(id, direction)
                    if (activeHit != null && activeHit.spec.value(direction)?.action == KeyAction.VoiceHold) {
                        beginVoiceGesture(id, activeHit)
                    }
                }
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
        val emojiGesture = emojiScrollGestures.remove(id)
        if (emojiGesture?.scrolling == true) return
        cancelTimer(id)
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
        if (voiceGestureCancelledPointers.remove(id)) {
            cursorMoved.remove(id)
            repeated.remove(id)
            accentActive.remove(id)
            accentSelected.remove(id)
            if (active.isEmpty()) {
                voiceHoldMultiPointer = false
                voiceGestureCancelledPointers.clear()
                voiceEntryPointer = null
            }
            dismissPopup()
            active.keys.firstOrNull()?.let(::syncPopup)
            invalidate()
            return
        }
        val accents = if (accentActive.remove(id)) accentChoices(hit.spec.center?.label.orEmpty()) else null
        if (accents != null) actionSink?.onKeyAction(KeyAction.CommitText(accents[accentSelected.remove(id) ?: 0]))
        else if (!cursorMoved.remove(id) && !repeated.remove(id)) dispatch(hit.spec, direction)
        cursorMoved.remove(id)
        if (active.isEmpty()) voiceHoldMultiPointer = false
        dismissPopup()
        active.keys.firstOrNull()?.let(::syncPopup)
        invalidate()
    }

    private fun beginVoiceGesture(id: Int, hit: HitTarget) {
        if (voiceHoldMultiPointer || active.size != 1 || active[id] != hit || voiceHoldOwner != null) return
        voiceGestureCancelledPointers += id
        voiceEntryPointer = id
        interpreter.cancel(id)
        actionSink?.onKeyAction(KeyAction.VoiceHold)
    }

    private fun cancelVoiceGesture() {
        val requestId = voiceHoldOwner?.second ?: return
        voiceHoldOwner = null
        voiceReadyRequestId = null
        secondVoiceHaptic?.let(::removeCallbacks)
        secondVoiceHaptic = null
        voiceHoldSink?.onVoiceHold(VoiceHoldEvent.Cancel(requestId))
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
            KeyAction.VoiceHold -> Unit
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
        val target = active[id]
        val action = target?.spec?.value(direction)?.action
        val enterPaste = target?.spec?.kind == KeyKind.ENTER && action == KeyAction.Paste
        val immediateSecondaryAlpha = if (direction == Direction.UP && !enterPaste) 0f else 1f
        val start = (labelFrames[id] ?: LabelFrame()).copy(secondaryAlpha = immediateSecondaryAlpha)
        val downSecondaryDy = active[id]?.takeIf { target ->
            usesDownLabelAnimation(target.spec)
        }?.let { target ->
            target.bounds.height() / (2f * density) - QWERTY_SECONDARY_IDLE_CENTER_DP
        } ?: 13f
        val enterPasteDy = target?.bounds?.height()?.div(2f * density)?.let { 8f - it } ?: 0f
        val end = when {
            enterPaste -> LabelFrame(mainAlpha = 0f, secondaryDy = enterPasteDy, secondaryScale = 1.7f)
            direction == Direction.UP -> LabelFrame(mainDy = -3f, secondaryAlpha = 0f)
            direction == Direction.DOWN -> LabelFrame(mainDy = 22f, mainAlpha = 0f, secondaryDy = downSecondaryDy, secondaryScale = 1.7f)
            else -> LabelFrame()
        }
        if (Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f) {
            labelFrames[id] = end
            invalidate()
            return
        }
        labelFrames[id] = start
        invalidate()
        labelAnimators[id] = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = LABEL_ANIMATION_MS; interpolator = PathInterpolator(.25f, .1f, .25f, 1f)
            addUpdateListener {
                val p = it.animatedValue as Float
                labelFrames[id] = LabelFrame(
                    mainDy = start.mainDy + (end.mainDy - start.mainDy) * p,
                    mainAlpha = start.mainAlpha + (end.mainAlpha - start.mainAlpha) * p,
                    secondaryDy = start.secondaryDy + (end.secondaryDy - start.secondaryDy) * p,
                    secondaryScale = start.secondaryScale + (end.secondaryScale - start.secondaryScale) * p,
                    secondaryAlpha = immediateSecondaryAlpha,
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
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            (v as? KeyboardView)?.applySystemBottomInset(systemBarBottomInset(insets))
            insets
        }
    }
}

/** Keeps keys above both visible system UI and the home-gesture priority area. */
internal fun systemBarBottomInset(insets: WindowInsetsCompat): Int =
    max(
        insets.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.systemBars()).bottom,
        insets.getInsets(WindowInsetsCompat.Type.systemGestures()).bottom,
    )
