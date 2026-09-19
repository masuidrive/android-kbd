package com.masuidrive.gestureime

import android.content.ClipboardManager
import android.app.Activity
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import androidx.emoji2.emojipicker.EmojiPickerView
import androidx.emoji2.emojipicker.RecentEmojiProvider
import com.masuidrive.gestureime.conversion.ConversionEngine
import com.masuidrive.gestureime.conversion.ConversionCommit
import com.masuidrive.gestureime.conversion.ConversionCandidate
import com.masuidrive.gestureime.conversion.ConversionCandidateSource
import com.masuidrive.gestureime.conversion.ConversionState
import com.masuidrive.gestureime.conversion.PredictionContext
import com.masuidrive.gestureime.keyboard.KeyAction
import com.masuidrive.gestureime.keyboard.KeyKind
import com.masuidrive.gestureime.keyboard.KeyboardLayouts
import com.masuidrive.gestureime.keyboard.KeyboardMode
import com.masuidrive.gestureime.keyboard.KeyboardView
import com.masuidrive.gestureime.keyboard.VoiceHoldEvent
import com.masuidrive.gestureime.suggestion.EnglishSuggestionEngine
import com.masuidrive.gestureime.ui.CandidateUiLongPressEvent
import com.masuidrive.gestureime.voice.VoiceRecognitionController
import com.masuidrive.gestureime.voice.VoiceRecognizerFactory
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ImeServiceEnglishSuggestionTest {
    @Test
    fun candidateTapReplacesComposingTextWithoutDuplicatingRawPrefix() {
        val harness = Harness { prefix, _ -> if (prefix == "he") listOf("hello", "help") else emptyList() }

        harness.key("h")
        harness.key("e")
        harness.idle()
        harness.root.findView { it.contentDescription?.toString() == "候補 1: hello" }!!.performClick()
        harness.idle()

        assertEquals("hello", harness.input.visibleText)
    }

    @Test
    fun slashShowsDistinctConfiguredCandidatesAndTapReplacesTheComposingSlash() {
        val harness = Harness(
            english = { _, _ -> emptyList() },
            slashCommands = listOf("compact", "/clear", "/clear", "", "/quit", ""),
        )

        harness.key("/")
        harness.idle()

        assertEquals("/", harness.input.visibleText)
        assertEquals("", harness.input.committed)
        assertEquals(null, harness.root.findView { it.contentDescription?.toString() == "候補 4: /quit" })
        harness.root.findView { it.contentDescription?.toString() == "候補 2: /clear" }!!.performClick()
        harness.idle()

        assertEquals("/clear", harness.input.visibleText)
        assertEquals("/clear", harness.input.committed)
    }

    @Test
    fun privateEditorCommitsSlashDirectlyWithoutCandidates() {
        val harness = Harness(english = { _, _ -> emptyList() }, privateEditor = true)

        harness.key("/")
        harness.idle()

        assertEquals("/", harness.input.committed)
        assertEquals(null, harness.root.findView { it.contentDescription?.toString()?.startsWith("候補 ") == true })
    }

    @Test
    fun typingAfterSlashKeepsTheRawSlashAndInvalidatesItsCandidateTap() {
        val harness = Harness(english = { _, _ -> emptyList() })
        harness.key("/")
        harness.idle()
        val stale = harness.root.findView { it.contentDescription?.toString() == "候補 1: /compact" }

        harness.key("x")
        harness.idle()
        stale!!.performClick()
        harness.idle()

        assertEquals("/x", harness.input.visibleText)
    }

    @Test
    fun enterCommitsRawBufferWithoutAddingNewlineAndSpaceCommitsThenInsertsSpace() {
        val enter = Harness { _, _ -> emptyList() }
        enter.key("h")
        enter.key("i")
        enter.service.onKeyAction(KeyAction.Enter)
        enter.idle()
        assertEquals("hi", enter.input.visibleText)

        val space = Harness { _, _ -> emptyList() }
        space.key("h")
        space.key("i")
        space.key(" ")
        space.idle()
        assertEquals("hi ", space.input.visibleText)
    }

    @Test
    fun conversionEnterFlickCommitsRawReadingExactlyOnceAndClearsConversion() {
        val harness = Harness(conversion = FakeConversion(listOf(ConversionCandidate(1, "日本語")))) { _, _ -> emptyList() }
        harness.service.onKeyAction(KeyAction.KanaInput("にほんご"))
        harness.idle()

        harness.service.onKeyAction(KeyAction.CommitWithoutConversion)
        harness.idle()
        harness.service.onKeyAction(KeyAction.KanaInput("あ"))
        harness.idle()

        assertEquals(listOf("にほんご"), harness.input.committedValues)
        assertEquals("にほんごあ", harness.input.visibleText)
    }

    @Test
    fun conversionEnterLeftFlickCommitsFullWidthKatakanaExactlyOnce() {
        val harness = Harness(conversion = FakeConversion(listOf(ConversionCandidate(1, "日本語")))) { _, _ -> emptyList() }
        harness.service.onKeyAction(KeyAction.KanaInput("にほんご"))
        harness.idle()

        harness.service.onKeyAction(KeyAction.ConvertToKatakana)
        harness.idle()

        assertEquals(listOf("ニホンゴ"), harness.input.committedValues)
        assertEquals("ニホンゴ", harness.input.committed)
    }

    @Test
    fun spaceSelectionAloneChangesEnterToConfirmAndReadingEditsClearThatSelection() {
        val conversion = FakeConversion(
            candidateValues = listOf(
                ConversionCandidate(1, "漢字"),
                ConversionCandidate(2, "感じ"),
            ),
            initialSelectedIndex = 0,
        )
        val harness = Harness(conversion = conversion) { _, _ -> emptyList() }
        harness.service.onKeyAction(KeyAction.SwitchLayer(KeyboardMode.KANA))
        harness.service.onKeyAction(KeyAction.KanaInput("かんじ"))
        harness.idle()
        val keyboard = harness.root.findView { it is KeyboardView } as KeyboardView
        val enterId = KeyboardLayouts.layout(KeyboardMode.KANA).rows.flatMap { it.keys }
            .indexOfFirst { it.kind == KeyKind.ENTER }
        val spaceId = KeyboardLayouts.layout(KeyboardMode.KANA).rows.flatMap { it.keys }
            .indexOfFirst { it.kind == KeyKind.SPACE }
        fun description(id: Int) = keyboard.accessibilityNodeProvider
            .createAccessibilityNodeInfo(id)?.contentDescription?.toString()

        assertTrue(description(spaceId)!!.startsWith("タップ 候補"))
        assertTrue(description(enterId)!!.startsWith("タップ 無変換"))

        harness.service.onKeyAction(KeyAction.CycleCandidate)
        harness.idle()
        assertTrue(description(enterId)!!.startsWith("タップ 確定"))
        harness.service.onKeyAction(KeyAction.CycleCandidate)
        harness.idle()
        harness.service.onKeyAction(KeyAction.CommitConversion)
        harness.idle()
        assertEquals(listOf(1), conversion.committedIndexes)
        assertEquals("感じ", harness.input.committed)

        harness.service.onKeyAction(KeyAction.KanaInput("か"))
        harness.idle()
        harness.service.onKeyAction(KeyAction.CycleCandidate)
        harness.idle()
        assertTrue(description(enterId)!!.startsWith("タップ 確定"))
        harness.service.onKeyAction(KeyAction.KanaInput("な"))
        harness.idle()
        assertTrue(description(enterId)!!.startsWith("タップ 無変換"))

        harness.service.onKeyAction(KeyAction.CycleCandidate)
        harness.idle()
        harness.service.onKeyAction(KeyAction.Backspace())
        harness.idle()
        assertTrue(description(enterId)!!.startsWith("タップ 無変換"))
    }

    @Test
    fun japaneseCommitShowsPredictionAndPredictionTapAppendsThenRefreshes() {
        val conversion = FakeConversion(
            candidateValues = listOf(ConversionCandidate(1, "日本語")),
            prediction = { context ->
                if (context.precedingText.endsWith("日本語")) listOf(ConversionCandidate(2, "を"))
                else listOf(ConversionCandidate(3, "入力"))
            },
        )
        val harness = Harness(conversion = conversion) { _, _ -> emptyList() }

        harness.service.onKeyAction(KeyAction.KanaInput("にほんご"))
        harness.idle()
        harness.root.findView { it.contentDescription?.toString() == "候補 1: 日本語" }!!.performClick()
        harness.idle()
        harness.root.findView { it.contentDescription?.toString() == "候補 1: を" }!!.performClick()
        harness.idle()
        harness.service.onUpdateSelection(3, 3, 4, 4, -1, -1)
        harness.idle()

        assertEquals("日本語を", harness.input.committed)
        assertEquals(listOf("日本語", "を"), harness.input.committedValues)
        assertEquals(listOf("日本語", "日本語を"), conversion.predictionContexts.map { it.precedingText })
        assertEquals("候補 1: 入力", harness.root.findView { it.contentDescription?.toString() == "候補 1: 入力" }?.contentDescription)
    }

    @Test
    fun predictionIsClearedByKanaInputAndNeverRequestedForPrivateEditor() {
        val conversion = FakeConversion(
            candidateValues = listOf(ConversionCandidate(1, "日本語")),
            prediction = { listOf(ConversionCandidate(2, "を")) },
        )
        val harness = Harness(conversion = conversion) { _, _ -> emptyList() }
        harness.service.onKeyAction(KeyAction.KanaInput("にほんご"))
        harness.idle()
        harness.root.findView { it.contentDescription?.toString() == "候補 1: 日本語" }!!.performClick()
        harness.idle()
        val stalePrediction = harness.root.findView { it.contentDescription?.toString() == "候補 1: を" }!!

        harness.service.onKeyAction(KeyAction.KanaInput("か"))
        harness.idle()
        stalePrediction.performClick()
        harness.idle()

        assertEquals("日本語か", harness.input.visibleText)

        val privateConversion = FakeConversion(listOf(ConversionCandidate(1, "日本語"))) { listOf(ConversionCandidate(2, "を")) }
        val privateHarness = Harness(privateEditor = true, conversion = privateConversion) { _, _ -> emptyList() }
        privateHarness.service.onKeyAction(KeyAction.KanaInput("にほんご"))
        privateHarness.idle()
        assertEquals("にほんご", privateHarness.input.committed)
        assertEquals(emptyList<PredictionContext>(), privateConversion.predictionContexts)
    }

    @Test
    fun selectionChangeInvalidatesPredictionTap() {
        val conversion = FakeConversion(
            candidateValues = listOf(ConversionCandidate(1, "日本語")),
            prediction = { listOf(ConversionCandidate(2, "を")) },
        )
        val harness = Harness(conversion = conversion) { _, _ -> emptyList() }
        harness.service.onKeyAction(KeyAction.KanaInput("にほんご"))
        harness.idle()
        harness.root.findView { it.contentDescription?.toString() == "候補 1: 日本語" }!!.performClick()
        harness.idle()
        val stalePrediction = harness.root.findView { it.contentDescription?.toString() == "候補 1: を" }!!

        harness.service.onUpdateSelection(3, 3, 0, 0, -1, -1)
        harness.idle()
        stalePrediction.performClick()
        harness.idle()

        assertEquals("日本語", harness.input.committed)
    }

    @Test
    fun compositionReplacementSelectionKeepsPredictionAndPredictionHistoryCanBeDeleted() {
        val conversion = FakeConversion(
            candidateValues = listOf(ConversionCandidate(1, "日本語")),
            prediction = { listOf(ConversionCandidate(2, "を")) },
        )
        val harness = Harness(conversion = conversion) { _, _ -> emptyList() }
        harness.service.onKeyAction(KeyAction.KanaInput("にほんご"))
        harness.idle()
        harness.root.findView { it.contentDescription?.toString() == "候補 1: 日本語" }!!.performClick()
        harness.idle()

        harness.service.onUpdateSelection(4, 4, 3, 3, -1, -1)
        harness.idle()
        val prediction = harness.root.findView { it.contentDescription?.toString() == "候補 1: を" }!!
        assertEquals(true, prediction.performLongClick())
        harness.idle()

        assertEquals(listOf(0), conversion.deletedIndexes)
    }

    @Test
    fun sameLengthHiraganaAndKatakanaCommitSelectionKeepsPrediction() {
        listOf(KeyAction.CommitWithoutConversion, KeyAction.ConvertToKatakana).forEach { action ->
            val conversion = FakeConversion(
                candidateValues = listOf(ConversionCandidate(1, "日本語")),
                prediction = { listOf(ConversionCandidate(2, "を")) },
            )
            val harness = Harness(conversion = conversion) { _, _ -> emptyList() }
            harness.service.onKeyAction(KeyAction.KanaInput("にほんご"))
            harness.idle()

            harness.service.onKeyAction(action)
            harness.idle()
            harness.service.onUpdateSelection(4, 4, 4, 4, -1, -1)
            harness.idle()

            assertEquals("候補 1: を", harness.root.findView { it.contentDescription?.toString() == "候補 1: を" }?.contentDescription)
        }
    }

    @Test
    fun expectedSelectionBeforePredictionCompletesKeepsItsResult() {
        val pending = CompletableDeferred<List<ConversionCandidate>>()
        val conversion = FakeConversion(
            candidateValues = listOf(ConversionCandidate(1, "日本語")),
            prediction = { pending.await() },
        )
        val harness = Harness(conversion = conversion) { _, _ -> emptyList() }
        harness.service.onKeyAction(KeyAction.KanaInput("にほんご"))
        harness.idle()
        harness.root.findView { it.contentDescription?.toString() == "候補 1: 日本語" }!!.performClick()
        harness.idle()

        harness.service.onUpdateSelection(4, 4, 3, 3, -1, -1)
        pending.complete(listOf(ConversionCandidate(2, "を")))
        harness.idle()

        assertEquals("候補 1: を", harness.root.findView { it.contentDescription?.toString() == "候補 1: を" }?.contentDescription)
    }

    @Test
    fun externalSelectionDuringPredictionLookupDiscardsTheDelayedResult() {
        val pending = CompletableDeferred<List<ConversionCandidate>>()
        val conversion = FakeConversion(
            candidateValues = listOf(ConversionCandidate(1, "日本語")),
            prediction = { pending.await() },
        )
        val harness = Harness(conversion = conversion) { _, _ -> emptyList() }
        harness.service.onKeyAction(KeyAction.KanaInput("にほんご"))
        harness.idle()
        harness.root.findView { it.contentDescription?.toString() == "候補 1: 日本語" }!!.performClick()
        harness.idle()

        harness.input.setSelection(0, 0)
        harness.service.onUpdateSelection(3, 3, 0, 0, -1, -1)
        pending.complete(listOf(ConversionCandidate(2, "を")))
        harness.idle()

        assertEquals(null, harness.root.findView { it.contentDescription?.toString() == "候補 1: を" })
        assertEquals("日本語", harness.input.committed)
    }

    @Test
    fun staleCandidateTapQueuedBehindNewCharacterDoesNotReplaceNewerBuffer() {
        val harness = Harness { prefix, _ -> listOf(if (prefix == "h") "hello" else "help") }
        harness.key("h")
        harness.idle()
        val staleCandidate = harness.root.findView { it.contentDescription?.toString() == "候補 1: hello" }

        harness.key("e")
        harness.idle()
        staleCandidate!!.performClick()
        harness.idle()

        assertEquals("he", harness.input.visibleText)
        assertEquals("", harness.input.committed)
    }

    @Test
    fun lateLookupAfterEditorSwitchAndPrivateEditorNeverCreatesCandidateState() {
        val pending = CompletableDeferred<List<String>>()
        val harness = Harness { _, _ -> pending.await() }
        harness.key("h")
        harness.service.onStartInput(EditorInfo(), false)
        pending.complete(listOf("hello"))
        harness.idle()
        harness.service.onKeyAction(KeyAction.SelectCandidate(0))
        harness.idle()
        assertEquals("", harness.input.committed)

        harness.service.onStartInput(EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }, false)
        harness.key("x")
        harness.idle()
        assertEquals("x", harness.input.committed)
    }

    @Test
    fun backspaceRefreshesBufferAndLayerSwitchFlushesRawBeforeChangingMode() {
        val harness = Harness { prefix, _ -> if (prefix == "he") listOf("hello") else emptyList() }
        harness.key("h")
        harness.key("e")
        harness.key("x")
        harness.service.onKeyAction(KeyAction.Backspace())
        harness.idle()
        assertEquals("he", harness.input.visibleText)

        harness.service.onKeyAction(KeyAction.SwitchLayer(KeyboardMode.KANA))
        harness.idle()
        assertEquals("he", harness.input.committed)
        assertEquals(KeyboardMode.KANA, ImePreferences.getLastKeyboardMode(harness.service))
    }

    @Test
    fun kanaAndNumberModeBoundariesKeepCompositionsSeparate() {
        val harness = Harness { _, _ -> emptyList() }
        harness.service.onKeyAction(KeyAction.KanaInput("か"))
        harness.key("a")
        harness.idle()
        assertEquals("かa", harness.input.visibleText)
        assertEquals("か", harness.input.committed)

        harness.service.onKeyAction(KeyAction.SwitchLayer(KeyboardMode.NUMBERS))
        harness.key("1")
        harness.service.onKeyAction(KeyAction.Enter)
        harness.idle()
        assertEquals("かa1", harness.input.committed)
    }

    @Test
    fun emojiCommitFlushesJapaneseEnglishAndSlashCompositionThenUpdatesRecentOnlyAfterSuccessfulCommit() {
        val japanese = Harness { _, _ -> emptyList() }
        japanese.clearEmojiRecents()
        japanese.service.onKeyAction(KeyAction.KanaInput("か"))
        japanese.idle()
        assertEquals("", japanese.input.committed)
        japanese.service.onKeyAction(KeyAction.CommitEmoji("❤️"))
        japanese.idle()
        assertEquals("か❤️", japanese.input.visibleText)
        assertEquals("か❤️", japanese.input.committed)
        assertEquals(listOf("❤️"), japanese.input.committedValues)
        assertEquals(listOf("❤️"), ImePreferences.getEmojiRecents(japanese.service))

        val harness = Harness { _, _ -> emptyList() }
        harness.clearEmojiRecents()
        harness.service.onKeyAction(KeyAction.KanaInput("か"))
        harness.idle()
        harness.key("a")
        harness.idle()
        val publicPicker = harness.root.findView { it is EmojiPickerView } as EmojiPickerView
        val recentProvider = EmojiPickerView::class.java.getDeclaredField("recentEmojiProvider").apply {
            isAccessible = true
        }
        val providerBeforeCommit = recentProvider.get(publicPicker) as RecentEmojiProvider
        harness.service.onKeyAction(KeyAction.SwitchLayer(KeyboardMode.EMOJI))
        harness.service.onKeyAction(KeyAction.CommitEmoji("❤️"))
        harness.idle()

        assertEquals("かa❤️", harness.input.visibleText)
        assertEquals(listOf("❤️"), ImePreferences.getEmojiRecents(harness.service))
        val providerAfterCommit = recentProvider.get(publicPicker) as RecentEmojiProvider
        assertNotSame(providerBeforeCommit, providerAfterCommit)
        assertEquals(listOf("❤️"), runBlocking { providerAfterCommit.getRecentEmojiList() })
        val keyboard = harness.root.findView { it is KeyboardView } as KeyboardView
        val emojiAzVirtualId = KeyboardLayouts.layout(KeyboardMode.EMOJI).rows
            .flatMap { it.keys }
            .indexOfFirst { it.center?.label == "AZ" }
        assertEquals("タップ AZ、上 日本語、右 QWERTY、下 テンキー", keyboard.accessibilityNodeProvider.createAccessibilityNodeInfo(emojiAzVirtualId)?.contentDescription)

        val slash = Harness(english = { _, _ -> emptyList() })
        slash.clearEmojiRecents()
        slash.key("/")
        slash.idle()
        slash.service.onKeyAction(KeyAction.CommitEmoji("😀"))
        slash.idle()
        assertEquals("/😀", slash.input.visibleText)
        assertEquals(listOf("😀"), ImePreferences.getEmojiRecents(slash.service))

        val rejected = Harness(commitAccepted = false) { _, _ -> emptyList() }
        rejected.clearEmojiRecents()
        rejected.service.onKeyAction(KeyAction.CommitEmoji("😀"))
        rejected.idle()
        assertEquals("", rejected.input.visibleText)
        assertEquals(emptyList<String>(), ImePreferences.getEmojiRecents(rejected.service))
    }

    @Test
    fun privateEmojiCommitDoesNotPersistRecentHistory() {
        val harness = Harness(privateEditor = true) { _, _ -> emptyList() }
        harness.clearEmojiRecents()

        harness.service.onKeyAction(KeyAction.CommitEmoji("👋"))
        harness.idle()

        assertEquals("👋", harness.input.visibleText)
        assertEquals(emptyList<String>(), ImePreferences.getEmojiRecents(harness.service))
    }

    @Test
    fun emojiQueuedForAnOldEditorDoesNotCommitOrUpdateRecent() {
        val pendingStart = CompletableDeferred<Unit>()
        val harness = Harness(conversion = FakeConversion(startGate = pendingStart)) { _, _ -> emptyList() }
        harness.clearEmojiRecents()

        harness.service.onKeyAction(KeyAction.KanaInput("か"))
        harness.idle()
        harness.service.onKeyAction(KeyAction.CommitEmoji("😀"))
        harness.service.onStartInput(EditorInfo(), false)
        pendingStart.complete(Unit)
        harness.idle()

        assertEquals("か", harness.input.visibleText)
        assertEquals(emptyList<String>(), ImePreferences.getEmojiRecents(harness.service))
    }

    @Test
    fun emojiEditorSwitchDuringConversionResetDoesNotCommitOrUpdateRecent() {
        val pendingReset = CompletableDeferred<Unit>()
        val harness = Harness(conversion = FakeConversion(resetGate = pendingReset)) { _, _ -> emptyList() }
        harness.clearEmojiRecents()

        harness.service.onKeyAction(KeyAction.CommitEmoji("😀"))
        harness.idle()
        harness.service.onStartInput(EditorInfo(), false)
        pendingReset.complete(Unit)
        harness.idle()

        assertEquals("", harness.input.visibleText)
        assertEquals(emptyList<String>(), ImePreferences.getEmojiRecents(harness.service))
    }

    @Test
    fun externalSelectionMoveInvalidatesCandidateAndNextTextStartsAtNewSelection() {
        val harness = Harness { _, _ -> listOf("hello") }
        harness.key("h")
        harness.key("e")
        harness.idle()
        val staleCandidate = harness.root.findView { it.contentDescription?.toString() == "候補 1: hello" }

        harness.input.setSelection(0, 0)
        harness.service.onUpdateSelection(2, 2, 0, 0, 0, 2)
        harness.key("x")
        staleCandidate!!.performClick()
        harness.idle()

        assertEquals("xhe", harness.input.visibleText)
    }

    @Test
    fun voiceHoldBeginFlushesRawEnglishBeforeStartingVoiceSession() {
        val harness = Harness { _, _ -> emptyList() }
        harness.key("h")
        harness.key("i")
        harness.idle()

        harness.service.onVoiceHold(VoiceHoldEvent.Begin(41L))
        harness.idle()

        assertEquals("hi", harness.input.committed)
    }

    @Test
    fun sixtyFifthAsciiCharacterStartsANewBufferSoEnterDoesNotInsertNewline() {
        val harness = Harness { _, _ -> emptyList() }
        repeat(65) { harness.key("a") }
        harness.service.onKeyAction(KeyAction.Enter)
        harness.idle()

        assertEquals("a".repeat(65), harness.input.committed)
    }

    @Test
    fun onlyMozcCandidateLongPressDeletesHistoryAndStaleViewsCannotDeleteIt() {
        val conversion = FakeConversion(
            listOf(
                ConversionCandidate(10, "履歴候補"),
                ConversionCandidate(-1, "個人辞書候補", ConversionCandidateSource.ANDROID_USER_DICTIONARY),
            ),
        )
        val harness = Harness(conversion = conversion) { _, _ -> emptyList() }

        harness.service.onKeyAction(KeyAction.KanaInput("か"))
        harness.idle()
        val history = harness.root.findView { it.contentDescription?.toString() == "候補 1: 履歴候補" }!!
        val dictionary = harness.root.findView { it.contentDescription?.toString() == "候補 2: 個人辞書候補" }!!

        assertEquals(false, dictionary.performLongClick())
        assertEquals(emptyList<Int>(), conversion.deletedIndexes)
        assertEquals(true, history.performLongClick())
        harness.idle()
        assertEquals(listOf(0), conversion.deletedIndexes)

        harness.service.onKeyAction(KeyAction.KanaInput("き"))
        harness.idle()
        assertEquals(
            false,
            isEligibleHistoryLongPress(
                CandidateUiLongPressEvent(1, 0),
                currentToken = 2,
                isJapaneseCandidateSource = true,
                conversionCandidates = listOf(ConversionCandidate(10, "履歴候補")),
            ),
        )
        assertEquals(listOf(0), conversion.deletedIndexes)
    }

    private class Harness(
        slashCommands: List<String>? = null,
        privateEditor: Boolean = false,
        commitAccepted: Boolean = true,
        conversion: FakeConversion = FakeConversion(),
        english: suspend (String, Int) -> List<String>,
    ) {
        private val controller = Robolectric.buildService(ImeService::class.java).create()
        val service = controller.get()
        val input = RecordingConnection(View(RuntimeEnvironment.getApplication()), commitAccepted)
        lateinit var root: View

        init {
            ImePreferences.setEnglishSuggestionsEnabled(service, true)
            ImePreferences.setSlashCommands(service, slashCommands ?: ImePreferences.DEFAULT_SLASH_COMMANDS)
            clearEmojiRecents()
            val text = TextInputController(
                connection = { input },
                context = service,
                clipboard = service.getSystemService(ClipboardManager::class.java),
            )
            service.installTestDependencies(
                voice = VoiceRecognitionController(
                    sdkInt = 30,
                    hasPermission = { false },
                    onDeviceAvailable = { false },
                    factory = VoiceRecognizerFactory { error("voice is not used") },
                    onState = service::onVoiceState,
                ),
                text = text,
                conversion = conversion,
                english = object : EnglishSuggestionEngine {
                    override suspend fun suggest(prefix: String, limit: Int) = english(prefix, limit)
                },
            )
            service.onStartInput(EditorInfo().apply {
                if (privateEditor) inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }, false)
            root = service.onCreateInputView()
            Robolectric.buildActivity(Activity::class.java).setup().get().setContentView(root)
            idle()
        }

        fun key(text: String) {
            service.onKeyAction(KeyAction.CommitText(text))
        }

        fun clearEmojiRecents() {
            service.getSharedPreferences("gesture_ime_preferences", 0).edit().apply {
                repeat(ImePreferences.EMOJI_RECENT_LIMIT) { remove("emoji_recent_$it") }
            }.commit()
        }

        fun idle() = Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
    }

    private fun View.findView(predicate: (View) -> Boolean): View? {
        if (predicate(this)) return this
        if (this !is ViewGroup) return null
        for (index in 0 until childCount) getChildAt(index).findView(predicate)?.let { return it }
        return null
    }

    private class RecordingConnection(view: View, private val commitAccepted: Boolean = true) : BaseInputConnection(view, true) {
        private val text = StringBuilder()
        private var composingStart = -1
        private var composingEnd = -1
        private var selectionStart = 0
        private var selectionEnd = 0
        val visibleText get() = text.toString()
        val committedValues = mutableListOf<String>()
        val committed: String get() = if (composingStart < 0) text.toString() else
            text.substring(0, composingStart) + text.substring(composingEnd)

        override fun setComposingText(text: CharSequence?, newCursorPosition: Int): Boolean {
            val start = if (composingStart >= 0) composingStart else minOf(selectionStart, selectionEnd)
            val end = if (composingEnd >= 0) composingEnd else maxOf(selectionStart, selectionEnd)
            val replacement = text?.toString().orEmpty()
            this.text.replace(start, end, replacement)
            composingStart = start
            composingEnd = start + replacement.length
            selectionStart = composingEnd
            selectionEnd = composingEnd
            return true
        }

        override fun finishComposingText(): Boolean {
            composingStart = -1
            composingEnd = -1
            return true
        }

        override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
            if (!commitAccepted) return false
            val start = if (composingStart >= 0) composingStart else minOf(selectionStart, selectionEnd)
            val end = if (composingEnd >= 0) composingEnd else maxOf(selectionStart, selectionEnd)
            val replacement = text?.toString().orEmpty()
            committedValues += replacement
            this.text.replace(start, end, replacement)
            selectionStart = start + replacement.length
            selectionEnd = selectionStart
            composingStart = -1
            composingEnd = -1
            return true
        }

        override fun setSelection(start: Int, end: Int): Boolean {
            selectionStart = start.coerceIn(0, text.length)
            selectionEnd = end.coerceIn(0, text.length)
            return true
        }

        override fun getTextBeforeCursor(length: Int, flags: Int): CharSequence =
            text.substring((selectionStart - length).coerceAtLeast(0), selectionStart)

        override fun getTextAfterCursor(length: Int, flags: Int): CharSequence =
            text.substring(selectionEnd, (selectionEnd + length).coerceAtMost(text.length))
    }

    private class FakeConversion(
        private val candidateValues: List<ConversionCandidate> = emptyList(),
        private val startGate: CompletableDeferred<Unit>? = null,
        private val resetGate: CompletableDeferred<Unit>? = null,
        private val initialSelectedIndex: Int = -1,
        private val prediction: suspend (PredictionContext) -> List<ConversionCandidate> = { emptyList() },
    ) : ConversionEngine {
        val deletedIndexes = mutableListOf<Int>()
        val committedIndexes = mutableListOf<Int>()
        val predictionContexts = mutableListOf<PredictionContext>()
        private var predictionActive = false
        private var reading = ""
        private var nextIndex = -1
        override suspend fun start(reading: String): ConversionState {
            startGate?.await()
            predictionActive = false
            this.reading = reading
            nextIndex = -1
            return ConversionState(reading, candidateValues, initialSelectedIndex)
        }
        override suspend fun update(reading: String) = start(reading)
        override suspend fun nextCandidate(): ConversionState {
            predictionActive = false
            nextIndex = if (candidateValues.isEmpty()) -1 else (nextIndex + 1) % candidateValues.size
            return ConversionState(reading, candidateValues, nextIndex)
        }
        override suspend fun predict(context: PredictionContext): ConversionState {
            predictionActive = true
            predictionContexts += context
            return ConversionState("", prediction(context), -1)
        }
        override suspend fun commit(index: Int): ConversionCommit? {
            if (!predictionActive) committedIndexes += index
            return (if (predictionActive) prediction(predictionContexts.last()) else candidateValues)
                .getOrNull(index)
                ?.let { ConversionCommit(it.value) }
        }
        override suspend fun deleteCandidateFromHistory(index: Int): ConversionState? {
            deletedIndexes += index
            return ConversionState("か", candidateValues, -1)
        }
        override suspend fun reset() { resetGate?.await() }
    }
}
