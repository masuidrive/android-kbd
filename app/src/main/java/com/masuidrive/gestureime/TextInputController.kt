package com.masuidrive.gestureime

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.icu.text.BreakIterator
import android.text.InputType
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputConnection
import com.masuidrive.gestureime.keyboard.Direction
import com.masuidrive.gestureime.keyboard.Modifier
import com.masuidrive.gestureime.keyboard.CursorBoundary

class TextInputController(
    private val connection: () -> InputConnection?,
    private val context: Context,
    private val clipboard: ClipboardManager,
) {
    private var editorInfo: EditorInfo = EditorInfo()
    private var composing = ""

    val isPrivateField: Boolean
        get() = editorInfo.isPasswordField() ||
            editorInfo.imeOptions and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING != 0

    fun beginInput(info: EditorInfo) {
        editorInfo = info
        composing = ""
    }

    fun appendComposing(text: String): String {
        if (isPrivateField) {
            commitText(text)
            return ""
        }
        composing += text
        connection()?.setComposingText(composing, 1)
        return composing
    }

    fun replaceComposing(reading: String) {
        composing = reading
        if (reading.isEmpty()) connection()?.finishComposingText()
        else connection()?.setComposingText(reading, 1)
    }

    fun commitCandidate(text: String) {
        connection()?.commitText(text, 1)
        composing = ""
    }

    fun commitText(text: String) {
        finishComposition()
        connection()?.commitText(text, 1)
    }

    fun finishComposition() {
        if (composing.isNotEmpty()) connection()?.finishComposingText()
        composing = ""
    }

    fun cancelComposition() {
        if (composing.isNotEmpty()) connection()?.setComposingText("", 1)
        composing = ""
    }

    fun abandonComposition() {
        composing = ""
    }

    fun backspace(): String {
        val input = connection() ?: return composing
        val selected = input.getSelectedText(0)
        if (!selected.isNullOrEmpty()) {
            input.commitText("", 1)
            return composing
        }
        if (composing.isNotEmpty()) {
            composing = composing.dropLastGrapheme()
            if (composing.isEmpty()) input.setComposingText("", 1)
            else input.setComposingText(composing, 1)
            return composing
        }
        val before = input.getTextBeforeCursor(64, 0)?.toString().orEmpty()
        val deleteChars = before.lastGraphemeLength()
        if (deleteChars > 0) input.deleteSurroundingText(deleteChars, 0)
        else sendKey(KeyEvent.KEYCODE_DEL)
        return composing
    }

    fun transformKana(): String {
        if (composing.isEmpty()) return composing
        val last = composing.substring(composing.offsetByCodePoints(composing.length, -1))
        val transformed = KANA_TRANSFORMS[last] ?: return composing
        composing = composing.dropLast(last.length) + transformed
        connection()?.setComposingText(composing, 1)
        return composing
    }

    fun moveCursor(direction: Direction, units: Int) {
        finishComposition()
        val input = connection() ?: return
        if (direction == Direction.UP || direction == Direction.DOWN) {
            repeat(units.coerceAtLeast(1)) {
                sendKey(if (direction == Direction.UP) KeyEvent.KEYCODE_DPAD_UP else KeyEvent.KEYCODE_DPAD_DOWN)
            }
            return
        }
        val extracted = input.getExtractedText(ExtractedTextRequest(), 0) ?: return
        val text = extracted.text?.toString().orEmpty()
        var cursor = extracted.selectionEnd.coerceIn(0, text.length)
        val iterator = BreakIterator.getCharacterInstance().apply { setText(text) }
        repeat(units.coerceAtLeast(1)) {
            cursor = when (direction) {
                Direction.LEFT -> iterator.preceding(cursor).takeUnless { it == BreakIterator.DONE } ?: 0
                Direction.RIGHT -> iterator.following(cursor).takeUnless { it == BreakIterator.DONE } ?: text.length
                else -> cursor
            }
        }
        val absoluteCursor = extracted.startOffset + cursor
        input.setSelection(absoluteCursor, absoluteCursor)
    }

    fun moveToBoundary(boundary: CursorBoundary) {
        finishComposition()
        val input = connection() ?: return
        val extracted = input.getExtractedText(ExtractedTextRequest(), 0) ?: return
        val position = when (boundary) {
            CursorBoundary.START -> 0
            CursorBoundary.END -> extracted.startOffset + (extracted.text?.length ?: 0)
        }
        input.setSelection(position, position)
    }

    fun enter() {
        finishComposition()
        val action = editorInfo.imeOptions and EditorInfo.IME_MASK_ACTION
        val noAction = editorInfo.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION != 0
        if (!noAction && action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
            connection()?.performEditorAction(action)
        } else {
            connection()?.commitText("\n", 1)
        }
    }

    fun paste() {
        if (isPrivateField || !clipboard.hasPrimaryClip()) return
        val clip = clipboard.primaryClip ?: return
        if (!clip.description.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) &&
            !clip.description.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML)
        ) return
        clip.getItemAt(0).coerceToText(context)?.toString()?.takeIf { it.isNotEmpty() }?.let(::commitText)
    }

    fun sendModifiedKey(label: String, modifier: Modifier) {
        finishComposition()
        if (isPrivateField && modifier == Modifier.CTRL && label.equals("v", ignoreCase = true)) return
        val contextAction = if (modifier == Modifier.CTRL) CTRL_CONTEXT_ACTIONS[label.lowercase()] else null
        if (contextAction != null && connection()?.performContextMenuAction(contextAction) == true) return
        val keyCode = KeyEvent.keyCodeFromString("KEYCODE_${label.uppercase()}")
        if (keyCode == KeyEvent.KEYCODE_UNKNOWN) return
        val meta = if (modifier == Modifier.CTRL) KeyEvent.META_CTRL_ON else KeyEvent.META_ALT_ON
        sendKey(keyCode, meta)
    }

    private fun sendKey(keyCode: Int, meta: Int = 0) {
        val input = connection() ?: return
        input.sendKeyEvent(KeyEvent(0L, 0L, KeyEvent.ACTION_DOWN, keyCode, 0, meta))
        input.sendKeyEvent(KeyEvent(0L, 0L, KeyEvent.ACTION_UP, keyCode, 0, meta))
    }
}

