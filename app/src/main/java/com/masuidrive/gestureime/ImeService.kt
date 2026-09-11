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
    private lateinit var textController: TextInputController
    private lateinit var voiceController: VoiceRecognitionController
    private var keyboardView: KeyboardView? = null
    private var candidateStrip: CandidateStripView? = null
    private var conversionGeneration = 0L
    private var reading = ""
    private var candidates = emptyList<String>()
    private var selectedCandidate = -1
    private var conversionPreview: String? = null
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
            it.setDualFlickEnabled(ImePreferences.isDualFlickEnabled(this))
            it.setQwertyLabelStyle(ImePreferences.getQwertyLabelStyle(this))
            keyboardView = it
        }
        val strip = CandidateStripView(this).also {
            it.setOnCandidateSelected { index -> onKeyAction(KeyAction.SelectCandidate(index)) }
            it.setOnVoiceActionListener(::onVoiceAction)
            candidateStrip = it
            setVoiceUi(if (textController.isPrivateField) VoiceUiState.Hidden else voiceController.initialState().toUiState())
        }
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(strip, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, candidateHeight))
            addView(keyboard, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        }
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        cancelVoiceHold()
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
        textController.beginInput(attribute)
        textController.terminalCursorEnabled = ImePreferences.isTerminalCursorEnabled(this)
        candidateStrip?.visibility = if (textController.isPrivateField) View.GONE else View.VISIBLE
        setVoiceUi(if (textController.isPrivateField) VoiceUiState.Hidden else voiceController.initialState().toUiState())
        keyboardView?.setMode(KeyboardMode.QWERTY)
        keyboardView?.setDualFlickEnabled(ImePreferences.isDualFlickEnabled(this))
        keyboardView?.setQwertyLabelStyle(ImePreferences.getQwertyLabelStyle(this))
    }

    override fun onFinishInput() {
        editorSession.advance()
        cancelVoiceHold()
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
    }

    override fun onKeyAction(action: KeyAction) {
        cancelVoiceHold()
        if (!textController.isPrivateField) setVoiceUi(voiceController.initialState().toUiState())
        if (action is KeyAction.SwitchLayer) {
            keyboardView?.setMode(action.target)
            return
        }
        if (action is KeyAction.SetModifier) return
        val queuedForEditor = editorSession.capture()
        val queuedCandidateSnapshot = if (action is KeyAction.SelectCandidate) candidates.toList() else null
        serviceScope.launch {
            actionMutex.withLock {
                if (!editorSession.isCurrent(queuedForEditor)) return@withLock
                if (queuedCandidateSnapshot != null && queuedCandidateSnapshot != candidates) return@withLock
                runCatching { processInputAction(action, queuedForEditor) }
                    .onFailure {
                        clearCandidateState()
                        candidateStrip?.showStatus("変換を利用できません")
                    }
            }
        }
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
                else if (voiceHoldReady) voiceController.stop() else cancelVoiceHold()
            }
            is VoiceHoldEvent.Cancel -> if (voiceHoldRequestId == event.requestId) cancelVoiceHold()
        }
    }

    private fun cancelVoiceHold() {
        voiceHoldGeneration++
        voiceHoldRequestId = null
        voiceHoldReady = false
        voiceHoldReleased = false
        voiceController.cancel(notify = false)
    }

    private suspend fun processInputAction(action: KeyAction, editorToken: Long) {
        when (action) {
            is KeyAction.CommitText -> {
                if (action.text == " " && reading.isNotEmpty()) cycleCandidate()
                else {
                    resetConversion(clearComposing = false)
                    editorSession.runIfCurrent(editorToken) { textController.commitText(action.text) }
                }
            }
            is KeyAction.KanaInput -> {
                restoreReadingPreview()
                updateReading(textController.appendComposing(action.reading))
            }
            is KeyAction.TransformKana -> {
                restoreReadingPreview()
                updateReading(textController.transformKana(action.transform))
            }
            is KeyAction.Backspace -> {
                restoreReadingPreview()
                val remaining = textController.backspace()
                if (reading.isNotEmpty()) updateReading(remaining)
            }
            KeyAction.Enter -> {
                if (reading.isNotEmpty()) {
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
                resetConversion(clearComposing = false)
                editorSession.runIfCurrent(editorToken) { textController.paste() }
            }
            KeyAction.Escape -> {
                resetConversion(clearComposing = false)
                editorSession.runIfCurrent(editorToken) { textController.escape() }
            }
            KeyAction.CommitConversion -> commitDisplayedConversion()
            KeyAction.CommitWithoutConversion -> showConversionPreview(reading)
            KeyAction.ConvertToKatakana -> showConversionPreview(reading.toKatakana())
            is KeyAction.MoveCursor -> {
                resetConversion(clearComposing = false)
                editorSession.runIfCurrent(editorToken) { textController.moveCursor(action.direction, action.units) }
            }
            is KeyAction.MoveToBoundary -> {
                resetConversion(clearComposing = false)
                editorSession.runIfCurrent(editorToken) { textController.moveToBoundary(action.boundary) }
            }
            is KeyAction.ModifiedKey -> {
                resetConversion(clearComposing = false)
                editorSession.runIfCurrent(editorToken) {
                    textController.sendModifiedKey(action.label, action.modifier)
                }
            }
            is KeyAction.SwitchLayer -> Unit
            is KeyAction.SelectCandidate -> commitCandidate(action.index)
            KeyAction.CycleCandidate -> cycleCandidate()
            is KeyAction.SetModifier -> Unit
        }
    }

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
        candidateStrip?.showCandidates(emptyList(), -1)
        keyboardView?.setCandidates(emptyList(), -1)
    }

    private fun restoreReadingPreview() {
        if (conversionPreview == null) return
        conversionPreview = null
        textController.replaceComposing(reading)
    }

    private fun applyConversion(state: ConversionState) {
        candidates = state.candidates.map { it.value }
        selectedCandidate = state.selectedIndex
        candidateStrip?.showCandidates(candidates, selectedCandidate)
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
        reading = ""
        candidates = emptyList()
        selectedCandidate = -1
        conversionPreview = null
        candidateStrip?.showCandidates(emptyList(), -1)
        keyboardView?.setCandidates(emptyList(), -1)
        keyboardView?.setConversionActive(false)
    }

    private fun onVoiceAction(event: VoiceUiEvent) {
        if (event.sessionToken != voiceUiToken) return
        val token = editorSession.capture()
        when (event.action) {
            VoiceUiAction.Start -> serviceScope.launch {
                actionMutex.withLock {
                    if (!editorSession.isCurrent(token) || event.sessionToken != voiceUiToken || textController.isPrivateField) return@withLock
                    resetConversion(clearComposing = false)
                    if (editorSession.isCurrent(token) && event.sessionToken == voiceUiToken) voiceController.start(token)
                }
            }
            VoiceUiAction.Stop -> voiceController.stop()
            VoiceUiAction.Cancel -> {
                cancelVoiceHold()
                if (editorSession.isCurrent(token)) setVoiceUi(voiceController.initialState().toUiState())
            }
            VoiceUiAction.Confirm -> {
                val text = voiceController.confirm(token) ?: return
                serviceScope.launch {
                    actionMutex.withLock {
                        if (!editorSession.isCurrent(token) || textController.isPrivateField) return@withLock
                        resetConversion(clearComposing = false)
                        editorSession.runIfCurrent(token) { textController.commitText(text) }
                        setVoiceUi(voiceController.initialState().toUiState())
                    }
                }
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
    ) {
        voiceController = voice
        textController = text
        testConversionEngine = conversion
    }
}

private fun VoiceBackendState.toUiState(): VoiceUiState = when (this) {
    VoiceBackendState.Idle -> VoiceUiState.Idle
    VoiceBackendState.PermissionRequired -> VoiceUiState.PermissionRequired
    VoiceBackendState.Recording -> VoiceUiState.Recording
    VoiceBackendState.Recognizing -> VoiceUiState.Recognizing
    is VoiceBackendState.Preview -> VoiceUiState.Preview(text)
    is VoiceBackendState.Unavailable -> VoiceUiState.Unavailable(message)
}

internal fun String.toKatakana(): String = map { char ->
    if (char in 'ぁ'..'ゖ') (char.code + 0x60).toChar() else char
}.joinToString("")
