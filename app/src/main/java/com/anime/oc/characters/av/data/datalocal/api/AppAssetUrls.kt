package com.anime.oc.characters.av.data.datalocal.api

import java.util.concurrent.CancellationException

object AppAssetUrls {
    const val PRIMARY = ApiConfig.BASE_URL_1 + ApiConfig.BASE_CONNECT
    const val FALLBACK = ApiConfig.BASE_URL_2 + ApiConfig.BASE_CONNECT

    fun fallbackFor(url: String): String? =
        if (url.startsWith(PRIMARY)) FALLBACK + url.removePrefix(PRIMARY) else null

    fun <T> loadWithFallback(url: String, load: (String) -> T): T {
        return try {
            load(url)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            val fallback = fallbackFor(url) ?: throw e
            load(fallback)
        }
    }
}
