package com.anime.oc.characters.avatar.ui.main.customize

import android.graphics.Color
import android.view.View
import androidx.core.graphics.drawable.DrawableCompat
import com.anime.oc.characters.avatar.R
import com.anime.oc.characters.avatar.core.base.BaseAdapter
import com.anime.oc.characters.avatar.data.model.custom.BodyPartModel
import com.anime.oc.characters.avatar.data.model.custom.ColorModel
import com.anime.oc.characters.avatar.databinding.ItemBottomCustomBinding
import com.anime.oc.characters.avatar.databinding.ItemColorBinding
import com.anime.oc.characters.avatar.databinding.ItemLayerBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy

private object SelectionChangedPayload

// ── NAV ADAPTER ───────────────────────────────────────────────────────────────
class NavAdapter :
    BaseAdapter<BodyPartModel, ItemBottomCustomBinding>(ItemBottomCustomBinding::inflate) {

    var posNav = 0
    var onClick: ((Int) -> Unit)? = null

    fun setPos(pos: Int) {
        val old = posNav
        posNav = pos
        if (old != pos) {
            if (old in items.indices) notifyItemChanged(old, SelectionChangedPayload)
            if (pos in items.indices) notifyItemChanged(pos, SelectionChangedPayload)
        }
    }

    private fun bindFocus(binding: ItemBottomCustomBinding, position: Int) {
        binding.forcus.visibility = if (posNav == position) View.VISIBLE else View.INVISIBLE
    }

    override fun onBind(binding: ItemBottomCustomBinding, item: BodyPartModel, position: Int) {
        binding.apply {
            bindFocus(binding, position)
            if (imvImage.tag != item.nav) {
                imvImage.tag = item.nav
                Glide.with(imvImage)
                    .load(item.nav)
                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                    .override(256)
                    .dontAnimate()
                    .placeholder(R.color.white)
                    .into(imvImage)
            }

            root.setOnClickListener { onClick?.invoke(position) }
        }
    }

    override fun onBindPayload(
        binding: ItemBottomCustomBinding,
        item: BodyPartModel,
        position: Int,
        payloads: List<Any>
    ) {
        if (payloads.all { it === SelectionChangedPayload }) {
            bindFocus(binding, position)
        } else {
            super.onBindPayload(binding, item, position, payloads)
        }
    }
}

// ── COLOR ADAPTER ─────────────────────────────────────────────────────────────
class ColorAdapter : BaseAdapter<ColorModel, ItemColorBinding>(ItemColorBinding::inflate) {

    var posColor = 0
    var onClick: ((Int) -> Unit)? = null

    fun setPos(pos: Int) {
        val old = posColor
        posColor = pos
        if (old != pos) {
            if (old in items.indices) notifyItemChanged(old, SelectionChangedPayload)
            if (pos in items.indices) notifyItemChanged(pos, SelectionChangedPayload)
        }
    }

    private fun bindFocus(binding: ItemColorBinding, position: Int) {
        binding.colorSelected.visibility =
            if (posColor == position) View.VISIBLE else View.INVISIBLE
    }

    override fun onBind(binding: ItemColorBinding, item: ColorModel, position: Int) {
        bindFocus(binding, position)

        val colorInt = runCatching {
            Color.parseColor("#${item.color.removePrefix("#").ifEmpty { "FFFFFF" }}")
        }.getOrDefault(Color.WHITE)

        DrawableCompat.setTint(binding.viewColor.background.mutate(), colorInt)
        binding.root.setOnClickListener { onClick?.invoke(position) }
    }

    override fun onBindPayload(
        binding: ItemColorBinding,
        item: ColorModel,
        position: Int,
        payloads: List<Any>
    ) {
        if (payloads.all { it === SelectionChangedPayload }) {
            bindFocus(binding, position)
        } else {
            super.onBindPayload(binding, item, position, payloads)
        }
    }
}

// ── PART ADAPTER ──────────────────────────────────────────────────────────────
class PartAdapter : BaseAdapter<String, ItemLayerBinding>(ItemLayerBinding::inflate) {

    var posPath: Int = 0
    var listThumb: List<String> = emptyList()
    var onClick: ((Int, String) -> Unit)? = null

    fun setPos(pos: Int) {
        val old = posPath
        posPath = pos
        if (old != pos) {
            if (old in items.indices) notifyItemChanged(old, SelectionChangedPayload)
            if (pos in items.indices) notifyItemChanged(pos, SelectionChangedPayload)
        }
    }

    private fun bindFocus(binding: ItemLayerBinding, position: Int) {
        binding.forcus.visibility = if (posPath == position) View.VISIBLE else View.INVISIBLE
    }

    override fun onBind(binding: ItemLayerBinding, item: String, position: Int) {
        binding.apply {
            bindFocus(binding, position)
            when (item) {
                "none" -> {
                    if (imvImage.tag != item) {
                        Glide.with(imvImage).clear(imvImage)
                        imvImage.tag = item
                        imvImage.setImageResource(R.drawable.ic_none)
                    }
                }

                "dice" -> {
                    if (imvImage.tag != item) {
                        Glide.with(imvImage).clear(imvImage)
                        imvImage.tag = item
                        imvImage.setImageResource(R.drawable.ic_dice)
                    }
                }

                else -> {
                    val thumbPath = listThumb.getOrElse(position) { item }
                    if (imvImage.tag != thumbPath) {
                        imvImage.tag = thumbPath
                        Glide.with(imvImage)
                            .load(thumbPath)
                            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                            .override(256)
                            .placeholder(R.color.white)
                            .dontAnimate()
                            .into(imvImage)
                    }
                }
            }
            root.setOnClickListener { onClick?.invoke(position, item) }
        }
    }

    override fun onBindPayload(
        binding: ItemLayerBinding,
        item: String,
        position: Int,
        payloads: List<Any>
    ) {
        if (payloads.all { it === SelectionChangedPayload }) {
            bindFocus(binding, position)
        } else {
            super.onBindPayload(binding, item, position, payloads)
        }
    }
}
