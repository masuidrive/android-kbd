package com.masuidrive.gestureime.keyboard

enum class KeyboardMode(val displayName: String) {
    KANA("日本語"), NUMBERS("テンキー"), EMOJI("絵文字"), QWERTY("QWERTY"), SYMBOLS("記号"), VOICE("音声")
}

enum class KeyboardHeightPreset(val rowPitchDp: Float) {
    SMALL(50f), STANDARD(55f), LARGE(60f),
}

enum class Direction { CENTER, LEFT, UP, RIGHT, DOWN }
enum class Modifier { CTRL, ALT }
enum class KanaTransform { CYCLE, SMALL, DAKUTEN, HANDAKUTEN }
enum class KeyKind { CHARACTER, KANA, MODE, LAYER_SWITCH, MODIFIER, BACKSPACE, SPACE, ENTER, ACCENT, EMPTY }
enum class CursorBoundary { START, END }

data class FlickValue(val label: String, val action: KeyAction)

data class KeySpec(
    val id: String,
    val kind: KeyKind,
    val center: FlickValue?,
    val left: FlickValue? = null,
    val up: FlickValue? = null,
    val right: FlickValue? = null,
    val down: FlickValue? = null,
    val widthUnits: Float = 1f,
    val rowSpan: Int = 1,
    val dark: Boolean = false,
) {
    fun value(direction: Direction): FlickValue? = when (direction) {
        Direction.CENTER -> center
        Direction.LEFT -> left
        Direction.UP -> up
        Direction.RIGHT -> right
        Direction.DOWN -> down
    }
}

data class KeyboardRow(val keys: List<KeySpec>)
data class KeyboardLayout(val mode: KeyboardMode, val rows: List<KeyboardRow>)

sealed interface KeyAction {
    data class CommitText(val text: String) : KeyAction
    /** Kept separate so the service can update local recents without inspecting unicode text. */
    data class CommitEmoji(val text: String) : KeyAction
    data class KanaInput(val reading: String) : KeyAction
    data class ModifiedKey(val label: String, val modifier: Modifier) : KeyAction
    data class MoveCursor(val direction: Direction, val units: Int = 1) : KeyAction
    data class MoveToBoundary(val boundary: CursorBoundary) : KeyAction
    data class SwitchLayer(val target: KeyboardMode) : KeyAction
    data class ChangeEmojiPage(val delta: Int) : KeyAction
    data class SetModifier(val modifier: Modifier?) : KeyAction
    data class TransformKana(val transform: KanaTransform) : KeyAction
    data object Escape : KeyAction
    data object CommitConversion : KeyAction
    data object CommitWithoutConversion : KeyAction
    data object ConvertToKatakana : KeyAction
    data class SelectCandidate(val index: Int) : KeyAction
    data object CycleCandidate : KeyAction
    data class Backspace(val repeat: Boolean = false) : KeyAction
    data object Enter : KeyAction
    data object Paste : KeyAction
    data object VoiceHold : KeyAction
    data object CancelVoice : KeyAction
}

fun interface KeyboardActionSink {
    fun onKeyAction(action: KeyAction)
}

sealed interface VoiceHoldEvent {
    val requestId: Long
    data class Begin(override val requestId: Long) : VoiceHoldEvent
    data class End(override val requestId: Long) : VoiceHoldEvent
    data class Cancel(override val requestId: Long) : VoiceHoldEvent
}

fun interface VoiceHoldSink {
    fun onVoiceHold(event: VoiceHoldEvent)
}

data class KeyboardUiState(
    val mode: KeyboardMode = KeyboardMode.QWERTY,
    val pendingModifier: Modifier? = null,
    val candidates: List<String> = emptyList(),
    val selectedCandidateIndex: Int = -1,
    val conversionActive: Boolean = false,
    val dualFlickEnabled: Boolean = false,
    val heightPreset: KeyboardHeightPreset = KeyboardHeightPreset.STANDARD,
    val emojiRecents: List<String> = emptyList(),
    val emojiPage: Int = 0,
)
