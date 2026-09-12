package com.masuidrive.gestureime.ui

import android.content.Context
import android.content.res.Configuration
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import com.masuidrive.gestureime.R

class CandidateStripView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    LinearLayout(context, attrs) {
    companion object {
        private const val WIDE_LAYOUT_MIN_WIDTH_DP = 600f
        private const val PHONE_FACE_INSET_DP = 6
        private const val WIDE_FACE_INSET_DP = 13
        private const val FACE_TOP_INSET_DP = 10
        private const val FACE_BOTTOM_INSET_DP = 2
    }

    private val candidateRow = LinearLayout(context).apply { orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
    private val candidateScroll = HorizontalScrollView(context).apply {
        isHorizontalScrollBarEnabled = false
        addView(candidateRow, ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }
    private var candidateSnapshot = CandidateUiSnapshot(0L, emptyList())
    private var onCandidateSelected: ((CandidateUiEvent) -> Unit)? = null
    private var onCandidateLongPressed: ((CandidateUiLongPressEvent) -> Boolean)? = null

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        applyFaceInsets(width)
        applyThemeColors()
        addView(candidateScroll, LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        render()
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        if (width != oldWidth) applyFaceInsets(width)
    }

    fun setOnCandidateSelected(listener: (CandidateUiEvent) -> Unit) { onCandidateSelected = listener }
    fun setOnCandidateLongPressed(listener: (CandidateUiLongPressEvent) -> Boolean) { onCandidateLongPressed = listener }

    fun showCandidates(snapshot: CandidateUiSnapshot) {
        val contentChanged = candidateSnapshot.candidates != snapshot.candidates
        candidateSnapshot = snapshot
        render()
        if (contentChanged) candidateScroll.scrollTo(0, 0)
    }

    fun showStatus(message: String) {
        candidateSnapshot = candidateSnapshot.copy(candidates = emptyList(), selectedIndex = -1)
        renderCandidateMessage(message)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applyThemeColors()
        render()
    }

    private fun applyThemeColors() {
        val background = context.getColor(R.color.keyboard_background)
        setBackgroundColor(background)
        candidateRow.setBackgroundColor(background)
        candidateScroll.setBackgroundColor(background)
    }

    private fun render() {
        candidateRow.removeAllViews()
        renderCandidates()
    }

    private fun renderCandidates() {
        val snapshot = candidateSnapshot
        snapshot.candidates.forEachIndexed { index, candidate ->
            candidateRow.addView(label(candidate, index == snapshot.selectedIndex).apply {
                isEnabled = snapshot.selectable
                isClickable = snapshot.selectable
                isFocusable = snapshot.selectable
                contentDescription = if (snapshot.selectable) "候補 ${index + 1}: $candidate" else "認識途中: $candidate"
                if (snapshot.selectable) {
                    setOnClickListener { onCandidateSelected?.invoke(CandidateUiEvent(snapshot.token, index)) }
                    setOnLongClickListener {
                        onCandidateLongPressed?.invoke(CandidateUiLongPressEvent(snapshot.token, index)) ?: false
                    }
                }
            }, candidateLayout(hasLeadingGap = index > 0))
        }
    }

    private fun applyFaceInsets(width: Int) {
        val horizontal = dp(if (width / resources.displayMetrics.density >= WIDE_LAYOUT_MIN_WIDTH_DP) {
            WIDE_FACE_INSET_DP
        } else {
            PHONE_FACE_INSET_DP
        })
        setPadding(horizontal, dp(FACE_TOP_INSET_DP), horizontal, dp(FACE_BOTTOM_INSET_DP))
    }

    private fun renderCandidateMessage(message: String, description: String = message) {
        candidateRow.removeAllViews()
        candidateRow.addView(label(message, false).apply { contentDescription = description; isFocusable = true }, candidateLayout())
        candidateScroll.post { candidateScroll.scrollTo(0, 0) }
    }

    private fun label(textValue: String, selected: Boolean) = TextView(context).apply {
        text = textValue
        setTextSize(TypedValue.COMPLEX_UNIT_PX, 15f * resources.displayMetrics.density)
        gravity = Gravity.CENTER
        minWidth = dp(82)
        setPadding(dp(14), 0, dp(14), 0)
        setTextColor(context.getColor(if (selected) R.color.candidate_selected_text else R.color.keyboard_text))
        background = candidateFace(selected)
        setTypeface(typeface, Typeface.NORMAL)
        maxLines = 1
    }

    private fun candidateLayout(hasLeadingGap: Boolean = false) = LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.MATCH_PARENT,
    ).apply {
        if (hasLeadingGap) marginStart = dp(5)
    }

    private fun candidateFace(selected: Boolean): LayerDrawable {
        fun layer(color: Int) = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(7).toFloat()
            setColor(color)
        }
        return LayerDrawable(arrayOf(
            layer(context.getColor(R.color.keyboard_shadow)),
            layer(context.getColor(if (selected) R.color.candidate_selected else R.color.candidate_background)),
        )).apply { setLayerInset(1, 0, 0, 0, dp(1)) }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
