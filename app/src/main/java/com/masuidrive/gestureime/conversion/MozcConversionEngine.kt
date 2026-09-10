package com.masuidrive.gestureime.conversion

import android.content.Context
import com.google.android.apps.inputmethod.libs.mozc.session.MozcJNI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCommands
import org.mozc.android.inputmethod.japanese.protobuf.ProtoConfig
import java.io.File

/** Local-only Mozc session. All protobuf evaluation runs away from the UI thread. */
class MozcConversionEngine(context: Context) : ConversionEngine {
    private val appContext = context.applicationContext
    private var sessionId: Long = 0
    private var state = ConversionState("", emptyList(), 0)
    private var initialized = false
    private val mutex = Mutex()

    override suspend fun start(reading: String): ConversionState = update(reading)

    override suspend fun update(reading: String): ConversionState = mutex.withLock { withContext(Dispatchers.Default) {
        require(reading.length <= MAX_READING_LENGTH) { "Reading has ${reading.length} characters; maximum is $MAX_READING_LENGTH" }
        ensureInitialized()
        recreateSession()
        reading.forEach { sendKey(it.toString()) }
        state = stateFrom(lastOutput)
        state
    } }

    override suspend fun nextCandidate(): ConversionState = mutex.withLock { withContext(Dispatchers.Default) {
        ensureInitialized()
        check(sessionId != 0L) { "Mozc session has not been started" }
        lastOutput = evaluate(commandForKey(ProtoCommands.KeyEvent.SpecialKey.SPACE))
        state = stateFrom(lastOutput)
        state
    } }

    override suspend fun commit(index: Int): ConversionCommit? = mutex.withLock { withContext(Dispatchers.Default) {
        ensureInitialized()
        val candidate = state.candidates.getOrNull(index) ?: return@withContext null
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
        val committed = commandResult(selectionOutput) + commandResult(lastOutput)
        if (committed.isEmpty()) return@withContext null
        state = ConversionState("", emptyList(), 0)
        ConversionCommit(committed)
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
        state = ConversionState("", emptyList(), 0)
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
                        ProtoConfig.Config.newBuilder()
                            .setIncognitoMode(true)
                            .setHistoryLearningLevel(ProtoConfig.Config.HistoryLearningLevel.NO_HISTORY),
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

    private fun evaluate(command: ProtoCommands.Command): ProtoCommands.Command =
        ProtoCommands.Command.parseFrom(MozcJNI.evalCommand(command.toByteArray()))

    private fun commandResult(command: ProtoCommands.Command): String =
        if (command.hasOutput() && command.output.hasResult()) command.output.result.value else ""

    private fun stateFrom(command: ProtoCommands.Command): ConversionState {
        val output = command.output
        val window = output.candidateWindow
        val candidates = if (output.hasCandidateWindow()) {
            window.candidateList.map { ConversionCandidate(it.id, it.value) }
        } else {
            emptyList()
        }
        val reading = if (output.hasPreedit()) output.preedit.segmentList.joinToString("") { it.value } else state.reading
        val selected = if (output.hasCandidateWindow() && output.candidateWindow.hasFocusedIndex()) {
            window.candidateList.indexOfFirst { it.index == window.focusedIndex }.coerceAtLeast(0)
        } else {
            0
        }
        return ConversionState(reading, candidates, selected.coerceIn(0, maxOf(0, candidates.lastIndex)))
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
    }
}
