package com.masuidrive.gestureime.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView

class CandidateStripView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    LinearLayout(context, attrs) {
    private val candidateRow = LinearLayout(context).apply { orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
    private val candidateScroll = HorizontalScrollView(context).apply {
        isHorizontalScrollBarEnabled = false
        addView(candidateRow, ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }
    private val voiceControls = LinearLayout(context).apply { orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
    private var candidates = emptyList<String>()
    private var selectedCandidateIndex = -1
    private var voiceState: VoiceUiState = VoiceUiState.Hidden
    private var onCandidateSelected: ((Int) -> Unit)? = null
    private var onVoiceAction: ((VoiceUiAction) -> Unit)? = null

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(0, 0, 0, dp(8))
        setBackgroundColor(BACKGROUND)
        candidateRow.setBackgroundColor(BACKGROUND)
        addView(candidateScroll, LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        addView(voiceControls, LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT))
        render()
    }

    fun setOnCandidateSelected(listener: (Int) -> Unit) { onCandidateSelected = listener }
    fun setOnVoiceActionListener(listener: (VoiceUiAction) -> Unit) { onVoiceAction = listener }

    fun showCandidates(candidates: List<String>, selectedIndex: Int) {
        this.candidates = candidates
        selectedCandidateIndex = selectedIndex
        render()
    }

    fun showStatus(message: String) {
        candidates = emptyList()
        selectedCandidateIndex = -1
        renderCandidateMessage(message)
    }

    fun setVoiceState(state: VoiceUiState) { voiceState = state; render() }

    private fun render() {
        candidateRow.removeAllViews()
        voiceControls.removeAllViews()
        voiceControls.visibility = if (voiceState == VoiceUiState.Hidden) View.GONE else View.VISIBLE
        when (val state = voiceState) {
            VoiceUiState.Hidden, VoiceUiState.Idle -> renderCandidates()
            VoiceUiState.Recording -> { renderCandidateMessage("音声を聞いています"); addVoiceButton("停止", "音声入力を停止", VoiceUiAction.Stop) }
            VoiceUiState.Recognizing -> { renderCandidateMessage("音声を認識しています"); addVoiceButton("処理中", "音声を認識しています", null); addVoiceButton("取消", "音声入力を取り消す", VoiceUiAction.Cancel) }
            is VoiceUiState.Preview -> {
                renderCandidateMessage(state.text, "認識結果: ${state.text}")
                addVoiceButton("確定", "認識結果を確定", VoiceUiAction.Confirm)
                addVoiceButton("取消", "認識結果を取り消す", VoiceUiAction.Cancel)
            }
            is VoiceUiState.Unavailable -> {
                renderCandidates()
                addVoiceButton("非対応", "音声入力を利用できない理由を表示。${state.message}", VoiceUiAction.ExplainUnavailable)
            }
            VoiceUiState.PermissionRequired -> {
                renderCandidates()
                addVoiceButton("許可", "マイクの使用を許可", VoiceUiAction.RequestPermission)
            }
        }
        if (voiceState == VoiceUiState.Idle) addVoiceButton("音声", "音声入力を開始", VoiceUiAction.Start)
        if (voiceState == VoiceUiState.Recording) addVoiceButton("取消", "音声入力を取り消す", VoiceUiAction.Cancel)
    }

    private fun renderCandidates() {
        var selectedView: TextView? = null
        candidates.forEachIndexed { index, candidate ->
            candidateRow.addView(label(candidate, index == selectedCandidateIndex).apply {
                isClickable = true; isFocusable = true
                contentDescription = "候補 ${index + 1}: $candidate"
                setOnClickListener { onCandidateSelected?.invoke(index) }
                if (index == selectedCandidateIndex) selectedView = this
            })
        }
        selectedView?.let { view -> post { view.requestRectangleOnScreen(Rect(0, 0, view.width, view.height), true) } }
    }

    private fun renderCandidateMessage(message: String, description: String = message) {
        candidateRow.removeAllViews()
        candidateRow.addView(label(message, false).apply { contentDescription = description; isFocusable = true })
        candidateScroll.post { candidateScroll.scrollTo(0, 0) }
    }

    private fun addVoiceButton(text: String, description: String, action: VoiceUiAction?) {
        voiceControls.addView(label(text, action != null).apply {
            contentDescription = description
            isEnabled = action != null; isClickable = action != null; isFocusable = true
            setPadding(dp(12), dp(6), dp(12), dp(6))
            if (action != null) setOnClickListener { onVoiceAction?.invoke(action) }
        })
    }

    private fun label(textValue: String, selected: Boolean) = TextView(context).apply {
        text = textValue
        setTextSize(TypedValue.COMPLEX_UNIT_PX, 15f * resources.displayMetrics.density)
        gravity = Gravity.CENTER
        setPadding(dp(18), dp(6), dp(18), dp(6))
        setTextColor(if (selected) SELECTED_INK else Color.WHITE)
        setBackgroundColor(if (selected) SELECTED else BACKGROUND)
        setTypeface(typeface, if (selected) Typeface.BOLD else Typeface.NORMAL)
        maxLines = 1
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private companion object {
        val BACKGROUND = Color.rgb(42, 49, 58)
        val SELECTED = Color.rgb(97, 210, 255)
        val SELECTED_INK = Color.rgb(0, 25, 35)
    }
}
