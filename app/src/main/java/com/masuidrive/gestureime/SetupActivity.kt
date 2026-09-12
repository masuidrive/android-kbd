package com.masuidrive.gestureime

import android.Manifest
import android.content.Intent
import android.content.res.ColorStateList
import android.content.pm.PackageManager
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
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
        val cardPadding = (16 * resources.displayMetrics.density).toInt()
        val controlHeight = (48 * resources.displayMetrics.density).toInt()
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundColor(getColor(R.color.setup_page))
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
            setSectionTitle(getString(R.string.initial_setup), padding)
            val initialCard = addCard(cardPadding)
            initialCard.addView(Button(context).apply {
                text = getString(R.string.enable_ime)
                minimumHeight = controlHeight
                setOnClickListener { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) }
            }, matchWidth())
            initialCard.addView(Button(context).apply {
                text = getString(R.string.choose_ime)
                minimumHeight = controlHeight
                setOnClickListener {
                    (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
                }
            }, matchWidth())
            initialCard.addView(Button(context).apply {
                text = getString(R.string.test_input)
                minimumHeight = controlHeight
                setOnClickListener { startActivity(Intent(context, ImeTestActivity::class.java)) }
            }, matchWidth())
            setSectionTitle(getString(R.string.input_settings), padding)
            val inputCard = addCard(cardPadding)
            inputCard.addView(Switch(context).apply {
                text = getString(R.string.dual_flick)
                minimumHeight = controlHeight
                isChecked = ImePreferences.isDualFlickEnabled(context)
                setOnCheckedChangeListener { _, checked ->
                    ImePreferences.setDualFlickEnabled(context, checked)
                }
            }, matchWidth())
            inputCard.addView(Switch(context).apply {
                text = getString(R.string.terminal_cursor)
                minimumHeight = controlHeight
                isChecked = ImePreferences.isTerminalCursorEnabled(context)
                setOnCheckedChangeListener { _, checked -> ImePreferences.setTerminalCursorEnabled(context, checked) }
            }, matchWidth())
            inputCard.addView(Switch(context).apply {
                text = getString(R.string.english_suggestions)
                minimumHeight = controlHeight
                isChecked = ImePreferences.isEnglishSuggestionsEnabled(context)
                setOnCheckedChangeListener { _, checked -> ImePreferences.setEnglishSuggestionsEnabled(context, checked) }
            }, matchWidth())
            inputCard.addView(Switch(context).apply {
                text = getString(R.string.android_user_dictionary)
                minimumHeight = controlHeight
                isChecked = ImePreferences.isAndroidUserDictionaryEnabled(context)
                setOnCheckedChangeListener { _, checked -> ImePreferences.setAndroidUserDictionaryEnabled(context, checked) }
            }, matchWidth())
            setSectionTitle(getString(R.string.slash_commands), padding)
            val slashCard = addCard(cardPadding)
            val slashInputs = ImePreferences.getSlashCommands(context).mapIndexed { index, command ->
                EditText(context).apply {
                    setText(command)
                    hint = getString(R.string.slash_command_hint, index + 1)
                    isSingleLine = true
                    contentDescription = getString(R.string.slash_command_description, index + 1)
                    minimumHeight = controlHeight
                    slashCard.addView(this, matchWidth())
                }
            }
            slashCard.addView(Button(context).apply {
                text = getString(R.string.save_slash_commands)
                minimumHeight = controlHeight
                setOnClickListener {
                    ImePreferences.setSlashCommands(context, slashInputs.map { input -> input.text.toString() })
                    slashInputs.zip(ImePreferences.getSlashCommands(context)).forEach { (input, value) -> input.setText(value) }
                }
            }, matchWidth())
            setSectionTitle(getString(R.string.app_information), padding)
            val infoCard = addCard(cardPadding)
            infoCard.addView(TextView(context).apply {
                text = getString(R.string.version_format, BuildConfig.VERSION_NAME)
                textSize = 16f
                minimumHeight = controlHeight
                gravity = Gravity.CENTER_VERTICAL
            }, matchWidth())
            infoCard.addView(Button(context).apply {
                text = getString(R.string.licenses)
                minimumHeight = controlHeight
                setOnClickListener { startActivity(Intent(context, LicenseActivity::class.java)) }
            }, matchWidth())
        }
        content.tintButtonSurfaces()
        setContentView(ScrollView(this).apply {
            setBackgroundColor(getColor(R.color.setup_page))
            isFillViewport = true
            addView(content)
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

    private fun LinearLayout.setSectionTitle(label: String, spacing: Int) {
        addView(TextView(context).apply {
            text = label
            textSize = 18f
            setTextColor(getColor(R.color.setup_heading))
            val accent = ColorDrawable(getColor(R.color.setup_divider)).apply {
                setBounds(0, 0, (6 * resources.displayMetrics.density).toInt(), (24 * resources.displayMetrics.density).toInt())
            }
            setCompoundDrawables(accent, null, null, null)
            compoundDrawablePadding = (12 * resources.displayMetrics.density).toInt()
            setPadding(0, spacing / 2, 0, spacing / 3)
        }, matchWidth())
    }

    private fun LinearLayout.addCard(padding: Int): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(padding, padding / 2, padding, padding / 2)
        background = GradientDrawable().apply {
            setColor(getColor(R.color.setup_surface))
            cornerRadius = 12 * resources.displayMetrics.density
        }
        elevation = resources.displayMetrics.density
        this@addCard.addView(this, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ).apply { bottomMargin = padding / 2 })
    }

    private fun View.tintButtonSurfaces() {
        if (this is Button) backgroundTintList = ColorStateList.valueOf(getColor(R.color.setup_control))
        if (this is ViewGroup) repeat(childCount) { getChildAt(it).tintButtonSurfaces() }
    }

    companion object {
        const val EXTRA_REQUEST_MICROPHONE_PERMISSION = "request_microphone_permission"
        private const val REQUEST_MICROPHONE_PERMISSION = 301
    }
}
