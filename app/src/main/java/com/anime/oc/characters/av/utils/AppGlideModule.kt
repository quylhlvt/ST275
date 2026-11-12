package com.anime.oc.characters.av.utils
import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory
import com.bumptech.glide.load.engine.cache.LruResourceCache
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.load.model.GlideUrl
import java.io.InputStream

@GlideModule
class AppGlideModule : AppGlideModule() {
    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        registry.replace(GlideUrl::class.java, InputStream::class.java, FallbackAssetLoader.Factory())
    }

    override fun applyOptions(context: Context, builder: GlideBuilder) {
        // Memory cache: 50MB
        builder.setMemoryCache(LruResourceCache(50L * 1024 * 1024))
        // Disk cache: 200MB — đủ cache toàn bộ ảnh template API
        builder.setDiskCache(InternalCacheDiskCacheFactory(context, 200L * 1024 * 1024))
    }
}
