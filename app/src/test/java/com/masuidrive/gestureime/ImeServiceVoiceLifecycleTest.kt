package com.masuidrive.gestureime

import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import com.masuidrive.gestureime.keyboard.KeyAction
import com.masuidrive.gestureime.keyboard.KeyboardMode
import com.masuidrive.gestureime.keyboard.KeyboardUiState
import com.masuidrive.gestureime.keyboard.KeyboardView
import org.junit.Assert.assertEquals
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
    fun switchedLayerIsRestoredWhenInputViewIsRecreated() {
        val controller = Robolectric.buildService(ImeService::class.java).create()
        val service = controller.get()
        service.onCreateInputView()

        service.onKeyAction(KeyAction.SwitchLayer(KeyboardMode.SYMBOLS))
        assertEquals(KeyboardMode.SYMBOLS, ImePreferences.getLastKeyboardMode(service))

        val recreated = service.onCreateInputView().keyboardView()
        assertEquals(KeyboardMode.SYMBOLS, recreated.mode())
        controller.destroy()
    }

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

    private fun View.keyboardView(): KeyboardView {
        if (this is KeyboardView) return this
        val group = this as? ViewGroup ?: error("KeyboardView not found")
        return (0 until group.childCount)
            .asSequence()
            .mapNotNull { runCatching { group.getChildAt(it).keyboardView() }.getOrNull() }
            .first()
    }

    private fun KeyboardView.mode(): KeyboardMode {
        val field = KeyboardView::class.java.getDeclaredField("state").apply { isAccessible = true }
        return (field.get(this) as KeyboardUiState).mode
    }
}
