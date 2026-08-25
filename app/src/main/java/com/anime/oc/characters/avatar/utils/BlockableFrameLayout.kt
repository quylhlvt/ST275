package com.anime.oc.characters.avatar.utils

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout


class BlockableFrameLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    var isBlocked = false

    override fun addView(child: View) {
        if (isBlocked) return
        super.addView(child)
    }

    override fun addView(child: View, index: Int) {
        if (isBlocked) return
        super.addView(child, index)
    }

    override fun addView(child: View, params: ViewGroup.LayoutParams) {
        if (isBlocked) return
        super.addView(child, params)
    }
}
