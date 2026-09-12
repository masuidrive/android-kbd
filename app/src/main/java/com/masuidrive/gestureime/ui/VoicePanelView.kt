package com.masuidrive.gestureime.ui

import android.content.Context
import android.content.res.Configuration
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.masuidrive.gestureime.R

/** Fixed voice-layer surface for partial text, errors, and one selectable candidate per row. */
class VoicePanelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {
    private val rows = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
    private val scroll = ScrollView(context).apply {
        isFillViewport = true
        isVerticalScrollBarEnabled = true
        addView(rows, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
    }
    private var candidates = CandidateUiSnapshot(0L, emptyList())
    private var voice = VoiceUiSnapshot(0L, VoiceUiState.Hidden)
    private var onCandidateSelected: ((CandidateUiEvent) -> Unit)? = null

    init {
        addView(scroll, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        applyThemeColors()
        render()
    }

    fun setOnCandidateSelected(listener: (CandidateUiEvent) -> Unit) {
        onCandidateSelected = listener
    }

    fun showCandidates(snapshot: CandidateUiSnapshot) {
        val contentChanged = candidates.candidates != snapshot.candidates
        candidates = snapshot
        render()
        if (contentChanged) scroll.scrollTo(0, 0)
    }

    fun setVoiceState(snapshot: VoiceUiSnapshot) {
        voice = snapshot
        render()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applyThemeColors()
        render()
    }

    private fun applyThemeColors() {
        setBackgroundColor(context.getColor(R.color.keyboard_background))
        rows.setBackgroundColor(context.getColor(R.color.keyboard_background))
        scroll.setBackgroundColor(context.getColor(R.color.keyboard_background))
    }

    private fun render() {
        rows.removeAllViews()
        when (val state = voice.state) {
            VoiceUiState.Hidden,
            VoiceUiState.Idle,
            VoiceUiState.Recording,
            VoiceUiState.Recognizing,
            -> Unit
            is VoiceUiState.Partial -> addPlainText(state.text, "認識途中: ${state.text}")
            is VoiceUiState.Preview -> renderCandidates()
            is VoiceUiState.Unavailable -> addPlainText(state.message, state.message)
            VoiceUiState.PermissionRequired -> addPlainText("マイクの許可が必要です", "マイクの許可が必要です")
        }
    }

    private fun renderCandidates() {
        val snapshot = candidates
        snapshot.candidates.forEachIndexed { index, candidate ->
            rows.addView(textRow(candidate).apply {
                isEnabled = snapshot.selectable
                isClickable = snapshot.selectable
                isFocusable = snapshot.selectable
                contentDescription = if (snapshot.selectable) {
                    "候補 ${index + 1}: $candidate"
                } else {
                    "認識途中: $candidate"
                }
                background = GradientDrawable().apply {
                    setColor(context.getColor(R.color.candidate_background))
                    cornerRadius = dp(7).toFloat()
                }
                if (snapshot.selectable) {
                    setOnClickListener {
                        onCandidateSelected?.invoke(CandidateUiEvent(snapshot.token, index))
                    }
                }
            }, rowLayout(withGap = index > 0))
        }
    }

    private fun addPlainText(text: String, description: String) {
        rows.addView(textRow(text).apply {
            contentDescription = description
            isEnabled = true
            isClickable = false
            isFocusable = false
            background = null
        }, rowLayout())
    }

    private fun textRow(value: String) = TextView(context).apply {
        text = value
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        setTextColor(context.getColor(R.color.keyboard_text))
        setTypeface(typeface, Typeface.NORMAL)
        gravity = Gravity.CENTER_VERTICAL
        minHeight = dp(48)
        setPadding(dp(16), dp(10), dp(16), dp(10))
    }

    private fun rowLayout(withGap: Boolean = false) = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT,
    ).apply {
        marginStart = dp(6)
        marginEnd = dp(6)
        if (withGap) topMargin = dp(5)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
