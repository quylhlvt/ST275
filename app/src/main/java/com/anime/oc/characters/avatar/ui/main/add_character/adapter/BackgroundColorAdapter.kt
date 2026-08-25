package com.anime.oc.characters.avatar.ui.main.add_character.adapter

import com.anime.oc.characters.avatar.core.base.BaseAdapter
import com.anime.oc.characters.avatar.core.extention.gone
import com.anime.oc.characters.avatar.core.extention.onClick
import com.anime.oc.characters.avatar.core.extention.visible
import com.anime.oc.characters.avatar.data.model.addcharacter.SelectedAddModel
import com.anime.oc.characters.avatar.databinding.ItemBackgroundColorBinding


class BackgroundColorAdapter : BaseAdapter<SelectedAddModel, ItemBackgroundColorBinding>(
    ItemBackgroundColorBinding::inflate
) {
    var onChooseColorClick: (() -> Unit) = {}
    var onBackgroundColorClick: ((Int, Int) -> Unit) = { _, _ -> }
    var currentSelected = -1

    override fun onBind(binding: ItemBackgroundColorBinding, item: SelectedAddModel, position: Int) {
        binding.apply {
            if (currentSelected == position) {
                materiaForcus.visible()
            } else {
                materiaForcus.gone()
            }
            if (position == 0) {
                imvAddColor.visible()
                imvColor.gone()
                root.onClick { onChooseColorClick() }
            } else {
                imvAddColor.gone()
                imvColor.visible()
                imvColor.setBackgroundColor(item.color)
                root.onClick { onBackgroundColorClick(item.color, position) }
            }

        }
    }

    fun selectItem(position: Int) {
        if (position == currentSelected) return
        val old = currentSelected
        currentSelected = position
        if (old >= 0) notifyItemChanged(old)
        if (position >= 0) notifyItemChanged(position)
    }

    fun clearSelection() {
        if (currentSelected < 0) return
        val old = currentSelected
        currentSelected = -1
        notifyItemChanged(old)
    }
}
