package com.anime.oc.characters.av

import android.content.Intent
import android.graphics.Bitmap
import android.os.SystemClock
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.anime.oc.characters.av.data.model.custom.BodyPartModel
import com.anime.oc.characters.av.data.model.custom.ColorModel
import com.anime.oc.characters.av.data.model.custom.CustomModel
import com.anime.oc.characters.av.ui.main.customize.CustomizeActivity
import com.anime.oc.characters.av.ui.main.customize.CustomizeViewModel
import com.anime.oc.characters.av.ui.main.home.HomeActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.data.DataFetcher
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.lvt.ads.util.Admob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

@RunWith(AndroidJUnit4::class)
class CustomizeLoadingTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    private fun awaitCondition(condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 10_000
        while (!condition() && SystemClock.uptimeMillis() < deadline) SystemClock.sleep(20)
        assertTrue("Timed out waiting for image loading", condition())
    }

    @Test
    fun randomAllKeepsNextDimmedUntilEveryLayerCompletesAcrossRendersAndResume() {
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
        val prefix = "https://customize-loading.invalid/${UUID.randomUUID()}/"
        val gate = ImageGate(prefix)
        Glide.get(instrumentation.targetContext).registry.prepend(
            GlideUrl::class.java, InputStream::class.java, gate
        )
        val basePaths = arrayListOf("dice", "${prefix}initial.png")
        val template = CustomModel(
            id = "online_loading_fixture",
            listPath = arrayListOf(
                BodyPartModel(nav = "body", listPath = arrayListOf(ColorModel(listPath = basePaths))),
                BodyPartModel(nav = "hat", position = 1, zIndex = 1,
                    listPath = arrayListOf(ColorModel(listPath = arrayListOf("none", "dice", "${prefix}hat.png"))))
            )
        )
        instrumentation.runOnMainSync { session.appDataManager.prependOnlineTemplates(listOf(template)) }
        try {
            val intent = Intent(instrumentation.targetContext, CustomizeActivity::class.java)
                .putExtra(CustomizeActivity.ARG_TEMPLATE_INDEX, 0)
                .putExtra(CustomizeActivity.ARG_TEMPLATE_ID, template.id)
            ActivityScenario.launch<CustomizeActivity>(intent).use { scenario ->
                fun assertNext(enabled: Boolean) = scenario.onActivity { activity ->
                    val next = activity.findViewById<View>(R.id.btnActionBarRightText)
                    assertEquals(enabled, next.isEnabled)
                    assertEquals(enabled, next.isClickable)
                    assertEquals(if (enabled) 1f else 0.5f, next.alpha, 0f)
                }
                awaitCondition {
                    var enabled = false
                    scenario.onActivity { enabled = it.findViewById<View>(R.id.btnActionBarRightText).isEnabled }
                    enabled
                }
                gate.hold = true
                scenario.onActivity {
                    // Give Random All one new choice per layer, avoiding random test outcomes.
                    basePaths[1] = "${prefix}random.png"
                    ViewModelProvider(it)[CustomizeViewModel::class.java].randomizeAll()
                }
                awaitCondition { gate.waiting.size == 2 }
                assertNext(false)
                scenario.onActivity {
                    ViewModelProvider(it)[CustomizeViewModel::class.java].toggleFlip()
                }
                instrumentation.waitForIdleSync()
                assertNext(false)
                scenario.moveToState(Lifecycle.State.CREATED)
                scenario.moveToState(Lifecycle.State.RESUMED)
                awaitCondition { gate.waiting.size == 2 }
                assertNext(false)
                val first = gate.waiting.keys.first()
                gate.complete(first)
                instrumentation.waitForIdleSync()
                assertNext(false)
                gate.hold = false
                gate.waiting.keys.toList().forEach(gate::complete)
                awaitCondition {
                    var enabled = false
                    scenario.onActivity { enabled = it.findViewById<View>(R.id.btnActionBarRightText).isEnabled }
                    enabled
                }
                assertNext(true)
            }
        } finally {
            instrumentation.runOnMainSync { session.appDataManager.prependOnlineTemplates(previousOnline) }
        }
    }

    private class ImageGate(private val prefix: String) : ModelLoaderFactory<GlideUrl, InputStream> {
        @Volatile var hold = false
        val waiting = ConcurrentHashMap<String, CopyOnWriteArrayList<DataFetcher.DataCallback<in InputStream>>>()
        private val png = ByteArrayOutputStream().use { output ->
            val bitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            bitmap.recycle()
            output.toByteArray()
        }

        fun complete(url: String) { waiting.remove(url)?.forEach { it.onDataReady(png.inputStream()) } }

        override fun build(multiFactory: MultiModelLoaderFactory) = object : ModelLoader<GlideUrl, InputStream> {
            override fun handles(model: GlideUrl) = model.toStringUrl().startsWith(prefix)
            override fun buildLoadData(model: GlideUrl, width: Int, height: Int, options: Options) =
                ModelLoader.LoadData(model, object : DataFetcher<InputStream> {
                    private var callback: DataFetcher.DataCallback<in InputStream>? = null
                    override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in InputStream>) {
                        this.callback = callback
                        if (hold) waiting.computeIfAbsent(model.toStringUrl()) { CopyOnWriteArrayList() }.add(callback)
                        else callback.onDataReady(png.inputStream())
                    }
                    override fun cleanup() = Unit
                    override fun cancel() {
                        val callbacks = waiting[model.toStringUrl()] ?: return
                        callback?.let(callbacks::remove)
                        if (callbacks.isEmpty()) waiting.remove(model.toStringUrl(), callbacks)
                    }
                    override fun getDataClass() = InputStream::class.java
                    override fun getDataSource() = DataSource.REMOTE
                })
        }
        override fun teardown() = Unit
    }
}
