package com.masuidrive.gestureime.ui

import android.content.Context
import android.content.res.Configuration
import android.graphics.Rect
import android.graphics.Typeface
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
    private val candidateRow = LinearLayout(context).apply { orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
    private val candidateScroll = HorizontalScrollView(context).apply {
        isHorizontalScrollBarEnabled = false
        addView(candidateRow, ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }
    private val voiceControls = LinearLayout(context).apply { orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
    private var candidateSnapshot = CandidateUiSnapshot(0L, emptyList())
    private var voiceSnapshot = VoiceUiSnapshot(0L, VoiceUiState.Hidden)
    private var onCandidateSelected: ((CandidateUiEvent) -> Unit)? = null
    private var onVoiceAction: ((VoiceUiEvent) -> Unit)? = null

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(3), dp(8), dp(3), dp(8))
        applyThemeColors()
        addView(candidateScroll, LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        addView(voiceControls, LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT))
        render()
    }

    fun setOnCandidateSelected(listener: (CandidateUiEvent) -> Unit) { onCandidateSelected = listener }
    fun setOnVoiceActionListener(listener: (VoiceUiEvent) -> Unit) { onVoiceAction = listener }

    fun showCandidates(snapshot: CandidateUiSnapshot) {
        candidateSnapshot = snapshot
        render()
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
            VoiceUiState.Recording -> { renderCandidateMessage("音声を聞いています"); addVoiceButton("取消", "音声入力を取り消す", VoiceUiAction.Cancel) }
            VoiceUiState.Recognizing -> { renderCandidateMessage("音声を認識しています"); addVoiceButton("取消", "音声入力を取り消す", VoiceUiAction.Cancel) }
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
        var selectedView: TextView? = null
        snapshot.candidates.forEachIndexed { index, candidate ->
            candidateRow.addView(label(candidate, index == snapshot.selectedIndex).apply {
                isClickable = true; isFocusable = true
                contentDescription = "候補 ${index + 1}: $candidate"
                setOnClickListener { onCandidateSelected?.invoke(CandidateUiEvent(snapshot.token, index)) }
                if (index == snapshot.selectedIndex) selectedView = this
            }, candidateLayout(hasLeadingGap = index > 0))
        }
        selectedView?.let { view -> post { view.requestRectangleOnScreen(Rect(0, 0, view.width, view.height), true) } }
    }

    private fun renderCandidateMessage(message: String, description: String = message) {
        candidateRow.removeAllViews()
        candidateRow.addView(label(message, false).apply { contentDescription = description; isFocusable = true }, candidateLayout())
        candidateScroll.post { candidateScroll.scrollTo(0, 0) }
    }

    private fun addVoiceButton(text: String, description: String, action: VoiceUiAction?, sessionToken: Long = voiceSnapshot.sessionToken) {
        voiceControls.addView(label(text, action != null).apply {
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
