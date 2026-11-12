package com.anime.oc.characters.av.ui.main.createPony

import com.anime.oc.characters.av.core.base.BaseAdapter
import com.anime.oc.characters.av.data.model.custom.CustomModel
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.anime.oc.characters.av.R
import com.anime.oc.characters.av.databinding.ItemChooseBinding
import com.anime.oc.characters.av.utils.DataLocal
import com.facebook.shimmer.ShimmerDrawable

class ChoosePonyAdapter(
    private val onClick: (character: CustomModel, position: Int) -> Unit
) : BaseAdapter<CustomModel, ItemChooseBinding>(ItemChooseBinding::inflate) {

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long =
        items.getOrNull(position)?.id?.hashCode()?.toLong() ?: position.toLong()

    override fun onBind(binding: ItemChooseBinding, item: CustomModel, position: Int) {
        val shimmerDrawable = ShimmerDrawable().apply { setShimmer(DataLocal.shimmer) }
        if (binding.imvImage.tag != item.avatar) {
            binding.imvImage.tag = item.avatar
            Glide.with(binding.imvImage)
                .load(item.avatar)
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .override(512)
                .dontAnimate()
                .placeholder(shimmerDrawable)
                .into(binding.imvImage)
        }

        binding.root.setOnClickListener { onClick(item, position) }
    }
}
