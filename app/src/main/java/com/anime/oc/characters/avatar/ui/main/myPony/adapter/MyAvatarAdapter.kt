package com.anime.oc.characters.avatar.ui.main.myPony.adapter

import android.content.Context
import com.anime.oc.characters.avatar.core.base.BaseAdapter
import com.anime.oc.characters.avatar.core.extention.gone
import com.anime.oc.characters.avatar.core.extention.loadImage
import com.anime.oc.characters.avatar.core.extention.onClick
import com.anime.oc.characters.avatar.core.extention.visible
import com.anime.oc.characters.avatar.data.model.mypony.MyAlbumModel
import com.anime.oc.characters.avatar.R
import com.anime.oc.characters.avatar.databinding.ItemMyAvatarBinding


class MyAvatarAdapter(val context: Context) :
    BaseAdapter<MyAlbumModel, ItemMyAvatarBinding>(ItemMyAvatarBinding::inflate) {
    var onItemClick: ((MyAlbumModel) -> Unit) = {}
    var onLongClick: ((Int) -> Unit) = {}
    var onItemTick: ((Int) -> Unit) = {}

    var onEditClick: ((String) -> Unit) = {}
    var onDeleteClick: ((String) -> Unit) = {}

    override fun onBind(binding: ItemMyAvatarBinding, item: MyAlbumModel, position: Int) {
        binding.apply {
            loadImage(root, item.path, imvImage)

            if (item.isShowSelection) {
                showDownSelect.visible()
                btnSelect.visible()
                btnEdit.gone()
                btnDelete.gone()
            } else {
                btnSelect.gone()
                btnEdit.visible()
                btnDelete.visible()
            }
            if (item.isSelected) showDownSelect.visible() else showDownSelect.gone()
            btnSelect.setImageResource(
                if (item.isSelected) R.drawable.ic_selected else R.drawable.ic_not_select
            )


            // Click luôn navigate, không check selection mode
            root.onClick { onItemClick.invoke(item) }

            root.setOnLongClickListener {
                if (items.any { it.isShowSelection }) return@setOnLongClickListener false
                onLongClick.invoke(position)
                true
            }

            btnEdit.onClick { onEditClick.invoke(item.idEdit) }
            btnDelete.onClick { onDeleteClick.invoke(item.path) }
            btnSelect.onClick { onItemTick.invoke(position) } // chỉ tick button mới toggle
        }
    }
}
