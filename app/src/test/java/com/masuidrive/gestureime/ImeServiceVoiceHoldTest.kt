package com.masuidrive.gestureime

import android.content.ClipboardManager
import android.view.View
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import com.masuidrive.gestureime.conversion.*
import com.masuidrive.gestureime.keyboard.VoiceHoldEvent
import com.masuidrive.gestureime.voice.*
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.*
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class) @Config(sdk=[35])
class ImeServiceVoiceHoldTest {
    @Test fun earlyResultWaitsForReleaseAndCommitsOnce() {
        val h = Harness(); h.begin(); h.recognizer.result("日本語"); assertEquals("", h.input.text)
        h.service.onVoiceHold(VoiceHoldEvent.End(1)); h.idle(); assertEquals("日本語", h.input.text)
        h.service.onVoiceHold(VoiceHoldEvent.End(1)); h.idle(); assertEquals("日本語", h.input.text)
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
    private class Harness(private val ready:Boolean=true) {
        val controller=Robolectric.buildService(ImeService::class.java).create(); val service=controller.get()
        val input=RecordingConnection(View(RuntimeEnvironment.getApplication())); val recognizer=FakeRecognizer()
        init { val text=TextInputController({input},service,service.getSystemService(ClipboardManager::class.java)); val voice=VoiceRecognitionController(35,{true},{true},{ l->recognizer.listener=l;recognizer },service::onVoiceState); service.installTestDependencies(voice,text,FakeConversion()); service.onStartInput(EditorInfo(),false); service.onCreateInputView() }
        fun begin(){ service.onVoiceHold(VoiceHoldEvent.Begin(1)); idle(); recognizer.support?.invoke(true); if(ready) recognizer.ready(); idle() }
        fun idle()=Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
    }
    private class RecordingConnection(v:View):BaseInputConnection(v,true){ var text=""; override fun commitText(t:CharSequence?,n:Int):Boolean { text+=t?.toString() ?: ""; return true } }
    private class FakeRecognizer:VoiceRecognizer { var listener:VoiceRecognizerListener?=null; var support:((Boolean?)->Unit)?=null; override fun checkJapaneseSupport(c:(Boolean?)->Unit){support=c}; override fun start(){}; override fun stop(){}; override fun cancel(){}; override fun destroy(){}; fun ready()=listener?.onReady(); fun result(s:String)=listener?.onResults(listOf(s)) }
    private class FakeConversion:ConversionEngine { override suspend fun start(reading:String)=ConversionState(reading, emptyList(),-1); override suspend fun update(reading:String)=start(reading); override suspend fun nextCandidate()=start(""); override suspend fun commit(index:Int)=null; override suspend fun reset(){} }
}
