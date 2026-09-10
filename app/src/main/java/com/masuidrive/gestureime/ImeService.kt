package com.masuidrive.gestureime

import android.content.ClipboardManager
import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import com.masuidrive.gestureime.conversion.ConversionEngine
import com.masuidrive.gestureime.conversion.ConversionState
import com.masuidrive.gestureime.conversion.MozcConversionEngine
import com.masuidrive.gestureime.keyboard.KeyAction
import com.masuidrive.gestureime.keyboard.KeyboardActionSink
import com.masuidrive.gestureime.keyboard.KeyboardMode
import com.masuidrive.gestureime.keyboard.KeyboardView
import com.masuidrive.gestureime.ui.CandidateStripView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ImeService : InputMethodService(), KeyboardActionSink {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val actionMutex = Mutex()
    private val editorSession = EditorSessionGate()
    private val conversionEngine: ConversionEngine by lazy { MozcConversionEngine(applicationContext) }
    private lateinit var textController: TextInputController
    private var keyboardView: KeyboardView? = null
    private var candidateStrip: CandidateStripView? = null
    private var conversionGeneration = 0L
    private var reading = ""
    private var candidates = emptyList<String>()
    private var selectedCandidate = -1

    override fun onCreate() {
        super.onCreate()
        textController = TextInputController(
            connection = { currentInputConnection },
            context = applicationContext,
            clipboard = getSystemService(ClipboardManager::class.java),
        )
    }

    override fun onCreateInputView(): View {
        val candidateHeight = (52 * resources.displayMetrics.density).toInt()
        val keyboard = KeyboardView(this).also {
            it.actionSink = this
            keyboardView = it
        }
        val strip = CandidateStripView(this).also {
            it.setOnCandidateSelected { index -> onKeyAction(KeyAction.SelectCandidate(index)) }
            candidateStrip = it
        }
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(strip, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, candidateHeight))
            addView(keyboard, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        }
    }

    override fun onStartInput(attribute: EditorInfo, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        editorSession.advance()
        invalidateConversion(clearComposing = false)
        textController.beginInput(attribute)
        candidateStrip?.visibility = if (textController.isPrivateField) View.GONE else View.VISIBLE
        keyboardView?.setMode(KeyboardMode.QWERTY)
    }

    override fun onFinishInput() {
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
    }

    override fun onKeyAction(action: KeyAction) {
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

    private suspend fun processInputAction(action: KeyAction, editorToken: Long) {
        when (action) {
            is KeyAction.CommitText -> {
                if (action.text == " " && reading.isNotEmpty()) cycleCandidate()
                else {
                    resetConversion(clearComposing = false)
                    editorSession.runIfCurrent(editorToken) { textController.commitText(action.text) }
                }
            }
            is KeyAction.KanaInput -> updateReading(textController.appendComposing(action.reading))
            KeyAction.TransformKana -> updateReading(textController.transformKana())
            is KeyAction.Backspace -> {
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
        if (textController.isPrivateField || newReading.isEmpty()) {
            resetConversion(clearComposing = false)
            return
        }
        val wasEmpty = reading.isEmpty()
        reading = newReading
        val generation = ++conversionGeneration
        val state = if (wasEmpty) conversionEngine.start(newReading) else conversionEngine.update(newReading)
        if (generation == conversionGeneration && reading == newReading) applyConversion(state)
    }

    private suspend fun cycleCandidate() {
        if (reading.isEmpty()) return textController.commitText(" ")
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

    private fun applyConversion(state: ConversionState) {
        candidates = state.candidates.map { it.value }
        selectedCandidate = state.selectedIndex
        candidateStrip?.showCandidates(candidates, selectedCandidate)
        keyboardView?.setCandidates(candidates, selectedCandidate)
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
        candidateStrip?.showCandidates(emptyList(), -1)
        keyboardView?.setCandidates(emptyList(), -1)
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
