package com.anime.oc.characters.av.data.model.addcharacter

import com.anime.oc.characters.av.data.datalocal.api.AppAssetUrls

data class StickerCategoryModel(
    val category: String,
    val quantity: Int,
    var isSelected: Boolean = false,
    val assetBaseUrl: String = AppAssetUrls.PRIMARY
) {
    fun imageUrls(): List<String> = (1..quantity).map { index ->
        "${assetBaseUrl}bg/sticker/$category/$index.png"
    }
}
