package com.masuidrive.gestureime

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SetupActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val padding = (24 * resources.displayMetrics.density).toInt()
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(padding, padding, padding, padding)
            addView(TextView(context).apply {
                text = getString(R.string.setup_title)
                textSize = 28f
            }, matchWidth())
            addView(TextView(context).apply {
                text = getString(R.string.setup_body)
                textSize = 16f
                setPadding(0, padding, 0, padding)
            }, matchWidth())
            addView(Button(context).apply {
                text = getString(R.string.enable_ime)
                setOnClickListener { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) }
            }, matchWidth())
            addView(Button(context).apply {
                text = getString(R.string.choose_ime)
                setOnClickListener {
                    (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
                }
            }, matchWidth())
            addView(Button(context).apply {
                text = getString(R.string.test_input)
                setOnClickListener { startActivity(Intent(context, ImeTestActivity::class.java)) }
            }, matchWidth())
        })
    }

    private fun matchWidth() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT,
    )
}
