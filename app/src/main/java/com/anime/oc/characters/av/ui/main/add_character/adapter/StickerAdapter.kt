package com.anime.oc.characters.av.ui.main.add_character.adapter

import com.anime.oc.characters.av.core.base.BaseAdapter
import com.anime.oc.characters.av.core.extention.onClick
import com.anime.oc.characters.av.data.model.addcharacter.SelectedAddModel
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.anime.oc.characters.av.R
import com.anime.oc.characters.av.databinding.ItemStickerBinding
import com.anime.oc.characters.av.utils.DataLocal
import com.facebook.shimmer.ShimmerDrawable

class StickerAdapter : BaseAdapter<SelectedAddModel, ItemStickerBinding>(ItemStickerBinding::inflate) {
    var onItemClick: ((String) -> Unit) = {}
    var currentSelected = -1

    override fun onBind(binding: ItemStickerBinding, item: SelectedAddModel, position: Int) {
        val shimmerDrawable = ShimmerDrawable().apply { setShimmer(DataLocal.shimmer1) }
        binding.apply {
            if (imvImage.tag != item.path) {
                imvImage.tag = item.path
                Glide.with(imvImage)
                    .load(item.path)
                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                    .override(256)
                    .placeholder(shimmerDrawable)
                    .into(imvImage)
            }
            root.onClick {
                selectItem(position)          // ← was missing entirely
                onItemClick.invoke(item.path)
            }
        }
    }

    fun selectItem(position: Int) {           // ← changed private → public
        if (position == currentSelected) return
        val old = currentSelected
        currentSelected = position
        if (old in items.indices) notifyItemChanged(old)
        if (position in items.indices) notifyItemChanged(position)
    }
}
