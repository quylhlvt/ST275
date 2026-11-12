package com.anime.oc.characters.av

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.anime.oc.characters.av.core.custom.DrawableDraw
import com.anime.oc.characters.av.core.custom.DrawView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DrawViewSizingTest {
    @Test
    fun customCharacterNearlyFillsCanvasWhileTextAndStickersKeepTheirSize() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val context = instrumentation.targetContext
            for ((canvasWidth, canvasHeight) in listOf(800 to 1200, 1200 to 800)) {
                for ((imageWidth, imageHeight) in listOf(400 to 900, 900 to 400)) {
                    val canvas = DrawView(context).apply { layout(0, 0, canvasWidth, canvasHeight) }
                    val bitmap = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888)
                    for (type in listOf("character", "text", "sticker")) {
                        val draw = DrawableDraw(BitmapDrawable(context.resources, bitmap), "fixture.png").apply {
                            isCharacter = type == "character"
                            isText = type == "text"
                        }
                        canvas.addDraw(draw)
                        val bounds = draw.mappedBound
                        val coverage = maxOf(bounds.width() / canvasWidth, bounds.height() / canvasHeight)
                        when (type) {
                            "character" -> assertTrue("Character should nearly fill its canvas", coverage in 0.94f..0.96f)
                            "text" -> assertTrue("Text size changed", coverage in 0.58f..0.59f)
                            else -> assertTrue("Sticker size changed", coverage in 0.34f..0.36f)
                        }
                        assertEquals(canvasWidth / 2f, bounds.centerX(), 0.01f)
                        assertEquals(canvasHeight / 2f, bounds.centerY(), 0.01f)
                        assertTrue(bounds.left >= 0f && bounds.top >= 0f)
                        assertTrue(bounds.right <= canvasWidth && bounds.bottom <= canvasHeight)
                    }
                }
            }
        }
    }
}
