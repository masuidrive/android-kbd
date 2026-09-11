package com.masuidrive.gestureime

import android.content.ClipboardManager
import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import android.widget.Toast
import com.masuidrive.gestureime.conversion.ConversionEngine
import com.masuidrive.gestureime.conversion.ConversionState
import com.masuidrive.gestureime.conversion.MozcConversionEngine
import com.masuidrive.gestureime.keyboard.KeyAction
import com.masuidrive.gestureime.keyboard.KeyboardActionSink
import com.masuidrive.gestureime.keyboard.KeyboardMode
import com.masuidrive.gestureime.keyboard.KeyboardView
import com.masuidrive.gestureime.keyboard.VoiceHoldEvent
import com.masuidrive.gestureime.keyboard.VoiceHoldSink
import com.masuidrive.gestureime.suggestion.BundledEnglishSuggestionEngine
import com.masuidrive.gestureime.suggestion.EnglishSuggestionEngine
import com.masuidrive.gestureime.ui.CandidateUiEvent
import com.masuidrive.gestureime.ui.CandidateUiSnapshot
import com.masuidrive.gestureime.ui.CandidateStripView
import com.masuidrive.gestureime.ui.VoiceUiAction
import com.masuidrive.gestureime.ui.VoiceUiEvent
import com.masuidrive.gestureime.ui.VoiceUiSnapshot
import com.masuidrive.gestureime.ui.VoiceUiState
import com.masuidrive.gestureime.voice.VoiceBackendState
import com.masuidrive.gestureime.voice.VoiceRecognitionController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ImeService : InputMethodService(), KeyboardActionSink, VoiceHoldSink {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val actionMutex = Mutex()
    private val editorSession = EditorSessionGate()
    private val productionConversionEngine: ConversionEngine by lazy { MozcConversionEngine(applicationContext) }
    private var testConversionEngine: ConversionEngine? = null
    private val conversionEngine: ConversionEngine get() = testConversionEngine ?: productionConversionEngine
    private val productionEnglishSuggestionEngine: EnglishSuggestionEngine by lazy {
        BundledEnglishSuggestionEngine(applicationContext)
    }
    private var testEnglishSuggestionEngine: EnglishSuggestionEngine? = null
    private val englishSuggestionEngine: EnglishSuggestionEngine
        get() = testEnglishSuggestionEngine ?: productionEnglishSuggestionEngine
    private lateinit var textController: TextInputController
    private lateinit var voiceController: VoiceRecognitionController
    private var keyboardView: KeyboardView? = null
    private var candidateStrip: CandidateStripView? = null
    private var conversionGeneration = 0L
    private var reading = ""
    private var candidates = emptyList<String>()
    private var selectedCandidate = -1
    private var conversionPreview: String? = null
    private var candidateSource = CandidateSource.NONE
    private var englishBuffer = ""
    private var englishGeneration = 0L
    private var slashBufferActive = false
    private var slashGeneration = 0L
    private var keyboardMode = KeyboardMode.QWERTY
    private var candidateUiToken = 0L
    private var voiceUiToken = 0L
    private var latestVoiceUnavailableMessage: String? = null
    private var voiceHoldRequestId: Long? = null
    private var voiceHoldEditorToken = 0L
    private var voiceHoldReady = false
    private var voiceHoldReleased = false
    private var voiceHoldGeneration = 0L

    override fun onCreate() {
        super.onCreate()
        textController = TextInputController(
            connection = { currentInputConnection },
            context = applicationContext,
            clipboard = getSystemService(ClipboardManager::class.java),
        )
        voiceController = VoiceRecognitionController(applicationContext, ::onVoiceState)
    }

    override fun onCreateInputView(): View {
        cancelVoiceHold()
        val candidateHeight = (50 * resources.displayMetrics.density).toInt()
        val keyboard = KeyboardView(this).also {
            it.actionSink = this
            it.voiceHoldSink = this
            keyboardMode = ImePreferences.getLastKeyboardMode(this)
            it.setMode(keyboardMode)
            it.setDualFlickEnabled(ImePreferences.isDualFlickEnabled(this))
            it.setQwertyLabelStyle(ImePreferences.getQwertyLabelStyle(this))
            keyboardView = it
        }
        val strip = CandidateStripView(this).also {
            it.setOnCandidateSelected(::onCandidateSelected)
            it.setOnVoiceActionListener(::onVoiceAction)
            it.visibility = if (textController.isPrivateField) View.INVISIBLE else View.VISIBLE
            candidateStrip = it
            setVoiceUi(if (textController.isPrivateField) VoiceUiState.Hidden else voiceController.initialState().toUiState())
        }
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(strip, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, candidateHeight))
            addView(keyboard, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        cancelVoiceHold()
        finishEnglishRaw()
        finishSlashRaw()
        setVoiceUi(VoiceUiState.Hidden)
        keyboardView?.cancelActiveGestures()
        super.onFinishInputView(finishingInput)
    }

    override fun onStartInputView(info: EditorInfo, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        cancelVoiceHold()
        keyboardView?.setQwertyLabelStyle(ImePreferences.getQwertyLabelStyle(this))
        setVoiceUi(if (textController.isPrivateField) VoiceUiState.Hidden else voiceController.initialState().toUiState())
    }

    override fun onStartInput(attribute: EditorInfo, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        editorSession.advance()
        cancelVoiceHold()
        invalidateConversion(clearComposing = false)
        invalidateEnglish(clearComposing = false)
        invalidateSlash(clearComposing = false)
        textController.beginInput(attribute)
        textController.terminalCursorEnabled = ImePreferences.isTerminalCursorEnabled(this)
        candidateStrip?.visibility = if (textController.isPrivateField) View.INVISIBLE else View.VISIBLE
        setVoiceUi(if (textController.isPrivateField) VoiceUiState.Hidden else voiceController.initialState().toUiState())
        keyboardMode = ImePreferences.getLastKeyboardMode(this)
        keyboardView?.setMode(keyboardMode)
        keyboardView?.setDualFlickEnabled(ImePreferences.isDualFlickEnabled(this))
        keyboardView?.setQwertyLabelStyle(ImePreferences.getQwertyLabelStyle(this))
    }

    override fun onFinishInput() {
        cancelVoiceHold()
        finishEnglishRaw()
        finishSlashRaw()
        editorSession.advance()
        invalidateConversion(clearComposing = false)
        keyboardView?.cancelActiveGestures()
        textController.finishComposition()
        super.onFinishInput()
    }

    override fun onUpdateSelection(
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
        candidatesStart: Int,
        candidatesEnd: Int,
    ) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        if (reading.isNotEmpty() && (candidatesStart < 0 || newSelStart !in candidatesStart..candidatesEnd)) {
            invalidateConversion(clearComposing = false)
            textController.abandonComposition()
        }
        if (englishBuffer.isNotEmpty() && (
                candidatesStart < 0 || newSelStart != newSelEnd || newSelEnd != candidatesEnd
            )
        ) {
            textController.finishComposition()
            invalidateEnglish(clearComposing = false)
        }
        if (slashBufferActive && (candidatesStart < 0 || newSelStart != newSelEnd || newSelEnd != candidatesEnd)) {
            textController.finishComposition()
            invalidateSlash(clearComposing = false)
        }
    }

    override fun onKeyAction(action: KeyAction) {
        cancelVoiceHold()
        if (!textController.isPrivateField) setVoiceUi(voiceController.initialState().toUiState())
        val queuedForEditor = editorSession.capture()
        val queuedCandidateSnapshot = if (action is KeyAction.SelectCandidate) candidateSnapshot() else null
        serviceScope.launch {
            actionMutex.withLock {
                if (!editorSession.isCurrent(queuedForEditor)) return@withLock
                if (queuedCandidateSnapshot != null && queuedCandidateSnapshot != candidateSnapshot()) return@withLock
                runCatching { processInputAction(action, queuedForEditor) }
                    .onFailure {
                        clearCandidateState()
                        candidateStrip?.showStatus("変換を利用できません")
                    }
            }
        }
    }

    private fun onCandidateSelected(event: CandidateUiEvent) {
        if (event.token != candidateUiToken) return
        onKeyAction(KeyAction.SelectCandidate(event.index))
    }

    override fun onVoiceHold(event: VoiceHoldEvent) {
        when (event) {
            is VoiceHoldEvent.Begin -> {
                cancelVoiceHold()
                if (textController.isPrivateField) return
                val token = editorSession.capture()
                voiceHoldRequestId = event.requestId
                voiceHoldEditorToken = token
                serviceScope.launch {
                    actionMutex.withLock {
                        if (voiceHoldRequestId != event.requestId || !editorSession.isCurrent(token)) return@withLock
                        finishEnglishRaw()
                        finishSlashRaw()
                        resetConversion(clearComposing = false)
                        if (voiceHoldRequestId == event.requestId && editorSession.isCurrent(token)) voiceController.start(token)
                    }
                }
            }
            is VoiceHoldEvent.End -> {
                if (voiceHoldRequestId != event.requestId) return
                voiceHoldReleased = true
                val text = voiceController.confirm(voiceHoldEditorToken)
                if (text != null) commitVoiceHold(event.requestId, voiceHoldEditorToken, text)
                else if (voiceHoldReady) voiceController.stop() else cancelVoiceHoldAndResetUi()
            }
            is VoiceHoldEvent.Cancel -> if (voiceHoldRequestId == event.requestId) cancelVoiceHoldAndResetUi()
        }
    }

    private fun cancelVoiceHold() {
        voiceHoldGeneration++
        voiceHoldRequestId = null
        voiceHoldReady = false
        voiceHoldReleased = false
        voiceController.cancel(notify = false)
    }

    private fun cancelVoiceHoldAndResetUi() {
        cancelVoiceHold()
        if (::textController.isInitialized && !textController.isPrivateField) {
            setVoiceUi(voiceController.initialState().toUiState())
        }
    }

    private suspend fun processInputAction(action: KeyAction, editorToken: Long) {
        if (slashBufferActive && action !is KeyAction.SelectCandidate && action !is KeyAction.Backspace) {
            finishSlashRaw()
        }
        when (action) {
            is KeyAction.CommitText -> {
                if (shouldStartSlashCandidates(action.text)) startSlashCandidates()
                else if (shouldBufferEnglish(action.text)) appendEnglish(action.text, editorToken)
                else if (action.text == " " && reading.isNotEmpty()) cycleCandidate()
                else {
                    finishEnglishRaw()
                    resetConversion(clearComposing = false)
                    editorSession.runIfCurrent(editorToken) { textController.commitText(action.text) }
                }
            }
            is KeyAction.KanaInput -> {
                finishEnglishRaw()
                restoreReadingPreview()
                updateReading(textController.appendComposing(action.reading))
            }
            is KeyAction.TransformKana -> {
                finishEnglishRaw()
                restoreReadingPreview()
                updateReading(textController.transformKana(action.transform))
            }
            is KeyAction.Backspace -> {
                if (slashBufferActive) {
                    textController.backspace()
                    invalidateSlash(clearComposing = false)
                    return
                }
                if (englishBuffer.isNotEmpty()) {
                    val remaining = textController.backspace()
                    englishBuffer = remaining
                    if (remaining.isEmpty()) invalidateEnglish(clearComposing = false)
                    else requestEnglishSuggestions(remaining, editorToken)
                    return
                }
                restoreReadingPreview()
                val remaining = textController.backspace()
                if (reading.isNotEmpty()) updateReading(remaining)
            }
            KeyAction.Enter -> {
                if (englishBuffer.isNotEmpty()) {
                    finishEnglishRaw()
                } else if (reading.isNotEmpty()) {
                    if (candidates.isEmpty()) {
                        val generation = ++conversionGeneration
                        val state = conversionEngine.nextCandidate()
                        if (generation != conversionGeneration || !editorSession.isCurrent(editorToken)) return
                        applyConversion(state)
                    }
                    if (candidates.isNotEmpty()) commitCandidate(selectedCandidate.coerceAtLeast(0))
                    else {
                        if (!editorSession.runIfCurrent(editorToken) { textController.commitCandidate(reading) }) return
                        resetConversion(clearComposing = false)
                    }
                } else textController.enter()
            }
            KeyAction.Paste -> {
                finishEnglishRaw()
                resetConversion(clearComposing = false)
                editorSession.runIfCurrent(editorToken) { textController.paste() }
            }
            KeyAction.Escape -> {
                finishEnglishRaw()
                resetConversion(clearComposing = false)
                editorSession.runIfCurrent(editorToken) { textController.escape() }
            }
            KeyAction.CommitConversion -> {
                finishEnglishRaw()
                commitDisplayedConversion()
            }
            KeyAction.CommitWithoutConversion -> {
                finishEnglishRaw()
                showConversionPreview(reading)
            }
            KeyAction.ConvertToKatakana -> {
                finishEnglishRaw()
                showConversionPreview(reading.toKatakana())
            }
            is KeyAction.MoveCursor -> {
                finishEnglishRaw()
                resetConversion(clearComposing = false)
                editorSession.runIfCurrent(editorToken) { textController.moveCursor(action.direction, action.units) }
            }
            is KeyAction.MoveToBoundary -> {
                finishEnglishRaw()
                resetConversion(clearComposing = false)
                editorSession.runIfCurrent(editorToken) { textController.moveToBoundary(action.boundary) }
            }
            is KeyAction.ModifiedKey -> {
                finishEnglishRaw()
                resetConversion(clearComposing = false)
                editorSession.runIfCurrent(editorToken) {
                    textController.sendModifiedKey(action.label, action.modifier)
                }
            }
            is KeyAction.SwitchLayer -> {
                finishEnglishRaw()
                ImePreferences.setLastKeyboardMode(this, action.target)
                keyboardMode = action.target
                keyboardView?.setMode(action.target)
            }
            is KeyAction.SelectCandidate -> {
                if (candidateSource == CandidateSource.ENGLISH) commitEnglishCandidate(action.index)
                else if (candidateSource == CandidateSource.SLASH) commitSlashCandidate(action.index)
                else commitCandidate(action.index)
            }
            KeyAction.CycleCandidate -> cycleCandidate()
            is KeyAction.SetModifier -> finishEnglishRaw()
            KeyAction.VoiceHold -> Unit
        }
    }

    private fun shouldBufferEnglish(text: String): Boolean =
        ImePreferences.isEnglishSuggestionsEnabled(this) &&
            !textController.isPrivateField &&
            text.length == 1 && text[0].isAsciiLetterOrDigit()

    private fun shouldStartSlashCandidates(text: String): Boolean =
        text == "/" && !textController.isPrivateField

    private suspend fun startSlashCandidates() {
        finishEnglishRaw()
        if (reading.isNotEmpty()) textController.finishComposition()
        resetConversion(clearComposing = false)
        textController.appendComposing("/")
        slashBufferActive = true
        slashGeneration++
        candidateSource = CandidateSource.SLASH
        candidates = ImePreferences.getSlashCommandCandidates(this)
        selectedCandidate = -1
        showCandidateState()
    }

    private fun commitSlashCandidate(index: Int) {
        if (!slashBufferActive || candidateSource != CandidateSource.SLASH || index !in candidates.indices) return
        textController.commitCandidate(candidates[index])
        invalidateSlash(clearComposing = false)
    }

    private fun finishSlashRaw() {
        if (!slashBufferActive) return
        textController.finishComposition()
        invalidateSlash(clearComposing = false)
    }

    private fun invalidateSlash(clearComposing: Boolean) {
        slashGeneration++
        slashBufferActive = false
        if (clearComposing) textController.cancelComposition()
        if (candidateSource == CandidateSource.SLASH) {
            candidateSource = CandidateSource.NONE
            candidates = emptyList()
            selectedCandidate = -1
            showCandidateState()
        }
    }

    private fun appendEnglish(text: String, editorToken: Long) {
        if (reading.isNotEmpty()) {
            textController.finishComposition()
            clearCandidateState()
        }
        if (englishBuffer.length >= MAX_ENGLISH_BUFFER) finishEnglishRaw()
        englishBuffer = textController.appendComposing(text)
        candidateSource = CandidateSource.ENGLISH
        requestEnglishSuggestions(englishBuffer, editorToken)
    }

    private fun requestEnglishSuggestions(prefix: String, editorToken: Long) {
        val generation = ++englishGeneration
        candidateSource = CandidateSource.ENGLISH
        candidates = emptyList()
        selectedCandidate = -1
        showCandidateState()
        serviceScope.launch {
            val suggestions = runCatching { englishSuggestionEngine.suggest(prefix, MAX_ENGLISH_CANDIDATES) }
                .getOrDefault(emptyList())
            actionMutex.withLock {
                if (!editorSession.isCurrent(editorToken) || textController.isPrivateField ||
                    generation != englishGeneration || englishBuffer != prefix ||
                    candidateSource != CandidateSource.ENGLISH
                ) return@withLock
                candidates = suggestions.take(MAX_ENGLISH_CANDIDATES)
                selectedCandidate = -1
                showCandidateState()
            }
        }
    }

    private fun commitEnglishCandidate(index: Int) {
        if (index !in candidates.indices || candidateSource != CandidateSource.ENGLISH) return
        textController.commitCandidate(candidates[index])
        invalidateEnglish(clearComposing = false)
    }

    private fun finishEnglishRaw() {
        if (englishBuffer.isEmpty()) return
        textController.finishComposition()
        invalidateEnglish(clearComposing = false)
    }

    private fun invalidateEnglish(clearComposing: Boolean) {
        englishGeneration++
        englishBuffer = ""
        if (clearComposing) textController.cancelComposition()
        if (candidateSource == CandidateSource.ENGLISH) {
            candidateSource = CandidateSource.NONE
            candidates = emptyList()
            selectedCandidate = -1
            showCandidateState()
        }
    }

    private fun showCandidateState() {
        showCandidateStrip(candidates, selectedCandidate)
        keyboardView?.setCandidates(candidates, selectedCandidate)
    }

    private fun showCandidateStrip(values: List<String>, selectedIndex: Int) {
        candidateStrip?.showCandidates(CandidateUiSnapshot(++candidateUiToken, values, selectedIndex))
    }

    private fun candidateSnapshot() = CandidateSnapshot(
        source = candidateSource,
        candidates = candidates,
        conversionGeneration = conversionGeneration,
        englishGeneration = englishGeneration,
        englishBuffer = englishBuffer,
        slashGeneration = slashGeneration,
        slashBufferActive = slashBufferActive,
    )

    private suspend fun updateReading(newReading: String) {
        conversionPreview = null
        if (textController.isPrivateField || newReading.isEmpty()) {
            resetConversion(clearComposing = false)
            return
        }
        val wasEmpty = reading.isEmpty()
        reading = newReading
        keyboardView?.setConversionActive(true)
        val generation = ++conversionGeneration
        val state = if (wasEmpty) conversionEngine.start(newReading) else conversionEngine.update(newReading)
        if (generation == conversionGeneration && reading == newReading) applyConversion(state)
    }

    private suspend fun cycleCandidate() {
        if (reading.isEmpty()) return textController.commitText(" ")
        conversionPreview = null
        textController.replaceComposing(reading)
        val generation = ++conversionGeneration
        val state = conversionEngine.nextCandidate()
        if (generation == conversionGeneration) applyConversion(state)
    }

    private suspend fun commitCandidate(index: Int) {
        if (index !in candidates.indices) return
        val generation = ++conversionGeneration
        val result = conversionEngine.commit(index)
        if (generation != conversionGeneration) return
        result?.value?.let(textController::commitCandidate)
        clearCandidateState()
    }

    private suspend fun commitDisplayedConversion() {
        val preview = conversionPreview
        if (preview != null) {
            textController.commitCandidate(preview)
            resetConversion(clearComposing = false)
        } else if (candidates.isNotEmpty()) {
            commitCandidate(selectedCandidate.coerceAtLeast(0))
        } else if (reading.isNotEmpty()) {
            textController.commitCandidate(reading)
            resetConversion(clearComposing = false)
        }
    }

    private fun showConversionPreview(value: String) {
        if (reading.isEmpty()) return
        conversionPreview = value
        textController.replaceComposing(value)
        showCandidateStrip(emptyList(), -1)
        keyboardView?.setCandidates(emptyList(), -1)
    }

    private fun restoreReadingPreview() {
        if (conversionPreview == null) return
        conversionPreview = null
        textController.replaceComposing(reading)
    }

    private fun applyConversion(state: ConversionState) {
        candidateSource = CandidateSource.JAPANESE
        candidates = state.candidates.map { it.value }
        selectedCandidate = state.selectedIndex
        showCandidateStrip(candidates, selectedCandidate)
        keyboardView?.setCandidates(candidates, selectedCandidate)
        keyboardView?.setConversionActive(reading.isNotEmpty())
    }

    private fun invalidateConversion(clearComposing: Boolean) {
        conversionGeneration++
        clearCandidateState()
        val invalidatedEditor = editorSession.capture()
        serviceScope.launch {
            actionMutex.withLock {
                if (editorSession.isCurrent(invalidatedEditor)) resetConversion(clearComposing)
            }
        }
    }

    private suspend fun resetConversion(clearComposing: Boolean) {
        conversionGeneration++
        if (clearComposing) textController.cancelComposition()
        clearCandidateState()
        conversionEngine.reset()
    }

    private fun clearCandidateState() {
        englishGeneration++
        englishBuffer = ""
        slashGeneration++
        slashBufferActive = false
        reading = ""
        candidates = emptyList()
        selectedCandidate = -1
        conversionPreview = null
        candidateSource = CandidateSource.NONE
        showCandidateStrip(emptyList(), -1)
        keyboardView?.setCandidates(emptyList(), -1)
        keyboardView?.setConversionActive(false)
    }

    private fun onVoiceAction(event: VoiceUiEvent) {
        if (event.sessionToken != voiceUiToken) return
        val token = editorSession.capture()
        when (event.action) {
            VoiceUiAction.Cancel -> {
                cancelVoiceHold()
                if (editorSession.isCurrent(token)) setVoiceUi(voiceController.initialState().toUiState())
            }
            VoiceUiAction.RequestPermission -> startActivity(
                Intent(this, SetupActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .putExtra(SetupActivity.EXTRA_REQUEST_MICROPHONE_PERMISSION, true),
            )
            VoiceUiAction.ExplainUnavailable -> {
                val message = latestVoiceUnavailableMessage ?: "端末内音声認識を利用できません"
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    internal fun onVoiceState(state: VoiceBackendState, token: Long) {
        if (!editorSession.isCurrent(token) || textController.isPrivateField) return
        val holdId = voiceHoldRequestId
        if (holdId != null && token == voiceHoldEditorToken) {
            when (state) {
                VoiceBackendState.Recording -> {
                    voiceHoldReady = true
                    keyboardView?.onVoiceRecordingReady(holdId)
                    if (voiceHoldReleased) voiceController.stop()
                }
                is VoiceBackendState.Preview -> {
                    if (!voiceHoldReleased) return
                    val text = voiceController.confirm(token) ?: return
                    commitVoiceHold(holdId, token, text)
                    return
                }
                is VoiceBackendState.Unavailable -> {
                    voiceHoldRequestId = null
                    voiceHoldReady = false
                    voiceHoldReleased = false
                }
                else -> Unit
            }
        }
        setVoiceUi(state.toUiState())
    }

    private fun commitVoiceHold(requestId: Long, token: Long, text: String) {
        if (voiceHoldRequestId != requestId) return
        val generation = voiceHoldGeneration
        voiceHoldRequestId = null
        voiceHoldReady = false
        serviceScope.launch {
            actionMutex.withLock {
                if (generation != voiceHoldGeneration || !editorSession.isCurrent(token) || textController.isPrivateField) return@withLock
                resetConversion(clearComposing = false)
                if (generation != voiceHoldGeneration || !editorSession.isCurrent(token)) return@withLock
                editorSession.runIfCurrent(token) { textController.commitText(text) }
                setVoiceUi(voiceController.initialState().toUiState())
            }
        }
    }

    private fun setVoiceUi(state: VoiceUiState) {
        if (state is VoiceUiState.Unavailable) latestVoiceUnavailableMessage = state.message
        candidateStrip?.setVoiceState(VoiceUiSnapshot(++voiceUiToken, state))
    }

    override fun onDestroy() {
        voiceController.destroy()
        serviceScope.cancel()
        super.onDestroy()
    }

    internal fun installTestDependencies(
        voice: VoiceRecognitionController,
        text: TextInputController,
        conversion: ConversionEngine,
        english: EnglishSuggestionEngine? = null,
    ) {
        voiceController = voice
        textController = text
        testConversionEngine = conversion
        testEnglishSuggestionEngine = english
    }
}

private enum class CandidateSource { NONE, JAPANESE, ENGLISH, SLASH }

private data class CandidateSnapshot(
    val source: CandidateSource,
    val candidates: List<String>,
    val conversionGeneration: Long,
    val englishGeneration: Long,
    val englishBuffer: String,
    val slashGeneration: Long,
    val slashBufferActive: Boolean,
)

private fun Char.isAsciiLetterOrDigit(): Boolean = this in 'a'..'z' || this in 'A'..'Z' || this in '0'..'9'

private const val MAX_ENGLISH_BUFFER = 64
private const val MAX_ENGLISH_CANDIDATES = 5

private fun VoiceBackendState.toUiState(): VoiceUiState = when (this) {
    VoiceBackendState.Idle -> VoiceUiState.Idle
    VoiceBackendState.PermissionRequired -> VoiceUiState.PermissionRequired
    VoiceBackendState.Recording -> VoiceUiState.Recording
    VoiceBackendState.Recognizing -> VoiceUiState.Recognizing
    is VoiceBackendState.Partial -> VoiceUiState.Partial(text)
    is VoiceBackendState.Preview -> VoiceUiState.Recognizing
    is VoiceBackendState.Unavailable -> VoiceUiState.Unavailable(message)
}

internal fun String.toKatakana(): String = map { char ->
    if (char in 'ぁ'..'ゖ') (char.code + 0x60).toChar() else char
}.joinToString("")
