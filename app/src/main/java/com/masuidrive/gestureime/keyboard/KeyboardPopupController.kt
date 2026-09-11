package com.masuidrive.gestureime.keyboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.Gravity
import android.view.View
import android.widget.PopupWindow
import kotlin.math.floor

/**
 * Owns the one non-interactive flick preview window for a [KeyboardView].
 *
 * The controller retains only key labels while visible.  [dismiss] clears that
 * request as well as the render view, so an old IME/editor view cannot retain
 * a popup after it is detached.
 */
class KeyboardPopupController(private val context: Context) {
    private data class Request(
        val anchor: View,
        val targetBounds: RectF,
        val spec: KeySpec,
        val direction: Direction,
        val accentChoices: List<String>,
        val accentIndex: Int,
    )

    private var request: Request? = null
    private var window: PopupWindow? = null
    private var renderView: KeyboardPopupRenderView? = null
    private var observedAnchor: View? = null
    private var shownPosition: PopupPosition? = null
    private var shownSize: PopupSize? = null

    private val attachListener = object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) = Unit
        override fun onViewDetachedFromWindow(v: View) = dismiss()
    }
    private val layoutListener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> refresh() }

    /** Shows or updates the one popup. A later pointer's call replaces the visible preview. */
    fun show(
        anchor: View,
        targetBounds: RectF,
        spec: KeySpec,
        direction: Direction,
        accentChoices: List<String> = emptyList(),
        accentIndex: Int = 0,
    ) {
        if (!anchor.isAttachedToWindow) {
            dismiss()
            return
        }
        observe(anchor)
        request = Request(anchor, RectF(targetBounds), spec, direction, accentChoices.toList(), accentIndex)
        refresh()
    }

    /** Hides the preview and drops all references to its anchor and labels. */
    fun dismiss() {
        observedAnchor?.removeOnAttachStateChangeListener(attachListener)
        observedAnchor?.removeOnLayoutChangeListener(layoutListener)
        observedAnchor = null
        request = null
        renderView?.clear()
        window?.let { popup -> runCatching { if (popup.isShowing) popup.dismiss() } }
        window = null
        renderView = null
        shownPosition = null
        shownSize = null
    }

    /**
     * Returns the accent choice under a local-x pointer.  KeyboardView owns the
     * active pointer; this is only shared geometry so drawing and hit selection
     * use the source rule: key-left minus 8dp, then clamp, then 3dp padding.
     */
    fun accentIndexFor(
        anchor: View,
        targetBounds: RectF,
        choiceCount: Int,
        pointerX: Float,
    ): Int {
        if (choiceCount <= 1) return 0
        val geometry = PopupGeometry(density())
        val size = geometry.windowSize(PopupKind.ACCENT, choiceCount)
        val frame = Rect().also(anchor::getWindowVisibleDisplayFrame)
        val placement = geometry.placement(anchor, targetBounds, PopupKind.ACCENT, choiceCount, size, frame)
        val anchorLocation = IntArray(2).also(anchor::getLocationOnScreen)
        val parentLeftInAnchor = placement.contentLeft - anchorLocation[0]
        return floor((pointerX - parentLeftInAnchor - geometry.accentPadding) / geometry.accentTile)
            .toInt().coerceIn(0, choiceCount - 1)
    }

    internal fun isShowingForTest(): Boolean = window?.isShowing == true
    internal fun popupForTest(): PopupWindow? = window

    private fun observe(anchor: View) {
        if (observedAnchor === anchor) return
        observedAnchor?.removeOnAttachStateChangeListener(attachListener)
        observedAnchor?.removeOnLayoutChangeListener(layoutListener)
        observedAnchor = anchor
        anchor.addOnAttachStateChangeListener(attachListener)
        anchor.addOnLayoutChangeListener(layoutListener)
    }

    private fun refresh() {
        val current = request ?: return
        val anchor = current.anchor
        if (!anchor.isAttachedToWindow) {
            dismiss()
            return
        }
        val geometry = PopupGeometry(density())
        val kind = popupKind(current.spec, current.accentChoices)
        val size = geometry.windowSize(kind, current.accentChoices.size)
        val frame = Rect().also(anchor::getWindowVisibleDisplayFrame)
        val placement = geometry.placement(anchor, current.targetBounds, kind, current.accentChoices.size, size, frame)
        val renderer = renderView ?: KeyboardPopupRenderView(context, geometry).also { renderView = it }
        renderer.bind(kind, current.spec, current.direction, current.accentChoices, current.accentIndex, size, placement.contentOffsetX, placement.contentOffsetY)
        val popup = window ?: PopupWindow(renderer, size.width, size.height, false).also { created ->
            created.isTouchable = false
            created.isFocusable = false
            created.isOutsideTouchable = false
            created.isClippingEnabled = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) created.setIsClippedToScreen(true)
            created.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window = created
        }
        try {
            if (popup.isShowing) {
                if (shownPosition != placement.position || shownSize != size) popup.update(placement.position.x, placement.position.y, size.width, size.height)
            } else popup.showAtLocation(anchor, Gravity.TOP or Gravity.START, placement.position.x, placement.position.y)
            shownPosition = placement.position
            shownSize = size
        } catch (_: RuntimeException) {
            // BadToken and a detached IME window are expected races during view teardown.
            dismiss()
        }
    }

    private fun popupKind(spec: KeySpec, accents: List<String>) = when {
        accents.isNotEmpty() -> PopupKind.ACCENT
        spec.kind == KeyKind.KANA -> PopupKind.KANA
        spec.kind == KeyKind.MODIFIER -> PopupKind.MODIFIER
        else -> PopupKind.LETTER
    }

    private fun density() = context.resources.displayMetrics.density
}

