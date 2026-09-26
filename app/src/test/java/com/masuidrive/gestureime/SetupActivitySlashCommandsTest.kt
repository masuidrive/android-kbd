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
