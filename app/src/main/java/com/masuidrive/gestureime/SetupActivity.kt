package com.masuidrive.gestureime

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Switch
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
            addView(Switch(context).apply {
                text = getString(R.string.dual_flick)
                isChecked = ImePreferences.isDualFlickEnabled(context)
                setOnCheckedChangeListener { _, checked ->
                    ImePreferences.setDualFlickEnabled(context, checked)
                }
            }, matchWidth())
            addView(Switch(context).apply {
                text = getString(R.string.terminal_cursor)
                isChecked = ImePreferences.isTerminalCursorEnabled(context)
                setOnCheckedChangeListener { _, checked -> ImePreferences.setTerminalCursorEnabled(context, checked) }
            }, matchWidth())
            addView(Switch(context).apply {
                text = getString(R.string.english_suggestions)
                isChecked = ImePreferences.isEnglishSuggestionsEnabled(context)
                setOnCheckedChangeListener { _, checked -> ImePreferences.setEnglishSuggestionsEnabled(context, checked) }
            }, matchWidth())
            addView(Button(context).apply {
                text = getString(R.string.adjust_qwerty_labels)
                setOnClickListener {
                    startActivity(Intent(context, QwertyLabelAdjustmentActivity::class.java))
                }
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
            addView(Button(context).apply {
                text = getString(R.string.licenses)
                setOnClickListener { startActivity(Intent(context, LicenseActivity::class.java)) }
            }, matchWidth())
        })
        if (intent.getBooleanExtra(EXTRA_REQUEST_MICROPHONE_PERMISSION, false) &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_MICROPHONE_PERMISSION)
        }
    }

    private fun matchWidth() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT,
    )

    companion object {
        const val EXTRA_REQUEST_MICROPHONE_PERMISSION = "request_microphone_permission"
        private const val REQUEST_MICROPHONE_PERMISSION = 301
    }
}
