package com.masuidrive.gestureime.ui

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import com.masuidrive.gestureime.R

enum class SavedValuePicker {
    SNIPPETS,
    PERSONAL_INFO,
}

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
    private val snippetAction = pickerAction(SavedValuePicker.SNIPPETS)
    private val personalInfoAction = pickerAction(SavedValuePicker.PERSONAL_INFO)
    private var candidateSnapshot = CandidateUiSnapshot(0L, emptyList())
    private var inlineSuggestionViews = emptyList<View>()
    private var candidatesAlongsideInline = false
    private var pickerActionsVisible = false
    private var openPicker: SavedValuePicker? = null
    private var onCandidateSelected: ((CandidateUiEvent) -> Unit)? = null
    private var onCandidateLongPressed: ((CandidateUiLongPressEvent) -> Boolean)? = null
    private var onPickerRequested: ((SavedValuePicker) -> Unit)? = null
    private var onPickerDismissed: (() -> Unit)? = null

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        applyFaceInsets(width)
        applyThemeColors()
        addView(snippetAction, pickerActionLayout())
        addView(personalInfoAction, pickerActionLayout())
        addView(candidateScroll, LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        render()
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        if (width != oldWidth) applyFaceInsets(width)
    }

    fun setOnCandidateSelected(listener: (CandidateUiEvent) -> Unit) { onCandidateSelected = listener }
    fun setOnCandidateLongPressed(listener: (CandidateUiLongPressEvent) -> Boolean) { onCandidateLongPressed = listener }
    fun setOnPickerRequested(listener: (SavedValuePicker) -> Unit) { onPickerRequested = listener }
    fun setOnPickerDismissed(listener: () -> Unit) { onPickerDismissed = listener }

    fun showCandidates(snapshot: CandidateUiSnapshot, alongsideInline: Boolean = false) {
        val contentChanged = candidateSnapshot.labels != snapshot.labels
        candidateSnapshot = snapshot
        candidatesAlongsideInline = alongsideInline
        render()
        if (contentChanged) candidateScroll.scrollTo(0, 0)
    }

    /**
     * Hosts provider-owned inline Autofill content without reading or transforming it. The
     * framework creates these views as protected InlineContentViews; keeping this method view
     * based lets the candidate strip remain unaware of Autofill provider data.
     */
    fun showInlineSuggestionViews(views: List<View>) {
        inlineSuggestionViews = views
        render()
        candidateScroll.scrollTo(0, 0)
    }

    fun clearInlineSuggestionViews() {
        inlineSuggestionViews = emptyList()
        render()
        candidateScroll.scrollTo(0, 0)
    }

    fun showSavedValuePickerActions(visible: Boolean, activePicker: SavedValuePicker? = null) {
        if (pickerActionsVisible == visible && openPicker == activePicker) return
        pickerActionsVisible = visible
        openPicker = activePicker
        updatePickerAction(snippetAction, SavedValuePicker.SNIPPETS)
        updatePickerAction(personalInfoAction, SavedValuePicker.PERSONAL_INFO)
    }

    fun showStatus(message: String) {
        candidateSnapshot = candidateSnapshot.copy(candidates = emptyList(), labels = emptyList(), selectedIndex = -1)
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
        applyPickerActionTheme()
    }

    private fun render() {
        candidateRow.removeAllViews()
        if (inlineSuggestionViews.isEmpty()) renderCandidates()
        else if (candidatesAlongsideInline) {
            // Keep the saved value beside the card even when a provider returns many accounts.
            renderCandidates()
            renderInlineSuggestions(afterCandidate = candidateSnapshot.labels.isNotEmpty())
        } else renderInlineSuggestions()
    }

    private fun renderInlineSuggestions(afterCandidate: Boolean = false) {
        inlineSuggestionViews.forEachIndexed { index, view ->
            candidateRow.addView(view, inlineSuggestionLayout(view, hasLeadingGap = index > 0 || afterCandidate))
        }
    }

    private fun renderCandidates() {
        val snapshot = candidateSnapshot
        snapshot.labels.forEachIndexed { index, candidateLabel ->
            candidateRow.addView(label(candidateLabel, index == snapshot.selectedIndex).apply {
                if (candidatesAlongsideInline && inlineSuggestionViews.isNotEmpty()) {
                    maxWidth = dp(160)
                    ellipsize = android.text.TextUtils.TruncateAt.END
                }
                isEnabled = snapshot.selectable
                isClickable = snapshot.selectable
                isFocusable = snapshot.selectable
                contentDescription = if (snapshot.selectable) "候補 ${index + 1}: $candidateLabel" else "認識途中: $candidateLabel"
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

    private fun pickerActionLayout() = LayoutParams(
        dp(38),
        ViewGroup.LayoutParams.MATCH_PARENT,
    ).apply {
        marginEnd = dp(10)
    }

    /** InlineContentView has no useful intrinsic size; retain the framework-rendered bounds. */
    private fun inlineSuggestionLayout(view: View, hasLeadingGap: Boolean) = LayoutParams(
        view.layoutParams?.width ?: ViewGroup.LayoutParams.WRAP_CONTENT,
        view.layoutParams?.height ?: ViewGroup.LayoutParams.MATCH_PARENT,
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

    private fun pickerAction(category: SavedValuePicker) = ImageButton(context).apply {
        setPadding(dp(7), dp(7), dp(7), dp(7))
        visibility = GONE
        setOnClickListener {
            if (openPicker == category) onPickerDismissed?.invoke()
            else onPickerRequested?.invoke(category)
        }
    }

    private fun updatePickerAction(action: ImageButton, category: SavedValuePicker) {
        val active = openPicker == category
        action.setImageResource(if (active) R.drawable.ic_arrow_back else when (category) {
            SavedValuePicker.SNIPPETS -> R.drawable.ic_text_snippet
            SavedValuePicker.PERSONAL_INFO -> R.drawable.ic_personal_info
        })
        action.contentDescription = context.getString(when (category) {
            SavedValuePicker.SNIPPETS -> if (active) R.string.text_snippet_back_description else R.string.text_snippet_action_description
            SavedValuePicker.PERSONAL_INFO -> if (active) R.string.personal_info_back_description else R.string.personal_info_action_description
        })
        action.visibility = if (pickerActionsVisible) VISIBLE else GONE
    }

    private fun applyPickerActionTheme() {
        val tint = ColorStateList.valueOf(context.getColor(R.color.keyboard_muted_text))
        listOf(snippetAction, personalInfoAction).forEach { action ->
            action.imageTintList = tint
            action.background = null
        }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
