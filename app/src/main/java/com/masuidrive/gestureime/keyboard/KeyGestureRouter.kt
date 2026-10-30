package com.masuidrive.gestureime.keyboard

/** Resolves a key's touch sequence independently of the View used to draw it. */
class KeyGestureRouter {
    sealed interface Step {
        data class Selection(val direction: Direction, val assigned: Boolean, val changed: Boolean) : Step
        data class Cursor(val direction: Direction, val units: Int) : Step
    }

    data class End(val direction: Direction, val cursorMoved: Boolean, val releaseCursor: Step.Cursor?)

    private data class Press(
        val spec: KeySpec,
        val shortVerticalCursor: Boolean,
        var direction: Direction = Direction.CENTER,
        var cursorMoved: Boolean = false,
    )

    private val interpreter = GestureInterpreter()
    private val presses = mutableMapOf<Int, Press>()

    fun begin(pointerId: Int, spec: KeySpec, xDp: Float, yDp: Float,
              thresholds: GestureThresholds, shortVerticalCursor: Boolean = false) {
        presses[pointerId] = Press(spec, shortVerticalCursor)
        val verticalOnly = spec.kind == KeyKind.MODIFIER ||
            (spec.kind == KeyKind.BACKSPACE && spec.center == null) ||
            (spec.kind == KeyKind.CHARACTER && spec.up != null && spec.down != null &&
                spec.left == null && spec.right == null)
        interpreter.start(pointerId, xDp, yDp, trackpad = spec.kind == KeyKind.SPACE,
            verticalOnly = verticalOnly, pointerThresholds = thresholds)
    }

    fun move(pointerId: Int, xDp: Float, yDp: Float): Step? {
        val press = presses[pointerId] ?: return null
        return when (val update = interpreter.move(pointerId, xDp, yDp)) {
            is GestureUpdate.Selection -> {
                val assigned = update.direction == Direction.CENTER || press.spec.value(update.direction) != null
                val direction = update.direction.takeIf { assigned } ?: Direction.CENTER
                val changed = press.direction != direction
                press.direction = direction
                Step.Selection(direction, assigned, changed)
            }
            is GestureUpdate.CursorDelta -> {
                press.cursorMoved = true
                Step.Cursor(update.direction, update.units)
            }
            null -> null
        }
    }

    fun end(pointerId: Int, xDp: Float, yDp: Float): End? {
        val press = presses.remove(pointerId) ?: return null
        val releaseCursor = if (press.shortVerticalCursor && !press.cursorMoved) {
            interpreter.finishTrackpad(pointerId, xDp, yDp)?.let { Step.Cursor(it.direction, it.units) }
        } else {
            interpreter.finish(pointerId)
            null
        }
        return End(press.direction, press.cursorMoved || releaseCursor != null, releaseCursor)
    }

    fun cancel(pointerId: Int) {
        presses.remove(pointerId)
        interpreter.cancel(pointerId)
    }

    fun cancelAll() {
        presses.clear()
        interpreter.cancelAll()
    }
}
