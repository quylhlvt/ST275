package com.anime.oc.characters.av

import android.app.Activity
import android.content.Intent
import android.view.WindowManager
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.anime.oc.characters.av.ui.main.home.HomeActivity
import com.anime.oc.characters.av.ui.main.myPony.MyPonyActivity
import com.anime.oc.characters.av.ui.main.success.SuccessActivity
import com.lvt.ads.util.Admob
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SuccessInteractionTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val blockedFlags = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE

    @Before
    fun disableAds() {
        instrumentation.runOnMainSync { Admob.getInstance().setOpenShowAllAds(false) }
    }

    private fun assertScreenAndClose(expected: Class<out Activity>) {
        instrumentation.waitForIdleSync()
        instrumentation.runOnMainSync {
            val resumed = ActivityLifecycleMonitorRegistry.getInstance()
                .getActivitiesInStage(Stage.RESUMED).single()
            assertTrue("Expected ${expected.simpleName}, got ${resumed.javaClass.simpleName}", expected.isInstance(resumed))
            resumed.finishAffinity()
        }
    }

    @Test
    fun firstEntryClearsBlockingWindowAndMyCreationAcceptsTouch() {
        ActivityScenario.launch(SuccessActivity::class.java).use { scenario ->
            scenario.moveToState(Lifecycle.State.CREATED)
            scenario.onActivity {
                it.showGlobalLoading()
                it.window.addFlags(blockedFlags)
            }
            scenario.moveToState(Lifecycle.State.RESUMED)
            scenario.onActivity { assertEquals(0, it.window.attributes.flags and blockedFlags) }
            onView(withId(R.id.btnBottomLeft)).perform(click())
            assertScreenAndClose(MyPonyActivity::class.java)
        }
    }

    @Test
    fun returningToSuccessRestoresTouchAndHomeClearsEditors() {
        val intent = Intent(instrumentation.targetContext, SuccessActivity::class.java)
            .putExtra("imagePath", "")
        ActivityScenario.launch<SuccessActivity>(intent).use { scenario ->
            scenario.moveToState(Lifecycle.State.CREATED)
            scenario.onActivity { it.window.addFlags(blockedFlags) }
            scenario.moveToState(Lifecycle.State.RESUMED)
            scenario.onActivity { assertEquals(0, it.window.attributes.flags and blockedFlags) }
            onView(withId(R.id.btnActionBarRight)).perform(click())
            assertScreenAndClose(HomeActivity::class.java)
        }
    }
}
