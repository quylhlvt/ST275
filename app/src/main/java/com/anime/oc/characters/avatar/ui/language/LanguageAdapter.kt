package com.anime.oc.characters.avatar.ui.language

import android.annotation.SuppressLint
import android.content.Context
import com.anime.oc.characters.avatar.core.base.BaseAdapter
import com.anime.oc.characters.avatar.core.extention.onClick
import com.anime.oc.characters.avatar.data.model.language.LanguageModel
import com.anime.oc.characters.avatar.R
import com.anime.oc.characters.avatar.databinding.ItemLanguageBinding

class LanguageAdapter (val context: Context) : BaseAdapter<LanguageModel, ItemLanguageBinding>(
    ItemLanguageBinding::inflate
) {
    var onItemClick: ((String) -> Unit) = {}
    override fun onBind(
        binding: ItemLanguageBinding, item: LanguageModel, position: Int
    ) {
        binding.apply {
            imvFlag.setImageResource(item.flag)

            imgLangFor.setImageResource(if (item.activate) R.drawable.frame_selected_language else R.drawable.frame_unselect_language)
            btnRadio.setImageResource(if (item.activate) R.drawable.ic_select_lang else R.drawable.ic_un_select_lang)
            tvLang.text = item.name
            root.onClick { onItemClick.invoke(item.code) }
        }
    }
    @SuppressLint("NotifyDataSetChanged")
    fun submitItem(position: Int) {
        val oldSelected = items.indexOfFirst { it.activate }
        items.forEach { it.activate = false }
        items[position].activate = true
        // Chỉ update 2 item thay đổi, không redraw toàn bộ list
        if (oldSelected >= 0) notifyItemChanged(oldSelected)
        notifyItemChanged(position)
    }
}