package com.anime.oc.characters.avatar.ui.main.add_character.adapter

import com.anime.oc.characters.avatar.core.base.BaseAdapter
import com.anime.oc.characters.avatar.core.extention.onClick
import com.anime.oc.characters.avatar.data.model.addcharacter.SelectedAddModel
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.anime.oc.characters.avatar.R
import com.anime.oc.characters.avatar.databinding.ItemSpeechBinding

class SpeechAdapter  : BaseAdapter<SelectedAddModel, ItemSpeechBinding>(ItemSpeechBinding::inflate) {
    var onItemClick: ((String) -> Unit) = {}
    var currentSelected = -1

    override fun onBind(binding: ItemSpeechBinding, item: SelectedAddModel, position: Int) {
        binding.apply {
            if (imvImage.tag != item.path) {
                imvImage.tag = item.path
                Glide.with(imvImage)
                    .load(item.path)
                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                    .override(256)
                    .dontAnimate()
                    .placeholder(R.color.white)
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
