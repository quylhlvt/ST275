package com.anime.oc.characters.avatar

import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.view.ContextThemeWrapper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.anime.oc.characters.avatar.data.model.custom.ColorModel
import com.anime.oc.characters.avatar.databinding.ActivityCustomizeBinding
import com.anime.oc.characters.avatar.ui.main.customize.ColorAdapter
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ColorBarLayoutTest {
    @Test
    fun colorBarWrapsSmallListsAndScrollsLongListsInsideItsAnchors() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val context = ContextThemeWrapper(instrumentation.targetContext, R.style.Base_Theme_BaseFragment)
            val binding = ActivityCustomizeBinding.inflate(LayoutInflater.from(context))
            val adapter = ColorAdapter()
            binding.rcvColor.adapter = adapter

            fun showColors(count: Int): Int {
                adapter.submitList(List(count) { ColorModel("%06X".format(it * 500_000)) })
                binding.root.measure(
                    View.MeasureSpec.makeMeasureSpec(720, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(1600, View.MeasureSpec.EXACTLY)
                )
                binding.root.layout(0, 0, 720, 1600)
                assertTrue("Color bar overflows its start anchor", binding.cardColor.left >= 0)
                assertTrue("Color bar overflows its end anchor", binding.cardColor.right <= binding.llColor.width)
                val frame = binding.llColor.getChildAt(0)
                assertEquals(binding.cardColor.left, frame.left)
                assertEquals(binding.cardColor.right, frame.right)
                return binding.cardColor.width
            }

            val smallWidth = showColors(2)
            assertTrue(smallWidth > 0)
            assertFalse(binding.rcvColor.canScrollHorizontally(1))
            val largeWidth = showColors(24)
            assertTrue("Long palette should expand beyond a two-color palette", largeWidth > smallWidth)
            assertTrue("Long palette must scroll inside the bounded bar", binding.rcvColor.canScrollHorizontally(1))
            binding.rcvColor.scrollBy(10_000, 0)
            val manager = binding.rcvColor.layoutManager as LinearLayoutManager
            assertEquals(23, manager.findLastCompletelyVisibleItemPosition())
            assertEquals("Short palette should shrink again", smallWidth, showColors(2))
        }
    }
}
