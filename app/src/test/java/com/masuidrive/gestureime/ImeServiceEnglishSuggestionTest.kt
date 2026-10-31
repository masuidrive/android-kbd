package com.masuidrive.gestureime

import android.content.ClipboardManager
import android.app.Activity
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.view.MotionEvent
import android.widget.TextView
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedText
import android.view.inputmethod.ExtractedTextRequest
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
import com.masuidrive.gestureime.ui.CandidateStripView
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
    fun explicitPostalAddressShowsEachSavedPartForSequentialSelection() {
        val context = RuntimeEnvironment.getApplication()
        clearPersonalInfo(context)
        ImePreferences.setPersonalInfo(context, PersonalInfoField.POSTAL_CODE, "1000001")
        ImePreferences.setPersonalInfo(context, PersonalInfoField.PREFECTURE, "東京都")
        ImePreferences.setPersonalInfo(context, PersonalInfoField.CITY, "千代田区")
        ImePreferences.setPersonalInfo(context, PersonalInfoField.ADDRESS_LINE1, "千代田1-1")
        try {
            val harness = Harness { _, _ -> emptyList() }
            harness.service.onStartInput(EditorInfo().apply {
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS
            }, false)
            harness.idle()
            listOf("1000001", "東京都", "千代田区", "千代田1-1").forEachIndexed { index, value ->
                assertTrue(harness.root.findView { it.contentDescription?.toString() == "候補 ${index + 1}: $value" } != null)
            }
            harness.root.findView { it.contentDescription?.toString() == "候補 2: 東京都" }!!.performClick()
            harness.idle()
            assertEquals("東京都", harness.input.committed)
            assertTrue(harness.root.findView { it.contentDescription?.toString() == "候補 3: 千代田区" } != null)
            harness.root.findView { it.contentDescription?.toString() == "候補 3: 千代田区" }!!.performClick()
            harness.idle()
            assertEquals("東京都千代田区", harness.input.committed)
        } finally { clearPersonalInfo(context) }
    }

    @Test
    fun explicitCandidateRemainsVisibleWhileInlineProviderHasNoInflatedView() {
        val context = RuntimeEnvironment.getApplication()
        clearPersonalInfo(context)
        ImePreferences.setPersonalInfo(context, PersonalInfoField.PREFECTURE, "東京都")
        try {
            val harness = Harness { _, _ -> emptyList() }
            harness.service.onStartInput(EditorInfo().apply {
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS
            }, false)
            harness.idle()
            val inline = ImeService::class.java.getDeclaredField("inlineSuggestionController").apply {
                isAccessible = true
            }.get(harness.service) as InlineSuggestionController
            inline.beginResponse()
            ImeService::class.java.getDeclaredMethod("showCandidateState").apply {
                isAccessible = true
            }.invoke(harness.service)
            assertTrue(harness.root.findView { it.contentDescription?.toString() == "候補 1: 東京都" } != null)
        } finally { clearPersonalInfo(context) }
    }

    @Test
    fun personalInfoPickerStaysOpenForSeparateAddressPartTaps() {
        val context = RuntimeEnvironment.getApplication()
        clearPersonalInfo(context)
        ImePreferences.setPersonalInfo(context, PersonalInfoField.PREFECTURE, "東京都")
        ImePreferences.setPersonalInfo(context, PersonalInfoField.CITY, "千代田区")
        try {
            val harness = Harness { _, _ -> emptyList() }
            harness.service.onStartInput(EditorInfo().apply { inputType = InputType.TYPE_CLASS_TEXT }, false)
            harness.idle()
            harness.root.findView { it.contentDescription?.toString() == "登録した個人情報を選ぶ" }!!.performClick()
            harness.idle()
            assertEquals(null, harness.root.findView { it.contentDescription?.toString() == "個人情報 1: 都道府県: 東京都" })
            harness.root.findView { it.contentDescription?.toString() == "その他の個人情報を表示" }!!.performClick()
            harness.idle()
            harness.root.findView { it.contentDescription?.toString() == "個人情報 1: 都道府県: 東京都" }!!.performClick()
            harness.idle()
            assertEquals("東京都", harness.input.committed)
            harness.root.findView { it.contentDescription?.toString() == "個人情報 2: 市区町村: 千代田区" }!!.performClick()
            harness.idle()
            assertEquals("東京都千代田区", harness.input.committed)
            assertTrue(harness.root.findView { it.contentDescription?.toString() == "個人情報の選択を戻る" } != null)
        } finally { clearPersonalInfo(context) }
    }

    @Test
    fun pickerBottomRowEditsWithoutClosingTheValueList() {
        val context = RuntimeEnvironment.getApplication()
        clearPersonalInfo(context)
        ImePreferences.setPersonalInfo(context, PersonalInfoField.PREFECTURE, "東京都")
        try {
            val harness = Harness { _, _ -> emptyList() }
            harness.service.onStartInput(EditorInfo().apply { inputType = InputType.TYPE_CLASS_TEXT }, false)
            harness.idle()
            harness.root.findView { it.contentDescription?.toString() == "登録した個人情報を選ぶ" }!!.performClick()
            harness.idle()
            harness.root.findView { it.contentDescription?.toString() == "その他の個人情報を表示" }!!.performClick()
            harness.idle()
            harness.root.findView { it.contentDescription?.toString() == "個人情報 1: 都道府県: 東京都" }!!.performClick()
            harness.idle()
            harness.root.findView { it.contentDescription?.toString() == "Space。上下左右フリックでカーソル移動" }!!.performClick()
            harness.idle()
            assertEquals("東京都 ", harness.input.committed)
            assertTrue(harness.root.findView { it.contentDescription?.toString() == "個人情報 1: 都道府県: 東京都" } != null)
        } finally { clearPersonalInfo(context) }
    }

    @Test
    fun pickerPunctuationFlickCommitsTheSelectedCharacterWithSnippetsVisible() {
        val context = RuntimeEnvironment.getApplication()
        clearPersonalInfo(context)
        ImePreferences.setPersonalInfo(context, PersonalInfoField.EMAIL, "name@example.com")
        ImePreferences.setTextSnippets(context, listOf("登録キーワード"))
        try {
            val harness = Harness { _, _ -> emptyList() }
            harness.service.onStartInput(EditorInfo().apply { inputType = InputType.TYPE_CLASS_TEXT }, false)
            harness.idle()
            harness.root.findView { it.contentDescription?.toString() == "登録した個人情報を選ぶ" }!!.performClick()
            harness.idle()
            harness.root.findView { it.contentDescription?.toString() == "その他の個人情報を表示" }!!.performClick()
            harness.idle()
            assertTrue(harness.root.findView { it.contentDescription?.toString() == "個人情報 1: 定型文: 登録キーワード" } != null)
            val key = harness.root.findView {
                it.contentDescription?.toString() == "タップで、。左フリックで。上フリックで？右フリックで！"
            }!!
            fun flick(dx: Float, dy: Float) {
                key.dispatchTouchEvent(MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 50f, 50f, 0))
                key.dispatchTouchEvent(MotionEvent.obtain(0, 25, MotionEvent.ACTION_MOVE, 50f + dx, 50f + dy, 0))
                key.dispatchTouchEvent(MotionEvent.obtain(0, 50, MotionEvent.ACTION_UP, 50f + dx, 50f + dy, 0))
                harness.idle()
            }
            flick(-50f, 0f)
            flick(0f, -50f)
            flick(50f, 0f)
            flick(0f, 50f)
            assertEquals("。？！、", harness.input.committed)
        } finally { clearPersonalInfo(context) }
    }

    @Test
    fun explicitNameFieldRestoresAllSavedSpellingsAfterInputViewReappears() {
        val context = RuntimeEnvironment.getApplication()
        clearPersonalInfo(context)
        ImePreferences.setPersonalInfo(context, PersonalInfoField.NAME, "山田　太郎")
        ImePreferences.setPersonalInfo(context, PersonalInfoField.NAME_HIRAGANA, "やまだ  たろう")
        ImePreferences.setPersonalInfo(context, PersonalInfoField.NAME_KATAKANA, "ヤマダ タロウ")
        ImePreferences.setPersonalInfo(context, PersonalInfoField.NAME_LATIN, "Taro Yamada")
        try {
            val harness = Harness { _, _ -> emptyList() }
            val editor = EditorInfo().apply {
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PERSON_NAME
            }
            harness.service.onStartInput(editor, false)
            harness.service.onStartInputView(editor, false)
            harness.idle()
            listOf("山田", "太郎", "やまだ", "たろう", "ヤマダ", "タロウ", "Taro", "Yamada").forEachIndexed { index, value ->
                assertTrue(harness.root.findView { it.contentDescription?.toString() == "候補 ${index + 1}: $value" } != null)
            }
            harness.service.onFinishInputView(false)
            harness.service.onStartInputView(editor, true)
            harness.idle()
            assertTrue(harness.root.findView { it.contentDescription?.toString() == "候補 1: 山田" } != null)
        } finally { clearPersonalInfo(context) }
    }

    @Test
    fun explicitFieldRestoresCandidatesWhenRecreatedViewHasAnEmptyPersonalSnapshot() {
        val context = RuntimeEnvironment.getApplication()
        clearPersonalInfo(context)
        ImePreferences.setPersonalInfo(context, PersonalInfoField.NAME, "山田 太郎")
        try {
            val harness = Harness { _, _ -> emptyList() }
            val editor = EditorInfo().apply {
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PERSON_NAME
            }
            harness.service.onStartInput(editor, false)
            harness.idle()
            ImeService::class.java.getDeclaredField("candidates").apply { isAccessible = true }
                .set(harness.service, emptyList<String>())
            harness.service.onStartInputView(editor, true)
            harness.idle()
            assertTrue(harness.root.findView { it.contentDescription?.toString() == "候補 1: 山田" } != null)
        } finally { clearPersonalInfo(context) }
    }

    @Test
    fun delayedPreviousEditorResetCannotEraseTheNewNameCandidate() {
        val context = RuntimeEnvironment.getApplication()
        clearPersonalInfo(context)
        ImePreferences.setPersonalInfo(context, PersonalInfoField.NAME, "山田 太郎")
        try {
            val resetGate = CompletableDeferred<Unit>()
            val harness = Harness(conversion = FakeConversion(resetGate = resetGate)) { _, _ -> emptyList() }
            val editor = EditorInfo().apply {
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PERSON_NAME
            }
            harness.service.onStartInput(editor, false)
            harness.idle()
            assertTrue(harness.root.findView { it.contentDescription?.toString() == "候補 1: 山田" } != null)

            resetGate.complete(Unit)
            harness.idle()
            assertTrue(harness.root.findView { it.contentDescription?.toString() == "候補 1: 山田" } != null)
        } finally { clearPersonalInfo(context) }
    }

    @Test
    fun explicitPersonalInfoFieldShowsItsSavedValueAndCommitsOnlyAfterTap() {
        val context = RuntimeEnvironment.getApplication()
        clearPersonalInfo(context)
        ImePreferences.setPersonalInfo(context, PersonalInfoField.EMAIL, "name@example.com")
        try {
            val harness = Harness { _, _ -> emptyList() }
            harness.service.onStartInput(EditorInfo().apply {
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            }, false)
            harness.idle()

            assertEquals("", harness.input.committed)
            assertEquals(View.VISIBLE, harness.root.findView {
                it.contentDescription?.toString() == "登録した個人情報を選ぶ"
            }!!.visibility)
            harness.root.findView { it.contentDescription?.toString() == "候補 1: name@example.com" }!!.performClick()
            harness.idle()
            assertEquals("name@example.com", harness.input.committed)
            assertTrue(harness.root.findView {
                it.contentDescription?.toString() == "候補 1: name@example.com"
            } != null)
        } finally {
            clearPersonalInfo(context)
        }
    }

    @Test
    fun fourSavedEmailsAppearSeparatelyInEmailFieldAndPickerStartsWithEmails() {
        val context = RuntimeEnvironment.getApplication()
        clearPersonalInfo(context)
        val emails = listOf("one@example.com", "two@example.com", "three@example.com", "four@example.com")
        emails.forEachIndexed { index, email -> ImePreferences.setPersonalInfoEmail(context, index, email) }
        ImePreferences.setPersonalInfo(context, PersonalInfoField.NAME, "山田 太郎")
        try {
            val harness = Harness { _, _ -> emptyList() }
            harness.service.onStartInput(EditorInfo().apply {
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            }, false)
            harness.idle()
            emails.forEachIndexed { index, email ->
                assertTrue(harness.root.findView { it.contentDescription?.toString() == "候補 ${index + 1}: $email" } != null)
            }
            harness.root.findView { it.contentDescription?.toString() == "登録した個人情報を選ぶ" }!!.performClick()
            harness.idle()
            emails.forEachIndexed { index, email ->
                assertTrue(harness.root.findView { it.contentDescription?.toString() ==
                    "個人情報 ${index + 1}: メールアドレス: $email" } != null)
            }
            assertEquals(null, harness.root.findView { it.contentDescription?.toString() == "個人情報 1: 氏名（漢字）（姓）: 山田" })
            harness.root.findView { it.contentDescription?.toString() == "その他の個人情報を表示" }!!.performClick()
            harness.idle()
            assertTrue(harness.root.findView { it.contentDescription?.toString()?.contains("山田") == true } != null)
        } finally { clearPersonalInfo(context) }
    }

    @Test
    fun registeredEmailRemainsSelectableAlongsideAnInlineProviderView() {
        val context = RuntimeEnvironment.getApplication()
        clearPersonalInfo(context)
        ImePreferences.setPersonalInfo(context, PersonalInfoField.EMAIL, "name@example.com")
        try {
            val harness = Harness { _, _ -> emptyList() }
            val editor = EditorInfo().apply {
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            }
            harness.service.onStartInput(editor, false)
            harness.idle()
            val strip = harness.root.findView { it is CandidateStripView } as CandidateStripView
            val providerView = TextView(context).apply { text = "protected provider view" }
            strip.showInlineSuggestionViews(listOf(providerView))
            ImeService::class.java.getDeclaredMethod("showCandidateState").apply { isAccessible = true }
                .invoke(harness.service)

            assertEquals(strip, providerView.parent?.parent?.parent)
            harness.root.findView { it.contentDescription?.toString() == "候補 1: name@example.com" }!!
                .performClick()
            harness.idle()
            assertEquals("name@example.com", harness.input.committed)
        } finally { clearPersonalInfo(context) }
    }

    @Test
    fun genericFieldUsesPersonalInfoActionAndDoesNotListEmptyFields() {
        val context = RuntimeEnvironment.getApplication()
        clearPersonalInfo(context)
        ImePreferences.setPersonalInfo(context, PersonalInfoField.NAME, "山田 太郎")
        ImePreferences.setPersonalInfo(context, PersonalInfoField.ADDRESS, "東京都千代田区")
        try {
            val harness = Harness { _, _ -> emptyList() }
            harness.service.onStartInput(EditorInfo().apply { inputType = InputType.TYPE_CLASS_TEXT }, false)
            harness.idle()

            assertEquals(null, harness.root.findView { it.contentDescription?.toString() == "候補 1: 山田 太郎" })
            harness.root.findView { it.contentDescription?.toString() == "登録した個人情報を選ぶ" }!!.performClick()
            harness.idle()
            harness.root.findView { it.contentDescription?.toString() == "その他の個人情報を表示" }!!.performClick()
            harness.idle()
            assertEquals(null, harness.root.findView { it.contentDescription?.toString() == "候補 2: " })
            harness.root.findView { it.contentDescription?.toString() == "個人情報 3: 住所1: 東京都千代田区" }!!.performClick()
            harness.idle()
            assertEquals("東京都千代田区", harness.input.committed)
        } finally {
            clearPersonalInfo(context)
        }
    }

    @Test
    fun genericFieldPickerIncludesSavedSnippetWithoutAVisibleCategoryLabel() {
        val context = RuntimeEnvironment.getApplication()
        clearPersonalInfo(context)
        ImePreferences.setTextSnippets(context, listOf("いつもありがとうございます。"))
        try {
            val harness = Harness { _, _ -> emptyList() }
            harness.service.onStartInput(EditorInfo().apply { inputType = InputType.TYPE_CLASS_TEXT }, false)
            harness.idle()
            harness.root.findView { it.contentDescription?.toString() == "登録した個人情報を選ぶ" }!!.performClick()
            harness.idle()
            val chip = harness.root.findView {
                it.contentDescription?.toString() == "個人情報 1: 定型文: いつもありがとうございます。"
            }!! as android.widget.TextView
            assertEquals(View.INVISIBLE, harness.root.findView {
                it.contentDescription?.toString() == "その他の個人情報を表示"
            }!!.visibility)
            assertEquals("いつもありがとうございます。", chip.text.toString())
            chip.performClick()
            harness.idle()
            assertEquals("いつもありがとうございます。", harness.input.committed)
        } finally {
            clearPersonalInfo(context)
        }
    }

    @Test
    fun personalInfoPickerBackReturnsWithoutCommitting() {
        val context = RuntimeEnvironment.getApplication()
        clearPersonalInfo(context)
        ImePreferences.setPersonalInfo(context, PersonalInfoField.NAME, "山田 太郎")
        try {
            val harness = Harness { _, _ -> emptyList() }
            harness.service.onStartInput(EditorInfo().apply { inputType = InputType.TYPE_CLASS_TEXT }, false)
            harness.idle()

            harness.root.findView { it.contentDescription?.toString() == "登録した個人情報を選ぶ" }!!.performClick()
            harness.idle()
            val back = harness.root.findView { it.contentDescription?.toString() == "個人情報の選択を戻る" }!!
            assertTrue(back.parent is com.masuidrive.gestureime.ui.CandidateStripView)
            back.performClick()
            harness.idle()

            assertEquals("", harness.input.committed)
            assertEquals(View.VISIBLE, harness.root.findView {
                it.contentDescription?.toString() == "登録した個人情報を選ぶ"
            }!!.visibility)
        } finally {
            clearPersonalInfo(context)
        }
    }

    @Test
    fun personalInfoFooterSpaceMovesVerticallyWithoutLeavingThePickerAndLayerTapReturnsToItsSource() {
        val context = RuntimeEnvironment.getApplication()
        clearPersonalInfo(context)
        ImePreferences.setPersonalInfo(context, PersonalInfoField.EMAIL, "name@example.com")
        try {
            val harness = Harness { _, _ -> emptyList() }
            harness.service.onStartInput(EditorInfo().apply { inputType = InputType.TYPE_CLASS_TEXT }, false)
            harness.service.onKeyAction(KeyAction.SwitchLayer(KeyboardMode.SYMBOLS))
            harness.idle()
            harness.input.commitText("aa\nbb\ncc", 1)
            harness.input.supportExtractedText = true
            harness.root.findView { it.contentDescription?.toString() == "登録した個人情報を選ぶ" }!!.performClick()
            harness.idle()
            val space = harness.root.findView { it.contentDescription?.toString()?.startsWith("Space。") == true }!!
            val density = space.resources.displayMetrics.density
            fun flick(dyDp: Float) {
                val x = space.width / 2f
                val y = space.height / 2f
                space.dispatchTouchEvent(MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, x, y, 0))
                space.dispatchTouchEvent(MotionEvent.obtain(0, 50, MotionEvent.ACTION_MOVE, x, y + dyDp * density, 0))
                space.dispatchTouchEvent(MotionEvent.obtain(0, 100, MotionEvent.ACTION_UP, x, y + dyDp * density, 0))
                harness.idle()
            }
            flick(-15f)
            assertEquals(5, harness.input.cursor)
            assertEquals("?}", harness.root.findView { it.contentDescription?.toString()?.startsWith("タップで記号へ戻る") == true }?.let { (it as TextView).text.toString() })
            flick(15f)
            assertEquals(8, harness.input.cursor)
            assertEquals("aa\nbb\ncc", harness.input.committed)

            harness.root.findView { it.contentDescription?.toString()?.startsWith("タップで記号へ戻る") == true }!!.performClick()
            harness.idle()
            assertEquals(View.VISIBLE, harness.root.findView {
                it.contentDescription?.toString() == "登録した個人情報を選ぶ"
            }!!.visibility)
            assertEquals(KeyboardMode.SYMBOLS, ImeService::class.java.getDeclaredField("keyboardMode")
                .apply { isAccessible = true }.get(harness.service))
        } finally { clearPersonalInfo(context) }
    }

    @Test
    fun personalInfoPickerKeepsTheInputViewHeightAtPhoneAndTabletWidths() {
        val context = RuntimeEnvironment.getApplication()
        clearPersonalInfo(context)
        ImePreferences.setPersonalInfo(context, PersonalInfoField.NAME, "山田 太郎")
        ImePreferences.setPersonalInfo(context, PersonalInfoField.EMAIL, "name@example.com")
        ImePreferences.setPersonalInfo(context, PersonalInfoField.ADDRESS, "東京都千代田区")
        ImePreferences.setTextSnippets(context, listOf("いつもありがとうございます。"))
        try {
            for (width in listOf(412, 840)) {
                val harness = Harness { _, _ -> emptyList() }
                harness.service.onStartInput(EditorInfo().apply { inputType = InputType.TYPE_CLASS_TEXT }, false)
                harness.idle()
                fun measuredHeight(): Int {
                    harness.root.measure(
                        View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(2000, View.MeasureSpec.AT_MOST),
                    )
                    harness.root.layout(0, 0, width, harness.root.measuredHeight)
                    return harness.root.measuredHeight
                }

                val initialHeight = measuredHeight()
                harness.root.findView { it.contentDescription?.toString() == "登録した個人情報を選ぶ" }!!.performClick()
                harness.idle()
                assertEquals(initialHeight, measuredHeight())
                assertTrue(harness.root.findView { it.contentDescription?.toString() == "個人情報 1: 定型文: いつもありがとうございます。" } != null)
                assertTrue(harness.root.findView { it.contentDescription?.toString() == "個人情報 2: メールアドレス: name@example.com" } != null)
                harness.root.findView { it.contentDescription?.toString() == "その他の個人情報を表示" }!!.performClick()
                harness.idle()
                assertEquals(initialHeight, measuredHeight())
                assertEquals(View.VISIBLE, harness.root.findView {
                    it.contentDescription?.toString() == "個人情報 1: 定型文: いつもありがとうございます。"
                }!!.visibility)

                harness.root.findView { it.contentDescription?.toString() == "個人情報の選択を戻る" }!!.performClick()
                harness.idle()
                assertEquals(initialHeight, measuredHeight())
                assertEquals("", harness.input.committed)
            }
        } finally {
            clearPersonalInfo(context)
        }
    }

    @Test
    fun hidingAndShowingWithInlineAutofillStillRestoresTheGenericCard() {
        val context = RuntimeEnvironment.getApplication()
        clearPersonalInfo(context)
        ImePreferences.setPersonalInfo(context, PersonalInfoField.NAME, "山田 太郎")
        try {
            val harness = Harness { _, _ -> emptyList() }
            harness.service.onStartInput(EditorInfo().apply { inputType = InputType.TYPE_CLASS_TEXT }, false)
            harness.idle()
            val inline = ImeService::class.java.getDeclaredField("inlineSuggestionController").apply {
                isAccessible = true
            }.get(harness.service) as InlineSuggestionController
            inline.beginResponse()

            harness.root.findView { it.contentDescription?.toString() == "登録した個人情報を選ぶ" }!!.performClick()
            harness.idle()
            harness.root.findView { it.contentDescription?.toString() == "その他の個人情報を表示" }!!.performClick()
            harness.idle()

            assertEquals(View.VISIBLE, harness.root.findView {
                it.contentDescription?.toString() == "個人情報 1: 氏名（姓）: 山田"
            }!!.visibility)
            harness.service.onFinishInputView(false)
            harness.service.onStartInputView(EditorInfo().apply { inputType = InputType.TYPE_CLASS_TEXT }, true)
            harness.idle()

            assertEquals(View.VISIBLE, harness.root.findView {
                it.contentDescription?.toString() == "登録した個人情報を選ぶ"
            }!!.visibility)
            assertEquals(null, harness.root.findView { it.contentDescription?.toString() == "候補 1: 山田 太郎" })
        } finally {
            clearPersonalInfo(context)
        }
    }

    @Test
    fun finishingAndSwitchingToPrivateClearsAnOpenPersonalInfoPicker() {
        val context = RuntimeEnvironment.getApplication()
        clearPersonalInfo(context)
        ImePreferences.setPersonalInfo(context, PersonalInfoField.NAME, "山田 太郎")
        try {
            val harness = Harness { _, _ -> emptyList() }
            harness.root.findView { it.contentDescription?.toString() == "登録した個人情報を選ぶ" }!!.performClick()
            harness.idle()
            harness.service.onFinishInputView(false)
            harness.service.onFinishInput()
            val privateInfo = EditorInfo().apply {
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            harness.service.onStartInput(privateInfo, false)
            harness.service.onStartInputView(privateInfo, false)
            harness.idle()

            assertEquals(null, harness.root.findView {
                it.contentDescription?.toString() == "個人情報 1: 氏名（姓）: 山田"
            })
            assertEquals(View.GONE, harness.root.findView {
                it.contentDescription?.toString() == "登録した個人情報を選ぶ"
            }!!.visibility)
        } finally {
            clearPersonalInfo(context)
        }
    }

    @Test
    fun hidingAndRestartingTheSameGenericInputViewRestoresCardWithoutPickerValues() {
        val context = RuntimeEnvironment.getApplication()
        clearPersonalInfo(context)
        ImePreferences.setPersonalInfo(context, PersonalInfoField.NAME, "山田 太郎")
        ImePreferences.setPersonalInfo(context, PersonalInfoField.ADDRESS, "東京都千代田区")
        try {
            val harness = Harness { _, _ -> emptyList() }
            val genericInfo = EditorInfo().apply { inputType = InputType.TYPE_CLASS_TEXT }
            harness.service.onStartInput(genericInfo, false)
            harness.idle()

            repeat(2) {
                harness.root.findView { it.contentDescription?.toString() == "登録した個人情報を選ぶ" }!!.performClick()
                harness.idle()

                harness.service.onFinishInputView(false)
                harness.service.onStartInputView(genericInfo, true)
                harness.idle()

                assertEquals(null, harness.root.findView { it.contentDescription?.toString() == "候補 1: 山田 太郎" })
                assertEquals(null, harness.root.findView { it.contentDescription?.toString() == "候補 2: 東京都千代田区" })
                assertEquals(View.VISIBLE, harness.root.findView {
                    it.contentDescription?.toString() == "登録した個人情報を選ぶ"
                }!!.visibility)
            }

            harness.root.findView {
                it.contentDescription?.toString() == "登録した個人情報を選ぶ"
            }!!.performClick()
            harness.idle()
            harness.root.findView { it.contentDescription?.toString() == "その他の個人情報を表示" }!!.performClick()
            harness.idle()
            harness.root.findView {
                it.contentDescription?.toString() == "個人情報 3: 住所1: 東京都千代田区"
            }!!.performClick()
            harness.idle()

            assertEquals("東京都千代田区", harness.input.committed)
        } finally {
            clearPersonalInfo(context)
        }
    }

    @Test
    fun hidingAndRestartingTheSameEmailInputViewRestoresOnlyItsAutomaticCandidate() {
        val context = RuntimeEnvironment.getApplication()
        clearPersonalInfo(context)
        ImePreferences.setPersonalInfo(context, PersonalInfoField.NAME, "山田 太郎")
        ImePreferences.setPersonalInfo(context, PersonalInfoField.EMAIL, "name@example.com")
        try {
            val harness = Harness { _, _ -> emptyList() }
            val emailInfo = EditorInfo().apply {
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            }
            harness.service.onStartInput(emailInfo, false)
            harness.idle()

            repeat(2) {
                harness.root.findView { it.contentDescription?.toString() == "登録した個人情報を選ぶ" }!!.performClick()
                harness.idle()

                harness.service.onFinishInputView(false)
                harness.service.onStartInputView(emailInfo, true)
                harness.idle()

                assertEquals(View.VISIBLE, harness.root.findView {
                    it.contentDescription?.toString() == "登録した個人情報を選ぶ"
                }!!.visibility)
                assertEquals(View.VISIBLE, harness.root.findView {
                    it.contentDescription?.toString() == "候補 1: name@example.com"
                }!!.visibility)
                assertEquals(null, harness.root.findView { it.contentDescription?.toString() == "候補 2: 山田 太郎" })
            }

            harness.root.findView { it.contentDescription?.toString() == "候補 1: name@example.com" }!!.performClick()
            harness.idle()

            assertEquals("name@example.com", harness.input.committed)
        } finally {
            clearPersonalInfo(context)
        }
    }

    @Test
    fun recreatingTheInputViewWhileThePickerIsOpenDoesNotPromotePickerValuesToCandidates() {
        val context = RuntimeEnvironment.getApplication()
        clearPersonalInfo(context)
        ImePreferences.setPersonalInfo(context, PersonalInfoField.NAME, "山田 太郎")
        ImePreferences.setPersonalInfo(context, PersonalInfoField.ADDRESS, "東京都千代田区")
        try {
            val harness = Harness { _, _ -> emptyList() }
            harness.service.onStartInput(EditorInfo().apply { inputType = InputType.TYPE_CLASS_TEXT }, false)
            harness.idle()
            harness.root.findView { it.contentDescription?.toString() == "登録した個人情報を選ぶ" }!!.performClick()
            harness.idle()

            val recreated = harness.service.onCreateInputView()
            harness.idle()

            assertEquals(null, recreated.findView { it.contentDescription?.toString() == "候補 1: 山田 太郎" })
            assertEquals(null, recreated.findView { it.contentDescription?.toString() == "候補 2: 東京都千代田区" })
            assertEquals(View.VISIBLE, recreated.findView {
                it.contentDescription?.toString() == "登録した個人情報を選ぶ"
            }!!.visibility)
        } finally {
            clearPersonalInfo(context)
        }
    }

    @Test
    fun inlineRecoveryKeepsAnOpenPickerOutOfTheCandidateStrip() {
        val context = RuntimeEnvironment.getApplication()
        clearPersonalInfo(context)
        ImePreferences.setPersonalInfo(context, PersonalInfoField.NAME, "山田 太郎")
        ImePreferences.setPersonalInfo(context, PersonalInfoField.ADDRESS, "東京都千代田区")
        try {
            val harness = Harness { _, _ -> emptyList() }
            harness.service.onStartInput(EditorInfo().apply { inputType = InputType.TYPE_CLASS_TEXT }, false)
            harness.idle()
            val inline = ImeService::class.java.getDeclaredField("inlineSuggestionController").apply {
                isAccessible = true
            }.get(harness.service) as InlineSuggestionController
            inline.beginResponse()
            harness.root.findView { it.contentDescription?.toString() == "登録した個人情報を選ぶ" }!!.performClick()
            harness.idle()
            harness.root.findView { it.contentDescription?.toString() == "その他の個人情報を表示" }!!.performClick()
            harness.idle()

            ImeService::class.java.getDeclaredMethod("clearInlineSuggestions", Boolean::class.javaPrimitiveType).apply {
                isAccessible = true
            }.invoke(harness.service, true)
            harness.idle()

            assertEquals(null, harness.root.findView { it.contentDescription?.toString() == "候補 1: 山田 太郎" })
            assertEquals(null, harness.root.findView { it.contentDescription?.toString() == "候補 2: 東京都千代田区" })
            assertEquals(View.VISIBLE, harness.root.findView {
                it.contentDescription?.toString() == "個人情報 1: 氏名（姓）: 山田"
            }!!.visibility)
        } finally {
            clearPersonalInfo(context)
        }
    }

    @Test
    fun privateFieldDoesNotExposePersonalInfoOrItsAction() {
        val context = RuntimeEnvironment.getApplication()
        clearPersonalInfo(context)
        ImePreferences.setPersonalInfo(context, PersonalInfoField.NAME, "山田 太郎")
        try {
            val harness = Harness { _, _ -> emptyList() }
            harness.service.onStartInput(EditorInfo().apply {
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }, false)
            harness.idle()

            assertEquals(null, harness.root.findView { it.contentDescription?.toString() == "候補 1: 山田 太郎" })
            assertEquals(
                View.GONE,
                harness.root.findView { it.contentDescription?.toString() == "登録した個人情報を選ぶ" }!!.visibility,
            )
        } finally {
            clearPersonalInfo(context)
        }
    }

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

        harness.service.onKeyAction(KeyAction.SwitchLayer(KeyboardMode.KANA))
        harness.service.onKeyAction(KeyAction.KanaInput("か"))
        harness.idle()
        val dictionary = harness.root.findView { it.contentDescription?.toString() == "候補 2: 個人辞書候補" }!!

        assertEquals(false, dictionary.performLongClick())
        assertEquals(emptyList<Int>(), conversion.deletedIndexes)
        harness.service.onKeyAction(KeyAction.CycleCandidate)
        harness.idle()
        val keyboard = harness.root.findView { it is KeyboardView } as KeyboardView
        val enterId = KeyboardLayouts.layout(KeyboardMode.KANA).rows.flatMap { it.keys }
            .indexOfFirst { it.kind == KeyKind.ENTER }
        fun enterDescription() = keyboard.accessibilityNodeProvider
            .createAccessibilityNodeInfo(enterId)?.contentDescription?.toString()
        assertTrue(enterDescription()!!.startsWith("タップ 確定"))

        val selectedHistory = harness.root.findView { it.contentDescription?.toString() == "候補 1: 履歴候補" }!!
        assertEquals(true, selectedHistory.performLongClick())
        harness.idle()
        assertEquals(listOf(0), conversion.deletedIndexes)
        assertTrue(enterDescription()!!.startsWith("タップ 無変換"))

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

    private fun clearPersonalInfo(context: android.content.Context) {
        PersonalInfoField.entries.forEach { ImePreferences.setPersonalInfo(context, it, "") }
        (1 until ImePreferences.PERSONAL_INFO_EMAIL_SLOTS).forEach { ImePreferences.setPersonalInfoEmail(context, it, "") }
        ImePreferences.setTextSnippets(context, emptyList())
    }

    private class RecordingConnection(view: View, private val commitAccepted: Boolean = true) : BaseInputConnection(view, true) {
        private val text = StringBuilder()
        private var composingStart = -1
        private var composingEnd = -1
        private var selectionStart = 0
        private var selectionEnd = 0
        var supportExtractedText = false
        val cursor: Int get() = selectionEnd
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

        override fun getExtractedText(request: ExtractedTextRequest?, flags: Int): ExtractedText? {
            if (!supportExtractedText) return null
            return ExtractedText().apply {
                text = this@RecordingConnection.text.toString()
                selectionStart = this@RecordingConnection.selectionStart
                selectionEnd = this@RecordingConnection.selectionEnd
                startOffset = 0
            }
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
