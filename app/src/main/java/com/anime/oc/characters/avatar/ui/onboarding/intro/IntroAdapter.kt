package com.anime.oc.characters.avatar.ui.onboarding.intro

import android.text.TextUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.anime.oc.characters.avatar.core.extention.strings
import com.anime.oc.characters.avatar.data.model.intro.IntroModel
import com.anime.oc.characters.avatar.databinding.ItemIntroBinding
import javax.inject.Inject
//import kotlin.io.root

private val pageIntroDiff = object : DiffUtil.ItemCallback<IntroModel>() {
    override fun areItemsTheSame(oldItem: IntroModel, newItem: IntroModel) =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: IntroModel, newItem: IntroModel) = oldItem == newItem
}

class IntroAdapter@Inject constructor() :
    ListAdapter<IntroModel, PagerIntroViewHolder>(pageIntroDiff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        PagerIntroViewHolder(
            ItemIntroBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )

    override fun onBindViewHolder(holder: PagerIntroViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
class PagerIntroViewHolder(
    private val binding: ItemIntroBinding
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(pager: IntroModel) {
        binding.run {
//            if (adapterPosition == 1) {
//                // Page 0 — hiện 2 dòng, không marquee
//                tvContent.maxLines = 2
//                tvContent.ellipsize = null
//                tvContent.isSelected = false
//                tvContent.isSingleLine = false
//            } else {
                // Page 1, 2 — chạy chữ marquee
                tvContent.maxLines = 1
                tvContent.isSingleLine = true
                tvContent.ellipsize = TextUtils.TruncateAt.MARQUEE
                tvContent.isSelected = true
//            }
            tvContent.text = root.context.strings(pager.content)
            imvImage.setImageResource(pager.image)
//            loadImage(root, pager.image, imvImage, false)
//            loadImage(root, pager.imageBottom, imvImageBottom, false)
        }
    }
}
