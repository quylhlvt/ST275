package com.anime.oc.characters.avatar

import android.app.Activity
import android.content.Context
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.anime.oc.characters.avatar.ui.language.LanguageActivity
import com.anime.oc.characters.avatar.ui.language.LanguageAdapter
import com.anime.oc.characters.avatar.ui.main.home.HomeActivity
import com.anime.oc.characters.avatar.ui.main.setting.SettingActivity
import com.lvt.ads.util.Admob
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class LanguageSwitchTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    private fun <T : Activity> navigate(type: Class<T>, click: () -> Unit): T {
        val monitor = instrumentation.addMonitor(type.name, null, false)
        try {
            instrumentation.waitForIdleSync()
            instrumentation.runOnMainSync(click)
            val activity = instrumentation.waitForMonitorWithTimeout(monitor, 10_000)
            assertNotNull("Did not open ${type.simpleName}", activity)
            instrumentation.waitForIdleSync()
            return type.cast(activity)!!
        } finally {
            instrumentation.removeMonitor(monitor)
        }
    }

    private fun select(language: LanguageActivity, code: String) {
        var name = ""
        instrumentation.runOnMainSync {
            val recycler = language.findViewById<RecyclerView>(R.id.recycleLanguage)
            val items = (recycler.adapter as LanguageAdapter).items
            val position = items.indexOfFirst { it.code == code }
            assertTrue(position >= 0)
            name = items[position].name
            recycler.scrollToPosition(position)
        }
        onView(withText(name)).perform(click())
        instrumentation.waitForIdleSync()
        instrumentation.runOnMainSync {
            val recycler = language.findViewById<RecyclerView>(R.id.recycleLanguage)
            val items = (recycler.adapter as LanguageAdapter).items
            assertEquals(listOf(code), items.filter { it.activate }.map { it.code })
        }
    }

    private fun assertSelected(language: LanguageActivity, code: String) {
        instrumentation.runOnMainSync {
            val recycler = language.findViewById<RecyclerView>(R.id.recycleLanguage)
            val items = (recycler.adapter as LanguageAdapter).items
            assertEquals(listOf(code), items.filter { it.activate }.map { it.code })
            assertEquals(code, items.first().code)
        }
    }

    private fun assertHomeLanguage(home: HomeActivity, code: String) {
        instrumentation.waitForIdleSync()
        val expectedTitles = mapOf(
            "hi" to listOf("एनीमे मेकर", "एनीमे कॉसप्ले", "रैंडम एनीमे", "मेरी रचना"),
            "fr" to listOf("Créateur d’anime", "Cosplay anime", "Anime aléatoire", "Ma création"),
            "en" to listOf("Anime Maker", "Cosplay Anime", "Anime Random", "My Creation")
        )
        instrumentation.runOnMainSync {
            assertEquals(code, home.resources.configuration.locales[0].language)
            val recycler = home.findViewById<RecyclerView>(R.id.rcvMain)
            for ((position, expected) in expectedTitles.getValue(code).withIndex()) {
                val row = recycler.findViewHolderForAdapterPosition(position)
                assertNotNull("Home menu row $position is missing", row)
                assertEquals(expected, row!!.itemView.findViewById<TextView>(R.id.tvTitle).text.toString())
            }
        }
    }

    private fun assertSettingsLanguage(setting: SettingActivity, code: String) {
        val expectedLabels = mapOf(
            "hi" to listOf("सेटिंग्स", "भाषा", "हमें रेट करें", "ऐप साझा करें", "गोपनीयता और नीति"),
            "fr" to listOf("Paramètres", "Langue", "Nous noter", "Partager l’application", "Confidentialité et politique"),
            "en" to listOf("Settings", "Language", "Rate us", "Share app", "Privacy & Policy")
        )
        instrumentation.runOnMainSync {
            assertEquals(code, setting.resources.configuration.locales[0].language)
            val ids = listOf(R.id.tvCenter, R.id.txt1, R.id.txt2, R.id.txt3, R.id.txt4)
            for ((id, expected) in ids.zip(expectedLabels.getValue(code))) {
                assertEquals(expected, setting.findViewById<TextView>(id).text.toString())
            }
        }
    }

    @Test
    fun repeatedSavesAndCancelledDraftAlwaysUseLastSavedLanguage() {
        val prefs = instrumentation.targetContext.getSharedPreferences("DEFAULT", Context.MODE_PRIVATE)
        val originalCode = prefs.getString("language_key", null)
        val originalScreen = prefs.getBoolean("language_screen", false)
        val originalLocale = Locale.getDefault()
        prefs.edit().putString("language_key", "en").putBoolean("language_screen", true).commit()
        instrumentation.runOnMainSync { Admob.getInstance().setOpenShowAllAds(false) }
        var home: HomeActivity? = null
        try {
            ActivityScenario.launch(HomeActivity::class.java).use { scenario ->
                scenario.onActivity { home = it }
                var savedCode = "en"
                assertHomeLanguage(home!!, savedCode)
                for (code in listOf("hi", "fr", "en", "hi", "en", "en")) {
                    val setting = navigate(SettingActivity::class.java) {
                        home!!.findViewById<android.view.View>(R.id.btnActionBarRight).performClick()
                    }
                    assertSettingsLanguage(setting, savedCode)
                    val language = navigate(LanguageActivity::class.java) {
                        setting.findViewById<android.view.View>(R.id.btnLang).performClick()
                    }
                    assertSelected(language, savedCode)
                    select(language, code)
                    home = navigate(HomeActivity::class.java) {
                        language.findViewById<android.view.View>(R.id.btnActionBarRight).performClick()
                    }
                    assertEquals(code, prefs.getString("language_key", ""))
                    assertHomeLanguage(home!!, code)
                    savedCode = code
                }
                val setting = navigate(SettingActivity::class.java) {
                    home!!.findViewById<android.view.View>(R.id.btnActionBarRight).performClick()
                }
                assertSettingsLanguage(setting, "en")
                val language = navigate(LanguageActivity::class.java) {
                    setting.findViewById<android.view.View>(R.id.btnLang).performClick()
                }
                assertSelected(language, "en")
                // Selection and Save must also work in the same frame. The app's
                // global click debounce used to discard these independent taps.
                val recycler = language.findViewById<RecyclerView>(R.id.recycleLanguage)
                instrumentation.runOnMainSync { recycler.scrollToPosition(0) }
                onView(withText("English")).check(matches(isDisplayed()))
                onView(withText("Hindi")).check(matches(isDisplayed()))
                home = navigate(HomeActivity::class.java) {
                    val items = (recycler.adapter as LanguageAdapter).items
                    val rows = listOf("hi", "en").associateWith { code ->
                        val position = items.indexOfFirst { it.code == code }
                        recycler.findViewHolderForAdapterPosition(position)!!.itemView
                    }
                    for (code in listOf("hi", "en", "hi", "en")) {
                        assertTrue(rows.getValue(code).performClick())
                    }
                    language.findViewById<android.view.View>(R.id.btnActionBarRight).performClick()
                }
                assertEquals("en", prefs.getString("language_key", ""))
                assertHomeLanguage(home!!, "en")
                val cancelSetting = navigate(SettingActivity::class.java) {
                    home!!.findViewById<android.view.View>(R.id.btnActionBarRight).performClick()
                }
                val cancelLanguage = navigate(LanguageActivity::class.java) {
                    cancelSetting.findViewById<android.view.View>(R.id.btnLang).performClick()
                }
                select(cancelLanguage, "hi")
                instrumentation.runOnMainSync {
                    cancelLanguage.findViewById<android.view.View>(R.id.btnActionBarLeft).performClick()
                }
                instrumentation.waitForIdleSync()
                assertEquals("en", prefs.getString("language_key", ""))
                assertSettingsLanguage(cancelSetting, "en")
                val reopened = navigate(LanguageActivity::class.java) {
                    cancelSetting.findViewById<android.view.View>(R.id.btnLang).performClick()
                }
                assertSelected(reopened, "en")
            }
        } finally {
            instrumentation.runOnMainSync { home?.takeUnless { it.isDestroyed }?.finishAffinity() }
            prefs.edit().putString("language_key", originalCode).putBoolean("language_screen", originalScreen).commit()
            Locale.setDefault(originalLocale)
        }
    }
}
