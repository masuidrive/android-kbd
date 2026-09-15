package com.masuidrive.gestureime

import android.content.res.ColorStateList
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat

class ImeTestActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SafeAreaUi.prepareWindow(this)

        val density = resources.displayMetrics.density
        val padding = (20 * density).toInt()
        val controlHeight = (48 * density).toInt()
        val appBarHeight = (56 * density).toInt()
        val content = LinearLayout(this).apply {
            id = R.id.ime_test_content
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundColor(getColor(R.color.setup_page))
            setPadding(padding, padding, padding, padding)
            addView(TextView(context).apply {
                text = getString(R.string.ime_test_normal_input)
                setTextColor(getColor(R.color.setup_heading))
            }, matchWidth())
            addView(EditText(context).apply {
                id = R.id.ime_test_normal_field
                hint = getString(R.string.test_hint)
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
                minLines = 4
            }, matchWidth())
            addView(TextView(context).apply {
                text = getString(R.string.ime_test_number_input)
                setTextColor(getColor(R.color.setup_heading))
                val top = (16 * density).toInt()
                setPadding(0, top, 0, 0)
            }, matchWidth())
            addView(EditText(context).apply {
                id = R.id.ime_test_number_field
                hint = getString(R.string.ime_test_number_hint)
                inputType = InputType.TYPE_CLASS_NUMBER or
                    InputType.TYPE_NUMBER_FLAG_SIGNED or InputType.TYPE_NUMBER_FLAG_DECIMAL
            }, matchWidth())
            addView(TextView(context).apply {
                text = getString(R.string.ime_test_email_input)
                setTextColor(getColor(R.color.setup_heading))
                val top = (16 * density).toInt()
                setPadding(0, top, 0, 0)
            }, matchWidth())
            addView(EditText(context).apply {
                id = R.id.ime_test_email_field
                hint = getString(R.string.ime_test_email_hint)
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            }, matchWidth())
            addView(TextView(context).apply {
                text = getString(R.string.ime_test_private_input)
                setTextColor(getColor(R.color.setup_heading))
                val top = (16 * density).toInt()
                setPadding(0, top, 0, 0)
            }, matchWidth())
            addView(EditText(context).apply {
                id = R.id.ime_test_password_field
                hint = getString(R.string.ime_test_password_hint)
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }, matchWidth())
        }
        val scroll = ScrollView(this).apply {
            id = R.id.ime_test_scroll
            setBackgroundColor(getColor(R.color.setup_page))
            isFillViewport = true
            clipToPadding = false
            addView(content)
        }
        val appBar = LinearLayout(this).apply {
            id = R.id.ime_test_app_bar
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(getColor(R.color.setup_page))
            setPadding((8 * density).toInt(), 0, padding, 0)
            addView(ImageButton(context).apply {
                id = R.id.ime_test_back
                contentDescription = getString(R.string.setup_back_description)
                minimumWidth = controlHeight
                minimumHeight = controlHeight
                setImageResource(R.drawable.ic_arrow_back)
                imageTintList = ColorStateList.valueOf(getColor(R.color.setup_heading))
                setBackgroundResource(selectableItemBackgroundRes())
                setOnClickListener { finish() }
            }, LinearLayout.LayoutParams(controlHeight, controlHeight))
            addView(TextView(context).apply {
                id = R.id.ime_test_app_bar_title
                text = getString(R.string.ime_test_app_bar_title)
                textSize = 20f
                gravity = Gravity.CENTER_VERTICAL
                setTextColor(getColor(R.color.setup_heading))
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply {
                leftMargin = (8 * density).toInt()
            })
        }
        val root = LinearLayout(this).apply {
            id = R.id.ime_test_root
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(getColor(R.color.setup_page))
            addView(appBar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, appBarHeight))
            addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        }
        SafeAreaUi.installFixedAppBarInsets(root, appBar, content, appBarHeight)
        setContentView(root)
        SafeAreaUi.applySystemBarIconAppearance(this, root)
        ViewCompat.requestApplyInsets(root)
    }

    private fun selectableItemBackgroundRes(): Int {
        val attribute = android.util.TypedValue()
        return if (theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, attribute, true)) {
            attribute.resourceId
        } else {
            0
        }
    }

    private fun matchWidth() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT,
    )
}
