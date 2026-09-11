package com.masuidrive.gestureime.conversion

import android.content.Context
import com.masuidrive.gestureime.ImePreferences
import com.google.android.apps.inputmethod.libs.mozc.session.MozcJNI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCandidateWindow
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCommands
import org.mozc.android.inputmethod.japanese.protobuf.ProtoConfig
import java.io.File

/** Local-only Mozc session. All protobuf evaluation runs away from the UI thread. */
class MozcConversionEngine(
    context: Context,
    private val userDictionaryEnabled: () -> Boolean = {
        ImePreferences.isAndroidUserDictionaryEnabled(context.applicationContext)
    },
) : ConversionEngine {
    private val appContext = context.applicationContext
    private val userDictionaryCandidates = AndroidUserDictionaryCandidates(appContext.contentResolver)
    private var sessionId: Long = 0
    private var state = ConversionState("", emptyList(), -1)
    private var initialized = false
    private val mutex = Mutex()

    override suspend fun start(reading: String): ConversionState = update(reading)

    override suspend fun update(reading: String): ConversionState = mutex.withLock { withContext(Dispatchers.Default) {
        require(reading.length <= MAX_READING_LENGTH) { "Reading has ${reading.length} characters; maximum is $MAX_READING_LENGTH" }
        ensureInitialized()
        recreateSession()
        state = ConversionState("", emptyList(), -1)
        if (reading.isEmpty()) return@withContext state
        reading.forEach { sendKey(it.toString()) }
        state = mergeAndroidUserDictionaryCandidates(stateFrom(lastOutput), platformDictionaryWords(reading))
        state
    } }

    override suspend fun nextCandidate(): ConversionState = mutex.withLock { withContext(Dispatchers.Default) {
        ensureInitialized()
        check(sessionId != 0L) { "Mozc session has not been started" }
        lastOutput = evaluate(commandForKey(ProtoCommands.KeyEvent.SpecialKey.SPACE))
        state = mergeAndroidUserDictionaryCandidates(
            stateFrom(lastOutput),
            platformDictionaryWords(state.reading),
        )
        state
    } }

    override suspend fun commit(index: Int): ConversionCommit? = mutex.withLock { withContext(Dispatchers.Default) {
        ensureInitialized()
        val candidate = state.candidates.getOrNull(index) ?: return@withContext null
        if (candidate.source == ConversionCandidateSource.ANDROID_USER_DICTIONARY) {
            // A platform dictionary word has no Mozc candidate ID.  The IME
            // replaces the whole composing reading with this explicit word.
            deleteSession()
            state = ConversionState("", emptyList(), -1)
            return@withContext ConversionCommit(candidate.value)
        }
        val selectCommand = ProtoCommands.Command.newBuilder().setInput(
            ProtoCommands.Input.newBuilder()
                .setType(ProtoCommands.Input.CommandType.SEND_COMMAND)
                .setId(sessionId)
                .setCommand(
                    ProtoCommands.SessionCommand.newBuilder()
                        .setType(ProtoCommands.SessionCommand.CommandType.SELECT_CANDIDATE)
                        .setId(candidate.id),
                ),
        ).build()
        val selectionOutput = evaluate(selectCommand)
        check(selectionOutput.output.consumed) { "Mozc did not select a candidate" }
        // SUBMIT_CANDIDATE commits only the focused segment in multi-segment
        // conversion. The IME contract replaces the whole reading, so select
        // the candidate and submit the complete conversion instead.
        lastOutput = evaluate(
            ProtoCommands.Command.newBuilder().setInput(
                ProtoCommands.Input.newBuilder()
                    .setType(ProtoCommands.Input.CommandType.SEND_COMMAND)
                    .setId(sessionId)
                    .setCommand(
                        ProtoCommands.SessionCommand.newBuilder()
                            .setType(ProtoCommands.SessionCommand.CommandType.SUBMIT),
                    ),
            ).build(),
        )
        check(lastOutput.output.consumed) { "Mozc did not submit the conversion" }
        val committed = commandResult(selectionOutput) + commandResult(lastOutput)
        if (committed.isEmpty()) return@withContext null
        state = ConversionState("", emptyList(), -1)
        ConversionCommit(committed)
    } }

    override suspend fun deleteCandidateFromHistory(index: Int): ConversionState? = mutex.withLock { withContext(Dispatchers.Default) {
        ensureInitialized()
        val candidate = state.candidates.getOrNull(index) ?: return@withContext null
        if (candidate.source != ConversionCandidateSource.MOZC || sessionId == 0L) return@withContext null
        val output = evaluate(
            ProtoCommands.Command.newBuilder().setInput(
                ProtoCommands.Input.newBuilder()
                    .setType(ProtoCommands.Input.CommandType.SEND_COMMAND)
                    .setId(sessionId)
                    .setCommand(
                        ProtoCommands.SessionCommand.newBuilder()
                            .setType(ProtoCommands.SessionCommand.CommandType.DELETE_CANDIDATE_FROM_HISTORY)
                            .setId(candidate.id),
                    ),
            ).build(),
        )
        if (!output.output.consumed) return@withContext null
        state = mergeAndroidUserDictionaryCandidates(
            stateFrom(output),
            platformDictionaryWords(state.reading),
        )
        state
    } }

    override suspend fun reset() = mutex.withLock { withContext(Dispatchers.Default) {
        if (sessionId != 0L && initialized) {
            evaluate(
                ProtoCommands.Command.newBuilder().setInput(
                    ProtoCommands.Input.newBuilder()
                        .setType(ProtoCommands.Input.CommandType.SEND_COMMAND)
                        .setId(sessionId)
                        .setCommand(
                            ProtoCommands.SessionCommand.newBuilder()
                                .setType(ProtoCommands.SessionCommand.CommandType.REVERT),
                        ),
                ).build(),
            )
        }
        deleteSession()
        state = ConversionState("", emptyList(), -1)
    } }

    private lateinit var lastOutput: ProtoCommands.Command

    private fun ensureInitialized() {
        if (initialized) return
        System.loadLibrary("mozc")
        check(MozcJNI.initialize()) { "Mozc JNI registration failed" }
        val dataFile = copyDataAsset()
        check(MozcJNI.onPostLoad(appContext.filesDir.absolutePath, dataFile.absolutePath)) {
            "Mozc data initialization failed"
        }
        val dataVersion = MozcJNI.getDataVersion()
        check(isUsableDataVersion(dataVersion)) {
            "Mozc dictionary was not loaded"
        }
        evaluate(
            ProtoCommands.Command.newBuilder().setInput(
                ProtoCommands.Input.newBuilder()
                    .setType(ProtoCommands.Input.CommandType.SET_CONFIG)
                    .setConfig(
                        learningConfig(),
                    ),
            ).build(),
        )
        evaluate(
            ProtoCommands.Command.newBuilder().setInput(
                ProtoCommands.Input.newBuilder()
                    .setType(ProtoCommands.Input.CommandType.SET_REQUEST)
                    .setRequest(
                        ProtoCommands.Request.newBuilder()
                            .setZeroQuerySuggestion(true)
                            .setMixedConversion(true)
                            .setUpdateInputModeFromSurroundingText(false)
                            .setSpecialRomanjiTable(
                                ProtoCommands.Request.SpecialRomanjiTable.TOGGLE_FLICK_TO_HIRAGANA,
                            )
                            .setKanaModifierInsensitiveConversion(true)
                            .setAutoPartialSuggestion(false)
                            .setLanguageAwareInput(
                                ProtoCommands.Request.LanguageAwareInputBehavior.NO_LANGUAGE_AWARE_INPUT,
                            ),
                    ),
            ).build(),
        )
        initialized = true
    }

    private fun recreateSession() {
        deleteSession()
        lastOutput = evaluate(
            ProtoCommands.Command.newBuilder().setInput(
                ProtoCommands.Input.newBuilder().setType(ProtoCommands.Input.CommandType.CREATE_SESSION),
            ).build(),
        )
        check(lastOutput.hasOutput() && lastOutput.output.id != 0L) { "Mozc did not create a session" }
        sessionId = lastOutput.output.id
        lastOutput = evaluate(commandForKey(ProtoCommands.KeyEvent.SpecialKey.ON))
        check(lastOutput.output.consumed) { "Mozc did not enter input mode" }
    }

    private fun deleteSession() {
        if (sessionId != 0L && initialized) {
            evaluate(ProtoCommands.Command.newBuilder().setInput(
                ProtoCommands.Input.newBuilder().setType(ProtoCommands.Input.CommandType.DELETE_SESSION).setId(sessionId),
            ).build())
        }
        sessionId = 0
    }

    private fun sendKey(text: String) {
        lastOutput = evaluate(commandForKey(text))
    }

    private fun platformDictionaryWords(reading: String): List<String> =
        if (userDictionaryEnabled()) userDictionaryCandidates.forReading(reading) else emptyList()

    private fun commandForKey(text: String): ProtoCommands.Command = ProtoCommands.Command.newBuilder().setInput(
        ProtoCommands.Input.newBuilder()
            .setType(ProtoCommands.Input.CommandType.SEND_KEY)
            .setId(sessionId)
            .setKey(ProtoCommands.KeyEvent.newBuilder().setKeyString(text)),
    ).build()

    private fun commandForKey(key: ProtoCommands.KeyEvent.SpecialKey): ProtoCommands.Command =
        ProtoCommands.Command.newBuilder().setInput(
            ProtoCommands.Input.newBuilder()
                .setType(ProtoCommands.Input.CommandType.SEND_KEY)
                .setId(sessionId)
                .setKey(ProtoCommands.KeyEvent.newBuilder().setSpecialKey(key)),
        ).build()

    private fun evaluate(command: ProtoCommands.Command): ProtoCommands.Command {
        val response = ProtoCommands.Command.parseFrom(MozcJNI.evalCommand(command.toByteArray()))
        check(response.hasOutput() && response.output.errorCode == ProtoCommands.Output.ErrorCode.SESSION_SUCCESS) {
            "Mozc session command failed"
        }
        return response
    }

    private fun commandResult(command: ProtoCommands.Command): String =
        if (command.hasOutput() && command.output.hasResult()) command.output.result.value else ""

    private fun stateFrom(command: ProtoCommands.Command): ConversionState {
        val output = command.output
        if (output.hasAllCandidateWords()) {
            val list = output.allCandidateWords
            val candidates = list.candidatesList.map { ConversionCandidate(it.id, it.value) }
            val selected = if (
                list.category == ProtoCandidateWindow.Category.CONVERSION &&
                list.hasFocusedIndex()
            ) {
                list.focusedIndex
            } else {
                -1
            }
            val reading = if (output.hasPreedit()) output.preedit.segmentList.joinToString("") { it.value } else ""
            return ConversionState(reading, candidates, selected)
        }
        val window = output.candidateWindow
        val candidates = if (output.hasCandidateWindow()) {
            window.candidateList.map { ConversionCandidate(it.id, it.value) }
        } else {
            emptyList()
        }
        val reading = if (output.hasPreedit()) output.preedit.segmentList.joinToString("") { it.value } else ""
        val selected = if (output.hasCandidateWindow() && output.candidateWindow.hasFocusedIndex()) {
            window.candidateList.indexOfFirst { it.index == window.focusedIndex }
        } else {
            -1
        }
        return ConversionState(reading, candidates, selected)
    }

    private fun copyDataAsset(): File {
        val destination = File(appContext.filesDir, DATA_FILE_NAME)
        if (!destination.isFile || destination.length() == 0L) {
            val temporary = File(appContext.filesDir, "$DATA_FILE_NAME.tmp")
            appContext.assets.open(DATA_FILE_NAME).use { input ->
                temporary.outputStream().use { output -> input.copyTo(output) }
            }
            check(temporary.length() > 0L) { "Mozc data asset copy is empty" }
            check(temporary.renameTo(destination)) { "Mozc data asset rename failed" }
        }
        check(destination.length() > 0L) { "Mozc data asset is empty" }
        return destination
    }

    companion object {
        const val DATA_FILE_NAME = "mozc.data"
        const val MAX_READING_LENGTH = 256
        const val MINIMAL_ENGINE_DATA_VERSION = "0.0.0"

        internal fun isUsableDataVersion(version: String): Boolean =
            version.isNotBlank() && version != MINIMAL_ENGINE_DATA_VERSION

        internal fun learningConfig(): ProtoConfig.Config = ProtoConfig.Config.newBuilder()
            .setPreeditMethod(ProtoConfig.Config.PreeditMethod.KANA)
            .setIncognitoMode(false)
            .setHistoryLearningLevel(ProtoConfig.Config.HistoryLearningLevel.DEFAULT_HISTORY)
            .build()

        internal fun mergeAndroidUserDictionaryCandidates(
            mozcState: ConversionState,
            userDictionaryWords: List<String>,
        ): ConversionState {
            val mozcValues = mozcState.candidates.mapTo(mutableSetOf()) { it.value }
            val userCandidates = userDictionaryWords
                .distinct()
                .filterNot(mozcValues::contains)
                .mapIndexed { index, value ->
                    ConversionCandidate(
                        id = -index - 1,
                        value = value,
                        source = ConversionCandidateSource.ANDROID_USER_DICTIONARY,
                    )
                }
            if (userCandidates.isEmpty()) return mozcState
            return mozcState.copy(
                candidates = userCandidates + mozcState.candidates,
                selectedIndex = mozcState.selectedIndex.takeIf { it >= 0 }?.plus(userCandidates.size) ?: -1,
            )
        }
    }
}
