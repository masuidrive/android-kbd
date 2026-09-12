package com.masuidrive.gestureime.ui

import android.content.Context
import android.content.res.Configuration
import android.graphics.Typeface
import android.text.TextUtils
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
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
    private val voiceControls = LinearLayout(context).apply { orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
    private var candidateSnapshot = CandidateUiSnapshot(0L, emptyList())
    private var voiceSnapshot = VoiceUiSnapshot(0L, VoiceUiState.Hidden)
    private var onCandidateSelected: ((CandidateUiEvent) -> Unit)? = null
    private var onCandidateLongPressed: ((CandidateUiLongPressEvent) -> Boolean)? = null
    private var onVoiceAction: ((VoiceUiEvent) -> Unit)? = null

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        applyFaceInsets(width)
        applyThemeColors()
        addView(candidateScroll, LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        addView(voiceControls, LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT))
        candidateScroll.addOnLayoutChangeListener { _, left, _, right, _, oldLeft, _, oldRight, _ ->
            if (right - left != oldRight - oldLeft) constrainVoiceCandidateWidths()
        }
        render()
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        if (width != oldWidth) applyFaceInsets(width)
    }

    fun setOnCandidateSelected(listener: (CandidateUiEvent) -> Unit) { onCandidateSelected = listener }
    fun setOnCandidateLongPressed(listener: (CandidateUiLongPressEvent) -> Boolean) { onCandidateLongPressed = listener }
    fun setOnVoiceActionListener(listener: (VoiceUiEvent) -> Unit) { onVoiceAction = listener }

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

    fun setVoiceState(snapshot: VoiceUiSnapshot) { voiceSnapshot = snapshot; render() }

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
        voiceControls.setBackgroundColor(background)
    }

    private fun render() {
        candidateRow.removeAllViews()
        voiceControls.removeAllViews()
        val snapshot = voiceSnapshot
        when (val state = snapshot.state) {
            VoiceUiState.Hidden, VoiceUiState.Idle -> renderCandidates()
            VoiceUiState.Recording, VoiceUiState.Recognizing -> renderCandidates()
            is VoiceUiState.Partial -> renderCandidateMessage(state.text, "認識途中: ${state.text}")
            is VoiceUiState.Preview -> renderCandidateMessage("音声を認識しました")
            is VoiceUiState.Unavailable -> {
                renderCandidates()
                addVoiceButton("非対応", "音声入力を利用できない理由を表示。${state.message}", VoiceUiAction.ExplainUnavailable, snapshot.sessionToken)
            }
            VoiceUiState.PermissionRequired -> {
                renderCandidates()
                addVoiceButton("許可", "マイクの使用を許可", VoiceUiAction.RequestPermission, snapshot.sessionToken)
            }
        }
        voiceControls.visibility = if (voiceControls.childCount == 0) View.GONE else View.VISIBLE
    }

    private fun renderCandidates() {
        val snapshot = candidateSnapshot
        snapshot.candidates.forEachIndexed { index, candidate ->
            candidateRow.addView(label(candidate, index == snapshot.selectedIndex).apply {
                if (snapshot.presentation == CandidatePresentation.VOICE) {
                    maxLines = 2
                    ellipsize = TextUtils.TruncateAt.END
                    includeFontPadding = false
                    setTextSize(TypedValue.COMPLEX_UNIT_PX, 13f * resources.displayMetrics.density)
                    maxWidth = visibleCandidateWidth()
                }
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

    private fun constrainVoiceCandidateWidths() {
        if (candidateSnapshot.presentation != CandidatePresentation.VOICE) return
        val width = visibleCandidateWidth()
        repeat(candidateRow.childCount) { (candidateRow.getChildAt(it) as TextView).maxWidth = width }
        candidateRow.requestLayout()
    }

    private fun visibleCandidateWidth(): Int =
        candidateScroll.width.takeIf { it > 0 } ?: (resources.displayMetrics.widthPixels - paddingLeft - paddingRight)

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

    private fun addVoiceButton(text: String, description: String, action: VoiceUiAction?, sessionToken: Long = voiceSnapshot.sessionToken) {
        voiceControls.addView(label(text, false).apply {
            contentDescription = description
            isEnabled = action != null; isClickable = action != null; isFocusable = true
            if (action != null) setOnClickListener { onVoiceAction?.invoke(VoiceUiEvent(sessionToken, action)) }
        }, candidateLayout(hasLeadingGap = true))
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
