package com.masuidrive.gestureime.keyboard

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.sign

data class GestureThresholds(
    val axisLockDp: Float = 12f,
    val selectionDp: Float = 18f,
    val returnHysteresisDp: Float = 10f,
    val trackpadStartDp: Float = 10f,
    val horizontalUnitDp: Float = 8f,
    val verticalUnitDp: Float = 24f,
)

enum class GestureAxis { HORIZONTAL, VERTICAL }

sealed interface GestureUpdate {
    data class Selection(val direction: Direction) : GestureUpdate
    data class CursorDelta(val direction: Direction, val units: Int) : GestureUpdate
}

class GestureInterpreter(private val thresholds: GestureThresholds = GestureThresholds()) {
    private data class PointerState(
        val startX: Float,
        val startY: Float,
        val trackpad: Boolean,
        val verticalOnly: Boolean,
        var direction: Direction = Direction.CENTER,
        var axis: GestureAxis? = null,
        var reportedHorizontalSelection: Boolean = false,
        var emittedUnits: Int = 0,
    )

    private val pointers = mutableMapOf<Int, PointerState>()

    fun start(pointerId: Int, xDp: Float, yDp: Float, trackpad: Boolean = false, verticalOnly: Boolean = false) {
        pointers[pointerId] = PointerState(xDp, yDp, trackpad, verticalOnly)
    }

    fun move(pointerId: Int, xDp: Float, yDp: Float): GestureUpdate? {
        val state = pointers[pointerId] ?: return null
        val dx = xDp - state.startX
        val dy = yDp - state.startY
        if (state.trackpad) return moveTrackpad(state, dx, dy)

        val distance = hypot(dx, dy)
        if (state.verticalOnly) {
            if (distance <= thresholds.returnHysteresisDp) {
                state.axis = null
                state.reportedHorizontalSelection = false
                if (state.direction != Direction.CENTER) {
                    state.direction = Direction.CENTER
                    return GestureUpdate.Selection(Direction.CENTER)
                }
            }
            if (state.axis == null && distance >= thresholds.axisLockDp) {
                state.axis = if (abs(dx) >= abs(dy)) GestureAxis.HORIZONTAL else GestureAxis.VERTICAL
            }
            if (state.axis == GestureAxis.HORIZONTAL) {
                if (distance < thresholds.selectionDp || state.reportedHorizontalSelection) return null
                state.reportedHorizontalSelection = true
                return GestureUpdate.Selection(if (dx < 0) Direction.LEFT else Direction.RIGHT)
            }
            if (abs(dy) < thresholds.selectionDp) return null
            val next = if (dy < 0) Direction.UP else Direction.DOWN
            if (next == state.direction) return null
            state.direction = next
            return GestureUpdate.Selection(next)
        }
        val next = when {
            state.direction != Direction.CENTER && distance <= thresholds.returnHysteresisDp -> Direction.CENTER
            distance < thresholds.selectionDp -> state.direction
            abs(dx) >= abs(dy) -> if (dx < 0) Direction.LEFT else Direction.RIGHT
            else -> if (dy < 0) Direction.UP else Direction.DOWN
        }
        if (next == state.direction) return null
        state.direction = next
        return GestureUpdate.Selection(next)
    }

    private fun moveTrackpad(state: PointerState, dx: Float, dy: Float): GestureUpdate? {
        if (state.axis == null) {
            if (hypot(dx, dy) < thresholds.trackpadStartDp) return null
            state.axis = if (abs(dx) >= abs(dy)) GestureAxis.HORIZONTAL else GestureAxis.VERTICAL
        }
        val distance = if (state.axis == GestureAxis.HORIZONTAL) dx else dy
        val unit = if (state.axis == GestureAxis.HORIZONTAL) thresholds.horizontalUnitDp else thresholds.verticalUnitDp
        val totalUnits = (floor(abs(distance) / unit) * sign(distance)).toInt()
        val delta = totalUnits - state.emittedUnits
        if (delta == 0) return null
        state.emittedUnits = totalUnits
        val direction = when (state.axis) {
            GestureAxis.HORIZONTAL -> if (delta < 0) Direction.LEFT else Direction.RIGHT
            GestureAxis.VERTICAL -> if (delta < 0) Direction.UP else Direction.DOWN
            null -> return null
        }
        return GestureUpdate.CursorDelta(direction, abs(delta))
    }

    fun finish(pointerId: Int): Direction? = pointers.remove(pointerId)?.direction
    fun cancel(pointerId: Int) { pointers.remove(pointerId) }
    fun cancelAll() { pointers.clear() }
    fun activePointerCount(): Int = pointers.size
}
