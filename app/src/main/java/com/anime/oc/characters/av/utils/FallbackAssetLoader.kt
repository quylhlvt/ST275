package com.anime.oc.characters.av.utils

import com.anime.oc.characters.av.data.datalocal.api.AppAssetUrls
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.data.DataFetcher
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.Headers
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.bumptech.glide.load.model.stream.HttpGlideUrlLoader
import java.io.InputStream

class FallbackAssetLoader : ModelLoader<GlideUrl, InputStream> {
    private val delegate = HttpGlideUrlLoader()

    override fun handles(model: GlideUrl) = delegate.handles(model)

    override fun buildLoadData(
        model: GlideUrl, width: Int, height: Int, options: Options
    ): ModelLoader.LoadData<InputStream>? {
        val primary = delegate.buildLoadData(model, width, height, options) ?: return null
        val fallbackUrl = AppAssetUrls.fallbackFor(model.toStringUrl()) ?: return primary
        val fallbackModel = GlideUrl(fallbackUrl, object : Headers {
            override fun getHeaders(): Map<String, String> = model.headers
        })
        val fallback = delegate.buildLoadData(fallbackModel, width, height, options) ?: return primary
        // Keep the original cache key so saved URLs and preloads share the same cache.
        return ModelLoader.LoadData(primary.sourceKey, FallbackAssetFetcher(primary.fetcher, fallback.fetcher))
    }

    class Factory : ModelLoaderFactory<GlideUrl, InputStream> {
        override fun build(multiFactory: MultiModelLoaderFactory) = FallbackAssetLoader()
        override fun teardown() = Unit
    }
}

internal class FallbackAssetFetcher(
    private val primary: DataFetcher<InputStream>,
    private val fallback: DataFetcher<InputStream>
) : DataFetcher<InputStream> {
    @Volatile private var cancelled = false

    override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in InputStream>) {
        if (cancelled) return
        primary.loadData(priority, object : DataFetcher.DataCallback<InputStream> {
            override fun onDataReady(data: InputStream?) {
                if (!cancelled) callback.onDataReady(data)
            }

            override fun onLoadFailed(e: Exception) {
                if (!cancelled) {
                    primary.cleanup()
                    fallback.loadData(priority, object : DataFetcher.DataCallback<InputStream> {
                        override fun onDataReady(data: InputStream?) {
                            if (!cancelled) callback.onDataReady(data)
                        }

                        override fun onLoadFailed(e: Exception) {
                            if (!cancelled) callback.onLoadFailed(e)
                        }
                    })
                }
            }
        })
    }

    override fun cleanup() {
        primary.cleanup()
        fallback.cleanup()
    }

    override fun cancel() {
        cancelled = true
        primary.cancel()
        fallback.cancel()
    }

    override fun getDataClass(): Class<InputStream> = InputStream::class.java
    override fun getDataSource(): DataSource = DataSource.REMOTE
}
