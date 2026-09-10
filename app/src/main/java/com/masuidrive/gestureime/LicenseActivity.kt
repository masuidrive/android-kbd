package com.masuidrive.gestureime

import android.os.Bundle
import android.text.Html
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class LicenseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.licenses)
        val padding = (20 * resources.displayMetrics.density).toInt()
        val notices = assets.list("licenses").orEmpty().sorted().joinToString("\n\n") { name ->
            val source = assets.open("licenses/$name").bufferedReader().use { it.readText() }
            "===== $name =====\n" + if (name.endsWith(".html")) {
                Html.fromHtml(source, Html.FROM_HTML_MODE_LEGACY).toString()
            } else source
        }
        setContentView(ScrollView(this).apply {
            addView(TextView(context).apply {
                text = notices
                textSize = 12f
                setPadding(padding, padding, padding, padding)
                setTextIsSelectable(true)
            })
        })
    }
}
