package com.masuidrive.gestureime.keyboard

enum class QwertyLabelGroup {
    LETTER_PRIMARY,
    LETTER_SECONDARY,
    SPACE_ENTER_PRIMARY,
    SPACE_ENTER_SECONDARY,
    COMPOSITE_SMALL,
}

data class LabelAdjustment(
    val scale: Float = 1f,
    val xOffsetDp: Float = 0f,
    val yOffsetDp: Float = 0f,
) {
    fun sanitized(): LabelAdjustment = LabelAdjustment(
        scale = scale.finiteOr(1f).coerceIn(MIN_SCALE, MAX_SCALE),
        xOffsetDp = xOffsetDp.finiteOr(0f).coerceIn(-MAX_X_OFFSET_DP, MAX_X_OFFSET_DP),
        yOffsetDp = yOffsetDp.finiteOr(0f).coerceIn(-MAX_Y_OFFSET_DP, MAX_Y_OFFSET_DP),
    )

    companion object {
        const val MIN_SCALE = .7f
        const val MAX_SCALE = 1.3f
        const val MAX_X_OFFSET_DP = 6f
        const val MAX_Y_OFFSET_DP = 8f
    }
}

@ConsistentCopyVisibility
data class QwertyLabelStyle private constructor(
    private val values: Map<QwertyLabelGroup, LabelAdjustment>,
) {
    operator fun get(group: QwertyLabelGroup): LabelAdjustment =
        (values[group] ?: LabelAdjustment()).sanitized()

    fun with(group: QwertyLabelGroup, adjustment: LabelAdjustment): QwertyLabelStyle =
        QwertyLabelStyle(values + (group to adjustment.sanitized()))

    fun sanitized(): QwertyLabelStyle = QwertyLabelStyle(
        QwertyLabelGroup.entries.associateWith(::get),
    )

    companion object {
        val DEFAULT = QwertyLabelStyle(emptyMap())
    }
}

private fun Float.finiteOr(default: Float) = if (isFinite()) this else default
