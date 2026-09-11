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
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity

class SetupActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val padding = (24 * resources.displayMetrics.density).toInt()
        val controlHeight = (48 * resources.displayMetrics.density).toInt()
        val content = LinearLayout(this).apply {
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
                setPadding(0, padding / 2, 0, padding / 2)
            }, matchWidth())
            addSectionTitle(getString(R.string.initial_setup), padding)
            addView(Button(context).apply {
                text = getString(R.string.enable_ime)
                minimumHeight = controlHeight
                setOnClickListener { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) }
            }, matchWidth())
            addView(Button(context).apply {
                text = getString(R.string.choose_ime)
                minimumHeight = controlHeight
                setOnClickListener {
                    (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
                }
            }, matchWidth())
            addView(Button(context).apply {
                text = getString(R.string.test_input)
                minimumHeight = controlHeight
                setOnClickListener { startActivity(Intent(context, ImeTestActivity::class.java)) }
            }, matchWidth())
            addSectionTitle(getString(R.string.input_settings), padding)
            addView(Switch(context).apply {
                text = getString(R.string.dual_flick)
                minimumHeight = controlHeight
                isChecked = ImePreferences.isDualFlickEnabled(context)
                setOnCheckedChangeListener { _, checked ->
                    ImePreferences.setDualFlickEnabled(context, checked)
                }
            }, matchWidth())
            addView(Switch(context).apply {
                text = getString(R.string.terminal_cursor)
                minimumHeight = controlHeight
                isChecked = ImePreferences.isTerminalCursorEnabled(context)
                setOnCheckedChangeListener { _, checked -> ImePreferences.setTerminalCursorEnabled(context, checked) }
            }, matchWidth())
            addView(Switch(context).apply {
                text = getString(R.string.english_suggestions)
                minimumHeight = controlHeight
                isChecked = ImePreferences.isEnglishSuggestionsEnabled(context)
                setOnCheckedChangeListener { _, checked -> ImePreferences.setEnglishSuggestionsEnabled(context, checked) }
            }, matchWidth())
            addView(Switch(context).apply {
                text = getString(R.string.android_user_dictionary)
                minimumHeight = controlHeight
                isChecked = ImePreferences.isAndroidUserDictionaryEnabled(context)
                setOnCheckedChangeListener { _, checked -> ImePreferences.setAndroidUserDictionaryEnabled(context, checked) }
            }, matchWidth())
            addSectionTitle(getString(R.string.slash_commands), padding)
            val slashInputs = ImePreferences.getSlashCommands(context).mapIndexed { index, command ->
                EditText(context).apply {
                    setText(command)
                    hint = getString(R.string.slash_command_hint, index + 1)
                    isSingleLine = true
                    contentDescription = getString(R.string.slash_command_description, index + 1)
                    minimumHeight = controlHeight
                    addView(this, matchWidth())
                }
            }
            addView(Button(context).apply {
                text = getString(R.string.save_slash_commands)
                minimumHeight = controlHeight
                setOnClickListener {
                    ImePreferences.setSlashCommands(context, slashInputs.map { input -> input.text.toString() })
                    slashInputs.zip(ImePreferences.getSlashCommands(context)).forEach { (input, value) -> input.setText(value) }
                }
            }, matchWidth())
            addSectionTitle(getString(R.string.app_information), padding)
            addView(TextView(context).apply {
                text = getString(R.string.version_format, BuildConfig.VERSION_NAME)
                textSize = 16f
                minimumHeight = controlHeight
                gravity = Gravity.CENTER_VERTICAL
            }, matchWidth())
            addView(Button(context).apply {
                text = getString(R.string.licenses)
                minimumHeight = controlHeight
                setOnClickListener { startActivity(Intent(context, LicenseActivity::class.java)) }
            }, matchWidth())
        }
        setContentView(ScrollView(this).apply { addView(content) })
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

    private fun LinearLayout.addSectionTitle(label: String, spacing: Int) {
        addView(TextView(context).apply {
            text = label
            textSize = 18f
            setPadding(0, spacing, 0, spacing / 3)
        }, matchWidth())
    }

    companion object {
        const val EXTRA_REQUEST_MICROPHONE_PERMISSION = "request_microphone_permission"
        private const val REQUEST_MICROPHONE_PERMISSION = 301
    }
}
