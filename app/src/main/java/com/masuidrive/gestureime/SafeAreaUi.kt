package com.masuidrive.gestureime

import android.app.Activity
import android.content.res.Configuration
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

/** Shared edge-to-edge treatment for the setup and input-test screens. */
internal object SafeAreaUi {
    fun prepareWindow(activity: Activity) {
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)
        activity.window.statusBarColor = Color.TRANSPARENT
        activity.window.navigationBarColor = Color.TRANSPARENT
    }

    fun installFixedAppBarInsets(
        root: View,
        appBar: View,
        content: View,
        appBarContentHeight: Int,
    ) {
        val appBarStart = appBar.paddingLeft
        val appBarEnd = appBar.paddingRight
        val contentStart = content.paddingLeft
        val contentTop = content.paddingTop
        val contentEnd = content.paddingRight
        val contentBottom = content.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val safeArea = safeAreaInsets(insets)
            appBar.setPadding(appBarStart + safeArea.left, safeArea.top, appBarEnd + safeArea.right, 0)
            appBar.layoutParams = appBar.layoutParams.apply {
                height = appBarContentHeight + safeArea.top
            }
            content.setPadding(
                contentStart + safeArea.left,
                contentTop,
                contentEnd + safeArea.right,
                contentBottom + safeArea.bottom,
            )
            insets
        }
    }

    fun applySystemBarIconAppearance(activity: Activity, root: View) {
        val lightTheme = activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK != Configuration.UI_MODE_NIGHT_YES
        WindowCompat.getInsetsController(activity.window, root).apply {
            isAppearanceLightStatusBars = lightTheme
            isAppearanceLightNavigationBars = lightTheme
        }
    }

    fun safeAreaInsets(insets: WindowInsetsCompat): Insets {
        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        val cutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
        return Insets.of(
            maxOf(bars.left, cutout.left),
            maxOf(bars.top, cutout.top),
            maxOf(bars.right, cutout.right),
            maxOf(bars.bottom, cutout.bottom),
        )
    }
}
