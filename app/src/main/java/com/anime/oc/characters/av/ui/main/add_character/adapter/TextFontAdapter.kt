package com.anime.oc.characters.av.ui.main.add_character.adapter

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.content.ContextCompat
import com.anime.oc.characters.av.core.base.BaseAdapter
import com.anime.oc.characters.av.core.extention.onClick
import com.anime.oc.characters.av.core.extention.setFont
import com.anime.oc.characters.av.data.model.addcharacter.SelectedAddModel
import com.anime.oc.characters.av.R
import com.anime.oc.characters.av.databinding.ItemFontBinding


class TextFontAdapter(val context: Context) : BaseAdapter<SelectedAddModel, ItemFontBinding>(ItemFontBinding::inflate) {
    var onTextFontClick: ((Int, Int) -> Unit) = { _, _ -> }
    private var currentSelected = 0

    override fun onBind(binding: ItemFontBinding, item: SelectedAddModel, position: Int) {
        binding.apply {
            val color = ContextCompat.getColor(
                context,
                if (item.isSelected) R.color.app_color6 else R.color.white
            )
            frame.setGradientColors(color, color, color)
            tvFont.setFont(item.color)

            root.onClick { onTextFontClick.invoke(item.color, position) }
        }
    }

    fun submitItem(position: Int, list: ArrayList<SelectedAddModel>) {
        if (position != currentSelected) {
            items.clear()
            items.addAll(list)

            notifyItemChanged(currentSelected)
            notifyItemChanged(position)

            currentSelected = position
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun submitListReset(list: ArrayList<SelectedAddModel>){
        items.clear()
        items.addAll(list)
        currentSelected = list.indexOfFirst { it.isSelected }.takeIf { it >= 0 } ?: 0
        notifyDataSetChanged()
    }
}
