package com.masuidrive.gestureime

import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ImeServiceVoiceLifecycleTest {
    @Test
    fun closingAndReopeningInputViewHidesThenRecreatesVoiceControl() {
        val controller = Robolectric.buildService(ImeService::class.java).create()
        val service = controller.get()
        val firstView = service.onCreateInputView()
        assertTrue(firstView.hasVoiceControl())

        service.onFinishInputView(false)
        assertFalse(firstView.hasVoiceControl())

        service.onStartInputView(EditorInfo(), true)
        assertTrue(firstView.hasVoiceControl())

        val reopenedView = service.onCreateInputView()
        assertTrue(reopenedView.hasVoiceControl())
        controller.destroy()
    }

    private fun View.hasVoiceControl(): Boolean {
        if (contentDescription?.let { it.contains("音声") || it.contains("マイク") } == true) return true
        val group = this as? ViewGroup ?: return false
        return (0 until group.childCount).any { group.getChildAt(it).hasVoiceControl() }
    }
}
