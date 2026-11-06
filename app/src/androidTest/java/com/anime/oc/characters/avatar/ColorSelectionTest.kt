package com.anime.oc.characters.avatar

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.SystemClock
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.anime.oc.characters.avatar.data.model.custom.BodyPartModel
import com.anime.oc.characters.avatar.data.model.custom.ColorModel
import com.anime.oc.characters.avatar.data.model.custom.CustomModel
import com.anime.oc.characters.avatar.data.model.custom.SelectionIndex
import com.anime.oc.characters.avatar.ui.main.customize.ColorAdapter
import com.anime.oc.characters.avatar.ui.main.customize.CustomizeActivity
import com.anime.oc.characters.avatar.ui.main.customize.CustomizeViewModel
import com.anime.oc.characters.avatar.ui.main.home.HomeActivity
import com.lvt.ads.util.Admob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ColorSelectionTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Test
    fun editKeepsSelectedColorVisibleAcrossTabsAndRecreation() {
        instrumentation.runOnMainSync { Admob.getInstance().setOpenShowAllAds(false) }
        lateinit var session: AppSession
        ActivityScenario.launch(HomeActivity::class.java).use { home ->
            home.onActivity { session = it.appSession }
            runBlocking {
                withTimeout(30_000) {
                    session.localDataReady.first { it }
                    session.isFetchingOnlineFlow.first { !it }
                }
            }
        }
        val previousOnline = session.templates.value.filter { it.id.startsWith("online_") }
        val image = File.createTempFile("color-selection-", ".png", instrumentation.targetContext.cacheDir)
        image.outputStream().use { output ->
            val bitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            bitmap.recycle()
        }
        val colors = ArrayList((0 until 24).map { index ->
            ColorModel("%06X".format(index * 500_000), arrayListOf("none", "dice", image.absolutePath))
        })
        val template = CustomModel(
            id = "online_color_selection_fixture",
            listPath = arrayListOf(
                BodyPartModel("body", colors),
                BodyPartModel("hair", ArrayList(colors), zIndex = 1),
                BodyPartModel("accessory", arrayListOf(colors.first()), zIndex = 2)
            )
        )
        instrumentation.runOnMainSync { session.appDataManager.prependOnlineTemplates(listOf(template)) }
        try {
            val intent = Intent(instrumentation.targetContext, CustomizeActivity::class.java)
                .putExtra(CustomizeActivity.ARG_TEMPLATE_ID, template.id)
                .putExtra(CustomizeActivity.ARG_TEMPLATE_INDEX, 0)
                .putExtra(CustomizeActivity.ARG_IS_EDIT, true)
                .putParcelableArrayListExtra(CustomizeActivity.ARG_SELECTIONS, arrayListOf(
                    SelectionIndex(0, 23, 2), SelectionIndex(1, 1, 2), SelectionIndex(2, 0, 2)
                ))
            ActivityScenario.launch<CustomizeActivity>(intent).use { scenario ->
                val deadline = SystemClock.uptimeMillis() + 10_000
                var ready = false
                while (!ready && SystemClock.uptimeMillis() < deadline) {
                    scenario.onActivity { ready = it.findViewById<View>(R.id.btnActionBarRightText).isEnabled }
                    if (!ready) SystemClock.sleep(20)
                }
                assertTrue("Editor did not finish loading", ready)

                fun selectTab(index: Int) {
                    scenario.onActivity { ViewModelProvider(it)[CustomizeViewModel::class.java].selectNav(index) }
                }
                fun assertColor(index: Int) {
                    onView(withId(R.id.rcvColor)).check { view, error ->
                        error?.let { throw it }
                        val recycler = view as RecyclerView
                        assertEquals(index, (recycler.adapter as ColorAdapter).posColor)
                        val row = recycler.findViewHolderForAdapterPosition(index)
                        assertNotNull("Selected color $index is outside the visible palette", row)
                        val focus = row!!.itemView.findViewById<View>(R.id.colorSelected)
                        assertEquals(View.VISIBLE, focus.visibility)
                        val visible = Rect()
                        assertTrue("Selected color is clipped by its parent", focus.getGlobalVisibleRect(visible))
                        assertEquals(focus.width, visible.width())
                    }
                }
                assertColor(23)
                selectTab(1)
                assertColor(1)
                selectTab(2)
                selectTab(0)
                assertColor(23)
                selectTab(1)
                assertColor(1)
                scenario.recreate()
                assertColor(1)
            }
        } finally {
            instrumentation.runOnMainSync { session.appDataManager.prependOnlineTemplates(previousOnline) }
            image.delete()
        }
    }
}
