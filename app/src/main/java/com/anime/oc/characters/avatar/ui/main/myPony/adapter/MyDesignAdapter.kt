package com.anime.oc.characters.avatar.ui.main.myPony.adapter

import com.anime.oc.characters.avatar.core.base.BaseAdapter
import com.anime.oc.characters.avatar.core.extention.gone
import com.anime.oc.characters.avatar.core.extention.loadImage
import com.anime.oc.characters.avatar.core.extention.onClick
import com.anime.oc.characters.avatar.core.extention.visible
import com.anime.oc.characters.avatar.data.model.mypony.MyAlbumModel
import com.anime.oc.characters.avatar.R
import com.anime.oc.characters.avatar.databinding.ItemMyDesignBinding

class MyDesignAdapter() : BaseAdapter<MyAlbumModel, ItemMyDesignBinding>(ItemMyDesignBinding::inflate) {
    var onItemClick: ((String) -> Unit) = {}
    var onLongClick: ((Int) -> Unit) = {}
    var onItemTick: ((Int) -> Unit) = {}

    var onDeleteClick: ((String) -> Unit) = {}

    override fun onBind(binding: ItemMyDesignBinding, item: MyAlbumModel, position: Int) {
        binding.apply {
            loadImage(root, item.path, imvImage)

            if (item.isShowSelection) {
                btnSelect.visible()
                btnDelete.gone()
            } else {
                btnSelect.gone()
                btnDelete.visible()
            }
            if (item.isSelected) showDownSelect.visible() else showDownSelect.gone()
            btnSelect.setImageResource(
                if (item.isSelected) R.drawable.ic_selected else R.drawable.ic_not_select
            )
            // Click luôn navigate
            root.onClick { onItemClick.invoke(item.path) }

            root.setOnLongClickListener {
                if (items.any { it.isShowSelection }) return@setOnLongClickListener false
                onLongClick.invoke(position)
                true
            }

            btnDelete.onClick { onDeleteClick.invoke(item.path) }
            btnSelect.onClick { onItemTick.invoke(position) }
        }
    }
}
