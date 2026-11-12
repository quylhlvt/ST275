package com.anime.oc.characters.av

import android.content.Context
import android.content.ContextWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.anime.oc.characters.av.data.datalocal.manager.AppDataManager
import com.anime.oc.characters.av.data.model.custom.CustomModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class CreationPersistenceTest {
    // Isolate the fixtures from the user's creations and MMKV store.
    private fun isolatedContext(): Context {
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(app.cacheDir, "creation_test_${UUID.randomUUID()}").apply { mkdirs() }
        return object : ContextWrapper(app) {
            override fun getFilesDir(): File = directory
            override fun getSharedPreferences(name: String, mode: Int) =
                super.getSharedPreferences("${directory.name}_$name", mode)
        }
    }

    @Test
    fun avatarRestoresWithoutItsOnlineTemplate() = runBlocking {
        val context = isolatedContext()
        val image = File(context.filesDir, "avatar.png").apply { writeBytes(byteArrayOf(1)) }
        val original = CustomModel(
            id = "saved_avatar", templateId = "online_data_2", imageSave = image.absolutePath
        )
        AppDataManager(context).updateCustomizedCharacter(original)

        val restarted = AppDataManager(context)
        restarted.loadQuickData()
        assertEquals(listOf(original.id), restarted.customizedCharacters.value.map { it.id })
        assertTrue(File(restarted.customizedCharacters.value.single().imageSave).exists())
    }

    @Test
    fun savingBeforeInitialLoadRetainsPreviousCreations() = runBlocking {
        val context = isolatedContext()
        AppDataManager(context).apply {
            updateCustomizedCharacter(CustomModel(id = "first"))
            addMyDesignPath("first.png")
        }
        AppDataManager(context).apply {
            updateCustomizedCharacter(CustomModel(id = "second"))
            addMyDesignPath("second.png")
        }
        val restarted = AppDataManager(context)
        restarted.loadQuickData()
        assertEquals(setOf("first", "second"), restarted.customizedCharacters.value.map { it.id }.toSet())
        assertEquals(listOf("second.png", "first.png"), restarted.myDesignPaths.value)
    }

    @Test
    fun concurrentSavesRetainEveryCreation() = runBlocking {
        val context = isolatedContext()
        val manager = AppDataManager(context)
        (1..12).map { index ->
            async(Dispatchers.IO) {
                manager.updateCustomizedCharacter(CustomModel(id = "avatar_$index"))
                manager.addMyDesignPath("design_$index.png")
            }
        }.awaitAll()
        val restarted = AppDataManager(context)
        restarted.loadQuickData()
        assertEquals(12, restarted.customizedCharacters.value.size)
        assertEquals(12, restarted.myDesignPaths.value.size)
    }

    @Test
    fun deletionUpdatesMemoryAndSurvivesReload() = runBlocking {
        val context = isolatedContext()
        val manager = AppDataManager(context)
        manager.updateCustomizedCharacter(CustomModel(id = "delete_me"))
        manager.addMyDesignPath("delete_me.png")
        manager.deleteCustomizedCharacter("delete_me")
        manager.saveMyDesignToJson(emptyList())
        assertTrue(manager.customizedCharacters.value.isEmpty())
        assertTrue(manager.myDesignPaths.value.isEmpty())
        val restarted = AppDataManager(context)
        restarted.loadQuickData()
        assertTrue(restarted.customizedCharacters.value.isEmpty())
        assertTrue(restarted.myDesignPaths.value.isEmpty())
    }
}
