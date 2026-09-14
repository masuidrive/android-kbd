package com.masuidrive.gestureime.ui

import android.content.Context
import android.content.res.Configuration
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.masuidrive.gestureime.R

private const val MAX_HIGHLIGHT_CANDIDATES = 8
private const val MAX_DIFF_MATRIX_CELLS = 262_144

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
        val differenceMasks = candidateDifferenceMasks(snapshot.candidates)
        snapshot.candidates.forEachIndexed { index, candidate ->
            rows.addView(textRow(highlightDifferences(candidate, differenceMasks[index])).apply {
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

    private fun highlightDifferences(value: String, differences: BooleanArray): CharSequence {
        if (differences.none { it }) return value
        val styled = SpannableString(value)
        val codePoints = value.codePoints().toArray()
        var utf16Offset = 0
        var rangeStart = -1
        codePoints.forEachIndexed { index, codePoint ->
            if (differences[index] && rangeStart < 0) rangeStart = utf16Offset
            if (!differences[index] && rangeStart >= 0) {
                applyDifferenceStyle(styled, rangeStart, utf16Offset)
                rangeStart = -1
            }
            utf16Offset += Character.charCount(codePoint)
        }
        if (rangeStart >= 0) applyDifferenceStyle(styled, rangeStart, utf16Offset)
        return styled
    }

    private fun applyDifferenceStyle(text: SpannableString, start: Int, end: Int) {
        text.setSpan(
            ForegroundColorSpan(context.getColor(R.color.candidate_difference_text)),
            start,
            end,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
        text.setSpan(StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    private fun textRow(value: CharSequence) = TextView(context).apply {
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

/** Marks code points that are not part of every candidate's pairwise longest common subsequence. */
private fun candidateDifferenceMasks(candidates: List<String>): List<BooleanArray> {
    val codePoints = candidates.map { it.codePoints().toArray() }
    val noDifferences = { codePoints.map { BooleanArray(it.size) } }
    if (codePoints.size !in 2..MAX_HIGHLIGHT_CANDIDATES) return noDifferences()
    val common = codePoints.map { BooleanArray(it.size) { true } }
    for (leftIndex in 0 until codePoints.lastIndex) {
        for (rightIndex in leftIndex + 1 until codePoints.size) {
            val (leftMatches, rightMatches) = alignedMatches(codePoints[leftIndex], codePoints[rightIndex])
                ?: return noDifferences()
            common[leftIndex].indices.forEach { common[leftIndex][it] = common[leftIndex][it] && leftMatches[it] }
            common[rightIndex].indices.forEach { common[rightIndex][it] = common[rightIndex][it] && rightMatches[it] }
        }
    }
    return common.map { flags -> BooleanArray(flags.size) { !flags[it] } }
}

private fun alignedMatches(left: IntArray, right: IntArray): Pair<BooleanArray, BooleanArray>? {
    val leftMatches = BooleanArray(left.size)
    val rightMatches = BooleanArray(right.size)
    val sharedLimit = minOf(left.size, right.size)
    var prefix = 0
    while (prefix < sharedLimit && left[prefix] == right[prefix]) {
        leftMatches[prefix] = true
        rightMatches[prefix] = true
        prefix++
    }
    var suffix = 0
    while (
        suffix < sharedLimit - prefix &&
        left[left.lastIndex - suffix] == right[right.lastIndex - suffix]
    ) {
        leftMatches[left.lastIndex - suffix] = true
        rightMatches[right.lastIndex - suffix] = true
        suffix++
    }
    val leftLength = left.size - prefix - suffix
    val rightLength = right.size - prefix - suffix
    val rowWidth = rightLength + 1
    val matrixCells = (leftLength + 1L) * rowWidth
    if (matrixCells > MAX_DIFF_MATRIX_CELLS) return null
    val lengths = IntArray(matrixCells.toInt())
    fun lengthAt(leftOffset: Int, rightOffset: Int) = lengths[leftOffset * rowWidth + rightOffset]
    for (leftOffset in leftLength - 1 downTo 0) {
        for (rightOffset in rightLength - 1 downTo 0) {
            lengths[leftOffset * rowWidth + rightOffset] = if (left[prefix + leftOffset] == right[prefix + rightOffset]) {
                lengthAt(leftOffset + 1, rightOffset + 1) + 1
            } else {
                maxOf(lengthAt(leftOffset + 1, rightOffset), lengthAt(leftOffset, rightOffset + 1))
            }
        }
    }
    var leftOffset = 0
    var rightOffset = 0
    while (leftOffset < leftLength && rightOffset < rightLength) {
        when {
            left[prefix + leftOffset] == right[prefix + rightOffset] -> {
                leftMatches[prefix + leftOffset++] = true
                rightMatches[prefix + rightOffset++] = true
            }
            lengthAt(leftOffset + 1, rightOffset) >= lengthAt(leftOffset, rightOffset + 1) -> leftOffset++
            else -> rightOffset++
        }
    }
    return leftMatches to rightMatches
}
