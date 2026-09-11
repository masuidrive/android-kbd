package com.masuidrive.gestureime

import android.os.Build
import android.content.Intent
import android.speech.RecognitionSupport
import android.speech.RecognitionSupportCallback
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class VoiceRecognitionAvailabilityTest {
    @Test
    fun reportsOnDeviceRecognizerAvailability() {
        assertTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        val available = SpeechRecognizer.isOnDeviceRecognitionAvailable(ApplicationProvider.getApplicationContext())
        Log.i("VoiceAvailability", "onDevice=$available sdk=${Build.VERSION.SDK_INT}")
    }

    @Test
    fun reportsInstalledJapaneseModelSupport() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        lateinit var recognizer: SpeechRecognizer
        val completed = CountDownLatch(1)
        var installed = emptyList<String>()
        var pending = emptyList<String>()
        var supported = emptyList<String>()
        var supportError: Int? = null
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ja-JP")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            recognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            recognizer.checkRecognitionSupport(intent, context.mainExecutor, object : RecognitionSupportCallback {
                override fun onSupportResult(support: RecognitionSupport) {
                    installed = support.installedOnDeviceLanguages
                    pending = support.pendingOnDeviceLanguages
                    supported = support.supportedOnDeviceLanguages
                    completed.countDown()
                }

                override fun onError(error: Int) {
                    supportError = error
                    completed.countDown()
                }
            })
        }
        assertTrue(completed.await(10, TimeUnit.SECONDS))
        Log.i("VoiceAvailability", "installed=$installed pending=$pending supported=$supported supportError=$supportError")
        InstrumentationRegistry.getInstrumentation().runOnMainSync { recognizer.destroy() }
    }
}
