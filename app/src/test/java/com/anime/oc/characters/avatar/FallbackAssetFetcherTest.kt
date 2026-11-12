package com.anime.oc.characters.av

import com.anime.oc.characters.av.utils.FallbackAssetFetcher
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.data.DataFetcher
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.io.InputStream

class FallbackAssetFetcherTest {
    private class Fetcher : DataFetcher<InputStream> {
        var callback: DataFetcher.DataCallback<in InputStream>? = null
        var cleaned = false
        var cancelled = false
        override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in InputStream>) {
            this.callback = callback
        }
        override fun cleanup() { cleaned = true }
        override fun cancel() { cancelled = true }
        override fun getDataClass() = InputStream::class.java
        override fun getDataSource() = DataSource.REMOTE
    }

    private class Callback : DataFetcher.DataCallback<InputStream> {
        var data: InputStream? = null
        var error: Exception? = null
        override fun onDataReady(data: InputStream?) { this.data = data }
        override fun onLoadFailed(e: Exception) { error = e }
    }

    @Test
    fun primaryImageSuccessDoesNotRequestFallback() {
        val primary = Fetcher()
        val fallback = Fetcher()
        val callback = Callback()
        FallbackAssetFetcher(primary, fallback).loadData(Priority.NORMAL, callback)
        val image = byteArrayOf(1).inputStream()
        primary.callback!!.onDataReady(image)
        assertSame(image, callback.data)
        assertNull(fallback.callback)
        assertNull(callback.error)
    }

    @Test
    fun failedImageRetriesFallbackBeforeReportingFailureAndCleansBoth() {
        val primary = Fetcher()
        val fallback = Fetcher()
        val callback = Callback()
        val fetcher = FallbackAssetFetcher(primary, fallback)
        fetcher.loadData(Priority.NORMAL, callback)
        primary.callback!!.onLoadFailed(IOException("HTTP 404"))
        assertTrue(primary.cleaned)
        assertNotNull(fallback.callback)
        assertNull(callback.error)
        val image = byteArrayOf(2).inputStream()
        fallback.callback!!.onDataReady(image)
        assertSame(image, callback.data)
        fetcher.cleanup()
        assertTrue(fallback.cleaned)
    }

    @Test
    fun bothHostsFailReportsFallbackError() {
        val primary = Fetcher()
        val fallback = Fetcher()
        val callback = Callback()
        FallbackAssetFetcher(primary, fallback).loadData(Priority.NORMAL, callback)
        primary.callback!!.onLoadFailed(IOException("HTTP 500"))
        val error = IOException("Fallback offline")
        fallback.callback!!.onLoadFailed(error)
        assertSame(error, callback.error)
    }

    @Test
    fun cancellationPreventsFallbackAfterPrimaryFailure() {
        val primary = Fetcher()
        val fallback = Fetcher()
        val callback = Callback()
        val fetcher = FallbackAssetFetcher(primary, fallback)
        fetcher.loadData(Priority.NORMAL, callback)
        fetcher.cancel()
        primary.callback!!.onLoadFailed(IOException("Timeout"))
        assertTrue(primary.cancelled)
        assertTrue(fallback.cancelled)
        assertNull(fallback.callback)
        assertNull(callback.error)
    }
}
