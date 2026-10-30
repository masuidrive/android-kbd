package com.masuidrive.gestureime

import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.RadioButton
import android.widget.TextView
import android.widget.LinearLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SetupActivitySlashCommandsTest {
    @Test
    fun nonTerminalInputSwitchesDefaultOnAndKeepExplicitFalseAfterRecreation() {
        val context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("gesture_ime_preferences", 0).edit().clear().commit()
        val activity = Robolectric.buildActivity(SetupActivity::class.java).setup().get()
        val root = activity.findViewById<View>(android.R.id.content)
        val toggles = root.descendants().filterIsInstance<Switch>()
        val dual = toggles.single { it.text.toString().startsWith("Dual Flick") }
        val terminal = toggles.single { it.text.toString().startsWith("ターミナル向け") }
        val english = toggles.single { it.text.toString() == "英数字候補（端末内・既定ON）" }
        val dictionary = toggles.single { it.text.toString() == "Android 個人辞書を使う（既定ON）" }

        assertTrue(dual.isChecked)
        assertFalse(terminal.isChecked)
        assertTrue(english.isChecked)
        assertTrue(dictionary.isChecked)

        dual.isChecked = false
        english.isChecked = false
        dictionary.isChecked = false
        activity.recreate()

        val recreated = activity.findViewById<View>(android.R.id.content).descendants().filterIsInstance<Switch>()
        assertFalse(recreated.single { it.text.toString().startsWith("Dual Flick") }.isChecked)
        assertFalse(recreated.single { it.text.toString().startsWith("英数字候補") }.isChecked)
        assertFalse(recreated.single { it.text.toString().startsWith("Android 個人辞書") }.isChecked)
    }

    @Test
    fun setupShowsSixEditableSlotsAndSavePersistsNormalizedCommands() {
        val context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("gesture_ime_preferences", 0).edit().clear().commit()
        val activity = Robolectric.buildActivity(SetupActivity::class.java).setup().get()
        val root = activity.findViewById<View>(android.R.id.content)
        val inputs = root.descendants().filterIsInstance<EditText>()
            .filter { it.tag !is PersonalInfoField && it.tag !is Int && it.tag != "text_snippet" }

        assertEquals(6, inputs.size)
        assertEquals(listOf("/compact", "/clear", "/quit", "", "", ""), inputs.map { it.text.toString() })

        inputs[0].setText("review")
        inputs[1].setText("/ship")
        root.descendants().filterIsInstance<Button>()
            .single { it.text.toString() == "スラッシュ候補を保存" }
            .performClick()

        assertEquals(listOf("/review", "/ship", "/quit", "", "", ""), ImePreferences.getSlashCommands(activity))
        assertEquals("/review", inputs[0].text.toString())
        assertNotNull(root)
    }

    @Test
    fun setupAddsEditsAndDeletesTextSnippets() {
        val context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("gesture_ime_preferences", 0).edit().clear().commit()
        var activity = Robolectric.buildActivity(SetupActivity::class.java).setup().get()
        fun currentRoot() = activity.findViewById<View>(android.R.id.content)
        fun buttons() = currentRoot().descendants().filterIsInstance<Button>()
        fun snippets() = currentRoot().descendants().filterIsInstance<EditText>().filter { it.tag == "text_snippet" }

        buttons().single { it.text.toString() == "定型文を追加" }.performClick()
        snippets().single().setText("いつもありがとうございます。")
        buttons().single { it.text.toString() == "定型文を保存" }.performClick()
        assertEquals(listOf("いつもありがとうございます。"), ImePreferences.getTextSnippets(activity))

        val previousRoot = currentRoot()
        activity = Robolectric.buildActivity(SetupActivity::class.java).setup().get()
        assertNotSame(previousRoot, currentRoot())
        assertEquals("いつもありがとうございます。", snippets().single().text.toString())
        buttons().single { it.text.toString() == "削除" }.performClick()
        buttons().single { it.text.toString() == "定型文を保存" }.performClick()
        assertEquals(emptyList<String>(), ImePreferences.getTextSnippets(activity))
    }

    @Test
    fun setupSavesAndRestoresSplitPersonalInfoFields() {
        val context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("gesture_ime_preferences", 0).edit().clear().commit()
        val activity = Robolectric.buildActivity(SetupActivity::class.java).setup().get()
        val root = activity.findViewById<View>(android.R.id.content)
        val inputs = root.descendants().filterIsInstance<EditText>()
            .filter { it.tag is PersonalInfoField }
        assertTrue(inputs.all { it.importantForAutofill == View.IMPORTANT_FOR_AUTOFILL_NO })
        val extraEmails = root.descendants().filterIsInstance<EditText>().filter { it.tag is Int && (it.tag as Int) in 1..3 }
        assertEquals(3, extraEmails.size)
        assertTrue(extraEmails.all { it.importantForAutofill == View.IMPORTANT_FOR_AUTOFILL_NO })

        val values = mapOf(
            PersonalInfoField.NAME to "山田 太郎",
            PersonalInfoField.NAME_HIRAGANA to "やまだ たろう",
            PersonalInfoField.NAME_KATAKANA to "ヤマダ タロウ",
            PersonalInfoField.NAME_LATIN to "Taro Yamada",
            PersonalInfoField.EMAIL to "name@example.com",
            PersonalInfoField.POSTAL_CODE to "1000001",
            PersonalInfoField.PREFECTURE to "東京都",
            PersonalInfoField.CITY to "千代田区",
            PersonalInfoField.ADDRESS_LINE1 to "千代田1-1",
            PersonalInfoField.ADDRESS_LINE2 to "建物名",
        )
        assertEquals(values.size, inputs.size)
        values.forEach { (field, value) -> inputs.single { it.tag == field }.setText(value) }
        extraEmails.forEachIndexed { index, input -> input.setText("email${index + 2}@example.com") }
        root.descendants().filterIsInstance<Button>()
            .single { it.text.toString() == "個人情報候補を保存" }
            .performClick()

        values.forEach { (field, value) -> assertEquals(value, ImePreferences.getPersonalInfo(activity, field)) }
        assertEquals(listOf("name@example.com", "email2@example.com", "email3@example.com", "email4@example.com"),
            ImePreferences.getPersonalInfoEmails(activity))
        activity.recreate()
        val recreatedInputs = activity.findViewById<View>(android.R.id.content).descendants()
            .filterIsInstance<EditText>().filter { it.tag is PersonalInfoField }
        values.forEach { (field, value) ->
            assertEquals(value, recreatedInputs.single { it.tag == field }.text.toString())
        }
        assertEquals(listOf("email2@example.com", "email3@example.com", "email4@example.com"),
            activity.findViewById<View>(android.R.id.content).descendants().filterIsInstance<EditText>()
                .filter { it.tag is Int && (it.tag as Int) in 1..3 }.map { it.text.toString() })

        recreatedInputs.single { it.tag == PersonalInfoField.EMAIL }.setText("")
        activity.findViewById<View>(android.R.id.content).descendants().filterIsInstance<Button>()
            .single { it.text.toString() == "個人情報候補を保存" }
            .performClick()
        assertEquals("", ImePreferences.getPersonalInfo(activity, PersonalInfoField.EMAIL))
    }

    @Test
    fun setupGroupsActionsShowsBuildVersionAndUsesAccessibleTouchTargets() {
        val activity = Robolectric.buildActivity(SetupActivity::class.java).setup().get()
        val descendants = activity.findViewById<View>(android.R.id.content).descendants()
        val labels = descendants.filterIsInstance<TextView>().map { it.text.toString() }

        assertTrue(labels.indexOf("初期設定") < labels.indexOf("入力設定"))
        assertTrue(labels.indexOf("入力設定") < labels.indexOf("スラッシュコマンド候補（最大6件）"))
        assertTrue(labels.indexOf("スラッシュコマンド候補（最大6件）") < labels.indexOf("アプリ情報"))
        assertTrue(labels.contains("バージョン ${BuildConfig.VERSION_NAME}"))
        assertFalse(labels.any { it.contains("QWERTYラベル") })

        val minimum = (48 * activity.resources.displayMetrics.density).toInt()
        descendants.filter { it is Button || it is Switch || it is EditText }
            .forEach { assertTrue("${it.javaClass.simpleName} touch target", it.minimumHeight >= minimum) }

        val sectionCards = descendants.filterIsInstance<LinearLayout>().filter { it.elevation > 0f }
        assertEquals("four visually distinct setting sections", 4, sectionCards.size)
        assertTrue(sectionCards.all { it.background != null && it.paddingLeft > 0 })
    }

    @Test
    fun keyboardHeightChoicesDefaultToStandardAndPersistTheSelectedPreset() {
        val context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("gesture_ime_preferences", 0).edit().clear().commit()
        val activity = Robolectric.buildActivity(SetupActivity::class.java).setup().get()
        val choices = activity.findViewById<View>(android.R.id.content).descendants().filterIsInstance<RadioButton>()
            .filter { it.tag is com.masuidrive.gestureime.keyboard.KeyboardHeightPreset }
        val minimum = (48 * activity.resources.displayMetrics.density).toInt()

        assertEquals(listOf("小", "標準", "大"), choices.map { it.text.toString() })
        assertEquals("標準", choices.single { it.isChecked }.text.toString())
        assertTrue(choices.all { it.minimumHeight >= minimum })

        choices.single { it.text.toString() == "大" }.performClick()
        assertEquals(com.masuidrive.gestureime.keyboard.KeyboardHeightPreset.LARGE, ImePreferences.getKeyboardHeightPreset(activity))
        activity.recreate()
        val recreated = activity.findViewById<View>(android.R.id.content).descendants().filterIsInstance<RadioButton>()
            .filter { it.tag is com.masuidrive.gestureime.keyboard.KeyboardHeightPreset }
        assertEquals("大", recreated.single { it.isChecked }.text.toString())
        ImePreferences.setKeyboardHeightPreset(activity, com.masuidrive.gestureime.keyboard.KeyboardHeightPreset.STANDARD)
    }

    @Test
    fun flickSensitivityChoicesPersistIndependentlyWithStandardDefaults() {
        val context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("gesture_ime_preferences", 0).edit().clear().commit()
        val activity = Robolectric.buildActivity(SetupActivity::class.java).setup().get()
        val descendants = activity.findViewById<View>(android.R.id.content).descendants()
        val labels = descendants.filterIsInstance<TextView>().map { it.text.toString() }
        val choices = descendants.filterIsInstance<RadioButton>()
            .filter { it.tag is com.masuidrive.gestureime.keyboard.FlickSensitivity }

        assertTrue(labels.contains("かな・テンキーのフリック感度"))
        assertTrue(labels.contains("QWERTY・記号のフリック感度"))
        assertEquals(listOf("高い", "標準", "低い", "高い", "標準", "低い"), choices.map { it.text.toString() })
        assertEquals(2, choices.count { it.isChecked && it.text.toString() == "標準" })

        choices[0].performClick()
        choices[5].performClick()
        assertEquals(
            com.masuidrive.gestureime.keyboard.FlickSensitivity.HIGH,
            ImePreferences.getKanaNumberFlickSensitivity(activity),
        )
        assertEquals(
            com.masuidrive.gestureime.keyboard.FlickSensitivity.LOW,
            ImePreferences.getQwertySymbolFlickSensitivity(activity),
        )

        activity.recreate()
        val recreated = activity.findViewById<View>(android.R.id.content).descendants().filterIsInstance<RadioButton>()
            .filter { it.tag is com.masuidrive.gestureime.keyboard.FlickSensitivity }
        assertTrue(recreated[0].isChecked)
        assertTrue(recreated[5].isChecked)
    }

    private fun View.descendants(): List<View> {
        val result = mutableListOf(this)
        if (this is ViewGroup) repeat(childCount) { result += getChildAt(it).descendants() }
        return result
    }
}
