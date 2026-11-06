package com.anime.oc.characters.avatar

import com.anime.oc.characters.avatar.data.datalocal.api.AppAssetUrls
import com.anime.oc.characters.avatar.data.model.addcharacter.SpeechCategoryModel
import com.anime.oc.characters.avatar.data.model.addcharacter.StickerCategoryModel
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.CancellationException

class AppAssetFallbackTest {
    @Test
    fun fallbackPreservesAssetPathEncodingAndQuery() {
        val path = "bg/speech%20bubble/cat/1.png?v=2"
        assertEquals(AppAssetUrls.FALLBACK + path, AppAssetUrls.fallbackFor(AppAssetUrls.PRIMARY + path))
        assertNull(AppAssetUrls.fallbackFor(AppAssetUrls.FALLBACK + path))
        assertNull(AppAssetUrls.fallbackFor("https://lvtglobal.tech/public/app/another/1.png"))
        assertNull(AppAssetUrls.fallbackFor("file:///saved/avatar.png"))
    }

    @Test
    fun configKeepsPrimaryWhenSuccessfulAndRetriesForNetworkOrInvalidData() {
        val primary = AppAssetUrls.PRIMARY + "bg/bg.json"
        val attempts = mutableListOf<String>()
        assertEquals("config", AppAssetUrls.loadWithFallback(primary) {
            attempts += it
            "config"
        })
        assertEquals(listOf(primary), attempts)

        for (error in listOf(IOException("HTTP 503"), SocketTimeoutException(), IllegalStateException("Empty config"))) {
            attempts.clear()
            assertEquals("config", AppAssetUrls.loadWithFallback(primary) {
                attempts += it
                if (it == primary) throw error
                "config"
            })
            assertEquals(listOf(primary, AppAssetUrls.FALLBACK + "bg/bg.json"), attempts)
        }
    }

    @Test
    fun cancellationDoesNotStartFallbackAndFallbackFailureIsReturned() {
        val primary = AppAssetUrls.PRIMARY + "bg/bg.json"
        var attempts = 0
        assertThrows(CancellationException::class.java) {
            AppAssetUrls.loadWithFallback(primary) { attempts++; throw CancellationException() }
        }
        assertEquals(1, attempts)

        val fallbackFailure = IOException("Fallback offline")
        val thrown = assertThrows(IOException::class.java) {
            AppAssetUrls.loadWithFallback(primary) {
                if (it == primary) throw SocketTimeoutException()
                throw fallbackFailure
            }
        }
        assertSame(fallbackFailure, thrown)
    }

    @Test
    fun categorySelectionKeepsHostUsedToLoadConfig() {
        assertEquals(listOf(AppAssetUrls.FALLBACK + "bg/sticker/cat/1.png"),
            StickerCategoryModel("cat", 1, assetBaseUrl = AppAssetUrls.FALLBACK).imageUrls())
        assertEquals(listOf(AppAssetUrls.FALLBACK + "bg/speech%20bubble/cat/1.png"),
            SpeechCategoryModel("cat", 1, assetBaseUrl = AppAssetUrls.FALLBACK).imageUrls())
    }
}
