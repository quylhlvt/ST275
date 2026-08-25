package com.anime.oc.characters.avatar.ui.main.add_character.adapter

import androidx.core.content.ContextCompat
import com.anime.oc.characters.avatar.R
import com.anime.oc.characters.avatar.core.base.BaseAdapter

import com.anime.oc.characters.avatar.core.extention.onClick
import com.anime.oc.characters.avatar.data.model.addcharacter.SpeechCategoryModel
import com.anime.oc.characters.avatar.databinding.ItemTittleBackgroundImageBinding

class SpeechCategoryAdapter :
    BaseAdapter<SpeechCategoryModel, ItemTittleBackgroundImageBinding>(ItemTittleBackgroundImageBinding::inflate) {
    var onCategoryClick: ((SpeechCategoryModel, Int) -> Unit) = { _, _ -> }

    override fun onBind(binding: ItemTittleBackgroundImageBinding, item: SpeechCategoryModel, position: Int) {
        val context = binding.root.context
        binding.apply {
            txtTittle.text = context.getString(R.string.bubbles)+" "+item.category
            if (item.isSelected) {
                txtTittle.setTextColor(ContextCompat.getColor(context,R.color.app_color))
                frameTittle.background = ContextCompat.getDrawable(context, R.drawable.bg_8_title)
            } else {
                txtTittle.setTextColor(ContextCompat.getColor(context,R.color.white))
                frameTittle.background = null
            }
        }
        binding.root.onClick { onCategoryClick(item, position) }
    }
}