private fun EditorInfo.isPasswordField(): Boolean {
    val variation = inputType and InputType.TYPE_MASK_VARIATION
    return when (inputType and InputType.TYPE_MASK_CLASS) {
        InputType.TYPE_CLASS_TEXT -> variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
        InputType.TYPE_CLASS_NUMBER -> variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD
        else -> false
    }
}

private fun String.dropLastGrapheme(): String = dropLast(lastGraphemeLength())

private fun String.lastGraphemeLength(): Int {
    if (isEmpty()) return 0
    val iterator = BreakIterator.getCharacterInstance().apply { setText(this@lastGraphemeLength) }
    val start = iterator.preceding(length).takeUnless { it == BreakIterator.DONE } ?: 0
    return length - start
}

private val KANA_TRANSFORMS = mapOf(
    "か" to "が", "が" to "か", "き" to "ぎ", "ぎ" to "き", "く" to "ぐ", "ぐ" to "く",
    "け" to "げ", "げ" to "け", "こ" to "ご", "ご" to "こ", "さ" to "ざ", "ざ" to "さ",
    "し" to "じ", "じ" to "し", "す" to "ず", "ず" to "す", "せ" to "ぜ", "ぜ" to "せ",
    "そ" to "ぞ", "ぞ" to "そ", "た" to "だ", "だ" to "た", "ち" to "ぢ", "ぢ" to "ち",
    "つ" to "っ", "っ" to "づ", "づ" to "つ", "て" to "で", "で" to "て", "と" to "ど", "ど" to "と",
    "は" to "ば", "ば" to "ぱ", "ぱ" to "は", "ひ" to "び", "び" to "ぴ", "ぴ" to "ひ",
    "ふ" to "ぶ", "ぶ" to "ぷ", "ぷ" to "ふ", "へ" to "べ", "べ" to "ぺ", "ぺ" to "へ",
    "ほ" to "ぼ", "ぼ" to "ぽ", "ぽ" to "ほ", "や" to "ゃ", "ゃ" to "や", "ゆ" to "ゅ",
    "ゅ" to "ゆ", "よ" to "ょ", "ょ" to "よ", "わ" to "ゎ", "ゎ" to "わ", "あ" to "ぁ",
    "ぁ" to "あ", "い" to "ぃ", "ぃ" to "い", "う" to "ぅ", "ぅ" to "ゔ", "ゔ" to "う",
    "え" to "ぇ", "ぇ" to "え", "お" to "ぉ", "ぉ" to "お",
)

private val CTRL_CONTEXT_ACTIONS = mapOf(
    "a" to android.R.id.selectAll,
    "c" to android.R.id.copy,
    "x" to android.R.id.cut,
    "v" to android.R.id.paste,
)