internal enum class PopupKind { KANA, LETTER, MODIFIER, ACCENT }

internal data class PopupSize(val width: Int, val height: Int)
internal data class PopupPosition(val x: Int, val y: Int)
internal data class PopupPlacement(
    val position: PopupPosition,
    val contentOffsetX: Float,
    val contentOffsetY: Float,
    val contentLeft: Float,
)

/** Pixel geometry shared by preview drawing, placement, and accent hit testing. */
internal class PopupGeometry(private val density: Float) {
    val tile = dp(50f)
    val accentTile = dp(34f)
    val accentPadding = dp(3f)
    private val shadowHorizontalInset = dp(7f)
    private val shadowTopInset = dp(7f)
    private val shadowBottomInset = dp(10f) // 3dp offset + 7dp blur

    fun windowSize(kind: PopupKind, accentCount: Int): PopupSize = when (kind) {
        PopupKind.KANA -> PopupSize((tile * 3 + shadowHorizontalInset * 2).toInt(), (tile * 3 + shadowTopInset + shadowBottomInset).toInt())
        PopupKind.LETTER -> PopupSize(dp(58f + 14f).toInt(), (dp(66f) + shadowTopInset + shadowBottomInset).toInt())
        PopupKind.MODIFIER -> PopupSize(dp(90f + 14f).toInt(), (dp(66f) + shadowTopInset + shadowBottomInset).toInt())
        PopupKind.ACCENT -> PopupSize((accentTile * accentCount + accentPadding * 2 + shadowHorizontalInset * 2).toInt(), (dp(54f) + shadowTopInset + shadowBottomInset).toInt())
    }

    fun accentParent(target: RectF, count: Int, anchorWidth: Float): RectF {
        val width = accentTile * count + accentPadding * 2
        val left = (target.left - dp(8f)).coerceIn(0f, (anchorWidth - width).coerceAtLeast(0f))
        return RectF(left, target.top - dp(56f), left + width, target.top - dp(2f))
    }

