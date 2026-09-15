package com.masuidrive.gestureime

import android.view.View
import android.text.InputType
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ImeTestActivitySafeAreaTest {
    @Test
    fun fixedAppBarOwnsBackControlWhileInputContentScrolls() {
        val activity = createActivity()
        val root = activity.findViewById<LinearLayout>(R.id.ime_test_root)
        val appBar = activity.findViewById<LinearLayout>(R.id.ime_test_app_bar)
        val scroll = activity.findViewById<ScrollView>(R.id.ime_test_scroll)
        val title = activity.findViewById<TextView>(R.id.ime_test_app_bar_title)
        val back = activity.findViewById<ImageButton>(R.id.ime_test_back)
        val minimum = (48 * activity.resources.displayMetrics.density).toInt()

        assertEquals(2, root.childCount)
        assertSame(appBar, root.getChildAt(0))
        assertSame(scroll, root.getChildAt(1))
        assertEquals("masuidrive-kbd 入力テスト", title.text.toString())
        assertEquals("戻る", back.contentDescription)
        assertTrue(back.minimumWidth >= minimum)
        assertTrue(back.minimumHeight >= minimum)
        assertSame(activity.findViewById<View>(R.id.ime_test_content), scroll.getChildAt(0))
        back.performClick()
        assertTrue(activity.isFinishing)
    }

    @Test
    fun safeAreaInsetsAreIdempotentAndKeepNavigationSpaceAtScrollEnd() {
        val activity = createActivity()
        val root = activity.findViewById<View>(R.id.ime_test_root)
        val appBar = activity.findViewById<View>(R.id.ime_test_app_bar)
        val content = activity.findViewById<View>(R.id.ime_test_content)
        val density = activity.resources.displayMetrics.density
        val baseContent = (20 * density).toInt()
        val baseAppBarStart = (8 * density).toInt()
        val inset = WindowInsetsCompat.Builder()
            .setInsets(WindowInsetsCompat.Type.systemBars(), Insets.of(2, 30, 3, 42))
            .setInsets(WindowInsetsCompat.Type.displayCutout(), Insets.of(7, 48, 9, 12))
            .build()

        ViewCompat.dispatchApplyWindowInsets(root, inset)
        val first = intArrayOf(appBar.paddingLeft, appBar.paddingTop, appBar.paddingRight,
            appBar.layoutParams.height, content.paddingLeft, content.paddingRight, content.paddingBottom)
        ViewCompat.dispatchApplyWindowInsets(root, inset)

        assertEquals(baseAppBarStart + 7, appBar.paddingLeft)
        assertEquals(48, appBar.paddingTop)
        assertEquals(baseContent + 7, content.paddingLeft)
        assertEquals(baseContent + 9, content.paddingRight)
        assertEquals(baseContent + 42, content.paddingBottom)
        assertEquals((56 * density).toInt() + 48, appBar.layoutParams.height)
        assertEquals(first.toList(), intArrayOf(appBar.paddingLeft, appBar.paddingTop, appBar.paddingRight,
            appBar.layoutParams.height, content.paddingLeft, content.paddingRight, content.paddingBottom).toList())
    }

    @Test
    fun appAndImeServiceExposeThePublicProductName() {
        val activity = createActivity()
        val packageManager = activity.packageManager

        assertEquals("masuidrive-kbd", packageManager.getApplicationInfo(activity.packageName, 0).loadLabel(packageManager))
        assertEquals(
            "masuidrive-kbd",
            packageManager.getServiceInfo(
                android.content.ComponentName(activity, ImeService::class.java),
                0,
            ).loadLabel(packageManager),
        )
    }

    @Test
    fun inputTestScreenExposesNormalNumberEmailAndPasswordEditorTypes() {
        val activity = createActivity()

        assertEquals(
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE,
            activity.findViewById<android.widget.EditText>(R.id.ime_test_normal_field).inputType,
        )
        assertEquals(
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED or InputType.TYPE_NUMBER_FLAG_DECIMAL,
            activity.findViewById<android.widget.EditText>(R.id.ime_test_number_field).inputType,
        )
        assertEquals(
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
            activity.findViewById<android.widget.EditText>(R.id.ime_test_email_field).inputType,
        )
        assertEquals(
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD,
            activity.findViewById<android.widget.EditText>(R.id.ime_test_password_field).inputType,
        )
    }

    private fun createActivity() = Robolectric.buildActivity(ImeTestActivity::class.java).setup().get()
}
