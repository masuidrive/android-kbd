package com.masuidrive.gestureime

import android.content.ClipboardManager
import android.view.View
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import com.masuidrive.gestureime.conversion.*
import com.masuidrive.gestureime.keyboard.VoiceHoldEvent
import com.masuidrive.gestureime.keyboard.KeyAction
import com.masuidrive.gestureime.keyboard.KeyboardMode
import com.masuidrive.gestureime.keyboard.KeyboardUiState
import com.masuidrive.gestureime.keyboard.KeyboardView
import com.masuidrive.gestureime.voice.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.*
import org.robolectric.annotation.Config
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking

@RunWith(RobolectricTestRunner::class) @Config(sdk=[35])
class ImeServiceVoiceHoldTest {
    @Test fun voiceLayerShowsMultipleCandidatesAndCommitsTappedChoiceThenRestarts() {
        val h = Harness()
        h.service.onKeyAction(KeyAction.VoiceHold)
        h.idle()
        h.recognizer.support?.invoke(true)
        h.recognizer.ready()
        h.recognizer.result("第一候補", "第二候補", "第三候補")
        h.idle()

        assertEquals("", h.input.text)
        assertTrue(h.root.allText().containsAll(listOf("第一候補", "第二候補", "第三候補")))
        h.root.findText("第二候補").performClick()
        h.idle()

        assertEquals("第二候補", h.input.text)
        assertEquals("音声キーボード", h.root.findKeyboard().contentDescription)
        assertTrue(!h.root.allText().contains("第一候補"))
        h.recognizer.support?.invoke(true)
        h.recognizer.ready()
        h.recognizer.result("次の候補")
        h.idle()
        assertTrue(h.root.allText().contains("次の候補"))
    }

    @Test fun voiceCandidateCanCommitTwiceThenCancelDropsOldCallbackAndRestoresLayer() {
        val h = Harness()
        h.service.onKeyAction(KeyAction.VoiceHold); h.idle()
        h.recognizer.support?.invoke(true); h.recognizer.ready(); h.recognizer.result("一回目"); h.idle()
        h.root.findText("一回目").performClick(); h.idle()
        h.recognizer.support?.invoke(true); h.recognizer.ready(); h.recognizer.result("二回目"); h.idle()
        h.root.findText("二回目").performClick(); h.idle()

        assertEquals("一回目二回目", h.input.text)
        assertEquals(KeyboardMode.VOICE, h.root.findKeyboard().mode())
        assertEquals(2, h.recognizer.startCount)

        val stale = h.recognizer.listener
        h.service.onKeyAction(KeyAction.CancelVoice); h.idle()
        stale?.onResults(listOf("破棄")); h.idle()

        assertEquals("一回目二回目", h.input.text)
        assertEquals(KeyboardMode.QWERTY, h.root.findKeyboard().mode())
        assertEquals(2, h.recognizer.startCount)
    }

    @Test fun editorChangeAndErrorStopContinuousVoiceWithoutStatusOrRestart() {
        val h = Harness()
        h.service.onKeyAction(KeyAction.VoiceHold); h.idle()
        h.recognizer.support?.invoke(true); h.recognizer.ready(); h.idle()
        assertTrue(h.root.findKeyboard().voiceSessionActive())
        val stale = h.recognizer.listener

        h.service.onStartInput(EditorInfo(), false)
        stale?.onResults(listOf("別editor")); h.idle()
        assertEquals("", h.input.text)
        assertEquals(KeyboardMode.QWERTY, h.root.findKeyboard().mode())
        assertEquals(1, h.recognizer.startCount)

        h.service.onKeyAction(KeyAction.VoiceHold); h.idle()
        h.recognizer.support?.invoke(true); h.recognizer.error(android.speech.SpeechRecognizer.ERROR_NO_MATCH); h.idle()
        assertTrue(!h.root.findKeyboard().voiceSessionActive())
        assertEquals(2, h.recognizer.startCount)
    }