    fun placement(
        anchor: View,
        target: RectF,
        kind: PopupKind,
        accentCount: Int,
        size: PopupSize,
        visibleFrame: Rect,
    ): PopupPlacement {
        // The visible frame and getLocationOnScreen() share screen coordinates,
        // while PopupWindow.showAtLocation() takes coordinates in the IME window.
        // Keep the two coordinate spaces separate: clamping and accent hit testing
        // are screen based, then the final surface is converted for the window API.
        val screenLocation = IntArray(2).also(anchor::getLocationOnScreen)
        val windowLocation = IntArray(2).also(anchor::getLocationInWindow)
        val content = when (kind) {
            PopupKind.KANA -> RectF(target.centerX() - tile * 1.5f, target.top - dp(50f), target.centerX() + tile * 1.5f, target.top + tile * 2f)
            PopupKind.LETTER, PopupKind.MODIFIER -> {
                val width = if (kind == PopupKind.MODIFIER) dp(90f) else dp(58f)
                RectF(target.centerX() - width / 2, target.top - dp(62f), target.centerX() + width / 2, target.top + dp(4f))
            }
            PopupKind.ACCENT -> accentParent(target, accentCount, anchor.width.toFloat())
        }
        val contentWidth = content.width()
        val contentHeight = content.height()
        val xInAnchor = content.left.coerceIn(0f, (anchor.width - contentWidth).coerceAtLeast(0f))
        val yInAnchor = content.top
        val desiredContentScreenX = screenLocation[0] + xInAnchor
        val desiredContentScreenY = screenLocation[1] + yInAnchor
        // Clamp the visual primitive first. Clamping only the larger shadow
        // surface would leave its content at a negative local offset.
        val contentScreenX = desiredContentScreenX.coerceIn(visibleFrame.left.toFloat(), (visibleFrame.right - contentWidth).coerceAtLeast(visibleFrame.left.toFloat()))
        val contentScreenY = desiredContentScreenY.coerceIn(visibleFrame.top.toFloat(), (visibleFrame.bottom - contentHeight).coerceAtLeast(visibleFrame.top.toFloat()))
        val unclampedX = contentScreenX - shadowHorizontalInset
        val unclampedY = contentScreenY - shadowTopInset
        val maxX = (visibleFrame.right - size.width).coerceAtLeast(visibleFrame.left)
        val maxY = (visibleFrame.bottom - size.height).coerceAtLeast(visibleFrame.top)
        val surfaceScreenPosition = PopupPosition(
            unclampedX.toInt().coerceIn(visibleFrame.left, maxX),
            unclampedY.toInt().coerceIn(visibleFrame.top, maxY),
        )
        val position = windowPosition(surfaceScreenPosition, windowLocation[0], windowLocation[1])
        val contentOffsetX = contentScreenX - surfaceScreenPosition.x
        val contentOffsetY = contentScreenY - surfaceScreenPosition.y
        return PopupPlacement(position, contentOffsetX, contentOffsetY, surfaceScreenPosition.x + contentOffsetX)
    }

    internal fun windowPosition(surfaceScreenPosition: PopupPosition, windowLeftOnScreen: Int, windowTopOnScreen: Int) =
        PopupPosition(surfaceScreenPosition.x - windowLeftOnScreen, surfaceScreenPosition.y - windowTopOnScreen)

    fun tileRect(direction: Direction, size: PopupSize, contentOffsetX: Float = shadowHorizontalInset, contentOffsetY: Float = shadowTopInset): RectF {
        val left = contentOffsetX
        val top = contentOffsetY
        val column = when (direction) { Direction.LEFT -> 0; Direction.RIGHT -> 2; else -> 1 }
        val row = when (direction) { Direction.UP -> 0; Direction.DOWN -> 2; else -> 1 }
        return RectF(left + column * tile, top + row * tile, left + (column + 1) * tile, top + (row + 1) * tile)
    }

