package com.anime.oc.characters.avatar

import android.os.Bundle
import android.view.WindowManager
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.NavHostFragment
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.lvt.ads.util.Admob
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SuccessInteractionTest {
    private val blockedFlags = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE

    private fun controller(activity: MainActivity) = (activity.supportFragmentManager
        .findFragmentById(R.id.nav_host_fragment) as NavHostFragment).navController

    private fun openSuccess(activity: MainActivity) {
        controller(activity).setGraph(R.navigation.nav_main)
        activity.showGlobalLoading()
        activity.window.addFlags(blockedFlags)
        controller(activity).navigate(R.id.successFragment, Bundle().apply {
            putString("imagePath", "")
        })
    }

    @Test
    fun firstEntryClearsBlockingWindowAndMyCreationAcceptsTouch() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            Admob.getInstance().setOpenShowAllAds(false)
        }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { openSuccess(it) }
            onView(withId(R.id.btnBottomLeft)).perform(click())
            scenario.onActivity {
                assertEquals(0, it.window.attributes.flags and blockedFlags)
                assertEquals(R.id.myPony, controller(it).currentDestination?.id)
            }
        }
    }

    @Test
    fun returningToSuccessRestoresTouch() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            Admob.getInstance().setOpenShowAllAds(false)
        }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { openSuccess(it) }
            scenario.moveToState(Lifecycle.State.CREATED)
            scenario.onActivity { it.window.addFlags(blockedFlags) }
            scenario.moveToState(Lifecycle.State.RESUMED)
            onView(withId(R.id.btnActionBarRight)).perform(click())
            scenario.onActivity {
                assertEquals(0, it.window.attributes.flags and blockedFlags)
                assertEquals(R.id.homeFragment, controller(it).currentDestination?.id)
            }
        }
    }
}
