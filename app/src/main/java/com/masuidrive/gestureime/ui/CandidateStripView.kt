package com.masuidrive.gestureime.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.Rect
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView

class CandidateStripView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : HorizontalScrollView(context, attrs) {
    private val row = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }
    private var onCandidateSelected: ((Int) -> Unit)? = null

    init {
        isHorizontalScrollBarEnabled = false
        setBackgroundColor(Color.rgb(42, 49, 58))
        row.setBackgroundColor(Color.rgb(42, 49, 58))
        addView(row, ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT))
        showMessage("かなを入力すると候補を表示します")
    }

    fun setOnCandidateSelected(listener: (Int) -> Unit) {
        onCandidateSelected = listener
    }

    fun showCandidates(candidates: List<String>, selectedIndex: Int) {
        row.removeAllViews()
        if (candidates.isEmpty()) {
            showMessage("かなを入力すると候補を表示します")
            return
        }
        var selectedView: TextView? = null
        candidates.forEachIndexed { index, candidate ->
            row.addView(label(candidate, index == selectedIndex).apply {
                isClickable = true
                isFocusable = true
                contentDescription = "候補 ${index + 1}: $candidate"
                setOnClickListener { onCandidateSelected?.invoke(index) }
                if (index == selectedIndex) selectedView = this
            })
        }
        selectedView?.let { view ->
            post {
                view.requestRectangleOnScreen(Rect(0, 0, view.width, view.height), true)
            }
        }
    }

    fun showStatus(message: String) {
        showMessage(message)
    }

    private fun showMessage(message: String) {
        row.removeAllViews()
        row.addView(label(message, false))
    }

    private fun label(textValue: String, selected: Boolean) = TextView(context).apply {
        text = textValue
        textSize = 18f
        gravity = Gravity.CENTER
        val h = (18 * resources.displayMetrics.density).toInt()
        val v = (10 * resources.displayMetrics.density).toInt()
        setPadding(h, v, h, v)
        setTextColor(if (selected) Color.rgb(0, 25, 35) else Color.WHITE)
        setBackgroundColor(if (selected) Color.rgb(97, 210, 255) else Color.rgb(42, 49, 58))
        setTypeface(typeface, if (selected) Typeface.BOLD else Typeface.NORMAL)
    }
}
