package com.anime.oc.characters.avatar.ui.main.add_character.adapter

import com.anime.oc.characters.avatar.core.base.BaseAdapter
import com.anime.oc.characters.avatar.core.extention.gone
import com.anime.oc.characters.avatar.core.extention.onClick
import com.anime.oc.characters.avatar.core.extention.visible
import com.anime.oc.characters.avatar.data.model.addcharacter.SelectedAddModel
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.anime.oc.characters.avatar.R
import com.anime.oc.characters.avatar.databinding.ItemBackgroundImageBinding


class BackgroundImageAdapter : BaseAdapter<SelectedAddModel, ItemBackgroundImageBinding>(
    ItemBackgroundImageBinding::inflate
) {
    var onAddImageClick: (() -> Unit) = {}
    var onNoneImageClick: (() -> Unit) = {}
    var onBackgroundImageClick: ((String, Int) -> Unit) = { _, _ -> }
    var currentSelected = -1

    override fun onBind(binding: ItemBackgroundImageBinding, item: SelectedAddModel, position: Int) {
        binding.apply {
            tvAddImage.isSelected = true
            if (currentSelected == position) {
                materiaForcus.visible()
            } else {
                materiaForcus.gone()
            }
            if (position == ADD_BACKGROUND_POSITION) {
                clearRemoteImageIfNeeded(imvImage, ADD_ITEM_TAG)
                lnlAddItem.visible()
                imvImage.gone()
                imvImageNone.gone()
                lnlAddItem.onClick { onAddImageClick() }
            } else if (position == NONE_BACKGROUND_POSITION) {
                clearRemoteImageIfNeeded(imvImage, NONE_ITEM_TAG)
                imvImageNone.visible()
                imvImage.gone()
                lnlAddItem.gone()
                imvImageNone.onClick { onNoneImageClick() }
            } else {
                lnlAddItem.gone()
                imvImageNone.gone()
                imvImage.visible()
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
                imvImage.onClick { onBackgroundImageClick(item.path, position) }
            }
        }
    }

    private fun clearRemoteImageIfNeeded(imageView: android.widget.ImageView, tag: String) {
        if (imageView.tag == tag) return
        Glide.with(imageView).clear(imageView)
        imageView.tag = tag
    }

    fun selectItem(position: Int) {
        if (position == currentSelected) return
        val old = currentSelected
        currentSelected = position
        if (old in items.indices) notifyItemChanged(old)
        if (position in items.indices) notifyItemChanged(position)
    }

    fun clearSelection() {
        if (currentSelected < 0) return
        val old = currentSelected
        currentSelected = -1
        if (old in items.indices) notifyItemChanged(old)
    }

    private companion object {
        const val ADD_BACKGROUND_POSITION = 0
        const val NONE_BACKGROUND_POSITION = 1
        const val ADD_ITEM_TAG = "background_add_item"
        const val NONE_ITEM_TAG = "background_none_item"
    }
}