    @Test fun rapidDoubleTapOfOneVoicePreviewCommitsAndRestartsOnlyOnce() {
        val h = Harness()
        h.service.onKeyAction(KeyAction.VoiceHold); h.idle()
        h.recognizer.support?.invoke(true); h.recognizer.result("一回だけ"); h.idle()
        val candidate = h.root.findText("一回だけ")

        candidate.performClick(); candidate.performClick(); h.idle()
        h.recognizer.support?.invoke(true); h.idle()

        assertEquals("一回だけ", h.input.text)
        assertEquals(2, h.recognizer.startCount)
        assertEquals(KeyboardMode.VOICE, h.root.findKeyboard().mode())
    }

    @Test fun queuedVoicePreviewTapCannotCommitOrRestartAfterCancelInvalidatesItsSession() {
        val h = Harness()
        h.service.onKeyAction(KeyAction.VoiceHold); h.idle()
        h.recognizer.support?.invoke(true); h.recognizer.result("古い候補"); h.idle()
        val mutex = ImeService::class.java.getDeclaredField("actionMutex").apply { isAccessible = true }.get(h.service) as kotlinx.coroutines.sync.Mutex
        runBlocking { mutex.lock() }
        try {
            h.service.onKeyAction(KeyAction.SelectCandidate(0))
            h.service.onKeyAction(KeyAction.CancelVoice)
        } finally {
            mutex.unlock()
        }
        h.idle()

        assertEquals("", h.input.text)
        assertEquals(1, h.recognizer.startCount)
        assertEquals(KeyboardMode.QWERTY, h.root.findKeyboard().mode())
    }