    fun contentRect(kind: PopupKind, size: PopupSize, contentOffsetX: Float = shadowHorizontalInset, contentOffsetY: Float = shadowTopInset): RectF = when (kind) {
        PopupKind.KANA -> RectF(contentOffsetX, contentOffsetY, contentOffsetX + tile * 3f, contentOffsetY + tile * 3f)
        PopupKind.LETTER -> RectF(contentOffsetX, contentOffsetY, contentOffsetX + dp(58f), contentOffsetY + dp(66f))
        PopupKind.MODIFIER -> RectF(contentOffsetX, contentOffsetY, contentOffsetX + dp(90f), contentOffsetY + dp(66f))
        PopupKind.ACCENT -> RectF(contentOffsetX, contentOffsetY, contentOffsetX + accentTile * ((size.width - shadowHorizontalInset * 2 - accentPadding * 2) / accentTile).toInt() + accentPadding * 2, contentOffsetY + dp(54f))
    }

    fun dp(value: Float) = value * density
}

internal class KeyboardPopupRenderView(
    context: Context,
    private val geometry: PopupGeometry = PopupGeometry(context.resources.displayMetrics.density),
) : View(context) {
    private var kind: PopupKind? = null
    private var spec: KeySpec? = null
    private var direction = Direction.CENTER
    private var accents: List<String> = emptyList()
    private var accentIndex = 0
    private var popupSize = PopupSize(1, 1)
    private var contentOffsetX = geometry.dp(7f)
    private var contentOffsetY = geometry.dp(7f)
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val border = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = geometry.dp(1f) }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }

    init {
        setWillNotDraw(false)
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    fun bind(kind: PopupKind, spec: KeySpec, direction: Direction, accents: List<String>, accentIndex: Int, size: PopupSize, contentOffsetX: Float, contentOffsetY: Float) {
        val changed = this.kind != kind || this.spec != spec || this.direction != direction || this.accents != accents || this.accentIndex != accentIndex || popupSize != size || this.contentOffsetX != contentOffsetX || this.contentOffsetY != contentOffsetY
        this.kind = kind
        this.spec = spec
        this.direction = direction
        this.accents = accents
        this.accentIndex = accentIndex.coerceIn(0, (accents.size - 1).coerceAtLeast(0))
        popupSize = size
        this.contentOffsetX = contentOffsetX
        this.contentOffsetY = contentOffsetY
        if (changed) invalidate()
    }

    fun clear() {
        kind = null
        spec = null
        accents = emptyList()
        accentIndex = 0
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val currentKind = kind ?: return
        val currentSpec = spec ?: return
        when (currentKind) {
            PopupKind.KANA -> drawKana(canvas, currentSpec)
            PopupKind.LETTER, PopupKind.MODIFIER -> drawLetter(canvas, currentKind, currentSpec)
            PopupKind.ACCENT -> drawAccents(canvas)
        }
    }

    private fun drawKana(canvas: Canvas, currentSpec: KeySpec) {
        val tiles = Direction.entries.associateWith { geometry.tileRect(it, popupSize, contentOffsetX, contentOffsetY) }
        val silhouette = Path().apply { tiles.values.forEach { addRoundRect(it, geometry.dp(6f), geometry.dp(6f), Path.Direction.CW) } }
        fill.color = POPUP
        fill.setShadowLayer(geometry.dp(7f), 0f, geometry.dp(3f), SHADOW)
        canvas.drawPath(silhouette, fill)
        fill.clearShadowLayer()
        Direction.entries.forEach { tileDirection ->
            val label = currentSpec.value(tileDirection)?.label ?: return@forEach
            drawTile(canvas, requireNotNull(tiles[tileDirection]), label, tileDirection == direction, 27f, true)
        }
    }

    private fun drawLetter(canvas: Canvas, currentKind: PopupKind, currentSpec: KeySpec) {
        val rect = geometry.contentRect(currentKind, popupSize, contentOffsetX, contentOffsetY)
        val modifier = currentKind == PopupKind.MODIFIER
        fill.color = if (modifier) SELECTED else POPUP
        fill.setShadowLayer(geometry.dp(7f), 0f, geometry.dp(3f), SHADOW)
        canvas.drawPath(letterPath(rect), fill)
        fill.clearShadowLayer()
        border.color = if (modifier) SELECTED else POPUP_BORDER
        canvas.drawPath(letterPath(RectF(rect).apply { inset(geometry.dp(.5f), geometry.dp(.5f)) }), border)
        val label = currentSpec.value(direction)?.label ?: currentSpec.center?.label ?: return
        text.color = if (modifier) SELECTED_INK else POPUP_INK
        text.textSize = sp(if (modifier) 24f else 40f)
        drawCentered(canvas, label, rect, rect.width() - geometry.dp(20f))
    }

    private fun drawAccents(canvas: Canvas) {
        if (accents.isEmpty()) return
        val parentWidth = geometry.accentTile * accents.size + geometry.accentPadding * 2
        val parent = RectF(contentOffsetX, contentOffsetY, contentOffsetX + parentWidth, contentOffsetY + geometry.dp(54f))
        fill.color = POPUP
        fill.setShadowLayer(geometry.dp(7f), 0f, geometry.dp(3f), SHADOW)
        canvas.drawRoundRect(parent, geometry.dp(8f), geometry.dp(8f), fill)
        fill.clearShadowLayer()
        border.color = POPUP_BORDER
        canvas.drawRoundRect(RectF(parent).apply { inset(geometry.dp(.5f), geometry.dp(.5f)) }, geometry.dp(8f), geometry.dp(8f), border)
        accents.forEachIndexed { index, label ->
            val rect = RectF(parent.left + geometry.accentPadding + geometry.accentTile * index, parent.top + geometry.accentPadding,
                parent.left + geometry.accentPadding + geometry.accentTile * (index + 1), parent.top + geometry.accentPadding + geometry.dp(48f))
            drawTile(canvas, rect, label, index == accentIndex, 25f, false)
        }
    }

    private fun drawTile(canvas: Canvas, rect: RectF, label: String, selected: Boolean, textSize: Float, bordered: Boolean) {
        fill.color = if (selected) SELECTED else POPUP
        canvas.drawRoundRect(rect, geometry.dp(6f), geometry.dp(6f), fill)
        if (bordered) {
            border.color = if (selected) SELECTED else POPUP_BORDER
            canvas.drawRoundRect(RectF(rect).apply { inset(geometry.dp(.5f), geometry.dp(.5f)) }, geometry.dp(6f), geometry.dp(6f), border)
        }
        text.color = if (selected) SELECTED_INK else POPUP_INK
        text.textSize = sp(textSize)
        drawCentered(canvas, label, rect, rect.width() - geometry.dp(6f))
    }

    private fun letterPath(rect: RectF) = Path().apply {
        addRoundRect(rect, floatArrayOf(geometry.dp(9f), geometry.dp(9f), geometry.dp(9f), geometry.dp(9f), geometry.dp(5f), geometry.dp(5f), geometry.dp(5f), geometry.dp(5f)), Path.Direction.CW)
    }

    private fun drawCentered(canvas: Canvas, label: String, bounds: RectF, maxWidth: Float) {
        val original = text.textSize
        val measured = text.measureText(label)
        if (measured > maxWidth && measured > 0f) text.textSize = original * maxWidth / measured
        canvas.drawText(label, bounds.centerX(), bounds.centerY() - (text.ascent() + text.descent()) / 2f, text)
        text.textSize = original
    }

    private fun sp(value: Float) = value * resources.displayMetrics.density

    private companion object {
        const val POPUP = 0xff55555a.toInt()
        const val POPUP_INK = 0xfff4f4f6.toInt()
        const val POPUP_BORDER = 0xff74747a.toInt()
        const val SELECTED = 0xffa8ceff.toInt()
        const val SELECTED_INK = 0xff102844.toInt()
        const val SHADOW = 0x88000000.toInt()
    }
}
