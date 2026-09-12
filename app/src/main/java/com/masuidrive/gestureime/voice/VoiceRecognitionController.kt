package com.masuidrive.gestureime.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognitionSupport
import android.speech.RecognitionSupportCallback
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import androidx.annotation.RequiresApi

sealed interface VoiceBackendState {
    data object Idle : VoiceBackendState
    data object PermissionRequired : VoiceBackendState
    data object Recording : VoiceBackendState
    data object Recognizing : VoiceBackendState
    data class Partial(val text: String) : VoiceBackendState
    data class Preview(val candidates: List<String>) : VoiceBackendState
    data class Unavailable(val message: String) : VoiceBackendState
}

internal interface VoiceRecognizer {
    fun checkJapaneseSupport(callback: (Boolean?) -> Unit)
    fun start()
    fun stop()
    fun cancel()
    fun destroy()
}

internal fun interface VoiceRecognizerFactory {
    fun create(listener: VoiceRecognizerListener): VoiceRecognizer
}

internal interface VoiceRecognizerListener {
    fun onReady()
    fun onEndOfSpeech()
    fun onPartialResults(results: List<String>)
    fun onResults(results: List<String>)
    fun onError(error: Int)
}

class VoiceRecognitionController internal constructor(
    private val sdkInt: Int,
    private val hasPermission: () -> Boolean,
    private val onDeviceAvailable: () -> Boolean,
    private val factory: VoiceRecognizerFactory,
    private val onState: (VoiceBackendState, Long) -> Unit,
) {
    companion object {
        internal const val END_OF_SPEECH_GRACE_MS = 500L
    }

    constructor(context: Context, onState: (VoiceBackendState, Long) -> Unit) : this(
        sdkInt = Build.VERSION.SDK_INT,
        hasPermission = {
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        },
        onDeviceAvailable = {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
        },
        factory = VoiceRecognizerFactory { listener ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) AndroidVoiceRecognizer(context, listener)
            else error("On-device speech recognition requires Android 12")
        },
        onState = onState,
    )

    private var generation = 0L
    private var editorToken = 0L
    private var recognizer: VoiceRecognizer? = null
    private var partial: String? = null
    private var preview: List<String>? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var endOfSpeechFallback: Runnable? = null

    fun initialState(): VoiceBackendState = when {
        sdkInt < Build.VERSION_CODES.S -> VoiceBackendState.Unavailable("Android 12以降で利用できます")
        !runCatching(onDeviceAvailable).getOrDefault(false) -> VoiceBackendState.Unavailable("端末内音声認識を利用できません")
        !hasPermission() -> VoiceBackendState.PermissionRequired
        else -> VoiceBackendState.Idle
    }

    fun start(token: Long) {
        requireMainThread()
        when (val initial = initialState()) {
            VoiceBackendState.Idle -> Unit
            else -> { onState(initial, token); return }
        }
        invalidate(destroy = true)
        editorToken = token
        val activeGeneration = generation
        val created = runCatching { factory.create(object : VoiceRecognizerListener {
            override fun onReady() = deliver(activeGeneration, VoiceBackendState.Recording)
            override fun onEndOfSpeech() {
                deliver(activeGeneration, partialState())
                scheduleEndOfSpeechFallback(activeGeneration)
            }
            override fun onPartialResults(results: List<String>) {
                if (activeGeneration != generation || preview != null) return
                val text = results.firstOrNull { it.isNotBlank() } ?: return
                partial = text
                deliver(activeGeneration, VoiceBackendState.Partial(text))
            }
            override fun onResults(results: List<String>) {
                if (activeGeneration != generation) return
                clearEndOfSpeechFallback()
                val candidates = results.filter { it.isNotBlank() }.distinct()
                if (candidates.isEmpty()) finishWith(activeGeneration, VoiceBackendState.Unavailable("認識結果がありません"))
                else {
                    preview = candidates
                    deliver(activeGeneration, VoiceBackendState.Preview(candidates))
                }
            }
            override fun onError(error: Int) = finishWith(activeGeneration, VoiceBackendState.Unavailable(errorMessage(error)))
        }) }.getOrElse {
            finishWith(activeGeneration, VoiceBackendState.Unavailable("端末内音声認識を開始できません"))
            return
        }
        recognizer = created
        onState(VoiceBackendState.Recognizing, token)
        runCatching {
            created.checkJapaneseSupport { installed ->
                if (activeGeneration != generation) return@checkJapaneseSupport
                if (installed == false) finishWith(activeGeneration, VoiceBackendState.Unavailable("日本語の端末内音声モデルがありません"))
                else runCatching { created.start() }.onFailure {
                    finishWith(activeGeneration, VoiceBackendState.Unavailable("端末内音声認識を開始できません"))
                }
            }
        }.onFailure { finishWith(activeGeneration, VoiceBackendState.Unavailable("音声認識の対応状況を確認できません")) }
    }

    fun stop() {
        requireMainThread()
        val activeGeneration = generation
        val active = recognizer ?: return
        if (runCatching { active.stop() }.isFailure) {
            finishWith(activeGeneration, VoiceBackendState.Unavailable("音声認識を停止できません"))
            return
        }
        onState(partialState(), editorToken)
    }

    fun confirm(token: Long, index: Int = 0): String? {
        requireMainThread()
        if (token != editorToken) return null
        val result = preview?.getOrNull(index) ?: return null
        invalidate(destroy = true)
        onState(VoiceBackendState.Idle, token)
        return result
    }

    fun cancel(notify: Boolean = true) {
        requireMainThread()
        val token = editorToken
        invalidate(destroy = true)
        if (notify) onState(VoiceBackendState.Idle, token)
    }

    fun destroy() {
        requireMainThread()
        invalidate(destroy = true)
    }

    private fun deliver(activeGeneration: Long, state: VoiceBackendState) {
        if (activeGeneration == generation) onState(state, editorToken)
    }

    private fun finishWith(activeGeneration: Long, state: VoiceBackendState) {
        if (activeGeneration != generation) return
        val token = editorToken
        invalidate(destroy = true)
        onState(state, token)
    }

    private fun invalidate(destroy: Boolean) {
        generation++
        clearEndOfSpeechFallback()
        partial = null
        preview = null
        recognizer?.let {
            runCatching { it.cancel() }
            if (destroy) runCatching { it.destroy() }
        }
        recognizer = null
    }

    private fun requireMainThread() {
        check(Looper.myLooper() == Looper.getMainLooper()) { "Voice recognition must run on the main thread" }
    }

    private fun partialState(): VoiceBackendState =
        partial?.let(VoiceBackendState::Partial) ?: VoiceBackendState.Recognizing

    private fun scheduleEndOfSpeechFallback(activeGeneration: Long) {
        clearEndOfSpeechFallback()
        if (partial.isNullOrBlank()) return
        endOfSpeechFallback = Runnable {
            endOfSpeechFallback = null
            if (activeGeneration != generation || preview != null) return@Runnable
            val candidate = partial?.takeIf(String::isNotBlank) ?: return@Runnable
            preview = listOf(candidate)
            deliver(activeGeneration, VoiceBackendState.Preview(listOf(candidate)))
        }.also { mainHandler.postDelayed(it, END_OF_SPEECH_GRACE_MS) }
    }

    private fun clearEndOfSpeechFallback() {
        endOfSpeechFallback?.let(mainHandler::removeCallbacks)
        endOfSpeechFallback = null
    }

    private fun errorMessage(error: Int) = when (error) {
        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED, SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE ->
            "日本語の端末内音声モデルがありません"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "マイクの許可が必要です"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "音声認識が処理中です"
        else -> "音声を認識できません（error $error）"
    }
}