    @Test fun earlyResultWaitsForReleaseAndCommitsOnce() {
        val h = Harness(); h.begin(); h.recognizer.result("日本語"); assertEquals("", h.input.text)
        h.service.onVoiceHold(VoiceHoldEvent.End(1)); h.idle(); assertEquals("日本語", h.input.text)
        h.service.onVoiceHold(VoiceHoldEvent.End(1)); h.idle(); assertEquals("日本語", h.input.text)
    }
    @Test fun partialResultIsVisibleButNeverCommittedAndCancelRestoresInitialUi() {
        val h=Harness(); h.begin(); h.recognizer.partial("途中の文"); h.idle()
        assertEquals("",h.input.text)
        assertEquals("途中の文",h.root.allText().single { it == "途中の文" })
        h.service.onVoiceHold(VoiceHoldEvent.Cancel(1)); h.idle()
        assertEquals("",h.input.text)
        assertEquals(false,h.root.allText().contains("途中の文"))
    }
    @Test fun unavailableAfterPartialClearsTheStalePartialCandidate() {
        val h=Harness(); h.service.onKeyAction(KeyAction.VoiceHold); h.idle()
        h.recognizer.support?.invoke(true); h.recognizer.ready(); h.recognizer.partial("古い途中結果"); h.idle()
        assertTrue(h.root.allText().contains("古い途中結果"))

        h.recognizer.error(android.speech.SpeechRecognizer.ERROR_NO_MATCH); h.idle()

        assertEquals(false, h.root.allText().contains("古い途中結果"))
        assertTrue(h.root.allText().contains("非対応"))
    }
    @Test fun releaseAfterPartialWaitsForFinalAndCommitsOnlyFinalText() {
        val h=Harness(); h.begin(); h.recognizer.partial("途中"); h.service.onVoiceHold(VoiceHoldEvent.End(1)); h.idle()
        assertEquals("",h.input.text)
        h.recognizer.result("最終結果"); h.idle()
        assertEquals("最終結果",h.input.text)
    }
    @Test fun releaseThenLateResultCommitsAndCancelDropsIt() {
        val h=Harness(); h.begin(); h.service.onVoiceHold(VoiceHoldEvent.End(1)); h.recognizer.result("後"); h.idle(); assertEquals("後",h.input.text)
        val h2=Harness(); h2.begin(); h2.service.onVoiceHold(VoiceHoldEvent.Cancel(1)); h2.recognizer.result("破棄"); h2.idle(); assertEquals("",h2.input.text)
    }
    @Test fun releaseBeforeReadyAndEditorSwitchDropResults() {
        val h=Harness(ready=false); h.begin(); h.service.onVoiceHold(VoiceHoldEvent.End(1)); h.recognizer.ready(); h.recognizer.result("早い"); h.idle(); assertEquals("",h.input.text)
        val h2=Harness(); h2.begin(); h2.service.onStartInput(EditorInfo(),false); h2.recognizer.result("別欄"); h2.idle(); assertEquals("",h2.input.text)
    }
    @Test fun privateEditorNeverStartsVoice() {
        val h=Harness(); h.service.onStartInput(EditorInfo().apply { inputType=android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD },false)
        h.service.onVoiceHold(VoiceHoldEvent.Begin(9)); h.idle(); assertEquals(null,h.recognizer.support)
    }
    @Test fun normalKeyCancelsVoiceCommitWaitingBehindReset() {
        val h=Harness(); h.begin(); h.recognizer.result("古い"); h.conversion.armReset(); h.service.onVoiceHold(VoiceHoldEvent.End(1)); h.idle(); assertEquals("",h.input.text)
        h.service.onKeyAction(KeyAction.CommitText("x")); h.conversion.releaseReset(); h.idle(); assertEquals("x",h.input.text)
    }
    private class Harness(private val ready:Boolean=true) {
        val controller=Robolectric.buildService(ImeService::class.java).create(); val service=controller.get()
        val input=RecordingConnection(View(RuntimeEnvironment.getApplication())); val recognizer=FakeRecognizer(); val conversion=FakeConversion(); lateinit var root:View
        init { ImePreferences.setEnglishSuggestionsEnabled(service, false); val text=TextInputController({input},service,service.getSystemService(ClipboardManager::class.java)); val voice=VoiceRecognitionController(35,{true},{true},{ l->recognizer.listener=l;recognizer },service::onVoiceState); service.installTestDependencies(voice,text,conversion); service.onStartInput(EditorInfo(),false); root=service.onCreateInputView() }
        fun begin(){ service.onVoiceHold(VoiceHoldEvent.Begin(1)); idle(); recognizer.support?.invoke(true); if(ready) recognizer.ready(); idle() }
        fun idle()=Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
    }
    private class RecordingConnection(v:View):BaseInputConnection(v,true){ var text=""; override fun commitText(t:CharSequence?,n:Int):Boolean { text+=t?.toString() ?: ""; return true } }
    private class FakeRecognizer:VoiceRecognizer { var listener:VoiceRecognizerListener?=null; var support:((Boolean?)->Unit)?=null; var startCount=0; override fun checkJapaneseSupport(c:(Boolean?)->Unit){support=c}; override fun start(){startCount++}; override fun stop(){}; override fun cancel(){}; override fun destroy(){}; fun ready()=listener?.onReady(); fun partial(s:String)=listener?.onPartialResults(listOf(s)); fun result(vararg s:String)=listener?.onResults(s.toList()); fun error(code:Int)=listener?.onError(code) }
    private class FakeConversion:ConversionEngine { private var resetGate:CompletableDeferred<Unit>?=null; fun armReset(){resetGate=CompletableDeferred()}; fun releaseReset(){resetGate?.complete(Unit)}; override suspend fun start(reading:String)=ConversionState(reading, emptyList(),-1); override suspend fun update(reading:String)=start(reading); override suspend fun nextCandidate()=start(""); override suspend fun commit(index:Int)=null; override suspend fun reset(){ resetGate?.await() } }

    private fun View.allText():List<String> { val result=mutableListOf<String>(); fun visit(v:View){ if(v is android.widget.TextView) result+=v.text.toString(); if(v is android.view.ViewGroup) repeat(v.childCount){visit(v.getChildAt(it))} }; visit(this); return result }
    private fun View.findText(text:String):android.widget.TextView { if(this is android.widget.TextView && this.text.toString()==text)return this; if(this is android.view.ViewGroup)repeat(childCount){runCatching{return getChildAt(it).findText(text)}}; error("missing $text") }
    private fun View.findKeyboard():com.masuidrive.gestureime.keyboard.KeyboardView { if(this is com.masuidrive.gestureime.keyboard.KeyboardView)return this; if(this is android.view.ViewGroup)repeat(childCount){runCatching{return getChildAt(it).findKeyboard()}}; error("missing keyboard") }
    private fun KeyboardView.mode(): KeyboardMode = (KeyboardView::class.java.getDeclaredField("state").apply { isAccessible = true }.get(this) as KeyboardUiState).mode
    private fun KeyboardView.voiceSessionActive(): Boolean = (KeyboardView::class.java.getDeclaredField("state").apply { isAccessible = true }.get(this) as KeyboardUiState).voiceSessionActive
}
