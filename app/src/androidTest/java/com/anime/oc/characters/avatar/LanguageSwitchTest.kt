package com.anime.oc.characters.avatar

import android.app.Activity
import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
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
        var position = -1
        instrumentation.runOnMainSync {
            val recycler = language.findViewById<RecyclerView>(R.id.recycleLanguage)
            position = (recycler.adapter as LanguageAdapter).items.indexOfFirst { it.code == code }
            assertTrue(position >= 0)
            recycler.scrollToPosition(position)
        }
        instrumentation.waitForIdleSync()
        instrumentation.runOnMainSync {
            val recycler = language.findViewById<RecyclerView>(R.id.recycleLanguage)
            assertTrue(recycler.findViewHolderForAdapterPosition(position)!!.itemView.performClick())
        }
        instrumentation.waitForIdleSync()
    }

    private fun assertSelected(language: LanguageActivity, code: String) {
        instrumentation.runOnMainSync {
            val recycler = language.findViewById<RecyclerView>(R.id.recycleLanguage)
            val items = (recycler.adapter as LanguageAdapter).items
            assertEquals(listOf(code), items.filter { it.activate }.map { it.code })
            assertEquals(code, items.first().code)
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
                val expectedSettings = mapOf("hi" to "सेटिंग्स", "fr" to "Paramètres", "en" to "Settings")
                for (code in listOf("hi", "fr", "en", "hi", "en")) {
                    val setting = navigate(SettingActivity::class.java) {
                        home!!.findViewById<android.view.View>(R.id.btnActionBarRight).performClick()
                    }
                    val language = navigate(LanguageActivity::class.java) {
                        setting.findViewById<android.view.View>(R.id.btnLang).performClick()
                    }
                    assertSelected(language, savedCode)
                    select(language, code)
                    home = navigate(HomeActivity::class.java) {
                        language.findViewById<android.view.View>(R.id.btnActionBarRight).performClick()
                    }
                    assertEquals(code, prefs.getString("language_key", ""))
                    instrumentation.runOnMainSync {
                        assertEquals(code, home!!.resources.configuration.locales[0].language)
                        assertEquals(expectedSettings[code], home!!.getString(R.string.settings))
                    }
                    savedCode = code
                }
                val setting = navigate(SettingActivity::class.java) {
                    home!!.findViewById<android.view.View>(R.id.btnActionBarRight).performClick()
                }
                val language = navigate(LanguageActivity::class.java) {
                    setting.findViewById<android.view.View>(R.id.btnLang).performClick()
                }
                assertSelected(language, "en")
                select(language, "hi")
                instrumentation.runOnMainSync {
                    language.findViewById<android.view.View>(R.id.btnActionBarLeft).performClick()
                }
                instrumentation.waitForIdleSync()
                assertEquals("en", prefs.getString("language_key", ""))
                val reopened = navigate(LanguageActivity::class.java) {
                    setting.findViewById<android.view.View>(R.id.btnLang).performClick()
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
