package com.masuidrive.gestureime.keyboard

object KeyboardLayouts {
    val all: Map<KeyboardMode, KeyboardLayout> = KeyboardMode.entries.associateWith(::layout)

    fun layout(
        mode: KeyboardMode,
        dualKana: Boolean = false,
        conversionActive: Boolean = false,
        emojiRecents: List<String> = emptyList(),
    ): KeyboardLayout = when (mode) {
        KeyboardMode.QWERTY -> qwerty()
        KeyboardMode.SYMBOLS -> symbols()
        KeyboardMode.KANA -> kana(dualKana, conversionActive)
        KeyboardMode.NUMBERS -> numbers()
        KeyboardMode.EMOJI -> emoji(emojiRecents)
        KeyboardMode.VOICE -> voice()
    }

    private fun text(label: String, secondary: String? = null, kind: KeyKind = KeyKind.CHARACTER) =
        KeySpec("key-$label", kind, FlickValue(label, KeyAction.CommitText(label)),
            up = if (label.length == 1 && label[0].isLetter()) FlickValue(label.uppercase(), KeyAction.CommitText(label.uppercase())) else null,
            down = secondary?.let { FlickValue(it, KeyAction.CommitText(it)) })

    private fun qwerty(): KeyboardLayout = KeyboardLayout(KeyboardMode.QWERTY, listOf(
        KeyboardRow("qwertyuiop".mapIndexed { i, c -> text(c.toString(), "1234567890"[i].toString()) }),
        KeyboardRow(listOf(modifier()) + "asdfghjkl".mapIndexed { i, c -> text(c.toString(), listOf("@", "#", "\$", "&", "*", "(", ")", "'", "\"")[i]) } + backspace(tapDelete = true, escapeOnDown = true, deleteSymbol = true)),
        KeyboardRow(listOf(modeKey("#!", KeyboardMode.SYMBOLS)) + "zxcvbnm".mapIndexed { i, c -> text(c.toString(), listOf("%", "-", "+", "=", "/", ";", ":")[i]) } + listOf(text(",", "!"), text(".", "?"))),
        KeyboardRow(listOf(layerKey("あん", KeyboardMode.KANA, 1.45f), space(width = 4.2f), enter(width = 2f)))
    ))

    private fun symbols(): KeyboardLayout = KeyboardLayout(KeyboardMode.SYMBOLS, listOf(
        KeyboardRow("1234567890".map { text(it.toString()) }),
        KeyboardRow(listOf(modifier(), text("^"), text("_"), text("\\"), text("|"), text("~"), text("{"), text("}"), text("["), text("]"), backspace())),
        KeyboardRow(listOf(modeKey("AZ", KeyboardMode.QWERTY), escape(), text("`"), text("!"), text("?"), text(";"), tab(), text("<"), text(">"), text("-"))),
        KeyboardRow(listOf(layerKey("あん", KeyboardMode.KANA, 1.45f), space(width = 4.2f), enter(width = 2f)))
    ))

    private fun kana(dual: Boolean, conversionActive: Boolean): KeyboardLayout {
        val rows = listOf(
        KeyboardRow(listOf(emojiPad(), kana("あ", "い", "う", "え", "お"), kana("か", "き", "く", "け", "こ"), kana("さ", "し", "す", "せ", "そ"), backspace(1f))),
        KeyboardRow(listOf(modeKey("#!", KeyboardMode.SYMBOLS), kana("た", "ち", "つ", "て", "と"), kana("な", "に", "ぬ", "ね", "の"), kana("は", "ひ", "ふ", "へ", "ほ"), space())),
        KeyboardRow(listOf(modeKey("19", KeyboardMode.NUMBERS), kana("ま", "み", "む", "め", "も"), kana("や", "（", "ゆ", "）", "よ"), kana("ら", "り", "る", "れ", "ろ"), enter(rowSpan = 2, conversionActive = conversionActive))),
        KeyboardRow(listOf(layerKey("AZ", KeyboardMode.QWERTY), accent(), kana("わ", "を", "ん", "ー", "〜"), punct()))
        )
        return KeyboardLayout(KeyboardMode.KANA, if (dual) rows.map(::duplicateKanaCenter) else rows)
    }

    private fun duplicateKanaCenter(row: KeyboardRow): KeyboardRow {
        val left = row.keys.first()
        val center = row.keys.drop(1).take(3)
        val right = row.keys.drop(4)
        return KeyboardRow(listOf(left) + center + center + right)
    }

