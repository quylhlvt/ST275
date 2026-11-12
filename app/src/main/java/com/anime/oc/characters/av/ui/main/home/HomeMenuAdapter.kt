package com.anime.oc.characters.av.ui.main.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import com.anime.oc.characters.av.R
import com.anime.oc.characters.av.databinding.ItemHomeMenuBinding

enum class HomeMenuAction {
    CREATE, COSPLAY, RANDOM, MY_ALBUM
}

data class HomeMenuItem(
    @StringRes val titleRes: Int,
    @DrawableRes val iconRes: Int,
    val action: HomeMenuAction
)

class HomeMenuAdapter(
    private val onItemClick: (HomeMenuAction) -> Unit
) : RecyclerView.Adapter<HomeMenuAdapter.HomeMenuViewHolder>() {

    private val items = listOf(
        HomeMenuItem(R.string.pony_maker,R.drawable.home1, HomeMenuAction.CREATE),
        HomeMenuItem(R.string.cosplay, R.drawable.home2,HomeMenuAction.COSPLAY),
        HomeMenuItem(R.string.cat_random, R.drawable.home3,HomeMenuAction.RANDOM),
        HomeMenuItem(R.string.my_creation, R.drawable.home4,HomeMenuAction.MY_ALBUM)
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeMenuViewHolder {
        val binding = ItemHomeMenuBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HomeMenuViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HomeMenuViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class HomeMenuViewHolder(
        private val binding: ItemHomeMenuBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: HomeMenuItem) {
            binding.tvTitle.isSelected = true
            binding.tvTitle.setText(item.titleRes)
            binding.imgHome.setImageResource(item.iconRes)
            binding.root.setOnClickListener { onItemClick(item.action) }
        }
    }
}