@RequiresApi(Build.VERSION_CODES.S)
private class AndroidVoiceRecognizer(
    context: Context,
    private val listener: VoiceRecognizerListener,
) : VoiceRecognizer, RecognitionListener {
    private val context = context.applicationContext
    private val recognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(context).also { it.setRecognitionListener(this) }
    private val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ja-JP")
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
    }

    override fun checkJapaneseSupport(callback: (Boolean?) -> Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) { callback(null); return }
        recognizer.checkRecognitionSupport(intent, context.mainExecutor, object : RecognitionSupportCallback {
            override fun onSupportResult(support: RecognitionSupport) {
                callback(support.installedOnDeviceLanguages.any { it.equals("ja", true) || it.startsWith("ja-", true) })
            }
            override fun onError(error: Int) = callback(null)
        })
    }

    override fun start() = recognizer.startListening(intent)
    override fun stop() = recognizer.stopListening()
    override fun cancel() = recognizer.cancel()
    override fun destroy() = recognizer.destroy()
    override fun onReadyForSpeech(params: Bundle?) = listener.onReady()
    override fun onEndOfSpeech() = listener.onEndOfSpeech()
    override fun onResults(results: Bundle?) = listener.onResults(
        results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty(),
    )
    override fun onError(error: Int) = listener.onError(error)
    override fun onBeginningOfSpeech() = Unit
    override fun onRmsChanged(rmsdB: Float) = Unit
    override fun onBufferReceived(buffer: ByteArray?) = Unit
    override fun onPartialResults(partialResults: Bundle?) = listener.onPartialResults(
        partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty(),
    )
    override fun onEvent(eventType: Int, params: Bundle?) = Unit
}