    private fun numbers(): KeyboardLayout = KeyboardLayout(KeyboardMode.NUMBERS, listOf(
        KeyboardRow(listOf(emojiPad(), text("1"), text("2"), text("3"), backspace(1f))),
        KeyboardRow(listOf(modeKey("#!", KeyboardMode.SYMBOLS), text("4"), text("5"), text("6"), space())),
        KeyboardRow(listOf(modeKey("あん", KeyboardMode.KANA), text("7"), text("8"), text("9"), enter(rowSpan = 2))),
        KeyboardRow(listOf(layerKey("AZ", KeyboardMode.QWERTY), fiveWay("-", "+", "/", "*", ","), text("0"), text(".")))
    ))

    /** Emoji content is rendered by AndroidX EmojiPickerView above this fixed control row. */
    private fun emoji(recents: List<String>): KeyboardLayout = KeyboardLayout(
        KeyboardMode.EMOJI,
        emojiContentRows(recents) + emojiControlRow(),
    )

    /** Three occupied rows are reserved for the AndroidX picker overlay. */
    fun emojiContentRows(recents: List<String>): List<KeyboardRow> =
        List(3) { KeyboardRow(listOf(empty(8f))) }

    fun emojiControlRow(): KeyboardRow = KeyboardRow(listOf(
        layerKey("AZ", KeyboardMode.QWERTY, 1.6f),
        empty(4.8f),
        backspace(1.6f),
    ))

    private fun voice(): KeyboardLayout = KeyboardLayout(KeyboardMode.VOICE, listOf(
        KeyboardRow(listOf(empty(6f))),
        KeyboardRow(listOf(empty(6f))),
        KeyboardRow(listOf(empty(6f))),
        KeyboardRow(listOf(voiceLayerKey(), voiceStatusSlot(), voicePunct(), space(), voiceBackspace(), enter())),
    ))

    private fun kana(center: String, left: String, up: String, right: String, down: String) = KeySpec(
        "kana-$center", KeyKind.KANA, kanaValue(center), kanaValue(left), kanaValue(up), kanaValue(right), kanaValue(down)
    )

    private fun kanaValue(label: String) = FlickValue(label, KeyAction.KanaInput(label))
    private fun fiveWay(center: String, left: String, up: String, right: String, down: String) = KeySpec(
        "five-$center", KeyKind.CHARACTER, value(center), value(left), value(up), value(right), value(down)
    )
    private fun value(label: String) = FlickValue(label, KeyAction.CommitText(label))

    private fun modifier() = KeySpec("modifier", KeyKind.MODIFIER, null,
        up = FlickValue("A", KeyAction.SetModifier(Modifier.ALT)),
        down = FlickValue("C", KeyAction.SetModifier(Modifier.CTRL)), widthUnits = .5f, dark = true)

    private fun backspace(width: Float = .5f, tapDelete: Boolean = width >= 1f, escapeOnDown: Boolean = false, deleteSymbol: Boolean = false) = KeySpec(
        "backspace", KeyKind.BACKSPACE,
        center = if (tapDelete) FlickValue(if (deleteSymbol || width >= 1f) "⌫" else "BS", KeyAction.Backspace()) else null,
        down = when {
            escapeOnDown -> FlickValue("ESC", KeyAction.Escape)
            width < 1f -> FlickValue("BS", KeyAction.Backspace())
            else -> null
        },
        widthUnits = width,
    )

    /** Voice editing deletes one character on tap only, without a directional fallback. */
    private fun voiceBackspace() = KeySpec(
        "voice-backspace", KeyKind.BACKSPACE,
        center = FlickValue("⌫", KeyAction.Backspace()),
    )

    private fun accent() = KeySpec(
        "accent", KeyKind.ACCENT,
        center = FlickValue("小", KeyAction.TransformKana(KanaTransform.CYCLE)),
        left = FlickValue("゛", KeyAction.TransformKana(KanaTransform.DAKUTEN)),
        up = FlickValue("゛", KeyAction.TransformKana(KanaTransform.DAKUTEN)),
        right = FlickValue("゜", KeyAction.TransformKana(KanaTransform.HANDAKUTEN)),
        dark = true,
    )

    private fun escape() = KeySpec("escape", KeyKind.CHARACTER, FlickValue("Esc", KeyAction.Escape))

