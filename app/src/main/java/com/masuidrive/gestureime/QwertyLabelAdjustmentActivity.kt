package com.masuidrive.gestureime

import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.masuidrive.gestureime.keyboard.KeyboardMode
import com.masuidrive.gestureime.keyboard.KeyboardView
import com.masuidrive.gestureime.keyboard.LabelAdjustment
import com.masuidrive.gestureime.keyboard.QwertyLabelGroup
import com.masuidrive.gestureime.keyboard.QwertyLabelStyle

class QwertyLabelAdjustmentActivity : AppCompatActivity() {
    private lateinit var preview: KeyboardView
    private lateinit var groupSpinner: Spinner
    private lateinit var scaleBar: SeekBar
    private lateinit var xBar: SeekBar
    private lateinit var yBar: SeekBar
    private lateinit var valueLabel: TextView
    private var style = QwertyLabelStyle.DEFAULT
    private var updatingControls = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        style = ImePreferences.getQwertyLabelStyle(this).sanitized()
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(12), dp(16), dp(12)) }
        root.addView(TextView(this).apply { text = "QWERTYラベル調整"; textSize = 24f }, matchWrap())

        val widthControls = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        widthControls.addView(TextView(this).apply { text = "プレビュー幅" })
        widthControls.addView(Button(this).apply { text = "狭い 412"; setOnClickListener { setPreviewWidth(false) } })
        widthControls.addView(Button(this).apply { text = "広い 840"; setOnClickListener { setPreviewWidth(true) } })
        root.addView(widthControls, matchWrap())

        preview = KeyboardView(this).apply {
            setMode(KeyboardMode.QWERTY)
            setPreviewOnly(true)
            setQwertyLabelStyle(style)
        }
        val previewScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = true
            addView(preview, ViewGroup.LayoutParams(dp(412), dp(220)))
        }
        root.addView(previewScroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(260)))

        val controls = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        controls.addView(TextView(this).apply { text = "変更は自動保存され、実際のキーボードへ反映されます。" }, matchWrap())
        groupSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(this@QwertyLabelAdjustmentActivity, android.R.layout.simple_spinner_dropdown_item, GROUP_LABELS)
            onItemSelectedListener = SimpleItemSelectedListener { loadGroup() }
        }
        controls.addView(groupSpinner, matchWrap())
        valueLabel = TextView(this)
        controls.addView(valueLabel, matchWrap())
        scaleBar = controls.addAdjustment("文字サイズ", 60)
        xBar = controls.addAdjustment("左右位置", 12)
        yBar = controls.addAdjustment("上下位置", 16)
        controls.addView(Button(this).apply {
            text = "初期値に戻して保存"
            setOnClickListener {
                style = QwertyLabelStyle.DEFAULT
                ImePreferences.resetQwertyLabelStyle(this@QwertyLabelAdjustmentActivity)
                preview.setQwertyLabelStyle(style)
                loadGroup()
            }
        }, matchWrap())
        root.addView(ScrollView(this).apply { addView(controls) }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)
        loadGroup()
    }

    private fun setPreviewWidth(wide: Boolean) {
        val params = preview.layoutParams
        params.width = dp(if (wide) 840 else 412)
        params.height = dp(if (wide) 248 else 220)
        preview.layoutParams = params
        preview.requestLayout()
    }

    private fun LinearLayout.addAdjustment(label: String, max: Int): SeekBar {
        addView(TextView(this@QwertyLabelAdjustmentActivity).apply { text = label }, matchWrap())
        return SeekBar(this@QwertyLabelAdjustmentActivity).also { bar ->
            bar.max = max
            bar.setOnSeekBarChangeListener(SimpleSeekBarListener { if (!updatingControls) updateCurrent() })
            addView(bar, matchWrap())
        }
    }

    private fun loadGroup() {
        if (!::groupSpinner.isInitialized) return
        val adjustment = style[currentGroup()]
        updatingControls = true
        scaleBar.progress = (adjustment.scale * 100).toInt() - 70
        xBar.progress = adjustment.xOffsetDp.toInt() + 6
        yBar.progress = adjustment.yOffsetDp.toInt() + 8
        updatingControls = false
        showValues(adjustment)
    }

    private fun updateCurrent() {
        val adjustment = LabelAdjustment(
            scale = (scaleBar.progress + 70) / 100f,
            xOffsetDp = xBar.progress - 6f,
            yOffsetDp = yBar.progress - 8f,
        ).sanitized()
        style = style.with(currentGroup(), adjustment)
        ImePreferences.setQwertyLabelStyle(this, style)
        preview.setQwertyLabelStyle(style)
        showValues(adjustment)
    }

    private fun showValues(value: LabelAdjustment) {
        valueLabel.text = "サイズ %.2f倍　左右 %+.0f　上下 %+.0f".format(value.scale, value.xOffsetDp, value.yOffsetDp)
    }

    private fun currentGroup() = QwertyLabelGroup.entries[groupSpinner.selectedItemPosition.coerceAtLeast(0)]
    private fun matchWrap() = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private companion object {
        val GROUP_LABELS = listOf("英字の主文字", "上部の補助文字", "Space・Enter", "矢印・paste", "C/A・BS・あん")
    }
}

private class SimpleItemSelectedListener(private val selected: () -> Unit) : android.widget.AdapterView.OnItemSelectedListener {
    override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) = selected()
    override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
}

private class SimpleSeekBarListener(private val changed: (Int) -> Unit) : SeekBar.OnSeekBarChangeListener {
    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) = changed(progress)
    override fun onStartTrackingTouch(seekBar: SeekBar) = Unit
    override fun onStopTrackingTouch(seekBar: SeekBar) = Unit
}
