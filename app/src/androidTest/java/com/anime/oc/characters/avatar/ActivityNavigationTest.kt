package com.anime.oc.characters.avatar

import android.content.Intent
import android.graphics.Bitmap
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.anime.oc.characters.avatar.core.extention.EXTRA_FROM_SETTINGS
import com.anime.oc.characters.avatar.core.extention.openScreen
import com.anime.oc.characters.avatar.core.extention.popBack
import com.anime.oc.characters.avatar.core.navigation.Screen
import com.anime.oc.characters.avatar.ui.language.LanguageActivity
import com.anime.oc.characters.avatar.ui.main.home.HomeActivity
import com.anime.oc.characters.avatar.ui.main.setting.SettingActivity
import com.lvt.ads.util.Admob
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ActivityNavigationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Before
    fun disableAds() {
        instrumentation.runOnMainSync { Admob.getInstance().setOpenShowAllAds(false) }
    }

    @Test
    fun sharedImageSurvivesScreenChangeAndBack() {
        val monitor = instrumentation.addMonitor(SettingActivity::class.java.name, null, false)
        try {
            ActivityScenario.launch(HomeActivity::class.java).use { scenario ->
                lateinit var session: AppSession
                val image = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
                scenario.onActivity {
                    session = it.appSession
                    session.customizeBitmap = image
                    assertTrue(it.openScreen(Screen.SETTING))
                    assertFalse(it.openScreen(Screen.SETTING)) // repeated click cannot open a duplicate
                }
                val setting = instrumentation.waitForMonitorWithTimeout(monitor, 5_000) as SettingActivity
                instrumentation.runOnMainSync {
                    assertSame(session, setting.appSession)
                    assertSame(image, setting.appSession.customizeBitmap)
                    setting.popBack()
                }
                instrumentation.waitForIdleSync()
                scenario.onActivity {
                    assertSame(session, it.appSession)
                    assertSame(image, it.appSession.customizeBitmap)
                    session.customizeBitmap = null
                }
            }
        } finally {
            instrumentation.removeMonitor(monitor)
        }
    }

    @Test
    fun languageKeepsSettingsOriginAfterRecreation() {
        val intent = Intent(instrumentation.targetContext, LanguageActivity::class.java)
            .putExtra(EXTRA_FROM_SETTINGS, true)
        ActivityScenario.launch<LanguageActivity>(intent).use { scenario ->
            scenario.recreate()
            scenario.onActivity {
                assertTrue(it.intent.getBooleanExtra(EXTRA_FROM_SETTINGS, false))
                assertTrue(it.onScreenBackPressed())
                assertTrue(it.isFinishing)
            }
        }
    }

    @Test
    fun everyScreenIsRegisteredAsAnActivity() {
        val context = instrumentation.targetContext
        Screen.entries.forEach { screen ->
            val resolved = Intent(context, screen.activityClass).resolveActivity(context.packageManager)
            assertNotNull("Missing manifest entry for $screen", resolved)
            assertEquals(screen.activityClass.name, resolved!!.className)
        }
    }
}