    private fun tab() = KeySpec("tab", KeyKind.CHARACTER, FlickValue("Tab", KeyAction.Tab))

    private fun punct() = KeySpec("punct", KeyKind.KANA, kanaValue("、"), left = kanaValue("。"),
        up = kanaValue("？"), right = kanaValue("！"))

    private fun empty(width: Float = 1f) = KeySpec("empty", KeyKind.EMPTY, null, widthUnits = width)

    private fun emojiPad() = modeKey("☺", KeyboardMode.EMOJI)

    private fun emojiKey(id: String, emoji: String) = KeySpec(
        id, KeyKind.CHARACTER, FlickValue(emoji, KeyAction.CommitEmoji(emoji)),
    )

    private fun space(width: Float = 1f) = KeySpec("space", KeyKind.SPACE,
        FlickValue("Space", KeyAction.CommitText(" ")),
        left = FlickValue("←", KeyAction.MoveCursor(Direction.LEFT)), up = FlickValue("↑", KeyAction.MoveCursor(Direction.UP)),
        right = FlickValue("→", KeyAction.MoveCursor(Direction.RIGHT)), down = FlickValue("↓", KeyAction.MoveCursor(Direction.DOWN)), widthUnits = width)

    private fun enter(rowSpan: Int = 1, width: Float = 1f, conversionActive: Boolean = false) = if (conversionActive) {
        KeySpec("enter", KeyKind.ENTER, FlickValue("無変換", KeyAction.CommitWithoutConversion),
            left = FlickValue("カタカナ", KeyAction.ConvertToKatakana),
            up = FlickValue("カタカナ", KeyAction.ConvertToKatakana), widthUnits = width, rowSpan = rowSpan)
    } else KeySpec("enter", KeyKind.ENTER, FlickValue("Enter", KeyAction.Enter),
        up = FlickValue("C-j", KeyAction.ModifiedKey("j", Modifier.CTRL)),
        down = FlickValue("paste", KeyAction.Paste), widthUnits = width, rowSpan = rowSpan)

    private fun modeKey(label: String, tap: KeyboardMode) = KeySpec("mode-$label", KeyKind.MODE,
        FlickValue(label, KeyAction.SwitchLayer(tap)), dark = true)

    private fun layerKey(label: String, tap: KeyboardMode, width: Float = 1f) = KeySpec("layer-$label", KeyKind.LAYER_SWITCH,
        FlickValue(label, KeyAction.SwitchLayer(tap)),
        left = FlickValue("音声", KeyAction.VoiceHold),
        up = FlickValue(KeyboardMode.KANA.displayName, KeyAction.SwitchLayer(KeyboardMode.KANA)),
        right = FlickValue(KeyboardMode.QWERTY.displayName, KeyAction.SwitchLayer(KeyboardMode.QWERTY)),
        down = FlickValue(KeyboardMode.NUMBERS.displayName, KeyAction.SwitchLayer(KeyboardMode.NUMBERS)), widthUnits = width, dark = true)

    private fun voiceLayerKey() = KeySpec("voice-cancel", KeyKind.LAYER_SWITCH,
        FlickValue("キャンセル", KeyAction.CancelVoice),
        up = FlickValue(KeyboardMode.KANA.displayName, KeyAction.SwitchLayer(KeyboardMode.KANA)),
        right = FlickValue(KeyboardMode.QWERTY.displayName, KeyAction.SwitchLayer(KeyboardMode.QWERTY)),
        down = FlickValue(KeyboardMode.NUMBERS.displayName, KeyAction.SwitchLayer(KeyboardMode.NUMBERS)),
        widthUnits = 1f,
        dark = true,
    )

    /** The spoken-state label is drawn over this non-action sixth-sized slot of the voice bottom row. */
    private fun voiceStatusSlot() = KeySpec("voice-status", KeyKind.EMPTY, null)

    /** Voice punctuation commits immediately: it must never enter the Mozc reading buffer. */
    private fun voicePunct() = KeySpec("voice-punct", KeyKind.CHARACTER,
        value("、"), left = value("。"), up = value("？"), right = value("！"), down = value("、"))

}

/** Shared recent normalization used by the AndroidX picker provider and IME state. */
object EmojiCatalog {
    const val RECENT_LIMIT = 100
    fun visibleRecents(recents: List<String>): List<String> = recents.filter(String::isNotEmpty).distinct().take(RECENT_LIMIT)
}
